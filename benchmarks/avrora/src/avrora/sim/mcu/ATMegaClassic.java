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
import cck.util.Util;

/**
 * The <code>ATMegaFamily</code> class encapsulates much of the common functionality among the
 * ATMega family microcontrollers from Atmel.
 *
 * @author Pekka Nikander
 * @author Ben L. Titzer
 */
public abstract class ATMegaClassic extends ATMegaFamilyNew {

    protected ATMegaClassic(ClockDomain cd, AVRProperties p, FiniteStateMachine fsm) {
        super(cd, p, fsm);
    }

    protected abstract class Timer8Bit extends ATMegaTimer {

        final TCNTnRegister TCNTn_reg;
        final Mode[] modes;

        protected Timer8Bit(int n, AtmelMicrocontroller m, int[] periods, String ovfName, String acfName) {
            super(n, m, periods, ovfName);

            // Install a Decorater to the TCNT register
            TCNTn_reg = new TCNTnRegister("TCNT" + n, m.getIOReg("TCNT" + n));
            m.installIOReg(TCNTn_reg.name, TCNTn_reg);

            AtmelMicrocontroller.Pin pin = (AtmelMicrocontroller.Pin) m.getPin("OC" + timerNumber);
            int interrupt = m.properties.getInterrupt(acfName);
            addComparator(Comparator._, new OutputCompareUnit(n, m, Comparator._, interrupt, pin));

            String ocfn = "OCF" + n;
            String ocrn = "OCR" + n;
            modes = new Mode[]{
                    new Mode(Mode.NORMAL.class, null, FixedTop.FF),
                    new Mode(Mode.FC_PWM.class, null, FixedTop.FF),
                    new Mode(Mode.CTC.class, m.getRegisterSet().getField(ocfn), m.getIOReg(ocrn)),
                    new Mode(Mode.FAST_PWM.class, null, FixedTop.FF)};

            resetMode(0);// XXX: is this always the default mode?
        }

        public int getCounter() {
            return TCNTn_reg.read();
        }

        public void setCounter(int count) {
            TCNTn_reg.write((byte) count);
        }

        public String getCounterName() {
            return "TCNT" + timerNumber;
        }

        public int getMax() {
            return 0xFF;
        }

        public void resetMode(int WGMn) {
            mode = modes[WGMn];
        }

        class OutputCompareUnit extends OutputComparator {
            final BufferedRegister OCRn_reg;

            OutputCompareUnit(int timerNumber, AtmelMicrocontroller m, String unit, int interruptNumber, AtmelMicrocontroller.Pin pin) {
                super(unit, m.getRegisterSet(), interruptNumber, pin);
                String name = "OCR" + timerNumber + unit;
                OCRn_reg = new BufferedRegister(m.getIOReg(name));
                m.installIOReg(name, OCRn_reg);
            }

            int read() {
                return OCRn_reg.read16();
            }

            int readBuffer() {
                return OCRn_reg.readBuffer();
            }
        }

    }

    protected abstract class Timer16Bit extends ATMegaTimer {
        final RW16Register TCNTn_reg;// The underlying 16-bit register
        final HighRegister TCNTnH_reg;
        final TCNTnRegister TCNTnL_reg;
        final Mode[] modes;

        protected Timer16Bit(int n, AtmelMicrocontroller m, int[] periods, String ovfName, String[] cfn) {
            super(n, m, periods, ovfName);

            TCNTn_reg = new RW16Register();
            TCNTnH_reg = (HighRegister) m.installIOReg("TCNT" + n + "H", new HighRegister());
            TCNTnL_reg = new TCNTnRegister("TCNT" + n + "L", new LowRegister(TCNTn_reg));
            m.installIOReg("TCNT" + n + "L", TCNTnL_reg);


            addComparator(Comparator.I, new InputCompareUnit(n, m, Comparator._, m.properties.getInterrupt(cfn[0]), (Pin) m.getPin("IC" + timerNumber)));
            addComparator(Comparator.A, new OutputCompareUnit(n, m, Comparator.A, m.properties.getInterrupt(cfn[1]), (Pin) m.getPin("OC" + timerNumber + "A")));
            addComparator(Comparator.B, new OutputCompareUnit(n, m, Comparator.B, m.properties.getInterrupt(cfn[2]), (Pin) m.getPin("OC" + timerNumber + "B")));
            addComparator(Comparator.C, new OutputCompareUnit(n, m, Comparator.C, m.properties.getInterrupt(cfn[3]), (Pin) m.getPin("OC" + timerNumber + "C")));

            String ocfn = "OCF" + n + "A";
            String icfn = "ICF" + n;
            modes = new Mode[] {
                    new Mode(Mode.NORMAL.class, null, FixedTop.FFFF),
                    new Mode(Mode.PWM.class, null, FixedTop.FF),
                    new Mode(Mode.PWM.class, null, FixedTop._1FF),
                    new Mode(Mode.PWM.class, null, FixedTop._3FF),
                    new Mode(Mode.CTC.class, getField(m, ocfn), gro(Comparator.A)),
                    new Mode(Mode.FAST_PWM.class, null, FixedTop.FF),
                    new Mode(Mode.FAST_PWM.class, null, FixedTop._1FF),
                    new Mode(Mode.FAST_PWM.class, null, FixedTop._3FF),
                    new Mode(Mode.FC_PWM.class, getField(m, icfn), gri(Comparator.I)),
                    new Mode(Mode.FC_PWM.class, getField(m, ocfn), gro(Comparator.A)),
                    new Mode(Mode.PWM.class, getField(m, icfn), gri(Comparator.I)),
                    new Mode(Mode.PWM.class, getField(m, ocfn), gro(Comparator.A)),
                    new Mode(Mode.CTC.class, getField(m, icfn), gri(Comparator.I)), null,
                    new Mode(Mode.FAST_PWM.class, getField(m, icfn), gri(Comparator.I)),
                    new Mode(Mode.FAST_PWM.class, getField(m, ocfn), gro(Comparator.A))};

            resetMode(0);// XXX: is this always the default mode?
        }

