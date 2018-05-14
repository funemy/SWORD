; @Harness: simplifier
; @Purpose: "Range test for relative jumps"
; @Result: "PASS"

START:
	brie TOOFAR

.byte 126

TOOFAR:
	nop
