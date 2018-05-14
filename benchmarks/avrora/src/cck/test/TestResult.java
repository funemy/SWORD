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
import cck.text.Terminal;
import cck.util.Util;

/**
 * The <code>TestResult</code> class represents the result of running a test cases. The test run could
 * succeed, it could cause an internal error, an unexpected exception (e.g.
 * <code>java.lang.NullPointerException</code>), or it could generate an expected error such as a compilation
 * error (for testing generation of error messages).
 *
 * @author Ben L. Titzer
 */
public abstract class TestResult {

    public static final int SUCCESS = 0;
    public static final int FAILURE = 1;
    public static final int EXCEPTION = 2;
    public static final int INTERNAL = 3;
    public static final int MALFORMED = 4;
    public static final int NONTERM = 5;
    public static final int MAX_CODE = 6;

    public final int code;

    public TestResult(int c) {
        code = c;
    }

    public abstract void shortReport();

    public boolean isSuccess() {
        return code == SUCCESS;
    }

    public int getColor() {
        return getColor(this.code);
    }

    static int getColor(int code) {
        switch ( code ) {
            case SUCCESS: return Terminal.COLOR_GREEN;
            case EXCEPTION: return Terminal.COLOR_YELLOW;
            case INTERNAL: return Terminal.COLOR_YELLOW;
            case NONTERM: return Terminal.COLOR_YELLOW;
            case MALFORMED: return Terminal.COLOR_CYAN;
        }
        return Terminal.COLOR_RED;
    }

    public void longReport() {
        shortReport();
    }

    public static class TestSuccess extends TestResult {
        public TestSuccess() {
            super(SUCCESS);
        }
        public void shortReport() {
            Terminal.print("passed");
        }
    }

    public static class TestFailure extends TestResult {

        public final String message;
        public final Throwable thrown;

        public TestFailure() {
            super(FAILURE);
            message = "failed";
            thrown = null;
        }

        public TestFailure(String r) {
            super(FAILURE);
            message = r;
            thrown = null;
        }

        public TestFailure(String r, Throwable t) {
            super(FAILURE);
            message = r;
            thrown = t;
        }

        public void shortReport() {
            Terminal.print(message);
        }
    }

    public static class IncorrectError extends TestFailure {
        public final String expected;
        public final SourceError encountered;

        public IncorrectError(String ex, SourceError ce) {
            super("expected error " + ex + ", but received " + ce.getErrorType(), ce);
            expected = ex;
            encountered = ce;
        }

        public void longReport() {
            Terminal.println("expected error " + expected + " but received");
            encountered.report();
        }
    }

    public static class ExpectedPass extends TestFailure {
        public final SourceError encountered;

        public ExpectedPass(SourceError e) {
            super("expected pass, but received error " + e.getErrorType(), e);
            encountered = e;
        }

        public void longReport() {
            Terminal.println("expected pass, but received error");
            encountered.report();
        }
    }

    public static class ExpectedError extends TestFailure {
        public final String expected;

        public ExpectedError(String e) {
            super("expected error " + e + ", but passed");
            expected = e;
        }
    }

    public static class InternalError extends TestFailure {
        public final Util.InternalError encountered;

        public InternalError(Util.InternalError e) {
            super("encountered internal error: " + e.getMessage(), e);
            encountered = e;
        }

        public void longReport() {
            Terminal.print("encountered internal error\n");
            encountered.report();
        }
    }

    public static class NonTermError extends TestFailure {
        public final TestEngine.NonTermination encountered;

        public NonTermError(TestEngine.NonTermination e) {
            super("Test did not terminate after " + e.milliseconds+" ms", e);
            encountered = e;
        }

        public void longReport() {
            Terminal.print("test did not terminate\n");
            encountered.report();
        }
    }

    public static class UnexpectedException extends TestFailure {
        public final Throwable encountered;

        public UnexpectedException(Throwable e) {
            super("encountered unexpected exception " + e.getClass(), e);
            encountered = e;
        }

        public UnexpectedException(String msg, Throwable e) {
            super(msg + e.getClass(), e);
            encountered = e;
        }

        public void longReport() {
            Terminal.println("encountered unexpected exception");
            encountered.printStackTrace();
            Throwable cause = encountered.getCause();
            if ( cause != null ) {
                Terminal.printRed("Caused by");
                Terminal.print(": ");
                cause.printStackTrace();
            }
        }
    }

    public static class Malformed extends TestResult {
        public final String error;

        public Malformed(String e) {
            super(MALFORMED);
            error = e;
        }

        public void shortReport() {
            Terminal.print("malformed testcase: " + error);
        }
    }
}
