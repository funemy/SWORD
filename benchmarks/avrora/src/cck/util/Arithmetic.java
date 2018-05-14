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

package cck.util;

/**
 * The <code>Arithmetic</code> class implements a set of useful methods that are used by the simulator and
 * assembler for converting java types to various data types used by the machine.
 *
 * @author Ben L. Titzer
 */
public class Arithmetic {
    public static short word(byte b1, byte b2) {
        return (short) ((b1 & 0xff) | (b2 << 8));
    }

    public static char uword(byte b1, byte b2) {
        return (char) ((b1 & 0xff) | ((b2 & 0xff) << 8));
    }

    public static int signExtend(int value, int signbit) {
        if (getBit(value, signbit)) {
            return value | 0xffffffff << signbit;
        }
        return value;
    }

    public static char ubyte(byte b1) {
        return (char) (b1 & 0xff);
    }

    public static byte low(short val) {
        return (byte) val;
    }

    public static byte high(short val) {
        return (byte) (val >> 8);
    }

    public static byte low(int val) {
        return (byte) val;
    }

    public static byte high(int val) {
        return (byte) ((val & 0xff00) >> 8);
    }

    public static char ulow(char val) {
        return (char) (val & 0xff);
    }

    public static char uhigh(char val) {
        return (char) (val >> 8);
    }

    public static char ulow(short val) {
        return (char) (val & 0xff);
    }

    public static char uhigh(short val) {
        return (char) ((val & 0xff00) >> 8);
    }

    public static boolean getBit(byte val, int bit) {
        return (val & (1 << bit)) != 0;
    }

    public static boolean getBit(long val, int bit) {
        return (val & (((long) 1) << bit)) != 0;
    }

    public static boolean getBit(int val, int bit) {
        return (val & (1 << bit)) != 0;
    }

    public static byte setBit(byte val, int bit) {
        return (byte) (val | (1 << bit));
    }

    public static byte setBit(byte val, int bit, boolean on) {
        if (on) return setBit(val, bit);
        else return clearBit(val, bit);
    }

    public static int setBit(int val, int bit, boolean on) {
        int mask = (1 << bit);
        return on ? (val | mask) : (val & ~mask);
    }

    public static long setBit(long val, int bit, boolean on) {
        long mask = (((long) 1) << bit);
        return on ? (val | mask) : (val & ~mask);
    }

    public static byte clearBit(byte val, int bit) {
        return (byte) (val & ~(1 << bit));
    }

    public static int lowestBit(long value) {
        int low = 0;

        if (((int) value) == 0) {
            low += 32;
            value = value >> 32;
        }
        if ((value & 0xFFFF) == 0) {
            low += 16;
            value = value >> 16;
        }
        if ((value & 0xFF) == 0) {
            low += 8;
            value = value >> 8;
        }
        if ((value & 0xF) == 0) {
            low += 4;
            value = value >> 4;
        }
        if ((value & 0x3) == 0) {
            low += 2;
            value = value >> 2;
        }
        if ((value & 0x1) == 0) {
            low += 1;
            value = value >> 1;
        }

        return (value == 0) ? -1 : low;
    }

    public static int highestBit(int value) {
        int high = 31;

        if ((value & 0xFFFF0000) == 0) {
            high -= 16;
            value = value << 16;
        }
        if ((value & 0xFF000000) == 0) {
            high -= 8;
            value = value << 8;
        }
        if ((value & 0xF0000000) == 0) {
            high -= 4;
            value = value << 4;
        }
        if ((value & 0xC0000000) == 0) {
            high -= 2;
            value = value << 2;
        }
        if ((value & 0x80000000) == 0) {
            high -= 1;
            value = value << 1;
        }

        return (value == 0) ? -1 : high;
    }

    public static int log(int value) {
        if ( value == 0 ) return 0;
        return 1 + highestBit(value - 1);
    }

