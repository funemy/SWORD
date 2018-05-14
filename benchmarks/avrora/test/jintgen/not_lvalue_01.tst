// @Harness: verifier
// @Purpose: "Test for syntactic Lvalue correctness"
// @Result: "NotAnLvalue @ 7:8"

architecture not_lalue_01 {
    subroutine foo(): void {
	3 = 0;
    }
}
