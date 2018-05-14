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

package avrora.arch.legacy;

import avrora.arch.AbstractArchitecture;
import avrora.arch.AbstractInstr;


/**
 * The <code>LegacyInstr</code> class and its descendants represent instructions within the assembly code. The
 * visitor pattern is applied here. Each instruction has an <code>accept()</code> method that allows it to be
 * visited with double dispatch by a <code>LegacyInstrVisitor</code>. Each instruction in the AVR instruction set is
 * represented by an inner class whose source has been generated from a simple specification.
 *
 * @author Ben L. Titzer
 * @see LegacyInstrVisitor
 */
public abstract class LegacyInstr implements LegacyInstrProto, AbstractInstr {

    /**
     * The <code>properties</code> field stores a reference to the properties of the instruction,
     * including its size, number of cycles, etc.
     */
    public final LegacyInstrProperties properties;

    /**
     * The constructor for the <code>LegacyInstr</code> class creates a new instruction with the specified
     * instruction properties. Since this class is abstract, <code>LegacyInstr</code> cannot be instantiated
     * directly, but is called through its subclasses.
     * @param ip the instruction properties for this instruction
     */
    public LegacyInstr(LegacyInstrProperties ip) {
        properties = ip;
    }

    /**
     * The <code>getOperands()</code> method returns a string representation of the operands of the
     * instruction. This is useful for printing and tracing of instructions as well as generating listings.
     *
     * @return a string representing the operands of the instruction
     */
    public abstract String getOperands();

    /**
     * The <code>getVariant()</code> method returns the variant name of the instruction as a string. Since
     * instructions like load and store have multiple variants, they each have specific variant names to
     * distinguish them internally in the core of Avrora. For example, for "ld x+, (addr)", the variant is
     * "ldpi" (load with post increment), but the actual instruction is "ld", so this method will return
     * "ldpi".
     *
     * @return the variant of the instruction that this prototype represents
     */
    public String getVariant() {
        return properties.variant;
    }

    /**
     * The <code>getSize()</code> method returns the size of the instruction in bytes.
     *
     * @return the size of this instruction in bytes
     */
    public int getSize() {
        return properties.size;
    }

    /**
     * The <code>getName()</code> method returns the name of the instruction as a string. For instructions
     * that are variants of instructions, this method returns the actual name of the instruction. For example,
     * for "ld x+, (addr)", the variant is "ldpi" (load with post increment), but the actual instruction is
     * "ld", so this method will return "ld".
     *
     * @return the name of the instruction
     */
    public String getName() {
        return properties.name;
    }

    /**
     * The <code>toString()</code> method simply converts this instruction to a string by appending
     * the operands to the variant of the instruction as a string.
     * @return a string representation of this instruction
     */
    public String toString() {
        return getVariant() + ' ' + getOperands();
    }

    /**
     * The <code>getCycles()</code> method returns the number of cylces consumed by the instruction in the
     * default case. Most instructions consume the same amount of clock cycles no matter what behavior. For
     * example, 8-bit arithmetic takes one cycle, load and stores take two cycles, etc. Some instructions like
     * the branch and skip instructions take more cycles if they are taken or not taken. In that case, this
     * count returned is the smallest number of cycles that can be consumed by this instruction.
     *
     * @return the number of cycles that this instruction consumes
     */
    public int getCycles() {
        return properties.cycles;
    }

    /**
     * The <code>asInstr()</code> method converts an instruction into an AVR instruction. This is used
     * internally for special types of instructions (that are used in the interpreter, for example)
     * so that they behave correctly when reading them from an executing program.
     * @return an object representing this instruction that is one of the valid AVR instructions; null
     * if this instruction is a "special" type of instruction such as those used internally in the
     * interpreter
     */
    public LegacyInstr asInstr() {
        return this;
    }

    /**
     * The <code>getArchitecture()</code> method returns a reference to the architecture
     * that this instruction is a member of.
     * @return a reference to the architecture that contains this instruction
     */
    public AbstractArchitecture getArchitecture() {
        return LegacyArchitecture.INSTANCE;
    }

    /**
     * The <code>accept()</code> method is part of the visitor pattern for instructions. The visitor pattern
     * uses two virtual dispatches combined with memory overloading to achieve dispatching on multiple types.
     * The result is clean and modular code.
     *
     * @param v the visitor to accept
     */
    public abstract void accept(LegacyInstrVisitor v);

    /**
     * The <code>InvalidOperand</code> class represents a runtime error thrown by the constructor of an
     * instruction or the <code>build</code> method of a prototype when an operand does not meet the
     * restrictions imposed by the AVR instruction set architecture.
     */
    public static class InvalidOperand extends RuntimeException {
        /**
         * The <code>number</code> field of the <code>InvalidOperand</code> instance records which operand
         * this error refers to. For example, if the first operand was the source of the problem, then this
         * field will be set to 1.
         */
        public final int number;

        InvalidOperand(int num, String msg) {
            super("invalid operand #" + num + ": " + msg);
            number = num;
        }
    }

    /**
     * The <code>InvalidRegister</code> class represents an error in constructing an instance of
     * <code>LegacyInstr</code> where a register operand does not meet the instruction set specification. For
     * example, the "ldi" instruction can only load values into the upper 16 registers; attempting to create a
     * <code>LegacyInstr.LDI</code> instance with a destination register of <code>LegacyRegister.RO</code> will generate
     * this exception.
     */
    public static class InvalidRegister extends InvalidOperand {
        /**
         * The <code>set</code> field records the expected register set for the operand.
         */
        public final LegacyRegister.Set set;

        /**
         * The <code>register</code> field records the offending register that was found not to be in the
         * expected register set.
         */
        public final LegacyRegister register;

        public InvalidRegister(int num, LegacyRegister reg, LegacyRegister.Set s) {
            super(num, "must be one of " + s.contents);
            set = s;
            register = reg;
        }
    }

    /**
     * The <code>InvalidImmediate</code> class represents an error in construction of an instance of
     * <code>LegacyInstr</code> where the given immediate operand is not within the range that is specified by the
     * instruction set manual. For example, the "sbic" instruction skips the next instruction if the specified
     * bit in the status register is clear. Its operand is expected to be in the range [0, ..., 7]. If the
     * specified operand is not in the range, then this exception will be thrown.
     */
    public static class InvalidImmediate extends InvalidOperand {

        /**
         * The <code>low</code> field stores the lowest value that is allowed for this operand.
         */
        public final int low;

        /**
         * The <code>high</code> field stores the highest value that is allowed for this operand.
         */
        public final int high;

        /**
         * The <code>value</code> field stores the actual value that was passed during the attempeted
         * construction of this instruction.
         */
        public final int value;

        public InvalidImmediate(int num, int v, int l, int h) {
            super(num, "value out of required range [" + l + ", " + h + ']');
            low = l;
            high = h;
            value = v;
        }
    }

    /**
     * The <code>RegisterRequired</code> class represents an error in construction of an instance of
     * <code>LegacyInstr</code> where the given operand is expected to be a register but is not.
     */
    public static class RegisterRequired extends RuntimeException {

        public final LegacyOperand operand;

        RegisterRequired(LegacyOperand o) {
            super("register required");
            operand = o;
        }
    }

    /**
     * The <code>ImmediateRequired</code> class represents an error in construction of an instance of
     * <code>LegacyInstr</code> where the given operand is expected to be an immediate but is not.
     */
    public static class ImmediateRequired extends RuntimeException {

        public final LegacyOperand operand;

        ImmediateRequired(LegacyOperand o) {
            super("immediate required");
            operand = o;
        }
    }

    /**
     * The <code>WrongNumberOfOperands</code> class represents a runtime error thrown by the
     * <code>build</code> method of a prototype when the wrong number of operands is passed to build an
     * instruction.
     */
    public static class WrongNumberOfOperands extends RuntimeException {
        public final int expected;
        public final int found;

        WrongNumberOfOperands(int f, int e) {
            super("wrong number of operands, expected " + e + " and found " + f);
            expected = e;
            found = f;
        }
    }


    /**
     * ------------------------------------------------------------
     * U T I L I T Y   F U N C T I O N S
     * ------------------------------------------------------------
     * These utility functions help in the checking of operands in individual instructions.
     */
    private static void need(int num, LegacyOperand[] ops) {
        if (ops.length != num)
            throw new WrongNumberOfOperands(ops.length, num);
    }

    private static LegacyRegister GPR(int num, LegacyRegister reg) {
        return checkReg(num, reg, LegacyRegister.GPR_set);
    }

    private static LegacyRegister HGPR(int num, LegacyRegister reg) {
        return checkReg(num, reg, LegacyRegister.HGPR_set);
    }

    private static LegacyRegister MGPR(int num, LegacyRegister reg) {
        return checkReg(num, reg, LegacyRegister.MGPR_set);
    }

    private static LegacyRegister ADR(int num, LegacyRegister reg) {
        return checkReg(num, reg, LegacyRegister.ADR_set);
    }

    private static LegacyRegister RDL(int num, LegacyRegister reg) {
        return checkReg(num, reg, LegacyRegister.RDL_set);
    }

    private static LegacyRegister EGPR(int num, LegacyRegister reg) {
        return checkReg(num, reg, LegacyRegister.EGPR_set);
    }

    private static LegacyRegister YZ(int num, LegacyRegister reg) {
        return checkReg(num, reg, LegacyRegister.YZ_set);
    }

    private static LegacyRegister Z(int num, LegacyRegister reg) {
        return checkReg(num, reg, LegacyRegister.Z_set);
    }

    private static int IMM3(int num, int val) {
        return checkImm(num, val, 0, 7);
    }

