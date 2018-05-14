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
 * Creation date: Sep 21, 2005
 */

package jintgen.isdl.verifier;

import cck.util.Arithmetic;
import cck.util.Util;
import jintgen.isdl.*;
import jintgen.isdl.parser.Token;
import jintgen.jigir.*;
import jintgen.types.*;
import java.util.*;

/**
 * The <code>TypeChecker</code> implements typecheck of JIGIR code. It visits each
 * expression and statement and checks that the types are not violated. After it has
 * processed the code, all expressions will be given types that can be used later
 * in generating Java code implementing the interpreter and analysis tools.
 *
 * @author Ben L. Titzer
 */
public class TypeChecker extends VerifierPass implements CodeAccumulator<Type, Environment>, StmtAccumulator<Environment, Environment> {

    final JIGIRErrorReporter ERROR;
    Type retType;

    TypeChecker(JIGIRErrorReporter er, ArchDecl a) {
        super(a);
        ERROR = er;
    }

    public void verify() {
        typeCheckAccessMethods();
        typeCheckSubroutines();
        typeCheckInstrBodies();
    }

    private void typeCheckAccessMethods() {
        Environment env = arch.globalEnv;
        // first step: resolve all read methods by type
        JIGIRTypeEnv te = arch.typeEnv;
        for ( OperandTypeDecl ot: arch.operandTypes ) {
            for ( OperandTypeDecl.AccessMethod m : ot.readDecls )
                ot.readAccessors.put(resolveAccessMethodType(m, te), m);
            for ( OperandTypeDecl.AccessMethod m : ot.writeDecls )
                ot.writeAccessors.put(resolveAccessMethodType(m, te), m);
        }
        // now typecheck the bodies
        for ( OperandTypeDecl ot: arch.operandTypes ) {
            // add each of the sub operands to the environment
            Environment oenv = new Environment(env);
            oenv.addVariable("this", operandRepresentationType(ot));
            for ( AddrModeDecl.Operand o : ot.subOperands ) {
                o.typeRef.resolve(arch.typeEnv);
                oenv.addDecl(o);
            }

            for ( OperandTypeDecl.AccessMethod m : ot.readDecls ) {
                typeCheck(m.code.getStmts(), resolveAccessMethodType(m, arch.typeEnv), new Environment(oenv));
            }
            for ( OperandTypeDecl.AccessMethod m : ot.writeDecls ) {
                Environment senv = new Environment(oenv);
                senv.addVariable("value", resolveAccessMethodType(m, arch.typeEnv));
                typeCheck(m.code.getStmts(), null, senv);
            }
        }

    }

    private Type operandRepresentationType(OperandTypeDecl ot) {
        if ( ot.isValue() ) {
            TypeRef kind = ((OperandTypeDecl.Value)ot).typeRef;
            return kind.resolve(arch.typeEnv);
        } else
            return arch.typeEnv.resolveOperandType(ot);
    }

    private Type resolveAccessMethodType(OperandTypeDecl.AccessMethod m, JIGIRTypeEnv te) {
        if ( m.type != null ) return m.type;
        m.type = m.typeRef.resolve(te);
        return m.type;
    }

    private void typeCheckInstrBodies() {
        Environment env = arch.globalEnv;
        for ( InstrDecl d : arch.instructions ) {
            if ( !d.code.hasBody() ) continue;
            Environment senv = new Environment(env);
            for ( AddrModeDecl.Operand p : d.getOperands() ) {
                p.typeRef.resolve(typeEnv);
                senv.addDecl(p);
            }
            typeCheck(d.code.getStmts(), null, senv);
        }
    }

    private void typeCheckSubroutines() {
        Environment env = arch.globalEnv;
        for ( SubroutineDecl d : arch.subroutines ) {
            Environment senv = new Environment(env);
            for ( SubroutineDecl.Parameter p : d.getParams() ) {
                p.type.resolve(typeEnv);
                senv.addDecl(p);
            }
            Type t = d.ret.resolve(arch.typeEnv);
            if ( d.code.hasBody() )
                typeCheck(d.code.getStmts(), t, senv);
        }
    }

