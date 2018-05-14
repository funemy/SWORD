// @Harness: verifier
// @Purpose: "Test for places where integer types are expected"
// @Result: "IntTypeExpected @ 7:27"

architecture int_expect_01 {
    subroutine foo(ind: int, e: boolean): void {
        local a: int = ind[e];
    }
}
