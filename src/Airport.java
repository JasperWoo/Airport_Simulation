//Peijun Wu
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import mpi.MPI;

public class Airport implements EventHandler {

    //TODO add landing and takeoff queues, random variables
    //Did in other places.
    private int m_inTheAir;
    private int m_onTheGround;

    private int m_numRunways;
    private boolean m_freeRunways[];
    private double m_runwayTimeToLand;
    private double m_runwayTimeToTakeoff;
    private double m_requiredTimeOnGround;
    private double m_checkDestinationPeriod = 0.05;
    private double m_circlingTime;
    private String m_airportName;
    private int m_numArrived;
    private int m_numDeparted;
    private Queue<Event> m_runwayQueue;
    private int m_airCapacity;
    private int m_groundCapacity;
    private boolean m_supportA380 = false;
    private double m_Lat;
    private double m_Long;
    private int m_LPid;


    public Airport(String name, double runwayTimeToLand, double requiredTimeOnGround, 
    		double runwayTimeToTakeoff, int groundCapacity, int airCapacity, 
    		int numRunways, boolean supportA380, int LPid, double Long, double Lat) {
        m_airportName = name;
        m_inTheAir =  0;
        m_onTheGround = 0;
        m_numArrived = 0;
        m_numDeparted = 0;
        m_circlingTime = 0;
        m_runwayTimeToLand = runwayTimeToLand;
        m_requiredTimeOnGround = requiredTimeOnGround;
        m_runwayTimeToTakeoff = runwayTimeToTakeoff;
        m_runwayQueue = new LinkedList<Event>();
        m_airCapacity = airCapacity;
        m_groundCapacity = groundCapacity;
        m_numRunways = numRunways;
        m_freeRunways = new boolean[m_numRunways];
        m_supportA380 = supportA380;
        m_Lat = Lat;
        m_Long = Long;
        m_LPid = LPid;
        Arrays.fill(m_freeRunways, true);
    }

    public String getName() { return m_airportName;}
    public int getGroundCapacity() {return m_groundCapacity;}

    public int getAirCapacity() {return m_airCapacity;}
    public int getNumArrived(){ return m_numArrived; }
    public int getNumDeparted(){ return m_numDeparted; }
    public double getCirclingTime(){ return m_circlingTime; }
    public double getInTheAir(){ return m_inTheAir; }
    public double getOnTheGround(){ return m_onTheGround; }
    public double getM_Lat() {
        return m_Lat;
    }
    public double getM_Long(){
        return m_Long;
    }

    // check if one of runways is free
    private void checkRunways(Airplane curAirplane, AirportEvent airportEvent){
        boolean flag = false;
        for (int i = 0; i < m_numRunways; i++){
            if (m_freeRunways[i]){
                flag = true;
                curAirplane.runway_number = i;
                break;
            }
        }
        // if so, we can schedule new event
        if(flag) {
            m_freeRunways[curAirplane.runway_number] = false;
            Simulator.schedule(airportEvent);
        }
        else
            m_runwayQueue.add(airportEvent);
    }
    // check if destination is reachable based on air capacity and ground capacity
    private boolean checkDestCapacity(int nextAirport){
        Airport airport = AirportSim.airportList[nextAirport];
        if(airport.getInTheAir()<airport.getAirCapacity() && airport.getOnTheGround()<airport.getGroundCapacity()){
            return true;
        }else
            return false;
    }


    private void continueRunway(Event event){
        AirportEvent airEvent = (AirportEvent)event;
        Airplane curairplane = airEvent.getPlane();
        //Once the runway is available, immediately check if there is any plane wanting to use the runway.
        if(!m_runwayQueue.isEmpty()){
            AirportEvent nextEvent = (AirportEvent) m_runwayQueue.remove();
            //If the next event is PLANE_LANDED, calculate the circling time.
            if (nextEvent.getType() == 1)
                m_circlingTime += airEvent.getTime() - nextEvent.getLastEventTime();
            Airplane nextAirplane = nextEvent.getPlane();
            nextAirplane.runway_number = curairplane.runway_number;
            Simulator.schedule(nextEvent);
        }
        else{
            m_freeRunways[curairplane.runway_number] =  true;
        }

    }

