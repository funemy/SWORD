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

package avrora.actions;

import avrora.Defaults;
import cck.test.TestEngine;
import cck.text.Status;
import cck.util.Option;

/**
 * The <code>TestAction</code> class represents an action to invoke the built-in automated testing framework
 * that is used for regression testing in Avrora.
 *
 * @author Ben L. Titzer
 */
public class TestAction extends Action {
    public static final String HELP = "The \"test\" action invokes the internal automated testing framework " +
            "that runs test cases supplied at the command line. The test cases are " +
            "used in regressions for diagnosing bugs.";

    public final Option.Bool DETAIL = newOption("detail", false, "This option selects whether " +
            "the automated testing framework will report detailed information for failed test cases.");

    public TestAction() {
        super(HELP);
    }

    /**
     * The <code>run()</code> method starts the test harness and begins processing test cases.
     * @param args the command line arguments; files containing tests to be run
     * @throws Exception
     */
    public void run(String[] args) throws Exception {
        TestEngine.LONG_REPORT = DETAIL.get();
        Status.ENABLED = false;
        TestEngine engine = new TestEngine(Defaults.getTestHarnessMap());
        boolean r = engine.runTests(args);
        if (!r) System.exit(1);
    }
}
