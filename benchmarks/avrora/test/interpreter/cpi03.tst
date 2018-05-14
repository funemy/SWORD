; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the CPI (compare immediate with register) instruction"
; @Result: "flags.h=0, flags.s=0, flags.v=0, flags.n=0, flags.z=1, flags.c=0, r16 = -128"

start:
    ldi r16, 0b10000000
    cpi r16, 0b10000000

end:
    break
