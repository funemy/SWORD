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

package jintgen.gen;

import cck.util.Util;
import jintgen.isdl.ArchDecl;
import jintgen.isdl.SubroutineDecl;
import jintgen.isdl.parser.ISDLParserConstants;
import jintgen.isdl.parser.Token;
import jintgen.jigir.*;
import java.util.*;

/**
 * The <code>Inliner</code> class implements a visitor over the code that inlines calls to known subroutines.
 * This produces code that is free of calls to the subroutines declared within the architecture description
 * and therefore is ready for constant and copy propagation optimizations.
 * <p/>
 * The <code>Inliner</code> will aggressively inline all calls, therefore it cannot detect recursion. It
 * assumes that return statements are at the end of subroutines and do not occur in branches. This is not
 * enforced by any checking, which should be done in the future.
 *
 * @author Ben L. Titzer
 */
public class Inliner extends StmtRebuilder<Object> {
    int tmpCount;

    private ArchDecl archDecl;

    Context context;

    private class Context {
        Context caller;
        String returnTemp;
        SubroutineDecl curSubroutine;
        HashMap<String, String> varMap;

        Context(Context c) {
            caller = c;
            varMap = new HashMap<String, String>();
        }

        String newTemp(String orig) {
            String nn = "tmp_" + (tmpCount++);

            if (orig != null) varMap.put(orig, nn);
            return nn;
        }

        String varName(String n) {
            String nn = varMap.get(n);
            if (nn == null && caller != null) nn = caller.varName(n);
            if (nn == null) return n;
            return nn;
        }
    }


    public Inliner(ArchDecl archDecl) {
        this.archDecl = archDecl;
        context = new Context(null);
    }

    public List<Stmt> process(List<Stmt> l) {
        return visitStmtList(l, null);
    }

    public Stmt visit(CallStmt s, Object env) {
        SubroutineDecl d = archDecl.getSubroutine(s.method.image);
        if (shouldNotInline(d)) {
            return super.visit(s, env);
        } else {
            inlineCall(s.method, d, s.args);
            return null;
        }
    }

    public Stmt visit(AssignStmt s, Object env) {
        throw Util.unimplemented();
    }

    public Stmt visit(DeclStmt s, Object env) {
        String nv = newTemp(s.name.image);
        return (new DeclStmt(newToken(nv), s.typeRef, s.init.accept(this, env)));
    }

    public Stmt visit(ReturnStmt s, Object env) {
        if (context.curSubroutine == null)
            throw Util.failure("return not within subroutine!");

        context.returnTemp = newTemp(null);
        return (new DeclStmt(newToken(context.returnTemp), context.curSubroutine.ret, s.expr.accept(this, env)));
    }


    protected String newTemp(String orig) {
        return context.newTemp(orig);
    }

    protected String inlineCall(Token m, SubroutineDecl d, List<Expr> args) {
        if (d.params.size() != args.size())
            Util.failure("arity mismatch in call to " + m.image + " @ " + m.beginLine + ':' + m.beginColumn);

        Context nc = new Context(context);

        Iterator<Expr> arg_iter = args.iterator();
        for ( SubroutineDecl.Parameter p : d.getParams() ) {
            Expr e = arg_iter.next();

            // get a new temporary
            String nn = nc.newTemp(p.name.image);

            // alpha-rename expression that is argument
            Expr ne = e.accept(this, null);
            addStmt(new DeclStmt(nn, p.type, ne));
        }

        // set the current subroutine
        nc.curSubroutine = d;
        context = nc;
        // process body
        visitStmts(d.code.getStmts(), null);
        context = nc.caller;

        return nc.returnTemp;
    }


    public Expr visit(CallExpr v, Object env) {
        SubroutineDecl d = archDecl.getSubroutine(v.method.image);
        if (shouldNotInline(d)) {
            return super.visit(v, null);
        } else {
            String result = inlineCall(v.method, d, v.args);
            return new VarExpr(result);
        }
    }

    protected boolean shouldNotInline(SubroutineDecl d) {
        return !ArchDecl.INLINE || d == null || !d.inline || !d.code.hasBody();
    }

    public Expr visit(VarExpr v, Object env) {
        // alpha rename all variables
        return new VarExpr(varName(v.variable));
    }

    protected String varName(String n) {
        return context.varName(n);
    }

    protected String varName(Token n) {
        return varName(n.image);
    }

    protected Token newToken(String t) {
        Token tk = new Token();
        tk.image = t;
        tk.kind = ISDLParserConstants.IDENTIFIER;
        return tk;
    }
}
