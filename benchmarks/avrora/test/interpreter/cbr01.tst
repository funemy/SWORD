; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the CBR (clear bits in register) instruction"
; @Result: "r16 = 0, flags.z = 1, flags.v = 0, flags.n = 0, flags.s = 0"

start:
    ser r17
    out sreg, r17

    ldi r16, 0b00001111
    cbr r16, 0b00001111

end:
    break
