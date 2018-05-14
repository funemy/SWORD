/**
 * Copyright (c) 2007, Regents of the University of California
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
 * Created Oct 10, 2007
 */
package avrora.sim.radio;

import avrora.sim.Simulator;
import avrora.sim.clock.Synchronizer;
import avrora.sim.mcu.*;
import avrora.sim.output.SimPrinter;
import avrora.sim.state.*;
import cck.text.StringUtil;
import cck.util.Arithmetic;
import cck.util.Util;
import avrora.sim.FiniteStateMachine;
import avrora.sim.energy.Energy;

import java.util.*;

/**
 * The <code>CC2420Radio</code> implements a simulation of the CC2420 radio
 * chip.The CC2420 radio is used with the Micaz and telosb platforms.
 * Verbose printers for this class include "radio.cc2420"
 *
 * @author Ben L. Titzer
 * @author Rodolfo de Paz
 */
public class CC2420Radio implements Radio {

    //-- Register addresses ---------------------------------------------------
    public static final int MAIN = 0x10;
    public static final int MDMCTRL0 = 0x11;
    public static final int MDMCTRL1 = 0x12;
    public static final int RSSI = 0x13;
    public static final int SYNCWORD = 0x14;
    public static final int TXCTRL = 0x15;
    public static final int RXCTRL0 = 0x16;
    public static final int RXCTRL1 = 0x17;
    public static final int FSCTRL = 0x18;
    public static final int SECCTRL0 = 0x19;
    public static final int SECCTRL1 = 0x1a;
    public static final int BATTMON = 0x1b;
    public static final int IOCFG0 = 0x1c;
    public static final int IOCFG1 = 0x1d;
    public static final int MANFIDL = 0x1e;
    public static final int MANFIDH = 0x1f;
    public static final int FSMTC = 0x20;
    public static final int MANAND = 0x21;
    public static final int MANOR = 0x22;
    public static final int AGCCTRL0 = 0x23;
    public static final int AGCTST0 = 0x24;
    public static final int AGCTST1 = 0x25;
    public static final int AGCTST2 = 0x26;
    public static final int FSTST0 = 0x27;
    public static final int FSTST1 = 0x28;
    public static final int FSTST2 = 0x29;
    public static final int FSTST3 = 0x2a;
    public static final int RXBPFTST = 0x2b;
    public static final int FSMSTATE = 0x2c;
    public static final int ADCTST = 0x2d;
    public static final int DACTST = 0x2e;
    public static final int TOPTST = 0x2f;
    public static final int TXFIFO = 0x3e;
    public static final int RXFIFO = 0x3f;

    //-- Command strobes ---------------------------------------------------
    public static final int SNOP = 0x00;
    public static final int SXOSCON = 0x01;
    public static final int STXCAL = 0x02;
    public static final int SRXON = 0x03;
    public static final int STXON = 0x04;
    public static final int STXONCCA = 0x05;
    public static final int SRFOFF = 0x06;
    public static final int SXOSCOFF = 0x07;
    public static final int SFLUSHRX = 0x08;
    public static final int SFLUSHTX = 0x09;
    public static final int SACK = 0x0a;
    public static final int SACKPEND = 0x0b;
    public static final int SRXDEC = 0x0c;
    public static final int STXENC = 0x0d;
    public static final int SAES = 0x0e;

    //-- Other constants --------------------------------------------------
    private static final int NUM_REGISTERS = 0x40;
    private static final int FIFO_SIZE = 128;
    private static final int RAMSECURITYBANK_SIZE = 113;

    private static final int XOSC_START_TIME = 1000;// oscillator start time
    private static final int PLL_LOCK_TIME = 192;// for startup and turnaround times

    //-- Simulation objects -----------------------------------------------
    protected final Microcontroller mcu;
    protected final Simulator sim;

    //-- Radio state ------------------------------------------------------
    protected final int xfreq;
    protected final char[] registers = new char[NUM_REGISTERS];
    protected final byte[] RAMSecurityRegisters = new byte[RAMSECURITYBANK_SIZE];
    protected final ByteFIFO txFIFO = new ByteFIFO(FIFO_SIZE);
    protected final ByteFIFO rxFIFO = new ByteFIFO(FIFO_SIZE);
    protected List BERlist = new ArrayList();

    protected Medium medium;
    protected Transmitter transmitter;
    protected Receiver receiver;

    //-- Strobes and status ----------------------------------------------
    // note that there is no actual "status register" on the CC2420.
    // The register here is used in the simulation implementation to
    // simplify the handling of radio states and state transitions.
    protected final Register statusRegister = new Register(8);
    protected boolean startingOscillator;
    protected boolean TXstartingUp;
    protected boolean RXstartingUp;
    protected boolean SRXDEC_switched;
    protected boolean STXENC_switched;

    //-- Views of bits in the status "register" ---------------------------
    protected final BooleanView oscStable = RegisterUtil.booleanView(statusRegister, 6);
    protected final BooleanView txUnderflow = RegisterUtil.booleanView(statusRegister, 5);
    protected final BooleanView txActive = RegisterUtil.booleanView(statusRegister, 3);
    protected final BooleanView signalLock = RegisterUtil.booleanView(statusRegister, 2);
    protected final BooleanView rssiValid = RegisterUtil.booleanView(statusRegister, 1);
    //-- Views of bits in the status "register" ---------------------------
    protected final RegisterView MDMCTRL0_reg = new RegisterUtil.CharArrayView(registers, MDMCTRL0);
    protected final BooleanView autoACK = RegisterUtil.booleanView(MDMCTRL0_reg, 4);
    protected final BooleanView autoCRC = RegisterUtil.booleanView(MDMCTRL0_reg, 5);
    protected final BooleanView ADR_DECODE = RegisterUtil.booleanView(MDMCTRL0_reg, 11);
    protected final BooleanView PAN_COORDINATOR = RegisterUtil.booleanView(MDMCTRL0_reg, 12);
    protected final BooleanView RESERVED_FRAME_MODE = RegisterUtil.booleanView(MDMCTRL0_reg, 13);

    protected final RegisterView IOCFG0_reg = new RegisterUtil.CharArrayView(registers, IOCFG0);
    protected final BooleanView BCN_ACCEPT = RegisterUtil.booleanView(IOCFG0_reg, 11);

