package org.apache.lucene.index;

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

import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.ArrayList;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.search.DefaultSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.Lock;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.AlreadyClosedException;

/** 
 * An IndexReader which reads indexes with multiple segments.
 */
class DirectoryReader extends IndexReader implements Cloneable {
  protected Directory directory;
  protected boolean readOnly;

  IndexWriter writer;

  private IndexDeletionPolicy deletionPolicy;
  private final HashSet synced = new HashSet();
  private Lock writeLock;
  private SegmentInfos segmentInfos;
  private SegmentInfos segmentInfosStart;
  private boolean stale;
  private final int termInfosIndexDivisor;

  private boolean rollbackHasChanges;
  private SegmentInfos rollbackSegmentInfos;

  private SegmentReader[] subReaders;
  private int[] starts;                           // 1st docno for each segment
  private Map normsCache = new HashMap();
  private int maxDoc = 0;
  private int numDocs = -1;
  private boolean hasDeletions = false;

  static IndexReader open(final Directory directory, final IndexDeletionPolicy deletionPolicy, final IndexCommit commit, final boolean readOnly,
                          final int termInfosIndexDivisor) throws CorruptIndexException, IOException {
    return (IndexReader) new SegmentInfos.FindSegmentsFile(directory) {
      protected Object doBody(String segmentFileName) throws CorruptIndexException, IOException {
        SegmentInfos infos = new SegmentInfos();
        infos.read(directory, segmentFileName);
        if (readOnly)
          return new ReadOnlyDirectoryReader(directory, infos, deletionPolicy, termInfosIndexDivisor);
        else
          return new DirectoryReader(directory, infos, deletionPolicy, false, termInfosIndexDivisor);
      }
    }.run(commit);
  }

  /** Construct reading the named set of readers. */
  DirectoryReader(Directory directory, SegmentInfos sis, IndexDeletionPolicy deletionPolicy, boolean readOnly, int termInfosIndexDivisor) throws IOException {
    this.directory = directory;
    this.readOnly = readOnly;
    this.segmentInfos = sis;
    this.deletionPolicy = deletionPolicy;
    this.termInfosIndexDivisor = termInfosIndexDivisor;

    if (!readOnly) {
      // We assume that this segments_N was previously
      // properly sync'd:
      synced.addAll(sis.files(directory, true));
    }

    // To reduce the chance of hitting FileNotFound
    // (and having to retry), we open segments in
    // reverse because IndexWriter merges & deletes
    // the newest segments first.

    SegmentReader[] readers = new SegmentReader[sis.size()];
    for (int i = sis.size()-1; i >= 0; i--) {
      boolean success = false;
      try {
        readers[i] = SegmentReader.get(readOnly, sis.info(i), termInfosIndexDivisor);
        success = true;
      } finally {
        if (!success) {
          // Close all readers we had opened:
          for(i++;i<sis.size();i++) {
            try {
              readers[i].close();
            } catch (Throwable ignore) {
              // keep going - we want to clean up as much as possible
            }
          }
        }
      }
    }

    initialize(readers);
  }

  // Used by near real-time search
  DirectoryReader(IndexWriter writer, SegmentInfos infos, int termInfosIndexDivisor) throws IOException {
    this.directory = writer.getDirectory();
    this.readOnly = true;
    this.segmentInfos = infos;
    segmentInfosStart = (SegmentInfos) infos.clone();
    this.termInfosIndexDivisor = termInfosIndexDivisor;
    if (!readOnly) {
      // We assume that this segments_N was previously
      // properly sync'd:
      synced.addAll(infos.files(directory, true));
    }

    // IndexWriter synchronizes externally before calling
    // us, which ensures infos will not change; so there's
    // no need to process segments in reverse order
    final int numSegments = infos.size();
    SegmentReader[] readers = new SegmentReader[numSegments];
    final Directory dir = writer.getDirectory();
    int upto = 0;

    for (int i=0;i<numSegments;i++) {
      boolean success = false;
      try {
        final SegmentInfo info = infos.info(upto);
        if (info.dir == dir) {
          readers[upto++] = writer.readerPool.getReadOnlyClone(info, true, termInfosIndexDivisor);
        }
        success = true;
      } finally {
        if (!success) {
          // Close all readers we had opened:
          for(upto--;upto>=0;upto--) {
            try {
              readers[upto].close();
            } catch (Throwable ignore) {
              // keep going - we want to clean up as much as possible
            }
          }
        }
      }
    }

    this.writer = writer;

    if (upto < readers.length) {
      // This means some segments were in a foreign Directory
      SegmentReader[] newReaders = new SegmentReader[upto];
      System.arraycopy(readers, 0, newReaders, 0, upto);
      readers = newReaders;
    }

    initialize(readers);
  }

