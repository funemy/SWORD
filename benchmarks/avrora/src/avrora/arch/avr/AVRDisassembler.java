package avrora.arch.avr;

import avrora.arch.AbstractDisassembler;
import avrora.arch.AbstractInstr;
import java.util.Arrays;

/**
 * The <code>AVRDisassembler</code> class decodes bit patterns into instructions. It has been generated automatically by
 * jIntGen from a file containing a description of the instruction set and their encodings.
 * <p/>
 * The following options have been specified to tune this implementation:
 * <p/>
 * </p>-word-size=16 </p>-parallel-trees=false </p>-multiple-trees=true </p>-chained-trees=true
 */
public class AVRDisassembler implements AbstractDisassembler {

    public static class InvalidInstruction extends Exception {

        InvalidInstruction(int pc) {
            super("Invalid instruction at " + pc);
        }
    }

    static final AVRSymbol.GPR[] GPR_table = { AVRSymbol.GPR.R0, // 0 (0b00000) -> r0
            AVRSymbol.GPR.R1, // 1 (0b00001) -> r1
            AVRSymbol.GPR.R2, // 2 (0b00010) -> r2
            AVRSymbol.GPR.R3, // 3 (0b00011) -> r3
            AVRSymbol.GPR.R4, // 4 (0b00100) -> r4
            AVRSymbol.GPR.R5, // 5 (0b00101) -> r5
            AVRSymbol.GPR.R6, // 6 (0b00110) -> r6
            AVRSymbol.GPR.R7, // 7 (0b00111) -> r7
            AVRSymbol.GPR.R8, // 8 (0b01000) -> r8
            AVRSymbol.GPR.R9, // 9 (0b01001) -> r9
            AVRSymbol.GPR.R10, // 10 (0b01010) -> r10
            AVRSymbol.GPR.R11, // 11 (0b01011) -> r11
            AVRSymbol.GPR.R12, // 12 (0b01100) -> r12
            AVRSymbol.GPR.R13, // 13 (0b01101) -> r13
            AVRSymbol.GPR.R14, // 14 (0b01110) -> r14
            AVRSymbol.GPR.R15, // 15 (0b01111) -> r15
            AVRSymbol.GPR.R16, // 16 (0b10000) -> r16
            AVRSymbol.GPR.R17, // 17 (0b10001) -> r17
            AVRSymbol.GPR.R18, // 18 (0b10010) -> r18
            AVRSymbol.GPR.R19, // 19 (0b10011) -> r19
            AVRSymbol.GPR.R20, // 20 (0b10100) -> r20
            AVRSymbol.GPR.R21, // 21 (0b10101) -> r21
            AVRSymbol.GPR.R22, // 22 (0b10110) -> r22
            AVRSymbol.GPR.R23, // 23 (0b10111) -> r23
            AVRSymbol.GPR.R24, // 24 (0b11000) -> r24
            AVRSymbol.GPR.R25, // 25 (0b11001) -> r25
            AVRSymbol.GPR.R26, // 26 (0b11010) -> r26
            AVRSymbol.GPR.R27, // 27 (0b11011) -> r27
            AVRSymbol.GPR.R28, // 28 (0b11100) -> r28
            AVRSymbol.GPR.R29, // 29 (0b11101) -> r29
            AVRSymbol.GPR.R30, // 30 (0b11110) -> r30
            AVRSymbol.GPR.R31 // 31 (0b11111) -> r31
    };
    static final AVRSymbol.ADR[] ADR_table = { null, // 0 (0b00000) -> null
            null, // 1 (0b00001) -> null
            null, // 2 (0b00010) -> null
            null, // 3 (0b00011) -> null
            null, // 4 (0b00100) -> null
            null, // 5 (0b00101) -> null
            null, // 6 (0b00110) -> null
            null, // 7 (0b00111) -> null
            null, // 8 (0b01000) -> null
            null, // 9 (0b01001) -> null
            null, // 10 (0b01010) -> null
            null, // 11 (0b01011) -> null
            null, // 12 (0b01100) -> null
            null, // 13 (0b01101) -> null
            null, // 14 (0b01110) -> null
            null, // 15 (0b01111) -> null
            null, // 16 (0b10000) -> null
            null, // 17 (0b10001) -> null
            null, // 18 (0b10010) -> null
            null, // 19 (0b10011) -> null
            null, // 20 (0b10100) -> null
            null, // 21 (0b10101) -> null
            null, // 22 (0b10110) -> null
            null, // 23 (0b10111) -> null
            null, // 24 (0b11000) -> null
            null, // 25 (0b11001) -> null
            AVRSymbol.ADR.X, // 26 (0b11010) -> X
            null, // 27 (0b11011) -> null
            AVRSymbol.ADR.Y, // 28 (0b11100) -> Y
            null, // 29 (0b11101) -> null
            AVRSymbol.ADR.Z // 30 (0b11110) -> Z
    };
    static final AVRSymbol.HGPR[] HGPR_table = { AVRSymbol.HGPR.R16, // 0 (0b0000) -> r16
            AVRSymbol.HGPR.R17, // 1 (0b0001) -> r17
            AVRSymbol.HGPR.R18, // 2 (0b0010) -> r18
            AVRSymbol.HGPR.R19, // 3 (0b0011) -> r19
            AVRSymbol.HGPR.R20, // 4 (0b0100) -> r20
            AVRSymbol.HGPR.R21, // 5 (0b0101) -> r21
            AVRSymbol.HGPR.R22, // 6 (0b0110) -> r22
            AVRSymbol.HGPR.R23, // 7 (0b0111) -> r23
            AVRSymbol.HGPR.R24, // 8 (0b1000) -> r24
            AVRSymbol.HGPR.R25, // 9 (0b1001) -> r25
            AVRSymbol.HGPR.R26, // 10 (0b1010) -> r26
            AVRSymbol.HGPR.R27, // 11 (0b1011) -> r27
            AVRSymbol.HGPR.R28, // 12 (0b1100) -> r28
            AVRSymbol.HGPR.R29, // 13 (0b1101) -> r29
            AVRSymbol.HGPR.R30, // 14 (0b1110) -> r30
            AVRSymbol.HGPR.R31 // 15 (0b1111) -> r31
    };
    static final AVRSymbol.EGPR[] EGPR_table = { AVRSymbol.EGPR.R0, // 0 (0b0000) -> r0
            AVRSymbol.EGPR.R2, // 1 (0b0001) -> r2
            AVRSymbol.EGPR.R4, // 2 (0b0010) -> r4
            AVRSymbol.EGPR.R6, // 3 (0b0011) -> r6
            AVRSymbol.EGPR.R8, // 4 (0b0100) -> r8
            AVRSymbol.EGPR.R10, // 5 (0b0101) -> r10
            AVRSymbol.EGPR.R12, // 6 (0b0110) -> r12
            AVRSymbol.EGPR.R14, // 7 (0b0111) -> r14
            AVRSymbol.EGPR.R16, // 8 (0b1000) -> r16
            AVRSymbol.EGPR.R18, // 9 (0b1001) -> r18
            AVRSymbol.EGPR.R20, // 10 (0b1010) -> r20
            AVRSymbol.EGPR.R22, // 11 (0b1011) -> r22
            AVRSymbol.EGPR.R24, // 12 (0b1100) -> r24
            AVRSymbol.EGPR.R26, // 13 (0b1101) -> r26
            AVRSymbol.EGPR.R28, // 14 (0b1110) -> r28
            AVRSymbol.EGPR.R30 // 15 (0b1111) -> r30
    };
    static final AVRSymbol.MGPR[] MGPR_table = { AVRSymbol.MGPR.R16, // 0 (0b000) -> r16
            AVRSymbol.MGPR.R17, // 1 (0b001) -> r17
            AVRSymbol.MGPR.R18, // 2 (0b010) -> r18
            AVRSymbol.MGPR.R19, // 3 (0b011) -> r19
            AVRSymbol.MGPR.R20, // 4 (0b100) -> r20
            AVRSymbol.MGPR.R21, // 5 (0b101) -> r21
            AVRSymbol.MGPR.R22, // 6 (0b110) -> r22
            AVRSymbol.MGPR.R23 // 7 (0b111) -> r23
    };
    static final AVRSymbol.YZ[] YZ_table = { AVRSymbol.YZ.Z, // 0 (0b0) -> Z
            AVRSymbol.YZ.Y // 1 (0b1) -> Y
    };
    static final AVRSymbol.RDL[] RDL_table = { AVRSymbol.RDL.R24, // 0 (0b00) -> r24
            AVRSymbol.RDL.R26, // 1 (0b01) -> r26
            AVRSymbol.RDL.R28, // 2 (0b10) -> r28
            AVRSymbol.RDL.R30 // 3 (0b11) -> r30
    };
    static final AVRSymbol.R0[] R0_table = { AVRSymbol.R0.R0 // 0 (0b0) -> r0
    };
    static final AVRSymbol.RZ[] RZ_table = { AVRSymbol.RZ.Z // 0 (0b0) -> Z
    };

