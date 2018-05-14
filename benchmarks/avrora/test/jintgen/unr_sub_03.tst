// @Harness: verifier
// @Purpose: "Test for unresolved subroutines"
// @Result: "UnresolvedSubroutine @ 8:12"

architecture un_sub_03 {
    instruction "I" {
    	execute {
	    bar();
	}
    }
}
