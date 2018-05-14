# @Harness: probes
# @Result: 
# @Program: array.asm
# @Purpose: tests read and write watches

watch A { | | | }
watch B { | | | }

main {
  insert A 0x100;
  insert B 0x200;
}

result {
  4 A.beforeRead;
  4 A.afterRead;
  6 B.beforeWrite;
  6 B.afterWrite;
}
