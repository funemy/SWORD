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
 * Creation date: Apr 23, 2007
 */

package cck.util;

/**
 * The <code>TimeUtil</code> class implements a number of utilities that are related
 * to converting, rendering, and manipulating quantities that represent time.
 *
 * @author Ben L. Titzer
 */
public class TimeUtil {

    public static final int[] DENOM = {24, 60, 60, 1000};
    public static final int[] DAYSECS = {60, 60};
    public static final int SECS_PER_DAY = 3600 * 24;
    public static final int SECS_PER_HOUR = 3600;
    public static final int SECS_PER_MIN = 60;
    public static final long MILLISECS_PER_DAY = 3600 * 24 * 1000;
    public static final long MILLISECS_PER_HOUR = 3600 * 1000;
    public static final long MILLISECS_PER_MIN = 60 * 1000;
    public static final long MILLISECS_PER_SEC = 1000;
    public static final int DAYS = 0;
    public static final int HOURS = 1;
    public static final int MINS = 2;
    public static final int SECS = 3;
    public static final int MILLIS = 4;

    public static String milliToSecs(long millis) {
        long secs = millis / 1000;
        millis = millis % 1000;
        StringBuffer buf = new StringBuffer(10);
        buf.append(secs);
        buf.append('.');
        zeropad3(millis, buf);
        return buf.toString();
    }

    /**
     * The <code>millisToDays()</code> method converts the given milliseconds into a breakdown of days, hours,
     * minutes, seconds, and milliseconds, returning a long array where the expr 0 corresponds to days, expr 1
     * corresponds to hours, etc.
     *
     * @param millis the number of milliseconds to convert
     * @return the breakdown of milliseconds into days, hours, minutes, seconds, and milliseconds in an array,
     *         with most significant units first
     */
    public static long[] millisToDays(long millis) {
        return Arithmetic.modulus(millis, DENOM);
    }

    public static void appendSecs(StringBuffer buf2, long seconds) {
        long[] res = Arithmetic.modulus(seconds, DAYSECS);
        for (int cntr = 0; cntr < res.length; cntr++) {
            if (cntr > 0) {
                // print separator and zero pad.
                buf2.append(':');
                zeropad2(res[cntr], buf2);
            } else {
                // just print the integer.
                buf2.append(res[cntr]);
            }
        }
    }

    public static long millisToCycles(double millis, double hz) {
        return (long)(hz * millis / 1000);
    }

    public static double cyclesToMillis(long cycles, double hz) {
        return 1000 * ((double)cycles) / hz;
    }

    public static long secondsToCycles(double secs, double hz) {
        return (long)(hz * secs);
    }

    public static double cyclesToSeconds(long cycles, double hz) {
        return ((double)cycles) / hz;
    }

    protected static void zeropad3(long v, StringBuffer buf) {
        if (v < 100) buf.append('0');
        if (v < 10) buf.append('0');
        buf.append(v);
    }

    protected static void zeropad2(long v, StringBuffer buf) {
        if (v < 10) buf.append('0');
        buf.append(v);
    }
}
