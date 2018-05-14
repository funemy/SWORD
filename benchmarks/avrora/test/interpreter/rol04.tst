; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the ROL (rotate left through carry) instruction"
; @Result: "flags.h = 0, flags.s = 1, flags.v = 1, flags.n = 0, flags.z = 0, flags.c = 1, r16 = 1"

start:
    sec
    ldi r16, 0b10000000
    rol r16

end:
    break
