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

package avrora.stack.isea;

import avrora.arch.legacy.*;
import avrora.core.Program;
import cck.text.*;
import cck.util.Util;
import java.util.*;

/**
 * The <code>ISEInterpreter</code> class implements an abstract interpreter for intraprocedural
 * side effect analysis. This abstract interpreter simply keeps tracks of which register values
 * have been written, read, or overwritten, as well as the values on the stack.
 *
 * @author Ben L. Titzer
 */
public class ISEInterpreter implements LegacyInstrVisitor {

    protected final Verbose.Printer printer = Verbose.getVerbosePrinter("analysis.isea.interpreter");

    public interface SummaryCache {
        public ISEState getProcedureSummary(int start);
        public void recordReturnSummary(int retaddr, ISEState rs);
    }

    protected final Program program;

    protected byte readRegister(LegacyRegister r) {
        return state.readRegister(r);
    }

    protected byte getRegister(LegacyRegister r) {
        return state.getRegister(r);
    }

    protected void writeRegister(LegacyRegister r, byte val) {
        state.writeRegister(r, val);
    }

    protected byte getSREG() {
        return state.getSREG();
    }

    protected void writeSREG(byte val) {
        state.writeSREG(val);
    }

    protected byte readIORegister(int ioreg) {
        return state.readIORegister(ioreg);
    }

    protected void writeIORegister(int ioreg, byte val) {
        state.writeIORegister(ioreg, val);
    }

    protected int relative(int offset) {
        return pc + 2 + 2 * offset;
    }

    protected int absolute(int offset) {
        return 2 * offset;
    }
    protected void branch(int addr) {
        addToWorkList("BRANCH", addr, state.dup());
    }

    protected void jump(int addr) {
        nextPC = addr;
    }

    protected ISEState processReturnState(ISEState caller, ISEState ret) {
        ISEState fs = ret.dup();
        fs.mergeWithCaller(caller);
        return fs;
    }

    protected void postReturn(ISEState state) {
        if ( returnState == null )
            returnState = state.dup();
        else returnState.merge(state);
        cache.recordReturnSummary(nextPC, state);
    }

    protected void postReturnFromInterrupt(ISEState state) {
        // TODO: deal with interrupts
        if ( returnState == null )
            returnState = state.dup();
        else returnState.merge(state);
        cache.recordReturnSummary(nextPC, state);
    }

    protected void skip() {
        branch(program.getNextPC(nextPC));
    }

    protected byte popByte() {
        return state.pop();
    }

    void pushByte(byte val) {
        state.push(val);
    }

    class Item {
        final int pc;
        final ISEState state;
        Item next;

        Item(int pc, ISEState s) {
            this.pc = pc;
            this.state = s;
        }
    }

    protected int pc;
    protected int nextPC;
    protected ISEState state;
    protected ISEState returnState;
    Item head;
    Item tail;

    protected HashMap states;
    protected final SummaryCache cache;

    protected void addToWorkList(String str, int pc, ISEState s) {
        printAdd(str, s, pc);
        s = mergeState(pc, s);

        if ( s == null ) return;

        Item i = new Item(pc, s);
        if ( head == null ) {
            head = tail = i;
        } else {
            tail.next = i;
            tail = i;
        }
    }

    private void printAdd(String str, ISEState s, int pc) {
        if ( !printer.enabled ) return;
        Terminal.printBrightGreen(str);
        Terminal.println(":");
        printState(s, pc);
    }

    private void printState(ISEState s, int pc) {
        if ( !printer.enabled ) return;
        s.print(pc);
    }

    private ISEState mergeState(int pc, ISEState s) {
        ISEState es = (ISEState)states.get(new Integer(pc));
        if ( es != null ) {
            if ( es.equals(s) ) {
                printSeen();
                return null;
            } else {
                printMerge(es, pc);
                es.merge(s);
                s = es;
                printState(s, pc);
            }
        } else {
            states.put(new Integer(pc), s);
        }
        return s;
    }

