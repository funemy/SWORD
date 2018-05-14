// @Harness: verifier
// @Purpose: "Test for places where integer types are expected"
// @Result: "IntTypeExpected @ 7:27"

architecture int_expect_01 {
    operand-type A[5]: int [0,31];
    subroutine foo(ind: int, e: A): void {
        local a: int = ind[e];
    }
}
