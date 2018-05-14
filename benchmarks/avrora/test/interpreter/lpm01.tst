; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the LPM (load program memory) instruction"
; @Result: "r0 = 42, z = @data"

start:
    ldi r30, data * 2
    lpm

end:
    break

data:
    .db 42
