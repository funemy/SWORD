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

import cck.parser.SourceError;
import cck.text.StringUtil;
import cck.util.Util;

import java.util.Properties;

/**
 * The <code>TestCase</code> class encapsulates the notion of a test case in the automated testing
 * framework. A test case corresponds to a single file and a list of properties that determine the
 * type of the test case and any parameters to the testing framework necessary to set up the test
 * case. Additionally, the test case contains an expected result that is checked against the
 * actual result by the <code>match()</code> method.
 *
 * @author Ben L. Titzer
 */
public abstract class TestCase {

    protected final String filename;
    protected final Properties properties;
    protected TestResult result;

    /**
     * The default constructor for the <code>TestCase</code> class creates a new test case corresponding
     * to the specified file with the specified properties.
     *
     * @param fname the name of the file that contains the test case
     * @param props the testing properties extracted automatically from the text of the test case
     */
    public TestCase(String fname, Properties props) {
        filename = fname;
        properties = props;
    }

    /**
     * The <code>getFileName()</code> method returns the filename corresponding to this test case.
     *
     * @return the name of the file that contains this test case
     */
    public String getFileName() {
        return filename;
    }

    /**
     * The <code>run()</code> method executes the test case and may generate an exception.
     * When completed, this method's result is compared agains the expected result by calling
     * the <code>match()</code> method.
     *
     * @throws Exception of any kind during the execution of the test case
     */
    public abstract void run() throws Exception;

    /**
     * The <code>match()</code> method of a test case is called after the test case completes. If
     * the <code>run()</code> method throws an exception (Throwable), this method will be passed
     * the exception that was generated. If no exception was generated, then the exception passed
     * will be null. This method should return a new instance of the <code>TestResult</code> class
     * that represents the results of the test case.
     *
     * @param t the exception (if any) that was thrown while running the test case
     * @return a new <code>TestResult</code> indicating success or error
     */
    public TestResult match(Throwable t) {
        // default behavior: no exception = pass
        if (t == null) return new TestResult.TestSuccess();
        // nontermination error encountered.
        if (t instanceof TestEngine.NonTermination) return new TestResult.NonTermError((TestEngine.NonTermination) t);
        // internal error encountered.
        if (t instanceof Util.InternalError) return new TestResult.InternalError((Util.InternalError) t);
        // default: unexpected exception
        return new TestResult.UnexpectedException(t);
    }

    protected String expectProperty(String prop) {
        String value = properties.getProperty(prop);
        if (value == null) Util.userError("Property " + StringUtil.quote(prop) + " not found in testcase");
        return trimString(value);
    }

    protected String trimString(String str) {
        return StringUtil.trimquotes(str.trim());
    }

    /**
     * The <code>ExpectSourceError</code> class implements a test case that expects that a
     * source error (or PASS) will be generated by the <code>run()</code> method of the test
     * case. This class implements matcher methods that check that the correct source error
     * is generated.
     */
    public abstract static class ExpectSourceError extends TestCase {

        protected boolean shouldPass;
        protected String error;

        public ExpectSourceError(String fname, Properties props) {
            super(fname, props);
            String result = StringUtil.trimquotes(props.getProperty("Result"));
            if ("PASS".equals(result)) shouldPass = true;
            else {
                // format = "$id @ $num:$num"
                int i = result.indexOf('@');
                if (i >= 0) error = result.substring(0, i).trim();
                else error = result;
            }
        }

        /**
         * The <code>match()</code> method of a test case is called after the test case completes. If
         * the <code>run()</code> method throws an exception (Throwable), this method will be passed
         * the exception that was generated. If no exception was generated, then the exception passed
         * will be null. This method should return a new instance of the <code>TestResult</code> class
         * that represents the results of the test case.
         *
         * @param t the exception (if any) that was thrown while running the test case
         * @return a new <code>TestResult</code> indicating success or error
         */
        public TestResult match(Throwable t) {
            return shouldPass ? expectPass(t) : expectError(t);
        }

        /**
         * The <code>expectPass()</code> method is called by the <code>match()</code> method
         * when the test case is expected to pass.
         *
         * @param t the exception (if any) thrown during the test case
         * @return the result of the test case
         */
        protected TestResult expectPass(Throwable t) {
            if (t == null) { // no exceptions encountered, check heap
                return checkPass();
            } else { // encountered some type of error.
                return checkError(t);
            }
        }

        /**
         * The <code>expectError()</code> method is called by the <code>match()</code> method
         * when the test case is expected to generate an error.
         *
         * @param t the exception (if any) thrown during the test case
         * @return the result of the test case
         */
        protected TestResult expectError(Throwable t) {
            if (t == null) {
                return new TestResult.ExpectedError(error);
            } else {
                return matchError(t);
            }
        }

        /**
         * The <code>checkPass()</code> method is called by the <code>match()</code> method
         * when the test case is expected to pass (without generating exceptions) and no
         * exception was thrown during the execution of the test case.
         *
         * @return the result of the test case
         */
        protected TestResult checkPass() {
            return new TestResult.TestSuccess();
        }

        /**
         * The <code>checkError()</code> method is called by the <code>match()</code> method
         * when the test case is expected to pass and an exception was
         * generated. This always results in failure; this method simply checks what type of
         * failure it was.
         *
         * @param t the exception generated (non-null)
         * @return a failure result based on the type of the error
         */
        protected TestResult checkError(Throwable t) {
            if (t instanceof SourceError) { // encountered compilation (or runtime) error.
                return new TestResult.ExpectedPass((SourceError) t);
            }
            return super.match(t);
        }

        /**
         * The <code>matchError()</code> method is called by the <code>match()</code> method
         * when the test case is expected to generate an error and some type of exception is
         * thrown. This method checks the exception thrown against the exception type expected
         * and returns success or failure.
         *
         * @param t the exception thrown during the run of the test case
         * @return a test result depending on the type of the exception
         */
        protected TestResult matchError(Throwable t) {
            if (t instanceof SourceError) {
                SourceError ce = (SourceError) t;
                if (ce.getErrorType().equals(error)) // correct error encountered.
                    return new TestResult.TestSuccess();
                else // incorrect compilation error.
                    return new TestResult.IncorrectError(error, ce);
            }
            return super.match(t);
        }
    }

    public static class Malformed extends TestCase {
        final String error;

        public Malformed(String fname, String e) {
            super(fname, null);
            error = e;
        }

        public void run() {
            // do nothing.
        }

        public TestResult match(Throwable t) {
            return new TestResult.Malformed(error);
        }
    }

    public static class InitFailure extends TestCase {
        final Throwable thrown;

        public InitFailure(String fname, Throwable t) {
            super(fname, null);
            thrown = t;
        }

        public void run() {
            // do nothing.
        }

        public TestResult match(Throwable t) {
            return new TestResult.Malformed(thrown.toString());
        }
    }

    /**
     * The <code>reportStatistics()</code> method can be used by a test case to optionally
     * report any statistics gathered by the test when it was executed. For example,
     * a common task is to record the time taken to execute the test.
     */
    public void reportStatistics() {
        // default: do nothing.
    }
}
