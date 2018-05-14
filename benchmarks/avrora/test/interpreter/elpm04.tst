; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the ELPM (extended load program memory) instruction"
; @Result: "r0 = 42, z = @data - 65536"

start:
    ldi r16, 1
    out rampz, r16
    ldi r30, low(data * 2)
    ldi r31, high(data * 2)
    elpm

end:
    break

    .byte 70000
data:
    .db 42