    protected final BooleanView CCA_assessor = new ClearChannelAssessor();
    protected BooleanView SFD_value = new BooleanRegister();

    //-- Pins ------------------------------------------------------------
    public final CC2420Pin SCLK_pin = new CC2420Pin("SCLK");
    public final CC2420Pin MISO_pin = new CC2420Pin("MISO");
    public final CC2420Pin MOSI_pin = new CC2420Pin("MOSI");
    public final CC2420Pin CS_pin = new CC2420Pin("CS");
    public final CC2420Pin VREN_pin = new CC2420Pin("VREN");
    public final CC2420Pin RSTN_pin = new CC2420Pin("RSTN");
    public final CC2420Output FIFO_pin = new CC2420Output("FIFO", new BooleanRegister());
    public final CC2420Output FIFOP_pin = new CC2420Output("FIFOP", new BooleanRegister());
    public final CC2420Output CCA_pin = new CC2420Output("CCA", CCA_assessor);
    public final CC2420Output SFD_pin = new CC2420Output("SFD", SFD_value);

    public final SPIInterface spiInterface = new SPIInterface();
    public final ADCInterface adcInterface = new ADCInterface();

    public int FIFOP_interrupt = -1;

    protected final SimPrinter printer;

    // the CC2420 allows reversing the polarity of these outputs.
    protected boolean FIFO_active;// selects active high (true) or active low.
    protected boolean FIFOP_active;
    protected boolean CCA_active;
    protected boolean SFD_active;

    //Acks variables
    protected boolean SendAck;
    protected boolean SendAckPend;
    protected byte DSN;

    //Address recognition variables
    protected byte[] PANId;
    protected byte[] macPANId;
    protected byte[] ShortAddr;
    protected byte[] macShortAddr;
    protected static final byte[] SHORT_BROADCAST_ADDR = {15, 15};
    protected byte[] LongAdr;
    protected byte[] IEEEAdr;
    protected static final byte[] LONG_BROADCAST_ADDR = {15, 15, 15, 15, 15, 15, 15, 15};

    //LUT from cubic spline interpolation with all transmission power values
    protected static final double [] POWER_dBm = {-37.917,-32.984,-28.697,-25,
    -21.837,-19.153,-16.893,-15,-13.42,-12.097,-10.975,-10,-9.1238,-8.3343,
    -7.6277,-7,-6.4442,-5.9408,-5.467,-5,-4.5212,-4.0275,-3.5201,-3,-2.4711,
    -1.9492,-1.4526,-1,-0.6099,-0.3008,-0.0914,0};

    //LUT for max and min correlation values depending on PER
    protected static final int [] Corr_MAX = {110,109,109,109,107,107,107,107,107,
    107,107,107,103,102,102,102,101,101,101,101,99,94,92,94,101,97,98,97,97,97,97,97,
    94,94,94,94,94,94,94,94,94,94,94,94,92,89,89,89,89,89,88,88,88,88,88,86,86,86,
    86,86,86,86,86,86,85,85,85,85,85,85,83,83,83,83,83,83,83,83,79,78,78,78,78,78,
    76,76,76,74,74,74,74,74,74,74,74,74,74,66,65,65,65};
    protected static final int [] Corr_MIN = {95,95,94,91,90,90,89,89,89,88,88,88,82,
    82,82,82,76,76,76,76,76,76,74,74,74,74,74,74,72,72,72,72,72,72,72,72,69,69,69,69,
    69,69,69,69,69,69,69,69,69,69,69,69,69,69,69,69,67,67,67,67,67,67,65,65,65,65,65,
    65,65,64,64,63,63,63,63,63,63,63,63,63,61,61,61,60,60,60,58,58,56,56,56,55,55,55,
    50,50,50,50,50,50,50};
    protected double Correlation;

    //CC2420Radio energy
    protected static final String[] allModeNames = CC2420Energy.allModeNames();
    protected static final int[][] ttm = FiniteStateMachine.buildSparseTTM(allModeNames.length, 0);
    protected final FiniteStateMachine stateMachine;

    //Clear TxFIFO flag boolean value
    protected boolean ClearFlag;

    /**
     * The constructor for the CC2420 class creates a new instance connected
     * to the specified microcontroller with the given external clock frequency.
     *
     * @param mcu   the microcontroller unit to which this radio is attached
     * @param xfreq the external clock frequency supplied to the CC2420 radio chip
     */
    public CC2420Radio(Microcontroller mcu, int xfreq) {
        // set up references to MCU and simulator
        this.mcu = mcu;
        this.sim = mcu.getSimulator();
        this.xfreq = xfreq;

        // create a private medium for this radio
        // the simulation may replace this later with a new one.
        setMedium(createMedium(null, null));

        //setup energy recording
        Simulator simulator = mcu.getSimulator();

        stateMachine = new FiniteStateMachine(simulator.getClock(), CC2420Energy.startMode, allModeNames, ttm);

        new Energy("Radio", CC2420Energy.modeAmpere, stateMachine, sim.getSimulation().getEnergyControl());

        // reset all registers
        reset();

        // get debugging channel.
        printer = mcu.getSimulator().getPrinter("radio.cc2420");


    }

    /**
     * The <code>getFiniteStateMachine()</code> method gets a reference to the finite state
     * machine that represents this radio's state. For example, there are states corresponding
     * to "on", "off", "transmitting", and "receiving". The state names and numbers will vary
     * by radio implementation. The <code>FiniteStateMachine</code> instance allows the user
     * to instrument the state transitions in order to gather information during simulation.
     * @return a reference to the finite state machine for this radio
     */
    public FiniteStateMachine getFiniteStateMachine() {
        return stateMachine;
    }

    private void reset() {
        for (int cntr = 0; cntr < NUM_REGISTERS; cntr++) {
            resetRegister(cntr);
        }

        // clear FIFOs.
        txFIFO.clear();
        rxFIFO.clear();

        // reset the status register
        statusRegister.setValue(0);

        // restore default CCA and SFD values.
        CCA_pin.level = CCA_assessor;
        SFD_pin.level = SFD_value;

        FIFO_active = true;// the default is active high for all of these pins
        FIFOP_active = true;
        CCA_active = true;
        SFD_active = true;

        SendAck = false;  // reset these internal variables
        SendAckPend = false;
        ClearFlag = false;

        // reset pins.
        FIFO_pin.level.setValue(!FIFO_active);
        FIFOP_pin.level.setValue(!FIFOP_active);

        transmitter.endTransmit();
        receiver.endReceive();
    }

