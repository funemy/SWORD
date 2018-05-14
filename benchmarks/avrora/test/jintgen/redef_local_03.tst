// @Harness: verifier
// @Purpose: "Test for redefinitions of local variables"
// @Result: "RedefinedLocal @ 10:17"

architecture redef_local_01 {
   operand-type A[5]: int [0,31];
   
   instruction "I" a: A {
       execute {
           local a: int = 0;
       }
   }
}
