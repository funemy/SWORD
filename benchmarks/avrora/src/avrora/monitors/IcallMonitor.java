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

import avrora.arch.legacy.*;
import avrora.core.Program;
import avrora.core.SourceMapping;
import avrora.sim.Simulator;
import avrora.sim.State;
import cck.text.StringUtil;
import cck.text.Terminal;

/**
 * The <code>Icall</code> monitor was motivated by a bug that we found
 * in avr-gcc that causes icalls to go off into neverneverland:
 * <p/>
 * http://gcc.gnu.org/bugzilla/show_bug.cgi?id=27192
 *
 * @author John Regehr
 * @author Ben L. Titzer
 */
public class IcallMonitor extends MonitorFactory {

    public class Mon implements Monitor {
        public final Simulator simulator;
        private final SourceMapping sourceMap;

        Mon(Simulator s) {
            simulator = s;

            Program p = s.getProgram();
            sourceMap = p.getSourceMapping();
            for (int pc = 0; pc < p.program_end; pc = p.getNextPC(pc)) {
                LegacyInstr i = (LegacyInstr) p.readInstr(pc);
                if (i != null) {
                    if (i instanceof LegacyInstr.ICALL) s.insertProbe(new IcallProbe(), pc);
                }
            }
        }

        public class IcallProbe extends Simulator.Probe.Empty {

            public void fireBefore(State state, int pc) {
                reportIndirectCall(state, pc);
            }
        }

        public void reportIndirectCall(State state, int pc) {
            LegacyState s = (LegacyState) simulator.getState();
            int icall_addr = 2 * s.getRegisterWord(LegacyRegister.getRegisterByNumber(30));

            String icall_fn = sourceMap.getName(icall_addr);

            if (icall_addr == 0) {
                Terminal.printRed("OOPS: icall to 0000");
                Terminal.nextln();
                System.exit(-1);
            }

            if (icall_fn == null) {
                Terminal.printRed("OOPS: probably bogus icall to ");
                Terminal.printRed(StringUtil.toHex(icall_addr, 4));
                Terminal.printRed(" " + icall_fn);
                Terminal.nextln();
            }
        }

        public void report() {
            // do nothing
        }

    }

    public IcallMonitor() {
        super("The \"icall\" monitor is used to detect possible bogus icalls, it " + "simply checks that the target for each icall has a source mapping " + "associated with it.");
    }

    public Monitor newMonitor(Simulator s) {
        return new Mon(s);
    }
}
