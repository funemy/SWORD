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

package avrora.sim;

import avrora.arch.AbstractInstr;
import avrora.arch.avr.AVRProperties;
import avrora.arch.legacy.*;
import avrora.core.Program;
import avrora.sim.mcu.RegisterSet;
import avrora.sim.util.*;
import avrora.sim.state.VolatileBehavior;
import cck.util.Arithmetic;
import cck.util.Util;

/**
 * The <code>BaseInterpreter</code> class represents a base class of the legacy interpreter and the generated
 * interpreter(s) that stores the state of the executing program, e.g. registers and flags, etc.
 *
 * @author Ben L. Titzer
 */
public abstract class AtmelInterpreter extends Interpreter implements LegacyInstrVisitor {

    public static final boolean INSTRUMENTED = true;
    public static final boolean UNINSTRUMENTED = false;
    public static final int NUM_REGS = 32;

    // fields (ordered roughly by their frequency of use)
    protected LegacyInstr[] shared_instr; // shared for performance reasons only

    protected int pc;
    protected int nextPC;
    protected int cyclesConsumed;
    protected boolean I;
    protected boolean T;
    protected boolean H;
    protected boolean S;
    protected boolean V;
    protected boolean N;
    protected boolean Z;
    protected boolean C;

    protected byte[] sram;
    protected final int sram_start;
    protected final int sram_max;
    protected MulticastWatch[] sram_watches;
    protected final VolatileBehavior[] sram_volatile;

    protected final ActiveRegister[] ioregs;

    protected final CodeSegment flash;
    protected final RWRegister SPL_reg;
    protected final RWRegister SPH_reg;

    public final int RAMPZ; // location of the RAMPZ IO register
    public final int SREG; // location of the SREG IO register

    protected final RegisterSet registers;
    protected final StateImpl state;

    protected int bootPC; // start up address
    protected int interruptBase; // base of interrupt vector table

    protected MulticastWatch error_watch;

    protected final MulticastProbe globalProbe;

    protected long delayCycles;

    protected boolean shouldRun;

    protected boolean sleeping;

    protected boolean justReturnedFromInterrupt;

    public class StateImpl implements LegacyState {

        /**
         * The <code>getSimulator()</code> method returns the simulator associated with this state
         * instance.
         * @return a reference to the simulator associated with this state instance.
         */
        public Simulator getSimulator() {
            return simulator;
        }

        /**
         * The <code>getInterruptTable()</code> method gets a reference to the interrupt table,
         * which contains information about each interrupt, such as whether it is enabled, posted,
         * pending, etc.
         * @return a reference to the <code>InterruptTable</code> instance
         */
        public InterruptTable getInterruptTable() {
            return interrupts;
        }

        /**
         * Read a general purpose register's current value as a byte.
         *
         * @param reg the register to read
         * @return the current value of the register
         */
        public byte getRegisterByte(LegacyRegister reg) {
            return sram[reg.getNumber()];
        }

        /**
         * Read a general purpose register's current value as an integer, without any sign extension.
         *
         * @param reg the register to read
         * @return the current unsigned value of the register
         */
        public int getRegisterUnsigned(LegacyRegister reg) {
            return sram[reg.getNumber()] & 0xff;
        }

        /**
         * Read a general purpose register pair as an unsigned word. This method will read the value of the
         * specified register and the value of the next register in numerical order and return the two values
         * combined as an unsigned integer The specified register should be less than r31, because r32 (the next
         * register) does not exist.
         *
         * @param reg the low register of the pair to read
         * @return the current unsigned word value of the register pair
         */
        public int getRegisterWord(LegacyRegister reg)  {
            int number = reg.getNumber();
            return Arithmetic.uword(sram[number], sram[number+1]);
        }


        /**
         * The <code>getSREG()</code> method reads the value of the status register. The status register contains
         * the I, T, H, S, V, N, Z, and C flags, in order from highest-order to lowest-order.
         *
         * @return the value of the status register as a byte.
         */
        public byte getSREG() {
            int value = 0;
            if (I) value |= LegacyState.SREG_I_MASK;
            if (T) value |= LegacyState.SREG_T_MASK;
            if (H) value |= LegacyState.SREG_H_MASK;
            if (S) value |= LegacyState.SREG_S_MASK;
            if (V) value |= LegacyState.SREG_V_MASK;
            if (N) value |= LegacyState.SREG_N_MASK;
            if (Z) value |= LegacyState.SREG_Z_MASK;
            if (C) value |= LegacyState.SREG_C_MASK;
            return (byte) value;
        }


