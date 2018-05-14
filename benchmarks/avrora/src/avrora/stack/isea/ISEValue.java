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

package avrora.stack.isea;

import avrora.arch.legacy.LegacyRegister;

/**
 * The <code>ISEValue</code> class contains a collection of constants and methods relating to
 * the abstract values used in intraprocedural side effect analysis.
 *
 * @author Ben L. Titzer
 */
public class ISEValue {

    public static final byte UNKNOWN = -1;
    public static final byte SREG = 33;
    public static final byte EIMSK = 34;
    public static final byte TIMSK = 35;
    public static final byte R0 = 0;
    public static final byte R1 = 1;
    public static final byte R2 = 2;
    public static final byte R3 = 3;
    public static final byte R4 = 4;
    public static final byte R5 = 5;
    public static final byte R6 = 6;
    public static final byte R7 = 7;
    public static final byte R8 = 8;
    public static final byte R9 = 9;
    public static final byte R10 = 10;
    public static final byte R11 = 11;
    public static final byte R12 = 12;
    public static final byte R13 = 13;
    public static final byte R14 = 14;
    public static final byte R15 = 15;
    public static final byte R16 = 16;
    public static final byte R17 = 17;
    public static final byte R18 = 18;
    public static final byte R19 = 19;
    public static final byte R20 = 20;
    public static final byte R21 = 21;
    public static final byte R22 = 22;
    public static final byte R23 = 23;
    public static final byte R24 = 24;
    public static final byte R25 = 25;
    public static final byte R26 = 26;
    public static final byte R27 = 27;
    public static final byte R28 = 28;
    public static final byte R29 = 29;
    public static final byte R30 = 30;
    public static final byte R31 = 31;

    public static byte merge(byte b1, byte b2) {
        if ( b1 == b2 ) return b1;
        return UNKNOWN;
    }

    public static String toString(byte b1) {
        if ( b1 == UNKNOWN ) return "---";
        if ( b1 == SREG ) return "SR";
        if ( b1 == TIMSK ) return "TM";
        if ( b1 == EIMSK ) return "EM";
        if ( b1 < 32 && b1 >= 0) return "R"+b1;
        return "???";
    }

    public static LegacyRegister asRegister(byte val) {
        if ( val >= R0 && val <= R31 )
            return LegacyRegister.getRegisterByNumber(val - R0);
        return null;
    }

    public static int asIORegister(byte val) {
        switch ( val ) {
            case SREG:
                return ISEState.SREG_NUM;
                case EIMSK:
                return ISEState.EIMSK_NUM;
                case TIMSK:
                return ISEState.TIMSK_NUM;
        }
        return -1;
    }
}
