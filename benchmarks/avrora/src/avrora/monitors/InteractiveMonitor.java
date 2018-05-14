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

import avrora.actions.SimAction;
import avrora.core.SourceMapping;
import avrora.sim.Simulator;
import avrora.sim.State;
import cck.util.Option;
import cck.util.Util;
import java.util.Iterator;

/**
 * The <code>InteractiveMonitor</code> class implements a monitor that allows the user to interact with
 * the simulation as it is running.
 *
 * @author Ben L. Titzer
 */
public class InteractiveMonitor extends MonitorFactory {

    protected final Option.List BREAKPOINTS = newOptionList("breakpoints", "",
            "This option selects a list of breakpoints in the program that can be either " +
            "labels (such as the start of a function) or hexadecimal addresses that begin with " +
            "\"0x\". Breakpoints will be inserted into the program and the simulation will " +
            "terminate when any of these locations are reached.");

    class Mon implements Monitor {

        Simulator simulator;
        SourceMapping sourceMap;

        Mon(Simulator s) {
            this.simulator = s;
            this.sourceMap = s.getProgram().getSourceMapping();
            Iterator i = BREAKPOINTS.get().iterator();
            while ( i.hasNext() ) {
                String str = (String)i.next();
                SourceMapping.Location l = sourceMap.getLocation(str);
                if ( l == null ) Util.userError("Label not found", str);
                simulator.insertProbe(new BreakPointProbe(), l.lma_addr);
            }
        }

        public void report() {
            // do nothing.
        }

    }

    /**
     * The constructor for the <code>CallMonitor</code> class simply initializes the help for this
     * class. Monitors are also help categories, so they will have an options section in their help
     * that explains each option and its use.
     */
    public InteractiveMonitor() {
        super("The \"interactive\" monitor allows the user to interact with the program as" +
                "it executes, including placing breakpoints, watchpoints, and inspecting the state" +
                "of the simulation. Currently, it only supports terminating the simulation at breakpoints.");
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

    public static class BreakPointProbe extends Simulator.Probe.Empty {
        public void fireBefore(State s, int pc) {
            throw new SimAction.BreakPointException(pc, s);
        }
    }
}