    private static int IMM5(int num, int val) {
        return checkImm(num, val, 0, 31);
    }

    private static int IMM6(int num, int val) {
        return checkImm(num, val, 0, 63);
    }

    private static int IMM8(int num, int val) {
        return checkImm(num, val, 0, 255);
    }

    private static int SREL(int pc, int num, int val) {
        return checkImm(num, val - pc - 1, -64, 63);
    }

    private static int LREL(int pc, int num, int val) {
        return checkImm(num, val - pc - 1, -2048, 2047);
    }

    private static int DADDR(int num, int val) {
        return checkImm(num, val, 0, 65536);
    }

    private static int PADDR(int num, int val) {
        return checkImm(num, val, 0, 65536);
    }

    private static int checkImm(int num, int val, int low, int high) {
        if (val < low || val > high) throw new InvalidImmediate(num, val, low, high);
        return val;

    }

    private static LegacyRegister checkReg(int num, LegacyRegister reg, LegacyRegister.Set set) {
        if (set.contains(reg)) return reg;
        throw new InvalidRegister(num, reg, set);
    }

    private static LegacyRegister REG(LegacyOperand o) {
        LegacyOperand.Register r = o.asRegister();
        if (r == null) throw new RegisterRequired(o);
        return r.getRegister();
    }

    private static int IMM(LegacyOperand o) {
        LegacyOperand.Constant c = o.asConstant();
        if (c == null) throw new ImmediateRequired(o);
        return c.getValue();
    }

    private static int WORD(LegacyOperand o) {
        LegacyOperand.Constant c = o.asConstant();
        if (c == null) throw new ImmediateRequired(o);
        return c.getValueAsWord();
    }

    /**
     * --------------------------------------------------------
     * A B S T R A C T   C L A S S E S
     * --------------------------------------------------------
     * <p/>
     * These abstract implementations of the instruction simplify the specification of each individual
     * instruction considerably.
     */
    public abstract static class REGREG_class extends LegacyInstr {
        public final LegacyRegister r1;
        public final LegacyRegister r2;

        REGREG_class(LegacyInstrProperties p, LegacyRegister _r1, LegacyRegister _r2) {
            super(p);
            r1 = _r1;
            r2 = _r2;
        }

        public String getOperands() {
            return r1 + ", " + r2;
        }

        public LegacyInstr build(int pc, LegacyOperand[] ops) {
            need(2, ops);
            return allocate(pc, REG(ops[0]), REG(ops[1]));
        }

        public boolean equals(Object o) {
            // is the other object the same as this one?
            if ( o == this ) return true;
             // is the other object an instruction?
            if ( !(o instanceof REGREG_class) ) return false;
            REGREG_class i = (REGREG_class)o;
             // is the other instruction of the same class?
            if ( i.properties != this.properties ) return false;
            if ( i.r1 != this.r1 ) return false;
            if ( i.r2 != this.r2 ) return false;
            return true;
        }

        abstract LegacyInstr allocate(int pc, LegacyRegister r1, LegacyRegister r2);
    }

    public abstract static class REGIMM_class extends LegacyInstr {
        public final LegacyRegister r1;
        public final int imm1;

        REGIMM_class(LegacyInstrProperties p, LegacyRegister r, int i) {
            super(p);
            r1 = r;
            imm1 = i;
        }

        public String getOperands() {
            return r1 + ", " + imm1;
        }

        public LegacyInstr build(int pc, LegacyOperand[] ops) {
            need(2, ops);
            return allocate(pc, REG(ops[0]), IMM(ops[1]));
        }

        public boolean equals(Object o) {
            // is the other object the same as this one?
            if ( o == this ) return true;
             // is the other object an instruction?
            if ( !(o instanceof REGIMM_class) ) return false;
            REGIMM_class i = (REGIMM_class)o;
             // is the other instruction of the same class?
            if ( i.properties != this.properties ) return false;
            if ( i.r1 != this.r1 ) return false;
            if ( i.imm1 != this.imm1 ) return false;
            return true;
        }

        abstract LegacyInstr allocate(int pc, LegacyRegister r1, int imm1);
    }

    public abstract static class IMMREG_class extends LegacyInstr {
        public final LegacyRegister r1;
        public final int imm1;

        IMMREG_class(LegacyInstrProperties p, int i, LegacyRegister r) {
            super(p);
            r1 = r;
            imm1 = i;
        }

        public String getOperands() {
            return imm1 + ", " + r1;
        }

        public LegacyInstr build(int pc, LegacyOperand[] ops) {
            need(2, ops);
            return allocate(pc, IMM(ops[0]), REG(ops[1]));
        }

        public boolean equals(Object o) {
            // is the other object the same as this one?
            if ( o == this ) return true;
             // is the other object an instruction?
            if ( !(o instanceof IMMREG_class) ) return false;
            IMMREG_class i = (IMMREG_class)o;
             // is the other instruction of the same class?
            if ( i.properties != this.properties ) return false;
            if ( i.r1 != this.r1 ) return false;
            if ( i.imm1 != this.imm1 ) return false;
            return true;
        }

        abstract LegacyInstr allocate(int pc, int imm1, LegacyRegister r1);
    }

    public abstract static class REG_class extends LegacyInstr {
        public final LegacyRegister r1;

        REG_class(LegacyInstrProperties p, LegacyRegister r) {
            super(p);
            r1 = r;
        }

        public String getOperands() {
            return r1.toString();
        }

        public LegacyInstr build(int pc, LegacyOperand[] ops) {
            need(1, ops);
            return allocate(pc, REG(ops[0]));
        }

        public boolean equals(Object o) {
            // is the other object the same as this one?
            if ( o == this ) return true;
             // is the other object an instruction?
            if ( !(o instanceof REG_class) ) return false;
            REG_class i = (REG_class)o;
             // is the other instruction of the same class?
            if ( i.properties != this.properties ) return false;
            if ( i.r1 != this.r1 ) return false;
            return true;
        }

        abstract LegacyInstr allocate(int pc, LegacyRegister r1);
    }

    public abstract static class IMMIMM_class extends LegacyInstr {
        public final int imm1;
        public final int imm2;

        IMMIMM_class(LegacyInstrProperties p, int i1, int i2) {
            super(p);
            imm1 = i1;
            imm2 = i2;
        }

        public String getOperands() {
            return imm1 + ", " + imm2;
        }

        public LegacyInstr build(int pc, LegacyOperand[] ops) {
            need(2, ops);
            return allocate(pc, IMM(ops[0]), IMM(ops[1]));
        }

        public boolean equals(Object o) {
            // is the other object the same as this one?
            if ( o == this ) return true;
             // is the other object an instruction?
            if ( !(o instanceof IMMIMM_class) ) return false;
            IMMIMM_class i = (IMMIMM_class)o;
             // is the other instruction of the same class?
            if ( i.properties != this.properties ) return false;
            if ( i.imm1 != this.imm1 ) return false;
            if ( i.imm2 != this.imm2 ) return false;
            return true;
        }

        abstract LegacyInstr allocate(int pc, int imm1, int imm2);
    }

    public abstract static class IMMWORD_class extends LegacyInstr {
        public final int imm1;
        public final int imm2;

        IMMWORD_class(LegacyInstrProperties p, int i1, int i2) {
            super(p);
            imm1 = i1;
            imm2 = i2;
        }

        public String getOperands() {
            return imm1 + ", " + imm2;
        }

        public boolean equals(Object o) {
            // is the other object the same as this one?
            if ( o == this ) return true;
             // is the other object an instruction?
            if ( !(o instanceof IMMWORD_class) ) return false;
            IMMWORD_class i = (IMMWORD_class)o;
             // is the other instruction of the same class?
            if ( i.properties != this.properties ) return false;
            if ( i.imm1 != this.imm1 ) return false;
            if ( i.imm2 != this.imm2 ) return false;
            return true;
        }

        public LegacyInstr build(int pc, LegacyOperand[] ops) {
            need(2, ops);
            return allocate(pc, IMM(ops[0]), WORD(ops[1]));
        }

        abstract LegacyInstr allocate(int pc, int imm1, int imm2);
    }

    public abstract static class IMM_class extends LegacyInstr {
        public final int imm1;

        IMM_class(LegacyInstrProperties p, int i1) {
            super(p);
            imm1 = i1;
        }

        public String getOperands() {
            return "" + imm1;
        }

        public LegacyInstr build(int pc, LegacyOperand[] ops) {
            need(1, ops);
            return allocate(pc, IMM(ops[0]));
        }

        public boolean equals(Object o) {
            // is the other object the same as this one?
            if ( o == this ) return true;
             // is the other object an instruction?
            if ( !(o instanceof IMM_class) ) return false;
            IMM_class i = (IMM_class)o;
             // is the other instruction of the same class?
            if ( i.properties != this.properties ) return false;
            if ( i.imm1 != this.imm1 ) return false;
            return true;
        }

        abstract LegacyInstr allocate(int pc, int imm1);
    }

    public abstract static class WORD_class extends LegacyInstr {
        public final int imm1;

        WORD_class(LegacyInstrProperties p, int i1) {
            super(p);
            imm1 = i1;
        }

        public String getOperands() {
            return "" + imm1;
        }

        public LegacyInstr build(int pc, LegacyOperand[] ops) {
            need(1, ops);
            return allocate(pc, WORD(ops[0]));
        }

        public boolean equals(Object o) {
            // is the other object the same as this one?
            if ( o == this ) return true;
             // is the other object an instruction?
            if ( !(o instanceof WORD_class) ) return false;
            WORD_class i = (WORD_class)o;
             // is the other instruction of the same class?
            if ( i.properties != this.properties ) return false;
            if ( i.imm1 != this.imm1 ) return false;
            return true;
        }

