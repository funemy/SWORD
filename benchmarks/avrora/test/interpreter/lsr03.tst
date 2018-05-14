; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the LSR (logical shift right) instruction"
; @Result: "flags.s = 1, flags.v = 1, flags.n = 0, flags.z = 1, flags.c = 1, r16 = 0"

start:
    ldi r16, 0b00000001
    lsr r16

end:
    break
