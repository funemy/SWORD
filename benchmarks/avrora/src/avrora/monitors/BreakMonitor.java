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

package avrora.monitors;

import avrora.arch.AbstractInstr;
import avrora.arch.legacy.*;
import avrora.core.Program;
import avrora.core.SourceMapping;
import avrora.sim.Simulator;
import avrora.sim.State;
import avrora.sim.output.SimPrinter;
import cck.text.StringUtil;
import cck.text.Terminal;

/**
 * The <code>Break</code> monitor prints a stack trace when the
 * program being simulated executes a break instruction.  Breaks are
 * are good implementation for assertion failures and similar.
 *
 * @author Reet D. Dhakal
 * @author John Regehr
 * @author Ben L. Titzer
 */
public class BreakMonitor extends MonitorFactory {

    public class Mon implements Monitor {
        public final Simulator simulator;
        public final SimPrinter printer;
        public final CallTrace trace;
        public final CallStack stack;
        private final SourceMapping sourceMap;

        Mon(Simulator s) {
            simulator = s;
            printer = s.getPrinter();
            trace = new CallTrace(s);
            stack = new CallStack();
            trace.attachMonitor(stack);

            Program p = s.getProgram();
            sourceMap = p.getSourceMapping();
            for (int pc = 0; pc < p.program_end; pc = p.getNextPC(pc)) {
                AbstractInstr i = p.readInstr(pc);
                if (i != null && i instanceof LegacyInstr.BREAK)
                    s.insertProbe(new BreakProbe(), pc);
            }
        }

        public class BreakProbe extends Simulator.Probe.Empty {

            public void fireBefore(State state, int pc) {
                LegacyState s = (LegacyState) simulator.getState();

                StringBuffer buf = printer.getBuffer();
                buf.append("break instruction @ ");
                Terminal.append(Terminal.COLOR_CYAN, buf, StringUtil.addrToString(pc));
                buf.append(", r30:r31 = ");
                int v = s.getRegisterWord(LegacyRegister.getRegisterByNumber(30));
                Terminal.append(Terminal.COLOR_GREEN, buf, StringUtil.to0xHex(v, 4));
                printer.printBuffer(buf);

                stack.printStack(printer, sourceMap);
            }
        }

        public void report() {
            // do nothing
        }
    }

    public BreakMonitor() {
        super("The \"break\" monitor watches for execution of an AVR break " +
                "instruction, which can be used to implement things like assertion " +
                "failures.  When a break is executed the simulator prints a stack " +
                "trace.");
    }

    public Monitor newMonitor(Simulator s) {
        return new Mon(s);
    }
}
