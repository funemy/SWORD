; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the DEC (decrement register) instruction"
; @Result: "r16 = 127, flags.z = 0, flags.v = 1, flags.n = 0, flags.s = 1"

start:
    ser r17
    out sreg, r17

    ldi r16, 0b10000000
    dec r16

end:
    break