    private void printMerge(ISEState es, int pc) {
        if ( !printer.enabled ) return;
        Terminal.printRed("MERGE WITH");
        Terminal.nextln();
        printState(es, pc);
        Terminal.printRed("RESULT");
        Terminal.nextln();
    }

    private void printSeen() {
        if ( !printer.enabled ) return;
        Terminal.printRed("SEEN");
        Terminal.nextln();
    }

    public ISEInterpreter(Program p, SummaryCache cs) {
        program = p;
        states = new HashMap();
        cache = cs;
    }

    public ISEState analyze(int begin_addr) {
        addToWorkList("START", begin_addr, new ISEState());
        run();
        return returnState;
    }

    protected void run() {
        // run the worklist to completion.
        while ( head != null ) {
            Item i = head;
            head = head.next;

            pc = i.pc;
            state = i.state.dup();
            LegacyInstr instr = (LegacyInstr)program.readInstr(i.pc);
            printItem(instr);
            int npc = program.getNextPC(i.pc);
            nextPC = npc;
            instr.accept(this);
            if ( nextPC >= 0 ) {
                String str = npc == nextPC ? "FALLTHROUGH" : "JUMP";
                addToWorkList(str, nextPC, state);

            }
        }
    }

    private void printItem(LegacyInstr instr) {
        if ( !printer.enabled ) return;
        TermUtil.printSeparator();
        printState(state, pc);
        Terminal.printBrightBlue(instr.getVariant());
        Terminal.println(" "+instr.getOperands());
        TermUtil.printThinSeparator(Terminal.MAXLINE);
    }

    private void mult(LegacyRegister r1, LegacyRegister r2) {
        readRegister(r1);
        readRegister(r2);
        writeSREG(ISEValue.UNKNOWN);
        writeRegister(LegacyRegister.R0, ISEValue.UNKNOWN);
        writeRegister(LegacyRegister.R1, ISEValue.UNKNOWN);
    }


    void binop(LegacyRegister r1, LegacyRegister r2) {
        readRegister(r1);
        readRegister(r2);
        writeSREG(ISEValue.UNKNOWN);
        writeRegister(r1, ISEValue.UNKNOWN);
    }

    void unop(LegacyRegister r1) {
        readRegister(r1);
        writeSREG(ISEValue.UNKNOWN);
        writeRegister(r1, ISEValue.UNKNOWN);
    }

    public void visit(LegacyInstr.ADC i) {
        binop(i.r1, i.r2);
    }

    public void visit(LegacyInstr.ADD i) {
        binop(i.r1, i.r2);
    }

    public void visit(LegacyInstr.ADIW i) {
        readRegister(i.r1);
        readRegister(i.r1.nextRegister());
        writeSREG(ISEValue.UNKNOWN);
        writeRegister(i.r1, ISEValue.UNKNOWN);
        writeRegister(i.r1.nextRegister(), ISEValue.UNKNOWN);
    }

    public void visit(LegacyInstr.AND i) {
        binop(i.r1, i.r2);
    }

    public void visit(LegacyInstr.ANDI i) {
        unop(i.r1);
    }

    public void visit(LegacyInstr.ASR i) {
        unop(i.r1);
    }

    public void visit(LegacyInstr.BCLR i) {
        writeIORegister(i.imm1, ISEValue.UNKNOWN);
        //getIOReg(SREG).writeBit(i.imm1, false);
    }

    public void visit(LegacyInstr.BLD i) {
        readRegister(i.r1);
        writeRegister(i.r1, ISEValue.UNKNOWN);
        //writeRegister(i.r1, Arithmetic.setBit(readRegister(i.r1), i.imm1, T));
    }

    public void visit(LegacyInstr.BRBC i) {
        branch(relative(i.imm1));
    }

    public void visit(LegacyInstr.BRBS i) {
        branch(relative(i.imm1));
    }

    public void visit(LegacyInstr.BRCC i) {
        branch(relative(i.imm1));
    }

    public void visit(LegacyInstr.BRCS i) {
        branch(relative(i.imm1));
    }

    public void visit(LegacyInstr.BREAK i) {
        end();
    }

