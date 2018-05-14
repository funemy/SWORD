// @Harness: verifier
// @Purpose: "Test for types in subroutine calls"
// @Result: "TypeMismatch @ :"

architecture type_mismatch_01 {
    subroutine foo(b: boolean): void {
        foo(0);
    }
}
