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

import avrora.arch.legacy.*;
import avrora.core.Program;
import avrora.sim.*;
import avrora.sim.clock.MainClock;
import avrora.sim.output.SimPrinter;
import cck.text.StringUtil;
import cck.util.Arithmetic;
import cck.util.Util;

/**
 * The <code>ReprogrammableCodeSegment</code> class represents a flash segment that stores code. This segment
 * can be updated during execution. It supports probing instructions in the flash segment and updating them.
 * This implementation uses special instructions that automatically disassemble themselves from the machine
 * code representation when they are accessed, allowing dynamic update and execution of machine code.
 *
 * @author Ben L. Titzer
 */
public class ReprogrammableCodeSegment extends CodeSegment {

    private static final double ERASE_MS_MIN = 3.7;
    private static final double WRITE_MS_MIN = 3.7;
    private static final double ERASE_MS_MAX = 4.5;
    private static final double WRITE_MS_MAX = 4.5;
    private static final int SPM_TIMEOUT = 4;

    private static final int STATE_NONE = 0;
    private static final int STATE_PGERASE = 1 << 1 | 1;
    private static final int STATE_RWWSRE  = 1 << 4 | 1;
    private static final int STATE_BLBSET  = 1 << 3 | 1;
    private static final int STATE_FILL    = 1;
    private static final int STATE_PGWRITE = 1 << 2 | 1;

    private static final int SPM_READY = 35;
    private static final int SPMCSR_LOWERBITS = 0x1f;

    private static final byte DEFAULT_VALUE = (byte)0xff;

    final SimPrinter flashPrinter;

    /**
     * The <code>ReprogrammableCodeSegment.Factory</code> class represents a class capable of creating a new
     * code segment for a new interpreter.
     */
    public static class Factory implements CodeSegment.Factory {
        final int pagesize;
        final int size;

        Factory(int size, int pagesize) {
            this.size = size;
            this.pagesize = pagesize;
        }

        public CodeSegment newCodeSegment(String name, AtmelInterpreter bi, Program p) {
            CodeSegment cs;
            if ( p != null ) {
                cs = new ReprogrammableCodeSegment(name, p.program_end, bi, pagesize);
                cs.load(p);
            } else {
                cs = new ReprogrammableCodeSegment(name, size, bi, pagesize);
            }
            return cs;
        }
    }

    /**
     * The <code>SPMCSR_reg</code> class represents an instanceof the <code>ActiveRegister</code> interface
     * that is used to represent the SPMCSR register on the ATMega family microcontrollers. This register
     * is used in reprogramming the flash memory from within the program.
     */
    private class SPMCSR_reg extends RWRegister {
        ResetEvent reset = new ResetEvent();

        public void write(byte val) {

            int lower = val & SPMCSR_LOWERBITS;
            switch ( lower ) {
                case STATE_PGERASE:
                case STATE_RWWSRE:
                case STATE_BLBSET:
                case STATE_FILL:
                case STATE_PGWRITE:
                    mainClock.removeEvent(reset);
                    mainClock.insertEvent(reset, SPM_TIMEOUT+2);
                    break;
                default:
                    lower = STATE_NONE;
            }

            this.value = (byte)(val & (~ SPMCSR_LOWERBITS) | lower);

            interpreter.setEnabled(SPM_READY, Arithmetic.getBit(value, 7));
            interpreter.setPosted(SPM_READY, !Arithmetic.getBit(value, 0));
        }

        class ResetEvent implements Simulator.Event {
            public void fire() {
                if ( flashPrinter != null )
                    flashPrinter.println("FLASH: write to SPMCSR timed out after 4 cycles");
                reset();
            }

        }

        int getState() {
            return value & SPMCSR_LOWERBITS;
        }

        void reset() {
            write((byte)(value & (~ SPMCSR_LOWERBITS)));
        }

        boolean isBusy() {
            return Arithmetic.getBit(value, 6);
        }

        void setBusy() {
            value = Arithmetic.setBit(value, 6);
        }

        void clearBusy() {
            value = Arithmetic.clearBit(value, 6);
        }
    }

    /**
     * The <code>disassembler</code> field stores a reference to a disassembler for this segment.
     * This is needed because disassemblers are currently not re-entrant.
     */
    LegacyDisassembler disassembler = new LegacyDisassembler();

    /**
     * The <code>buffer</code> field stores a reference to the bytes in the temporary page buffer
     * which is used to rewrite the flash memory.
     */
    byte[] buffer;

