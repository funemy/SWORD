package avrora.arch.avr;

/**
 * The <code>AVROperandVisitor</code> interface allows clients to use the Visitor pattern to resolve the types of
 * operands to instructions.
 */
public interface AVROperandVisitor {

    public void visit(AVROperand.op_GPR o);

    public void visit(AVROperand.op_HGPR o);

    public void visit(AVROperand.op_MGPR o);

    public void visit(AVROperand.op_YZ o);

    public void visit(AVROperand.op_EGPR o);

    public void visit(AVROperand.op_RDL o);

    public void visit(AVROperand.IMM3 o);

    public void visit(AVROperand.IMM5 o);

    public void visit(AVROperand.IMM6 o);

    public void visit(AVROperand.IMM7 o);

    public void visit(AVROperand.IMM8 o);

    public void visit(AVROperand.SREL o);

    public void visit(AVROperand.LREL o);

    public void visit(AVROperand.PADDR o);

    public void visit(AVROperand.DADDR o);

    public void visit(AVROperand.R0_B o);

    public void visit(AVROperand.RZ_W o);

    public void visit(AVROperand.AI_RZ_W o);

    public void visit(AVROperand.XYZ o);

    public void visit(AVROperand.AI_XYZ o);

    public void visit(AVROperand.PD_XYZ o);
}
