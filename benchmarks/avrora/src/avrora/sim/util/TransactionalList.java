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

package avrora.sim.util;



/**
 * The <code>TransactionalList</code> class implements a list of objects that has the special property that it
 * can be frozen temporarily and unfrozen. During the time that the list is "frozen", requests to add or remove
 * objects from the list are queued. Upon unfreezing, the queue of requests is processed in order, and then
 * the list is free to have objects added and removed again.
 *
 * @author Ben L. Titzer
 */
public class TransactionalList {
    /**
     * The <code>Link</code> class is used internally to represent links for the list of objects and
     * updates.
     */
    public static class Link {
        public boolean addTransaction; // used for transactions; true if the transaction was an add, false if it was a remove
        public final Object object;
        public Link next;

        Link(Object o) {
            object = o;
        }

        Link(Object o, boolean a) {
            object = o;
            addTransaction = a;
        }
    }

    protected Link head;
    protected Link tail;

    protected Link transHead;
    protected Link transTail;

    protected int nesting;

    /**
     * The <code>add()</code> method allows another probe to be inserted into the multicast set. It will be
     * inserted at the end of the list of current probes and will therefore fire after any probes already in
     * the multicast set.
     *
     * @param b the probe to insert
     */
    public void add(Object b) {
        if (nesting > 0) {
            addTransaction(b, true);
            return;
        }

        if (head == null) {
            head = tail = new Link(b);
        } else {
            tail.next = new Link(b);
            tail = tail.next;
        }
    }

    /**
     * The <code>remove</code> method removes a probe from the multicast set. The order of the remaining
     * probes is not changed. The comparison used is reference equality, not the <code>.equals()</code>
     * method.
     *
     * @param o the probe to remove
     */
    public void remove(Object o) {
        if (nesting > 0) {
            addTransaction(o, false);
            return;
        }

        Link prev = null;
        Link pos = head;
        while (pos != null) {
            Link next = pos.next;

            // matched?
            if (pos.object == o) {
                // remove the head ?
                if (prev == null) head = pos.next;
                // somewhere in the middle (or at end)
                else prev.next = pos.next;
                // remove the tail item ?
                if (pos == tail) tail = prev;

            } else {
                // no match; continue
                prev = pos;
            }
            pos = next;
        }
    }

    private void addTransaction(Object o, boolean isAdd) {
        if (transHead == null) {
            transHead = transTail = new Link(o, isAdd);
        } else {
            transTail.next = new Link(o, isAdd);
            transTail = transTail.next;
        }
    }

    /**
     * The <code>isEmpty()</code> method tests whether this list is empty. If this list is currently in
     * a transaction (frozen), this method does not consider pending updates to the list.
     *
     * @return true if there are no objects in this list; false otherwise
     */
    public boolean isEmpty() {
        return head == null;
    }

    /**
     * The <code>beginTransaction()</code> method freezes this list so that further requests for updates
     * (such as to add or remove objects) are queued until the transaction is completed, which is triggered
     * by a called to <code>endTransaction()</code>. When this method is called, all pending requests to
     * add or remove objects are processed in order.
     */
    public void beginTransaction() {
        nesting++;
    }

    /**
     * The <code>endTransaction()</code> method unlocks this list from the transaction and will process
     * any queued adds or removes in order from the time the <code>beginTransaction()</code> method was called.
     */
    public void endTransaction() {
        nesting--;
        if ( nesting == 0 ) {
            Link thead = transHead;
            transHead = null;
            transTail = null;
            for (Link pos = thead; pos != null; pos = pos.next) {
                if ( pos.addTransaction ) add(pos.object);
                else remove(pos.object);
            }
        }
    }

    public Link getHead() {
        return head;
    }

}
