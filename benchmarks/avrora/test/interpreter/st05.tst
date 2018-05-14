; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the variants of the ST instruction"
; @Result: "sram[@memory] = 42, z = @memory"

start:
    ldi r16, 42
    ldi r30, low(memory)
    ldi r31, high(memory)
    st z, r16

end:
    break

data:

.dseg

    .byte 224 ; skip any IO registers
memory:
    .byte 2
    .byte 2
