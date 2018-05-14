; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the ELPM (extended load program memory) instruction"
; @Result: "r2 = 42, z = @data + 1 - 65536"

start:
    ldi r16, 1
    out rampz, r16
    ldi r30, low(data * 2)
    ldi r31, high(data * 2)
    elpm r2, z+

end:
    break

    .byte 70000
data:
    .db 42, 43
