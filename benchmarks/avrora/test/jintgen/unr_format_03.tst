// @Harness: verifier
// @Purpose: "Test for unresolved formats"
// @Result: "UnresolvedFormat @ 7:19"

architecture unr_format_01 {
    instruction "I" {
	encoding = F where { s = 1 }
    }
}
