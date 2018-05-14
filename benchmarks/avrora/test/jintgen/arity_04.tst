// @Harness: verifier
// @Purpose: "Test for arity mismatch in subroutine calls"
// @Result: "ArityMismatch @ 8:23"

architecture arity_01 {
    external subroutine a(): int;
    subroutine b(): void {
        local e: int = a(0);
    }
}
