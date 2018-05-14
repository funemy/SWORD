; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the branch instructions for correct conditions and target"
; @Result: "@target = 12, r17 = 0, r18 = 3, sp = 253, sram[sp+2] = 10, sram[sp+1] = 0"

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
    break

start:
    ldi r30, target
    icall

end:
    break
