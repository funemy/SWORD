/**
 * Copyright (c) 2004-2005, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * Neither the name of the University of California, Los Angeles nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package cck.test;

import cck.text.Status;
import cck.text.Terminal;
import cck.util.*;

import java.io.*;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * The <code>AutomatedTester</code> is a class that is designed to be an in-program test facility. It is
 * capable of reading in test cases from files, extracting properties specified in those test cases, and then
 * creating a test case of the right form for the right system.
 * <p/>
 * This is done through the use of another class, the <code>TestHarness</code>, which is capable of creating
 * instances of the <code>TestCase</code> class that are collected by this framework.
 *
 * @author Ben L. Titzer
 * @see TestCase
 * @see TestResult
 */
public class TestEngine {

    public static int MAXIMUM_TEST_MS = 5000;

    public static boolean LONG_REPORT;
    public static boolean PROGRESS_REPORT;
    public static boolean STATISTICS;
    public static int THREADS = 1;
    public static int VERBOSE = 1;

    /**
     * The <code>TestHarness</code> interface encapsulates the notion of a testing harness that is capable of
     * creating the correct type of test cases given the file and a list of properties extracted from the file by
     * the automated testing framework.
     *
     * @author Ben L. Titzer
     */
    public interface Harness {

        /**
         * The <code>newTestCase()</code> method creates a new test case of the right type given the file name and
         * the properties already extracted from the file by the testing framework.
         *
         * @param fname the name of the file
         * @param props a list of properties extracted from the file
         * @return an instance of the <code>TestCase</code> class
         * @throws Exception if there is a problem creating the testcase or reading it
         */
        public TestCase newTestCase(String fname, Properties props) throws Exception;
    }

    public List[] results;
    public List successes;

    private String[] testNames;
    private int numTests;
    private int currentTest;
    private int finishedTests;

    private final ClassMap harnessMap;

    /**
     * The constructor for the <code>TestEngine</code> class creates a new test engine
     * with the specified class map. The class map is used to map short names in the
     * properties of test files to the class that implements the harness.
     * @param hm the class map that maps string names to harnesses
     */
    public TestEngine(ClassMap hm) {
        harnessMap = hm;
    }

    /**
     * The <code>runTests()</code> method runs the testing framework on each of the specified filenames. The
     * name of the target for the testcase is specified in the text of each file. The testing framework
     * extracts that name and passes that to the test harness which can create an instance of
     * <code>TestCase</code>. Each test case is then run and the results are tabulated.
     *
     * @param fnames an array of the filenames of tests to run
     * @throws IOException if there is a problem loading the test cases
     * @return true if all the tests pass
     */
    public boolean runTests(String[] fnames) throws IOException {
        // record start time
        long time = System.currentTimeMillis();

        // initialize the lists of tests and fields
        initTests(fnames);

        // run all the test cases
        runAllTests();

        // record end time
        time = System.currentTimeMillis() - time;

        // report failures
        reportFailures();

        // report successes
        reportSuccesses(time);

        // report statistics
        reportStatistics(results);

        // return true if all tests passed
        return successes.size() == numTests;
    }

    private void reportFailures() {
        if (VERBOSE > 0) {
            report("Internal errors", results, TestResult.INTERNAL, numTests);
            report("Unexpected exceptions", results, TestResult.EXCEPTION, numTests);
            report("Failed", results, TestResult.FAILURE, numTests);
            report("Malformed test cases", results, TestResult.MALFORMED, numTests);
            report("Nonterminating cases", results, TestResult.NONTERM, numTests);
        }
    }

    private void reportSuccesses(long time) {
        if (VERBOSE > 0) {
            Terminal.printBrightGreen("Passed");
            Terminal.print(": " + successes.size());
            Terminal.print(" of " + numTests);
            Terminal.print(" in " + TimeUtil.milliToSecs(time) +" seconds");
            Terminal.nextln();
        }
    }

    private void initTests(String[] fnames) {
        Status.ENABLED = false;
        this.testNames = fnames;
        this.numTests = fnames.length;
        currentTest = 0;

        results = new LinkedList[TestResult.MAX_CODE];
        for ( int cntr = 0; cntr < TestResult.MAX_CODE; cntr++ )
            results[cntr] = new LinkedList();

        successes = results[TestResult.SUCCESS];
    }

    private void runAllTests() {
        try {
            // create worker threads
            WorkThread[] threads = new WorkThread[THREADS];
            for ( int cntr = 0; cntr < THREADS; cntr++ ) {
                WorkThread thread = new WorkThread();
                threads[cntr] = thread;
                thread.start();
            }
            // if any thread is still running, check it hasn't run too long.
            while ( finishedTests < numTests) {
                long now = System.currentTimeMillis();
                for ( int cntr = 0; cntr < THREADS; cntr++ ) {
                    WorkThread thread = threads[cntr];
                    if ( thread.intest && (now - thread.test_began) > MAXIMUM_TEST_MS) {
                        thread.interrupt();
                        thread.stop(new NonTermination(now - thread.test_began));
                    }
                }
                synchronized(this) {
                    // last thread will signal us upon completion, but wait 1000 ms tops.
                    this.wait(1000);
                }
            }
        } catch (InterruptedException e) {
            throw Util.unexpected(e);
        }
    }

