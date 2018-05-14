// @Harness: verifier
// @Purpose: "Test for unresolved subroutines"
// @Result: "UnresolvedSubroutine @ 7:23"

architecture unr_sub_02 {
    subroutine foo(): void {
        local e: int = bar();
    }
}
