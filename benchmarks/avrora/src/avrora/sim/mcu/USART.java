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
import avrora.sim.output.SimPrinter;
import cck.text.StringUtil;
import cck.util.Arithmetic;

import java.util.LinkedList;

/**
 * The USART class implements a Universal Synchronous Asynchronous Receiver/Transmitter, which is a
 * serial device on the Atmel microcontrollers. The ATMega128, for example, has two USARTs, USART0 and
 * USART1.
 *
 * This implementation of the USART does not yet support "Synchronous Mode" or "Multi-processor Communication
 * Mode". Also, this implementation does not search for parity errors or framing errors. Presumably, these
 * errors will not occur.
 *
 * @author Daniel Lee
 * @author Ben L. Titzer
 */
public class USART extends AtmelInternalDevice {

    static final int RXCn = 7;
    static final int TXCn = 6;
    static final int UDREn = 5;
    static final int FEn = 4;
    static final int DORn = 3;
    static final int UPEn = 2;
    static final int U2Xn = 1;
    static final int MPCMn = 0;

    static final int RXCIEn = 7;
    static final int TXCIEn = 6;
    static final int UDRIEn = 5;
    static final int RXENn = 4;
    static final int TXENn = 3;
    static final int UCSZn2 = 2;
    static final int RXB8n = 1;
    static final int TXB8n = 0;

    // bit 7 is reserved
    static final int UMSELn = 6;
    static final int UPMn1 = 5;
    static final int UPMn0 = 4;
    static final int USBSn = 3;
    static final int UCSZn1 = 2;
    static final int UCSZn0 = 1;
    static final int UCPOLn = 0;

    // parity modes
    static final int PARITY_DISABLED = 0;
    // 2 is reserved
    static final int PARITY_EVEN = 2;
    static final int PARITY_ODD = 3;

    static final int[] FRAME_SIZE = {5, 6, 7, 8, 8, 8, 8, 9};

    static class USARTProperties {
        String subID;

        int USART_RX_inum;
        int USART_UDRE_inum;
        int USART_TX_inum;

        int[] interruptMapping;

        String USART_name;
        String UDR_name;
        String UCSR_name;
        String UBRR_name;
    }

    static USARTProperties getUSARTProperties(String subID, Microcontroller m) {
        MCUProperties mp = m.getProperties();
        USARTProperties props = new USARTProperties();

        props.subID = subID;
        props.USART_name = "USART"+subID;
        props.UDR_name = "UDR"+subID;
        props.UCSR_name = "UCSR"+subID;
        props.UBRR_name = "UBRR"+subID;
        props.USART_RX_inum = mp.getInterrupt(props.USART_name+", RX");
        props.USART_UDRE_inum = mp.getInterrupt(props.USART_name+", UDRE");
        props.USART_TX_inum = mp.getInterrupt(props.USART_name+", TX");

        props.interruptMapping = new int[] { -1, -1, -1, -1, -1, props.USART_UDRE_inum, props.USART_TX_inum, props.USART_RX_inum };

        // TODO: cache the properties for each (subID, microcontroller) pair
        return props;
    }

    final DataRegister UDRn_reg;
    final ControlRegisterA UCSRnA_reg;
    final ControlRegisterB UCSRnB_reg;
    final ControlRegisterC UCSRnC_reg;
    final UBRRnLReg UBRRnL_reg;
    final UBRRnHReg UBRRnH_reg;

    final Transmitter transmitter;
    final Receiver receiver;

    final USARTProperties properties;

    public USARTDevice connectedDevice;

    int period;
    int UBRRMultiplier = 16;


    /**
     * The <code>USARTDevice</code> interface describes USARTs and other serial devices which can be connected
     * to the USART. For simplicity, a higher-level interface communicating by frames of data is used, rather
     * than bits or a representation of changing voltages.
     */
    public interface USARTDevice {
        /**
         * Transmit a frame from this device.
         *
         * @return the frame for transmission
         */
        public Frame transmitFrame();


