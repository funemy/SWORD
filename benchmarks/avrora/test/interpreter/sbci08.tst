; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the SBCI (subtract immediate from register with carry) instruction"
; @Result: "flags.h=1, flags.s=1, flags.v=0, flags.n=1, flags.z=0, flags.c=1, r16 = -1"

start:
    sec
    ldi r16, 0b10000000
    sbci r16, 0b10000000

end:
    break
