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

import avrora.sim.Simulator;

/**
 * The <code>DeltaQueue</code> class implements an amortized constant time delta-queue for processing of
 * scheduled events. Events are put into the queue that will fire at a given number of cycles in the future.
 * An internal delta list is maintained where each link in the list represents a set of events to be fired
 * some number of clock cycles after the previous link.
 * <p/>
 * Each delta between links is maintained to be non-zero. Thus, to insert an event X cycles in the future, at
 * most X nodes will be skipped over. Therefore, when the list is advanced over X time steps, this cost is
 * amortized to be constant.
 * <p/>
 * For each clock cycle, only the first node in the list must be checked, leading to constant time work per
 * clock cycle.
 * <p/>
 * This class allows the clock to be advanced multiple ticks at a time.
 * <p/>
 * Also, since this class is used heavily in the simulator, its performance is important and maintains an
 * internal cache of objects. Thus, it does not create garbage over its execution and never uses more space
 * than is required to store the maximum encountered simultaneous events. It does not use standard libraries,
 * casts, virtual dispatch, etc.
 */
public class DeltaQueue {

    /**
     * The <code>EventList</code> class represents a link in the list of events for a given <code>Link</code>
     * in the delta queue chain.
     */
    private static class EventList {
        Simulator.Event event;
        EventList next;

        /**
         * The constructor for the <code>EventList</code> class simply initializes the internal references to
         * the event and the next link in the chain based on the parameters passed.
         *
         * @param t a reference the event
         * @param n the next link in the chain
         */
        EventList(Simulator.Event t, EventList n) {
            event = t;
            next = n;
        }

    }

    /**
     * The <code>Link</code> class represents a link in the list of delta queue items that are being stored.
     * It contains a list of events that share the same delta.
     */
    private class Link {
        EventList events;

        Link next;
        long delta;

        Link(Simulator.Event t, long d) {
            events = newEventList(t, null);
            delta = d;
        }

        void add(Simulator.Event t) {
            events = newEventList(t, events);
        }

        void remove(Simulator.Event t) {
            EventList prev = null;
            EventList pos = events;
            while (pos != null) {
                EventList next = pos.next;

                if (pos.event == t) {
                    if (prev == null)
                    // remove the whole thing.
                        events = pos.next;
                    else
                    // remove the "pos" link
                        prev.next = pos.next;

                    free(pos);
                } else {
                    prev = pos;
                }
                pos = next;
            }
        }

        void fire() {
            for (EventList pos = events; pos != null; pos = pos.next) {
                pos.event.fire();
            }
        }
    }

    /**
     * The <code>head</code> field stores a reference to the head of the delta queue, which represents the
     * event that is nearest in the future.
     */
    protected Link head;

    /**
     * The <code>freeLinks</code> field stores a reference to any free links that have become unused during
     * the processing of events. A free list is used to prevent garbage from accumulating.
     */
    protected Link freeLinks;

    /**
     * The <code>freeEventLists</code> field stores a reference to any free event links that have become
     * unused during the processing of events. A free list is used to prevent garbage from accumulating.
     */
    protected EventList freeEventLists;

    /**
     * The <code>count</code> field stores the total number of cycles that this queue has been advanced, i.e.
     * the sum of all <code>advance()</code> calls.
     */
    protected long count;

    /**
     * The <code>add</code> method adds an event to be executed in the future.
     *
     * @param t      the event to add
     * @param cycles the number of clock cycles in the future
     */
    public void insertEvent(Simulator.Event t, long cycles) {
        // degenerate case, nothing in the queue.
        if (head == null) {
            head = newLink(t, cycles, null);
            return;
        }

        // search for first link that is "after" this cycle delta
        Link prev = null;
        Link pos = head;
        while (pos != null && cycles > pos.delta) {
            cycles -= pos.delta;
            prev = pos;
            pos = pos.next;
        }

        if (pos == null) {
            // end of the head
            insertAfter(prev, t, cycles, null);
        } else if (cycles == pos.delta) {
            // exactly matched the delta of some other event
            pos.add(t);
        } else {
            // insert a new link in the chain
            insertAfter(prev, t, cycles, pos);
        }
    }

