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
import jintgen.isdl.parser.Token;
import jintgen.jigir.JIGIRTypeEnv;
import java.util.HashMap;
import java.util.List;

/**
 * @author Ben L. Titzer
 */
public abstract class VerifierPass {

    protected final ArchDecl arch;
    protected final JIGIRErrorReporter ERROR;
    protected final JIGIRTypeEnv typeEnv;

    protected VerifierPass(ArchDecl a) {
        arch = a;
        ERROR = a.ERROR;
        typeEnv = a.typeEnv;
    }

    public abstract void verify();

    protected void uniqueCheck(String type, String name, Iterable<? extends Item> it) {
        HashMap<String, Token> items = new HashMap<String, Token>();
        for ( Item i : it ) {
            String nm = i.name.image;
            if ( items.containsKey(nm) )
                ERROR.redefined(type, name, items.get(nm), i.name);
            items.put(nm, i.name);
        }
    }

    protected void resolveOperandTypes(List<AddrModeDecl.Operand> operands) {
        HashMap<String, Token> set = new HashMap<String, Token>();
        for ( AddrModeDecl.Operand o : operands ) {
            if ( set.containsKey(o.name.image) )
                ERROR.RedefinedOperand(o.name, set.get(o.name.image));
            set.put(o.name.image, o.name);
            o.typeRef.resolve(typeEnv);
        }
    }
}
