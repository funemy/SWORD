; @Harness: simplifier
; @Purpose: "Test variants of all instructions"
; @Result: PASS

        adc    r0,  r0
        add    r0,  r0
        adiw   r24, 0
        and    r0,  r0
        andi   r16, 0
        asr    r0
        bclr   0
        bld    r0,  0
        brbc   0,   0
        brbs   0,   0
        brcc   0
        brcs   0
        break
        breq   0
        brge   0
        brhc   0
        brhs   0
        brid   0
        brie   0
        brlo   0
        brlt   0
        brmi   0
        brne   0
        brpl   0
        brsh   0
        brtc   0
        brts   0
        brvc   0
        brvs   0
        bset   0
        bst    r0,  0
        call   0
        cbi    0,   0
        cbr    r16, 0
        clc
        clh
        cli
        cln
        clr    r0
        cls
        clt
        clv
        clz    
        com    r0
        cp     r0,  r0
        cpc    r0,  r0
        cpi    r16, 0
        cpse   r0,  r0
        dec    r0
        eicall
        eijmp
        elpm
        elpm   r0,  Z
        elpm   r0,  Z+
        eor    r0,  r0
        fmul   r16, r16
        fmuls  r16, r16
        fmulsu r16, r16
        icall
        ijmp
        in     r0,  0
        inc    r0
        jmp    0
        ld     r0,  X
        ldd    r0,  Y+0
        ldi    r16, 0
        ld     r0,  -X
        ld     r0,  X+
        lds    r0,  0
        lpm
        lpm    r0,  Z
        lpm    r0,  Z+
        lsl    r0
        lsr    r0
        mov    r0,  r0
        movw   r0,  r0
        mul    r0,  r0
        muls   r16, r16
        mulsu  r16, r16
        neg    r0
        nop
        or     r0,  r0
        ori    r16, 0
        out    0,   r0
        pop    r0
        push   r0
        rcall  0
        ret
        reti
        rjmp   0
        rol    r0
        ror    r0
        sbc    r0,  r0
        sbci   r16, 0
        sbi    0,   0
        sbic   0,   0
        sbis   0,   0
        sbiw   r24, 0
        sbr    r16, 0
        sbrc   r0,  0
        sbrs   r0,  0
        sec
        seh
        sei
        sen
        ser    r0
        ses
        set
        sev
        sez
        sleep
        spm
        st     X,   r0
        std    Y+0, r0
        st     -X,  r0
        st     X+,  r0
        sts    0,   r0
        sub    r0,  r0
        subi   r16, 0
        swap   r0
        tst    r0
        wdr
