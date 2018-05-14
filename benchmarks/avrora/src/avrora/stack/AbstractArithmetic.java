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

package avrora.stack;

/**
 * The <code>AbstractArithmetic</code> arithmetic class implements operations that are useful for working on
 * abstract integers which are represented as characters. <br><br>
 * <p/>
 * The abstract values (e.g. register values) are represented as characters. Thus, an 8 bit register is
 * modelled using a 16-bit character. The upper 8 bits represent the "mask", those bits which are known. The
 * lower 8 bits represent the known bits of the value. Thus, if bit(regs[R], i+8) is set, then bit(R, i) is
 * known and its value is bit(regs[R], i). If bit(regs[R], i+8) is clear, then the value of bit(regs[R], i) is
 * unknown in this abstract value. <br><br>
 * <p/>
 * Since there are 3 possible values (on, off, unknown) for each bit in the abstract state and there are two
 * bits reserved for representing each of these states, there are 4 bit states to represent 3 values. We
 * canonicalize the values when the bit value is unknown, i.e. when the known mask bit is clear, then the
 * value bit is clear as well. This makes comparison of canonical abstract values the same as character
 * equality. All abstract values stored within <code>AbstractState</code> are canonical for efficiency and
 * clarity.
 *
 * @author Ben L. Titzer
 */
public class AbstractArithmetic {
    private static final char KNOWN_MASK = 0xFF00;
    private static final char BIT_MASK = 0x00FF;
    private static final int SHIFT = 8;

    /**
     * The <code>ZERO</code> field represents the abstract value where all bits are known to be zero.
     */
    public static final char ZERO = KNOWN_MASK;

    /**
     * The <code>TRUE</code> field represents the abstract bit that is known to be true.
     */
    public static final char TRUE = 0x101;

    /**
     * The <code>FALSE</code> field represents the abstract bit that is known to be false.
     */
    public static final char FALSE = 0x100;

    /**
     * The <code>UNKNOWN</code> field represents the abstract value where none of the bits are known.
     */
    public static final char UNKNOWN = 0;


    /**
     * The <code>merge()</code> method merges abstract values. The merge of two abstract values is defined
     * intuitively as the intersection of the known bits of the two values that agree, and all other bits are
     * unknown. This variant of the method accepts two concrete values to merge.
     *
     * @param cv1 the first (concrete) value to merge
     * @param cv2 the second (concrete) value to merge
     * @return the abstract value representing the results of merging the two values
     */
    public static char merge(byte cv1, byte cv2) {
        int mm = ~(cv1 ^ cv2);
        return canon((char)mm, (char)cv1);
    }

    /**
     * The <code>merge()</code> method merges abstract values. The merge of two abstract values is defined
     * intuitively as the intersection of the known bits of the two values that agree, and all other bits are
     * unknown. This variant of the method accepts one abstract value and one concrete value to merge
     * together.
     *
     * @param av1 the first (abstract) value to merge
     * @param cv2 the second (concrete) value to merge
     * @return the abstract value representing the results of merging the two values
     */
    public static char merge(char av1, byte cv2) {
        int mm = ~(knownBitsOf(av1) ^ cv2);
        return canon((char)(mm & maskOf(av1)), av1);
    }

    /**
     * The <code>merge()</code> method merges abstract values. The merge of two abstract values is defined
     * intuitively as the intersection of the known bits of the two values that agree, and all other bits are
     * unknown. This variant of the method accepts three concrete values to merge.
     *
     * @param cv1 the first (concrete) value to merge
     * @param cv2 the second (concrete) value to merge
     * @param cv3 the third (concrete) value to merge
     * @return the abstract value representing the results of merging the two values
     */
    public static char merge(byte cv1, byte cv2, byte cv3) {
        return merge(merge(cv1, cv2), cv3);
    }

