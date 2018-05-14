; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the CLx and SEx instructions for setting flags in registers"
; @Result: "flags.i=1, flags.t=0, flags.h=0, flags.s=0, flags.v=0, flags.n=0, flags.z=0, flags.c=0"

start:
    sei

end:
    break
