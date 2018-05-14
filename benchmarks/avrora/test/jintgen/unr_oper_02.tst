// @Harness: verifier
// @Purpose: "Test for operator resolution"
// @Result: "UnresolvedOperator @ 7:25"

architecture unr_oper_01 {
    subroutine foo(a: boolean, b: int): void {
        local e: int = a + b;
    }
}
