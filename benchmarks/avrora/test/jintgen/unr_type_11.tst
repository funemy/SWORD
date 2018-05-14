// @Harness: verifier
// @Purpose: "Test for unresolved types"
// @Result: "UnresolvedType @ 7:16"

architecture unr_type_11 {
    operand-type A[5]: int [0,31];
    subroutine foo(a: A): void {
        write : duck(a, 0);
    }
}

