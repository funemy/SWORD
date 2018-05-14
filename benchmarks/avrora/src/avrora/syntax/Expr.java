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

package avrora.syntax;

import cck.parser.AbstractToken;
import cck.text.StringUtil;
import cck.util.Util;

/**
 * The <code>Expr</code> class represents an expression within the program that must be evaluated to a value.
 * This could be an integer constant, a computable expression involving arithmetic operators, a variable, or
 * an expression relative to the current position within the program.
 *
 * @author Ben L. Titzer
 */
public abstract class Expr extends ASTNode {

    /**
     * The <code>evaluate()</code> method computes the value of the expression in this context and returns its
     * value.
     *
     * @param currentByteAddress the current byte address within the program
     * @param c                  the context in which to evaluate this expression
     * @return the value of the expression as a 32-bit integer
     */
    public abstract int evaluate(int currentByteAddress, Context c);

    private static int asInt(boolean b) {
        return b ? 1 : 0;
    }

    private static boolean asBool(int i) {
        return i != 0;
    }

    /**
     * The <code>BinOp</code> class represents a simple binary arithmetic operator such as addition,
     * multiplication, etc. It contains two internal expressions, the left and right.
     */
    public static class BinOp extends Expr {

        /**
         * The <code>op</code> field records the token that corresponds to the actual arithmetic operator.
         */
        public final AbstractToken op;

        /**
         * The <code>left</code> field records an expression that represents the operand on the left side of
         * the operator.
         */
        public final Expr left;

        /**
         * The <code>right</code> field records an expression that represents the operand on the right side of
         * the operator.
         */
        public final Expr right;

        public BinOp(AbstractToken tok, Expr l, Expr r) {
            op = tok;
            left = l;
            right = r;
        }

        /**
         * The <code>evaluate()</code> method computes the value of the expression in this context and returns
         * its value. This implementation works straightforwardly by first evaluating the left operand, then
         * the right, and then computing the result of the arithmetic operation.
         *
         * @param currentByteAddress the current byte address within the program
         * @param c                  the context in which to evaluate this expression
         * @return the value of the expression as a 32-bit integer
         */
        public int evaluate(int currentByteAddress, Context c) {
            int lval = left.evaluate(currentByteAddress, c);
            int rval = right.evaluate(currentByteAddress, c);
            String o = op.image;

            // TODO: this is a bit ugly.
            if ("*".equals(o)) return lval * rval;
            if ("/".equals(o)) return lval / rval;
            if ("-".equals(o)) return lval - rval;
            if ("+".equals(o)) return lval + rval;
            if ("<<".equals(o)) return lval << rval;
            if (">>".equals(o)) return lval >> rval;
            if ("<".equals(o)) return asInt(lval < rval);
            if (">".equals(o)) return asInt(lval > rval);
            if ("<=".equals(o)) return asInt(lval <= rval);
            if (">=".equals(o)) return asInt(lval >= rval);
            if ("==".equals(o)) return asInt(lval == rval);
            if ("!=".equals(o)) return asInt(lval != rval);
            if ("&".equals(o)) return lval & rval;
            if ("^".equals(o)) return lval ^ rval;
            if ("|".equals(o)) return lval | rval;
            if ("&&".equals(o)) return asInt(asBool(lval) && asBool(rval));
            if ("||".equals(o)) return asInt(asBool(lval) || asBool(rval));

            throw Util.failure("unknown binary operator: " + op);
        }

        public AbstractToken getLeftMostToken() {
            return left.getLeftMostToken();
        }

        public AbstractToken getRightMostToken() {
            return right.getRightMostToken();
        }

        public String toString() {
            return StringUtil.embed(op.image, left, right);
        }

    }

    /**
     * The <code>UnOp</code> class represents an expression that is a single operand with a unary operation
     * applied to it. For example, "!" takes the logical complement of an operand, "~" takes the bitwise
     * complement, etc.
     */
    public static class UnOp extends Expr {

        public final AbstractToken op;
        public final Expr operand;

        public UnOp(AbstractToken tok, Expr oper) {
            op = tok;
            operand = oper;
        }

