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

/**
 * @author Ben L. Titzer
 */
public interface LegacyInstrVisitor {
//--BEGIN INSTRVISITOR GENERATOR--
    public void visit(LegacyInstr.ADC i); // add register to register with carry

    public void visit(LegacyInstr.ADD i); // add register to register

    public void visit(LegacyInstr.ADIW i); // add immediate to word register

    public void visit(LegacyInstr.AND i); // and register with register

    public void visit(LegacyInstr.ANDI i); // and register with immediate

    public void visit(LegacyInstr.ASR i); // arithmetic shift right

    public void visit(LegacyInstr.BCLR i); // clear bit in status register

    public void visit(LegacyInstr.BLD i); // load bit from T flag into register

    public void visit(LegacyInstr.BRBC i); // branch if bit in status register is clear

    public void visit(LegacyInstr.BRBS i); // branch if bit in status register is set

    public void visit(LegacyInstr.BRCC i); // branch if carry flag is clear

    public void visit(LegacyInstr.BRCS i); // branch if carry flag is set

    public void visit(LegacyInstr.BREAK i); // break

    public void visit(LegacyInstr.BREQ i); // branch if equal

    public void visit(LegacyInstr.BRGE i); // branch if greater or equal (signed)

    public void visit(LegacyInstr.BRHC i); // branch if H flag is clear

    public void visit(LegacyInstr.BRHS i); // branch if H flag is set

    public void visit(LegacyInstr.BRID i); // branch if interrupts are disabled

    public void visit(LegacyInstr.BRIE i); // branch if interrupts are enabled

    public void visit(LegacyInstr.BRLO i); // branch if lower

    public void visit(LegacyInstr.BRLT i); // branch if less than zero (signed)

    public void visit(LegacyInstr.BRMI i); // branch if minus

    public void visit(LegacyInstr.BRNE i); // branch if not equal

    public void visit(LegacyInstr.BRPL i); // branch if positive

    public void visit(LegacyInstr.BRSH i); // branch if same or higher

    public void visit(LegacyInstr.BRTC i); // branch if T flag is clear

    public void visit(LegacyInstr.BRTS i); // branch if T flag is set

    public void visit(LegacyInstr.BRVC i); // branch if V flag is clear

    public void visit(LegacyInstr.BRVS i); // branch if V flag is set

    public void visit(LegacyInstr.BSET i); // set flag in status register

    public void visit(LegacyInstr.BST i); // store bit in register into T flag

    public void visit(LegacyInstr.CALL i); // call absolute address

    public void visit(LegacyInstr.CBI i); // clear bit in IO register

    public void visit(LegacyInstr.CBR i); // clear bits in register

    public void visit(LegacyInstr.CLC i); // clear C flag

    public void visit(LegacyInstr.CLH i); // clear H flag

    public void visit(LegacyInstr.CLI i); // clear I flag

    public void visit(LegacyInstr.CLN i); // clear N flag

    public void visit(LegacyInstr.CLR i); // clear register (set to zero)

    public void visit(LegacyInstr.CLS i); // clear S flag

    public void visit(LegacyInstr.CLT i); // clear T flag

    public void visit(LegacyInstr.CLV i); // clear V flag

    public void visit(LegacyInstr.CLZ i); // clear Z flag

    public void visit(LegacyInstr.COM i); // one's compliment register

    public void visit(LegacyInstr.CP i); // compare registers

    public void visit(LegacyInstr.CPC i); // compare registers with carry

    public void visit(LegacyInstr.CPI i); // compare register with immediate

    public void visit(LegacyInstr.CPSE i); // compare registers and skip if equal

    public void visit(LegacyInstr.DEC i); // decrement register by one

    public void visit(LegacyInstr.EICALL i); // extended indirect call

    public void visit(LegacyInstr.EIJMP i); // extended indirect jump

    public void visit(LegacyInstr.ELPM i); // extended load program memory to r0

    public void visit(LegacyInstr.ELPMD i); // extended load program memory to register

    public void visit(LegacyInstr.ELPMPI i); // extended load program memory to register and post-increment

    public void visit(LegacyInstr.EOR i); // exclusive or register with register

    public void visit(LegacyInstr.FMUL i); // fractional multiply register with register to r0

    public void visit(LegacyInstr.FMULS i); // signed fractional multiply register with register to r0

    public void visit(LegacyInstr.FMULSU i); // signed/unsigned fractional multiply register with register to r0

    public void visit(LegacyInstr.ICALL i); // indirect call through Z register

    public void visit(LegacyInstr.IJMP i); // indirect jump through Z register

    public void visit(LegacyInstr.IN i); // read from IO register into register

    public void visit(LegacyInstr.INC i); // increment register by one

    public void visit(LegacyInstr.JMP i); // absolute jump

    public void visit(LegacyInstr.LD i); // load from SRAM

    public void visit(LegacyInstr.LDD i); // load from SRAM with displacement

    public void visit(LegacyInstr.LDI i); // load immediate into register

    public void visit(LegacyInstr.LDPD i); // load from SRAM with pre-decrement

    public void visit(LegacyInstr.LDPI i); // load from SRAM with post-increment

    public void visit(LegacyInstr.LDS i); // load direct from SRAM

    public void visit(LegacyInstr.LPM i); // load program memory into r0

    public void visit(LegacyInstr.LPMD i); // load program memory into register

    public void visit(LegacyInstr.LPMPI i); // load program memory into register and post-increment

    public void visit(LegacyInstr.LSL i); // logical shift left

    public void visit(LegacyInstr.LSR i); // logical shift right

    public void visit(LegacyInstr.MOV i); // copy register to register

    public void visit(LegacyInstr.MOVW i); // copy two registers to two registers

    public void visit(LegacyInstr.MUL i); // multiply register with register to r0

    public void visit(LegacyInstr.MULS i); // signed multiply register with register to r0

    public void visit(LegacyInstr.MULSU i); // signed/unsigned multiply register with register to r0

    public void visit(LegacyInstr.NEG i); // two's complement register

    public void visit(LegacyInstr.NOP i); // do nothing operation

    public void visit(LegacyInstr.OR i); // or register with register

    public void visit(LegacyInstr.ORI i); // or register with immediate

    public void visit(LegacyInstr.OUT i); // write from register to IO register

    public void visit(LegacyInstr.POP i); // pop from the stack to register

    public void visit(LegacyInstr.PUSH i); // push register to the stack

    public void visit(LegacyInstr.RCALL i); // relative call

    public void visit(LegacyInstr.RET i); // return to caller

    public void visit(LegacyInstr.RETI i); // return from interrupt

    public void visit(LegacyInstr.RJMP i); // relative jump

    public void visit(LegacyInstr.ROL i); // rotate left through carry flag

    public void visit(LegacyInstr.ROR i); // rotate right through carry flag

    public void visit(LegacyInstr.SBC i); // subtract register from register with carry

    public void visit(LegacyInstr.SBCI i); // subtract immediate from register with carry

    public void visit(LegacyInstr.SBI i); // set bit in IO register

    public void visit(LegacyInstr.SBIC i); // skip if bit in IO register is clear

    public void visit(LegacyInstr.SBIS i); // skip if bit in IO register is set

    public void visit(LegacyInstr.SBIW i); // subtract immediate from word

    public void visit(LegacyInstr.SBR i); // set bits in register

    public void visit(LegacyInstr.SBRC i); // skip if bit in register cleared

    public void visit(LegacyInstr.SBRS i); // skip if bit in register set

    public void visit(LegacyInstr.SEC i); // set C (carry) flag

    public void visit(LegacyInstr.SEH i); // set H (half carry) flag

    public void visit(LegacyInstr.SEI i); // set I (interrupt enable) flag

    public void visit(LegacyInstr.SEN i); // set N (negative) flag

    public void visit(LegacyInstr.SER i); // set bits in register

    public void visit(LegacyInstr.SES i); // set S (signed) flag

    public void visit(LegacyInstr.SET i); // set T flag

    public void visit(LegacyInstr.SEV i); // set V (overflow) flag

    public void visit(LegacyInstr.SEZ i); // set Z (zero) flag

    public void visit(LegacyInstr.SLEEP i); // invoke sleep mode

    public void visit(LegacyInstr.SPM i); // store to program memory from r0

    public void visit(LegacyInstr.ST i); // store from register to SRAM

    public void visit(LegacyInstr.STD i); // store from register to SRAM with displacement

    public void visit(LegacyInstr.STPD i); // store from register to SRAM with pre-decrement

    public void visit(LegacyInstr.STPI i); // store from register to SRAM with post-increment

    public void visit(LegacyInstr.STS i); // store direct to SRAM

    public void visit(LegacyInstr.SUB i); // subtract register from register

    public void visit(LegacyInstr.SUBI i); // subtract immediate from register

    public void visit(LegacyInstr.SWAP i); // swap nibbles in register

    public void visit(LegacyInstr.TST i); // test for zero or minus

    public void visit(LegacyInstr.WDR i); // watchdog timer reset
//--END INSTRVISITOR GENERATOR--
}
