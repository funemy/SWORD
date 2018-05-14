package avrora.arch.msp430;
/**
 * The <code>MSP430Operand</code> interface represents operands that are
 * allowed to instructions in this architecture. Inner classes of this
 * interface enumerate the possible operand types to instructions and
 * their constructors allow for dynamic checking of correctness
 * constraints as expressed in the instruction set description.
 */
public abstract class MSP430Operand {
    public static final byte SREG_val = 1;
    public static final byte AIREG_B_val = 2;
    public static final byte AIREG_W_val = 3;
    public static final byte IREG_val = 4;
    public static final byte IMM_val = 5;
    public static final byte IMML_val = 6;
    public static final byte INDX_val = 7;
    public static final byte SYMB_val = 8;
    public static final byte ABSO_val = 9;
    public static final byte JUMP_val = 10;

    /**
     * The <code>op_type</code> field stores a code that determines the type
     * of the operand. This code can be used to dispatch on the type of the
     * operand by switching on the code.
     */
    public final byte op_type;

    /**
     * The <code>accept()</code> method implements the visitor pattern for
     * operand types, allowing a user to double-dispatch on the type of an
     * operand.
     */
    public abstract void accept(MSP430OperandVisitor v);

    /**
     * The default constructor for the <code>$operand</code> class simply
     * stores the type of the operand in a final field.
     */
    protected MSP430Operand(byte t) {
        op_type = t;
    }

    /**
     * The <code>MSP430Operand.Int</code> class is the super class of
     * operands that can take on integer values. It implements rendering the
     * operand as an integer literal.
     */
    abstract static class Int extends MSP430Operand {
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
     * The <code>MSP430Operand.Sym</code> class is the super class of
     * operands that can take on symbolic (enumerated) values. It implements
     * rendering the operand as the name of the corresponding enumeration
     * type.
     */
    abstract static class Sym extends MSP430Operand {
        public final MSP430Symbol value;
        Sym(byte t, MSP430Symbol sym) {
            super(t);
            if ( sym == null ) throw new Error();
            this.value = sym;
        }
        public String toString() {
            return value.symbol;
        }
    }


    /**
     * The <code>MSP430Operand.Addr</code> class is the super class of
     * operands that represent an address. It implements rendering the
     * operand as a hexadecimal number.
     */
    abstract static class Addr extends MSP430Operand {
        public final int value;
        Addr(byte t, int addr) {
            super(t);
            this.value = addr;
        }
        public String toString() {
            String hs = Integer.toHexString(value);
            StringBuffer buf = new StringBuffer("0x");
            for ( int cntr = hs.length(); cntr < 4; cntr++ ) buf.append('0');
            buf.append(hs);
            return buf.toString();
        }
    }


    /**
     * The <code>MSP430Operand.Rel</code> class is the super class of
     * operands that represent an address that is computed relative to the
     * program counter. It implements rendering the operand as the PC plus an
     * offset.
     */
    abstract static class Rel extends MSP430Operand {
        public final int value;
        public final int relative;
        Rel(byte t, int addr, int rel) {
            super(t);
            this.value = addr;
            this.relative = rel;
        }
        public String toString() {
            if ( relative >= 0 ) return ".+"+relative;
            else return "."+relative;
        }
    }

    public static class SREG extends Sym {
        SREG(String s) {
            super(SREG_val, MSP430Symbol.get_GPR(s));
        }
        SREG(MSP430Symbol.GPR sym) {
            super(SREG_val, sym);
        }
        public void accept(MSP430OperandVisitor v) {
            v.visit(this);
        }
    }

    public static class AIREG_B extends Sym {
        AIREG_B(String s) {
            super(AIREG_B_val, MSP430Symbol.get_GPR(s));
        }
        AIREG_B(MSP430Symbol.GPR sym) {
            super(AIREG_B_val, sym);
        }
        public void accept(MSP430OperandVisitor v) {
            v.visit(this);
        }
    }

    public static class AIREG_W extends Sym {
        AIREG_W(String s) {
            super(AIREG_W_val, MSP430Symbol.get_GPR(s));
        }
        AIREG_W(MSP430Symbol.GPR sym) {
            super(AIREG_W_val, sym);
        }
        public void accept(MSP430OperandVisitor v) {
            v.visit(this);
        }
    }

    public static class IREG extends Sym {
        IREG(String s) {
            super(IREG_val, MSP430Symbol.get_GPR(s));
        }
        IREG(MSP430Symbol.GPR sym) {
            super(IREG_val, sym);
        }
        public void accept(MSP430OperandVisitor v) {
            v.visit(this);
        }
    }

    public static class IMM extends Int {
        public static final int low = -32768;
        public static final int high = 65536;
        IMM(int val) {
            super(IMM_val, MSP430InstrBuilder.checkValue(val, low, high));
        }
        public void accept(MSP430OperandVisitor v) {
            v.visit(this);
        }
    }

    public static class IMML extends Int {
        public static final int low = -32768;
        public static final int high = 65536;
        IMML(int val) {
            super(IMML_val, MSP430InstrBuilder.checkValue(val, low, high));
        }
        public void accept(MSP430OperandVisitor v) {
            v.visit(this);
        }
    }

    public static class INDX extends MSP430Operand {
        public final MSP430Operand.SREG reg;
        public final MSP430Operand.IMM index;
        public INDX(MSP430Operand.SREG reg, MSP430Operand.IMM index)  {
            super(INDX_val);
            this.reg = reg;
            this.index = index;
        }
        public void accept(MSP430OperandVisitor v) {
            v.visit(this);
        }
    }

    public static class SYMB extends Rel {
        public static final int low = -32768;
        public static final int high = 65535;
        SYMB(int pc, int rel) {
            super(SYMB_val, pc + 1 + rel, MSP430InstrBuilder.checkValue(rel, low, high));
        }
        public void accept(MSP430OperandVisitor v) {
            v.visit(this);
        }
    }

    public static class ABSO extends Addr {
        public static final int low = -32768;
        public static final int high = 65535;
        ABSO(int addr) {
            super(ABSO_val, MSP430InstrBuilder.checkValue(addr, low, high));
        }
        public void accept(MSP430OperandVisitor v) {
            v.visit(this);
        }
    }

    public static class JUMP extends Rel {
        public static final int low = -512;
        public static final int high = 511;
        JUMP(int pc, int rel) {
            super(JUMP_val, pc + 2 + 2 * rel, MSP430InstrBuilder.checkValue(rel, low, high));
        }
        public void accept(MSP430OperandVisitor v) {
            v.visit(this);
        }
    }

}
