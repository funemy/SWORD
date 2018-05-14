# @Harness: interrupt
# @Result: 
# @Program: handlers.od
# @Interrupt-schedule: interrupt-sched.txt
# @Purpose: Test interrupt scheduler (timing) using watches

###########################################################
#  Notes/TODO:
# ========================================================= 
# - most interrupts (or at least their handlers) run 17 
#     cycles after they are scheduled to.  All those after
#     22 is run have a delay of 18, I do not know why.
#
# - interrupt 22 fires with a delay of 56, most likely due
#     to it's larger handler (bits need to be set and reset
#     for 22 not to swallow all later interrupts)
#
# - interrupt 1 fires with an odd delay, most likely due
#     to some set up not being complete that early.
#
# - interrupts 19-21 do not fire at the right time.
#     interrupt 22 fires before them for some reason
#
# - handlers.od: if the handler for 30 is run the program will 
#     end 
#
# - Disabled interrupts: 23 (called automatically & hard to
#                                               account for)
#                        24,31-35 (wont fire, fire too 
#                                  often. problem in handlers.od)
#                        19-21 (see above)
###########################################################

watch A { | | | }
watch B { | | | }
watch C { | | | }
watch D { | | | }
watch E { | | | }
watch F { | | | }
watch G { | | | }
watch H { | | | }
watch I { | | | }
watch J { | | | }
watch K { | | | }
watch L { | | | }
watch M { | | | }
watch N { | | | }
watch O { | | | }
watch P { | | | }
watch Q { | | | }
watch R { | | | }
watch S { | | | }
watch T { | | | }
watch U { | | | }
watch V { | | | }
watch W { | | | }
watch X { | | | }
watch Y { | | | }
watch Z { | | | }
watch AA { | | | }
watch AB { | | | }
watch AC { | | | }
watch AD { | | | }
watch AE { | | | }
watch AF { | | | }
watch AG { | | | }
watch AH { | | | }
watch AI { | | | }

event INS {
  insert A 0x0902;
  insert B 0x0903;
  insert C 0x0904;
  insert D 0x0905;
  insert E 0x0906;
  insert F 0x0907;
  insert G 0x0908;
  insert H 0x0909;
  insert I 0x0910;
  insert J 0x0911;
  insert K 0x0912;
  insert L 0x0913;
  insert M 0x0914;
  insert N 0x0915;
  insert O 0x0916;
  insert P 0x0917;
  insert Q 0x0918;
#  insert R 0x0919; #<----- aren't scheduled correctly - I don't know why
#  insert S 0x0920; #<----- vector 22 (U) seems always to come before any
#  insert T 0x0921; #<----- of these
  insert U 0x0922;
#  insert V 0x0923; #<----- disabled b/c 23 fires automatically and its hard to know when
#  insert W 0x0924; # wasn't able to enable this one, enable bit didn't work
  insert X 0x0925;
  insert Y 0x0926;
  insert Z 0x0927;
  insert AA 0x0928;
  insert AB 0x0929;
  insert AC 0x0930;
#  insert AD 0x0931; #<--- fires numerous times even when not scheduled
#  insert AE 0x0932; #     all vectors after don't tend to fire or behave 
#  insert AF 0x0934; #     oddly
#  insert AG 0x0935;
}

main {
  insert INS 0;
}

result {
0 INS;
126 A.beforeRead;
126 A.afterRead;
217 B.beforeRead;
217 B.afterRead;
317 C.beforeRead;
317 C.afterRead;
417 D.beforeRead;
417 D.afterRead;
517 E.beforeRead;
517 E.afterRead;
617 F.beforeRead;
617 F.afterRead;
717 G.beforeRead;
717 G.afterRead;
817 H.beforeRead;
817 H.afterRead;
917 I.beforeRead;
917 I.afterRead;
1017 J.beforeRead;
1017 J.afterRead;
1117 K.beforeRead;
1117 K.afterRead;
1217 L.beforeRead;
1217 L.afterRead;
1317 M.beforeRead;
1317 M.afterRead;
1417 N.beforeRead;
1417 N.afterRead;
1517 O.beforeRead;
1517 O.afterRead;
1617 P.beforeRead;
1617 P.afterRead;
1717 Q.beforeRead;
1717 Q.afterRead;
 # 1817 R.beforeRead; #<--- R through T disabled in this file only
 # 1817 R.afterRead; #<---- see note above in event INS
 # 1917 S.beforeRead; 
 # 1917 S.afterRead;
 # 2017 T.beforeRead;
 # 2017 T.afterRead;

2156 U.beforeRead; #<--------- larger delay due to bigger handler
2156 U.afterRead;  

#2217 V.beforeRead; <-- disabled (see note above in INS)
#2217 V.afterRead; 

# delay in interrupt firing is 18 here after for some reason

2418 X.beforeRead;
2418 X.afterRead;
2518 Y.beforeRead;
2518 Y.afterRead;
2618 Z.beforeRead;
2618 Z.afterRead;
2718 AA.beforeRead;
2718 AA.afterRead;
2818 AB.beforeRead;
2818 AB.afterRead;
2918 AC.beforeRead;
2918 AC.afterRead;
}
