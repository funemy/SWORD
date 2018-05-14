; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the ASR (arithmetic shift right) instruction"
; @Result: "r16 = 1, flags.c = 0, flags.s = 0"

start:
    ldi r16, 0b10
    asr r16

end:
    break
