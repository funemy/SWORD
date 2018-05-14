; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the SUB (subtract two registers) instruction"
; @Result: "flags.h=1, flags.s=0, flags.v=0, flags.n=0, flags.z=0, flags.c=0, r16 = 8"

start:
    ldi r16, 0b00010000
    ldi r17, 0b00001000
    sub r16, r17

end:
    break