    public void setSFDView(BooleanView sfd) {
        if (SFD_pin.level == SFD_value) {
            SFD_pin.level = sfd;
        }
        SFD_value = sfd;
    }

    /**
     * The <code>readRegister()</code> method reads the value from the specified register
     * and takes any action(s) that are necessary for the specific register.
     *
     * @param addr the address of the register
     * @return an integer value representing the result of reading the register
     */
    int readRegister(int addr) {
        int val = (int) registers[addr];
        if (printer != null) {
            printer.println("CC2420 " + regName(addr) + " => " + StringUtil.toMultirepString(val, 16));
        }
        return val;
    }

    /**
     * The <code>writeRegister()</code> method writes the specified value to the specified
     * register, taking any action(s) necessary and activating any command strobes as
     * required.
     *
     * @param addr the address of the register
     * @param val  the value to write to the specified register
     */
    void writeRegister(int addr, int val) {
        if (printer != null) {
            printer.println("CC2420 " + regName(addr) + " <= " + StringUtil.toMultirepString(val, 16));
        }
        registers[addr] = (char) val;
        switch (addr) {
            case MAIN:
                if ((val & 0x8000) != 0) {
                    reset();
                    stateMachine.transition(1);//change to power down state
                }
                break;
            case IOCFG1:
                int ccaMux = val & 0x1f;
                int sfdMux = (val >> 5) & 0x1f;
                setCCAMux(ccaMux);
                setSFDMux(sfdMux);
                break;
            case IOCFG0:
                // set the polarities for the output pins.
                FIFO_active = !Arithmetic.getBit(val, 10);
                FIFOP_active = !Arithmetic.getBit(val, 9);
                SFD_active = !Arithmetic.getBit(val, 8);
                CCA_active = !Arithmetic.getBit(val, 7);
                break;
        }
        computeStatus();
    }

    private void setSFDMux(int sfdMux) {
        // TODO: SFD multiplexor
    }

    private void setCCAMux(int ccaMux) {
        // TODO: handle all the possible CCA multiplexing sources
        // and possibility of active low.
        if (ccaMux == 24) CCA_pin.level = oscStable;
        else CCA_pin.level = CCA_assessor;
    }

    void strobe(int addr) {
        if (printer != null) {
            printer.println("CC2420 Strobe " + strobeName(addr));
        }
        switch (addr) {
            case SNOP:
                break;
            case SXOSCON:
                startOscillator();
                break;
            case STXCAL:
                break;
            case SRXON:
                transmitter.shutdown();
                receiver.startup();
                break;
            case STXONCCA:
                if (CCA_assessor.getValue()) {
                    receiver.shutdown();
                    transmitter.startup();
                }
                break;
            case STXON:
                receiver.shutdown();
                transmitter.startup();
                break;
            case SRFOFF:
                stateMachine.transition(2);//change to idle state
                break;
            case SXOSCOFF:
                oscStable.setValue(false);
                stateMachine.transition(1);//change to power down state
                break;
            case SFLUSHRX:
                rxFIFO.clear();
                receiver.resetOverflow();
                FIFO_pin.level.setValue(!FIFO_active);
                FIFOP_pin.level.setValue(!FIFOP_active);
                break;
            case SFLUSHTX:
                txFIFO.clear();
                txUnderflow.setValue(false);
                break;
            case SACK:
                SendAck = true;
                if (!receiver.locked) {
                    receiver.shutdown();
                    transmitter.startup();
                }
                break;
            case SACKPEND:
                SendAckPend = true;
                if (!receiver.locked) {
                    receiver.shutdown();
                    transmitter.startup();
                }
                break;
            case SRXDEC:
                // start RXFIFO in-line decryption/authentication as set by SPI_SEC_MODE
                break;
            case STXENC:
                // start TXFIFO in-line encryption/authentication as set by SPI_SEC_MODE
                break;
            case SAES:
                // SPI_SEC_MODE is not required to be 0, but the encrypt. module must be idle; else strobe is ignored
                break;
        }
    }

    private void startOscillator() {
        if (!oscStable.getValue() && !startingOscillator) {
            startingOscillator = true;
            sim.insertEvent(new Simulator.Event() {
                public void fire() {
                    oscStable.setValue(true);
                    startingOscillator = false;
                    stateMachine.transition(2);//change to idle state
                    if (printer != null) {
                       printer.println("CC2420 Oscillator established");
                    }
                }
            }, toCycles(XOSC_START_TIME));
        }
    }

    /**
     * The <code>resetRegister()</code> method resets the specified register's value
     * to its default.
     *
     * @param addr the address of the register to reset
     */
    void resetRegister(int addr) {
        char val = 0x0000;
        switch (addr) {
            case MAIN:
                val = 0xf800;
                break;
            case MDMCTRL0:
                val = 0x0ae2;
                break;
            case SYNCWORD:
                val = 0xa70f;
                break;
            case TXCTRL:
                val = 0xa0ff;
                break;
            case RXCTRL0:
                val = 0x12e5;
                break;
            case RXCTRL1:
                val = 0x0a56;
                break;
            case FSCTRL:
                val = 0x4165;
                break;
            case IOCFG0:
                val = 0x0040;
                break;
        }
        registers[addr] = val;
    }

    /**
     * The <code>computeStatus()</code> method computes the status byte of the radio.
     */
    void computeStatus() {
        // do nothing.
    }

    protected static final int CMD_R_REG = 0;
    protected static final int CMD_W_REG = 1;
    protected static final int CMD_R_RX = 2;
    protected static final int CMD_W_RX = 3;
    protected static final int CMD_R_TX = 4;
    protected static final int CMD_W_TX = 5;
    protected static final int CMD_R_RAM = 6;
    protected static final int CMD_W_RAM = 7;

    //-- state for managing configuration information
    protected int configCommand;
    protected int configByteCnt;
    protected int configRegAddr;
    protected byte configByteHigh;
    protected int configRAMAddr;
    protected int configRAMBank;

