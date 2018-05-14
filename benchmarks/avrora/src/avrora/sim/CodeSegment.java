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

import avrora.arch.legacy.*;
import avrora.core.Program;
import avrora.sim.util.MulticastProbe;
import cck.util.Util;
import java.util.Arrays;

/**
 * The <code>CodeSegment</code> class represents a segment of memory that stores executable
 * code. The program memory on the AVR chips, for example, is a flash segment.
 *
 * @author Ben L. Titzer
 */
public class CodeSegment extends Segment {

    protected final AtmelInterpreter interpreter;

    /**
     * The <code>replaceInstr()</code> method is used internally to update an instruction in the flash segment
     * without losing all of its attached instrumentation (i.e. probes and watches).
     * @param address the address in the code segment of the instruction
     * @param i the new instruction to place at this location in the flash
     */
    protected void replaceInstr(int address, LegacyInstr i) {
        LegacyInstr instr = getInstr(address);
        if ( instr == null || !(instr instanceof ProbedLegacyInstr) ) {
            writeInstr(address, i);
        } else {
            ProbedLegacyInstr pi = new ProbedLegacyInstr(i, (ProbedLegacyInstr)instr);
            writeInstr(address, pi);
        }
    }

    /**
     * The <code>CodeSegment.Factory</code> class is used to create a new code segment for a new interpreter.
     */
    public interface Factory {
        public CodeSegment newCodeSegment(String name, AtmelInterpreter bi, Program p);
    }

    /**
     * The <code>DefaultFactory</code> class represents a factory capable of creating a simple code segment
     * that is not reprogrammable.
     */
    public static class DefaultFactory implements Factory {
        final int size;

        public DefaultFactory(int s) {
            size = s;
        }

        public CodeSegment newCodeSegment(String name, AtmelInterpreter bi, Program p) {
            return new CodeSegment(name, size, bi);
        }
    }

    /**
     * The <code>DEFAULT_VALUE</code> field stores the default value that is used to
     * initialize the flash memory.
     */
    public static final byte DEFAULT_VALUE = (byte)0xff;

    /**
     * The <code>segment_instr</code> field stores a reference to an array that contains the
     * disassembled instructions that correspond to the machine code.
     */
    protected LegacyInstr[] segment_instr;

    protected final NoLegacyInstr NO_INSTR = new NoLegacyInstr();

    protected CodeSharer codeSharer;

    /**
     * The <code>CodeSharer</code> interface is used to allow sharing of the underlying array
     * representing the code. Only the interpreter should share this representation. If there
     * is an update to this array (e.g. it must be grown), the sharer will be notified to
     * update its reference.
     */
    public interface CodeSharer {
        public void update(LegacyInstr[] segment);
    }

    /**
     * The constructor for the <code>CodeSegment</code> class creates a new code segment, complete
     * with binary and instruction representations.
     * @param name the name of the segment as a string
     * @param size the size of the segment in bytes
     * @param bi the interpreter that will use this segment
     */
    public CodeSegment(String name, int size, AtmelInterpreter bi) {
        super(name, size, DEFAULT_VALUE, bi.state);
        interpreter = bi;
        segment_instr = new LegacyInstr[size];
    }

    /**
     * The <code>update()</code> method is called when the program attempts an update
     * to the flash memory through the SPM instruction. This is only supported on some
     * flash memory types.
     */
    public void update() {
        throw Util.failure("Update of flash memory not supported for this segment");
    }

    public LegacyInstr[] shareCode(CodeSharer s) {
        codeSharer = s;
        return segment_instr;
    }

    /**
     * The <code>load()</code> method loads a program into the flash memory, writing the
     * binary machine code and the disassembled instructions. This should only be done
     * once for a <code>FlashMemory</code> object, before any instrumentation is added.
     * @param p the program to be loaded into the flash memory
     */
    public void load(Program p) {
        Arrays.fill(segment_instr, NO_INSTR);
        for (int cntr = p.program_start; cntr < p.program_end;) {
            LegacyInstr i = (LegacyInstr)p.readInstr(cntr);
            if (i != null) {
                segment_instr[cntr] = i;
                cntr += i.getSize();
            } else {
                cntr += 2;
            }
        }

        // now initialize the flash data
        for (int cntr = p.program_start; cntr < p.program_end; cntr++)
            segment_data[cntr] = p.readProgramByte(cntr);
    }

    /**
     * The <code>readInstr()</code> method reads an LegacyInstr from the specified address in
     * the flash. For misaligned accesses and accesses to places in the flash where there
     * are no valid instructions, this method returns <code>null</code>. This method does
     * not return <code>ProbedLegacyInstr</code> or <code>DisassembleLegacyInstr</code> objects which
     * are used internally to accomplish probing and dynamically updated code.
     *
     * @param address the address in the flash from which to read the instruction
     * @return a reference to the <code>LegacyInstr</code> object at this address in the flash;
     * <code>null</code> if there is no instruction at this address.
     */
    public LegacyInstr readInstr(int address) {
        try {
            return segment_instr[address].asInstr();
        } catch ( ArrayIndexOutOfBoundsException e) {
            throw new AddressOutOfBoundsException(address);
        }
    }

