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
 * The <code>CodeVisitor</code> interface represents a visitor that is more specific than the
 * <code>ExprVisitor</code> visitor, in that it contains visit methods for every type of arithmetic and
 * logical operation in the IR.
 *
 * @author Ben L. Titzer
 */
public interface CodeVisitor {

    public void visit(BinOpExpr e);

    public void visit(UnOpExpr e);

    public void visit(IndexExpr e);

    public void visit(FixedRangeExpr e);

    public void visit(CallExpr e);

    public void visit(ReadExpr e);

    public void visit(ConversionExpr e);

    public void visit(Literal.BoolExpr e);

    public void visit(Literal.IntExpr e);

    public void visit(Literal.EnumVal e);

    public void visit(VarExpr e);

    public void visit(DotExpr e);


    /**
     * The <code>DepthFirst</code> class is a base implementation of the <code>CodeVisitor</code> interface
     * that visits the tree in depth-first order.
     *
     * @author Ben L. Titzer
     */
    public class DepthFirst implements CodeVisitor {

        public void visit(BinOpExpr e) {
            e.left.accept(this);
            e.right.accept(this);
        }

        public void visit(IndexExpr e) {
            e.expr.accept(this);
            e.index.accept(this);
        }

        public void visit(FixedRangeExpr e) {
            e.expr.accept(this);
        }

        public void visit(CallExpr e) {
            for ( Expr a : e.args ) {
                a.accept(this);
            }
        }

        public void visit(ReadExpr e) {
            // terminal node
        }

        public void visit(ConversionExpr e) {
            e.expr.accept(this);
        }

        public void visit(Literal.BoolExpr e) {
            // terminal node in the tree
        }

        public void visit(Literal.IntExpr e) {
            // terminal node in the tree
        }

        public void visit(Literal.EnumVal e) {
            // terminal node in the tree
        }

        public void visit(UnOpExpr e) {
            e.expr.accept(this);
        }

        public void visit(VarExpr e) {
            // terminal node in the tree
        }

        public void visit(DotExpr e) {
            e.expr.accept(this);
        }
    }

    /**
     * The <code>Default</code> class is a base implementation of the <code>CodeVisitor</code> interface
     * that visits the tree in depth-first order.
     *
     * @author Ben L. Titzer
     */
    public abstract class Default implements CodeVisitor {

        public abstract void error(Expr e);

        public void visit(BinOpExpr e) {
            error(e);
        }

        public void visit(IndexExpr e) {
            error(e);
        }

        public void visit(FixedRangeExpr e) {
            error(e);
        }

        public void visit(CallExpr e) {
            error(e);
        }

        public void visit(ReadExpr e) {
            error(e);
        }

        public void visit(ConversionExpr e) {
            error(e);
        }

        public void visit(Literal.BoolExpr e) {
            error(e);
        }

        public void visit(Literal.IntExpr e) {
            error(e);
        }

        public void visit(Literal.EnumVal e) {
            error(e);
        }

        public void visit(UnOpExpr e) {
            error(e);
        }

        public void visit(VarExpr e) {
            error(e);
        }

        public void visit(DotExpr e) {
            error(e);
        }

    }
}
