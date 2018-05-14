; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the BST (bit store to register T) instruction"
; @Result: "flags.t = 0, r17 = 11"

start:
    set
    ldi r17, 0b1011
    bst r17, 2

end:
    break