        /**
         * The <code>evaluate()</code> method computes the value of the expression in this context and returns
         * its value. This implementation works straightforwardly by first evaluating the operand and then
         * computing the result of the arithmetic operation.
         *
         * @param currentByteAddress the current byte address within the program
         * @param c                  the context in which to evaluate this expression
         * @return the value of the expression as a 32-bit integer
         */
        public int evaluate(int currentByteAddress, Context c) {
            int oval = operand.evaluate(currentByteAddress, c);
            String o = op.image;

            if ("!".equals(o)) return asInt(!asBool(oval));
            if ("~".equals(o)) return ~oval;
            if ("-".equals(o)) return -oval;

            throw Util.failure("unknown unary operator: " + op);
        }

        public AbstractToken getLeftMostToken() {
            return op;
        }

        public AbstractToken getRightMostToken() {
            return operand.getRightMostToken();
        }

        public String toString() {
            return StringUtil.embed(op.image, operand);
        }
    }

    /**
     * The <code>Func</code> class represents a builtin function that is applied to an operand. For example, a
     * function might be "high" which returns the high byte of a 16-bit operand, "log2" which returns the
     * logarithm, etc.
     */
    public static class Func extends Expr {

        public final AbstractToken func;
        public final Expr argument;
        public final AbstractToken last;

        public Func(AbstractToken tok, Expr arg, AbstractToken l) {
            func = tok;
            argument = arg;
            last = l;
        }

        /**
         * The <code>evaluate()</code> method computes the value of the expression in this context and returns
         * its value. This implementation works straightforwardly by first evaluating the operand, and then
         * computing the result of the function.
         *
         * @param currentByteAddress the current byte address within the program
         * @param c                  the context in which to evaluate this expression
         * @return the value of the expression as a 32-bit integer
         */
        public int evaluate(int currentByteAddress, Context c) {
            int aval = argument.evaluate(currentByteAddress, c);
            String f = func.image;

            // TODO: verify correctness of these functions
            if ("byte".equalsIgnoreCase(f)) return aval & 0xff;
            if ("low".equalsIgnoreCase(f) || "lo8".equals(f)) return aval & 0xff;
            if ("high".equalsIgnoreCase(f) || "hi8".equals(f)) return (aval >>> 8) & 0xff;
            if ("byte2".equalsIgnoreCase(f)) return (aval >>> 8) & 0xff;
            if ("byte3".equalsIgnoreCase(f)) return (aval >>> 16) & 0xff;
            if ("byte4".equalsIgnoreCase(f)) return (aval >>> 24) & 0xff;
            if ("lwrd".equalsIgnoreCase(f)) return aval & 0xffff;
            if ("hwrd".equalsIgnoreCase(f)) return (aval >>> 16) & 0xffff;
            if ("page".equalsIgnoreCase(f)) return (aval >>> 16) & 0x3f;
            if ("exp2".equalsIgnoreCase(f)) return 1 << aval;
            if ("log2".equalsIgnoreCase(f)) return log(aval);

            throw Util.failure("unknown function: " + func);
        }

        private int log(int val) {
            int log = 1;

            // TODO: verify correctness of this calculation
            if ((val & 0xffff0000) != 0) {
                log += 16;
                val = val >> 16;
            }
            if ((val & 0xffff00) != 0) {
                log += 8;
                val = val >> 8;
            }
            if ((val & 0xffff0) != 0) {
                log += 4;
                val = val >> 4;
            }
            if ((val & 0xffffc) != 0) {
                log += 2;
                val = val >> 2;
            }
            if ((val & 0xffffe) != 0) {
                log += 1;
            }


            return log;
        }

        public AbstractToken getLeftMostToken() {
            return func;
        }

        public AbstractToken getRightMostToken() {
            return last;
        }

        public String toString() {
            return StringUtil.embed(func.image, argument);
        }

    }

    /**
     * The <code>Term</code> class is a superclass for all expressions that consist of a single lexical
     * token.
     */
    public abstract static class Term extends Expr {

        public final AbstractToken token;

        protected Term(AbstractToken tok) {
            token = tok;
        }

        public AbstractToken getLeftMostToken() {
            return token;
        }

        public AbstractToken getRightMostToken() {
            return token;
        }

