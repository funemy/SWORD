; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the SUBI (subtract immediate from register) instruction"
; @Result: "flags.h=0, flags.s=0, flags.v=1, flags.n=1, flags.z=0, flags.c=1, r16 = -128"

start:
    ldi r16, 0b00000000
    subi r16, 0b10000000

end:
    break