        public boolean getFlag(int bit) {
            return AtmelInterpreter.this.getFlag(bit);
        }


        /**
         * The <code>getStackByte()</code> method reads a byte from the address specified by SP+1. This method
         * should not be called with an empty stack, as it will cause an exception consistent with trying to read
         * non-existent memory.
         *
         * @return the value on the top of the stack
         */
        public byte getStackByte() {
            int address = getSP();
            return getDataByte(address);
        }

        /**
         * The <code>getSP()</code> method reads the current value of the stack pointer. Since the stack pointer
         * is stored in two IO registers, this method will cause the invocation of the <code>.read()</code> method
         * on each of the <code>IOReg</code> objects that store these values.
         *
         * @return the value of the stack pointer as a byte address
         */
        public int getSP() {
            byte low = SPL_reg.value;
            byte high = SPH_reg.value;
            return Arithmetic.uword(low, high);
        }

        /**
         * The <code>getPC()</code> retrieves the current program counter.
         *
         * @return the program counter as a byte address
         */
        public int getPC() {
            return pc;
        }

        /**
         * The <code>getInstr()</code> can be used to retrieve a reference to the <code>LegacyInstr</code> object
         * representing the instruction at the specified program address. Care should be taken that the address in
         * program memory specified does not contain data. This is because Avrora does have a functioning
         * disassembler and assumes that the <code>LegacyInstr</code> objects for each instruction in the program are
         * known a priori.
         *
         * @param address the byte address from which to read the instruction
         * @return a reference to the <code>LegacyInstr</code> object representing the instruction at that address in
         *         the program
         */
        public AbstractInstr getInstr(int address) {
            return flash.readInstr(address);
        }

        /**
         * The <code>getDataByte()</code> method reads a byte value from the data memory (SRAM) at the specified
         * address.
         *
         * @param address the byte address to read
         * @return the value of the data memory at the specified address
         * @throws ArrayIndexOutOfBoundsException if the specified address is not the valid memory range
         */
        public byte getDataByte(int address) {
            return readSRAM(UNINSTRUMENTED, address);
        }

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
        public byte getProgramByte(int address) {
            return flash.get(address);
        }

        /**
         * The <code>getIORegisterByte()</code> method reads the value of an IO register. Invocation of this
         * method causes an invocatiobn of the <code>.read()</code> method on the corresponding internal
         * <code>IOReg</code> object, and its value returned.
         *
         * @param ioreg the IO register number
         * @return the value of the IO register
         */
        public byte getIORegisterByte(int ioreg) {
            return getAR(ioreg).read();
        }

        /**
         * The <code>getIOReg()</code> method is used to retrieve a reference to the actual <code>IOReg</code>
         * instance stored internally in the state. This is generally only used in the simulator and device
         * implementations, and clients should probably not call this memory directly.
         *
         * @param ioreg the IO register number to retrieve
         * @return a reference to the <code>ActiveRegister</code> instance of the specified IO register
         */
        public ActiveRegister getIOReg(int ioreg) {
            return getAR(ioreg);
        }

        private ActiveRegister getAR(int ioreg) {
            return ioregs[ioreg];
        }

        /**
         * The <code>getCycles()</code> method returns the clock cycle count recorded so far in the simulation.
         *
         * @return the number of clock cycles elapsed in the simulation
         */
        public long getCycles() {
            return clock.getCount();
        }

        /**
         * The <code>getSleepMode()</code> method returns an integer code describing which sleep mode the
         * microcontroller is currently in.
         *
         * @return an integer code representing the current sleep mode
         */
        public int getSleepMode() {
            throw Util.unimplemented();
        }

    }

