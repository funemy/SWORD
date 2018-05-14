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

package avrora.stack;

import avrora.arch.legacy.*;
import avrora.core.Program;
import cck.text.StringUtil;
import cck.util.Util;

/**
 * The <code>AbstractInterpreter</code> class implements the abstract transfer function for each instruction
 * type. Given an abstract state, it updates the abstract state according to the semantics of each
 * instruction. The abstract interpreter works on the simple instructions. For complex instructions such as
 * calls, returns, and pushes, it consults a <code>Policy</code> instance that implements the context
 * sensitivity/insensitivity and stack modelling behavior of the particular analysis.
 * <p/>
 * The <code>AbstractInterpreter</code> works on abstract values and uses abstract arithmetic. It operates on
 * instances of the <code>AbstractState</code> class that represent the state of the processor.
 *
 * @author Ben L. Titzer
 * @see AbstractArithmetic
 * @see MutableState
 */
public class AbstractInterpreter extends AbstractArithmetic implements LegacyInstrVisitor {

    protected final AnalyzerPolicy policy;
    protected final Program program;
    protected StateCache.State oldState;
    protected MutableState state;

    AbstractInterpreter(Program pr, AnalyzerPolicy p) {
        policy = p;
        program = pr;
    }

    /**
     * The <code>computeNextStates()</code> method computes the possible next states that follow the given
     * immutable old state and then will push them to the <code>AnalyzerPolicy</code> instance that was passed
     * in the constructor to this interpreter instance.
     *
     * @param os the immutable old state to compute the next state from
     */
    public void computeNextStates(StateCache.State os) {
        oldState = os;
        state = oldState.copy();

        if (state.getFlag_I() != FALSE) {
            // produce interrupt edges to possibly enabled interrupts.
            char eimsk = state.getIORegisterAV(IORegisterConstants.EIMSK);
            for (int cntr = 0; cntr < 8; cntr++) {
                char msk = AbstractArithmetic.getBit(eimsk, cntr);
                if (msk == FALSE) continue;
                policy.interrupt(state.copy(), cntr + 2);
            }

            // produce interrupt edges to possibly enabled interrupts.
            char timsk = state.getIORegisterAV(IORegisterConstants.TIMSK);
            for (int cntr = 0; cntr < 8; cntr++) {
                char msk = AbstractArithmetic.getBit(timsk, cntr);
                if (msk == FALSE) continue;
                policy.interrupt(state.copy(), 17 - cntr);
            }
        }

        int pc = state.getPC();
        LegacyInstr i = readInstr(pc);
        i.accept(this);

        if (state != null) {
            // if we didn't reach a dead end (e.g. a break instruction, return, etc)
            state.setPC(pc + i.getSize());
            policy.pushState(state);
        }
    }

    private LegacyInstr readInstr(int pc) {
        if (pc >= program.program_end)
            throw Util.failure("PC beyond end of program: " + StringUtil.addrToString(pc));
        LegacyInstr i = (LegacyInstr)program.readInstr(pc);
        if (i == null)
            throw Util.failure("Misaligned instruction access at PC: " + StringUtil.addrToString(pc));
        return i;
    }

    //-----------------------------------------------------------------------
    //  V I S I T O R   M E T H O D S
    //-----------------------------------------------------------------------
    //
    //  These visit methods implement the analysis of individual
    //  instructions for building the reachable state space of the
    //  program.
    //
    //-----------------------------------------------------------------------

    public void visit(LegacyInstr.ADC i) { // add register to register with carry
        char r1 = state.getRegisterAV(i.r1);
        char r2 = state.getRegisterAV(i.r2);
        char result = performAddition(r1, r2, state.getFlag_C());
        state.setRegisterAV(i.r1, result);
    }

    public void visit(LegacyInstr.ADD i) { // add register to register
        char r1 = state.getRegisterAV(i.r1);
        char r2 = state.getRegisterAV(i.r2);
        char result = performAddition(r1, r2, FALSE);
        state.setRegisterAV(i.r1, result);
    }

    public void visit(LegacyInstr.ADIW i) {// add immediate to word register
        char rh = state.getRegisterAV(i.r1.nextRegister());

        // add the immediate value into the actual register
        addImmediateToRegister(i.r1, i.imm1);

        // read the upper and lower parts of result from register
        char RL = state.getRegisterAV(i.r1);
        char RH = state.getRegisterAV(i.r1.nextRegister());

        // extract some bits of interest
        char R15 = getBit(RH, 7);
        char Rdh7 = getBit(rh, 7);

        // flag computations
        state.setFlag_C(and(not(R15), Rdh7));
        state.setFlag_N(R15);
        state.setFlag_V(and(not(Rdh7), R15));
        state.setFlag_Z(couldBeZero(RL, RH));
        state.setFlag_S(xor(state.getFlag_N(), state.getFlag_Z()));
    }

