// @Harness: verifier
// @Purpose: "Test for value types expected in operand type decl"
// @Result: "ValueTypeExpected @ 7:20"

architecture value_expect_04 {
    operand-type A[5]: int [0,31];
    operand-type B[5]: A;
}
