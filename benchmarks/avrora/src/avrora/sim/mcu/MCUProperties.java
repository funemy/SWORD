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

import cck.text.StringUtil;
import java.util.*;

/**
 * @author Ben L. Titzer
 */
public class MCUProperties {

    protected static final int BASE_ADDRESS = 32;

    protected final HashMap pinAssignments;
    protected final RegisterLayout layout;
    protected final HashMap interruptAssignments;
    protected final String[] ioreg_name;
    protected final String[] interrupt_name;

    public final int num_interrupts;

    protected MCUProperties(HashMap pa, RegisterLayout rl, HashMap inta, int ni) {
        pinAssignments = pa;
        layout = rl;
        interruptAssignments = inta;

        ioreg_name = initIORNames();
        interrupt_name = initInterruptNames();
        num_interrupts = ni;
    }

    public RegisterLayout getRegisterLayout() {
        return layout;
    }

    protected String[] initInterruptNames() {
        int max = getMax();
        String[] interrupt_name = new String[max+1];
        Iterator i = interruptAssignments.keySet().iterator();
        while ( i.hasNext() ) {
            String s = (String)i.next();
            Integer iv = (Integer)interruptAssignments.get(s);
            interrupt_name[iv.intValue()] = s;
        }
        return interrupt_name;
    }

    private int getMax() {
        int max = 0;
        Iterator i = interruptAssignments.keySet().iterator();
        while ( i.hasNext() ) {
            String s = (String)i.next();
            int v = ((Integer)interruptAssignments.get(s)).intValue();
            if ( max < v ) max = v;
        }
        return max;
    }

    protected String[] initIORNames() {
        String[] ioreg_name = new String[layout.ioreg_size];
        for ( int cntr = 0; cntr < layout.ioreg_size; cntr++ ) {
            RegisterLayout.RegisterInfo registerInfo = layout.info[cntr];
            if ( registerInfo != null )
                ioreg_name[cntr] = registerInfo.name;
        }
        return ioreg_name;
    }

    /**
     * The <code>getPin()</code> method retrieves the pin number for the given pin name for this
     * microcontroller.
     * @param n the name of the pin such as "OC0"
     * @return an integer representing the physical pin number if it exists;
     * @throws NoSuchElementException if the specified pin name does not have an assignment
     */
    public int getPin(String n) {
        Integer i = (Integer)pinAssignments.get(n);
        if ( i == null )
            throw new NoSuchElementException(StringUtil.quote(n)+" pin not found");
        return i.intValue();
    }

    /**
     * The <code>getIOReg()</code> method retrieves the IO register number for the given IO
     * Register name for this microcontroller.
     * @param n the name of the IO register such as "TCNT0"
     * @return an integer representing the IO register number if it exists
     * @throws NoSuchElementException if the specified IO register name does not have an assignment
     */
    public int getIOReg(String n) {
        return layout.getIOReg(n);
    }

    /**
     * The <code>getIORegAddr()</code> method retrieves the IO register address (in SRAM) for the
     * given IO Register name for this microcontroller.
     * @param n the name of the IO register such as "TCNT0"
     * @return an integer representing the IO register address if it exists
     * @throws NoSuchElementException if the specified IO register name does not have an assignment
     */
    public int getIORegAddr(String n) {
        return getIOReg(n) + BASE_ADDRESS;
    }

    /**
     * The <code>hasIOReg()</code> method queries whether the IO register exists on this device.
     * @param n the name of the IO register
     * @return true if the IO register exists on this device; false otherwise
     */
    public boolean hasIOReg(String n) {
        return layout.hasIOReg(n);
    }

    /**
     * The <code>getInterrupt()</code> method retrieves the interrupt number for the given interrupt
     * name for this microcontroller
     * @param n the name of the interrupt such as "RESET"
     * @return an integer representing the interrupt number if it exists
     * @throws NoSuchElementException if the specified interrupt name does not have an assignment
     */
    public int getInterrupt(String n) {
        Integer i = (Integer)interruptAssignments.get(n);
        if ( i == null )
            throw new NoSuchElementException(StringUtil.quote(n)+" interrupt not found");
        return i.intValue();
    }

    /**
     * The <code>getIORegName()</code> method returns the name of the IO register specified by
     * the given number.
     * @param ioreg the io register number for which to get a string name
     * @return the string name of the IO register if there is such a name
     */
    public String getIORegName(int ioreg) {
        return ioreg_name[ioreg];
    }

    /**
     * The <code>getInterruptName()</code> method returns the name of an interrupt specified by
     * the given number.
     * @param inum the interrupt number for which to get a string name
     * @return the string name of the interrupt if there is such a name
     */
    public String getInterruptName(int inum) {
        return interrupt_name[inum];
    }
}