    protected byte receiveConfigByte(byte val) {
        configByteCnt++;
        if (configByteCnt == 1) {
            // the first byte is the address byte
            byte status = getStatus();
            boolean ramop = Arithmetic.getBit(val, 7);
            boolean readop = Arithmetic.getBit(val, 6);
            configRegAddr = val & 0x3f;
            configRAMAddr = val & 0x7f;
            computeStatus();
            if (configRegAddr <= 15) {
                // execute the command strobe
                strobe(configRegAddr);
                configByteCnt = 0;
            } else {
                if (ramop) configCommand = CMD_R_RAM;
                else if (configRegAddr == TXFIFO) configCommand = readop ? CMD_R_TX : CMD_W_TX;
                else if (configRegAddr == RXFIFO) configCommand = readop ? CMD_R_RX : CMD_W_RX;
                else configCommand = readop ? CMD_R_REG : CMD_W_REG;
            }
            return status;
        } else if (configByteCnt == 2) {
            // the second byte is the MSB for a write, unused for read
            switch (configCommand) {
                case CMD_R_REG:
                    return Arithmetic.high(readRegister(configRegAddr));
                case CMD_W_REG:
                    configByteHigh = val;
                    return 0;
                case CMD_R_TX:
                    return readFIFO(txFIFO);
                case CMD_R_RX:
                    return readFIFO(rxFIFO);
                case CMD_W_TX:
                    return writeFIFO(txFIFO, val, true);
                case CMD_W_RX:
                    return writeFIFO(rxFIFO, val, false);
                case CMD_R_RAM:
                    configRAMBank = (val >> 6) & 0x3;
                    if (Arithmetic.getBit(val, 5)) configCommand = CMD_R_RAM;
                    else configCommand = CMD_W_RAM;
                    return 0;
            }
        } else {
            // the third byte completes a read or write register
            // while subsequent bytes are valid for fifo and RAM accesses
            switch (configCommand) {
                case CMD_R_REG:
                    configByteCnt = 0;
                    return Arithmetic.low(readRegister(configRegAddr));
                case CMD_W_REG:
                    configByteCnt = 0;
                    writeRegister(configRegAddr, Arithmetic.word(val, configByteHigh));
                    return 0;
                case CMD_R_TX:
                    return readFIFO(txFIFO);
                case CMD_R_RX:
                    return readFIFO(rxFIFO);
                case CMD_W_TX:
                    return writeFIFO(txFIFO, val, true);
                case CMD_W_RX:
                    return writeFIFO(rxFIFO, val, false);
                case CMD_R_RAM:
                    if (configRAMBank == 0x00) return txFIFO.peek(configRAMAddr);
                    else if (configRAMBank == 0x01) return rxFIFO.peek(configRAMAddr);
                    else if (configRAMBank == 0x02) return ReadSecurityBank(configRAMAddr + (configByteCnt - 3));
                    return 0;
                case CMD_W_RAM:
                    if (configRAMBank == 0x00) return txFIFO.poke(configRAMAddr, val);
                    else if (configRAMBank == 0x01) return rxFIFO.poke(configRAMAddr, val);
                    else if (configRAMBank == 0x02)  return WriteSecurityBank(configRAMAddr + (configByteCnt - 3), val);
                    return 0;
            }
        }
        return 0;
    }

    private byte getStatus() {
        byte status = (byte) statusRegister.getValue();
        if (printer != null) {
            printer.println("CC2420 status: " + StringUtil.toBin(status, 8));
        }
        return status;
    }

    protected byte ReadSecurityBank(int address) {
        int value = (int) RAMSecurityRegisters[address];
        if (printer != null) {
            printer.println("CC2420 " + SecurityRAMName(address) + "(addr " + StringUtil.to0xHex(address, 2) + ") -> " + StringUtil.toMultirepString(value, 8));
        }
        return (byte) value;
    }

    protected byte WriteSecurityBank(int address, byte value) {
        if (printer != null) {
            printer.println("CC2420 " + SecurityRAMName(address) + "(addr " + StringUtil.to0xHex(address, 2) + ") <= " + StringUtil.toMultirepString(value, 8));
        }
        RAMSecurityRegisters[address] = value;
        //If RAM PANId = 0xffff set IOCFG0.BCN_ACCEPT
        if ((RAMSecurityRegisters[104] == 255) && (RAMSecurityRegisters[105] == 255)) {
            BCN_ACCEPT.setValue(true);
        }
        return value;
    }

    protected byte readFIFO(ByteFIFO fifo) {
        byte val = fifo.remove();
        if (printer != null) {
            printer.println("CC2420 Read " + fifoName(fifo) + " -> " + StringUtil.toMultirepString(val, 8));
        }
        if (fifo == rxFIFO) {
            if (fifo.empty()) {
                // reset the FIFO pin when the read FIFO is empty.
                FIFO_pin.level.setValue(!FIFO_active);
            } else if (fifo.size() < getFIFOThreshold()) {
                FIFOP_pin.level.setValue(!FIFOP_active);
            }
        }
        return val;
    }

    protected byte writeFIFO(ByteFIFO fifo, byte val, boolean st) {
        if (printer != null) {
            printer.println("CC2420 Write " + fifoName(fifo) + " <= " + StringUtil.toMultirepString(val, 8));
        }
        byte result = st ? getStatus() : 0;
        if (getClearFlag()){
            fifo.clear();
            ClearFlag = false;
        }
        fifo.add(val);
        computeStatus();
        return result;
    }

    protected boolean getClearFlag(){
        return ClearFlag;
    }

    protected void setClearFlag(){
        ClearFlag = true;
    }

    private int getFIFOThreshold() {
        // get the FIFOP_THR value from the configuration register
        return (int) registers[IOCFG0] & 0x7f;
    }

    public Simulator getSimulator() {
        return sim;
    }

    public double getPower() {
        //return power in dBm
        return POWER_dBm[(readRegister(TXCTRL) & 0x1f)];
    }

    public double getFrequency() {
        //return frequency in Mhz
        return (double) (2048 + readRegister(FSCTRL) & 0x03ff);
    }

    public class ClearChannelAssessor implements BooleanView {
        public void setValue(boolean val) {
             // ignore writes.
        }

        public boolean getValue() {
            return receiver.isChannelClear(readRegister(RSSI),readRegister(MDMCTRL0));
        }
    }

