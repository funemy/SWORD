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

package avrora.core;

import avrora.arch.*;
import avrora.arch.legacy.LegacyInstr;
import cck.text.StringUtil;
import cck.util.Util;
import java.util.*;

/**
 * The <code>Program</code> class represents a complete program of AVR instructions. It stores the actual
 * instructions and initialized data of the program.
 *
 * @author Ben L. Titzer
 * @see LegacyInstr
 * @see ControlFlowGraph
 * @see ProcedureMap
 */
public class Program {

    private final AbstractArchitecture arch;

    private final HashMap indirectEdges;

    private SourceMapping sourceMapping;

    private ControlFlowGraph cfg;

    /**
     * The <code>program_start</code> field records the lowest address in the program segment that contains
     * valid code or data.
     */
    public final int program_start;

    /**
     * The <code>program_end</code> field records the address following the highest address in the program
     * segment that contains valid code or data.
     */
    public final int program_end;

    /**
     * The <code>program_length</code> field records the size of the program (the difference between
     * <code>program_start</code> and <code>program_end</code>.
     */
    public final int program_length;

    /**
     * The <code>flash_data</code> field stores a reference to the array that contains the raw data (bytes) of the
     * program segment. NO EFFORT IS MADE IN THIS CLASS TO KEEP THIS CONSISTENT WITH THE INSTRUCTION
     * REPRESENTATIONS.
     */
    protected final byte[] flash_data;

    /**
     * The <code>flash_instrs</code> field stores a reference to the array that contains the instruction
     * representations of the program segment. NO EFFORT IS MADE IN THIS CLASS TO KEEP THIS CONSISTENT WITH
     * THE RAW DATA OF THE PROGRAM SEGMENT.
     */
    protected final AbstractInstr[] flash_instrs;

    /**
     * The constructor of the <code>Program</code> class builds an internal representation of the program that
     * is initially empty, but has the given parameters in terms of how big segments are and where they
     * start.
     *
     * @param a a reference to the <code>AbstractArchitecture</code> class that represents the
     * instruction set architecture for this program
     * @param pstart the start of the program segment
     * @param pend   the end of the program segment
     */
    public Program(AbstractArchitecture a, int pstart, int pend) {
        arch = a;
        program_start = pstart;
        program_end = pend;
        program_length = pend - pstart;

        int size = program_end - program_start;
        flash_data = new byte[size];
        flash_instrs = arch.newInstrArray(size);
        Arrays.fill(flash_data, (byte)0xff);

        indirectEdges = new HashMap();
    }

    /**
     * The <code>writeInstr()</code> method is used to write an instruction to the internal representation of
     * the program at the given address. No attempt to assemble the instruction machine code is made; thus the
     * raw data (if any) at that location in the program will not be modified.
     *
     * @param i       the instruction to write
     * @param address the byte address to write the instruction to that must be aligned on a 2-byte boundary.
     * @throws Util.InternalError if the address is not within the limits put on the program instance when
     *                              it was created.
     */
    public void writeInstr(AbstractInstr i, int address) {
        int size = i.getSize();
        checkAddress(address);
        checkAddress(address + size - 1);

        flash_instrs[address - program_start] = i;
        for (int cntr = 1; cntr < size; cntr++) {
            flash_instrs[address - program_start + cntr] = null;
        }
    }

    /**
     * The <code>readInstr()</code> method reads an instruction from the specified address in the program. No
     * attempt to disassemble raw data into usable instructions is made, and unaligned accesses will return
     * null.
     *
     * @param address the byte address in the program
     * @return the <code>LegacyInstr</code> instance at that address if that address is valid code from creation of
     *         the <code>Program</code> instance; null if the instruction is misaligned or only raw data is
     *         present at that location.
     * @throws Util.InternalError if the address is not within the limits put on the program instance when
     *                              it was created.
     */
    public AbstractInstr readInstr(int address) {
        if ( address < program_start || address >= program_end ) return null;
        return flash_instrs[address - program_start];
    }

    public AbstractInstr disassembleInstr(int address) {
        if ( address < program_start || address >= program_end ) return null;
        AbstractDisassembler d = arch.getDisassembler();
        int offset = address - program_start;
        AbstractInstr instr = d.disassemble(program_start, offset, flash_data);
        if ( instr != null ) flash_instrs[offset] = instr; 
        return instr;
    }

    /**
     * The <code>readProgramByte()</code> method reads a byte into the program segment at the specified byte
     * address. If the address overlaps with an instruction, no effort is made to get the correct encoded byte
     * of the instruction.
     *
     * @param address the program address from which to read the byte
     * @return the byte value of the program segment at that location
     */
    public byte readProgramByte(int address) {
        checkAddress(address);
        return flash_data[address - program_start];
    }

