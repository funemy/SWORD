; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the BLD (bit load from register T) instruction"
; @Result: "flags.t = 0, r17 = 11"

start:
    clt
    ldi r17, 0b1111
    bld r17, 2

end:
    break