    public void visit(LegacyInstr.BREQ i) {
        branch(relative(i.imm1));
    }

    public void visit(LegacyInstr.BRGE i) {
        branch(relative(i.imm1));
    }

    public void visit(LegacyInstr.BRHC i) {
        branch(relative(i.imm1));
    }

    public void visit(LegacyInstr.BRHS i) {
        branch(relative(i.imm1));
    }

    public void visit(LegacyInstr.BRID i) {
        branch(relative(i.imm1));
    }

    public void visit(LegacyInstr.BRIE i) {
        branch(relative(i.imm1));
    }

    public void visit(LegacyInstr.BRLO i) {
        branch(relative(i.imm1));
    }

    public void visit(LegacyInstr.BRLT i) {
        branch(relative(i.imm1));
    }

    public void visit(LegacyInstr.BRMI i) {
        branch(relative(i.imm1));
    }

    public void visit(LegacyInstr.BRNE i) {
        branch(relative(i.imm1));
    }

    public void visit(LegacyInstr.BRPL i) {
        branch(relative(i.imm1));
    }

    public void visit(LegacyInstr.BRSH i) {
        branch(relative(i.imm1));
    }

    public void visit(LegacyInstr.BRTC i) {
        branch(relative(i.imm1));
    }

    public void visit(LegacyInstr.BRTS i) {
        branch(relative(i.imm1));
    }

    public void visit(LegacyInstr.BRVC i) {
        branch(relative(i.imm1));
    }

    public void visit(LegacyInstr.BRVS i) {
        branch(relative(i.imm1));
    }

    public void visit(LegacyInstr.BSET i) {
        writeIORegister(i.imm1, ISEValue.UNKNOWN);
        // getIOReg(SREG).writeBit(i.imm1, true);
    }

    public void visit(LegacyInstr.BST i) {
        byte tmp0 = readRegister(i.r1);
        writeSREG(ISEValue.UNKNOWN);
        //T = Arithmetic.getBit(tmp0, i.imm1);
    }

    public void visit(LegacyInstr.CALL i) {
        int target = absolute(i.imm1);
        ISEState rs = cache.getProcedureSummary(target);
        ISEState fs = processReturnState(state, rs);
        addToWorkList("RET", nextPC, fs);
        end();
    }

    public void visit(LegacyInstr.CBI i) {
        writeIORegister(i.imm1, ISEValue.UNKNOWN);
        //getIOReg(i.imm1).writeBit(i.imm2, false);
    }

    public void visit(LegacyInstr.CBR i) {
        unop(i.r1);
    }

    public void visit(LegacyInstr.CLC i) {
        writeSREG(ISEValue.UNKNOWN);
        // C = false;
    }

    public void visit(LegacyInstr.CLH i) {
        writeSREG(ISEValue.UNKNOWN);
        // H = false;
    }

    public void visit(LegacyInstr.CLI i) {
        writeSREG(ISEValue.UNKNOWN);
        // disableInterrupts();
    }

    public void visit(LegacyInstr.CLN i) {
        writeSREG(ISEValue.UNKNOWN);
        // N = false;
    }

    public void visit(LegacyInstr.CLR i) {
        writeSREG(ISEValue.UNKNOWN);
        writeRegister(i.r1, ISEValue.UNKNOWN);
    }

    public void visit(LegacyInstr.CLS i) {
        writeSREG(ISEValue.UNKNOWN);
        // S = false;
    }

    public void visit(LegacyInstr.CLT i) {
        writeSREG(ISEValue.UNKNOWN);
        // T = false;
    }

    public void visit(LegacyInstr.CLV i) {
        writeSREG(ISEValue.UNKNOWN);
        // V = false;
    }

    public void visit(LegacyInstr.CLZ i) {
        writeSREG(ISEValue.UNKNOWN);
        // Z = false;
    }

    public void visit(LegacyInstr.COM i) {
        unop(i.r1);
    }

    public void visit(LegacyInstr.CP i) {
        readRegister(i.r1);
        readRegister(i.r2);
        writeSREG(ISEValue.UNKNOWN);
    }

