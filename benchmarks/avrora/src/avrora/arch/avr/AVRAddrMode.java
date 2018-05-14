package avrora.arch.avr;

/**
 * The <code>AVRAddrMode</code> class represents an addressing mode for this architecture. An addressing mode fixes the
 * number and type of operands, the syntax, and the encoding format of the instruction.
 */
public interface AVRAddrMode {

    public void accept(AVRInstr i, AVRAddrModeVisitor v);

    public interface XLPM extends AVRAddrMode {

        public AVROperand get_source();

        public AVROperand get_dest();
    }

    public interface LD_ST extends AVRAddrMode {

        public AVROperand get_rd();

        public AVROperand get_ar();
    }

    public static class GPRGPR implements AVRAddrMode {

        public final AVROperand.op_GPR rd;
        public final AVROperand.op_GPR rr;

        public GPRGPR(AVROperand.op_GPR rd, AVROperand.op_GPR rr) {
            this.rd = rd;
            this.rr = rr;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_GPRGPR(i, rd, rr);
        }

        public String toString() {
            return "" + ' ' + rd + ", " + rr;
        }

        public AVROperand get_rd() {
            return rd;
        }

        public AVROperand get_rr() {
            return rr;
        }
    }

    public static class MGPRMGPR implements AVRAddrMode {

        public final AVROperand.op_MGPR rd;
        public final AVROperand.op_MGPR rr;

        public MGPRMGPR(AVROperand.op_MGPR rd, AVROperand.op_MGPR rr) {
            this.rd = rd;
            this.rr = rr;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_MGPRMGPR(i, rd, rr);
        }

        public String toString() {
            return "" + ' ' + rd + ", " + rr;
        }

        public AVROperand get_rd() {
            return rd;
        }

        public AVROperand get_rr() {
            return rr;
        }
    }

    public static class GPR implements AVRAddrMode {

        public final AVROperand.op_GPR rd;

        public GPR(AVROperand.op_GPR rd) {
            this.rd = rd;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_GPR(i, rd);
        }

        public String toString() {
            return "" + ' ' + rd;
        }

        public AVROperand get_rd() {
            return rd;
        }
    }

    public static class HGPRIMM8 implements AVRAddrMode {

        public final AVROperand.op_HGPR rd;
        public final AVROperand.IMM8 imm;

        public HGPRIMM8(AVROperand.op_HGPR rd, AVROperand.IMM8 imm) {
            this.rd = rd;
            this.imm = imm;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_HGPRIMM8(i, rd, imm);
        }

        public String toString() {
            return "" + ' ' + rd + ", " + imm;
        }

        public AVROperand get_rd() {
            return rd;
        }

        public AVROperand get_imm() {
            return imm;
        }
    }

    public static class ABS implements AVRAddrMode {

        public final AVROperand.PADDR target;

        public ABS(AVROperand.PADDR target) {
            this.target = target;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_ABS(i, target);
        }

        public String toString() {
            return "" + ' ' + target;
        }

        public AVROperand get_target() {
            return target;
        }
    }

    public static class BRANCH implements AVRAddrMode {

        public final AVROperand.SREL target;

        public BRANCH(AVROperand.SREL target) {
            this.target = target;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_BRANCH(i, target);
        }

        public String toString() {
            return "" + ' ' + target;
        }

        public AVROperand get_target() {
            return target;
        }
    }

    public static class CALL implements AVRAddrMode {

        public final AVROperand.LREL target;

        public CALL(AVROperand.LREL target) {
            this.target = target;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_CALL(i, target);
        }

        public String toString() {
            return "" + ' ' + target;
        }

        public AVROperand get_target() {
            return target;
        }
    }

    public static class WRITEBIT implements AVRAddrMode {

        public WRITEBIT() {
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_WRITEBIT(i);
        }

        public String toString() {
            return "";
        }
    }

    public static class XLPM_REG implements AVRAddrMode, XLPM {

        public final AVROperand.R0_B dest;
        public final AVROperand.RZ_W source;

