; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the skip instructions for correct conditions and target"
; @Result: "r16 = 42, r17 = 42, r18 = 0, r19 = 2"

start:
    ldi r16, 42
    ldi r17, 42
    cpse r16, r17   ; should skip next instruction
    jmp end
    ldi r19, 2

end:
    break