    /**
     * The constructor for the <code>BaseInterpreter</code> class initializes the node's flash,
     * SRAM, general purpose registers, IO registers, and loads the program onto the flash. It
     * uses the <code>MicrocontrollerProperties</code> instance to configure the interpreter
     * such as the size of flash, SRAM, and location of IO registers.
     * @param simulator the simulator instance for this interpreter
     * @param p the program to load onto this interpreter instance
     * @param pr the properties of the microcontroller being simulated
     */
    protected AtmelInterpreter(Simulator simulator, Program p, AVRProperties pr) {
        super(simulator);
        // this class and its methods are performance critical
        // observed speedup with this call on Hotspot
        Compiler.compileClass(getClass());

        state = new StateImpl();

        globalProbe = new MulticastProbe();

        SREG = pr.getIOReg("SREG");

        // only look for the RAMPZ register if the flash is more than 64kb
        if ( pr.hasIOReg("RAMPZ") )
            RAMPZ = pr.getIOReg("RAMPZ");
        else
            RAMPZ = -1;

        // if program will not fit onto hardware, error
        if (p.program_end > pr.flash_size)
            throw Util.failure("program will not fit into " + pr.flash_size + " bytes");

        // beginning address of SRAM array
        sram_start = toSRAM(pr.ioreg_size);

        // maximum data address
        sram_max = NUM_REGS + pr.ioreg_size + pr.sram_size;

        // allocate SRAM
        sram = new byte[sram_max];

        // initialize IO registers to default values
        registers = simulator.getMicrocontroller().getRegisterSet();

        // create the behavior for the volatile region
        sram_volatile = new VolatileBehavior[sram_start];
        VolatileBehavior b = new VolatileBehavior();
        for ( int i = 0; i < sram_volatile.length; i++ )
            sram_volatile[i] = b;
        // support the old ActiveRegisters with wrappers
        ioregs = registers.share();
        for ( int i = 0; i < ioregs.length; i++ )
            sram_volatile[toSRAM(i)] = new IORegBehavior(ioregs[i]);

        // set up the status register volatile
        sram_volatile[toSRAM(SREG)] = new SREGBehavior();

        // allocate FLASH
        flash = pr.codeSegmentFactory.newCodeSegment("flash", this, p);
        // for performance, we share a reference to the LegacyInstr[] array representing flash
        // TODO: implement share() method
        shared_instr = flash.shareCode(null);

        // initialize the interrupt table
        interrupts = new InterruptTable(this, pr.num_interrupts);

        SPL_reg = (RWRegister) ioregs[pr.getIOReg("SPL")];
        SPH_reg = (RWRegister) ioregs[pr.getIOReg("SPH")];
    }

    public void start() {
        shouldRun = true;
        runLoop();
    }

    public void stop() {
        shouldRun = false;
        innerLoop = false;
    }

    public State getState() {
        return state;
    }

    protected abstract void runLoop();

    /**
     * The <code>getInterruptVectorAddress()</code> method computes the location in memory to jump to for the
     * given interrupt number. On the Atmega128, the starting point is the beginning of memory and each
     * interrupt vector slot is 4 bytes. On older architectures, this is not the case, therefore this method
     * has to be implemented according to the specific device being simulated.
     *
     * @param inum the interrupt number
     * @return the byte address that represents the address in the program to jump to when this interrupt is
     *         fired
     */
    protected int getInterruptVectorAddress(int inum) {
        return interruptBase + (inum - 1) * 4;
    }

    /**
     * The <code>setPosted()<code> method is used by external devices to post and unpost interrupts.
     * @param inum the interrupt number to post or unpost
     * @param post true if the interrupt should be posted; false if the interrupt should be unposted
     */
    public void setPosted(int inum, boolean post) {
        if ( post ) interrupts.post(inum);
        else interrupts.unpost(inum);
    }

    /**
     * The <code>setEnabled()</code> method is used by external devices (and mask registers) to enable
     * and disable interrupts.
     * @param inum the interrupt number to enable or disable
     * @param enabled true if the interrupt should be enabled; false if the interrupt should be disabled
     */
    public void setEnabled(int inum, boolean enabled) {
        if ( enabled ) {
            innerLoop = false;
            interrupts.enable(inum);
        } else interrupts.disable(inum);
    }

    /**
     * The <code>insertProbe()</code> method is used internally to insert a probe on a particular instruction.
     * @param p the probe to insert on an instruction
     * @param addr the address of the instruction on which to insert the probe
     */
    protected void insertProbe(Simulator.Probe p, int addr) {
        flash.insertProbe(addr, p);
    }

