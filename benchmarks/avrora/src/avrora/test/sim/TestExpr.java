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
 * Creation date: Nov 29, 2005
 */

package avrora.test.sim;

import avrora.core.Program;
import avrora.core.SourceMapping;

/**
 * @author Ben L. Titzer
 */
abstract class TestExpr {
    public abstract int evaluate(StateAccessor access);
    public void set(StateAccessor access, int val) {
        // do nothing.
    }

    static class Index extends TestExpr {
        final String name;
        final TestExpr index;
        Index(String name, TestExpr i) {
            this.name = name;
            this.index = i;
        }
        public int evaluate(StateAccessor access) {
            int indv = index.evaluate(access);
            return access.getIndex(name, indv);
        }
        public String toString() {
            return name+"["+index+"]";
        }
        public void set(StateAccessor access, int val) {
            int indv = index.evaluate(access);
            access.setIndex(name, indv, val);
        }
    }

    static class Var extends TestExpr {
        final String name;
        Var(String name) {
            this.name = name;
        }
        public int evaluate(StateAccessor access) {
            return access.get(name);
        }
        public String toString() {
            return name;
        }
        public void set(StateAccessor access, int val) {
            access.set(name, val);
        }
    }

    static class Label extends TestExpr {
        String name;

        Label(String n) {
            name = n;
        }

        public int evaluate(StateAccessor access) {
            Program p = access.getProgram();
            SourceMapping smap = p.getSourceMapping();
            SourceMapping.Location l = smap.getLocation(name);
            if (l == null) throw new UnknownLabel(name);
            return l.lma_addr;
        }

        public String toString() {
            return name;
        }
    }

    static class Const extends TestExpr {
        int value;

        Const(int v) {
            value = v;
        }

        public int evaluate(StateAccessor access) {
            return value;
        }

        public String toString() {
            return Integer.toString(value);
        }
    }

    abstract static class BinOp extends TestExpr {
        TestExpr left, right;
        String op;

        BinOp(TestExpr l, TestExpr r, String o) {
            left = l;
            right = r;
            op = o;
        }

        public String toString() {
            return left + op + right;
        }
    }

    static class Add extends BinOp {

        Add(TestExpr l, TestExpr r) {
            super(l, r, "+");
        }

        public int evaluate(StateAccessor access) {
            int lval = left.evaluate(access);
            int rval = right.evaluate(access);
            return lval + rval;
        }
    }

    static class Subtract extends BinOp {

        Subtract(TestExpr l, TestExpr r) {
            super(l, r, "-");
        }

        public int evaluate(StateAccessor access) {
            int lval = left.evaluate(access);
            int rval = right.evaluate(access);
            return lval - rval;
        }
    }

    static class UnknownLabel extends RuntimeException {
        String name;

        UnknownLabel(String n) {
            name = n;
        }
    }
}
