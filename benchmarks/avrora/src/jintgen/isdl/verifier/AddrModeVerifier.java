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
 * Creation date: Nov 3, 2005
 */

package jintgen.isdl.verifier;

import jintgen.isdl.*;
import jintgen.jigir.JIGIRTypeEnv;
import jintgen.types.Type;
import jintgen.types.TypeCon;

/**
 * @author Ben L. Titzer
 */
public class AddrModeVerifier extends VerifierPass {

    public AddrModeVerifier(ArchDecl arch) {
        super(arch);
    }

    public void verify() {
        uniqueCheck("AddrMode", "Addressing mode", arch.addrModes);
        uniqueCheck("AddrModeSet", "AddressingModeSet", arch.addrSets);
        uniqueCheck("Instr", "Instruction", arch.instructions);

        verifyOperands();
        verifyAddrModes();
    }

    void verifyOperands() {
        for ( OperandTypeDecl od : arch.operandTypes ) {
            if ( od.isCompound() ) {
                OperandTypeDecl.Compound cd = (OperandTypeDecl.Compound)od;
                resolveOperandTypes(cd.subOperands);
            } else {
                OperandTypeDecl.Value vd = (OperandTypeDecl.Value)od;
                Type t = vd.typeRef.resolve(typeEnv);
                TypeCon tc = t.getTypeCon();
                if ( tc instanceof JIGIRTypeEnv.TYPE_operand )
                    ERROR.ValueTypeExpected(vd.typeRef);
            }
        }
    }

    void verifyAddrModes() {
        for ( AddrModeDecl am : arch.addrModes ) {
            resolveOperandTypes(am.operands);
        }
    }

}
