; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the LDS (load direct from SRAM) instruction"
; @Result: "r16 = 0"

start:
    ldi r16, 42
    lds r16, memory

end:
    break

data:

.dseg

    .byte 224 ; skip any IO registers
memory:
    .byte 2
