; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the ELPM (extended load program memory) instruction"
; @Result: "r2 = 42, z = @data + 1"

start:
    ldi r30, data * 2
    elpm r2, z+

end:
    break

data:
    .db 42, 43
