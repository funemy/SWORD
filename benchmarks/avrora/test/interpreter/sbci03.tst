; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the SBCI (subtract immediate from register with carry) instruction"
; @Result: "flags.h=0, flags.s=0, flags.v=0, flags.n=0, flags.z=0, flags.c=0, r16 = 0"

start:
    ldi r16, 0b10000000
    sbci r16, 0b10000000

end:
    break
