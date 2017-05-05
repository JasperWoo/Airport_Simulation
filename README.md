# Airport_Simulation
CSE6730 project. Object oriented programming applied to the discrete event simulation.

MPJ Download Website: http://mpj-express.org/download.php 

### Variables to change in the code:
int airportTotalNum = 100;  
int numInitials = 5; //airplane number per airport  
int stopTime = 20;  

### To Run the code
First, download and install MPJ library. Then open terminal, cd /src. The commands to compile and run the simulation is as below:(IF using 8 LPs)  
_MPJ(version: 044) and java(version: jdk1.8.0_121)_

Compile: 

IF $MPJ_HOME has been configured:
  javac -cp .:$MPJ_HOME/lib/mpj.jar *.java
ELSE:
  javac -cp .:../../mpj-v0_44-1/lib/mpj.jar *.java

Run:

mpjrun.sh -np 8 AirportSim

