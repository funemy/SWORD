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

package jintgen.gen.disassembler;

import cck.text.Printer;
import cck.text.Verbose;
import cck.util.Arithmetic;
import jintgen.gen.ConstantPropagator;
import jintgen.isdl.*;
import jintgen.jigir.Expr;
import jintgen.jigir.Literal;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Ben L. Titzer
 */
public class EncodingInst {
    public static final byte ENC_ONE  = 1;
    public static final byte ENC_ZERO = 2;
    public static final byte ENC_MATCHED_ONE = 3;
    public static final byte ENC_MATCHED_ZERO = 4;
    public static final byte ENC_VAR  = 0;

    /**
     * The <code>Field</code> class represents a single expression within the encoding
     * of an instruction. The field has a length and an offset and is used in the disassembler
     * generator to generate the code to extract the operand from the bits of the instruction.
     *
     * @author Ben L. Titzer
     */
    public class Field {
        final int bitsize;
        final Expr expr;
        final int offset;

        Field(int o, int s, Expr e) {
            offset = o;
            expr = e;
            bitsize = s;
        }
    }

    final InstrDecl instr;
    final AddrModeDecl addrMode;
    final FormatDecl encoding;
    final byte[] bitStates;
    final List<Field> simplifiedExprs;


    EncodingInst(InstrDecl id, AddrModeDecl am, FormatDecl ed) {
        instr = id;
        addrMode = am;
        bitStates = new byte[ed.bitWidth];
        simplifiedExprs = new LinkedList<Field>();
        encoding = ed;

        initializeBitStates();
    }

    EncodingInst(EncodingInst prev) {
        instr = prev.instr;
        addrMode = prev.addrMode;
        bitStates = new byte[prev.bitStates.length];
        simplifiedExprs = prev.simplifiedExprs;
        encoding = prev.encoding;
        System.arraycopy(prev.bitStates, 0, bitStates, 0, bitStates.length);
    }

    public String toString() {
        return instr.name + " x "+addrMode.name;
    }

    private void initializeBitStates() {
        FormatDecl ed = encoding;
        // create a constant propagator needed to evaluate integer literals and operands
        ConstantPropagator cp = new ConstantPropagator();
        ConstantPropagator.Environ ce = cp.createEnvironment();

        List<FormatDecl.BitField> fields = DGUtil.initConstantEnviron(ce, instr, addrMode, ed);

        // scan through the expressions corresponding to the fields that make up this encoding
        // and initialize the bitState array to either ENC_ONE, ENC_ZERO, or ENC_VAR
        int offset = 0;
        for ( FormatDecl.BitField e : fields ) {
            // get the bit width of the parent encoding field
            int size = e.getWidth();
            // evaluate the parent encoding expression, given values for operands
            Expr simpleExpr = e.field.accept(cp,ce);

            addExpr(offset, size, simpleExpr);
            offset += size;
        }
    }

    private void addExpr(int offset, int size, Expr simpleExpr) {
        // store the expression for future use
        Field ee = new Field(offset, size, simpleExpr);
        simplifiedExprs.add(ee);

        setBitStates(simpleExpr, size, offset);
    }

    private void setBitStates(Expr simpleExpr, int size, int offset) {
        // if this field corresponds to an integer literal, initialize each bit to
        // either ENC_ZERO or ENC_ONE
        if ( simpleExpr instanceof Literal.IntExpr ) {
            Literal.IntExpr l = (Literal.IntExpr)simpleExpr;
            for ( int cntr = 0; cntr < size; cntr++) {
                boolean bit = Arithmetic.getBit(l.value, size-cntr-1);
                bitStates[offset++] = bit ? ENC_ONE : ENC_ZERO;
            }
        } else if (simpleExpr instanceof Literal.BoolExpr) {
            // if it is a boolean literal, initialize one bit
            Literal.BoolExpr l = (Literal.BoolExpr)simpleExpr;
            bitStates[offset] = l.value ? ENC_ONE : ENC_ZERO;
        } else {
            // not a known value; initialize each bit to variable
            for ( int cntr = 0; cntr < size; cntr++) {
                bitStates[offset++] = ENC_VAR;
            }
        }
    }

    void print(int indent, Printer p) {
        for ( int cntr = 0; cntr < indent; cntr++ )
            p.print("    ");
        p.print(DGUtil.toString(this));
        p.nextln();
    }

    void printVerbose(int indent, Verbose.Printer p) {
        if ( !p.enabled ) return;
        print(indent, p);
    }

    public int getLength() {
        return bitStates.length;
    }

    public EncodingInst copy() {
        return new EncodingInst(this);
    }

    public boolean isConcrete(int bit) {
        byte bitState = bitStates[bit];
        return bitState == ENC_ONE || bitState == ENC_ZERO;
    }
}
