; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the NEG (two's complement register) instruction"
; @Result: "r16 = 0, flags.z = 1, flags.v = 0, flags.n = 0, flags.s = 0, flags.c = 0"

start:
    ser r17
    out sreg, r17

    ldi r16, 0
    neg r16

end:
    break
