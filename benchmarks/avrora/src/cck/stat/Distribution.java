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
import cck.util.Util;

/**
 * This class keeps track of the min, max, and median of a stream of integers, as well as the distribution of
 * each. The distribution aids in computing the median without having to store the sequence of integers.
 *
 * @author Ben L. Titzer
 */
public class Distribution extends MinMaxMean {

    /**
     * The <code>distrib</code> field stores an array that records the number of occurrences for each value in
     * the distribution. The <code>distribMin</code> field stores the value which corresponds to expr 0 in the
     * array. Therefore, <code>distrib[myval - distribMin]</code> contains the count for <code>myval</code>,
     * provided that the array is large enough. Values outside the array have have a count of zero.
     */
    public int[] distrib; // table of number of occurrences of each value

    /**
     * The <code>median</code> field stores the median value of the distribution. This field is not computed
     * until the <code>process()</code> method has been called after data has been collected.
     */
    public int median;

    /**
     * The <code>distribMin</code> field stores the value corresponding to expr 0 in the <code>distrib</code>
     * array.
     */
    public int distribMin; // the base value of the occurences table

    /**
     * The <code>distribname</code> field stores the string that should be reported as the name of the
     * distribution, e.g. "Distribution of hashcodes". When this string is non-null, a textual table of the
     * distribution will be printed to the terminal when the <code>report()</code> method is called.
     */
    protected String distribname;

    /**
     * This is the public constructor.
     */
    public Distribution(String name) {
        super(name);
        distribMin = observedMinimum;
    }

    /**
     * Public constructor initializes the statistics for a sequence of integers.
     */
    public Distribution(String newname, String tn, String cn) {
        super(newname, tn, cn);
        distribMin = observedMinimum;
    }

    /**
     * Public constructor initializes the statistics for a sequence of integers.
     */
    public Distribution(String newname, String tn, String cn, String dn) {
        super(newname, tn, cn);
        distribname = dn;
        distribMin = observedMinimum;
    }

    /**
     * Record the next value and update internal state.
     */
    public synchronized void record(int value) {

        if (!someData) {
            distrib = new int[1];
            distrib[0] = 1;
            distribMin = value;
            super.record(value);
            return;
        }

        int oldMax = observedMaximum, oldMin = observedMinimum;
        super.record(value);

        if (observedMaximum > oldMax) recomputeMaxDistrib(observedMaximum);
        else if (observedMinimum < oldMin) recomputeMinDistrib(observedMinimum);
        else incrementDistrib(value);
    }

    /**
     * Generate a textual report of the data gathered.
     * @param printer
     */
    public void print(Printer printer) {
        printer.print("\n " + name);
        printer.print("\n---------------------------------------------------------------------\n");

        if (totalname != null) {
            printer.print("   " + totalname + ": " + total);
        }
        if (cumulname != null) {
            printer.print("   " + cumulname + ": " + accumulation);
        }

        printer.print("\n Statistics: ");
        printer.print("\n   Minimum: " + observedMinimum + ", " + countMinimum + " occurences of min.");
        printer.print("\n   Maximum: " + observedMaximum + ", " + countMaximum + " occurences of max.");
        printer.print("\n   Mean: " + mean + ", Median: " + median + '\n');

        if (distribname != null) {
            printer.print("\n Distribution: ");
            printDistribution(printer, distribMin, distrib);
        }
    }

    /**
     * PrintVST the distribution using stars
     */
    protected void printDistribution(Printer printer, int base, int[] data) {
        int cntr, max;
        float scale = 1;
        int num = data.length;

        if (num == 0) {
            printer.print("\n");
            return;
        }

        for (max = data[0], cntr = 0; cntr < data.length; cntr++) {
            if (data[cntr] > max) max = data[cntr];
        }

        if (max > 70) scale = ((float) max) / 70;

        // loop through occurrences and print distribution
        for (cntr = 0; cntr < num; cntr++) {
            float fstars = ((float) data[cntr]) / scale;
            int stars = (int) fstars;
            if ((fstars - stars) >= 0.5) stars++;

            // collapse a string of redundant 0's.
            if (data[cntr] == 0) {
                if (cntr > 0 && cntr < num - 1) {
                    if ((data[cntr - 1] == 0) && (data[cntr + 1] == 0)) {
                        printer.print("\n   . . .");
                        while ((data[cntr + 1] == 0) && (cntr < data.length - 1)) cntr++;
                    }
                }
            }

            printer.print("\n   " + (base + cntr) + ": " + data[cntr] + " \t");

            for (int scntr = 0; scntr < stars; scntr++) {
                printer.print("*");
            }
        }
        printer.print("\n");
    }

    public void expandInterval(int min, int max) {
        // TODO: this will not work unless there is data already present
        if (distribMin < min) min = distribMin;
        if (max < distribMin + distrib.length) max = distribMin + distrib.length;

        int newsize = max - min + 1;

        if (newsize == distrib.length) return;

        int[] newdistrib = new int[newsize];

        // copy the old data to the new array
        System.arraycopy(distrib, 0, newdistrib, distribMin - min, distrib.length);

        distrib = newdistrib;
    }


    /**
     * Increment the number of occurrences for a particular integer.
     */
    protected void incrementDistrib(int value) {
        distrib[value - distribMin]++;
    }

    /**
     * Resize the occurrence table with a new maximum value.
     */
    protected void recomputeMaxDistrib(int newMax) {
        int newsize = 1 + newMax - distribMin;
        int[] newdistrib = new int[newsize];

        // copy the old data to the new array
        System.arraycopy(distrib, 0, newdistrib, 0, distrib.length);

        newdistrib[newsize - 1] = 1;
        distrib = newdistrib;
    }

    /**
     * Resize the occurences table with the given new minimum.
     */
    protected void recomputeMinDistrib(int newMin) {
        int mindiff = distribMin - newMin;
        int newsize = mindiff + distrib.length;
        int[] newdistrib = new int[newsize];

        // copy the old data to the new array
        System.arraycopy(distrib, 0, newdistrib, mindiff, distrib.length);

        newdistrib[0] = 1;
        distrib = newdistrib;
        distribMin = newMin;
    }

    public void process() {
        super.process();
        int mid = total / 2;
        if ((distrib == null)) {
            median = 0;
            return;
        }
        if (mid == 0) median = observedMinimum;

        median = observedMinimum - 1;
        for (int cntr = 0; cntr < distrib.length; cntr++) {
            mid = mid - distrib[cntr];
            if (mid < 0) {
                median = cntr + distribMin;
                break;
            }
        }
    }

    /**
     * Merge this statistical information with another.
     */
    public MinMaxMean merge(MinMaxMean m) {
        throw Util.unimplemented();
    }
}
