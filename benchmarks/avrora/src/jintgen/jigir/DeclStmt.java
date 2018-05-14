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

import jintgen.isdl.parser.Token;
import jintgen.types.Type;
import jintgen.types.TypeRef;

/**
 * The <code>DeclStmt</code> represents a declaration of a local, temporary value in the IR. A named temporary
 * is given a type and an initial value at declaration time, allowing typechecking and ensuring that every
 * variable is initialized before it is used.
 *
 * @author Ben L. Titzer
 */
public class DeclStmt extends Stmt implements Decl {
    /**
     * The <code>name</code> field stores a reference to the name of the local.
     */
    public final Token name;

    /**
     * The <code>type</code> field stores a reference to a token representing the type of the local.
     */
    public final TypeRef typeRef;

    /**
     * The <code>init</code> field stores a reference to the expression which is evaluated to give an initial
     * value to the local.
     */
    public final Expr init;

    /**
     * The constructor of the <code>DeclStmt</code> class initializes the references to the name, type, and
     * initial value of the declared local.
     *
     * @param n the name of the local as a token
     * @param t the type of the local as a token
     * @param i a reference to the expression evaluated to give the local an initial value
     */
    public DeclStmt(Token n, TypeRef t, Expr i) {
        name = n;
        typeRef = t;
        init = i;
    }

    /**
     * The constructor of the <code>DeclStmt</code> class initializes the references to the name, type, and
     * initial value of the declared local.
     *
     * @param n the name of the local as a string
     * @param t the type of the local as a token
     * @param i a reference to the expression evaluated to give the local an initial value
     */
    public DeclStmt(String n, TypeRef t, Expr i) {
        name = new Token();
        name.image = n;
        typeRef = t;
        init = i;
    }

    /**
     * The constructor of the <code>DeclStmt</code> class initializes the references to the name, type, and
     * initial value of the declared local.
     *
     * @param n the name of the local as a string
     * @param t the type of the local as a token
     * @param i a reference to the expression evaluated to give the local an initial value
     */
    public DeclStmt(String n, String t, Expr i) {
        name = new Token();
        name.image = n;
        Token tok = new Token();
        tok.image = t;
        typeRef = new TypeRef(tok);
        init = i;
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
        return "local " + name.image + " : " + typeRef + " = " + init.toString() + ';';
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

    public String getName() {
        return name.image;
    }

    public Type getType() {
        return typeRef.getType();
    }
}
