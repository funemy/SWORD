/*
 * Copyright 2004-2018 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.engine;

import java.util.WeakHashMap;

import org.h2.message.Trace;

/**
 * This class is responsible to close a database on JVM shutdown.
 */
class OnExitDatabaseCloser extends Thread {

    private static final WeakHashMap<Database, Void> DATABASES = new WeakHashMap<>();

    private static final Thread INSTANCE = new OnExitDatabaseCloser();

    private static boolean registered;

    static synchronized void register(Database db) {
        DATABASES.put(db, null);
        if (!registered) {
            // Mark as registered unconditionally to avoid further attempts to register a
            // shutdown hook in case of exception.
            registered = true;
            try {
                Runtime.getRuntime().addShutdownHook(INSTANCE);
            } catch (IllegalStateException e) {
                // shutdown in progress - just don't register the handler
                // (maybe an application wants to write something into a
                // database at shutdown time)
            } catch (SecurityException e) {
                // applets may not do that - ignore
                // Google App Engine doesn't allow
                // to instantiate classes that extend Thread
            }
        }
    }

    static synchronized void unregister(Database db) {
        DATABASES.remove(db);
        if (DATABASES.isEmpty() && registered) {
            try {
                Runtime.getRuntime().removeShutdownHook(INSTANCE);
            } catch (IllegalStateException e) {
                // ignore
            } catch (SecurityException e) {
                // applets may not do that - ignore
            }
            registered = false;
        }
    }

    private OnExitDatabaseCloser() {
    }

    @Override
    public void run() {
        RuntimeException root = null;
        for (Database database : DATABASES.keySet()) {
            try {
                database.close(true);
            } catch (RuntimeException e) {
                // this can happen when stopping a web application,
                // if loading classes is no longer allowed
                // it would throw an IllegalStateException
                try {
                    database.getTrace(Trace.DATABASE).error(e, "could not close the database");
                    // if this was successful, we ignore the exception
                    // otherwise not
                } catch (Throwable e2) {
                    e.addSuppressed(e2);
                    if (root == null) {
                        root = e;
                    } else {
                        root.addSuppressed(e);
                    }
                }
            }
        }
        if (root != null) {
            throw root;
        }
    }

}
