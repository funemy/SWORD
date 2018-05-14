a0000:	jmp main
a0002:	jmp int01
a0004:	reti
a0005:	nop
a0006:	reti
a0007:	nop
a0008:	reti
a0009:	nop
a000a:	reti
a000b:	nop
a000c:	reti
a000d:	nop
a000e:	reti
a000f:	nop
a0010:	reti
a0011:	nop
a0012:	reti
a0013:	nop

main:
	ldi r17, 1
	out eimsk, r17
	sei
forever:
	rjmp forever

int01:
	ldi r16, 42
	reti

	