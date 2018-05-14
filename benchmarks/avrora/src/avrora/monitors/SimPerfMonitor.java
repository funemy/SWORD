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

import avrora.sim.Simulator;
import cck.stat.TimeSequence;
import cck.text.*;
import cck.util.Option;

/**
 * This monitor measures the instantaneous performance of the simulator by inserting
 * events into the queue at regular intervals and recording the elapsed (wall clock)
 * time used for simulation.
 *
 * NOTE: This monitor is only meant for a single node simulation!
 * @author Ben L. Titzer
 */
public class SimPerfMonitor extends MonitorFactory {

    public final Option.Double FREQUENCY = newOption("frequency", 100.0,
            "This option is used in the simulator profiling monitor to determine how many " +
            "times per simulated second to sample the simulator's performance.");

    public class Mon implements Monitor {
        final Simulator simulator;
        final TimeSequence data;
        long start;
        long interval;

        Mon(Simulator s) {
            simulator = s;
            data = new TimeSequence();
            start = System.currentTimeMillis();
            interval = (long)(simulator.getClock().getHZ() / FREQUENCY.get());
            simulator.insertEvent(new Event(), interval);
        }

        public void report() {

            TermUtil.printSeparator("Simulator performance results for node "+simulator.getID());
            Terminal.printGreen("  Time    Millis  Instant     Cumulative");
            Terminal.nextln();
            TermUtil.printSeparator();

            TimeSequence.Measurement m = new TimeSequence.Measurement();
            TimeSequence.Iterator i = data.iterator(0);
            int cntr = 0;
            long pcycles = 0;
            long pmillis = 0;
            while ( i.hasNext() ) {
                i.next(m);
                long icycle; // cycles for this interval
                long imilli; // milliseconds for this interval
                long cycles = m.time;
                int millis = m.value;

                if ( cntr == 0 ) {
                    icycle = cycles;
                    imilli = millis;
                }
                else {
                    icycle = cycles-pcycles;
                    imilli = millis-pmillis;
                }

                float cumul = (float)cycles / millis / 1000;
                float inst = (float)icycle / imilli / 1000;
                String mstr = StringUtil.leftJustify((float)millis / 1000, 6);
                String ccstr = StringUtil.leftJustify(imilli, 6);
                String cstr = StringUtil.leftJustify(cumul, 12);
                String istr = StringUtil.leftJustify(inst, 12);
                Terminal.println("  "+mstr+"  "+ccstr+"  "+istr+""+cstr);
                cntr++;
                pmillis = millis;
                pcycles = cycles;
            }
            Terminal.nextln();
        }

        class Event implements Simulator.Event {
            public void fire() {
                long time = simulator.getState().getCycles();
                long millis = System.currentTimeMillis() - start;
                data.add(time, (int)millis);
                simulator.insertEvent(this, interval);
            }
        }
    }

    /**
     * The constructor for the <code>ProfileMonitor</code> class creates a factory that is capable of
     * producing profile monitors for each simulator passed.
     */
    public SimPerfMonitor() {
        super("The \"simperf\" monitor profiles the performance of the " +
                "simulator itself by periodically recording the cycles executed and total " +
                "time consumed by simulation and generates a report.");
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
