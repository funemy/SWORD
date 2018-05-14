; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the ROR (rotate right through carry) instruction"
; @Result: "flags.s = 1, flags.v = 0, flags.n = 1, flags.z = 0, flags.c = 1, r16 = -128"

start:
    sec
    ldi r16, 0b00000001
    ror r16

end:
    break
