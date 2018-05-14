// @Harness: verifier
// @Purpose: "Test that indexing is applied only to the appropriate types"
// @Result: "TypeDoesNotSupportIndex @ 7:13"

architecture no_index_01 {
    subroutine foo(e: boolean): void {
        local foo: int = e[0];
    }
}