    /**
     * The <code>insertExceptionWatch()</code> method registers an </code>ExceptionWatch</code> to listen for
     * exceptional conditions in the machine.
     *
     * @param watch The <code>ExceptionWatch</code> instance to add.
     */
    protected void insertErrorWatch(Simulator.Watch watch) {
        if ( error_watch == null ) error_watch = new MulticastWatch();
        error_watch.add(watch);
    }

    /**
     * The <code>insertProbe()</code> method allows a probe to be inserted that is executed before and after
     * every instruction that is executed by the simulator
     *
     * @param p the probe to insert
     */
    protected void insertProbe(Simulator.Probe p) {
        innerLoop = false;
        globalProbe.add(p);
    }

    /**
     * The <code>removeProbe()</code> method is used internally to remove a probe from a particular instruction.
     * @param p the probe to remove from an instruction
     * @param addr the address of the instruction from which to remove the probe
     */
    protected void removeProbe(Simulator.Probe p, int addr) {
        flash.removeProbe(addr, p);
    }

    /**
     * The <code>removeProbe()</code> method removes a probe from the global probe table (the probes executed
     * before and after every instruction). The comparison used is reference equality, not
     * <code>.equals()</code>.
     *
     * @param b the probe to remove
     */
    public void removeProbe(Simulator.Probe b) {
        innerLoop = false;
        globalProbe.remove(b);
    }

    /**
     * The <code>insertWatch()</code> method is used internally to insert a watch on a particular memory location.
     * @param p the watch to insert on a memory location
     * @param data_addr the address of the memory location on which to insert the watch
     */
    protected void insertWatch(Simulator.Watch p, int data_addr) {
        if (sram_watches == null)
            sram_watches = new MulticastWatch[sram.length];

        // add the probe to the multicast probe present at the location (if there is one)
        MulticastWatch w = sram_watches[data_addr];
        if (w == null) w = sram_watches[data_addr] = new MulticastWatch();
        w.add(p);
    }

    /**
     * The <code>removeWatch()</code> method is used internally to remove a watch from a particular memory location.
     * @param p the watch to remove from the memory location
     * @param data_addr the address of the memory location from which to remove the watch
     */
    protected void removeWatch(Simulator.Watch p, int data_addr) {
        if (sram_watches == null)
            return;

        // remove the probe from the multicast probe present at the location (if there is one)
        MulticastWatch w = sram_watches[data_addr];
        if (w == null) return;
        w.remove(p);
    }

    /**
     * The <code>advanceClock()</code> method advances the clock by the specified number of cycles. It SHOULD NOT
     * be used externally. It also clears the <code>cyclesConsumed</code> variable that is used to track the
     * number of cycles consumed by a single instruction.
     * @param delta the number of cycles to advance the clock
     */
    protected void advanceClock(long delta) {
        clock.advance(delta);
        cyclesConsumed = 0;
    }

    /**
     * The <code>delay()</code> method is used to add some delay cycles before the next instruction is executed.
     * This is necessary because some devices such as the EEPROM actually delay execution of instructions while
     * they are working
     * @param cycles the number of cycles to delay the execution
     */
    protected void delay(long cycles) {
        innerLoop = false;
        delayCycles += cycles;
    }

    /**
     * The <code>storeProgramMemory()</code> method is called when the program executes the SPM instruction
     * which stores to the program memory.
     */
    protected void storeProgramMemory() {
        flash.update();
    }

    /**
     * Read a general purpose register's current value as a byte.
     *
     * @param reg the register to read
     * @return the current value of the register
     */
    public byte getRegisterByte(LegacyRegister reg) {
        return sram[reg.getNumber()];
    }

    public byte getRegisterByte(int reg) {
        return sram[reg];
    }

    /**
     * Read a general purpose register's current value as an integer, without any sign extension.
     *
     * @param reg the register to read
     * @return the current unsigned value of the register
     */
    public int getRegisterUnsigned(LegacyRegister reg) {
        return sram[reg.getNumber()] & 0xff;
    }

    /**
     * The <code>getRegisterUnsigned()</code> method reads a register's value (without sign extension)
     * @param reg the index into the register file
     * @return the value of the register as an unsigned integer
     */
    public int getRegisterUnsigned(int reg) {
        return sram[reg] & 0xff;
    }

