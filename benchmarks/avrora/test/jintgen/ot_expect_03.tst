// @Harness: verifier
// @Purpose: "Test for places where operand types are expected"
// @Result: "OperandTypeExpected @ 7:13"

architecture ot_expect_01 {
   
   subroutine foo(e: map<int, int>): void {
       local f: int = read(e);
   }
}
