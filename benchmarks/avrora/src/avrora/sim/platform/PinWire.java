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

import avrora.sim.FiniteStateMachine;
import avrora.sim.Simulator;
import avrora.sim.output.SimPrinter;
import avrora.sim.clock.Clock;
import avrora.sim.mcu.ATMegaFamily;
import avrora.sim.mcu.Microcontroller;
import avrora.sim.util.SimUtil;
import cck.text.Terminal;


/**
 * The <code>PinWire</code> class is the interface for making wire connections
 * to other microcontrollers.
 *
 * @author Jacob Everist
 */

public class PinWire {

    protected Simulator sim;

    protected final FiniteStateMachine state;

    protected final int colorNum;
    protected final String pinName;

    protected final ATMegaFamily atmel;

    // names of the states of this device
    private static final String[] modeName = {"low", "high"};

    // default mode of the device is off
    private static final int startMode = 0;

    // whether the microcontroller can read this as input
    private boolean acceptsInput;

    // whether the microcontroller can write this as output
    private boolean acceptsOutput;

    // whether this pin is an interrupt pin
    private boolean isInterruptPin;

    // the interrupt number
    private int interruptNum;

    // input/output connectors
    public WireInput wireInput;
    public WireOutput wireOutput;

    // probe of the PinWire activity
    protected final PinWireProbe probe;

    // propagation delay in cycles
    protected final long propDelay;

    protected PinWire(Simulator s, int colorNum, String pinName) {

        sim = s;
        Clock clock = sim.getClock();
        state = new FiniteStateMachine(clock, startMode, modeName, 0);
        wireOutput = new WireOutput();
        wireInput = new WireInput();

        acceptsInput = false;
        acceptsOutput = false;

        probe = new PinWireProbe();

        this.colorNum = colorNum;
        this.pinName = pinName;
        isInterruptPin = false;
        interruptNum = 0;
        atmel = null;

       	propDelay = clock.millisToCycles(0.0014);

    }

    protected PinWire(Simulator s, int colorNum, String pinName, int interruptNum, Microcontroller mcu) {

        sim = s;
        Clock clock = sim.getClock();
        state = new FiniteStateMachine(clock, startMode, modeName, 0);
        wireOutput = new WireOutput();
        wireInput = new WireInput();

        acceptsInput = false;
        acceptsOutput = false;

        probe = new PinWireProbe();

        this.colorNum = colorNum;
        this.pinName = pinName;
        atmel = (ATMegaFamily) mcu;
        isInterruptPin = true;
        this.interruptNum = interruptNum;

       	propDelay = clock.millisToCycles(0.0014);

    }

    public String readName() {
    	return pinName;
    }

    public void enableConnect() {
        state.insertProbe(probe);
    }

    public void disableConnect() {
        state.removeProbe(probe);
    }

    public boolean inputReady() {
        return acceptsInput;
    }

    public boolean outputReady() {
        return acceptsOutput;
    }

    /**
     * The <code>PinWireProbe</code> class implements a probe from the (tiny) finite state machine
     * that represents an PinWire's state. An PinWire can be "off" or "on". This probe will simply
     * display changes to the state.
     */
    class PinWireProbe implements FiniteStateMachine.Probe {
        final SimPrinter printer;

        PinWireProbe() {
            printer = sim.getPrinter();
        }

        public void fireBeforeTransition(int beforeState, int afterState) {
            // do nothing
        }

        public void fireAfterTransition(int beforeState, int afterState) {
            if (beforeState == afterState) return;

            // print the status of the PinWire
            StringBuffer buf = printer.getBuffer(20);
            Terminal.append(colorNum, buf, pinName);
            buf.append(": ");
            buf.append(modeName[afterState]);
            printer.printBuffer(buf);
            
            // if this is an interrupt pin, and the transition triggers an interrupt
            // post an interrupt
            if (isInterruptPin) {

                // for now we only trigger on a rising edge
                if (beforeState == 0 && afterState == 1) {

                    // add delay event before flagging EIFR

                    // flag the EIFR register
                    ATMegaFamily.FlagRegister flag = atmel.getEIFR_reg();
                    flag.flagBit(interruptNum - 2);

                }
            }
        }
    }

    class WireInput implements Microcontroller.Pin.Input {

        /**
         * Constructor
         */
        protected WireInput() {
            // do nothing for now
        }

        /**
         * The <code>enableInput()</code> method is called by the simulator when the program changes the
         * direction of the pin. The device connected to this pin can then take action accordingly.
         */
        public void enableInput() {
            acceptsInput = true;

            // automatically disable output
            acceptsOutput = false;
        }

        /**
         * The <code>read()</code> method is called by the simulator when the program attempts to read the
         * level of the pin. The device can then compute and return the current level of the pin.
         *
         * @return true if the level of the pin is high; false otherwise
         */
        public boolean read() {
            // read the current state and return boolean value
            return state.getCurrentState() == 1;
        }
    }

    class WireOutput implements Microcontroller.Pin.Output {


        /**
         * Constructor
         */
        protected WireOutput() {
        }

        /**
         * The <code>enableOutput()</code> method is called by the simulator when the program changes the
         * direction of the pin. The device connected to this pin can then take action accordingly.
         */
        public void enableOutput() {
            acceptsOutput = true;

            // automatically disable input
            acceptsInput = false;
        }

        /**
         * The <code>write()</code> method is called by the simulator when the program writes a logical
         * level to the pin. The device can then take the appropriate action.
         *
         * @param level a boolean representing the logical level of the write
         */
        public void write(boolean level) {

        	// propagate signal after 1.4 uS =
        	//sim.insertEvent(new WirePropagationEvent(level), propDelay);


            if (level)
                state.transition(1);
            else
                state.transition(0);

        }

        protected class WirePropagationEvent implements Simulator.Event {
        	private boolean value;

			public WirePropagationEvent(boolean value) {
				this.value = value;
			}

			// propagate signal to the pin finally
			public void fire() {
		        if (value)
	                state.transition(1);
	            else
	                state.transition(0);
			}
        }

    }

}
