// @Harness: verifier
// @Purpose: "Test for variable resolution"
// @Result: "UnresolvedVariable @ 9:11"

architecture unr_var_04 {
   instruction "I" {
       execute {
           local e: int = 0;
           a = 0;
	}
    }
}
