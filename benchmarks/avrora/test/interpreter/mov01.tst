; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the MOV (move between registers) instruction"
; @Result: "r15 = 21, r16 = 21"

start:
    ldi r16, 21
    mov r15, r16

end:
    break