    /**
     * Read a general purpose register pair as an unsigned word. This method will read the value of the
     * specified register and the value of the next register in numerical order and return the two values
     * combined as an unsigned integer The specified register should be less than r31, because r32 (the next
     * register) does not exist.
     *
     * @param reg the low register of the pair to read
     * @return the current unsigned word value of the register pair
     */
    public int getRegisterWord(LegacyRegister reg) {
        byte low = getRegisterByte(reg);
        byte high = getRegisterByte(reg.nextRegister());
        return Arithmetic.uword(low, high);
    }

    /**
     * Read a general purpose register pair as an unsigned word. This method will read the value of the
     * specified register and the value of the next register in numerical order and return the two values
     * combined as an unsigned integer The specified register should be less than r31, because r32 (the next
     * register) does not exist.
     *
     * @param reg the low register of the pair to read
     * @return the current unsigned word value of the register pair
     */
    public int getRegisterWord(int reg) {
        byte low = getRegisterByte(reg);
        byte high = getRegisterByte(reg + 1);
        return Arithmetic.uword(low, high);
    }

    public boolean getFlag(int bit) {
        switch (bit) {
            case LegacyState.SREG_I: return I;
            case LegacyState.SREG_T: return T;
            case LegacyState.SREG_H: return H;
            case LegacyState.SREG_S: return S;
            case LegacyState.SREG_V: return V;
            case LegacyState.SREG_N: return N;
            case LegacyState.SREG_Z: return Z;
            case LegacyState.SREG_C: return C;
        }
        return false;
    }

    public void setFlag(int bit, boolean on) {
        switch (bit) {
            case LegacyState.SREG_I:
                if (on) enableInterrupts();
                else disableInterrupts();
                break;
            case LegacyState.SREG_T: T = on; break;
            case LegacyState.SREG_H: H = on; break;
            case LegacyState.SREG_S: S = on; break;
            case LegacyState.SREG_V: V = on; break;
            case LegacyState.SREG_N: N = on; break;
            case LegacyState.SREG_Z: Z = on; break;
            case LegacyState.SREG_C: C = on; break;
        }
    }

    protected void setIORegBit(int ior, int bit, boolean on) {
        byte curv = readSRAM(INSTRUMENTED, toSRAM(ior));
        curv = Arithmetic.setBit(curv, bit, on);
        writeSRAM(INSTRUMENTED, toSRAM(ior), curv);
    }

    protected boolean getIORegBit(int ior, int bit) {
        return Arithmetic.getBit(readSRAM(INSTRUMENTED, toSRAM(ior)), bit);
    }

    /**
     * The <code>getDataByte()</code> method reads a byte value from the data memory (SRAM) at the specified
     * address.
     *
     * @param address the byte address to read
     * @return the value of the data memory at the specified address
     * @throws ArrayIndexOutOfBoundsException
     *          if the specified address is not the valid memory range
     */
    public byte getDataByte(int address) {
        return readSRAM(INSTRUMENTED, address);
    }

    private byte readSRAM(boolean w, int addr) {
        if ( addr < 0 ) {
            // an error.
            return fireReadError(w, addr);
        } else if ( addr < sram.length ) {
            // a valid RAM access.
            // PERFORMANCE: consider wrapping in if(sram_watches)
            byte val;
            fireBeforeRead(w, addr);
            if ( addr < sram_start ) val = sram[addr] = readVolatile(addr);
            else val = sram[addr];
            fireAfterRead(w, addr, val);
            return val;
        } else {
            // an error.
            return fireReadError(w, addr);
        }
    }

    private void writeSRAM(boolean w, int addr, byte val) {
        if ( addr < 0 ) {
            // an error.
            fireWriteError(w, addr, val);
        } else if ( addr < sram.length ) {
            // a valid RAM access.
            // PERFORMANCE: consider wrapping in if(sram_watches)
            fireBeforeWrite(w, addr, val);
            if ( addr < sram_start ) sram[addr] = writeVolatile(addr, val);
            else sram[addr] = val;
            fireAfterWrite(w, addr, val);
        } else {
            // an error.
            fireWriteError(w, addr, val);
        }
    }

    private void fireWriteError(boolean w, int addr, byte val) {
        if ( w && error_watch != null ) error_watch.fireBeforeWrite(state, addr, val);
    }

