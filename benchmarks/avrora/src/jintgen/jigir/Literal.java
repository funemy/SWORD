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

import cck.parser.SourcePoint;
import cck.text.StringUtil;
import cck.util.Util;
import jintgen.isdl.EnumDecl;
import jintgen.isdl.SymbolMapping;
import jintgen.isdl.parser.Token;

/**
 * The <code>Literal</code> class represents a literal (constant value) as part of an expression. Literals
 * have known, constant values, either boolean or integer.
 *
 * @author Ben L. Titzer
 */
public abstract class Literal extends Expr {

    /**
     * The <code>token</code> fields stores a reference to the original token representing this literal.
     */
    public final Token token;

    public boolean isLiteral() {
        return true;
    }

    /**
     * The <code>isConstantExpr()</code> method tests whether this expression is a constant expression (i.e.
     * it is reducable to a constant and has no references to variables, maps, etc). For literals, this method
     * will always return true because a literal is a constant value.
     *
     * @return true if this expression can be evaluated to a constant; false otherwise
     */
    public boolean isConstantExpr() {
        return true;
    }

    public Literal(Token t) {
        token = t;
    }

    public SourcePoint getSourcePoint() {
        return token.getSourcePoint();
    }

    /**
     * The <code>IntExpr</code> inner class represents an integer literal that has a known, constant value.
     *
     * @author Ben L. Titzer
     */
    public static class IntExpr extends Literal {

        /**
         * The <code>value</code> class stores the constant integer value of this literal.
         */
        public final int value;

        /**
         * The constructor of the <code>IntExpr</code> class evaluates the token's string value to an integer
         * and stores it in the publicly accessable <code>value</code> field, as well as storing a reference
         * to the original token.
         *
         * @param v the token representing the value of this integer
         */
        public IntExpr(Token v) {
            super(v);
            value = Expr.tokenToInt(v);
        }

        /**
         * The constructor of the <code>IntExpr</code> class evaluates the token's string value to an integer
         * and stores it in the publicly accessable <code>value</code> field, as well as storing a reference
         * to the original token.
         *
         * @param v the token representing the value of this integer
         */
        public IntExpr(int v) {
            super(new Token());
            value = v;
        }

        /**
         * The <code>accept()</code> method implements one half of the visitor pattern so that client visitors
         * can traverse the syntax tree easily and in an extensible way.
         *
         * @param v the visitor to accept
         */
        public void accept(CodeVisitor v) {
            v.visit(this);
        }

        public <Res, Env> Res accept(CodeAccumulator<Res, Env> r, Env env) {
            return r.visit(this, env);
        }

        public String toString() {
            return Integer.toString(value);
        }

        public int getPrecedence() {
            return PREC_TERM;
        }
    }

    /**
     * The <code>BoolExpr</code> inner class represents a boolean literal that has a known, constant value
     * (true or false).
     */
    public static class BoolExpr extends Literal {

        /**
         * The <code>value</code> class stores the constant boolean value of this literal.
         */
        public final boolean value;

        /**
         * The constructor of the <code>BoolExpr</code> class evaluates the token's string value as a boolean
         * and stores it in the publicly accessable <code>value</code> field, as well as storing a reference
         * to the original token
         *
         * @param v the token representing the value of this boolean
         */
        public BoolExpr(Token v) {
            super(v);
            value = Expr.tokenToBool(v);
        }

        /**
         * The constructor of the <code>BoolExpr</code> class evaluates the token's string value as a boolean
         * and stores it in the publicly accessable <code>value</code> field, as well as storing a reference
         * to the original token
         *
         * @param b the value of this boolean
         */
        public BoolExpr(boolean b) {
            super(new Token());
            value = b;
        }

        /**
         * The <code>accept()</code> method implements one half of the visitor pattern so that client visitors
         * can traverse the syntax tree easily and in an extensible way.
         *
         * @param v the visitor to accept
         */
        public void accept(CodeVisitor v) {
            v.visit(this);
        }

        public <Res, Env> Res accept(CodeAccumulator<Res, Env> r, Env env) {
            return r.visit(this, env);
        }

        public String toString() {
            return value ? "true" : "false";
        }

        public int getPrecedence() {
            return PREC_TERM;
        }
    }

    public static class EnumVal extends Literal {
        public final EnumDecl enumDecl;
        public final SymbolMapping.Entry entry;

        public EnumVal(EnumDecl d, Token m) {
            super(m);
            enumDecl = d;
            entry = enumDecl.map.get(m.image);
            if ( entry == null )
                throw Util.failure("Unresolved member "+ StringUtil.quote(m.image)+" of enum "+d.name);
        }

        /**
         * The <code>accept()</code> method implements one half of the visitor pattern so that client visitors
         * can traverse the syntax tree easily and in an extensible way.
         *
         * @param v the visitor to accept
         */
        public void accept(CodeVisitor v) {
            v.visit(this);
        }

        public <Res, Env> Res accept(CodeAccumulator<Res, Env> r, Env env) {
            return r.visit(this, env);
        }

        public String toString() {
            return enumDecl.name.image+ '.' +token;
        }

        public int getPrecedence() {
            return PREC_TERM;
        }
    }
}
