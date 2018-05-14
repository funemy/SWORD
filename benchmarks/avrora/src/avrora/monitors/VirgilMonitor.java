/**
 * Copyright (c) 2006, Regents of the University of California
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
 * Creation date: Feb 26, 2007
 */

package avrora.monitors;

import avrora.sim.Simulator;
import avrora.sim.State;
import avrora.sim.output.SimPrinter;
import avrora.sim.util.SimUtil;
import avrora.core.SourceMapping;
import avrora.core.Program;
import avrora.arch.AbstractInstr;
import avrora.arch.legacy.*;
import cck.text.Terminal;
import cck.text.StringUtil;
import cck.util.Option;

/**
 * The <code>VirgilMonitor</code> monitor prints a stack trace when the
 * program being simulated executes a break instruction.  This version
 * uses the code in R0 to determine the specific source-level exception
 * that occurred.
 *
 * @author Ben L. Titzer
 */
public class VirgilMonitor extends MonitorFactory {

    public static final int ABORT_TYPE_CODE = 127;
    public static final int ABORT_NULL_CODE = 126;
    public static final int ABORT_BOUNDS_CODE = 125;
    public static final int ABORT_DIV_CODE = 124;
    public static final int ABORT_ALLOC_CODE = 123;
    public static final int ABORT_UNIMP_CODE = 122;

    public final Option.Long STATUS_ADDR = newOption("status-addr", 0x91,
            "This option specifies the address in memory where the status register lies. " +
                    "The status register is used to diagnose the cause of a program abort.");

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
                StringBuffer buf = printer.getBuffer();
                LegacyState s = (LegacyState) simulator.getState();
                int code = s.getDataByte((int)STATUS_ADDR.get());
                String name = "UnknownException";
                String msg = "an unknown exception occurred";
                switch (code) {
                    case 0: // normal program return from main
                        return;
                    case ABORT_TYPE_CODE:
                        name = "TypeCheckException";
                        msg = "type check exception in explicit cast";
                        break;
                    case ABORT_NULL_CODE:
                        name = "NullCheckException";
                        msg = "null check exception";
                        break;
                    case ABORT_BOUNDS_CODE:
                        name = "BoundsCheckException";
                        msg = "array bounds check exception";
                        break;
                    case ABORT_DIV_CODE:
                        name = "DivideByZeroException";
                        msg = "division by zero";
                        break;
                    case ABORT_ALLOC_CODE:
                        name = "AllocationException";
                        msg = "dynamic memory allocation failed";
                        break;
                    case ABORT_UNIMP_CODE:
                        name = "UnimplementedException";
                        msg = "method not implemented";
                        break;
                }
                Terminal.append(Terminal.COLOR_RED, buf, name);
                buf.append(": ").append(msg).append(" @ ");
                Terminal.append(Terminal.COLOR_BRIGHT_CYAN, buf, StringUtil.addrToString(pc));
                printer.printBuffer(buf);

                stack.printStack(printer, sourceMap);
            }
        }

        public void report() {
            // do nothing
        }
    }

    public VirgilMonitor() {
        super("The \"virgil\" monitor watches for execution of an AVR break " +
                "instruction, which is used by the Virgil compiler to signal fatal " +
                "exceptions.");
    }

    public Monitor newMonitor(Simulator s) {
        return new Mon(s);
    }
}
