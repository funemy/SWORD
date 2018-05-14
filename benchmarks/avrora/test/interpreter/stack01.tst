; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the behavior of the software stack"
; @Result: "sram[sp+1] = 42, sp = 254"

start:
    ldi r17, 255
    out spl, r17
    ldi r16, 42
    push r16

end:
    break

data:

