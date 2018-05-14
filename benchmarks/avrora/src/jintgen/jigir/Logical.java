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

import cck.util.Util;
import jintgen.types.*;

/**
 * The <code>Logical</code> class is a container for classes that represent expressions that produce booleans
 * in the IR. For example, the class <code>Logical.AndExpr</code> represents an expression that is the logical
 * AND of two boolean values. The result of all operations on integers are boolean; therefore, every
 * expression that is a subclass of <code>Logical</code> has a result type of boolean.
 *
 * @author Ben L. Titzer
 */
public class Logical {

    public static class AND extends BinOpExpr.BinOpImpl {
        public AND() {
            super("and", Expr.PREC_L_AND);
        }
        public Type typeCheck(TypeEnv env, Typeable left, Typeable right) {
            JIGIRTypeEnv jenv = (JIGIRTypeEnv)env;
            if ( left.getType() != jenv.BOOLEAN ) jenv.ERROR.TypeMismatch("logical and", left, jenv.BOOLEAN);
            if ( right.getType() != jenv.BOOLEAN ) jenv.ERROR.TypeMismatch("logical and", right, jenv.BOOLEAN);
            return jenv.BOOLEAN;
        }
        public Literal evaluate(Literal left, Literal right) {
            throw Util.unimplemented();
        }
    }

    public static class OR extends BinOpExpr.BinOpImpl {
        public OR() {
            super("or", Expr.PREC_L_OR);
        }
        public Type typeCheck(TypeEnv env, Typeable left, Typeable right) {
            JIGIRTypeEnv jenv = (JIGIRTypeEnv)env;
            if ( left.getType() != jenv.BOOLEAN ) jenv.ERROR.TypeMismatch("logical or", left, jenv.BOOLEAN);
            if ( right.getType() != jenv.BOOLEAN ) jenv.ERROR.TypeMismatch("logical or", right, jenv.BOOLEAN);
            return jenv.BOOLEAN;
        }
        public Literal evaluate(Literal left, Literal right) {
            throw Util.unimplemented();
        }
    }

    public static class XOR extends BinOpExpr.BinOpImpl {
        public XOR() {
            super("xor", Expr.PREC_L_XOR);
        }
        public Type typeCheck(TypeEnv env, Typeable left, Typeable right) {
            JIGIRTypeEnv jenv = (JIGIRTypeEnv)env;
            if ( left.getType() != jenv.BOOLEAN ) jenv.ERROR.TypeMismatch("logical xor", left, jenv.BOOLEAN);
            if ( right.getType() != jenv.BOOLEAN ) jenv.ERROR.TypeMismatch("logical xor", right, jenv.BOOLEAN);
            return jenv.BOOLEAN;
        }
        public Literal evaluate(Literal left, Literal right) {
            throw Util.unimplemented();
        }
    }

    public static class EQU extends BinOpExpr.BinOpImpl {
        public EQU() {
            super("==", Expr.PREC_L_EQU);
        }
        public Type typeCheck(TypeEnv env, Typeable left, Typeable right) {
            JIGIRTypeEnv jenv = (JIGIRTypeEnv)env;
            if ( jenv.COMPARABLE.contains(typeConOf(left), typeConOf(right)) ) return jenv.BOOLEAN;
            else jenv.ERROR.TypesCannotBeCompared(left, right);
            return null;
        }

        public Literal evaluate(Literal left, Literal right) {
            throw Util.unimplemented();
        }
    }

    public static class NEQU extends BinOpExpr.BinOpImpl {
        public NEQU() {
            super("!=", Expr.PREC_L_EQU);
        }
        public Type typeCheck(TypeEnv env, Typeable left, Typeable right) {
            JIGIRTypeEnv jenv = (JIGIRTypeEnv)env;
            if (jenv.COMPARABLE.contains(typeConOf(left), typeConOf(right))) return jenv.BOOLEAN;
            jenv.ERROR.TypesCannotBeCompared(left, right);
            return null;
        }
        public Literal evaluate(Literal left, Literal right) {
            throw Util.unimplemented();
        }
    }

    public static class LESS extends BinOpExpr.BinOpImpl {
        public LESS() {
            super("<", Expr.PREC_L_REL);
        }
        public Type typeCheck(TypeEnv env, Typeable left, Typeable right) {
            return ((JIGIRTypeEnv)env).BOOLEAN;
        }
        public Literal evaluate(Literal left, Literal right) {
            throw Util.unimplemented();
        }
    }

    public static class LESSEQU extends BinOpExpr.BinOpImpl {
        public LESSEQU() {
            super("<=", Expr.PREC_L_REL);
        }
        public Type typeCheck(TypeEnv env, Typeable left, Typeable right) {
            return ((JIGIRTypeEnv)env).BOOLEAN;
        }
        public Literal evaluate(Literal left, Literal right) {
            throw Util.unimplemented();
        }
    }

    public static class GR extends BinOpExpr.BinOpImpl {
        public GR() {
            super(">", Expr.PREC_L_REL);
        }
        public Type typeCheck(TypeEnv env, Typeable left, Typeable right) {
            return ((JIGIRTypeEnv)env).BOOLEAN;
        }
        public Literal evaluate(Literal left, Literal right) {
            throw Util.unimplemented();
        }
    }

    public static class GREQ extends BinOpExpr.BinOpImpl {
        public GREQ() {
            super(">=", Expr.PREC_L_REL);
        }
        public Type typeCheck(TypeEnv env, Typeable left, Typeable right) {
            return ((JIGIRTypeEnv)env).BOOLEAN;
        }
        public Literal evaluate(Literal left, Literal right) {
            throw Util.unimplemented();
        }

    }

    public static class NOT extends UnOpExpr.UnOpImpl {
        public NOT() {
            super("!", Expr.PREC_UN);
        }
        public Type typeCheck(TypeEnv env, Typeable inner) {
            JIGIRTypeEnv jenv = (JIGIRTypeEnv)env;
            if ( inner.getType() != jenv.BOOLEAN ) jenv.ERROR.TypeMismatch("logical not", inner, jenv.BOOLEAN);
            return jenv.BOOLEAN;
        }
        public Literal evaluate(Literal inner) {
            throw Util.unimplemented();
        }

    }

    private static TypeCon typeConOf(Typeable expr) {
        return expr.getType().getTypeCon();
    }

}
