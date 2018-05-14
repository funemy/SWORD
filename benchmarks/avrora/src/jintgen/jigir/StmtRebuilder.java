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

package jintgen.jigir;

import java.util.LinkedList;
import java.util.List;

/**
 * The <code>StmtVisitor</code> interface implements the visitor pattern so that clients can visit the abstract syntax
 * tree nodes representing statements in the program.
 *
 * @author Ben L. Titzer
 */
public class StmtRebuilder<Env> extends CodeRebuilder<Env> implements StmtAccumulator<Stmt, Env> {

    List<Stmt> newList;
    boolean changed;

    public Stmt visit(CallStmt s, Env env) {
        List<Expr> na = visitExprList(s.args, env);
        if (na != s.args) return rebuild(s, na);
        else return s;
    }

    protected Stmt rebuild(CallStmt s, List<Expr> na) {
        CallStmt callStmt = new CallStmt(s.method, na);
        callStmt.setDecl(s.getDecl());
        return callStmt;
    }

    public Stmt visit(WriteStmt s, Env env) {
        Expr ne = visitExpr(s.expr, env);
        if ( ne != s.expr ) return rebuild(s, ne);
        return s;
    }

    protected WriteStmt rebuild(WriteStmt s, Expr ne) {
        WriteStmt writeStmt = new WriteStmt(s.method, s.typeRef, s.operand, ne);
        writeStmt.setAccessor(s.getAccessor());
        return writeStmt;
    }

    public Stmt visit(CommentStmt s, Env env) {
        return s;
    }

    public Stmt visit(DeclStmt s, Env env) {
        Expr ni = visitExpr(s.init, env);
        if (ni != s.init)
            return new DeclStmt(s.name, s.typeRef, ni);
        else
            return s;
    }

    public Stmt visit(IfStmt s, Env env) {
        Expr nc = visitExpr(s.cond, env);
        List<Stmt> nt = visitStmtList(s.trueBranch, env);
        List<Stmt> nf = visitStmtList(s.falseBranch, env);

        if (nc != s.cond || nt != s.trueBranch || nf != s.falseBranch)
            return new IfStmt(nc, nt, nf);
        else
            return s;
    }

    public List<Stmt> visitStmtList(List<Stmt> l, Env env) {
        List<Stmt> oldList = this.newList;
        boolean oldChanged = changed;
        newList = new LinkedList<Stmt>();
        changed = false;

        visitStmts(l, env);

        if (changed) l = newList;
        this.newList = oldList;
        changed = oldChanged;
        return l;
    }

    protected void visitStmts(List<Stmt> l, Env env) {
        for (Stmt sa : l) {
            Stmt na = sa.accept(this, env);
            if (na != sa) changed = true;
            if (na != null)
                newList.add(na);
        }
    }

    protected void addStmt(Stmt s) {
        newList.add(s);
        changed = true;
    }

    public Stmt visit(AssignStmt s, Env env) {
        Expr ni = visitExpr(s.dest, env);
        Expr ne = visitExpr(s.expr, env);
        if (ni != s.dest || ne != s.expr)
            return new AssignStmt(ni, ne);
        else
            return s;
    }

    public Stmt visit(AssignStmt.Var s, Env env) {
        VarExpr ni = (VarExpr)visitExpr(s.dest, env);
        Expr ne = visitExpr(s.expr, env);
        if (ni != s.dest || ne != s.expr)
            return new AssignStmt.Var(ni, ne);
        else
            return s;
    }

    public Stmt visit(AssignStmt.Map s, Env env) {
        VarExpr nm = (VarExpr)visitExpr(s.map, env);
        Expr ni = visitExpr(s.index, env);
        Expr ne = visitExpr(s.expr, env);
        if (nm != s.map || ni != s.index || ne != s.expr)
            return new AssignStmt.Map(nm, ni, ne);
        else
            return s;
    }

    public Stmt visit(AssignStmt.Bit s, Env env) {
        VarExpr ni = (VarExpr)visitExpr(s.dest, env);
        Expr nb = visitExpr(s.bit, env);
        Expr ne = visitExpr(s.expr, env);
        if (ni != s.dest || nb != s.bit || ne != s.expr)
            return new AssignStmt.Bit(ni, nb, ne);
        else
            return s;
    }

    public Stmt visit(AssignStmt.FixedRange s, Env env) {
        VarExpr ni = (VarExpr)visitExpr(s.dest, env);
        Expr ne = visitExpr(s.expr, env);
        if (ni != s.dest || ne != s.expr)
            return new AssignStmt.FixedRange(ni, s.low_bit, s.high_bit, ne);
        else
            return s;
    }

    public Stmt visit(ReturnStmt s, Env env) {
        Expr ne = visitExpr(s.expr, env);
        if (ne != s.expr)
            return new ReturnStmt(ne);
        else
            return s;
    }
}
