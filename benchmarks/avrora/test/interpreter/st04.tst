; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the variants of the ST instruction"
; @Result: "sram[@memory] = 42, y = @memory"

start:
    ldi r16, 42
    ldi r28, low(memory)
    ldi r29, high(memory)
    st y, r16

end:
    break

data:

.dseg

    .byte 224 ; skip any IO registers
memory:
    .byte 2
    .byte 2
