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
import avrora.core.Program;
import avrora.sim.FiniteStateMachine;
import avrora.sim.Simulation;
import avrora.sim.clock.ClockDomain;

/**
 * The <code>ATMega88</code> class represents the ATMega88 series microcontrollers from Atmel. These
 * microcontrollers various amounts of memory, and a host of internal devices such as
 * ADC, SPI, and timers.
 *
 * @author Ben L. Titzer
 * @author Pekka Nikander
 * @author Bastian Schlich
 * @author John F. Schommer
 *
 */
public class ATMega88 extends ATMegaX8 {

    public static final int ATMEGA88_IOREG_SIZE  = ATMEGAX8_IOREG_SIZE;
    public static final int ATMEGA88_SRAM_SIZE   = _1kb;
    public static final int ATMEGA88_FLASH_SIZE  = 8 * _1kb;
    public static final int ATMEGA88_EEPROM_SIZE = _512b;
    public static final int ATMEGA88_NUM_PINS    = ATMEGAX8_NUM_PINS;
    public static final int ATMEGA88_NUM_INTS    = ATMEGAX8_NUM_INTS;

    /**
     * The <code>props</code> field stores a static reference to a properties
     * object shared by all of the instances of these microcontrollers. This object
     * stores the IO register size, pin assignments, etc.
     */
    public static final AVRProperties props;

    static {

        props = new AVRProperties(ATMEGA88_IOREG_SIZE, // number of io registers
                ATMEGA88_SRAM_SIZE,   // size of sram in bytes
                ATMEGA88_FLASH_SIZE,  // size of flash in bytes
                ATMEGA88_EEPROM_SIZE, // size of eeprom in bytes
                ATMEGA88_NUM_PINS,    // number of pins
                ATMEGA88_NUM_INTS,    // number of interrupts
                new ReprogrammableCodeSegment.Factory(ATMEGA88_FLASH_SIZE, 6),
                pinAssignments,	      // the assignment of names to physical pins
                rl, // the assignment of names to IO registers
                interruptAssignments);
    }

    public static class Factory implements MicrocontrollerFactory {

        /**
         * The <code>newMicrocontroller()</code> method is used to
         * instantiate a microcontroller instance for the particular
         * program. It will construct an instance of the
         * <code>Simulator</code> class that has all the properties of
         * this hardware device and has been initialized with the
         * specified program.
         *
         * @param sim the simulation
         * @param p the program to load onto the microcontroller @return a <code>Microcontroller</code> instance that
         *         represents the specific hardware device with the
         *         program loaded onto it
         */
        public Microcontroller newMicrocontroller(int id, Simulation sim, ClockDomain cd, Program p) {
            return new ATMega88(id, sim, cd, p);
        }

    }

    // XXX: The following is probably wrong, not checked with 88 specs.
    protected static final int[] wakeupTimes = {
        0, 0, 0, 1000, 1000, 0, 0, 6, 0
    };

    private static final int[][] transitionTimeMatrix =
	FiniteStateMachine.buildBimodalTTM(idleModeNames.length, 0, wakeupTimes, new int[wakeupTimes.length]);

    public ATMega88(int id, Simulation sim, ClockDomain cd, Program p) {
        super(id, sim, props, cd, p, transitionTimeMatrix);
    }

}
