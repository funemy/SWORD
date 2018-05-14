// @Harness: verifier
// @Purpose: "Test for lvalue correctness"
// @Result: "NotAnLvalue @ 7:8"

architecture _ {
    subroutine foo(): void {
	3 + 8 = 0;
    }
}
