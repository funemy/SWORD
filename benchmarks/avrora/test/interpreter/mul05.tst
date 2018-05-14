; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the MUL (multiply registers) instruction"
; @Result: "r16 = -1, r17 = -64, r0 = 64, r1 = -65, flags.z = 0, flags.c = 1"

start:
    ser r17
    out sreg, r17

    ldi r16, 0b11111111
    ldi r17, 0b11000000
    mul r16, r17

end:
    break
