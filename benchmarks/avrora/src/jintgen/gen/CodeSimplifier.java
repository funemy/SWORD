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
 * Creation date: Oct 24, 2005
 */

package jintgen.gen;

import cck.util.Util;
import cck.util.Arithmetic;
import jintgen.isdl.*;
import jintgen.isdl.parser.Token;
import jintgen.jigir.*;
import jintgen.types.*;
import java.util.*;

/**
 * The <code>CodeSimplifier</code> class simplifies ISDL code by translating code that has
 * bit accesses, reads, writes, conversions, etc. into simpler code that only
 * contains masks, shifts, and subroutine calls.
 *
 * @author Ben L. Titzer
 */
public class CodeSimplifier extends StmtRebuilder<CGEnv> {

    protected final ArchDecl arch;
    protected final Type INT;
    protected final Type LONG;

    public CodeSimplifier(ArchDecl a) {
        arch = a;
        INT = arch.typeEnv.newIntType(true, 32);
        LONG = arch.typeEnv.newIntType(true, 64);
    }

    public void genAccessMethods() {
        for ( OperandTypeDecl ot : arch.operandTypes ) {

            for ( OperandTypeDecl.AccessMethod m : ot.readDecls ) {
                List<SubroutineDecl.Parameter> p = new LinkedList<SubroutineDecl.Parameter>();
                Canonicalizer canon = addRenames(ot);
                Token name = token("$read_" + getTypeString(m.type));
                p.add(new SubroutineDecl.Parameter(token("_this"), newTypeRef(ot)));
                List<Stmt> stmts = canon.process(m.code.getStmts());
                SubroutineDecl d = new SubroutineDecl(true, name, p, m.typeRef, stmts);
                arch.addSubroutine(d);
                m.setSubroutine(d);
            }
            for ( OperandTypeDecl.AccessMethod m : ot.writeDecls ) {
                List<SubroutineDecl.Parameter> p = new LinkedList<SubroutineDecl.Parameter>();
                Canonicalizer canon = addRenames(ot);
                p.add(new SubroutineDecl.Parameter(token("_this"), newTypeRef(ot)));
                p.add(new SubroutineDecl.Parameter(token("value"), m.typeRef));
                Token name = token("$write_" + getTypeString(m.type));
                List<Stmt> stmts = canon.process(m.code.getStmts());
                SubroutineDecl d = new SubroutineDecl(true, name, p, newTypeRef(token("void")), stmts);
                arch.addSubroutine(d);
                m.setSubroutine(d);
            }
        }
    }

    private Canonicalizer addRenames(OperandTypeDecl ot) {
        Canonicalizer canon = new Canonicalizer();
        for ( AddrModeDecl.Operand o : ot.subOperands)
            canon.renameVariable(o.name.image, "_this."+o.name);
        canon.renameVariable("this", "_this.value");
        return canon;
    }

    private TypeRef newTypeRef(OperandTypeDecl ot) {
        Token name = ot.name;
        return newTypeRef(name);
    }

    private TypeRef newTypeRef(Token name) {
        TypeRef ref = new TypeRef(name);
        ref.resolve(arch.typeEnv);
        return ref;
    }

    public static String getTypeString(Type t) {
        if ( t instanceof JIGIRTypeEnv.TYPE_int ) {
            JIGIRTypeEnv.TYPE_int it = (JIGIRTypeEnv.TYPE_int)t;
            String base = it.isSigned() ? "int" : "uint";
            return base+it.getSize();
        } else return t.getTypeCon().getName();
    }

    public Expr visit(BinOpExpr e, CGEnv env) {
        BinOpExpr.BinOpImpl b = e.getBinOp();
        // TODO: catch special cases of shifting, masking, etc
        // TODO: transform boolean operations to Java operations
        Expr nl = promote(e.left);
        Expr nr = promote(e.right);
        return convert(rebuild(e, nl, nr), env.expect, env.shift);
    }

    public Expr visit(UnOpExpr e, CGEnv env) {
        UnOpExpr.UnOpImpl unop = e.getUnOp();
        if ( unop instanceof Arith.UNSIGN ) {
            JIGIRTypeEnv.TYPE_int it = (JIGIRTypeEnv.TYPE_int)e.expr.getType();
            Expr inner = convert(promote(e.expr), arch.typeEnv.newIntType(false, it.getSize()), env.shift);
            return convert(inner, env.expect, 0);
        } else {
            Expr no = promote(e.expr, env.expect, 0);
            return convert(rebuild(e, no), env.expect, env.shift);
        }
    }

