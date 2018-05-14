; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the LSL (logical shift left instruction"
; @Result: "flags.h = 0, flags.s = 0, flags.v = 0, flags.n = 0, flags.z = 0, flags.c = 0, r16 = 68"

start:
    ldi r16, 0b10001000
    lsr r16

end:
    break
