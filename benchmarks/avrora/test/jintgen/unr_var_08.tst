// @Harness: verifier
// @Purpose: "Test for variable resolution"
// @Result: "UnresolvedVariable @ 9:15"

architecture unr_var_03 {
   instruction "I" {
       execute {
           local e: int = 0;
           e = a;
	}
    }
}
