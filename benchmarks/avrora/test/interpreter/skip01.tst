; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the skip instructions for correct conditions and target"
; @Result: "r16 = 41, r17 = 42, r18 = 1, r19 = 2"

start:
    ldi r16, 41
    ldi r17, 42
    cpse r16, r17   ; should not skip next instruction
    ldi r18, 1
    ldi r19, 2

end:
    break
