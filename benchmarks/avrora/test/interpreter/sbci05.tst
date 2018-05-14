; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the SBCI (subtract immediate from register with carry) instruction"
; @Result: "flags.h=0, flags.s=0, flags.v=1, flags.n=1, flags.z=0, flags.c=1, r16 = -128"

start:
    ldi r16, 0b01000000
    sbci r16, 0b11000000

end:
    break
