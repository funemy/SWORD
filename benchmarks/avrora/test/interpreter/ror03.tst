; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the ROR (rotate right through carry) instruction"
; @Result: "flags.s = 1, flags.v = 1, flags.n = 0, flags.z = 1, flags.c = 1, r16 = 0"

start:
    ldi r16, 0b00000001
    ror r16

end:
    break