        public XLPM_REG(AVROperand.R0_B dest, AVROperand.RZ_W source) {
            this.dest = dest;
            this.source = source;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_XLPM_REG(i, dest, source);
        }

        public String toString() {
            return "" + " ";
        }

        public AVROperand get_dest() {
            return dest;
        }

        public AVROperand get_source() {
            return source;
        }
    }

    public static class XLPM_D implements AVRAddrMode, XLPM {

        public final AVROperand.op_GPR dest;
        public final AVROperand.RZ_W source;

        public XLPM_D(AVROperand.op_GPR dest, AVROperand.RZ_W source) {
            this.dest = dest;
            this.source = source;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_XLPM_D(i, dest, source);
        }

        public String toString() {
            return "" + " " + dest + ", " + source;
        }

        public AVROperand get_dest() {
            return dest;
        }

        public AVROperand get_source() {
            return source;
        }
    }

    public static class XLPM_INC implements AVRAddrMode, XLPM {

        public final AVROperand.op_GPR dest;
        public final AVROperand.AI_RZ_W source;

        public XLPM_INC(AVROperand.op_GPR dest, AVROperand.AI_RZ_W source) {
            this.dest = dest;
            this.source = source;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_XLPM_INC(i, dest, source);
        }

        public String toString() {
            return "" + " " + dest + ", " + source + "+";
        }

        public AVROperand get_dest() {
            return dest;
        }

        public AVROperand get_source() {
            return source;
        }
    }

    public static class LD_ST_XYZ implements AVRAddrMode, LD_ST {

        public final AVROperand.op_GPR rd;
        public final AVROperand.XYZ ar;

        public LD_ST_XYZ(AVROperand.op_GPR rd, AVROperand.XYZ ar) {
            this.rd = rd;
            this.ar = ar;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_LD_ST_XYZ(i, rd, ar);
        }

        public String toString() {
            return "" + " " + rd + ", " + ar;
        }

        public AVROperand get_rd() {
            return rd;
        }

        public AVROperand get_ar() {
            return ar;
        }
    }

    public static class LD_ST_AI_XYZ implements AVRAddrMode, LD_ST {

        public final AVROperand.op_GPR rd;
        public final AVROperand.AI_XYZ ar;

        public LD_ST_AI_XYZ(AVROperand.op_GPR rd, AVROperand.AI_XYZ ar) {
            this.rd = rd;
            this.ar = ar;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_LD_ST_AI_XYZ(i, rd, ar);
        }

        public String toString() {
            return "" + " " + rd + ", " + ar + "+";
        }

        public AVROperand get_rd() {
            return rd;
        }

        public AVROperand get_ar() {
            return ar;
        }
    }

    public static class LD_ST_PD_XYZ implements AVRAddrMode, LD_ST {

        public final AVROperand.op_GPR rd;
        public final AVROperand.PD_XYZ ar;

        public LD_ST_PD_XYZ(AVROperand.op_GPR rd, AVROperand.PD_XYZ ar) {
            this.rd = rd;
            this.ar = ar;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_LD_ST_PD_XYZ(i, rd, ar);
        }

        public String toString() {
            return "" + " " + rd + ", -" + ar;
        }

        public AVROperand get_rd() {
            return rd;
        }

        public AVROperand get_ar() {
            return ar;
        }
    }

    public static class $adiw$ implements AVRAddrMode {

        public final AVROperand.op_RDL rd;
        public final AVROperand.IMM6 imm;

        public $adiw$(AVROperand.op_RDL rd, AVROperand.IMM6 imm) {
            this.rd = rd;
            this.imm = imm;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_$adiw$(i, rd, imm);
        }

        public String toString() {
            return "" + ' ' + rd + ", " + imm;
        }

        public AVROperand get_rd() {
            return rd;
        }

        public AVROperand get_imm() {
            return imm;
        }
    }

    public static class $bclr$ implements AVRAddrMode {

        public final AVROperand.IMM3 bit;

        public $bclr$(AVROperand.IMM3 bit) {
            this.bit = bit;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_$bclr$(i, bit);
        }

        public String toString() {
            return "" + ' ' + bit;
        }

        public AVROperand get_bit() {
            return bit;
        }
    }

