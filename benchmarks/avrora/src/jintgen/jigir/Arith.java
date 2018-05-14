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

import cck.util.Arithmetic;
import cck.util.Util;
import jintgen.types.*;

/**
 * The <code>Arith</code> class is a container for classes that represent integer arithmetic in the IR. For
 * example, the class <code>Arith.AddExpr</code> represents an expression that is the addition of two
 * integers. The result of all operations on integers are integers, therefore, every expression that is a
 * subclass of <code>Arith</code> has a result type of integer.
 *
 * @author Ben L. Titzer
 */
public class Arith {

    protected abstract static class INTINT extends BinOpExpr.BinOpImpl {
        protected INTINT(String op, int p) {
            super(op, p);
        }
        public Type typeCheck(TypeEnv env, Typeable left, Typeable right) {
            JIGIRTypeEnv jenv = (JIGIRTypeEnv)env;
            JIGIRTypeEnv.TYPE_int lt = (JIGIRTypeEnv.TYPE_int)left.getType();
            JIGIRTypeEnv.TYPE_int rt = (JIGIRTypeEnv.TYPE_int)right.getType();
            return typeCheck(jenv, lt, rt);
        }
        public Literal evaluate(Literal left, Literal right) {
            int ll = ((Literal.IntExpr)left).value;
            int lr = ((Literal.IntExpr)right).value;
            return new Literal.IntExpr(evaluate(ll, lr));
        }
        protected abstract Type typeCheck(JIGIRTypeEnv jenv, JIGIRTypeEnv.TYPE_int lt, JIGIRTypeEnv.TYPE_int rt);
        protected abstract int evaluate(int ll, int lr);
    }

    public static class ADD extends INTINT {
        public ADD() {
            super("+", Expr.PREC_A_ADD);
        }
        public Type typeCheck(JIGIRTypeEnv jenv, JIGIRTypeEnv.TYPE_int lt, JIGIRTypeEnv.TYPE_int rt) {
            return jenv.newIntType(lt.isSigned() || rt.isSigned(), max(lt, rt) + 1);
        }
        public int evaluate(int ll, int lr) {
            return ll + lr;
        }
    }

    public static class SUB extends INTINT {
        public SUB() {
            super("-", Expr.PREC_A_ADD);
        }
        public Type typeCheck(JIGIRTypeEnv jenv, JIGIRTypeEnv.TYPE_int lt, JIGIRTypeEnv.TYPE_int rt) {
            return jenv.newIntType(true, max(lt, rt) + 1);
        }
        public int evaluate(int ll, int lr) {
            return ll - lr;
        }
    }

    public static class MUL extends INTINT {
        public MUL() {
            super("*", Expr.PREC_A_MUL);
        }
        public Type typeCheck(JIGIRTypeEnv jenv, JIGIRTypeEnv.TYPE_int lt, JIGIRTypeEnv.TYPE_int rt) {
            return jenv.newIntType(lt.isSigned() || rt.isSigned(), lt.getSize() + rt.getSize());
        }
        public int evaluate(int ll, int lr) {
            return ll * lr;
        }
    }

    public static class DIV extends INTINT {
        public DIV() {
            super("/", Expr.PREC_A_MUL);
        }
        public Type typeCheck(JIGIRTypeEnv jenv, JIGIRTypeEnv.TYPE_int lt, JIGIRTypeEnv.TYPE_int rt) {
            return jenv.newIntType(lt.isSigned() || rt.isSigned(), lt.getSize());
        }
        public int evaluate(int ll, int lr) {
            return ll / lr;
        }
    }

    public static class MOD extends INTINT {
        public MOD() {
            super("%", Expr.PREC_A_MUL);
        }
        public Type typeCheck(JIGIRTypeEnv jenv, JIGIRTypeEnv.TYPE_int lt, JIGIRTypeEnv.TYPE_int rt) {
            return jenv.newIntType(lt.isSigned() || rt.isSigned(), lt.getSize());
        }
        public int evaluate(int ll, int lr) {
            return ll % lr;
        }
    }

