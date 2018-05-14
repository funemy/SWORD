; @Harness: simplifier
; @Purpose: "Test generation of UnknownRegister error"
; @Result: "UnknownRegister @ 6:9"

start:
    add r0, foo

end:
    break
