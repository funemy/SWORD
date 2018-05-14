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

import avrora.arch.legacy.LegacyInstr;
import avrora.arch.legacy.LegacyInstrVisitor;
import cck.text.StringUtil;
import cck.text.Verbose;
import cck.util.Util;

/**
 * The <code>CFGBuilder</code> class is a visitor that builds a representation of the control flow graph for a
 * given program. It uses the visitor pattern, (the <code>LegacyInstrVisitor</code> interface), to visit each
 * instruction in the program and construct and instance of the <code>ControlFlowGraph</code> class
 * representing the control flow graph of the given program.
 *
 * @author Ben L. Titzer
 */
class CFGBuilder implements LegacyInstrVisitor {

    final Program program;

    ControlFlowGraph cfg;
    InstrInfo[] info;

    private int pc;

    final Verbose.Printer printer = Verbose.getVerbosePrinter("cfg.builder");

    /**
     * The <code>CFGBuilder</code> constructor constructs an instance that is capable of creating multiple new
     * control flow graphs for the specified program.
     *
     * @param p the program
     */
    public CFGBuilder(Program p) {
        program = p;
    }

    /**
     * The <code>InstrInfo</code> class contains several flags that are tracked for each instruction
     * that are needed to build the control flow graph. On the first pass, these flags are initialized
     * for each instruction and on the second pass the basic blocks are created.
     */
    static class InstrInfo {
        boolean start;
        boolean fallthrough;
        boolean ret;
        boolean reti;
        boolean call;
        boolean indirect;
        LegacyInstr instr;
        int branchTo;

        InstrInfo() {
            start = false;
            fallthrough = true;
            branchTo = -1;
        }
    }

    /**
     * The <code>buildCFG()</code> method visits all of the machine instructions of the of the program and
     * populates the control flow graph with basic blocks. Each call to this method creates a new control flow
     * graph.
     *
     * @return an instance of the <code>ControlFlowGraph</code> class
     */
    public ControlFlowGraph buildCFG() {
        // initialize the info table
        initializeInfo();

        // first pass: discover entrances to basic blocks
        discoverEntrypoints();

        // second pass: build actual basic blocks
        buildBlocks();

        return cfg;
    }

    private void buildBlocks() {
        cfg = new ControlFlowGraph(program);
        ControlFlowGraph.Block block = cfg.newBlock(0);

        // second pass over program: create basic blocks
        for (int pc = 0; pc < program.program_end;) {
            // next block is by default this block
            ControlFlowGraph.Block nextblock = block;
            InstrInfo ii = info[pc];

            int size;

            if (ii.instr == null) {
                // invalid instruction
                size = 2;
            } else {
                // valid instruction
                size = ii.instr.getSize();
            }

            // check for any jumps into the middle of this instruction
            for (int cntr = 1; cntr < size; cntr++) {
                if (info[pc + cntr].start || info[pc + cntr].instr != null) {
                    Util.warning("misaligned branch target at " + StringUtil.addrToString(pc + cntr));
                }
            }

            // get the info of the next instruction (if there is a next instruction)
            if (pc + size < program.program_end) {
                InstrInfo in = info[pc + size];

                // check if next instruction starts a new basic block
                if (in.start) {
                    nextblock = cfg.getBlockStartingAt(pc + size);
                    if (nextblock == null)
                        nextblock = cfg.newBlock(pc + size);
                    if (ii.fallthrough)
                        cfg.addEdge(block, nextblock);
                }
            }

            // check if this instruction branches to another address
            if (ii.branchTo >= 0) {
                ControlFlowGraph.Block tblock;
                if (ii.indirect) {
                    tblock = null;
                } else {
                    tblock = cfg.getBlockStartingAt(ii.branchTo);
                    if (tblock == null) tblock = cfg.newBlock(ii.branchTo);
                }

                if (ii.call)
                    cfg.addEdge(block, tblock, "CALL");
                else
                    cfg.addEdge(block, tblock);
            }

            // check if the instruction is a return
            if (ii.ret) {
                cfg.addEdge(block, null, "RET");
            }

            // check if the instruction is a return from an interrupt
            if (ii.reti) {
                cfg.addEdge(block, null, "RETI");
            }

            // add instruction to the current block
            if (ii.instr != null)
                block.addInstr(ii.instr);
            pc += size;
            block = nextblock;
        }

        // delete the basic block info
        info = null;
    }

