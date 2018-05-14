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

/**
 * The <code>IndexExpr</code> class represents an access of an individual bit within a value. In the IR,
 * individual bits of values can be addressed for both reading and writing.
 *
 * @author Ben L. Titzer
 */
public class IndexExpr extends Expr {

    /**
     * The <code>expr</code> field stores a reference to the expression whose value the bit will be extracted
     * from.
     */
    public final Expr expr;

    /**
     * The <code>bit</code> field stores a reference to an expression that when evaluated indicates which bit
     * to read.
     */
    public final Expr index;

    /**
     * The constructor of the <code>IndexExpr</code> class simply initializes the references to the expression
     * and the bit.
     *
     * @param e the expression representing the value to extract the bit from
     * @param b the expression representing the number of the bit to extract
     */
    public IndexExpr(Expr e, Expr b) {
        expr = e;
        index = b;
    }

    /**
     * The <code>isConstantExpr()</code> method tests whether this expression is a constant expression (i.e.
     * it is reducable to a constant and has no references to variables, maps, etc).
     *
     * @return true if this expression can be evaluated to a constant; false otherwise
     */
    public boolean isConstantExpr() {
        return expr.isConstantExpr() && index.isConstantExpr();
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
        return innerString(expr) + '[' + index.toString() + ']';
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
        return new SourcePoint(expr.getSourcePoint(), index.getSourcePoint());
    }

    public boolean isLvalue() {
        return true;
    }

}
