/*
 * Copyright 2004-2013 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.test.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;

import org.h2.test.TestBase;
import org.h2.util.Utils;

/**
 * Tests the memory usage of the cache.
 */
public class TestMemoryUsage extends TestBase {

    private Connection conn;

    /**
     * Run just this test.
     *
     * @param a ignored
     */
    public static void main(String... a) throws Exception {
        TestBase.createCaller().init().test();
    }

    @Override
    public void test() throws SQLException {
        testOpenCloseConnections();
        if (getBaseDir().indexOf(':') >= 0) {
            // can't test in-memory databases
            return;
        }
        testCreateDropLoop();
        testCreateIndex();
        testClob();
        testReconnectOften();
        deleteDb("memoryUsage");
        reconnect();
        insertUpdateSelectDelete();
        reconnect();
        insertUpdateSelectDelete();
        conn.close();
        deleteDb("memoryUsage");
    }

    private void testOpenCloseConnections() throws SQLException {
        if (!config.big) {
            return;
        }
        deleteDb("memoryUsage");
        conn = getConnection("memoryUsage");
        eatMemory(4000);
        for (int i = 0; i < 4000; i++) {
            Connection c2 = getConnection("memoryUsage");
            c2.createStatement();
            c2.close();
        }
        freeMemory();
        conn.close();
    }

    private void testCreateDropLoop() throws SQLException {
        deleteDb("memoryUsage");
        conn = getConnection("memoryUsage");
        Statement stat = conn.createStatement();
        for (int i = 0; i < 100; i++) {
            stat.execute("CREATE TABLE TEST(ID INT)");
            stat.execute("DROP TABLE TEST");
        }
        int used = Utils.getMemoryUsed();
        for (int i = 0; i < 1000; i++) {
            stat.execute("CREATE TABLE TEST(ID INT PRIMARY KEY)");
            stat.execute("DROP TABLE TEST");
        }
        int usedNow = Utils.getMemoryUsed();
        if (usedNow > used * 1.3) {
            assertEquals(used, usedNow);
        }
        conn.close();
    }


    private void reconnect() throws SQLException {
        if (conn != null) {
            conn.close();
        }
        // Class.forName("org.hsqldb.jdbcDriver");
        // conn = DriverManager.getConnection("jdbc:hsqldb:test", "sa", "");
        conn = getConnection("memoryUsage");
    }

    private void testClob() throws SQLException {
        if (config.memory || !config.big) {
            return;
        }
        deleteDb("memoryUsage");
        conn = getConnection("memoryUsage");
        Statement stat = conn.createStatement();
        stat.execute("SET MAX_LENGTH_INPLACE_LOB 8192");
        stat.execute("SET CACHE_SIZE 8000");
        stat.execute("CREATE TABLE TEST(ID IDENTITY, DATA CLOB)");
        freeSoftReferences();
        try {
            int base = Utils.getMemoryUsed();
            for (int i = 0; i < 4; i++) {
                stat.execute("INSERT INTO TEST(DATA) " +
                        "SELECT SPACE(8000) FROM SYSTEM_RANGE(1, 800)");
                freeSoftReferences();
                int used = Utils.getMemoryUsed();
                if ((used - base) > 3 * 8192) {
                    fail("Used: " + (used - base) + " i: " + i);
                }
            }
        } finally {
            conn.close();
            freeMemory();
        }
    }

    /**
     * Eat memory so that all soft references are garbage collected.
     */
    void freeSoftReferences() {
        try {
            eatMemory(1);
        } catch (OutOfMemoryError e) {
            // ignore
        }
        System.gc();
        System.gc();
        freeMemory();
    }

