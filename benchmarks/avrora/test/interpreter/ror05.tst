; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the ROR (rotate right through carry) instruction"
; @Result: "flags.h = 0, flags.s = 0, flags.v = 1, flags.n = 1, flags.z = 0, flags.c = 0, r16 = -60"

start:
    sec
    ldi r16, 0b10001000
    ror r16

end:
    break
