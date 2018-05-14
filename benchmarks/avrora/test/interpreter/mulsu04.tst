; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the MULSU (multiply signed and unsigned registers) instruction"
; @Result: "r16 = 31, r17 = 42, r0 = 22, r1 = 5, flags.z = 0, flags.c = 0"

start:
    ser r17
    out sreg, r17

    ldi r16, 31
    ldi r17, 42
    mulsu r16, r17

end:
    break
