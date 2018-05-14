// @Harness: verifier
// @Purpose: "Test for redefinitions of local variables"
// @Result: "RedefinedLocal @ 11:17"

architecture redef_local_01 {
   operand-type A[5]: int [0,31];
   
   instruction "I" ab: A {
       execute {
           local a: int = 0;
           local a: boolean = false;
       }
   }
}
