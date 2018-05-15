package org.apache.lucene.store;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.io.File;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.util.HashSet;
import java.util.Random;

/**
 * <p>Implements {@link LockFactory} using native OS file
 * locks.  Note that because this LockFactory relies on
 * java.nio.* APIs for locking, any problems with those APIs
 * will cause locking to fail.  Specifically, on certain NFS
 * environments the java.nio.* locks will fail (the lock can
 * incorrectly be double acquired) whereas {@link
 * SimpleFSLockFactory} worked perfectly in those same
 * environments.  For NFS based access to an index, it's
 * recommended that you try {@link SimpleFSLockFactory}
 * first and work around the one limitation that a lock file
 * could be left when the JVM exits abnormally.</p>
 *
 * <p>The primary benefit of {@link NativeFSLockFactory} is
 * that lock files will be properly removed (by the OS) if
 * the JVM has an abnormal exit.</p>
 * 
 * <p>Note that, unlike {@link SimpleFSLockFactory}, the existence of
 * leftover lock files in the filesystem on exiting the JVM
 * is fine because the OS will free the locks held against
 * these files even though the files still remain.</p>
 *
 * <p>If you suspect that this or any other LockFactory is
 * not working properly in your environment, you can easily
 * test it by using {@link VerifyingLockFactory}, {@link
 * LockVerifyServer} and {@link LockStressTest}.</p>
 *
 * @see LockFactory
 */

public class NativeFSLockFactory extends FSLockFactory {

  private volatile boolean tested = false;

  // Simple test to verify locking system is "working".  On
  // NFS, if it's misconfigured, you can hit long (35
  // second) timeouts which cause Lock.obtain to take far
  // too long (it assumes the obtain() call takes zero
  // time). 
  private synchronized void acquireTestLock() {
    if (tested) return;
    tested = true;
    
    // Ensure that lockDir exists and is a directory.
    if (!lockDir.exists()) {
      if (!lockDir.mkdirs())
        throw new RuntimeException("Cannot create directory: " +
                              lockDir.getAbsolutePath());
    } else if (!lockDir.isDirectory()) {
      throw new RuntimeException("Found regular file where directory expected: " + 
                            lockDir.getAbsolutePath());
    }

    String randomLockName = "lucene-" + Long.toString(new Random().nextInt(), Character.MAX_RADIX) + "-test.lock";
    
    Lock l = makeLock(randomLockName);
    try {
      l.obtain();
      l.release();
    } catch (IOException e) {
      RuntimeException e2 = new RuntimeException("Failed to acquire random test lock; please verify filesystem for lock directory '" + lockDir + "' supports locking");
      e2.initCause(e);
      throw e2;
    }    
  }

  /**
   * Create a NativeFSLockFactory instance, with null (unset)
   * lock directory. When you pass this factory to a {@link FSDirectory}
   * subclass, the lock directory is automatically set to the
   * directory itsself. Be sure to create one instance for each directory
   * your create!
   */
  public NativeFSLockFactory() throws IOException {
    this((File) null);
  }

  /**
   * Create a NativeFSLockFactory instance, storing lock
   * files into the specified lockDirName:
   *
   * @param lockDirName where lock files are created.
   */
  public NativeFSLockFactory(String lockDirName) throws IOException {
    this(new File(lockDirName));
  }

  /**
   * Create a NativeFSLockFactory instance, storing lock
   * files into the specified lockDir:
   * 
   * @param lockDir where lock files are created.
   */
  public NativeFSLockFactory(File lockDir) throws IOException {
    setLockDir(lockDir);
  }

  public synchronized Lock makeLock(String lockName) {
    acquireTestLock();
    if (lockPrefix != null)
      lockName = lockPrefix + "-" + lockName;
    return new NativeFSLock(lockDir, lockName);
  }

  public void clearLock(String lockName) throws IOException {
    // Note that this isn't strictly required anymore
    // because the existence of these files does not mean
    // they are locked, but, still do this in case people
    // really want to see the files go away:
    if (lockDir.exists()) {
      if (lockPrefix != null) {
        lockName = lockPrefix + "-" + lockName;
      }
      File lockFile = new File(lockDir, lockName);
      if (lockFile.exists() && !lockFile.delete()) {
        throw new IOException("Cannot delete " + lockFile);
      }
    }
  }
};

class NativeFSLock extends Lock {

