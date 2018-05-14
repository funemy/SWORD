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
package avrora.sim.state;

import cck.text.StringUtil;

/**
 * The <code>NamedRegister</code> class is a utility that helps in debugging and
 * printing of registers. A <code>NamedRegister</code> can have subfields that
 * are remembered and automatically printed out when converting the register
 * to a string (e.g. for printing).
 *
 * @author Ben L. Titzer
 */
public class NamedRegister extends Register {

    protected abstract class Printer {
        final String name;
        Printer next;
        abstract void print(StringBuffer buf);
        Printer(String n) {
            name = n;
        }
    }

    protected class RegPrinter extends Printer {
        final RegisterView reg;
        RegPrinter(String n, RegisterView r) {
            super(n);
            reg = r;
        }
        void print(StringBuffer buf) {
            buf.append(name);
            buf.append(" = ");
            buf.append(StringUtil.toMultirepString(reg.getValue(), reg.getWidth()));
        }
    }

    protected class BoolPrinter extends Printer {
        final BooleanView reg;
        BoolPrinter(String n, BooleanView r) {
            super(n);
            reg = r;
        }
        void print(StringBuffer buf) {
            buf.append(name);
            buf.append(" = ");
            buf.append(reg.getValue() ? "true" : "false");
        }
    }

    public final String name;
    protected Printer head;
    protected Printer tail;

    public NamedRegister(String n, int w) {
        super(w);
        name = n;
    }

    /**
     * The <code>booleanView()</code> method creates a boolean view of a bit within
     * this register.
     * If a name is specified, then this register view will be added to an internal
     * list that will print out the value of the bits in the <code>.toString()</code>
     * representation of this register.
     * @param n the name of the view of the register
     * @param b the bit number to view as a boolean
     * @return an object that allows reading and writing of the specified bit as a boolean
     * according to the <code>BooleanView</code> interface
     */
    public BooleanView booleanView(String n, int b) {
        BooleanView view = RegisterUtil.booleanView(this, b);
        if (n != null) addPrinter(new BoolPrinter(n, view));
        return view;
    }

    /**
     * The <code>bitRangeView()</code> method creates a view of some bits in this
     * register that are in order.
     * If a name is specified, then this register view will be added to an internal
     * list that will print out the value of the bits in the <code>.toString()</code>
     * representation of this register.
     * @param n the name of the register view
     * @param l the low bit in the bit range
     * @param h the high bit in the bit range (inclusive)
     * @return a new <code>RegisterView</code> instance that allows reading and writing
     * the specified bit range.
     */
    public RegisterView bitRangeView(String n, int l, int h) {
        RegisterView view = RegisterUtil.bitRangeView(this, l, h);
        if (n != null) addPrinter(new RegPrinter(n, view));
        return view;
    }

    /**
     * The <code>permutedView()</code> method creates a new view of some of the
     * bits in this register that may be permuted abritrarily.
     * If a name is specified, then this register view will be added to an internal
     * list that will print out the value of the bits in the <code>.toString()</code>
     * representation of this register.
     * @param n the name of this register view
     * @param perm the permutation of bits (low to high)
     * @return a new <code>RegisterView</code> instance that allows reading and
     * writing the specified permuted bits.
     */
    public RegisterView permutedView(String n, byte[] perm) {
        RegisterView view = RegisterUtil.permutedView(this, perm);
        if (n != null) addPrinter(new RegPrinter(n, view));
        return view;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer(name);
        buf.append(" = ");
        buf.append(StringUtil.toMultirepString(getValue(), getWidth()));
        if ( head != null ) {
            buf.append('(');
            for ( Printer p = head; p != null; p = p.next) {
                p.print(buf);
                if (p.next != null) buf.append(", ");
            }
            buf.append(')');
        }
        return buf.toString();
    }

    void addPrinter(Printer p) {
        if ( head == null ) {
            head = tail = p;
        } else {
            tail.next = p;
            tail = p;
        }
    }
}