    public class SPIInterface implements SPIDevice {

        public SPI.Frame exchange(SPI.Frame frame) {
            if (printer != null) {
                printer.println("CC2420 new SPI frame exchange " + StringUtil.toMultirepString(frame.data, 8));
            }
            if (!CS_pin.level && VREN_pin.level && RSTN_pin.level) {
                // configuration requires CS pin to be held low, and VREN pin and RSTN pin to be held high
                return SPI.newFrame(receiveConfigByte(frame.data));
            } else {
                return SPI.newFrame((byte) 0);
            }
        }

        public void connect(SPIDevice d) {
            // do nothing.
        }
    }

    public class ADCInterface implements ADC.ADCInput {

        public float getVoltage() {
            throw Util.unimplemented();
        }
    }

    private void pinChange_CS(boolean level) {
        // a change in the CS level always restarts a config command.
        configByteCnt = 0;
    }

    private void pinChange_VREN(boolean level) {
      if (level) {
        // the voltage regulator has been switched on
        if (stateMachine.getCurrentState() == 0) {
          // actually, there is a startup time for the voltage regulator
          // but we assume here that it starts immediately
          stateMachine.transition(1);//change to power down state
          if (printer != null) {
            printer.println("CC2420 Voltage Regulator started");
          }
        }
      }
      else {
        if (stateMachine.getCurrentState() > 0) {
          // switch the chip off, but stop all things first
          transmitter.endTransmit();
          receiver.endReceive();
          stateMachine.transition(0);//change to off state
          if (printer != null) {
            printer.println("CC2420 Voltage Regulator switched off");
          }
        }
      }
    }

    private void pinChange_RSTN(boolean level) {
      if (!level) {
        // high->low indicates reset
        reset();
        stateMachine.transition(1);//change to power down state
        if (printer != null) {
          printer.println("CC2420 reset by pin");
        }
      }
    }

    private static final int TX_IN_PREAMBLE = 0;
    private static final int TX_SFD_1 = 1;
    private static final int TX_SFD_2 = 2;
    private static final int TX_LENGTH = 3;
    private static final int TX_IN_PACKET = 4;
    private static final int TX_CRC_1 = 5;
    private static final int TX_CRC_2 = 6;
    private static final int TX_END = 7;

    public class Transmitter extends Medium.Transmitter {

        protected int state;
        protected int counter;
        protected int length;
        protected char crc;
        protected boolean wasAck;

        public Transmitter(Medium m) {
            super(m, sim.getClock());
        }

        public byte nextByte() {
            byte val = 0;
            switch (state) {
                case TX_IN_PREAMBLE:
                    counter++;
                    if (counter >= getPreambleLength()) {
                        state = TX_SFD_1;
                    }
                    break;
                case TX_SFD_1:
                    state = TX_SFD_2;
                    val = Arithmetic.low(registers[SYNCWORD]);
                    break;
                case TX_SFD_2:
                    state = TX_LENGTH;
                    val = Arithmetic.high(registers[SYNCWORD]);
                    break;
                case TX_LENGTH:
                    if (SendAck || SendAckPend) {//ack frame
                        wasAck = true;
                        length = 5;
                    } else {//data frame
                        wasAck = false;
                        txFIFO.saveState();  // save FIFO state for later refill
                        length = txFIFO.remove() & 0x7f;
                    }
                    state = TX_IN_PACKET;
                    counter = 0;
                    crc = 0;
                    val = (byte) length;
                    SFD_value.setValue(SFD_active);
                    break;
                case TX_IN_PACKET:
                    if (!SendAck && !SendAckPend) {//data frame
                        if (txFIFO.empty()) {
                            if (printer != null) {
                              printer.println("txFIFO underflow");
                            }
                            // a transmit underflow has occurred. set the flag and stop transmitting.
                            txUnderflow.setValue(true);
                            val = 0;
                            state = TX_END;
                            // SFD is also set to inactive when an underflow occurs
                            SFD_value.setValue(!SFD_active);
                            // auto transition back to receive mode.
                            shutdown();
                            receiver.startup();// auto transition back to receive mode.
                            break;
                        }
                        //  no underflow occurred.
                        val = txFIFO.remove();
                        counter++;
                    } else {//ack frame
                        switch (counter) {
                            case 0://FCF_low
                                if (SendAck) {
                                    val = 2;
                                    break;
                                } else if (SendAckPend) {
                                    val = 9;
                                    break;
                                }
                            case 1://FCF_hi
                                val = 0;
                                break;
                            case 2://Sequence number
                                val = DSN;
                                if (SendAck) SendAck = false;
                                else if (SendAckPend) SendAckPend = false;
                                break;
                        }
                        counter++;
                    }
                    //Calculate CRC and switch state if necessary
                    if (autoCRC.getValue()) {
                        // accumulate CRC if enabled.
                        crc = crcAccumulate(crc, val);
                        if (counter >= length - 2) {
                            // switch to CRC state if when 2 bytes remain.
                            state = TX_CRC_1;
                        }
                    } else if (counter >= length) {
                        // AUTOCRC not enabled, switch to packet end mode when done.
                        state = TX_END;
                    }
                    break;
                case TX_CRC_1:
                    state = TX_CRC_2;
                    val = Arithmetic.low(crc);
                    break;
                case TX_CRC_2:
                    val = Arithmetic.high(crc);
                    state = TX_END;
                    counter = 0;
                    SFD_value.setValue(!SFD_active);
                    if (!wasAck) {  //data frame only
                      //After complete tx of data frame the txFIFO is automatically refilled
                      txFIFO.refill();
                      //writing txFIFO after frame transmitted will cause it to be flushed
                      setClearFlag();
                    }
                    // auto transition back to receive mode.
                    shutdown();
                    receiver.startup();// auto transition back to receive mode.
                    break;
                    // and fall through.
                default:
                    state = TX_IN_PREAMBLE;
                    counter = 0;
                    break;
            }
            if (printer != null) {
                printer.println("CC2420 " + StringUtil.to0xHex(val, 2) + " --------> ");
            }
            return val;
        }

        private int getPreambleLength() {
            int val = registers[MDMCTRL0] & 0xf;
            return val + 1;
        }

