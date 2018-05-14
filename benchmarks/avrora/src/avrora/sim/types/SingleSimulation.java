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

package avrora.sim.types;

import avrora.Main;
import avrora.core.LoadableProgram;
import avrora.sim.Simulation;
import avrora.sim.clock.Synchronizer;
import avrora.sim.platform.PlatformFactory;
import cck.util.Options;
import cck.util.Util;

/**
 * The <code>SingleSimulation</code> class implements a simulation for a single node. This class
 * has its own built-in synchronizer that is designed to accept only one node. It processes command
 * line options to configure monitors and load one program onto one microcontroller and simulate it.
 *
 * @author Ben L. Titzer
 */
public class SingleSimulation extends Simulation {

    protected static final String HELP = "The \"single\" simulation type corresponds to a standard simulation " +
            "of a single microcontroller with a single program.";

    public SingleSimulation() {
        super("single", HELP, new Synchronizer.Single());

        addSection("SINGLE NODE SIMULATION OVERVIEW", help);
        addOptionSection("The most basic type of simulation, the single node simulation, is designed to " +
                "simulate a single microcontroller running a single program. Help for specific options " +
                "relating to simulating a single node is below.", options);
        MONITORS.setNewDefault("leds");
    }

    /**
     * The <code>process()</code> method processes options and arguments from the command line. This
     * implementation accepts only a single command line argument that specifies the name of the program
     * to load onto the microcontroller to simulate.
     * @param o the options extracted from the command line
     * @param args the arguments from the command line
     * @throws Exception if there was a problem loading the file or creating the simulation
     */
    public void process(Options o, String[] args) throws Exception {
        options.process(o);
        processMonitorList();

        if ( args.length == 0 )
            Util.userError("Simulation error", "No program specified");
        if ( args.length > 1 )
            Util.userError("Simulation error", "Single node simulation accepts only one program");
        Main.checkFilesExist(args);

        LoadableProgram p = new LoadableProgram(args[0]);
        p.load();
        PlatformFactory pf = getPlatform();
        createNode(pf, p);

    }
}
