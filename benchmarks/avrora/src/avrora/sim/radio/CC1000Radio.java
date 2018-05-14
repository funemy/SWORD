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

package avrora.sim.radio;

import avrora.sim.FiniteStateMachine;
import avrora.sim.Simulator;
import avrora.sim.output.SimPrinter;
import avrora.sim.clock.Clock;
import avrora.sim.clock.Synchronizer;
import avrora.sim.energy.Energy;
import avrora.sim.mcu.*;
import cck.text.StringUtil;
import cck.util.Arithmetic;

/**
 * The <code>CC1000Radio</code> class is a simulation of the CC1000 radio for use with avrora. The CC1000
 * radio is used with the Mica2 platform in the real world. Verbose printers for this class include
 * "sim.cc1000", "sim.cc1000.data", "sim.cc1000.pinconfig".
 *
 * @author Ben L. Titzer
 * @author Daniel Lee
 */
public class CC1000Radio implements Radio {

    private static final double FXOSC_FREQUENCY = 14745600.0;

    /**
     * Register addresses.
     */
    public static final int MAIN = 0x00;
    public static final int FREQ_2A = 0x01;
    public static final int FREQ_1A = 0x02;
    public static final int FREQ_0A = 0x03;
    public static final int FREQ_2B = 0x04;
    public static final int FREQ_1B = 0x05;
    public static final int FREQ_0B = 0x06;
    public static final int FSEP1 = 0x07;
    public static final int FSEP0 = 0x08;
    public static final int CURRENT = 0x09;
    public static final int FRONT_END = 0x0a;
    public static final int PA_POW = 0x0b;
    public static final int PLL = 0x0c;
    public static final int LOCK = 0x0d;
    public static final int CAL = 0x0e;
    public static final int MODEM2 = 0x0f;
    public static final int MODEM1 = 0x10;
    public static final int MODEM0 = 0x11;
    public static final int MATCH = 0x12;
    public static final int FSCTRL = 0x13;
    public static final int PRESCALER = 0x1c;
    public static final int TEST6 = 0x40;
    public static final int TEST5 = 0x41;
    public static final int TEST4 = 0x42;
    public static final int TEST3 = 0x43;
    public static final int TEST2 = 0x44;
    public static final int TEST1 = 0x45;
    public static final int TEST0 = 0x46;

    protected static final String[] allModeNames = RadioEnergy.allModeNames();
    protected static final int[][] ttm = FiniteStateMachine.buildSparseTTM(allModeNames.length, 0);

    protected RadioRegister[] registers  = new RadioRegister[0x47];

    /**
     * Registers
     */
    protected final MainRegister MAIN_reg;
    protected final FrequencyRegister FREQ_A_reg;
    protected final FrequencyRegister FREQ_B_reg;
    protected final FrequencySeparationRegister FSEP_reg;
    protected final CurrentRegister CURRENT_reg;
    protected final FrontEndRegister FRONT_END_reg;
    protected final PA_POWRegister PA_POW_reg;
    protected final PLLRegister PLL_reg;
    protected final LockRegister LOCK_reg;
    protected final CALRegister CAL_reg;
    protected final Modem2Register MODEM_2_reg;
    protected final Modem1Register MODEM_1_reg;
    protected final Modem0Register MODEM_0_reg;
    protected final MatchRegister MATCH_reg;
    protected final FSCTRLRegister FSCTRL_reg;
    protected final PrescalerRegister PRESCALER_reg;

    protected final SimPrinter radioPrinter;


    protected final long xoscFrequency;

    protected FrequencyRegister currentFrequencyRegister;

    /**
     * Connected Microcontroller, Simulator and SimulatorThread should all correspond.
     */
    protected final Microcontroller mcu;
    protected final Simulator sim;
    protected final Clock clock;
    protected final FiniteStateMachine stateMachine;
    public final CC1000Radio.SerialConfigurationInterface config;

