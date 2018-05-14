; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the SWAP (swap nibbles in register) instruction"
; @Result: "r16 = 112"

start:
    ldi r16, 0x07
    swap r16

end:
    break
