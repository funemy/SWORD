// @Harness: verifier
// @Purpose: "Test that indexing is applied only to the appropriate types"
// @Result: "TypeDoesNotSupportIndex @ 8:25"

architecture no_index_01 {
    operand-type A[5]: int [0,31];
    subroutine foo(e: A): void {
        local foo: int = e[0];
    }
}
