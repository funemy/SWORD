; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the skip instructions for correct conditions and target"
; @Result: "sram[59] = 1, r18 = 0, r19 = 2"

start:
    ldi r17, 1
    out 0x1b, r17  ; write to PORTA
    nop
    sbis 0x1b, 0   ; should not skip next instruction
    ldi r18, 1
    ldi r19, 2

end:
    break
