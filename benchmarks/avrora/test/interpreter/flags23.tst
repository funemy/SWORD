; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the BST (bit store to register T) instruction"
; @Result: "flags.t = 1, r17 = 4"

start:
    ldi r17, 0b100
    bst r17, 2

end:
    break
