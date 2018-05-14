; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the LSL (logical shift left instruction"
; @Result: "flags.h = 1, flags.s = 0, flags.v = 1, flags.n = 1, flags.z = 0, flags.c = 0, r16 = -112"

start:
    ldi r16, 0b01001000
    lsl r16

end:
    break
