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

package avrora.sim;

import avrora.actions.SimAction;
import avrora.sim.clock.Synchronizer;
import cck.text.StringUtil;
import cck.text.Terminal;
import cck.util.Util;


/**
 * The <code>SimulatorThread</code> class is a thread intended to run a <code>Simulator</code> in a
 * multiple-node simulation. The mapping is one-to-one: each simulator is expected to be run in its own
 * thread. Multiple simulators are then synchronized by being inserted into a group using the
 * <code>GlobalClock</code> class.
 *
 * @author Ben L. Titzer
 */
public class SimulatorThread extends Thread {

    protected final Simulation.Node node;

    /**
     * The <code>synchronizer</code> field stores a reference to the synchronizer that this thread
     * is a part of; this is needed so that when the thread finishes execution (either through
     * a timeout or error, etc), it can be removed from the simulation and the rest of the simulation
     * can continue.
     */
    protected Synchronizer synchronizer;

    /**
     * The constructor for the simulator thread accepts an instance of <code>Simulator</code> as a parameter
     * and stores it internally.
     *
     * @param n the node
     */
    public SimulatorThread(Simulation.Node n) {
        super("node-"+n.id);
        node = n;
    }

    /**
     * The <code>getNode()</code> method gets a reference to the <code>Simulation.Node</code> that this
     * simulator thread is simulating.
     * @return a reference to the node which this thread is simulating
     */
    public Simulation.Node getNode() {
        return node;
    }

    /**
     * The <code>getSimulator()</code> method gets the <code>Simulator</code> instance that this thread is
     * bound to.
     *
     * @return the instance of <code>Simulator</code> this thread is intended to run.
     */
    public Simulator getSimulator() {
        return node.getSimulator();
    }

    /**
     * The <code>run()</code> method begins the simulation, calling the <code>start()</code> method of the
     * <code>Simulator</code> instance associated with this thread.
     */
    public void run() {
        try {
            Simulator simulator = node.getSimulator();
            simulator.start();
        } catch (SimAction.TimeoutException te) {
            // suppress timeout exceptions.
        } catch (SimAction.BreakPointException e) {
            Terminal.printYellow("Simulation terminated");
            Terminal.println(": breakpoint at " + StringUtil.addrToString(e.address) + " reached.");
        } catch (Util.Error e) {
            e.report();
        } finally {
            if ( synchronizer != null )
                synchronizer.removeNode(node);
        }
    }

    /**
     * The <code>setSynchronizer()</code> method sets the synchronizer for this thread.
     * @param s the synchronizer for this node
     */
    public void setSynchronizer(Synchronizer s) {
        synchronizer = s;
    }
}
