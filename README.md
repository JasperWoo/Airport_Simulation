# Airport_Simulation
CSE6730 project. Object oriented programming applied to the discrete event simulation.

All the parameter need to change for different case
int airportTotalNum = 100;
int numInitials = 5; //airplane number per airport
int stopTime = 20;

MPJ compile and run   
IF using 30 LP  
mpjrun.sh -np 30 AirportSim  
javac -cp .:$MPJ_HOME/lib/mpj.jar *.java
