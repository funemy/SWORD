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

import avrora.sim.*;
import cck.util.Util;
import java.util.HashMap;
import java.util.Iterator;

/**
 * The <code>BarrierSynchronizer</code> class implements a global timer among multiple simulators by inserting
 * periodic events into the queue of each simulator.
 *
 * @author Ben L. Titzer, Daniel Lee
 */
public class BarrierSynchronizer extends Synchronizer {

    /**
     * <code>period</code> is the number of cycles on a member local clock per cycle on the global clock. Some
     * re-coding must be done if microcontrollers running at difference speeds are to be accurately
     * simulated.
     */
    protected long period;
    protected final HashMap threadMap;
    protected final Simulator.Event action;

    protected final Object condition;
    protected int goal;
    protected int meet_count;
    protected int wait_count;

    protected WaitSlot waitSlotList;

    /**
     * The constructor for the <code>IntervalSynchronizer</code> class creates a new synchronizer
     * with the specified period, that will fire the specified event each time all threads meet at
     * a synchronization point.
     * @param p the period in clock cycles which to synchronize the threads
     * @param a the event to fire each time all threads meet at a synchronization point
     */
    public BarrierSynchronizer(long p, Simulator.Event a) {
        period = p;
        action = a;
        threadMap = new HashMap();
        condition = new Object();
    }

    /**
     * The <code>SynchEvent</code> class represents an event that is inserted into the event
     * queue of each simulator at the same global time. When this event fires, it will stop the thread
     * running this simulator by waiting on a shared
     * condition variable. The last thread to fire the event will then notify the condition variable
     * which frees the other threads to run again in parallel.
     */
    protected class SynchEvent implements Simulator.Event {

        protected final SimulatorThread thread;
        protected final MainClock clock;
        protected boolean removed;
        protected boolean met;
        protected WaitSlot waitSlot;

        protected SynchEvent(SimulatorThread t) {
            thread = t;
            clock = t.getSimulator().getClock();
        }

        /**
         * The <code>fire()</code> method of this event is called by the individual event queues of each
         * simulator as they reach this point in time. The implementation of this method waits for all threads
         * to join.
         */
        public void fire() {
            try {
                synchronized (condition) {
                    // if we have been removed since the last synchronization, return!
                    if ( removed ) return;

                    met = true;

                    // increment the count of the number of threads that have entered
                    meet_count++;

                    if ( !signalOthers() )
                        condition.wait();

                    met = false;
                }
                // if we have been removed since the last synchronization, don't insert synch event
                if ( removed ) return;

                // we have not been removed, we can reinsert the synch event
                clock.insertEvent(this, period);
            } catch (InterruptedException e) {
                throw Util.unexpected(e);
            }
        }

    }

    /**
     * The <code>signalOthers()</code> method is used to check whether the thread that has just arrived
     * should signal other threads to continue.
     * @return true if this thread signalled the others to continue; false if this thread should stop
     * and wait for the other threads before continuing
     */
    protected boolean signalOthers() {

        // check for any waiters that need to be woken
        checkWaiters();

        // have we reached the goal?
        if (meet_count < goal) {
            return false;
        } else {
            // last thread to arrive sets the count to zero and notifies all other threads
            meet_count = 0;
            wait_count = 0;
            // perform the action that should be run while all threads are stopped (serial)
            action.fire();
            // release threads
            condition.notifyAll();
            return true;
        }
    }

    /**
     * The <code>start()</code> method starts the threads executing, and the synchronizer
     * will add whatever synchronization to their execution that is necessary to preserve
     * the global timing properties of simulation.
     */
    public synchronized void start() {
        Iterator threadIterator = threadMap.keySet().iterator();
        while (threadIterator.hasNext()) {
            SimulatorThread thread = (SimulatorThread)threadIterator.next();
            thread.start();
        }
    }

    /**
     * The <code>join()</code> method will block the caller until all of the threads in
     * this synchronization interval have terminated, either through <code>stop()</code>
     * being called, or terminating normally such as through a timeout.
     */
    public void join() throws InterruptedException {
        Iterator threadIterator = threadMap.keySet().iterator();
        while (threadIterator.hasNext()) {
            SimulatorThread thread = (SimulatorThread)threadIterator.next();
            thread.join();
        }
    }

    /**
     * The <code>stop()</code> method will terminate all the simulation threads. It is
     * not guaranteed to stop all the simulation threads at the same global time.
     */
    public synchronized void stop() {
        Iterator threadIterator = threadMap.keySet().iterator();
        while (threadIterator.hasNext()) {
            SimulatorThread thread = (SimulatorThread)threadIterator.next();
            thread.getSimulator().stop();
        }
    }

    /**
     * The <code>pause()</code> method temporarily pauses the simulation. The nodes are
     * not guaranteed to stop at the same global time. This method will return when all
     * threads in the simulation have been paused and will no longer make progress until
     * the <code>start()</code> method is called again.
     */
    public synchronized void pause() {
        throw Util.unimplemented();
    }

    /**
     * The <code>synch()</code> method will pause all of the nodes at the same global time.
     * This method can only be called when the simulation is paused. It will run all threads
     * forward until the global time specified and pause them.
     * @param globalTime the global time in clock cycles to run all threads ahead to
     */
    public synchronized void synch(long globalTime) {
        throw Util.unimplemented();
    }