    public void handle(Event event) {
        AirportEvent airEvent = (AirportEvent)event;
        int curAirport = Arrays.asList(AirportSim.airportList).indexOf(airEvent.getHandler());
        Airplane curAirplane = airEvent.getPlane();
        NumberFormat formatter = new DecimalFormat("#0.00");
        switch(airEvent.getType()) {
            case AirportEvent.PLANE_ARRIVES:
                m_inTheAir++;
                System.out.println(formatter.format(Simulator.getCurrentTime()) + "(hours): flight " + curAirplane.getName() + " arrived at airport " + AirportSim.airportList[curAirport].getName());
                AirportEvent landedEvent = new AirportEvent(m_runwayTimeToLand, this, AirportEvent.PLANE_LANDED, curAirplane, airEvent.getNumPassengers(), airEvent.getTime());
                //Since this process takes up the runway, we need to check if the runway is empty.
                checkRunways(curAirplane, landedEvent);
                break;

            case AirportEvent.PLANE_LANDED:
                m_inTheAir--;
                m_onTheGround++;
                System.out.println(formatter.format(Simulator.getCurrentTime())+ "(hours): flight " + curAirplane.getName() + " lands at airport " + AirportSim.airportList[curAirport].getName() + " with " + airEvent.getNumPassengers() + " passengers.");
                m_numArrived += airEvent.getNumPassengers();
                //This process does not take up the runway, no need to check.
                AirportEvent departureEvent = new AirportEvent( m_requiredTimeOnGround, this, AirportEvent.PLANE_DEPARTS, curAirplane, airEvent.getNumPassengers(), airEvent.getTime());
                Simulator.schedule(departureEvent);
                continueRunway(airEvent);
                break;

            case AirportEvent.PLANE_DEPARTS:
                //Set random number of passengers.
                int newNumPassengers = ThreadLocalRandom.current().nextInt(curAirplane.getCapacity()/2, curAirplane.getCapacity() + 1);
                m_numDeparted += newNumPassengers;
                //Print out departing msg.
                System.out.println(formatter.format(Simulator.getCurrentTime())+ "(hours): flight " + curAirplane.getName() + " is ready to depart " + " with " + newNumPassengers + " passengers.");
                
                //Choose a random remote airport, get corresponding distance and flight time. Pass it to the  AirportEvent.
                int numAirports = AirportSim.airportList.length;
                int[] airportIndex = IntStream.range(0, numAirports).toArray();
                int nextAirport;
                // AUS and SEA don't take A380.
                if (curAirplane.getName().equals("A380")){
                    do
                        nextAirport = airportIndex[ThreadLocalRandom.current().nextInt(0, numAirports)];
                    while(nextAirport == curAirport && !AirportSim.airportList[nextAirport].m_supportA380); //Prevent next airport being the same as the current airport.
                }
                else{
                    do
                        nextAirport = ThreadLocalRandom.current().nextInt(0,numAirports);
                    while(nextAirport == curAirport); //Prevent next airport being the same as the current airport.
                }

                //record the destination for takeoffEvent to check destination capacity.
                curAirplane.destination = nextAirport;
                
                //create a new takeoffEvent with delay m_checkDestinationPeriod.
                AirportEvent takeoffEvent = new AirportEvent(m_checkDestinationPeriod, this, AirportEvent.PLANE_TAKEOFF, curAirplane, newNumPassengers, airEvent.getTime());
                
                checkRunways(curAirplane, takeoffEvent);
                
                break;

            case AirportEvent.PLANE_TAKEOFF:
            		int destination = curAirplane.destination;
            		
            		//check the capacity of destination airport
                if(checkDestCapacity(destination)){
                		m_onTheGround--;
                		
                		//reset airplane's destination to -1
                		curAirplane.destination = -1;
                		
                    double dist = AirportSim.distanceMatrix[curAirport][destination];
                    double m_flightTime = dist / curAirplane.getSpeed();
                    double delay = m_runwayTimeToTakeoff + m_flightTime;
                   
                    //This process does not take up the runway, no need to check.
                    AirportEvent landingEvent = new AirportEvent(delay,  AirportSim.airportList[destination],
                            AirportEvent.PLANE_ARRIVES, curAirplane, airEvent.getNumPassengers(), airEvent.getTime()); //The number of passengers is just the newNumPassengers
                    System.out.println(formatter.format(Simulator.getCurrentTime()) + "(hours): flight " + curAirplane.getName() + " takes off at airport " +
                            m_airportName + " and flies to " + AirportSim.airportList[destination].getName() + ".");
                    
                    int destId = AirportSim.airportList[destination].getM_LPid();
                    int startId = this.getM_LPid();
                    
                    //if destination is in the same LP, then schedule it,
                    //otherwise send message to that LP.
                    
                    //Simulator.schedule(landingEvent);
                    
                    if (startId == destId) {
                        Simulator.schedule(landingEvent);
                    }
                    else {
                    		double[] message = new double[6];
                    		message[0] = Simulator.getCurrentTime();
                    		message[1] = delay;
                    		message[2] = (double)destination;
                    		message[3] = curAirplane.getName().equals("A380_1")? 1 : 0;
                    		message[4] = airEvent.getNumPassengers();
                    		message[5] = startId;
                    		
                    		MPI.COMM_WORLD.Isend(message, 0, 6, MPI.DOUBLE, destId, 0);
                    }
                    continueRunway(airEvent);  
                    
                    
                }else{
                		//if destination capacity is not enough, delay it by adding it to queue.
                		continueRunway(airEvent);
                		m_runwayQueue.add(airEvent);
                }

                break;
        }
    }

	public int getM_LPid() {
		return m_LPid;
	}
}
