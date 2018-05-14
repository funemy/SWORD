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
import jintgen.isdl.parser.Token;
import jintgen.types.Type;
import jintgen.types.Typeable;

/**
 * The <code>Expr</code> class represents an expression in the IR. Expressions are evaluated and produce a
 * value that can be assigned to locals, globalMap, maps, used in if statements, and passed as parameters.
 *
 * @author Ben L. Titzer
 */
public abstract class Expr implements Typeable {

    public static final int PREC_L_OR = 0;
    public static final int PREC_L_XOR = 1;
    public static final int PREC_L_AND = 2;
    public static final int PREC_A_OR = 3;
    public static final int PREC_A_XOR = 4;
    public static final int PREC_A_AND = 5;
    public static final int PREC_L_EQU = 6;
    public static final int PREC_L_REL = 7;
    public static final int PREC_A_SHIFT = 8;
    public static final int PREC_A_ADD = 9;
    public static final int PREC_A_MUL = 10;
    public static final int PREC_UN = 11;
    public static final int PREC_TERM = 12;

    protected Type type;

    /**
     * The <code>getType()</code> method returns the type of this expression as computed by the
     * TypeChecker.
     * @return a reference to a <code>Type</code> object that represents the type of this expression.
     */
    public Type getType() {
        return type;
    }

    /**
     * The <code>setType()</code> method sets the type of this expression. This is meant to be called
     * by the TypeChecker as it visits the program and computes the type of each expression.
     * @param t the new type of this expression
     */
    public void setType(Type t) {
        type = t;
    }

    /**
     * The <code>isVariable()</code> method tests whether this expression is a single variable use.
     *
     * @return true if this expression is a direct use of a variable; false otherwise
     */
    public boolean isVariable() {
        return false;
    }

    /**
     * The <code>isLiteral()</code> method tests whether this expression is a known constant directly (i.e. a
     * literal).
     *
     * @return true if this expression is a literal; false otherwise
     */
    public boolean isLiteral() {
        return false;
    }

    /**
     * The <code>isConstantExpr()</code> method tests whether this expression is a constant expression (i.e.
     * it is reducable to a constant and has no references to variables, maps, etc).
     *
     * @return true if this expression can be evaluated to a constant; false otherwise
     */
    public boolean isConstantExpr() {
        return false;
    }

    /**
     * The <code>isBitRangeExpr()</code> method tests whether the expression is an access of a range of bits.
     * This is used in pattern matching in some parts of the code.
     *
     * @return true if this expression is a bit range expression; false otherwise
     */
    public boolean isBitRangeExpr() {
        return false;
    }

    /**
     * The <code>getPrecedence()</code> method gets the binding precedence for this expression. This is used
     * to compute when inner expressions must be nested within parentheses in order to preserve the implied
     * order of evaluation.
     *
     * @return an integer representing the precedence of this expression; higher numbers are higher
     *         precedence
     */
    public abstract int getPrecedence();

    /**
     * The <code>tokenToInt()</code> static method is a utility to evaluate a token as an integer.
     *
     * @param i the value of the integer as a token
     * @return the integer value of the token if it exists
     */
    public static int tokenToInt(Token i) {
        if (i == null) return -1;
        return StringUtil.evaluateIntegerLiteral(i.image);
    }

    /**
     * The <code>tokenToBool()</code> static method is a utility to evaluate a token as a boolean literal.
     *
     * @param i the value of the boolean as a token
     * @return the boolean value of the token if it exists
     */
    public static boolean tokenToBool(Token i) {
        return Boolean.valueOf(i.image);
    }

    /**
     * The <code>accept()</code> method implements one half of the visitor pattern so that client visitors can
     * traverse the syntax tree easily and in an extensible way.
     *
     * @param v the visitor to accept
     */
    public abstract void accept(CodeVisitor v);

    /**
     * The <code>accept()</code> method implements one half of the visitor pattern for rebuilding of
     * expressions. This visitor allows code to be slightly modified while only writing visit methods for the
     * parts of the syntax tree affected.
     *
     * @param r the accumulator to accept
     * @return the result of calling the appropriate <code>visit()</code> method of the rebuilder
     */
    public abstract <Res, Env> Res accept(CodeAccumulator<Res, Env> r, Env env);

    /**
     * The <code>innerString()</code> method is a utility to embed an expression in parentheses only if its
     * precedence is less than the precedence of this expression. This is useful in converting nested
     * expressions into strings
     *
     * @param e the expression nested inside of this expression
     * @return a string that is the result of converting the specified expression to a string and nesting it
     *         in parentheses if necessary
     */
    public String innerString(Expr e) {
        if (e.getPrecedence() < this.getPrecedence())
            return StringUtil.embed(e.toString());
        else
            return e.toString();
    }

    public boolean isLvalue() {
        return false;
    }

    /**
     * The <code>getSourcePoint()</code> method returns a reference to a <code>SourcePoint</code>
     * instance that represents the location in the source program of this expression.
     * @return a <code>SourcePoint</code> instance that represents this expression in the
     * source program
     */
    public abstract SourcePoint getSourcePoint();
}
