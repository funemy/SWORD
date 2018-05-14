// @Harness: verifier
// @Purpose: "Test for unresolved types"
// @Result: "UnresolvedType @ 8:30"

architecture unr_type_07 {
    operand-type A[5]: int [0,31];
    subroutine foo(a: A): void {
        local e: int = read : duck(a);
    }
}
