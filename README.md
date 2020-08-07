# SWORD: A Fast Concurrency Bug Detection for The Whole Program
**This project is no longer under maintainance, apologies for the terrible code quality. There are a lot of follow-ups going on, if you have any specific question regarding this project, [email me](mailto:liyzunique@gmail.com).**

## Introduction
SWORD is an interactive Eclipse Plugin to detect data races and deadlocks in Java programs. The fundamental techniques (i.e., points-to analysis and static happens-before analysis) that SWORD adopts are in: 

[FSE'16] "[ECHO: Instantaneous In Situ Race Detection in the IDE](https://parasol.tamu.edu/~jeff/academic/echo.pdf)"

[PLDI'18] "[D4: Fast Concurrency Debugging with Parallel Differential Analysis](https://parasol.tamu.edu/~jeff/d4.pdf)"

## Software Dependencies
- Java 1.8 
- Eclipse PDE

## Quick Start
1. Clone the project
2. Import all the projects in Eclipse and build.

### How To Use
#### Start The Detection
1. Select a main class in the _Package Explorer_ 
2. Right click _ASER_ -> _SWORD_ to start the detection. 

#### SWORD Views
Goto _Windows_ -> _Show View_ -> _Others_ to open the ECHO views (i.e., _ECHO Race List_, _ECHO Deadlock List_, _ECHO Concurrency Relation_):
  * _SWORD Race List_: contains all the detected races with a pair of read/write accesses and their possible traces
  * _SWORD Deadlock List_: contains all the detected deadlocks with a pair of nested locks and their possible traces
  * _SWORD Concurrency Relation_: contains all read and write operations that may happen in parallel

## Large Benchmarks
The github repo includes 6 large, real-world Java projects. To test the practicality of SWORD on these projects, you can import them into your target workspace. 

Some large benchmarks are available [here](https://github.com/funemy/echo-benchmark)

## License
[MIT](LICENSE)
