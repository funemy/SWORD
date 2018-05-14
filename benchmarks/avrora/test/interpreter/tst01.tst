; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the TST (test for zero or minus) instruction"
; @Result: "flags.s=0, flags.v=0, flags.n=0, flags.z=1, r16 = 0"

start:
    ldi r16, 0b00000000
    tst r16

end:
    break