    /**
     * The <code>merge()</code> method merges abstract values. The merge of two abstract values is defined
     * intuitively as the intersection of the known bits of the two values that agree, and all other bits are
     * unknown. This variant of the method accepts four concrete values to merge.
     *
     * @param cv1 the first (concrete) value to merge
     * @param cv2 the second (concrete) value to merge
     * @param cv3 the third (concrete) value to merge
     * @param cv4 the fourth (concrete) value to merge
     * @return the abstract value representing the results of merging the two values
     */
    public static char merge(byte cv1, byte cv2, byte cv3, byte cv4) {
        return merge(merge(cv1, cv2), merge(cv3, cv4));
    }

    /**
     * The <code>merge()</code> method merges abstract values. The merge of two abstract values is defined
     * intuitively as the intersection of the known bits of the two values that agree, and all other bits are
     * unknown. This variant of the method accepts two abstract values to merge.
     *
     * @param av1 the first (abstract) value to merge
     * @param av2 the second (abstract) value to merge
     * @return the abstract value representing the results of merging the two values
     */
    public static char merge(char av1, char av2) {
        if (av1 == av2) return av1;

        char v1k = maskOf(av1); // known mask of av1
        char v2k = maskOf(av2); // known mask of av2

        int mm = ~(knownBitsOf(av1) ^ knownBitsOf(av2)); // matched bits
        int rk = v1k & v2k & mm & 0xff; // known bits of result

        return canon((char)rk, av1);
    }

    /**
     * The <code>isKnown()</code> method tests whether an abstract value represents a single, fully known
     * value.
     *
     * @param av1 the abstract value to test
     * @return true if all of the bits of the abstract value are known; false if any bits are unknown
     */
    public static boolean isUnknown(char av1) {
        return (av1 & KNOWN_MASK) != KNOWN_MASK;
    }

    /**
     * The <code>areKnown()</code> method tests whether two abstract values each represent a single, fully
     * known value.
     *
     * @param av1 the first abstract value to test
     * @param av2 the second abstract value to test
     * @return true if all of the bits of the both abstract values are known; false if any bits are unknown
     */
    public static boolean areKnown(char av1, char av2) {
        return (av1 & av2 & KNOWN_MASK) == KNOWN_MASK;
    }

    /**
     * The <code>areEqual()</code> method tests whether two abstract values are equivalent in the "abstract
     * value" sense. Two abstract values are equivalent if their known bits are equal and their known masks
     * are equal
     *
     * @param val1 the first abstract value
     * @param val2 the second abstract value
     * @return true if the abstract values are equal; false otherwise
     */
    public static boolean areEqual(char val1, char val2) {
        if (val1 == val2) return true;
        return canon(val1) == canon(val2);
    }

    /**
     * The <code>canon()</code> method canonicalizes an abstract value. An abstract value is canonical if all
     * of its unknown bits are set to zero. This variant takes a single abstract value and ensures that it is
     * canonical.
     *
     * @param av1 the abstract value to canonicalize
     * @return the canonicalized representation of this abstract value
     */
    public static char canon(char av1) {
        int vk = av1 & KNOWN_MASK;
        return (char)((vk) | (av1 & (vk >> SHIFT)));
    }

    /**
     * The <code>canon()</code> method canonicalizes an abstract value. An abstract value is canonical if all
     * of its unknown bits are set to zero. This variant takes a mask and an abstract value and returns an
     * abstract value that is canonical with the specified known bit mask.
     *
     * @param mask the known bit mask to canonicalize with respect to
     * @param av1  the abstract value to canonicalize
     * @return the canonicalized representation of this abstract value
     */
    public static char canon(char mask, char av1) {
        return (char)((mask << SHIFT) | (av1 & mask));
    }

    /**
     * The <code>knownVal()</code> method creates a canonical abstract value from the given concrete value.
     *
     * @param cv1 the concrete value to create an abstract value for
     * @return a canonical abstract value representing the concrete value.
     */
    public static char knownVal(byte cv1) {
        return (char)(KNOWN_MASK | (cv1 & 0xff));
    }

