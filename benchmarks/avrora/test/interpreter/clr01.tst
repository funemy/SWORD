; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the CLR (clear register) instruction"
; @Result: "flags.s=0, flags.v=0, flags.n=0, flags.z=1, r16 = 0"

start:
    ldi r16, 0b00100100
    clr r16

end:
    break
