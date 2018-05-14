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

import avrora.arch.legacy.LegacyInstrProto;
import cck.parser.AbstractToken;
import cck.util.Util;

/**
 * The <code>Item</code> class represents either an assembler directive, an instruction, or a sequence of
 * initialized data with a source program.
 */
public abstract class Item {

    protected final Module module;
    protected final Module.Seg segment;
    protected final int byteAddress;

    /**
     * The <code>simplify()</code> method reduces any computable constants to values, resolves register
     * aliases, and creates instruction instances within this item, depending on exactly which type of item it
     * is.
     */
    public abstract void simplify();

    protected Item(Module.Seg seg) {
        byteAddress = seg.getCurrentAddress();
        segment = seg;
        module = seg.getModule();
    }

    public int itemSize() {
        return 0;
    }

    /**
     * The <code>NamedConstant</code> item in a source program represents a directive that assigns a
     * computable value to a name.
     */
    public static class NamedConstant extends Item {

        private final AbstractToken name;
        private final Expr value;

        NamedConstant(Module.Seg s, AbstractToken n, Expr v) {
            super(s);
            name = n;
            value = v;
        }

        public void simplify() {
            int result = value.evaluate(byteAddress, module);
            module.addVariable(name.image, result);
        }

        public String toString() {
            return ".equ " + name;
        }
    }

    /**
     * The <code>RegisterAlias</code> item in a source program represents a directive that adds an alias for a
     * register. This can appear in program, data, and eeprom segments.
     */
    public static class RegisterAlias extends Item {

        private final AbstractToken name;
        private final AbstractToken register;

        RegisterAlias(Module.Seg s, AbstractToken n, AbstractToken r) {
            super(s);
            name = n;
            register = r;
        }

        public void simplify() {
            module.addRegisterName(name.image, register);
        }

        public String toString() {
            return ".def " + name + " = " + register;
        }
    }


    /**
     * The <code>Instruction</code> item in a source program represents an instruction that must be simplified
     * and added to the program. This is generally only applicable to the program (code) section.
     */
    public static class Instruction extends Item {

        protected final String variant;
        protected final AbstractToken name;
        protected final SyntacticOperand[] operands;
        protected final LegacyInstrProto proto;

        Instruction(Module.Seg s, String v, AbstractToken n, LegacyInstrProto p, SyntacticOperand[] ops) {
            super(s);
            variant = v;
            name = n;
            operands = ops;
            proto = p;
        }

        public void simplify() {

            // TODO: define correct error for this
            if ((byteAddress & 0x1) == 0x1) throw Util.failure("misaligned instruction");

            for (int cntr = 0; cntr < operands.length; cntr++)
                operands[cntr].simplify(byteAddress + proto.getSize(), module);

            segment.writeInstr(name, byteAddress, proto.build(byteAddress >> 1, operands));
        }

        public int itemSize() {
            return proto.getSize();
        }

        public String toString() {
            return "instr: " + variant + " @ " + byteAddress;
        }
    }

    /**
     * The <code>Label</code> item represents a labelled location in the program that is given a name. This
     * can appear in program, data, or eeprom sections.
     */
    public static class Label extends Item {

        private final AbstractToken name;

        Label(Module.Seg s, AbstractToken n) {
            super(s);
            name = n;
        }

        public void simplify() {
            segment.addLabel(name.image, byteAddress, byteAddress);
        }

        public int getByteAddress() {
            return byteAddress;
        }

        public String toString() {
            return "label: " + name + " in " + segment.getName() + " @ " + byteAddress;
        }
    }

    /**
     * The <code>InitializedData</code> item represents a section of programmer-declared initialized data
     * within the program. This is generally only applicable to the program (code) section.
     */
    public static class InitializedData extends Item {

        private final ExprList list;
        private final int width;
        private final int size;

        InitializedData(Module.Seg s, ExprList l, int w) {
            super(s);
            list = l;
            width = w;
            size = computeSize(l, w);
        }

        public void simplify() {
            int cursor = byteAddress;

            for (ExprList.ExprItem item = list.head; item != null; item = item.next) {
                Expr e = item.expr;
                if (e instanceof Expr.StringLiteral) {
                    cursor = writeString(e, segment, cursor);
                } else {
                    cursor = writeValue(e, module, segment, cursor);
                }
            }
        }

        private int writeValue(Expr e, Module module, Module.Seg s, int cursor) {
            int val = e.evaluate(byteAddress, module);
            for (int cntr = 0; cntr < width; cntr++) {
                s.writeDataByte(e, cursor, (byte)val);
                val = val >> 8;
                cursor++;
            }
            return cursor;
        }

        private int writeString(Expr e, Module.Seg s, int cursor) {
            String str = ((Expr.StringLiteral)e).value;
            // TODO: should this string literal be evaluated first?
            s.writeDataBytes(e, cursor, str.getBytes());
            // align the cursor
            return align(cursor, width);
        }

        private int computeSize(ExprList l, int width) {
            int count = 0;
            for (ExprList.ExprItem item = l.head; item != null; item = item.next) {
                Expr e = item.expr;
                if (e instanceof Expr.StringLiteral) {
                    // TODO: is this the right size?
                    count += align(((Expr.StringLiteral)e).value.length(), width);
                } else {
                    count += width;
                }
            }
            return count;
        }

        private int align(int cursor, int width) {
            if (width > 1) {
                int i = cursor % width;
                cursor = i == 0 ? cursor : cursor + (i - width);
            }
            return cursor;
        }

        public int itemSize() {
            return size;
        }

        public String toString() {
            return "initialized data @ " + byteAddress;
        }
    }

    /**
     * The <code>UnitializedData</code> item represents a declared section of data that is not given a value
     * (a reservation of space). This can appear in the program, data, or eeprom segments.
     */
    public static class UninitializedData extends Item {

        private final int length;

        UninitializedData(Module.Seg s, int l) {
            super(s);
            length = l;
        }

        public void simplify() {
            // nothing to build with a reserve item
        }

        public String toString() {
            return "reserve " + length + " in " + segment.getName();
        }

        public int itemSize() {
            return length;
        }
    }
}
