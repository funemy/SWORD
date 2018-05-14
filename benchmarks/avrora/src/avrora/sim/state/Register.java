/**
 * Copyright (c) 2006, Regents of the University of California
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
 * Creation date: Sep 20, 2006
 */

package avrora.sim.state;

import avrora.sim.util.TransactionalList;

/**
 * The <code>Register</code> class represents a register of a certain
 * bit width within a simulated CPU or device. A <code>Register</code>
 * object allows instrumentation to be both state access (reads)
 * and state updates (writes). This allows registers to be instrumented
 * by the user for profiling and debugging purposes, and also by
 * device implementations that may sub ranges of registers.
 *
 * </p>
 * This implementation models registers that can be as wide as 32 bits.
 *
 * @author Ben L. Titzer
 */
public class Register implements RegisterView {

    public final int width;
    public final int mask;

    protected int value;

    protected VolatileBehavior behavior;
    protected TransactionalList watches;

    /**
     * The constructor for the <code>Register</code> class creates a new register
     * with the specified width in bits.
     * @param w the width of the register in bits
     */
    public Register(int w) {
        width = w;
        mask = ~(-1 << w);
    }

    /**
     * The <code>Watch</code> interface allows clients to add instrumentation
     * to a register. The object implementing the watch is then consulted
     * when reads and writes to the register occur.
     *
     */
    public interface Watch {
        public void fireAfterWrite(Register r, int oldv, int newv);
        public void fireAfterRead(Register r, int oldv, int newv);

        public static class Empty implements Watch {
            public void fireAfterWrite(Register r, int oldv, int newv) {
                // do nothing.
            }
            public void fireAfterRead(Register r, int oldv, int newv) {
                // do nothing.
            }
        }
    }

    protected static class NotifyItem {
        protected final Watch notify;
        protected NotifyItem next;
        protected NotifyItem(Watch n, NotifyItem nx) {
            notify = n;
            next = nx;
        }
    }

    /**
     * The <code>write()</code> method writes a value to the register. This method
     * will notify any objects that have been added to the watch list.
     *
     * @param newv the value to write to this register
     */
    public void write(int newv) {
        int oldv = value;
        newv = newv & mask;
        value = (behavior != null ? mask & behavior.write(value, newv) : newv);
        if ( watches != null ) {
            // call instrumentation code.
            watches.beginTransaction();
            for ( TransactionalList.Link n = watches.getHead(); n != null; n = n.next ) {
                ((Watch)n.object).fireAfterWrite(this, oldv, newv);
            }
            watches.endTransaction();
        }
    }
    /**
     * The <code>read()</code> method reads a value from this register. This method
     * will trigger calls to an objects in the watch list.
     * @return the value in this register
     */
    public int read() {
        int oldv = value;
        int newv = value;
        if ( behavior != null ) value = newv = mask & behavior.read(value);
        if ( watches != null ) {
            // call instrumentation code.
            watches.beginTransaction();
            for ( TransactionalList.Link n = watches.getHead(); n != null; n = n.next ) {
                ((Watch)n.object).fireAfterRead(this, oldv, newv);
            }
            watches.endTransaction();
        }
        return value;
    }

    /**
     * The <code>setBehavior()</code> method sets the behavior of this register.
     * @param b the behavior for this register
     */
    public void setBehavior(VolatileBehavior b) {
        behavior = b;
    }

    /**
     * The <code>setValue()</code> method sets the value of this register, without triggering
     * the notification of any objects in the notification list. This interface should not
     * be used by client (user) code, but is intended for subfields and devices using
     * subfields.
     * @param val the value to which to set the register
     */
    public void setValue(int val) {
        value = val & mask;
    }

    /**
     * The <code>getValue()</code> method retrieves the value from this register, without triggering
     * the notification of any objects in the notification list. This interface should be used
     * by client code (if necessary at all) to avoid recursive triggering of notifications.
     * @return the value of this register
     */
    public int getValue() {
        return value;
    }

    /**
     * The <code>getWidth()</code> method returns the width of this register (or register view)
     * in bits.
     * @return the width of this view in bits.
     */
    public int getWidth() {
        return width;
    }

    public void addWatch(Watch w) {
        if ( watches == null ) {
            watches = new TransactionalList();
        }
        watches.add(w);
    }

    public void removeWatch(Watch w) {
        watches.remove(w);
        if ( watches.isEmpty() ) watches = null;
    }
}