    static int readop_14(AVRDisassembler d) {
        int result = ((d.word0 >>> 4) & 0x0003);
        return result;
    }

    static int readop_11(AVRDisassembler d) {
        int result = ((d.word0 >>> 4) & 0x000F);
        return result;
    }

    static int readop_17(AVRDisassembler d) {
        int result = (d.word1 & 0xFFFF);
        return result;
    }

    static int readop_13(AVRDisassembler d) {
        int result = (d.word0 & 0x000F);
        result |= ((d.word0 >>> 9) & 0x0001) << 4;
        return result;
    }

    static int readop_12(AVRDisassembler d) {
        int result = (d.word0 & 0x000F);
        return result;
    }

    static int readop_10(AVRDisassembler d) {
        int result = ((d.word0 >>> 3) & 0x007F);
        return result;
    }

    static int readop_3(AVRDisassembler d) {
        int result = (d.word0 & 0x0007);
        result |= ((d.word0 >>> 10) & 0x0003) << 3;
        result |= ((d.word0 >>> 13) & 0x0001) << 5;
        return result;
    }

    static int readop_18(AVRDisassembler d) {
        int result = (d.word0 & 0x000F);
        result |= ((d.word0 >>> 8) & 0x000F) << 4;
        return result;
    }

    static int readop_7(AVRDisassembler d) {
        int result = ((d.word0 >>> 4) & 0x000F);
        result |= ((d.word0 >>> 8) & 0x0001) << 4;
        return result;
    }

    static int readop_16(AVRDisassembler d) {
        int result = (d.word0 & 0x000F);
        result |= ((d.word0 >>> 9) & 0x0003) << 4;
        return result;
    }

    static int readop_2(AVRDisassembler d) {
        int result = ((d.word0 >>> 3) & 0x0001);
        return result;
    }

    static int readop_6(AVRDisassembler d) {
        int result = ((d.word0 >>> 4) & 0x0007);
        return result;
    }

    static int readop_5(AVRDisassembler d) {
        int result = (d.word0 & 0x0007);
        return result;
    }

    static int readop_0(AVRDisassembler d) {
        int result = ((d.word0 >>> 4) & 0x001F);
        return result;
    }

    static int readop_15(AVRDisassembler d) {
        int result = (d.word0 & 0x000F);
        result |= ((d.word0 >>> 6) & 0x0003) << 4;
        return result;
    }

    static int readop_4(AVRDisassembler d) {
        int result = (d.word0 & 0x0001);
        result |= ((d.word1 >>> -15) & 0x7FFF) << 1;
        return result;
    }

    static int readop_8(AVRDisassembler d) {
        int result = ((d.word0 >>> 3) & 0x001F);
        return result;
    }

    static int readop_1(AVRDisassembler d) {
        int result = ((d.word0 >>> 1) & 0x07FF);
        return result;
    }

    static int readop_9(AVRDisassembler d) {
        return 0;
    }

    /**
     * The <code>NULL_reader</code> class is used for instructions that define their own addressing mode and have no
     * operands. This reader sets the size of the instruction to the appropriate size for the encoding and the
     * addressing mode to <code>null</code>.
     */
    public static class NULL_reader extends OperandReader {

        final int size;

        NULL_reader(int sz) {
            this.size = sz;
        }

        AVRAddrMode read(AVRDisassembler d) {
            d.size = size;
            return null;
        }
    }

    private static int signExtend(int val, int size) {
        // shift all the way to the left and then back (arithmetically)
        int shift = 32 - size;
        return (val << shift) >> shift;
    }

    static class LD_ST_PD_XYZ_0_reader extends OperandReader {

        AVRAddrMode read(AVRDisassembler d) {
            d.size = 2;
            AVROperand.op_GPR rd = new AVROperand.op_GPR(GPR_table[readop_0(d)]);
            AVROperand.PD_XYZ ar = new AVROperand.PD_XYZ(AVRSymbol.ADR.X);
            return new AVRAddrMode.LD_ST_PD_XYZ(rd, ar);
        }
    }

    static class $rjmp$_0_reader extends OperandReader {

        AVRAddrMode read(AVRDisassembler d) {
            d.size = 2;
            AVROperand.LREL target = new AVROperand.LREL(signExtend(readop_1(d), 11));
            return new AVRAddrMode.$rjmp$(target);
        }
    }

    static class $ldd$_0_reader extends OperandReader {

        AVRAddrMode read(AVRDisassembler d) {
            d.size = 2;
            AVROperand.op_GPR rd = new AVROperand.op_GPR(GPR_table[readop_0(d)]);
            AVROperand.op_YZ ar = new AVROperand.op_YZ(YZ_table[readop_2(d)]);
            AVROperand.IMM6 imm = new AVROperand.IMM6(readop_3(d));
            return new AVRAddrMode.$ldd$(rd, ar, imm);
        }
    }

    static class LD_ST_PD_XYZ_1_reader extends OperandReader {

        AVRAddrMode read(AVRDisassembler d) {
            d.size = 2;
            AVROperand.op_GPR rd = new AVROperand.op_GPR(GPR_table[readop_0(d)]);
            AVROperand.PD_XYZ ar = new AVROperand.PD_XYZ(AVRSymbol.ADR.Y);
            return new AVRAddrMode.LD_ST_PD_XYZ(rd, ar);
        }
    }

    static class $jmp$_0_reader extends OperandReader {

        AVRAddrMode read(AVRDisassembler d) {
            d.size = 4;
            AVROperand.PADDR target = new AVROperand.PADDR(readop_4(d));
            return new AVRAddrMode.$jmp$(target);
        }
    }

    static class $bld$_0_reader extends OperandReader {

        AVRAddrMode read(AVRDisassembler d) {
            d.size = 2;
            AVROperand.op_GPR rr = new AVROperand.op_GPR(GPR_table[readop_0(d)]);
            AVROperand.IMM3 bit = new AVROperand.IMM3(readop_5(d));
            return new AVRAddrMode.$bld$(rr, bit);
        }
    }

