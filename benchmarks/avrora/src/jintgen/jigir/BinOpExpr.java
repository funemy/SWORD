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
 * Creation date: Oct 4, 2005
 */

package jintgen.jigir;

import cck.parser.SourcePoint;
import jintgen.isdl.parser.Token;
import jintgen.types.*;

/**
 * The <code>BinOp</code> inner class represents an operation on two integers with an infix binary
 * operation. For example, addition, multiplication, bitwise and, and such operations are binary infix and
 * therefore subclasses of this class.
 */
public class BinOpExpr extends Expr {
    /**
     * The <code>operation</code> field stores the string name of the operation of this binary operation.
     * For example, '+' represents addition.
     */
    public final Token operation;

    /**
     * The <code>left</code> field stores a reference to the expression that is the left operand of the
     * binary operation.
     */
    public final Expr left;

    /**
     * The <code>left</code> field stores a reference to the expression that is the right operand of the
     * binary operation.
     */
    public final Expr right;

    /**
     * The <code>binop</code> field stores a reference to the actual binary operation that this
     * syntax tree node corresponds to.
     */
    protected BinOpImpl binop;

    /**
     * The constructor of the <code>BinOp</code> class initializes the public final fields that form the
     * structure of this expression.
     *
     * @param l the left expression operand
     * @param o the a token representing the name of the operation such as '+'
     * @param r the right expression operand
     */
    public BinOpExpr(Expr l, Token o, Expr r) {
        left = l;
        right = r;
        operation = o;
    }

    /**
     * The <code>isConstantExpr()</code> method tests whether this expression is a constant expression
     * (i.e. it is reducable to a constant and has no references to variables, maps, etc).
     *
     * @return true if this expression can be evaluated to a constant; false otherwise
     */
    public boolean isConstantExpr() {
        return left.isConstantExpr() && right.isConstantExpr();
    }

    /**
     * The <code>toString()</code> method recursively converts this expression to a string. For binary
     * operations, inner expressions will be nested within parentheses if their precedence is lower than
     * the precedence of the parent expression.
     *
     * @return a string representation of this expression
     */
    public String toString() {
        return innerString(left) + ' ' + operation + ' ' + innerString(right);
    }

    /**
     * The <code>getBinOp()</code> method returns a reference to the actual binary operation
     * represented by this abstract syntax tree node. Since all binary operations are represented
     * with the same type of syntax tree node, this field stores a reference to an implementation
     * of the actual operation (e.g. logical and or arithmetic addition).
     * @return a reference to the binary operation that this node represents
     */
    public BinOpImpl getBinOp() {
        return binop;
    }

    /**
     * The <code>setBinOp()</code> method sets the reference to the actual binary operation for this
     * node.
     * @param b the binary operation implementation for this node
     */
    public void setBinOp(BinOpImpl b) {
        binop = b;
    }

    /**
     * The <code>getPrecedence()</code> method gets the binding precedence for this expression. This is
     * used to compute when inner expressions must be nested within parentheses in order to preserve the
     * implied order of evaluation.
     *
     * @return an integer representing the precedence of this expression; higher numbers are higher
     *         precedence
     */
    public int getPrecedence() {
        if ( binop == null ) return -1;
        return binop.prec;
    }

    /**
     * The <code>getSourcePoint()</code> method returns a reference to a <code>SourcePoint</code>
     * instance that represents the location in the source program of this expression.
     * @return a <code>SourcePoint</code> instance that represents this expression in the
     * source program
     */
    public SourcePoint getSourcePoint() {
        return operation.getSourcePoint();
    }

    public SourcePoint getSourceRange() {
        return new SourcePoint(left.getSourcePoint(), right.getSourcePoint());
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

    /**
     * The <code>accept()</code> method implements one half of the visitor pattern for rebuilding of
     * expressions. This visitor allows code to be slightly modified while only writing visit methods for
     * the parts of the syntax tree affected.
     *
     * @param r the rebuilder to accept
     * @return the result of calling the appropriate <code>visit()</code> method of the rebuilder
     */
    public <Res, Env> Res accept(CodeAccumulator<Res, Env> r, Env env) {
        return r.visit(this, env);
    }

    /**
     * The <code>BinOpImpl</code> class represents a binary operation implementation. Descendants
     * of this class implement the functionality of a binary operator, including typechecking,
     * constant evaluation, etc.
     */
    public abstract static class BinOpImpl implements TypeCon.BinOp {
        public final String operation;
        public final int prec;

        protected BinOpImpl(String op, int p) {
            operation = op;
            this.prec = p;
        }

        public String getOperation() {
            return operation;
        }

        public abstract Literal evaluate(Literal left, Literal right);
        public abstract Type typeCheck(TypeEnv env, Typeable left, Typeable right);
    }

}