    private void insertAfter(Link prev, Simulator.Event t, long cycles, Link next) {
        if (prev != null)
            prev.next = newLink(t, cycles, next);
        else
            head = newLink(t, cycles, next);
    }

    /**
     * The <code>remove</code> method removes all occurrences of the specified event within the delta queue.
     *
     * @param e the event to remove
     */
    public void removeEvent(Simulator.Event e) {
        if (head == null) return;

        // search for first link that is "after" this cycle delta
        Link prev = null;
        Link pos = head;
        while (pos != null) {
            Link next = pos.next;
            pos.remove(e);

            if (pos.events == null) {
                // the link became empty because of removing this event
                if (prev == null)
                    head = pos.next;
                else
                    prev.next = pos.next;

                // fixes up the delta of the next item in the queue
                if (pos.next != null) {
                    pos.next.delta += pos.delta;
                }

                free(pos);
            } else {
                // the link did not become empty, just move on
                prev = pos;
            }
            // advance to next link in the list
            pos = next;
        }
    }

    /**
     * The <code>advance</code> method advances timesteps through the queue by the specified number of clock
     * cycles, processing any events.
     *
     * @param cycles the number of clock cycles to advance
     */
    public void advance(long cycles) {
        if ( head == null ) {
            // fast path 1: nothing in the queue
            count += cycles;
            return;
        }

        if ( head.delta > cycles ) {
            // fast path 2: head does not fire
            count += cycles;
            head.delta -= cycles;
            return;
        }

        advanceSlow(cycles);

    }

    /**
     * The <code>skipAhead()</code> method skips ahead to the next event in the queue and fires it.
     */
    public void skipAhead() {
        if ( head == null ) {
            // fast path 1: nothing in the queue
            count++;
            return;
        }

        Link h = head;
        count += h.delta;
        head = h.next;
        h.fire();
        free(h);
    }

    private void advanceSlow(long cycles) {
        // slow path: head (and maybe more) fires
        while (head != null && cycles > 0) {

            Link pos = head;
            Link next = pos.next;

            // cache pos.delta because it is used twice
            long delta = pos.delta;
            // number of cycles leftover after advancing by pos.delta
            long leftover = cycles - delta;

            // if haven't arrived yet, advance and return
            if (leftover < 0) {
                count += cycles;
                pos.delta = -leftover;
                return;
            }

            // advance by the number of cycles at the head of the queue
            count += delta;

            // chop off head
            head = next;

            // fire all events at head
            pos.fire();

            // free the head
            free(pos);

            // process leftover cycles next time through loop
            cycles = leftover;
        }

        // add in leftover cycles if we reached end of queue (head == null)
        count += cycles;
    }

    /**
     * The <code>getHeadDelta()</code> method gets the number of clock cycles until the first event will
     * fire.
     *
     * @return the number of clock cycles until the first event will fire
     */
    public long getFirstEventTime() {
        if (head != null) return head.delta;
        return -1;
    }

    /**
     * The <code>getCount()</code> gets the total cumulative count of all the <code>advance()</code> calls on
     * this delta queue.
     *
     * @return the total number of cycles this queue has been advanced
     */
    public long getCount() {
        return count;
    }

    private void free(Link l) {
        l.next = freeLinks;
        freeLinks = l;

        freeEventLists = l.events;
        l.events = null;
    }

    private void free(EventList l) {
        l.event = null;
        l.next = freeEventLists;
        freeEventLists = l;
    }

    private Link newLink(Simulator.Event t, long cycles, Link next) {
        Link l;
        if (freeLinks == null)
        // if none in the free list, allocate one
            l = new Link(t, cycles);
        else {
            // grab one from the free list
            l = freeLinks;
            freeLinks = freeLinks.next;
            l.delta = cycles;
            l.add(t);
        }

        // adjust delta in the next link in the chain
        if (next != null) {
            next.delta -= cycles;
        }

        l.next = next;
        return l;
    }

    private EventList newEventList(Simulator.Event t, EventList next) {
        EventList l;

        if (freeEventLists == null) {
            // no free links, so allocate one
            l = new EventList(t, next);
        } else {
            // grab the first link off the free chain
            l = freeEventLists;
            freeEventLists = freeEventLists.next;
            l.next = next;
            l.event = t;
        }

        return l;
    }
}
