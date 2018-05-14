; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the FMUL (fractional multiply registers) instruction"
; @Result: "r16 = 0, r17 = 42, r0 = 0, r1 = 0, flags.z = 1, flags.c = 0"

start:
    ser r17
    out sreg, r17

    ldi r16, 0
    ldi r17, 42
    fmul r16, r17

end:
    break
