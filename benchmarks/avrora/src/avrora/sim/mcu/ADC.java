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

/**
 * The <code>ADC</code> class represents an on-chip device on the ATMega series of microcontroller that is
 * capable of converting an analog voltage value into a 10-bit digital value.
 *
 * @author Daniel Lee
 * @author Ben L. Titzer
 */
public class ADC extends AtmelInternalDevice {


    public static final float VBG_LEVEL = 1.0f;
    public static final float GND_LEVEL = 0.0f;

    private final ADCInput VBG_INPUT = new ADCInput() {
        public float getVoltage() {
            return voltageRef;
        }
    };

    private static final ADCInput GND_INPUT = new ADCInput() {
        public float getVoltage() {
            return GND_LEVEL;
        }
    };

    final MUXRegister ADMUX_reg = new MUXRegister();
    final ControlRegister ADCSRA_reg = new ControlRegister();
    final RWRegister ADCH_reg = new RWRegister();
    final RWRegister ADCL_reg = new RWRegister();

    final int channels;
    final int interruptNum;

    final ADCInput[] connectedDevices;

    float voltageRef = VBG_LEVEL;

    /**
     * The <code>ADCInput</code> interface is used by inputs into the analog to digital converter.
     */
    public interface ADCInput {

        /**
         * Report the current voltage level of the input.
         *
         * @return an integer value representing the voltage level of the input, in millivolts
         */
        public float getVoltage();
    }


    public ADC(AtmelMicrocontroller m, int channels) {
        super("adc", m);

        this.channels = channels;

        connectedDevices = new ADCInput[channels + 2];

        // the last two channels correspond to VBG and GND
        connectedDevices[channels] = VBG_INPUT;
        connectedDevices[channels + 1] = GND_INPUT;

        interruptNum = m.getProperties().getInterrupt("ADC");

        installIOReg("ADMUX", ADMUX_reg);
        installIOReg("ADCH", ADCH_reg);
        installIOReg("ADCL", ADCL_reg);
        installIOReg("ADCSRA", ADCSRA_reg);

        interpreter.getInterruptTable().registerInternalNotification(ADCSRA_reg, interruptNum);
    }

    /**
     * The <code>setVoltageRef()</code> method sets the external (Vref) voltage for the ADC converter.
     * @param vref the voltage reference in volts
     */
    public void setVoltageRef(float vref) {
        voltageRef = vref;
    }

    /**
     * The <code>getVoltageRef()</code> method returns the external (Vref) voltage that is currently
     * being used by the ADC converter.
     * @return the voltage reference in volts
     */
    public float getVoltageRef() {
        return voltageRef;
    }

    /**
     * The <code>connectADCInput()</code> method connects an <code>ADCInput</code> object to the specified
     * input port on the ADC chip.
     *
     * @param input the <code>ADCInput</code> object to attach to the input
     * @param num   the input port number to attach the device to
     */
    public void connectADCInput(ADCInput input, int num) {
        connectedDevices[num] = input;
    }

    static final byte[] SINGLE_ENDED_INPUT = {
             0,  1,  2,  3,  4,  5,  6,  7,
            -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1,  8,  9
    };

    static final short[] GAIN = {
             -1, -1,  -1,  -1,  -1,  -1,  -1,  -1,
             10, 10, 200, 200,  10,  10, 200, 200,
             1,   1,   1,   1,   1,   1,   1,   1,
             1,   1,   1,   1,   1,   1,  -1,  -1
    };

    static final byte[] POS_INPUT = {
            -1, -1, -1, -1, -1, -1, -1, -1,
             0,  1,  0,  1,  2,  3,  2,  3,
             0,  1,  2,  3,  4,  5,  6,  7,
             0,  1,  2,  3,  4,  5, -1, -1
    };

    static final byte[] NEG_INPUT = {
            -1, -1, -1, -1, -1, -1, -1, -1,
             0,  0,  0,  0,  2,  2,  2,  2,
             1,  1,  1,  1,  1,  1,  1,  1,
             2,  2,  2,  2,  2,  2, -1, -1
    };

    /**
     * <code>MUXRegister</code> defines the behavior of the ADMUX register.
     */
    protected class MUXRegister extends RWRegister {

        final RegisterView _mux = RegisterUtil.bitRangeView(this, 0, 4);

        boolean isSingleEnded() {
            return getSingleIndex() >= 0;
        }

        int getSingleIndex() {
            return SINGLE_ENDED_INPUT[_mux.getValue()];
        }

        int getPosIndex() {
            return POS_INPUT[_mux.getValue()];
        }

        int getNegIndex() {
            return NEG_INPUT[_mux.getValue()];
        }

        int getGain() {
            return GAIN[_mux.getValue()];
        }
    }


    static final short[] PRESCALER = { 2, 2, 4, 8, 16, 32, 64, 128 };

    /**
     * <code>ControlRegister</code> defines the behavior of the ADC control register,
     */
    protected class ControlRegister extends RWRegister implements InterruptTable.Notification {

        final ConversionEvent conversion = new ConversionEvent();

        final BooleanView _aden = RegisterUtil.booleanView(this, 7);
        final BooleanView _adsc = RegisterUtil.booleanView(this, 6);
        final BooleanView _adfr = RegisterUtil.booleanView(this, 5);
        final BooleanView _adif = RegisterUtil.booleanView(this, 4);
        final BooleanView _adie = RegisterUtil.booleanView(this, 3);
        final RegisterView _prescaler = RegisterUtil.bitRangeView(this, 0, 2);

        int cycles = 25;

        boolean converting;

        private void unpostADCInterrupt() {
            _adif.setValue(false);
            interpreter.setPosted(interruptNum, false);
        }

        public void write(byte nval) {

            value = nval;

            if (_aden.getValue()) {
                // if enabled and start conversion
                if (_adsc.getValue()) startConversion();
            } else {
                // else, stop conversion
                stopConversion();
            }

            // reset the flag bit if written by the user
            if ( _adif.getValue() ) unpostADCInterrupt();

            // enable the interrupt if the flag is set
            interpreter.setEnabled(interruptNum, _adie.getValue());
        }

        private void startConversion() {
            if ( !converting ) {
                // queue event for converting
                converting = true;
                insertConversion();
            }
        }

        private void insertConversion() {
            mainClock.insertEvent(conversion, getPrescaler() * cycles);
            if (ADMUX_reg.isSingleEnded()) {
                if (devicePrinter != null) {
                    devicePrinter.println("ADC: beginning sample of channel " + ADMUX_reg.getSingleIndex());
                }
            } else {
                if (devicePrinter != null) {
                    devicePrinter.println("ADC: beginning sample of channels " + ADMUX_reg.getPosIndex() + " - " + ADMUX_reg.getNegIndex());
                }
            }
            cycles = 13;
        }

        private void stopConversion() {
            _adsc.setValue(false);
            if ( converting ) {
                converting = false;
                mainClock.removeEvent(conversion);
            }
        }

        private int getPrescaler() {
            return PRESCALER[_prescaler.getValue()];
        }

        /**
         * The conversion event for the ADC. It is fired at a certain delay after the start conversion bit
         * in the control register is set.
         */
        private class ConversionEvent implements Simulator.Event {

            public void fire() {

                int val = convertVoltage();
                write16(val, ADCH_reg, ADCL_reg);
                if (devicePrinter != null) {
                    devicePrinter.println("ADC: conversion completed -> " + val);
                }
                _adif.setValue(true);
                //value = Arithmetic.setBit(value, ADIF, adif = true);
                interpreter.setPosted(interruptNum, true);

                if ( _adfr.getValue() ) {
                    // in free running mode, start the next conversion
                    insertConversion();
                } else {
                    // otherwise, stop conversion
                    stopConversion();
                }
            }
        }

        private int convertVoltage() {
            if (ADMUX_reg.isSingleEnded()) {
                // single ended conversions don't require amplification
                ADCInput dev = connectedDevices[ADMUX_reg.getSingleIndex()];
                float vin = dev != null ? dev.getVoltage() : 0;
                // TODO: select correct voltage reference from ADMUX register.
                float vref = voltageRef;
                if ( vin >= vref ) return 0x3ff;
                return 0x3ff & (int)(vin * 1024 / vref);
            } else {
                // use the differential gain amplifier.
                ADCInput pos = connectedDevices[ADMUX_reg.getPosIndex()];
                ADCInput neg = connectedDevices[ADMUX_reg.getNegIndex()];
                float vpos = pos != null ? pos.getVoltage() : 0;
                float vneg = neg != null ? neg.getVoltage() : 0;
                // TODO: select correct voltage reference from ADMUX register.
                float vref = voltageRef;
                float val = ((vpos - vneg) * ADMUX_reg.getGain() * 512 / vref);
                if ( val < -512 ) return 0x3ff;
                if ( val > 511 ) return 0x1ff;
                return 0x3ff & (int)val;
            }
        }


        public void force(int inum) {
            // set the interrupt flag accordingly
            _adif.setValue(true);
        }

        public void invoke(int inum) {
            unpostADCInterrupt();
        }
    }
}
