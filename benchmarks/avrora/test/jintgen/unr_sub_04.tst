// @Harness: verifier
// @Purpose: "Test for unresolved subroutines"
// @Result: "UnresolvedSubroutine @ 8:27"

architecture unr_sub_04 {
    instruction "I" {
    	execute {
	    local e: int = bar();
	}
    }
}
