; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the ASR (arithmetic shift right) instruction"
; @Result: "r16 = 0, flags.c = 1, flags.s = 1"

start:
    ldi r16, 0b1
    asr r16

end:
    break
