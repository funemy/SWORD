/**
 * Copyright (c) 2007, Regents of the University of California
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
 *
 * Created Oct 20, 2007
 */
package avrora.sim.output;

import avrora.sim.Simulator;

import java.util.List;
import java.util.ArrayList;

/**
 * The <code>EventBuffer</code> definition.
 *
 * @author Ben L. Titzer
 */
public class EventBuffer {

    public class Event {
        public final long time;
        public final Object object;
        public final long param;
        public Event next;

        Event(Object object, long param) {
            this.time = sim.getClock().getCount();
            this.object = object;
            this.param = param;
        }

        public Simulator getSimulator() {
            return sim;
        }
    }

    public final Simulator sim;
    protected Event head;
    protected Event tail;

    public EventBuffer(Simulator s) {
        sim = s;
    }

    protected Event recordEvent(Object o, long param) {
        Event e = new Event(o, param);
        if (tail == null) {
            head = tail = e;
        } else {
            tail.next = e;
            tail = e;
        }
        return e;
    }

    public Event extract(long time) {
        Event prev = null;
        Event cur = head;
        while (cur != null) {
            if (cur.time >= time) {
                // chop the list before this node
                if (prev != null) {
                    // extracted at least one node
                    prev.next = null;
                    prev = head;
                    head = cur;
                    return prev;
                }
                // did not extract any nodes
                return null;
            }
            prev = cur;
            cur = cur.next;
        }
        // consumed the whole list
        cur = head;
        head = tail = null;
        return cur;
    }
}