    public static class $bld$ implements AVRAddrMode {

        public final AVROperand.op_GPR rr;
        public final AVROperand.IMM3 bit;

        public $bld$(AVROperand.op_GPR rr, AVROperand.IMM3 bit) {
            this.rr = rr;
            this.bit = bit;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_$bld$(i, rr, bit);
        }

        public String toString() {
            return "" + ' ' + rr + ", " + bit;
        }

        public AVROperand get_rr() {
            return rr;
        }

        public AVROperand get_bit() {
            return bit;
        }
    }

    public static class $brbc$ implements AVRAddrMode {

        public final AVROperand.IMM3 bit;
        public final AVROperand.SREL target;

        public $brbc$(AVROperand.IMM3 bit, AVROperand.SREL target) {
            this.bit = bit;
            this.target = target;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_$brbc$(i, bit, target);
        }

        public String toString() {
            return "" + ' ' + bit + ", " + target;
        }

        public AVROperand get_bit() {
            return bit;
        }

        public AVROperand get_target() {
            return target;
        }
    }

    public static class $brbs$ implements AVRAddrMode {

        public final AVROperand.IMM3 bit;
        public final AVROperand.SREL target;

        public $brbs$(AVROperand.IMM3 bit, AVROperand.SREL target) {
            this.bit = bit;
            this.target = target;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_$brbs$(i, bit, target);
        }

        public String toString() {
            return "" + ' ' + bit + ", " + target;
        }

        public AVROperand get_bit() {
            return bit;
        }

        public AVROperand get_target() {
            return target;
        }
    }

    public static class $bset$ implements AVRAddrMode {

        public final AVROperand.IMM3 bit;

        public $bset$(AVROperand.IMM3 bit) {
            this.bit = bit;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_$bset$(i, bit);
        }

        public String toString() {
            return "" + ' ' + bit;
        }

        public AVROperand get_bit() {
            return bit;
        }
    }

    public static class $bst$ implements AVRAddrMode {

        public final AVROperand.op_GPR rr;
        public final AVROperand.IMM3 bit;

        public $bst$(AVROperand.op_GPR rr, AVROperand.IMM3 bit) {
            this.rr = rr;
            this.bit = bit;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_$bst$(i, rr, bit);
        }

        public String toString() {
            return "" + ' ' + rr + ", " + bit;
        }

        public AVROperand get_rr() {
            return rr;
        }

        public AVROperand get_bit() {
            return bit;
        }
    }

    public static class $call$ implements AVRAddrMode {

        public final AVROperand.PADDR target;

        public $call$(AVROperand.PADDR target) {
            this.target = target;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_$call$(i, target);
        }

        public String toString() {
            return "" + ' ' + target;
        }

        public AVROperand get_target() {
            return target;
        }
    }

    public static class $cbi$ implements AVRAddrMode {

        public final AVROperand.IMM5 ior;
        public final AVROperand.IMM3 bit;

        public $cbi$(AVROperand.IMM5 ior, AVROperand.IMM3 bit) {
            this.ior = ior;
            this.bit = bit;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_$cbi$(i, ior, bit);
        }

        public String toString() {
            return "" + ' ' + ior + ", " + bit;
        }

        public AVROperand get_ior() {
            return ior;
        }

        public AVROperand get_bit() {
            return bit;
        }
    }

    public static class $clr$ implements AVRAddrMode {

        public final AVROperand.op_GPR rd;

        public $clr$(AVROperand.op_GPR rd) {
            this.rd = rd;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_$clr$(i, rd);
        }

        public String toString() {
            return "" + ' ' + rd;
        }

        public AVROperand get_rd() {
            return rd;
        }
    }

    public static class $fmul$ implements AVRAddrMode {

        public final AVROperand.op_MGPR rd;
        public final AVROperand.op_MGPR rr;

        public $fmul$(AVROperand.op_MGPR rd, AVROperand.op_MGPR rr) {
            this.rd = rd;
            this.rr = rr;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_$fmul$(i, rd, rr);
        }

        public String toString() {
            return "" + ' ' + rd + ", " + rr;
        }