    public void typeCheck(List<Stmt> s, Type retType, Environment env) {
        this.retType = retType;
        for ( Stmt st : s ) typeCheck(st, env);
        this.retType = null;
    }

    public void typeCheck(Stmt s, Environment env) {
        s.accept(this, env);
    }

    public Environment visit(CallStmt s, Environment env) {
        SubroutineDecl d = typeCheckCall(env, s.method, s.args);
        s.setDecl(d);
        return env;
    }

    public Environment visit(WriteStmt s, Environment env) {
        OperandTypeDecl d = operandTypeOf(s.operand, env);
        OperandTypeDecl.Accessor accessor = resolveMethod("Write", s.typeRef, d, s.method, d.writeAccessors);
        s.setAccessor(accessor);
        typeCheck("write", s.expr, accessor.type, env);
        return env;
    }

    private OperandTypeDecl.Accessor resolveMethod(String access, TypeRef wt, OperandTypeDecl d, Token m, HashMap<Type, OperandTypeDecl.Accessor> accessors) {
        if ( wt != null ) {
            // type given to the write; look up appropriate write method
            Type t = wt.resolve(typeEnv);
            OperandTypeDecl.Accessor meth = accessors.get(t);
            if ( meth == null ) ERROR.UnresolvedAccess(access, d, m, t);
            return meth;
        } else {
            // no type given to the write; check that there is only one accessor available
            if (accessors.isEmpty() ) ERROR.UnresolvedAccess(access, d, m, null);
            if ( accessors.size() > 1 ) ERROR.AmbiguousAccess(access, d, m);
            return accessors.values().iterator().next();
        }
    }

    private SubroutineDecl typeCheckCall(Environment env, Token method, List<Expr> args) {
        SubroutineDecl d = env.resolveSubroutine(method.image);
        if ( d == null ) ERROR.UnresolvedSubroutine(method);
        Iterator<Expr> eiter = args.iterator();
        if ( d.getParams().size() != args.size() )
            ERROR.ArityMismatch(method);
        for ( SubroutineDecl.Parameter p : d.getParams() ) {
            Type t = p.type.resolve(typeEnv);
            typeCheck("invocation", eiter.next(), t, env);
        }
        return d;
    }

    public Environment visit(CommentStmt s, Environment env) {
        return env;
    }

    public Environment visit(DeclStmt s, Environment env) {
        if ( env.isDefinedLocally(s.name.image) ) ERROR.RedefinedLocal(s.name);
        Type t = s.typeRef.resolve(typeEnv);
        typeCheck("initialization", s.init, t, env);
        env.addDecl(s);
        return env;
    }

    public Environment visit(IfStmt s, Environment env) {
        typeCheck("if condition", s.cond, typeEnv.BOOLEAN, env);
        Environment te = new Environment(env);
        visitStmtList(s.trueBranch, te);
        Environment fe = new Environment(env);
        visitStmtList(s.falseBranch, fe);
        return env;
    }

    public List<Environment> visitStmtList(List<Stmt> l, Environment env) {
        for ( Stmt s : l ) typeCheck(s, env);
        return null;
    }

    public Environment visit(ReturnStmt s, Environment env) {
        if ( retType == null ) ERROR.ReturnStmtNotInSubroutine(s);
        typeCheck("return", s.expr, retType, env);
        return env;
    }

    public Environment visit(AssignStmt s, Environment env) {
        if ( !s.dest.isLvalue() ) ERROR.NotAnLvalue(s.dest);
        Type lt = typeOf(s.dest, env);
        typeCheck("assignment", s.expr, lt, env);
        return env;
    }

    public Environment visit(AssignStmt.Var s, Environment env) {
        throw Util.unimplemented();
    }

    public Environment visit(AssignStmt.Map s, Environment env) {
        throw Util.unimplemented();
    }

    public Environment visit(AssignStmt.Bit s, Environment env) {
        throw Util.unimplemented();
    }

    public Environment visit(AssignStmt.FixedRange s, Environment env) {
        throw Util.unimplemented();
    }

