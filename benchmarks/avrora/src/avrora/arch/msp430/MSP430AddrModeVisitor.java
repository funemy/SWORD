package avrora.arch.msp430;
/**
 * The <code>MSP430AddrModeVisitor</code> interface implements the
 * visitor pattern for addressing modes.
 */
public interface MSP430AddrModeVisitor {
    public void visit_REG(MSP430Instr i, MSP430Operand.SREG source);
    public void visit_REGREG(MSP430Instr i, MSP430Operand.SREG source, MSP430Operand.SREG dest);
    public void visit_REGIND(MSP430Instr i, MSP430Operand.SREG source, MSP430Operand.INDX dest);
    public void visit_REGSYM(MSP430Instr i, MSP430Operand.SREG source, MSP430Operand.SYMB dest);
    public void visit_REGABS(MSP430Instr i, MSP430Operand.SREG source, MSP430Operand.ABSO dest);
    public void visit_IND(MSP430Instr i, MSP430Operand.INDX source);
    public void visit_INDREG(MSP430Instr i, MSP430Operand.INDX source, MSP430Operand.SREG dest);
    public void visit_INDIND(MSP430Instr i, MSP430Operand.INDX source, MSP430Operand.INDX dest);
    public void visit_SYM(MSP430Instr i, MSP430Operand.SYMB source);
    public void visit_SYMREG(MSP430Instr i, MSP430Operand.SYMB source, MSP430Operand.SREG dest);
    public void visit_INDSYM(MSP430Instr i, MSP430Operand.INDX source, MSP430Operand.SYMB dest);
    public void visit_INDABS(MSP430Instr i, MSP430Operand.INDX source, MSP430Operand.ABSO dest);
    public void visit_SYMABS(MSP430Instr i, MSP430Operand.SYMB source, MSP430Operand.ABSO dest);
    public void visit_SYMIND(MSP430Instr i, MSP430Operand.SYMB source, MSP430Operand.INDX dest);
    public void visit_SYMSYM(MSP430Instr i, MSP430Operand.SYMB source, MSP430Operand.SYMB dest);
    public void visit_ABSSYM(MSP430Instr i, MSP430Operand.ABSO source, MSP430Operand.SYMB dest);
    public void visit_ABS(MSP430Instr i, MSP430Operand.ABSO source);
    public void visit_ABSREG(MSP430Instr i, MSP430Operand.ABSO source, MSP430Operand.SREG dest);
    public void visit_ABSIND(MSP430Instr i, MSP430Operand.ABSO source, MSP430Operand.INDX dest);
    public void visit_ABSABS(MSP430Instr i, MSP430Operand.ABSO source, MSP430Operand.ABSO dest);
    public void visit_IREGSYM(MSP430Instr i, MSP430Operand.IREG source, MSP430Operand.SYMB dest);
    public void visit_IREG(MSP430Instr i, MSP430Operand.IREG source);
    public void visit_IREGREG(MSP430Instr i, MSP430Operand.IREG source, MSP430Operand.SREG dest);
    public void visit_IREGIND(MSP430Instr i, MSP430Operand.IREG source, MSP430Operand.INDX dest);
    public void visit_IREGABS(MSP430Instr i, MSP430Operand.IREG source, MSP430Operand.ABSO dest);
    public void visit_IMM(MSP430Instr i, MSP430Operand.IMM source);
    public void visit_IMMREG(MSP430Instr i, MSP430Operand.IMM source, MSP430Operand.SREG dest);
    public void visit_IMMIND(MSP430Instr i, MSP430Operand.IMM source, MSP430Operand.INDX dest);
    public void visit_IMMSYM(MSP430Instr i, MSP430Operand.IMM source, MSP430Operand.SYMB dest);
    public void visit_IMMABS(MSP430Instr i, MSP430Operand.IMM source, MSP430Operand.ABSO dest);
    public void visit_IMML(MSP430Instr i, MSP430Operand.IMML source);
    public void visit_IMMLREG(MSP430Instr i, MSP430Operand.IMML source, MSP430Operand.SREG dest);
    public void visit_IMMLIND(MSP430Instr i, MSP430Operand.IMML source, MSP430Operand.INDX dest);
    public void visit_IMMLSYM(MSP430Instr i, MSP430Operand.IMML source, MSP430Operand.SYMB dest);
    public void visit_IMMLABS(MSP430Instr i, MSP430Operand.IMML source, MSP430Operand.ABSO dest);
    public void visit_AUTO_B(MSP430Instr i, MSP430Operand.AIREG_B source);
    public void visit_AUTOREG_B(MSP430Instr i, MSP430Operand.AIREG_B source, MSP430Operand.SREG dest);
    public void visit_AUTOIND_B(MSP430Instr i, MSP430Operand.AIREG_B source, MSP430Operand.INDX dest);
    public void visit_AUTOSYM_B(MSP430Instr i, MSP430Operand.AIREG_B source, MSP430Operand.SYMB dest);
    public void visit_AUTOABS_B(MSP430Instr i, MSP430Operand.AIREG_B source, MSP430Operand.ABSO dest);
    public void visit_AUTO_W(MSP430Instr i, MSP430Operand.AIREG_W source);
    public void visit_AUTOREG_W(MSP430Instr i, MSP430Operand.AIREG_W source, MSP430Operand.SREG dest);
    public void visit_AUTOIND_W(MSP430Instr i, MSP430Operand.AIREG_W source, MSP430Operand.INDX dest);
    public void visit_AUTOSYM_W(MSP430Instr i, MSP430Operand.AIREG_W source, MSP430Operand.SYMB dest);
    public void visit_AUTOABS_W(MSP430Instr i, MSP430Operand.AIREG_W source, MSP430Operand.ABSO dest);
    public void visit_JMP(MSP430Instr i, MSP430Operand.JUMP target);
}