        public String toString() {
            return token.image;
        }
    }

    /**
     * The <code>Variable</code> class represents a variable reference within the program.
     */
    public static class Variable extends Term {

        public Variable(AbstractToken n) {
            super(n);
        }

        /**
         * The <code>evaluate()</code> method computes the value of the expression in this context and returns
         * its value. This implementation works straightforwardly by looking up the variable in the context
         * and returning its value.
         *
         * @param currentByteAddress the current byte address within the program
         * @param c                  the context in which to evaluate this expression
         * @return the value of the expression as a 32-bit integer
         */
        public int evaluate(int currentByteAddress, Context c) {
            return c.getVariable(token);
        }

    }

    /**
     * The <code>Constant</code> class represents a integer literal (a constant) within the program.
     */
    public static class Constant extends Term {

        public final int value;

        public Constant(AbstractToken tok) {
            super(tok);
            value = evaluateLiteral(tok.image);
        }

        /**
         * The <code>evaluate()</code> method computes the value of the expression in this context and returns
         * its value. Since this is a constant, it simply returns its value.
         *
         * @param currentByteAddress the current byte address within the program
         * @param c                  the context in which to evaluate this expression
         * @return the value of the expression as a 32-bit integer
         */
        public int evaluate(int currentByteAddress, Context c) {
            return value;
        }

        private static int evaluateLiteral(String val) {
            if (val.charAt(0) == '$')                          // hexadecimal
                return Integer.parseInt(val.substring(1), 16);
            else {
                try {
                    return StringUtil.evaluateIntegerLiteral(val);
                } catch (Exception e) {
                    throw Util.unexpected(e);
                }
            }
        }
    }

    /**
     * The <code>StringLiteral</code> class represents a string literal within the program. A string literal
     * can be used within a list of initialized data and occupies a span of bytes. However, it cannot be
     * evaluated to an integer. It is treated specifially in the simplification phase.
     */
    public static class StringLiteral extends Term {

        public final String value;

        public StringLiteral(AbstractToken tok) {
            super(tok);
            try {
                value = StringUtil.evaluateStringLiteral(tok.image);
            } catch (Exception e) {
                throw Util.unexpected(e);
            }
        }

        /**
         * The <code>evaluate()</code> method computes the value of the expression in this context and returns
         * its value. A string cannot be evaluated to a 32-bit integer; this method throws an exception.
         *
         * @param currentByteAddress the current byte address within the program
         * @param c                  the context in which to evaluate this expression
         * @return the value of the expression as a 32-bit integer
         * @throws Util.InternalError because a string cannot be evaluated to a 32-bit integer
         */
        public int evaluate(int currentByteAddress, Context c) {
            throw Util.failure("cannot evaluate a string to an integer");
        }
    }

    /**
     * The <code>RelativeAddress</code> class represents an expression that is derived from the addition (or
     * subtraction) of a constant to the current byte address. This occurs in the GAS formats and Objdump.
     */
    public static class RelativeAddress extends Expr {

        public final AbstractToken dot;
        public final AbstractToken op;
        public final AbstractToken num;

        public RelativeAddress(AbstractToken d, AbstractToken o, AbstractToken n) {
            dot = d;
            op = o;
            num = n;
        }

        public AbstractToken getLeftMostToken() {
            return dot;
        }

        public AbstractToken getRightMostToken() {
            return num;
        }

        public String toString() {
            return '.' + op.image + num.image;
        }

        /**
         * The <code>evaluate()</code> method computes the value of the expression in this context and returns
         * its value. Since this is a relative address, it simply evaluates the offset and adds it to the
         * current address in the program and returns that value.
         *
         * @param currentByteAddress the current byte address within the program
         * @param c                  the context in which to evaluate this expression
         * @return the value of the expression as a 32-bit integer
         */
        public int evaluate(int currentByteAddress, Context c) {
            int offset = StringUtil.evaluateIntegerLiteral(num.image);
            if ("+".equals(op.image)) return currentByteAddress + offset;
            if ("-".equals(op.image)) return currentByteAddress - offset;

            throw Util.failure("unknown operation in relative computation: " + op.image);
        }
    }

}
