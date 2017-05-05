# Airport_Simulation
CSE6730 project. Object oriented programming applied to the discrete event simulation.

**Variables to change in the code** for different simulation configuration:
int airportTotalNum = 100;
int numInitials = 5; //airplane number per airport
int stopTime = 20;

**To Run the code**
Open terminal, cd /src. The commands to compile and run the simulation is as below:(IF using 8 LPs )
_MPJ(version: 044) and java(version: jdk1.8.0_121)_

Compile: 
javac -cp .:$MPJ_HOME/lib/mpj.jar *.java

Run:

mpjrun.sh -np 8 AirportSim

