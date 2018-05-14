; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the FMUL (fractional multiply registers) instruction"
; @Result: "r16 = -1, r17 = -64, r0 = -128, r1 = 126, flags.z = 0, flags.c = 1"

start:
    ser r17
    out sreg, r17

    ldi r16, 0b11111111
    ldi r17, 0b11000000
    fmul r16, r17

end:
    break
