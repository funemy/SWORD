# @Harness: probes
# @Result: 
# @Program: array.asm
# @Purpose: Tests whether probes inserted and removed from within watches behave correctly

watch A { insert B 0x0A; | | | }
watch C { | | remove B 0x0A; | }
probe B { | }

main {
  insert A 0x10e;
  insert C 0x20e;
}

result {
  102 A.beforeRead;
  102 A.afterRead;
  104 B.before;
  104 C.beforeWrite;
  104 C.afterWrite;
  106 B.after;
}
