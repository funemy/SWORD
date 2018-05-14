; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the BLD (bit load from register T) instruction"
; @Result: "flags.t = 1, r17 = 4"

start:
    set
    bld r17, 2

end:
    break