    /**
     * The <code>writeProgramByte()</code> method writes a byte into the program segment at the specified byte
     * address. If the address overlaps with an instruction, no effort is made to keep the instruction
     * representation up to date.
     *
     * @param val         the value to write
     * @param byteAddress the byte address in the program segment to write the byte value to
     */
    public void writeProgramByte(byte val, int byteAddress) {
        checkAddress(byteAddress);
        int offset = byteAddress - program_start;
        writeByteInto(val, offset);
    }

    private void writeByteInto(byte val, int offset) {
        flash_data[offset] = val;
    }

    /**
     * The <code>writeProgramBytes()</code> method writes an array of bytes into the program segment at the
     * specified byte address. If the range of addresses modified overlaps with any instructions, no effort is
     * made to keep the instruction representations up to date.
     *
     * @param val         the byte values to write
     * @param byteAddress the byte address to begin writing the values to
     */
    public void writeProgramBytes(byte[] val, int byteAddress) {
        checkAddress(byteAddress);
        checkAddress(byteAddress + val.length - 1);
        int offset = byteAddress - program_start;
        for (int cntr = 0; cntr < val.length; cntr++)
            writeByteInto(val[cntr], offset + cntr);
    }

    /**
     * The <code>checkAddress()</code> method simply checks an address against the bounds of the program and
     * throws an error if the address is not within the bounds.
     *
     * @param addr the byte address to check
     * @throws Util.InternalError if the address is not within the limits of the program segment
     */
    protected void checkAddress(int addr) {
        // TODO: throw correct error type
        if (addr < program_start || addr >= program_end)
            throw Util.failure("address out of range: " + StringUtil.addrToString(addr));
    }

    /**
     * The <code>getNextPC()</code> method computes the program counter value of the next instruction
     * following the instruction referenced by the given program counter value. Thus, it simply adds the size
     * of the instruction at the specified pc to the pc. It is useful as a commonly-used utility method.
     *
     * @param pc the program counter location of the current instruction
     * @return the program counter value of the instruction following the specified instruction in program
     *         order
     */
    public int getNextPC(int pc) {
        // TODO: better error checking
        if (pc > program_end)
            throw Util.failure("no next PC after: " + StringUtil.addrToString(pc));
        AbstractInstr i = readInstr(pc);
        if (i == null) return pc + 2;
        return pc + i.getSize();
    }

    /**
     * The <code>getIndirectEdges</code> returns a list of integers representing the possible target program
     * locations for a given callsite. This is auxilliary information that is supplied at the command line
     * which is used for a variety of analysis questions.
     *
     * @param callsite the program counter location of an indirect branch or call
     * @return a list of <code>java.lang.Integer</code> objects that represent the possible targets of the
     *         call or branch instruction
     */
    public List getIndirectEdges(int callsite) {
        return (List)indirectEdges.get(new Integer(callsite));
    }

    /**
     * The <code>addIndirectEdge</code> adds an indirect edge between a callsite and possible target. This is
     * auxilliary information that is supplied at the command line which is used for a variety of analysis
     * questions.
     *
     * @param callsite the program counter location of the call or branch instruction
     * @param target   the possible target of the call or branch instruction
     */
    public void addIndirectEdge(int callsite, int target) {
        Integer c = new Integer(callsite);
        Integer t = new Integer(target);

        List l = (List)indirectEdges.get(c);

        if (l == null) {
            l = new LinkedList();
            l.add(t);
            indirectEdges.put(c, l);
        } else {
            l.add(t);
        }

    }

    /**
     * The <code>getArchitecture()</code> method returns a reference to the <code>AbstractArchitecture</code>
     * object that represents the instruction set architecture for this program.
     * @return a reference to the abstract architecture for this program
     */
    public AbstractArchitecture getArchitecture() {
        return arch;
    }

    /**
     * The <code>getSourceMapping()</code> method returns a reference to the <code>SourceMapping</code>
     * class that contains information about mapping the machine code program back to the source code,
     * either a high-level language such as C, or a source-assembly program.
     * @return a reference to the source mapping for this program
     */
    public SourceMapping getSourceMapping() {
        return sourceMapping;
    }

    /**
     * The <code>setSourceMapping()</code> method updates the reference to the <code>SourceMapping</code>
     * for this program.
     * @param s the new source mapping for this program
     */
    public void setSourceMapping(SourceMapping s) {
        sourceMapping = s;
    }

    /**
     * The <code>getCFG()</code> method returns a reference to the control flow graph of the program. This is
     * an instance of <code>ControlFlowGraph</code> that is constructed lazily--i.e. the first time this
     * method is called. No effort is made to keep the control flow graph up to date with a changing program
     * representation; adding instructions or writing bytes into the program segment of the program will not
     * alter the CFG once it has been constructed.
     *
     * @return a reference to the <code>ControlFlowGraph</code> instance that represents the control flow
     *         graph for this program
     */
    public synchronized ControlFlowGraph getCFG() {
        if (cfg == null) {
            cfg = new CFGBuilder(this).buildCFG();
        }
        return cfg;
    }

}
