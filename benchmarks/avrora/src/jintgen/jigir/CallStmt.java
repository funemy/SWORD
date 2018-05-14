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
import jintgen.isdl.SubroutineDecl;
import jintgen.isdl.parser.Token;
import java.util.List;

/**
 * The <code>CallStmt</code> class represents a call to a subroutine that does not produce a value.
 *
 * @author Ben L. Titzer
 */
public class CallStmt extends Stmt {

    /**
     * The <code>method</code> field stores a string that represents the name of the subroutine being called.
     */
    public final Token method;

    /**
     * The <code>args</code> fields stores a reference to a list of expressions that are evaluated and passed
     * as arguments to the subroutine.
     */
    public final List<Expr> args;

    protected SubroutineDecl decl;

    /**
     * The constructor of the <code>CallStmt</code> class simply initializes the references to the subroutine
     * name and arguments.
     *
     * @param m the name of the subroutine as a string
     * @param a list of expressions representing the arguments to the subroutine
     */
    public CallStmt(Token m, List<Expr> a) {
        method = m;
        args = a;
    }

    /**
     * The constructor of the <code>CallStmt</code> class simply initializes the references to the subroutine
     * name and arguments.
     *
     * @param m the name of the subroutine as a string
     * @param a list of expressions representing the arguments to the subroutine
     */
    public CallStmt(String m, List<Expr> a) {
        method = new Token();
        method.image = m;
        args = a;
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
        return StringUtil.embed(method.image, StringUtil.commalist(args)) + ';';
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

    public void setDecl(SubroutineDecl d) {
        decl = d;
    }

    public SubroutineDecl getDecl() {
        return decl;
    }
}
