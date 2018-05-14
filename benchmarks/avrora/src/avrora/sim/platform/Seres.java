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
import avrora.sim.mcu.ATMega128;
import avrora.sim.mcu.Microcontroller;
import cck.text.Terminal;

/**
 * The <code>Seres</code> class is an implementation of the <code>Platform</code> interface that represents both a
 * specific microcontroller and the devices connected to it.
 *
 * @author Jacob Everist
 */
public class Seres extends Platform {

    protected final Microcontroller mcu;
    protected final Simulator sim;
    protected PinConnect pinConnect;

    private Seres(Microcontroller m) {
        super(m);
        mcu = m;
        sim = m.getSimulator();
        addDevices();

    }

    public static class Factory implements PlatformFactory {

        public Platform newPlatform(int id, Simulation sim, Program p) {
            ClockDomain cd = new ClockDomain(7372800);
            cd.newClock("external", 32768);

            return new Seres(new ATMega128(id, sim, cd, p));
        }
    }

    /**
     * The <code>addDevices()</code> method is used to add the external (off-chip) devices to the platform.
     */
    protected void addDevices() {

        // transmit pins
        PinWire northPinTx = new PinWire(sim, Terminal.COLOR_YELLOW, "North Tx");
        PinWire eastPinTx = new PinWire(sim, Terminal.COLOR_GREEN, "East Tx");
        PinWire southPinTx = new PinWire(sim, Terminal.COLOR_RED, "South Tx");
        PinWire westPinTx = new PinWire(sim, Terminal.COLOR_BLUE, "West Tx");

        // connect transmit pins to physical pins
        mcu.getPin("PC0").connectInput(northPinTx.wireInput);
        mcu.getPin("PC0").connectOutput(northPinTx.wireOutput);
        mcu.getPin("PC1").connectInput(eastPinTx.wireInput);
        mcu.getPin("PC1").connectOutput(eastPinTx.wireOutput);
        mcu.getPin("PF5").connectInput(southPinTx.wireInput);
        mcu.getPin("PF5").connectOutput(southPinTx.wireOutput);
        mcu.getPin("PF6").connectInput(westPinTx.wireInput);
        mcu.getPin("PF6").connectOutput(westPinTx.wireOutput);

        // enable printing on output pins
        northPinTx.enableConnect();
        eastPinTx.enableConnect();
        southPinTx.enableConnect();
        westPinTx.enableConnect();

        // receive pins
        PinWire northPinRx = new PinWire(sim, Terminal.COLOR_YELLOW, "North Rx");
        PinWire eastPinRx = new PinWire(sim, Terminal.COLOR_GREEN, "East Rx");
        PinWire southPinRx = new PinWire(sim, Terminal.COLOR_RED, "South Rx");
        PinWire westPinRx = new PinWire(sim, Terminal.COLOR_BLUE, "West Rx");

        // connect receive pins to physical pins
        mcu.getPin("PD1").connectInput(northPinRx.wireInput);
        mcu.getPin("PD1").connectOutput(northPinRx.wireOutput);
        mcu.getPin("PF2").connectInput(eastPinRx.wireInput);
        mcu.getPin("PF2").connectOutput(eastPinRx.wireOutput);
        mcu.getPin("PD0").connectInput(southPinRx.wireInput);
        mcu.getPin("PD0").connectOutput(southPinRx.wireOutput);
        mcu.getPin("PF3").connectInput(westPinRx.wireInput);
        mcu.getPin("PF3").connectOutput(westPinRx.wireOutput);

        // enable printing on output pins
        northPinRx.enableConnect();
        eastPinRx.enableConnect();
        southPinRx.enableConnect();
        westPinRx.enableConnect();

        // receive interrupt pins
        PinWire northPinInt = new PinWire(sim, Terminal.COLOR_YELLOW, "North Int", 1 + 2, mcu);
        PinWire eastPinInt = new PinWire(sim, Terminal.COLOR_GREEN, "East Int", 2 + 2, mcu);
        PinWire southPinInt = new PinWire(sim, Terminal.COLOR_RED, "South Int", 2, mcu);
        PinWire westPinInt = new PinWire(sim, Terminal.COLOR_BLUE, "West Int", 6 + 2, mcu);

        // connect receive interrupt pins to physical pins
        mcu.getPin("INT1").connectInput(northPinInt.wireInput);
        mcu.getPin("INT1").connectOutput(northPinInt.wireOutput);
        mcu.getPin("INT2").connectInput(eastPinInt.wireInput);
        mcu.getPin("INT2").connectOutput(eastPinInt.wireOutput);
        mcu.getPin("INT0").connectInput(southPinInt.wireInput);
        mcu.getPin("INT0").connectOutput(southPinInt.wireOutput);
        mcu.getPin("INT6").connectInput(westPinInt.wireInput);
        mcu.getPin("INT6").connectOutput(westPinInt.wireOutput);

        // enable printing on output pins
        northPinInt.enableConnect();
        eastPinInt.enableConnect();
        southPinInt.enableConnect();
        westPinInt.enableConnect();

        // pin management device
        pinConnect = PinConnect.pinConnect;

        pinConnect.addSeresNode(mcu, northPinTx, eastPinTx, southPinTx, westPinTx, northPinRx, eastPinRx, southPinRx, westPinRx, northPinInt, eastPinInt, southPinInt, westPinInt);
    }

}