        void startup() {
//            if (!txActive.getValue() && !TXstartingUp){
//            TXstartingUp = true;
//                sim.insertEvent(new Simulator.Event() {
//                    public void fire() {
//                        TXstartingUp = false;
          if (!txActive.getValue()) {
                        stateMachine.transition((readRegister(TXCTRL) & 0x1f)+4);//change to Tx(Level) state
                        txActive.setValue(true);
                        state = TX_IN_PREAMBLE;
                        beginTransmit(getPower(),getFrequency());
//                        if (printer != null) {
//                            printer.println("TX Started Up");
//                        }
//                    }
//                }, toCycles(PLL_LOCK_TIME));
           }
       }

        void shutdown() {
            stateMachine.transition(2);//change to idle state
            txActive.setValue(false);
            endTransmit();
        }
    }

    char crcAccumulate(char crc, byte val) {
        int i = 8;
        crc = (char) (crc ^ val << 8);
        do {
            if ((crc & 0x8000) != 0) crc = (char) (crc << 1 ^ 0x1021);
            else crc = (char) (crc << 1);
        } while (--i > 0);
        return crc;
    }

    private static final int RECV_SFD_SCAN = 0;
    private static final int RECV_SFD_MATCHED_1 = 1;
    private static final int RECV_SFD_MATCHED_2 = 2;
    private static final int RECV_IN_PACKET = 3;
    private static final int RECV_CRC_1 = 4;
    private static final int RECV_CRC_2 = 5;
    private static final int RECV_END_STATE = 6;
    private static final int RECV_OVERFLOW = 7;

    public class Receiver extends Medium.Receiver {
        protected int state;
        protected int counter;
        protected int length;
        protected char crc;
        protected byte crcLow;

        public Receiver(Medium m) {
            super(m, sim.getClock());
        }

        public void setRssiValid (boolean v){
            rssiValid.setValue(v);
            if (v){//RSSI valid (rssi initialized to KTB - Rssi_offset=-91-45=-136)
                int cca_thr = (readRegister(RSSI) & 0xff00);
                if (cca_thr == 0) cca_thr = -32;
                int rssi_val = cca_thr;
                rssi_val = rssi_val << 8;
                int rssi =(0x00ff&-95);
                rssi_val = rssi | rssi_val;
                writeRegister(RSSI,rssi_val);
            }else{//RSSI no valid (rssi_val = -128)
                if (getRssiValid()){
                    int cca_thr = (readRegister(RSSI) & 0xff00);
                    if (cca_thr == 0) cca_thr = -32;
                    int rssi_val = cca_thr;
                    rssi_val = rssi_val << 8;
                    int rssi =(0x00ff&-128);
                    rssi_val = rssi | rssi_val;
                    writeRegister(RSSI,rssi_val);
                }
            }
        }
        public boolean getRssiValid (){
            return rssiValid.getValue();
        }
        public double getCorrelation (){
            int PERindex = (int)(getPER()*100);
            Random random = new Random();
            //get the range, casting to long to avoid overflow problems
            long range = (long)Corr_MAX[PERindex] - (long)Corr_MIN[PERindex] + 1;
            // compute a fraction of the range, 0 <= frac < range
            long fraction = (long)(range * random.nextDouble());
            double corr = fraction + Corr_MIN[PERindex];
            return corr;
        }

        public void setRSSI (double Prec){
            //compute Rssi as Pr + RssiOffset
            int rssi_val =(int)Math.rint(Prec + 45.0D);
            rssi_val = rssi_val & 0x00ff;
            int cca_thr = (readRegister(RSSI) & 0xff00);
            rssi_val = rssi_val | cca_thr;
            writeRegister(RSSI,rssi_val);
        }

        public void setBER (double BER){
            BERlist.add(new Double(BER));
        }
        public double getPER (){
            //compute average BER after SHR
            double Total = 0.0D;
            int size = BERlist.size();
            for (int i = 5; i<BERlist.size(); i++) Total+=((Double)BERlist.get(i)).doubleValue();
            BERlist.clear();
            double BER = Total/(size-5);
            //considering i.i.s errors i compute PER
            return 1D-Math.pow((1D-BER),(size-5)*8);
        }