    /**
     * The <code>SPMCSR</code> field stores a reference to the SPMCSR register which is an IO register
     * that the program uses to select which flash operations to perform.
     */
    final SPMCSR_reg SPMCSR;

    /**
     * The <code>ERASE_CYCLES</code> field stores the number of cycles needed to complete an erase operation.
     */
    final int ERASE_CYCLES; // from hardware manual

    /**
     * The <code>WRITE_CYCLES</code> field stores the number of cycles needed to complete a write operation.
     */
    final int WRITE_CYCLES; // from hardware manual

    /**
     * The <code>pagesize</code> field stores the number of bits in the page offset field of an address; i.e.
     * it is the log of the size of a page in words.
     */
    final int pagesize;

    /**
     * The <code>addressMask</code> field stores an integer used to mask out the page offset of an address.
     */
    final int addressMask;

    /**
     * The <code>mainClock</code> method stores a reference to the main clock signal of the chip.
     */
    final MainClock mainClock;

    /**
     * The constructor for the <code>ReprogrammableCodeSegment</code> creates a new instance with the specified
     * name, with the specified size, connected to the specified microcontroller, with the given page size.
     * @param name the name of the segment as a string
     * @param size the size of the segment in bytes
     * @param bi the the interpreter the code segment is attached to
     * @param pagesize the size of the page offset field of an address into the flash
     */
    public ReprogrammableCodeSegment(String name, int size, AtmelInterpreter bi, int pagesize) {
        super(name, size, bi);
        SPMCSR = new SPMCSR_reg();
        mainClock = bi.getMainClock();
        this.pagesize = pagesize;
        this.addressMask = Arithmetic.getBitRangeMask(1, pagesize + 1);
        resetBuffer();
        MCUProperties props = bi.getSimulator().getMicrocontroller().getProperties();
        bi.installIOReg(props.getIOReg("SPMCSR"), SPMCSR);

        ERASE_CYCLES = (int)((mainClock.getHZ() * ERASE_MS_MAX / 1000));
        WRITE_CYCLES = (int)((mainClock.getHZ() * WRITE_MS_MAX / 1000));

        flashPrinter = bi.getSimulator().getPrinter("atmel.flash");
    }

    /**
     * The <code>update()</code> method is called by the interpreter when the program executes an instruction
     * that updates the program memory. For example, the SPM instruction.
     */
    public void update() {
        // TODO: check that PC is in the bootloader section
        //int pc = interpreter.getState().getPC();
        int Z = interpreter.getRegisterWord(LegacyRegister.Z);
        int pageoffset = (Z & addressMask);
        int pagenum = Z >> (pagesize + 1);
        // for models with more than 128k flash, we need to use RAMPZ
        if (interpreter.RAMPZ > 0) {
            pagenum += interpreter.getIORegisterByte(interpreter.RAMPZ) << (16 - pagesize - 1);
        }
        // do not update the ReprogrammableCodeSegment register yet
        int state = SPMCSR.getState();
        switch (state) {
            case STATE_PGERASE:
                if (flashPrinter != null) flashPrinter.println("FLASH: page erase of page " + pagenum);
                pageErase(pagenum, pageoffset);
                break;
            case STATE_RWWSRE:
                if (flashPrinter != null) flashPrinter.println("FLASH: reset RWW section ");
                resetRWW();
                break;
            case STATE_BLBSET:
                if (flashPrinter != null) flashPrinter.println("FLASH: boot lock bits set");
                mainClock.removeEvent(SPMCSR.reset);
                break;
            case STATE_FILL:
                if (flashPrinter != null) flashPrinter.println("FLASH: fill buffer @ " + pageoffset);
                fillBuffer(pagenum, pageoffset);
                break;
            case STATE_PGWRITE:
                if (flashPrinter != null) flashPrinter.println("FLASH: page write to page " + pagenum);
                pageWrite(pagenum, pageoffset);
                break;
            default:
        }
    }

    private void pageErase(int pagenum, int pageoffset) {
        mainClock.removeEvent(SPMCSR.reset);
        SPMCSR.setBusy();
        mainClock.insertEvent(new EraseEvent(pagenum), ERASE_CYCLES);
    }

    private void pageWrite(int pagenum, int pageoffset) {
        mainClock.removeEvent(SPMCSR.reset);
        SPMCSR.setBusy();
        mainClock.insertEvent(new WriteEvent(pagenum, buffer), WRITE_CYCLES);
        resetBuffer();
    }

