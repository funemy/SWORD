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
import avrora.core.Program;
import avrora.sim.Simulator;
import avrora.sim.State;
import cck.stat.StatUtil;
import cck.text.*;
import cck.util.Option;
import java.util.*;

/**
 * The <code>ProfileMonitor</code> class represents a monitor that can collect profiling information such as
 * counts and branchcounts about the program as it executes.
 *
 * @author Ben L. Titzer
 */
public class ProfileMonitor extends MonitorFactory {

    public final Option.Bool CYCLES = newOption("record-cycles", true,
            "This option controls whether this monitor will record " +
            "the cycles consumed by each instruction or basic block. ");
    public final Option.Long PERIOD = newOption("period", 0,
            "This option specifies whether the profiling will be exact or periodic. When " +
            "this option is set to non-zero, then a sample of the program counter is taken at " +
            "the specified period in clock cycles, rather than through probes at each instruction.");
    public final Option.Bool CLASSES = newOption("instr-classes", false,
            "This option selects whether the profiling monitor will generate a report of the " +
            "types of instructions that were executed most frequently by the program.");

    /**
     * The <code>Monitor</code> inner class contains the probes and formatting code that
     * can report the profile for the program after it has finished executing.
     */
    public class Mon implements Monitor {
        public final Simulator simulator;
        public final Program program;

        public final long[] icount;
        public final long[] itime;

        Mon(Simulator s) {
            simulator = s;
            program = s.getProgram();

            // allocate a global array for the count of each instruction
            icount = new long[program.program_end];
            // allocate a global array for the cycles of each instruction
            itime = new long[program.program_end];

            long period = PERIOD.get();
            if ( period > 0 ) {
                // insert the periodic probe
                s.insertEvent(new PeriodicProfile(period), period);
            } else if ( CYCLES.get() ) {
                // insert the count and cycles probe
                s.insertProbe(new CCProbe());
            } else {
                // insert just the count probe
                s.insertProbe(new CProbe());
            }
        }

        /**
         * The <code>PeriodicProfile</code> class can be used as a simulator event to periodically
         * sample the program counter value. This can be used to get an approximation of
         * the execution profile.
         */
        public class PeriodicProfile implements Simulator.Event {
            private final long period;

            PeriodicProfile(long p) {
                period = p;
            }

            public void fire() {
                icount[simulator.getState().getPC()]++;
                simulator.insertEvent(this, period);
            }
        }

        /**
         * The <code>CCProbe</code> class implements a probe that keeps track of the
         * execution count of each instruction as well as the number of cycles that
         * it has consumed.
         */
        public class CCProbe implements Simulator.Probe {
            protected long timeBegan;

            public void fireBefore(State state, int pc) {
                icount[pc]++;
                timeBegan = state.getCycles();
            }

            public void fireAfter(State state, int pc) {
                itime[pc] += state.getCycles() - timeBegan;
            }
        }

        /**
         * The <code>CProbe</code> class implements a simple probe that keeps a count
         * of how many times each instruction in the program has been executed.
         */
        public class CProbe extends Simulator.Probe.Empty {

            public void fireBefore(State state, int pc) {
                icount[pc]++;
            }
        }

        public void report() {

            computeTotals();
            reportProfile();

            if ( CLASSES.get() ) {
                reportInstrProfile();
            }
            Terminal.nextln();
        }

        long totalcount;
        long totalcycles;