    private void discoverEntrypoints() {
        if (printer.enabled)
            printer.println("CFGBuilder: discovering entrypoints...");

        for (pc = 0; pc < program.program_end;) {
            if (printer.enabled)
                printer.print(StringUtil.addrToString(pc) + ": ");

            LegacyInstr i = (LegacyInstr)program.readInstr(pc);
            if (i == null) {
                if (printer.enabled)
                    printer.println("(invalid)");

                // TODO: something about invalid instructions.
                pc += 2;
            } else {
                if (printer.enabled)
                    printer.print(StringUtil.leftJustify(i.toString(), 20));
                i.accept(this);
                pc += i.getSize();
            }
        }
    }

    private void initializeInfo() {
        info = new InstrInfo[program.program_end];
        for (int cntr = 0; cntr < program.program_end; cntr++) {
            info[cntr] = new InstrInfo();
        }
        info[0].start = true;
    }

    private void enter(int byteAddress) {
        if (byteAddress < 0 || byteAddress >= program.program_end) {
            return;
        }
        info[byteAddress].start = true;
    }

    private void add(LegacyInstr i) {
        info[pc].instr = i;
        if (printer.enabled)
            printer.println("    -> add");
    }

    private void branch(LegacyInstr i, int byteAddress) {
        info[pc].instr = i;
        info[pc].branchTo = byteAddress;
        if (printer.enabled)
            printer.println("    -> branch to " + StringUtil.addrToString(byteAddress));

        enter(pc + i.getSize());
        enter(byteAddress);
    }

    private void call(LegacyInstr i, int byteAddress) {
        info[pc].call = true;
        info[pc].instr = i;
        info[pc].branchTo = byteAddress;
        if (printer.enabled)
            printer.println("    -> call to " + StringUtil.addrToString(byteAddress));

        enter(pc + i.getSize());
        enter(byteAddress);
    }

    private void end(LegacyInstr i) {
        info[pc].instr = i;
        info[pc].fallthrough = false;
        if (printer.enabled)
            printer.println("    -> end");
        enter(pc + i.getSize());
    }

    private void jump(LegacyInstr i, int byteAddress) {
        info[pc].instr = i;
        info[pc].fallthrough = false;
        info[pc].branchTo = byteAddress;
        if (printer.enabled)
            printer.println("    -> jump to " + StringUtil.addrToString(byteAddress));
        enter(pc + i.getSize());
        enter(byteAddress);
    }

    private int relative(int i) {
        return i * 2 + 2 + pc;
    }

    private int absolute(int i) {
        return i * 2;
    }

    private void skip(LegacyInstr i) {
        int npc = pc + i.getSize();
        LegacyInstr next = (LegacyInstr)program.readInstr(npc);
        branch(i, npc + next.getSize());
    }


    public void visit(LegacyInstr.ADC i) { // add register to register with carry
        add(i);
    }

    public void visit(LegacyInstr.ADD i) { // add register to register
        add(i);
    }

    public void visit(LegacyInstr.ADIW i) { // add immediate to word register
        add(i);
    }

    public void visit(LegacyInstr.AND i) { // and register with register
        add(i);
    }

    public void visit(LegacyInstr.ANDI i) { // and register with immediate
        add(i);
    }

    public void visit(LegacyInstr.ASR i) { // arithmetic shift right
        add(i);
    }

    public void visit(LegacyInstr.BCLR i) { // clear bit in status register
        add(i);
    }

    public void visit(LegacyInstr.BLD i) { // load bit from T flag into register
        add(i);
    }

    public void visit(LegacyInstr.BRBC i) { // branch if bit in status register is clear
        branch(i, relative(i.imm2));
    }

