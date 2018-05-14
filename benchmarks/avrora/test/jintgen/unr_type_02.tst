// @Harness: verifier
// @Purpose: "Test for unresolved types"
// @Result: "UnresolvedType @ 6:21"

architecture unr_type_02 {
   subroutine foo(e: duck): void {
   }
}
