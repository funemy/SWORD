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
import avrora.core.*;
import avrora.sim.Simulator;
import avrora.sim.State;
import avrora.sim.output.SimPrinter;
import avrora.sim.util.SimUtil;
import cck.text.*;
import cck.util.Option;
import cck.util.Util;
import java.util.Iterator;

/**
 * The <code>ProfileMonitor</code> class represents a monitor that can collect profiling information such as
 * counts and branchcounts about the program as it executes.
 *
 * @author Ben L. Titzer
 */
public class TraceMonitor extends MonitorFactory {

    final Option.List FROMTO = newOptionList("trace-from", "",
            "The \"trace-from\" option specifies the list of program point pairs for which " +
            "to enable the tracing. The tracing will be enabled when the first point is entered " +
            "and be disabled when the second point is reached. Nesting of multiple point pairs " +
            "is handled correctly.");
    final Option.Long TIME = newOption("trace-start", 0,
            "The \"trace-start\" option specifies the time to start the instruction trace, in " +
            "clock cycles. This option can be useful for diagnosing problems in long simulations " +
            "that happens after a given time is reached.");

    /**
     * The <code>Monitor</code> class implements the monitor for the profiler. It contains a
     * <code>ProgramProfiler</code> instance which is a probe that is executed after every instruction that
     * collects execution counts for every instruction in the program.
     */
    public class Mon implements Monitor {
        public final Simulator simulator;
        public final SimPrinter printer;
        public final Program program;
        public final GlobalProbe PROBE;
        public int count;
        int nesting;

        public class GlobalProbe implements Simulator.Probe {
            public void fireBefore(State s, int addr) {
                print(s, s.getInstr(addr));
            }

            public void fireAfter(State s, int addr) {
                count++;
            }
        }

        public class StartProbe extends Simulator.Probe.Empty {
            int start, end;
            int traceNum;
            String pair;

            StartProbe(int s, int e) {
                start = s;
                end = e;
                pair = StringUtil.addrToString(s)+":"+ StringUtil.addrToString(e);
            }

            public void fireBefore(State s, int addr) {
                traceNum++;
                if ( nesting == 0 ) {
                    print("trace ("+pair+") begin: "+traceNum+" --------------------------");
                    print(s, s.getInstr(addr));
                    simulator.insertProbe(PROBE);
                } else {
                    print("nested ("+pair+") begin: "+traceNum+" --------------------------");
                }
                nesting++;
            }
        }

        public class StartEvent implements Simulator.Event {
            public void fire() {
                simulator.insertProbe(PROBE);
            }
        }

        public class EndProbe extends Simulator.Probe.Empty {
            int start, end;
            String pair;

            EndProbe(int s, int e) {
                start = s;
                end = e;
                pair = StringUtil.addrToString(s)+":"+StringUtil.addrToString(e);
            }

            public void fireAfter(State s, int addr) {
                nesting--;
                if ( nesting == 0 ) {
                    print("trace ("+pair+") end --------------------------");
                    simulator.removeProbe(PROBE);
                } else {
                    print("nested ("+pair+") end --------------------------");

                }
            }
        }

        int nextpc;

        private void print(State s, AbstractInstr i) {
            //"#k{%x}: #k{%s} %s", color, pc, color, i.getVariant(), i.getOperands()

            StringBuffer buf = printer.getBuffer(100);
            int pc = s.getPC();
            int color = pc == nextpc ? Terminal.COLOR_BLUE : Terminal.COLOR_CYAN;
            Terminal.append(color, buf, StringUtil.to0xHex(pc, 4));
            buf.append(": ");
            buf.append(i.toString());
            printer.printBuffer(buf);
            nextpc = pc + i.getSize();
        }

        private void print(String s) {
            printer.println(s);
        }

        Mon(Simulator s) {
            simulator = s;
            printer = s.getPrinter();
            program = s.getProgram();
            PROBE = new GlobalProbe();
            long time = TIME.get();
            if ( time > 0 ) {
                // if start time is specified, add an event to start the global probe
                s.insertEvent(new StartEvent(), time);
            } else if ( FROMTO.get().isEmpty() ){
                // if there are no fromt/to pairs, insert the global probe
                s.insertProbe(PROBE);
            } else {
                // if there are from/to pairs, insert the start and end probes
                addPairs();
            }
        }

        private void addPairs() {
            Iterator i = FROMTO.get().iterator();
            while (i.hasNext()) {
                String str = (String)i.next();
                int ind = str.indexOf(':');
                if (ind <= 0)
                    throw Util.failure("invalid address format: " + StringUtil.quote(str));
                String src = str.substring(0, ind);
                String dst = str.substring(ind + 1);

                SourceMapping.Location loc = getLocation(src);
                SourceMapping.Location tar = getLocation(dst);

                addPair(loc.lma_addr, tar.lma_addr);
            }
        }

        private SourceMapping.Location getLocation(String src) {
            SourceMapping lm = program.getSourceMapping();
            SourceMapping.Location loc = lm.getLocation(src);
            if ( loc == null )
                Util.userError("Invalid program address: ", src);
            if ( program.readInstr(loc.lma_addr) == null )
                Util.userError("Invalid program address: ", src);
            return loc;
        }

        private void addPair(int start, int end) {
            if ( program.readInstr(start) == null ) return;
            if ( program.readInstr(end) == null ) return;

            simulator.insertProbe(new StartProbe(start, end), start);
            simulator.insertProbe(new EndProbe(start, end), end);
        }

        /**
         * The <code>report()</code> method generates a textual report for the profiling information gathered
         * from the execution of the program. The result is a table of performance information giving the
         * number of executions of each instruction, compressed for basic blocks.
         */
        public void report() {
            TermUtil.printSeparator("Trace results for node "+simulator.getID());
            long cycles = simulator.getClock().getCount();
            float ipc = count / (float)cycles;
            TermUtil.reportQuantity("Instructions executed", count, "");
            TermUtil.reportQuantity("Program throughput", ipc, "instrs/cycle");
            TermUtil.reportQuantity("Program throughput", ipc * simulator.getClock().getHZ() / 1000000, "mips");
            Terminal.nextln();
        }
    }

    /**
     * The constructor for the <code>ProfileMonitor</code> class creates a factory that is capable of
     * producing profile monitors for each simulator passed.
     */
    public TraceMonitor() {
        super("The \"trace\" monitor traces the execution of the entire program " +
                "by printing every instruction as it executes. ");
    }

    /**
     * The <code>newMonitor()</code> method creates a new monitor for the given simulator that is capable of
     * collecting performance information as the program executes.
     *
     * @param s the simulator to create the monitor for
     * @return an instance of the <code>Monitor</code> interface that tracks performance information from the
     *         program
     */
    public Monitor newMonitor(Simulator s) {
        return new Mon(s);
    }
}