        public AVROperand get_rd() {
            return rd;
        }

        public AVROperand get_rr() {
            return rr;
        }
    }

    public static class $fmuls$ implements AVRAddrMode {

        public final AVROperand.op_MGPR rd;
        public final AVROperand.op_MGPR rr;

        public $fmuls$(AVROperand.op_MGPR rd, AVROperand.op_MGPR rr) {
            this.rd = rd;
            this.rr = rr;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_$fmuls$(i, rd, rr);
        }

        public String toString() {
            return "" + ' ' + rd + ", " + rr;
        }

        public AVROperand get_rd() {
            return rd;
        }

        public AVROperand get_rr() {
            return rr;
        }
    }

    public static class $fmulsu$ implements AVRAddrMode {

        public final AVROperand.op_MGPR rd;
        public final AVROperand.op_MGPR rr;

        public $fmulsu$(AVROperand.op_MGPR rd, AVROperand.op_MGPR rr) {
            this.rd = rd;
            this.rr = rr;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_$fmulsu$(i, rd, rr);
        }

        public String toString() {
            return "" + ' ' + rd + ", " + rr;
        }

        public AVROperand get_rd() {
            return rd;
        }

        public AVROperand get_rr() {
            return rr;
        }
    }

    public static class $in$ implements AVRAddrMode {

        public final AVROperand.op_GPR rd;
        public final AVROperand.IMM6 imm;

        public $in$(AVROperand.op_GPR rd, AVROperand.IMM6 imm) {
            this.rd = rd;
            this.imm = imm;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_$in$(i, rd, imm);
        }

        public String toString() {
            return "" + ' ' + rd + ", " + imm;
        }

        public AVROperand get_rd() {
            return rd;
        }

        public AVROperand get_imm() {
            return imm;
        }
    }

    public static class $jmp$ implements AVRAddrMode {

        public final AVROperand.PADDR target;

        public $jmp$(AVROperand.PADDR target) {
            this.target = target;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_$jmp$(i, target);
        }

        public String toString() {
            return "" + ' ' + target;
        }

        public AVROperand get_target() {
            return target;
        }
    }

    public static class $ldd$ implements AVRAddrMode {

        public final AVROperand.op_GPR rd;
        public final AVROperand.op_YZ ar;
        public final AVROperand.IMM6 imm;

        public $ldd$(AVROperand.op_GPR rd, AVROperand.op_YZ ar, AVROperand.IMM6 imm) {
            this.rd = rd;
            this.ar = ar;
            this.imm = imm;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_$ldd$(i, rd, ar, imm);
        }

        public String toString() {
            return "" + ' ' + rd + ", " + ar + ", " + imm;
        }

        public AVROperand get_rd() {
            return rd;
        }

        public AVROperand get_ar() {
            return ar;
        }

        public AVROperand get_imm() {
            return imm;
        }
    }

    public static class $lds$ implements AVRAddrMode {

        public final AVROperand.op_GPR rd;
        public final AVROperand.DADDR addr;

        public $lds$(AVROperand.op_GPR rd, AVROperand.DADDR addr) {
            this.rd = rd;
            this.addr = addr;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_$lds$(i, rd, addr);
        }

        public String toString() {
            return "" + ' ' + rd + ", " + addr;
        }

        public AVROperand get_rd() {
            return rd;
        }

        public AVROperand get_addr() {
            return addr;
        }
    }

    public static class $lsl$ implements AVRAddrMode {

        public final AVROperand.op_GPR rd;

        public $lsl$(AVROperand.op_GPR rd) {
            this.rd = rd;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_$lsl$(i, rd);
        }

        public String toString() {
            return "" + ' ' + rd;
        }

        public AVROperand get_rd() {
            return rd;
        }
    }

    public static class $movw$ implements AVRAddrMode {

        public final AVROperand.op_EGPR rd;
        public final AVROperand.op_EGPR rr;

        public $movw$(AVROperand.op_EGPR rd, AVROperand.op_EGPR rr) {
            this.rd = rd;
            this.rr = rr;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_$movw$(i, rd, rr);
        }