    public static class AND extends INTINT {
        public AND() {
            super("&", Expr.PREC_A_AND);
        }
        public Type typeCheck(JIGIRTypeEnv jenv, JIGIRTypeEnv.TYPE_int lt, JIGIRTypeEnv.TYPE_int rt) {
            // TODO: is this type rule reasonable?
            return jenv.newIntType(lt.isSigned() || rt.isSigned(), max(lt, rt));
        }
        public int evaluate(int ll, int lr) {
            return ll & lr;
        }
    }

    public static class OR extends INTINT {
        public OR() {
            super("|", Expr.PREC_A_OR);
        }
        public Type typeCheck(JIGIRTypeEnv jenv, JIGIRTypeEnv.TYPE_int lt, JIGIRTypeEnv.TYPE_int rt) {
            // TODO: is this type rule reasonable?
            return jenv.newIntType(lt.isSigned() || rt.isSigned(), max(lt, rt));
        }
        public int evaluate(int ll, int lr) {
            return ll | lr;
        }
    }

    public static class XOR extends INTINT {
        public XOR() {
            super("^", Expr.PREC_A_XOR);
        }
        public Type typeCheck(JIGIRTypeEnv jenv, JIGIRTypeEnv.TYPE_int lt, JIGIRTypeEnv.TYPE_int rt) {
            // TODO: is this type rule reasonable?
            return jenv.newIntType(lt.isSigned() || rt.isSigned(), max(lt, rt));
        }
        public int evaluate(int ll, int lr) {
            return ll ^ lr;
        }
    }

    public static class SHL extends INTINT {
        public SHL() {
            super("<<", Expr.PREC_A_SHIFT);
        }
        public Type typeCheck(JIGIRTypeEnv jenv, JIGIRTypeEnv.TYPE_int lt, JIGIRTypeEnv.TYPE_int rt) {
            // TODO: is this type rule reasonable?
            return jenv.newIntType(lt.isSigned(), lt.getSize());
        }
        public int evaluate(int ll, int lr) {
            return ll << lr;
        }
    }

    public static class SHR extends INTINT {
        public SHR() {
            super(">>", Expr.PREC_A_SHIFT);
        }
        public Type typeCheck(JIGIRTypeEnv jenv, JIGIRTypeEnv.TYPE_int lt, JIGIRTypeEnv.TYPE_int rt) {
            // TODO: is this type rule reasonable?
            return jenv.newIntType(lt.isSigned(), lt.getSize());
        }
        public int evaluate(int ll, int lr) {
            return ll >> lr;
        }
    }

    public static class COMP extends UnOpExpr.UnOpImpl {
        public COMP() {
            super("~", Expr.PREC_UN);
        }
        public Type typeCheck(TypeEnv env, Typeable inner) {
            // JIGIRTypeEnv jenv = (JIGIRTypeEnv)env;
            return (JIGIRTypeEnv.TYPE_int)inner.getType();
        }
        public Literal evaluate(Literal inner) {
            int ll = ((Literal.IntExpr)inner).value;
            return new Literal.IntExpr(~ll);
        }
    }

    public static class NEG extends UnOpExpr.UnOpImpl {
        public NEG() {
            super("-", Expr.PREC_UN);
        }
        public Type typeCheck(TypeEnv env, Typeable inner) {
            JIGIRTypeEnv jenv = (JIGIRTypeEnv)env;
            JIGIRTypeEnv.TYPE_int it = (JIGIRTypeEnv.TYPE_int)inner.getType();
            return jenv.newIntType(true, it.getSize() + 1);
        }
        public Literal evaluate(Literal inner) {
            int ll = ((Literal.IntExpr)inner).value;
            return new Literal.IntExpr(-ll);
        }
    }

    public static class UNSIGN extends UnOpExpr.UnOpImpl {
        public UNSIGN() {
            super("+", Expr.PREC_UN);
        }
        public Type typeCheck(TypeEnv env, Typeable inner) {
            JIGIRTypeEnv jenv = (JIGIRTypeEnv)env;
            JIGIRTypeEnv.TYPE_int it = (JIGIRTypeEnv.TYPE_int)inner.getType();
            return jenv.newIntType(false, it.getSize());
        }
        public Literal evaluate(Literal inner) {
            throw Util.unimplemented();
        }
    }

    protected static int max(JIGIRTypeEnv.TYPE_int lt, JIGIRTypeEnv.TYPE_int rt) {
        return Arithmetic.max(lt.getSize(), rt.getSize());
    }
}
