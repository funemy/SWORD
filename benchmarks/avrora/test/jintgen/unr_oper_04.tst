// @Harness: verifier
// @Purpose: "Test for operator resolution"
// @Result: "UnresolvedOperator @ 8:25"

architecture unr_oper_01 {
    operand-type A[5]: int [0,31];
    subroutine foo(a: int, b: A): void {
        local e: int = a + b;
    }
}