    private byte fireReadError(boolean w, int addr) {
        if ( w && error_watch != null ) error_watch.fireBeforeRead(state, addr);
        return 0;
    }

    private static class IORegBehavior extends VolatileBehavior {
        final ActiveRegister reg;
        IORegBehavior(ActiveRegister r) {
            reg = r;
        }
        public int read(int cur) {
            return reg.read();
        }
        public int write(int cur, int nv) {
            reg.write((byte)nv);
            return nv;
        }
    }

    private class SREGBehavior extends VolatileBehavior {
        public int read(int cur) {
            int val = 0;
            if (I) val |= LegacyState.SREG_I_MASK;
            if (T) val |= LegacyState.SREG_T_MASK;
            if (H) val |= LegacyState.SREG_H_MASK;
            if (S) val |= LegacyState.SREG_S_MASK;
            if (V) val |= LegacyState.SREG_V_MASK;
            if (N) val |= LegacyState.SREG_N_MASK;
            if (Z) val |= LegacyState.SREG_Z_MASK;
            if (C) val |= LegacyState.SREG_C_MASK;
            return (byte) val;
        }
        public int write(int cur, int nv) {
            boolean enabled = (nv & LegacyState.SREG_I_MASK) != 0;
            if (enabled) enableInterrupts();
            else disableInterrupts();
            T = (nv & LegacyState.SREG_T_MASK) != 0;
            H = (nv & LegacyState.SREG_H_MASK) != 0;
            S = (nv & LegacyState.SREG_S_MASK) != 0;
            V = (nv & LegacyState.SREG_V_MASK) != 0;
            N = (nv & LegacyState.SREG_N_MASK) != 0;
            Z = (nv & LegacyState.SREG_Z_MASK) != 0;
            C = (nv & LegacyState.SREG_C_MASK) != 0;
            return nv;
        }
    }

    private byte readVolatile(int addr) {
        VolatileBehavior behavior = sram_volatile[addr];
        return (byte) behavior.read(sram[addr] & 0xff);
    }

    private byte writeVolatile(int addr, byte val) {
        VolatileBehavior behavior = sram_volatile[addr];
        return (byte) behavior.write(sram[addr] & 0xff, val & 0xff);
    }

    private void fireBeforeRead(boolean w, int addr) {
        if ( w && sram_watches != null ) {
            Simulator.Watch p = sram_watches[addr];
            if ( p != null ) p.fireBeforeRead(state, addr);
        }
    }

    private void fireAfterRead(boolean w, int addr, byte val) {
        if ( w && sram_watches != null ) {
            Simulator.Watch p = sram_watches[addr];
            if ( p != null ) p.fireAfterRead(state, addr, val);
        }
    }

    private void fireBeforeWrite(boolean w, int addr, byte val) {
        if ( w && sram_watches != null ) {
            Simulator.Watch p = sram_watches[addr];
            if ( p != null ) p.fireBeforeWrite(state, addr, val);
        }
    }

    private void fireAfterWrite(boolean w, int addr, byte val) {
        if ( w && sram_watches != null ) {
            Simulator.Watch p = sram_watches[addr];
            if ( p != null ) p.fireAfterWrite(state, addr, val);
        }
    }

    /**
     * The <code>getInstrSize()</code> method reads the size of the instruction at the given program address.
     * This is needed in the interpreter to compute the target of a skip instruction (an instruction that
     * skips over the instruction following it).
     *
     * @param npc the program address of the instruction
     * @return the size in bytes of the instruction at the specified program address
     */
    public int getInstrSize(int npc) {
        return getInstr(npc).getSize();
    }

    /**
     * The <code>getIORegisterByte()</code> method reads the value of an IO register. Invocation of this
     * method causes an invocatiobn of the <code>.read()</code> method on the corresponding internal
     * <code>IOReg</code> object, and its value returned.
     *
     * @param ioreg the IO register number
     * @return the value of the IO register
     */
    public byte getIORegisterByte(int ioreg) {
        return readSRAM(INSTRUMENTED, toSRAM(ioreg));
    }

