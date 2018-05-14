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

import jintgen.jigir.*;
import java.util.List;

/**
 * @author Ben L. Titzer
 */
public abstract class CodeProcessor<Env> extends StmtRebuilder<Env> {

    protected final VarRenamer renamer;

    public CodeProcessor(VarRenamer rn) {
        renamer = rn;
    }

    public Expr visit(VarExpr e, Env env) {
        return renamer.getVarExpr(e);
    }

    public Expr visit(ReadExpr e, Env env) {
        return renamer.getReadExpr(e);
    }

    public Stmt visit(DeclStmt s, Env env) {
        return renamer.getDecl((DeclStmt)super.visit(s, env));
    }

    public Stmt visit(WriteStmt s, Env env) {
        return renamer.getWrite((WriteStmt)super.visit(s, env));
    }

    public abstract List<Stmt> process(List<Stmt> l);

    public void renameVariable(String orig, String n) {
        renamer.addVariable(orig, n);
    }
}
