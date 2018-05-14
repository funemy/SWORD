; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the SWAP (swap nibbles in register) instruction"
; @Result: "r16 = 119"

start:
    ldi r16, 0x77
    swap r16

end:
    break
