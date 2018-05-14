; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the ADD (add two registers) instruction"
; @Result: "flags.h=0, flags.s=1, flags.v=1, flags.n=0, flags.z=1, flags.c=1, r16 = 0"

start:
    ldi r16, 0b10000000
    ldi r17, 0b10000000
    add r16, r17

end:
    break
