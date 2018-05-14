; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the branch instructions for correct conditions and target"
; @Result: "@target = 12, r16 = 0, r17 = 0, r18 = 3, sp = 253, sram[sp+2] = 4, sram[sp+1] = 0"

setstack:
    ldi r21, 255
    out spl, r21

start:
    call target

return:
    ldi r16, 1

before:
    ldi r17, 2
target:
    ldi r18, 3

end:
    break
