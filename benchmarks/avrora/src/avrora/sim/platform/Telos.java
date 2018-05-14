/**
 * Copyright (c) 2005, Regents of the University of California
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
 * Creation date: Nov 22, 2005
 */

package avrora.sim.platform;

import avrora.arch.msp430.mcu.F1611;
import avrora.core.Program;
import avrora.sim.Simulator;
import avrora.sim.Simulation;
import avrora.sim.clock.ClockDomain;
import avrora.sim.mcu.Microcontroller;
import cck.text.Terminal;

/**
 * @author Ben L. Titzer
 */
public class Telos extends Platform {

    protected static final int MAIN_HZ = 8000000;
    protected static final int EXT_HZ = 32768;
    protected static final int RADIO_HZ = 7372800 * 2;

    public static class Factory implements PlatformFactory {
        /**
         * The <code>newPlatform()</code> method is a factory method used to create new instances of the
         * <code>Telos</code> class.
         * @param id the integer ID of the node
         * @param sim
         *@param p the program to load onto the node @return a new instance of the <code>Mica2</code> platform
         */
        public Platform newPlatform(int id, Simulation sim, Program p) {
            ClockDomain cd = new ClockDomain(MAIN_HZ);
            cd.newClock("external", EXT_HZ);

            return new Telos(new F1611(id, sim, cd, p));
        }
    }

    protected final Simulator sim;

    protected Telos(Microcontroller m) {
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

        yellow.enablePrinting();
        green.enablePrinting();
        red.enablePrinting();

        // TODO: these pin assignments are not correct!
        //mcu.getPin(30).connect(yellow);
        //mcu.getPin(31).connect(green);
        //mcu.getPin(32).connect(red);

        // radio
        //radio = new CC2420Radio(mcu, RADIO_HZ);
        //addDevice("radio", radio);
        // sensor board
        //sensorboard = new SensorBoard(sim);
        // external flash
        //externalFlash = new ExternalFlash(mcu);
        // light sensor
        //AtmelMicrocontroller amcu = (AtmelMicrocontroller)mcu;
        //lightSensor = new LightSensor(mcu, 1, "PC2", "PE5");
        //addDevice("light-sensor", lightSensor);
    }

}

