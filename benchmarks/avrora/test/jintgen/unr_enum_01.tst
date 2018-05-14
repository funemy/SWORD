// @Harness: verifier
// @Purpose: "Test for unresolved enumerations"
// @Result: "UnresolvedEnum @ 6:19"

architecture unr_enum_01 {
    enum-subset E: A {
	r = 0
    }
}
