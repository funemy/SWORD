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
import avrora.sim.energy.Energy;
import avrora.sim.mcu.Microcontroller;
import cck.text.Terminal;

/**
 * The <code>LED</code> class implements an LED (light emitting diode) that can be hooked up
 * to a pin on the microcontroller. The LED prints its state when it is initialized and each
 * time it is turned on or off.
 *
 * @author Ben L. Titzer
 */
public class LED implements Microcontroller.Pin.Output {
    public Simulator sim;

    public final int colornum;
    public final String color;

    public final FiniteStateMachine state;
    protected final LEDProbe probe;

    //energy profile of this device
    //private Energy energy;
    // names of the states of this device
    private static final String[] modeName = {"off", "on"};
    // power consumption of the device states
    private static final double[] modeAmpere = {0.0, 0.0022};
    // default mode of the device is off
    private static final int startMode = 0;

    /**
     * The <code>LEDProbe</code> class implements a probe from the (tiny) finite state machine
     * that represents an LED's state. An LED can be "off" or "on". This probe will simply
     * display changes to the state.
     */
    class LEDProbe implements FiniteStateMachine.Probe {
        final SimPrinter printer;

        LEDProbe() {
            printer = sim.getPrinter();
        }

        public void fireBeforeTransition(int beforeState, int afterState) {
            // do nothing
        }

        public void fireAfterTransition(int beforeState, int afterState) {
            if ( beforeState == afterState ) return;
            // print the status of the LED
            StringBuffer buf = printer.getBuffer(20);
            Terminal.append(colornum, buf, color);
            buf.append(": ");
            buf.append(modeName[afterState]);
            printer.printBuffer(buf);
        }
    }

    public static class LEDGroup {
        public final Simulator sim;
        public final LED[] leds;

        public LEDGroup(Simulator sim, LED[] nleds) {
            this.sim = sim;
            this.leds = nleds;
        }
    }

    protected LED(Simulator s, int n, String c) {
        sim = s;
        colornum = n;
        color = c;
        //setup energy recording
        Clock clk = sim.getClock();

        state = new FiniteStateMachine(clk, startMode, modeName, 0);
        probe = new LEDProbe();
        new Energy(c, modeAmpere, state, sim.getSimulation().getEnergyControl());
    }

    public void write(boolean level) {
        // NOTE: there is an inverter between the port and the LED, we reverse the level
        int snum = level ? 0 : 1;
        state.transition(snum);
    }

    public void enablePrinting() {
        state.insertProbe(probe);
    }

    public void disablePrinting() {
        state.removeProbe(probe);
    }

    public int getState() {
        return state.getCurrentState();
    }

    /**
     * The <code>getFSM()</code> method returns the <code>FiniteStateMachine</code> instance corresponding
     * to this LED. This finite state machine representation allows probes to be inserted that are notified
     * when the state of the LED is changed.
     * @return an instance of the <code>FiniteStateMachine</code> class for this LED instance
     */
    public FiniteStateMachine getFSM() {
        return state;
    }
}