    private void runTest(int num) {
        try {
            TestCase tc = runTest(testNames[num]);
        } catch (IOException e) {
            throw Util.unexpected(e);
        }
    }

    protected int nextTest() {
        synchronized(this) {
            if ( currentTest < numTests) return currentTest++;
            else return -1;
        }
    }

    private void finishTest(TestCase tc) {
        synchronized(this) {
            results[tc.result.code].add(tc);
            finishedTests++;
            reportVerbose(tc);
            if ( finishedTests >= numTests ) {
                this.notifyAll();
            }
        }
    }
    private void reportStatistics(List[] tests) {
        if ( STATISTICS ) {
            for ( int cntr = 0; cntr < tests.length; cntr++ ) {
                Iterator i = tests[cntr].iterator();
                while (i.hasNext()) {
                    TestCase tc = (TestCase) i.next();
                    tc.reportStatistics();
                }
            }
        }
    }

    private static void report(String c, List[] lists, int w, int total) {
        List list = lists[w];
        if (list.isEmpty()) return;

        Terminal.print(TestResult.getColor(w), c);
        Terminal.println(": " + list.size() + " of " + total);
        Iterator i = list.iterator();
        while (i.hasNext()) {
            TestCase tc = (TestCase) i.next();
            report(tc.getFileName(), tc.result);
        }
    }

    /**
     * The <code>report()</code> method generates a textual report of the results of running the test case.
     *
     * @param fname  the name of the file
     * @param result the result of the test
     */
    private static void report(String fname, TestResult result) {
        Terminal.print("  ");
        Terminal.printRed(fname);
        Terminal.print(": ");
        if (LONG_REPORT) result.longReport();
        else result.shortReport();
        Terminal.print("\n");
    }

    private TestCase runTest(String fname) throws IOException {
        TestCase tc = readTestCase(fname);
        Throwable exception = null;

        try {
            beginVerbose(fname);
            tc.run();
        } catch (Throwable t) {
            exception = t;
        }

        try {
            tc.result = tc.match(exception);
        } catch (Throwable t) {
            tc.result = new TestResult.UnexpectedException("exception in match routine: ", t);
        }
        finishTest(tc);
        return tc;
    }

    private void beginVerbose(String fname) {
        if ( VERBOSE == 3 ) {
            Terminal.print("Running "+fname+"...");
            Terminal.flush();
        }
    }

    private void reportVerbose(TestCase tc) {
        if ( VERBOSE == 3 ) {
            if ( tc.result.isSuccess() ) Terminal.printGreen("passed");
            else Terminal.printRed("failed");
            Terminal.nextln();
        } else if (VERBOSE == 2) {
            if ( tc.result.isSuccess() ) {
                Terminal.printGreen("o");
            } else {
                Terminal.printRed("X");
            }
            if (finishedTests % 50 == 0 || finishedTests >= numTests) {
                Terminal.print(" "+ finishedTests + " of " + numTests);
                Terminal.nextln();
            } else if (finishedTests % 10 == 0) {
                Terminal.print(" ");
            }
            Terminal.flush();
        }
    }

    private TestCase readTestCase(String fname) throws IOException {
        BufferedReader r = new BufferedReader(new FileReader(fname));
        Properties vars = new Properties();

        while (true) {
            String buffer = r.readLine();
            if (buffer == null) break;

            int index = buffer.indexOf('@');
            if (index < 0) break;

            int index2 = buffer.indexOf(':');
            if (index2 < 0) break;

            String var = buffer.substring(index + 1, index2).trim();
            String val = buffer.substring(index2 + 1).trim();

            vars.put(var, val);
        }

        r.close();

        String expect = vars.getProperty("Result");
        String hname = vars.getProperty("Harness");

        if (expect == null) return new TestCase.Malformed(fname, "no result specified");
        if (hname == null) return new TestCase.Malformed(fname, "no test harness specified");

        try {
            Harness harness = (Harness) harnessMap.getObjectOfClass(hname);
            return harness.newTestCase(fname, vars);
        } catch (Throwable t) {
            return new TestCase.InitFailure(fname, t);
        }
    }

    public class NonTermination extends Util.Error {
        public long milliseconds;

        public NonTermination(long ms) {
            super("Nontermination Error", "test did not terminate after "+ms+" ms");
            milliseconds = ms;
        }
    }

    protected class WorkThread extends Thread {
        volatile boolean intest;
        volatile long test_began;

        public void run() {
            for ( int num = nextTest(); num >= 0; num = nextTest() ) {
                test_began = System.currentTimeMillis();
                intest = true;
                runTest(num);
                intest = false;
            }
        }

    }
}