        abstract LegacyInstr allocate(int pc, int imm1);
    }


    public abstract static class REGREGIMM_class extends LegacyInstr {
        public final LegacyRegister r1;
        public final LegacyRegister r2;
        public final int imm1;

        REGREGIMM_class(LegacyInstrProperties p, LegacyRegister r1, LegacyRegister r2, int i1) {
            super(p);
            this.r1 = r1;
            this.r2 = r2;
            imm1 = i1;
        }

        public String getOperands() {
            return r1 + ", " + r2 + '+' + imm1;
        }

        public LegacyInstr build(int pc, LegacyOperand[] ops) {
            need(3, ops);
            return allocate(pc, REG(ops[0]), REG(ops[1]), IMM(ops[2]));
        }

        public boolean equals(Object o) {
            // is the other object the same as this one?
            if ( o == this ) return true;
             // is the other object an instruction?
            if ( !(o instanceof REGREGIMM_class) ) return false;
            REGREGIMM_class i = (REGREGIMM_class)o;
             // is the other instruction of the same class?
            if ( i.properties != this.properties ) return false;
            if ( i.r1 != this.r1 ) return false;
            if ( i.r2 != this.r2 ) return false;
            if ( i.imm1 != this.imm1 ) return false;
            return true;
        }

        abstract LegacyInstr allocate(int pc, LegacyRegister r1, LegacyRegister r2, int imm1);
    }

    public abstract static class REGIMMREG_class extends LegacyInstr {
        public final LegacyRegister r1;
        public final LegacyRegister r2;
        public final int imm1;

        REGIMMREG_class(LegacyInstrProperties p, LegacyRegister r1, int i1, LegacyRegister r2) {
            super(p);
            this.r1 = r1;
            this.r2 = r2;
            imm1 = i1;
        }

        public String getOperands() {
            return r1 + "+" + imm1 + ", " + r2;
        }

        public LegacyInstr build(int pc, LegacyOperand[] ops) {
            need(3, ops);
            return allocate(pc, REG(ops[0]), IMM(ops[1]), REG(ops[2]));
        }

        public boolean equals(Object o) {
            // is the other object the same as this one?
            if ( o == this ) return true;
             // is the other object an instruction?
            if ( !(o instanceof REGIMMREG_class) ) return false;
            REGIMMREG_class i = (REGIMMREG_class)o;
             // is the other instruction of the same class?
            if ( i.properties != this.properties ) return false;
            if ( i.r1 != this.r1 ) return false;
            if ( i.r2 != this.r2 ) return false;
            if ( i.imm1 != this.imm1 ) return false;
            return true;
        }

        abstract LegacyInstr allocate(int pc, LegacyRegister r1, int imm1, LegacyRegister r2);
    }

    public abstract static class NONE_class extends LegacyInstr {

        NONE_class(LegacyInstrProperties p) {
            super(p);
        }

        public String getOperands() {
            return "";
        }

        public LegacyInstr build(int pc, LegacyOperand[] ops) {
            need(0, ops);
            return allocate(pc);
        }

        public boolean equals(Object o) {
            // is the other object the same as this one?
            if ( o == this ) return true;
             // is the other object an instruction?
            if ( !(o instanceof NONE_class) ) return false;
            NONE_class i = (NONE_class)o;
             // is the other instruction of the same class?
            if ( i.properties != this.properties ) return false;
            return true;
        }

        abstract LegacyInstr allocate(int pc);
    }

    private static int IMM3_default = 0;
    private static int IMM5_default = 0;
    private static int IMM6_default = 0;
    private static int IMM8_default = 0;
    private static int SREL_default = 0;
    private static int LREL_default = 0;
    private static int PADDR_default = 0;
    private static int DADDR_default = 0;
    private static LegacyRegister GPR_default = LegacyRegister.R0;
    private static LegacyRegister MGPR_default = LegacyRegister.R16;
    private static LegacyRegister HGPR_default = LegacyRegister.R16;
    private static LegacyRegister EGPR_default = LegacyRegister.R0;
    private static LegacyRegister ADR_default = LegacyRegister.X;
    private static LegacyRegister RDL_default = LegacyRegister.R24;
    private static LegacyRegister YZ_default = LegacyRegister.Y;
    private static LegacyRegister Z_default = LegacyRegister.Z;


    /**
     * ----------------------------------------------------------------
     *  I N S T R U C T I O N   D E S C R I P T I O N S
     * ----------------------------------------------------------------
     * <p/>
     * These are the actual instruction descriptions that contain the constraints on operands and sizes, etc.
     * <p/>
     * DO NOT MODIFY THIS CODE!!!!
     */
//--BEGIN INSTR GENERATOR--
    public static class ADC extends REGREG_class { // add register to register with carry
        static final LegacyInstrProperties props = new LegacyInstrProperties("adc", "adc", 2, 1);
        static final LegacyInstrProto prototype = new ADC(0, GPR_default, GPR_default);

        LegacyInstr allocate(int pc, LegacyRegister a, LegacyRegister b) {
            return new ADC(pc, a, b);
        }

