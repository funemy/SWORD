// @Harness: verifier
// @Purpose: "Test for unresolved enumerations"
// @Result: "UnresolvedEnum @ 7:19"

architecture unr_enum_02 {
    operand-type A[5]: int [0,31];
    enum-subset E: A {
	r = 0
    }
}
