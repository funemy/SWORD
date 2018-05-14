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

import cck.text.StringUtil;
import java.util.List;

/**
 * The <code>IfStmt</code> class represents a simple branch within the IR. Since loops and switch statements
 * are not allowed, if statements (and subroutine calls) are the only form of control flow.
 *
 * @author Ben L. Titzer
 */
public class IfStmt extends Stmt {
    /**
     * The <code>cond</code> field stores a reference to the expression that is evaluated as the condition
     * determining which branch is executed.
     */
    public final Expr cond;

    /**
     * The <code>trueBranch</code> field stores a reference to the list of statements to be executed if the
     * condition is true.
     */
    public final List<Stmt> trueBranch;

    /**
     * The <code>falseBranch</code> field stores a reference to the list of statements to be executed if the
     * condition is false.
     */
    public final List<Stmt> falseBranch;

    /**
     * The constructor of the <code>IfStmt</code> class simply initializes the internal fields based on the
     * parameters.
     *
     * @param c a reference to the expression representing the condition
     * @param t a reference to the list of statements to execute if the condition evaluates to true
     * @param f a reference to the list of statements to execute if the condition evaluates to false
     */
    public IfStmt(Expr c, List<Stmt> t, List<Stmt> f) {
        cond = c;
        trueBranch = t;
        falseBranch = f;
    }

    /**
     * The <code>accept()</code> method implements one half of the visitor pattern for visiting the abstract
     * syntax trees representing the code of a particular instruction or subroutine.
     *
     * @param v the visitor to accept
     */
    public void accept(StmtVisitor v) {
        v.visit(this);
    }

    /**
     * The <code>toString()</code> method recursively converts this statement to a string.
     *
     * @return a string representation of this statement
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("if ( ");
        buf.append(cond);
        buf.append(" ) {\n");
        StringUtil.linelist(buf, trueBranch);
        buf.append("} else {\n");
        StringUtil.linelist(buf, falseBranch);
        buf.append('}');
        return buf.toString();
    }

    /**
     * The <code>accept()</code> method implements one half of the visitor pattern for visiting the abstract
     * syntax trees representing the code of a particular instruction or subroutine. The
     * <code>StmtRebuilder</code> interface allows visitors to rearrange and rebuild the statements.
     *
     * @param r the visitor to accept
     * @return the result of calling the appropriate <code>visit()</code> of the rebuilder passed
     */
    public <Res, Env> Res accept(StmtAccumulator<Res, Env> r, Env env) {
        return r.visit(this, env);
    }

}