    public void visit(LegacyInstr.CPC i) {
        readRegister(i.r1);
        readRegister(i.r2);
        writeSREG(ISEValue.UNKNOWN);
    }

    public void visit(LegacyInstr.CPI i) {
        readRegister(i.r1);
        writeSREG(ISEValue.UNKNOWN);
    }

    public void visit(LegacyInstr.CPSE i) {
        readRegister(i.r1);
        readRegister(i.r2);
        writeSREG(ISEValue.UNKNOWN);
        skip();
    }

    public void visit(LegacyInstr.DEC i) {
        unop(i.r1);
    }

    public void visit(LegacyInstr.EICALL i) {
        throw Util.unimplemented();
    }

    public void visit(LegacyInstr.EIJMP i) {
        throw Util.unimplemented();
    }

    public void visit(LegacyInstr.ELPM i) {
        readRegister(LegacyRegister.Z);
        readRegister(LegacyRegister.Z.nextRegister());
        writeRegister(LegacyRegister.R0, ISEValue.UNKNOWN);
    }

    public void visit(LegacyInstr.ELPMD i) {
        readRegister(LegacyRegister.Z);
        readRegister(LegacyRegister.Z.nextRegister());
        writeRegister(i.r1, ISEValue.UNKNOWN);
    }

    public void visit(LegacyInstr.ELPMPI i) {
        readRegister(LegacyRegister.Z);
        readRegister(LegacyRegister.Z.nextRegister());
        writeRegister(i.r1, ISEValue.UNKNOWN);
        writeRegister(LegacyRegister.Z, ISEValue.UNKNOWN);
        writeRegister(LegacyRegister.Z.nextRegister(), ISEValue.UNKNOWN);
    }

    public void visit(LegacyInstr.EOR i) {
        if ( i.r1 == i.r2 ) {
            // special case: clear the register
            writeSREG(ISEValue.UNKNOWN);
            writeRegister(i.r1, ISEValue.UNKNOWN);
        } else {
            binop(i.r1, i.r2);
        }
    }

    public void visit(LegacyInstr.FMUL i) {
        mult(i.r1, i.r2);
    }

    public void visit(LegacyInstr.FMULS i) {
        mult(i.r1, i.r2);
    }

    public void visit(LegacyInstr.FMULSU i) {
        mult(i.r1, i.r2);
    }

    public void visit(LegacyInstr.ICALL i) {
        List iedges = program.getIndirectEdges(pc);
        if (iedges == null)
            throw Util.failure("No control flow information for indirect call at: " +
                    StringUtil.addrToString(pc));
        Iterator it = iedges.iterator();
        while (it.hasNext()) {
            int target_address = ((Integer)it.next()).intValue();
            ISEState rs = cache.getProcedureSummary(target_address);
            ISEState fs = processReturnState(state, rs);
            addToWorkList("RET", nextPC, fs);
        }
        end();
    }

    public void visit(LegacyInstr.IJMP i) {
        List iedges = program.getIndirectEdges(pc);
        if (iedges == null)
            throw Util.failure("No control flow information for indirect call at: " +
                    StringUtil.addrToString(pc));
        Iterator it = iedges.iterator();
        while (it.hasNext()) {
            int target_address = ((Integer)it.next()).intValue();
            jump(target_address);
        }
    }

    public void visit(LegacyInstr.IN i) {
        writeRegister(i.r1, readIORegister(i.imm1));
    }

    public void visit(LegacyInstr.INC i) {
        unop(i.r1);
    }

    public void visit(LegacyInstr.JMP i) {
        jump(absolute(i.imm1));
    }

    public void visit(LegacyInstr.LD i) {
        writeRegister(i.r1, ISEValue.UNKNOWN);
    }

    public void visit(LegacyInstr.LDD i) {
        writeRegister(i.r1, ISEValue.UNKNOWN);
    }

    public void visit(LegacyInstr.LDI i) {
        writeRegister(i.r1, ISEValue.UNKNOWN);
    }