    /**
     * The <code>knownBitsOf()</code> method returns computes the concrete value from the given abstract value
     * where all unknown bits of the abstract value are set to zero.
     *
     * @param val the abstract value to get the known bits of
     * @return a concrete value such that all unknown bits are set to zero
     */
    public static byte knownBitsOf(char val) {
        return (byte)(((val & KNOWN_MASK) >> SHIFT) & val);
    }

    /**
     * The <code>bitsOf()</code> method returns the lower 8 bits (the value bits) of the abstract value,
     * ignoring the known bit mask. For a canonical abstract value, this method will return the same result as
     * <code>knownBitsOf</code>, because, by definition, the unknown bits of a canonical abstract value are
     * set to zero.
     *
     * @param av1 the abstract value
     * @return the lower bits of the abstract value as a concrete value
     */
    public static char bitsOf(char av1) {
        return (char)(av1 & BIT_MASK);
    }

    /**
     * The <code>maskOf()</code> method returns the upper 8 bits of the abstract (the mask bits) of the
     * abstract value. This mask represents those bits that are known.
     *
     * @param av1 the abstract value
     * @return the mask of known bits of the abstract value
     */
    public static char maskOf(char av1) {
        return (char)((av1 & KNOWN_MASK) >> SHIFT);
    }

    /**
     * The <code>getBit()</code> method extracts the specified abstract bit from the specified abstract
     * value.
     *
     * @param av1 the abstract value
     * @param bit the bit number
     * @return <code>AbstractArithmetic.TRUE</code> if the bit is known to be on;
     *         <code>AbstractArithmetic.FALSE</code> if the bit is known to be off;
     *         <code>AbstractArithmetic.UNKNOWN</code> otherwise
     */
    public static char getBit(char av1, int bit) {
        return (char)((av1 >> bit) & TRUE);
    }

    /**
     * The <code>setBit()</code> method updates the specified abstract bit within the specified abstract
     * value.
     *
     * @param av1 the abstract value
     * @param bit the bit number
     * @param on  the new abstract value of the bit
     * @return a new abstract value where the specified bit has been replaced with the specified abstract
     *         value
     */
    public static char setBit(char av1, int bit, char on) {
        int mask = ~(TRUE << bit);
        return (char)((av1 & mask) | ((on & TRUE) << bit));
    }

    /**
     * The <code>couldBeZero</code> method performs a "fuzzy" equality test against zero for an abstract
     * value. It will return one of three values, depending on whether the specified abstract value is
     * definately zero, definately not zero, or unknown.
     *
     * @param av1 the abstract value
     * @return <code>AbstractArithmetic.TRUE</code> if the specified abstract value is definately zero;
     *         <code>AbstractArithmetic.FALSE</code> if the specified abstract value cannot possibly be zero
     *         (it has one bit that is known to be on); <code>AbstractArithmetic.UNKNOWN</code> otherwise
     */
    public static char couldBeZero(char av1) {
        if (av1 == ZERO) return TRUE;
        if (knownBitsOf(av1) != 0) return FALSE;
        return UNKNOWN;
    }

    /**
     * The <code>couldBeZero()</code> method performs a "fuzzy" equality test against zero for two abstract
     * values. It will return one of three values, depending on whether the specified abstract values are
     * definately zero, definately not zero, or unknown.
     *
     * @param av1 the first abstract value
     * @param av2 the second abstract value
     * @return <code>AbstractArithmetic.TRUE</code> if both abstract values are definately zero;
     *         <code>AbstractArithmetic.FALSE</code> if either of the specified abstract values cannot
     *         possibly be zero (it has one bit that is known to be on); <code>AbstractArithmetic.UNKNOWN</code>
     *         otherwise
     */
    public static char couldBeZero(char av1, char av2) {
        if (av1 == ZERO && av2 == ZERO) return TRUE;
        if (knownBitsOf(av1) != 0 || knownBitsOf(av2) != 0) return FALSE;
        return UNKNOWN;
    }

