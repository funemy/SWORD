; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the ORI (or register with immediate) instruction"
; @Result: "r16 = -113, flags.z = 0, flags.v = 0, flags.n = 1, flags.s = 1"

start:
    ser r17
    out sreg, r17

    ldi r16, 0b10001010
    ori r16, 0b10000101

end:
    break
