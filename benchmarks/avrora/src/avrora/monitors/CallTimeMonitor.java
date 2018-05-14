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

import avrora.core.*;
import avrora.sim.Simulator;
import cck.text.*;
import cck.util.Option;
import cck.util.Util;

/**
 * @author Akop Palyan
 * @author Ben L. Titzer
 */
public class CallTimeMonitor extends MonitorFactory {

    final Option.Str METHOD = newOption("method", "",
            "This option specifies the name of the method to profile.");
    final Option.Bool IGNR_INTRS = newOption("ignore-interrupts", false,
            "This option selects whether this monitor will consider time spent in nested interrupts to " +
            "be part of a method's execution time.");

    public CallTimeMonitor() {
        super("The \"MethodTimeMonitor\" monitor records profiling information about the " +
                "method that consists of the time it takes (on average) to execute a call.");
    }

    protected class CallTimeMon extends CallStack implements Monitor {

        final Simulator simulator;
        final Program program;

        final SourceMapping.Location start;
        final boolean ignore_interrupts;

        long cumul;
        long cumul_sqr;
        int count;
        long max;
        long min;

        int call_depth;
        long[] call_time = new long[256];

        long startInterrupt;
        long endInterrupt;

        CallTimeMon(Simulator s) {
            simulator = s;
            program = s.getProgram();

            cumul = 0;
            cumul_sqr = 0;
            max = 0;
            min = Long.MAX_VALUE;
            count = 0;

            ignore_interrupts = IGNR_INTRS.get();
            startInterrupt = 0;
            endInterrupt = 0;

            start = getLocation(METHOD.get());
            CallTrace trace = new CallTrace(s);
            trace.attachMonitor(this);
        }

        public void fireAfterReturn(long time, int pc, int retaddr) {
            if (getTarget(depth - 1) == start.lma_addr) {
                record(time - call_time[--call_depth] - (endInterrupt - startInterrupt));
                startInterrupt = endInterrupt = 0;
            }
            pop();
        }

        public void fireAfterInterruptReturn(long time, int pc, int retaddr) {
            if (ignore_interrupts && findCallAddress(start.lma_addr)) {
                endInterrupt = time;
            }
            super.fireAfterInterruptReturn(time, pc, retaddr);
        }

        public void fireBeforeCall(long time, int pc, int target) {
            if (target == start.lma_addr) call_time[call_depth++] = time;
            super.fireBeforeCall(time, pc, target);
        }

        public void fireBeforeInterrupt(long time, int pc, int inum) {
            if (ignore_interrupts && findCallAddress(start.lma_addr)) {
                startInterrupt = time;
            }
            super.fireBeforeInterrupt(time, pc, inum);
        }

        private boolean findCallAddress(int address) {
            for (int i = depth - 1; i >= 0; --i) {
                if (getTarget(i) == address) return true;
            }
            return false;
        }

        private void record(long time) {
            cumul += time;
            cumul_sqr += (time * time);
            max = Math.max(max, time);
            min = Math.min(min, time);
            count++;
        }

        private SourceMapping.Location getLocation(String src) {
            SourceMapping lm = program.getSourceMapping();
            SourceMapping.Location loc = lm.getLocation(src);
            if (loc == null) Util.userError("Invalid program address: ", src);
            if (program.readInstr(loc.lma_addr) == null) Util.userError("Invalid program address: ", src);
            return loc;
        }

        public void report() {
            TermUtil.printSeparator("Call time results for node "+simulator.getID());
            Terminal.printGreen(" function                 calls         avg       cumul        max        min");
            Terminal.nextln();
            TermUtil.printThinSeparator(Terminal.MAXLINE);

            float avg = (float)cumul / count;
            double std = Math.sqrt(((double)cumul_sqr / count) - (avg * avg));

            Terminal.println(" " + StringUtil.leftJustify(METHOD.get(), 20) + "  " + StringUtil.rightJustify(count, 8) + "  " + StringUtil.rightJustify(avg, 10) + "  " + StringUtil.rightJustify(cumul, 10) + "  " + StringUtil.rightJustify((float)max, 9) + "  " + StringUtil.rightJustify((float)min, 9));
            Terminal.nextln();
        }
    }

    public Monitor newMonitor(Simulator s) {
        return new CallTimeMon(s);
    }
}
