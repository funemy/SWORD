; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the DEC (decrement register) instruction"
; @Result: "r16 = 0, flags.z = 1, flags.v = 0, flags.n = 0, flags.s = 0"

start:
    ser r17
    out sreg, r17

    ldi r16, 0b00000001
    dec r16

end:
    break