  /** This constructor is only used for {@link #reopen()} */
  DirectoryReader(Directory directory, SegmentInfos infos, SegmentReader[] oldReaders, int[] oldStarts,
                  Map oldNormsCache, boolean readOnly, boolean doClone, int termInfosIndexDivisor) throws IOException {
    this.directory = directory;
    this.readOnly = readOnly;
    this.segmentInfos = infos;
    this.termInfosIndexDivisor = termInfosIndexDivisor;
    if (!readOnly) {
      // We assume that this segments_N was previously
      // properly sync'd:
      synced.addAll(infos.files(directory, true));
    }

    // we put the old SegmentReaders in a map, that allows us
    // to lookup a reader using its segment name
    Map segmentReaders = new HashMap();

    if (oldReaders != null) {
      // create a Map SegmentName->SegmentReader
      for (int i = 0; i < oldReaders.length; i++) {
        segmentReaders.put(oldReaders[i].getSegmentName(), new Integer(i));
      }
    }
    
    SegmentReader[] newReaders = new SegmentReader[infos.size()];
    
    // remember which readers are shared between the old and the re-opened
    // DirectoryReader - we have to incRef those readers
    boolean[] readerShared = new boolean[infos.size()];
    
    for (int i = infos.size() - 1; i>=0; i--) {
      // find SegmentReader for this segment
      Integer oldReaderIndex = (Integer) segmentReaders.get(infos.info(i).name);
      if (oldReaderIndex == null) {
        // this is a new segment, no old SegmentReader can be reused
        newReaders[i] = null;
      } else {
        // there is an old reader for this segment - we'll try to reopen it
        newReaders[i] = oldReaders[oldReaderIndex.intValue()];
      }

      boolean success = false;
      try {
        SegmentReader newReader;
        if (newReaders[i] == null || infos.info(i).getUseCompoundFile() != newReaders[i].getSegmentInfo().getUseCompoundFile()) {

          // We should never see a totally new segment during cloning
          assert !doClone;

          // this is a new reader; in case we hit an exception we can close it safely
          newReader = SegmentReader.get(readOnly, infos.info(i), termInfosIndexDivisor);
        } else {
          newReader = newReaders[i].reopenSegment(infos.info(i), doClone, readOnly);
        }
        if (newReader == newReaders[i]) {
          // this reader will be shared between the old and the new one,
          // so we must incRef it
          readerShared[i] = true;
          newReader.incRef();
        } else {
          readerShared[i] = false;
          newReaders[i] = newReader;
        }
        success = true;
      } finally {
        if (!success) {
          for (i++; i < infos.size(); i++) {
            if (newReaders[i] != null) {
              try {
                if (!readerShared[i]) {
                  // this is a new subReader that is not used by the old one,
                  // we can close it
                  newReaders[i].close();
                } else {
                  // this subReader is also used by the old reader, so instead
                  // closing we must decRef it
                  newReaders[i].decRef();
                }
              } catch (IOException ignore) {
                // keep going - we want to clean up as much as possible
              }
            }
          }
        }
      }
    }    
    
    // initialize the readers to calculate maxDoc before we try to reuse the old normsCache
    initialize(newReaders);
    
    // try to copy unchanged norms from the old normsCache to the new one
    if (oldNormsCache != null) {
      Iterator it = oldNormsCache.entrySet().iterator();
      while (it.hasNext()) {
        Map.Entry entry = (Map.Entry) it.next();
        String field = (String) entry.getKey();
        if (!hasNorms(field)) {
          continue;
        }

        byte[] oldBytes = (byte[]) entry.getValue();

        byte[] bytes = new byte[maxDoc()];

        for (int i = 0; i < subReaders.length; i++) {
          Integer oldReaderIndex = ((Integer) segmentReaders.get(subReaders[i].getSegmentName()));

          // this SegmentReader was not re-opened, we can copy all of its norms 
          if (oldReaderIndex != null &&
               (oldReaders[oldReaderIndex.intValue()] == subReaders[i] 
                 || oldReaders[oldReaderIndex.intValue()].norms.get(field) == subReaders[i].norms.get(field))) {
            // we don't have to synchronize here: either this constructor is called from a SegmentReader,
            // in which case no old norms cache is present, or it is called from MultiReader.reopen(),
            // which is synchronized
            System.arraycopy(oldBytes, oldStarts[oldReaderIndex.intValue()], bytes, starts[i], starts[i+1] - starts[i]);
          } else {
            subReaders[i].norms(field, bytes, starts[i]);
          }
        }

        normsCache.put(field, bytes);      // update cache
      }
    }
  }

