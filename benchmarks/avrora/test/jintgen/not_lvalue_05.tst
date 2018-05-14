// @Harness: verifier
// @Purpose: "Test for syntactic Lvalue correctness"
// @Result: "NotAnLvalue @ 7:8"

architecture not_lalue_01 {
    operand-type A[5]: int [0,31];
    subroutine foo(a: A): void {
	read(a) = 0;
    }
}
