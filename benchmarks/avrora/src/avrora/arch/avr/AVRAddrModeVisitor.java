package avrora.arch.avr;

/**
 * The <code>AVRAddrModeVisitor</code> interface implements the visitor pattern for addressing modes.
 */
public interface AVRAddrModeVisitor {

    public void visit_GPRGPR(AVRInstr i, AVROperand.op_GPR rd, AVROperand.op_GPR rr);

    public void visit_MGPRMGPR(AVRInstr i, AVROperand.op_MGPR rd, AVROperand.op_MGPR rr);

    public void visit_GPR(AVRInstr i, AVROperand.op_GPR rd);

    public void visit_HGPRIMM8(AVRInstr i, AVROperand.op_HGPR rd, AVROperand.IMM8 imm);

    public void visit_ABS(AVRInstr i, AVROperand.PADDR target);

    public void visit_BRANCH(AVRInstr i, AVROperand.SREL target);

    public void visit_CALL(AVRInstr i, AVROperand.LREL target);

    public void visit_WRITEBIT(AVRInstr i);

    public void visit_XLPM_REG(AVRInstr i, AVROperand.R0_B dest, AVROperand.RZ_W source);

    public void visit_XLPM_D(AVRInstr i, AVROperand.op_GPR dest, AVROperand.RZ_W source);

    public void visit_XLPM_INC(AVRInstr i, AVROperand.op_GPR dest, AVROperand.AI_RZ_W source);

    public void visit_LD_ST_XYZ(AVRInstr i, AVROperand.op_GPR rd, AVROperand.XYZ ar);

    public void visit_LD_ST_AI_XYZ(AVRInstr i, AVROperand.op_GPR rd, AVROperand.AI_XYZ ar);

    public void visit_LD_ST_PD_XYZ(AVRInstr i, AVROperand.op_GPR rd, AVROperand.PD_XYZ ar);

    public void visit_$adiw$(AVRInstr i, AVROperand.op_RDL rd, AVROperand.IMM6 imm);

    public void visit_$bclr$(AVRInstr i, AVROperand.IMM3 bit);

    public void visit_$bld$(AVRInstr i, AVROperand.op_GPR rr, AVROperand.IMM3 bit);

    public void visit_$brbc$(AVRInstr i, AVROperand.IMM3 bit, AVROperand.SREL target);

    public void visit_$brbs$(AVRInstr i, AVROperand.IMM3 bit, AVROperand.SREL target);

    public void visit_$bset$(AVRInstr i, AVROperand.IMM3 bit);

    public void visit_$bst$(AVRInstr i, AVROperand.op_GPR rr, AVROperand.IMM3 bit);

    public void visit_$call$(AVRInstr i, AVROperand.PADDR target);

    public void visit_$cbi$(AVRInstr i, AVROperand.IMM5 ior, AVROperand.IMM3 bit);

    public void visit_$clr$(AVRInstr i, AVROperand.op_GPR rd);

    public void visit_$fmul$(AVRInstr i, AVROperand.op_MGPR rd, AVROperand.op_MGPR rr);

    public void visit_$fmuls$(AVRInstr i, AVROperand.op_MGPR rd, AVROperand.op_MGPR rr);

    public void visit_$fmulsu$(AVRInstr i, AVROperand.op_MGPR rd, AVROperand.op_MGPR rr);

    public void visit_$in$(AVRInstr i, AVROperand.op_GPR rd, AVROperand.IMM6 imm);

    public void visit_$jmp$(AVRInstr i, AVROperand.PADDR target);

    public void visit_$ldd$(AVRInstr i, AVROperand.op_GPR rd, AVROperand.op_YZ ar, AVROperand.IMM6 imm);

    public void visit_$lds$(AVRInstr i, AVROperand.op_GPR rd, AVROperand.DADDR addr);

    public void visit_$lsl$(AVRInstr i, AVROperand.op_GPR rd);

    public void visit_$movw$(AVRInstr i, AVROperand.op_EGPR rd, AVROperand.op_EGPR rr);

    public void visit_$muls$(AVRInstr i, AVROperand.op_HGPR rd, AVROperand.op_HGPR rr);

    public void visit_$mulsu$(AVRInstr i, AVROperand.op_MGPR rd, AVROperand.op_MGPR rr);

    public void visit_$out$(AVRInstr i, AVROperand.IMM6 ior, AVROperand.op_GPR rr);

    public void visit_$rcall$(AVRInstr i, AVROperand.LREL target);

    public void visit_$rjmp$(AVRInstr i, AVROperand.LREL target);

    public void visit_$rol$(AVRInstr i, AVROperand.op_GPR rd);

    public void visit_$sbi$(AVRInstr i, AVROperand.IMM5 ior, AVROperand.IMM3 bit);

    public void visit_$sbic$(AVRInstr i, AVROperand.IMM5 ior, AVROperand.IMM3 bit);

    public void visit_$sbis$(AVRInstr i, AVROperand.IMM5 ior, AVROperand.IMM3 bit);

    public void visit_$sbiw$(AVRInstr i, AVROperand.op_RDL rd, AVROperand.IMM6 imm);

    public void visit_$sbrc$(AVRInstr i, AVROperand.op_GPR rr, AVROperand.IMM3 bit);

    public void visit_$sbrs$(AVRInstr i, AVROperand.op_GPR rr, AVROperand.IMM3 bit);

    public void visit_$ser$(AVRInstr i, AVROperand.op_HGPR rd);

    public void visit_$std$(AVRInstr i, AVROperand.op_YZ ar, AVROperand.IMM6 imm, AVROperand.op_GPR rr);

    public void visit_$sts$(AVRInstr i, AVROperand.DADDR addr, AVROperand.op_GPR rr);

    public void visit_$tst$(AVRInstr i, AVROperand.op_GPR rd);
}