    public void visit(LegacyInstr.AND i) {// and register with register
        char r1 = state.getRegisterAV(i.r1);
        char r2 = state.getRegisterAV(i.r2);
        char result = performAnd(r1, r2);
        state.setRegisterAV(i.r1, result);
    }

    public void visit(LegacyInstr.ANDI i) {// and register with immediate
        char r1 = state.getRegisterAV(i.r1);
        char r2 = knownVal((byte)i.imm1);
        char result = performAnd(r1, r2);
        state.setRegisterAV(i.r1, result);
    }

    public void visit(LegacyInstr.ASR i) {// arithmetic shift right
        char val = state.getRegisterAV(i.r1);
        char result = performRightShift(val, getBit(val, 7));
        state.setRegisterAV(i.r1, result);
    }

    public void visit(LegacyInstr.BCLR i) {// clear bit in status register
        state.setSREG_bit(i.imm1, FALSE);
    }

    public void visit(LegacyInstr.BLD i) {// load bit from T flag into register
        char T = state.getFlag_T();
        char val = state.getRegisterAV(i.r1);
        char result = setBit(val, i.imm1, T);
        state.setRegisterAV(i.r1, result);
    }

    public void visit(LegacyInstr.BRBC i) {// branch if bit in status register is clear
        char val = state.getSREG();
        char bit = getBit(val, i.imm1);
        branchOnCondition(not(bit), i.imm2);
    }

    public void visit(LegacyInstr.BRBS i) {// branch if bit in status register is set
        char val = state.getSREG();
        char bit = getBit(val, i.imm1);
        branchOnCondition(bit, i.imm2);
    }

    public void visit(LegacyInstr.BRCC i) {// branch if carry flag is clear
        char cond = state.getFlag_C();
        branchOnCondition(not(cond), i.imm1);
    }

    public void visit(LegacyInstr.BRCS i) { // branch if carry flag is set
        char cond = state.getFlag_C();
        branchOnCondition(cond, i.imm1);
    }

    public void visit(LegacyInstr.BREAK i) {// break
        state = null;
    }

    public void visit(LegacyInstr.BREQ i) {// branch if equal
        branchOnCondition(state.getFlag_Z(), i.imm1);
    }

    public void visit(LegacyInstr.BRGE i) {// branch if greater or equal (signed)
        branchOnCondition(not(xor(state.getFlag_N(), state.getFlag_V())), i.imm1);
    }

    public void visit(LegacyInstr.BRHC i) {// branch if H flag is clear
        branchOnCondition(not(state.getFlag_H()), i.imm1);
    }

    public void visit(LegacyInstr.BRHS i) {// branch if H flag is set
        branchOnCondition(state.getFlag_H(), i.imm1);
    }

    public void visit(LegacyInstr.BRID i) {// branch if interrupts are disabled
        branchOnCondition(not(state.getFlag_I()), i.imm1);
    }

    public void visit(LegacyInstr.BRIE i) {// branch if interrupts are enabled
        branchOnCondition(state.getFlag_I(), i.imm1);
    }

    public void visit(LegacyInstr.BRLO i) { // branch if lower
        branchOnCondition(state.getFlag_C(), i.imm1);
    }

    public void visit(LegacyInstr.BRLT i) { // branch if less than zero (signed)
        branchOnCondition(xor(state.getFlag_N(), state.getFlag_V()), i.imm1);
    }

    public void visit(LegacyInstr.BRMI i) { // branch if minus
        branchOnCondition(state.getFlag_N(), i.imm1);
    }

    public void visit(LegacyInstr.BRNE i) { // branch if not equal
        branchOnCondition(state.getFlag_Z(), i.imm1);
    }

    public void visit(LegacyInstr.BRPL i) { // branch if positive
        branchOnCondition(not(state.getFlag_N()), i.imm1);
    }

    public void visit(LegacyInstr.BRSH i) { // branch if same or higher
        branchOnCondition(not(state.getFlag_C()), i.imm1);
    }

    public void visit(LegacyInstr.BRTC i) { // branch if T flag is clear
        branchOnCondition(not(state.getFlag_T()), i.imm1);
    }

    public void visit(LegacyInstr.BRTS i) { // branch if T flag is set
        branchOnCondition(state.getFlag_T(), i.imm1);
    }

    public void visit(LegacyInstr.BRVC i) { // branch if V flag is clear
        branchOnCondition(not(state.getFlag_V()), i.imm1);
    }

    public void visit(LegacyInstr.BRVS i) { // branch if V flag is set
        branchOnCondition(state.getFlag_V(), i.imm1);
    }

