; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the ROR (rotate right through carry) instruction"
; @Result: "flags.s = 1, flags.v = 0, flags.n = 1, flags.z = 0, flags.c = 1, r16 = -64"

start:
    sec
    ldi r16, 0b10000001
    ror r16

end:
    break