    /**
     * The <code>couldBeEqual()</code> method performs a "fuzzy" equality test between two abstract values. It
     * will return one of three values, depending on whether the abstract values are definately equal,
     * definately not equal, or unknown.
     *
     * @param av1 the first abstract value
     * @param av2 the second abstract value
     * @return <code>AbstractArithmetic.TRUE</code> if the abstract values are definately equal;
     *         <code>AbstractArithmetic.FALSE</code> if the abstract values are definately not equal;
     *         <code>AbstractArithmetic.UNKNOWN</code> otherwise
     */
    public static char couldBeEqual(char av1, char av2) {
        if (areKnown(av1, av2) && av1 == av2) return TRUE;
        if (knownBitsOf(av1) != knownBitsOf(av2)) return FALSE;
        return UNKNOWN;
    }

    /**
     * The <code>commonMask()</code> method computes the intersection of the known bit masks of two abstract
     * values.
     *
     * @param av1 the first abstract value
     * @param av2 the second abstract value
     * @return the intersection of the known bit masks of each abstract value
     */
    public static char commonMask(char av1, char av2) {
        return (char)(maskOf(av1) & maskOf(av2));
    }

    /**
     * The <code>commonMask()</code> method computes the intersection of the known bit masks of three abstract
     * values.
     *
     * @param av1 the first abstract value
     * @param av2 the second abstract value
     * @param av3 the third abstract value
     * @return the intersection of the known bit masks of each abstract value
     */
    public static char commonMask(char av1, char av2, char av3) {
        return (char)(maskOf(av1) & maskOf(av2) & maskOf(av3));
    }

    /**
     * The <code>logicalAnd</code> method computes the logical bitwise AND of two abstract values.
     *
     * @param av1 the first abstract value
     * @param av2 the second abstract value
     * @return an abstract value representing the bitwise AND of the two abstract value operands
     */
    public static char logicalAnd(char av1, char av2) {
        return (char)(av1 & av2 & TRUE);
    }

    /**
     * The <code>add()</code> method performs addition of two abstract values. It relies on the
     * <code>ceiling()</code> and <code>floor()</code> functions that allow abstract addition to be expressed
     * in terms of two concrete additions, resulting in a straightforward and clean implementation.
     *
     * @param av1 the first abstract value
     * @param av2 the second abstract value
     * @return an abstract value that represents the sum of the two abstract values.
     */
    public static char add(char av1, char av2) {
        char common = commonMask(av1, av2);

        if (areKnown(av1, av2)) // common case of all bits are known.
            return knownVal((byte)(bitsOf(av1) + bitsOf(av2)));

        int resultA = ceiling(av1) + ceiling(av2);
        int resultB = floor(av1) + floor(av2);

        return mergeMask(common, merge((byte)resultA, (byte)resultB));
    }

    /**
     * The <code>add()</code> method performs subtraction of two abstract values. It relies on the
     * <code>ceiling()</code> and <code>floor()</code> functions that allow abstract subtraction to be
     * expressed in terms of two concrete subtractions, resulting in a straightforward and clean
     * implementation.
     *
     * @param av1 the first abstract value
     * @param av2 the second abstract value
     * @return an abstract value that represents the difference of the two abstract values.
     */
    public static char subtract(char av1, char av2) {
        char common = commonMask(av1, av2);

        if (areKnown(av1, av2)) // common case of all bits are known.
            return knownVal((byte)(bitsOf(av1) - bitsOf(av2)));

        int resultA = ceiling(av1) - ceiling(av2);
        int resultB = floor(av1) - floor(av2);

        return mergeMask(common, merge((byte)resultA, (byte)resultB));
    }

    /**
     * The <code>increment()</code> method simply adds 1 to the abstract value. It is a special case of the
     * <code>add()</code> that is common enough to warrant its own method.
     *
     * @param av1 the abstract value
     * @return an abstract value that represents the sum of the specified abstract value and the known value
     *         1
     */
    public static char increment(char av1) {
        char mask = maskOf(av1);
        // TODO: optimize for common case of known / unknown values.
        int resultA = ceiling(av1) + 1;
        int resultB = floor(av1) + 1;
        return mergeMask(mask, merge((byte)resultA, (byte)resultB));
    }

