; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the STS (store direct to SRAM) instruction"
; @Result: "sram[@memory] = 42"

start:
    ldi r16, 42
    sts memory, r16

end:
    break

data:

.dseg

    .byte 224 ; skip any IO registers
memory:
    .byte 2
    .byte 2
