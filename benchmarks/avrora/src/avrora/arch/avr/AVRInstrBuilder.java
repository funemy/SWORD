package avrora.arch.avr;

import java.util.HashMap;

public abstract class AVRInstrBuilder {

    public abstract AVRInstr build(int size, AVRAddrMode am);

    static final HashMap builders = new HashMap();

    static AVRInstrBuilder add(String name, AVRInstrBuilder b) {
        builders.put(name, b);
        return b;
    }

    public static class ADC_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.ADC(size, (AVRAddrMode.GPRGPR)am);
        }
    }

    public static class ADD_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.ADD(size, (AVRAddrMode.GPRGPR)am);
        }
    }

    public static class ADIW_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.ADIW(size, (AVRAddrMode.$adiw$)am);
        }
    }

    public static class AND_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.AND(size, (AVRAddrMode.GPRGPR)am);
        }
    }

    public static class ANDI_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.ANDI(size, (AVRAddrMode.HGPRIMM8)am);
        }
    }

    public static class ASR_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.ASR(size, (AVRAddrMode.GPR)am);
        }
    }

    public static class BCLR_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.BCLR(size, (AVRAddrMode.$bclr$)am);
        }
    }

    public static class BLD_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.BLD(size, (AVRAddrMode.$bld$)am);
        }
    }

    public static class BRBC_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.BRBC(size, (AVRAddrMode.$brbc$)am);
        }
    }

    public static class BRBS_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.BRBS(size, (AVRAddrMode.$brbs$)am);
        }
    }

    public static class BRCC_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.BRCC(size, (AVRAddrMode.BRANCH)am);
        }
    }

    public static class BRCS_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.BRCS(size, (AVRAddrMode.BRANCH)am);
        }
    }

    public static class BREAK_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.BREAK(size);
        }
    }

    public static class BREQ_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.BREQ(size, (AVRAddrMode.BRANCH)am);
        }
    }

    public static class BRGE_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.BRGE(size, (AVRAddrMode.BRANCH)am);
        }
    }

    public static class BRHC_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.BRHC(size, (AVRAddrMode.BRANCH)am);
        }
    }

    public static class BRHS_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.BRHS(size, (AVRAddrMode.BRANCH)am);
        }
    }

    public static class BRID_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.BRID(size, (AVRAddrMode.BRANCH)am);
        }
    }

    public static class BRIE_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.BRIE(size, (AVRAddrMode.BRANCH)am);
        }
    }

    public static class BRLO_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.BRLO(size, (AVRAddrMode.BRANCH)am);
        }
    }

    public static class BRLT_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.BRLT(size, (AVRAddrMode.BRANCH)am);
        }
    }

    public static class BRMI_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.BRMI(size, (AVRAddrMode.BRANCH)am);
        }
    }

    public static class BRNE_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.BRNE(size, (AVRAddrMode.BRANCH)am);
        }
    }

    public static class BRPL_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.BRPL(size, (AVRAddrMode.BRANCH)am);
        }
    }

    public static class BRSH_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.BRSH(size, (AVRAddrMode.BRANCH)am);
        }
    }

    public static class BRTC_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.BRTC(size, (AVRAddrMode.BRANCH)am);
        }
    }

    public static class BRTS_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.BRTS(size, (AVRAddrMode.BRANCH)am);
        }
    }

    public static class BRVC_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.BRVC(size, (AVRAddrMode.BRANCH)am);
        }
    }

    public static class BRVS_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.BRVS(size, (AVRAddrMode.BRANCH)am);
        }
    }

    public static class BSET_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.BSET(size, (AVRAddrMode.$bset$)am);
        }
    }

    public static class BST_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.BST(size, (AVRAddrMode.$bst$)am);
        }
    }

    public static class CALL_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.CALL(size, (AVRAddrMode.$call$)am);
        }
    }

    public static class CBI_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.CBI(size, (AVRAddrMode.$cbi$)am);
        }
    }

    public static class CBR_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.CBR(size, (AVRAddrMode.HGPRIMM8)am);
        }
    }

    public static class CLC_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.CLC(size);
        }
    }

    public static class CLH_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.CLH(size);
        }
    }

    public static class CLI_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.CLI(size);
        }
    }

    public static class CLN_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.CLN(size);
        }
    }

    public static class CLR_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.CLR(size, (AVRAddrMode.$clr$)am);
        }
    }

    public static class CLS_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.CLS(size);
        }
    }

    public static class CLT_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.CLT(size);
        }
    }

    public static class CLV_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.CLV(size);
        }
    }

    public static class CLZ_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.CLZ(size);
        }
    }

    public static class COM_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.COM(size, (AVRAddrMode.GPR)am);
        }
    }

    public static class CP_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.CP(size, (AVRAddrMode.GPRGPR)am);
        }
    }

    public static class CPC_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.CPC(size, (AVRAddrMode.GPRGPR)am);
        }
    }

    public static class CPI_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.CPI(size, (AVRAddrMode.HGPRIMM8)am);
        }
    }

    public static class CPSE_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.CPSE(size, (AVRAddrMode.GPRGPR)am);
        }
    }

    public static class DEC_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.DEC(size, (AVRAddrMode.GPR)am);
        }
    }

    public static class EICALL_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.EICALL(size);
        }
    }

    public static class EIJMP_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.EIJMP(size);
        }
    }

    public static class EOR_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.EOR(size, (AVRAddrMode.GPRGPR)am);
        }
    }

    public static class FMUL_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.FMUL(size, (AVRAddrMode.$fmul$)am);
        }
    }

    public static class FMULS_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.FMULS(size, (AVRAddrMode.$fmuls$)am);
        }
    }

    public static class FMULSU_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.FMULSU(size, (AVRAddrMode.$fmulsu$)am);
        }
    }

    public static class ICALL_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.ICALL(size);
        }
    }

    public static class IJMP_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.IJMP(size);
        }
    }

    public static class IN_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.IN(size, (AVRAddrMode.$in$)am);
        }
    }

    public static class INC_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.INC(size, (AVRAddrMode.GPR)am);
        }
    }

    public static class JMP_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.JMP(size, (AVRAddrMode.$jmp$)am);
        }
    }

    public static class LDD_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.LDD(size, (AVRAddrMode.$ldd$)am);
        }
    }

    public static class LDI_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.LDI(size, (AVRAddrMode.HGPRIMM8)am);
        }
    }

    public static class LDS_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.LDS(size, (AVRAddrMode.$lds$)am);
        }
    }

    public static class LSL_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.LSL(size, (AVRAddrMode.$lsl$)am);
        }
    }

    public static class LSR_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.LSR(size, (AVRAddrMode.GPR)am);
        }
    }

    public static class MOV_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.MOV(size, (AVRAddrMode.GPRGPR)am);
        }
    }

    public static class MOVW_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.MOVW(size, (AVRAddrMode.$movw$)am);
        }
    }

    public static class MUL_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.MUL(size, (AVRAddrMode.GPRGPR)am);
        }
    }

    public static class MULS_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.MULS(size, (AVRAddrMode.$muls$)am);
        }
    }

    public static class MULSU_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.MULSU(size, (AVRAddrMode.$mulsu$)am);
        }
    }

    public static class NEG_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.NEG(size, (AVRAddrMode.GPR)am);
        }
    }

    public static class NOP_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.NOP(size);
        }
    }

    public static class OR_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.OR(size, (AVRAddrMode.GPRGPR)am);
        }
    }

    public static class ORI_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.ORI(size, (AVRAddrMode.HGPRIMM8)am);
        }
    }

    public static class OUT_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.OUT(size, (AVRAddrMode.$out$)am);
        }
    }

    public static class POP_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.POP(size, (AVRAddrMode.GPR)am);
        }
    }

    public static class PUSH_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.PUSH(size, (AVRAddrMode.GPR)am);
        }
    }

    public static class RCALL_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.RCALL(size, (AVRAddrMode.$rcall$)am);
        }
    }

    public static class RET_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.RET(size);
        }
    }

    public static class RETI_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.RETI(size);
        }
    }

    public static class RJMP_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.RJMP(size, (AVRAddrMode.$rjmp$)am);
        }
    }

    public static class ROL_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.ROL(size, (AVRAddrMode.$rol$)am);
        }
    }

    public static class ROR_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.ROR(size, (AVRAddrMode.GPR)am);
        }
    }

    public static class SBC_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.SBC(size, (AVRAddrMode.GPRGPR)am);
        }
    }

    public static class SBCI_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.SBCI(size, (AVRAddrMode.HGPRIMM8)am);
        }
    }

    public static class SBI_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.SBI(size, (AVRAddrMode.$sbi$)am);
        }
    }

    public static class SBIC_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.SBIC(size, (AVRAddrMode.$sbic$)am);
        }
    }

    public static class SBIS_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.SBIS(size, (AVRAddrMode.$sbis$)am);
        }
    }

    public static class SBIW_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.SBIW(size, (AVRAddrMode.$sbiw$)am);
        }
    }

    public static class SBR_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.SBR(size, (AVRAddrMode.HGPRIMM8)am);
        }
    }

    public static class SBRC_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.SBRC(size, (AVRAddrMode.$sbrc$)am);
        }
    }

    public static class SBRS_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.SBRS(size, (AVRAddrMode.$sbrs$)am);
        }
    }

    public static class SEC_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.SEC(size);
        }
    }

    public static class SEH_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.SEH(size);
        }
    }

    public static class SEI_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.SEI(size);
        }
    }

    public static class SEN_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.SEN(size);
        }
    }

    public static class SER_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.SER(size, (AVRAddrMode.$ser$)am);
        }
    }

    public static class SES_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.SES(size);
        }
    }

    public static class SET_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.SET(size);
        }
    }

    public static class SEV_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.SEV(size);
        }
    }

    public static class SEZ_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.SEZ(size);
        }
    }

    public static class SLEEP_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.SLEEP(size);
        }
    }

    public static class SPM_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.SPM(size);
        }
    }

    public static class STD_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.STD(size, (AVRAddrMode.$std$)am);
        }
    }

    public static class STS_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.STS(size, (AVRAddrMode.$sts$)am);
        }
    }

    public static class SUB_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.SUB(size, (AVRAddrMode.GPRGPR)am);
        }
    }

    public static class SUBI_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.SUBI(size, (AVRAddrMode.HGPRIMM8)am);
        }
    }

    public static class SWAP_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.SWAP(size, (AVRAddrMode.GPR)am);
        }
    }

    public static class TST_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.TST(size, (AVRAddrMode.$tst$)am);
        }
    }

    public static class WDR_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.WDR(size);
        }
    }

    public static class ELPM_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.ELPM(size, (AVRAddrMode.XLPM)am);
        }
    }

    public static class LPM_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.LPM(size, (AVRAddrMode.XLPM)am);
        }
    }

    public static class LD_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.LD(size, (AVRAddrMode.LD_ST)am);
        }
    }

    public static class ST_builder extends AVRInstrBuilder {

        public AVRInstr build(int size, AVRAddrMode am) {
            return new AVRInstr.ST(size, (AVRAddrMode.LD_ST)am);
        }
    }

    public static final AVRInstrBuilder ADC = add("adc", new ADC_builder());
    public static final AVRInstrBuilder ADD = add("add", new ADD_builder());
    public static final AVRInstrBuilder ADIW = add("adiw", new ADIW_builder());
    public static final AVRInstrBuilder AND = add("and", new AND_builder());
    public static final AVRInstrBuilder ANDI = add("andi", new ANDI_builder());
    public static final AVRInstrBuilder ASR = add("asr", new ASR_builder());
    public static final AVRInstrBuilder BCLR = add("bclr", new BCLR_builder());
    public static final AVRInstrBuilder BLD = add("bld", new BLD_builder());
    public static final AVRInstrBuilder BRBC = add("brbc", new BRBC_builder());
    public static final AVRInstrBuilder BRBS = add("brbs", new BRBS_builder());
    public static final AVRInstrBuilder BRCC = add("brcc", new BRCC_builder());
    public static final AVRInstrBuilder BRCS = add("brcs", new BRCS_builder());
    public static final AVRInstrBuilder BREAK = add("break", new BREAK_builder());
    public static final AVRInstrBuilder BREQ = add("breq", new BREQ_builder());
    public static final AVRInstrBuilder BRGE = add("brge", new BRGE_builder());
    public static final AVRInstrBuilder BRHC = add("brhc", new BRHC_builder());
    public static final AVRInstrBuilder BRHS = add("brhs", new BRHS_builder());
    public static final AVRInstrBuilder BRID = add("brid", new BRID_builder());
    public static final AVRInstrBuilder BRIE = add("brie", new BRIE_builder());
    public static final AVRInstrBuilder BRLO = add("brlo", new BRLO_builder());
    public static final AVRInstrBuilder BRLT = add("brlt", new BRLT_builder());
    public static final AVRInstrBuilder BRMI = add("brmi", new BRMI_builder());
    public static final AVRInstrBuilder BRNE = add("brne", new BRNE_builder());
    public static final AVRInstrBuilder BRPL = add("brpl", new BRPL_builder());
    public static final AVRInstrBuilder BRSH = add("brsh", new BRSH_builder());
    public static final AVRInstrBuilder BRTC = add("brtc", new BRTC_builder());
    public static final AVRInstrBuilder BRTS = add("brts", new BRTS_builder());
    public static final AVRInstrBuilder BRVC = add("brvc", new BRVC_builder());
    public static final AVRInstrBuilder BRVS = add("brvs", new BRVS_builder());
    public static final AVRInstrBuilder BSET = add("bset", new BSET_builder());
    public static final AVRInstrBuilder BST = add("bst", new BST_builder());
    public static final AVRInstrBuilder CALL = add("call", new CALL_builder());
    public static final AVRInstrBuilder CBI = add("cbi", new CBI_builder());
    public static final AVRInstrBuilder CBR = add("cbr", new CBR_builder());
    public static final AVRInstrBuilder CLC = add("clc", new CLC_builder());
    public static final AVRInstrBuilder CLH = add("clh", new CLH_builder());
    public static final AVRInstrBuilder CLI = add("cli", new CLI_builder());
    public static final AVRInstrBuilder CLN = add("cln", new CLN_builder());
    public static final AVRInstrBuilder CLR = add("clr", new CLR_builder());
    public static final AVRInstrBuilder CLS = add("cls", new CLS_builder());
    public static final AVRInstrBuilder CLT = add("clt", new CLT_builder());
    public static final AVRInstrBuilder CLV = add("clv", new CLV_builder());
    public static final AVRInstrBuilder CLZ = add("clz", new CLZ_builder());
    public static final AVRInstrBuilder COM = add("com", new COM_builder());
    public static final AVRInstrBuilder CP = add("cp", new CP_builder());
    public static final AVRInstrBuilder CPC = add("cpc", new CPC_builder());
    public static final AVRInstrBuilder CPI = add("cpi", new CPI_builder());
    public static final AVRInstrBuilder CPSE = add("cpse", new CPSE_builder());
    public static final AVRInstrBuilder DEC = add("dec", new DEC_builder());
    public static final AVRInstrBuilder EICALL = add("eicall", new EICALL_builder());
    public static final AVRInstrBuilder EIJMP = add("eijmp", new EIJMP_builder());
    public static final AVRInstrBuilder EOR = add("eor", new EOR_builder());
    public static final AVRInstrBuilder FMUL = add("fmul", new FMUL_builder());
    public static final AVRInstrBuilder FMULS = add("fmuls", new FMULS_builder());
    public static final AVRInstrBuilder FMULSU = add("fmulsu", new FMULSU_builder());
    public static final AVRInstrBuilder ICALL = add("icall", new ICALL_builder());
    public static final AVRInstrBuilder IJMP = add("ijmp", new IJMP_builder());
    public static final AVRInstrBuilder IN = add("in", new IN_builder());
    public static final AVRInstrBuilder INC = add("inc", new INC_builder());
    public static final AVRInstrBuilder JMP = add("jmp", new JMP_builder());
    public static final AVRInstrBuilder LDD = add("ldd", new LDD_builder());
    public static final AVRInstrBuilder LDI = add("ldi", new LDI_builder());
    public static final AVRInstrBuilder LDS = add("lds", new LDS_builder());
    public static final AVRInstrBuilder LSL = add("lsl", new LSL_builder());
    public static final AVRInstrBuilder LSR = add("lsr", new LSR_builder());
    public static final AVRInstrBuilder MOV = add("mov", new MOV_builder());
    public static final AVRInstrBuilder MOVW = add("movw", new MOVW_builder());
    public static final AVRInstrBuilder MUL = add("mul", new MUL_builder());
    public static final AVRInstrBuilder MULS = add("muls", new MULS_builder());
    public static final AVRInstrBuilder MULSU = add("mulsu", new MULSU_builder());
    public static final AVRInstrBuilder NEG = add("neg", new NEG_builder());
    public static final AVRInstrBuilder NOP = add("nop", new NOP_builder());
    public static final AVRInstrBuilder OR = add("or", new OR_builder());
    public static final AVRInstrBuilder ORI = add("ori", new ORI_builder());
    public static final AVRInstrBuilder OUT = add("out", new OUT_builder());
    public static final AVRInstrBuilder POP = add("pop", new POP_builder());
    public static final AVRInstrBuilder PUSH = add("push", new PUSH_builder());
    public static final AVRInstrBuilder RCALL = add("rcall", new RCALL_builder());
    public static final AVRInstrBuilder RET = add("ret", new RET_builder());
    public static final AVRInstrBuilder RETI = add("reti", new RETI_builder());
    public static final AVRInstrBuilder RJMP = add("rjmp", new RJMP_builder());
    public static final AVRInstrBuilder ROL = add("rol", new ROL_builder());
    public static final AVRInstrBuilder ROR = add("ror", new ROR_builder());
    public static final AVRInstrBuilder SBC = add("sbc", new SBC_builder());
    public static final AVRInstrBuilder SBCI = add("sbci", new SBCI_builder());
    public static final AVRInstrBuilder SBI = add("sbi", new SBI_builder());
    public static final AVRInstrBuilder SBIC = add("sbic", new SBIC_builder());
    public static final AVRInstrBuilder SBIS = add("sbis", new SBIS_builder());
    public static final AVRInstrBuilder SBIW = add("sbiw", new SBIW_builder());
    public static final AVRInstrBuilder SBR = add("sbr", new SBR_builder());
    public static final AVRInstrBuilder SBRC = add("sbrc", new SBRC_builder());
    public static final AVRInstrBuilder SBRS = add("sbrs", new SBRS_builder());
    public static final AVRInstrBuilder SEC = add("sec", new SEC_builder());
    public static final AVRInstrBuilder SEH = add("seh", new SEH_builder());
    public static final AVRInstrBuilder SEI = add("sei", new SEI_builder());
    public static final AVRInstrBuilder SEN = add("sen", new SEN_builder());
    public static final AVRInstrBuilder SER = add("ser", new SER_builder());
    public static final AVRInstrBuilder SES = add("ses", new SES_builder());
    public static final AVRInstrBuilder SET = add("set", new SET_builder());
    public static final AVRInstrBuilder SEV = add("sev", new SEV_builder());
    public static final AVRInstrBuilder SEZ = add("sez", new SEZ_builder());
    public static final AVRInstrBuilder SLEEP = add("sleep", new SLEEP_builder());
    public static final AVRInstrBuilder SPM = add("spm", new SPM_builder());
    public static final AVRInstrBuilder STD = add("std", new STD_builder());
    public static final AVRInstrBuilder STS = add("sts", new STS_builder());
    public static final AVRInstrBuilder SUB = add("sub", new SUB_builder());
    public static final AVRInstrBuilder SUBI = add("subi", new SUBI_builder());
    public static final AVRInstrBuilder SWAP = add("swap", new SWAP_builder());
    public static final AVRInstrBuilder TST = add("tst", new TST_builder());
    public static final AVRInstrBuilder WDR = add("wdr", new WDR_builder());
    public static final AVRInstrBuilder ELPM = add("elpm", new ELPM_builder());
    public static final AVRInstrBuilder LPM = add("lpm", new LPM_builder());
    public static final AVRInstrBuilder LD = add("ld", new LD_builder());
    public static final AVRInstrBuilder ST = add("st", new ST_builder());

    public static int checkValue(int val, int low, int high) {
        if (val < low || val > high) {
            throw new Error();
        }
        return val;
    }
}
