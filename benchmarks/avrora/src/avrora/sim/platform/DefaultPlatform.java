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
import avrora.sim.clock.ClockDomain;
import avrora.sim.mcu.Microcontroller;
import avrora.sim.mcu.MicrocontrollerFactory;
import avrora.sim.Simulation;

/**
 * The <code>DefaultPlatform</code> class represents the simplest type of platform, a microcontroller
 * with no externally connected devices. This makes it easier to instantiate bare microcontrollers
 * (rather than doing backflips processing options elsewhere in the code).
 *
 * @author Ben L. Titzer
 */
public class DefaultPlatform extends Platform {

    public final int id;

    DefaultPlatform(int id, Microcontroller mcu) {
        super(mcu);
        this.id = id;
    }

    /**
     * The <code>DefaultPlatform.Factory</code> class implements a factory for a default platform. The
     * speed of the main clock, the speed of the external clock (called "external"), and the microcontroller
     * factory are specified. This class will implement the <code>newPlatform()</code> method which is used
     * to instantiate a new platform instance.
     */
    public static class Factory implements PlatformFactory {
        public final MicrocontrollerFactory mcf;
        public final long mainClockSpeed;
        public final long extClockSpeed;

        /**
         * The constructor for the <code>DefaultPlatform.Factory</code> class accepts three parameters: the
         * speed of the main clock, the speed of the external clock, and the microcontroller factory for
         * this platform
         * @param mc the speed of the microcontroller's main clock in hz
         * @param ext the speed of the external clock (usually an input to the MCU) in hz
         * @param mcf the microcontroller factory capable of creating a new microcontroller
         */
        public Factory(long mc, long ext, MicrocontrollerFactory mcf) {
            this.mcf = mcf;
            this.mainClockSpeed = mc;
            this.extClockSpeed = ext;
        }

        /**
         * The <code>newPlatform()</code> method creates a new instance of the platform with the specified
         * ID number, using the interpreter created by the given interpreter factory, containing the specified
         * progarm.
         * @param id the ID number of the platform to create
         * @param sim the simulation
         * @param p the program to load into the platform @return a new instance of the <code>Platform</code> interface for this platform
         */
        public Platform newPlatform(int id, Simulation sim, Program p) {
            ClockDomain cd = new ClockDomain(mainClockSpeed);
            cd.newClock("external", extClockSpeed);
            return new DefaultPlatform(id, mcf.newMicrocontroller(id, sim, cd, p));
        }
    }


}