        /**
         * Receive a frame.
         *
         * @param frame the frame to be received
         */
        public void receiveFrame(Frame frame);

    }

    /**
     * A <code>USARTFrame</code> is a representation of the serial frames being passed between the USART
     * and a connected device.
     */
    public static class Frame {
        public final int value;
        public final int size;

        /**
         * Constructor for a USARTFrame. The <code>high</code> bit is used for 9 bit frame sizes.
         */
        public Frame(byte low, boolean high, int sz) {
            int val = low;
            if ( sz > 8 ) val = Arithmetic.setBit(val, 8, high);
            value = val;
            size = sz;
        }

        public String toString() {
            return StringUtil.toMultirepString(value, size);
        }
    }

    /* *********************************************** */
    /* Methods to implement the USARTDevice interface */

    public Frame transmitFrame() {
        return new Frame(UDRn_reg.transmitRegister.read(), UCSRnB_reg.readBit(TXB8n), UCSRnC_reg.getFrameSize());
    }

    public void receiveFrame(Frame frame) {
        UDRn_reg.receiveRegister.writeFrame(frame);
    }

    public USART(String subID, AtmelMicrocontroller m) {
        super("usart"+subID, m);
        properties = getUSARTProperties(subID, m);

        UDRn_reg = new DataRegister();

        UCSRnA_reg = new ControlRegisterA();
        UCSRnB_reg = new ControlRegisterB();
        UCSRnC_reg = new ControlRegisterC();
        UBRRnL_reg = new UBRRnLReg();
        UBRRnH_reg = new UBRRnHReg();

        transmitter = new Transmitter();
        receiver = new Receiver();

        installIOReg(properties.UDR_name, UDRn_reg);
        installIOReg(properties.UCSR_name+"A", UCSRnA_reg);
        installIOReg(properties.UCSR_name+"B", UCSRnB_reg);
        installIOReg(properties.UCSR_name+"C", UCSRnC_reg);
        installIOReg(properties.UBRR_name+"L", UBRRnL_reg);
        installIOReg(properties.UBRR_name+"H", UBRRnH_reg);

        connect(new SerialPrinter());
    }

    public void connect(USARTDevice d) {
        connectedDevice = d;
    }

    void updatePeriod() {
        period = read16(UBRRnH_reg, UBRRnL_reg) + 1;
        if (devicePrinter != null)
            devicePrinter.println(properties.USART_name+": period set to "+period);
        period *= UBRRMultiplier;
    }

    protected class Transmitter {
        boolean transmitting;
        Transmit transmit = new Transmit();

        protected void enableTransmit() {
            if (!transmitting) {
                // grab the frame from the UDR register
                transmit.frame = new Frame(UDRn_reg.transmitRegister.read(), UCSRnB_reg.readBit(TXB8n), UCSRnC_reg.getFrameSize());
                // now the shift register has the data, the UDR is free
                UCSRnA_reg.UDRE_flag.flag(true);
                transmitting = true;
                mainClock.insertEvent(transmit, (1 + UCSRnC_reg.getFrameSize() + UCSRnC_reg.getStopBits()) * period);
            }
        }

        protected class Transmit implements Simulator.Event {
            Frame frame;

            public void fire() {
                connectedDevice.receiveFrame(frame);

                if (devicePrinter != null)
                    devicePrinter.println(properties.USART_name+": Transmitted frame " + frame);
                transmitting = false;
                UCSRnA_reg.TXC_flag.flag(true);
                if (!UCSRnA_reg.UDRE_flag.get()) {
                    transmitter.enableTransmit();
                }
            }
        }
    }

    /**
     * Initiate a receive between the UART and the connected device.
     */
    public void startReceive() {
        receiver.enableReceive();
    }

    protected class Receiver {

        boolean receiving;
        Receive receive = new Receive();

        protected void enableReceive() {
            if (!receiving) {
                receive.frame = connectedDevice.transmitFrame();
                mainClock.insertEvent(receive, (1 + UCSRnC_reg.getFrameSize() + UCSRnC_reg.getStopBits()) * period);
                receiving = true;
            }
        }


