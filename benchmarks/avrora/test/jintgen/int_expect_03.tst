// @Harness: verifier
// @Purpose: "Test for places where integer types are expected"
// @Result: "IntTypeExpected @ 7:23"

architecture int_expect_01 {
    enum E { r = 0 }
    subroutine foo(e: E): void {
        local a: int = e[1:0];
    }
}