  private RandomAccessFile f;
  private FileChannel channel;
  private FileLock lock;
  private File path;
  private File lockDir;

  /*
   * The javadocs for FileChannel state that you should have
   * a single instance of a FileChannel (per JVM) for all
   * locking against a given file.  To ensure this, we have
   * a single (static) HashSet that contains the file paths
   * of all currently locked locks.  This protects against
   * possible cases where different Directory instances in
   * one JVM (each with their own NativeFSLockFactory
   * instance) have set the same lock dir and lock prefix.
   */
  private static HashSet LOCK_HELD = new HashSet();

  public NativeFSLock(File lockDir, String lockFileName) {
    this.lockDir = lockDir;
    path = new File(lockDir, lockFileName);
  }

  private synchronized boolean lockExists() {
    return lock != null;
  }

  public synchronized boolean obtain() throws IOException {

    if (lockExists()) {
      // Our instance is already locked:
      return false;
    }

    // Ensure that lockDir exists and is a directory.
    if (!lockDir.exists()) {
      if (!lockDir.mkdirs())
        throw new IOException("Cannot create directory: " +
                              lockDir.getAbsolutePath());
    } else if (!lockDir.isDirectory()) {
      throw new IOException("Found regular file where directory expected: " + 
                            lockDir.getAbsolutePath());
    }

    String canonicalPath = path.getCanonicalPath();

    boolean markedHeld = false;

    try {

      // Make sure nobody else in-process has this lock held
      // already, and, mark it held if not:

      synchronized(LOCK_HELD) {
        if (LOCK_HELD.contains(canonicalPath)) {
          // Someone else in this JVM already has the lock:
          return false;
        } else {
          // This "reserves" the fact that we are the one
          // thread trying to obtain this lock, so we own
          // the only instance of a channel against this
          // file:
          LOCK_HELD.add(canonicalPath);
          markedHeld = true;
        }
      }

      try {
        f = new RandomAccessFile(path, "rw");
      } catch (IOException e) {
        // On Windows, we can get intermittent "Access
        // Denied" here.  So, we treat this as failure to
        // acquire the lock, but, store the reason in case
        // there is in fact a real error case.
        failureReason = e;
        f = null;
      }

      if (f != null) {
        try {
          channel = f.getChannel();
          try {
            lock = channel.tryLock();
          } catch (IOException e) {
            // At least on OS X, we will sometimes get an
            // intermittent "Permission Denied" IOException,
            // which seems to simply mean "you failed to get
            // the lock".  But other IOExceptions could be
            // "permanent" (eg, locking is not supported via
            // the filesystem).  So, we record the failure
            // reason here; the timeout obtain (usually the
            // one calling us) will use this as "root cause"
            // if it fails to get the lock.
            failureReason = e;
          } finally {
            if (lock == null) {
              try {
                channel.close();
              } finally {
                channel = null;
              }
            }
          }
        } finally {
          if (channel == null) {
            try {
              f.close();
            } finally {
              f = null;
            }
          }
        }
      }

    } finally {
      if (markedHeld && !lockExists()) {
        synchronized(LOCK_HELD) {
          if (LOCK_HELD.contains(canonicalPath)) {
            LOCK_HELD.remove(canonicalPath);
          }
        }
      }
    }
    return lockExists();
  }

  public synchronized void release() throws IOException {
    if (lockExists()) {
      try {
        lock.release();
      } finally {
        lock = null;
        try {
          channel.close();
        } finally {
          channel = null;
          try {
            f.close();
          } finally {
            f = null;
            synchronized(LOCK_HELD) {
              LOCK_HELD.remove(path.getCanonicalPath());
            }
          }
        }
      }
      if (!path.delete())
        throw new LockReleaseFailedException("failed to delete " + path);
    }
  }

  public synchronized boolean isLocked() {
    // The test for is isLocked is not directly possible with native file locks:
    
    // First a shortcut, if a lock reference in this instance is available
    if (lockExists()) return true;
    
    // Look if lock file is present; if not, there can definitely be no lock!
    if (!path.exists()) return false;
    
    // Try to obtain and release (if was locked) the lock
    try {
      boolean obtained = obtain();
      if (obtained) release();
      return !obtained;
    } catch (IOException ioe) {
      return false;
    }    
  }

  public String toString() {
    return "NativeFSLock@" + path;
  }
}
