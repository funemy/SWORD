; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the MOV (move between registers) instruction"
; @Result: "r16 = 42"

start:
    ldi r16, 42
    mov r16, r16

end:
    break
