//Peijun Wu
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.LinkedList;

public class Airport implements EventHandler {

    //TODO add landing and takeoff queues, random variables
    //Did in other places.
    private int m_inTheAir;
    private int m_onTheGround;

    private boolean m_freeRunway;
    private double m_runwayTimeToLand;
    private double m_runwayTimeToTakeoff;
    private double m_requiredTimeOnGround;
    private double m_circlingTime;
    private String m_airportName;
    private int m_numArrived;
    private int m_numDeparted;
    private LinkedList<Event> m_runwayList;

    public Airport(String name, double runwayTimeToLand, double requiredTimeOnGround, double runwayTimeToTakeoff) {
        m_airportName = name;
        m_inTheAir =  0;
        m_onTheGround = 0;
        m_numArrived = 0;
        m_numDeparted = 0;
        m_circlingTime = 0;
        m_freeRunway = true;
        m_runwayTimeToLand = runwayTimeToLand;
        m_requiredTimeOnGround = requiredTimeOnGround;
        m_runwayTimeToTakeoff = runwayTimeToTakeoff;
        m_runwayList = new LinkedList<Event>();
    }

    public String getName() {
        return m_airportName;
    }
    public int getNumArrived(){ return m_numArrived; }
    public int getNumDeparted(){ return m_numDeparted; }
    public double getCirclingTime(){ return m_circlingTime; }
    public double getInTheAir(){ return m_inTheAir; }
    public double getOnTheGround(){ return m_onTheGround; }

    private void continueRunway(Event airEvent){
        //Once the runway is available, immediately check if there is any plane wanting to use the runway.
        if(!m_runwayList.isEmpty()){
            AirportEvent nextEvent = (AirportEvent) m_runwayList.removeFirst();
            //If the next event is PLANE_LANDED, calculate the circling time.
            if (nextEvent.getType() == 1)
                m_circlingTime += airEvent.getTime() - nextEvent.getLastEventTime();
            Simulator.schedule(nextEvent);
        }
        else
            m_freeRunway = true;
    }

    public void handle(Event event) {
        AirportEvent airEvent = (AirportEvent)event;
        int curAirport = Arrays.asList(AirportSim.airportList).indexOf(airEvent.getHandler());
        NumberFormat formatter = new DecimalFormat("#0.00");
        switch(airEvent.getType()) {
            case AirportEvent.PLANE_ARRIVES:
                m_inTheAir++;
                System.out.println(formatter.format(Simulator.getCurrentTime()) + "(hours): flight arrived at airport " + AirportSim.airportList[curAirport].getName());
                AirportEvent landedEvent = new AirportEvent(m_runwayTimeToLand, this, AirportEvent.PLANE_LANDED, airEvent.getPlane(), airEvent.getNumPassengers(), airEvent.getTime());
                //Since this process takes up the runway, we need to check if the runway is empty.
                if(m_freeRunway) {
                    m_freeRunway = false;
                    Simulator.schedule(landedEvent);
                }
                else
                    m_runwayList.add(landedEvent);
                break;

            case AirportEvent.PLANE_LANDED:
                m_inTheAir--;
                m_onTheGround++;
                System.out.println(formatter.format(Simulator.getCurrentTime())+ "(hours): flight lands at airport " + AirportSim.airportList[curAirport].getName() + " with " + airEvent.getNumPassengers() + " passengers.");
                m_numArrived += airEvent.getNumPassengers();
                //This process does not take up the runway, no need to check.
                AirportEvent departureEvent = new AirportEvent( m_requiredTimeOnGround, this, AirportEvent.PLANE_DEPARTS, airEvent.getPlane(), airEvent.getNumPassengers(), airEvent.getTime());
                Simulator.schedule(departureEvent);
                continueRunway(airEvent);
                break;

            case AirportEvent.PLANE_DEPARTS:
                //Set random number of passengers.
                int newNumPassengers = ThreadLocalRandom.current().nextInt(airEvent.getPlane().getCapacity()/2, airEvent.getPlane().getCapacity() + 1);
                m_numDeparted += newNumPassengers;
                //Print out departing msg.
                System.out.println(formatter.format(Simulator.getCurrentTime())+ "(hours): flight is ready to depart " + " with " + newNumPassengers + " passengers.");
                AirportEvent takeoffEvent = new AirportEvent(m_runwayTimeToTakeoff, this, AirportEvent.PLANE_TAKEOFF, airEvent.getPlane(), newNumPassengers, airEvent.getTime());
                //Since this event takes up the runway
                if(m_freeRunway) {
                    m_freeRunway = false;
                    Simulator.schedule(takeoffEvent);
                }
                else
                    m_runwayList.add(takeoffEvent);
                break;

            case AirportEvent.PLANE_TAKEOFF:
                //Choose a random remote airport, get corresponding distance and flight time. Pass it to the  AirportEvent.
                m_onTheGround--;

                //Choose a random remote airport, get corresponding distance and flight time. Pass it to the  AirportEvent.
                int numAirports = AirportSim.airportList.length;
                int nextAirport;
                // AUS and SEA don't take A380.
                if (airEvent.getPlane().getName() == "A380"){
                    int[] selectAirports = {0,2,4};
                    do
                        nextAirport = selectAirports[ThreadLocalRandom.current().nextInt(0,selectAirports.length)];
                    while(nextAirport == curAirport); //Prevent next airport being the same as the current airport.
                }
                else{
                    do
                        nextAirport = ThreadLocalRandom.current().nextInt(0,numAirports);
                    while(nextAirport == curAirport); //Prevent next airport being the same as the current airport.
                }

                double dist = AirportSim.distAirports[curAirport][nextAirport];
                double m_flightTime = dist / airEvent.getPlane().getSpeed();

                //This process does not take up the runway, no need to check.
                AirportEvent landingEvent = new AirportEvent( m_flightTime,  AirportSim.airportList[nextAirport], AirportEvent.PLANE_ARRIVES, airEvent.getPlane(), airEvent.getNumPassengers(), airEvent.getTime()); //The number of passengers is just the newNumPassengers
                System.out.println(formatter.format(Simulator.getCurrentTime()) + "(hours): flight takes off at airport " + m_airportName + " and flies to " + AirportSim.airportList[nextAirport].getName() + ".");
                Simulator.schedule(landingEvent);
                continueRunway( airEvent);
                break;
        }
    }
}