    public Expr visit(IndexExpr e, CGEnv env) {
        if ( isMap(e.expr) ) {
            // translate map access into a call to map_get
            CallExpr ce = newCallExpr(token("map_get"), promote(e.expr), promote(e.index));
            ce.setType(e.getType());
            return convert(ce, env.expect, env.shift);
        } else {
            // translate bit access into a mask and shift
            // TODO: transform bit access more efficiently
            CallExpr ce = newCallExpr(token("bit_get"), promote(e.expr), promote(e.index));
            ce.setType(e.getType());
            return convert(ce, env.expect, env.shift);
        }
    }

    public Expr visit(FixedRangeExpr e, CGEnv env) {
        JIGIRTypeEnv.TYPE_int t = (JIGIRTypeEnv.TYPE_int)env.expect;
        int width = e.high_bit - e.low_bit + 1;
        if ( t.getSize() < width ) width = t.getSize();
        int mask = Arithmetic.getBitRangeMask(env.shift, env.shift + width - 1);
        Expr ne = promote(e.expr, INT, env.shift - e.low_bit);
        return newAnd(ne, mask, e.getType());
    }

    private BinOpExpr newAnd(Expr l, int mask, Type t) {
        BinOpExpr binop = new BinOpExpr(l, token("&"), newLiteralInt(mask));
        binop.setBinOp(arch.typeEnv.AND);
        binop.setType(t);
        return binop;
    }

    public List<Expr> visitExprList(List<Expr> l, CGEnv env) {
        throw Util.failure("should not rebuild expr list directly");
    }

    public Expr visit(CallExpr e, CGEnv env) {
        List<Expr> na = rebuildParams(e.getDecl().params, e.args);
        return convert(rebuild(e, na), env.expect, env.shift);
    }

    private List<Expr> rebuildParams(List<SubroutineDecl.Parameter> params, List<Expr> args) {
        List<Expr> na = new LinkedList<Expr>();
        Iterator<SubroutineDecl.Parameter> pi = params.iterator();
        for ( Expr a : args ) {
            SubroutineDecl.Parameter p = pi.next();
            na.add(promote(a, p.type.resolve(arch.typeEnv), 0));
        }
        return na;
    }

    public Expr visit(ReadExpr e, CGEnv env) {
        OperandTypeDecl.Accessor accessor = e.getAccessor();
        if ( !accessor.polymorphic ) {
            SubroutineDecl s = accessor.getSubroutine();
            CallExpr ce = newCallExpr(s, new VarExpr(e.operand));
            ce.setDecl(s);
            return ce;
        } else {
            return newCallExpr(token("$read_poly_"+ getTypeString(accessor.type)), new VarExpr(e.operand));
        }
    }

    public Expr visit(ConversionExpr e, CGEnv env) {
        Type rt = env.expect;

        Expr inner = promote(e.expr);
        TypeCon typecon = inner.getType().getTypeCon();
        if ( typecon instanceof JIGIRTypeEnv.TYPE_enum ) {
            if ( e.getType() instanceof JIGIRTypeEnv.TYPE_int ) {
                inner = new DotExpr(inner, token("value"));
                inner.setType(INT);
            } else throw Util.failure("cannot convert enum to type other than int");
        } else if ( typecon instanceof JIGIRTypeEnv.TYPE_operand ) {
            OperandTypeDecl decl = ((JIGIRTypeEnv.TYPE_operand)typecon).decl;
            if ( decl.isValue() ) {
                OperandTypeDecl.Value vt = (OperandTypeDecl.Value)decl;
                Type ovt = vt.typeRef.getType();
                inner = new DotExpr(e.expr, token("value"));
                inner.setType(ovt);

		if (ovt.getTypeCon() instanceof JIGIRTypeEnv.TYPE_enum) {
		    // Enums need to retain the conversion expression to
		    // generate the explicit type cast when generating 
		    // java code.
		    inner = new ConversionExpr(inner, vt.typeRef);
		    inner.setType(ovt);
		}
            } else throw Util.failure("cannot convert complex operand to any other type");
        } else {
            inner = convert(inner, e.getType(), env.shift);
            env.shift = 0;
        }

        return convert(inner, rt, env.shift);
    }

    public Expr visit(Literal.BoolExpr e, CGEnv env) {
        if ( env.expect == arch.typeEnv.BOOLEAN ) {
            if ( env.shift != 0 ) throw Util.failure("invalid expected shift of boolean at "+e.getSourcePoint());
            return e;
        }
        else if ( env.expect.isBasedOn("int") ) {
            Literal.IntExpr ne = newLiteralInt(1 << env.shift);
            ne.setType(env.expect);
            return ne;
        } else
            throw Util.failure("unexpected promotion type for boolean literal at "+e.getSourcePoint());
    }

    public Expr visit(Literal.IntExpr e, CGEnv env) {
        if ( env.expect.isBasedOn("int") ) {
            Literal.IntExpr ne = newLiteralInt(e.value << env.shift);
            ne.setType(env.expect);
            return ne;
        } else
            throw Util.failure("unexpected promotion type for int literal at "+e.getSourcePoint());
    }

