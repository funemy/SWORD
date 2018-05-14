// @Harness: verifier
// @Purpose: "Test for variable resolution"
// @Result: "UnresolvedVariable @ 7:11"

architecture unr_var_02 {
   subroutine foo(e: int): void {
       e = a[0];
   }
}
