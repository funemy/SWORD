package avrora.arch.avr;

/**
 * The <code>AVRInstrVisitor</code> interface allows user code that implements the interface to easily dispatch on the
 * type of an instruction without casting using the visitor pattern.
 */
public interface AVRInstrVisitor {

    public void visit(AVRInstr.ADC i);

    public void visit(AVRInstr.ADD i);

    public void visit(AVRInstr.ADIW i);

    public void visit(AVRInstr.AND i);

    public void visit(AVRInstr.ANDI i);

    public void visit(AVRInstr.ASR i);

    public void visit(AVRInstr.BCLR i);

    public void visit(AVRInstr.BLD i);

    public void visit(AVRInstr.BRBC i);

    public void visit(AVRInstr.BRBS i);

    public void visit(AVRInstr.BRCC i);

    public void visit(AVRInstr.BRCS i);

    public void visit(AVRInstr.BREAK i);

    public void visit(AVRInstr.BREQ i);

    public void visit(AVRInstr.BRGE i);

    public void visit(AVRInstr.BRHC i);

    public void visit(AVRInstr.BRHS i);

    public void visit(AVRInstr.BRID i);

    public void visit(AVRInstr.BRIE i);

    public void visit(AVRInstr.BRLO i);

    public void visit(AVRInstr.BRLT i);

    public void visit(AVRInstr.BRMI i);

    public void visit(AVRInstr.BRNE i);

    public void visit(AVRInstr.BRPL i);

    public void visit(AVRInstr.BRSH i);

    public void visit(AVRInstr.BRTC i);

    public void visit(AVRInstr.BRTS i);

    public void visit(AVRInstr.BRVC i);

    public void visit(AVRInstr.BRVS i);

    public void visit(AVRInstr.BSET i);

    public void visit(AVRInstr.BST i);

    public void visit(AVRInstr.CALL i);

    public void visit(AVRInstr.CBI i);

    public void visit(AVRInstr.CBR i);

    public void visit(AVRInstr.CLC i);

    public void visit(AVRInstr.CLH i);

    public void visit(AVRInstr.CLI i);

    public void visit(AVRInstr.CLN i);

    public void visit(AVRInstr.CLR i);

    public void visit(AVRInstr.CLS i);

    public void visit(AVRInstr.CLT i);

    public void visit(AVRInstr.CLV i);

    public void visit(AVRInstr.CLZ i);

    public void visit(AVRInstr.COM i);

    public void visit(AVRInstr.CP i);

    public void visit(AVRInstr.CPC i);

    public void visit(AVRInstr.CPI i);

    public void visit(AVRInstr.CPSE i);

    public void visit(AVRInstr.DEC i);

    public void visit(AVRInstr.EICALL i);

    public void visit(AVRInstr.EIJMP i);

    public void visit(AVRInstr.EOR i);

    public void visit(AVRInstr.FMUL i);

    public void visit(AVRInstr.FMULS i);

    public void visit(AVRInstr.FMULSU i);

    public void visit(AVRInstr.ICALL i);

    public void visit(AVRInstr.IJMP i);

    public void visit(AVRInstr.IN i);

    public void visit(AVRInstr.INC i);

    public void visit(AVRInstr.JMP i);

    public void visit(AVRInstr.LDD i);

    public void visit(AVRInstr.LDI i);

    public void visit(AVRInstr.LDS i);

    public void visit(AVRInstr.LSL i);

    public void visit(AVRInstr.LSR i);

    public void visit(AVRInstr.MOV i);

    public void visit(AVRInstr.MOVW i);

    public void visit(AVRInstr.MUL i);

    public void visit(AVRInstr.MULS i);

    public void visit(AVRInstr.MULSU i);

    public void visit(AVRInstr.NEG i);

    public void visit(AVRInstr.NOP i);

    public void visit(AVRInstr.OR i);

    public void visit(AVRInstr.ORI i);

    public void visit(AVRInstr.OUT i);

    public void visit(AVRInstr.POP i);

    public void visit(AVRInstr.PUSH i);

    public void visit(AVRInstr.RCALL i);

    public void visit(AVRInstr.RET i);

    public void visit(AVRInstr.RETI i);

    public void visit(AVRInstr.RJMP i);

    public void visit(AVRInstr.ROL i);

    public void visit(AVRInstr.ROR i);

    public void visit(AVRInstr.SBC i);

    public void visit(AVRInstr.SBCI i);

    public void visit(AVRInstr.SBI i);

    public void visit(AVRInstr.SBIC i);

    public void visit(AVRInstr.SBIS i);

    public void visit(AVRInstr.SBIW i);

    public void visit(AVRInstr.SBR i);

    public void visit(AVRInstr.SBRC i);

    public void visit(AVRInstr.SBRS i);

    public void visit(AVRInstr.SEC i);

    public void visit(AVRInstr.SEH i);

    public void visit(AVRInstr.SEI i);

    public void visit(AVRInstr.SEN i);

    public void visit(AVRInstr.SER i);

    public void visit(AVRInstr.SES i);

    public void visit(AVRInstr.SET i);

    public void visit(AVRInstr.SEV i);

    public void visit(AVRInstr.SEZ i);

    public void visit(AVRInstr.SLEEP i);

    public void visit(AVRInstr.SPM i);

    public void visit(AVRInstr.STD i);

    public void visit(AVRInstr.STS i);

    public void visit(AVRInstr.SUB i);

    public void visit(AVRInstr.SUBI i);

    public void visit(AVRInstr.SWAP i);

    public void visit(AVRInstr.TST i);

    public void visit(AVRInstr.WDR i);

    public void visit(AVRInstr.ELPM i);

    public void visit(AVRInstr.LPM i);

    public void visit(AVRInstr.LD i);

    public void visit(AVRInstr.ST i);
}
