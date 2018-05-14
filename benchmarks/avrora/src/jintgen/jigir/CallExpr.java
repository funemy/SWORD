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
import jintgen.isdl.SubroutineDecl;
import jintgen.isdl.parser.Token;
import java.util.List;

/**
 * The <code>CallExpr</code> class represents a subroutine call within the IR. Subroutines can be called for
 * side effects and produce results in the IR, allowing factoring of common pieces of code.
 *
 * @author Ben L. Titzer
 */
public class CallExpr extends Expr {

    /**
     * The <code>method</code> field stores a string that represents the name of the subroutine being called.
     */
    public final Token method;

    protected SubroutineDecl decl;

    /**
     * The <code>args</code> fields stores a reference to a list of expressions that are evaluated and passed
     * as arguments to the subroutine.
     */
    public final List<Expr> args;

    /**
     * The constructor of the <code>CallExpr</code> class simply initializes the references to the subroutine
     * name and arguments.
     *
     * @param m the name of the subroutine as a string
     * @param a list of expressions representing the arguments to the subroutine
     */
    public CallExpr(Token m, List<Expr> a) {
        method = m;
        args = a;
    }

    /**
     * The constructor of the <code>CallExpr</code> class simply initializes the references to the subroutine
     * name and arguments.
     *
     * @param m the name of the subroutine as a string
     * @param a list of expressions representing the arguments to the subroutine
     */
    public CallExpr(String m, List<Expr> a) {
        method = new Token();
        method.image = m;
        args = a;
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
        return StringUtil.embed(method.image, StringUtil.commalist(args));
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
        return method.getSourcePoint();
    }

    public void setDecl(SubroutineDecl d) {
        decl = d;
    }

    public SubroutineDecl getDecl() {
        return decl;
    }
}
