; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the LDD (load from SRAM with displacement) instruction"
; @Initial: "[@memory + 2] = 42"
; @Result: "r16 = 42, z = @memory"

start:
    ldi r17, 42
    sts memory + 2, r17
    ldi r30, low(memory)
    ldi r31, high(memory)
    ldd r16, z + 2

end:
    break

data:

.dseg

    .byte 224 ; skip any IO registers
memory:
    .byte 2
    .byte 2
