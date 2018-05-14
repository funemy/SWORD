; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the ADD (add two registers) instruction"
; @Result: "flags.h=0, flags.s=1, flags.v=0, flags.n=1, flags.z=0, flags.c=0, r16 = -128"

start:
    ldi r16, 0b10000000
    ldi r17, 0b00000000
    add r16, r17

end:
    break
