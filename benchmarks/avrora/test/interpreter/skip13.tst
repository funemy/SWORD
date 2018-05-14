; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the skip instructions for correct conditions and target"
; @Result: "r17 = 1, r18 = 0, r19 = 2"

start:
    ldi r17, 1
    sbrs r17, 0   ; should skip next instruction
    jmp end
    ldi r19, 2

end:
    break
