; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test instructions for reading/writing to IO registers"
; @Result: "r17 = 42, sram[95] = 42, r18 = 42, sram[0] = 0"

start:
    ldi r17, 42
    out sreg, r17
    in r18, sreg

end:
    break
