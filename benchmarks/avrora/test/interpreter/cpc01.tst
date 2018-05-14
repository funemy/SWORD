; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the CPC (compare two registers with carry) instruction"
; @Result: "flags.h=0, flags.s=0, flags.v=0, flags.n=0, flags.z=1, flags.c=0, r16 = 0"

start:
    sez
    ldi r16, 0b00000000
    ldi r17, 0b00000000
    cpc r16, r17

end:
    break
