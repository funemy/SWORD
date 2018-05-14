; Array.asm -- simply load 16 bytes from 0x100 into r0

start:
    ldi r26, 0
    ldi r27, 1
    ldi r28, 0
    ldi r29, 2

loop:
    ld r0, x+
    st y+, r0
    cpi r26, 16
    brne loop

end:
    break 
