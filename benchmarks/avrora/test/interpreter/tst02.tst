; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the TST (test for zero or minus) instruction"
; @Result: "flags.s=1, flags.v=0, flags.n=1, flags.z=0, r16 = -128"

start:
    ldi r16, 0b10000000
    tst r16

end:
    break