    public void visit(LegacyInstr.BSET i) { // set flag in status register
        state.setSREG_bit(i.imm1, TRUE);
    }

    public void visit(LegacyInstr.BST i) { // store bit in register into T flag
        char val = state.getRegisterAV(i.r1);
        char T = getBit(val, i.imm1);
        state.setFlag_T(T);
    }

    public void visit(LegacyInstr.CALL i) { // call absolute address
        state = policy.call(state, absolute(i.imm1));
    }

    public void visit(LegacyInstr.CBI i) { // clear bit in IO register
        char val = state.getIORegisterAV(i.imm1);
        char result = setBit(val, i.imm2, FALSE);
        state.setIORegisterAV(i.imm1, result);
    }

    public void visit(LegacyInstr.CBR i) { // clear bits in register
        char r1 = state.getRegisterAV(i.r1);
        char r2 = knownVal((byte)~i.imm1);
        char result = performAnd(r1, r2);
        state.setRegisterAV(i.r1, result);
    }

    public void visit(LegacyInstr.CLC i) { // clear C flag
        state.setFlag_C(FALSE);
    }

    public void visit(LegacyInstr.CLH i) { // clear H flag
        state.setFlag_H(FALSE);
    }

    public void visit(LegacyInstr.CLI i) { // clear I flag
        state.setFlag_I(FALSE);
    }

    public void visit(LegacyInstr.CLN i) { // clear N flag
        state.setFlag_N(FALSE);
    }

    public void visit(LegacyInstr.CLR i) { // clear register (set to zero)
        state.setFlag_S(FALSE);
        state.setFlag_V(FALSE);
        state.setFlag_N(FALSE);
        state.setFlag_Z(TRUE);
        state.setRegisterAV(i.r1, ZERO);
    }

    public void visit(LegacyInstr.CLS i) { // clear S flag
        state.setFlag_S(FALSE);
    }

    public void visit(LegacyInstr.CLT i) { // clear T flag
        state.setFlag_T(FALSE);
    }

    public void visit(LegacyInstr.CLV i) { // clear V flag
        state.setFlag_V(FALSE);
    }

    public void visit(LegacyInstr.CLZ i) { // clear Z flag
        state.setFlag_Z(FALSE);
    }

    public void visit(LegacyInstr.COM i) { // one's compliment register
        char r1 = state.getRegisterAV(i.r1);
        char mask = maskOf(r1);
        char result = canon(mask, (char)~r1);

        char C = TRUE;
        char N = getBit(result, 7);
        char Z = couldBeZero(result);
        char V = FALSE;
        char S = xor(N, V);
        setFlag_CNZVS(C, N, Z, V, S);

        state.setRegisterAV(i.r1, result);
    }

    public void visit(LegacyInstr.CP i) { // compare registers
        char r1 = state.getRegisterAV(i.r1);
        char r2 = state.getRegisterAV(i.r2);
        // perform subtraction for flag side effects.
        performSubtraction(r1, r2, FALSE);
    }

    public void visit(LegacyInstr.CPC i) { // compare registers with carry
        char r1 = state.getRegisterAV(i.r1);
        char r2 = state.getRegisterAV(i.r2);
        // perform subtraction for flag side effects.
        performSubtraction(r1, r2, state.getFlag_C());
    }

    public void visit(LegacyInstr.CPI i) { // compare register with immediate
        char r1 = state.getRegisterAV(i.r1);
        char r2 = knownVal((byte)i.imm1);
        // perform subtraction for flag side effects.
        performSubtraction(r1, r2, FALSE);
    }

    public void visit(LegacyInstr.CPSE i) { // compare registers and skip if equal
        char r1 = state.getRegisterAV(i.r1);
        char r2 = state.getRegisterAV(i.r2);
        performSubtraction(r1, r2, FALSE);
        skipOnCondition(state.getFlag_Z());
    }

    public void visit(LegacyInstr.DEC i) { // decrement register by one
        char r1 = state.getRegisterAV(i.r1);
        char result = decrement(r1);

        char N = getBit(result, 7);
        char Z = couldBeZero(result);
        char V = couldBeEqual(r1, knownVal((byte)0x80));
        char S = xor(N, V);
        setFlag_NZVS(N, Z, V, S);

        state.setRegisterAV(i.r1, result);
    }

    public void visit(LegacyInstr.EICALL i) { // extended indirect call
        char rl = state.getRegisterAV(LegacyRegister.Z);
        char rh = state.getRegisterAV(LegacyRegister.Z.nextRegister());
        char ext = state.getIORegisterAV(IORegisterConstants.RAMPZ);
        state = policy.indirectCall(state, rl, rh, ext);
    }

