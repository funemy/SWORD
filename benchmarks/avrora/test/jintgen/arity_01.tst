// @Harness: verifier
// @Purpose: "Test for arity mismatch in subroutine calls"
// @Result: "ArityMismatch @ 8:8"

architecture arity_01 {
    external subroutine a(): void;
    subroutine b(): void {
        a(0);
    }
}
