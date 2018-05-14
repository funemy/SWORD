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
import cck.util.Arithmetic;

/**
 * The <code>ATMegaFamily</code> class encapsulates much of the common functionality among the
 * ATMega family microcontrollers from Atmel.
 *
 * @author Pekka Nikander
 * @author Ben L. Titzer
 */
public abstract class ATMegaFamilyNew extends AtmelMicrocontroller {

    // TODO: merge shared classes with ATMegaFamily
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

        protected Pin[] pins;

        protected DirectionRegister(Pin[] p) {
            pins = p;
        }

        public void write(byte val) {
            for (int bit = 0; bit < pins.length; bit++)
                if (pins[bit] != null) pins[bit].setOutputDir(Arithmetic.getBit(val, bit));
            value = val;
        }

    }

    /**
     * The <code>PortRegister</code> class implements an active register that acts as the
     * write register (output register) for the general purpose IO pins.
     */
    public static class PortRegister extends RWRegister {
        protected Pin[] pins;

        protected PortRegister(Pin[] p) {
            pins = p;
        }

        public void write(byte val) {
            for (int bit = 0; bit < pins.length; bit++)
                if (pins[bit] != null) pins[bit].write(Arithmetic.getBit(val, bit));
            value = val;
        }

    }

    /**
     * The <code>PinRegister</code> class implements an active register that acts as the
     * read register (input register) for the general purpose IO pins.
     */
    public static class PinRegister implements ActiveRegister {
        protected Pin[] pins;

        protected PinRegister(Pin[] p) {
            pins = p;
        }

        public byte read() {
            byte value = 0;
            for (int bit = 0; bit < pins.length; bit++)
                value |= pinHigh(bit) ? 1 << bit : 0;
            return value;
        }

        private boolean pinHigh(int bit) {
            return pins[bit] != null && pins[bit].read();
        }

        public void write(byte val) {
            // ignore writes.
        }

    }

    protected ATMegaFamilyNew(ClockDomain cd, AVRProperties p, FiniteStateMachine fsm) {
        super(cd, p, fsm);
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
        buildPort(p, 8);
    }

    protected void buildPort(char p, int pins) {
        Pin[] portPins = new Pin[8];
        for (int bit = 0; bit < pins; bit++)
            portPins[bit] = (Pin) getPin("P" + p + bit);
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
            for (int bit = 0; bit < numVects; bit++) mapping[bit] = baseVect + bit;
        } else {
            for (int bit = 0; bit < numVects; bit++) mapping[bit] = baseVect - bit;
        }
        for (int i = numVects; i < mapping.length; i++) {
            mapping[i] = -1;
        }
        FlagRegister fr = new FlagRegister(interpreter, mapping);
        MaskRegister mr = new MaskRegister(interpreter, mapping);
        installIOReg(maskRegNum, mr);
        installIOReg(flagRegNum, fr);
        return fr;
    }


    protected FlagRegister EIFR_reg;

    /**
     * The getEIFR_reg() method is used to access the external interrupt flag register.
     *
     * @return the <code>ActiveRegister</code> object corresponding to the EIFR IO register
     */
    public FlagRegister getEIFR_reg() {
        return EIFR_reg;
    }

}
