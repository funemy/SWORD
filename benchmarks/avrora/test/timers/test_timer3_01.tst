; @Harness: simulator
; @Format: atmel
; @Arch: avr
; @Purpose: "Test the functionality of timer3 in generating interrupts"
; @Result: "r16 = 42, r17 = 42"

;  this tests the overflow functionality in normal mode
	
.equ FAILURE = 1
.equ SUCCESS = 42
.equ INTERRUPTED = 12

	;;  I need to figure out the interrupt number for TIMER2 overflow
; Interrupt Jump Table
L000:	jmp    MAIN             ; reset #1

L004:	jmp    INT_FAILURE           ; interrupt #2
L008:	jmp    INT_FAILURE           ; interrupt #3
L00C:	jmp    INT_FAILURE           ; interrupt #4
L010:	jmp    INT_FAILURE           ; interrupt #5
L014:	jmp    INT_FAILURE           ; interrupt #6
L018:	jmp    INT_FAILURE           ; interrupt #7
L01C:	jmp    INT_FAILURE           ; interrupt #8
L020:	jmp    INT_FAILURE           ; interrupt #9
L024:	jmp    INT_FAILURE           ; interrupt #10
L028:	jmp    INT_FAILURE           ; interrupt #11
L02C:	jmp    INT_FAILURE           ; interrupt #12
L030:	jmp    INT_FAILURE           ; interrupt #13
L034:	jmp    INT_FAILURE           ; interrupt #14
L038:	jmp    INT_FAILURE           ; interrupt #15
L03C:	jmp    INT_FAILURE           ; interrupt #16
L040:	jmp    INT_FAILURE           ; interrupt #17


L044:	jmp    INT_FAILURE           ; interrupt #18
L048:	jmp    INT_FAILURE           ; interrupt #19
L04C:   jmp    INT_FAILURE           ; interrupt #20
L050:	jmp    INT_FAILURE           ; interrupt #21
L054:	jmp    INT_FAILURE           ; interrupt #22
L058:	jmp    INT_FAILURE           ; interrupt #23
L05C:	jmp    INT_FAILURE           ; interrupt #24
L060:	jmp    INT_FAILURE           ; interrupt #25
L064:	jmp    INT_FAILURE           ; interrupt #26
L068:	jmp    INT_FAILURE           ; interrupt #27
L06C:	jmp    INT_FAILURE           ; interrupt #28
L070:	jmp    INT_FAILURE           ; interrupt #29
L074:	jmp    TIMER_OVF           ; interrupt #30
L078:	jmp    INT_FAILURE           ; interrupt #31
L07C:	jmp    INT_FAILURE           ; interrupt #32
L080:	jmp    INT_FAILURE           ; interrupt #33
L084:	jmp    INT_FAILURE           ; interrupt #34
L088:	jmp    INT_FAILURE           ; interrupt #35

INT_FAILURE:
	ldi r16, INTERRUPTED 		; indicate failure
	break


TIMER_OVF:
	ldi r16, SUCCESS
	reti

MAIN:
	ldi r16, FAILURE
	sei
	ldi r18, 255
	out SPL, r18		;  initialize stack pointer

	ldi r18, 0b00000100
	sts 0x7d, r18		; initialize ETIMSK

	ldi r18, 0b11111111
	;; initialize TCNT3
	sts 0x89, r18		; initialize TCNT3H
	ldi r18, 0b11111000
	sts 0x88, r18		; initialize TCNT3L
	;; turn on timer
	ldi r18, 0b0000001
	sts 0x8A, r18		; initialize TCCR3B
	
CHECK:
	nop
	nop
	nop
	nop

	
	cpi r16, SUCCESS
	breq OK
	inc r0
	brne CHECK
	ldi r17, FAILURE
	break
OK:
	ldi r17, SUCCESS
	break
