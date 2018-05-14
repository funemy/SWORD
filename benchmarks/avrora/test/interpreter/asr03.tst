; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the ASR (arithmetic shift right) instruction"
; @Result: "r16 = -64, flags.c = 1, flags.s = 1, flags.n = 1"

start:
    ldi r16, 0b10000001
    asr r16

end:
    break