        public String toString() {
            return "" + ' ' + rd + ", " + rr;
        }

        public AVROperand get_rd() {
            return rd;
        }

        public AVROperand get_rr() {
            return rr;
        }
    }

    public static class $muls$ implements AVRAddrMode {

        public final AVROperand.op_HGPR rd;
        public final AVROperand.op_HGPR rr;

        public $muls$(AVROperand.op_HGPR rd, AVROperand.op_HGPR rr) {
            this.rd = rd;
            this.rr = rr;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_$muls$(i, rd, rr);
        }

        public String toString() {
            return "" + ' ' + rd + ", " + rr;
        }

        public AVROperand get_rd() {
            return rd;
        }

        public AVROperand get_rr() {
            return rr;
        }
    }

    public static class $mulsu$ implements AVRAddrMode {

        public final AVROperand.op_MGPR rd;
        public final AVROperand.op_MGPR rr;

        public $mulsu$(AVROperand.op_MGPR rd, AVROperand.op_MGPR rr) {
            this.rd = rd;
            this.rr = rr;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_$mulsu$(i, rd, rr);
        }

        public String toString() {
            return "" + ' ' + rd + ", " + rr;
        }

        public AVROperand get_rd() {
            return rd;
        }

        public AVROperand get_rr() {
            return rr;
        }
    }

    public static class $out$ implements AVRAddrMode {

        public final AVROperand.IMM6 ior;
        public final AVROperand.op_GPR rr;

        public $out$(AVROperand.IMM6 ior, AVROperand.op_GPR rr) {
            this.ior = ior;
            this.rr = rr;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_$out$(i, ior, rr);
        }

        public String toString() {
            return "" + ' ' + ior + ", " + rr;
        }

        public AVROperand get_ior() {
            return ior;
        }

        public AVROperand get_rr() {
            return rr;
        }
    }

    public static class $rcall$ implements AVRAddrMode {

        public final AVROperand.LREL target;

        public $rcall$(AVROperand.LREL target) {
            this.target = target;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_$rcall$(i, target);
        }

        public String toString() {
            return "" + ' ' + target;
        }

        public AVROperand get_target() {
            return target;
        }
    }

    public static class $rjmp$ implements AVRAddrMode {

        public final AVROperand.LREL target;

        public $rjmp$(AVROperand.LREL target) {
            this.target = target;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_$rjmp$(i, target);
        }

        public String toString() {
            return "" + ' ' + target;
        }

        public AVROperand get_target() {
            return target;
        }
    }

    public static class $rol$ implements AVRAddrMode {

        public final AVROperand.op_GPR rd;

        public $rol$(AVROperand.op_GPR rd) {
            this.rd = rd;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_$rol$(i, rd);
        }

        public String toString() {
            return "" + ' ' + rd;
        }

        public AVROperand get_rd() {
            return rd;
        }
    }

    public static class $sbi$ implements AVRAddrMode {

        public final AVROperand.IMM5 ior;
        public final AVROperand.IMM3 bit;

        public $sbi$(AVROperand.IMM5 ior, AVROperand.IMM3 bit) {
            this.ior = ior;
            this.bit = bit;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_$sbi$(i, ior, bit);
        }

        public String toString() {
            return "" + ' ' + ior + ", " + bit;
        }

        public AVROperand get_ior() {
            return ior;
        }

        public AVROperand get_bit() {
            return bit;
        }
    }

    public static class $sbic$ implements AVRAddrMode {

        public final AVROperand.IMM5 ior;
        public final AVROperand.IMM3 bit;

        public $sbic$(AVROperand.IMM5 ior, AVROperand.IMM3 bit) {
            this.ior = ior;
            this.bit = bit;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_$sbic$(i, ior, bit);
        }

        public String toString() {
            return "" + ' ' + ior + ", " + bit;
        }

        public AVROperand get_ior() {
            return ior;
        }

        public AVROperand get_bit() {
            return bit;
        }
    }

    public static class $sbis$ implements AVRAddrMode {

        public final AVROperand.IMM5 ior;
        public final AVROperand.IMM3 bit;

