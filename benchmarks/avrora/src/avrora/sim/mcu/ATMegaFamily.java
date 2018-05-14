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

package avrora.sim.mcu;

import avrora.arch.avr.AVRProperties;
import avrora.sim.*;
import avrora.sim.clock.ClockDomain;
import avrora.sim.state.BooleanView;
import cck.util.Arithmetic;

/**
 * The <code>ATMegaFamily</code> class encapsulates much of the common functionality among the
 * ATMega family microcontrollers from Atmel.
 *
 * @author Ben L. Titzer
 */
public abstract class ATMegaFamily extends AtmelMicrocontroller {

    public static class FlagBit implements InterruptTable.Notification {
        final AtmelInterpreter interpreter;
        final boolean autoclear;
        final int inum;
        boolean val;

        public FlagBit(AtmelInterpreter i, boolean auto, int in) {
            interpreter = i;
            autoclear = auto;
            inum = in;
            interpreter.getInterruptTable().registerInternalNotification(this, inum);
        }

        public void flag() {
            val = true;
            interpreter.setPosted(inum, true);
        }

        public void unflag() {
            val = false;
            interpreter.setPosted(inum, false);
        }

        public boolean get() {
            return val;
        }

        public void force(int inum) {
            val = true;
            interpreter.setPosted(inum, true);
        }

        public void invoke(int inum) {
            if (autoclear) {
                val = false;
                interpreter.setPosted(inum, false);
            }
        }
    }

    // TODO: migrate flag register to use InterruptFlag
    public static class FlagRegister extends RWRegister {

        class Notification implements InterruptTable.Notification {
            final int bit;

            Notification(int bit) {
                this.bit = bit;
            }

            public void force(int inum) {
                value = Arithmetic.setBit(value, bit, true);
            }

            public void invoke(int inum) {
                value = Arithmetic.setBit(value, bit, false);
                interpreter.setPosted(inum, false);
            }
        }

        /**
         * The <code>mapping</code> array maps a bit number (0-7) to an interrupt number (0-35). This is used
         * for calculating the posted interrupts.
         */
        protected final int[] mapping;
        protected final AtmelInterpreter interpreter;

        public FlagRegister(AtmelInterpreter interp, int[] map) {
            mapping = map;
            interpreter = interp;
            InterruptTable it = interpreter.getInterruptTable();
            for (int cntr = 0; cntr < 8; cntr++) {
                if (mapping[cntr] > 0) it.registerInternalNotification(new Notification(cntr), mapping[cntr]);
            }
        }

        public void write(byte val) {
            value = (byte) (value & ~val);
            for (int cntr = 0; cntr < 8; cntr++) {
                // do nothing for zero bits
                if (!Arithmetic.getBit(val, cntr)) continue;
                setPosted(cntr, false);
            }
        }

        private void setPosted(int inum, boolean p) {
            if (mapping[inum] > 0) interpreter.setPosted(mapping[inum], p);
        }

        public void flagBit(int bit) {
            value = Arithmetic.setBit(value, bit);
            setPosted(bit, true);
        }

        public void unflagBit(int bit) {
            value = Arithmetic.clearBit(value, bit);
            setPosted(bit, false);
        }

    }

    public static class MaskRegister extends RWRegister {

        /**
         * The <code>mapping</code> array maps a bit number (0-7) to an interrupt number (0-35). This is used
         * for calculating the posted interrupts.
         */
        protected final int[] mapping;
        protected final AtmelInterpreter interpreter;

        public MaskRegister(AtmelInterpreter interp, int[] map) {
            mapping = map;
            interpreter = interp;
        }

        public void write(byte val) {
            value = val;
            for (int cntr = 0; cntr < 8; cntr++) {
                setEnabled(cntr, Arithmetic.getBit(val, cntr));
            }
        }

        void setEnabled(int cntr, boolean e) {
            if (mapping[cntr] > 0) interpreter.setEnabled(mapping[cntr], e);
        }

    }

    /**
     * The <code>DirectionRegister</code> class implements an active register that sets the output
     * direction of the general purpose IO pins which are present on the ATMega series.
     */
    public static class DirectionRegister extends RWRegister {

        protected ATMegaFamily.Pin[] pins;

        protected DirectionRegister(ATMegaFamily.Pin[] p) {
            pins = p;
        }

