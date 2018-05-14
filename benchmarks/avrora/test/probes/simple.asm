; @Harness: simulator
; @Purpose: "Test the branch instructions for correct conditions and target"
; @Result: "target = 10, r16 = 0, r17 = 0, r18 = 3, sp = 253, $(sp+2) = 3, $(sp+1) = 0"

setstack:
    ldi r21, 255
    out spl, r21

start:
    rcall target

return:
    ldi r16, 1

before:
    ldi r17, 2
target:
    ldi r18, 3

end:
    break
