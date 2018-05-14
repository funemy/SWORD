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

package avrora.arch.avr;

import avrora.sim.CodeSegment;
import avrora.sim.mcu.MCUProperties;
import avrora.sim.mcu.RegisterLayout;
import java.util.HashMap;

/**
 * The <code>MicrocontrollerProperties</code> class is simply a wrapper class around several
 * properties of a microcontroller including the size of the IO registers, the size of SRAM,
 * flash, and EEPROM, as well as the mapping between names of pins and their physical pin
 * number.
 *
 * @author Ben L. Titzer
 */
public class AVRProperties extends MCUProperties {

    /**
     * The <code>ioreg_size</code> field stores the number of IO registers on this microcontroller.
     */
    public final int ioreg_size;

    /**
     * The <code>sram_size</code> field stores the size of the SRAM (excluding the general purpose
     * registers and IO registers) on this microcontroller.
     */
    public final int sram_size;

    /**
     * The <code>flash_size</code> field stores the size of the code segment (flash) on this microcontroller.
     */
    public final int flash_size;

    /**
     * The <code>eeprom_size</code> field stores the size of the EEPROM on this microcontroller.
     */
    public final int eeprom_size;

    /**
     * The <code>num_pins</code> field stores the number of physical pins on this microcontroller.
     */
    public final int num_pins;

    public final CodeSegment.Factory codeSegmentFactory;

    /**
     * The constructor for the <code>MicrocontrollerProperties</code> class creates a new
     * instance with the specified register size, flash size, etc. All such fields are immutable,
     * and the pin assignments and IO register assignments cannot be changed.
     *
     * @param is the number of IO registers on this microcontroller
     * @param ss the size of the SRAM in bytes
     * @param fs the size of the flash in bytes
     * @param es the size of the EEPROM in bytes
     * @param np the number of physical pins on the microcontroller
     * @param ni the number of interrupts on the microcontroller
     * @param pa a <code>HashMap</code> instance mapping string names to <code>Integer</code>
     * indexes for the pins
     * @param rl a <code>RegisterLayout</code> instance mapping string names to IO register addresses
     * @param inta a <code>HashMap</code> instance mapping string names to <code>Integer</code>
     * indexes for each type of interrupt
     */
    public AVRProperties(int is, int ss, int fs, int es, int np, int ni, CodeSegment.Factory csf, HashMap pa, RegisterLayout rl, HashMap inta) {
        super(pa, rl, inta, ni);

        ioreg_size = is;
        sram_size = ss;
        flash_size = fs;
        eeprom_size = es;
        num_pins = np;

        codeSegmentFactory = csf;
    }

}
