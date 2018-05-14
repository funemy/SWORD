; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the COM (one's complement register) instruction"
; @Result: "r16 = -16, flags.z = 0, flags.v = 0, flags.n = 1, flags.s = 1, flags.c = 1"

start:
    ser r17
    out sreg, r17

    ldi r16, 15
    com r16

end:
    break
