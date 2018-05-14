/**
 * Copyright (c) 2007, Regents of the University of California
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
 *
 * Created Sep 1, 2007
 */
package avrora.actions;

import cck.util.Option;
import cck.util.Arithmetic;
import cck.text.*;
import avrora.sim.state.*;

/**
 * The <code>InternalTestAction</code> definition implements an action
 * that tests various simulation internals for correctness.
 *
 * @author Ben L. Titzer
 */
public class RegisterTestAction extends Action {
    public static final String HELP = "The \"test\" action invokes the internal automated testing framework " +
            "that runs test cases supplied at the command line. The test cases are " +
            "used in regressions for diagnosing bugs.";

    public final Option.Bool DETAIL = newOption("detail", false, "This option selects whether " +
            "the testing framework will report detailed information for failed test cases.");

    protected boolean detail;

    public RegisterTestAction() {
        super(HELP);
    }

    /**
     * The <code>run()</code> method starts the test harness and begins processing test cases.
     * @param args the command line arguments; files containing tests to be run
     * @throws Exception
     */
    public void run(String[] args) throws Exception {
        detail = DETAIL.get();

        Register r1 = new Register(16);
        writeAndPrint("R1", r1, 0x89);
        writeAndPrint("R1", r1, 0x1189);
        writeAndPrint("R1", r1, 0x89);
        writeAndPrint("R1", r1, 0x1AC89);

        RegisterView r2 = RegisterUtil.bitRangeView(r1, 0, 15);
        r1.write(0x89);
        checkRegister("R2", r2, 0x89);
        r2.setValue(0x7a);
        checkRegister("R1", r1, 0x7a);

        Register r3 = new Register(8);
        Register r4 = new Register(8);

        RegisterView r5 = RegisterUtil.stackedView(r3, r4);
        r5.setValue(0xa3b7);
        checkRegister("R3", r3, 0xb7);
        checkRegister("R4", r4, 0xa3);

        RegisterView r6 = RegisterUtil.bitRangeView(r5, 4, 7);
        RegisterView r7 = RegisterUtil.bitRangeView(r5, 10, 15);
        r5.setValue(0xabcd);
        checkRegister("R6", r6, 0xc);
        r6.setValue(0x7);
        checkRegister("R5", r5, 0xab7d);
        checkRegister("R7", r7, 0x2a);
        r7.setValue(0xff);
        checkRegister("R5", r5, 0xff7d);

        assert Arithmetic.getBitMask(16) == 0xffff;
        assert Arithmetic.getBitMask(32) == 0xffffffff;
    }

    private void writeAndPrint(String name, Register r, int val) {
        r.write(val);
        checkRegister(name, r, val);
    }

    void checkRegister(String n, RegisterView r, int val) {
        int rw = r.getWidth();
        String rval = StringUtil.toMultirepString(r.getValue(), rw);
        if ( (val & Arithmetic.getBitMask(rw)) == r.getValue() ) {
            Terminal.printGreen(n);
            Terminal.print(": ");
            Terminal.printCyan(rval);
            Terminal.nextln();
        } else {
            Terminal.printRed(n);
            Terminal.print(": ");
            Terminal.printCyan(rval);
            Terminal.print(" != ");
            Terminal.printCyan(StringUtil.toMultirepString(val, rw));
            Terminal.nextln();
        }
    }
}

