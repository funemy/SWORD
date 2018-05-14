// @Harness: verifier
// @Purpose: "Test for variable resolution"
// @Result: "UnresolvedVariable @ 9:19"

architecture unr_var_06 {
    external subroutine foo(a: int): int;
    instruction "I" {
        execute {
           local e: int = 0;
           e = foo(a);
	}
    }
}