    public void visit(LegacyInstr.EIJMP i) { // extended indirect jump
        char rl = state.getRegisterAV(LegacyRegister.Z);
        char rh = state.getRegisterAV(LegacyRegister.Z.nextRegister());
        char ext = state.getIORegisterAV(IORegisterConstants.RAMPZ);
        state = policy.indirectJump(state, rl, rh, ext);
    }

    public void visit(LegacyInstr.ELPM i) { // extended load program memory to r0
        state.setRegisterAV(LegacyRegister.R0, UNKNOWN);
    }

    public void visit(LegacyInstr.ELPMD i) { // extended load program memory to register
        state.setRegisterAV(i.r1, UNKNOWN);
    }

    public void visit(LegacyInstr.ELPMPI i) { // extended load program memory to register and post-increment
        state.setRegisterAV(i.r1, UNKNOWN);
        addImmediateToRegister(i.r2, 1);
    }

    public void visit(LegacyInstr.EOR i) { // exclusive or register with register
        char result;

        if (i.r1 == i.r2) { // recognize A ^ A = A
            result = ZERO;
        } else {
            char r1 = state.getRegisterAV(i.r1);
            char r2 = state.getRegisterAV(i.r2);
            result = xor(r1, r2);
        }

        char N = getBit(result, 7);
        char Z = couldBeZero(result);
        char V = FALSE;
        char S = xor(N, V);
        setFlag_NZVS(N, Z, V, S);

        state.setRegisterAV(i.r1, result);
    }

    public void visit(LegacyInstr.FMUL i) { // fractional multiply register with register to r0
        char r1 = state.getRegisterAV(i.r1);
        char r2 = state.getRegisterAV(i.r2);
        int result = mul8(r1, false, r2, false);
        finishFMUL(result);

    }

    public void visit(LegacyInstr.FMULS i) { // signed fractional multiply register with register to r0
        char r1 = state.getRegisterAV(i.r1);
        char r2 = state.getRegisterAV(i.r2);
        int result = mul8(r1, true, r2, true);
        finishFMUL(result);
    }

    public void visit(LegacyInstr.FMULSU i) { // signed/unsigned fractional multiply register with register to r0
        char r1 = state.getRegisterAV(i.r1);
        char r2 = state.getRegisterAV(i.r2);
        int result = mul8(r1, true, r2, false);
        finishFMUL(result);
    }

    private void finishFMUL(int result) {
        char RL = lowAbstractByte(result);
        char RH = highAbstractByte(result);
        char R15 = getBit(RH, 7);
        char R7 = getBit(RL, 7);

        RL = shiftLeftOne(RL);
        RH = shiftLeftOne(RH, R7);

        state.setFlag_C(R15);
        state.setFlag_Z(couldBeZero(RL, RH));
        writeRegisterWord(LegacyRegister.R0, RL, RH);
    }


    public void visit(LegacyInstr.ICALL i) { // indirect call through Z register
        char rl = state.getRegisterAV(LegacyRegister.Z);
        char rh = state.getRegisterAV(LegacyRegister.Z.nextRegister());
        state = policy.indirectCall(state, rl, rh);
    }

    public void visit(LegacyInstr.IJMP i) { // indirect jump through Z register
        char rl = state.getRegisterAV(LegacyRegister.Z);
        char rh = state.getRegisterAV(LegacyRegister.Z.nextRegister());
        state = policy.indirectJump(state, rl, rh);
    }

    public void visit(LegacyInstr.IN i) { // read from IO register into register
        char val = state.getIORegisterAV(i.imm1);
        state.setRegisterAV(i.r1, val);
    }

    public void visit(LegacyInstr.INC i) { // increment register by one
        char r1 = state.getRegisterAV(i.r1);
        char result = increment(r1);

        char N = getBit(result, 7);
        char Z = couldBeZero(result);
        char V = couldBeEqual(r1, knownVal((byte)0x7f));
        char S = xor(N, V);
        setFlag_NZVS(N, Z, V, S);

        state.setRegisterAV(i.r1, result);
    }

    public void visit(LegacyInstr.JMP i) { // absolute jump
        state.setPC(absolute(i.imm1));
        policy.pushState(state);
        state = null;
    }

    public void visit(LegacyInstr.LD i) { // load from SRAM
        state.setRegisterAV(i.r1, UNKNOWN);
    }

    public void visit(LegacyInstr.LDD i) { // load from SRAM with displacement
        state.setRegisterAV(i.r1, UNKNOWN);
    }

    public void visit(LegacyInstr.LDI i) { // load immediate into register
        state.setRegisterAV(i.r1, knownVal((byte)i.imm1));
    }

    public void visit(LegacyInstr.LDPD i) { // load from SRAM with pre-decrement
        state.setRegisterAV(i.r1, UNKNOWN);
        addImmediateToRegister(i.r2, -1);
    }

    public void visit(LegacyInstr.LDPI i) { // load from SRAM with post-increment
        state.setRegisterAV(i.r1, UNKNOWN);
        addImmediateToRegister(i.r2, 1);
    }

    public void visit(LegacyInstr.LDS i) { // load direct from SRAM
        state.setRegisterAV(i.r1, UNKNOWN);
    }

    public void visit(LegacyInstr.LPM i) { // load program memory into r0
        state.setRegisterAV(LegacyRegister.R0, UNKNOWN);
    }

    public void visit(LegacyInstr.LPMD i) { // load program memory into register
        state.setRegisterAV(i.r1, UNKNOWN);
    }

    public void visit(LegacyInstr.LPMPI i) { // load program memory into register and post-increment
        state.setRegisterAV(i.r1, UNKNOWN);
        addImmediateToRegister(i.r2, 1);
    }

    public void visit(LegacyInstr.LSL i) { // logical shift left
        char val = state.getRegisterAV(i.r1);
        char result = performLeftShift(val, FALSE);
        state.setRegisterAV(i.r1, result);
    }

    public void visit(LegacyInstr.LSR i) { // logical shift right
        char val = state.getRegisterAV(i.r1);
        char result = performRightShift(val, FALSE);
        state.setRegisterAV(i.r1, result);
    }

    public void visit(LegacyInstr.MOV i) { // copy register to register
        char result = state.getRegisterAV(i.r2);
        state.setRegisterAV(i.r1, result);
    }

    public void visit(LegacyInstr.MOVW i) { // copy two registers to two registers
        char vall = state.getRegisterAV(i.r2);
        char valh = state.getRegisterAV(i.r2.nextRegister());

        state.setRegisterAV(i.r1, vall);
        state.setRegisterAV(i.r1.nextRegister(), valh);
    }

    public void visit(LegacyInstr.MUL i) { // multiply register with register to r0
        char r1 = state.getRegisterAV(i.r1);
        char r2 = state.getRegisterAV(i.r2);
        int result = mul8(r1, false, r2, false);
        finishMultiply(result);
    }

    public void visit(LegacyInstr.MULS i) { // signed multiply register with register to r0
        char r1 = state.getRegisterAV(i.r1);
        char r2 = state.getRegisterAV(i.r2);
        int result = mul8(r1, true, r2, true);
        finishMultiply(result);
    }

    public void visit(LegacyInstr.MULSU i) { // signed/unsigned multiply register with register to r0
        char r1 = state.getRegisterAV(i.r1);
        char r2 = state.getRegisterAV(i.r2);
        int result = mul8(r1, true, r2, false);
        finishMultiply(result);
    }

    private void finishMultiply(int result) {
        char RL = lowAbstractByte(result);
        char RH = highAbstractByte(result);
        state.setFlag_C(getBit(RH, 7));
        state.setFlag_Z(couldBeZero(RL, RH));
        writeRegisterWord(LegacyRegister.R0, RL, RH);
    }


    public void visit(LegacyInstr.NEG i) { // two's complement register
        char r1 = state.getRegisterAV(i.r1);
        char result = performSubtraction(ZERO, r1, FALSE);
        state.setRegisterAV(i.r1, result);
    }

    public void visit(LegacyInstr.NOP i) { // do nothing operation
        // do nothing.
    }

    public void visit(LegacyInstr.OR i) { // or register with register
        char r1 = state.getRegisterAV(i.r1);
        char r2 = state.getRegisterAV(i.r2);
        char result = performOr(r1, r2);
        state.setRegisterAV(i.r1, result);
    }

    public void visit(LegacyInstr.ORI i) { // or register with immediate
        char r1 = state.getRegisterAV(i.r1);
        char r2 = knownVal((byte)i.imm1);
        char result = performOr(r1, r2);
        state.setRegisterAV(i.r1, result);
    }

    public void visit(LegacyInstr.OUT i) { // write from register to IO register
        char val = state.getRegisterAV(i.r1);
        state.setIORegisterAV(i.imm1, val);
    }

    public void visit(LegacyInstr.POP i) { // pop from the stack to register
        char val = policy.pop(state);
        state.setRegisterAV(i.r1, val);
    }

    public void visit(LegacyInstr.PUSH i) { // push register to the stack
        char val = state.getRegisterAV(i.r1);
        policy.push(state, val);
    }

    public void visit(LegacyInstr.RCALL i) { // relative call
        state = policy.call(state, relative(i.imm1));
    }

    public void visit(LegacyInstr.RET i) { // return to caller
        state = policy.ret(state);
    }

    public void visit(LegacyInstr.RETI i) { // return from interrupt
        state = policy.reti(state);
    }

    public void visit(LegacyInstr.RJMP i) { // relative jump
        state.setPC(relative(i.imm1));
        policy.pushState(state);
        state = null;
    }

    public void visit(LegacyInstr.ROL i) { // rotate left through carry flag
        char val = state.getRegisterAV(i.r1);
        char result = performLeftShift(val, state.getFlag_C());
        state.setRegisterAV(i.r1, result);
    }

    public void visit(LegacyInstr.ROR i) { // rotate right through carry flag
        char val = state.getRegisterAV(i.r1);
        char result = performRightShift(val, state.getFlag_C());
        state.setRegisterAV(i.r1, result);
    }

    public void visit(LegacyInstr.SBC i) { // subtract register from register with carry
        char r1 = state.getRegisterAV(i.r1);
        char r2 = state.getRegisterAV(i.r2);
        char result = performSubtraction(r1, r2, state.getFlag_C());
        state.setRegisterAV(i.r1, result);
    }

    public void visit(LegacyInstr.SBCI i) { // subtract immediate from register with carry
        char r1 = state.getRegisterAV(i.r1);
        char imm = knownVal((byte)i.imm1);
        char result = performSubtraction(r1, imm, state.getFlag_C());
        state.setRegisterAV(i.r1, result);
    }

    public void visit(LegacyInstr.SBI i) { // set bit in IO register
        char val = state.getIORegisterAV(i.imm1);
        char result = setBit(val, i.imm2, TRUE);
        state.setIORegisterAV(i.imm1, result);
    }

    public void visit(LegacyInstr.SBIC i) { // skip if bit in IO register is clear
        char reg = state.getIORegisterAV(i.imm1);
        char bit = getBit(reg, i.imm2);
        skipOnCondition(not(bit));
    }

    public void visit(LegacyInstr.SBIS i) { // skip if bit in IO register is set
        char reg = state.getIORegisterAV(i.imm1);
        char bit = getBit(reg, i.imm2);
        skipOnCondition(bit);
    }

    public void visit(LegacyInstr.SBIW i) { // subtract immediate from word
        char rh = state.getRegisterAV(i.r1.nextRegister());

        // compute partial results
        addImmediateToRegister(i.r1, -i.imm1);

        // compute upper and lower parts of result from partial results
        char RL = state.getRegisterAV(i.r1);
        char RH = state.getRegisterAV(i.r1.nextRegister());

        char Rdh7 = getBit(rh, 7);
        char R15 = getBit(RH, 7);

        // compute and adjust flags as per instruction set documentation.
        char V = and(Rdh7, not(R15));
        char N = R15;
        char Z = couldBeZero(RL, RH);
        char C = and(R15, not(Rdh7));
        char S = xor(N, V);
        setFlag_CNZVS(C, N, Z, V, S);
    }

    public void visit(LegacyInstr.SBR i) { // set bits in register
        char r1 = state.getRegisterAV(i.r1);
        char r2 = knownVal((byte)i.imm1);
        char result = performOr(r1, r2);
        state.setRegisterAV(i.r1, result);
    }

    public void visit(LegacyInstr.SBRC i) { // skip if bit in register cleared
        char bit = getBit(state.getRegisterAV(i.r1), i.imm1);
        skipOnCondition(not(bit));
    }

    public void visit(LegacyInstr.SBRS i) { // skip if bit in register set
        char bit = getBit(state.getRegisterAV(i.r1), i.imm1);
        skipOnCondition(bit);
    }

    public void visit(LegacyInstr.SEC i) { // set C (carry) flag
        state.setFlag_C(TRUE);
    }

    public void visit(LegacyInstr.SEH i) { // set H (half carry) flag
        state.setFlag_H(TRUE);
    }

    public void visit(LegacyInstr.SEI i) { // set I (interrupt enable) flag
        state.setFlag_I(TRUE);
    }

    public void visit(LegacyInstr.SEN i) { // set N (negative) flag
        state.setFlag_N(TRUE);
    }

    public void visit(LegacyInstr.SER i) { // set bits in register
        state.setRegisterAV(i.r1, knownVal((byte)0xff));
    }

    public void visit(LegacyInstr.SES i) { // set S (signed) flag
        state.setFlag_S(TRUE);
    }

    public void visit(LegacyInstr.SET i) { // set T flag
        state.setFlag_T(TRUE);
    }

    public void visit(LegacyInstr.SEV i) { // set V (overflow) flag
        state.setFlag_V(TRUE);
    }

    public void visit(LegacyInstr.SEZ i) { // set Z (zero) flag
        state.setFlag_Z(TRUE);
    }

    public void visit(LegacyInstr.SLEEP i) { // invoke sleep mode
    }

    public void visit(LegacyInstr.SPM i) { // store to program memory from r0
        // do nothing, ignore this instruction
    }

    public void visit(LegacyInstr.ST i) { // store from register to SRAM
        // we do not model memory now.
    }

