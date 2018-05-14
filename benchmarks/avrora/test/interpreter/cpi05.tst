; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the CPI (compare immediate with register) instruction"
; @Result: "flags.h=0, flags.s=0, flags.v=1, flags.n=1, flags.z=0, flags.c=1, r16 = 64"

start:
    ldi r16, 0b01000000
    cpi r16, 0b11000000

end:
    break
