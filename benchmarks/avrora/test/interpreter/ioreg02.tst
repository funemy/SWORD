; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test instructions for reading/writing to IO registers"
; @Result: "r17 = 42, r18 = 42, sram[59] = 42, sram[0] = 0"

start:
    ldi r17, 42
    out 0x1b, r17
    in r18, 0x1b

end:
    break
