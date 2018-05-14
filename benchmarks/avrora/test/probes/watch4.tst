# @Harness: probes
# @Result: 
# @Program: array.asm
# @Purpose: Tests watch for writes on multiple memory locations

watch A { | | | }

main {
  insert A 0x200;
  insert A 0x201;
  insert A 0x202;
  insert A 0x203;
  insert A 0x204;
  insert A 0x205;
  insert A 0x206;
  insert A 0x207;
  insert A 0x208;
  insert A 0x209;
  insert A 0x20a;
  insert A 0x20b;
  insert A 0x20c;
  insert A 0x20d;
  insert A 0x20e;
  insert A 0x20f;
}

result {
  6 A.beforeWrite;
  6 A.afterWrite;
  13 A.beforeWrite;
  13 A.afterWrite;
  20 A.beforeWrite;
  20 A.afterWrite;
  27 A.beforeWrite;
  27 A.afterWrite;
  34 A.beforeWrite;
  34 A.afterWrite;
  41 A.beforeWrite;
  41 A.afterWrite;
  48 A.beforeWrite;
  48 A.afterWrite;
  55 A.beforeWrite;
  55 A.afterWrite;
  62 A.beforeWrite;
  62 A.afterWrite;
  69 A.beforeWrite;
  69 A.afterWrite;
  76 A.beforeWrite;
  76 A.afterWrite;
  83 A.beforeWrite;
  83 A.afterWrite;
  90 A.beforeWrite;
  90 A.afterWrite;
  97 A.beforeWrite;
  97 A.afterWrite;
  104 A.beforeWrite;
  104 A.afterWrite;
  111 A.beforeWrite;
  111 A.afterWrite;
}
