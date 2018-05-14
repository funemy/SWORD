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
import cck.help.HelpCategory;
import cck.util.Options;

/**
 * The <code>MonitorFactory</code> class represents a profiling utility that is able to produce a
 * <code>Monitor</code> for a simulation. The monitor can use probes, watches, and events to monitor the
 * execution of the program and issue a report after the simulation is completed.
 *
 * @author Ben L. Titzer
 */
public abstract class MonitorFactory extends HelpCategory {

    /**
     * The constructor for the <code>MonitorFactory</code> class initializes the <code>options</code> field,
     * as well as the references to the help string and the short name of the monitor.
     *
     * @param h  the help item for the monitor as a string
     */
    protected MonitorFactory(String h) {
        super("monitor", h);

        addSection("MONITOR OVERVIEW", help);
        addOptionSection("Help for the options accepted by this monitor is below.", options);
    }


    /**
     * The <code>newMonitor()</code> method creates a new monitor for the specified instance of
     * <code>Simulator</code>. The resulting monitor may insert probes, watches, or events into the simulation
     * to collect information and later report that information after the simulation is complete.
     *
     * @param s the <code>Simulator</code> instance to create the monitor for
     * @return an instance of the <code>Monitor</code> interface that represents the monitor for the
     *         simulator
     */
    public abstract Monitor newMonitor(Simulator s);

    /**
     * The <code>processOptions()</code> method is called after the <code> MonitorFactory</code> instance is
     * created. These options are the options left over from processing the command line, extracting the
     * parameters to the main program, extracting the parameters from the action.
     *
     * @param o the options representing the known and unknown options from the command line
     */
    public void processOptions(Options o) {
        options.process(o);
    }
}
