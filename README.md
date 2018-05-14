# ECHO: A Fast Concurrency Bug Detection for The Whole Program
### Introduction
ECHO is an interactive Eclipse Plugin to detect data races and deadlocks in Java programs. The fundamental techniques (i.e., points-to analysis and static happens-before analysis) that ECHO adopts are in: 

[PLDI'18] "[D4: Fast Concurrency Debugging with Parallel Differential Analysis](https://parasol.tamu.edu/~jeff/d4.pdf)"

### Software Dependencies
- Java 1.8 
- Eclipse PDE

### Quick Start
1. git clone https://github.tamu.edu/April1989/ECHO_the_whole_program.git.
2. Import all the projects in Eclipse and build.

### How To Use
#### Start The Detection
1. Select a main class in the _Package Explorer_ 
2. Right click _ASER_ -> _ECHO_ to start the detection. 

#### ECHO Views
Goto _Windows_ -> _Show View_ -> _Others_ to open the ECHO views (i.e., _ECHO Race List_, _ECHO Deadlock List_, _ECHO Concurrency Relation_):
  * _ECHO Race List_: contains all the detected races with a pair of read/write accesses and their possible traces
  * _ECHO Deadlock List_: contains all the detected deadlocks with a pair of nested locks and their possible traces
  * _ECHO Concurrency Relation_: contains all read and write operations that may happen in parallel

### Large Benchmarks
The github repo includes 6 large, real-world Java projects. To test the practicality of ECHO on these projects, you can import them into your target workspace. 