    public void visit(LegacyInstr.BRBS i) { // branch if bit in status register is set
        branch(i, relative(i.imm2));
    }

    public void visit(LegacyInstr.BRCC i) { // branch if carry flag is clear
        branch(i, relative(i.imm1));
    }

    public void visit(LegacyInstr.BRCS i) { // branch if carry flag is set
        branch(i, relative(i.imm1));
    }

    public void visit(LegacyInstr.BREAK i) { // break
        end(i);
    }

    public void visit(LegacyInstr.BREQ i) { // branch if equal
        branch(i, relative(i.imm1));
    }

    public void visit(LegacyInstr.BRGE i) { // branch if greater or equal (signed)
        branch(i, relative(i.imm1));
    }

    public void visit(LegacyInstr.BRHC i) { // branch if H flag is clear
        branch(i, relative(i.imm1));
    }

    public void visit(LegacyInstr.BRHS i) { // branch if H flag is set
        branch(i, relative(i.imm1));
    }

    public void visit(LegacyInstr.BRID i) { // branch if interrupts are disabled
        branch(i, relative(i.imm1));
    }

    public void visit(LegacyInstr.BRIE i) { // branch if interrupts are enabled
        branch(i, relative(i.imm1));
    }

    public void visit(LegacyInstr.BRLO i) { // branch if lower
        branch(i, relative(i.imm1));
    }

    public void visit(LegacyInstr.BRLT i) { // branch if less than zero (signed)
        branch(i, relative(i.imm1));
    }

    public void visit(LegacyInstr.BRMI i) { // branch if minus
        branch(i, relative(i.imm1));
    }

    public void visit(LegacyInstr.BRNE i) { // branch if not equal
        branch(i, relative(i.imm1));
    }

    public void visit(LegacyInstr.BRPL i) { // branch if positive
        branch(i, relative(i.imm1));
    }

    public void visit(LegacyInstr.BRSH i) { // branch if same or higher
        branch(i, relative(i.imm1));
    }

    public void visit(LegacyInstr.BRTC i) { // branch if T flag is clear
        branch(i, relative(i.imm1));
    }

    public void visit(LegacyInstr.BRTS i) { // branch if T flag is set
        branch(i, relative(i.imm1));
    }

    public void visit(LegacyInstr.BRVC i) { // branch if V flag is clear
        branch(i, relative(i.imm1));
    }

    public void visit(LegacyInstr.BRVS i) { // branch if V flag is set
        branch(i, relative(i.imm1));
    }

    public void visit(LegacyInstr.BSET i) { // set flag in status register
        add(i);
    }

    public void visit(LegacyInstr.BST i) { // store bit in register into T flag
        add(i);
    }

    public void visit(LegacyInstr.CALL i) { // call absolute address
        call(i, absolute(i.imm1));
    }

    public void visit(LegacyInstr.CBI i) { // clear bit in IO register
        add(i);
    }

    public void visit(LegacyInstr.CBR i) { // clear bits in register
        add(i);
    }

    public void visit(LegacyInstr.CLC i) { // clear C flag
        add(i);
    }

    public void visit(LegacyInstr.CLH i) { // clear H flag
        add(i);
    }

    public void visit(LegacyInstr.CLI i) { // clear I flag
        add(i);
    }

    public void visit(LegacyInstr.CLN i) { // clear N flag
        add(i);
    }

    public void visit(LegacyInstr.CLR i) { // clear register (set to zero)
        add(i);
    }

    public void visit(LegacyInstr.CLS i) { // clear S flag
        add(i);
    }

    public void visit(LegacyInstr.CLT i) { // clear T flag
        add(i);
    }

    public void visit(LegacyInstr.CLV i) { // clear V flag
        add(i);
    }

    public void visit(LegacyInstr.CLZ i) { // clear Z flag
        add(i);
    }

    public void visit(LegacyInstr.COM i) { // one's compliment register
        add(i);
    }

    public void visit(LegacyInstr.CP i) { // compare registers
        add(i);
    }

