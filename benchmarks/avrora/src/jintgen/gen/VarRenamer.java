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
 * Created Oct 25, 2005
 */
package jintgen.gen;

import jintgen.isdl.GlobalDecl;
import jintgen.isdl.parser.Token;
import jintgen.jigir.*;
import jintgen.types.Type;
import java.util.HashMap;

/**
 * @author Ben L. Titzer
 */
public class VarRenamer {

    protected final HashMap<String, String> varMap;
    int tmpcount;

    VarRenamer() {
        varMap = new HashMap<String, String>();
    }

    public VarExpr getVarExpr(VarExpr e) {
        Decl decl = e.getDecl();
        if ( decl == null || !(decl instanceof GlobalDecl) ) {
            String orig = e.variable.image;
            String nn = varMap.get(orig);
            if ( nn != null && !orig.equals(nn) ) {
                VarExpr ne = new VarExpr(nn);
                ne.setDecl(decl);
                ne.setType(e.getType());
                return ne;
            }
        }
        return e;
    }

    public ReadExpr getReadExpr(ReadExpr e) {
        String orig = e.operand.image;
        String nn = varMap.get(orig);
        if ( nn != null && !orig.equals(nn) ) {
            Token tok = new Token();
            tok.image = nn;
            ReadExpr ne = new ReadExpr(e.method, e.typeRef, tok);
            ne.setType(e.getType());
            ne.setAccessor(e.getAccessor());
            return ne;
        }
        return e;
    }

    public VarExpr visit(String v) {
        String nn = varMap.get(v);
        if ( !v.equals(nn) ) v = nn;
        return new VarExpr(v);
    }

    public void addVariable(String v, String nv) {
        varMap.put(v, nv);
    }

    public DeclStmt getDecl(DeclStmt s) {
        // default: do nothing
        return s;
    }

    public WriteStmt getWrite(WriteStmt s) {
        String orig = s.operand.image;
        String nn = varMap.get(orig);
        if ( nn != null && !orig.equals(nn) ) {
            Token tok = new Token();
            tok.image = nn;
            WriteStmt writeStmt = new WriteStmt(s.method, s.typeRef, tok, s.expr);
            writeStmt.setAccessor(s.getAccessor());
            return writeStmt;
        }
        return s;
    }

    public VarExpr newTemp(Type t) {
        Token tok = new Token();
        tok.image = "$tmp_" + (tmpcount++);
        VarExpr varExpr = new VarExpr(tok);
        varExpr.setType(t);
        return varExpr;
    }
}
