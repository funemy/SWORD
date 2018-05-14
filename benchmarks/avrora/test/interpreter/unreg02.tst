; @Harness: simplifier
; @Purpose: "Test generation of UnknownRegister error"
; @Result: "UnknownRegister @ 6:10"

start:
    add foo, r0

end:
    break
