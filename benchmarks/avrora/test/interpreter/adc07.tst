; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the ADC (add two registers with carry) instruction"
; @Result: "flags.h=0, flags.s=1, flags.v=0, flags.n=1, flags.z=0, flags.c=0, r16 = -127"

start:
    ldi r16, 0b10000000
    ldi r17, 0b00000000
    sec
    adc r16, r17

end:
    break
