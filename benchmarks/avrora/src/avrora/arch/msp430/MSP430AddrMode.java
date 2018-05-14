package avrora.arch.msp430;
/**
 * The <code>MSP430AddrMode</code> class represents an addressing mode
 * for this architecture. An addressing mode fixes the number and type of
 * operands, the syntax, and the encoding format of the instruction.
 */
public interface MSP430AddrMode {
    public void accept(MSP430Instr i, MSP430AddrModeVisitor v);
    public interface DOUBLE_W extends MSP430AddrMode {
        public MSP430Operand get_source();
        public MSP430Operand get_dest();
    }
    public interface SINGLE_W extends MSP430AddrMode {
        public MSP430Operand get_source();
    }
    public interface DOUBLE_B extends MSP430AddrMode {
        public MSP430Operand get_source();
        public MSP430Operand get_dest();
    }
    public interface SINGLE_B extends MSP430AddrMode {
        public MSP430Operand get_source();
    }
    public static class REG implements MSP430AddrMode, SINGLE_W, SINGLE_B {
        public final MSP430Operand.SREG source;
        public REG(MSP430Operand.SREG source)  {
            this.source = source;
        }
        public void accept(MSP430Instr i, MSP430AddrModeVisitor v) {
            v.visit_REG(i, source);
        }
        public String toString() {
            return ""+" "+source;
        }
        public MSP430Operand get_source() { return source; }
    }
    public static class REGREG implements MSP430AddrMode, DOUBLE_W, DOUBLE_B {
        public final MSP430Operand.SREG source;
        public final MSP430Operand.SREG dest;
        public REGREG(MSP430Operand.SREG source, MSP430Operand.SREG dest)  {
            this.source = source;
            this.dest = dest;
        }
        public void accept(MSP430Instr i, MSP430AddrModeVisitor v) {
            v.visit_REGREG(i, source, dest);
        }
        public String toString() {
            return ""+" "+source+", "+dest;
        }
        public MSP430Operand get_source() { return source; }
        public MSP430Operand get_dest() { return dest; }
    }
    public static class REGIND implements MSP430AddrMode, DOUBLE_W, DOUBLE_B {
        public final MSP430Operand.SREG source;
        public final MSP430Operand.INDX dest;
        public REGIND(MSP430Operand.SREG source, MSP430Operand.INDX dest)  {
            this.source = source;
            this.dest = dest;
        }
        public void accept(MSP430Instr i, MSP430AddrModeVisitor v) {
            v.visit_REGIND(i, source, dest);
        }
        public String toString() {
            return ""+" "+source+", "+dest.index+"("+dest.reg+")";
        }
        public MSP430Operand get_source() { return source; }
        public MSP430Operand get_dest() { return dest; }
    }
    public static class REGSYM implements MSP430AddrMode, DOUBLE_W, DOUBLE_B {
        public final MSP430Operand.SREG source;
        public final MSP430Operand.SYMB dest;
        public REGSYM(MSP430Operand.SREG source, MSP430Operand.SYMB dest)  {
            this.source = source;
            this.dest = dest;
        }
        public void accept(MSP430Instr i, MSP430AddrModeVisitor v) {
            v.visit_REGSYM(i, source, dest);
        }
        public String toString() {
            return ""+" "+source+", "+dest;
        }
        public MSP430Operand get_source() { return source; }
        public MSP430Operand get_dest() { return dest; }
    }
    public static class REGABS implements MSP430AddrMode, DOUBLE_W, DOUBLE_B {
        public final MSP430Operand.SREG source;
        public final MSP430Operand.ABSO dest;
        public REGABS(MSP430Operand.SREG source, MSP430Operand.ABSO dest)  {
            this.source = source;
            this.dest = dest;
        }
        public void accept(MSP430Instr i, MSP430AddrModeVisitor v) {
            v.visit_REGABS(i, source, dest);
        }
        public String toString() {
            return ""+" "+source+", &"+dest;
        }
        public MSP430Operand get_source() { return source; }
        public MSP430Operand get_dest() { return dest; }
    }
    public static class IND implements MSP430AddrMode, SINGLE_W, SINGLE_B {
        public final MSP430Operand.INDX source;
        public IND(MSP430Operand.INDX source)  {
            this.source = source;
        }
        public void accept(MSP430Instr i, MSP430AddrModeVisitor v) {
            v.visit_IND(i, source);
        }
        public String toString() {
            return ""+" "+source.index+"("+source.reg+")";
        }
        public MSP430Operand get_source() { return source; }
    }
    public static class INDREG implements MSP430AddrMode, DOUBLE_W, DOUBLE_B {
        public final MSP430Operand.INDX source;
        public final MSP430Operand.SREG dest;
        public INDREG(MSP430Operand.INDX source, MSP430Operand.SREG dest)  {
            this.source = source;
            this.dest = dest;
        }
        public void accept(MSP430Instr i, MSP430AddrModeVisitor v) {
            v.visit_INDREG(i, source, dest);
        }
        public String toString() {
            return ""+" "+source.index+"("+source.reg+"), "+dest;
        }
        public MSP430Operand get_source() { return source; }
        public MSP430Operand get_dest() { return dest; }
    }
    public static class INDIND implements MSP430AddrMode, DOUBLE_W, DOUBLE_B {
        public final MSP430Operand.INDX source;
        public final MSP430Operand.INDX dest;
        public INDIND(MSP430Operand.INDX source, MSP430Operand.INDX dest)  {
            this.source = source;
            this.dest = dest;
        }
        public void accept(MSP430Instr i, MSP430AddrModeVisitor v) {
            v.visit_INDIND(i, source, dest);
        }
        public String toString() {
            return ""+" "+source.index+"("+source.reg+"), "+dest.index+"("+dest.reg+")";
        }
        public MSP430Operand get_source() { return source; }
        public MSP430Operand get_dest() { return dest; }
    }
    public static class SYM implements MSP430AddrMode, SINGLE_W, SINGLE_B {
        public final MSP430Operand.SYMB source;
        public SYM(MSP430Operand.SYMB source)  {
            this.source = source;
        }
        public void accept(MSP430Instr i, MSP430AddrModeVisitor v) {
            v.visit_SYM(i, source);
        }
        public String toString() {
            return ""+" "+source;
        }
        public MSP430Operand get_source() { return source; }
    }
    public static class SYMREG implements MSP430AddrMode, DOUBLE_W, DOUBLE_B {
        public final MSP430Operand.SYMB source;
        public final MSP430Operand.SREG dest;
        public SYMREG(MSP430Operand.SYMB source, MSP430Operand.SREG dest)  {
            this.source = source;
            this.dest = dest;
        }
        public void accept(MSP430Instr i, MSP430AddrModeVisitor v) {
            v.visit_SYMREG(i, source, dest);
        }
        public String toString() {
            return ""+" "+source+", "+dest;
        }
        public MSP430Operand get_source() { return source; }
        public MSP430Operand get_dest() { return dest; }
    }
    public static class INDSYM implements MSP430AddrMode, DOUBLE_W, DOUBLE_B {
        public final MSP430Operand.INDX source;
        public final MSP430Operand.SYMB dest;
        public INDSYM(MSP430Operand.INDX source, MSP430Operand.SYMB dest)  {
            this.source = source;
            this.dest = dest;
        }
        public void accept(MSP430Instr i, MSP430AddrModeVisitor v) {
            v.visit_INDSYM(i, source, dest);
        }
        public String toString() {
            return ""+" "+source.index+"("+source.reg+"), "+dest;
        }
        public MSP430Operand get_source() { return source; }
        public MSP430Operand get_dest() { return dest; }
    }
    public static class INDABS implements MSP430AddrMode, DOUBLE_W, DOUBLE_B {
        public final MSP430Operand.INDX source;
        public final MSP430Operand.ABSO dest;
        public INDABS(MSP430Operand.INDX source, MSP430Operand.ABSO dest)  {
            this.source = source;
            this.dest = dest;
        }
        public void accept(MSP430Instr i, MSP430AddrModeVisitor v) {
            v.visit_INDABS(i, source, dest);
        }
        public String toString() {
            return ""+" "+source.index+"("+source.reg+"), &"+dest;
        }
        public MSP430Operand get_source() { return source; }
        public MSP430Operand get_dest() { return dest; }
    }
    public static class SYMABS implements MSP430AddrMode, DOUBLE_W, DOUBLE_B {
        public final MSP430Operand.SYMB source;
        public final MSP430Operand.ABSO dest;
        public SYMABS(MSP430Operand.SYMB source, MSP430Operand.ABSO dest)  {
            this.source = source;
            this.dest = dest;
        }
        public void accept(MSP430Instr i, MSP430AddrModeVisitor v) {
            v.visit_SYMABS(i, source, dest);
        }
        public String toString() {
            return ""+" "+source+", &"+dest;
        }
        public MSP430Operand get_source() { return source; }
        public MSP430Operand get_dest() { return dest; }
    }
    public static class SYMIND implements MSP430AddrMode, DOUBLE_W, DOUBLE_B {
        public final MSP430Operand.SYMB source;
        public final MSP430Operand.INDX dest;
        public SYMIND(MSP430Operand.SYMB source, MSP430Operand.INDX dest)  {
            this.source = source;
            this.dest = dest;
        }
        public void accept(MSP430Instr i, MSP430AddrModeVisitor v) {
            v.visit_SYMIND(i, source, dest);
        }
        public String toString() {
            return ""+" "+source+", "+dest.index+"("+dest.reg+")";
        }
        public MSP430Operand get_source() { return source; }
        public MSP430Operand get_dest() { return dest; }
    }
    public static class SYMSYM implements MSP430AddrMode, DOUBLE_W, DOUBLE_B {
        public final MSP430Operand.SYMB source;
        public final MSP430Operand.SYMB dest;
        public SYMSYM(MSP430Operand.SYMB source, MSP430Operand.SYMB dest)  {
            this.source = source;
            this.dest = dest;
        }
        public void accept(MSP430Instr i, MSP430AddrModeVisitor v) {
            v.visit_SYMSYM(i, source, dest);
        }
        public String toString() {
            return ""+" "+source+", "+dest;
        }
        public MSP430Operand get_source() { return source; }
        public MSP430Operand get_dest() { return dest; }
    }
    public static class ABSSYM implements MSP430AddrMode, DOUBLE_W, DOUBLE_B {
        public final MSP430Operand.ABSO source;
        public final MSP430Operand.SYMB dest;
        public ABSSYM(MSP430Operand.ABSO source, MSP430Operand.SYMB dest)  {
            this.source = source;
            this.dest = dest;
        }
        public void accept(MSP430Instr i, MSP430AddrModeVisitor v) {
            v.visit_ABSSYM(i, source, dest);
        }
        public String toString() {
            return ""+" &"+source+", "+dest;
        }
        public MSP430Operand get_source() { return source; }
        public MSP430Operand get_dest() { return dest; }
    }
    public static class ABS implements MSP430AddrMode, SINGLE_W, SINGLE_B {
        public final MSP430Operand.ABSO source;
        public ABS(MSP430Operand.ABSO source)  {
            this.source = source;
        }
        public void accept(MSP430Instr i, MSP430AddrModeVisitor v) {
            v.visit_ABS(i, source);
        }
        public String toString() {
            return ""+" &"+source;
        }
        public MSP430Operand get_source() { return source; }
    }
    public static class ABSREG implements MSP430AddrMode, DOUBLE_W, DOUBLE_B {
        public final MSP430Operand.ABSO source;
        public final MSP430Operand.SREG dest;
        public ABSREG(MSP430Operand.ABSO source, MSP430Operand.SREG dest)  {
            this.source = source;
            this.dest = dest;
        }
        public void accept(MSP430Instr i, MSP430AddrModeVisitor v) {
            v.visit_ABSREG(i, source, dest);
        }
        public String toString() {
            return ""+" &"+source+", "+dest;
        }
        public MSP430Operand get_source() { return source; }
        public MSP430Operand get_dest() { return dest; }
    }
    public static class ABSIND implements MSP430AddrMode, DOUBLE_W, DOUBLE_B {
        public final MSP430Operand.ABSO source;
        public final MSP430Operand.INDX dest;
        public ABSIND(MSP430Operand.ABSO source, MSP430Operand.INDX dest)  {
            this.source = source;
            this.dest = dest;
        }
        public void accept(MSP430Instr i, MSP430AddrModeVisitor v) {
            v.visit_ABSIND(i, source, dest);
        }
        public String toString() {
            return ""+" &"+source+", "+dest.index+"("+dest.reg+")";
        }
        public MSP430Operand get_source() { return source; }
        public MSP430Operand get_dest() { return dest; }
    }
    public static class ABSABS implements MSP430AddrMode, DOUBLE_W, DOUBLE_B {
        public final MSP430Operand.ABSO source;
        public final MSP430Operand.ABSO dest;
        public ABSABS(MSP430Operand.ABSO source, MSP430Operand.ABSO dest)  {
            this.source = source;
            this.dest = dest;
        }
        public void accept(MSP430Instr i, MSP430AddrModeVisitor v) {
            v.visit_ABSABS(i, source, dest);
        }
        public String toString() {
            return ""+" &"+source+", &"+dest;
        }
        public MSP430Operand get_source() { return source; }
        public MSP430Operand get_dest() { return dest; }
    }
    public static class IREGSYM implements MSP430AddrMode, DOUBLE_W, DOUBLE_B {
        public final MSP430Operand.IREG source;
        public final MSP430Operand.SYMB dest;
        public IREGSYM(MSP430Operand.IREG source, MSP430Operand.SYMB dest)  {
            this.source = source;
            this.dest = dest;
        }
        public void accept(MSP430Instr i, MSP430AddrModeVisitor v) {
            v.visit_IREGSYM(i, source, dest);
        }
        public String toString() {
            return ""+" @"+source+", "+dest;
        }
        public MSP430Operand get_source() { return source; }
        public MSP430Operand get_dest() { return dest; }
    }
    public static class IREG implements MSP430AddrMode, SINGLE_W, SINGLE_B {
        public final MSP430Operand.IREG source;
        public IREG(MSP430Operand.IREG source)  {
            this.source = source;
        }
        public void accept(MSP430Instr i, MSP430AddrModeVisitor v) {
            v.visit_IREG(i, source);
        }
        public String toString() {
            return ""+" @"+source;
        }
        public MSP430Operand get_source() { return source; }
    }
    public static class IREGREG implements MSP430AddrMode, DOUBLE_W, DOUBLE_B {
        public final MSP430Operand.IREG source;
        public final MSP430Operand.SREG dest;
        public IREGREG(MSP430Operand.IREG source, MSP430Operand.SREG dest)  {
            this.source = source;
            this.dest = dest;
        }
        public void accept(MSP430Instr i, MSP430AddrModeVisitor v) {
            v.visit_IREGREG(i, source, dest);
        }
        public String toString() {
            return ""+" @"+source+", "+dest;
        }
        public MSP430Operand get_source() { return source; }
        public MSP430Operand get_dest() { return dest; }
    }
    public static class IREGIND implements MSP430AddrMode, DOUBLE_W, DOUBLE_B {
        public final MSP430Operand.IREG source;
        public final MSP430Operand.INDX dest;
        public IREGIND(MSP430Operand.IREG source, MSP430Operand.INDX dest)  {
            this.source = source;
            this.dest = dest;
        }
        public void accept(MSP430Instr i, MSP430AddrModeVisitor v) {
            v.visit_IREGIND(i, source, dest);
        }
        public String toString() {
            return ""+" @"+source+", "+dest.index+"("+dest.reg+")";
        }
        public MSP430Operand get_source() { return source; }
        public MSP430Operand get_dest() { return dest; }
    }
    public static class IREGABS implements MSP430AddrMode, DOUBLE_W, DOUBLE_B {
        public final MSP430Operand.IREG source;
        public final MSP430Operand.ABSO dest;
        public IREGABS(MSP430Operand.IREG source, MSP430Operand.ABSO dest)  {
            this.source = source;
            this.dest = dest;
        }
        public void accept(MSP430Instr i, MSP430AddrModeVisitor v) {
            v.visit_IREGABS(i, source, dest);
        }
        public String toString() {
            return ""+" @"+source+", &"+dest;
        }
        public MSP430Operand get_source() { return source; }
        public MSP430Operand get_dest() { return dest; }
    }
    public static class IMM implements MSP430AddrMode, SINGLE_W, SINGLE_B {
        public final MSP430Operand.IMM source;
        public IMM(MSP430Operand.IMM source)  {
            this.source = source;
        }
        public void accept(MSP430Instr i, MSP430AddrModeVisitor v) {
            v.visit_IMM(i, source);
        }
        public String toString() {
            return ""+" #"+source;
        }
        public MSP430Operand get_source() { return source; }
    }
    public static class IMMREG implements MSP430AddrMode, DOUBLE_W, DOUBLE_B {
        public final MSP430Operand.IMM source;
        public final MSP430Operand.SREG dest;
        public IMMREG(MSP430Operand.IMM source, MSP430Operand.SREG dest)  {
            this.source = source;
            this.dest = dest;
        }
        public void accept(MSP430Instr i, MSP430AddrModeVisitor v) {
            v.visit_IMMREG(i, source, dest);
        }
        public String toString() {
            return ""+" #"+source+", "+dest;
        }
        public MSP430Operand get_source() { return source; }
        public MSP430Operand get_dest() { return dest; }
    }
    public static class IMMIND implements MSP430AddrMode, DOUBLE_W, DOUBLE_B {
        public final MSP430Operand.IMM source;
        public final MSP430Operand.INDX dest;
        public IMMIND(MSP430Operand.IMM source, MSP430Operand.INDX dest)  {
            this.source = source;
            this.dest = dest;
        }
        public void accept(MSP430Instr i, MSP430AddrModeVisitor v) {
            v.visit_IMMIND(i, source, dest);
        }
        public String toString() {
            return ""+" #"+source+", "+dest.index+"("+dest.reg+")";
        }
        public MSP430Operand get_source() { return source; }
        public MSP430Operand get_dest() { return dest; }
    }
    public static class IMMSYM implements MSP430AddrMode, DOUBLE_W, DOUBLE_B {
        public final MSP430Operand.IMM source;
        public final MSP430Operand.SYMB dest;
        public IMMSYM(MSP430Operand.IMM source, MSP430Operand.SYMB dest)  {
            this.source = source;
            this.dest = dest;
        }
        public void accept(MSP430Instr i, MSP430AddrModeVisitor v) {
            v.visit_IMMSYM(i, source, dest);
        }
        public String toString() {
            return ""+" #"+source+", "+dest;
        }
        public MSP430Operand get_source() { return source; }
        public MSP430Operand get_dest() { return dest; }
    }
    public static class IMMABS implements MSP430AddrMode, DOUBLE_W, DOUBLE_B {
        public final MSP430Operand.IMM source;
        public final MSP430Operand.ABSO dest;
        public IMMABS(MSP430Operand.IMM source, MSP430Operand.ABSO dest)  {
            this.source = source;
            this.dest = dest;
        }
        public void accept(MSP430Instr i, MSP430AddrModeVisitor v) {
            v.visit_IMMABS(i, source, dest);
        }
        public String toString() {
            return ""+" #"+source+", &"+dest;
        }
        public MSP430Operand get_source() { return source; }
        public MSP430Operand get_dest() { return dest; }
    }
    public static class IMML implements MSP430AddrMode, SINGLE_W, SINGLE_B {
        public final MSP430Operand.IMML source;
        public IMML(MSP430Operand.IMML source)  {
            this.source = source;
        }
        public void accept(MSP430Instr i, MSP430AddrModeVisitor v) {
            v.visit_IMML(i, source);
        }
        public String toString() {
            return ""+" #"+source;
        }
        public MSP430Operand get_source() { return source; }
    }
    public static class IMMLREG implements MSP430AddrMode, DOUBLE_W, DOUBLE_B {
        public final MSP430Operand.IMML source;
        public final MSP430Operand.SREG dest;
        public IMMLREG(MSP430Operand.IMML source, MSP430Operand.SREG dest)  {
            this.source = source;
            this.dest = dest;
        }
        public void accept(MSP430Instr i, MSP430AddrModeVisitor v) {
            v.visit_IMMLREG(i, source, dest);
        }
        public String toString() {
            return ""+" #"+source+", "+dest;
        }
        public MSP430Operand get_source() { return source; }
        public MSP430Operand get_dest() { return dest; }
    }
    public static class IMMLIND implements MSP430AddrMode, DOUBLE_W, DOUBLE_B {
        public final MSP430Operand.IMML source;
        public final MSP430Operand.INDX dest;
        public IMMLIND(MSP430Operand.IMML source, MSP430Operand.INDX dest)  {
            this.source = source;
            this.dest = dest;
        }
        public void accept(MSP430Instr i, MSP430AddrModeVisitor v) {
            v.visit_IMMLIND(i, source, dest);
        }
        public String toString() {
            return ""+" #"+source+", "+dest.index+"("+dest.reg+")";
        }
        public MSP430Operand get_source() { return source; }
        public MSP430Operand get_dest() { return dest; }
    }
    public static class IMMLSYM implements MSP430AddrMode, DOUBLE_W, DOUBLE_B {
        public final MSP430Operand.IMML source;
        public final MSP430Operand.SYMB dest;
        public IMMLSYM(MSP430Operand.IMML source, MSP430Operand.SYMB dest)  {
            this.source = source;
            this.dest = dest;
        }
        public void accept(MSP430Instr i, MSP430AddrModeVisitor v) {
            v.visit_IMMLSYM(i, source, dest);
        }
        public String toString() {
            return ""+" #"+source+", "+dest;
        }
        public MSP430Operand get_source() { return source; }
        public MSP430Operand get_dest() { return dest; }
    }
    public static class IMMLABS implements MSP430AddrMode, DOUBLE_W, DOUBLE_B {
        public final MSP430Operand.IMML source;
        public final MSP430Operand.ABSO dest;
        public IMMLABS(MSP430Operand.IMML source, MSP430Operand.ABSO dest)  {
            this.source = source;
            this.dest = dest;
        }
        public void accept(MSP430Instr i, MSP430AddrModeVisitor v) {
            v.visit_IMMLABS(i, source, dest);
        }
        public String toString() {
            return ""+" #"+source+", &"+dest;
        }
        public MSP430Operand get_source() { return source; }
        public MSP430Operand get_dest() { return dest; }
    }
    public static class AUTO_B implements MSP430AddrMode, SINGLE_B {
        public final MSP430Operand.AIREG_B source;
        public AUTO_B(MSP430Operand.AIREG_B source)  {
            this.source = source;
        }
        public void accept(MSP430Instr i, MSP430AddrModeVisitor v) {
            v.visit_AUTO_B(i, source);
        }
        public String toString() {
            return ""+" @"+source+"+";
        }
        public MSP430Operand get_source() { return source; }
    }
    public static class AUTOREG_B implements MSP430AddrMode, DOUBLE_B {
        public final MSP430Operand.AIREG_B source;
        public final MSP430Operand.SREG dest;
        public AUTOREG_B(MSP430Operand.AIREG_B source, MSP430Operand.SREG dest)  {
            this.source = source;
            this.dest = dest;
        }
        public void accept(MSP430Instr i, MSP430AddrModeVisitor v) {
            v.visit_AUTOREG_B(i, source, dest);
        }
        public String toString() {
            return ""+" @"+source+"+, "+dest;
        }
        public MSP430Operand get_source() { return source; }
        public MSP430Operand get_dest() { return dest; }
    }
    public static class AUTOIND_B implements MSP430AddrMode, DOUBLE_B {
        public final MSP430Operand.AIREG_B source;
        public final MSP430Operand.INDX dest;
        public AUTOIND_B(MSP430Operand.AIREG_B source, MSP430Operand.INDX dest)  {
            this.source = source;
            this.dest = dest;
        }
        public void accept(MSP430Instr i, MSP430AddrModeVisitor v) {
            v.visit_AUTOIND_B(i, source, dest);
        }
        public String toString() {
            return ""+" @"+source+"+, "+dest.index+"("+dest.reg+")";
        }
        public MSP430Operand get_source() { return source; }
        public MSP430Operand get_dest() { return dest; }
    }
    public static class AUTOSYM_B implements MSP430AddrMode, DOUBLE_B {
        public final MSP430Operand.AIREG_B source;
        public final MSP430Operand.SYMB dest;
        public AUTOSYM_B(MSP430Operand.AIREG_B source, MSP430Operand.SYMB dest)  {
            this.source = source;
            this.dest = dest;
        }
        public void accept(MSP430Instr i, MSP430AddrModeVisitor v) {
            v.visit_AUTOSYM_B(i, source, dest);
        }
        public String toString() {
            return ""+" @"+source+"+, "+dest;
        }
        public MSP430Operand get_source() { return source; }
        public MSP430Operand get_dest() { return dest; }
    }
    public static class AUTOABS_B implements MSP430AddrMode, DOUBLE_B {
        public final MSP430Operand.AIREG_B source;
        public final MSP430Operand.ABSO dest;
        public AUTOABS_B(MSP430Operand.AIREG_B source, MSP430Operand.ABSO dest)  {
            this.source = source;
            this.dest = dest;
        }
        public void accept(MSP430Instr i, MSP430AddrModeVisitor v) {
            v.visit_AUTOABS_B(i, source, dest);
        }
        public String toString() {
            return ""+" @"+source+"+, "+dest;
        }
        public MSP430Operand get_source() { return source; }
        public MSP430Operand get_dest() { return dest; }
    }
    public static class AUTO_W implements MSP430AddrMode, SINGLE_W {
        public final MSP430Operand.AIREG_W source;
        public AUTO_W(MSP430Operand.AIREG_W source)  {
            this.source = source;
        }
        public void accept(MSP430Instr i, MSP430AddrModeVisitor v) {
            v.visit_AUTO_W(i, source);
        }
        public String toString() {
            return ""+" @"+source+"+";
        }
        public MSP430Operand get_source() { return source; }
    }
    public static class AUTOREG_W implements MSP430AddrMode, DOUBLE_W {
        public final MSP430Operand.AIREG_W source;
        public final MSP430Operand.SREG dest;
        public AUTOREG_W(MSP430Operand.AIREG_W source, MSP430Operand.SREG dest)  {
            this.source = source;
            this.dest = dest;
        }
        public void accept(MSP430Instr i, MSP430AddrModeVisitor v) {
            v.visit_AUTOREG_W(i, source, dest);
        }
        public String toString() {
            return ""+" @"+source+"+, "+dest;
        }
        public MSP430Operand get_source() { return source; }
        public MSP430Operand get_dest() { return dest; }
    }
    public static class AUTOIND_W implements MSP430AddrMode, DOUBLE_W {
        public final MSP430Operand.AIREG_W source;
        public final MSP430Operand.INDX dest;
        public AUTOIND_W(MSP430Operand.AIREG_W source, MSP430Operand.INDX dest)  {
            this.source = source;
            this.dest = dest;
        }
        public void accept(MSP430Instr i, MSP430AddrModeVisitor v) {
            v.visit_AUTOIND_W(i, source, dest);
        }
        public String toString() {
            return ""+" @"+source+"+, "+dest.index+"("+dest.reg+")";
        }
        public MSP430Operand get_source() { return source; }
        public MSP430Operand get_dest() { return dest; }
    }
    public static class AUTOSYM_W implements MSP430AddrMode, DOUBLE_W {
        public final MSP430Operand.AIREG_W source;
        public final MSP430Operand.SYMB dest;
        public AUTOSYM_W(MSP430Operand.AIREG_W source, MSP430Operand.SYMB dest)  {
            this.source = source;
            this.dest = dest;
        }
        public void accept(MSP430Instr i, MSP430AddrModeVisitor v) {
            v.visit_AUTOSYM_W(i, source, dest);
        }
        public String toString() {
            return ""+" @"+source+"+, "+dest;
        }
        public MSP430Operand get_source() { return source; }
        public MSP430Operand get_dest() { return dest; }
    }
    public static class AUTOABS_W implements MSP430AddrMode, DOUBLE_W {
        public final MSP430Operand.AIREG_W source;
        public final MSP430Operand.ABSO dest;
        public AUTOABS_W(MSP430Operand.AIREG_W source, MSP430Operand.ABSO dest)  {
            this.source = source;
            this.dest = dest;
        }
        public void accept(MSP430Instr i, MSP430AddrModeVisitor v) {
            v.visit_AUTOABS_W(i, source, dest);
        }
        public String toString() {
            return ""+" @"+source+"+, "+dest;
        }
        public MSP430Operand get_source() { return source; }
        public MSP430Operand get_dest() { return dest; }
    }
    public static class JMP implements MSP430AddrMode {
        public final MSP430Operand.JUMP target;
        public JMP(MSP430Operand.JUMP target)  {
            this.target = target;
        }
        public void accept(MSP430Instr i, MSP430AddrModeVisitor v) {
            v.visit_JMP(i, target);
        }
        public String toString() {
            return "" + ' ' + target;
        }
        public MSP430Operand get_target() { return target; }
    }
}
