// @Harness: verifier
// @Purpose: "Test for variable resolution"
// @Result: "UnresolvedVariable @ 7:15"

architecture unr_var_01 {
   subroutine foo(e: int): void {
       e = foo(a);
   }
}
