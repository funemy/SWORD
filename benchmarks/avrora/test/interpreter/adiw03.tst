; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the ADIW (add immediate to word) instruction"
; @Result: "flags.s=0, flags.v=0, flags.n=0, flags.z=1, flags.c=1, r26 = 0, r27 = 0"

start:
    ldi  r26, 0b11100000
    ldi  r27, 0b11111111
    adiw r26, 0b00100000

end:
    break