    /**
     * The <code>addNode()</code> method adds a node to this synchronization group.
     * This method should only be called before the <code>start()</code> method is
     * called.
     * @param t the simulator representing the node to add to this group
     */
    public synchronized void addNode(Simulation.Node t) {
        // if we already have this thread, do nothing
        SimulatorThread st = t.getThread();
        if (threadMap.containsKey(st)) return;

        st.setSynchronizer(this);

        // create a new synchronization event for this thread's queue
        SynchEvent event = new SynchEvent(st);
        threadMap.put(st, event);
        // insert the synch event in the thread's queue
        event.clock.insertEvent(event, period);
        goal++;
    }

    /**
     * The <code>removeNode()</code> method removes a node from this synchronization
     * group, and wakes any nodes that might be waiting on it.
     * @param t the simulator thread to remove from this synchronization group
     */
    public synchronized void removeNode(Simulation.Node t) {
        // don't try to remove a thread that's not here!
        SimulatorThread st = t.getThread();
        if ( !threadMap.containsKey(st) ) return;
        synchronized ( condition ) {
            SynchEvent e = (SynchEvent)threadMap.get(st);
            e.removed = true; // just in case the thread is still running, don't let it synch
            if ( e.met ) meet_count--;

            if ( stillWaiting(e.waitSlot) ) {
                // if this wait slot hasn't happened yet, we need to decrement wait_count
                // and to decrement the number of waiters in that slot
                e.waitSlot.numWaiters--;
                wait_count--;
            }
            threadMap.remove(e);
            goal--;
            // signal any other threads (and wake waiters as necessary) but don't wait
            signalOthers();
        }
    }

    /**
     * The <code>waitForNeighbors()</code> method is called from within the execution
     * of a node when that node needs to wait for its neighbors to catch up to it
     * in execution time. The node will be blocked until the other nodes in other
     * threads catch up in global time.
     */
    public void waitForNeighbors(long time) {

        // get the current simulator thread
        SimulatorThread thread = (SimulatorThread)Thread.currentThread();
        SynchEvent event = (SynchEvent)threadMap.get(thread);
        // if the current thread is not in the synchronizer, do nothing
        if ( event == null ) return;

        WaitSlot w;
        synchronized ( condition ) {
            // allocate a wait slot for this thread
            w = insertWaiter(event, time);
            // check for other waiters and wake them if necessary
            WaitSlot h = checkWaiters();
            // if we were at the head and just woken up, we can just return
            if ( w == h ) return;
        }

        // falling through means that we are either not at the head
        // or that not all threads have performed a meet or a wait
        try {
            // we must grab the lock for this wait slot
            synchronized ( w ) {
                 // check for intervening wakeup between dropping global lock and taking local lock
                if ( w.shouldWait )
                    w.wait();
            }
        } catch ( InterruptedException e) {
            throw Util.unexpected(e);
        }
    }

    /**
     * The <code>WaitSlot</code> class represents a slot in time where multiple threads are waiting
     * for others to catch up.
     */
    static class WaitSlot {
        final long time;
        int numWaiters;
        WaitSlot next;
        boolean shouldWait;

        WaitSlot(long t) {
            shouldWait = true;
            time = t;
        }
    }

    protected WaitSlot insertWaiter(SynchEvent event, long time) {
        // get a wait slot for this waiter
        WaitSlot w = getWaitSlot(time);

        // now this thread is officially waiting
        wait_count++;
        // remember the wait slot this waiter is in
        event.waitSlot = w;
        // increment the number of waiters in this slot
        w.numWaiters++;

        return w;
    }

    private WaitSlot getWaitSlot(long time) {
        WaitSlot prev = waitSlotList;
        // search through the wait list from front to back
        for ( WaitSlot slot = waitSlotList; ; slot = slot.next ) {
            // if we are at the end of the list, or in-between links, create a new link
            if ( slot == null || slot.time > time ) {
                return insertAfter(prev, new WaitSlot(time));
            }
            // if we matched the time of some other waiter exactly
            if ( slot.time == time ) {
                return slot;
            }
            // keep track of previous link
            prev = slot;
        }
    }

    private WaitSlot insertAfter(WaitSlot prev, WaitSlot w) {
        if ( prev != null ) {
            w.next = prev.next;
            prev.next = w;
        } else {
            waitSlotList = w;
        }
        return w;
    }

    protected WaitSlot checkWaiters() {
        // have all threads reached either a meet or a wait?
        if ( wait_count + meet_count < goal ) return null;

        // are there any waiters at all?
        if ( waitSlotList == null ) return null;

        // there is a ready wait slot, wake those threads waiting on it
        WaitSlot h = waitSlotList;
        // move the wait list ahead to the next link
        waitSlotList = h.next;
        synchronized ( h ) {
            // notify the threads waiting on this wait slot
            h.shouldWait = false;
            h.notifyAll();
        }
        // reduce the wait count by the number of waiters in this slot
        wait_count -= h.numWaiters;
        return h;
    }

    protected boolean stillWaiting(WaitSlot w) {
        if ( w == null ) return false;
        for ( WaitSlot h = waitSlotList; h != null; h = h.next )
            if ( h == w ) return true;
        return false;
    }

}
