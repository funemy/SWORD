// @Harness: verifier
// @Purpose: "Test for variable resolution"
// @Result: "UnresolvedVariable @ 7:15"

architecture _ {
   subroutine foo(e: int): void {
       e = a[3:0];
   }
}