    public void visit(LegacyInstr.LDPD i) {
        writeRegister(i.r1, ISEValue.UNKNOWN);
        writeRegister(i.r2, ISEValue.UNKNOWN);
        writeRegister(i.r2.nextRegister(), ISEValue.UNKNOWN);
    }

    public void visit(LegacyInstr.LDPI i) {
        writeRegister(i.r1, ISEValue.UNKNOWN);
        writeRegister(i.r2, ISEValue.UNKNOWN);
        writeRegister(i.r2.nextRegister(), ISEValue.UNKNOWN);
    }

    public void visit(LegacyInstr.LDS i) {
        writeRegister(i.r1, ISEValue.UNKNOWN);
    }

    public void visit(LegacyInstr.LPM i) {
        readRegister(LegacyRegister.Z);
        readRegister(LegacyRegister.Z.nextRegister());
        writeRegister(LegacyRegister.R0, ISEValue.UNKNOWN);
    }

    public void visit(LegacyInstr.LPMD i) {
        readRegister(LegacyRegister.Z);
        readRegister(LegacyRegister.Z.nextRegister());
        writeRegister(i.r1, ISEValue.UNKNOWN);
    }

    public void visit(LegacyInstr.LPMPI i) {
        readRegister(LegacyRegister.Z);
        readRegister(LegacyRegister.Z.nextRegister());
        writeRegister(i.r1, ISEValue.UNKNOWN);
        writeRegister(LegacyRegister.Z, ISEValue.UNKNOWN);
        writeRegister(LegacyRegister.Z.nextRegister(), ISEValue.UNKNOWN);
    }

    public void visit(LegacyInstr.LSL i) {
        unop(i.r1);
    }

    public void visit(LegacyInstr.LSR i) {
        unop(i.r1);
    }

    public void visit(LegacyInstr.MOV i) {
        writeRegister(i.r1, getRegister(i.r2));
    }

    public void visit(LegacyInstr.MOVW i) {
        writeRegister(i.r1, getRegister(i.r2));
        writeRegister(i.r1.nextRegister(), getRegister(i.r2.nextRegister()));
    }

    public void visit(LegacyInstr.MUL i) {
        mult(i.r1, i.r2);
    }

    public void visit(LegacyInstr.MULS i) {
        mult(i.r1, i.r2);
    }

    public void visit(LegacyInstr.MULSU i) {
        mult(i.r1, i.r2);
    }

    public void visit(LegacyInstr.NEG i) {
        unop(i.r1);
    }

    public void visit(LegacyInstr.NOP i) {
        // do nothing.
    }

    public void visit(LegacyInstr.OR i) {
        binop(i.r1, i.r2);
    }

    public void visit(LegacyInstr.ORI i) {
        unop(i.r1);
    }

    public void visit(LegacyInstr.OUT i) {
        writeIORegister(i.imm1, readRegister(i.r1));
    }

    public void visit(LegacyInstr.POP i) {
        writeRegister(i.r1, popByte());
    }

    public void visit(LegacyInstr.PUSH i) {
        pushByte(getRegister(i.r1));
    }

    public void visit(LegacyInstr.RCALL i) {
        int target = relative(i.imm1);
        ISEState rs = cache.getProcedureSummary(target);
        ISEState fs = processReturnState(state, rs);
        addToWorkList("RET", nextPC, fs);
        end();
    }

    private void end() {
        nextPC = -1;
    }

    public void visit(LegacyInstr.RET i) {
        postReturn(state);
        end();
    }

    public void visit(LegacyInstr.RETI i) {
        postReturnFromInterrupt(state);
        end();
    }

    public void visit(LegacyInstr.RJMP i) {
        jump(relative(i.imm1));
    }

    public void visit(LegacyInstr.ROL i) {
        unop(i.r1);
    }

    public void visit(LegacyInstr.ROR i) {
        unop(i.r1);
    }

    public void visit(LegacyInstr.SBC i) {
        binop(i.r1, i.r2);
    }

    public void visit(LegacyInstr.SBCI i) {
        unop(i.r1);
    }

    public void visit(LegacyInstr.SBI i) {
        writeIORegister(i.imm1, ISEValue.UNKNOWN);
        //getIOReg(i.imm1).writeBit(i.imm2, true);
    }

