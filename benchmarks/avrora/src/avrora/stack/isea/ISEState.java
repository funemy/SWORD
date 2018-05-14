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
import cck.util.Util;

/**
 * @author Ben L. Titzer
 */
public class ISEState extends ISEAbstractState {

    public static final int NUM_REGISTERS = 32;
    public static final int SREG_NUM = 0x3f;
    public static final int EIMSK_NUM = 0x39;
    public static final int TIMSK_NUM = 0x37;
    public static final int SREG_OFF = NUM_REGISTERS;
    public static final int EIMSK_OFF = NUM_REGISTERS + 1;
    public static final int TIMSK_OFF = NUM_REGISTERS + 2;
    public static final int MAX_STACK = 40;

    protected static final ISEAbstractState.Element[] defaultElements;
    protected static final byte[] defaultStack;

    static {
        defaultElements = new ISEAbstractState.Element[NUM_REGISTERS+3];
        int cntr = 0;
        for ( ; cntr < NUM_REGISTERS; cntr++ ) {
            defaultElements[cntr] = new ISEAbstractState.Element("R"+cntr, (byte)(ISEValue.R0+cntr), false);
        }
        defaultElements[SREG_OFF] = new ISEAbstractState.Element("SR", ISEValue.SREG, false);
        defaultElements[EIMSK_OFF] = new ISEAbstractState.Element("EM", ISEValue.EIMSK, false);
        defaultElements[TIMSK_OFF] = new ISEAbstractState.Element("TM", ISEValue.TIMSK, false);
        defaultStack = new byte[MAX_STACK];
    }

    public ISEState() {
        super(defaultElements, defaultStack, 0);
    }

    public ISEState(ISEState r) {
        super(r.elements, r.stack, r.depth);
    }

    public byte readRegister(LegacyRegister r) {
        byte value = getElement(r.getNumber());
        int off = getElemOffset(value);
        if ( off != -1 ) elements[off].read = true;
        return value;
    }

    public byte getRegister(LegacyRegister r) {
        return getElement(r.getNumber());
    }

    public void writeRegister(LegacyRegister r, byte val) {
        writeElement(r.getNumber(), val);
    }

    public boolean isRegisterRead(LegacyRegister reg) {
        return elements[reg.getNumber()].read;
    }

    public byte readIORegister(int reg) {
        switch ( reg ) {
            case SREG_NUM: return readElement(SREG_OFF);
            case EIMSK_NUM: return readElement(EIMSK_OFF);
            case TIMSK_NUM: return readElement(TIMSK_OFF);
        }
        return ISEValue.UNKNOWN;
    }

    public boolean isIORegisterRead(int ior) {
        switch ( ior ) {
            case SREG_NUM: return elements[SREG_OFF].read;
            case EIMSK_NUM: return elements[EIMSK_OFF].read;
            case TIMSK_NUM: return elements[TIMSK_OFF].read;
        }
        return true;
    }

    public void writeIORegister(int reg, byte val) {
        switch ( reg ) {
            case SREG_NUM:
                writeElement(SREG_OFF, val);
                break;
            case EIMSK_NUM:
                writeElement(EIMSK_OFF, val);
                break;
            case TIMSK_NUM:
                writeElement(TIMSK_OFF, val);
                break;
        }
    }

    public byte getSREG() {
        return readElement(SREG_OFF);
    }

    public void writeSREG(byte val) {
        writeElement(SREG_OFF, val);
    }

    public void mergeWithCaller(ISEState caller) {
        for ( int cntr = 0; cntr < elements.length; cntr++ ) {
            elements[cntr].value = computeValue(cntr, caller);
        }

        for ( int cntr = 0; cntr < elements.length; cntr++ ) {
            if ( !elements[cntr].read )
                elements[cntr].read = computeNewRead(cntr, caller);
        }

        if ( depth != 0)
            throw Util.failure("return with nonzero stack height");

        System.arraycopy(caller.stack, 0, stack, 0, caller.depth);
        depth = caller.depth;
    }

    private byte computeValue(int elem, ISEState caller) {
        Element relem = elements[elem];
        int origElem = getElemOffset(relem.value);

        if ( origElem >= 0 ) {
            Element oelem = caller.elements[origElem];
            return oelem.value;
        }
        return relem.value;
    }

    private boolean computeNewRead(int elem, ISEState caller) {
        Element relem = elements[elem];
        int origElem = getElemOffset(relem.value);

        if ( origElem >= 0 ) {
            Element oelem = caller.elements[origElem];
            return oelem.read;
        }
        return false;
    }


    private int getElemOffset(byte value) {
        int off = -1;
        if ( value >= ISEValue.R0 && value <= ISEValue.R31 ) {
            off = value - ISEValue.R0;
        } else {
            switch ( value ) {
                case ISEValue.SREG: off = SREG_OFF; break;
                case ISEValue.EIMSK: off = EIMSK_OFF; break;
                case ISEValue.TIMSK: off = TIMSK_OFF; break;
            }
        }
        return off;
    }

    public ISEState dup() {
        return new ISEState(this);
    }

}
