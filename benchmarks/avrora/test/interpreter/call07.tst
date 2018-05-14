; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the branch instructions for correct conditions and target"
; @Result: "@target = 14, r16 = 0, r17 = 0, r18 = 3, r19 = 4, sp = 255"

setstack:
    ldi r21, 255
    out spl, r21

start:
    rcall target
    ldi r19, 4
    break

return:
    ldi r16, 1

before:
    ldi r17, 2
target:
    ldi r18, 3
    ret

end:
    break
