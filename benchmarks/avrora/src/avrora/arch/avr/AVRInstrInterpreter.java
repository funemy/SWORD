package avrora.arch.avr;

import cck.util.Util;

public abstract class AVRInstrInterpreter extends AVRState implements AVRInstrVisitor {
    boolean bit_get(int v, int bit) {
        return (v & (1 << bit)) != 0;
    }
    int bit_set(int v, int bit, boolean value) {
        if ( value ) return v | (1 << bit);
        else return v & ~(1 << bit);
    }
    int bit_update(int v, int mask, int e) {
        return (v & ~mask) | (e & mask);
    }
    int b2i(boolean v, int val) {
        if ( v ) return val;
        else return 0;
    }
    int $read_poly_int8(AVROperand o) {
        switch ( o.op_type ) {
            case AVROperand.R0_B_val: return $read_int8((AVROperand.R0_B)o);
            case AVROperand.op_GPR_val: return $read_int8((AVROperand.op_GPR)o);
        }
        throw Util.failure("invalid operand type in read");
    }
    int $read_poly_uint16(AVROperand o) {
        switch ( o.op_type ) {
            case AVROperand.AI_XYZ_val: return $read_uint16((AVROperand.AI_XYZ)o);
            case AVROperand.RZ_W_val: return $read_uint16((AVROperand.RZ_W)o);
            case AVROperand.PD_XYZ_val: return $read_uint16((AVROperand.PD_XYZ)o);
            case AVROperand.AI_RZ_W_val: return $read_uint16((AVROperand.AI_RZ_W)o);
            case AVROperand.XYZ_val: return $read_uint16((AVROperand.XYZ)o);
        }
        throw Util.failure("invalid operand type in read");
    }
    void $write_poly_int8(AVROperand o, int value) {
        switch ( o.op_type ) {
            case AVROperand.R0_B_val: $write_int8((AVROperand.R0_B)o, value); return;
            case AVROperand.op_GPR_val: $write_int8((AVROperand.op_GPR)o, value); return;
        }
        throw Util.failure("invalid operand type in write");
    }
    public int get_reg(int r) {
        return map_get(regs, r);
    }
    public int get_wreg(int r) {
        return uword(map_get(regs, r), map_get(regs, r + 1));
    }
    public void set_reg(int r, int v) {
        map_set(regs, r, v);
    }
    public void set_wreg(int r, int v) {
        map_set(regs, r, low(v));
        map_set(regs, r + 1, high(v));
    }
    protected abstract int popByte();
    protected abstract void pushByte(int b);
    protected abstract int extended(int addr);
    protected abstract void enableInterrupts();
    protected abstract void disableInterrupts();
    protected abstract void enterSleepMode();
    protected abstract void storeProgramMemory();
    protected abstract void stop();
    protected abstract void skip();
    protected abstract boolean getIORbit(int ior, int bit);
    protected abstract void setIORbit(int ior, int bit, boolean v);
    public int bit(boolean b) {
        if ( b ) {
            return 1;
        }
        else {
            return 0;
        }
    }
    public int performAddition(int r1, int r2, int carry) {
        int result = r1 + r2 + carry;
        int ral = r1 & 15;
        int rbl = r2 & 15;
        boolean Rd7 = bit_get(r1, 7);
        boolean Rr7 = bit_get(r2, 7);
        boolean R7 = bit_get(result, 7);
        H = bit_get(ral + rbl + carry, 4);
        C = bit_get(result, 8);
        N = R7;
        Z = low(result) == 0;
        V = Rd7 && Rr7 && !R7 || !Rd7 && !Rr7 && R7;
        S = N != V;
        return low(result);
    }
    public int performSubtraction(int r1, int r2, int carry) {
        int result = r1 - r2 - carry;
        boolean Rd7 = bit_get(r1, 7);
        boolean Rr7 = bit_get(r2, 7);
        boolean R7 = bit_get(result, 7);
        boolean Rd3 = bit_get(r1, 3);
        boolean Rr3 = bit_get(r2, 3);
        boolean R3 = bit_get(result, 3);
        H = !Rd3 && Rr3 || Rr3 && R3 || R3 && !Rd3;
        C = !Rd7 && Rr7 || Rr7 && R7 || R7 && !Rd7;
        N = R7;
        Z = low(result) == 0;
        V = Rd7 && !Rr7 && !R7 || !Rd7 && Rr7 && R7;
        S = N != V;
        return low(result);
    }
    public int performSubtractionPZ(int r1, int r2, int carry) {
        int result = r1 - r2 - carry;
        boolean Rd7 = bit_get(r1, 7);
        boolean Rr7 = bit_get(r2, 7);
        boolean R7 = bit_get(result, 7);
        boolean Rd3 = bit_get(r1, 3);
        boolean Rr3 = bit_get(r2, 3);
        boolean R3 = bit_get(result, 3);
        H = !Rd3 && Rr3 || Rr3 && R3 || R3 && !Rd3;
        C = !Rd7 && Rr7 || Rr7 && R7 || R7 && !Rd7;
        N = R7;
        Z = low(result) == 0 && Z;
        V = Rd7 && !Rr7 && !R7 || !Rd7 && Rr7 && R7;
        S = N != V;
        return low(result);
    }
    public int performLeftShift(int r1, boolean lowbit) {
        int result = r1 << 1;
        result = bit_update(result, 1, b2i(lowbit, 1));
        H = bit_get(result, 4);
        C = bit_get(result, 8);
        N = bit_get(result, 7);
        Z = low(result) == 0;
        V = N != C;
        S = N != V;
        return low(result);
    }
    public int performRightShift(int r1, boolean highbit) {
        int result = (r1 & 255) >> 1;
        result = bit_update(result, 128, b2i(highbit, 128));
        C = bit_get(r1, 0);
        N = highbit;
        Z = low(result) == 0;
        V = N != C;
        S = N != V;
        return low(result);
    }
    public int performOr(int r1, int r2) {
        int result = r1 | r2;
        N = bit_get(result, 7);
        Z = low(result) == 0;
        V = false;
        S = N != V;
        return low(result);
    }
    public int performAnd(int r1, int r2) {
        int result = r1 & r2;
        N = bit_get(result, 7);
        Z = low(result) == 0;
        V = false;
        S = N != V;
        return low(result);
    }
    public void relativeBranch(AVROperand.SREL target) {
        nextpc = relative(target.value);
        cycles = cycles + 1;
    }
    public int relative(int target) {
        return target * 2 + nextpc;
    }
    public int absolute(int target) {
        return target * 2;
    }
    public void pushPC(int npc) {
        npc = npc / 2;
        pushByte(low(npc));
        pushByte(high(npc));
    }
    public int popPC() {
        int high = popByte();
        int low = popByte();
        return uword(low, high) * 2;
    }
    public int low(int v) {
        return v << 24 >> 24;
    }
    public int high(int v) {
        return v >> 8 << 24 >> 24;
    }
    public int uword(int low, int high) {
        return (high << 8 | low) & 65535;
    }
    public int $read_int8(AVROperand.op_GPR _this) {
        return get_reg(_this.value.value);
    }
    public void $write_int8(AVROperand.op_GPR _this, int value) {
        set_reg(_this.value.value, value);
    }
    public int $read_int8(AVROperand.op_HGPR _this) {
        return get_reg(_this.value.value);
    }
    public void $write_int8(AVROperand.op_HGPR _this, int value) {
        set_reg(_this.value.value, value);
    }
    public int $read_int8(AVROperand.op_MGPR _this) {
        return get_reg(_this.value.value);
    }
    public void $write_int8(AVROperand.op_MGPR _this, int value) {
        set_reg(_this.value.value, value);
    }
    public int $read_uint16(AVROperand.op_YZ _this) {
        return get_wreg(_this.value.value);
    }
    public void $write_uint16(AVROperand.op_YZ _this, int value) {
        set_wreg(_this.value.value, value);
    }
    public int $read_uint16(AVROperand.op_EGPR _this) {
        return get_wreg(_this.value.value);
    }
    public void $write_uint16(AVROperand.op_EGPR _this, int value) {
        set_wreg(_this.value.value, value);
    }
    public int $read_uint16(AVROperand.op_RDL _this) {
        return get_wreg(_this.value.value);
    }
    public void $write_uint16(AVROperand.op_RDL _this, int value) {
        set_wreg(_this.value.value, value);
    }
    public int $read_int8(AVROperand.R0_B _this) {
        return get_reg(0);
    }
    public void $write_int8(AVROperand.R0_B _this, int value) {
        set_reg(0, value);
    }
    public int $read_uint16(AVROperand.RZ_W _this) {
        return get_wreg(30);
    }
    public int $read_uint16(AVROperand.AI_RZ_W _this) {
        int temp = get_wreg(30);
        set_wreg(30, temp + 1);
        return temp;
    }
    public int $read_uint16(AVROperand.XYZ _this) {
        return get_wreg(_this.value.value);
    }
    public int $read_uint16(AVROperand.AI_XYZ _this) {
        int tmp = get_wreg(_this.value.value);
        set_wreg(_this.value.value, tmp + 1);
        return tmp;
    }
    public int $read_uint16(AVROperand.PD_XYZ _this) {
        int tmp = get_wreg(_this.value.value) - 1 & 65535;
        set_wreg(_this.value.value, tmp);
        return tmp;
    }
    public void visit(AVRInstr.ADC i)  {
        $write_int8(i.rd, performAddition($read_int8(i.rd) & 255, $read_int8(i.rr) & 255, bit(C)));
    }
    public void visit(AVRInstr.ADD i)  {
        $write_int8(i.rd, performAddition($read_int8(i.rd) & 255, $read_int8(i.rr) & 255, 0));
    }
    public void visit(AVRInstr.ADIW i)  {
        int r1 = $read_uint16(i.rd);
        int result = r1 + i.imm.value;
        boolean R15 = bit_get(result, 15);
        boolean Rdh7 = bit_get(r1, 15);
        C = !R15 && Rdh7;
        N = R15;
        V = !Rdh7 && R15;
        Z = (result & 65535) == 0;
        S = N != V;
        $write_uint16(i.rd, result);
    }
    public void visit(AVRInstr.AND i)  {
        $write_int8(i.rd, performAnd($read_int8(i.rd), $read_int8(i.rr)));
    }
    public void visit(AVRInstr.ANDI i)  {
        $write_int8(i.rd, performAnd($read_int8(i.rd), i.imm.value));
    }
    public void visit(AVRInstr.ASR i)  {
        int r1 = $read_int8(i.rd);
        $write_int8(i.rd, performRightShift(r1, bit_get(r1, 7)));
    }
    public void visit(AVRInstr.BCLR i)  {
        setIORbit(SREG, i.bit.value, false);
    }
    public void visit(AVRInstr.BLD i)  {
        int val = $read_int8(i.rr);
        val = bit_set(val, i.bit.value, T);
        $write_int8(i.rr, val);
    }
    public void visit(AVRInstr.BRBC i)  {
        if ( !getIORbit(SREG, i.bit.value) ) {
            relativeBranch(i.target);
        }
    }
    public void visit(AVRInstr.BRBS i)  {
        if ( getIORbit(SREG, i.bit.value) ) {
            relativeBranch(i.target);
        }
    }
    public void visit(AVRInstr.BRCC i)  {
        if ( !C ) {
            relativeBranch(i.target);
        }
    }
    public void visit(AVRInstr.BRCS i)  {
        if ( C ) {
            relativeBranch(i.target);
        }
    }
    public void visit(AVRInstr.BREAK i)  {
        stop();
    }
    public void visit(AVRInstr.BREQ i)  {
        if ( Z ) {
            relativeBranch(i.target);
        }
    }
    public void visit(AVRInstr.BRGE i)  {
        if ( !S ) {
            relativeBranch(i.target);
        }
    }
    public void visit(AVRInstr.BRHC i)  {
        if ( !H ) {
            relativeBranch(i.target);
        }
    }
    public void visit(AVRInstr.BRHS i)  {
        if ( H ) {
            relativeBranch(i.target);
        }
    }
    public void visit(AVRInstr.BRID i)  {
        if ( !I ) {
            relativeBranch(i.target);
        }
    }
    public void visit(AVRInstr.BRIE i)  {
        if ( I ) {
            relativeBranch(i.target);
        }
    }
    public void visit(AVRInstr.BRLO i)  {
        if ( C ) {
            relativeBranch(i.target);
        }
    }
    public void visit(AVRInstr.BRLT i)  {
        if ( S ) {
            relativeBranch(i.target);
        }
    }
    public void visit(AVRInstr.BRMI i)  {
        if ( N ) {
            relativeBranch(i.target);
        }
    }
    public void visit(AVRInstr.BRNE i)  {
        if ( !Z ) {
            relativeBranch(i.target);
        }
    }
    public void visit(AVRInstr.BRPL i)  {
        if ( !N ) {
            relativeBranch(i.target);
        }
    }
    public void visit(AVRInstr.BRSH i)  {
        if ( !C ) {
            relativeBranch(i.target);
        }
    }
    public void visit(AVRInstr.BRTC i)  {
        if ( !T ) {
            relativeBranch(i.target);
        }
    }
    public void visit(AVRInstr.BRTS i)  {
        if ( T ) {
            relativeBranch(i.target);
        }
    }
    public void visit(AVRInstr.BRVC i)  {
        if ( !V ) {
            relativeBranch(i.target);
        }
    }
    public void visit(AVRInstr.BRVS i)  {
        if ( V ) {
            relativeBranch(i.target);
        }
    }
    public void visit(AVRInstr.BSET i)  {
        setIORbit(SREG, i.bit.value, true);
    }
    public void visit(AVRInstr.BST i)  {
        T = bit_get($read_int8(i.rr), i.bit.value);
    }
    public void visit(AVRInstr.CALL i)  {
        pushPC(nextpc);
        nextpc = absolute(i.target.value);
    }
    public void visit(AVRInstr.CBI i)  {
        setIORbit(i.ior.value, i.bit.value, false);
    }
    public void visit(AVRInstr.CBR i)  {
        $write_int8(i.rd, performAnd($read_int8(i.rd), ~i.imm.value));
    }
    public void visit(AVRInstr.CLC i)  {
        C = false;
    }
    public void visit(AVRInstr.CLH i)  {
        H = false;
    }
    public void visit(AVRInstr.CLI i)  {
        disableInterrupts();
    }
    public void visit(AVRInstr.CLN i)  {
        N = false;
    }
    public void visit(AVRInstr.CLR i)  {
        S = false;
        V = false;
        N = false;
        Z = true;
        $write_int8(i.rd, low(0));
    }
    public void visit(AVRInstr.CLS i)  {
        S = false;
    }
    public void visit(AVRInstr.CLT i)  {
        T = false;
    }
    public void visit(AVRInstr.CLV i)  {
        V = false;
    }
    public void visit(AVRInstr.CLZ i)  {
        Z = false;
    }
    public void visit(AVRInstr.COM i)  {
        int result = 255 - $read_int8(i.rd);
        C = true;
        N = bit_get(result, 7);
        Z = low(result) == 0;
        V = false;
        S = N != V;
        $write_int8(i.rd, low(result));
    }
    public void visit(AVRInstr.CP i)  {
        performSubtraction($read_int8(i.rd), $read_int8(i.rr), 0);
    }
    public void visit(AVRInstr.CPC i)  {
        performSubtractionPZ($read_int8(i.rd), $read_int8(i.rr), bit(C));
    }
    public void visit(AVRInstr.CPI i)  {
        performSubtraction($read_int8(i.rd), i.imm.value, 0);
    }
    public void visit(AVRInstr.CPSE i)  {
        int r1 = $read_int8(i.rd);
        int r2 = $read_int8(i.rr);
        performSubtraction(r1, r2, 0);
        if ( r1 == r2 ) {
            skip();
        }
    }
    public void visit(AVRInstr.DEC i)  {
        int r1 = $read_int8(i.rd) & 255;
        int result = low(r1 - 1);
        N = bit_get(result, 7);
        Z = result == 0;
        V = r1 == 128;
        S = N != V;
        $write_int8(i.rd, result);
    }
    public void visit(AVRInstr.EICALL i)  {
    }
    public void visit(AVRInstr.EIJMP i)  {
    }
    public void visit(AVRInstr.EOR i)  {
        int result = $read_int8(i.rd) ^ $read_int8(i.rr);
        N = bit_get(result, 7);
        Z = result == 0;
        V = false;
        S = N != V;
        $write_int8(i.rd, result);
    }
    public void visit(AVRInstr.FMUL i)  {
        int result = ($read_int8(i.rd) & 255) * ($read_int8(i.rr) & 255) << 1;
        Z = (result & 65535) == 0;
        C = bit_get(result, 16);
        set_wreg(0, result);
    }
    public void visit(AVRInstr.FMULS i)  {
        int result = $read_int8(i.rd) * $read_int8(i.rr) << 1;
        Z = (result & 65535) == 0;
        C = bit_get(result, 16);
        set_wreg(0, result);
    }
    public void visit(AVRInstr.FMULSU i)  {
        int result = $read_int8(i.rd) * ($read_int8(i.rr) & 255) << 1;
        Z = (result & 65535) == 0;
        C = bit_get(result, 16);
        set_wreg(0, result);
    }
    public void visit(AVRInstr.ICALL i)  {
        pushPC(nextpc);
        nextpc = absolute(get_wreg(30));
    }
    public void visit(AVRInstr.IJMP i)  {
        nextpc = absolute(get_wreg(30));
    }
    public void visit(AVRInstr.IN i)  {
        $write_int8(i.rd, map_get(ioregs, i.imm.value));
    }
    public void visit(AVRInstr.INC i)  {
        int r1 = $read_int8(i.rd) & 255;
        int result = low(r1 + 1);
        N = bit_get(result, 7);
        Z = result == 0;
        V = r1 == 127;
        S = N != V;
        $write_int8(i.rd, result);
    }
    public void visit(AVRInstr.JMP i)  {
        nextpc = absolute(i.target.value);
    }
    public void visit(AVRInstr.LDD i)  {
        $write_int8(i.rd, map_get(sram, $read_uint16(i.ar) + i.imm.value));
    }
    public void visit(AVRInstr.LDI i)  {
        $write_int8(i.rd, i.imm.value);
    }
    public void visit(AVRInstr.LDS i)  {
        $write_int8(i.rd, map_get(sram, i.addr.value));
    }
    public void visit(AVRInstr.LSL i)  {
        $write_int8(i.rd, performLeftShift($read_int8(i.rd), false));
    }
    public void visit(AVRInstr.LSR i)  {
        $write_int8(i.rd, performRightShift($read_int8(i.rd), false));
    }
    public void visit(AVRInstr.MOV i)  {
        $write_int8(i.rd, $read_int8(i.rr));
    }
    public void visit(AVRInstr.MOVW i)  {
        $write_uint16(i.rd, $read_uint16(i.rr));
    }
    public void visit(AVRInstr.MUL i)  {
        int result = ($read_int8(i.rd) & 255) * ($read_int8(i.rr) & 255);
        C = bit_get(result, 15);
        Z = (result & 65535) == 0;
        set_wreg(0, result);
    }
    public void visit(AVRInstr.MULS i)  {
        int result = $read_int8(i.rd) * $read_int8(i.rr);
        C = bit_get(result, 15);
        Z = (result & 65535) == 0;
        set_wreg(0, result);
    }
    public void visit(AVRInstr.MULSU i)  {
        int result = $read_int8(i.rd) * ($read_int8(i.rr) & 255);
        C = bit_get(result, 15);
        Z = (result & 65535) == 0;
        set_wreg(0, result);
    }
    public void visit(AVRInstr.NEG i)  {
        $write_int8(i.rd, performSubtraction(0, $read_int8(i.rd), 0));
    }
    public void visit(AVRInstr.NOP i)  {
    }
    public void visit(AVRInstr.OR i)  {
        $write_int8(i.rd, performOr($read_int8(i.rd), $read_int8(i.rr)));
    }
    public void visit(AVRInstr.ORI i)  {
        $write_int8(i.rd, performOr($read_int8(i.rd), i.imm.value));
    }
    public void visit(AVRInstr.OUT i)  {
        map_set(ioregs, i.ior.value, $read_int8(i.rr));
    }
    public void visit(AVRInstr.POP i)  {
        $write_int8(i.rd, popByte());
    }
    public void visit(AVRInstr.PUSH i)  {
        pushByte($read_int8(i.rd));
    }
    public void visit(AVRInstr.RCALL i)  {
        pushPC(nextpc);
        nextpc = relative(i.target.value);
    }
    public void visit(AVRInstr.RET i)  {
        nextpc = popPC();
    }
    public void visit(AVRInstr.RETI i)  {
        nextpc = popPC();
        enableInterrupts();
        justReturnedFromInterrupt = true;
    }
    public void visit(AVRInstr.RJMP i)  {
        nextpc = relative(i.target.value);
    }
    public void visit(AVRInstr.ROL i)  {
        $write_int8(i.rd, performLeftShift($read_int8(i.rd) & 255, C));
    }
    public void visit(AVRInstr.ROR i)  {
        $write_int8(i.rd, performRightShift($read_int8(i.rd), C));
    }
    public void visit(AVRInstr.SBC i)  {
        $write_int8(i.rd, performSubtractionPZ($read_int8(i.rd), $read_int8(i.rr), bit(C)));
    }
    public void visit(AVRInstr.SBCI i)  {
        $write_int8(i.rd, performSubtractionPZ($read_int8(i.rd), i.imm.value, bit(C)));
    }
    public void visit(AVRInstr.SBI i)  {
        setIORbit(i.ior.value, i.bit.value, true);
    }
    public void visit(AVRInstr.SBIC i)  {
        if ( !getIORbit(i.ior.value, i.bit.value) ) {
            skip();
        }
    }
    public void visit(AVRInstr.SBIS i)  {
        if ( getIORbit(i.ior.value, i.bit.value) ) {
            skip();
        }
    }
    public void visit(AVRInstr.SBIW i)  {
        int val = $read_uint16(i.rd);
        int result = val - i.imm.value;
        boolean Rdh7 = bit_get(val, 15);
        boolean R15 = bit_get(result, 15);
        V = Rdh7 && !R15;
        N = R15;
        Z = (result & 65535) == 0;
        C = R15 && !Rdh7;
        S = N != V;
        $write_uint16(i.rd, result);
    }
    public void visit(AVRInstr.SBR i)  {
        $write_int8(i.rd, performOr($read_int8(i.rd), i.imm.value));
    }
    public void visit(AVRInstr.SBRC i)  {
        if ( !bit_get($read_int8(i.rr), i.bit.value) ) {
            skip();
        }
    }
    public void visit(AVRInstr.SBRS i)  {
        if ( bit_get($read_int8(i.rr), i.bit.value) ) {
            skip();
        }
    }
    public void visit(AVRInstr.SEC i)  {
        C = true;
    }
    public void visit(AVRInstr.SEH i)  {
        H = true;
    }
    public void visit(AVRInstr.SEI i)  {
        enableInterrupts();
    }
    public void visit(AVRInstr.SEN i)  {
        N = true;
    }
    public void visit(AVRInstr.SER i)  {
        $write_int8(i.rd, low(255));
    }
    public void visit(AVRInstr.SES i)  {
        S = true;
    }
    public void visit(AVRInstr.SET i)  {
        T = true;
    }
    public void visit(AVRInstr.SEV i)  {
        V = true;
    }
    public void visit(AVRInstr.SEZ i)  {
        Z = true;
    }
    public void visit(AVRInstr.SLEEP i)  {
        enterSleepMode();
    }
    public void visit(AVRInstr.SPM i)  {
        storeProgramMemory();
    }
    public void visit(AVRInstr.STD i)  {
        map_set(sram, $read_uint16(i.ar) + i.imm.value, $read_int8(i.rr));
    }
    public void visit(AVRInstr.STS i)  {
        map_set(sram, i.addr.value, $read_int8(i.rr));
    }
    public void visit(AVRInstr.SUB i)  {
        $write_int8(i.rd, performSubtraction($read_int8(i.rd), $read_int8(i.rr), 0));
    }
    public void visit(AVRInstr.SUBI i)  {
        $write_int8(i.rd, performSubtraction($read_int8(i.rd), i.imm.value, 0));
    }
    public void visit(AVRInstr.SWAP i)  {
        int val = $read_int8(i.rd) & 255;
        int result = 0;
        result = bit_update(result, 15, val << 4 & 15);
        result = bit_update(result, 240, val >> 4 & 240);
        $write_int8(i.rd, low(result));
    }
    public void visit(AVRInstr.TST i)  {
        int r1 = $read_int8(i.rd);
        V = false;
        Z = low(r1) == 0;
        N = bit_get(r1, 7);
        S = N != V;
    }
    public void visit(AVRInstr.WDR i)  {
    }
    public void visit(AVRInstr.ELPM i)  {
        int addr = extended($read_poly_uint16(i.source));
        $write_poly_int8(i.dest, map_get(flash, addr));
    }
    public void visit(AVRInstr.LPM i)  {
        int addr = $read_poly_uint16(i.source);
        $write_poly_int8(i.dest, map_get(flash, addr));
    }
    public void visit(AVRInstr.LD i)  {
        int addr = $read_poly_uint16(i.ar);
        $write_poly_int8(i.rd, map_get(sram, addr));
    }
    public void visit(AVRInstr.ST i)  {
        int addr = $read_poly_uint16(i.ar);
        int val = $read_poly_int8(i.rd);
        map_set(sram, addr, val);
    }
}
