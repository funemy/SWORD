// @Harness: verifier
// @Purpose: "Test for unresolved types"
// @Result: "UnresolvedType @ 6:27"

architecture unr_type_03 {
   subroutine foo(e: int): duck {
   }
}
