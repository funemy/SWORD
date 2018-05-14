; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the ASR (arithmetic shift right) instruction"
; @Result: "r16 = -63, flags.c = 0, flags.s = 0, flags.n = 1"

start:
    ldi r16, 0b10000010
    asr r16

end:
    break
