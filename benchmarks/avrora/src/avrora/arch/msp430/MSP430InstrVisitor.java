package avrora.arch.msp430;
/**
 * The <code>MSP430InstrVisitor</code> interface allows user code that
 * implements the interface to easily dispatch on the type of an
 * instruction without casting using the visitor pattern.
 */
public interface MSP430InstrVisitor {
    public void visit(MSP430Instr.ADD i);
    public void visit(MSP430Instr.ADD_B i);
    public void visit(MSP430Instr.ADDC i);
    public void visit(MSP430Instr.ADDC_B i);
    public void visit(MSP430Instr.AND i);
    public void visit(MSP430Instr.AND_B i);
    public void visit(MSP430Instr.BIC i);
    public void visit(MSP430Instr.BIC_B i);
    public void visit(MSP430Instr.BIS i);
    public void visit(MSP430Instr.BIS_B i);
    public void visit(MSP430Instr.BIT i);
    public void visit(MSP430Instr.BIT_B i);
    public void visit(MSP430Instr.CALL i);
    public void visit(MSP430Instr.CMP i);
    public void visit(MSP430Instr.CMP_B i);
    public void visit(MSP430Instr.DADD i);
    public void visit(MSP430Instr.DADD_B i);
    public void visit(MSP430Instr.JC i);
    public void visit(MSP430Instr.JHS i);
    public void visit(MSP430Instr.JEQ i);
    public void visit(MSP430Instr.JZ i);
    public void visit(MSP430Instr.JGE i);
    public void visit(MSP430Instr.JL i);
    public void visit(MSP430Instr.JMP i);
    public void visit(MSP430Instr.JN i);
    public void visit(MSP430Instr.JNC i);
    public void visit(MSP430Instr.JLO i);
    public void visit(MSP430Instr.JNE i);
    public void visit(MSP430Instr.JNZ i);
    public void visit(MSP430Instr.MOV i);
    public void visit(MSP430Instr.MOV_B i);
    public void visit(MSP430Instr.PUSH i);
    public void visit(MSP430Instr.PUSH_B i);
    public void visit(MSP430Instr.RETI i);
    public void visit(MSP430Instr.RRA i);
    public void visit(MSP430Instr.RRA_B i);
    public void visit(MSP430Instr.RRC i);
    public void visit(MSP430Instr.RRC_B i);
    public void visit(MSP430Instr.SUB i);
    public void visit(MSP430Instr.SUB_B i);
    public void visit(MSP430Instr.SUBC i);
    public void visit(MSP430Instr.SUBC_B i);
    public void visit(MSP430Instr.SWPB i);
    public void visit(MSP430Instr.SXT i);
    public void visit(MSP430Instr.TST i);
    public void visit(MSP430Instr.TST_B i);
    public void visit(MSP430Instr.XOR i);
    public void visit(MSP430Instr.XOR_B i);
}
