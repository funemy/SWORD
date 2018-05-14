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
import jintgen.isdl.parser.Token;

/**
 * The <code>FixedRangeExpr</code> class represents an expression whose value is the result of extracting a
 * range of the bits from another expression. In the IR, ranges of bits can be specified for reading and
 * writing, allowing cleaner expression of sub-byte fields. The bounds of these ranges are static: the
 * endpoints must be constants.
 *
 * @author Ben L. Titzer
 */
public class FixedRangeExpr extends Expr {
    /**
     * The <code>operand</code> field stores a reference to the expression that is the operand of the bit
     * range expression, i.e. the value from which the range of bits will be extracted.
     */
    public final Expr expr;

    /**
     * The <code>low_bit</code> field represents the lowest bit in the range to be extracted, inclusive.
     */
    public final int low_bit;

    /**
     * The <code>high_bit</code> field represents the highest bit in the range to be extracted, inclusive.
     */
    public final int high_bit;

    /**
     * The constructor of the <code>FixedRangeExpr</code> class simply initializes the references to the
     * operands of the bit range expression.
     *
     * @param o a reference to the expression operand
     * @param l the lowest bit in the range, inclusive
     * @param h the highest bit in the range, inclusive
     */
    public FixedRangeExpr(Expr o, Token l, Token h) {
        expr = o;
        int low = Expr.tokenToInt(l);
        int high = Expr.tokenToInt(h);

        low_bit = low < high ? low : high;
        high_bit = low > high ? low : high;
    }

    /**
     * The constructor of the <code>FixedRangeExpr</code> class simply initializes the references to the
     * operands of the bit range expression.
     *
     * @param o a reference to the expression operand
     * @param l the lowest bit in the range, inclusive
     * @param h the highest bit in the range, inclusive
     */
    public FixedRangeExpr(Expr o, int l, int h) {
        expr = o;
        low_bit = l;
        high_bit = h;
    }

    /**
     * The <code>isConstantExpr()</code> method tests whether this expression is a constant expression (i.e.
     * it is reducable to a constant and has no references to variables, maps, etc).
     *
     * @return true if this expression can be evaluated to a constant; false otherwise
     */
    public boolean isConstantExpr() {
        return expr.isConstantExpr();
    }

    /**
     * The <code>isBitRangeExpr()</code> method tests whether the expression is an access of a range of bits.
     * This is used in pattern matching in some parts of the code.
     *
     * @return true
     */
    public boolean isBitRangeExpr() {
        return true;
    }

    /**
     * The <code>accept()</code> method implements one half of the visitor pattern so that client visitors can
     * traverse the syntax tree easily and in an extensible way.
     *
     * @param v the visitor to accept
     */
    public void accept(CodeVisitor v) {
        v.visit(this);
    }

    /**
     * The <code>accept()</code> method implements one half of the visitor pattern for rebuilding of
     * expressions. This visitor allows code to be slightly modified while only writing visit methods for the
     * parts of the syntax tree affected.
     *
     * @param r the rebuilder to accept
     * @return the result of calling the appropriate <code>visit()</code> method of the rebuilder
     */
    public <Res, Env> Res accept(CodeAccumulator<Res, Env> r, Env env) {
        return r.visit(this, env);
    }

    /**
     * The <code>toString()</code> method recursively converts this expression to a string. For binary
     * operations, inner expressions will be nested within parentheses if their precedence is lower than the
     * precedence of the parent expression.
     *
     * @return a string representation of this expression
     */
    public String toString() {
        return innerString(expr) + '[' + low_bit + ':' + high_bit + ']';
    }

    /**
     * The <code>getPrecedence()</code> method gets the binding precedence for this expression. This is used
     * to compute when inner expressions must be nested within parentheses in order to preserve the implied
     * order of evaluation.
     *
     * @return an integer representing the precedence of this expression; higher numbers are higher
     *         precedence
     */
    public int getPrecedence() {
        return PREC_TERM;
    }


    public SourcePoint getSourcePoint() {
        return expr.getSourcePoint();
    }

    public boolean isLvalue() {
        return true;
    }
}