    static class $rcall$_0_reader extends OperandReader {

        AVRAddrMode read(AVRDisassembler d) {
            d.size = 2;
            AVROperand.LREL target = new AVROperand.LREL(signExtend(readop_1(d), 11));
            return new AVRAddrMode.$rcall$(target);
        }
    }

    static class $bst$_0_reader extends OperandReader {

        AVRAddrMode read(AVRDisassembler d) {
            d.size = 2;
            AVROperand.op_GPR rr = new AVROperand.op_GPR(GPR_table[readop_0(d)]);
            AVROperand.IMM3 bit = new AVROperand.IMM3(readop_5(d));
            return new AVRAddrMode.$bst$(rr, bit);
        }
    }

    static class $fmulsu$_0_reader extends OperandReader {

        AVRAddrMode read(AVRDisassembler d) {
            d.size = 2;
            AVROperand.op_MGPR rd = new AVROperand.op_MGPR(MGPR_table[readop_6(d)]);
            AVROperand.op_MGPR rr = new AVROperand.op_MGPR(MGPR_table[readop_5(d)]);
            return new AVRAddrMode.$fmulsu$(rd, rr);
        }
    }

    static class GPR_0_reader extends OperandReader {

        AVRAddrMode read(AVRDisassembler d) {
            d.size = 2;
            AVROperand.op_GPR rd = new AVROperand.op_GPR(GPR_table[readop_7(d)]);
            return new AVRAddrMode.GPR(rd);
        }
    }

    static class $call$_0_reader extends OperandReader {

        AVRAddrMode read(AVRDisassembler d) {
            d.size = 4;
            AVROperand.PADDR target = new AVROperand.PADDR(readop_4(d));
            return new AVRAddrMode.$call$(target);
        }
    }

    static class LD_ST_AI_XYZ_0_reader extends OperandReader {

        AVRAddrMode read(AVRDisassembler d) {
            d.size = 2;
            AVROperand.op_GPR rd = new AVROperand.op_GPR(GPR_table[readop_0(d)]);
            AVROperand.AI_XYZ ar = new AVROperand.AI_XYZ(AVRSymbol.ADR.X);
            return new AVRAddrMode.LD_ST_AI_XYZ(rd, ar);
        }
    }

    static class $cbi$_0_reader extends OperandReader {

        AVRAddrMode read(AVRDisassembler d) {
            d.size = 2;
            AVROperand.IMM5 ior = new AVROperand.IMM5(readop_8(d));
            AVROperand.IMM3 bit = new AVROperand.IMM3(readop_5(d));
            return new AVRAddrMode.$cbi$(ior, bit);
        }
    }

    static class XLPM_REG_0_reader extends OperandReader {

        AVRAddrMode read(AVRDisassembler d) {
            d.size = 2;
            AVROperand.R0_B dest = new AVROperand.R0_B(R0_table[readop_9(d)]);
            AVROperand.RZ_W source = new AVROperand.RZ_W(RZ_table[readop_9(d)]);
            return new AVRAddrMode.XLPM_REG(dest, source);
        }
    }

    static class BRANCH_0_reader extends OperandReader {

        AVRAddrMode read(AVRDisassembler d) {
            d.size = 2;
            AVROperand.SREL target = new AVROperand.SREL(signExtend(readop_10(d), 7));
            return new AVRAddrMode.BRANCH(target);
        }
    }

    static class LD_ST_XYZ_0_reader extends OperandReader {

        AVRAddrMode read(AVRDisassembler d) {
            d.size = 2;
            AVROperand.op_GPR rd = new AVROperand.op_GPR(GPR_table[readop_0(d)]);
            AVROperand.XYZ ar = new AVROperand.XYZ(AVRSymbol.ADR.X);
            return new AVRAddrMode.LD_ST_XYZ(rd, ar);
        }
    }

    static class LD_ST_PD_XYZ_2_reader extends OperandReader {

        AVRAddrMode read(AVRDisassembler d) {
            d.size = 2;
            AVROperand.op_GPR rd = new AVROperand.op_GPR(GPR_table[readop_0(d)]);
            AVROperand.PD_XYZ ar = new AVROperand.PD_XYZ(AVRSymbol.ADR.Z);
            return new AVRAddrMode.LD_ST_PD_XYZ(rd, ar);
        }
    }

    static class $muls$_0_reader extends OperandReader {

        AVRAddrMode read(AVRDisassembler d) {
            d.size = 2;
            AVROperand.op_HGPR rd = new AVROperand.op_HGPR(HGPR_table[readop_11(d)]);
            AVROperand.op_HGPR rr = new AVROperand.op_HGPR(HGPR_table[readop_12(d)]);
            return new AVRAddrMode.$muls$(rd, rr);
        }
    }

    static class LD_ST_XYZ_2_reader extends OperandReader {

        AVRAddrMode read(AVRDisassembler d) {
            d.size = 2;
            AVROperand.op_GPR rd = new AVROperand.op_GPR(GPR_table[readop_0(d)]);
            AVROperand.XYZ ar = new AVROperand.XYZ(AVRSymbol.ADR.Z);
            return new AVRAddrMode.LD_ST_XYZ(rd, ar);
        }
    }

    static class $movw$_0_reader extends OperandReader {

        AVRAddrMode read(AVRDisassembler d) {
            d.size = 2;
            AVROperand.op_EGPR rd = new AVROperand.op_EGPR(EGPR_table[readop_11(d)]);
            AVROperand.op_EGPR rr = new AVROperand.op_EGPR(EGPR_table[readop_12(d)]);
            return new AVRAddrMode.$movw$(rd, rr);
        }
    }

    static class $sbrs$_0_reader extends OperandReader {

        AVRAddrMode read(AVRDisassembler d) {
            d.size = 2;
            AVROperand.op_GPR rr = new AVROperand.op_GPR(GPR_table[readop_0(d)]);
            AVROperand.IMM3 bit = new AVROperand.IMM3(readop_5(d));
            return new AVRAddrMode.$sbrs$(rr, bit);
        }
    }

    static class $sbic$_0_reader extends OperandReader {

        AVRAddrMode read(AVRDisassembler d) {
            d.size = 2;
            AVROperand.IMM5 ior = new AVROperand.IMM5(readop_8(d));
            AVROperand.IMM3 bit = new AVROperand.IMM3(readop_5(d));
            return new AVRAddrMode.$sbic$(ior, bit);
        }
    }

    static class $fmul$_0_reader extends OperandReader {

        AVRAddrMode read(AVRDisassembler d) {
            d.size = 2;
            AVROperand.op_MGPR rd = new AVROperand.op_MGPR(MGPR_table[readop_6(d)]);
            AVROperand.op_MGPR rr = new AVROperand.op_MGPR(MGPR_table[readop_5(d)]);
            return new AVRAddrMode.$fmul$(rd, rr);
        }
    }

    static class $std$_0_reader extends OperandReader {

        AVRAddrMode read(AVRDisassembler d) {
            d.size = 2;
            AVROperand.op_YZ ar = new AVROperand.op_YZ(YZ_table[readop_2(d)]);
            AVROperand.IMM6 imm = new AVROperand.IMM6(readop_3(d));
            AVROperand.op_GPR rr = new AVROperand.op_GPR(GPR_table[readop_0(d)]);
            return new AVRAddrMode.$std$(ar, imm, rr);
        }
    }

