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
 * The <code>CodeRebuilder</code> interface represents a visitor that is more specific than the
 * <code>ExprVisitor</code> visitor, in that it contains visit methods for every type of arithmetic and
 * logical operation in the IR.
 *
 * @author Ben L. Titzer
 */
public class CodeRebuilder<Env> implements CodeAccumulator<Expr, Env> {

    public Expr visit(BinOpExpr e, Env env) {
        Expr left = e.left;
        Expr l = visitExpr(left, env);
        Expr r = visitExpr(e.right, env);
        return rebuild(e, l, r);
    }

    protected Expr rebuild(BinOpExpr e, Expr l, Expr r) {
        if (l != e.left || r != e.right) {
            BinOpExpr binOpExpr = new BinOpExpr(l, e.operation, r);
            binOpExpr.setBinOp(e.getBinOp());
            binOpExpr.setType(e.getType());
            return binOpExpr;
        }
        return e;
    }

    public Expr visit(IndexExpr e, Env env) {
        Expr i = visitExpr(e.expr, env);
        Expr j = visitExpr(e.index, env);
        if (i != e.expr || j != e.index) return new IndexExpr(i, j);
        return e;
    }

    public Expr visit(FixedRangeExpr e, Env env) {
        Expr o = visitExpr(e.expr, env);
        if (o != e.expr) return new FixedRangeExpr(o, e.low_bit, e.high_bit);
        return e;
    }

    public List<Expr> visitExprList(List<Expr> l, Env env) {
        List<Expr> nl = new LinkedList<Expr>();
        boolean changed = false;

        for ( Expr a : l ) {
            Expr na = visitExpr(a, env);
            if (na != a) changed = true;

            nl.add(na);
        }

        if (changed) return nl;
        return l;
    }

    public Expr visit(CallExpr e, Env env) {
        List<Expr> nargs = visitExprList(e.args, env);
        return rebuild(e, nargs);
    }

    protected Expr rebuild(CallExpr e, List<Expr> nargs) {
        if (nargs != e.args) {
            CallExpr callExpr = new CallExpr(e.method, nargs);
            callExpr.setDecl(e.getDecl());
            callExpr.setType(e.getType());
            return callExpr;
        }
        else
            return e;
    }

    public Expr visit(ReadExpr e, Env env) {
        // terminal node in the tree
        return e;
    }

    public Expr visit(ConversionExpr e, Env env) {
        Expr ne = visitExpr(e.expr, env);
        if (ne != e.expr) return new ConversionExpr(ne, e.typeRef);
        return e;
    }

    public Expr visit(Literal.BoolExpr e, Env env) {
        // terminal node in the tree
        return e;
    }

    public Expr visit(Literal.IntExpr e, Env env) {
        // terminal node in the tree
        return e;
    }

    public Expr visit(Literal.EnumVal e, Env env) {
        // terminal node in the tree
        return e;
    }

    public Expr visit(UnOpExpr e, Env env) {
        Expr ne = visitExpr(e.expr, env);
        return rebuild(e, ne);
    }

    protected Expr rebuild(UnOpExpr e, Expr ne) {
        if (ne != e.expr) {
            UnOpExpr unOpExpr = new UnOpExpr(e.operation, ne);
            unOpExpr.setUnOp(e.getUnOp());
            unOpExpr.setType(e.getType());
            return unOpExpr;
        }
        return e;
    }

    public Expr visit(VarExpr e, Env env) {
        // terminal node in the tree
        return e;
    }

    public Expr visit(DotExpr e, Env env) {
        // terminal node in the tree
        return e;
    }

    protected Expr visitExpr(Expr e, Env env) {
        Expr expr = e.accept(this, env);
        if ( expr.getType() == null ) expr.setType(e.getType());
        return expr;
    }


}
