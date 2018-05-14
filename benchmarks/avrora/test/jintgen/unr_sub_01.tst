// @Harness: verifier
// @Purpose: "Test for unresolved subroutines"
// @Result: "UnresolvedSubroutine @ 7:7"

architecture unr_sub_01 {
   subroutine foo(): void {
       bar();
   }
}
