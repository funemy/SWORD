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

package avrora.arch.legacy;

import avrora.sim.*;

/**
 * The <code>LegacyState</code> class represents the state of the simulator, including the contents of registers and
 * memory.
 *
 * @author Ben L. Titzer
 */
public interface LegacyState extends State {

    int NUM_REGS = 32; // number of general purpose registers
    int IOREG_BASE = 32; // base address of IO registers
    int SREG_I = 7;
    int SREG_T = 6;
    int SREG_H = 5;
    int SREG_S = 4;
    int SREG_V = 3;
    int SREG_N = 2;
    int SREG_Z = 1;
    int SREG_C = 0;
    int SREG_I_MASK = 1 << SREG_I;
    int SREG_T_MASK = 1 << SREG_T;
    int SREG_H_MASK = 1 << SREG_H;
    int SREG_S_MASK = 1 << SREG_S;
    int SREG_V_MASK = 1 << SREG_V;
    int SREG_N_MASK = 1 << SREG_N;
    int SREG_Z_MASK = 1 << SREG_Z;
    int SREG_C_MASK = 1;

    /**
     * The <code>getInterruptTable()</code> method gets a reference to the interrupt table,
     * which contains information about each interrupt, such as whether it is enabled, posted,
     * pending, etc.
     * @return a reference to the <code>InterruptTable</code> instance
     */
    public InterruptTable getInterruptTable();

    /**
     * Read a general purpose register's current value as a byte.
     *
     * @param reg the register to read
     * @return the current value of the register
     */
    public byte getRegisterByte(LegacyRegister reg);

    /**
     * Read a general purpose register's current value as an integer, without any sign extension.
     *
     * @param reg the register to read
     * @return the current unsigned value of the register
     */
    public int getRegisterUnsigned(LegacyRegister reg);

    /**
     * Read a general purpose register pair as an unsigned word. This method will read the value of the
     * specified register and the value of the next register in numerical order and return the two values
     * combined as an unsigned integer The specified register should be less than r31, because r32 (the next
     * register) does not exist.
     *
     * @param reg the low register of the pair to read
     * @return the current unsigned word value of the register pair
     */
    public int getRegisterWord(LegacyRegister reg);


    /**
     * The <code>getSREG()</code> method reads the value of the status register. The status register contains
     * the I, T, H, S, V, N, Z, and C flags, in order from highest-order to lowest-order.
     *
     * @return the value of the status register as a byte.
     */
    public byte getSREG();


    /**
     * The <code>getStackByte()</code> method reads a byte from the address specified by SP+1. This method
     * should not be called with an empty stack, as it will cause an exception consistent with trying to read
     * non-existent memory.
     *
     * @return the value on the top of the stack
     */
    public byte getStackByte();

    /**
     * The <code>getDataByte()</code> method reads a byte value from the data memory (SRAM) at the specified
     * address.
     *
     * @param address the byte address to read
     * @return the value of the data memory at the specified address
     * @throws ArrayIndexOutOfBoundsException if the specified address is not the valid memory range
     */
    public byte getDataByte(int address);

    /**
     * The <code>getProgramByte()</code> method reads a byte value from the program (Flash) memory. The flash
     * memory generally stores read-only values and the instructions of the program. Care should be taken that
     * the program memory at the specified address does not contain an instruction. This is because, in
     * general, programs should not read instructions as data, and secondly, because no assembler is present
     * in Avrora and therefore the actual byte value of an instruction may not be known.
     *
     * @param address the byte address at which to read
     * @return the byte value of the program memory at the specified address
     * @throws ArrayIndexOutOfBoundsException if the specified address is not the valid program memory range
     */
    public byte getProgramByte(int address);

    /**
     * The <code>getIORegisterByte()</code> method reads the value of an IO register. Invocation of this
     * method causes an invocatiobn of the <code>.read()</code> method on the corresponding internal
     * <code>IOReg</code> object, and its value returned.
     *
     * @param ioreg the IO register number
     * @return the value of the IO register
     */
    public byte getIORegisterByte(int ioreg);

    /**
     * The <code>getIOReg()</code> method is used to retrieve a reference to the actual <code>IOReg</code>
     * instance stored internally in the state. This is generally only used in the simulator and device
     * implementations, and clients should probably not call this memory directly.
     *
     * @param ioreg the IO register number to retrieve
     * @return a reference to the <code>ActiveRegister</code> instance of the specified IO register
     */
    public ActiveRegister getIOReg(int ioreg);

    /**
     * The <code>getSleepMode()</code> method returns an integer code describing which sleep mode the
     * microcontroller is currently in.
     *
     * @return an integer code representing the current sleep mode
     */
    public int getSleepMode();

}
