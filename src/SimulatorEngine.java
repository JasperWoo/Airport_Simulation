//Peijun Wu

import java.sql.Time;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import mpi.*;


public class SimulatorEngine implements EventHandler {

    private double m_currentTime;
    private TreeSet<Event> m_eventList;
    private boolean m_running;
    
    private int rank;
    private int size;
    private mpi.Request req[] = new mpi.Request[size];
    private double[][] recvBuf = new double[size][6];
    private double[] fastestSpeed;
    private double[] shortestDistance;
    private double[] lookaheadTable;
    
    
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
    	m_running = true;
    	double startTime = System.nanoTime();
    	while (m_running) {
    		nullLoop();
    	}
    	sendNullMessage(-2);
    	double endTime = System.nanoTime();
    	//elapsed time in seconds
    	double elapsedTime = (endTime - startTime) / 1000000000.0;
    	System.out.println("Elapsed Time for the " + rank + "th LP: " + elapsedTime);
    }
    
    public void nullMessageInitialize() {
    	//get current LP's ID
        rank = MPI.COMM_WORLD.Rank();
        //get total number of LP
        size = MPI.COMM_WORLD.Size();
        
        queueCount = new int[size];
        queueCount[rank] = 1;
        recvBuf = new double[size][6];
        req = new mpi.Request[size];
        incomingQueue = new TreeSet<>();
    }    
    
    public void nullLoop() {
        for (int i = 0; i < size; i++) {
        	if (i == rank) continue;
        	
        	//if req[i] hasn't been initialize
        	if (req[i] == null) continue;
        	
        	//check if the receive has finished yet
        	if (!req[i].Is_null()) {
        		req[i].Test();
        		//the receive request hasn't finished
        		if (!req[i].Is_null()) continue;
        		
        		//put event in the incomingQueue and update incomingQueue and corresponding count
        		Message m = new Message(recvBuf[i]);
        		incomingQueue.add(m);
        		queueCount[i]++;
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
        
        //if it's not blocked
        if (blockedList.isEmpty()) {
        	double LBTS = getCurrentTime();
        	Message m = null;
        	while (!incomingQueue.isEmpty()) {
            	m = incomingQueue.pollFirst();
            	int fromLPid = (int) m.message[5];
            	queueCount[fromLPid]--;
            	
            	//if not a null message, then schedule a new event, 
            	//otherwise execute all event before the time of the null message.
            	if (m.message[2] >= 0) {
    	        	Event nextIncomingEvent = createEvent(m.message);
    	        	Simulator.schedule(nextIncomingEvent);
    	        	LBTS = nextIncomingEvent.getTime();
    	        	break;
            	} else if (m.message[2] == -1){
            		LBTS = m.message[0] + m.message[1];
            		
            		//check if the null message is out-dated
            		if (LBTS <= getCurrentTime()) {
            			LBTS = getCurrentTime();
            			//if the null message is out-dated and the queue is empty now
            			if (queueCount[fromLPid] == 0) {
            				break;
            			}
            		} 
            		else break;
            	}
            	else {
            		queueCount[fromLPid]++;
            	}
        	}
        	
        	//all the other processors have finished, set LBTS to infinity
        	if (incomingQueue.isEmpty() && (size == 1 || m == null || m.message[2] == -2))
        		LBTS = Double.MAX_VALUE;
        	 
    		while(m_running && m_eventList.first().getTime() <= LBTS){
    			Event event = m_eventList.pollFirst();
    	        m_currentTime = event.getTime();
    			event.getHandler().handle(event);
    		}
    		
    		m_currentTime = LBTS;
        } 
        //if it's blocked, send null message and wait.
        else {
        	sendNullMessage(-1);
        	for (int i : blockedList) {
        		if (req[i] == null){
        			req[i] = MPI.COMM_WORLD.Irecv(recvBuf[i], 0, 6, MPI.DOUBLE, i, 0);
        		}
        		
        		req[i].Wait();
	        	
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
		if (airplaneType == 9) 
			curAirplane = new Airplane(AirportSim.airplaneNameList[9], 707, 466);
		else 
			curAirplane = new Airplane(AirportSim.airplaneNameList[airplaneType], 614, 416);
		AirportEvent landingEvent = new AirportEvent(correctDelay,  AirportSim.airportList[destination],
                AirportEvent.PLANE_ARRIVES, curAirplane, passengerNum, eventStart);
		return landingEvent;
    }
    
    public void sendNullMessage(int type) {
    	for (int i = 0; i < size; i++) {
    		if (i == rank) continue;
    		
    		//send null message here
    		double[] sendNull = new double[6];
    		sendNull[0] = getCurrentTime();
    		
    		sendNull[1] = lookaheadTable[i];					//lookahead
    		sendNull[2] = type;					//use destination field to specify if it is a null message
    		sendNull[5] = rank;
    		MPI.COMM_WORLD.Isend(sendNull, 0, 6, MPI.DOUBLE, i, 0);
    	}
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
 

	public double[] getFastestSpeed() {
		return fastestSpeed;
	}


	public void setFastestSpeed(double[] fastestSpeed) {
		this.fastestSpeed = fastestSpeed;
	}


	public double[] getShortestDistance() {
		return shortestDistance;
	}


	public void setShortestDistance(double[] shortestDistance) {
		this.shortestDistance = shortestDistance;
	}


	public double[] getLookaheadTable() {
		return lookaheadTable;
	}


	public void setLookaheadTable(double[] lookaheadTable) {
		this.lookaheadTable = lookaheadTable;
	}
}
