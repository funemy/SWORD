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
import avrora.sim.SimulatorThread;
import avrora.sim.platform.PinConnect;
import avrora.sim.platform.PlatformFactory;
import cck.text.StringUtil;
import cck.util.*;
import java.util.Iterator;
import java.util.Random;

/**
 * The <code>WiredSimulation</code> class represents a simulation type where multiple nodes, each with a microcontroller
 * are connected together by wires. It supports options from the command line that allow a simulation to be constructed
 * with multiple nodes with multiple different programs.
 *
 * @author Jacob Everist
 */
public class WiredSimulation extends Simulation {

    public static String HELP = "The wired network simulation is used for simulating multiple nodes " +
            "simultaneously. These nodes can communicate with each other over wires.";

    public final Option.List NODECOUNT = newOptionList("nodecount", "1",
            "This option is used to specify the number of nodes to be instantiated. " +
            "The format is a list of integers, where each integer specifies the number of " +
            "nodes to instantiate with each program supplied on the command line. For example, " +
            "when set to \"1,2\" one node will be created with the first program loaded onto it, " +
            "and two nodes created with the second program loaded onto them.");
    public final Option.Interval RANDOM_START = newOption("random-start", 0, 0,
            "This option inserts a random delay before starting " +
            "each node in order to prevent artificial cycle-level synchronization. The " +
            "starting delay is pseudo-randomly chosen with uniform distribution over the " +
            "specified interval, which is measured in clock cycles. If the \"random-seed\" " +
            "option is set to a non-zero value, then its value is used as the seed to the " +
            "pseudo-random number generator.");
    public final Option.Long STAGGER_START = newOption("stagger-start", 0,
            "This option causes the simulator to insert a progressively longer delay " +
            "before starting each node in order to avoid artificial cycle-level " +
            "synchronization between nodes. The starting times are staggered by the specified number " +
            "of clock cycles. For example, if this option is given the " +
            "value X, then node 0 will start at time 0, node 1 at time 1*X, node 2 at " +
            "time 2*X, etc.");

    /**
     * The <code>WiredNode</code> class extends the <code>Node</code> class of a simulation by adding a reference to the
     * radio device as well as sensor data input. It extends the <code>instantiate()</code> method to create a new
     * thread for the node and to attach the sensor data input.
     */
    protected class WiredNode extends Node {

        long startup;

        WiredNode(int id, PlatformFactory pf, LoadableProgram p) {
            super(id, pf, p);
        }

        /**
         * The <code>instantiate()</code> method of the sensor node extends the default simulation node by creating a
         * new thread to execute the node as well as getting references to the radio and adding it to the radio model,
         * adding an optional start up delay for each node, and connecting the node's sensor input to replay or random
         * data as specified on the command line.
         */
        protected void instantiate() {
            createNode();
        }

        private void createNode() {
            thread = new SimulatorThread(this);
            super.instantiate();
            simulator.delay(startup);
        }

        /**
         * The <code>remove()</code> method removes this node from the simulation. This method extends the default
         * simulation remove method by removing the node from the radio air implementation.
         */
        protected void remove() {
            // FIXME:perhaps disconnect wires?
        }
    }

    long stagger;

    // class that connects all the nodes together via wire interconnect
    PinConnect pinConnect;

    public WiredSimulation() {
        super("wired", HELP, null);

        pinConnect = PinConnect.pinConnect;
        synchronizer = pinConnect.synchronizer;

        addSection("WIRED SIMULATION OVERVIEW", help);
        addOptionSection("This simulation type supports simulating multiple nodes that communicate " +
                "with each other over wires. There are options to specify how many of each type of node to " +
                "instantiate, as well as the program to be loaded onto each node.", options);

        PLATFORM.setNewDefault("seres");
    }

    /**
     * The <code>newNode()</code> method creates a new node in the simulation. In this implementation, a
     * <code>WiredNode</code> is created that contains, in addition to the simulator, ID, program, etc, a reference to
     * the <code>Radio</code> instance for the node and a <code>SimulatorThread</code> for the node.
     *
     * @param id the integer identifier for the node
     * @param pf the platform factory to use to instantiate the node
     * @param p  the program to load onto the node
     * @return a new instance of the <code>WiredNode</code> class for the node
     */
    public Node newNode(int id, PlatformFactory pf, LoadableProgram p) {
        return new WiredNode(id, pf, p);
    }

    /**
     * The <code>process()</code> method processes options and arguments from the command line. In this implementation,
     * this method accepts multiple programs from the command line as arguments as well as options that describe how
     * many of each type of node to instantiate.
     *
     * @param o    the options from the command line
     * @param args the arguments from the command line
     * @throws Exception if there is a problem loading any of the files or instantiating the simulation
     */
    public void process(Options o, String[] args) throws Exception {
        options.process(o);
        processMonitorList();

        if (args.length == 0) Util.userError("Simulation error", "No program specified");
        Main.checkFilesExist(args);
        PlatformFactory pf = getPlatform();

        // create the nodes based on arguments
        createNodes(args, pf);

    }

    protected void instantiateNodes() {
        super.instantiateNodes();

        pinConnect.initializeConnections();
    }

    private void createNodes(String[] args, PlatformFactory pf) throws Exception {
        int cntr = 0;
        Iterator i = NODECOUNT.get().iterator();
        while (i.hasNext()) {

            if (args.length <= cntr) break;

            String pname = args[cntr++];
            LoadableProgram lp = new LoadableProgram(pname);
            lp.load();

            // create a number of nodes with the same program
            int max = StringUtil.evaluateIntegerLiteral((String)i.next());
            for (int node = 0; node < max; node++) {
                WiredNode n = (WiredNode)createNode(pf, lp);
                long r = processRandom();
                long s = processStagger();
                n.startup = r + s;
            }
        }
    }

    long processRandom() {
        long low = RANDOM_START.getLow();
        long size = RANDOM_START.getHigh() - low;
        long delay = 0;
        if (size > 0) {
            Random r = getRandom();
            delay = r.nextLong();
            if (delay < 0) delay = -delay;
            delay = delay % size;
        }

        return (low + delay);
    }

    long processStagger() {
        long st = stagger;
        stagger += STAGGER_START.get();
        return st;
    }

}
