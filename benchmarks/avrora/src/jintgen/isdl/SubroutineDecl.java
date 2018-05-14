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
import jintgen.jigir.*;
import jintgen.types.Type;
import jintgen.types.TypeRef;
import java.util.List;

/**
 * The <code>SubroutineDecl</code> class represents the declaration of a subroutine within the instruction set
 * description. A subroutine has a list of formal parameters and a return type as well as a list of statements
 * that represent its body.
 *
 * @author Ben L. Titzer
 */
public class SubroutineDecl extends Item {

    public static class Parameter implements Decl {
        public final Token name;
        public final TypeRef type;

        public Parameter(Token n, TypeRef t) {
            name = n;
            type = t;
        }

        public String getName() {
            return name.image;
        }

        public Type getType() {
            return type.getType();
        }

    }

    public final TypeRef ret;
    public final boolean inline;
    public final CodeRegion code;
    public final List<Parameter> params;

    public SubroutineDecl(boolean i, Token n, List<Parameter> pl, TypeRef r, List<Stmt> s) {
        super(n);
        inline = i;
        ret = r;
        params = pl;
        code = new CodeRegion(s);
    }

    public List<Parameter> getParams() {
        return params;
    }

}
