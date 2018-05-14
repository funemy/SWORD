; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the AND (and two registers) instruction"
; @Result: "r16 = 15, flags.z = 0, flags.v = 0, flags.n = 0, flags.s = 0"

start:
    ser r17
    out sreg, r17

    ldi r16, 15
    and r16, r16

end:
    break
