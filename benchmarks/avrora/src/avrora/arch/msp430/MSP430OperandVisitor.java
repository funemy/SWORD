package avrora.arch.msp430;
/**
 * The <code>MSP430OperandVisitor</code> interface allows clients to use
 * the Visitor pattern to resolve the types of operands to instructions.
 */
public interface MSP430OperandVisitor {
    public void visit(MSP430Operand.SREG o);
    public void visit(MSP430Operand.AIREG_B o);
    public void visit(MSP430Operand.AIREG_W o);
    public void visit(MSP430Operand.IREG o);
    public void visit(MSP430Operand.IMM o);
    public void visit(MSP430Operand.IMML o);
    public void visit(MSP430Operand.INDX o);
    public void visit(MSP430Operand.SYMB o);
    public void visit(MSP430Operand.ABSO o);
    public void visit(MSP430Operand.JUMP o);
}
