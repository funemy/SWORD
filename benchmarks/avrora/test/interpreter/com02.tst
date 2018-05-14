; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the COM (one's complement register) instruction"
; @Result: "r16 = 0, flags.z = 1, flags.v = 0, flags.n = 0, flags.s = 0, flags.c = 1"

start:
    ser r17
    out sreg, r17

    ldi r16, 0xff
    com r16

end:
    break
