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
 * Creation date: Nov 11, 2005
 */

package jintgen.isdl.verifier;

import jintgen.isdl.*;
import java.util.LinkedList;

/**
 * The <code>InstrVerifier</code> class verifies that each of the instructions is well formed.
 * 
 * @author Ben L. Titzer
 */
public class InstrVerifier extends VerifierPass {

    public InstrVerifier(ArchDecl arch) {
        super(arch);
    }

    public void verify() {
        uniqueCheck("Instr", "Instruction", arch.instructions);
        verifyInstructions();
    }


    void verifyInstructions() {
        for ( InstrDecl id : arch.instructions ) {
            // verify the addressing mode use
            if ( id.addrMode.ref != null ) {
                resolveAddressingModeRef(id.addrMode);
            } else {
                resolveOperandTypes(id.addrMode.localDecl.operands);
            }
        }
    }

    private void resolveAddressingModeRef(AddrModeUse am) {
        AddrModeSetDecl asd = arch.getAddressingModeSet(am.ref.image);
        if ( asd == null ) {
            AddrModeDecl amd = arch.getAddressingMode(am.ref.image);
            if ( amd == null ) {
                ERROR.UnresolvedAddressingMode(am.ref);
            } else {
                am.operands = amd.operands;
                am.addrModes = new LinkedList<AddrModeDecl>();
                am.addrModes.add(amd);
            }
        } else {
            am.operands = asd.unionOperands;
            am.addrModes = asd.addrModes;
        }
    }

}