    public Expr visit(VarExpr e, CGEnv env) {
        return convert(e, env.expect, env.shift);
    }

    public Expr visit(DotExpr e, CGEnv env) {
        Expr ne = promote(e.expr);
        DotExpr de = new DotExpr(ne, e.field);
        de.setType(e.getType());
        return convert(de, env.expect, env.shift);
    }

    public Expr convert(Expr e, Type tt, int shift) {
        Type ft = e.getType();

        if ( ft == tt ) {
            return shift(e, shift);
        } else {
            // conversion between types is necessary
            if ( ft.isBasedOn("boolean") ) {
                if ( tt.isBasedOn("int") ) {
                    CallExpr ce = newCallExpr(token("b2i"), e, newLiteralInt(1 << shift));
                    ce.setType(tt);
                    return ce;
                } else {
                    throw Util.failure("cannot convert boolean to other type");
                }
            } else if ( ft.isBasedOn("int") && tt.isBasedOn("int") ) {
                Expr ne = intConversion(e, intSizeOf(ft), intSizeOf(tt), shift);
                ne.setType(tt);
                return ne;
            } else if ( ft.isBasedOn("address") && tt.isBasedOn("int") ) {
                JIGIRTypeEnv.TYPE_addr it = ((JIGIRTypeEnv.TYPE_addr)ft);
                int size = it.getAlign();
                int ftsize = it.isRelative() ? -size : size;
                Expr ne = intConversion(e, ftsize, intSizeOf(tt), shift);
                ne.setType(tt);
                return ne;
            } else {
                throw Util.failure("cannot convert from "+ft+" to "+tt);
            }

        }

    }

    private Expr shift(Expr e, int shift) {
        // no conversion is necessary
        if ( shift == 0 ) return e;
        else {
            Token t = token(shift > 0 ? "<<" : ">>");
            BinOpExpr.BinOpImpl impl = shift > 0 ? arch.typeEnv.SHL : arch.typeEnv.SHR;
            int dist = shift < 0 ? -shift : shift;
            BinOpExpr ne = new BinOpExpr(e, t, newLiteralInt(dist));
            ne.setBinOp(impl);
            ne.setType(e.getType());
            return ne;
        }
    }

    protected int intSizeOf(Type t) {
        JIGIRTypeEnv.TYPE_int it = ((JIGIRTypeEnv.TYPE_int)t);
        int size = it.getSize();
        return it.isSigned() ? -size : size;
    }

    protected Expr intConversion(Expr e, int k1, int k2, int shift) {
        if ( k2 == -8 ) { }  // (byte)e
        if ( k2 == -16 ) { } // (short)e
        if ( k2 == 16 ) { }  // (char)e
        if ( k2 == -32 ) { return shift(e, shift); }  // (int)e
        if ( k1 < 0 && k1 >= -32 ) {
            if ( k2 < 0 && abs(k2) >= abs(k1) ) { return shift(e, shift); } // e
            if ( k2 < 0 && abs(k2) < abs(k1)) { return SE(e, k2, shift); } // SE(e, k2)
            if ( k2 > 0 && abs(k2) < 32 ) { return ZE(e, k2, shift); } // ZE(e, k2)
        }
        if ( k1 > 0 && k1 < 32) {
            if ( k2 > 0 && abs(k2) >= abs(k1) ) { return shift(e, shift); } // e
            if ( k2 > 0 && abs(k2) < abs(k1) ) { return ZE(e, k2, shift); } // ZE(e, k2)
            if ( k2 < 0 && abs(k2) <= abs(k1) ) { return SE(e, k2, shift); } // SE(e, k2)
            if ( k2 < 0 && abs(k2) > abs(k1) ) { return shift(e, shift); } // e
        }
	return e;
        //throw Util.failure("cannot convert integer type "+k1+" to "+k2+" at "+e.getSourcePoint());
    }

    protected Expr SE(Expr e, int k2, int shift) {
        assert k2 < 0;
        int L = 32 + k2;
        int R = 32 + k2 - shift;
        if ( R < 0 )
            return shift(e, L - R);
        else
            return shift(shift(e, L), -R);
    }

    private Literal.IntExpr newLiteralInt(int r) {
        Literal.IntExpr intExpr = new Literal.IntExpr(r);
        intExpr.setType(INT);
        return intExpr;
    }

    protected Expr ZE(Expr e, int k2, int shift) {
        assert k2 > 0;
        int val = mask_val(0, k2 - 1);
        Expr ne = newAnd(e, val, INT);
        return shift(ne, shift);
    }

    protected int abs(int v) {
        return v < 0 ? -v : v;
    }

