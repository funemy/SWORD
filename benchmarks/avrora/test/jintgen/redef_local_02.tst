// @Harness: verifier
// @Purpose: "Test for redefinitions of local variables"
// @Result: "RedefinedLocal @ 8:13"

architecture redef_local_01 {
   subroutine foo(b: int): void {
       local a: int = 0;
       local a: boolean = false;
   }
}