        public $sbis$(AVROperand.IMM5 ior, AVROperand.IMM3 bit) {
            this.ior = ior;
            this.bit = bit;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_$sbis$(i, ior, bit);
        }

        public String toString() {
            return "" + ' ' + ior + ", " + bit;
        }

        public AVROperand get_ior() {
            return ior;
        }

        public AVROperand get_bit() {
            return bit;
        }
    }

    public static class $sbiw$ implements AVRAddrMode {

        public final AVROperand.op_RDL rd;
        public final AVROperand.IMM6 imm;

        public $sbiw$(AVROperand.op_RDL rd, AVROperand.IMM6 imm) {
            this.rd = rd;
            this.imm = imm;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_$sbiw$(i, rd, imm);
        }

        public String toString() {
            return "" + ' ' + rd + ", " + imm;
        }

        public AVROperand get_rd() {
            return rd;
        }

        public AVROperand get_imm() {
            return imm;
        }
    }

    public static class $sbrc$ implements AVRAddrMode {

        public final AVROperand.op_GPR rr;
        public final AVROperand.IMM3 bit;

        public $sbrc$(AVROperand.op_GPR rr, AVROperand.IMM3 bit) {
            this.rr = rr;
            this.bit = bit;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_$sbrc$(i, rr, bit);
        }

        public String toString() {
            return "" + ' ' + rr + ", " + bit;
        }

        public AVROperand get_rr() {
            return rr;
        }

        public AVROperand get_bit() {
            return bit;
        }
    }

    public static class $sbrs$ implements AVRAddrMode {

        public final AVROperand.op_GPR rr;
        public final AVROperand.IMM3 bit;

        public $sbrs$(AVROperand.op_GPR rr, AVROperand.IMM3 bit) {
            this.rr = rr;
            this.bit = bit;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_$sbrs$(i, rr, bit);
        }

        public String toString() {
            return "" + ' ' + rr + ", " + bit;
        }

        public AVROperand get_rr() {
            return rr;
        }

        public AVROperand get_bit() {
            return bit;
        }
    }

    public static class $ser$ implements AVRAddrMode {

        public final AVROperand.op_HGPR rd;

        public $ser$(AVROperand.op_HGPR rd) {
            this.rd = rd;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_$ser$(i, rd);
        }

        public String toString() {
            return "" + ' ' + rd;
        }

        public AVROperand get_rd() {
            return rd;
        }
    }

    public static class $std$ implements AVRAddrMode {

        public final AVROperand.op_YZ ar;
        public final AVROperand.IMM6 imm;
        public final AVROperand.op_GPR rr;

        public $std$(AVROperand.op_YZ ar, AVROperand.IMM6 imm, AVROperand.op_GPR rr) {
            this.ar = ar;
            this.imm = imm;
            this.rr = rr;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_$std$(i, ar, imm, rr);
        }

        public String toString() {
            return "" + ' ' + ar + ", " + imm + ", " + rr;
        }

        public AVROperand get_ar() {
            return ar;
        }

        public AVROperand get_imm() {
            return imm;
        }

        public AVROperand get_rr() {
            return rr;
        }
    }

    public static class $sts$ implements AVRAddrMode {

        public final AVROperand.DADDR addr;
        public final AVROperand.op_GPR rr;

        public $sts$(AVROperand.DADDR addr, AVROperand.op_GPR rr) {
            this.addr = addr;
            this.rr = rr;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_$sts$(i, addr, rr);
        }

        public String toString() {
            return "" + ' ' + addr + ", " + rr;
        }

        public AVROperand get_addr() {
            return addr;
        }

        public AVROperand get_rr() {
            return rr;
        }
    }

    public static class $tst$ implements AVRAddrMode {

        public final AVROperand.op_GPR rd;

        public $tst$(AVROperand.op_GPR rd) {
            this.rd = rd;
        }

        public void accept(AVRInstr i, AVRAddrModeVisitor v) {
            v.visit_$tst$(i, rd);
        }

        public String toString() {
            return "" + ' ' + rd;
        }

        public AVROperand get_rd() {
            return rd;
        }
    }
}