    static class $sbi$_0_reader extends OperandReader {

        AVRAddrMode read(AVRDisassembler d) {
            d.size = 2;
            AVROperand.IMM5 ior = new AVROperand.IMM5(readop_8(d));
            AVROperand.IMM3 bit = new AVROperand.IMM3(readop_5(d));
            return new AVRAddrMode.$sbi$(ior, bit);
        }
    }

    static class XLPM_INC_0_reader extends OperandReader {

        AVRAddrMode read(AVRDisassembler d) {
            d.size = 2;
            AVROperand.op_GPR dest = new AVROperand.op_GPR(GPR_table[readop_0(d)]);
            AVROperand.AI_RZ_W source = new AVROperand.AI_RZ_W(RZ_table[readop_9(d)]);
            return new AVRAddrMode.XLPM_INC(dest, source);
        }
    }

    static class LD_ST_XYZ_1_reader extends OperandReader {

        AVRAddrMode read(AVRDisassembler d) {
            d.size = 2;
            AVROperand.op_GPR rd = new AVROperand.op_GPR(GPR_table[readop_0(d)]);
            AVROperand.XYZ ar = new AVROperand.XYZ(AVRSymbol.ADR.Y);
            return new AVRAddrMode.LD_ST_XYZ(rd, ar);
        }
    }

    static class $sbrc$_0_reader extends OperandReader {

        AVRAddrMode read(AVRDisassembler d) {
            d.size = 2;
            AVROperand.op_GPR rr = new AVROperand.op_GPR(GPR_table[readop_0(d)]);
            AVROperand.IMM3 bit = new AVROperand.IMM3(readop_5(d));
            return new AVRAddrMode.$sbrc$(rr, bit);
        }
    }

    static class $sbis$_0_reader extends OperandReader {

        AVRAddrMode read(AVRDisassembler d) {
            d.size = 2;
            AVROperand.IMM5 ior = new AVROperand.IMM5(readop_8(d));
            AVROperand.IMM3 bit = new AVROperand.IMM3(readop_5(d));
            return new AVRAddrMode.$sbis$(ior, bit);
        }
    }

    static class XLPM_D_0_reader extends OperandReader {

        AVRAddrMode read(AVRDisassembler d) {
            d.size = 2;
            AVROperand.op_GPR dest = new AVROperand.op_GPR(GPR_table[readop_0(d)]);
            AVROperand.RZ_W source = new AVROperand.RZ_W(RZ_table[readop_9(d)]);
            return new AVRAddrMode.XLPM_D(dest, source);
        }
    }

    static class LD_ST_AI_XYZ_1_reader extends OperandReader {

        AVRAddrMode read(AVRDisassembler d) {
            d.size = 2;
            AVROperand.op_GPR rd = new AVROperand.op_GPR(GPR_table[readop_0(d)]);
            AVROperand.AI_XYZ ar = new AVROperand.AI_XYZ(AVRSymbol.ADR.Y);
            return new AVRAddrMode.LD_ST_AI_XYZ(rd, ar);
        }
    }

    static class GPRGPR_0_reader extends OperandReader {

        AVRAddrMode read(AVRDisassembler d) {
            d.size = 2;
            AVROperand.op_GPR rd = new AVROperand.op_GPR(GPR_table[readop_7(d)]);
            AVROperand.op_GPR rr = new AVROperand.op_GPR(GPR_table[readop_13(d)]);
            return new AVRAddrMode.GPRGPR(rd, rr);
        }
    }

    static class $adiw$_0_reader extends OperandReader {

        AVRAddrMode read(AVRDisassembler d) {
            d.size = 2;
            AVROperand.op_RDL rd = new AVROperand.op_RDL(RDL_table[readop_14(d)]);
            AVROperand.IMM6 imm = new AVROperand.IMM6(readop_15(d));
            return new AVRAddrMode.$adiw$(rd, imm);
        }
    }

    static class $mulsu$_0_reader extends OperandReader {

        AVRAddrMode read(AVRDisassembler d) {
            d.size = 2;
            AVROperand.op_MGPR rd = new AVROperand.op_MGPR(MGPR_table[readop_6(d)]);
            AVROperand.op_MGPR rr = new AVROperand.op_MGPR(MGPR_table[readop_5(d)]);
            return new AVRAddrMode.$mulsu$(rd, rr);
        }
    }

    static class $out$_0_reader extends OperandReader {

        AVRAddrMode read(AVRDisassembler d) {
            d.size = 2;
            AVROperand.IMM6 ior = new AVROperand.IMM6(readop_16(d));
            AVROperand.op_GPR rr = new AVROperand.op_GPR(GPR_table[readop_0(d)]);
            return new AVRAddrMode.$out$(ior, rr);
        }
    }

    static class $fmuls$_0_reader extends OperandReader {

        AVRAddrMode read(AVRDisassembler d) {
            d.size = 2;
            AVROperand.op_MGPR rd = new AVROperand.op_MGPR(MGPR_table[readop_6(d)]);
            AVROperand.op_MGPR rr = new AVROperand.op_MGPR(MGPR_table[readop_5(d)]);
            return new AVRAddrMode.$fmuls$(rd, rr);
        }
    }

    static class $in$_0_reader extends OperandReader {

        AVRAddrMode read(AVRDisassembler d) {
            d.size = 2;
            AVROperand.op_GPR rd = new AVROperand.op_GPR(GPR_table[readop_0(d)]);
            AVROperand.IMM6 imm = new AVROperand.IMM6(readop_16(d));
            return new AVRAddrMode.$in$(rd, imm);
        }
    }

    static class LD_ST_AI_XYZ_2_reader extends OperandReader {

        AVRAddrMode read(AVRDisassembler d) {
            d.size = 2;
            AVROperand.op_GPR rd = new AVROperand.op_GPR(GPR_table[readop_0(d)]);
            AVROperand.AI_XYZ ar = new AVROperand.AI_XYZ(AVRSymbol.ADR.Z);
            return new AVRAddrMode.LD_ST_AI_XYZ(rd, ar);
        }
    }

    static class $lds$_0_reader extends OperandReader {

        AVRAddrMode read(AVRDisassembler d) {
            d.size = 4;
            AVROperand.op_GPR rd = new AVROperand.op_GPR(GPR_table[readop_0(d)]);
            AVROperand.DADDR addr = new AVROperand.DADDR(readop_17(d));
            return new AVRAddrMode.$lds$(rd, addr);
        }
    }

    static class $sbiw$_0_reader extends OperandReader {

        AVRAddrMode read(AVRDisassembler d) {
            d.size = 2;
            AVROperand.op_RDL rd = new AVROperand.op_RDL(RDL_table[readop_14(d)]);
            AVROperand.IMM6 imm = new AVROperand.IMM6(readop_15(d));
            return new AVRAddrMode.$sbiw$(rd, imm);
        }
    }

    static class $sts$_0_reader extends OperandReader {

        AVRAddrMode read(AVRDisassembler d) {
            d.size = 4;
            AVROperand.DADDR addr = new AVROperand.DADDR(readop_17(d));
            AVROperand.op_GPR rr = new AVROperand.op_GPR(GPR_table[readop_0(d)]);
            return new AVRAddrMode.$sts$(addr, rr);
        }
    }

    static class HGPRIMM8_0_reader extends OperandReader {

