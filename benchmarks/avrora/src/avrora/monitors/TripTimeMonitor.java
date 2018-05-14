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
import avrora.sim.State;
import cck.stat.Distribution;
import cck.text.*;
import cck.util.Option;
import cck.util.Util;
import java.util.Iterator;

/**
 * The <code>TripTimeMonitor</code> class implements a monitor that tracks the time from
 * executing instruction A in the program until the program reaches instruction B. For
 * example, if A is the beginning of an interrupt handler and B is the end of an interrupt
 * handler, then the monitor will record the time it takes to execute the entire interrupt
 * handler. For each pair of points A and B, it collects statistics about each "trip"
 * between the two points, reporting the results at the end of execution. 
 *
 * @author Ben L. Titzer
 */
public class TripTimeMonitor extends MonitorFactory {

    final Option.List PAIRS = newOptionList("pairs", "",
            "The \"pairs\" option specifies the list of program point pairs for which " +
            "to measure the point-to-point trip time. ");
    final Option.List FROM = newOptionList("from", "",
            "The \"from\" option specifies the list of program points for which " +
            "to measure to every other instruction in the program. ");
    final Option.List TO = newOptionList("to", "",
            "The \"from\" option specifies the list of program points for which " +
            "to measure from every other instruction in the program. ");
    final Option.Bool DISTRIBUTION = newOption("distribution", false,
            "This option, when specified, causes the trip time monitor to print a complete distribution of the " +
            "trip times for each pair of program points. WARNING: this option can consume large amounts of memory " +
            "and generate a large amount of output.");

    public TripTimeMonitor() {
        super("The \"trip-time\" monitor records profiling " +
                "information about the program that consists of the time it takes " +
                "(on average) to reach one point from another point in the program.");
    }

    protected class PointToPointMon implements Monitor {

        class Pair {
            final int start;
            final int end;
            long cumul;
            long cumul_sqr;
            int count;
            long max;
            long min;

            Pair startLink;
            Pair endLink;

            Distribution distrib;


            Pair(int start, int end) {
                this.start = start;
                this.end = end;

                cumul = 0;
                cumul_sqr = 0;
                max = 0;
                min = Long.MAX_VALUE;
                if ( DISTRIBUTION.get() )
                    distrib = new Distribution("trip time "
                            +StringUtil.addrToString(start)+" -to- "
                            +StringUtil.addrToString(end), "Trips", "Total Time", "Distribution");
            }

            void record(long time) {
                if ( distrib != null ) {
                    distrib.record((int)time);
                } else {
                    cumul += time;
                    cumul_sqr += (time * time);
                    max = Math.max(max, time);
                    min = Math.min(min, time);
                }
                count++;
            }

            void report(Printer printer) {
                if ( distrib == null ) {
                float avg = (float)cumul / count;
                double std = Math.sqrt(((double)cumul_sqr / count) - (avg * avg));
                Terminal.println("  "+ StringUtil.addrToString(start)+"  "
                        +StringUtil.addrToString(end)+"  "
                        +StringUtil.rightJustify(count, 8)+"  "
                        +StringUtil.rightJustify(avg, 10)+"  "
                        +StringUtil.rightJustify((float)std, 10)+"  "
                        +StringUtil.rightJustify((float)max, 9)+"  "
                        +StringUtil.rightJustify((float)min, 9)
                        );
                } else {
                    distrib.process();
                    distrib.print(printer);
                }
            }
        }

        final Pair[] startArray;
        final Pair[] endArray;
        final long[] lastEnter;

        final Simulator simulator;
        final Program program;
        final PTPProbe PROBE;

        PointToPointMon(Simulator s) {
            simulator = s;
            program = s.getProgram();
            int psize = program.program_end;
            startArray = new Pair[psize];
            endArray = new Pair[psize];
            lastEnter = new long[psize];
            PROBE = new PTPProbe();

            addPairs();
            addFrom();
            addTo();
        }

        private void addPairs() {
            Iterator i = PAIRS.get().iterator();
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

        private void addFrom() {
            Iterator i = FROM.get().iterator();
            SourceMapping sm = program.getSourceMapping();
            while (i.hasNext()) {
                String str = (String)i.next();
                SourceMapping.Location loc = sm.getLocation(str);
                for ( int cntr = 0; cntr < program.program_end; cntr = program.getNextPC(cntr) )
                    addPair(loc.lma_addr, cntr);
            }
        }

        private void addTo() {
            Iterator i = TO.get().iterator();
            SourceMapping sm = program.getSourceMapping();
            while (i.hasNext()) {
                String str = (String)i.next();
                SourceMapping.Location loc = sm.getLocation(str);
                for ( int cntr = 0; cntr < program.program_end; cntr = program.getNextPC(cntr) )
                    addPair(cntr, loc.lma_addr);
            }
        }

        void addPair(int start, int end) {

            if ( program.readInstr(start) == null ) return;
            if ( program.readInstr(end) == null ) return;

            Pair p = new Pair(start, end);

            if (startArray[p.start] == null && endArray[p.start] == null)
                simulator.insertProbe(PROBE, p.start);

            p.startLink = startArray[p.start];
            startArray[p.start] = p;

            if (startArray[p.end] == null && endArray[p.end] == null)
                simulator.insertProbe(PROBE, p.end);


            p.endLink = endArray[p.end];
            endArray[p.end] = p;

        }

        protected class PTPProbe extends Simulator.Probe.Empty {
            public void fireBefore(State state, int pc) {
                long time = state.getCycles();

                for ( Pair p = endArray[pc]; p != null; p = p.startLink ) {
                    if ( lastEnter[p.start] < 0 ) continue;
                    p.record(time - lastEnter[p.start]);
                }

                lastEnter[pc] = time;
            }
        }

        public void report() {
            TermUtil.printSeparator("Trip time results for node "+simulator.getID());
            Terminal.printGreen("  start      end     count         avg         std        max        min");
            Terminal.nextln();
            TermUtil.printThinSeparator(Terminal.MAXLINE);
            for ( int cntr = 0; cntr < lastEnter.length; cntr++ )
                for ( Pair p = startArray[cntr]; p != null; p = p.startLink ) {
                    if ( p.count > 0 ) p.report(Printer.STDOUT);
                }
            Terminal.nextln();
        }
    }

    public Monitor newMonitor(Simulator s) {
        return new PointToPointMon(s);
    }
}
