; @Harness: disassembler
; @Result: PASS
  section .text  size=0x00000002 vma=0x00000000 lma=0x00000000 offset=0x00000034 ;2**0 
  section .data  size=0x00000000 vma=0x00000000 lma=0x00000000 offset=0x00000036 ;2**0 

start .text:

label 0x00000000  ".text":
      0x0: 0x08 0x95  ret

start .data:

