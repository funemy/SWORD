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

import java.util.HashMap;

/**
 * The <code>LegacyInstrSet</code> class contains static methods that allow the instruction set of the AVR
 * microcontroller to be accessed from one place.
 *
 * @author Ben L. Titzer
 */
public class LegacyInstrSet {

    private static final HashMap instructions = new HashMap(150);

    static {
//--BEGIN INSTRUCTIONSET GENERATOR--
        instructions.put("adc", LegacyInstr.ADC.prototype);
        instructions.put("add", LegacyInstr.ADD.prototype);
        instructions.put("adiw", LegacyInstr.ADIW.prototype);
        instructions.put("and", LegacyInstr.AND.prototype);
        instructions.put("andi", LegacyInstr.ANDI.prototype);
        instructions.put("asr", LegacyInstr.ASR.prototype);
        instructions.put("bclr", LegacyInstr.BCLR.prototype);
        instructions.put("bld", LegacyInstr.BLD.prototype);
        instructions.put("brbc", LegacyInstr.BRBC.prototype);
        instructions.put("brbs", LegacyInstr.BRBS.prototype);
        instructions.put("brcc", LegacyInstr.BRCC.prototype);
        instructions.put("brcs", LegacyInstr.BRCS.prototype);
        instructions.put("break", LegacyInstr.BREAK.prototype);
        instructions.put("breq", LegacyInstr.BREQ.prototype);
        instructions.put("brge", LegacyInstr.BRGE.prototype);
        instructions.put("brhc", LegacyInstr.BRHC.prototype);
        instructions.put("brhs", LegacyInstr.BRHS.prototype);
        instructions.put("brid", LegacyInstr.BRID.prototype);
        instructions.put("brie", LegacyInstr.BRIE.prototype);
        instructions.put("brlo", LegacyInstr.BRLO.prototype);
        instructions.put("brlt", LegacyInstr.BRLT.prototype);
        instructions.put("brmi", LegacyInstr.BRMI.prototype);
        instructions.put("brne", LegacyInstr.BRNE.prototype);
        instructions.put("brpl", LegacyInstr.BRPL.prototype);
        instructions.put("brsh", LegacyInstr.BRSH.prototype);
        instructions.put("brtc", LegacyInstr.BRTC.prototype);
        instructions.put("brts", LegacyInstr.BRTS.prototype);
        instructions.put("brvc", LegacyInstr.BRVC.prototype);
        instructions.put("brvs", LegacyInstr.BRVS.prototype);
        instructions.put("bset", LegacyInstr.BSET.prototype);
        instructions.put("bst", LegacyInstr.BST.prototype);
        instructions.put("call", LegacyInstr.CALL.prototype);
        instructions.put("cbi", LegacyInstr.CBI.prototype);
        instructions.put("cbr", LegacyInstr.CBR.prototype);
        instructions.put("clc", LegacyInstr.CLC.prototype);
        instructions.put("clh", LegacyInstr.CLH.prototype);
        instructions.put("cli", LegacyInstr.CLI.prototype);
        instructions.put("cln", LegacyInstr.CLN.prototype);
        instructions.put("clr", LegacyInstr.CLR.prototype);
        instructions.put("cls", LegacyInstr.CLS.prototype);
        instructions.put("clt", LegacyInstr.CLT.prototype);
        instructions.put("clv", LegacyInstr.CLV.prototype);
        instructions.put("clz", LegacyInstr.CLZ.prototype);
        instructions.put("com", LegacyInstr.COM.prototype);
        instructions.put("cp", LegacyInstr.CP.prototype);
        instructions.put("cpc", LegacyInstr.CPC.prototype);
        instructions.put("cpi", LegacyInstr.CPI.prototype);
        instructions.put("cpse", LegacyInstr.CPSE.prototype);
        instructions.put("dec", LegacyInstr.DEC.prototype);
        instructions.put("eicall", LegacyInstr.EICALL.prototype);
        instructions.put("eijmp", LegacyInstr.EIJMP.prototype);
        instructions.put("elpm", LegacyInstr.ELPM.prototype);
        instructions.put("elpmd", LegacyInstr.ELPMD.prototype);
        instructions.put("elpmpi", LegacyInstr.ELPMPI.prototype);
        instructions.put("eor", LegacyInstr.EOR.prototype);
        instructions.put("fmul", LegacyInstr.FMUL.prototype);
        instructions.put("fmuls", LegacyInstr.FMULS.prototype);
        instructions.put("fmulsu", LegacyInstr.FMULSU.prototype);
        instructions.put("icall", LegacyInstr.ICALL.prototype);
        instructions.put("ijmp", LegacyInstr.IJMP.prototype);
        instructions.put("in", LegacyInstr.IN.prototype);
        instructions.put("inc", LegacyInstr.INC.prototype);
        instructions.put("jmp", LegacyInstr.JMP.prototype);
        instructions.put("ld", LegacyInstr.LD.prototype);
        instructions.put("ldd", LegacyInstr.LDD.prototype);
        instructions.put("ldi", LegacyInstr.LDI.prototype);
        instructions.put("ldpd", LegacyInstr.LDPD.prototype);
        instructions.put("ldpi", LegacyInstr.LDPI.prototype);
        instructions.put("lds", LegacyInstr.LDS.prototype);
        instructions.put("lpm", LegacyInstr.LPM.prototype);
        instructions.put("lpmd", LegacyInstr.LPMD.prototype);
        instructions.put("lpmpi", LegacyInstr.LPMPI.prototype);
        instructions.put("lsl", LegacyInstr.LSL.prototype);
        instructions.put("lsr", LegacyInstr.LSR.prototype);
        instructions.put("mov", LegacyInstr.MOV.prototype);
        instructions.put("movw", LegacyInstr.MOVW.prototype);
        instructions.put("mul", LegacyInstr.MUL.prototype);
        instructions.put("muls", LegacyInstr.MULS.prototype);
        instructions.put("mulsu", LegacyInstr.MULSU.prototype);
        instructions.put("neg", LegacyInstr.NEG.prototype);
        instructions.put("nop", LegacyInstr.NOP.prototype);
        instructions.put("or", LegacyInstr.OR.prototype);
        instructions.put("ori", LegacyInstr.ORI.prototype);
        instructions.put("out", LegacyInstr.OUT.prototype);
        instructions.put("pop", LegacyInstr.POP.prototype);
        instructions.put("push", LegacyInstr.PUSH.prototype);
        instructions.put("rcall", LegacyInstr.RCALL.prototype);
        instructions.put("ret", LegacyInstr.RET.prototype);
        instructions.put("reti", LegacyInstr.RETI.prototype);
        instructions.put("rjmp", LegacyInstr.RJMP.prototype);
        instructions.put("rol", LegacyInstr.ROL.prototype);
        instructions.put("ror", LegacyInstr.ROR.prototype);
        instructions.put("sbc", LegacyInstr.SBC.prototype);
        instructions.put("sbci", LegacyInstr.SBCI.prototype);
        instructions.put("sbi", LegacyInstr.SBI.prototype);
        instructions.put("sbic", LegacyInstr.SBIC.prototype);
        instructions.put("sbis", LegacyInstr.SBIS.prototype);
        instructions.put("sbiw", LegacyInstr.SBIW.prototype);
        instructions.put("sbr", LegacyInstr.SBR.prototype);
        instructions.put("sbrc", LegacyInstr.SBRC.prototype);
        instructions.put("sbrs", LegacyInstr.SBRS.prototype);
        instructions.put("sec", LegacyInstr.SEC.prototype);
        instructions.put("seh", LegacyInstr.SEH.prototype);
        instructions.put("sei", LegacyInstr.SEI.prototype);
        instructions.put("sen", LegacyInstr.SEN.prototype);
        instructions.put("ser", LegacyInstr.SER.prototype);
        instructions.put("ses", LegacyInstr.SES.prototype);
        instructions.put("set", LegacyInstr.SET.prototype);
        instructions.put("sev", LegacyInstr.SEV.prototype);
        instructions.put("sez", LegacyInstr.SEZ.prototype);
        instructions.put("sleep", LegacyInstr.SLEEP.prototype);
        instructions.put("spm", LegacyInstr.SPM.prototype);
        instructions.put("st", LegacyInstr.ST.prototype);
        instructions.put("std", LegacyInstr.STD.prototype);
        instructions.put("stpd", LegacyInstr.STPD.prototype);
        instructions.put("stpi", LegacyInstr.STPI.prototype);
        instructions.put("sts", LegacyInstr.STS.prototype);
        instructions.put("sub", LegacyInstr.SUB.prototype);
        instructions.put("subi", LegacyInstr.SUBI.prototype);
        instructions.put("swap", LegacyInstr.SWAP.prototype);
        instructions.put("tst", LegacyInstr.TST.prototype);
        instructions.put("wdr", LegacyInstr.WDR.prototype);
//--END INSTRUCTIONSET GENERATOR--
    }

    /**
     * The <code>getPrototype()</code> method looks up the prototype for the given instruction name and
     * returns it.
     *
     * @param name the name (variant) of the instruction
     * @return an instruction prototype instance corresponding to that variant of the instruction
     */
    public static LegacyInstrProto getPrototype(String name) {
        return (LegacyInstrProto)instructions.get(name.toLowerCase());
    }

}