    /**
     * The <code>getProgramByte()</code> method reads a byte value from the program (Flash) memory. The flash
     * memory generally stores read-only values and the instructions of the program. Care should be taken that
     * the program memory at the specified address does not contain an instruction. This is because, in
     * general, programs should not read instructions as data, and secondly, because no assembler is present
     * in Avrora and therefore the actual byte value of an instruction may not be known.
     *
     * @param address the byte address at which to read
     * @return the byte value of the program memory at the specified address
     * @throws InterpreterError.AddressOutOfBoundsException if the specified address is not the valid program memory range
     */
    public byte getFlashByte(int address) {
        return flash.read(address);
    }

    /**
     * The <code>writeRegisterByte()</code> method writes a value to a general purpose register. This is a
     * destructive update and should only be called from the appropriate places in the simulator.
     *
     * @param reg the register to write the value to
     * @param val the value to write to the register
     */
    protected void writeRegisterByte(LegacyRegister reg, byte val) {
        sram[reg.getNumber()] = val;
    }

    /**
     * The <code>writeRegisterWord</code> method writes a word value to a general purpose register pair. This is
     * a destructive update and should only be called from the appropriate places in the simulator. The
     * specified register and the next register in numerical order are updated with the low-order and
     * high-order byte of the value passed, respectively. The specified register should be less than r31,
     * since r32 (the next register) does not exist.
     *
     * @param reg the low register of the pair to write
     * @param val the word value to write to the register pair
     */
    protected void writeRegisterWord(LegacyRegister reg, int val) {
        byte low = Arithmetic.low(val);
        byte high = Arithmetic.high(val);
        writeRegisterByte(reg, low);
        writeRegisterByte(reg.nextRegister(), high);
    }

    /**
     * The <code>writeRegisterByte()</code> method writes a value to a general purpose register. This is a
     * destructive update and should only be called from the appropriate places in the simulator.
     *
     * @param reg the register to write the value to
     * @param val the value to write to the register
     */
    public void writeRegisterByte(int reg, byte val) {
        sram[reg] = val;
    }

    /**
     * The <code>writeRegisterWord</code> method writes a word value to a general purpose register pair. This is
     * a destructive update and should only be called from the appropriate places in the simulator. The
     * specified register and the next register in numerical order are updated with the low-order and
     * high-order byte of the value passed, respectively. The specified register should be less than r31,
     * since r32 (the next register) does not exist.
     *
     * @param reg the low register of the pair to write
     * @param val the word value to write to the register pair
     */
    public void writeRegisterWord(int reg, int val) {
        byte low = Arithmetic.low(val);
        byte high = Arithmetic.high(val);
        writeRegisterByte(reg, low);
        writeRegisterByte(reg + 1, high);
    }

    /**
     * The <code>writeDataByte()</code> method writes a value to the data memory (SRAM) of the state. This is
     * generally meant for the simulator, related classes, and device implementations to use, but could also
     * be used by debuggers and other tools.
     *
     * @param address the byte address at which to write the value
     * @param val     the value to write
     */
    public void writeDataByte(int address, byte val) {
        writeSRAM(INSTRUMENTED, address, val);
    }

    /**
     * The <code>writeFlashByte()</code> method updates the flash memory one byte at a time.
     * WARNING: this method should NOT BE CALLED UNLESS EXTREME CARE IS TAKEN. The program
     * cannot alter its own flash data except through the flash writing procedure supported
     * in <code>ReprogrammableCodeSegment</code>. This method is only meant for updating
     * node ID's that are stored in flash. DO NOT USE during execution!
     * @param address the address of the byte in flash
     * @param val the new value to write into the flash
     */
    public void writeFlashByte(int address, byte val) {
        flash.set(address, val);
    }

    /**
     * The <code>installIOReg()</code> method installs the specified <code>IOReg</code> object to the specified IO
     * register number. This method is generally only used in the simulator and in device implementations to
     * set up the state correctly during initialization.
     *
     * @param ioreg the IO register number
     * @param reg   the <code>IOReg<code> object to install
     */
    public void installIOReg(int ioreg, ActiveRegister reg) {
        sram_volatile[toSRAM(ioreg)] = new IORegBehavior(reg);
        ioregs[ioreg] = reg;
    }

    public ActiveRegister getIOReg(int ioreg) {
        return ioregs[ioreg];
    }

    private static int toSRAM(int ioreg) {
        return ioreg + NUM_REGS;
    }

