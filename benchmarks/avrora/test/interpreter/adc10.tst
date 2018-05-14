; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the ADC (add two registers with carry) instruction"
; @Result: "flags.h=0, flags.s=0, flags.v=0, flags.n=0, flags.z=0, flags.c=1, r16 = 1"

start:
    ldi r16, 0b01000000
    ldi r17, 0b11000000
    sec
    adc r16, r17

end:
    break
