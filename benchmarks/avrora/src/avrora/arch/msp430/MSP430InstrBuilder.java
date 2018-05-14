package avrora.arch.msp430;
import java.util.HashMap;
public abstract class MSP430InstrBuilder {
    public abstract MSP430Instr build(int size, MSP430AddrMode am);
    static final HashMap builders = new HashMap();
    static MSP430InstrBuilder add(String name, MSP430InstrBuilder b) {
        builders.put(name, b);
        return b;
    }
    public static class ADD_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.ADD(size, (MSP430AddrMode.DOUBLE_W)am);
        }
    }
    public static class ADD_B_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.ADD_B(size, (MSP430AddrMode.DOUBLE_B)am);
        }
    }
    public static class ADDC_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.ADDC(size, (MSP430AddrMode.DOUBLE_W)am);
        }
    }
    public static class ADDC_B_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.ADDC_B(size, (MSP430AddrMode.DOUBLE_B)am);
        }
    }
    public static class AND_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.AND(size, (MSP430AddrMode.DOUBLE_W)am);
        }
    }
    public static class AND_B_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.AND_B(size, (MSP430AddrMode.DOUBLE_B)am);
        }
    }
    public static class BIC_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.BIC(size, (MSP430AddrMode.DOUBLE_W)am);
        }
    }
    public static class BIC_B_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.BIC_B(size, (MSP430AddrMode.DOUBLE_B)am);
        }
    }
    public static class BIS_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.BIS(size, (MSP430AddrMode.DOUBLE_W)am);
        }
    }
    public static class BIS_B_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.BIS_B(size, (MSP430AddrMode.DOUBLE_B)am);
        }
    }
    public static class BIT_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.BIT(size, (MSP430AddrMode.DOUBLE_W)am);
        }
    }
    public static class BIT_B_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.BIT_B(size, (MSP430AddrMode.DOUBLE_B)am);
        }
    }
    public static class CALL_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.CALL(size, (MSP430AddrMode.SINGLE_W)am);
        }
    }
    public static class CMP_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.CMP(size, (MSP430AddrMode.DOUBLE_W)am);
        }
    }
    public static class CMP_B_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.CMP_B(size, (MSP430AddrMode.DOUBLE_B)am);
        }
    }
    public static class DADD_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.DADD(size, (MSP430AddrMode.DOUBLE_W)am);
        }
    }
    public static class DADD_B_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.DADD_B(size, (MSP430AddrMode.DOUBLE_B)am);
        }
    }
    public static class JC_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.JC(size, (MSP430AddrMode.JMP)am);
        }
    }
    public static class JHS_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.JHS(size, (MSP430AddrMode.JMP)am);
        }
    }
    public static class JEQ_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.JEQ(size, (MSP430AddrMode.JMP)am);
        }
    }
    public static class JZ_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.JZ(size, (MSP430AddrMode.JMP)am);
        }
    }
    public static class JGE_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.JGE(size, (MSP430AddrMode.JMP)am);
        }
    }
    public static class JL_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.JL(size, (MSP430AddrMode.JMP)am);
        }
    }
    public static class JMP_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.JMP(size, (MSP430AddrMode.JMP)am);
        }
    }
    public static class JN_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.JN(size, (MSP430AddrMode.JMP)am);
        }
    }
    public static class JNC_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.JNC(size, (MSP430AddrMode.JMP)am);
        }
    }
    public static class JLO_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.JLO(size, (MSP430AddrMode.JMP)am);
        }
    }
    public static class JNE_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.JNE(size, (MSP430AddrMode.JMP)am);
        }
    }
    public static class JNZ_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.JNZ(size, (MSP430AddrMode.JMP)am);
        }
    }
    public static class MOV_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.MOV(size, (MSP430AddrMode.DOUBLE_W)am);
        }
    }
    public static class MOV_B_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.MOV_B(size, (MSP430AddrMode.DOUBLE_B)am);
        }
    }
    public static class PUSH_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.PUSH(size, (MSP430AddrMode.SINGLE_W)am);
        }
    }
    public static class PUSH_B_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.PUSH_B(size, (MSP430AddrMode.SINGLE_B)am);
        }
    }
    public static class RETI_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.RETI(size);
        }
    }
    public static class RRA_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.RRA(size, (MSP430AddrMode.SINGLE_W)am);
        }
    }
    public static class RRA_B_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.RRA_B(size, (MSP430AddrMode.SINGLE_B)am);
        }
    }
    public static class RRC_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.RRC(size, (MSP430AddrMode.SINGLE_W)am);
        }
    }
    public static class RRC_B_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.RRC_B(size, (MSP430AddrMode.SINGLE_B)am);
        }
    }
    public static class SUB_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.SUB(size, (MSP430AddrMode.DOUBLE_W)am);
        }
    }
    public static class SUB_B_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.SUB_B(size, (MSP430AddrMode.DOUBLE_B)am);
        }
    }
    public static class SUBC_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.SUBC(size, (MSP430AddrMode.DOUBLE_W)am);
        }
    }
    public static class SUBC_B_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.SUBC_B(size, (MSP430AddrMode.DOUBLE_B)am);
        }
    }
    public static class SWPB_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.SWPB(size, (MSP430AddrMode.SINGLE_W)am);
        }
    }
    public static class SXT_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.SXT(size, (MSP430AddrMode.SINGLE_W)am);
        }
    }
    public static class TST_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.TST(size, (MSP430AddrMode.SINGLE_W)am);
        }
    }
    public static class TST_B_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.TST_B(size, (MSP430AddrMode.SINGLE_B)am);
        }
    }
    public static class XOR_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.XOR(size, (MSP430AddrMode.DOUBLE_W)am);
        }
    }
    public static class XOR_B_builder extends MSP430InstrBuilder {
        public MSP430Instr build(int size, MSP430AddrMode am) {
            return new MSP430Instr.XOR_B(size, (MSP430AddrMode.DOUBLE_B)am);
        }
    }
    public static final MSP430InstrBuilder ADD = add("add", new ADD_builder());
    public static final MSP430InstrBuilder ADD_B = add("add.b", new ADD_B_builder());
    public static final MSP430InstrBuilder ADDC = add("addc", new ADDC_builder());
    public static final MSP430InstrBuilder ADDC_B = add("addc.b", new ADDC_B_builder());
    public static final MSP430InstrBuilder AND = add("and", new AND_builder());
    public static final MSP430InstrBuilder AND_B = add("and.b", new AND_B_builder());
    public static final MSP430InstrBuilder BIC = add("bic", new BIC_builder());
    public static final MSP430InstrBuilder BIC_B = add("bic.b", new BIC_B_builder());
    public static final MSP430InstrBuilder BIS = add("bis", new BIS_builder());
    public static final MSP430InstrBuilder BIS_B = add("bis.b", new BIS_B_builder());
    public static final MSP430InstrBuilder BIT = add("bit", new BIT_builder());
    public static final MSP430InstrBuilder BIT_B = add("bit.b", new BIT_B_builder());
    public static final MSP430InstrBuilder CALL = add("call", new CALL_builder());
    public static final MSP430InstrBuilder CMP = add("cmp", new CMP_builder());
    public static final MSP430InstrBuilder CMP_B = add("cmp.b", new CMP_B_builder());
    public static final MSP430InstrBuilder DADD = add("dadd", new DADD_builder());
    public static final MSP430InstrBuilder DADD_B = add("dadd.b", new DADD_B_builder());
    public static final MSP430InstrBuilder JC = add("jc", new JC_builder());
    public static final MSP430InstrBuilder JHS = add("jhs", new JHS_builder());
    public static final MSP430InstrBuilder JEQ = add("jeq", new JEQ_builder());
    public static final MSP430InstrBuilder JZ = add("jz", new JZ_builder());
    public static final MSP430InstrBuilder JGE = add("jge", new JGE_builder());
    public static final MSP430InstrBuilder JL = add("jl", new JL_builder());
    public static final MSP430InstrBuilder JMP = add("jmp", new JMP_builder());
    public static final MSP430InstrBuilder JN = add("jn", new JN_builder());
    public static final MSP430InstrBuilder JNC = add("jnc", new JNC_builder());
    public static final MSP430InstrBuilder JLO = add("jlo", new JLO_builder());
    public static final MSP430InstrBuilder JNE = add("jne", new JNE_builder());
    public static final MSP430InstrBuilder JNZ = add("jnz", new JNZ_builder());
    public static final MSP430InstrBuilder MOV = add("mov", new MOV_builder());
    public static final MSP430InstrBuilder MOV_B = add("mov.b", new MOV_B_builder());
    public static final MSP430InstrBuilder PUSH = add("push", new PUSH_builder());
    public static final MSP430InstrBuilder PUSH_B = add("push.b", new PUSH_B_builder());
    public static final MSP430InstrBuilder RETI = add("reti", new RETI_builder());
    public static final MSP430InstrBuilder RRA = add("rra", new RRA_builder());
    public static final MSP430InstrBuilder RRA_B = add("rra.b", new RRA_B_builder());
    public static final MSP430InstrBuilder RRC = add("rrc", new RRC_builder());
    public static final MSP430InstrBuilder RRC_B = add("rrc.b", new RRC_B_builder());
    public static final MSP430InstrBuilder SUB = add("sub", new SUB_builder());
    public static final MSP430InstrBuilder SUB_B = add("sub.b", new SUB_B_builder());
    public static final MSP430InstrBuilder SUBC = add("subc", new SUBC_builder());
    public static final MSP430InstrBuilder SUBC_B = add("subc.b", new SUBC_B_builder());
    public static final MSP430InstrBuilder SWPB = add("swpb", new SWPB_builder());
    public static final MSP430InstrBuilder SXT = add("sxt", new SXT_builder());
    public static final MSP430InstrBuilder TST = add("tst", new TST_builder());
    public static final MSP430InstrBuilder TST_B = add("tst.b", new TST_B_builder());
    public static final MSP430InstrBuilder XOR = add("xor", new XOR_builder());
    public static final MSP430InstrBuilder XOR_B = add("xor.b", new XOR_B_builder());
    public static int checkValue(int val, int low, int high) {
        if ( val < low || val > high ) {
            throw new Error();
        }
        return val;
    }
}
