; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the ADD (add two registers) instruction"
; @Result: "flags.h=1, flags.s=0, flags.v=0, flags.n=0, flags.z=0, flags.c=0, r16 = 16"

start:
    ldi r16, 0b00001000
    ldi r17, 0b00001000
    add r16, r17

end:
    break
