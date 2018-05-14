; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the INC (increment register) instruction"
; @Result: "r16 = -128, flags.z = 0, flags.v = 1, flags.n = 1, flags.s = 0"

start:
    ser r17
    out sreg, r17

    ldi r16, 0b01111111
    inc r16

end:
    break