        protected class Receive implements Simulator.Event {
            Frame frame;

            public void fire() {
                receiveFrame(frame);

                if (devicePrinter != null)
                    devicePrinter.println(properties.USART_name+": Received frame " + frame + ' ' + UBRRnH_reg.read() + ' ' + UBRRnL_reg.read() + ' ' + UBRRMultiplier + ' ');

                UCSRnA_reg.RXC_flag.flag(true);

                receiving = false;
            }
        }
    }

    /**
     * The <code>DataRegister</code> class represents a Transmit Data Buffer Register for a USART. It
     * is really two registers, a transmit register and a receive register. The transmit register is
     * the destination of data written to the register at this address. The receive register is the
     * source of data read from this address.
     */
    protected class DataRegister extends RWRegister {
        RWRegister transmitRegister;
        TwoLevelFIFO receiveRegister;

        DataRegister() {
            transmitRegister = new RWRegister();
            receiveRegister = new TwoLevelFIFO();
        }

        public void write(byte val) {
            transmitRegister.write(val);
            // we now have data in UDRE, so the user data register is not ready yet
            UCSRnA_reg.UDRE_flag.flag(false);
            if (UCSRnB_reg.readBit(TXENn)) {
                transmitter.enableTransmit();
            }
        }

        public byte read() {
            if ( !true) UCSRnA_reg.RXC_flag.flag(false);
            //UCSRnA_reg.writeBit(RXCn, true);
            return receiveRegister.read();
        }


        /**
         * An implementation of the FIFO used to buffer the received frames. This is not quite a
         * two-level FIFO, as the shift-receive register in the actual implementation can act as a
         * third level to the buffer. In order to account for this, the FIFO is implemented as a queue
         * that can hold at most three elements (limited by the implementation). Although the
         * implementation does not mirror the how the hardware does this, functionally it should
         * behave the same way.
         */
        private class TwoLevelFIFO extends RWRegister {

            LinkedList readyQueue;
            LinkedList waitQueue;

            TwoLevelFIFO() {
                readyQueue = new LinkedList();
                waitQueue = new LinkedList();
                waitQueue.add(new USARTFrameWrapper());
                waitQueue.add(new USARTFrameWrapper());
                waitQueue.add(new USARTFrameWrapper());
            }

            public byte read() {
                if (readyQueue.isEmpty()) {
                    UCSRnA_reg.UDRE_flag.flag(true);//we must indicate register data empty
                    return (byte)0;
                }
                USARTFrameWrapper current = (USARTFrameWrapper)readyQueue.removeLast();
                if (readyQueue.isEmpty()) {
                    UCSRnA_reg.RXC_flag.flag(false);
                }
                UCSRnB_reg._rxb8n.setValue(Arithmetic.getBit(current.frame.value, 8));
                waitQueue.add(current);
                return (byte)current.frame.value;
            }

            public void writeFrame(Frame frame) {
                if (waitQueue.isEmpty()) {
                    // data overrun. drop frame
                    UCSRnA_reg._dor.setValue(true);
                } else {
                    USARTFrameWrapper current = (USARTFrameWrapper)(waitQueue.removeLast());
                    current.frame = frame;
                    readyQueue.addFirst(current);
                }
            }

            protected void flush() {
                while (!waitQueue.isEmpty()) {
                    // empty the wait queue. fill the ready queue.
                    readyQueue.add(waitQueue.removeLast());
                }
            }

            private class USARTFrameWrapper {
                Frame frame;
            }

        }

    }

    /**
     * UCSRnA (<code>ControlRegisterA</code>) is one of three control/status registers for the USART.
     * The high three bits are actually interrupt flag bits.
     */
    protected class ControlRegisterA extends RWRegister {

        final ATMegaFamily.InterruptFlag UDRE_flag;
        final ATMegaFamily.InterruptFlag TXC_flag;
        final ATMegaFamily.InterruptFlag RXC_flag;

