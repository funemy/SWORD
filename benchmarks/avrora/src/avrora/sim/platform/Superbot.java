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
 * The <code>Seres</code> class is an implementation of the <code>Platform</code> interface that represents
 * both a specific microcontroller and the devices connected to it.
 *
 * @author Jacob Everist
 */
public class Superbot extends Platform {

    protected final Microcontroller mcu;
    protected final Simulator sim;
    protected PinConnect pinConnect;

    private Superbot(Microcontroller m) {
    	super(m);
        mcu = m;
        sim = m.getSimulator();
        addDevices();
    }


    public static class Factory implements PlatformFactory {
        public Platform newPlatform(int id, Simulation sim, Program p) {
            ClockDomain cd = new ClockDomain(7372800);
            cd.newClock("external", 32768);

            return new Superbot(new ATMega128(id, sim, cd, p));
        }
    }

    /**
     * The <code>addDevices()</code> method is used to add the external (off-chip) devices to the
     * platform.
     */
    protected void addDevices() {

        // transmit pins
        PinWire LED1PinTx = new PinWire(sim, Terminal.COLOR_YELLOW, "LED1 Tx");
        PinWire LED2PinTx = new PinWire(sim, Terminal.COLOR_GREEN, "LED2 Tx");
        PinWire LED3PinTx = new PinWire(sim, Terminal.COLOR_RED, "LED3 Tx");
        PinWire LED4PinTx = new PinWire(sim, Terminal.COLOR_BLUE, "LED4 Tx");
        PinWire LED5PinTx = new PinWire(sim, Terminal.COLOR_RED, "LED5 Tx");
        PinWire LED6PinTx = new PinWire(sim, Terminal.COLOR_BLUE, "LED6 Tx");

        // connect transmit pins to physical pins
   		mcu.getPin("PC3").connectInput(LED1PinTx.wireInput);
		mcu.getPin("PC3").connectOutput(LED1PinTx.wireOutput);
		mcu.getPin("PC2").connectInput(LED2PinTx.wireInput);
		mcu.getPin("PC2").connectOutput(LED2PinTx.wireOutput);
		mcu.getPin("PC1").connectInput(LED3PinTx.wireInput);
		mcu.getPin("PC1").connectOutput(LED3PinTx.wireOutput);
		mcu.getPin("PC0").connectInput(LED4PinTx.wireInput);
		mcu.getPin("PC0").connectOutput(LED4PinTx.wireOutput);
		mcu.getPin("PD6").connectInput(LED5PinTx.wireInput);
		mcu.getPin("PD6").connectOutput(LED5PinTx.wireOutput);
		mcu.getPin("PD5").connectInput(LED6PinTx.wireInput);
		mcu.getPin("PD5").connectOutput(LED6PinTx.wireOutput);

		// enable printing on output pins
		LED1PinTx.enableConnect();
		LED2PinTx.enableConnect();
		LED3PinTx.enableConnect();
		LED4PinTx.enableConnect();
		LED5PinTx.enableConnect();
		LED6PinTx.enableConnect();

        // receive pins
        PinWire LED1PinRx = new PinWire(sim, Terminal.COLOR_YELLOW, "LED1 Rx");
        PinWire LED2PinRx = new PinWire(sim, Terminal.COLOR_GREEN, "LED2 Rx");
        PinWire LED3PinRx = new PinWire(sim, Terminal.COLOR_RED, "LED3 Rx");
        PinWire LED4PinRx = new PinWire(sim, Terminal.COLOR_BLUE, "LED4 Rx");
        PinWire LED5PinRx = new PinWire(sim, Terminal.COLOR_RED, "LED5 Rx");
        PinWire LED6PinRx = new PinWire(sim, Terminal.COLOR_BLUE, "LED6 Rx");

        // connect receive pins to physical pins
   		mcu.getPin("PF0").connectInput(LED1PinRx.wireInput);
		mcu.getPin("PF0").connectOutput(LED1PinRx.wireOutput);
		mcu.getPin("PF1").connectInput(LED2PinRx.wireInput);
		mcu.getPin("PF1").connectOutput(LED2PinRx.wireOutput);
		mcu.getPin("PF2").connectInput(LED3PinRx.wireInput);
		mcu.getPin("PF2").connectOutput(LED3PinRx.wireOutput);
		mcu.getPin("PF3").connectInput(LED4PinRx.wireInput);
		mcu.getPin("PF3").connectOutput(LED4PinRx.wireOutput);
		mcu.getPin("PF4").connectInput(LED5PinRx.wireInput);
		mcu.getPin("PF4").connectOutput(LED5PinRx.wireOutput);
		mcu.getPin("PF5").connectInput(LED6PinRx.wireInput);
		mcu.getPin("PF5").connectOutput(LED6PinRx.wireOutput);

		// enable printing on output pins
		LED1PinRx.enableConnect();
		LED2PinRx.enableConnect();
		LED3PinRx.enableConnect();
		LED4PinRx.enableConnect();
		LED5PinRx.enableConnect();
		LED6PinRx.enableConnect();

        // receive interrupt pins
        PinWire LED1PinInt = new PinWire(sim, Terminal.COLOR_YELLOW, "LED1 Int", 1+2, mcu);
        PinWire LED2PinInt = new PinWire(sim, Terminal.COLOR_GREEN, "LED2 Int", 2+2, mcu);
        PinWire LED3PinInt = new PinWire(sim, Terminal.COLOR_RED, "LED3 Int", 3+2, mcu);
        PinWire LED4PinInt = new PinWire(sim, Terminal.COLOR_BLUE, "LED4 Int", 4+2, mcu);
        PinWire LED5PinInt = new PinWire(sim, Terminal.COLOR_RED, "LED5 Int", 5+2, mcu);
        PinWire LED6PinInt = new PinWire(sim, Terminal.COLOR_BLUE, "LED6 Int", 6+2, mcu);

        // connect receive interrupt pins to physical pins
   		mcu.getPin("INT1").connectInput(LED1PinInt.wireInput);
		mcu.getPin("INT1").connectOutput(LED1PinInt.wireOutput);
		mcu.getPin("INT2").connectInput(LED2PinInt.wireInput);
		mcu.getPin("INT2").connectOutput(LED2PinInt.wireOutput);
		mcu.getPin("INT3").connectInput(LED3PinInt.wireInput);
		mcu.getPin("INT3").connectOutput(LED3PinInt.wireOutput);
		mcu.getPin("INT4").connectInput(LED4PinInt.wireInput);
		mcu.getPin("INT4").connectOutput(LED4PinInt.wireOutput);
		mcu.getPin("INT5").connectInput(LED5PinInt.wireInput);
		mcu.getPin("INT5").connectOutput(LED5PinInt.wireOutput);
		mcu.getPin("INT6").connectInput(LED6PinInt.wireInput);
		mcu.getPin("INT6").connectOutput(LED6PinInt.wireOutput);

		// enable printing on output pins
		LED1PinInt.enableConnect();
		LED2PinInt.enableConnect();
		LED3PinInt.enableConnect();
		LED4PinInt.enableConnect();
		LED5PinInt.enableConnect();
		LED6PinInt.enableConnect();

		// pin management device
        pinConnect = PinConnect.pinConnect;

        pinConnect.addSuperbotNode(mcu, LED1PinTx, LED2PinTx, LED3PinTx, LED4PinTx, LED5PinTx, LED6PinTx,
        		LED1PinRx, LED2PinRx, LED3PinRx, LED4PinRx, LED5PinRx, LED6PinRx,
				LED1PinInt, LED2PinInt, LED3PinInt, LED4PinInt, LED5PinInt, LED6PinInt);
    }

}