        public void write(byte val) {
            for (int cntr = 0; cntr < 8; cntr++)
                pins[cntr].setOutputDir(Arithmetic.getBit(val, cntr));
            value = val;
        }

    }

    /**
     * The <code>PortRegister</code> class implements an active register that acts as the
     * write register (output register) for the general purpose IO pins.
     */
    public static class PortRegister extends RWRegister {
        protected ATMegaFamily.Pin[] pins;

        protected PortRegister(ATMegaFamily.Pin[] p) {
            pins = p;
        }

        public void write(byte val) {
            for (int cntr = 0; cntr < 8; cntr++)
                pins[cntr].write(Arithmetic.getBit(val, cntr));
            value = val;
        }

    }

    /**
     * The <code>PinRegister</code> class implements an active register that acts as the
     * read register (input register) for the general purpose IO pins.
     */
    public static class PinRegister implements ActiveRegister {
        protected ATMegaFamily.Pin[] pins;

        protected PinRegister(ATMegaFamily.Pin[] p) {
            pins = p;
        }

        public byte read() {
            int value = 0;
            value |= pins[0].read() ? 1 << 0 : 0;
            value |= pins[1].read() ? 1 << 1 : 0;
            value |= pins[2].read() ? 1 << 2 : 0;
            value |= pins[3].read() ? 1 << 3 : 0;
            value |= pins[4].read() ? 1 << 4 : 0;
            value |= pins[5].read() ? 1 << 5 : 0;
            value |= pins[6].read() ? 1 << 6 : 0;
            value |= pins[7].read() ? 1 << 7 : 0;
            return (byte) value;
        }

        public void write(byte val) {
            // ignore writes.
        }

    }

    protected ATMegaFamily(ClockDomain cd, AVRProperties p, FiniteStateMachine fsm) {
        super(cd, p, fsm);
    }

    protected static final int[] periods0 = {0, 1, 8, 32, 64, 128, 256, 1024};

    /**
     * <code>Timer0</code> is the default 8-bit timer on the ATMega128.
     */
    protected class Timer0 extends Timer8Bit {

        protected Timer0() {
            super(ATMegaFamily.this, 0, 1, 0, 1, 0, periods0);
            installIOReg("ASSR", new ASSRRegister());
        }

        // See pg. 104 of the ATmega128 doc
        protected class ASSRRegister extends RWRegister {
            static final int AS0 = 3;
            static final int TCN0UB = 2;
            static final int OCR0UB = 1;
            static final int TCR0UB = 0;

            public void write(byte val) {
                super.write((byte) (0xf & val));
                decode(val);
            }

            protected void decode(byte val) {
                // TODO: if there is a change, remove ticker and requeue?
                timerClock = Arithmetic.getBit(val, AS0) ? externalClock : mainClock;
            }


        }


    }

    protected static final int[] periods2 = {0, 1, 8, 64, 256, 1024, 0, 0};

    /**
     * <code>Timer2</code> is an additional 8-bit timer on the ATMega128. It is not available in
     * ATMega103 compatibility mode.
     */
    protected class Timer2 extends Timer8Bit {
        protected Timer2() {
            super(ATMegaFamily.this, 2, 7, 6, 7, 6, periods2);
        }
    }

    protected static final int[] periods1 = new int[]{0, 1, 8, 64, 256, 1024};

    /**
     * <code>Timer1</code> is a 16-bit timer available on the ATMega128.
     */
    protected class Timer1 extends Timer16Bit {

        protected void initValues() {
            // bit numbers
            OCIEnA = 4;
            OCIEnB = 3;
            OCIEnC = 0;// on ETIMSK
            TOIEn = 2;
            TOVn = 2;
            OCFnA = 4;
            OCFnB = 3;
            OCFnC = 0;// on ETIFR
            ICFn = 5;

            periods = periods1;
          
            // all bits are on TIFR/TIMSK
            xTIFR_reg = TIFR_reg;
            xTIMSK_reg = TIMSK_reg;
            // except for OCIE1C and OCF1C
            cTIFR_reg = ETIFR_reg;
            cTIMSK_reg = ETIMSK_reg;
        }

        protected Timer1(int compareUnits) {
            super(1, compareUnits, ATMegaFamily.this);
        }

    }

    protected static final int[] periods3 = {0, 1, 8, 64, 256, 1024};