    public Stmt visit(WriteStmt s, CGEnv env) {
        OperandTypeDecl.Accessor accessor = s.getAccessor();
        if ( !accessor.polymorphic ) {
            SubroutineDecl sub = accessor.getSubroutine();
            if ( sub == null )
                throw Util.failure("Unresolved write at "+s.method.getSourcePoint());
            CallStmt cs = newCallStmt(sub.name, new VarExpr(s.operand), promote(s.expr, machineType(accessor.type), 0));
            cs.setDecl(sub);
            return cs;
        } else {
            return newCallStmt(token("$write_poly_"+ getTypeString(accessor.type)), new VarExpr(s.operand), promote(s.expr, machineType(accessor.type), 0));
        }
    }

    public Stmt visit(CallStmt s, CGEnv env) {
        List<Expr> na = rebuildParams(s.getDecl().params, s.args);
        CallStmt cs = new CallStmt(s.method, na);
        cs.setDecl(s.getDecl());
        return cs;
    }

    public Stmt visit(AssignStmt s, CGEnv env) {
        throw Util.failure("Assignment statement not canonicalized");
    }

    public Stmt visit(AssignStmt.Var s, CGEnv env) {
        Expr ne = promote(s.expr, s.dest.getType(), 0);
        return new AssignStmt.Var(s.dest, ne);
    }

    public Stmt visit(AssignStmt.Map s, CGEnv env) {
        List<Expr> a = new LinkedList<Expr>();
        a.add(promote(s.map));
        a.add(promote(s.index));
        a.add(promote(s.expr));
        return new CallStmt(token("map_set"), a);
    }

    private Expr promote(Expr expr) {
        return promote(expr, expr.getType(), 0);
    }

    public Stmt visit(AssignStmt.Bit s, CGEnv env) {
        Expr nb = promote(s.bit);
        if ( nb.isLiteral() ) {
            int shift = ((Literal.IntExpr)nb).value;
            Expr ne = promote(s.expr, INT, shift);
            CallExpr ce = newCallExpr(token("bit_update"), s.dest, mask(shift, shift), ne);
            return new AssignStmt.Var(s.dest, ce);
        } else {
            Expr ne = promote(s.expr, arch.typeEnv.BOOLEAN, 0);
            CallExpr ce = newCallExpr(token("bit_set"), s.dest, nb, ne);
            return new AssignStmt.Var(s.dest, ce);
        }
    }

    public Stmt visit(AssignStmt.FixedRange s, CGEnv env) {
        Expr ne = promote(s.expr, INT, s.low_bit);
        CallExpr ce = newCallExpr(token("bit_update"), s.dest, mask(s.low_bit, s.high_bit), ne);
        return new AssignStmt.Var(s.dest, ce);
    }

    private Expr promote(Expr e, Type t, int shift) {
        return visitExpr(e, new CGEnv(machineType(t), shift));
    }

    private Type machineType(Type t) {
        if ( t instanceof JIGIRTypeEnv.TYPE_int ) {
            JIGIRTypeEnv.TYPE_int it = (JIGIRTypeEnv.TYPE_int)t;
            if ( it.getSize() > 32 )
                return LONG;
            else return INT;
        } else {
            return t;
        }
    }

    protected Expr visitExpr(Expr e, CGEnv env) {
        Expr ne;
        if ( env.expect == null ) {
            env.expect = e.getType();
            ne = e.accept(this, env);
            env.expect = null;
        } else {
            ne = e.accept(this, env);
        }
        if ( ne.getType() == null ) ne.setType(e.getType());
        return ne;
    }

    private Token token(String s) {
        Token t = new Token();
        t.image = s;
        return t;
    }

    private CallExpr newCallExpr(SubroutineDecl d, Expr... e) {
        Token name = d.name;
        CallExpr ce = newCallExpr(name, e);
        ce.setDecl(d);
        return ce;
    }

    private CallExpr newCallExpr(Token name, Expr... e) {
        List<Expr> l = new LinkedList<Expr>();
        for ( Expr a : e ) l.add(a);
        return new CallExpr(name, l);
    }

    private CallStmt newCallStmt(Token name, Expr... e) {
        List<Expr> l = new LinkedList<Expr>();
        for ( Expr a : e ) l.add(a);
        return new CallStmt(name, l);
    }

    private Literal.IntExpr mask(int low_bit, int high_bit) {
        int val = mask_val(low_bit, high_bit);
        return newLiteralInt(val);
    }

    private int mask_val(int low_bit, int high_bit) {
        if ( low_bit < 0 ) low_bit = 0;
        return Arithmetic.getBitRangeMask(low_bit, high_bit);
    }

    public static boolean isMap(Expr expr) {
        return expr.getType().isBasedOn("map");
    }
}
