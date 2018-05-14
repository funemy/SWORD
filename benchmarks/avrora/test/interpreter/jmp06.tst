; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the branch instructions for correct conditions and target"
; @Result: "@target = 8, r17 = 0, r18 = 3, pc = @target + 4"

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
    ijmp

end:
    break
