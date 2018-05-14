; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the branch instructions for correct conditions and target"
; @Result: "@target = 12, r17 = 0, r18 = 3, r19 = 4, sp = 255"

setstack:
    ldi r21, 255
    out spl, r21

genesis:
    jmp start
    break

before:
    ldi r17, 2
target:
    ldi r18, 3
    ret
    break

start:
    rcall target
    ldi r19, 4

end:
    break
