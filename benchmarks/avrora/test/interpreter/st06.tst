; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the STD (store with displacement) instruction"
; @Result: "sram[@memory + 2] = 42, y = @memory"

start:
    ldi r16, 42
    ldi r28, memory
    std y+2, r16

end:
    break

data:

.dseg

memory:
    .byte 2
    .byte 2
