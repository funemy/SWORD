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

package cck.stat;

import cck.text.Printer;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * This class represents the proportion of different items with respect to one another. For example, the
 * proportion of cars, trucks, vans, etc on the highway.
 *
 * @author Ben L. Titzer
 */
public class Proportion implements DataItem {

    protected final String name;
    protected LinkedList shares;
    protected int total;

    /**
     * Public constructor that takes a string name.
     */
    public Proportion(String newname) {
        name = newname;
        shares = new LinkedList();
    }

    public String getName() {
        return name;
    }

    /**
     * Generate a text report of the shares.
     * @param printer
     */
    public void print(Printer printer) {
        Iterator i = shares.iterator();
        printer.print("\n " + name);
        printer.print("\n---------------------------------------------------------------------");

        while ( i.hasNext() ) {
            Counter s = (Counter) i.next();
            // TODO: print out the fraction
            s.print(printer);
        }

        printer.print("\n");
    }

    /**
     * LegacyRegister a counter object with this proportion.
     */
    public Counter newCounter(String name) {
        Counter s = new Counter(name);
        shares.add(s);
        return s;
    }

    /**
     * Register an integer count with this proportion object and return a Counter object.
     */
    public void addCounter(Counter c) {
        shares.add(c);
    }

    /**
     * Search for the counter with the specified string name and return it if it is registered.
     */
    public Counter getCounter(String name) {
        Iterator i = shares.iterator();

        while ( i.hasNext() ) {
            Counter s = (Counter) i.next();
            if (name.equals(s.getName())) return s;
        }

        return null;
    }

    /**
     * Search for the counter with the specified name and report its proportion. Return -1 if not found.
     */
    public float getFraction(String name) {
        process(); // make sure proportions up to date

        Counter c = getCounter(name);
        return c == null ? -1 : c.getTotal() / (float)total;
    }

    /**
     * Do the computations and compute the proportions of each share.
     */
    public void process() {
        Iterator i = shares.iterator();
        total = 0;

        while (i.hasNext()) {
            total += ((Counter) i.next()).getTotal();
        }
    }

    /**
     * Return true if this proportion has any information available.
     */
    public boolean empty() {
        return (shares.isEmpty());
    }
}
