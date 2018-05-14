; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the branch instructions for correct conditions and target"
; @Result: "@target = 8, r16 = 0, r17 = 0, r18 = 3, r19 = 4"

start:
    cli
    brbc 7, target     ; should be taken

fall:
    ldi r16, 1

before:
    ldi r17, 2
target:
    ldi r18, 3
after:
    ldi r19, 4

end:
    break
