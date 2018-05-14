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
 */

package jintgen.jigir;

import cck.parser.SourcePoint;
import jintgen.isdl.parser.Token;

/**
 * The <code>DotExpr</code> class represents an access of a field of an operand
 * (or addressing mode). For example, <code>a.f</code>.
 *
 * @author Ben L. Titzer
 */
public class DotExpr extends Expr {

    /**
     * The <code>operand</code> field stores a reference to the name of the operand
     * or variable that has the field.
     */
    public final Expr expr;

    /**
     * The <code>field</code> field stores the name of the field being accessed.
     */
    public final Token field;

    public DotExpr(Expr e, Token f) {
        expr = e;
        field = f;
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
     * The <code>getPrecedence()</code> method returns a number representing the precedence (order
     * of operations) for this operator. This is used to correct nest the operators with parentheses
     * when printing them out, for example.
     * @return an precedence number for this operation
     */
    public int getPrecedence() {
        return PREC_TERM;
    }

    public SourcePoint getSourcePoint() {
        return new SourcePoint(expr.getSourcePoint(), field.getSourcePoint());
    }
}