    public void visit(LegacyInstr.CPC i) { // compare registers with carry
        add(i);
    }

    public void visit(LegacyInstr.CPI i) { // compare register with immediate
        add(i);
    }

    public void visit(LegacyInstr.CPSE i) { // compare registers and skip if equal
        add(i);
    }

    public void visit(LegacyInstr.DEC i) { // decrement register by one
        add(i);
    }

    public void visit(LegacyInstr.EICALL i) { // extended indirect call
        info[pc].indirect = true;
        call(i, 0);
    }

    public void visit(LegacyInstr.EIJMP i) { // extended indirect jump
        info[pc].indirect = true;
        branch(i, 0);
    }

    public void visit(LegacyInstr.ELPM i) { // extended load program memory to r0
        add(i);
    }

    public void visit(LegacyInstr.ELPMD i) { // extended load program memory to register
        add(i);
    }

    public void visit(LegacyInstr.ELPMPI i) { // extended load program memory to register and post-increment
        add(i);
    }

    public void visit(LegacyInstr.EOR i) { // exclusive or register with register
        add(i);
    }

    public void visit(LegacyInstr.FMUL i) { // fractional multiply register with register to r0
        add(i);
    }

    public void visit(LegacyInstr.FMULS i) { // signed fractional multiply register with register to r0
        add(i);
    }

    public void visit(LegacyInstr.FMULSU i) { // signed/unsigned fractional multiply register with register to r0
        add(i);
    }

    public void visit(LegacyInstr.ICALL i) { // indirect call through Z register
        info[pc].indirect = true;
        call(i, 0);
    }

    public void visit(LegacyInstr.IJMP i) { // indirect jump through Z register
        info[pc].indirect = true;
        info[pc].branchTo = 0;
        end(i);
    }

    public void visit(LegacyInstr.IN i) { // read from IO register into register
        add(i);
    }

    public void visit(LegacyInstr.INC i) { // increment register by one
        add(i);
    }

    public void visit(LegacyInstr.JMP i) { // absolute jump
        jump(i, absolute(i.imm1));
    }

    public void visit(LegacyInstr.LD i) { // load from SRAM
        add(i);
    }

    public void visit(LegacyInstr.LDD i) { // load from SRAM with displacement
        add(i);
    }

    public void visit(LegacyInstr.LDI i) { // load immediate into register
        add(i);
    }

    public void visit(LegacyInstr.LDPD i) { // load from SRAM with pre-decrement
        add(i);
    }

    public void visit(LegacyInstr.LDPI i) { // load from SRAM with post-increment
        add(i);
    }

    public void visit(LegacyInstr.LDS i) { // load direct from SRAM
        add(i);
    }

    public void visit(LegacyInstr.LPM i) { // load program memory into r0
        add(i);
    }

    public void visit(LegacyInstr.LPMD i) { // load program memory into register
        add(i);
    }

    public void visit(LegacyInstr.LPMPI i) { // load program memory into register and post-increment
        add(i);
    }

    public void visit(LegacyInstr.LSL i) { // logical shift left
        add(i);
    }

    public void visit(LegacyInstr.LSR i) { // logical shift right
        add(i);
    }

    public void visit(LegacyInstr.MOV i) { // copy register to register
        add(i);
    }

    public void visit(LegacyInstr.MOVW i) { // copy two registers to two registers
        add(i);
    }

    public void visit(LegacyInstr.MUL i) { // multiply register with register to r0
        add(i);
    }

    public void visit(LegacyInstr.MULS i) { // signed multiply register with register to r0
        add(i);
    }

    public void visit(LegacyInstr.MULSU i) { // signed/unsigned multiply register with register to r0
        add(i);
    }

    public void visit(LegacyInstr.NEG i) { // two's complement register
        add(i);
    }

    public void visit(LegacyInstr.NOP i) { // do nothing operation
        add(i);
    }

    public void visit(LegacyInstr.OR i) { // or register with register
        add(i);
    }

    public void visit(LegacyInstr.ORI i) { // or register with immediate
        add(i);
    }

