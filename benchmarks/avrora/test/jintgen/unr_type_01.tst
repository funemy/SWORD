// @Harness: verifier
// @Purpose: "Test for unresolved types"
// @Result: "UnresolvedType @ 6:15"

architecture unr_type_01 {
   global foo: duck;
}
