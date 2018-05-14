; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the skip instructions for correct conditions and target"
; @Result: "r17 = 2, r18 = 0, r19 = 2"

start:
    ldi r17, 2
    sbrc r17, 0   ; should skip next instruction
    ldi r18, 1
    ldi r19, 2

end:
    break