    /**
     * The <code>decrement()</code> method simply subtracts 1 to the abstract value. It is a special case of
     * the <code>subtract()</code> that is common enough to warrant its own method.
     *
     * @param av1 the abstract value
     * @return an abstract value that represents the difference of the specified abstract value and the known
     *         value 1
     */
    public static char decrement(char av1) {
        char mask = maskOf(av1);
        // TODO: optimize for common case of known / unknown values.
        int resultA = ceiling(av1) - 1;
        int resultB = floor(av1) - 1;
        return mergeMask(mask, merge((byte)resultA, (byte)resultB));
    }

    /**
     * The <code>mergeMask()</code> merges the given abstract value with the known bit mask passed. This means
     * that the known bits will be the intersection of the known bits of the mask and the known bits of the
     * abstract value.
     *
     * @param mask the known bit mask
     * @param av1  the abstract value
     * @return an abstract value in which the known bit mask is the intersection of the given bit mask and the
     *         bit mask of the given abstract value
     */
    public static char mergeMask(char mask, char av1) {
        char common = (char)(mask & maskOf(av1));
        return canon(common, av1);
    }

    /**
     * The <code>xor()</code> method computes the bitwise exclusive or operation on the two given abstract
     * values.
     *
     * @param av1 the first abstract value
     * @param av2 the second abstract value
     * @return the bitwise exclusive or of the two abstract values
     */
    public static char xor(char av1, char av2) {
        char mask = commonMask(av1, av2);
        return canon(mask, (char)(av1 ^ av2));
    }

    /**
     * The <code>and()</code> method computes the logical bitwise AND of two abstract values.
     *
     * @param av1 the first abstract value
     * @param av2 the second abstract value
     * @return an abstract value representing the bitwise AND of the two abstract value operands
     */
    public static char and(char av1, char av2) {
        return (char)(av1 & av2);
    }

    /**
     * The <code>or()</code> method computes the logical bitwise or of two abstract values.
     *
     * @param av1 the first abstract value
     * @param av2 the second abstract value
     * @return an abstract value representing the bitwise inclusive or of the two abstract value operands
     */
    public static char or(char av1, char av2) {
        return canon(commonMask(av1, av2), (char)(av1 | av2));
    }

    /**
     * The <code>and()</code> method computes the logical bitwise AND of three abstract values.
     *
     * @param av1 the first abstract value
     * @param av2 the second abstract value
     * @param av3 the third abstract value
     * @return an abstract value representing the bitwise AND of the three abstract value operands
     */
    public static char and(char av1, char av2, char av3) {
        return (char)(av1 & av2 & av3);
    }

    /**
     * The <code>or()</code> method computes the logical bitwise or of three abstract values.
     *
     * @param av1 the first abstract value
     * @param av2 the second abstract value
     * @param av3 the third abstract value
     * @return an abstract value representing the bitwise inclusive or of the two abstract value operands
     */
    public static char or(char av1, char av2, char av3) {
        return canon(commonMask(av1, av2, av3), (char)(av1 | av2 | av3));
    }

    /**
     * The <code>not()</code> method computes the bitwise negation (one's complement) of the specified
     * abstract value
     *
     * @param av1 the abstract value
     * @return the abstract value representing the bitwise negation of the operand
     */
    public static char not(char av1) {
        return canon((char)(av1 ^ 0xff));
    }

    /**
     * The <code>ceiling()</code> function computes the concrete value with all unknown bits set to one. This
     * is useful for implementation of some arithmetic operations.
     *
     * @param av1 the abstract value to compute the ceiling of
     * @return a concrete value where each of the unknown bits of the abstract value are set to one
     */
    public static int ceiling(char av1) {
        int invmask = (~maskOf(av1)) & 0xff;
        return bitsOf(av1) | invmask;
    }

