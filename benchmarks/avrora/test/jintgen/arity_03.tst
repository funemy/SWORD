// @Harness: verifier
// @Purpose: "Test for arity mismatch in subroutine calls"
// @Result: "ArityMismatch @ 9:12"

architecture arity_03 {
    external subroutine a(x: int): void;
    instruction "I" {
        execute {
            a();
        }
    }
}