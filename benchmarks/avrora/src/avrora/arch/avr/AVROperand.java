package avrora.arch.avr;

/**
 * The <code>AVROperand</code> interface represents operands that are allowed to instructions in this architecture.
 * Inner classes of this interface enumerate the possible operand types to instructions and their constructors allow for
 * dynamic checking of correctness constraints as expressed in the instruction set description.
 */
public abstract class AVROperand {

    public static final byte op_GPR_val = 1;
    public static final byte op_HGPR_val = 2;
    public static final byte op_MGPR_val = 3;
    public static final byte op_YZ_val = 4;
    public static final byte op_EGPR_val = 5;
    public static final byte op_RDL_val = 6;
    public static final byte IMM3_val = 7;
    public static final byte IMM5_val = 8;
    public static final byte IMM6_val = 9;
    public static final byte IMM7_val = 10;
    public static final byte IMM8_val = 11;
    public static final byte SREL_val = 12;
    public static final byte LREL_val = 13;
    public static final byte PADDR_val = 14;
    public static final byte DADDR_val = 15;
    public static final byte R0_B_val = 16;
    public static final byte RZ_W_val = 17;
    public static final byte AI_RZ_W_val = 18;
    public static final byte XYZ_val = 19;
    public static final byte AI_XYZ_val = 20;
    public static final byte PD_XYZ_val = 21;

    /**
     * The <code>op_type</code> field stores a code that determines the type of the operand. This code can be used to
     * dispatch on the type of the operand by switching on the code.
     */
    public final byte op_type;

    /**
     * The <code>accept()</code> method implements the visitor pattern for operand types, allowing a user to
     * double-dispatch on the type of an operand.
     */
    public abstract void accept(AVROperandVisitor v);

    /**
     * The default constructor for the <code>$operand</code> class simply stores the type of the operand in a final
     * field.
     */
    protected AVROperand(byte t) {
        op_type = t;
    }

    /**
     * The <code>AVROperand.Int</code> class is the super class of operands that can take on integer values. It
     * implements rendering the operand as an integer literal.
     */
    abstract static class Int extends AVROperand {

        public final int value;

        Int(byte t, int val) {
            super(t);
            this.value = val;
        }

        public String toString() {
            return Integer.toString(value);
        }
    }


    /**
     * The <code>AVROperand.Sym</code> class is the super class of operands that can take on symbolic (enumerated)
     * values. It implements rendering the operand as the name of the corresponding enumeration type.
     */
    abstract static class Sym extends AVROperand {

        public final AVRSymbol value;

        Sym(byte t, AVRSymbol sym) {
            super(t);
            if (sym == null) throw new Error();
            this.value = sym;
        }

        public String toString() {
            return value.symbol;
        }
    }


    /**
     * The <code>AVROperand.Addr</code> class is the super class of operands that represent an address. It implements
     * rendering the operand as a hexadecimal number.
     */
    abstract static class Addr extends AVROperand {

        public final int value;

        Addr(byte t, int addr) {
            super(t);
            this.value = addr;
        }

        public String toString() {
            String hs = Integer.toHexString(value);
            StringBuffer buf = new StringBuffer("0x");
            for (int cntr = hs.length(); cntr < 4; cntr++)
                buf.append('0');
            buf.append(hs);
            return buf.toString();
        }
    }


    /**
     * The <code>AVROperand.Rel</code> class is the super class of operands that represent an address that is computed
     * relative to the program counter. It implements rendering the operand as the PC plus an offset.
     */
    abstract static class Rel extends AVROperand {

        public final int value;
        public final int relative;

        Rel(byte t, int addr, int rel) {
            super(t);
            this.value = addr;
            this.relative = rel;
        }

        public String toString() {
            if (relative >= 0) return ".+" + relative;
            else
                return "." + relative;
        }
    }

    public static class op_GPR extends Sym {

        op_GPR(String s) {
            super(op_GPR_val, AVRSymbol.get_GPR(s));
        }

        op_GPR(AVRSymbol.GPR sym) {
            super(op_GPR_val, sym);
        }

        public void accept(AVROperandVisitor v) {
            v.visit(this);
        }
    }

    public static class op_HGPR extends Sym {

        op_HGPR(String s) {
            super(op_HGPR_val, AVRSymbol.get_HGPR(s));
        }

        op_HGPR(AVRSymbol.HGPR sym) {
            super(op_HGPR_val, sym);
        }

        public void accept(AVROperandVisitor v) {
            v.visit(this);
        }
    }

    public static class op_MGPR extends Sym {

        op_MGPR(String s) {
            super(op_MGPR_val, AVRSymbol.get_MGPR(s));
        }

        op_MGPR(AVRSymbol.MGPR sym) {
            super(op_MGPR_val, sym);
        }

        public void accept(AVROperandVisitor v) {
            v.visit(this);
        }
    }

    public static class op_YZ extends Sym {

        op_YZ(String s) {
            super(op_YZ_val, AVRSymbol.get_YZ(s));
        }

        op_YZ(AVRSymbol.YZ sym) {
            super(op_YZ_val, sym);
        }

        public void accept(AVROperandVisitor v) {
            v.visit(this);
        }
    }

    public static class op_EGPR extends Sym {

        op_EGPR(String s) {
            super(op_EGPR_val, AVRSymbol.get_EGPR(s));
        }

