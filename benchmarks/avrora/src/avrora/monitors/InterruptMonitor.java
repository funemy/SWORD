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

import avrora.sim.*;
import avrora.sim.output.SimPrinter;
import avrora.sim.mcu.MCUProperties;
import avrora.sim.util.SimUtil;
import cck.stat.MinMaxMean;
import cck.text.*;
import cck.util.Option;
import java.util.Arrays;

/**
 * The <code>InterruptMonitor</code> class implements a monitor that tracks the interrupts
 * that happen during a simulation. It collects statistics about which interrupts occur, how often,
 * and the latency between the interrupt being posted and being handled. After execution terminates,
 * it generates a textual report to the user.
 *
 * @author Ben L. Titzer
 */
public class InterruptMonitor extends MonitorFactory {

    protected final Option.Bool SHOW = newOption("show-interrupts", false,
            "This option, when specified, will cause the interrupt monitor to bring out changes " +
            "to the state of each interrupt.");
    protected final Option.Bool INV_ONLY = newOption("invocations-only", true,
            "This option, when specified, will cause the interrupt to print only invocations " +
            "of the specified interrupts, and not enablings, disablings, postings, and unpostings.");

    class Mon implements Monitor, Simulator.InterruptProbe {

        final MCUProperties props;
        final Simulator simulator;
        final InterruptTable interrupts;
        final SimPrinter printer;
        final long[] invocations;
        final long[] lastInvoke;
        final long[] lastPost;
        final MinMaxMean[] meanInvoke;
        final MinMaxMean[] meanLatency;
        final MinMaxMean[] meanWake;
        boolean show;
        boolean invokeOnly;

        Mon(Simulator s) {
            simulator = s;
            printer = s.getPrinter();
            props = simulator.getMicrocontroller().getProperties();
            InterruptTable interruptTable = simulator.getInterpreter().getInterruptTable();
            interruptTable.insertProbe(this);
            interrupts = interruptTable;
            int numInts = interrupts.getNumberOfInterrupts();
            invocations = new long[numInts];
            lastInvoke = new long[numInts];
            lastPost = new long[numInts];
            meanInvoke = new MinMaxMean[numInts];
            meanLatency = new MinMaxMean[numInts];
            meanWake = new MinMaxMean[numInts];
            Arrays.fill(lastInvoke, -1);
            Arrays.fill(lastPost, -1);
            for ( int cntr = 0; cntr < numInts; cntr++ ) {
                meanInvoke[cntr] = new MinMaxMean("Inter-arrival time");
                meanLatency[cntr] = new MinMaxMean("Latency");
                meanWake[cntr] = new MinMaxMean("Wakeup time");
            }
            show = SHOW.get();
            invokeOnly = INV_ONLY.get();
        }

        private void print(String s, int inum) {

            StringBuffer buf = printer.getBuffer();
            Terminal.append(Terminal.COLOR_GREEN, buf, s);
            if ( inum > 0) {
                buf.append(": ");
                Terminal.append(Terminal.COLOR_BRIGHT_CYAN, buf, "#"+inum+" ("+props.getInterruptName(inum)+")");
            }
            printer.printBuffer(buf);
        }

        /**
         * The <code>fireBeforeInvoke()</code> method of an interrupt probe will be called by the
         * simulator before control is transferred to this interrupt, before the microcontroller
         * has been woken from its current sleep mode.
         * @param s the state of the simulator
         * @param inum the number of the interrupt being entered
         */
        public void fireBeforeInvoke(State s, int inum) {
            if ( show ) {
                print("invoke interrupt", inum);
            }
            invocations[inum]++;
            long time = s.getCycles();
            if ( lastInvoke[inum] > 0 ) {
                meanInvoke[inum].record((int)(time - lastInvoke[inum]));
            }
            if ( lastPost[inum] > 0 && lastPost[inum] > lastInvoke[inum]) {
                meanLatency[inum].record((int)(time - lastPost[inum]));
            }
            lastInvoke[inum] = time;
        }

        /**
         * The <code>fireAfterInvoke()</code> method of an interrupt probe will be called by the
         * simulator after control is transferred to this interrupt handler, i.e. after the current
         * PC is pushed onto the stack, interrupts are disabled, and the current PC is set to
         * the start of the interrupt handler.
         * @param s the state of the simulator
         * @param inum the number of the interrupt being entered
         */
        public void fireAfterInvoke(State s, int inum) {
            long time = s.getCycles();
            if ( lastInvoke[inum] > 0 ) {
                meanWake[inum].record((int)(time - lastInvoke[inum]));
            }
        }

