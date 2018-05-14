; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the SUBI (subtract immediate from register) instruction"
; @Result: "flags.h=1, flags.s=0, flags.v=0, flags.n=0, flags.z=0, flags.c=0, r16 = 8"

start:
    ldi r16, 0b00010000
    subi r16, 0b00001000

end:
    break