    private void resetRWW() {
        mainClock.removeEvent(SPMCSR.reset);
        if ( !SPMCSR.isBusy() ) {
            SPMCSR.clearBusy();
            SPMCSR.reset();
        }
	    resetBuffer();
    }

    private void fillBuffer(int pagenum, int pageoffset) {
        // write the word in R0:R1 into the buffer
        byte r0 = interpreter.getRegisterByte(LegacyRegister.R0);
        byte r1 = interpreter.getRegisterByte(LegacyRegister.R1);
        SPMCSR.reset();
        buffer[pageoffset] = r0;
        buffer[pageoffset+1] = r1;
        mainClock.removeEvent(SPMCSR.reset);
    }

    /**
     * The <code>EraseEvent</code> class is used as an event to schedule the timing of erasing a page.
     * When a page erase operation is begun, this event is inserted into the queue of the simulator
     * so that when it fires, the page in the code segment is erased.
     */
    class EraseEvent implements Simulator.Event {
        int pagenum;

        EraseEvent(int pagenum) {
            this.pagenum = pagenum;
        }

        public void fire() {
            // erase the page
            if ( flashPrinter != null )
                flashPrinter.println("FLASH: page erase completed for page "+pagenum);
            int size = bufferSize();
            int addr = pagenum * size;
            for ( int offset = 0; offset < size; offset++) {
                int baddr = addr + offset;
                write(baddr, DEFAULT_VALUE);
                if ( (offset & 1) == 0)
                    replaceInstr(baddr, new DisassembleLegacyInstr(baddr));
            }
            SPMCSR.reset();
        }
    }

    /**
     * The <code>WriteEvent</code> class is used as an event to schedule the timing of writing a page.
     * When a page write operation is begun, this event is inserted into the queue of the simulator so
     * that when it fires, the page in the code segment is written with the contents of the temporary
     * buffer.
      */
    class WriteEvent implements Simulator.Event {
        int pagenum;
        byte[] buffer;

        WriteEvent(int pagenum, byte[] buf) {
            this.pagenum = pagenum;
            this.buffer = buf;
        }

        public void fire() {
            // write the page
            if ( flashPrinter != null )
                flashPrinter.println("FLASH: page write completed for page "+pagenum);
            int size = bufferSize();
            int addr = pagenum * size;
            for ( int offset = 0; offset < size; offset++) {
                int baddr = addr + offset;
                write(baddr, buffer[offset]);
                if ( (offset & 1) == 0)
                    replaceInstr(baddr, new DisassembleLegacyInstr(baddr));
            }
            SPMCSR.reset();
        }
    }

    /**
     * The <code>resetBuffer()</code> method resets the temporary buffer used for the SPM instruction
     * to its default value.
     */
    protected void resetBuffer() {
        buffer = new byte[bufferSize()];
        for ( int cntr = 0; cntr < buffer.length; cntr++) {
            buffer[cntr] = DEFAULT_VALUE;
        }
    }

    private int bufferSize() {
        // pagesize stores the number of bits representing the word offset in an address
        return 2 << pagesize;
    }

    /**
     * The <code>DisasssembleInstr</code> class represents an instruction that is used by the
     * interpreter to support dynamic code update. Whenever machine code is altered, this
     * instruction will replace the instruction(s) at that location so that when the program
     * attempts to execute the instruction, it will first be disassembled and then it will
     * be executed.
     */
    public class DisassembleLegacyInstr extends LegacyInstr {

        protected final int address;

        DisassembleLegacyInstr(int addr) {
            super(null);
            address = addr;
        }

        public void accept(LegacyInstrVisitor v) {
            LegacyInstr i = disassembler.disassembleLegacy(segment_data, 0, address);
            if ( i == null ) throw Util.failure("invalid instruction at "+ StringUtil.addrToString(address));
            replaceInstr(address, i);
            i.accept(v);
        }

        public LegacyInstr build(int address, LegacyOperand[] ops) {
            throw Util.failure("DisassembleLegacyInstr should be confined to BaseInterpreter");
        }

        public String getOperands() {
            throw Util.failure("DisassembleLegacyInstr has no operands");
        }

        public LegacyInstr asInstr() {
            LegacyInstr i = disassembler.disassembleLegacy(segment_data, 0, address);
            if ( i == null ) return null;
            replaceInstr(address, i);
            return i;
        }
    }

}