        public byte nextByte(boolean lock, byte b) {
            if (!lock) {
                // the transmission lock has been lost
                SFD_value.setValue(!SFD_active);
                switch (state) {
                    case RECV_IN_PACKET:
                        //packet lost in middle -> rxFIFO clearing and the
                        //packet will not be passed to higher layers
                        rxFIFO.clear();
                        state = RECV_SFD_SCAN;
                        break;
                    case RECV_END_STATE:
                        if (SendAck || SendAckPend) {//Send Ack?
                            shutdown();
                            transmitter.startup();
                        } else {
                            state = RECV_SFD_SCAN;
                        }
                        break;
                    case RECV_OVERFLOW:
                        //We enconter overflow
                        break;
                    default:
                        // if we did not encounter overflow, return to the scan state.
                        state = RECV_SFD_SCAN;
                        break;
                }
                return b;
            }
            if (printer != null) {
                printer.println("CC2420 <======== " + StringUtil.to0xHex(b, 2));
            }
            switch (state) {
                case RECV_SFD_SCAN:
                    // check against the first byte of the SYNCWORD register.
                    if (b == Arithmetic.low(registers[SYNCWORD])) {
                        state = RECV_SFD_MATCHED_1;
                    } else {
                        state = RECV_SFD_SCAN;
                    }
                    break;
                case RECV_SFD_MATCHED_1:
                    // check against the second byte of the SYNCWORD register.
                    if (b == Arithmetic.high(registers[SYNCWORD])) {
                        state = RECV_SFD_MATCHED_2;
                        SFD_value.setValue(SFD_active);
                        break;
                    }
                    // fallthrough if we failed to match the second byte
                    // and try to match the first byte again.

                case RECV_SFD_MATCHED_2:
                    // SFD matched. read the length from the next byte.
                    length = b & 0x7f;
                    rxFIFO.add(b);
                    counter = 0;
                    state = RECV_IN_PACKET;
                    crc = 0;
                    break;
                case RECV_IN_PACKET:
                    // we are in the body of the packet.
                    counter++;
                    rxFIFO.add(b);
                    if (rxFIFO.overFlow()) {
                        // an RX overflow has occurred.
                        FIFO_pin.level.setValue(!FIFO_active);
                        signalFIFOP();
                        state = RECV_OVERFLOW;
                        break;
                    }

                    // no overflow occurred.
                    FIFO_pin.level.setValue(FIFO_active);
                    if (rxFIFO.size() >= getFIFOThreshold()) {
                        signalFIFOP();
                    }
                    if (autoCRC.getValue()) {
                        crc = crcAccumulate(crc, b);
                        if (counter == length - 2) {
                            // transition to receiving the CRC.
                            state = RECV_CRC_1;
                        }
                    } else if (counter == length) {
                        // no AUTOCRC, but reached end of packet.
                        state = RECV_END_STATE;
                    }
                    //Address Recognition
                    if (state == RECV_IN_PACKET && ADR_DECODE.getValue()) {
                        boolean satisfied = matchAddress(b, counter);
                        if (!satisfied) {
                            //reject frame
                            FIFO_pin.level.setValue(!FIFO_active);
                            unsignalFIFOP();
                            rxFIFO.clear();
                        }
                    }
                    else {
                      // sequence number - save it outside of address recognition since it is needed for SACK/SACKPEND commands as well!!!
                      if (counter == 3 && (rxFIFO.peek(1) & 0x07) != 0 && (rxFIFO.peek(1) & 0x04) != 4) DSN = b;
                    }

                    break;
                case RECV_CRC_1:
                    crcLow = b;
                    state = RECV_CRC_2;
                    //RSSI value is written in this position of the rxFIFO
                    b = (byte)(readRegister(RSSI)&0x00ff);
                    rxFIFO.add(b);
                    break;
                case RECV_CRC_2:
                    state = RECV_END_STATE;
                    char crcResult = (char) Arithmetic.word(crcLow, b);
                    if (crcResult == crc) {
                        // signal FIFOP and unsignal SFD
                        if (printer != null) {
                            printer.println("CC2420 CRC passed");
                        }
                        //Corr value and CRCok are written in this position of the rxFIFO
                        int corr = (int)getCorrelation();
                        b = (byte)(corr|0x0080);
                        rxFIFO.add(b);
                        signalFIFOP();
                        SFD_value.setValue(!SFD_active);
                        if (autoACK.getValue() && (rxFIFO.peek(1) & 0x20) == 0x20) {//autoACK
                            //send ack if we are not receiving ack frame
                            if ((rxFIFO.peek(1) & 0x07) != 2) {
                                if ((rxFIFO.peek(1) & 0x10) != 0x10) SendAck = true;
                                else if ((rxFIFO.peek(1) & 0x10) == 0x10) SendAckPend = true;
                            }
                        }
                    } else {
                        // CRC failure: flush the packet.
                        if (printer != null) {
                            printer.println("CC2420 CRC failed");
                        }
                        FIFO_pin.level.setValue(!FIFO_active);
                        unsignalFIFOP();
                        rxFIFO.clear();
                    }
                    break;
                case RECV_OVERFLOW:
                    // do nothing. we have encountered an overflow.
                    break;
            }
            return b;
        }

        private boolean matchAddress(byte b, int counter) {
            switch (counter) {
                case 1://frame type subfield contents an illegal frame type?
                    if ((rxFIFO.peek(1) & 0x04) == 4 && !(RESERVED_FRAME_MODE.getValue()))
                        return false;
                    break;
                case 3://Sequence number
                    if ((rxFIFO.peek(1) & 0x07) != 0 && (rxFIFO.peek(1) & 0x04) != 4) DSN = b;
                    break;
                case 5:
                    PANId = rxFIFO.peekField(4, 6);
                    macPANId = ByteFIFO.copyOfRange(RAMSecurityRegisters, 104, 106);
                    if (((rxFIFO.peek(2) >> 2) & 0x02) != 0) {//DestPANId present?
                        if (!Arrays.equals(PANId, macPANId) && !Arrays.equals(PANId, SHORT_BROADCAST_ADDR))
                            return false;
                    } else
                    if (((rxFIFO.peek(2) >> 2) & 0x03) == 0) {//DestPANId and dest addresses are not present
                        if (((rxFIFO.peek(2) >> 6) & 0x02) != 0) {//SrcPANId present
                            if ((rxFIFO.peek(1) & 0x07) == 0) {//beacon frame: SrcPANid shall match macPANId unless macPANId = 0xffff
                                if (!Arrays.equals(PANId, macPANId) && !Arrays.equals(PANId, SHORT_BROADCAST_ADDR))
                                    return false;
                            } else
                            if (((rxFIFO.peek(1) & 0x07) == 1) || ((rxFIFO.peek(1) & 0x07) == 3)) {//data or mac command
                                if (!PAN_COORDINATOR.getValue() || !Arrays.equals(PANId,macPANId)) return false;
                            }
                        }
                    }
                    break;
                case 7://If 32-bit Destination Address exits check if  match
                    ShortAddr = rxFIFO.peekField(6, 8);
                    macShortAddr = ByteFIFO.copyOfRange(RAMSecurityRegisters, 106, 108);
                    if (((rxFIFO.peek(2) >> 2) & 0x03) == 2) {
                        if (!Arrays.equals(ShortAddr, ShortAddr) && !Arrays.equals(ShortAddr, SHORT_BROADCAST_ADDR))
                            return false;
                    }
                    break;
                case 12://If 64-bit Destination Address exits check if match
                    if (((rxFIFO.peek(2) >> 2) & 0x03) == 3) {
                        LongAdr = rxFIFO.peekField(8, 16);
                        IEEEAdr = ByteFIFO.copyOfRange(RAMSecurityRegisters, 96, 104);
                        if (!Arrays.equals(LongAdr, IEEEAdr) && !Arrays.equals(LongAdr, LONG_BROADCAST_ADDR))
                            return false;
                    }
                    break;
            }
            return true;
        }

        private void signalFIFOP() {
            FIFOP_pin.level.setValue(FIFOP_active);
            if (FIFOP_interrupt > 0) {
                sim.getInterpreter().getInterruptTable().post(FIFOP_interrupt);
            }
        }

        private void unsignalFIFOP() {
            FIFOP_pin.level.setValue(!FIFOP_active);
            if (FIFOP_interrupt > 0) {
                sim.getInterpreter().getInterruptTable().unpost(FIFOP_interrupt);
            }
        }

