// @Harness: verifier
// @Purpose: "Test for unresolved types"
// @Result: "UnresolvedType @ 7:27"

architecture unr_type_06 {
    subroutine foo(): void {
        local e: int = 0 : duck;
    }
}
