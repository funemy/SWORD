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
import java.sql.Statement;
import org.h2.test.TestBase;

/**
 * Test recursive queries using WITH.
 */
public class TestRecursiveQueries extends TestBase {

    /**
     * Run just this test.
     *
     * @param a ignored
     */
    public static void main(String... a) throws Exception {
        TestBase.createCaller().init().test();
    }

    @Override
    public void test() throws Exception {
        testWrongLinkLargeResult();
        testSimple();
    }

    private void testWrongLinkLargeResult() throws Exception {
        deleteDb("recursiveQueries");
        Connection conn = getConnection("recursiveQueries");
        Statement stat;
        stat = conn.createStatement();
        stat.execute("create table test(parent varchar(255), child varchar(255))");
        stat.execute("insert into test values('/', 'a'), ('a', 'b1'), " +
                "('a', 'b2'), ('a', 'c'), ('c', 'd1'), ('c', 'd2')");

        ResultSet rs = stat.executeQuery(
                "with recursive rec_test(depth, parent, child) as (" +
                "select 0, parent, child from test where parent = '/' " +
                "union all " +
                "select depth+1, r.parent, r.child from test i join rec_test r " +
                "on (i.parent = r.child) where depth<9 " +
                ") select count(*) from rec_test");
        rs.next();
        assertEquals(29524, rs.getInt(1));
        stat.execute("with recursive rec_test(depth, parent, child) as ( "+
                "select 0, parent, child from test where parent = '/' "+
                "union all "+
                "select depth+1, i.parent, i.child from test i join rec_test r "+
                "on (r.child = i.parent) where depth<10 "+
                ") select * from rec_test");
        conn.close();
        deleteDb("recursiveQueries");
    }

    private void testSimple() throws Exception {
        deleteDb("recursiveQueries");
        Connection conn = getConnection("recursiveQueries");
        Statement stat;
        PreparedStatement prep, prep2;
        ResultSet rs;

        stat = conn.createStatement();
        rs = stat.executeQuery("with recursive t(n) as " +
                "(select 1 union all select n+1 from t where n<3) " +
                "select * from t");
        assertTrue(rs.next());
        assertEquals(1, rs.getInt(1));
        assertTrue(rs.next());
        assertEquals(2, rs.getInt(1));
        assertTrue(rs.next());
        assertEquals(3, rs.getInt(1));
        assertFalse(rs.next());

        prep = conn.prepareStatement("with recursive t(n) as " +
                "(select 1 union all select n+1 from t where n<3) " +
                "select * from t where n>?");
        prep.setInt(1, 2);
        rs = prep.executeQuery();
        assertTrue(rs.next());
        assertEquals(3, rs.getInt(1));
        assertFalse(rs.next());

        prep.setInt(1, 1);
        rs = prep.executeQuery();
        assertTrue(rs.next());
        assertEquals(2, rs.getInt(1));
        assertTrue(rs.next());
        assertEquals(3, rs.getInt(1));
        assertFalse(rs.next());

        prep = conn.prepareStatement("with recursive t(n) as " +
                "(select @start union all select n+@inc from t where n<@end) " +
                "select * from t");
        prep2 = conn.prepareStatement("select @start:=?, @inc:=?, @end:=?");
        prep2.setInt(1, 10);
        prep2.setInt(2, 2);
        prep2.setInt(3, 14);
        prep2.execute();
        rs = prep.executeQuery();
        assertTrue(rs.next());
        assertEquals(10, rs.getInt(1));
        assertTrue(rs.next());
        assertEquals(12, rs.getInt(1));
        assertTrue(rs.next());
        assertEquals(14, rs.getInt(1));
        assertFalse(rs.next());

        prep2.setInt(1, 100);
        prep2.setInt(2, 3);
        prep2.setInt(3, 103);
        prep2.execute();
        rs = prep.executeQuery();
        assertTrue(rs.next());
        assertEquals(100, rs.getInt(1));
        assertTrue(rs.next());
        assertEquals(103, rs.getInt(1));
        assertFalse(rs.next());

        conn.close();
        deleteDb("recursiveQueries");
    }

}
