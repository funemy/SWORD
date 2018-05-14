; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the LDS (load direct from SRAM) instruction"
; @Initial: "[memory] = 42"
; @Result: "r16 = 42"

start:
    ldi r17, 42
    sts memory, r17
    lds r16, memory

end:
    break

data:

.dseg

    .byte 224 ; skip any IO registers
memory:
    .byte 2
