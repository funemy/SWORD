; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the NEG (two's complement register) instruction"
; @Result: "r16 = -15, flags.z = 0, flags.v = 0, flags.n = 1, flags.s = 1, flags.c = 1"

start:
    ser r17
    out sreg, r17

    ldi r16, 15
    neg r16

end:
    break
