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

import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * The <code>Sequence</code> class implements a simple array-like data structure that collects
 * a large list of integers and supports iterating over that list. For memory-efficient storage, it
 * uses a set of arrays where each array represents a fragment of the measurements obtained.
 *
 * @author Ben L. Titzer
 */
public class Sequence {

    public class Iterator {
        int cursor;
        int[] frag;
        java.util.Iterator fiter;

        Iterator(int start) {
            fiter = fragments.iterator();
            frag = (int[]) fiter.next();
            while (start >= fragSize) {
                if (!fiter.hasNext()) break;
                frag = (int[]) fiter.next();
                start -= fragSize;
            }
            cursor = start;
        }

        public boolean hasNext() {
            return fiter.hasNext() || cursor < offset;
        }

        public int next() {
            if (frag == currentFrag) {
                // we are in the last fragment, did we run off the end?
                if (cursor >= offset) {
                    throw new NoSuchElementException();
                }
            } else {
                // we are in an old (complete) fragment
                if (cursor >= fragSize) {
                    frag = (int[]) fiter.next();
                    cursor = 0;
                }
            }
            return frag[cursor++];
        }
    }

    final LinkedList fragments;
    final int fragSize;

    int[] currentFrag;
    int offset;
    int total;

    int min;
    int max;

    /**
     * The default constructor for the <code>Measurements</code> class creates a new instance where the
     * fragment size is 500.
     */
    public Sequence() {
        this(500);
    }

    /**
     * This constructor for the <code>Measurements</code> class creates a new instance with the specified
     * fragment size.
     *
     * @param fragsize the fragment size to use for internal representation
     */
    public Sequence(int fragsize) {
        fragSize = fragsize;
        fragments = new LinkedList();
        newFragment();
    }

    /**
     * The <code>add()</code> method adds a new number to this sequence.
     *
     * @param nm the new measurement to add
     */
    public void add(int nm) {
        // add the number to the current fragment
        currentFrag[offset++] = nm;

        if (total == 0) {
            // if it is the first one, it is both the minimum and maximum
            min = max = nm;
        } else if ( nm > max ) {
            // if it is greater than max, record a new max
            max = nm;
        } else if ( nm < min ) {
            // if it is less than min, record a new min
            min = nm;
        }
        if (offset >= fragSize) {
            // if the fragment is full, allocate a new one
            newFragment();
        }
        total++;
    }

    /**
     * The <code>iterator()</code> method returns an interator over the measurements, starting with the
     * specified measurement index.
     *
     * @param start the index of the first measurement to start from
     * @return an iterator that will start at the specified measurement and continue until the end
     */
    public Iterator iterator(int start) {
        return new Iterator(start);
    }

    /**
     * The <code>size()</code> method returns the number of entries in this measurement data.
     *
     * @return the number of measurements
     */
    public int size() {
        return total;
    }

    /**
     * The <code>addAll()</code> method adds all of the measurements from another measurement structure
     * to the end of this measurement structure.
     *
     * @param m the measurements to add to the end of this list
     */
    public void addAll(Sequence m) {
        Iterator i = m.iterator(0);
        while (i.hasNext()) {
            add(i.next());
        }
    }

    public int min() {
        return min;
    }

    public int max() {
        return max;
    }

    private void newFragment() {
        currentFrag = new int[fragSize];
        fragments.add(currentFrag);
        offset = 0;
    }

}
