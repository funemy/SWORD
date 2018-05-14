; test case for call/return behavior.

ldi r17, 0b0001
call test
ldi r17, 0b0100
	call test2
break

test:
    ldi r17, 0b010
    ret

test2:
	ldi r18, 0b100
	ldi r19, 0b1000
	call test
	ret
