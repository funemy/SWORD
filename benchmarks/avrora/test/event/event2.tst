# @Harness: eventqueue
# @Result: 5 A; 10 A; 15 A; 20 A; 25 A
# @Purpose: this tests the operation of the delta queue using simple events

event A { insert A 5; }

main {
  insert A 5;
  advance 25;
}
