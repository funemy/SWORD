; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the branch instructions for correct conditions and target"
; @Result: "@target = 10, r16 = 1, r17 = 2, r18 = 3, r19 = 4"

start:
    cln
    ses
    brge target     ; should not be taken

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
