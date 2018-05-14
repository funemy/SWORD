// @Harness: verifier
// @Purpose: "Test for return not being subroutine"
// @Result: "ReturnStmtNotInSubroutine @ 8:31"

architecture ret_insub_01 {
    instruction "I" {
    	execute {
            if ( true ) return 0;
	}
    }
}
