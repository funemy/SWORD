// @Harness: verifier
// @Purpose: "Test for arity mismatch in subroutine calls"
// @Result: "ArityMismatch @ 8:8"

architecture arity_02 {
    external subroutine a(x: int): void;
    subroutine b(): void {
        a();
    }
}