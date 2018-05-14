; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the ADIW (add immediate to word) instruction"
; @Result: "flags.s=1, flags.v=0, flags.n=1, flags.z=0, flags.c=0, r26 = 0, r27 = -64"

start:
    ldi  r26, 0b11100000
    ldi  r27, 0b10111111
    adiw r26, 0b00100000

end:
    break