        public ADC(int pc, LegacyRegister a, LegacyRegister b) {
            super(props, GPR(1, a), GPR(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class ADD extends REGREG_class { // add register to register
        static final LegacyInstrProperties props = new LegacyInstrProperties("add", "add", 2, 1);
        static final LegacyInstrProto prototype = new ADD(0, GPR_default, GPR_default);

        LegacyInstr allocate(int pc, LegacyRegister a, LegacyRegister b) {
            return new ADD(pc, a, b);
        }

        public ADD(int pc, LegacyRegister a, LegacyRegister b) {
            super(props, GPR(1, a), GPR(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class ADIW extends REGIMM_class { // add immediate to word register
        static final LegacyInstrProperties props = new LegacyInstrProperties("adiw", "adiw", 2, 2);
        static final LegacyInstrProto prototype = new ADIW(0, RDL_default, IMM6_default);

        LegacyInstr allocate(int pc, LegacyRegister a, int b) {
            return new ADIW(pc, a, b);
        }

        public ADIW(int pc, LegacyRegister a, int b) {
            super(props, RDL(1, a), IMM6(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class AND extends REGREG_class { // and register with register
        static final LegacyInstrProperties props = new LegacyInstrProperties("and", "and", 2, 1);
        static final LegacyInstrProto prototype = new AND(0, GPR_default, GPR_default);

        LegacyInstr allocate(int pc, LegacyRegister a, LegacyRegister b) {
            return new AND(pc, a, b);
        }

        public AND(int pc, LegacyRegister a, LegacyRegister b) {
            super(props, GPR(1, a), GPR(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class ANDI extends REGIMM_class { // and register with immediate
        static final LegacyInstrProperties props = new LegacyInstrProperties("andi", "andi", 2, 1);
        static final LegacyInstrProto prototype = new ANDI(0, HGPR_default, IMM8_default);

        LegacyInstr allocate(int pc, LegacyRegister a, int b) {
            return new ANDI(pc, a, b);
        }

        public ANDI(int pc, LegacyRegister a, int b) {
            super(props, HGPR(1, a), IMM8(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class ASR extends REG_class { // arithmetic shift right
        static final LegacyInstrProperties props = new LegacyInstrProperties("asr", "asr", 2, 1);
        static final LegacyInstrProto prototype = new ASR(0, GPR_default);

        LegacyInstr allocate(int pc, LegacyRegister a) {
            return new ASR(pc, a);
        }

        public ASR(int pc, LegacyRegister a) {
            super(props, GPR(1, a));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class BCLR extends IMM_class { // clear bit in status register
        static final LegacyInstrProperties props = new LegacyInstrProperties("bclr", "bclr", 2, 1);
        static final LegacyInstrProto prototype = new BCLR(0, IMM3_default);

        LegacyInstr allocate(int pc, int a) {
            return new BCLR(pc, a);
        }

        public BCLR(int pc, int a) {
            super(props, IMM3(1, a));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class BLD extends REGIMM_class { // load bit from T flag into register
        static final LegacyInstrProperties props = new LegacyInstrProperties("bld", "bld", 2, 1);
        static final LegacyInstrProto prototype = new BLD(0, GPR_default, IMM3_default);

        LegacyInstr allocate(int pc, LegacyRegister a, int b) {
            return new BLD(pc, a, b);
        }

        public BLD(int pc, LegacyRegister a, int b) {
            super(props, GPR(1, a), IMM3(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class BRBC extends IMMWORD_class { // branch if bit in status register is clear
        static final LegacyInstrProperties props = new LegacyInstrProperties("brbc", "brbc", 2, 1);
        static final LegacyInstrProto prototype = new BRBC(0, IMM3_default, SREL_default);

        LegacyInstr allocate(int pc, int a, int b) {
            return new BRBC(pc, a, b);
        }

        public BRBC(int pc, int a, int b) {
            super(props, IMM3(1, a), SREL(pc, 2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class BRBS extends IMMWORD_class { // branch if bit in status register is set
        static final LegacyInstrProperties props = new LegacyInstrProperties("brbs", "brbs", 2, 1);
        static final LegacyInstrProto prototype = new BRBS(0, IMM3_default, SREL_default);

        LegacyInstr allocate(int pc, int a, int b) {
            return new BRBS(pc, a, b);
        }

        public BRBS(int pc, int a, int b) {
            super(props, IMM3(1, a), SREL(pc, 2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class BRCC extends WORD_class { // branch if carry flag is clear
        static final LegacyInstrProperties props = new LegacyInstrProperties("brcc", "brcc", 2, 1);
        static final LegacyInstrProto prototype = new BRCC(0, SREL_default);

        LegacyInstr allocate(int pc, int a) {
            return new BRCC(pc, a);
        }

        public BRCC(int pc, int a) {
            super(props, SREL(pc, 1, a));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class BRCS extends WORD_class { // branch if carry flag is set
        static final LegacyInstrProperties props = new LegacyInstrProperties("brcs", "brcs", 2, 1);
        static final LegacyInstrProto prototype = new BRCS(0, SREL_default);

        LegacyInstr allocate(int pc, int a) {
            return new BRCS(pc, a);
        }

        public BRCS(int pc, int a) {
            super(props, SREL(pc, 1, a));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class BREAK extends NONE_class { // break
        static final LegacyInstrProperties props = new LegacyInstrProperties("break", "break", 2, 1);
        static final LegacyInstrProto prototype = new BREAK(0);

        LegacyInstr allocate(int pc) {
            return new BREAK(pc);
        }

        public BREAK(int pc) {
            super(props);
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class BREQ extends WORD_class { // branch if equal
        static final LegacyInstrProperties props = new LegacyInstrProperties("breq", "breq", 2, 1);
        static final LegacyInstrProto prototype = new BREQ(0, SREL_default);

        LegacyInstr allocate(int pc, int a) {
            return new BREQ(pc, a);
        }

        public BREQ(int pc, int a) {
            super(props, SREL(pc, 1, a));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class BRGE extends WORD_class { // branch if greater or equal (signed)
        static final LegacyInstrProperties props = new LegacyInstrProperties("brge", "brge", 2, 1);
        static final LegacyInstrProto prototype = new BRGE(0, SREL_default);

        LegacyInstr allocate(int pc, int a) {
            return new BRGE(pc, a);
        }

        public BRGE(int pc, int a) {
            super(props, SREL(pc, 1, a));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class BRHC extends WORD_class { // branch if H flag is clear
        static final LegacyInstrProperties props = new LegacyInstrProperties("brhc", "brhc", 2, 1);
        static final LegacyInstrProto prototype = new BRHC(0, SREL_default);

        LegacyInstr allocate(int pc, int a) {
            return new BRHC(pc, a);
        }

        public BRHC(int pc, int a) {
            super(props, SREL(pc, 1, a));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class BRHS extends WORD_class { // branch if H flag is set
        static final LegacyInstrProperties props = new LegacyInstrProperties("brhs", "brhs", 2, 1);
        static final LegacyInstrProto prototype = new BRHS(0, SREL_default);

        LegacyInstr allocate(int pc, int a) {
            return new BRHS(pc, a);
        }

        public BRHS(int pc, int a) {
            super(props, SREL(pc, 1, a));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class BRID extends WORD_class { // branch if interrupts are disabled
        static final LegacyInstrProperties props = new LegacyInstrProperties("brid", "brid", 2, 1);
        static final LegacyInstrProto prototype = new BRID(0, SREL_default);

        LegacyInstr allocate(int pc, int a) {
            return new BRID(pc, a);
        }

        public BRID(int pc, int a) {
            super(props, SREL(pc, 1, a));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class BRIE extends WORD_class { // branch if interrupts are enabled
        static final LegacyInstrProperties props = new LegacyInstrProperties("brie", "brie", 2, 1);
        static final LegacyInstrProto prototype = new BRIE(0, SREL_default);

        LegacyInstr allocate(int pc, int a) {
            return new BRIE(pc, a);
        }

        public BRIE(int pc, int a) {
            super(props, SREL(pc, 1, a));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class BRLO extends WORD_class { // branch if lower
        static final LegacyInstrProperties props = new LegacyInstrProperties("brlo", "brlo", 2, 1);
        static final LegacyInstrProto prototype = new BRLO(0, SREL_default);

        LegacyInstr allocate(int pc, int a) {
            return new BRLO(pc, a);
        }

        public BRLO(int pc, int a) {
            super(props, SREL(pc, 1, a));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class BRLT extends WORD_class { // branch if less than zero (signed)
        static final LegacyInstrProperties props = new LegacyInstrProperties("brlt", "brlt", 2, 1);
        static final LegacyInstrProto prototype = new BRLT(0, SREL_default);

        LegacyInstr allocate(int pc, int a) {
            return new BRLT(pc, a);
        }

        public BRLT(int pc, int a) {
            super(props, SREL(pc, 1, a));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class BRMI extends WORD_class { // branch if minus
        static final LegacyInstrProperties props = new LegacyInstrProperties("brmi", "brmi", 2, 1);
        static final LegacyInstrProto prototype = new BRMI(0, SREL_default);

        LegacyInstr allocate(int pc, int a) {
            return new BRMI(pc, a);
        }

        public BRMI(int pc, int a) {
            super(props, SREL(pc, 1, a));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class BRNE extends WORD_class { // branch if not equal
        static final LegacyInstrProperties props = new LegacyInstrProperties("brne", "brne", 2, 1);
        static final LegacyInstrProto prototype = new BRNE(0, SREL_default);

        LegacyInstr allocate(int pc, int a) {
            return new BRNE(pc, a);
        }

        public BRNE(int pc, int a) {
            super(props, SREL(pc, 1, a));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class BRPL extends WORD_class { // branch if positive
        static final LegacyInstrProperties props = new LegacyInstrProperties("brpl", "brpl", 2, 1);
        static final LegacyInstrProto prototype = new BRPL(0, SREL_default);

        LegacyInstr allocate(int pc, int a) {
            return new BRPL(pc, a);
        }

        public BRPL(int pc, int a) {
            super(props, SREL(pc, 1, a));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class BRSH extends WORD_class { // branch if same or higher
        static final LegacyInstrProperties props = new LegacyInstrProperties("brsh", "brsh", 2, 1);
        static final LegacyInstrProto prototype = new BRSH(0, SREL_default);

        LegacyInstr allocate(int pc, int a) {
            return new BRSH(pc, a);
        }

        public BRSH(int pc, int a) {
            super(props, SREL(pc, 1, a));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class BRTC extends WORD_class { // branch if T flag is clear
        static final LegacyInstrProperties props = new LegacyInstrProperties("brtc", "brtc", 2, 1);
        static final LegacyInstrProto prototype = new BRTC(0, SREL_default);

        LegacyInstr allocate(int pc, int a) {
            return new BRTC(pc, a);
        }

        public BRTC(int pc, int a) {
            super(props, SREL(pc, 1, a));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class BRTS extends WORD_class { // branch if T flag is set
        static final LegacyInstrProperties props = new LegacyInstrProperties("brts", "brts", 2, 1);
        static final LegacyInstrProto prototype = new BRTS(0, SREL_default);

        LegacyInstr allocate(int pc, int a) {
            return new BRTS(pc, a);
        }

        public BRTS(int pc, int a) {
            super(props, SREL(pc, 1, a));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class BRVC extends WORD_class { // branch if V flag is clear
        static final LegacyInstrProperties props = new LegacyInstrProperties("brvc", "brvc", 2, 1);
        static final LegacyInstrProto prototype = new BRVC(0, SREL_default);

        LegacyInstr allocate(int pc, int a) {
            return new BRVC(pc, a);
        }

        public BRVC(int pc, int a) {
            super(props, SREL(pc, 1, a));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class BRVS extends WORD_class { // branch if V flag is set
        static final LegacyInstrProperties props = new LegacyInstrProperties("brvs", "brvs", 2, 1);
        static final LegacyInstrProto prototype = new BRVS(0, SREL_default);

        LegacyInstr allocate(int pc, int a) {
            return new BRVS(pc, a);
        }

        public BRVS(int pc, int a) {
            super(props, SREL(pc, 1, a));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class BSET extends IMM_class { // set flag in status register
        static final LegacyInstrProperties props = new LegacyInstrProperties("bset", "bset", 2, 1);
        static final LegacyInstrProto prototype = new BSET(0, IMM3_default);

        LegacyInstr allocate(int pc, int a) {
            return new BSET(pc, a);
        }

        public BSET(int pc, int a) {
            super(props, IMM3(1, a));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class BST extends REGIMM_class { // store bit in register into T flag
        static final LegacyInstrProperties props = new LegacyInstrProperties("bst", "bst", 2, 1);
        static final LegacyInstrProto prototype = new BST(0, GPR_default, IMM3_default);

        LegacyInstr allocate(int pc, LegacyRegister a, int b) {
            return new BST(pc, a, b);
        }

        public BST(int pc, LegacyRegister a, int b) {
            super(props, GPR(1, a), IMM3(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class CALL extends WORD_class { // call absolute address
        static final LegacyInstrProperties props = new LegacyInstrProperties("call", "call", 4, 4);
        static final LegacyInstrProto prototype = new CALL(0, PADDR_default);

        LegacyInstr allocate(int pc, int a) {
            return new CALL(pc, a);
        }

        public CALL(int pc, int a) {
            super(props, PADDR(1, a));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class CBI extends IMMIMM_class { // clear bit in IO register
        static final LegacyInstrProperties props = new LegacyInstrProperties("cbi", "cbi", 2, 2);
        static final LegacyInstrProto prototype = new CBI(0, IMM5_default, IMM3_default);

        LegacyInstr allocate(int pc, int a, int b) {
            return new CBI(pc, a, b);
        }

        public CBI(int pc, int a, int b) {
            super(props, IMM5(1, a), IMM3(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class CBR extends REGIMM_class { // clear bits in register
        static final LegacyInstrProperties props = new LegacyInstrProperties("cbr", "cbr", 2, 1);
        static final LegacyInstrProto prototype = new CBR(0, HGPR_default, IMM8_default);

        LegacyInstr allocate(int pc, LegacyRegister a, int b) {
            return new CBR(pc, a, b);
        }

        public CBR(int pc, LegacyRegister a, int b) {
            super(props, HGPR(1, a), IMM8(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class CLC extends NONE_class { // clear C flag
        static final LegacyInstrProperties props = new LegacyInstrProperties("clc", "clc", 2, 1);
        static final LegacyInstrProto prototype = new CLC(0);

        LegacyInstr allocate(int pc) {
            return new CLC(pc);
        }

        public CLC(int pc) {
            super(props);
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class CLH extends NONE_class { // clear H flag
        static final LegacyInstrProperties props = new LegacyInstrProperties("clh", "clh", 2, 1);
        static final LegacyInstrProto prototype = new CLH(0);

        LegacyInstr allocate(int pc) {
            return new CLH(pc);
        }

        public CLH(int pc) {
            super(props);
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class CLI extends NONE_class { // clear I flag
        static final LegacyInstrProperties props = new LegacyInstrProperties("cli", "cli", 2, 1);
        static final LegacyInstrProto prototype = new CLI(0);

        LegacyInstr allocate(int pc) {
            return new CLI(pc);
        }

        public CLI(int pc) {
            super(props);
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class CLN extends NONE_class { // clear N flag
        static final LegacyInstrProperties props = new LegacyInstrProperties("cln", "cln", 2, 1);
        static final LegacyInstrProto prototype = new CLN(0);

        LegacyInstr allocate(int pc) {
            return new CLN(pc);
        }

        public CLN(int pc) {
            super(props);
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class CLR extends REG_class { // clear register (set to zero)
        static final LegacyInstrProperties props = new LegacyInstrProperties("clr", "clr", 2, 1);
        static final LegacyInstrProto prototype = new CLR(0, GPR_default);

        LegacyInstr allocate(int pc, LegacyRegister a) {
            return new CLR(pc, a);
        }

        public CLR(int pc, LegacyRegister a) {
            super(props, GPR(1, a));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class CLS extends NONE_class { // clear S flag
        static final LegacyInstrProperties props = new LegacyInstrProperties("cls", "cls", 2, 1);
        static final LegacyInstrProto prototype = new CLS(0);

        LegacyInstr allocate(int pc) {
            return new CLS(pc);
        }

        public CLS(int pc) {
            super(props);
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class CLT extends NONE_class { // clear T flag
        static final LegacyInstrProperties props = new LegacyInstrProperties("clt", "clt", 2, 1);
        static final LegacyInstrProto prototype = new CLT(0);

        LegacyInstr allocate(int pc) {
            return new CLT(pc);
        }

        public CLT(int pc) {
            super(props);
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class CLV extends NONE_class { // clear V flag
        static final LegacyInstrProperties props = new LegacyInstrProperties("clv", "clv", 2, 1);
        static final LegacyInstrProto prototype = new CLV(0);

        LegacyInstr allocate(int pc) {
            return new CLV(pc);
        }

        public CLV(int pc) {
            super(props);
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class CLZ extends NONE_class { // clear Z flag
        static final LegacyInstrProperties props = new LegacyInstrProperties("clz", "clz", 2, 1);
        static final LegacyInstrProto prototype = new CLZ(0);

        LegacyInstr allocate(int pc) {
            return new CLZ(pc);
        }

        public CLZ(int pc) {
            super(props);
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class COM extends REG_class { // one's compliment register
        static final LegacyInstrProperties props = new LegacyInstrProperties("com", "com", 2, 1);
        static final LegacyInstrProto prototype = new COM(0, GPR_default);

        LegacyInstr allocate(int pc, LegacyRegister a) {
            return new COM(pc, a);
        }

        public COM(int pc, LegacyRegister a) {
            super(props, GPR(1, a));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class CP extends REGREG_class { // compare registers
        static final LegacyInstrProperties props = new LegacyInstrProperties("cp", "cp", 2, 1);
        static final LegacyInstrProto prototype = new CP(0, GPR_default, GPR_default);

        LegacyInstr allocate(int pc, LegacyRegister a, LegacyRegister b) {
            return new CP(pc, a, b);
        }

        public CP(int pc, LegacyRegister a, LegacyRegister b) {
            super(props, GPR(1, a), GPR(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class CPC extends REGREG_class { // compare registers with carry
        static final LegacyInstrProperties props = new LegacyInstrProperties("cpc", "cpc", 2, 1);
        static final LegacyInstrProto prototype = new CPC(0, GPR_default, GPR_default);

        LegacyInstr allocate(int pc, LegacyRegister a, LegacyRegister b) {
            return new CPC(pc, a, b);
        }

        public CPC(int pc, LegacyRegister a, LegacyRegister b) {
            super(props, GPR(1, a), GPR(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class CPI extends REGIMM_class { // compare register with immediate
        static final LegacyInstrProperties props = new LegacyInstrProperties("cpi", "cpi", 2, 1);
        static final LegacyInstrProto prototype = new CPI(0, HGPR_default, IMM8_default);

        LegacyInstr allocate(int pc, LegacyRegister a, int b) {
            return new CPI(pc, a, b);
        }

        public CPI(int pc, LegacyRegister a, int b) {
            super(props, HGPR(1, a), IMM8(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class CPSE extends REGREG_class { // compare registers and skip if equal
        static final LegacyInstrProperties props = new LegacyInstrProperties("cpse", "cpse", 2, 1);
        static final LegacyInstrProto prototype = new CPSE(0, GPR_default, GPR_default);

        LegacyInstr allocate(int pc, LegacyRegister a, LegacyRegister b) {
            return new CPSE(pc, a, b);
        }

        public CPSE(int pc, LegacyRegister a, LegacyRegister b) {
            super(props, GPR(1, a), GPR(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class DEC extends REG_class { // decrement register by one
        static final LegacyInstrProperties props = new LegacyInstrProperties("dec", "dec", 2, 1);
        static final LegacyInstrProto prototype = new DEC(0, GPR_default);

        LegacyInstr allocate(int pc, LegacyRegister a) {
            return new DEC(pc, a);
        }

        public DEC(int pc, LegacyRegister a) {
            super(props, GPR(1, a));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class EICALL extends NONE_class { // extended indirect call
        static final LegacyInstrProperties props = new LegacyInstrProperties("eicall", "eicall", 2, 4);
        static final LegacyInstrProto prototype = new EICALL(0);

        LegacyInstr allocate(int pc) {
            return new EICALL(pc);
        }

        public EICALL(int pc) {
            super(props);
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class EIJMP extends NONE_class { // extended indirect jump
        static final LegacyInstrProperties props = new LegacyInstrProperties("eijmp", "eijmp", 2, 2);
        static final LegacyInstrProto prototype = new EIJMP(0);

        LegacyInstr allocate(int pc) {
            return new EIJMP(pc);
        }

        public EIJMP(int pc) {
            super(props);
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class ELPM extends NONE_class { // extended load program memory to r0
        static final LegacyInstrProperties props = new LegacyInstrProperties("elpm", "elpm", 2, 3);
        static final LegacyInstrProto prototype = new ELPM(0);

        LegacyInstr allocate(int pc) {
            return new ELPM(pc);
        }

        public ELPM(int pc) {
            super(props);
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class ELPMD extends REGREG_class { // extended load program memory to register
        static final LegacyInstrProperties props = new LegacyInstrProperties("elpm", "elpmd", 2, 3);
        static final LegacyInstrProto prototype = new ELPMD(0, GPR_default, Z_default);

        LegacyInstr allocate(int pc, LegacyRegister a, LegacyRegister b) {
            return new ELPMD(pc, a, b);
        }

        public ELPMD(int pc, LegacyRegister a, LegacyRegister b) {
            super(props, GPR(1, a), Z(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class ELPMPI extends REGREG_class { // extended load program memory to register and post-increment
        static final LegacyInstrProperties props = new LegacyInstrProperties("elpm", "elpmpi", 2, 3);
        static final LegacyInstrProto prototype = new ELPMPI(0, GPR_default, Z_default);

        LegacyInstr allocate(int pc, LegacyRegister a, LegacyRegister b) {
            return new ELPMPI(pc, a, b);
        }

        public ELPMPI(int pc, LegacyRegister a, LegacyRegister b) {
            super(props, GPR(1, a), Z(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class EOR extends REGREG_class { // exclusive or register with register
        static final LegacyInstrProperties props = new LegacyInstrProperties("eor", "eor", 2, 1);
        static final LegacyInstrProto prototype = new EOR(0, GPR_default, GPR_default);

        LegacyInstr allocate(int pc, LegacyRegister a, LegacyRegister b) {
            return new EOR(pc, a, b);
        }

        public EOR(int pc, LegacyRegister a, LegacyRegister b) {
            super(props, GPR(1, a), GPR(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class FMUL extends REGREG_class { // fractional multiply register with register to r0
        static final LegacyInstrProperties props = new LegacyInstrProperties("fmul", "fmul", 2, 2);
        static final LegacyInstrProto prototype = new FMUL(0, MGPR_default, MGPR_default);

        LegacyInstr allocate(int pc, LegacyRegister a, LegacyRegister b) {
            return new FMUL(pc, a, b);
        }

        public FMUL(int pc, LegacyRegister a, LegacyRegister b) {
            super(props, MGPR(1, a), MGPR(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class FMULS extends REGREG_class { // signed fractional multiply register with register to r0
        static final LegacyInstrProperties props = new LegacyInstrProperties("fmuls", "fmuls", 2, 2);
        static final LegacyInstrProto prototype = new FMULS(0, MGPR_default, MGPR_default);

        LegacyInstr allocate(int pc, LegacyRegister a, LegacyRegister b) {
            return new FMULS(pc, a, b);
        }

        public FMULS(int pc, LegacyRegister a, LegacyRegister b) {
            super(props, MGPR(1, a), MGPR(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class FMULSU extends REGREG_class { // signed/unsigned fractional multiply register with register to r0
        static final LegacyInstrProperties props = new LegacyInstrProperties("fmulsu", "fmulsu", 2, 2);
        static final LegacyInstrProto prototype = new FMULSU(0, MGPR_default, MGPR_default);

        LegacyInstr allocate(int pc, LegacyRegister a, LegacyRegister b) {
            return new FMULSU(pc, a, b);
        }

        public FMULSU(int pc, LegacyRegister a, LegacyRegister b) {
            super(props, MGPR(1, a), MGPR(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class ICALL extends NONE_class { // indirect call through Z register
        static final LegacyInstrProperties props = new LegacyInstrProperties("icall", "icall", 2, 3);
        static final LegacyInstrProto prototype = new ICALL(0);

        LegacyInstr allocate(int pc) {
            return new ICALL(pc);
        }

        public ICALL(int pc) {
            super(props);
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class IJMP extends NONE_class { // indirect jump through Z register
        static final LegacyInstrProperties props = new LegacyInstrProperties("ijmp", "ijmp", 2, 2);
        static final LegacyInstrProto prototype = new IJMP(0);

        LegacyInstr allocate(int pc) {
            return new IJMP(pc);
        }

        public IJMP(int pc) {
            super(props);
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class IN extends REGIMM_class { // read from IO register into register
        static final LegacyInstrProperties props = new LegacyInstrProperties("in", "in", 2, 1);
        static final LegacyInstrProto prototype = new IN(0, GPR_default, IMM6_default);

        LegacyInstr allocate(int pc, LegacyRegister a, int b) {
            return new IN(pc, a, b);
        }

        public IN(int pc, LegacyRegister a, int b) {
            super(props, GPR(1, a), IMM6(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class INC extends REG_class { // increment register by one
        static final LegacyInstrProperties props = new LegacyInstrProperties("inc", "inc", 2, 1);
        static final LegacyInstrProto prototype = new INC(0, GPR_default);

        LegacyInstr allocate(int pc, LegacyRegister a) {
            return new INC(pc, a);
        }

        public INC(int pc, LegacyRegister a) {
            super(props, GPR(1, a));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class JMP extends WORD_class { // absolute jump
        static final LegacyInstrProperties props = new LegacyInstrProperties("jmp", "jmp", 4, 3);
        static final LegacyInstrProto prototype = new JMP(0, PADDR_default);

        LegacyInstr allocate(int pc, int a) {
            return new JMP(pc, a);
        }

        public JMP(int pc, int a) {
            super(props, PADDR(1, a));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class LD extends REGREG_class { // load from SRAM
        static final LegacyInstrProperties props = new LegacyInstrProperties("ld", "ld", 2, 2);
        static final LegacyInstrProto prototype = new LD(0, GPR_default, ADR_default);

        LegacyInstr allocate(int pc, LegacyRegister a, LegacyRegister b) {
            return new LD(pc, a, b);
        }

        public LD(int pc, LegacyRegister a, LegacyRegister b) {
            super(props, GPR(1, a), ADR(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class LDD extends REGREGIMM_class { // load from SRAM with displacement
        static final LegacyInstrProperties props = new LegacyInstrProperties("ldd", "ldd", 2, 2);
        static final LegacyInstrProto prototype = new LDD(0, GPR_default, YZ_default, IMM6_default);

        LegacyInstr allocate(int pc, LegacyRegister a, LegacyRegister b, int c) {
            return new LDD(pc, a, b, c);
        }

        public LDD(int pc, LegacyRegister a, LegacyRegister b, int c) {
            super(props, GPR(1, a), YZ(2, b), IMM6(3, c));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class LDI extends REGIMM_class { // load immediate into register
        static final LegacyInstrProperties props = new LegacyInstrProperties("ldi", "ldi", 2, 1);
        static final LegacyInstrProto prototype = new LDI(0, HGPR_default, IMM8_default);

        LegacyInstr allocate(int pc, LegacyRegister a, int b) {
            return new LDI(pc, a, b);
        }

        public LDI(int pc, LegacyRegister a, int b) {
            super(props, HGPR(1, a), IMM8(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class LDPD extends REGREG_class { // load from SRAM with pre-decrement
        static final LegacyInstrProperties props = new LegacyInstrProperties("ld", "ldpd", 2, 2);
        static final LegacyInstrProto prototype = new LDPD(0, GPR_default, ADR_default);

        LegacyInstr allocate(int pc, LegacyRegister a, LegacyRegister b) {
            return new LDPD(pc, a, b);
        }

        public LDPD(int pc, LegacyRegister a, LegacyRegister b) {
            super(props, GPR(1, a), ADR(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class LDPI extends REGREG_class { // load from SRAM with post-increment
        static final LegacyInstrProperties props = new LegacyInstrProperties("ld", "ldpi", 2, 2);
        static final LegacyInstrProto prototype = new LDPI(0, GPR_default, ADR_default);

        LegacyInstr allocate(int pc, LegacyRegister a, LegacyRegister b) {
            return new LDPI(pc, a, b);
        }

        public LDPI(int pc, LegacyRegister a, LegacyRegister b) {
            super(props, GPR(1, a), ADR(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class LDS extends REGIMM_class { // load direct from SRAM
        static final LegacyInstrProperties props = new LegacyInstrProperties("lds", "lds", 4, 2);
        static final LegacyInstrProto prototype = new LDS(0, GPR_default, DADDR_default);

        LegacyInstr allocate(int pc, LegacyRegister a, int b) {
            return new LDS(pc, a, b);
        }

        public LDS(int pc, LegacyRegister a, int b) {
            super(props, GPR(1, a), DADDR(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class LPM extends NONE_class { // load program memory into r0
        static final LegacyInstrProperties props = new LegacyInstrProperties("lpm", "lpm", 2, 3);
        static final LegacyInstrProto prototype = new LPM(0);

        LegacyInstr allocate(int pc) {
            return new LPM(pc);
        }

        public LPM(int pc) {
            super(props);
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class LPMD extends REGREG_class { // load program memory into register
        static final LegacyInstrProperties props = new LegacyInstrProperties("lpm", "lpmd", 2, 3);
        static final LegacyInstrProto prototype = new LPMD(0, GPR_default, Z_default);

        LegacyInstr allocate(int pc, LegacyRegister a, LegacyRegister b) {
            return new LPMD(pc, a, b);
        }

        public LPMD(int pc, LegacyRegister a, LegacyRegister b) {
            super(props, GPR(1, a), Z(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class LPMPI extends REGREG_class { // load program memory into register and post-increment
        static final LegacyInstrProperties props = new LegacyInstrProperties("lpm", "lpmpi", 2, 3);
        static final LegacyInstrProto prototype = new LPMPI(0, GPR_default, Z_default);

        LegacyInstr allocate(int pc, LegacyRegister a, LegacyRegister b) {
            return new LPMPI(pc, a, b);
        }

        public LPMPI(int pc, LegacyRegister a, LegacyRegister b) {
            super(props, GPR(1, a), Z(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class LSL extends REG_class { // logical shift left
        static final LegacyInstrProperties props = new LegacyInstrProperties("lsl", "lsl", 2, 1);
        static final LegacyInstrProto prototype = new LSL(0, GPR_default);

        LegacyInstr allocate(int pc, LegacyRegister a) {
            return new LSL(pc, a);
        }

        public LSL(int pc, LegacyRegister a) {
            super(props, GPR(1, a));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class LSR extends REG_class { // logical shift right
        static final LegacyInstrProperties props = new LegacyInstrProperties("lsr", "lsr", 2, 1);
        static final LegacyInstrProto prototype = new LSR(0, GPR_default);

        LegacyInstr allocate(int pc, LegacyRegister a) {
            return new LSR(pc, a);
        }

        public LSR(int pc, LegacyRegister a) {
            super(props, GPR(1, a));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class MOV extends REGREG_class { // copy register to register
        static final LegacyInstrProperties props = new LegacyInstrProperties("mov", "mov", 2, 1);
        static final LegacyInstrProto prototype = new MOV(0, GPR_default, GPR_default);

        LegacyInstr allocate(int pc, LegacyRegister a, LegacyRegister b) {
            return new MOV(pc, a, b);
        }

        public MOV(int pc, LegacyRegister a, LegacyRegister b) {
            super(props, GPR(1, a), GPR(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class MOVW extends REGREG_class { // copy two registers to two registers
        static final LegacyInstrProperties props = new LegacyInstrProperties("movw", "movw", 2, 1);
        static final LegacyInstrProto prototype = new MOVW(0, EGPR_default, EGPR_default);

        LegacyInstr allocate(int pc, LegacyRegister a, LegacyRegister b) {
            return new MOVW(pc, a, b);
        }

        public MOVW(int pc, LegacyRegister a, LegacyRegister b) {
            super(props, EGPR(1, a), EGPR(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class MUL extends REGREG_class { // multiply register with register to r0
        static final LegacyInstrProperties props = new LegacyInstrProperties("mul", "mul", 2, 2);
        static final LegacyInstrProto prototype = new MUL(0, GPR_default, GPR_default);

        LegacyInstr allocate(int pc, LegacyRegister a, LegacyRegister b) {
            return new MUL(pc, a, b);
        }

        public MUL(int pc, LegacyRegister a, LegacyRegister b) {
            super(props, GPR(1, a), GPR(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class MULS extends REGREG_class { // signed multiply register with register to r0
        static final LegacyInstrProperties props = new LegacyInstrProperties("muls", "muls", 2, 2);
        static final LegacyInstrProto prototype = new MULS(0, HGPR_default, HGPR_default);

        LegacyInstr allocate(int pc, LegacyRegister a, LegacyRegister b) {
            return new MULS(pc, a, b);
        }

        public MULS(int pc, LegacyRegister a, LegacyRegister b) {
            super(props, HGPR(1, a), HGPR(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class MULSU extends REGREG_class { // signed/unsigned multiply register with register to r0
        static final LegacyInstrProperties props = new LegacyInstrProperties("mulsu", "mulsu", 2, 2);
        static final LegacyInstrProto prototype = new MULSU(0, MGPR_default, MGPR_default);

        LegacyInstr allocate(int pc, LegacyRegister a, LegacyRegister b) {
            return new MULSU(pc, a, b);
        }

        public MULSU(int pc, LegacyRegister a, LegacyRegister b) {
            super(props, MGPR(1, a), MGPR(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class NEG extends REG_class { // two's complement register
        static final LegacyInstrProperties props = new LegacyInstrProperties("neg", "neg", 2, 1);
        static final LegacyInstrProto prototype = new NEG(0, GPR_default);

        LegacyInstr allocate(int pc, LegacyRegister a) {
            return new NEG(pc, a);
        }

        public NEG(int pc, LegacyRegister a) {
            super(props, GPR(1, a));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class NOP extends NONE_class { // do nothing operation
        static final LegacyInstrProperties props = new LegacyInstrProperties("nop", "nop", 2, 1);
        static final LegacyInstrProto prototype = new NOP(0);

        LegacyInstr allocate(int pc) {
            return new NOP(pc);
        }

        public NOP(int pc) {
            super(props);
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class OR extends REGREG_class { // or register with register
        static final LegacyInstrProperties props = new LegacyInstrProperties("or", "or", 2, 1);
        static final LegacyInstrProto prototype = new OR(0, GPR_default, GPR_default);

        LegacyInstr allocate(int pc, LegacyRegister a, LegacyRegister b) {
            return new OR(pc, a, b);
        }

        public OR(int pc, LegacyRegister a, LegacyRegister b) {
            super(props, GPR(1, a), GPR(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class ORI extends REGIMM_class { // or register with immediate
        static final LegacyInstrProperties props = new LegacyInstrProperties("ori", "ori", 2, 1);
        static final LegacyInstrProto prototype = new ORI(0, HGPR_default, IMM8_default);

        LegacyInstr allocate(int pc, LegacyRegister a, int b) {
            return new ORI(pc, a, b);
        }

        public ORI(int pc, LegacyRegister a, int b) {
            super(props, HGPR(1, a), IMM8(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class OUT extends IMMREG_class { // write from register to IO register
        static final LegacyInstrProperties props = new LegacyInstrProperties("out", "out", 2, 1);
        static final LegacyInstrProto prototype = new OUT(0, IMM6_default, GPR_default);

        LegacyInstr allocate(int pc, int a, LegacyRegister b) {
            return new OUT(pc, a, b);
        }

        public OUT(int pc, int a, LegacyRegister b) {
            super(props, IMM6(1, a), GPR(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class POP extends REG_class { // pop from the stack to register
        static final LegacyInstrProperties props = new LegacyInstrProperties("pop", "pop", 2, 2);
        static final LegacyInstrProto prototype = new POP(0, GPR_default);

        LegacyInstr allocate(int pc, LegacyRegister a) {
            return new POP(pc, a);
        }

        public POP(int pc, LegacyRegister a) {
            super(props, GPR(1, a));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class PUSH extends REG_class { // push register to the stack
        static final LegacyInstrProperties props = new LegacyInstrProperties("push", "push", 2, 2);
        static final LegacyInstrProto prototype = new PUSH(0, GPR_default);

        LegacyInstr allocate(int pc, LegacyRegister a) {
            return new PUSH(pc, a);
        }

        public PUSH(int pc, LegacyRegister a) {
            super(props, GPR(1, a));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class RCALL extends WORD_class { // relative call
        static final LegacyInstrProperties props = new LegacyInstrProperties("rcall", "rcall", 2, 3);
        static final LegacyInstrProto prototype = new RCALL(0, LREL_default);

        LegacyInstr allocate(int pc, int a) {
            return new RCALL(pc, a);
        }

        public RCALL(int pc, int a) {
            super(props, LREL(pc, 1, a));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class RET extends NONE_class { // return to caller
        static final LegacyInstrProperties props = new LegacyInstrProperties("ret", "ret", 2, 4);
        static final LegacyInstrProto prototype = new RET(0);

        LegacyInstr allocate(int pc) {
            return new RET(pc);
        }

        public RET(int pc) {
            super(props);
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class RETI extends NONE_class { // return from interrupt
        static final LegacyInstrProperties props = new LegacyInstrProperties("reti", "reti", 2, 4);
        static final LegacyInstrProto prototype = new RETI(0);

        LegacyInstr allocate(int pc) {
            return new RETI(pc);
        }

        public RETI(int pc) {
            super(props);
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class RJMP extends WORD_class { // relative jump
        static final LegacyInstrProperties props = new LegacyInstrProperties("rjmp", "rjmp", 2, 2);
        static final LegacyInstrProto prototype = new RJMP(0, LREL_default);

        LegacyInstr allocate(int pc, int a) {
            return new RJMP(pc, a);
        }

        public RJMP(int pc, int a) {
            super(props, LREL(pc, 1, a));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class ROL extends REG_class { // rotate left through carry flag
        static final LegacyInstrProperties props = new LegacyInstrProperties("rol", "rol", 2, 1);
        static final LegacyInstrProto prototype = new ROL(0, GPR_default);

        LegacyInstr allocate(int pc, LegacyRegister a) {
            return new ROL(pc, a);
        }

        public ROL(int pc, LegacyRegister a) {
            super(props, GPR(1, a));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class ROR extends REG_class { // rotate right through carry flag
        static final LegacyInstrProperties props = new LegacyInstrProperties("ror", "ror", 2, 1);
        static final LegacyInstrProto prototype = new ROR(0, GPR_default);

        LegacyInstr allocate(int pc, LegacyRegister a) {
            return new ROR(pc, a);
        }

        public ROR(int pc, LegacyRegister a) {
            super(props, GPR(1, a));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class SBC extends REGREG_class { // subtract register from register with carry
        static final LegacyInstrProperties props = new LegacyInstrProperties("sbc", "sbc", 2, 1);
        static final LegacyInstrProto prototype = new SBC(0, GPR_default, GPR_default);

        LegacyInstr allocate(int pc, LegacyRegister a, LegacyRegister b) {
            return new SBC(pc, a, b);
        }

        public SBC(int pc, LegacyRegister a, LegacyRegister b) {
            super(props, GPR(1, a), GPR(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class SBCI extends REGIMM_class { // subtract immediate from register with carry
        static final LegacyInstrProperties props = new LegacyInstrProperties("sbci", "sbci", 2, 1);
        static final LegacyInstrProto prototype = new SBCI(0, HGPR_default, IMM8_default);

        LegacyInstr allocate(int pc, LegacyRegister a, int b) {
            return new SBCI(pc, a, b);
        }

        public SBCI(int pc, LegacyRegister a, int b) {
            super(props, HGPR(1, a), IMM8(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class SBI extends IMMIMM_class { // set bit in IO register
        static final LegacyInstrProperties props = new LegacyInstrProperties("sbi", "sbi", 2, 2);
        static final LegacyInstrProto prototype = new SBI(0, IMM5_default, IMM3_default);

        LegacyInstr allocate(int pc, int a, int b) {
            return new SBI(pc, a, b);
        }

        public SBI(int pc, int a, int b) {
            super(props, IMM5(1, a), IMM3(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class SBIC extends IMMIMM_class { // skip if bit in IO register is clear
        static final LegacyInstrProperties props = new LegacyInstrProperties("sbic", "sbic", 2, 1);
        static final LegacyInstrProto prototype = new SBIC(0, IMM5_default, IMM3_default);

        LegacyInstr allocate(int pc, int a, int b) {
            return new SBIC(pc, a, b);
        }

        public SBIC(int pc, int a, int b) {
            super(props, IMM5(1, a), IMM3(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class SBIS extends IMMIMM_class { // skip if bit in IO register is set
        static final LegacyInstrProperties props = new LegacyInstrProperties("sbis", "sbis", 2, 1);
        static final LegacyInstrProto prototype = new SBIS(0, IMM5_default, IMM3_default);

        LegacyInstr allocate(int pc, int a, int b) {
            return new SBIS(pc, a, b);
        }

        public SBIS(int pc, int a, int b) {
            super(props, IMM5(1, a), IMM3(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class SBIW extends REGIMM_class { // subtract immediate from word
        static final LegacyInstrProperties props = new LegacyInstrProperties("sbiw", "sbiw", 2, 2);
        static final LegacyInstrProto prototype = new SBIW(0, RDL_default, IMM6_default);

        LegacyInstr allocate(int pc, LegacyRegister a, int b) {
            return new SBIW(pc, a, b);
        }

        public SBIW(int pc, LegacyRegister a, int b) {
            super(props, RDL(1, a), IMM6(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class SBR extends REGIMM_class { // set bits in register
        static final LegacyInstrProperties props = new LegacyInstrProperties("sbr", "sbr", 2, 1);
        static final LegacyInstrProto prototype = new SBR(0, HGPR_default, IMM8_default);

        LegacyInstr allocate(int pc, LegacyRegister a, int b) {
            return new SBR(pc, a, b);
        }

        public SBR(int pc, LegacyRegister a, int b) {
            super(props, HGPR(1, a), IMM8(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class SBRC extends REGIMM_class { // skip if bit in register cleared
        static final LegacyInstrProperties props = new LegacyInstrProperties("sbrc", "sbrc", 2, 1);
        static final LegacyInstrProto prototype = new SBRC(0, GPR_default, IMM3_default);

        LegacyInstr allocate(int pc, LegacyRegister a, int b) {
            return new SBRC(pc, a, b);
        }

        public SBRC(int pc, LegacyRegister a, int b) {
            super(props, GPR(1, a), IMM3(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class SBRS extends REGIMM_class { // skip if bit in register set
        static final LegacyInstrProperties props = new LegacyInstrProperties("sbrs", "sbrs", 2, 1);
        static final LegacyInstrProto prototype = new SBRS(0, GPR_default, IMM3_default);

        LegacyInstr allocate(int pc, LegacyRegister a, int b) {
            return new SBRS(pc, a, b);
        }

        public SBRS(int pc, LegacyRegister a, int b) {
            super(props, GPR(1, a), IMM3(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class SEC extends NONE_class { // set C (carry) flag
        static final LegacyInstrProperties props = new LegacyInstrProperties("sec", "sec", 2, 1);
        static final LegacyInstrProto prototype = new SEC(0);

        LegacyInstr allocate(int pc) {
            return new SEC(pc);
        }

        public SEC(int pc) {
            super(props);
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class SEH extends NONE_class { // set H (half carry) flag
        static final LegacyInstrProperties props = new LegacyInstrProperties("seh", "seh", 2, 1);
        static final LegacyInstrProto prototype = new SEH(0);

        LegacyInstr allocate(int pc) {
            return new SEH(pc);
        }

        public SEH(int pc) {
            super(props);
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class SEI extends NONE_class { // set I (interrupt enable) flag
        static final LegacyInstrProperties props = new LegacyInstrProperties("sei", "sei", 2, 1);
        static final LegacyInstrProto prototype = new SEI(0);

        LegacyInstr allocate(int pc) {
            return new SEI(pc);
        }

        public SEI(int pc) {
            super(props);
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class SEN extends NONE_class { // set N (negative) flag
        static final LegacyInstrProperties props = new LegacyInstrProperties("sen", "sen", 2, 1);
        static final LegacyInstrProto prototype = new SEN(0);

        LegacyInstr allocate(int pc) {
            return new SEN(pc);
        }

        public SEN(int pc) {
            super(props);
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class SER extends REG_class { // set bits in register
        static final LegacyInstrProperties props = new LegacyInstrProperties("ser", "ser", 2, 1);
        static final LegacyInstrProto prototype = new SER(0, GPR_default);

        LegacyInstr allocate(int pc, LegacyRegister a) {
            return new SER(pc, a);
        }

        public SER(int pc, LegacyRegister a) {
            super(props, GPR(1, a));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class SES extends NONE_class { // set S (signed) flag
        static final LegacyInstrProperties props = new LegacyInstrProperties("ses", "ses", 2, 1);
        static final LegacyInstrProto prototype = new SES(0);

        LegacyInstr allocate(int pc) {
            return new SES(pc);
        }

        public SES(int pc) {
            super(props);
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class SET extends NONE_class { // set T flag
        static final LegacyInstrProperties props = new LegacyInstrProperties("set", "set", 2, 1);
        static final LegacyInstrProto prototype = new SET(0);

        LegacyInstr allocate(int pc) {
            return new SET(pc);
        }

        public SET(int pc) {
            super(props);
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class SEV extends NONE_class { // set V (overflow) flag
        static final LegacyInstrProperties props = new LegacyInstrProperties("sev", "sev", 2, 1);
        static final LegacyInstrProto prototype = new SEV(0);

        LegacyInstr allocate(int pc) {
            return new SEV(pc);
        }

        public SEV(int pc) {
            super(props);
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class SEZ extends NONE_class { // set Z (zero) flag
        static final LegacyInstrProperties props = new LegacyInstrProperties("sez", "sez", 2, 1);
        static final LegacyInstrProto prototype = new SEZ(0);

        LegacyInstr allocate(int pc) {
            return new SEZ(pc);
        }

        public SEZ(int pc) {
            super(props);
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class SLEEP extends NONE_class { // invoke sleep mode
        static final LegacyInstrProperties props = new LegacyInstrProperties("sleep", "sleep", 2, 1);
        static final LegacyInstrProto prototype = new SLEEP(0);

        LegacyInstr allocate(int pc) {
            return new SLEEP(pc);
        }

        public SLEEP(int pc) {
            super(props);
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class SPM extends NONE_class { // store to program memory from r0
        static final LegacyInstrProperties props = new LegacyInstrProperties("spm", "spm", 2, 1);
        static final LegacyInstrProto prototype = new SPM(0);

        LegacyInstr allocate(int pc) {
            return new SPM(pc);
        }

        public SPM(int pc) {
            super(props);
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class ST extends REGREG_class { // store from register to SRAM
        static final LegacyInstrProperties props = new LegacyInstrProperties("st", "st", 2, 2);
        static final LegacyInstrProto prototype = new ST(0, ADR_default, GPR_default);

        LegacyInstr allocate(int pc, LegacyRegister a, LegacyRegister b) {
            return new ST(pc, a, b);
        }

        public ST(int pc, LegacyRegister a, LegacyRegister b) {
            super(props, ADR(1, a), GPR(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class STD extends REGIMMREG_class { // store from register to SRAM with displacement
        static final LegacyInstrProperties props = new LegacyInstrProperties("std", "std", 2, 2);
        static final LegacyInstrProto prototype = new STD(0, YZ_default, IMM6_default, GPR_default);

        LegacyInstr allocate(int pc, LegacyRegister a, int b, LegacyRegister c) {
            return new STD(pc, a, b, c);
        }

        public STD(int pc, LegacyRegister a, int b, LegacyRegister c) {
            super(props, YZ(1, a), IMM6(2, b), GPR(3, c));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class STPD extends REGREG_class { // store from register to SRAM with pre-decrement
        static final LegacyInstrProperties props = new LegacyInstrProperties("st", "stpd", 2, 2);
        static final LegacyInstrProto prototype = new STPD(0, ADR_default, GPR_default);

        LegacyInstr allocate(int pc, LegacyRegister a, LegacyRegister b) {
            return new STPD(pc, a, b);
        }

        public STPD(int pc, LegacyRegister a, LegacyRegister b) {
            super(props, ADR(1, a), GPR(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class STPI extends REGREG_class { // store from register to SRAM with post-increment
        static final LegacyInstrProperties props = new LegacyInstrProperties("st", "stpi", 2, 2);
        static final LegacyInstrProto prototype = new STPI(0, ADR_default, GPR_default);

        LegacyInstr allocate(int pc, LegacyRegister a, LegacyRegister b) {
            return new STPI(pc, a, b);
        }

        public STPI(int pc, LegacyRegister a, LegacyRegister b) {
            super(props, ADR(1, a), GPR(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class STS extends IMMREG_class { // store direct to SRAM
        static final LegacyInstrProperties props = new LegacyInstrProperties("sts", "sts", 4, 2);
        static final LegacyInstrProto prototype = new STS(0, DADDR_default, GPR_default);

        LegacyInstr allocate(int pc, int a, LegacyRegister b) {
            return new STS(pc, a, b);
        }

        public STS(int pc, int a, LegacyRegister b) {
            super(props, DADDR(1, a), GPR(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class SUB extends REGREG_class { // subtract register from register
        static final LegacyInstrProperties props = new LegacyInstrProperties("sub", "sub", 2, 1);
        static final LegacyInstrProto prototype = new SUB(0, GPR_default, GPR_default);

        LegacyInstr allocate(int pc, LegacyRegister a, LegacyRegister b) {
            return new SUB(pc, a, b);
        }

        public SUB(int pc, LegacyRegister a, LegacyRegister b) {
            super(props, GPR(1, a), GPR(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class SUBI extends REGIMM_class { // subtract immediate from register
        static final LegacyInstrProperties props = new LegacyInstrProperties("subi", "subi", 2, 1);
        static final LegacyInstrProto prototype = new SUBI(0, HGPR_default, IMM8_default);

        LegacyInstr allocate(int pc, LegacyRegister a, int b) {
            return new SUBI(pc, a, b);
        }

        public SUBI(int pc, LegacyRegister a, int b) {
            super(props, HGPR(1, a), IMM8(2, b));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class SWAP extends REG_class { // swap nibbles in register
        static final LegacyInstrProperties props = new LegacyInstrProperties("swap", "swap", 2, 1);
        static final LegacyInstrProto prototype = new SWAP(0, GPR_default);

        LegacyInstr allocate(int pc, LegacyRegister a) {
            return new SWAP(pc, a);
        }

        public SWAP(int pc, LegacyRegister a) {
            super(props, GPR(1, a));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class TST extends REG_class { // test for zero or minus
        static final LegacyInstrProperties props = new LegacyInstrProperties("tst", "tst", 2, 1);
        static final LegacyInstrProto prototype = new TST(0, GPR_default);

        LegacyInstr allocate(int pc, LegacyRegister a) {
            return new TST(pc, a);
        }

        public TST(int pc, LegacyRegister a) {
            super(props, GPR(1, a));
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }

    public static class WDR extends NONE_class { // watchdog timer reset
        static final LegacyInstrProperties props = new LegacyInstrProperties("wdr", "wdr", 2, 1);
        static final LegacyInstrProto prototype = new WDR(0);

        LegacyInstr allocate(int pc) {
            return new WDR(pc);
        }

        public WDR(int pc) {
            super(props);
        }

        public void accept(LegacyInstrVisitor v) {
            v.visit(this);
        }
    }
//--END INSTR GENERATOR--

}
