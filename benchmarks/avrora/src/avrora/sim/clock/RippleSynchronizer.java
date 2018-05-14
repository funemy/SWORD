/**
 * Copyright (c) 2007, Ben L. Titzer
 * See the file "license.txt" for details.
 *
 * Created Nov 11, 2007
 */
package avrora.sim.clock;

import avrora.sim.*;

import java.util.HashMap;
import java.util.Iterator;

import cck.util.Util;

/**
 * The <code>IntervalSynchronizer</code> class implements a global timer among multiple simulators by inserting
 * periodic events into the queue of each simulator.
 *
 * @author Ben L. Titzer
 */
public class RippleSynchronizer extends Synchronizer {

    /**
     * <code>period</code> is the number of cycles on a member local clock per cycle on the global clock. Some
     * re-coding must be done if microcontrollers running at difference speeds are to be accurately
     * simulated.
     */
    protected long notifyPeriod;
    protected final HashMap threadMap;
    protected final Simulator.Event action;

    protected int goal;
    protected long wallTime;
    protected int meet_count;
    protected int wait_count;

    protected WaitLink waitListHead;

    /**
     * The constructor for the <code>RippleSynchronizer</code> class creates a new synchronizer
     * with the specified period, that will fire the specified event each time all threads meet at
     * a synchronization point.
     * @param p the period in clock cycles which to synchronize the threads
     * @param a the event to fire each time all threads meet at a synchronization point
     */
    public RippleSynchronizer(long p, Simulator.Event a) {
        notifyPeriod = p;
        action = a;
        threadMap = new HashMap();
        WaitLink end = new WaitLink(Long.MAX_VALUE);
        WaitLink start = new WaitLink(-1);
        start.numPassed = goal;
        start.next = end;
        waitListHead = start;

    }

    /**
     * The <code>NotifyEvent</code> class represents an event that is inserted into the event
     * queue of each simulator to periodically notify the synchronizer of the simulator's
     * progress through global time. This is necessary so that all threads eventually notify
     * the synchronizer of their progress (regardless of whether they need to wait for others)
     * so that waiting threads can make progress.
     */
    protected class NotifyEvent implements Simulator.Event {

        protected final SimulatorThread thread;
        protected final MainClock clock;
        protected boolean removed;
        protected WaitLink lastLink;

        protected NotifyEvent(SimulatorThread t) {
            thread = t;
            clock = t.getSimulator().getClock();
        }

        /**
         * The <code>fire()</code> method of this event is called by the individual event queues of each
         * simulator as they reach this point in time. The implementation of this method waits for all threads
         * to join.
         */
        public void fire() {
            if (!removed) {
                long now = clock.getCount();
                long delta;
                synchronized (RippleSynchronizer.this) {
                    lastLink = advance(now, currentWaitLink());
                    delta = lastLink.next.time - now;
                }
                if (delta < notifyPeriod) {
                    clock.insertEvent(this, delta);
                } else {
                    clock.insertEvent(this, notifyPeriod);
                }
            }
        }

        private WaitLink currentWaitLink() {
            if (lastLink == null) {
                return waitListHead;
            }
            return lastLink;
        }

    }


    /**
     * The <code>WaitLink</code> class represents a slot in time where multiple threads are waiting
     * for others to catch up.
     */
    private static class WaitLink {
        protected final long time;
        protected int numPassed;
        protected WaitLink next;

        WaitLink(long t) {
            time = t;
        }
    }

    private WaitLink advance(long time, WaitLink link) {
        assert time >= link.time;
        if (time == link.time) {
            return link;
        }
        WaitLink prev = link;
        for ( link = link.next; ; link = link.next ) {
            // if we are in-between links, create a new link
            assert link != null;
            if (time < link.time) {
                // we met a link that has a greater time than this one.
                WaitLink nlink = new WaitLink(time);
                nlink.numPassed = link.numPassed;
                nlink.next = link;

                assert prev.next == link;
                prev.next = nlink;
                notifyLink(nlink);
                return nlink;
            } else if (time == link.time) {
                // this notifier just met the link exactly in time
                notifyLink(link);
                return link;
            } else {
                // this notifier just has already passed the link in time
                notifyLink(link);
                prev = link;
            }
        }
    }

    private void waitFor(long time, WaitLink link) throws InterruptedException {
        if (time <= link.time) {
            waitForLink(link);
            return;
        }
        WaitLink prev = link;
        for ( link = link.next; ; link = link.next ) {
            // search for this wait link
            assert link != null;
            if ( time < link.time) {
                // we met a link that has a greater time than this one.
                WaitLink nlink = insertLink(time, prev, link);
                waitForLink(nlink);
                return;
            } else if (time == link.time) {
                // this notifier just met the link exactly in time
                waitForLink(link);
                return;
            }
            // skip this link
            prev = link;
        }
    }

    private void waitForLink(WaitLink nlink) throws InterruptedException {
        assert nlink.numPassed >= 1;
        while (nlink.numPassed < goal) {
            RippleSynchronizer.this.wait();
        }
    }

    private WaitLink insertLink(long time, WaitLink prev, WaitLink next) {
        WaitLink nlink = new WaitLink(time);
        nlink.numPassed = next.numPassed;
        nlink.next = next;

        assert prev != null;
        assert prev.next == next;
        prev.next = nlink;
        return nlink;
    }

    private void notifyLink(WaitLink link) {
        if (++link.numPassed >= goal) {
            RippleSynchronizer.this.notifyAll();
            waitListHead = link;
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
        NotifyEvent event = new NotifyEvent(st);
        threadMap.put(st, event);
        // insert the synch event in the thread's queue
        event.clock.insertEvent(event, notifyPeriod);
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
        if (threadMap.containsKey(st)) {
          waitForNeighbors(t.getSimulator().getClock().getCount());
            synchronized (this) {
                goal--;
                this.notifyAll();
            }
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
        NotifyEvent event = (NotifyEvent)threadMap.get(thread);
        try {
            long now = thread.getSimulator().getClock().getCount();
            assert time <= now;
            synchronized (RippleSynchronizer.this) {
                WaitLink link = event.currentWaitLink();
                event.lastLink = advance(now, link);
                waitFor(time, link);
            }
        } catch (InterruptedException e) {
            throw Util.unimplemented();
        }
    }

}
