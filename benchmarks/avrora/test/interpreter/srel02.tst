; @Harness: simplifier
; @Purpose: "Range test for relative jumps"
; @Result: ConstantOutOfRange @ 6:13

toofar:
.byte 127
	brie toofar

