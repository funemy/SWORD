; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the ADIW (add immediate to word) instruction"
; @Result: "flags.s=0, flags.v=0, flags.n=0, flags.z=0, flags.c=0, r26 = 31, r27 = 1"

start:
    ldi  r26, 0b11100000
    ldi  r27, 0b00000000
    adiw r26, 0b00111111

end:
    break
