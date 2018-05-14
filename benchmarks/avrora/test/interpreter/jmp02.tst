; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the branch instructions for correct conditions and target"
; @Result: "@target = 8, r16 = 0, r17 = 0, r18 = 3"

start:
    jmp target

fall:
    ldi r16, 1

before:
    ldi r17, 2
target:
    ldi r18, 3

end:
    break
