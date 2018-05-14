; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the FMUL (fractional multiply registers) instruction"
; @Result: "r16 = 2, r17 = 42, r0 = -88, r1 = 0, flags.z = 0, flags.c = 0"

start:
    ser r17
    out sreg, r17

    ldi r16, 2
    ldi r17, 42
    fmul r16, r17

end:
    break
