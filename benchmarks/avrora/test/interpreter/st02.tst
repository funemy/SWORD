; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the variants of the ST instruction"
; @Result: "sram[@memory] = 42, x = @memory + 1"

start:
    ldi r16, 42
    ldi r26, low(memory)
    ldi r27, high(memory)
    st x+, r16

end:
    break

data:

.dseg

    .byte 224 ; skip any IO registers
memory:
    .byte 2
    .byte 2
