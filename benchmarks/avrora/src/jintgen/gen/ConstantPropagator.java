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

import cck.util.Arithmetic;
import cck.util.Util;
import jintgen.jigir.*;
import java.util.*;

/**
 * @author Ben L. Titzer
 */
public class ConstantPropagator extends StmtRebuilder<ConstantPropagator.Environ> {

    protected static Literal.IntExpr ZERO = new Literal.IntExpr(0);
    protected static Literal.IntExpr ONE = new Literal.IntExpr(1);
    protected static Literal.BoolExpr TRUE = new Literal.BoolExpr(true);
    protected static Literal.BoolExpr FALSE = new Literal.BoolExpr(false);

    protected static HashSet<String> trackedMaps;

    public class Environ {
        Environ parent;
        HashMap<String, Expr> constantMap;
        HashMap<String, HashMap<Integer, Expr>> mapMap; // HashMap<String, HashMap>

        Environ(Environ p) {
            parent = p;
            constantMap = new HashMap<String, Expr>();
            mapMap = new HashMap<String, HashMap<Integer, Expr>>();
        }

        public Expr lookup(String name) {
            Expr e = constantMap.get(name);
            if (e != null) return e;
            if (parent != null)
                return parent.lookup(name);
            else
                return null;
        }

        public void put(String name, Expr e) {
            if (e instanceof Literal.IntExpr) {
                int ival = intValueOf(e);
                if (ival == 0)
                    e = ZERO;
                else if (ival == 1) e = ONE;
            } else if (e instanceof Literal.BoolExpr) {
                boolean bval = boolValueOf(e);
                e = bval ? TRUE : FALSE;
            }
            constantMap.put(name, e);
        }

        public void remove(String name) {
            constantMap.remove(name);
            if (parent != null) parent.remove(name);
        }

        Expr lookupMap(String name, int index) {
            if (!trackedMaps.contains(name)) return null;

            return lookupMap_fast(name, index);
        }

        private Expr lookupMap_fast(String name, int index) {
            HashMap<Integer, Expr> map = mapMap.get(name);
            if (map != null) {
                Expr e = map.get(new Integer(index));
                if (e != null) return e;
            }

            if (parent != null) {
                return parent.lookupMap_fast(name, index);
            }

            return null;
        }

        void putMap(String mapname, int index, Expr e) {
            if (!trackedMaps.contains(mapname)) return;

            HashMap<Integer, Expr> map = mapMap.get(mapname);
            if (map == null) {
                map = new HashMap<Integer, Expr>();
                mapMap.put(mapname, map);
            }

            map.put(new Integer(index), e);

        }

        void removeMap(String mapname, int index) {
            if (!trackedMaps.contains(mapname)) return;

            HashMap<Integer, Expr> map = mapMap.get(mapname);
            if (map != null) {
                mapMap.remove(new Integer(index));
            }

            if (parent != null) parent.removeMap(mapname, index);
        }

        void removeAll(String mapname) {
            if (!trackedMaps.contains(mapname)) return;

            mapMap.remove(mapname);

            if (parent != null) parent.removeAll(mapname);
        }

        void mergeToParent(Environ sibling) {
            mergeIntoParent(this, sibling);
            mergeIntoParent(sibling, this);
        }

        void mergeIntoParent(Environ a, Environ b) {
            for ( String key : a.constantMap.keySet() ) {
                Expr e = a.lookup(key);
                Expr o = b.lookup(key);
                // TODO: reference equality is just a first-order approximation of expression equality
                if (e == o)
                    a.parent.put(key, e);
                else
                    a.parent.remove(key);
            }
        }
    }

    public ConstantPropagator() {
        trackedMaps = new HashSet<String>();
        trackedMaps.add("regs"); // this is all for now
    }

    public Environ createEnvironment() {
        return new Environ(null);
    }

    public List<Stmt> process(List<Stmt> stmts) {
        return visitStmtList(stmts, new Environ(null));
    }

    public Stmt visit(DeclStmt s, Environ cenv) {
        Expr ne = update(s.name.toString(), s.init, cenv);
        if (s.init != ne)
            return new DeclStmt(s.name, s.typeRef, ne);
        else
            return s;
    }

    public Stmt visit(AssignStmt s, Environ cenv) {
        throw Util.unimplemented();
    }

    private Expr update(String name, Expr val, Environ cenv) {
        Expr ne = val.accept(this, cenv);
        if (ne.isLiteral()) {
            // propagate this constant forward
            cenv.put(name, ne);
        } else if (ne.isVariable()) {
            VarExpr ve = (VarExpr)ne;
            Expr e = cenv.lookup(ve.variable.toString());
            if (e != null) {
                // propagate the constant
                cenv.put(name, e);
            } else {
                // propagate the copy
                cenv.put(name, ve);
            }
        } else {
            // complex expression: remove from constant map
            cenv.remove(name);
        }
        return ne;
    }

    public Expr visit(VarExpr e, Environ cenv) {
        Expr ce = cenv.lookup(e.variable.toString());
        if (ce != null)
            return ce;
        else
            return e;
    }

    public Expr visit(IndexExpr e, Environ cenv) {
        Expr nexpr = e.expr.accept(this, cenv);
        Expr nbit = e.index.accept(this, cenv);

        if (nexpr.isLiteral() && nbit.isLiteral()) {
            int eval = intValueOf(nexpr);
            int bval = intValueOf(nbit);
            return new Literal.BoolExpr(Arithmetic.getBit(eval, bval));
        }

        if (nexpr != e.expr || nbit != e.index)
            return new IndexExpr(nexpr, nbit);
        else
            return e;
    }

    public Expr visit(FixedRangeExpr e, Environ cenv) {
        Expr nexpr = e.expr.accept(this, cenv);

        if (nexpr.isLiteral()) {
            int eval = intValueOf(nexpr);
            int mask = Arithmetic.getBitRangeMask(e.low_bit, e.high_bit);
            return new Literal.IntExpr((eval & mask) >> e.low_bit);
        }

        if (nexpr != e.expr)
            return new FixedRangeExpr(nexpr, e.low_bit, e.high_bit);
        else
            return e;
    }

    // --- binary operations ---

    public Expr visit(BinOpExpr e, Environ cenv) {
        Expr l = e.left.accept(this, cenv);
        Expr r = e.right.accept(this, cenv);

        if (l.isLiteral() && r.isLiteral()) {
            return e.getBinOp().evaluate((Literal)l, (Literal)r);
        }

        return rebuild(e, l, r);
    }

    public Expr visit(Literal.BoolExpr e, Environ cenv) {
        if (e.value)
            return TRUE;
        else
            return FALSE;
    }

    public Expr visit(Literal.IntExpr e, Environ cenv) {
        if (e.value == 0)
            return ZERO;
        else if (e.value == 1)
            return ONE;
        else
            return e;
    }

    public Expr visit(UnOpExpr e, Environ cenv) {
        Expr ne = e.expr.accept(this, cenv);

        if ( ne.isLiteral() ) return e.getUnOp().evaluate((Literal)ne);

        return rebuild(e, ne);
    }

    // --- utilities ---

    private int intValueOf(Expr nexpr) {
        if ( nexpr instanceof Literal.BoolExpr ) return boolValueOf(nexpr) ? 1 : 0;
        return ((Literal.IntExpr)nexpr).value;
    }

    private boolean boolValueOf(Expr nexpr) {
        return ((Literal.BoolExpr)nexpr).value;
    }
}
