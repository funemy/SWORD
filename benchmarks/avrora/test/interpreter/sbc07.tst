; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the SBC (subtract two registers with carry) instruction"
; @Result: "flags.h=1, flags.s=0, flags.v=0, flags.n=0, flags.z=0, flags.c=1, r16 = 127"

start:
    sec
    ldi r16, 0b00000000
    ldi r17, 0b10000000
    sbc r16, r17

end:
    break