        op_EGPR(AVRSymbol.EGPR sym) {
            super(op_EGPR_val, sym);
        }

        public void accept(AVROperandVisitor v) {
            v.visit(this);
        }
    }

    public static class op_RDL extends Sym {

        op_RDL(String s) {
            super(op_RDL_val, AVRSymbol.get_RDL(s));
        }

        op_RDL(AVRSymbol.RDL sym) {
            super(op_RDL_val, sym);
        }

        public void accept(AVROperandVisitor v) {
            v.visit(this);
        }
    }

    public static class IMM3 extends Int {

        public static final int low = 0;
        public static final int high = 7;

        IMM3(int val) {
            super(IMM3_val, AVRInstrBuilder.checkValue(val, low, high));
        }

        public void accept(AVROperandVisitor v) {
            v.visit(this);
        }
    }

    public static class IMM5 extends Int {

        public static final int low = 0;
        public static final int high = 31;

        IMM5(int val) {
            super(IMM5_val, AVRInstrBuilder.checkValue(val, low, high));
        }

        public void accept(AVROperandVisitor v) {
            v.visit(this);
        }
    }

    public static class IMM6 extends Int {

        public static final int low = 0;
        public static final int high = 63;

        IMM6(int val) {
            super(IMM6_val, AVRInstrBuilder.checkValue(val, low, high));
        }

        public void accept(AVROperandVisitor v) {
            v.visit(this);
        }
    }

    public static class IMM7 extends Int {

        public static final int low = 0;
        public static final int high = 127;

        IMM7(int val) {
            super(IMM7_val, AVRInstrBuilder.checkValue(val, low, high));
        }

        public void accept(AVROperandVisitor v) {
            v.visit(this);
        }
    }

    public static class IMM8 extends Int {

        public static final int low = 0;
        public static final int high = 255;

        IMM8(int val) {
            super(IMM8_val, AVRInstrBuilder.checkValue(val, low, high));
        }

        public void accept(AVROperandVisitor v) {
            v.visit(this);
        }
    }

    public static class SREL extends Int {

        public static final int low = -64;
        public static final int high = 63;

        SREL(int val) {
            super(SREL_val, AVRInstrBuilder.checkValue(val, low, high));
        }

        public void accept(AVROperandVisitor v) {
            v.visit(this);
        }
    }

    public static class LREL extends Int {

        public static final int low = -1024;
        public static final int high = 1023;

        LREL(int val) {
            super(LREL_val, AVRInstrBuilder.checkValue(val, low, high));
        }

        public void accept(AVROperandVisitor v) {
            v.visit(this);
        }
    }

    public static class PADDR extends Int {

        public static final int low = 0;
        public static final int high = 65536;

        PADDR(int val) {
            super(PADDR_val, AVRInstrBuilder.checkValue(val, low, high));
        }

        public void accept(AVROperandVisitor v) {
            v.visit(this);
        }
    }

    public static class DADDR extends Int {

        public static final int low = 0;
        public static final int high = 65536;

        DADDR(int val) {
            super(DADDR_val, AVRInstrBuilder.checkValue(val, low, high));
        }

        public void accept(AVROperandVisitor v) {
            v.visit(this);
        }
    }

    public static class R0_B extends Sym {

        R0_B(String s) {
            super(R0_B_val, AVRSymbol.get_R0(s));
        }

        R0_B(AVRSymbol.R0 sym) {
            super(R0_B_val, sym);
        }

        public void accept(AVROperandVisitor v) {
            v.visit(this);
        }
    }

    public static class RZ_W extends Sym {

        RZ_W(String s) {
            super(RZ_W_val, AVRSymbol.get_RZ(s));
        }

        RZ_W(AVRSymbol.RZ sym) {
            super(RZ_W_val, sym);
        }

        public void accept(AVROperandVisitor v) {
            v.visit(this);
        }
    }

    public static class AI_RZ_W extends Sym {

        AI_RZ_W(String s) {
            super(AI_RZ_W_val, AVRSymbol.get_RZ(s));
        }

        AI_RZ_W(AVRSymbol.RZ sym) {
            super(AI_RZ_W_val, sym);
        }

        public void accept(AVROperandVisitor v) {
            v.visit(this);
        }
    }

    public static class XYZ extends Sym {

        XYZ(String s) {
            super(XYZ_val, AVRSymbol.get_ADR(s));
        }

        XYZ(AVRSymbol.ADR sym) {
            super(XYZ_val, sym);
        }

        public void accept(AVROperandVisitor v) {
            v.visit(this);
        }
    }

    public static class AI_XYZ extends Sym {

        AI_XYZ(String s) {
            super(AI_XYZ_val, AVRSymbol.get_ADR(s));
        }

        AI_XYZ(AVRSymbol.ADR sym) {
            super(AI_XYZ_val, sym);
        }

        public void accept(AVROperandVisitor v) {
            v.visit(this);
        }
    }

    public static class PD_XYZ extends Sym {

        PD_XYZ(String s) {
            super(PD_XYZ_val, AVRSymbol.get_ADR(s));
        }

        PD_XYZ(AVRSymbol.ADR sym) {
            super(PD_XYZ_val, sym);
        }

        public void accept(AVROperandVisitor v) {
            v.visit(this);
        }
    }

}
