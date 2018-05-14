// @Harness: verifier
// @Purpose: "Test for operator resolution"
// @Result: "UnresolvedOperator @ 8:25"

architecture unr_oper_01 {
    enum E { r = 0 }
    subroutine foo(a: int, b: E): void {
        local e: int = a + b;
    }
}
