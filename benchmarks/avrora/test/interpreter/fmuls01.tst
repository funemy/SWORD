; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the FMULS (fractional multiply signed registers) instruction"
; @Result: "r16 = 0, r17 = 0, r0 = 0, r1 = 0, flags.z = 1, flags.c = 0"

start:
    ser r17
    out sreg, r17

    ldi r16, 0
    ldi r17, 0
    fmuls r16, r17

end:
    break
