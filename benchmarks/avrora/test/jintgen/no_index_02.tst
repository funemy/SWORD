// @Harness: verifier
// @Purpose: "Test that indexing is applied only to the appropriate types"
// @Result: "TypeDoesNotSupportIndex @ 7:13"

architecture no_index_01 {
    enum E { r = 0 }
    subroutine foo(e: E): void {
        local foo: int = e[0];
    }
}
