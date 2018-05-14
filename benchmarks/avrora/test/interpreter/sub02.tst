; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the SUB (subtract two registers) instruction"
; @Result: "flags.h=0, flags.s=0, flags.v=1, flags.n=1, flags.z=0, flags.c=1, r16 = -128"

start:
    ldi r16, 0b00000000
    ldi r17, 0b10000000
    sub r16, r17

end:
    break
