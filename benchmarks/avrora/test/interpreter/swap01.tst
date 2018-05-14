; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the SWAP (swap nibbles in register) instruction"
; @Result: "r16 = 15"

start:
    ldi r16, 0xF0
    swap r16

end:
    break
