; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the CPC (compare two registers with carry) instruction"
; @Result: "flags.h=0, flags.s=0, flags.v=1, flags.n=1, flags.z=0, flags.c=1, r16 = 64"

start:
    ldi r16, 0b01000000
    ldi r17, 0b11000000
    cpc r16, r17

end:
    break
