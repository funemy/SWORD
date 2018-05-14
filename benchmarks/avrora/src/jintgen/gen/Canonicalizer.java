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
import jintgen.jigir.*;
import jintgen.types.Type;
import jintgen.types.TypeRef;
import java.util.List;

/**
 * @author Ben L. Titzer
 */
public class Canonicalizer extends CodeProcessor<Object> {

    int tempcount;

    public Canonicalizer() {
        super(new VarRenamer());
    }

    public Stmt visit(AssignStmt s, Object env) {
        Expr expr = s.expr;
        if ( s.dest instanceof IndexExpr ) {
            return transformIndexAssign((IndexExpr)s.dest, expr, env);
        } else if ( s.dest instanceof FixedRangeExpr ) {
            return transformFixedRangeAssign((FixedRangeExpr)s.dest, expr, env);
        } else if ( s.dest instanceof VarExpr ) {
            expr = visitExpr(expr, null);
            return new AssignStmt.Var(getVarExpr(s.dest), expr);
        } else {
            throw Util.failure("cannot canonicalize assign to "+s.dest.getClass());
        }
    }

    private VarExpr getVarExpr(Expr e) {
        return renamer.getVarExpr((VarExpr)e);
    }

    private Stmt transformFixedRangeAssign(FixedRangeExpr fre, Expr expr, Object env) {
        if ( fre.expr instanceof IndexExpr) {
            IndexExpr ind = (IndexExpr)fre.expr;
            if ( ind.expr.getType().isBasedOn("map") ) {
                VarExpr vmap = liftVar(ind.expr);
                VarExpr vind = liftVar(ind.index);
                IndexExpr index = new IndexExpr(vmap, vind);
                index.setType(ind.getType());
                VarExpr velem = extractExpr(index);
                expr = visitExpr(expr, null);
                addStmt(new AssignStmt.FixedRange(velem, fre.low_bit, fre.high_bit, expr));
                return new AssignStmt.Map(vmap, vind, velem);
            } else {
                throw Util.failure("cannot canonicalize fixed-range assign to bit access");
            }
        } else if ( fre.expr instanceof VarExpr ) {
            expr = visitExpr(expr, null);
            return new AssignStmt.FixedRange(getVarExpr(fre.expr), fre.low_bit, fre.high_bit, expr);
        } else {
            throw Util.failure("cannot canonicalize fixed-range assign to "+fre.expr.getClass());
        }
    }

    private Stmt transformIndexAssign(IndexExpr ind, Expr expr, Object env) {
        if ( ind.expr.getType().isBasedOn("map") ) {
            Expr inde = visitExpr(ind.expr, null);
            Expr indi = visitExpr(ind.index, null);
            expr = visitExpr(expr, null);
            return new AssignStmt.Map(inde, indi, expr);
        } else {
            VarExpr inde = liftVar(ind.expr);
            Expr indi = visitExpr(ind.index, null);
            expr = visitExpr(expr, null);
            return new AssignStmt.Bit(inde, indi, expr);
        }
    }

    protected VarExpr liftVar(Expr e) {
        Expr ne = visitExpr(e, null);

        if (ne.isVariable()) return getVarExpr(ne);
        else return extractExpr(ne);
    }

    private VarExpr extractExpr(Expr ne) {
        ne = visitExpr(ne, null);
        Type type = ne.getType();
        VarExpr ve = renamer.newTemp(type);
        addStmt(new DeclStmt(ve.variable, new TypeRef(type), ne));
        return ve;
    }

    public List<Stmt> process(List<Stmt> stmts) {
        return visitStmtList(stmts, null);
    }
}
