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

import java.util.Arrays;

/**
 * The <code>TimeSequence</code> class implements a simple array-like data structure that collects
 * a large list of integers and supports iterating over that list. For memory-efficient storage, it
 * uses a set of arrays where each array represents a fragment of the measurements obtained.
 *
 * @author Ben L. Titzer
 */
public class TimeSequence {

    public static class Measurement {
        public long time;
        public int value;
    }

    static class TreeNode {
        final long beginTime;
        final TreeNode left;
        TreeNode right;
        TreeNode parent;

        TreeNode(TreeNode left, long bt) {
            this.left = left;
            beginTime = bt;
        }

        Fragment find(long time) {
            if (right == null || time < right.beginTime) {
                return left != null ? left.find(time) : (Fragment) this;
            } else {
                return right.find(time);
            }
        }
    }

    public class Iterator {
        int cursor;
        Fragment frag;

        Iterator(Fragment f, int index) {
            frag = f;
            cursor = index;
        }

        public boolean hasNext() {
            return cursor < frag.offset || frag.next != null && frag.next.offset > 0;
        }

        public void next(Measurement m) {
            if (cursor >= frag.offset) {
                frag = frag.next;
                cursor = 0;
            }
            m.time = frag.times[cursor];
            m.value = frag.values[cursor];
            cursor++;
        }
    }

    static class Fragment extends TreeNode {

        Fragment next;
        final long[] times;
        final int[] values;
        int offset;

        Fragment(long bt, int fragSize) {
            super(null, bt);
            times = new long[fragSize];
            Arrays.fill(times, Long.MAX_VALUE);
            values = new int[fragSize];
        }
    }

    TreeNode addNode(TreeNode tn, TreeNode parent) {
        if (parent == null) {
            // no parent exists, create oldest ancestor
            TreeNode np = new TreeNode(tn, tn.beginTime);
            root = np;
            tn.parent = np;
            return np;
        }
        if (parent.right == null) {
            // there is free space in this parent
            parent.right = tn;
            tn.parent = parent;
            return parent;
        } else {
            // both full, recursively create uncle
            TreeNode uncle = new TreeNode(tn, tn.beginTime);
            uncle.parent = addNode(uncle, parent.parent);
            return uncle;
        }
    }

    final int fragSize;
    Fragment current;
    Fragment prev;
    TreeNode root;

    long currentTime;
    int total;

    int min;
    int max;

    /**
     * The default constructor for the <code>Measurements</code> class creates a new instance where the
     * fragment size is 500.
     */
    public TimeSequence() {
        this(500);
    }

    /**
     * This constructor for the <code>Measurements</code> class creates a new instance with the specified
     * fragment size.
     *
     * @param fragsize the fragment size to use for internal representation
     */
    public TimeSequence(int fragsize) {
        fragSize = fragsize;
        newFragment();
    }

    /**
     * The <code>add()</code> method adds a new measurement to this set.
     *
     * @param time the time of the measurement
     * @param nm   the new measurement to add
     */
    public void add(long time, int nm) {
        assert (currentTime > time);

        recordMinMax(nm);
        int off = current.offset;
        current.values[off] = nm;
        current.times[off] = time;
        total++;
        currentTime = time;
        current.offset++;
        if (current.offset >= fragSize) {
            newFragment();
        }
    }

    /**
     * The <code>iterator()</code> method returns an interator over the measurements, starting with the
     * specified measurement.
     *
     * @param startTime the time to start
     * @return an iterator that will start at the specified measurement and continue until the end
     */
    public Iterator iterator(long startTime) {
        Fragment f = root.find(startTime);
        if (startTime <= f.beginTime) {
            return new Iterator(f, 0);
        }
        int ind = Arrays.binarySearch(f.times, startTime);
        if (ind < 0) return new Iterator(f, f.offset - 1);
        else return new Iterator(f, ind);
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
    public void addAll(TimeSequence m) {
        Measurement nm = new Measurement();
        Iterator i = m.iterator(0);
        while (i.hasNext()) {
            i.next(nm);
            add(nm.time, nm.value);
        }
    }

    /**
     * The <code>getLastTime()</code> method returns the time when the last measurement was recorded.
     *
     * @return the time in clock cycles that the last measurement was recorded
     */
    public long getLastTime() {
        return currentTime;
    }

    public int min() {
        return min;
    }

    public int max() {
        return max;
    }

    private void recordMinMax(int nm) {
        if (total == 0) {
            min = max = nm;
        } else {
            max = max > nm ? max : nm;
            min = min < nm ? min : nm;
        }
    }


    private void newFragment() {
        Fragment nf = new Fragment(currentTime, fragSize);
        if (current != null) {
            current.next = nf;
            addNode(nf, current.parent);
        } else {
            addNode(nf, null);
        }
        prev = current;
        current = nf;
    }

}
