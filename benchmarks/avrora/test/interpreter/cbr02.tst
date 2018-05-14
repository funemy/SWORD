; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the CBR (clear bits in register) instruction"
; @Result: "r16 = -16, flags.z = 0, flags.v = 0, flags.n = 1, flags.s = 1"

start:
    ser r17
    out sreg, r17

    ldi r16, 0b11111111
    cbr r16, 0b00001111

end:
    break
