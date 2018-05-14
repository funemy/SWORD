; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the behavior of the software stack"
; @Result: "r17 = 42, sp = 255"

start:
    ldi r17, 255
    out spl, r17
    ldi r16, 42
    push r16
    pop r17

end:
    break

data:

