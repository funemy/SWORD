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

package avrora.sim.platform;

import avrora.core.Program;
import avrora.sim.Simulator;
import avrora.sim.Simulation;
import avrora.sim.clock.ClockDomain;
import avrora.sim.mcu.*;
import avrora.sim.platform.sensors.LightSensor;
import avrora.sim.platform.sensors.SensorBoard;
import avrora.sim.radio.*;
import cck.text.Terminal;

/**
 * The <code>MicaZ</code> class is an implementation of the <code>Platform</code> interface that represents
 * both a specific microcontroller and the devices connected to it. This implementation therefore uses the
 * ATMega128L microcontroller and uses LED and Radio devices, etc. The MicaZ class differs from Mica2 in that
 * the CC2420 radio implementation is installed instead of the CC1000.
 *
 * @author Ben L. Titzer, Daniel Lee
 */
public class MicaZ extends Platform {

    protected static final int MAIN_HZ = 7372800;

    public static class Factory implements PlatformFactory {

        /**
         * The <code>newPlatform()</code> method is a factory method used to create new instances of the
         * <code>Mica2</code> class.
         * @param id the integer ID of the node
         * @param sim the simulation
         * @param p the program to load onto the node @return a new instance of the <code>Mica2</code> platform
         */
        public Platform newPlatform(int id, Simulation sim, Program p) {
            ClockDomain cd = new ClockDomain(MAIN_HZ);
            cd.newClock("external", 32768);

            return new MicaZ(new ATMega128(id, sim, cd, p));
        }
    }

    protected final Simulator sim;

    protected CC2420Radio radio;
    protected SensorBoard sensorboard;
    protected ExternalFlash externalFlash;
    protected LightSensor lightSensor;
    protected LED.LEDGroup ledGroup;

    private MicaZ(Microcontroller m) {
        super(m);
        sim = m.getSimulator();
        addDevices();
    }

    /**
     * The <code>addDevices()</code> method is used to add the external (off-chip) devices to the
     * platform. For the mica2, these include the LEDs, the radio, and the sensor board.
     */
    protected void addDevices() {
        LED yellow = new LED(sim, Terminal.COLOR_YELLOW, "Yellow");
        LED green = new LED(sim, Terminal.COLOR_GREEN, "Green");
        LED red = new LED(sim, Terminal.COLOR_RED, "Red");

        ledGroup = new LED.LEDGroup(sim, new LED[] { yellow, green, red });
        addDevice("leds", ledGroup);

        AtmelMicrocontroller amcu = (AtmelMicrocontroller)mcu;

        mcu.getPin("PA0").connectOutput(yellow);
        mcu.getPin("PA1").connectOutput(green);
        mcu.getPin("PA2").connectOutput(red);

        // install the new CC2420 radio
        CC2420Radio radio = new CC2420Radio(mcu, MAIN_HZ * 2);
        mcu.getPin(11).connectOutput(radio.SCLK_pin);
        mcu.getPin(12).connectOutput(radio.MOSI_pin);
        mcu.getPin(13).connectInput(radio.MISO_pin);
        mcu.getPin(17).connectInput(radio.FIFO_pin);
        mcu.getPin(8).connectInput(radio.FIFOP_pin);
        mcu.getPin(31).connectInput(radio.CCA_pin);
        mcu.getPin(29).connectInput(radio.SFD_pin);
        mcu.getPin(10).connectOutput(radio.CS_pin);
        mcu.getPin("PA5").connectOutput(radio.VREN_pin);
        mcu.getPin("PA6").connectOutput(radio.RSTN_pin);
        ADC adc = (ADC)amcu.getDevice("adc");
        adc.connectADCInput(radio.adcInterface, 0);
        SPI spi = (SPI)amcu.getDevice("spi");
        spi.connect(radio.spiInterface);
        addDevice("radio", radio);
        // TODO: install FIFOP pin.
        radio.FIFOP_interrupt = mcu.getProperties().getInterrupt("INT6");
        // install the input capture pin.
        Timer16Bit timer1 = (Timer16Bit)amcu.getDevice("timer1");
        radio.setSFDView(timer1.getInputCapturePin());

        // sensor board
        sensorboard = new SensorBoard(sim);
        // external flash
        externalFlash = new ExternalFlash(mcu, 2048, 264);
        // light sensor
        lightSensor = new LightSensor(amcu, 1, "PC2", "PE5");
        addDevice("light-sensor", lightSensor);
    }

}