    /**
     * <code>Timer3</code> is an additional 16-bit timer available on the ATMega128, but not in ATMega103
     * compatability mode.
     */
    protected class Timer3 extends Timer16Bit {

        protected void initValues() {
            // bit numbers
            OCIEnA = 4; // all OCIEn on ETIMSK
            OCIEnB = 3;
            OCIEnC = 1;
            TOIEn = 2;
            TOVn = 2;
            OCFnA = 4; // on OFCn on ETIFR
            OCFnB = 3;
            OCFnC = 1;
            ICFn = 5;

            periods = periods3;
          
            // all bits are on ETIFR/ETIMSK
            xTIFR_reg = ETIFR_reg;
            xTIMSK_reg = ETIMSK_reg;
            cTIFR_reg = ETIFR_reg;
            cTIMSK_reg = ETIMSK_reg;
        }

        protected Timer3(int compareUnits) {
            super(3, compareUnits, ATMegaFamily.this);
        }

    }

    /**
     * The <code>buildPort()</code> method builds the IO registers corresponding to a general purpose IO port.
     * These ports are named A-G, and each consist of a PORT register (for writing), a PIN register (for reading),
     * and a direction register for setting whether each pin in the port is input or output. This method
     * is a utility to build these registers for each port given the last character of the name (e.g. 'A' in
     * PORTA).
     *
     * @param p the last character of the port name
     */
    protected void buildPort(char p) {
        Pin[] portPins = new Pin[8];
        for (int cntr = 0; cntr < 8; cntr++)
            portPins[cntr] = (Pin) getPin("P" + p + cntr);
        installIOReg("PORT" + p, new PortRegister(portPins));
        installIOReg("DDR" + p, new DirectionRegister(portPins));
        installIOReg("PIN" + p, new PinRegister(portPins));
    }

    /**
     * The <code>buildInterruptRange()</code> method creates the IO registers and <code>MaskableInterrupt</code>
     * instances corresponding to a complete range of interrupts.
     *
     * @param increasing a flag indicating that the vector numbers increase with bit number of the IO register
     * @param maskRegNum the IO register number of the mask register
     * @param flagRegNum the IO register number of the flag register
     * @param baseVect   the beginning vector of this range of interrupts
     * @param numVects   the number of vectors in this range
     * @return a flag register that corresponds to the interrupt range
     */
    protected FlagRegister buildInterruptRange(boolean increasing, String maskRegNum, String flagRegNum, int baseVect, int numVects) {
        int[] mapping = new int[8];
        if (increasing) {
            for (int cntr = 0; cntr < 8; cntr++) mapping[cntr] = baseVect + cntr;
        } else {
            for (int cntr = 0; cntr < 8; cntr++) mapping[cntr] = baseVect - cntr;
        }

        FlagRegister fr = new FlagRegister(interpreter, mapping);
        MaskRegister mr = new MaskRegister(interpreter, mapping);
        installIOReg(maskRegNum, mr);
        installIOReg(flagRegNum, fr);
        return fr;
    }


    protected FlagRegister EIFR_reg;

    protected FlagRegister TIFR_reg;
    protected MaskRegister TIMSK_reg;

    protected FlagRegister ETIFR_reg;
    protected MaskRegister ETIMSK_reg;

    /**
     * The getEIFR_reg() method is used to access the external interrupt flag register.
     *
     * @return the <code>ActiveRegister</code> object corresponding to the EIFR IO register
     */
    public FlagRegister getEIFR_reg() {
        return EIFR_reg;
    }

    public static class InterruptFlag implements InterruptTable.Notification {
        final AtmelInterpreter interpreter;
        final boolean autoclear;
        final int inum;
        final BooleanView view;

        public InterruptFlag(AtmelInterpreter i, boolean auto, int in, BooleanView b) {
            interpreter = i;
            autoclear = auto;
            inum = in;
            interpreter.getInterruptTable().registerInternalNotification(this, inum);
            view = b;
        }

        public void flag(boolean flag) {
            view.setValue(flag);
            interpreter.setPosted(inum, flag);
        }

        public boolean get() {
            return view.getValue();
        }

        public void force(int inum) {
            view.setValue(true);
            interpreter.setPosted(inum, true);
        }

        public void sync() {
            interpreter.setPosted(inum, view.getValue());
        }

        public void invoke(int inum) {
            if (autoclear) {
                view.setValue(false);
                interpreter.setPosted(inum, false);
            }
        }
    }
}
