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
 * Creation date: Sep 7, 2005
 */

package avrora.arch.avr;

import avrora.sim.*;
import avrora.sim.clock.DeltaQueue;

/**
 * The <code>AVRState</code> class represents an instance of the internal state of an <code>AVRInstrInterpreter</code>
 * instance. This class allows access to the state of the interpreter without exposing the details of the implementation
 * or jeopardizing the soundness of the simulation.
 * <p/>
 * </p> An <code>AVRState</code> instance contains the state of registers, memory, the code segment, and the IO
 * registers, as well as the interrupt table and <code>MainClock</code> instance. It provides a public interface through
 * the <code>get_XXX()</code> methods and a protected interface used in <code>AVRInstrInterpreter</code> that allows direct
 * access to the fields representing the actual state.
 *
 * @author Ben L. Titzer
 */
public abstract class AVRState implements State {

    public static int NUM_REGS = 32;

    protected Simulator simulator;

    protected InterruptTable interrupts;

    protected int pc;
    protected int nextpc;
    protected boolean I, T, H, V, S, N, Z, C;
    protected AVRDataSegment sram;
    protected byte[] regs;
    protected CodeSegment flash;
    protected ActiveRegister[] ioregs;
    protected DeltaQueue queue;
    protected ActiveRegister SREG_reg;
    protected int SREG;
    protected int cycles;
    protected boolean justReturnedFromInterrupt;

    /**
     * The <code>getPC()</code> retrieves the current program counter.
     *
     * @return the program counter as a byte address
     */
    public int getPC() {
        return pc;
    }

    /**
     * The <code>getSP()</code> method reads the current value of the stack pointer. Since the stack pointer is stored
     * in two IO registers, this method will cause the invocation of the <code>.read()</code> method on each of the
     * <code>IOReg</code> objects that store these values.
     *
     * @return the value of the stack pointer as a byte address
     */
    public abstract int getSP();

    /**
     * The <code>getSRAM()</code> method reads a byte value from the data memory (SRAM) at the specified address. This
     * method is intended for use by probes and watches; thus, it does not trigger any watches that may be installed at
     * the memory address specified, since doing so could lead to infinite recursion (if a watch attempts to get the
     * value of the byte at the location where it itself is installed) or alter the metrics being measured by the
     * instrumentation at that address.
     *
     * @param address the byte address to read
     * @return the value of the data memory at the specified address
     * @throws ArrayIndexOutOfBoundsException if the specified address is not the valid memory range
     */
    public byte getSRAM(int address) {
        return sram.get(address);
    }

    /**
     * The <code>getFlash()</code> method reads a byte value from the program (Flash) memory. The flash memory generally
     * stores read-only values and the instructions of the program. This method is intended for use by probes and
     * watches; thus, it does not trigger any watches or probes that may be installed at the memory address specified,
     * since doing so could lead to infinite recursion (if a watch attempts to get the value of the byte at the location
     * where it itself is installed) or alter the metrics being measured by the instrumentation at that address.
     *
     * @param address the byte address at which to read
     * @return the byte value of the program memory at the specified address
     * @throws ArrayIndexOutOfBoundsException if the specified address is not the valid program memory range
     */
    public byte getFlash(int address) {
        return flash.get(address);
    }

    /**
     * The <code>getIOReg()</code> method reads the value of an IO register as a byte. Invocation of this method causes
     * an invocation of the <code>.read()</code> method on the corresponding internal <code>IOReg</code> object, and its
     * value returned. Very few devices have behavior that is triggered by a read from an IO register, but care should
     * be taken when calling this method for one of those IO registers.
     *
     * @param ior the IO register number
     * @return the value of the IO register
     */
    public byte getIOReg(int ior) {
        return ioregs[ior].read();
    }

    /**
     * The <code>getRegister()</code> method reads a general purpose register's current value as a byte.
     *
     * @param reg the register to read
     * @return the current value of the specified register as a byte
     */
    public byte getRegister(AVRSymbol.GPR reg) {
        return regs[reg.value];
    }

    /**
     * The <code>getCycles()</code> method returns the clock cycle count recorded so far in the simulation.
     *
     * @return the number of clock cycles elapsed in the simulation
     */
    public long getCycles() {
        return queue.getCount();
    }

    /**
     * The <code>getSREG()</code> method reads the value of the status register. The status register contains the I, T,
     * H, S, V, N, Z, and C flags, in order from highest-order to lowest-order.
     *
     * @return the value of the status register as a byte.
     */
    public byte getSREG() {
        return SREG_reg.read();
    }

    /**
     * The <code>isEnabled()</code> method checks whether the specified interrupt is currently enabled.
     *
     * @param inum the interrupt number to check
     * @return true if the specified interrupt is currently enabled; false otherwise
     */
    public boolean isEnabled(int inum) {
        return interrupts.isEnabled(inum);
    }

    /**
     * The <code>isPosted()</code> method checks whether the specified interrupt is currently posted.
     *
     * @param inum the interrupt number to check
     * @return true if the specified interrupt is currently posted; false otherwise
     */
    public boolean isPosted(int inum) {
        return interrupts.isPosted(inum);
    }

    /**
     * The <code>isPending()</code> method checks whether the specified interrupt is currently pending.
     *
     * @param inum the interrupt number to check
     * @return true if the specified interrupt is currently pending; false otherwise
     */
    public boolean isPending(int inum) {
        return interrupts.isPending(inum);
    }

    /**
     * The <code>getSimulator()</code> method returns the simulator associated with this state instance.
     *
     * @return a reference to the simulator associated with this state instance.
     */
    public Simulator getSimulator() {
        return simulator;
    }

    protected static int map_get(byte[] a, int indx) {
        return a[indx];
    }

    protected static void map_set(byte[] a, int indx, int val) {
        a[indx] = (byte)val;
    }

    protected static int map_get(Segment s, int addr) {
        return s.read(addr);
    }

    protected static void map_set(Segment s, int addr, int val) {
        s.write(addr, (byte)val);
    }

    protected static int map_get(ActiveRegister[] s, int addr) {
        return s[addr].read();
    }

    protected static void map_set(ActiveRegister[] s, int addr, int val) {
        s[addr].write((byte)val);
    }
}