    public void installVolatileBehavior(int addr, VolatileBehavior b) {
        sram_volatile[addr] = b;
    }

    /**
     * The <code>writeIORegisterByte()</code> method writes a value to the specified IO register. This is
     * generally only used internally to the simulator and device implementations, and client interfaces
     * should probably not call this method.
     *
     * @param ioreg the IO register number to which to write the value
     * @param val   the value to write to the IO register
     */
    public void writeIORegisterByte(int ioreg, byte val) {
        writeSRAM(INSTRUMENTED, toSRAM(ioreg), val);
    }

    /**
     * The <code>popByte()</code> method pops a byte from the stack by reading from the address pointed to by
     * SP+1 and incrementing the stack pointer. This method, like all of the other methods that change the
     * state, should probably only be used within the simulator. This method should not be called with an
     * empty stack, as it will cause an exception consistent with trying to read non-existent memory.
     *
     * @return the value on the top of the stack
     */
    public byte popByte() {
        int address = getSP() + 1;
        setSP(address);
        return getDataByte(address);
    }

    /**
     * The <code>pushByte()</code> method pushes a byte onto the stack by writing to the memory address
     * pointed to by the stack pointer and decrementing the stack pointer. This method, like all of the other
     * methods that change the state, should probably only be used within the simulator.
     *
     * @param val the value to push onto the stack
     */
    public void pushByte(byte val) {
        int address = getSP();
        setSP(address - 1);
        writeDataByte(address, val);
    }

    /**
     * The <code>setSP()</code> method updates the value of the stack pointer. Generally the stack pointer is
     * stored in two IO registers <code>SPL</code> and <code>SPH</code>. This method should generally only be
     * used within the simulator.
     *
     * @param val the new value of the stack pointer
     */
    public void setSP(int val) {
        SPL_reg.value = Arithmetic.low(val);
        SPH_reg.value = Arithmetic.high(val);
    }

    /**
     * This method sets the booting address of the interpreter. It should only be used before execution begins.
     *
     * @param npc the new PC to boot this interpreter from
     */
    public void setBootPC(int npc) {
        bootPC = npc;
    }

    /**
     * The <code>getInterruptBase()</code> method returns the base address of the interrupt table.
     * @return the base address of the interrupt table
     */
    public int getInterruptBase() {
        return interruptBase;
    }

    /**
     * The <code>setInterruptBase()</code> method sets the base of the interrupt table.
     * @param npc the new base of the interrupt table
     */
    public void setInterruptBase(int npc) {
        interruptBase = npc;
    }

    /**
     * The <code>getInstr()</code> can be used to retrieve a reference to the <code>LegacyInstr</code> object
     * representing the instruction at the specified program address. Care should be taken that the address in
     * program memory specified does not contain data. This is because Avrora does have a functioning
     * disassembler and assumes that the <code>LegacyInstr</code> objects for each instruction in the program are
     * known a priori.
     *
     * @param address the byte address from which to read the instruction
     * @return a reference to the <code>LegacyInstr</code> object representing the instruction at that address in
     *         the program; null if there is no instruction at the specified address
     */
    public LegacyInstr getInstr(int address) {
        return flash.readInstr(address);
    }

    /**
     * The <code>getSP()</code> method reads the current value of the stack pointer. Since the stack pointer
     * is stored in two IO registers, this method will cause the invocation of the <code>.read()</code> method
     * on each of the <code>IOReg</code> objects that store these values.
     *
     * @return the value of the stack pointer as a byte address
     */
    public int getSP() {
        byte low = SPL_reg.value;
        byte high = SPH_reg.value;
        return Arithmetic.uword(low, high);
    }

    /**
     * The <code>enableInterrupts()</code> method enables all of the interrupts.
     */
    public void enableInterrupts() {
        I = true;
        innerLoop = false;
        interrupts.enableAll();
    }

    /**
     * The <code>disableInterrupts()</code> method disables all of the interrupts.
     */
    public void disableInterrupts() {
        I = false;
        interrupts.disableAll();
    }

    /**
     * The <code>commit()</code> method is used internally to commit the results of the instructiobn just executed.
     * This should only be used internally.
     */
    protected void commit() {
        pc = nextPC;
        clock.advance(cyclesConsumed);
        cyclesConsumed = 0;
    }

}
