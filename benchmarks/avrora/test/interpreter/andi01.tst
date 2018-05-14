; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the ANDI (and register with immediate) instruction"
; @Result: "r16 = 15, flags.z = 0, flags.v = 0, flags.n = 0, flags.s = 0"

start:
    ser r17
    out sreg, r17

    ldi r16, 15
    andi r16, 15

end:
    break
