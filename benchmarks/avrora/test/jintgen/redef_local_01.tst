// @Harness: verifier
// @Purpose: "Test for redefinitions of local variables"
// @Result: "RedefinedLocal @ 7:13"

architecture redef_local_01 {
   subroutine foo(a: int): void {
       local a: int = 0;
   }
}
