# @Harness: probes
# @Result: 
# @Program: simple.asm
# @Purpose: this tests the operation of the low-level probing infrastructure

probe A { remove A 0x000; | }

main {
  insert A 0x000;
}

result {
  0 A.before;
  1 A.after;
}
