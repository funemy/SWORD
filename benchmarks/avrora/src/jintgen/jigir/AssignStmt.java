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

/**
 * The <code>AssignStmt</code> class represents an assignment statement in the IR. This is an abstract class
 * whose subclasses represent the different cases of assignments: assignments to locals, to maps, and to
 * ranges of bits, individual bits, etc.
 *
 * @author Ben L. Titzer
 */
public class AssignStmt extends Stmt {

    /**
     * The <code>dest</code> field stores a reference to the expression (which must be an Lvalue)
     * that specifies the location to store the value to.
     */
    public final Expr dest;

    /**
     * The <code>expr</code> field stores a reference to the expression whose result is assigned to the left
     * hand side.
     */
    public final Expr expr;

    /**
     * The constructor of the <code>AssignStmt</code> class simply stores a reference to the right hand side
     * expression internally.
     *
     * @param r the expression representing the right hand side of the assignment
     */
    public AssignStmt(Expr d, Expr r) {
        dest = d;
        expr = r;
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
     * The <code>accept()</code> method implements one half of the visitor pattern for visiting the abstract
     * syntax trees representing the code of a particular instruction or subroutine. The
     * <code>StmtRebuilder</code> interface allows visitors to rearrange and rebuild the statements.
     *
     * @param v the visitor to accept
     * @return the result of calling the appropriate <code>visit()</code> of the rebuilder passed
     */
    public <Res, Env> Res accept(StmtAccumulator<Res,Env> v, Env env) {
        return v.visit(this, env);
    }

    public static class Var extends Stmt {
        public final VarExpr dest;
        public final Expr expr;

        public Var(VarExpr d, Expr e) {
            dest = d;
            expr = e;
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
         * The <code>accept()</code> method implements one half of the visitor pattern for visiting the abstract
         * syntax trees representing the code of a particular instruction or subroutine. The
         * <code>StmtRebuilder</code> interface allows visitors to rearrange and rebuild the statements.
         *
         * @param v the visitor to accept
         * @return the result of calling the appropriate <code>visit()</code> of the rebuilder passed
         */
        public <Res, Env> Res accept(StmtAccumulator<Res,Env> v, Env env) {
            return v.visit(this, env);
        }
    }

    public static class Map extends Stmt {
        public final Expr map;
        public final Expr index;
        public final Expr expr;

        public Map(Expr m, Expr i, Expr e) {
            map = m;
            index = i;
            expr = e;
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
         * The <code>accept()</code> method implements one half of the visitor pattern for visiting the abstract
         * syntax trees representing the code of a particular instruction or subroutine. The
         * <code>StmtRebuilder</code> interface allows visitors to rearrange and rebuild the statements.
         *
         * @param v the visitor to accept
         * @return the result of calling the appropriate <code>visit()</code> of the rebuilder passed
         */
        public <Res, Env> Res accept(StmtAccumulator<Res,Env> v, Env env) {
            return v.visit(this, env);
        }
    }

    public static class Bit extends Stmt {
        public final VarExpr dest;
        public final Expr bit;
        public final Expr expr;

        public Bit(VarExpr d, Expr b, Expr e) {
            dest = d;
            bit = b;
            expr = e;
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
         * The <code>accept()</code> method implements one half of the visitor pattern for visiting the abstract
         * syntax trees representing the code of a particular instruction or subroutine. The
         * <code>StmtRebuilder</code> interface allows visitors to rearrange and rebuild the statements.
         *
         * @param v the visitor to accept
         * @return the result of calling the appropriate <code>visit()</code> of the rebuilder passed
         */
        public <Res, Env> Res accept(StmtAccumulator<Res,Env> v, Env env) {
            return v.visit(this, env);
        }
    }

    public static class FixedRange extends Stmt {
        public final VarExpr dest;
        public final int low_bit;
        public final int high_bit;
        public final Expr expr;

        public FixedRange(VarExpr d, int l, int h, Expr e) {
            dest = d;
            low_bit = l;
            high_bit = h;
            expr = e;
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
         * The <code>accept()</code> method implements one half of the visitor pattern for visiting the abstract
         * syntax trees representing the code of a particular instruction or subroutine. The
         * <code>StmtRebuilder</code> interface allows visitors to rearrange and rebuild the statements.
         *
         * @param v the visitor to accept
         * @return the result of calling the appropriate <code>visit()</code> of the rebuilder passed
         */
        public <Res, Env> Res accept(StmtAccumulator<Res,Env> v, Env env) {
            return v.visit(this, env);
        }
    }
}
