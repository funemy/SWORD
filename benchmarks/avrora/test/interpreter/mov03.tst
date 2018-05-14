; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the MOV (move between registers) instruction"
; @Result: "r0 = 42, r31 = 42"

start:
    ldi r31, 42
    mov r0, r31

end:
    break
