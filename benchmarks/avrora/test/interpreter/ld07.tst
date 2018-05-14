; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the LD (load from SRAM) instruction"
; @Initial: "[@memory] = 42"
; @Result: "r16 = 42, x = @memory"

start:
    ldi r17, 42
    sts memory, r17
    ldi r26, low(memory)
    ldi r27, high(memory)
    ld r16, x

end:
    break

data:

.dseg

    .byte 224 ; skip any IO registers
memory:
    .byte 2