    public void visit(LegacyInstr.OUT i) { // write from register to IO register
        add(i);
    }

    public void visit(LegacyInstr.POP i) { // pop from the stack to register
        add(i);
    }

    public void visit(LegacyInstr.PUSH i) { // push register to the stack
        add(i);
    }

    public void visit(LegacyInstr.RCALL i) { // relative call
        call(i, relative(i.imm1));
    }

    public void visit(LegacyInstr.RET i) { // return to caller
        info[pc].ret = true;
        end(i);
    }

    public void visit(LegacyInstr.RETI i) { // return from interrupt
        info[pc].reti = true;
        end(i);
    }

    public void visit(LegacyInstr.RJMP i) { // relative jump
        jump(i, relative(i.imm1));
    }

    public void visit(LegacyInstr.ROL i) { // rotate left through carry flag
        add(i);
    }

    public void visit(LegacyInstr.ROR i) { // rotate right through carry flag
        add(i);
    }

    public void visit(LegacyInstr.SBC i) { // subtract register from register with carry
        add(i);
    }

    public void visit(LegacyInstr.SBCI i) { // subtract immediate from register with carry
        add(i);
    }

    public void visit(LegacyInstr.SBI i) { // set bit in IO register
        add(i);
    }

    public void visit(LegacyInstr.SBIC i) { // skip if bit in IO register is clear
        skip(i);
    }

    public void visit(LegacyInstr.SBIS i) { // skip if bit in IO register is set
        skip(i);
    }

    public void visit(LegacyInstr.SBIW i) { // subtract immediate from word
        add(i);
    }

    public void visit(LegacyInstr.SBR i) { // set bits in register
        add(i);
    }

    public void visit(LegacyInstr.SBRC i) { // skip if bit in register cleared
        skip(i);
    }

    public void visit(LegacyInstr.SBRS i) { // skip if bit in register set
        skip(i);
    }

    public void visit(LegacyInstr.SEC i) { // set C (carry) flag
        add(i);
    }

    public void visit(LegacyInstr.SEH i) { // set H (half carry) flag
        add(i);
    }

    public void visit(LegacyInstr.SEI i) { // set I (interrupt enable) flag
        add(i);
    }

    public void visit(LegacyInstr.SEN i) { // set N (negative) flag
        add(i);
    }

    public void visit(LegacyInstr.SER i) { // set bits in register
        add(i);
    }

    public void visit(LegacyInstr.SES i) { // set S (signed) flag
        add(i);
    }

    public void visit(LegacyInstr.SET i) { // set T flag
        add(i);
    }

    public void visit(LegacyInstr.SEV i) { // set V (overflow) flag
        add(i);
    }

    public void visit(LegacyInstr.SEZ i) { // set Z (zero) flag
        add(i);
    }

    public void visit(LegacyInstr.SLEEP i) { // invoke sleep mode
        add(i);
    }

    public void visit(LegacyInstr.SPM i) { // store to program memory from r0
        add(i);
    }

    public void visit(LegacyInstr.ST i) { // store from register to SRAM
        add(i);
    }

    public void visit(LegacyInstr.STD i) { // store from register to SRAM with displacement
        add(i);
    }

    public void visit(LegacyInstr.STPD i) { // store from register to SRAM with pre-decrement
        add(i);
    }

    public void visit(LegacyInstr.STPI i) { // store from register to SRAM with post-increment
        add(i);
    }

    public void visit(LegacyInstr.STS i) { // store direct to SRAM
        add(i);
    }

    public void visit(LegacyInstr.SUB i) { // subtract register from register
        add(i);
    }

    public void visit(LegacyInstr.SUBI i) { // subtract immediate from register
        add(i);
    }

    public void visit(LegacyInstr.SWAP i) { // swap nibbles in register
        add(i);
    }

    public void visit(LegacyInstr.TST i) { // test for zero or minus
        add(i);
    }

    public void visit(LegacyInstr.WDR i) { // watchdog timer reset
        add(i);
    }

}