        private RegisterSet.Field getField(AtmelMicrocontroller m, String name) {
            return m.getRegisterSet().getField(name);
        }

        private TopValue gro(String n) {
            return ((OutputCompareUnit) getComparator(n)).OCRnX_reg;
        }

        private TopValue gri(String n) {
            return ((InputCompareUnit) getComparator(n)).OCRnX_reg;
        }

        public int getCounter() {
            return TCNTn_reg.read16();
        }

        public void setCounter(int count) {
            TCNTn_reg.write(count);
        }

        public String getCounterName() {
            return "TCNT" + timerNumber;
        }

        public int getMax() {
            return 0xFFFF;
        }

        public void resetMode(int WGMn) {
            mode = modes[WGMn];
        }

        class OutputCompareUnit extends OutputComparator {
            final HighRegister OCRnXH_reg;
            final LowRegister OCRnXL_reg;
            final BufferedRegister OCRnX_reg;

            OutputCompareUnit(int timerNumber, AtmelMicrocontroller m, String unit, int interruptNumber, AtmelMicrocontroller.Pin pin) {
                super(unit, m.getRegisterSet(), interruptNumber, pin);

                String name = "OCR" + timerNumber + unit;
                OCRnX_reg = new BufferedRegister(new RW16Register());
                OCRnXH_reg = (HighRegister) m.installIOReg(name + "H", new OCRnxHighRegister(OCRnX_reg));
                OCRnXL_reg = (LowRegister) m.installIOReg(name + "L", new LowRegister(OCRnX_reg));
            }

            int read() {
                return OCRnX_reg.read16();
            }

            int readBuffer() {
                return OCRnX_reg.readBuffer();
            }
        }

        /**
         * XXX: Incomplete, will require some refactoring
         */
        class InputCompareUnit extends InputComparator {
            final HighRegister OCRnXH_reg;
            final LowRegister OCRnXL_reg;
            final BufferedRegister OCRnX_reg;

            InputCompareUnit(int timerNumber, AtmelMicrocontroller m, String unit, int interruptNumber, AtmelMicrocontroller.Pin pin) {
                super(unit, m.getRegisterSet(), interruptNumber, pin);

                String name = "ICR" + timerNumber + unit;
                OCRnX_reg = new BufferedRegister(new RW16Register());
                OCRnXH_reg = (HighRegister) m.installIOReg(name + "H", new OCRnxHighRegister(OCRnX_reg));
                OCRnXL_reg = (LowRegister) m.installIOReg(name + "L", new LowRegister(OCRnX_reg));
            }

            int read() {
                return OCRnX_reg.read16();
            }

            int readBuffer() {
                return OCRnX_reg.readBuffer();
            }
        }

    }

    protected static final int[] periods0 = {0, 1, 8, 32, 64, 128, 256, 1024};

    /**
     * <code>Timer0</code> is the default 8-bit timer on the ATMega128.
     */
    protected class Timer0 extends Timer8Bit {

        protected Timer0() {
            super(0, ATMegaClassic.this, periods0, "TIMER0 OVF", "TIMER0 COMP");
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

    protected static final int[] periods2 = {0, 1, 8, 64, 256, 1024};

    /**
     * <code>Timer2</code> is an additional 8-bit timer on the ATMega128. It is not available in
     * ATMega103 compatibility mode.
     */
    protected class Timer2 extends Timer8Bit {
        protected Timer2() {
            super(2, ATMegaClassic.this, periods2, "TIMER2 OVF", "TIMER2 COMP");
        }
    }

    protected static final int[] periods1 = new int[]{0, 1, 8, 64, 256, 1024};
    protected static final String[] cf1Names = new String[]{"TIMER1 CAPT", "TIMER1 COMPA", "TIMER1 COMPB", "TIMER1 COMPC"};

    /**
     * <code>Timer1</code> is a 16-bit timer available on the ATMega128.
     */
    protected class Timer1 extends Timer16Bit {

        protected Timer1(int compareUnits) {
            super(1, ATMegaClassic.this, periods1, "TIMER1 OVF", cf1Names);
        }

    }

    protected static final int[] periods3 = {0, 1, 8, 64, 256, 1024};
    protected static final String[] cf3Names = new String[]{"TIMER3 CAPT", "TIMER3 COMPA", "TIMER3 COMPB", "TIMER3 COMPC"};

    /**
     * <code>Timer3</code> is an additional 16-bit timer available on the ATMega128, but not in ATMega103
     * compatability mode.
     */
    protected class Timer3 extends Timer16Bit {

        protected Timer3(int compareUnits) {
            super(3, ATMegaClassic.this, periods3, "TIMER3 OVF", cf3Names);
        }

    }
}