    public void visit(LegacyInstr.STD i) { // store from register to SRAM with displacement
        // we do not model memory now.
    }

    public void visit(LegacyInstr.STPD i) { // store from register to SRAM with pre-decrement
        addImmediateToRegister(i.r1, -1);
        // we do not model memory now.
    }

    public void visit(LegacyInstr.STPI i) { // store from register to SRAM with post-increment
        addImmediateToRegister(i.r1, 1);
        // we do not model memory now.
    }

    public void visit(LegacyInstr.STS i) { // store direct to SRAM
        // we do not model memory now.
    }

    public void visit(LegacyInstr.SUB i) { // subtract register from register
        char r1 = state.getRegisterAV(i.r1);
        char r2 = state.getRegisterAV(i.r2);
        char result = performSubtraction(r1, r2, FALSE);
        state.setRegisterAV(i.r1, result);
    }

    public void visit(LegacyInstr.SUBI i) { // subtract immediate from register
        char r1 = state.getRegisterAV(i.r1);
        char imm = knownVal((byte)i.imm1);
        char result = performSubtraction(r1, imm, FALSE);
        state.setRegisterAV(i.r1, result);
    }

    public void visit(LegacyInstr.SWAP i) { // swap nibbles in register
        char result = state.getRegisterAV(i.r1);
        int high = ((result & 0xF0F0) >> 4);
        int low = ((result & 0x0F0F) << 4);
        result = (char)(low | high);
        state.setRegisterAV(i.r1, result);
    }

    public void visit(LegacyInstr.TST i) { // test for zero or minus
        char r1 = state.getRegisterAV(i.r1);
        state.setFlag_V(FALSE);
        state.setFlag_Z(couldBeZero(r1));
        state.setFlag_N(getBit(r1, 7));
        state.setFlag_S(xor(state.getFlag_N(), state.getFlag_V()));
    }

    public void visit(LegacyInstr.WDR i) { // watchdog timer reset
        // do nothing.
    }

    //-----------------------------------------------------------------------
    //  U T I L I T I E S
    //-----------------------------------------------------------------------
    //
    //  These are some utility functions to help with implementing the
    // transfer functions.
    //
    //-----------------------------------------------------------------------

    private void branchOnCondition(char cond, int offset) {
        if (cond == FALSE) return; // branch is not taken

        // compute taken branch
        MutableState taken = state.copy();
        relativeBranch(taken, offset);
        policy.pushState(taken);

        // if condition is definately true, then the not taken branch is dead
        if (cond == TRUE) state = null;
    }

    private void skipOnCondition(char cond) {
        int pc = state.getPC();
        int npc = pc + 2;
        int offset = program.readInstr(npc).getSize() / 2;
        branchOnCondition(cond, offset);
    }

    private void relativeBranch(MutableState s, int offset) {
        s.setPC(relative(offset));
    }

    private void setFlag_HCNZVS(char H, char C, char N, char Z, char V, char S) {
        state.setFlag_H(H);
        state.setFlag_C(C);
        state.setFlag_N(N);
        state.setFlag_Z(Z);
        state.setFlag_V(V);
        state.setFlag_S(S);
    }

    private void setFlag_CNZVS(char C, char N, char Z, char V, char S) {
        state.setFlag_C(C);
        state.setFlag_N(N);
        state.setFlag_Z(Z);
        state.setFlag_V(V);
        state.setFlag_S(S);
    }

    private void setFlag_NZVS(char N, char Z, char V, char S) {
        state.setFlag_N(N);
        state.setFlag_Z(Z);
        state.setFlag_V(V);
        state.setFlag_S(S);
    }

    private char performAddition(char r1, char r2, char carry) {

        char result = add(r1, r2);

        if (carry == TRUE)
            result = increment(result);
        else if (carry == FALSE)
            ; /* do nothing. */
        else
            result = merge(result, increment(result));

        char Rd7 = getBit(r1, 7);
        char Rr7 = getBit(r2, 7);
        char R7 = getBit(result, 7);
        char Rd3 = getBit(r1, 3);
        char Rr3 = getBit(r2, 3);
        char R3 = getBit(result, 3);

        // set the flags as per instruction set documentation.
        char H = or(and(Rd3, Rr3), and(not(R3), Rd3, and(not(R3), Rr3)));
        char C = or(and(Rd7, Rr7), and(not(R7), Rd7, and(not(R7), Rr7)));
        char N = getBit(result, 7);
        char Z = couldBeZero(result);
        char V = or(and(Rd7, Rr7, not(R7)), (and(not(Rd7), not(Rr7), R7)));
        char S = xor(N, V);

        setFlag_HCNZVS(H, C, N, Z, V, S);
        return result;

    }