        final BooleanView _dor = RegisterUtil.booleanView(this, 3);
        final BooleanView _u2xn = RegisterUtil.booleanView(this, 1);

        public ControlRegisterA() {
            UDRE_flag = new ATMegaFamily.InterruptFlag(interpreter, false, properties.USART_UDRE_inum, RegisterUtil.booleanView(this, 5));
            TXC_flag = new ATMegaFamily.InterruptFlag(interpreter, true, properties.USART_TX_inum, RegisterUtil.booleanView(this, 6));
            RXC_flag = new ATMegaFamily.InterruptFlag(interpreter, false, properties.USART_RX_inum, RegisterUtil.booleanView(this, 7));
            // user data register is empty initially
            UDRE_flag.flag(true);
        }

        public void write(byte val) {
            // bits 0 and 1 are R/W, all others read only. writing a 1 to bit 6 clears it.
            if ((val & 0x40) == 1)
              value &= 0xBF;
            value = (byte)((value & 0xFC) | (val & 0x3));
            // since RCX and UDRE cannot be changed, no sync is necessary
//            RXC_flag.sync();
            TXC_flag.sync();
//            UDRE_flag.sync();

            if (UCSRnC_reg._umsel.getValue() == 1) UBRRMultiplier = 2;
            else if (_u2xn.getValue()) UBRRMultiplier = 8;
            else UBRRMultiplier = 16;

            if ( devicePrinter != null )
                devicePrinter.println(properties.USART_name+": multiplier set to "+UBRRMultiplier);
        }

    }

    /**
     * UCSRnB (<code>ControlRegisterB</code>) is one of three control/status registers for the USART.
     * The high three bits are actually interrupt mask bits.
     */
    protected class ControlRegisterB extends ATMegaFamily.MaskRegister {

        final RegisterView _ucszHigh = RegisterUtil.bitView(this, UCSZn2);
        final BooleanView _rxb8n = RegisterUtil.booleanView(this, RXB8n);

        ControlRegisterB() {
            super(USART.this.interpreter, properties.interruptMapping);
        }
    }

    /**
     * UCSRnC (<code>ControlRegisterC</code>) is one of three control/status registers for the USART.
     */
    protected class ControlRegisterC extends RWRegister {

        final RegisterView _stopBits = RegisterUtil.bitView(this, USBSn);
        final RegisterView _ucszLow = RegisterUtil.bitRangeView(this, UCSZn0, UCSZn1);
        final RegisterView _umsel = RegisterUtil.bitView(this, UMSELn);

        public int getFrameSize() {
            if (false) {
                int indx = (UCSRnB_reg._ucszHigh.getValue() << 2) | _ucszLow.getValue();
                return FRAME_SIZE[indx];
            } else {
                return 8;
            }
        }

        public int getStopBits() {
            if (_stopBits.getValue() == 1) return 2;
            else return 1;
        }
    }

    /**
     * The high byte of the Baud Rate register.
     */
    protected class UBRRnHReg extends RWRegister {

        public void write(byte val) {
            super.write((byte)(0x0f & val));
        }

    }

    /**
     * The low byte of the Baud Rate register. The baud rate is not updated until the low bit is
     * updated.
     */
    protected class UBRRnLReg extends RWRegister {

        public void write(byte val) {
            super.write(val);
            updatePeriod();
        }

    }

    /**
     * A simple implementation of the USARTDevice interface that connects to a USART on the processor.
     * It simply prints out a representation of each frame it receives.
     */
    protected class SerialPrinter implements USARTDevice {

        SimPrinter serialPrinter = simulator.getPrinter("atmel.usart.printer");

        char[] stream = {'h', 'e', 'l', 'l', 'o', 'w', 'o', 'r', 'l', 'd'};

        int count;

        public Frame transmitFrame() {
            return new Frame((byte)stream[count++ % stream.length], false, 8);
        }

        public void receiveFrame(Frame frame) {
            if (serialPrinter != null) serialPrinter.println("Serial Printer " + frame.toString());
        }
    }

}
