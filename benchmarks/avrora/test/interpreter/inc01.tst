; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the INC (increment register) instruction"
; @Result: "r16 = 16, flags.z = 0, flags.v = 0, flags.n = 0, flags.s = 0"

start:
    ser r17
    out sreg, r17

    ldi r16, 0b00001111
    inc r16

end:
    break