    /**
     * The <code>ceiling()</code> function computes the concrete value with all unknown bits set to one. This
     * is useful for implementation of some arithmetic operations. This variant takes two abstract values
     * representing the lower and upper bytes of a word and returns a concrete unsigned 16-bit word
     * representing the ceiling function.
     *
     * @param av1 the abstract value representing the lower byte
     * @param av2 the abstract value representing the high byte
     * @return a concrete word value where each of the unknown bits of the abstract value are set to one
     */
    public static int ceiling(char av1, char av2) {
        return ceiling(av1) | (ceiling(av2) << 8);
    }

    /**
     * The <code>floor()</code> function computes the concrete value with all unknown bits set to zero. This
     * is useful for implementation of some arithmetic operations.
     *
     * @param av1 the abstract value to compute the ceiling of
     * @return a concrete value where each of the unknown bits of the abstract value are set to zero
     */
    public static int floor(char av1) {
        return bitsOf(av1);
    }

    /**
     * The <code>floor()</code> function computes the concrete value with all unknown bits set to zero. This
     * is useful for implementation of some arithmetic operations. This variant takes two abstract values
     * representing the lower and upper bytes of a word and returns a concrete unsigned 16-bit word
     * representing the floor function.
     *
     * @param av1 the abstract value representing the lower byte
     * @param av2 the abstract value representing the high byte
     * @return a concrete word value where each of the unknown bits of the abstract value are set to zero
     */
    public static int floor(char av1, char av2) {
        return bitsOf(av1) | (bitsOf(av2) << 8);
    }

    /**
     * The <code>shiftLeftOne()</code> method shifts the abstract value left by one bit.
     *
     * @param av1 the abstract value
     * @return an abstract value representing the operand shifted left by one and the lower bit is set to
     *         known zero
     */
    public static char shiftLeftOne(char av1) {
        return (char)(((av1 & 0x7f7f) << 1) | FALSE);
    }

    /**
     * The <code>shiftLeftOne()</code> method shifts the abstract value left by one bit and sets the lowest
     * bit to the given value.
     *
     * @param av1    the abstract value
     * @param lowbit the value of the lowest bit
     * @return an abstract value representing the operand shifted left by one and the lower bit is set to the
     *         given value
     */
    public static char shiftLeftOne(char av1, char lowbit) {
        return (char)(((av1 & 0x7f7f) << 1) | (lowbit & TRUE));
    }

    /**
     * The <code>toString()</code> method converts an 8-bit abstract value to a string representation. Each
     * bit's value is represented as either '0', '1', or '.' and listed with the most significant first.
     *
     * @param av1 the abstract value to convert to a string
     * @return a string representation of the abstract value
     */
    public static String toString(char av1) {
        StringBuffer buf = new StringBuffer(9);
        toString(av1, buf);
        return buf.toString();
    }

    /**
     * The <code>toShortString()</code> method converts an 8-bit abstract value to a string representation.
     * Each bit's value is represented as either '0', '1', or '.' and listed with the most significant first.
     *
     * @param av1 the abstract value to convert to a string
     * @return a string representation of the abstract value
     */
    public static String toShortString(char av1) {
        if (av1 == ZERO) return "0";
        StringBuffer buf = new StringBuffer(9);
        toString(av1, buf);
        return buf.toString();
    }

    /**
     * The <code>toString()</code> method converts an 1-bit abstract value to a string representation. The
     * bit's value is represented as either '0', '1', or '.'.
     *
     * @param av1 the abstract bit to convert to a string
     * @return a character representation of the abstract bit
     */
    public static char bitToChar(char av1) {
        switch (av1) {
            case TRUE:
                return '1';
            case FALSE:
                return '0';
            default:
                return '.';
        }
    }

    /**
     * The <code>toString()</code> method converts an 8-bit abstract value to a string representation and
     * appends it to the end of the given string buffer. Each bit's value is represented as either '0', '1',
     * or '.' and listed with the most significant first.
     *
     * @param av1 the abstract value to convert to a string
     * @param buf the string buffer to append the result to
     */
    public static void toString(char av1, StringBuffer buf) {
        for (int cntr = 7; cntr >= 0; cntr--) {
            buf.append(bitToChar(getBit(av1, cntr)));
        }
    }
}
