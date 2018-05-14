/**
 * Created on 18. September 2004, 22:02
 *
 * Copyright (c) 2004, Olaf Landsiedel, Protocol Engineering and
 * Distributed Systems, University of Tuebingen
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
 * Neither the name of the Protocol Engineering and Distributed Systems
 * Group, the name of the University of Tuebingen nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
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

import avrora.core.SourceMapping;
import avrora.sim.Simulator;
import avrora.sim.mcu.MCUProperties;
import avrora.sim.util.SimUtil;
import cck.text.StringUtil;
import cck.text.Terminal;
import cck.util.Option;

/**
 * The <code>CallMonitor</code> class implements a monitor that is capable of tracing the call/return behavior
 * of a program while it executes.
 *
 * @author Ben L. Titzer
 */
public class CallMonitor extends MonitorFactory {

    protected final Option.Bool SITE = newOption("call-sites", true,
            "When this option is specified, the call monitor will report the address " +
            "of the instruction in the caller when a call or an interrupt " +
            "occurs.");
    protected final Option.Bool SHOW = newOption("show-stack", true,
            "When this option is specified, the call monitor trace will print the " +
            "call stack with each call, interrupt or return. When this option " +
            "is set to false, this monitor will only indent calls and returns, " +
            "without printing the entire call stack.");
    protected final Option.Bool EDGE = newOption("edge-types", true,
            "When this option is specified, the call monitor trace will print the " +
            "type of each call or return. For example, if an interrupt occurs, then " +
            "the interrupt number and name will be reported.");

    class Mon implements Monitor, CallTrace.Monitor {

        private final CallStack stack;
        private final Simulator simulator;
        private final MCUProperties props;
        private final SourceMapping sourceMap;

        private String[] shortNames;

        Mon(Simulator s) {
            simulator = s;
            sourceMap = s.getProgram().getSourceMapping();
            CallTrace trace = new CallTrace(s);
            props = simulator.getMicrocontroller().getProperties();
            trace.attachMonitor(this);
            buildInterruptNames();

            stack = new CallStack();
        }

        private void buildInterruptNames() {
            String[] longNames = new String[props.num_interrupts + 1];
            for ( int cntr = 0; cntr < props.num_interrupts; cntr++ )
                longNames[cntr] = getLongInterruptName(cntr);
            shortNames = new String[props.num_interrupts+1];
            for ( int cntr = 0; cntr < props.num_interrupts; cntr++ )
                shortNames[cntr] = getShortInterruptName(cntr);
        }

        public void report() {
            // do nothing
        }

        private void push(int callsite, int color, String edge, int inum, int target) {
            synchronized (Terminal.class ) {
                printStack(stack.getDepth(), callsite);
                if ( EDGE.get() ) {
                    Terminal.print(" --(");
                    Terminal.print(color, edge);
                    Terminal.print(")-> ");
                } else {
                    Terminal.print(" --> ");
                }
                printStackEntry(inum, target);
                Terminal.nextln();
            }
        }

        private void printStack(int depth, int callsite) {
            // print the ID and time string for this simulator
            Terminal.print(SimUtil.getIDTimeString(simulator));
            // print each stack entry
            for ( int cntr = 0; cntr < depth; cntr++ ) {
                if ( SHOW.get() ) {
                    printStackEntry(cntr);
                    if ( cntr != depth ) Terminal.print(":");
                } else {
                    Terminal.print("    ");
                }
            }
            // print the call site
            if ( SITE.get() ) {
                Terminal.print(" @ ");
                Terminal.printBrightCyan(StringUtil.addrToString(callsite));
            }
        }

        private void printStackEntry(int indx) {
            printStackEntry(stack.getInterrupt(indx), stack.getTarget(indx));
        }

        private void printStackEntry(int inum, int target) {
            if ( inum >= 0 ) Terminal.printRed(shortNames[inum]);
            Terminal.printGreen(sourceMap.getName(target));
        }

        private void pop(int callsite, String edge, int color) {
            synchronized (Terminal.class ) {
                printStack(stack.getDepth() - 1, callsite);
                if ( EDGE.get() ) {
                    Terminal.print(" <-(");
                    Terminal.print(color, edge);
                    Terminal.print(")-- ");
                } else {
                    Terminal.print(" <-- ");
                }
                printStackEntry(stack.getDepth() - 1);
                Terminal.nextln();
            }
        }

        public void fireBeforeCall(long time, int pc, int target) {
            push(pc, Terminal.COLOR_BROWN, "CALL", -1, target);
            stack.fireBeforeCall(time, pc, target);
        }

        public void fireBeforeInterrupt(long time, int pc, int inum) {
            // TODO: factor out code to compute interrupt handler start
            push(pc, Terminal.COLOR_RED, shortNames[inum], inum, (inum - 1) * 4);
            if ( inum == 1 ) stack.clear(); // clear stack on reset
            stack.fireBeforeInterrupt(time, pc, inum);
        }

        public void fireAfterReturn(long time, int pc, int retaddr) {
            pop(pc, "RET ", Terminal.COLOR_BROWN);
            stack.fireAfterReturn(time, pc, retaddr);
        }

        public void fireAfterInterruptReturn(long time, int pc, int retaddr) {
            pop(pc, "RETI", Terminal.COLOR_RED);
            stack.fireAfterInterruptReturn(time, pc, retaddr);
        }

        private String getLongInterruptName(int inum) {
            return inum == 1 ? "RESET": "#"+inum+","+props.getInterruptName(inum);
        }

        private String getShortInterruptName(int inum) {
            return inum == 1 ? "RESET" :"#"+inum+" ";
        }

    }

    /**
     * The constructor for the <code>CallMonitor</code> class simply initializes the help for this
     * class. Monitors are also help categories, so they will have an options section in their help
     * that explains each option and its use.
     */
    public CallMonitor() {
        super("The \"calls\" monitor tracks the call/return behavior of the program as it executes, " +
                "displaying the stacking up of function calls and interrupt handlers.");
    }

    /**
     * The <code>newMonitor()</code> method simply creates a new call monitor for each simulator. The
     * call monitor will print out each call, interrupt, and return during the execution of the program.
     * @param s the simulator to create a new monitor for
     * @return a new monitor that tracks the call and return behavior of the simulator as it executes
     */
    public Monitor newMonitor(Simulator s) {
        return new Mon(s);
    }
}
