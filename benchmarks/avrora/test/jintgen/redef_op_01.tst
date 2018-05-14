// @Harness: verifier
// @Purpose: "Test for redefinitions of operands"
// @Result: "RedefinedOperand @ 8:23"

architecture redef_op_01 {
   operand-type A[5]: int [0, 31];

   addr-mode B foo: A, foo: A { }
}