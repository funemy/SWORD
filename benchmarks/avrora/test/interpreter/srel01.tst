; @Harness: simplifier
; @Purpose: "Range test for relative jumps"
; @Result: "ConstantOutOfRange @ 5:13"

start:
	brie toofar

.byte 128

toofar:
	nop
