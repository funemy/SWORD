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

import avrora.sim.Simulator;
import avrora.sim.clock.ClockDomain;
import avrora.sim.platform.Platform;

/**
 * The <code>Microcontroller</code> interface corresponds to a hardware device that implements the AVR
 * instruction set. This interface contains methods that get commonly needed information about the particular
 * hardware device and and can load programs onto this virtual device.
 *
 * @author Ben L. Titzer
 */
public interface Microcontroller {

    /**
     * The <code>Pin</code> interface encapsulates the notion of a physical pin on the microcontroller chip.
     * It is generally used in wiring up external devices to the microcontroller.
     *
     * @author Ben L. Titzer
     */
    public interface Pin {
        /**
         * The <code>Input</code> interface represents an input pin. When the pin is configured to be an input
         * and the microcontroller attempts to read from this pin, the installed instance of this interface
         * will be called.
         */
        public interface Input {
            /**
             * The <code>read()</code> method is called by the simulator when the program attempts to read the
             * level of the pin. The device can then compute and return the current level of the pin.
             *
             * @return true if the level of the pin is high; false otherwise
             */
            public boolean read();
        }

        /**
         * The <code>Output</code> interface represents an output pin. When the pin is configured to be an
         * output and the microcontroller attempts to wrote to this pin, the installed instance of this
         * interface will be called.
         */
        public interface Output {
            /**
             * The <code>write()</code> method is called by the simulator when the program writes a logical
             * level to the pin. The device can then take the appropriate action.
             *
             * @param level a boolean representing the logical level of the write
             */
            public void write(boolean level);
        }

        /**
         * The <code>connect()</code> method will connect this pin to the specified input. Attempts by the
         * microcontroller to read from this pin when it is configured as an input will then call this
         * instance's <code>read()</code> method.
         *
         * @param i the <code>Input</code> instance to connect to
         */
        public void connectInput(Input i);

        /**
         * The <code>connect()</code> method will connect this pin to the specified output. Attempts by the
         * microcontroller to write to this pin when it is configured as an output will then call this
         * instance's <code>write()</code> method.
         *
         * @param o the <code>Output</code> instance to connect to
         */
        public void connectOutput(Output o);
    }

    /**
     * The <code>getSimulator()</code> method gets a simulator instance that is capable of emulating this
     * hardware device.
     *
     * @return a <code>Simulator</code> instance corresponding to this device
     */
    public Simulator getSimulator();

    /**
     * The <code>getPlatform()</code> method gets a platform instance that contains this microcontroller.
     *
     * @return the platform instance containing this microcontroller, if it exists; null otherwise
     */
    public Platform getPlatform();

    /**
     * The <code>setPlatform()</code> method sets the platform instance that contains this microcontroller.
     * @param p the new platform for this microcontroller
     */
    public void setPlatform(Platform p);

    /**
     * The <code>getPin()</code> method looks up the named pin and returns a reference to that pin. Names of
     * pins should be UPPERCASE. The intended users of this method are external device implementors which
     * connect their devices to the microcontroller through the pins.
     *
     * @param name the name of the pin; for example "PA0" or "OC1A"
     * @return a reference to the <code>Pin</code> object corresponding to the named pin if it exists; null
     *         otherwise
     */
    public Pin getPin(String name);

    /**
     * The <code>getPin()</code> method looks up the specified pin by its number and returns a reference to
     * that pin. The intended users of this method are external device implementors which connect their
     * devices to the microcontroller through the pins.
     *
     * @param num the pin number to look up
     * @return a reference to the <code>Pin</code> object corresponding to the named pin if it exists; null
     *         otherwise
     */
    public Pin getPin(int num);

    /**
     * The <code>sleep()</code> method puts the microcontroller into the sleep mode defined by its
     * internal sleep configuration register. It may shutdown devices and disable some clocks. This
     * method should only be called from within the interpreter.
     */
    public void sleep();

    /**
     * The <code>wakeup()</code> method wakes the microcontroller from a sleep mode. It may resume
     * devices, turn clocks back on, etc. This method is expected to return the number of cycles that
     * is required for the microcontroller to wake completely from the sleep state it was in.
     *
     * @return cycles required to wake from the current sleep mode
     */
    public int wakeup();

    /**
     * The <code>getClockDomain()</code> method returns the clock domain for this microcontroller. The clock
     * domain contains all of the clocks attached to the microcontroller and platform, including the main clock.
     * @return an instance of the <code>ClockDomain</code> class representing the clock domain for this
     * microcontroller
     */
    public ClockDomain getClockDomain();

    /**
     * The <code>getRegisterSet()</code> method returns the register set containing all of the IO registers
     * for this microcontroller.
     * @return a reference to the <code>RegisterSet</code> instance which stores all of the IO registers
     * for this microcontroller.
     */
    public RegisterSet getRegisterSet();

    /**
     * The <code>getProperties()</code> method gets an object that describes the microcontroller
     * including the size of the RAM, EEPROM, flash, etc.
     * @return an instance of the <code>MicrocontrollerProperties</code> class that contains all
     * the relevant information about this microcontroller
     */
    public MCUProperties getProperties();

}