        AVRAddrMode read(AVRDisassembler d) {
            d.size = 2;
            AVROperand.op_HGPR rd = new AVROperand.op_HGPR(HGPR_table[readop_11(d)]);
            AVROperand.IMM8 imm = new AVROperand.IMM8(readop_18(d));
            return new AVRAddrMode.HGPRIMM8(rd, imm);
        }
    }

    /**
     * The <code>DTNode</code> class represents a node in a decoding graph. Each node compares a range of bits and
     * branches to other nodes based on the value. Each node may also have an action (such as fixing the addressing mode
     * or instruction) that is executed when the node is reached. Actions on the root node are not executed.
     */
    abstract static class DTNode {

        final int left_bit;
        final int mask;
        final Action action;

        DTNode(Action a, int lb, int msk) {
            action = a;
            left_bit = lb;
            mask = msk;
        }

        abstract DTNode move(AVRDisassembler d, int val);
    }

    /**
     * The <code>DTArrayNode</code> implementation is used for small (less than 32) and dense (more than 50% full) edge
     * lists. It uses an array of indices that is directly indexed by the bits extracted from the stream.
     */
    static class DTArrayNode extends DTNode {

        final DTNode[] nodes;

        DTArrayNode(Action a, int lb, int msk, DTNode[] n) {
            super(a, lb, msk);
            nodes = n;
        }

        DTNode move(AVRDisassembler d, int val) {
            if (action != null) action.execute(d);
            return nodes[val];
        }
    }

    /**
     * The DTSortedNode implementation is used for sparse edge lists. It uses a sorted array of indices and uses binary
     * search on the value of the bits.
     */
    static class DTSortedNode extends DTNode {

        final DTNode def;
        final DTNode[] nodes;
        final int[] values;

        DTSortedNode(Action a, int lb, int msk, int[] v, DTNode[] n, DTNode d) {
            super(a, lb, msk);
            values = v;
            nodes = n;
            def = d;
        }

        DTNode move(AVRDisassembler d, int val) {
            if (action != null) action.execute(d);
            int ind = Arrays.binarySearch(values, val);
            if (ind >= 0 && ind < values.length && values[ind] == val) return nodes[ind];
            else
                return def;
        }
    }

    /**
     * The <code>DTErrorTerm</code> class is a node that terminates the exploration of the instruction decoder with
     * failure.
     */
    static class DTErrorTerm extends DTNode {

        DTErrorTerm() {
            super(null, 0, 0);
        }

        DTNode move(AVRDisassembler d, int bits) {
            d.state = ERR;
            return this;
        }
    }

    /**
     * The <code>ERROR</code> node is reached for incorrectly encoded instructions and indicates that the bit pattern
     * was an incorrectly encoded instruction.
     */
    public static final DTErrorTerm ERROR = new DTErrorTerm();

    /**
     * The <code>Action</code> class represents an action that can happen when the decoder reaches a particular node in
     * the tree. The action may be to fix the instruction or addressing mode, or to signal an error.
     */
    abstract static class Action {

        abstract void execute(AVRDisassembler d);
    }

    /**
     * The <code>ErrorAction</code> class is an action that is fired when the decoding tree reaches a state which
     * indicates the bit pattern is not a valid instruction.
     */
    static class ErrorAction extends Action {

        void execute(AVRDisassembler d) {
            d.state = ERR;
        }
    }

    /**
     * The <code>DTTerm</code> class represents a terminal node in the decoding tree. Terminal nodes are reached when
     * decoding is finished, and represent either successful decoding (meaning instruction and addressing mode were
     * discovered) or unsucessful decoding (meaning the bit pattern does not encode a valid instruction.
     */
    static class DTTerm extends DTNode {

        DTTerm(Action a) {
            super(a, 0, 0);
        }

        DTNode move(AVRDisassembler d, int val) {
            d.state = OK;
            if (action != null) action.execute(d);
            return this;
        }
    }

    /**
     * The <code>SetBuilderAndRead</code> class is an action that is fired when the decoding tree reaches a node where
     * both the instruction and encoding are known. This action fires and sets the <code>builder</code> field to point
     * the appropriate builder for the instruction, as well as setting the <code>addrMode</code> field to point to the
     * operands extracted from the instruction stream.
     */
    static class SetBuilderAndRead extends Action {

        AVRInstrBuilder builder;
        OperandReader reader;

        SetBuilderAndRead(AVRInstrBuilder b, OperandReader r) {
            builder = b;
            reader = r;
        }

        void execute(AVRDisassembler d) {
            d.builder = builder;
            d.addrMode = reader.read(d);
        }
    }

    /**
     * The <code>DTTerminal</code> class is a node that terminates the exploration of the decoder.
     */
    static class DTTerminal extends DTNode {

        DTTerminal(Action a) {
            super(a, 0, 0);
        }

        DTNode move(AVRDisassembler d, int bits) {
            d.state = OK;
            if (action != null) action.execute(d);
            return this;
        }
    }

    /**
     * The <code>OperandReader</code> class is an object that is capable of reading the operands from the bit pattern of
     * an instruction, once the addressing mode is known. One of these classes is generated for each addressing mode.
     * When the addressing mode is finally known, an action will fire that sets the operand reader which is used to read
     * the operands from the bit pattern.
     */
    abstract static class OperandReader {

        abstract AVRAddrMode read(AVRDisassembler d);
    }

    /**
     * The <code>size</code> field is set to the length of the instruction when the decoder reaches a terminal state
     * with a valid instruction.
     */
    private int size;

    /**
     * The <code>builder</code> field stores a reference to the builder that was discovered as a result of traversing
     * the decoder tree. The builder corresponds to one and only one instruction and has a method that can build a new
     * instance of the instruction from the operands.
     */
    private AVRInstrBuilder builder;

    /**
     * The <code>addrMode</code> field stores a reference to the operands that were extracted from the bit pattern as a
     * result of traversing the decoding tree. When a node is reached where the addressing mode is known, then the
     * action on that node executes and reads the operands from the bit pattern, storing them in this field.
     */
    private AVRAddrMode addrMode;

    /**
     * The <code>state</code> field controls the execution of the main decoder loop. When the decoder begins execution,
     * the state field is set to <code>MOVE</code>. The decoder continues until an action fires or a terminal node is
     * reached that sets this field to either <code>OK</code> or <code>ERR</code>.
     */
    private int state;

    /**
     * The <code>pc</code> field stores the current PC, which is needed for PC-relative calculations in loading some
     * operand types.
     */
    private int pc;

    /**
     * The <code>state</code> field is set to <code>MOVE</code> at the beginning of the decoding process and remains
     * this value until a terminal state is reached. This value indicates the main loop should continue.
     */
    private static final int MOVE = 0;

    /**
     * The <code>state</code> field is set to <code>OK</code> when the decoder has reached a terminal state
     * corresponding to a valid instruction.
     */
    private static final int OK = 1;

    /**
     * The <code>state</code> field is set to <code>ERR</code> when the decoder reaches a state corresponding to an
     * incorrectly encoded instruction.
     */
    private static final int ERR = -1;

    /**
     * The <code>word0</code> field stores a word-sized chunk of the instruction stream. It is used by the decoders
     * instead of repeatedly accessing the array. This implementation has been configured with 16-bit words.
     */
    private int word0;

    /**
     * The <code>word1</code> field stores a word-sized chunk of the instruction stream. It is used by the decoders
     * instead of repeatedly accessing the array. This implementation has been configured with 16-bit words.
     */
    private int word1;

