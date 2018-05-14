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
 * The <code>UnOp</code> inner class represents an operation on a single integer value. For example, the
 * bitwise complement and the negation of an integer are operations on a single integer that produce a
 * single integer result.
 */
public class UnOpExpr extends Expr {
    /**
     * The <code>operation</code> field stores the string name of the operation being performed on the
     * expression. For example, '~' represents bitwise negation.
     */
    public final Token operation;

    /**
     * The <code>operand</code> field stores a reference to the expression operand of this operation.
     */
    public final Expr expr;

    /**
     * The <code>unop</code> field stores a reference to the actual unary operator implementation.
     */
    protected UnOpImpl unop;

    /**
     * The constructor of the <code>UnOp</code> class initializes the public final fields that form the
     * structure of this expression.
     *
     * @param op the string name of the operation
     * @param o  the operand of this operation
     */
    public UnOpExpr(Token op, Expr o) {
        expr = o;
        operation = op;
    }

    /**
     * The <code>isConstantExpr()</code> method tests whether this expression is a constant expression
     * (i.e. it is reducable to a constant and has no references to variables, maps, etc).
     *
     * @return true if this expression can be evaluated to a constant; false otherwise
     */
    public boolean isConstantExpr() {
        return expr.isConstantExpr();
    }

    /**
     * The <code>toString()</code> method recursively converts this expression to a string. For binary
     * operations, inner expressions will be nested within parentheses if their precedence is lower than
     * the precedence of the parent expression.
     *
     * @return a string representation of this expression
     */
    public String toString() {
        return operation + innerString(expr);
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
        return PREC_UN;
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
        return new SourcePoint(operation.getSourcePoint(), expr.getSourcePoint());
    }

    /**
     * The <code>getUnOp()</code> method gets a reference to the unary operation implementation
     * associated with this syntax tree node.
     * @return a reference to the unary operator implementation corresponding to this node
     */
    public UnOpImpl getUnOp() {
        return unop;
    }

    /**
     * The <code>setUnOp()</code> method allows update of the unary operation implementation for
     * this syntax tree node. For example, during typechecking, it is necessary to resolve the
     * correct unary operation for a particular node, based on the types, e.g.
     * @param un the unary operator implementation
     */
    public void setUnOp(UnOpImpl un) {
        unop = un;
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
     * The <code>UnOpImpl</code> class represents a unary operation implementation. Descendants
     * of this class implement the functionality of an unary operator, including typechecking,
     * constant evaluation, etc.
     */
    public abstract static class UnOpImpl implements TypeCon.UnOp {
        public final String operation;
        public final int prec;

        protected UnOpImpl(String op, int p) {
            operation = op;
            prec = p;
        }

        public String getOperation() {
            return operation;
        }

        public abstract Literal evaluate(Literal inner);
        public abstract Type typeCheck(TypeEnv env, Typeable inner);
    }
}