    /**
     * The <code>getBitField()</code> method reads a bit field from a value where the bits of the field
     * are not consecutive or in order. This method accepts an array of integers that denotes the permutation
     * of the bit field within the given value from highest order bits to lowest order bits
     *
     * @param value       the integer value to extract the bit field from
     * @param permutation the permutation of the bits for which to extract the bit field from highest order bit to
     *                    lowest order bit
     * @return the integer value resulting from concatentating the bits: <code>value[permutation[0]]</code>,
     *         <code>value[permutation[1]]</code>, etc into a single integer value
     */
    public static int getBitField(int value, int[] permutation) {
        int result = 0;
        for (int cntr = 0; cntr < permutation.length; cntr++) {
            result = result << 1;
            if (getBit(value, permutation[cntr])) result |= 1;
        }
        return result;
    }

    public static int getBitField(int value, int lowbit, int length) {
        return value >> lowbit & (0xffffffff >>> (32 - length));
    }

    // bit patterns for reversing the order of bits of a 4 bit quantity.
    private static final int[] reverseKey = {0, 8, 4, 12, 2, 10, 6, 14, 1, 9, 5, 13, 3, 11, 7, 15};

    public static byte reverseBits(byte value) {
        return (byte) (reverseKey[value & 0x0f] << 4 | reverseKey[(value >> 4) & 0x0f]);
    }

    // key for the number of bits set to one in a 4 bit quantity
    private static final int[] bitcountKey = {0, 1, 1, 2, 1, 2, 2, 3, 1, 2, 2, 3, 2, 3, 3, 4};

    public static int bitCount(byte value) {
        return bitcountKey[value & 0x0f] + bitcountKey[(value >> 4) & 0x0f];
    }

    public static int getSingleBitMask(int bit) {
        return 1 << bit;
    }

    public static int getSingleInverseBitMask(int bit) {
        return ~(1 << bit);
    }

    public static int getBitRangeMask(int low, int high) {
        return (0xffffffff >>> (31 - (high - low))) << low;
    }

    public static int getBitMask(int width) {
        return (0xffffffff >>> (31 - (width - 1)));
    }

    public static long getLongBitRangeMask(int low, int high) {
        return (-1L >>> (63 - (high - low))) << low;
    }

    public static int getInverseBitRangeMask(int low, int high) {
        return ~getBitRangeMask(low, high);
    }

    public static long[] modulus(long val, int[] denom) {
        long[] result = new long[denom.length + 1];
        for (int cntr = denom.length - 1; cntr >= 0; cntr--) {
            int radix = denom[cntr];
            result[cntr + 1] = (val % radix);
            val = val / radix;
        }
        result[0] = val;
        return result;
    }

    public static long mult(long[] vals, int[] denom) {
        long accum = 0;
        int radix = 1;
        assert (vals.length - 1 == denom.length);
        for (int cntr = 0; cntr < vals.length - 1; cntr++) {
            accum += vals[cntr] * radix;
            radix = radix * denom[cntr];
        }
        accum += vals[vals.length - 1] * radix;
        return accum;
    }

    public static void inc(long[] vals, int[] denom, int pos) {
        assert (vals.length - 1 == denom.length);

        for (int cntr = pos; cntr < vals.length; cntr++) {
            vals[cntr]++;
            // is there a carry out?
            if (vals[cntr] < denom[cntr]) break;
        }
    }

    public static int max(int m1, int m2) {
        return m1 > m2 ? m1 : m2;
    }

    public static int min(int m1, int m2) {
        return m1 < m2 ? m1 : m2;
    }

    public static byte packBits(boolean b0, boolean b1, boolean b2, boolean b3,
                                boolean b4, boolean b5, boolean b6, boolean b7) {
        int val = 0;
        if ( b0 ) val |= 0x1;
        if ( b1 ) val |= (0x1 << 1);
        if ( b2 ) val |= (0x1 << 2);
        if ( b3 ) val |= (0x1 << 3);
        if ( b4 ) val |= (0x1 << 4);
        if ( b5 ) val |= (0x1 << 5);
        if ( b6 ) val |= (0x1 << 6);
        if ( b7 ) val |= (0x1 << 7);
        return (byte)val;
    }

    public static int roundup(int val, int den) {
        return (val + den - 1) / 8;
    }
}