        /**
         * The <code>fireWhenDisabled()</code> method of an interrupt probe will be called by the
         * simulator when the interrupt is masked out (disabled) by the program.
         * @param s the state of the simulator
         * @param inum the number of the interrupt being masked out
         */
        public void fireWhenDisabled(State s, int inum) {
            if (show && !invokeOnly) {
                if ( inum != 0 )
                    print("disable interrupt", inum);
                else
                    print("disable interrupts", inum);
            }
        }

        /**
         * The <code>fireWhenEnabled()</code> method of an interrupt probe will be called by the
         * simulator when the interrupt is unmasked (enabled) by the program.
         * @param s the state of the simulator
         * @param inum the number of the interrupt being unmasked
         */
        public void fireWhenEnabled(State s, int inum) {
            if (show && !invokeOnly) {
                if ( inum != 0 )
                    print("enable interrupt", inum);
                else
                    print("enable interrupts", inum);
            }
        }

        /**
         * The <code>fireWhenPosted()</code> method of an interrupt probe will be called by the
         * simulator when the interrupt is posted. When an interrupt is posted to the simulator,
         * it will be coming pending if it is enabled (unmasked) and eventually be handled.
         * @param s the state of the simulator
         * @param inum the number of the interrupt being posted
         */
        public void fireWhenPosted(State s, int inum) {
            if (show && !invokeOnly) {
                print("post interrupt", inum);
            }
            lastPost[inum] = s.getCycles();
        }

        /**
         * The <code>fireWhenUnposted()</code> method of an interrupt probe will be called by the
         * simulator when the interrupt is unposted. This can happen if the software resets the
         * flag bit of the corresponding IO register or, for most interrupts, when the pending
         * interrupt is handled.
         * @param s the state of the simulator
         * @param inum the number of the interrupt being unposted
         */
        public void fireWhenUnposted(State s, int inum) {
            if (show && !invokeOnly) {
                print("unpost interrupt", inum);
            }
        }

        public void report() {
            TermUtil.printSeparator("Interrupt monitor results for node "+simulator.getID());
            Terminal.printGreen("Num  Name        Invocations  Separation  Latency     Wakeup");
            Terminal.nextln();
            TermUtil.printThinSeparator(Terminal.MAXLINE);
            for ( int cntr = 1; cntr < invocations.length; cntr++ ) {
                meanInvoke[cntr].process();
                meanLatency[cntr].process();
                Terminal.printBrightCyan(StringUtil.rightJustify(cntr, 3));
                Terminal.print("  ");
                Terminal.printGreen(StringUtil.leftJustify(props.getInterruptName(cntr), 15));
                Terminal.printBrightCyan(StringUtil.rightJustify(invocations[cntr], 8));
                Terminal.print("  ");
                Terminal.print(invocationSeparation(cntr));
                Terminal.print("  ");
                Terminal.print(invocationLatency(cntr));
                Terminal.print("  ");
                Terminal.print(invocationWakeup(cntr));
                Terminal.nextln();
            }
            Terminal.nextln();
        }

        private String invocationLatency(int cntr) {
            if ( invocations[cntr] > 0 )
                return StringUtil.leftJustify(meanLatency[cntr].mean, 10);
            else
                return StringUtil.space(10);
        }

        private String invocationSeparation(int cntr) {
            if ( invocations[cntr] > 0 )
                return StringUtil.leftJustify(meanInvoke[cntr].mean, 10);
            else
                return StringUtil.space(10);
        }

        private String invocationWakeup(int cntr) {
            if ( invocations[cntr] > 0 )
                return StringUtil.leftJustify(meanWake[cntr].mean, 10);
            else
                return StringUtil.space(10);
        }
    }

    public InterruptMonitor() {
        super("The interrupt monitor tracks changes to the state of interrupts, including " +
                "posting, enabling, and invoking of interrupts.");
    }

    /**
     * The <code>newMonitor()</code> method creates a new interrupt monitor for the specified simulator that
     * will collect statistics about the interrupts executed during simulation.
     * @param s the simulator to create the monitor for
     * @return a new monitor instance for the specified simulator
     */
    public Monitor newMonitor(Simulator s) {
        return new Mon(s);
    }
}