        private void reportProfile() {
            int imax = icount.length;

            TermUtil.printSeparator("Profiling results for node "+simulator.getID());
            Terminal.printGreen("       Address     Count  Run     Cycles     Cumulative");
            Terminal.nextln();
            TermUtil.printThinSeparator(Terminal.MAXLINE);

            // report the profile for each instruction in the program
            for (int cntr = 0; cntr < imax; cntr = program.getNextPC(cntr)) {
                int start = cntr;
                int runlength = 1;
                long curcount = icount[cntr];
                long cumulcycles = itime[cntr];

                // collapse long runs of equivalent counts (e.g. basic blocks)
                int nextpc;
                for (; cntr < imax - 2; cntr = nextpc) {
                    nextpc = program.getNextPC(cntr);
                    if (nextpc >= icount.length || icount[nextpc] != curcount) break;
                    runlength++;
                    cumulcycles += itime[nextpc];
                }

                // format the results appropriately (columnar)
                String cnt = StringUtil.rightJustify(curcount, 8);
                float pcnt = computePercent(runlength*curcount, cumulcycles);
                String percent = "";
                String addr;
                if (runlength > 1) {
                    // if there is a run, adjust the count and address strings appropriately
                    addr = StringUtil.addrToString(start) + '-' + StringUtil.addrToString(cntr);
                    percent = " x" + runlength;
                } else {
                    addr = "       " + StringUtil.addrToString(start);
                }

                percent = StringUtil.leftJustify(percent, 7);

                // compute the percentage of total execution time
                if (curcount != 0) {
                    percent += StringUtil.rightJustify(cumulcycles, 8);
                    percent += " = " + StringUtil.rightJustify(StringUtil.toFixedFloat(pcnt, 4),8) + " %";
                }

                TermUtil.reportQuantity(' ' + addr, cnt, percent);
            }
        }

        private void computeTotals() {
            // compute the total cycle count
            totalcycles = StatUtil.sum(itime);
            totalcount = StatUtil.sum(icount);
        }

        private float computePercent(long count, long cycles) {
            if ( CYCLES.get() )
                return 100.0f * cycles / totalcycles;
            else
                return 100.0f * count / totalcount;
        }

        class InstrProfileEntry implements Comparable {
            String name;
            long count;
            long cycles;

            public int compareTo(Object o) {
                InstrProfileEntry other = (InstrProfileEntry)o;
                if ( this.cycles > 0 ) {
                    if ( other.cycles > this.cycles ) return 1;
                    if ( other.cycles < this.cycles ) return -1;
                } else {
                    if ( other.count > this.count ) return 1;
                    if ( other.count < this.count ) return -1;
                }

                return 0;
            }
        }

        private void reportInstrProfile() {
            List l = computeInstrProfile();

            TermUtil.printSeparator(Terminal.MAXLINE, "Profiling Results by Instruction Type");
            Terminal.printGreen(" Instruction      Count    Cycles   Percent");
            Terminal.nextln();
            TermUtil.printThinSeparator(Terminal.MAXLINE);

            Iterator i = l.iterator();
            while ( i.hasNext() ) {
                InstrProfileEntry ipe = (InstrProfileEntry)i.next();
                float pcnt = computePercent(ipe.count, ipe.cycles);
                String p = StringUtil.toFixedFloat(pcnt, 4) + " %";
                Terminal.printGreen("   "+StringUtil.rightJustify(ipe.name, 9));
                Terminal.print(": ");
                Terminal.printBrightCyan(StringUtil.rightJustify(ipe.count, 9));
                Terminal.print("  "+StringUtil.rightJustify(ipe.cycles, 8));
                Terminal.print("  "+StringUtil.rightJustify(p, 10));
                Terminal.nextln();
            }
        }

        private List computeInstrProfile() {
            HashMap cmap = new HashMap();

            for ( int cntr = 0; cntr < icount.length; cntr++ ) {
                if ( icount[cntr] == 0 ) continue;
                AbstractInstr i = program.readInstr(cntr);
                if ( i == null ) continue;
                String variant = i.getName();
                InstrProfileEntry entry = (InstrProfileEntry)cmap.get(variant);
                if  ( entry == null ) {
                    entry = new InstrProfileEntry();
                    entry.name = variant;
                    cmap.put(variant, entry);
                }
                entry.count += icount[cntr];
                entry.cycles += itime[cntr];
            }

            Enumeration e = Collections.enumeration(cmap.values());
            List l = Collections.list(e);
            Collections.sort(l);
            return l;
        }

    }

    public ProfileMonitor() {
        super("The \"profile\" monitor profiles the execution history " +
                "of every instruction in the program and generates a textual report " +
                "of the execution frequency for all instructions.");
    }

    public Monitor newMonitor(Simulator s) {
        return new Mon(s);
    }
}
