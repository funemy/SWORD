// @Harness: verifier
// @Purpose: "Test for unresolved types"
// @Result: "UnresolvedType @ 6:23"

architecture unr_type_08 {
    global m: map<int, duck>;
}