    /**
     * The <code>make_root1()</code> method creates a new instance of a decoding tree by allocating the DTNode instances
     * and connecting the references together correctly. It is called only once in the static initialization of the
     * disassembler to build a single shared instance of the decoder tree implementation and the reference to the root
     * node is stored in a single private static field of the same name.
     */
    static DTNode make_root1() {
        DTNode T1 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.STD, new $std$_0_reader()));
        DTNode T2 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.LDD, new $ldd$_0_reader()));
        DTNode N3 = new DTArrayNode(null, 9, 1, new DTNode[] { T2, T1 });
        DTNode N4 = new DTArrayNode(null, 12, 1, new DTNode[] { N3, ERROR });
        DTNode N0 = new DTArrayNode(null, 14, 3, new DTNode[] { ERROR, ERROR, N4, ERROR });
        return N0;
    }

    /**
     * The <code>root1</code> field stores a reference to the root of a decoding tree. It is the starting point for
     * decoding a bit pattern.
     */
    private static final DTNode root1 = make_root1();

    /**
     * The <code>make_root0()</code> method creates a new instance of a decoding tree by allocating the DTNode instances
     * and connecting the references together correctly. It is called only once in the static initialization of the
     * disassembler to build a single shared instance of the decoder tree implementation and the reference to the root
     * node is stored in a single private static field of the same name.
     */
    static DTNode make_root0() {
        DTNode T1 = new DTTerminal(null);
        DTNode N2 = new DTArrayNode(new SetBuilderAndRead(AVRInstrBuilder.BST, new $bst$_0_reader()), 3, 1, new DTNode[] {
                T1, root1 });
        DTNode N3 = new DTArrayNode(new SetBuilderAndRead(AVRInstrBuilder.BLD, new $bld$_0_reader()), 3, 1, new DTNode[] {
                T1, root1 });
        DTNode N4 = new DTArrayNode(null, 9, 1, new DTNode[] { N3, N2 });
        DTNode T5 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.BRPL, new BRANCH_0_reader()));
        DTNode T6 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.BRGE, new BRANCH_0_reader()));
        DTNode T7 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.BRTC, new BRANCH_0_reader()));
        DTNode T8 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.BRNE, new BRANCH_0_reader()));
        DTNode T9 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.BRVC, new BRANCH_0_reader()));
        DTNode T10 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.BRID, new BRANCH_0_reader()));
        DTNode T11 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.BRHC, new BRANCH_0_reader()));
        DTNode T12 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.BRCC, new BRANCH_0_reader()));
        DTNode N13 = new DTArrayNode(null, 0, 7, new DTNode[] { T12, T8, T5, T9, T6, T11, T7, T10 });
        DTNode N14 = new DTArrayNode(new SetBuilderAndRead(AVRInstrBuilder.SBRS, new $sbrs$_0_reader()), 3, 1, new DTNode[] {
                T1, root1 });
        DTNode N15 = new DTArrayNode(new SetBuilderAndRead(AVRInstrBuilder.SBRC, new $sbrc$_0_reader()), 3, 1, new DTNode[] {
                T1, root1 });
        DTNode N16 = new DTArrayNode(null, 9, 1, new DTNode[] { N15, N14 });
        DTNode T17 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.BRMI, new BRANCH_0_reader()));
        DTNode T18 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.BRLT, new BRANCH_0_reader()));
        DTNode T19 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.BRTS, new BRANCH_0_reader()));
        DTNode T20 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.BREQ, new BRANCH_0_reader()));
        DTNode T21 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.BRVS, new BRANCH_0_reader()));
        DTNode T22 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.BRIE, new BRANCH_0_reader()));
        DTNode T23 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.BRHS, new BRANCH_0_reader()));
        DTNode T24 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.BRCS, new BRANCH_0_reader()));
        DTNode N25 = new DTArrayNode(null, 0, 7, new DTNode[] { T24, T20, T17, T21, T18, T23, T19, T22 });
        DTNode N26 = new DTArrayNode(null, 10, 3, new DTNode[] { N25, N13, N4, N16 });
        DTNode T27 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.SBCI, new HGPRIMM8_0_reader()));
        DTNode T28 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.ST, new LD_ST_XYZ_1_reader()));
        DTNode T29 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.ST, new LD_ST_XYZ_2_reader()));
        DTNode N30 = new DTArrayNode(null, 0, 15, new DTNode[] { T29, root1, root1, root1, root1, root1, root1, root1,
                T28, root1, root1, root1, root1, root1, root1, root1 });
        DTNode T31 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.LD, new LD_ST_XYZ_1_reader()));
        DTNode T32 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.LD, new LD_ST_XYZ_2_reader()));
        DTNode N33 = new DTArrayNode(null, 0, 15, new DTNode[] { T32, root1, root1, root1, root1, root1, root1, root1,
                T31, root1, root1, root1, root1, root1, root1, root1 });
        DTNode N34 = new DTArrayNode(null, 9, 7, new DTNode[] { N33, N30, root1, root1, root1, root1, root1, root1 });
        DTNode T35 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.OUT, new $out$_0_reader()));
        DTNode T36 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.IN, new $in$_0_reader()));
        DTNode N37 = new DTArrayNode(null, 11, 1, new DTNode[] { T36, T35 });
        DTNode T38 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.CPI, new HGPRIMM8_0_reader()));
        DTNode T39 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.ANDI, new HGPRIMM8_0_reader()));
        DTNode T40 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.RJMP, new $rjmp$_0_reader()));
        DTNode T41 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.OR, new GPRGPR_0_reader()));
        DTNode T42 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.EOR, new GPRGPR_0_reader()));
        DTNode T43 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.MOV, new GPRGPR_0_reader()));
        DTNode T44 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.AND, new GPRGPR_0_reader()));
        DTNode N45 = new DTArrayNode(null, 10, 3, new DTNode[] { T44, T42, T41, T43 });
        DTNode T46 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.RCALL, new $rcall$_0_reader()));
        DTNode T47 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.SBI, new $sbi$_0_reader()));
        DTNode T48 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.SBIC, new $sbic$_0_reader()));
        DTNode T49 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.SBIS, new $sbis$_0_reader()));
        DTNode T50 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.CBI, new $cbi$_0_reader()));
        DTNode N51 = new DTArrayNode(null, 8, 3, new DTNode[] { T50, T48, T47, T49 });
        DTNode T52 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.SBIW, new $sbiw$_0_reader()));
        DTNode T53 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.ADIW, new $adiw$_0_reader()));
        DTNode N54 = new DTArrayNode(null, 8, 1, new DTNode[] { T53, T52 });
        DTNode T55 = new DTTerminal(null);
        DTNode N56 = new DTArrayNode(new SetBuilderAndRead(AVRInstrBuilder.ASR, new GPR_0_reader()), 0, 1, new DTNode[] {
                root1, T55 });
        DTNode T57 = new DTTerminal(null);
        DTNode N58 = new DTArrayNode(new SetBuilderAndRead(AVRInstrBuilder.CLI, new NULL_reader(2)), 0, 1, new DTNode[] {
                T57, root1 });
        DTNode N59 = new DTArrayNode(new SetBuilderAndRead(AVRInstrBuilder.SES, new NULL_reader(2)), 0, 1, new DTNode[] {
                T57, root1 });
        DTNode N60 = new DTArrayNode(new SetBuilderAndRead(AVRInstrBuilder.SPM, new NULL_reader(2)), 0, 1, new DTNode[] {
                T57, root1 });
        DTNode N61 = new DTArrayNode(new SetBuilderAndRead(AVRInstrBuilder.CLC, new NULL_reader(2)), 0, 1, new DTNode[] {
                T57, root1 });
        DTNode N62 = new DTArrayNode(new SetBuilderAndRead(AVRInstrBuilder.WDR, new NULL_reader(2)), 0, 1, new DTNode[] {
                T57, root1 });
        DTNode N63 = new DTArrayNode(new SetBuilderAndRead(AVRInstrBuilder.CLV, new NULL_reader(2)), 0, 1, new DTNode[] {
                T57, root1 });
        DTNode T64 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.ICALL, new NULL_reader(2)));
        DTNode T65 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.RET, new NULL_reader(2)));
        DTNode N66 = new DTArrayNode(null, 0, 1, new DTNode[] { T65, T64 });
        DTNode N67 = new DTArrayNode(new SetBuilderAndRead(AVRInstrBuilder.SEV, new NULL_reader(2)), 0, 1, new DTNode[] {
                T57, root1 });
        DTNode N68 = new DTArrayNode(new SetBuilderAndRead(AVRInstrBuilder.SEI, new NULL_reader(2)), 0, 1, new DTNode[] {
                T57, root1 });
        DTNode N69 = new DTArrayNode(new SetBuilderAndRead(AVRInstrBuilder.CLS, new NULL_reader(2)), 0, 1, new DTNode[] {
                T57, root1 });
        DTNode T70 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.EICALL, new NULL_reader(2)));
        DTNode T71 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.RETI, new NULL_reader(2)));
        DTNode N72 = new DTArrayNode(null, 0, 1, new DTNode[] { T71, T70 });
        DTNode N73 = new DTArrayNode(new SetBuilderAndRead(AVRInstrBuilder.SEN, new NULL_reader(2)), 0, 1, new DTNode[] {
                T57, root1 });
        DTNode N74 = new DTArrayNode(new SetBuilderAndRead(AVRInstrBuilder.CLH, new NULL_reader(2)), 0, 1, new DTNode[] {
                T57, root1 });
        DTNode N75 = new DTArrayNode(new SetBuilderAndRead(AVRInstrBuilder.CLZ, new NULL_reader(2)), 0, 1, new DTNode[] {
                T57, root1 });
        DTNode N76 = new DTArrayNode(new SetBuilderAndRead(AVRInstrBuilder.LPM, new XLPM_REG_0_reader()), 0, 1, new DTNode[] {
                T57, root1 });
        DTNode N77 = new DTArrayNode(new SetBuilderAndRead(AVRInstrBuilder.SET, new NULL_reader(2)), 0, 1, new DTNode[] {
                T57, root1 });
        DTNode T78 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.EIJMP, new NULL_reader(2)));
        DTNode T79 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.SEZ, new NULL_reader(2)));
        DTNode N80 = new DTArrayNode(null, 0, 1, new DTNode[] { T79, T78 });
        DTNode N81 = new DTArrayNode(new SetBuilderAndRead(AVRInstrBuilder.ELPM, new XLPM_REG_0_reader()), 0, 1, new DTNode[] {
                T57, root1 });
        DTNode N82 = new DTArrayNode(new SetBuilderAndRead(AVRInstrBuilder.CLT, new NULL_reader(2)), 0, 1, new DTNode[] {
                T57, root1 });
        DTNode N83 = new DTArrayNode(new SetBuilderAndRead(AVRInstrBuilder.BREAK, new NULL_reader(2)), 0, 1, new DTNode[] {
                T57, root1 });
        DTNode N84 = new DTArrayNode(new SetBuilderAndRead(AVRInstrBuilder.SLEEP, new NULL_reader(2)), 0, 1, new DTNode[] {
                T57, root1 });
        DTNode N85 = new DTArrayNode(new SetBuilderAndRead(AVRInstrBuilder.CLN, new NULL_reader(2)), 0, 1, new DTNode[] {
                T57, root1 });
        DTNode N86 = new DTArrayNode(new SetBuilderAndRead(AVRInstrBuilder.SEH, new NULL_reader(2)), 0, 1, new DTNode[] {
                T57, root1 });
        DTNode T87 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.IJMP, new NULL_reader(2)));
        DTNode T88 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.SEC, new NULL_reader(2)));
        DTNode N89 = new DTArrayNode(null, 0, 1, new DTNode[] { T88, T87 });
        DTNode N90 = new DTArrayNode(null, 4, 31, new DTNode[] { N89, N80, N73, N67, N59, N86, N77, N68, N61, N75, N85,
                N63, N69, N74, N82, N58, N66, N72, root1, root1, root1, root1, root1, root1, N84, N83, N62, root1, N76,
                N81, N60, root1 });
        DTNode T91 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.JMP, new $jmp$_0_reader()));
        DTNode T92 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.INC, new GPR_0_reader()));
        DTNode T93 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.SWAP, new GPR_0_reader()));
        DTNode N94 = new DTArrayNode(null, 0, 1, new DTNode[] { T93, T92 });
        DTNode T95 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.ROR, new GPR_0_reader()));
        DTNode T96 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.LSR, new GPR_0_reader()));
        DTNode N97 = new DTArrayNode(null, 0, 1, new DTNode[] { T96, T95 });
        DTNode T98 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.CALL, new $call$_0_reader()));
        DTNode N99 = new DTArrayNode(new SetBuilderAndRead(AVRInstrBuilder.DEC, new GPR_0_reader()), 0, 1, new DTNode[] {
                T55, root1 });
        DTNode T100 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.NEG, new GPR_0_reader()));
        DTNode T101 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.COM, new GPR_0_reader()));
        DTNode N102 = new DTArrayNode(null, 0, 1, new DTNode[] { T101, T100 });
        DTNode N103 = new DTArrayNode(null, 1, 7, new DTNode[] { N102, N94, N56, N97, N90, N99, T91, T98 });
        DTNode N104 = new DTArrayNode(null, 9, 1, new DTNode[] { N103, N54 });
        DTNode T105 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.MUL, new GPRGPR_0_reader()));
        DTNode T106 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.ST, new LD_ST_PD_XYZ_2_reader()));
        DTNode T107 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.PUSH, new GPR_0_reader()));
        DTNode T108 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.ST, new LD_ST_AI_XYZ_0_reader()));
        DTNode T109 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.ST, new LD_ST_AI_XYZ_1_reader()));
        DTNode T110 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.ST, new LD_ST_AI_XYZ_2_reader()));
        DTNode T111 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.ST, new LD_ST_PD_XYZ_0_reader()));
        DTNode T112 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.ST, new LD_ST_PD_XYZ_1_reader()));
        DTNode T113 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.ST, new LD_ST_XYZ_0_reader()));
        DTNode T114 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.STS, new $sts$_0_reader()));
        DTNode N115 = new DTArrayNode(null, 0, 15, new DTNode[] { T114, T110, T106, root1, root1, root1, root1, root1,
                root1, T109, T112, root1, T113, T108, T111, T107 });
        DTNode T116 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.POP, new GPR_0_reader()));
        DTNode T117 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.LPM, new XLPM_D_0_reader()));
        DTNode T118 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.ELPM, new XLPM_INC_0_reader()));
        DTNode T119 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.LD, new LD_ST_XYZ_0_reader()));
        DTNode T120 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.LD, new LD_ST_PD_XYZ_2_reader()));
        DTNode T121 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.LD, new LD_ST_AI_XYZ_0_reader()));
        DTNode T122 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.LD, new LD_ST_AI_XYZ_1_reader()));
        DTNode T123 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.ELPM, new XLPM_D_0_reader()));
        DTNode T124 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.LD, new LD_ST_AI_XYZ_2_reader()));
        DTNode T125 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.LD, new LD_ST_PD_XYZ_0_reader()));
        DTNode T126 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.LD, new LD_ST_PD_XYZ_1_reader()));
        DTNode T127 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.LPM, new XLPM_INC_0_reader()));
        DTNode T128 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.LDS, new $lds$_0_reader()));
        DTNode N129 = new DTArrayNode(null, 0, 15, new DTNode[] { T128, T124, T120, root1, T117, T127, T123, T118,
                root1, T122, T126, root1, T119, T121, T125, T116 });
        DTNode N130 = new DTArrayNode(null, 9, 1, new DTNode[] { N129, N115 });
        DTNode N131 = new DTArrayNode(null, 10, 3, new DTNode[] { N130, N104, N51, T105 });
        DTNode T132 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.ORI, new HGPRIMM8_0_reader()));
        DTNode T133 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.SUB, new GPRGPR_0_reader()));
        DTNode T134 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.CP, new GPRGPR_0_reader()));
        DTNode T135 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.ADC, new GPRGPR_0_reader()));
        DTNode T136 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.CPSE, new GPRGPR_0_reader()));
        DTNode N137 = new DTArrayNode(null, 10, 3, new DTNode[] { T136, T134, T133, T135 });
        DTNode T138 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.LDI, new HGPRIMM8_0_reader()));
        DTNode T139 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.SUBI, new HGPRIMM8_0_reader()));
        DTNode T140 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.SBC, new GPRGPR_0_reader()));
        DTNode T141 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.CPC, new GPRGPR_0_reader()));
        DTNode T142 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.ADD, new GPRGPR_0_reader()));
        DTNode T143 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.MULS, new $muls$_0_reader()));
        DTNode T144 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.MOVW, new $movw$_0_reader()));
        DTNode T145 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.FMULSU, new $fmulsu$_0_reader()));
        DTNode T146 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.FMULS, new $fmuls$_0_reader()));
        DTNode N147 = new DTArrayNode(null, 3, 1, new DTNode[] { T146, T145 });
        DTNode T148 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.FMUL, new $fmul$_0_reader()));
        DTNode T149 = new DTTerminal(new SetBuilderAndRead(AVRInstrBuilder.MULSU, new $mulsu$_0_reader()));
        DTNode N150 = new DTArrayNode(null, 3, 1, new DTNode[] { T149, T148 });
        DTNode N151 = new DTArrayNode(null, 7, 1, new DTNode[] { N150, N147 });
        DTNode N152 = new DTSortedNode(new SetBuilderAndRead(AVRInstrBuilder.NOP, new NULL_reader(2)), 0, 255, new int[] {
                0 }, new DTNode[] { T57 }, root1);
        DTNode N153 = new DTArrayNode(null, 8, 3, new DTNode[] { N152, T144, T143, N151 });
        DTNode N154 = new DTArrayNode(null, 10, 3, new DTNode[] { N153, T141, T140, T142 });
        DTNode N0 = new DTArrayNode(null, 12, 15, new DTNode[] { N154, N137, N45, T38, T27, T139, T132, T39, N34, N131,
                root1, N37, T40, T46, T138, N26 });
        return N0;
    }

    /**
     * The <code>root0</code> field stores a reference to the root of a decoding tree. It is the starting point for
     * decoding a bit pattern.
     */
    private static final DTNode root0 = make_root0();

    /**
     * The <code>disassemble()</code> method disassembles a single instruction from a stream of bytes. If the binary
     * data at that location contains a valid instruction, then it is created and returned. If the binary data at the
     * specified location is not a valid instruction, this method returns null.
     *
     * @param base  the base address corresponding to index 0 in the array
     * @param index the index into the specified array where to begin disassembling
     * @param code  the binary data to disassemble into an instruction
     * @return a reference to a new instruction object representing the instruction at that location; null if the binary
     *         data at the specified location does not represent a valid instruction
     */
    public AbstractInstr disassemble(int base, int index, byte[] code) {
        return decode(base, index, code);
    }

    /**
     * The <code>decode()</code> method is the main entrypoint to the disassembler. Given an array of type
     * <code>byte[]</code>, a base address, and an index, the disassembler will attempt to decode one instruction at
     * that location. If successful, the method will return a reference to a new <code>AVRInstr</code> object.
     *
     * @param base  the base address of the array
     * @param index the index into the array where to begin decoding
     * @param code  the actual code
     * @return an instance of the <code>AVRInstr</code> class corresponding to the instruction at this address if a
     *         valid instruction exists here; null otherwise
     */
    public AVRInstr decode(int base, int index, byte[] code) {
        word0 = ((code[index] & 0xFF)) | ((code[index + 1] & 0xFF) << 8);
        word1 = ((code[index + 2] & 0xFF)) | ((code[index + 3] & 0xFF) << 8);
        pc = base + index;
        return decode_root();
    }

    /**
     * The <code>decode()</code> method is the main entrypoint to the disassembler. Given an array of type
     * <code>char[]</code>, a base address, and an index, the disassembler will attempt to decode one instruction at
     * that location. If successful, the method will return a reference to a new <code>AVRInstr</code> object.
     *
     * @param base  the base address of the array
     * @param index the index into the array where to begin decoding
     * @param code  the actual code
     * @return an instance of the <code>AVRInstr</code> class corresponding to the instruction at this address if a
     *         valid instruction exists here; null otherwise
     */
    public AVRInstr decode(int base, int index, char[] code) {
        word0 = (code[index]);
        word1 = (code[index + 1]);
        pc = base + index * 2;
        return decode_root();
    }

    /**
     * The <code>decode()</code> method is the main entrypoint to the disassembler. Given an array of type
     * <code>short[]</code>, a base address, and an index, the disassembler will attempt to decode one instruction at
     * that location. If successful, the method will return a reference to a new <code>AVRInstr</code> object.
     *
     * @param base  the base address of the array
     * @param index the index into the array where to begin decoding
     * @param code  the actual code
     * @return an instance of the <code>AVRInstr</code> class corresponding to the instruction at this address if a
     *         valid instruction exists here; null otherwise
     */
    public AVRInstr decode(int base, int index, short[] code) {
        word0 = ((code[index] & 0xFFFF));
        word1 = ((code[index + 1] & 0xFFFF));
        pc = base + index * 2;
        return decode_root();
    }

    /**
     * The <code>decoder_root()</code> method begins decoding the bit pattern into an instruction.
     */
    AVRInstr decode_root() {
        size = 0;
        builder = null;
        addrMode = null;
        return run_decoder(root0);
    }

    /**
     * The <code>run_decoder()</code> method begins decoding the bit pattern into an instruction starting at the
     * specified <code>DTNode</code> representing the root of a decoder. This implementation resolves both instruction
     * and addressing mode with one decoder. It begins at the root node and continues comparing bits and following the
     * appropriate paths until a terminal node is reached.
     *
     * @param node a reference to the root of the decoder where to begin decoding
     */
    private AVRInstr run_decoder(DTNode node) {
        state = MOVE;
        while (state == MOVE) {
            int bits = (word0 >> node.left_bit) & node.mask;
            node = node.move(this, bits);
        }
        if (state == ERR) return null;
        else
            return builder.build(size, addrMode);
    }
}
