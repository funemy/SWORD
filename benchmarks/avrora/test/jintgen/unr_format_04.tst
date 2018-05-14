// @Harness: verifier
// @Purpose: "Test for unresolved formats"
// @Result: "UnresolvedFormat @ 7:19"

architecture unr_format_02 {
    format F = { s[15:0] }
    instruction "I" {
	encoding = F where { s = 1 }
        encoding = F2 where { s = 0 }
    }
}
