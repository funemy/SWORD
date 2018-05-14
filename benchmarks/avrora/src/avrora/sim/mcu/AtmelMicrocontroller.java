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
import avrora.sim.*;
import avrora.sim.clock.ClockDomain;
import avrora.sim.clock.MainClock;
import cck.text.StringUtil;
import java.util.HashMap;
import java.util.NoSuchElementException;

/**
 * The <code>AtmelMicrocontroller</code> class represents the common functionality among microcontrollers
 * for the Atmel series. These all contain a clock domain (collection of internal, external clocks), a
 * simulator, an interpreter, microcontroller properties, and a mapping between string names and IO reg
 * addresses, etc.
 *
 * @author Ben L. Titzer
 */
public abstract class AtmelMicrocontroller extends DefaultMCU {

    protected final MainClock mainClock;
    protected AtmelInterpreter interpreter;

    public final AVRProperties properties;

    protected final HashMap devices;
    public static final int MODE_ACTIVE = 0;

    /**
     * The <code>sleep()</code> method is called by the interpreter when the program executes a SLEEP
     * instruction. This method transitions the microcontroller into a sleep mode, including turning
     * off any devices, shutting down clocks, and transitioning the sleep FSM into a sleep mode.
     *
     * @see Microcontroller#sleep()
     */
    public void sleep() {
        // transition to the sleep state in the MCUCR register
        sleepState.transition(getSleepMode());
    }

    protected abstract int getSleepMode();

    /**
     * The <code>wakeup()</code> method is called by the interpreter when the microcontroller is
     * woken from a sleep mode by an interrupt or other event. This method transitions the
     * microcontroller back into active mode, turning back on devices. This method returns
     * the number of clock cycles necessary to wake the MCU from sleep.
     *
     * @return cycles it takes to wake up
     * @see Microcontroller#wakeup()
     */
    public int wakeup() {
        // transition to the active state (may insert transition event into event queue)
        sleepState.transition(MODE_ACTIVE);
        // return the number of cycles consumed by waking up
        return sleepState.getTransitionTime(sleepState.getCurrentState(), MODE_ACTIVE);
    }

    protected AtmelMicrocontroller(ClockDomain cd, AVRProperties p, FiniteStateMachine fsm) {
        super(cd, p.num_pins, p.getRegisterLayout().instantiate(), fsm);
        mainClock = cd.getMainClock();
        properties = p;
        devices = new HashMap();
    }

    /**
     * The <code>installIOReg()</code> method installs an IO register with the specified name. The register
     * layout for this microcontroller is used to get the address of the register (if it exists) and
     * install the <code>ActiveRegister</code> object into the correct place.
     * @param name the name of the IO register as a string
     * @param reg the register to install
     */
    protected ActiveRegister installIOReg(String name, ActiveRegister reg) {
        interpreter.installIOReg(properties.getIOReg(name), reg);
	return reg;
    }

    /**
     * The <code>getIOReg()</code> method gets a reference to the active register currently installed for
     * the specified name. The register layout for this microcontroller is used to get the correct address.
     * @param name the name of the IO register as a string
     * @return a reference to the active register object if it exists
     */
    protected ActiveRegister getIOReg(String name) {
        return interpreter.getIOReg(properties.getIOReg(name));
    }

    /**
     * The <code>addDevice()</code> method adds a new internal device to this microcontroller so that it can
     * be retrieved later with <code>getDevice()</code>
     * @param d the device to add to this microcontroller
     */
    protected void addDevice(AtmelInternalDevice d) {
        devices.put(d.name, d);
    }

    /**
     * The <code>getDevice()</code> method is used to get a reference to an internal device with the given name.
     * For example, the ADC device will be under the name "adc" and Timer0 will be under the name "timer0". This
     * is useful for external devices that need to connect to the input of internal devices.
     *
     * @param name the name of the internal device as a string
     * @return a reference to the internal device if it exists; null otherwise
     */
    public AtmelInternalDevice getDevice(String name) {
        return (AtmelInternalDevice)devices.get(name);
    }

    public static void addPin(HashMap pinMap, int p, String n) {
        pinMap.put(n, new Integer(p));
    }

    public static void addPin(HashMap pinMap, int p, String n1, String n2) {
        Integer i = new Integer(p);
        pinMap.put(n1, i);
        pinMap.put(n2, i);
    }

    public static void addPin(HashMap pinMap, int p, String n1, String n2, String n3) {
        Integer i = new Integer(p);
        pinMap.put(n1, i);
        pinMap.put(n2, i);
        pinMap.put(n3, i);
    }

    public static void addPin(HashMap pinMap, int p, String n1, String n2, String n3, String n4) {
        Integer i = new Integer(p);
        pinMap.put(n1, i);
        pinMap.put(n2, i);
        pinMap.put(n3, i);
        pinMap.put(n4, i);
    }

    public static void addInterrupt(HashMap iMap, String n, int i) {
        iMap.put(n, new Integer(i));
    }

    /**
     * The <code>getPin()</code> method looks up the named pin and returns a reference to that pin. Names of
     * pins should be UPPERCASE. The intended users of this method are external device implementors which
     * connect their devices to the microcontroller through the pins.
     *
     * @param n the name of the pin; for example "PA0" or "OC1A"
     * @return a reference to the <code>Pin</code> object corresponding to the named pin if it exists; null
     *         otherwise
     */
    public Microcontroller.Pin getPin(String n) {
        return pins[properties.getPin(n)];
    }

    /**
     * The <code>getProperties()</code> method gets a reference to the microcontroller properties for this
     * microcontroller instance.
     * @return a reference to the microcontroller properties for this instance
     */
    public MCUProperties getProperties() {
        return properties;
    }

}
