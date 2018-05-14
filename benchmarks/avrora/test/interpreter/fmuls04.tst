; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the FMULS (fractional multiply signed registers) instruction"
; @Result: "r16 = 31, r17 = 42, r0 = 44, r1 = 10, flags.z = 0, flags.c = 0"

start:
    ser r17
    out sreg, r17

    ldi r16, 31
    ldi r17, 42
    fmuls r16, r17

end:
    break