    public Type visit(BinOpExpr e, Environment env) {
        Type lt = typeOf(e.left, env);
        Type rt = typeOf(e.right, env);
        TypeCon.BinOp binop = typeEnv.resolveBinOp(lt, rt, e.operation.image);
        if ( binop == null ) ERROR.UnresolvedOperator(e.operation, lt, rt);
        e.setBinOp((BinOpExpr.BinOpImpl)binop);
        return binop.typeCheck(typeEnv, e.left, e.right);
    }

    public Type visit(IndexExpr e, Environment env) {
        Type lt = typeOf(e.expr, env);
        if ( lt.isBasedOn("int") ) {
            Type rt = intTypeOf(e.index, env);
            return typeEnv.BOOLEAN;
        }
        else if ( lt.isBasedOn("map") ) {
            List<TypeRef> pm = (List<TypeRef>)lt.getDimension("types");
            Type it = pm.get(0).resolve(typeEnv);
            Type et = pm.get(1).resolve(typeEnv);
            typeCheck("indexing", e.index, it, env);
            return et;
        } else {
            ERROR.TypeDoesNotSupportIndex(e.expr);
            return null;
        }
    }

    public Type visit(FixedRangeExpr e, Environment env) {
        Type lt = intTypeOf(e.expr, env);

	int size = ((JIGIRTypeEnv.TYPE_int) lt).getSize();
	if ((e.low_bit < 0) || (e.high_bit >= size)) {
	    ERROR.RangeOutOfBounds(e, e.low_bit, e.high_bit, size);
	} else if (e.low_bit >= e.high_bit) {
	    // Only invalid range is where low_bit == high_bit, because
	    // they are set such that low_bit is always <= high_bit
	    ERROR.InvalidRange(e, e.low_bit, e.high_bit);
	}
        return typeEnv.newIntType(false, e.high_bit - e.low_bit + 1);
    }

    public List<Type> visitExprList(List<Expr> l, Environment env) {
        List<Type> lt = new LinkedList<Type>();
        for ( Expr e : l ) lt.add(typeOf(e, env));
        return lt;
    }

    public Type visit(CallExpr e, Environment env) {
        SubroutineDecl d = typeCheckCall(env, e.method, e.args);
        e.setDecl(d);
        return d.ret.resolve(typeEnv);
    }

    public Type visit(ReadExpr e, Environment env) {
        OperandTypeDecl d = operandTypeOf(e.operand, env);
        OperandTypeDecl.Accessor accessMethod = resolveMethod("Read", e.typeRef, d, e.method, d.readAccessors);
        e.setAccessor(accessMethod);
        return accessMethod.type;
    }

    public Type visit(ConversionExpr e, Environment env) {
        Type ft = typeOf(e.expr, env);
        Type tt = e.typeRef.resolve(typeEnv);
        if ( !typeEnv.CONVERTIBLE.contains(ft.getTypeCon(), tt.getTypeCon())) {
            ERROR.TypeCannotBeConverted(e.expr, tt);
        }
        return tt;
    }

    public Type visit(Literal.BoolExpr e, Environment env) {
        return typeEnv.BOOLEAN;
    }

    public Type visit(Literal.IntExpr e, Environment env) {
        return getTypeOfLiteral(typeEnv, e.value);
    }

    public static JIGIRTypeEnv.TYPE_int getTypeOfLiteral(JIGIRTypeEnv typeEnv, int val) {
        if ( val == 0 ) return typeEnv.newIntType(false, 1);
        if ( val < 0 ) {
            return typeEnv.newIntType(true, Arithmetic.highestBit(~val)+1);
        } else {
            return typeEnv.newIntType(false, Arithmetic.highestBit(val)+1);
        }
    }

    public Type visit(Literal.EnumVal e, Environment env) {
        return typeEnv.resolveType(e.enumDecl.name);
    }

    public Type visit(UnOpExpr e, Environment env) {
        Type lt = typeOf(e.expr, env);
        TypeCon.UnOp unop = typeEnv.resolveUnOp(lt, e.operation.image);
        if ( unop == null ) ERROR.UnresolvedOperator(e.operation, lt);
        e.setUnOp((UnOpExpr.UnOpImpl)unop);
        return unop.typeCheck(typeEnv, e.expr);
    }

