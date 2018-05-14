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

package avrora.sim.mcu;

import avrora.sim.FiniteStateMachine;
import avrora.sim.Simulator;
import avrora.sim.output.SimPrinter;
import avrora.sim.clock.Clock;
import avrora.sim.clock.ClockDomain;
import avrora.sim.platform.Platform;

/**
 * @author Ben L. Titzer
 */
public abstract class DefaultMCU implements Microcontroller {

    protected final Microcontroller.Pin[] pins;
    protected final RegisterSet registers;
    protected Platform platform;
    protected Simulator simulator;
    protected SimPrinter pinPrinter;
    protected final ClockDomain clockDomain;
    protected final FiniteStateMachine sleepState;
    private boolean pinPrinterInit;

    protected DefaultMCU(ClockDomain cd, int np, RegisterSet regs, FiniteStateMachine sleep) {
        clockDomain = cd;
        pins = new Microcontroller.Pin[np];
        sleepState = sleep;
        registers = regs;
    }

    /**
     * The <code>getFSM()</code> method gets a reference to the finite state machine that represents
     * the sleep modes of the MCU. The finite state machine allows probing of the sleep mode transitions.
     * @return a reference to the finite state machine representing the sleep mode of the MCU
     */
    public FiniteStateMachine getFSM() {
        return sleepState;
    }

    /**
     * The <code>getRegisterSet()</code> method gets a reference to the register set of the microcontroller.
     * The register set contains all of the IO registers for this microcontroller.
     *
     * @return a reference to the register set of this microcontroller instance
     */
    public RegisterSet getRegisterSet() {
        return registers;
    }

    /**
     * The <code>getPin()</code> method looks up the specified pin by its number and returns a reference to
     * that pin. The intended users of this method are external device implementors which connect their
     * devices to the microcontroller through the pins.
     *
     * @param num the pin number to look up
     * @return a reference to the <code>Pin</code> object corresponding to the named pin if it exists; null
     *         otherwise
     */
    public Microcontroller.Pin getPin(int num) {
        if (num < 0 || num > pins.length) return null;
        return pins[num];
    }

    /**
     * The <code>getClock()</code> method gets a reference to a specific clock on this device. For example,
     * the external clock, or a specific device's clock can be accessed by specifying its name.
     * @param name the name of the clock to get
     * @return a reference to the <code>Clock</code> instance for the specified clock if it exists
     */
    public Clock getClock(String name) {
        return clockDomain.getClock(name);
    }

    /**
     * The <code>getSimulator()</code> method gets a reference to the simulator for this microcontroller instance.
     * @return a reference to the simulator instance for this microcontroller
     */
    public Simulator getSimulator() {
        return simulator;
    }

    /**
     * The <code>getPlatform()</code> method returns the platform for this microcontroller.
     * @return the platform instance containing this microcontroller
     */
    public Platform getPlatform() {
        return platform;
    }

    /**
     * The <code>setPlatform()</code> method sets the platform instance for this microcontroller
     * @param p the platform instance associated with this microcontroller
     */
    public void setPlatform(Platform p) {
        platform = p;
    }

    /**
     * The <code>getClockDomain()</code> method gets a reference to the <code>ClockDomain</code> instance for
     * this node that contains the main clock and any derived clocks for this microcontroller.
     * @return a reference to the clock domain for this microcontroller
     */
    public ClockDomain getClockDomain() {
        return clockDomain;
    }

    /**
     * The <code>Pin</code> class implements a model of a pin on the ATMegaFamily for the general purpose IO
     * ports.
     */
    protected class Pin implements Microcontroller.Pin {
        protected final int number;

        boolean level;
        boolean outputDir;
        boolean pullup;

        Input input;
        Output output;

        protected Pin(int num) {
            number = num;
        }

        public void connectOutput(Output o) {
            output = o;
        }

        public void connectInput(Input i) {
            input = i;
        }

        protected void setOutputDir(boolean out) {
            outputDir = out;
            if (out) write(level);
        }

        protected void setPullup(boolean pull) {
            pullup = pull;
        }

        protected boolean read() {
            boolean result;
            if (!outputDir) {
                if (input != null)
                    result = input.read();
                else
                    result = pullup;

            } else {
                result = level;
            }
            // print the result of the read
            printRead(result);
            return result;
        }

        private void printRead(boolean result) {
            if (pinPrinter == null) pinPrinter = simulator.getPrinter("mcu.pin");
            if (pinPrinter != null) {
                String dir = getDirection();
                pinPrinter.println("READ PIN: " + number + ' ' + dir + "<- " + result);
            }
        }

        private String getDirection() {
            if (!outputDir) {
                if (input != null)
                    return "[input] ";
                else
                    return "[pullup:" + pullup + "] ";

            } else {
                return "[output] ";
            }
        }

        protected void write(boolean value) {
            level = value;
            // print the write
            printWrite(value);
            if (outputDir && output != null) output.write(value);
        }

        private void printWrite(boolean value) {
            if (pinPrinterInit) {
                pinPrinter = simulator.getPrinter("mcu.pin");
                pinPrinterInit = true;
            }
            if (pinPrinter != null) {
                String dir = getDirection();
                pinPrinter.println("WRITE PIN: " + number + ' ' + dir + "-> " + value);
            }
        }
    }
}
