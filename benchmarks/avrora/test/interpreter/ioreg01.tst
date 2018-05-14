; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test instructions for reading/writing to IO registers"
; @Result: "r17 = 1, flags.c = 1"

start:
    ldi r17, 42
    sec
    in r17, sreg

end:
    break