        void startup() {
//            if (!RXstartingUp){
//                RXstartingUp = true;
//                sim.insertEvent(new Simulator.Event() {
//                    public void fire() {
                        stateMachine.transition(3);//change to receive state
                        state = RECV_SFD_SCAN;
                        beginReceive(getFrequency());
//                        RXstartingUp = false;
//                        if (printer!=null) printer.println("RX Started Up");
//                    }
//                }, toCycles(PLL_LOCK_TIME));
//            }
        }

        void shutdown() {
            stateMachine.transition(2);//change to idle state
            endReceive();
        }

        void resetOverflow() {
            state = RECV_SFD_SCAN;
        }
    }

    /**
     * The <code>CC2420Pin</code>() class models pins that are inputs and outputs to the CC2420 chip.
     */
    public class CC2420Pin implements Microcontroller.Pin.Input, Microcontroller.Pin.Output {
        protected final String name;
        protected boolean level;

        public CC2420Pin(String n) {
            name = n;
        }

        public void write(boolean level) {
            if (this.level != level) {
                // level changed
                this.level = level;
                if (this == CS_pin) pinChange_CS(level);
                else if (this == VREN_pin) pinChange_VREN(level);
                else if (this == RSTN_pin) pinChange_RSTN(level);
                if (printer != null) {
                    printer.println("CC2420 Write pin " + name + " -> " + level);
                }
            }
        }

        public boolean read() {
            if (printer != null) {
                printer.println("CC2420 Read pin " + name + " -> " + level);
            }
            return level;
        }
    }

    public class CC2420Output implements Microcontroller.Pin.Input {

        protected BooleanView level;
        protected final String name;

        public CC2420Output(String n, BooleanView lvl) {
            name = n;
            level = lvl;
        }

        public boolean read() {
            boolean val = level.getValue();
            if (printer != null) {
                printer.println("CC2420 Read pin " + name + " -> " + val);
            }
            return val;
        }
    }

    public static String regName(int reg) {
        switch (reg) {
            case MAIN:
                return "MAIN    ";
            case MDMCTRL0:
                return "MDMCTRL0";
            case MDMCTRL1:
                return "MDMCTRL1";
            case RSSI:
                return "RSSI    ";
            case SYNCWORD:
                return "SYNCWORD";
            case TXCTRL:
                return "TXCTRL  ";
            case RXCTRL0:
                return "RXCTRL0 ";
            case RXCTRL1:
                return "RXCTRL1 ";
            case FSCTRL:
                return "FSCTRL  ";
            case SECCTRL0:
                return "SECCTRL0";
            case SECCTRL1:
                return "SECCTRL1";
            case BATTMON:
                return "BATTMON ";
            case IOCFG0:
                return "IOCFG0  ";
            case IOCFG1:
                return "IOCFG1  ";
            case MANFIDL:
                return "MANFIDL ";
            case MANFIDH:
                return "MANFIDH ";
            case FSMTC:
                return "FSMTC   ";
            case MANAND:
                return "MANAND  ";
            case MANOR:
                return "MANOR   ";
            case AGCCTRL0:
                return "AGCCTRL0";
            case AGCTST0:
                return "AGCTST0 ";
            case AGCTST1:
                return "AGCTST1 ";
            case AGCTST2:
                return "AGCTST2 ";
            case FSTST0:
                return "FSTST0  ";
            case FSTST1:
                return "FSTST1  ";
            case FSTST2:
                return "FSTST2  ";
            case FSTST3:
                return "FSTST3  ";
            case RXBPFTST:
                return "RXBPFTST";
            case FSMSTATE:
                return "FSMSTATE";
            case ADCTST:
                return "ADCTST  ";
            case DACTST:
                return "DACTST  ";
            case TOPTST:
                return "TOPTST  ";
            case TXFIFO:
                return "TXFIFO  ";
            case RXFIFO:
                return "RXFIFO  ";
            default:
                return StringUtil.to0xHex(reg, 2) + "    ";
        }
    }

    public static String strobeName(int strobe) {
        switch (strobe) {
            case SNOP:
                return "SNOP    ";
            case SXOSCON:
                return "SXOSCON ";
            case STXCAL:
                return "STXCAL  ";
            case SRXON:
                return "SRXON   ";
            case STXON:
                return "STXON   ";
            case STXONCCA:
                return "STXONCCA";
            case SRFOFF:
                return "SRFOFF  ";
            case SXOSCOFF:
                return "SXOSCOFF";
            case SFLUSHRX:
                return "SFLUSHRX";
            case SFLUSHTX:
                return "SFLUSHTX";
            case SACK:
                return "SACK    ";
            case SACKPEND:
                return "SACKPEND";
            case SRXDEC:
                return "SRXDEC  ";
            case STXENC:
                return "STXENC  ";
            case SAES:
                return "SAES    ";
            default:
                return StringUtil.to0xHex(strobe, 2) + "    ";
        }
    }

    String fifoName(ByteFIFO fifo) {
        if (fifo == txFIFO) return "TX FIFO";
        if (fifo == rxFIFO) return "RX FIFO";
        return "XX FIFO";
    }

    public static String SecurityRAMName(int address) {
        if (address < 16) return "KEY0";
        else if (address < 32) return "RX_NONCE_COUNTER";
        else if (address < 48) return "SABUF";
        else if (address < 64) return "KEY1";
        else if (address < 80) return "TX_NONCE_COUNTER";
        else if (address < 96) return "CBCSTATE";
        else if (address < 104) return "IEEADR";
        else if (address < 106) return "PANID";
        else if (address < 112) return "SHORTADR";
        else return " ";
    }

    private long toCycles(long us) {
        return us * sim.getClock().getHZ() / 1000000;
    }

    public static Medium createMedium(Synchronizer synch, Medium.Arbitrator arbitrator) {
        return new Medium(synch, arbitrator, 250000, 48, 8, 8 * 128);
    }

    public Medium.Transmitter getTransmitter() {
        return transmitter;
    }

    public Medium.Receiver getReceiver() {
        return receiver;
    }

    public void setMedium(Medium m) {
        medium = m;
        transmitter = new Transmitter(m);
        receiver = new Receiver(m);
    }

    public Medium getMedium() {
        return medium;
    }

}

