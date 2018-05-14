// @Harness: verifier
// @Purpose: "Test for places where operand types are expected"
// @Result: "OperandTypeExpected @ 7:13"

architecture ot_expect_02 {
   
   subroutine foo(e: boolean): void {
       local f: int = read(e);
   }
}