  private void initialize(SegmentReader[] subReaders) {
    this.subReaders = subReaders;
    starts = new int[subReaders.length + 1];    // build starts array
    for (int i = 0; i < subReaders.length; i++) {
      starts[i] = maxDoc;
      maxDoc += subReaders[i].maxDoc();      // compute maxDocs

      if (subReaders[i].hasDeletions())
        hasDeletions = true;
    }
    starts[subReaders.length] = maxDoc;
  }

  public final synchronized Object clone() {
    try {
      return clone(readOnly); // Preserve current readOnly
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public final synchronized IndexReader clone(boolean openReadOnly) throws CorruptIndexException, IOException {
    DirectoryReader newReader = doReopen((SegmentInfos) segmentInfos.clone(), true, openReadOnly);

    if (this != newReader) {
      newReader.deletionPolicy = deletionPolicy;
    }
    newReader.writer = writer;
    // If we're cloning a non-readOnly reader, move the
    // writeLock (if there is one) to the new reader:
    if (!openReadOnly && writeLock != null) {
      // In near real-time search, reader is always readonly
      assert writer == null;
      newReader.writeLock = writeLock;
      newReader.hasChanges = hasChanges;
      newReader.hasDeletions = hasDeletions;
      writeLock = null;
      hasChanges = false;
    }

    return newReader;
  }

  public final synchronized IndexReader reopen() throws CorruptIndexException, IOException {
    // Preserve current readOnly
    return doReopen(readOnly, null);
  }

  public final synchronized IndexReader reopen(boolean openReadOnly) throws CorruptIndexException, IOException {
    return doReopen(openReadOnly, null);
  }

  public final synchronized IndexReader reopen(final IndexCommit commit) throws CorruptIndexException, IOException {
    return doReopen(true, commit);
  }

  private synchronized IndexReader doReopen(final boolean openReadOnly, IndexCommit commit) throws CorruptIndexException, IOException {
    ensureOpen();

    assert commit == null || openReadOnly;

    // If we were obtained by writer.getReader(), re-ask the
    // writer to get a new reader.
    if (writer != null) {
      assert readOnly;

      if (!openReadOnly) {
        throw new IllegalArgumentException("a reader obtained from IndexWriter.getReader() can only be reopened with openReadOnly=true (got false)");
      }

      if (commit != null) {
        throw new IllegalArgumentException("a reader obtained from IndexWriter.getReader() cannot currently accept a commit");
      }

      if (!writer.isOpen(true)) {
        throw new AlreadyClosedException("cannot reopen: the IndexWriter this reader was obtained from is now closed");
      }

      // TODO: right now we *always* make a new reader; in
      // the future we could have write make some effort to
      // detect that no changes have occurred
      IndexReader reader = writer.getReader();
      reader.setDisableFakeNorms(getDisableFakeNorms());
      return reader;
    }

    if (commit == null) {
      if (hasChanges) {
        // We have changes, which means we are not readOnly:
        assert readOnly == false;
        // and we hold the write lock:
        assert writeLock != null;
        // so no other writer holds the write lock, which
        // means no changes could have been done to the index:
        assert isCurrent();

        if (openReadOnly) {
          return (IndexReader) clone(openReadOnly);
        } else {
          return this;
        }
      } else if (isCurrent()) {
        if (openReadOnly != readOnly) {
          // Just fallback to clone
          return (IndexReader) clone(openReadOnly);
        } else {
          return this;
        }
      }
    } else {
      if (directory != commit.getDirectory())
        throw new IOException("the specified commit does not match the specified Directory");
      if (segmentInfos != null && commit.getSegmentsFileName().equals(segmentInfos.getCurrentSegmentFileName())) {
        if (readOnly != openReadOnly) {
          // Just fallback to clone
          return (IndexReader) clone(openReadOnly);
        } else {
          return this;
        }
      }
    }

    return (IndexReader) new SegmentInfos.FindSegmentsFile(directory) {
      protected Object doBody(String segmentFileName) throws CorruptIndexException, IOException {
        SegmentInfos infos = new SegmentInfos();
        infos.read(directory, segmentFileName);
        return doReopen(infos, false, openReadOnly);
      }
    }.run(commit);
  }

  private synchronized DirectoryReader doReopen(SegmentInfos infos, boolean doClone, boolean openReadOnly) throws CorruptIndexException, IOException {
    DirectoryReader reader;
    if (openReadOnly) {
      reader = new ReadOnlyDirectoryReader(directory, infos, subReaders, starts, normsCache, doClone, termInfosIndexDivisor);
    } else {
      reader = new DirectoryReader(directory, infos, subReaders, starts, normsCache, false, doClone, termInfosIndexDivisor);
    }
    reader.setDisableFakeNorms(getDisableFakeNorms());
    return reader;
  }

  /** Version number when this IndexReader was opened. */
  public long getVersion() {
    ensureOpen();
    return segmentInfos.getVersion();
  }

  public TermFreqVector[] getTermFreqVectors(int n) throws IOException {
    ensureOpen();
    int i = readerIndex(n);        // find segment num
    return subReaders[i].getTermFreqVectors(n - starts[i]); // dispatch to segment
  }

  public TermFreqVector getTermFreqVector(int n, String field)
      throws IOException {
    ensureOpen();
    int i = readerIndex(n);        // find segment num
    return subReaders[i].getTermFreqVector(n - starts[i], field);
  }


  public void getTermFreqVector(int docNumber, String field, TermVectorMapper mapper) throws IOException {
    ensureOpen();
    int i = readerIndex(docNumber);        // find segment num
    subReaders[i].getTermFreqVector(docNumber - starts[i], field, mapper);
  }

  public void getTermFreqVector(int docNumber, TermVectorMapper mapper) throws IOException {
    ensureOpen();
    int i = readerIndex(docNumber);        // find segment num
    subReaders[i].getTermFreqVector(docNumber - starts[i], mapper);
  }

  /**
   * Checks is the index is optimized (if it has a single segment and no deletions)
   * @return <code>true</code> if the index is optimized; <code>false</code> otherwise
   */
  public boolean isOptimized() {
    ensureOpen();
    return segmentInfos.size() == 1 && !hasDeletions();
  }
  
  public synchronized int numDocs() {
    // Don't call ensureOpen() here (it could affect performance)
    if (numDocs == -1) {        // check cache
      int n = 0;                // cache miss--recompute
      for (int i = 0; i < subReaders.length; i++)
        n += subReaders[i].numDocs();      // sum from readers
      numDocs = n;
    }
    return numDocs;
  }

  public int maxDoc() {
    // Don't call ensureOpen() here (it could affect performance)
    return maxDoc;
  }

  // inherit javadoc
  public Document document(int n, FieldSelector fieldSelector) throws CorruptIndexException, IOException {
    ensureOpen();
    int i = readerIndex(n);                          // find segment num
    return subReaders[i].document(n - starts[i], fieldSelector);    // dispatch to segment reader
  }

  public boolean isDeleted(int n) {
    // Don't call ensureOpen() here (it could affect performance)
    final int i = readerIndex(n);                           // find segment num
    return subReaders[i].isDeleted(n - starts[i]);    // dispatch to segment reader
  }

  public boolean hasDeletions() {
    // Don't call ensureOpen() here (it could affect performance)
    return hasDeletions;
  }

  protected void doDelete(int n) throws CorruptIndexException, IOException {
    numDocs = -1;                             // invalidate cache
    int i = readerIndex(n);                   // find segment num
    subReaders[i].deleteDocument(n - starts[i]);      // dispatch to segment reader
    hasDeletions = true;
  }

  protected void doUndeleteAll() throws CorruptIndexException, IOException {
    for (int i = 0; i < subReaders.length; i++)
      subReaders[i].undeleteAll();

    hasDeletions = false;
    numDocs = -1;                                 // invalidate cache
  }

  private int readerIndex(int n) {    // find reader for doc n:
    return readerIndex(n, this.starts, this.subReaders.length);
  }
  
  final static int readerIndex(int n, int[] starts, int numSubReaders) {    // find reader for doc n:
    int lo = 0;                                      // search starts array
    int hi = numSubReaders - 1;                  // for first element less

    while (hi >= lo) {
      int mid = (lo + hi) >>> 1;
      int midValue = starts[mid];
      if (n < midValue)
        hi = mid - 1;
      else if (n > midValue)
        lo = mid + 1;
      else {                                      // found a match
        while (mid+1 < numSubReaders && starts[mid+1] == midValue) {
          mid++;                                  // scan to last match
        }
        return mid;
      }
    }
    return hi;
  }

  public boolean hasNorms(String field) throws IOException {
    ensureOpen();
    for (int i = 0; i < subReaders.length; i++) {
      if (subReaders[i].hasNorms(field)) return true;
    }
    return false;
  }

  private byte[] ones;
  private byte[] fakeNorms() {
    if (ones==null) ones=SegmentReader.createFakeNorms(maxDoc());
    return ones;
  }

  public synchronized byte[] norms(String field) throws IOException {
    ensureOpen();
    byte[] bytes = (byte[])normsCache.get(field);
    if (bytes != null)
      return bytes;          // cache hit
    if (!hasNorms(field))
      return getDisableFakeNorms() ? null : fakeNorms();

    bytes = new byte[maxDoc()];
    for (int i = 0; i < subReaders.length; i++)
      subReaders[i].norms(field, bytes, starts[i]);
    normsCache.put(field, bytes);      // update cache
    return bytes;
  }

  public synchronized void norms(String field, byte[] result, int offset)
    throws IOException {
    ensureOpen();
    byte[] bytes = (byte[])normsCache.get(field);
    if (bytes==null && !hasNorms(field)) {
      Arrays.fill(result, offset, result.length, DefaultSimilarity.encodeNorm(1.0f));
    } else if (bytes != null) {                           // cache hit
      System.arraycopy(bytes, 0, result, offset, maxDoc());
    } else {
      for (int i = 0; i < subReaders.length; i++) {      // read from segments
        subReaders[i].norms(field, result, offset + starts[i]);
      }
    }
  }

  protected void doSetNorm(int n, String field, byte value)
    throws CorruptIndexException, IOException {
    synchronized (normsCache) {
      normsCache.remove(field);                         // clear cache      
    }
    int i = readerIndex(n);                           // find segment num
    subReaders[i].setNorm(n-starts[i], field, value); // dispatch
  }

  public TermEnum terms() throws IOException {
    ensureOpen();
    return new MultiTermEnum(this, subReaders, starts, null);
  }

  public TermEnum terms(Term term) throws IOException {
    ensureOpen();
    return new MultiTermEnum(this, subReaders, starts, term);
  }

  public int docFreq(Term t) throws IOException {
    ensureOpen();
    int total = 0;          // sum freqs in segments
    for (int i = 0; i < subReaders.length; i++)
      total += subReaders[i].docFreq(t);
    return total;
  }

  public TermDocs termDocs() throws IOException {
    ensureOpen();
    return new MultiTermDocs(this, subReaders, starts);
  }

  public TermPositions termPositions() throws IOException {
    ensureOpen();
    return new MultiTermPositions(this, subReaders, starts);
  }

  /**
   * Tries to acquire the WriteLock on this directory. this method is only valid if this IndexReader is directory
   * owner.
   *
   * @throws StaleReaderException  if the index has changed since this reader was opened
   * @throws CorruptIndexException if the index is corrupt
   * @throws org.apache.lucene.store.LockObtainFailedException
   *                               if another writer has this index open (<code>write.lock</code> could not be
   *                               obtained)
   * @throws IOException           if there is a low-level IO error
   */
  protected void acquireWriteLock() throws StaleReaderException, CorruptIndexException, LockObtainFailedException, IOException {

    if (readOnly) {
      // NOTE: we should not reach this code w/ the core
      // IndexReader classes; however, an external subclass
      // of IndexReader could reach this.
      ReadOnlySegmentReader.noWrite();
    }

    if (segmentInfos != null) {
      ensureOpen();
      if (stale)
        throw new StaleReaderException("IndexReader out of date and no longer valid for delete, undelete, or setNorm operations");

      if (writeLock == null) {
        Lock writeLock = directory.makeLock(IndexWriter.WRITE_LOCK_NAME);
        if (!writeLock.obtain(IndexWriter.WRITE_LOCK_TIMEOUT)) // obtain write lock
          throw new LockObtainFailedException("Index locked for write: " + writeLock);
        this.writeLock = writeLock;

        // we have to check whether index has changed since this reader was opened.
        // if so, this reader is no longer valid for deletion
        if (SegmentInfos.readCurrentVersion(directory) > segmentInfos.getVersion()) {
          stale = true;
          this.writeLock.release();
          this.writeLock = null;
          throw new StaleReaderException("IndexReader out of date and no longer valid for delete, undelete, or setNorm operations");
        }
      }
    }
  }

  /** @deprecated  */
  protected void doCommit() throws IOException {
    doCommit(null);
  }

  /**
   * Commit changes resulting from delete, undeleteAll, or setNorm operations
   * <p/>
   * If an exception is hit, then either no changes or all changes will have been committed to the index (transactional
   * semantics).
   *
   * @throws IOException if there is a low-level IO error
   */
  protected void doCommit(Map commitUserData) throws IOException {
    if (hasChanges) {
      segmentInfos.setUserData(commitUserData);
      // Default deleter (for backwards compatibility) is
      // KeepOnlyLastCommitDeleter:
      IndexFileDeleter deleter = new IndexFileDeleter(directory,
                                                      deletionPolicy == null ? new KeepOnlyLastCommitDeletionPolicy() : deletionPolicy,
                                                      segmentInfos, null, null);

      // Checkpoint the state we are about to change, in
      // case we have to roll back:
      startCommit();

      boolean success = false;
      try {
        for (int i = 0; i < subReaders.length; i++)
          subReaders[i].commit();

        // Sync all files we just wrote
        Iterator it = segmentInfos.files(directory, false).iterator();
        while (it.hasNext()) {
          final String fileName = (String) it.next();
          if (!synced.contains(fileName)) {
            assert directory.fileExists(fileName);
            directory.sync(fileName);
            synced.add(fileName);
          }
        }

        segmentInfos.commit(directory);
        success = true;
      } finally {

        if (!success) {

          // Rollback changes that were made to
          // SegmentInfos but failed to get [fully]
          // committed.  This way this reader instance
          // remains consistent (matched to what's
          // actually in the index):
          rollbackCommit();

          // Recompute deletable files & remove them (so
          // partially written .del files, etc, are
          // removed):
          deleter.refresh();
        }
      }

      // Have the deleter remove any now unreferenced
      // files due to this commit:
      deleter.checkpoint(segmentInfos, true);
      deleter.close();

      if (writeLock != null) {
        writeLock.release();  // release write lock
        writeLock = null;
      }
    }
    hasChanges = false;
  }

  void startCommit() {
    rollbackHasChanges = hasChanges;
    rollbackSegmentInfos = (SegmentInfos) segmentInfos.clone();
    for (int i = 0; i < subReaders.length; i++) {
      subReaders[i].startCommit();
    }
  }

  void rollbackCommit() {
    hasChanges = rollbackHasChanges;
    for (int i = 0; i < segmentInfos.size(); i++) {
      // Rollback each segmentInfo.  Because the
      // SegmentReader holds a reference to the
      // SegmentInfo we can't [easily] just replace
      // segmentInfos, so we reset it in place instead:
      segmentInfos.info(i).reset(rollbackSegmentInfos.info(i));
    }
    rollbackSegmentInfos = null;
    for (int i = 0; i < subReaders.length; i++) {
      subReaders[i].rollbackCommit();
    }
  }

  public Map getCommitUserData() {
    ensureOpen();
    return segmentInfos.getUserData();
  }

  public boolean isCurrent() throws CorruptIndexException, IOException {
    ensureOpen();
    if (writer == null || writer.isClosed()) {
      // we loaded SegmentInfos from the directory
      return SegmentInfos.readCurrentVersion(directory) == segmentInfos.getVersion();
    } else {
      return writer.nrtIsCurrent(segmentInfosStart);
    }
  }

  protected synchronized void doClose() throws IOException {
    IOException ioe = null;
    normsCache = null;
    for (int i = 0; i < subReaders.length; i++) {
      // try to close each reader, even if an exception is thrown
      try {
        subReaders[i].decRef();
      } catch (IOException e) {
        if (ioe == null) ioe = e;
      }
    }
    // throw the first exception
    if (ioe != null) throw ioe;
  }

  public Collection getFieldNames (IndexReader.FieldOption fieldNames) {
    ensureOpen();
    return getFieldNames(fieldNames, this.subReaders);
  }
  
  static Collection getFieldNames (IndexReader.FieldOption fieldNames, IndexReader[] subReaders) {
    // maintain a unique set of field names
    Set fieldSet = new HashSet();
    for (int i = 0; i < subReaders.length; i++) {
      IndexReader reader = subReaders[i];
      Collection names = reader.getFieldNames(fieldNames);
      fieldSet.addAll(names);
    }
    return fieldSet;
  } 
  
  public IndexReader[] getSequentialSubReaders() {
    return subReaders;
  }

  public void setDisableFakeNorms(boolean disableFakeNorms) {
    super.setDisableFakeNorms(disableFakeNorms);
    for (int i = 0; i < subReaders.length; i++)
        subReaders[i].setDisableFakeNorms(disableFakeNorms);
  }

  /** Returns the directory this index resides in. */
  public Directory directory() {
    // Don't ensureOpen here -- in certain cases, when a
    // cloned/reopened reader needs to commit, it may call
    // this method on the closed original reader
    return directory;
  }

  public int getTermInfosIndexDivisor() {
    return termInfosIndexDivisor;
  }

  /**
   * Expert: return the IndexCommit that this reader has opened.
   * <p/>
   * <p><b>WARNING</b>: this API is new and experimental and may suddenly change.</p>
   */
  public IndexCommit getIndexCommit() throws IOException {
    return new ReaderCommit(segmentInfos, directory);
  }

  /** @see org.apache.lucene.index.IndexReader#listCommits */
  public static Collection listCommits(Directory dir) throws IOException {
    final String[] files = dir.listAll();

    Collection commits = new ArrayList();

    SegmentInfos latest = new SegmentInfos();
    latest.read(dir);
    final long currentGen = latest.getGeneration();

    commits.add(new ReaderCommit(latest, dir));

    for(int i=0;i<files.length;i++) {

      final String fileName = files[i];

      if (fileName.startsWith(IndexFileNames.SEGMENTS) &&
          !fileName.equals(IndexFileNames.SEGMENTS_GEN) &&
          SegmentInfos.generationFromSegmentsFileName(fileName) < currentGen) {

        SegmentInfos sis = new SegmentInfos();
        try {
          // IOException allowed to throw there, in case
          // segments_N is corrupt
          sis.read(dir, fileName);
        } catch (FileNotFoundException fnfe) {
          // LUCENE-948: on NFS (and maybe others), if
          // you have writers switching back and forth
          // between machines, it's very likely that the
          // dir listing will be stale and will claim a
          // file segments_X exists when in fact it
          // doesn't.  So, we catch this and handle it
          // as if the file does not exist
          sis = null;
        }

        if (sis != null)
          commits.add(new ReaderCommit(sis, dir));
      }
    }

    return commits;
  }

  private static final class ReaderCommit extends IndexCommit {
    private String segmentsFileName;
    Collection files;
    Directory dir;
    long generation;
    long version;
    final boolean isOptimized;
    final Map userData;

    ReaderCommit(SegmentInfos infos, Directory dir) throws IOException {
      segmentsFileName = infos.getCurrentSegmentFileName();
      this.dir = dir;
      userData = infos.getUserData();
      files = Collections.unmodifiableCollection(infos.files(dir, true));
      version = infos.getVersion();
      generation = infos.getGeneration();
      isOptimized = infos.size() == 1 && !infos.info(0).hasDeletions();
    }

    public boolean isOptimized() {
      return isOptimized;
    }

    public String getSegmentsFileName() {
      return segmentsFileName;
    }

    public Collection getFileNames() {
      return files;
    }

    public Directory getDirectory() {
      return dir;
    }

    public long getVersion() {
      return version;
    }

    public long getGeneration() {
      return generation;
    }

    public boolean isDeleted() {
      return false;
    }

    public Map getUserData() {
      return userData;
    }
  }

  static class MultiTermEnum extends TermEnum {
    IndexReader topReader; // used for matching TermEnum to TermDocs
    private SegmentMergeQueue queue;
  
    private Term term;
    private int docFreq;
    final SegmentMergeInfo[] matchingSegments; // null terminated array of matching segments

    public MultiTermEnum(IndexReader topReader, IndexReader[] readers, int[] starts, Term t)
      throws IOException {
      this.topReader = topReader;
      queue = new SegmentMergeQueue(readers.length);
      matchingSegments = new SegmentMergeInfo[readers.length+1];
      for (int i = 0; i < readers.length; i++) {
        IndexReader reader = readers[i];
        TermEnum termEnum;
  
        if (t != null) {
          termEnum = reader.terms(t);
        } else
          termEnum = reader.terms();
  
        SegmentMergeInfo smi = new SegmentMergeInfo(starts[i], termEnum, reader);
        smi.ord = i;
        if (t == null ? smi.next() : termEnum.term() != null)
          queue.put(smi);          // initialize queue
        else
          smi.close();
      }
  
      if (t != null && queue.size() > 0) {
        next();
      }
    }
  
    public boolean next() throws IOException {
      for (int i=0; i<matchingSegments.length; i++) {
        SegmentMergeInfo smi = matchingSegments[i];
        if (smi==null) break;
        if (smi.next())
          queue.put(smi);
        else
          smi.close(); // done with segment
      }
      
      int numMatchingSegments = 0;
      matchingSegments[0] = null;

      SegmentMergeInfo top = (SegmentMergeInfo)queue.top();

      if (top == null) {
        term = null;
        return false;
      }
  
      term = top.term;
      docFreq = 0;
  
      while (top != null && term.compareTo(top.term) == 0) {
        matchingSegments[numMatchingSegments++] = top;
        queue.pop();
        docFreq += top.termEnum.docFreq();    // increment freq
        top = (SegmentMergeInfo)queue.top();
      }

      matchingSegments[numMatchingSegments] = null;
      return true;
    }
  
    public Term term() {
      return term;
    }
  
    public int docFreq() {
      return docFreq;
    }
  
    public void close() throws IOException {
      queue.close();
    }
  }

  static class MultiTermDocs implements TermDocs {
    IndexReader topReader;  // used for matching TermEnum to TermDocs
    protected IndexReader[] readers;
    protected int[] starts;
    protected Term term;
  
    protected int base = 0;
    protected int pointer = 0;
  
    private TermDocs[] readerTermDocs;
    protected TermDocs current;              // == readerTermDocs[pointer]

    private MultiTermEnum tenum;  // the term enum used for seeking... can be null
    int matchingSegmentPos;  // position into the matching segments from tenum
    SegmentMergeInfo smi;     // current segment mere info... can be null

    public MultiTermDocs(IndexReader topReader, IndexReader[] r, int[] s) {
      this.topReader = topReader;
      readers = r;
      starts = s;
  
      readerTermDocs = new TermDocs[r.length];
    }

    public int doc() {
      return base + current.doc();
    }
    public int freq() {
      return current.freq();
    }
  
    public void seek(Term term) {
      this.term = term;
      this.base = 0;
      this.pointer = 0;
      this.current = null;
      this.tenum = null;
      this.smi = null;
      this.matchingSegmentPos = 0;
    }
  
    public void seek(TermEnum termEnum) throws IOException {
      seek(termEnum.term());
      if (termEnum instanceof MultiTermEnum) {
        tenum = (MultiTermEnum)termEnum;
        if (topReader != tenum.topReader)
          tenum = null;
      }
    }
  
    public boolean next() throws IOException {
      for(;;) {
        if (current!=null && current.next()) {
          return true;
        }
        else if (pointer < readers.length) {
          if (tenum != null) {
            smi = tenum.matchingSegments[matchingSegmentPos++];
            if (smi==null) {
              pointer = readers.length;
              return false;
            }
            pointer = smi.ord;
          }
          base = starts[pointer];
          current = termDocs(pointer++);
        } else {
          return false;
        }
      }
    }
  
    /** Optimized implementation. */
    public int read(final int[] docs, final int[] freqs) throws IOException {
      while (true) {
        while (current == null) {
          if (pointer < readers.length) {      // try next segment
            if (tenum != null) {
              smi = tenum.matchingSegments[matchingSegmentPos++];
              if (smi==null) {
                pointer = readers.length;
                return 0;
              }
              pointer = smi.ord;
            }
            base = starts[pointer];
            current = termDocs(pointer++);
          } else {
            return 0;
          }
        }
        int end = current.read(docs, freqs);
        if (end == 0) {          // none left in segment
          current = null;
        } else {            // got some
          final int b = base;        // adjust doc numbers
          for (int i = 0; i < end; i++)
           docs[i] += b;
          return end;
        }
      }
    }
  
   /* A Possible future optimization could skip entire segments */ 
    public boolean skipTo(int target) throws IOException {
      for(;;) {
        if (current != null && current.skipTo(target-base)) {
          return true;
        } else if (pointer < readers.length) {
          if (tenum != null) {
            SegmentMergeInfo smi = tenum.matchingSegments[matchingSegmentPos++];
            if (smi==null) {
              pointer = readers.length;
              return false;
            }
            pointer = smi.ord;
          }
          base = starts[pointer];
          current = termDocs(pointer++);
        } else
          return false;
      }
    }
  
    private TermDocs termDocs(int i) throws IOException {
      TermDocs result = readerTermDocs[i];
      if (result == null)
        result = readerTermDocs[i] = termDocs(readers[i]);
      if (smi != null) {
        assert(smi.ord == i);
        assert(smi.termEnum.term().equals(term));
        result.seek(smi.termEnum);
      } else {
        result.seek(term);
      }
      return result;
    }
  
    protected TermDocs termDocs(IndexReader reader)
      throws IOException {
      return term==null ? reader.termDocs(null) : reader.termDocs();
    }
  
    public void close() throws IOException {
      for (int i = 0; i < readerTermDocs.length; i++) {
        if (readerTermDocs[i] != null)
          readerTermDocs[i].close();
      }
    }
  }

  static class MultiTermPositions extends MultiTermDocs implements TermPositions {
    public MultiTermPositions(IndexReader topReader, IndexReader[] r, int[] s) {
      super(topReader,r,s);
    }
  
    protected TermDocs termDocs(IndexReader reader) throws IOException {
      return (TermDocs)reader.termPositions();
    }
  
    public int nextPosition() throws IOException {
      return ((TermPositions)current).nextPosition();
    }
    
    public int getPayloadLength() {
      return ((TermPositions)current).getPayloadLength();
    }
     
    public byte[] getPayload(byte[] data, int offset) throws IOException {
      return ((TermPositions)current).getPayload(data, offset);
    }
  
  
    // TODO: Remove warning after API has been finalized
    public boolean isPayloadAvailable() {
      return ((TermPositions) current).isPayloadAvailable();
    }
  }
}
