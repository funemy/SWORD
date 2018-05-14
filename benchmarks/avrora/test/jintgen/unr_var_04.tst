// @Harness: verifier
// @Purpose: "Test for variable resolution"
// @Result: "UnresolvedVariable @ 7:7"

architecture unr_var_04 {
   subroutine foo(e: int): void {
       a = 0;
   }
}
