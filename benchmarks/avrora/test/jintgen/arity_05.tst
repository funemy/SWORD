// @Harness: verifier
// @Purpose: "Test for arity mismatch in subroutine calls"
// @Result: "ArityMismatch @ 9:27"

architecture arity_03 {
    external subroutine a(x: int): int;
    instruction "I" {
        execute {
            local e: int = a();
        }
    }
}