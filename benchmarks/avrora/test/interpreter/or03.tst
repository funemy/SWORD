; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the OR (or two registers) instruction"
; @Result: "r16 = -113, flags.z = 0, flags.v = 0, flags.n = 1, flags.s = 1"

start:
    ser r17
    out sreg, r17

    ldi r16, 0b10001010
    ldi r17, 0b10000101
    or r16, r17

end:
    break
