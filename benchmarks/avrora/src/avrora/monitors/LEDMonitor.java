/**
 * Copyright (c) 2007, Ben L. Titzer
 * See the file "license.txt" for details.
 *
 * Created Nov 18, 2007
 */
package avrora.monitors;

import avrora.sim.Simulator;
import avrora.sim.FiniteStateMachine;
import avrora.sim.output.SimPrinter;
import avrora.sim.platform.LED;
import avrora.sim.platform.Platform;
import avrora.sim.util.SimUtil;
import cck.text.Terminal;
import cck.text.Printer;

/**
 * The <code>LEDMonitor</code> class implements a monitor that tracks any LEDs on
 * a device, printing them to the console as they are turned on and off by the
 * microcontroller program.
 *
 * @author Ben L. Titzer
 */
public class LEDMonitor extends MonitorFactory {

    public LEDMonitor() {
        super("This monitor records and traces each change to the state of the LEDs of " +
                "a device.");
    }

    protected class Mon implements Monitor, FiniteStateMachine.Probe {

        LED.LEDGroup ledgroup;
        SimPrinter printer;

        public Mon(Simulator s) {
            Platform platform = s.getMicrocontroller().getPlatform();
            printer = s.getPrinter();
            Object dev = platform.getDevice("leds");
            if (dev instanceof LED.LEDGroup) {
                ledgroup = (LED.LEDGroup)dev;
                LED[] leds = ledgroup.leds;
                for ( int cntr = 0; cntr < leds.length; cntr++ ) {
                    leds[cntr].state.insertProbe(this);
                }
            }
        }

        public void fireBeforeTransition(int beforeState, int afterState) {
            // do nothing.
        }

        public void fireAfterTransition(int beforeState, int afterState) {
            if ( beforeState == afterState ) return;
            // print the status of the LED
            StringBuffer buf = printer.getBuffer(30);
            LED[] leds = ledgroup.leds;
            for ( int cntr = 0; cntr < leds.length; cntr++ ) {
                if ( leds[cntr].getState() == 0 ) buf.append("off ");
                else Terminal.append(leds[cntr].colornum, buf, "on  ");
            }
            printer.printBuffer(buf);
        }

        public void report() {
            // do nothing.
        }
    }

    public Monitor newMonitor(Simulator s) {
        return new Mon(s);
    }
}
