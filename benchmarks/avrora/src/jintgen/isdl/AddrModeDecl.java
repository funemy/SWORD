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

package jintgen.isdl;

import jintgen.isdl.parser.Token;
import jintgen.jigir.Decl;
import jintgen.types.Type;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Ben L. Titzer
 */
public class AddrModeDecl extends Item {

    public static class Operand implements Decl {
        public final Token name;
        public final OperandTypeRef typeRef;

        public Operand(Token n, OperandTypeRef t) {
            name = n;
            typeRef = t;
        }

        public OperandTypeDecl getOperandType() {
            return typeRef.getOperandTypeDecl();
        }

        public String getName() {
            return name.image;
        }

        public Type getType() {
            return typeRef.getType();
        }
    }

    public final List<Operand> operands;
    public final List<FormatDecl> encodings;
    public final List<AddrModeSetDecl> sets;

    public AddrModeDecl(Token n, List<Operand> ol) {
        super(n);
        operands = ol;
        encodings = new LinkedList<FormatDecl>();
        sets = new LinkedList<AddrModeSetDecl>();
    }

    public void addEncoding(FormatDecl d) {
        encodings.add(d);
    }

    public void joinSet(AddrModeSetDecl d) {
        sets.add(d);
    }

}
