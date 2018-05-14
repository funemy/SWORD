// @Harness: verifier
// @Purpose: "Test for redefinitions of operands"
// @Result: "RedefinedOperand @ 8:27"

architecture redef_op_02 {
   operand-type A[5]: int [0, 31];

   instruction "I" foo: A, foo: A { }
}
