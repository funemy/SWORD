// @Harness: verifier
// @Purpose: "Test for redefinitions of subroutines"
// @Result: "RedefinedSubroutine @ 7:14"

architecture redef_sub_01 {
   external subroutine foo(): void;
   subroutine foo(): void { }
}
