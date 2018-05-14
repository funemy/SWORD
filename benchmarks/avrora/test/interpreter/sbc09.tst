; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the SBC (subtract two registers with carry) instruction"
; @Result: "flags.h=1, flags.s=0, flags.v=0, flags.n=0, flags.z=0, flags.c=0, r16 = 7"

start:
    sec
    ldi r16, 0b00010000
    ldi r17, 0b00001000
    sbc r16, r17

end:
    break
