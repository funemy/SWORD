// @Harness: verifier
// @Purpose: "Test for return not being subroutine"
// @Result: "ReturnStmtNotInSubroutine @ 8:19"

architecture ret_insub_01 {
    instruction "I" {
    	execute {
	    return 0;
	}
    }
}
