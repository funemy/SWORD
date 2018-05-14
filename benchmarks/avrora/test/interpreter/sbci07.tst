; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the SBCI (subtract immediate from register with carry) instruction"
; @Result: "flags.h=1, flags.s=0, flags.v=0, flags.n=0, flags.z=0, flags.c=1, r16 = 127"

start:
    sec
    ldi r16, 0b00000000
    sbci r16, 0b10000000

end:
    break
