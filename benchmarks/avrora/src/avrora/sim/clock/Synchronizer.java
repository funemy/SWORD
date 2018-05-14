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

package avrora.sim.clock;

import avrora.sim.Simulation;
import avrora.sim.SimulatorThread;
import cck.util.Util;

/**
 * The <code>Synchronizer</code> class represents an object that controls the progress
 * of a multi-node simulation. The synchronizer preserves the timing and order of events
 * that influence other nodes' actions (e.g. communication). Since there are multiple
 * strategies for doing this, this class abstracts the actual mechanism so that clients
 * can simply create the appropriate synchronizer for their simulation.
 *
 * @author Ben L. Titzer
 */
public abstract class Synchronizer {

    /**
     * The <code>addNode()</code> method adds a node to this synchronization group.
     * This method should only be called before the <code>start()</code> method is
     * called.
     * @param n the simulator representing the node to add to this group
     */
    public abstract void addNode(Simulation.Node n);

    /**
     * The <code>removeNode()</code> method removes a node from this synchronization
     * group, and wakes any nodes that might be waiting on it.
     * @param n the simulator thread to remove from this synchronization group
     */
    public abstract void removeNode(Simulation.Node n);

    /**
     * The <code>waitForNeighbors()</code> method is called from within the execution
     * of a node when that node needs to wait for its neighbors to catch up to it
     * in execution time. The node will be blocked until the other nodes in other
     * threads catch up in global time.
     * @param time the global time to wait for all neighbors to reach
     */
    public abstract void waitForNeighbors(long time);

    /**
     * The <code>start()</code> method starts the threads executing, and the synchronizer
     * will add whatever synchronization to their execution that is necessary to preserve
     * the global timing properties of simulation.
     */
    public abstract void start();

    /**
     * The <code>join()</code> method will block the caller until all of the threads in
     * this synchronization interval have terminated, either through <code>stop()</code>
     * being called, or terminating normally such as through a timeout.
     * @throws InterruptedException if the thread was interrupted
     */
    public abstract void join() throws InterruptedException;

    /**
     * The <code>pause()</code> method temporarily pauses the simulation. The nodes are
     * not guaranteed to stop at the same global time. This method will return when all
     * threads in the simulation have been paused and will no longer make progress until
     * the <code>start()</code> method is called again.
     */
    public abstract void pause();

    /**
     * The <code>stop()</code> method will terminate all the simulation threads. It is
     * not guaranteed to stop all the simulation threads at the same global time.
     */
    public abstract void stop();


    /**
     * The <code>synch()</code> method will pause all of the nodes at the same global time.
     * This method can only be called when the simulation is paused. It will run all threads
     * forward until the global time specified and pause them.
     * @param globalTime the global time in clock cycles to run all threads ahead to
     */
    public abstract void synch(long globalTime);

    public static class Single extends Synchronizer {

        public Simulation.Node node;
        public SimulatorThread thread;

        /**
         * The <code>addNode()</code> method adds a node to this synchronization group.
         * This method should only be called before the <code>start()</code> method is
         * called.
         * @param n the simulator representing the node to add to this group
         */
        public void addNode(Simulation.Node n) {
            if ( node != null )
                throw Util.failure("Only one node supported at a time");
            node = n;
        }

        /**
         * The <code>removeNode()</code> method removes a node from this synchronization
         * group, and wakes any nodes that might be waiting on it.
         * @param n the simulator thread to remove from this synchronization group
         */
        public void removeNode(Simulation.Node n) {
            if ( node == n ) node = null;
        }

        /**
         * The <code>waitForNeighbors()</code> method is called from within the execution
         * of a node when that node needs to wait for its neighbors to catch up to it
         * in execution time. The node will be blocked until the other nodes in other
         * threads catch up in global time.
         */
        public void waitForNeighbors(long time) {
            // do nothing
        }

        /**
         * The <code>start()</code> method starts the threads executing, and the synchronizer
         * will add whatever synchronization to their execution that is necessary to preserve
         * the global timing properties of simulation.
         */
        public void start() {
            if ( node == null )
                throw Util.failure("No nodes in simulation");
            thread = new SimulatorThread(node);
            thread.start();
        }

        /**
         * The <code>join()</code> method will block the caller until all of the threads in
         * this synchronization interval have terminated, either through <code>stop()</code>
         * being called, or terminating normally such as through a timeout.
         */
        public void join() throws InterruptedException {
            thread.join();
        }

        /**
         * The <code>pause()</code> method temporarily pauses the simulation. The nodes are
         * not guaranteed to stop at the same global time. This method will return when all
         * threads in the simulation have been paused and will no longer make progress until
         * the <code>start()</code> method is called again.
         */
        public void pause() {
            throw Util.unimplemented();
        }

        /**
         * The <code>stop()</code> method will terminate all the simulation threads. It is
         * not guaranteed to stop all the simulation threads at the same global time.
         */
        public void stop() {
            throw Util.unimplemented();
        }


        /**
         * The <code>synch()</code> method will pause all of the nodes at the same global time.
         * This method can only be called when the simulation is paused. It will run all threads
         * forward until the global time specified and pause them.
         * @param globalTime the global time in clock cycles to run all threads ahead to
         */
        public void synch(long globalTime) {
            throw Util.unimplemented();
        }
    }
}