    /**
     * The <code>getInstr()</code> method reads an LegacyInstr from the specified address in
     * the flash. For misaligned accesses and accesses to places in the flash where there
     * are no valid instructions, this method may return instances of <code>MisalignedLegacyInstr</code>
     * or <code>DisassembleLegacyInstr</code> objects which are used internally to check for errors
     * and support dynamically updated code. Additionally, this method may return instances of
     * <code>ProbedLegacyInstr</code> that are used internally to support probing of instructions.
     *
     * @param address the address in the flash from which to read the instruction
     * @return a reference to the <code>LegacyInstr</code> object at this address in the flash;
     * <code>null</code> if there is no instruction at this address.
     */
    public LegacyInstr getInstr(int address) {
        try {
            return segment_instr[address];
        } catch ( ArrayIndexOutOfBoundsException e) {
            throw new AddressOutOfBoundsException(address);
        }
    }

    /**
     * The <code>insertProbe()</code> method inserts a probe on an instruction at the
     * specified address. No equality testing is done; i.e. inserting the same probe
     * on the same address more than once will result in it being triggered more than
     * once when the instruction executes.
     * @param address the address of the instruction on which to insert the probe
     * @param p the probe to insert on this instruction
     */
    public void insertProbe(int address, Simulator.Probe p) {
        LegacyInstr instr = getInstr(address);
        if ( instr instanceof ProbedLegacyInstr ) {
            ProbedLegacyInstr pri = (ProbedLegacyInstr)instr;
            pri.add(p);
        } else {
            ProbedLegacyInstr pri = new ProbedLegacyInstr(instr, address);
            pri.add(p);
            writeInstr(address, pri);
        }
    }

    /**
     * The <code>removeProbe()</code> method removes a probe from a particular instruction
     * in the program. The probe will no longer be triggered for subsequent executions of
     * this instruction. Reference equality is used for testing whether the probe should be
     * removed; i.e. the <code>Object.equals()</code> method is not consulted.
     * @param address the address of the instructiobn on which to insert the probe
     * @param p the probe to isnert on this instruction
     */
    public void removeProbe(int address, Simulator.Probe p) {
        LegacyInstr instr = getInstr(address);
        if ( instr instanceof ProbedLegacyInstr ) {
            ProbedLegacyInstr pri = (ProbedLegacyInstr)instr;
            pri.remove(p);
        }
    }

    protected void writeInstr(int address, LegacyInstr i) {
        segment_instr[address] = i;
    }

    /**
     * The ProbedLegacyInstr class represents a wrapper around an instruction in the program that executes the
     * probes before executing the instruction and after the instruction. For most methods on the
     * <code>LegacyInstr</code> class, it simply forwards the call to the original instruction.
     */
    protected class ProbedLegacyInstr extends LegacyInstr {
        protected final int address;
        protected final LegacyInstr instr;
        protected final MulticastProbe probe;

        public ProbedLegacyInstr(LegacyInstr i, int a) {
            super(new LegacyInstrProperties(i.properties.name, i.properties.variant, i.properties.size, 0));
            instr = i;
            address = a;
            probe = new MulticastProbe();
        }

        public ProbedLegacyInstr(LegacyInstr i, ProbedLegacyInstr prev) {
            super(new LegacyInstrProperties(i.properties.name, i.properties.variant, i.properties.size, 0));
            instr = i;
            address = prev.address;
            probe = prev.probe;
        }

        void add(Simulator.Probe p) {
            probe.add(p);
        }

        void remove(Simulator.Probe p) {
            probe.remove(p);
        }

        boolean isEmpty() {
            return probe.isEmpty();
        }

        public void accept(LegacyInstrVisitor v) {
            probe.fireBefore(interpreter.state, address);
            instr.accept(interpreter);
            interpreter.commit();
            probe.fireAfter(interpreter.state, address);

            if ( probe.isEmpty() ) {
                // if the probed instruction has no more probes, remove it altogether
                writeInstr(address, instr);
            }
        }

        public LegacyInstr build(int address, LegacyOperand[] ops) {
            throw Util.failure("ProbedLegacyInstr should be confined to BaseInterpreter");
        }

        public String getOperands() {
            return instr.getOperands();
        }

        public LegacyInstr asInstr() {
            return instr;
        }
    }

    static LegacyInstrProperties NO_INSTR_PROPS = new LegacyInstrProperties("<none>", "<none>", 2, 1);

    private class NoLegacyInstr extends LegacyInstr {

        NoLegacyInstr() {
            super(NO_INSTR_PROPS);
        }

        /**
         * The <code>getOperands()</code> method returns a string representation of the operands of the
         * instruction. This is useful for printing and tracing of instructions as well as generating
         * listings.
         *
         * @return a string representing the operands of the instruction
         */
        public String getOperands() {
            throw Util.failure("no instruction here");
        }

        /**
         * The <code>accept()</code> method is part of the visitor pattern for instructions. The visitor
         * pattern uses two virtual dispatches combined with memory overloading to achieve dispatching on
         * multiple types. The result is clean and modular code.
         *
         * @param v the visitor to accept
         */
        public void accept(LegacyInstrVisitor v) {
            throw new InterpreterError.NoSuchInstructionException(interpreter.getState().getPC());
        }

        /**
         * The <code>build()</code> method constructs a new <code>LegacyInstr</code> instance with the given
         * operands, checking the operands against the constraints that are specific to each instruction.
         *
         * @param pc  the address at which the instruction will be located
         * @param ops the operands to the instruction
         * @return a new <code>LegacyInstr</code> instance representing the instruction with the given operands
         */
        public LegacyInstr build(int pc, LegacyOperand[] ops) {
            throw Util.failure("no instruction here");
        }

        public LegacyInstr asInstr() {
            return null;
        }
    }

}
