; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the behavior of the software stack"
; @Result: "sram[sp+1] = 42, sram[sp+2] = 43, sp = 253"

start:
    ldi r17, 255
    out spl, r17
    ldi r16, 43
    push r16
    ldi r17, 42
    push r17

end:
    break

data:

