; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the ROR (rotate right through carry) instruction"
; @Result: "flags.s = 1, flags.v = 1, flags.n = 0, flags.z = 0, flags.c = 1, r16 = 64"

start:
    ldi r16, 0b10000001
    ror r16

end:
    break