    public Type visit(VarExpr e, Environment env) {
        Decl d = env.resolveDecl(e.variable.image);
        if ( d == null ) ERROR.UnresolvedVariable(e.variable);
        e.setDecl(d);
        return d.getType();
    }

    public Type visit(DotExpr e, Environment env) {
        Type t = typeOf(e.expr, env);
        TypeCon typecon = t.getTypeCon();
        if ( typecon instanceof JIGIRTypeEnv.TYPE_enum_kind ) {
            return typeEnv.resolveType(((JIGIRTypeEnv.TYPE_enum_kind)typecon).decl.name);
        } else if ( typecon instanceof JIGIRTypeEnv.TYPE_operand ) {
	    OperandTypeDecl decl = ((JIGIRTypeEnv.TYPE_operand) typecon).decl;
	    if (decl.isCompound()) {
		OperandTypeDecl.Compound operand = 
		    (OperandTypeDecl.Compound) decl;
		AddrModeDecl.Operand subOperand = null;
		for (AddrModeDecl.Operand op : operand.subOperands) {
		    if (op.name.image.equals(e.field.image)) {
			subOperand = op;
			break;
		    }
		}
		
		if (subOperand == null) {
		    ERROR.UnresolvedSubOperand(e.field);
		}
		
		Type rt = subOperand.typeRef.resolve(arch.typeEnv);
		
		// Redundant check?
		if (t == null) {
		    ERROR.UnresolvedSubOperand(e.field);
		}
		return rt;
	    } else {
		// Can only apply dot operator to compound operands.
		ERROR.CompoundTypeExpected(e.expr);
	    }
        }
        throw Util.unimplemented();
    }

    protected Type typeOf(Expr e, Environment env) {
        Type t = e.accept(this, env);
        if ( t == null ) throw Util.failure("null type at "+e.getSourcePoint());
        e.setType(t);
        return t;
    }

    protected Type intTypeOf(Expr e, Environment env) {
        Type t = e.accept(this, env);
        e.setType(t);
        if (!t.isBasedOn("int"))
            ERROR.IntTypeExpected("expression", e);
        return t;
    }

    protected OperandTypeDecl operandTypeOf(Token o, Environment env) {
        Decl d = env.resolveDecl(o.image);
        if ( d == null ) ERROR.UnresolvedVariable(o);
        Type t = d.getType();
        TypeCon tc = t.getTypeCon();
        if (!(tc instanceof JIGIRTypeEnv.TYPE_operand))
            ERROR.OperandTypeExpected(o, t);
        return ((JIGIRTypeEnv.TYPE_operand)tc).decl;
    }

    protected Type typeCheck(String what, Expr e, Type exp, Environment env) {
        Type t = typeOf(e, env);
        if (!isAssignableTo(t, exp)) {
            ERROR.TypeMismatch(what, e, exp);
	}

	if (exp.isBasedOn("int") && t.isBasedOn("int")) {
	    JIGIRTypeEnv.TYPE_int i1 = (JIGIRTypeEnv.TYPE_int) t;
	    JIGIRTypeEnv.TYPE_int i2 = (JIGIRTypeEnv.TYPE_int) exp;
	    
	    int size1 = i1.getSize();
	    int size2 = i2.getSize();
	    boolean sign1 = i1.isSigned();
	    boolean sign2 = i2.isSigned();

	    if (size1 > size2) {
		ERROR.IntTypeTooLarge(e, size2, size1);
	    } else if (size1 == size2 && (sign1 ^ sign2)) {
		ERROR.SignMismatch(e, sign2, sign1);
	    } else if (size1 < size2 && !sign2 && sign1) {
		ERROR.SignMismatch(e, sign2, sign1);
	    }
	}
        return t;
    }

    private boolean isAssignableTo(Type t, Type exp) {
        return typeEnv.ASSIGNABLE.contains(t.getTypeCon(), exp.getTypeCon());
    }

    int max(int a, int b) {
        return a > b ? a : b;
    }
}
