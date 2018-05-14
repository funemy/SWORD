
label:
	push r0
	call test
	rjmp label

test:
	ldi r16, 99
	mov r0, r16
	inc r16
	ret