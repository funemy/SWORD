// @Harness: verifier
// @Purpose: "Test for redefinitions of instructions"
// @Result: "RedefinedInstr @ 11:15"

architecture redef_inst_01 {
   operand-type A[5]: int [0,31];

   instruction "I" {
   }

   instruction "I" {
   }
}