    private char performSubtraction(char r1, char r2, char carry) {
        char result = subtract(r1, r2);

        if (carry == TRUE)
            result = decrement(result);
        else if (carry == FALSE)
            ; /* do nothing. */
        else
            result = merge(result, decrement(result));

        char Rd7 = getBit(r1, 7);
        char Rr7 = getBit(r2, 7);
        char R7 = getBit(result, 7);
        char Rd3 = getBit(r1, 3);
        char Rr3 = getBit(r2, 3);
        char R3 = getBit(result, 3);

        // set the flags as per instruction set documentation.
        char H = or(and(not(Rd3), Rr3), and(Rr3, R3), and(R3, not(Rd3)));
        char C = or(and(not(Rd7), Rr7), and(Rr7, R7), and(R7, not(Rd7)));
        char N = R7;
        char Z = couldBeZero(result);
        char V = or(and(Rd7, not(Rr7), not(R7)), and(not(Rd7), Rr7, R7));
        char S = xor(N, V);

        setFlag_HCNZVS(H, C, N, Z, V, S);
        return result;

    }

    private char performRightShift(char val, char highbit) {
        char result = (char)(((val & 0xfefe) >> 1) | (highbit));

        char C = getBit(val, 1);
        char N = highbit;
        char Z = couldBeZero(result);
        char V = xor(N, C);
        char S = xor(N, V);
        setFlag_CNZVS(C, N, Z, V, S);
        return result;
    }


    private char performLeftShift(char val, char lowbit) {
        char result = shiftLeftOne(val, lowbit);

        char H = getBit(result, 3);
        char C = getBit(val, 7);
        char N = getBit(result, 7);
        char Z = couldBeZero(result);
        char V = xor(N, C);
        char S = xor(N, V);
        setFlag_HCNZVS(H, C, N, Z, V, S);
        return result;

    }

    private char performOr(char r1, char r2) {
        char result = or(r1, r2);

        char N = getBit(result, 7);
        char Z = couldBeZero(result);
        char V = FALSE;
        char S = xor(N, V);
        setFlag_NZVS(N, Z, V, S);

        return result;
    }

    private char performAnd(char r1, char r2) {
        char result = and(r1, r2);

        char N = getBit(result, 7);
        char Z = couldBeZero(result);
        char V = FALSE;
        char S = xor(N, V);
        setFlag_NZVS(N, Z, V, S);

        return result;
    }

    private void addImmediateToRegister(LegacyRegister r, int imm) {
        char v1 = state.getRegisterAV(r);
        char v2 = state.getRegisterAV(r.nextRegister());

        int resultA = ceiling(v1, v2) + imm;
        int resultB = floor(v1, v2) + imm;

        char RL = mergeMask(maskOf(v1), merge((byte)resultA, (byte)resultB));
        char RH = mergeMask(maskOf(v2), merge((byte)(resultA >> 8), (byte)(resultB >> 8)));

        state.setRegisterAV(r, RL);
        state.setRegisterAV(r.nextRegister(), RH);
    }

    private int mul8(char v1, boolean s1, char v2, boolean s2) {
        int ceil1 = ceiling(v1, s1);
        int ceil2 = ceiling(v2, s2);
        int floor1 = floor(v1, s1);
        int floor2 = floor(v2, s2);

        int resultA = ceil1 * ceil2;
        int resultB = ceil1 * floor2;
        int resultC = floor1 * ceil2;
        int resultD = floor1 * floor2;

        // merge partial results into upper and lower abstract bytes
        char RL = merge((byte)resultA, (byte)resultB, (byte)resultC, (byte)resultD);
        char RH = merge((byte)(resultA >> 8), (byte)(resultB >> 8),
                (byte)(resultC >> 8), (byte)(resultD >> 8));

        // pack the two results into a single integer
        return RH << 16 | RL;
    }

    private void writeRegisterWord(LegacyRegister r, char vl, char vh) {
        state.setRegisterAV(r, vl);
        state.setRegisterAV(r.nextRegister(), vh);
    }

    private int ceiling(char v1, boolean s1) {
        // sign extend the value if s1 is true.
        if (s1)
            return (int)(byte)ceiling(v1);
        else
            return ceiling(v1);
    }

    private int floor(char v1, boolean s1) {
        // sign extend the value if s1 is true.
        if (s1)
            return (int)(byte)floor(v1);
        else
            return floor(v1);
    }

    private char highAbstractByte(int result) {
        return (char)((result >> 16) & 0xffff);
    }

    private char lowAbstractByte(int result) {
        return (char)(result & 0xffff);
    }

    private int relative(int imm1) {
        return 2 + 2 * imm1 + state.getPC();
    }

    private int absolute(int imm1) {
        return 2 * imm1;
    }
}