    private void testCreateIndex() throws SQLException {
        if (config.memory) {
            return;
        }
        deleteDb("memoryUsage");
        conn = getConnection("memoryUsage");
        Statement stat = conn.createStatement();
        stat.execute("create table test(id int, name varchar(255))");
        PreparedStatement prep = conn.prepareStatement(
                "insert into test values(?, space(200))");
        int len = getSize(10000, 100000);
        for (int i = 0; i < len; i++) {
            if (i % 1000 == 0) {
                // trace("[" + i + "/" + len + "] KB: " +
                //         MemoryUtils.getMemoryUsed());
            }
            prep.setInt(1, i);
            prep.executeUpdate();
        }
        int base = Utils.getMemoryUsed();
        stat.execute("create index idx_test_id on test(id)");
        System.gc();
        System.gc();
        int used = Utils.getMemoryUsed();
        if ((used - base) > getSize(7500, 12000)) {
            fail("Used: " + (used - base));
        }
        stat.execute("drop table test");
        conn.close();
    }

    private void testReconnectOften() throws SQLException {
        deleteDb("memoryUsage");
        Connection conn1 = getConnection("memoryUsage");
        int len = getSize(1, 2000);
        printTimeMemory("start", 0);
        long time = System.currentTimeMillis();
        for (int i = 0; i < len; i++) {
            Connection conn2 = getConnection("memoryUsage");
            conn2.close();
            if (i % 10000 == 0) {
                printTimeMemory("connect", System.currentTimeMillis() - time);
            }
        }
        printTimeMemory("connect", System.currentTimeMillis() - time);
        conn1.close();
    }

    private void insertUpdateSelectDelete() throws SQLException {
        Statement stat = conn.createStatement();
        long time;
        int len = getSize(1, 2000);

        // insert
        time = System.currentTimeMillis();
        stat.execute("DROP TABLE IF EXISTS TEST");
        trace("drop=" + (System.currentTimeMillis() - time));
        stat.execute("CREATE CACHED TABLE TEST(ID INT PRIMARY KEY, NAME VARCHAR(255))");
        PreparedStatement prep = conn.prepareStatement(
                "INSERT INTO TEST VALUES(?, 'Hello World')");
        printTimeMemory("start", 0);
        time = System.currentTimeMillis();
        for (int i = 0; i < len; i++) {
            prep.setInt(1, i);
            prep.execute();
            if (i % 50000 == 0) {
                trace("  " + (100 * i / len) + "%");
            }
        }
        printTimeMemory("insert", System.currentTimeMillis() - time);

        // update
        time = System.currentTimeMillis();
        prep = conn.prepareStatement(
                "UPDATE TEST SET NAME='Hallo Welt' || ID WHERE ID = ?");
        for (int i = 0; i < len; i++) {
            prep.setInt(1, i);
            prep.execute();
            if (i % 50000 == 0) {
                trace("  " + (100 * i / len) + "%");
            }
        }
        printTimeMemory("update", System.currentTimeMillis() - time);

        // select
        time = System.currentTimeMillis();
        prep = conn.prepareStatement("SELECT * FROM TEST WHERE ID = ?");
        for (int i = 0; i < len; i++) {
            prep.setInt(1, i);
            ResultSet rs = prep.executeQuery();
            rs.next();
            assertFalse(rs.next());
            if (i % 50000 == 0) {
                trace("  " + (100 * i / len) + "%");
            }
        }
        printTimeMemory("select", System.currentTimeMillis() - time);

        // select randomized
        Random random = new Random(1);
        time = System.currentTimeMillis();
        prep = conn.prepareStatement("SELECT * FROM TEST WHERE ID = ?");
        for (int i = 0; i < len; i++) {
            prep.setInt(1, random.nextInt(len));
            ResultSet rs = prep.executeQuery();
            rs.next();
            assertFalse(rs.next());
            if (i % 50000 == 0) {
                trace("  " + (100 * i / len) + "%");
            }
        }
        printTimeMemory("select randomized", System.currentTimeMillis() - time);

        // delete
        time = System.currentTimeMillis();
        prep = conn.prepareStatement("DELETE FROM TEST WHERE ID = ?");
        for (int i = 0; i < len; i++) {
            prep.setInt(1, random.nextInt(len));
            prep.executeUpdate();
            if (i % 50000 == 0) {
                trace("  " + (100 * i / len) + "%");
            }
        }
        printTimeMemory("delete", System.currentTimeMillis() - time);
    }

}
