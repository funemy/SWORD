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

import avrora.sim.*;
import avrora.sim.state.*;
import cck.text.StringUtil;
import cck.util.Arithmetic;

/**
 * Serial Peripheral Interface. Used on the <code>Mica2</code> platform for radio communication.
 *
 * @author Daniel Lee, Simon Han
 */
public class SPI extends AtmelInternalDevice implements SPIDevice, InterruptTable.Notification {

    final SPDReg SPDR_reg;
    final SPCRReg SPCR_reg;
    final SPSReg SPSR_reg;

    SPIDevice connectedDevice;

    final TransferEvent transferEvent = new TransferEvent();

    boolean spifAccessed;

    int interruptNum;

    protected int period;

    /**
     * A single byte data frame for the SPI.
     */
    public static class Frame {
        public final byte data;

        protected Frame(byte data) {
            this.data = data;
        }
    }

    private static final Frame[] frameCache = new Frame[256];
    public static final Frame ZERO_FRAME;
    public static final Frame FF_FRAME;

    static {
        for ( int cntr = 0; cntr < 256; cntr++ )
            frameCache[cntr] = new Frame((byte)cntr);
        ZERO_FRAME = frameCache[0];
        FF_FRAME = frameCache[0xff];
    }

    public static Frame newFrame(byte data) {
        return frameCache[data & 0xff];
    }

    public void connect(SPIDevice d) {
        connectedDevice = d;
    }

    public Frame exchange(Frame frame) {
        Frame result = newFrame(SPDR_reg.transmitReg.read());
        receive(frame);
        return result;
    }

    public void receive(Frame frame) {
        SPDR_reg.receiveReg.write(frame.data);
        if (!SPCR_reg._master.getValue() && !transferEvent.transmitting) postSPIInterrupt();
    }

    public SPI(AtmelMicrocontroller m) {
        super("spi", m);
        SPDR_reg = new SPDReg();
        SPCR_reg = new SPCRReg();
        SPSR_reg = new SPSReg();

        interruptNum = m.getProperties().getInterrupt("SPI, STC");

        installIOReg("SPDR", SPDR_reg);
        installIOReg("SPSR", SPSR_reg);
        installIOReg("SPCR", SPCR_reg);

        interpreter.getInterruptTable().registerInternalNotification(this, interruptNum);
    }

    /**
     * Post SPI interrupt
     */
    private void postSPIInterrupt() {
        interpreter.setPosted(interruptNum, true);
        SPSR_reg.setSPIF();
    }

    private void unpostSPIInterrupt() {
        interpreter.setPosted(interruptNum, false);
        SPSR_reg.clearSPIF();
    }


    /**
     * The SPI transfer event. Upon firing delivers frames in both directions.
     */
    protected class TransferEvent implements Simulator.Event {

        Frame frame;
        boolean transmitting;

        protected void enableTransfer() {

            if (SPCR_reg._master.getValue() && SPCR_reg._enabled.getValue() && !transmitting) {
                if (devicePrinter != null) {
                    devicePrinter.println("SPI: Master mode. Enabling transfer. ");
                }
                SPSR_reg.clearSPIF();
                transmitting = true;
                frame = newFrame(SPDR_reg.transmitReg.read());
                mainClock.insertEvent(this, period);
            }
        }

        public void fire() {
            if (SPCR_reg._enabled.getValue()) {
                SPSR_reg.clearSPIF();//after every reading SPSR must be a cleared
                receive(connectedDevice.exchange(frame));
                transmitting = false;
                postSPIInterrupt();
            }
        }
    }

    public void force(int inum) {
        SPSR_reg.setSPIF();
    }

    public void invoke(int inum) {
        unpostSPIInterrupt();
    }

    /**
     * SPI data register. Writes to this register are transmitted to the connected device and reads
     * from the register read the data received from the connected device.
     */
    class SPDReg implements ActiveRegister {

        protected final RWRegister receiveReg;
        protected final TransmitRegister transmitReg;

        protected class TransmitRegister extends RWRegister {

            public void write(byte val) {
                super.write(val);
                transferEvent.enableTransfer();
            }

        }

        SPDReg() {
            receiveReg = new RWRegister();
            transmitReg = new TransmitRegister();
        }

        /**
         * The <code>read()</code> method
         *
         * @return the value from the receive buffer
         */
        public byte read() {
            if ( spifAccessed ) unpostSPIInterrupt();
            return receiveReg.read();
        }

        /**
         * The <code>write()</code> method
         *
         * @param val the value to transmit buffer
         */
        public void write(byte val) {
            // TODO: implement write collision detection
            transmitReg.write(val);
        }
    }

    /**
     * SPI control register.
     */
    protected class SPCRReg extends RWRegister {

        static final int SPIE = 7;
        static final int SPE = 6;
        static final int MSTR = 4;
        static final int SPR1 = 1;
        static final int SPR0 = 0;

        boolean prev_spie;

        final BooleanView _master = RegisterUtil.booleanView(this, MSTR);
        final BooleanView _enabled = RegisterUtil.booleanView(this, SPE);
        final RegisterView _spr = RegisterUtil.bitRangeView(this, SPR0, SPR1);

        public void write(byte val) {
            if (devicePrinter != null)
                devicePrinter.println("SPI: wrote " + StringUtil.toMultirepString(val, 8) + " to SPCR");
            super.write(val);
            decode(val);
        }

        protected void decode(byte val) {

            // Reset spi interrupt flag when enabling SPI interrupt
            boolean spie = Arithmetic.getBit(val, SPIE);
            interpreter.setEnabled(interruptNum, spie);
            if (spie && !prev_spie) {
                prev_spie = true;
                SPSR_reg.clearSPIF();
            }
            if (!spie && prev_spie) {
                prev_spie = false;
            }

            // calculate the period of the clock
            int divider = 0;
            switch (_spr.getValue()) {
                case 0: divider = 4; break;
                case 1: divider = 16; break;
                case 2: divider = 64; break;
                case 3: divider = 128; break;
            }

            if (SPSR_reg._spi2x.getValue()) divider /= 2;
            period = divider * 8;
        }

    }

    /**
     * SPI status register.
     */
    class SPSReg extends RWRegister {

        static final int SPIF = 7;
        static final int WCOL = 6;

        final BooleanView _spif = RegisterUtil.booleanView(this, SPIF);
        final BooleanView _spi2x = RegisterUtil.booleanView(this, 0);

        byte prev_value;

        public void write(byte val) {
            if (devicePrinter != null)
                devicePrinter.println("SPI: wrote " + val + " to SPSR");
            super.write(val);
            decode(val);
        }

        public byte read() {
            if (_spif.getValue()) spifAccessed = true;
            return super.read();
        }

        protected void decode(byte val) {

            if (!Arithmetic.getBit(prev_value, SPIF) && Arithmetic.getBit(val, SPIF)) {
                postSPIInterrupt();
            }

            spifAccessed = false;
            prev_value = val;
        }

        public void setSPIF() {
            _spif.setValue(true);
            spifAccessed = false;
        }

        public void clearSPIF() {
            _spif.setValue(false);
            spifAccessed = false;
        }

    }
}
