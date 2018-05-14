// @Harness: verifier
// @Purpose: "Test for unresolved types"
// @Result: "UnresolvedType @ 7:17"

architecture unr_type_04 {
    subroutine foo(): void {
        local e: duck = 0;
    }
}
