//Peijun Wu

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.TreeSet;

import mpi.*;
import mpjdev.natmpjdev.Intracomm;


public class SimulatorEngine implements EventHandler {

    private double m_currentTime;
    private TreeSet<Event> m_eventList;
    private boolean m_running;
    private double m_lookAhead;
    
    private int rank;
    private int size;
    private mpi.Request req[] = new mpi.Request[size];
    private double[][] recvBuf = new double[size][6];
    
    private TreeSet<Message> incomingQueue;
    private int[] queueCount;
    
	class Message implements Comparable<Message>{
		double[] message;
		public Message(double[] message) {
			this.message = message.clone();
		}
		
		@Override
		public int compareTo(Message m) {
			return Double.compare(message[0] + message[1], m.message[0] + m.message[1]);
		}
	}

    SimulatorEngine() {
        m_running = false;
        m_currentTime = 0.0;
        m_eventList = new TreeSet<Event>();
    }
    

    public static void printResult(int airportNum){
        NumberFormat formatter = new DecimalFormat("#0.00");
        Airport curAirport = AirportSim.airportList[airportNum];
        double circlingTimeInMins = curAirport.getCirclingTime() * 60;
        System.out.println("The total number of passengers arrived and departed, and circling time(mins) at "+ curAirport.getName() + ": " );
        System.out.println(curAirport.getNumArrived() + "," + curAirport.getNumDeparted() + "," + formatter.format(circlingTimeInMins));

    }

    void run() {
        m_running = true;
        while(m_running && !m_eventList.isEmpty()) {
            Event ev = m_eventList.pollFirst();
            m_currentTime = ev.getTime();
            ev.getHandler().handle(ev);
        }
        //Print the result of simulation:
        for (int i = 0; i<AirportSim.airportList.length;i++)
            printResult(i);
    }
    
    /*
     * NULL-Message run function and other helper functions
     */
    public void runNull() {
    	nullMessageInitialize();
    	while (m_running) {
    		nullLoop();
    	}
    }
    
    public void nullMessageInitialize() {
        rank = MPI.COMM_WORLD.Rank();
        size = MPI.COMM_WORLD.Size();
        queueCount = new int[size];
        queueCount[rank] = 1;
        req = new mpi.Request[size];
        recvBuf = new double[size][6];
    }    
    
    public void nullLoop() {
    	m_running = true;
        while(m_running && !m_eventList.isEmpty()) {
            Event ev = m_eventList.pollFirst();
            m_currentTime = ev.getTime();
            ev.getHandler().handle(ev);
        }

        if (!m_running) return;
        
        for (int i = 0; i < size; i++) {
        	if (i == rank) continue;
        	
        	//if the last receive has finished, the event should be scheduled now
        	if (req[i].Test() != null) {
        		//put event in the incomingQueue and update incomingQueue and corresponding count
        		Message m = new Message(recvBuf[i]);
        		incomingQueue.add(m);
        		queueCount[i]++;
        	}
        	else {
            	//if incoming queue for this LP is empty and non-blocking receive hasn't finished receiving, this means it 
            	//should be changed to blocking receive in the below blocking loop
        		//
        		//otherwise it can keep waiting the same non-blocking receive
        		if (queueCount[i] == 0) 
            		req[i].Cancel();
        		continue;
        	}	
        	
        	//safe to start another non-blocking receive
        	req[i] = MPI.COMM_WORLD.Irecv(recvBuf[i], 0, 6, MPI.DOUBLE, i, 0);
        }
        
        //check if it's blocked 
        List<Integer> blockedList = new LinkedList<>();
        for (int i = 0; i < size; i++) {
        	if (queueCount[i] == 0)
        		blockedList.add(i);
        }
        
        if (blockedList.isEmpty()) {
        	Message m = incomingQueue.pollFirst();
        	queueCount[(int) m.message[5]]--;
        	double LBTS = 0.0;
        	//if not a null message, then schedule a new event, 
        	//otherwise execute all event before the time null message specifies.
        	if (m.message[2] >= 0) {
	        	Event nextIncomingEvent = createEvent(m.message);
	        	schedule(nextIncomingEvent);
	        	LBTS = nextIncomingEvent.getTime();
        	} else {
        		LBTS = m.message[0] + m.message[1];
        	}
        	 
    		while(m_running && m_eventList.first().getTime() <= LBTS){
    			Event event = m_eventList.pollFirst();
    	        m_currentTime = event.getTime();
    			event.getHandler().handle(event);
    		}
        } else {
        	for (int i = 0; i < size; i++) {
        		if (i == rank) continue;
        		
        		//send null message here
        		double[] sendNull = new double[6];
        		sendNull[0] = getCurrentTime();
        		
        		sendNull[1] = LBTS;					//lookahead
        		sendNull[2] = -1;					//use destination field to specify if it is a null message
        		MPI.COMM_WORLD.Isend(sendNull, 0, 6, MPI.DOUBLE, i, 0);
        	}
        	for (int i : blockedList) {
        		MPI.COMM_WORLD.Recv(recvBuf[i], 0, 6, MPI.DOUBLE, i, 0);
        		//put event in the incomingQueue and update incomingQueue and corresponding count
        		Message m = new Message(recvBuf[i]);
        		incomingQueue.add(m);
        		queueCount[i]++;
        	}
        }   
    }
    
    Event createEvent(double[] recv) {
		double currentTime = Simulator.getCurrentTime();
		double eventStart = recv[0];
		double eventDelay = recv[1];
		double correctDelay = eventStart + eventDelay - currentTime;
		int destination = (int)recv[2];
		int airplaneType = (int)recv[3];
		int passengerNum = (int)recv[4];
		Airplane curAirplane;
		if (airplaneType == 0) 
			curAirplane = new Airplane("Boe747", 614, 416);
		else 
			curAirplane = new Airplane("A380_1", 634, 853);
		AirportEvent landingEvent = new AirportEvent(correctDelay,  AirportSim.airportList[destination],
                AirportEvent.PLANE_ARRIVES, curAirplane, passengerNum, eventStart);
		return landingEvent;
    }

    
    public void handle(Event event) {
        SimulatorEvent ev = (SimulatorEvent)event;

        switch(ev.getType()) {
            case SimulatorEvent.STOP_EVENT:
                m_running = false;
                System.out.println("Simulator stopping at time: " + ev.getTime());
                break;
            default:
                System.out.println("Invalid event type");
        }
    }

    public void schedule(Event event) {
        m_eventList.add(event);
    }

    public void stop() {
        m_running = false;
    }

    public double getCurrentTime() {
        return m_currentTime;
    }
    
	public void setLookAhead(double lookAhead) {
		m_lookAhead = lookAhead;
	}
}