    public void visit(LegacyInstr.SBIC i) {
        readIORegister(i.imm1);
        skip();
    }

    public void visit(LegacyInstr.SBIS i) {
        readIORegister(i.imm1);
        skip();
    }

    public void visit(LegacyInstr.SBIW i) {
        readRegister(i.r1);
        readRegister(i.r1.nextRegister());
        writeSREG(ISEValue.UNKNOWN);
        writeRegister(i.r1, ISEValue.UNKNOWN);
        writeRegister(i.r1.nextRegister(), ISEValue.UNKNOWN);
    }

    public void visit(LegacyInstr.SBR i) {
        unop(i.r1);
    }

    public void visit(LegacyInstr.SBRC i) {
        readRegister(i.r1);
        skip();
    }

    public void visit(LegacyInstr.SBRS i) {
        readRegister(i.r1);
        skip();
    }

    public void visit(LegacyInstr.SEC i) {
        writeSREG(ISEValue.UNKNOWN);
        // C = true;
    }

    public void visit(LegacyInstr.SEH i) {
        writeSREG(ISEValue.UNKNOWN);
        // H = true;
    }

    public void visit(LegacyInstr.SEI i) {
        writeSREG(ISEValue.UNKNOWN);
        // enableInterrupts();
    }

    public void visit(LegacyInstr.SEN i) {
        writeSREG(ISEValue.UNKNOWN);
        // N = true;
    }

    public void visit(LegacyInstr.SER i) {
        writeRegister(i.r1, ISEValue.UNKNOWN);
    }

    public void visit(LegacyInstr.SES i) {
        writeSREG(ISEValue.UNKNOWN);
        // S = true;
    }

    public void visit(LegacyInstr.SET i) {
        writeSREG(ISEValue.UNKNOWN);
        // T = true;
    }

    public void visit(LegacyInstr.SEV i) {
        writeSREG(ISEValue.UNKNOWN);
        // V = true;
    }

    public void visit(LegacyInstr.SEZ i) {
        writeSREG(ISEValue.UNKNOWN);
        // Z = true;
    }

    public void visit(LegacyInstr.SLEEP i) {
        // do nothing.
    }

    public void visit(LegacyInstr.SPM i) {
        readRegister(LegacyRegister.R0);
        readRegister(LegacyRegister.R1);
        readRegister(LegacyRegister.Z);
        readRegister(LegacyRegister.Z.nextRegister());
    }

    public void visit(LegacyInstr.ST i) {
        readRegister(i.r1);
        readRegister(i.r1.nextRegister());
        readRegister(i.r2);
    }

    public void visit(LegacyInstr.STD i) {
        readRegister(i.r1);
        readRegister(i.r1.nextRegister());
        readRegister(i.r2);
    }

    public void visit(LegacyInstr.STPD i) {
        readRegister(i.r1);
        readRegister(i.r1.nextRegister());
        readRegister(i.r2);
        writeRegister(i.r1, ISEValue.UNKNOWN);
        writeRegister(i.r1.nextRegister(), ISEValue.UNKNOWN);
    }

    public void visit(LegacyInstr.STPI i) {
        readRegister(i.r1);
        readRegister(i.r1.nextRegister());
        readRegister(i.r2);
        writeRegister(i.r1, ISEValue.UNKNOWN);
        writeRegister(i.r1.nextRegister(), ISEValue.UNKNOWN);
    }

    public void visit(LegacyInstr.STS i) {
        readRegister(i.r1);
    }

    public void visit(LegacyInstr.SUB i) {
        binop(i.r1, i.r2);
    }

    public void visit(LegacyInstr.SUBI i) {
        unop(i.r1);
    }

    public void visit(LegacyInstr.SWAP i) {
        unop(i.r1);
    }

    public void visit(LegacyInstr.TST i) {
        readRegister(i.r1);
        writeSREG(ISEValue.UNKNOWN);
    }

    public void visit(LegacyInstr.WDR i) {
        // do nothing.
    }
}