    public CC1000Radio(Microcontroller mcu, long xfreq) {
        xoscFrequency = xfreq;

        this.mcu = mcu;
        this.sim = mcu.getSimulator();
        this.clock = sim.getClock();

        radioPrinter = sim.getPrinter("radio.cc1000");

        for (int i = 0x14; i < registers.length; i++) {
            registers[i] = new DummyRegister(i);
        }

        registers[MAIN] = MAIN_reg = new MainRegister();

        FREQ_A_reg = new FrequencyRegister("A");
        registers[FREQ_2A] = FREQ_A_reg.reg2;
        registers[FREQ_1A] = FREQ_A_reg.reg1;
        registers[FREQ_0A] = FREQ_A_reg.reg0;

        FREQ_B_reg = new FrequencyRegister("B");
        registers[FREQ_2B] = FREQ_B_reg.reg2;
        registers[FREQ_1B] = FREQ_B_reg.reg1;
        registers[FREQ_0B] = FREQ_B_reg.reg0;

        FSEP_reg = new FrequencySeparationRegister();
        registers[FSEP1] = FSEP_reg.reg1;
        registers[FSEP0] = FSEP_reg.reg0;

        registers[CURRENT] = CURRENT_reg = new CurrentRegister();
        registers[FRONT_END] = FRONT_END_reg = new FrontEndRegister();
        registers[PA_POW] = PA_POW_reg = new PA_POWRegister();
        registers[PLL] = PLL_reg = new PLLRegister();
        registers[LOCK] = LOCK_reg = new LockRegister();
        registers[CAL] = CAL_reg  = new CALRegister();
        registers[MODEM2] = MODEM_2_reg = new Modem2Register();
        registers[MODEM1] = MODEM_1_reg = new Modem1Register();
        registers[MODEM0] = MODEM_0_reg = new Modem0Register();
        registers[MATCH] = MATCH_reg = new MatchRegister();
        registers[FSCTRL] = FSCTRL_reg = new FSCTRLRegister();
        registers[PRESCALER] = PRESCALER_reg = new PrescalerRegister();

        //setup energy recording
        Simulator simulator = mcu.getSimulator();

        stateMachine = new FiniteStateMachine(simulator.getClock(), RadioEnergy.startMode, allModeNames, ttm);

        new Energy("Radio", RadioEnergy.modeAmpere, stateMachine, sim.getSimulation().getEnergyControl());

        ATMegaFamily amcu = (ATMegaFamily) mcu;
        ticker = new SPITicker();
        ticker.spiDevice = (SPIDevice)amcu.getDevice("spi");
        setMedium(createMedium(null, null));
        rssiOutput = new RSSIOutput();
        ADC adc = ((ADC) amcu.getDevice("adc"));
        adc.connectADCInput(rssiOutput, 0);
        config = new SerialConfigurationInterface();
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

    /**
     * The <code>RadioRegister</code> is an abstract register grouping together registers on the CC1000
     * radio.
     */
    protected abstract class RadioRegister {
        protected final String id; // name of this register

        protected byte value; // current value of this register

        RadioRegister(String id, byte def) {
            this.id = id;
            this.value = def;
        }

        public void write(byte val) {
            value = val;
            decode(value);
            if (radioPrinter != null) {
                printStatus();
            }
        }

        protected abstract void decode(byte val);

        protected void printStatus() {
            // default: do nothing
        }
    }

    /**
     * The <code>DummyRegister</code> is a filler class for registers within the 7-bit address space of the
     * radio registers, but do not actually exist/do anything in the real radio.
     */
    protected class DummyRegister extends RadioRegister {
        DummyRegister(int i) {
            super("Dummy " + Integer.toHexString(i), (byte)0);
        }

        protected void decode(byte val) {
        }
    }

    /**
     * The main register on the CC1000.
     */
    protected class MainRegister extends RadioRegister {
        public static final int RXTX = 7;      // 0: RX, 1: TX
        public static final int F_REG = 6;     // 0: A, 1: B
        public static final int RX_PD = 5;     // Power down of RX part of interface
        public static final int TX_PD = 4;     // Power down of TX part of interface
        public static final int FS_PD = 3;     // Power down of Frequency Synthesizer
        public static final int CORE_PD = 2;   // Power down of Crystal Oscillator Core
        public static final int BIAS_PD = 1;   // Power down of BIAS and Crystal Oscillator Buffer
        public static final int RESET_N = 0;   // Reset other registers to default value

        boolean rxtx;
        boolean fReg;
        boolean rxPd;
        boolean txPd;
        boolean fsPd;
        boolean corePd;
        boolean biasPd;
        boolean resetN;

        byte oldVal;

        boolean transmit_activated;
        boolean receive_activated;

        MainRegister() {
            super("MAIN", (byte)0x3e);
        }

        protected void decode(byte val) {

            rxtx = Arithmetic.getBit(val, RXTX);
            fReg = Arithmetic.getBit(val, F_REG);
            rxPd = Arithmetic.getBit(val, RX_PD);
            txPd = Arithmetic.getBit(val, TX_PD);
            fsPd = Arithmetic.getBit(val, FS_PD);
            corePd = Arithmetic.getBit(val, CORE_PD);
            biasPd = Arithmetic.getBit(val, BIAS_PD);
            resetN = Arithmetic.getBit(val, RESET_N);

            if (rxPd) receiver.endReceive();
            else receiver.beginReceive(2.4);

            if (txPd) transmitter.endTransmit();
            else transmitter.beginTransmit(getPower(),getFrequency());

            if (!rxPd || !txPd) {
                ticker.activate();
            } else {
                ticker.deactivate();
            }

            boolean oldrxtx = Arithmetic.getBit(oldVal, RXTX);
            if (rxtx && !oldrxtx) {
                // switch from receive to transmit
                if (radioPrinter != null) {
                    radioPrinter.println("CC1000: RX end receiving -> begin transmitting");
                }
            } else if (!rxtx && oldrxtx) {
                // switch from transmit to receive
                if (radioPrinter != null) {
                    radioPrinter.println("CC1000: TX end transmitting -> begin receiving");
                }
            }

            currentFrequencyRegister = fReg ? FREQ_B_reg : FREQ_A_reg;

            if (resetN && !Arithmetic.getBit(oldVal, RESET_N)) {
                oldVal = val;
                // TODO: reset the radio.
                return;
            }

            if (val != oldVal) {
                // TODO: reduce this code to compute state more easily
                int state;
                if (corePd) state = 1; //power down state
                else state = 2; // core, e.g. crystal on state
                if (!corePd && !biasPd) state = 3; // crystal and bias on state
                if (!corePd && !biasPd && !fsPd) state = 4; // crystal, bias and synth. on
                if (!corePd && !biasPd && !fsPd && !rxtx && !rxPd) state = 5; // receive state
                if (!corePd && !biasPd && !fsPd && rxtx && !txPd) state = PA_POW_reg.getPower() + 6;
                stateMachine.transition(state);
            }

            oldVal = val;
        }

        protected void printStatus() {
            String rxtxS = rxtx ? "TX" : "RX";
            String fRegS = fReg ? "B" : "A";
            StringBuffer buf = new StringBuffer(100);

            buf.append("CC1000[MAIN]: ");
            buf.append(rxtxS);
            buf.append(", freg: ");
            buf.append(fRegS);
            buf.append(", rx pd: ");
            buf.append(StringUtil.toBit(rxPd));
            buf.append(", tx pd: ");
            buf.append(StringUtil.toBit(txPd));
            buf.append(", fs pd: ");
            buf.append(StringUtil.toBit(fsPd));
            buf.append(", core pd: ");
            buf.append(StringUtil.toBit(corePd));
            buf.append(", bias pd: ");
            buf.append(StringUtil.toBit(biasPd));
            buf.append(", reset: ");
            buf.append(StringUtil.toBit(resetN));
            radioPrinter.println(buf.toString());
        }

    }

    /**
     * A frequency register on the CC1000. It is divided into three 8-bit registers.
     */
    protected class FrequencyRegister {
        // TODO: use stacked register view.
        protected final FrequencySubRegister reg2;
        protected final FrequencySubRegister reg1;
        protected final FrequencySubRegister reg0;

        int frequency;

        // subId should be either A or b
        FrequencyRegister(String subId) {

            reg2 = new FrequencySubRegister("FREQ2" + subId);
            reg1 = new FrequencySubRegister("FREQ1" + subId);
            reg0 = new FrequencySubRegister("FREQ0" + subId);

            setFrequency(0x75a0cb); // default frequency is 0b 01111 0101 1010 0000 1100 1011,
        }

        protected void updateFrequency() {
            frequency = 0x00ff0000 & (reg2.value << 16);
            frequency |= 0x0000ff00 & (reg1.value << 8);
            frequency |= 0x000000ff & reg0.value;
        }

        protected void setFrequency(int frequency) {
            reg2.write((byte)((0x00ff0000 & frequency) >> 16));
            reg1.write((byte)((0x0000ff00 & frequency) >> 8));
            reg0.write((byte)((0x000000ff & frequency)));
        }

        /**
         * One of the three sub-registers in the 24-bit frequency register.
         */
        protected class FrequencySubRegister extends RadioRegister {

            FrequencySubRegister(String id) {
                super(id, (byte)0);
            }

            protected void decode(byte val) {
                updateFrequency();
            }
        }

    }

    /**
     * The frequency separation register on the CC1000. It is divided into two 8-bit registers.
     */
    protected class FrequencySeparationRegister {
        // TODO: use stacked register view.
        protected final SubRegister reg1 = new SubRegister("FSEP1");
        protected final SubRegister reg0 = new SubRegister("FSEP0");

        FrequencySeparationRegister() {
            setFrequencySeparation(0x59); // default frequency separation is 0b 0000 0000 0101 1001
        }

        int frequencySeparation;

        protected void updateFrequencySeparation() {
            frequencySeparation = (reg1.value & 0x0f) << 8;
            frequencySeparation |= reg0.value;
        }

        protected void setFrequencySeparation(int val) {
            reg1.write((byte)((0x0f00 & val) >> 8));
            reg0.write((byte)(0xff & val));
        }

        /**
         * One of the two sub-registers in the 18-bit frequency separation register.
         */
        protected class SubRegister extends RadioRegister {

            SubRegister(String id) {
                super(id, (byte)0);
            }

            protected void decode(byte val) {
                updateFrequencySeparation();
            }

        }
    }

    static final int[] VCO_CURRENT = {150, 250, 350, 450, 950, 1050, 1150, 1250,
                                   1450, 1550, 1650, 1750, 2250, 2350, 2450, 2550}; // in microamperes

    static final double[] LO_DRIVE = {0.5, 1.0, 1.5, 2.0}; // in milliamperes
    static final int[] PA_DRIVE = {1, 2, 3, 4}; // in milliamperes

    /**
     * The <code>CurrentRegister</code> controls various currents running through the CC1000 wiring.
     */
    protected class CurrentRegister extends RadioRegister {

        int vcoCurrent = 150;
        double loDrive = 0.5;
        int paDrive = 1;

        CurrentRegister() {
            super("CURRENT", (byte)0xca);
            // default value 0b 1100 1010
        }

        protected void decode(byte val) {
            vcoCurrent = VCO_CURRENT[(val & 0xf0) >> 4];
            loDrive = LO_DRIVE[(val & 0x0c) >> 2];
            paDrive = PA_DRIVE[(val & 0x3)];
        }

        protected void printStatus() {
            radioPrinter.println("CC1000[CURRENT]: vco current: " + vcoCurrent + ", LO drive: " + loDrive
                    + ", PA drive: " + paDrive);
        }
    }

    static final int[] BUF_CURRENT = {520, 690}; // in microamperes
    static final double[] LNA_CURRENT = {0.8, 1.4, 1.8, 2.2}; // in milliamperes

    protected class FrontEndRegister extends RadioRegister {

        int bufCurrent = 520;

        double lnaCurrent = 0.8;

        static final int IF_RSSI_INACTIVE = 0;
        static final int IF_RSSI_ACTIVE = 1;
        static final int IF_RSSI_MIXER = 2;
        int ifRSSI;

        boolean xoscBypassExternal;

        FrontEndRegister() {
            super("FRONT_END", (byte)0);
        }

        protected void decode(byte val) {
            bufCurrent = BUF_CURRENT[(val & 0x20) >> 5];
            lnaCurrent = LNA_CURRENT[(val & 0x18) >> 3];
            ifRSSI = (val & 0x06) >> 1;

            xoscBypassExternal = Arithmetic.getBit(val, 0);

        }
    }

    protected class PA_POWRegister extends RadioRegister {

        int paHighPower;
        int paLowPower;

        PA_POWRegister() {
            super("PA_POW", (byte)0x0f);
            // default value 0b 0000 1111
        }

        protected void decode(byte val) {
            paHighPower = (value & 0xf0) >> 4;
            paLowPower = (value & 0x0f);

            // TODO: probes.fireAtPowerChange(CC1000Radio.this, getPower());

            //start energy tracking
            //check for transmission mode enabled
            if (!MAIN_reg.corePd && !MAIN_reg.biasPd && !MAIN_reg.fsPd && MAIN_reg.rxtx && !MAIN_reg.txPd)
                stateMachine.transition(getPower() + 6);
        }

        protected int getPower() {
            return value & 0xff;
        }

        protected void printStatus() {
            radioPrinter.println("CC1000[PA_POW]: PA high power: " + paHighPower + ", PA low power: " + paLowPower);
        }
    }

    protected class PLLRegister extends RadioRegister {
        boolean extFilter;
        int refDiv;
        boolean alarmDisable;
        boolean alarmHigh;
        boolean alarmLow;

        PLLRegister() {
            super("PLL", (byte)0x10);
            // default value 0b 00010000
        }

        protected void decode(byte val) {
            extFilter = Arithmetic.getBit(val, 7);
            refDiv = (value & 0x78) >> 3;
            alarmDisable = Arithmetic.getBit(val, 2);
            alarmHigh = Arithmetic.getBit(val, 1);
            alarmLow = Arithmetic.getBit(val, 0);
        }
    }

    //PLL_LOCK_ACCURACY
    static final int[] SETS_LOCK_THRESHOLD = {127, 31};
    static final int[] RESET_LOCK_THRESHOLD = {111, 15};

    protected class LockRegister extends RadioRegister {

        static final int LOCK_NORMAL = 0;
        static final int LOCK_CONTINUOUS = 1;
        static final int LOCK_INSTANT = 2;
        static final int ALARM_H = 3;
        static final int ALARM_L = 4;
        static final int CAL_COMPLETE = 5;
        static final int IF_OUT = 6;
        static final int REFERENCE_DIVIDER = 7;
        static final int TX_DPB = 8;
        static final int MANCHESTER_VIOLATION = 9;
        static final int RX_PDB = 10;
        // 11 undefined
        // 12 undefined
        static final int LOCK_AVG_FILTER = 13;
        static final int N_DIVIDER = 14;
        static final int F_COMP = 15;

        final String[] LOCK_SELECT = {"LOCK NORMAL", "LOCK CONTINUOUS", "LOCK INSTANT", "ALARM HIGH", "ALARM LOW",
                                      "CAL COMPLETE", "IF OUT", "REFERENCE DIVIDER", "TX DPB", "MANCHESTER VIOLATION",
                                      "RX PDB", "NOT DEFINED (11)", "NOT DEFINED (12)", "LOCK AVG FILTER",
                                      "N DIVIDER", "F COMP"};

        int lockSelect;

        boolean pllLockLength;

        int setsLockThreshold = 127;
        int resetLockThreshold = 111;

        boolean lockInstant;
        boolean lockContinuous;

        LockRegister() {
            super("LOCK", (byte)0);
        }

        protected void decode(byte val) {
            lockSelect = (val & 0xf0) >> 4;
            int pllLockAccuracy = (val & 0x0c) >> 2;
            setsLockThreshold = SETS_LOCK_THRESHOLD[pllLockAccuracy];
            resetLockThreshold = RESET_LOCK_THRESHOLD[pllLockAccuracy];

            pllLockLength = Arithmetic.getBit(val, 2);
            lockInstant = Arithmetic.getBit(val, 1);
            lockContinuous = Arithmetic.getBit(val, 0);
        }


        protected void printStatus() {
            StringBuffer buf = new StringBuffer(100);
            buf.append("CC1000[LOCK]: lock select: ");
            buf.append(LOCK_SELECT[lockSelect]);
            buf.append(", set thr: ");
            buf.append(setsLockThreshold);
            buf.append(", reset thr: ");
            buf.append(resetLockThreshold);
            buf.append(", inst: ");
            buf.append(StringUtil.toBit(lockInstant));
            buf.append(", contin: ");
            buf.append(StringUtil.toBit(lockContinuous));
            radioPrinter.println(buf.toString());
        }

        public byte read() {
            return (byte)(value & 0x03);
        }
    }

    protected class CALRegister extends RadioRegister {

        static final int CAL_START = 7;
        static final int CAL_DUAL = 6;
        static final int CAL_WAIT = 5;
        static final int CAL_CURRENT = 4;
        static final int CAL_COMPLETE = 3;

        boolean calStart;
        boolean calDual;
        boolean calWait;
        boolean calCurrent;
        boolean calComplete;

        static final int CAL_ITERATE_NORMAL = 0x6;

        int calIterate;

        Calibrate calibrate = new Calibrate();


        CALRegister() {
            super("CAL", (byte)0x05);
            // default value 0b 00000101
        }

        boolean calibrating;

        protected void decode(byte val) {
            boolean oldCalStart = calStart;
            calStart = Arithmetic.getBit(val, 7);
            calDual = Arithmetic.getBit(val, 6);
            calWait = Arithmetic.getBit(val, 5);
            calCurrent = Arithmetic.getBit(val, 4);
            calComplete = Arithmetic.getBit(val, 3);

            calIterate = (value & 0x7);

            if (!oldCalStart && calStart && !calibrating) {
                calibrating = true;
                //OL: calibration time depends on the reference frequency
                //worst case is 34ms
                //it is determined with: 34ms * 1MHz / (Fxosc / REFDIV)
                //with Fxosc is 14.7456 MHz for CC1000 on Mica2
                //and REFDIV is set in the PLL register
                //in the current TinyOS version (1.1.7) REFDIV seems to be 14
                //resulting in a delay of a little more than 32ms
                //Reference: CC1000 datasheet (rev 2.1) pages 20 and 22
                double calMs = (34.0 * 1000000.0 / FXOSC_FREQUENCY) * PLL_reg.refDiv;
                clock.insertEvent(calibrate, clock.millisToCycles(calMs));
            }

        }

        protected void printStatus() {
            StringBuffer buf = new StringBuffer(100);
            buf.append("CC1000[CAL]: cal start: ");
            buf.append(StringUtil.toBit(calStart));
            buf.append(", dual: ");
            buf.append(StringUtil.toBit(calDual));
            buf.append(", wait: ");
            buf.append(StringUtil.toBit(calWait));
            buf.append(", current: ");
            buf.append(StringUtil.toBit(calCurrent));
            buf.append(", complete: ");
            buf.append(StringUtil.toBit(calComplete));
            buf.append(", iterate: ");
            buf.append(calIterate);
            radioPrinter.println(buf.toString());
        }

        /** */
        protected class Calibrate implements Simulator.Event {

            public void fire() {
                // TODO: multiple calls to decode()
                value = Arithmetic.setBit(value, CAL_START, false);
                decode(value);
                value = Arithmetic.setBit(value, CAL_COMPLETE, true);
                decode(value);
                LOCK_reg.write((byte)((LOCK_reg.read() & 0x0f) | 0x50)); // LOCK = CAL_COMPLETE
                if (radioPrinter != null) {
                    radioPrinter.println("CC1000: Calibration complete ");
                }
                calibrating = false;
            }
        }

    }

    protected class Modem2Register extends RadioRegister {

        boolean peakDetect;
        int peakLevelOffset;

        Modem2Register() {
            super("MODEM2", (byte)0x96);
            // default value 0b 1001 0110
        }

        protected void decode(byte val) {
            peakDetect = Arithmetic.getBit(val, 7);
            peakLevelOffset = val & 0x7f;
        }
    }

    static final int[] SETTLING = {11, 22, 43, 86};

    protected class Modem1Register extends RadioRegister {

        int mlimit;

        boolean lockAvgN;

        boolean lockAvgMode;

        int settling = 11;

        boolean modemResetN;

        Modem1Register() {
            super("MODEM1", (byte)0x67);
            // default value 0b 0110 0111
        }

        protected void decode(byte val) {
            mlimit = (val & 0xe0) >> 5;
            lockAvgN = Arithmetic.getBit(val, 4);
            lockAvgMode = Arithmetic.getBit(val, 3);
            settling = SETTLING[(val & 0x06) >> 1];
            modemResetN = Arithmetic.getBit(val, 0);
        }
    }

    static final int[] BAUDRATE = {600, 1200, 2400, 4800, 9600, 19200, 0, 0};
    static final int[] XOSC_FREQ = {3686400, // 3-4 Mhz
                             7372800, // 6-8 Mhz
                             1105920, // 9-12 Mhz
                             1474560};// 12-16 Mhz

    /**
     * The baud rate of the system is determined by values on the MODEM0 register. TinyOS uses a baud rate of
     * 19.2 kBaud with manchester encoding, which translates into 9.6 kbps of data.
     */
    protected class Modem0Register extends RadioRegister {
        int baudrate = 2400;
        int bitrate = 1200;

        static final int DATA_FORMAT_NRZ = 0;
        static final int DATA_FORMAT_MANCHESTER = 1;
        static final int DATA_FORMAT_UART = 2;
        int dataFormat = DATA_FORMAT_MANCHESTER;

        int xoscFreqRange = XOSC_FREQ[0];

        Modem0Register() {
            super("MODEM0", (byte)0x24);
            decode(value);
            // default value 0b 0010 0100
        }

        protected void decode(byte val) {

            int baudIndex = (val & 0x70) >> 4;
            int xoscIndex = (val & 0x3);
            dataFormat = (val & 0x0c) >> 2;
            xoscFreqRange = XOSC_FREQ[xoscIndex];
            calculateBaudRate(baudIndex, xoscIndex);
            bitrate = baudrate / (dataFormat == DATA_FORMAT_MANCHESTER ? 2 : 1);
            // TODO: probes.fireAtBitRateChange(CC1000Radio.this, bitrate);
        }

        private void calculateBaudRate(int baudIndex, int xoscIndex) {
            if ( baudIndex == 5 && xoscFrequency > XOSC_FREQ[2]) {
                if ( xoscIndex == 0 ) baudrate = 76800;
                else if ( xoscIndex == 1 ) baudrate = 38400;
                else baudrate = BAUDRATE[baudIndex];
            } else {
                baudrate = BAUDRATE[baudIndex];
            }
        }

        protected void printStatus() {
            radioPrinter.println("CC1000[MODEM0]: "+baudrate+" baud, "+bitrate+" bit rate, manchester: "+
                    (dataFormat == DATA_FORMAT_MANCHESTER));
        }
    }

    protected class MatchRegister extends RadioRegister {
        int rxMatch;
        int txMatch;

        MatchRegister() {
            super("MATCH", (byte)0);
        }

        protected void decode(byte val) {
            rxMatch = (val & 0xf0) >> 4;
            txMatch = (val & 0x0f);
        }

    }

    protected class FSCTRLRegister extends RadioRegister {

        boolean fsResetN;

        FSCTRLRegister() {
            super("FSCTRL", (byte)0x01);
            // default value 0b 0000 0001
        }

        protected void decode(byte val) {
            fsResetN = Arithmetic.getBit(val, 0);
        }
    }

    static final double[] PRE_SWING = {1.0, 2.0 / 3, 7.0 / 3, 5.0 / 3};
    static final double[] PRE_CURRENT = {1.0, 2.0 / 3, 1.0 / 2, 2.0 / 5};

    protected class PrescalerRegister extends RadioRegister {

        double preSwing = 1.0;

        double preCurrent = 1.0;

        boolean ifInput;
        boolean ifFront;

        PrescalerRegister() {
            super("PRESCALER", (byte)0);
        }

        protected void decode(byte val) {
            preSwing = PRE_SWING[(val & 0xc0) >> 6];
            preCurrent = PRE_CURRENT[(val & 0x30) >> 4];
            ifInput = Arithmetic.getBit(val, 3);
            ifFront = Arithmetic.getBit(val, 4);
        }

    }


    /**
     * Reads the three pins used in the three wire serial configuration interface. Microcontrollers can
     * program this radio by communication over this interfance. Debug output for communication over this
     * interface is available on "sim.cc1000.pinconfig"
     */
    public class SerialConfigurationInterface {

        public final PCLKOutput PCLK_in = new PCLKOutput();
        public final PDATAOutput PDATA_in = new PDATAOutput();
        public final PDATAInput PDATA_out = new PDATAInput();
        public final PALEOutput PALE_in = new PALEOutput();

        byte address;
        boolean writeCommand;
        int writeValue;
        boolean inputPin;

        byte readData;

        int bitsRead;

        SimPrinter readerPrinter;

        SerialConfigurationInterface() {

            readerPrinter = sim.getPrinter("radio.cc1000.pinconfig");
        }

        /**
         * Clocking the PCLK pin is what drives the action of the configuration interface. One bit of data on
         * PDATA per clock.
         */
        protected class PCLKOutput implements Microcontroller.Pin.Output {
            protected boolean last;
            public void write(boolean level) {
                // only trigger on level changes
                if (level != last) {
                    if (!level) clockInBit(); // perform action on falling edge.
                    last = level;
                }
            }
        }

        protected class PDATAInput implements Microcontroller.Pin.Input {
            boolean outputPin;
            public boolean read() {
                return outputPin;
            }
        }

        protected class PDATAOutput implements Microcontroller.Pin.Output {

            public void write(boolean level) {
                inputPin = level;
            }
        }

        protected class PALEOutput implements Microcontroller.Pin.Output {
            protected boolean last;
            public void write(boolean level) {
                if ( level == last ) return;
                if (!level) {
                    bitsRead = 0;
                } else {
                    bitsRead = 8;
                }
                last = level;
            }
        }

        private void clockInBit() {
            if (bitsRead < 7) {
                // the first 7 bits are the address
                address <<= 1;
                address |= inputPin ? 0x1 : 0x0;
            } else if (bitsRead == 7) {
                // the 8th bit is the read/write bit
                writeCommand = inputPin;
                if (!writeCommand) {
                    readData = registers[address].value;
                    outputReadBit();
                }
            } else if ( bitsRead < 16 ) {
                // the 9-16th bits are either the value to write or the value of the register
                if (writeCommand) {
                    // shift in the new bit into the value to write
                    inputWriteBit();
                } else {
                    // shift out another bit from the register value
                    outputReadBit();
                }
            }
            bitsRead++;
            if (bitsRead == 16) {
                // complete the command.
                if (writeCommand) {
                    registers[address].write((byte)writeValue);
                    if (readerPrinter != null)
                        readerPrinter.println("CC1000.Reg[" + StringUtil.toHex(address, 2) + "] <= " + StringUtil.toMultirepString(writeValue, 8));
                } else {
                    if (readerPrinter != null)
                        readerPrinter.println("CC1000.Reg[" + StringUtil.toHex(address, 2) + "] -> " + StringUtil.toMultirepString(readData, 8));
                }
                // reset the state
                bitsRead = 0;
                address = 0;
            }
        }

        private void inputWriteBit() {
            writeValue = writeValue << 1 | (inputPin ? 0x1 : 0x0);
        }

        private void outputReadBit() {
            PDATA_out.outputPin = Arithmetic.getBit(readData, 14 - bitsRead);
        }

    }


    /**
     * get the transmission power (dB)
     */
    public double getPower() {
        double powerSet = (double)PA_POW_reg.getPower();
        double power;
        //convert to dB (by linearization) we distinguish values less than 16
        //and higher ones. Probably a lookup table and Spline
        //interpolation would be nice here
        if (powerSet < 16)
            power = 0.12 * powerSet - 1.8;
        else
            power = 0.00431 * powerSet - 0.06459;

      return power;
   }

    /**
     * get transmission frequency (Mhz)
     */
    public double getFrequency() {
        double fref = FXOSC_FREQUENCY / PLL_reg.refDiv;
        int freq = !MAIN_reg.fReg ? FREQ_A_reg.frequency : FREQ_B_reg.frequency;
        return fref * (freq + 8192) / (16384*1E6);
    }

    private class SPITicker implements Simulator.Event {
        protected SPIDevice spiDevice;
        protected boolean activated;
        public void fire() {
            // exchange a byte with the SPI device.
            spiTick = clock.getCount();
            SPI.Frame frame = spiDevice.exchange(SPI.newFrame((byte)(rxBuffer >> 8)));
            txBuffer = frame.data;
            clock.insertEvent(this, receiver.cyclesPerByte);
        }
        protected void activate() {
            if (!activated) {
                activated = true;
                clock.insertEvent(this, receiver.cyclesPerByte);
            }
        }
        protected void deactivate() {
            if (activated) {
                activated = false;
                clock.removeEvent(this);
            }
        }
    }

    private class Transmitter extends Medium.Transmitter {
        Transmitter(Medium m) {
            super(m, sim.getClock());
        }
        public byte nextByte() {
            if (radioPrinter != null) {
                radioPrinter.println("CC1000 "+StringUtil.to0xHex(txBuffer, 2)+" --------> ");
            }
            return txBuffer;
        }
    }

    private class Receiver extends Medium.Receiver {
        Receiver(Medium m) {
            super(m, sim.getClock());
        }
        //cc2420 functions that have to be written by radio
         public void setRssiValid (boolean v){
             //do nothing
         }
          public boolean getRssiValid (){
              //do nothing
              return false;
         }
          public void setRSSI (double PRec){
              //do nothing
          }
          public void setBER (double BER){
            //do nothing
        }
        public byte nextByte(boolean lock, byte val) {
            if (lock) {
                int delta = (int)(clock.getCount() - spiTick);

                int offset = (int)(8 * (delta % cyclesPerByte) / cyclesPerByte);
                // shift in the new bits
                rxBuffer = (rxBuffer << 8) | (~val & 0xff) << offset;
                if (radioPrinter != null) {
                    radioPrinter.println("CC1000 <======== "+StringUtil.to0xHex(val, 2));
                }
            } else {
                rxBuffer = 0;
                if (radioPrinter != null) {
                    radioPrinter.println("CC1000 lock lost");
                }
            }
            return val;
        }
    }

    private class RSSIOutput implements ADC.ADCInput {
        public float getVoltage() {
            if (receiver.isChannelClear(0,0)) {
                return ADC.VBG_LEVEL;
            }
            else return 0.000f;
        }
    }

    protected Medium medium;
    protected Transmitter transmitter;
    protected Receiver receiver;
    protected SPITicker ticker;
    protected RSSIOutput rssiOutput;

    long spiTick;
    byte txBuffer;
    int rxBuffer;

    public static Medium createMedium(Synchronizer synch, Medium.Arbitrator arb) {
        // TODO: we only support 19200 kbit/s
        return new Medium(synch, arb, 19200, 4, 8, 128 * 8);
    }

    public Simulator getSimulator() {
        return sim;
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