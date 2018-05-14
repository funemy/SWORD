

	ldi r17, 255
	out SPL, r17
label:

	ldi r16, 200
	call test
	break
	
test:

	inc r16
	breq skip
	call test
skip:	
	ret