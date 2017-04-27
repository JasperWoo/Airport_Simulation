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
    
    private double[] sendBuf;
    private int sizeOfSendBuf;
    private int sendCount[];
    private int sendDispls[];
    private double[] recvBuf;
    private int sizeOfRecvBuf;
    private int recvCount[];
    private int recvDispls[];


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
    
    public void nullMessageInitialize() {
        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();
        queueCount = new int[size];
        queueCount[rank] = 1;
    }
    

    
    public void allToAllInitialize() {
		int size = MPI.COMM_WORLD.Size();
		sendBuf = new double[500];
		sizeOfSendBuf = 0;
		sendCount = new int[size];
		sendDispls = new int[size];
		recvBuf = new double[500];
		sizeOfRecvBuf = 0;
		recvCount = new int[size];
		recvDispls = new int[size];
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
     * YAWNS run function and other helper functions
     */
    void runYAWNS() {
    		m_running = true;
    		int[] running = new int[] {0};
    		while (!m_eventList.isEmpty()) {
    			//reset alltoall buffers
    			allToAllInitialize();
    			double LBTS = m_eventList.first().getTime() + m_lookAhead;
    			while(running[0] == 0 && m_eventList.first().getTime() <= LBTS){
    				Event event = m_eventList.pollFirst();
    				
    		        if (event.getType() == SimulatorEvent.STOP_EVENT) {
    		        		running[0] = 1;
    		        }
    		        m_currentTime = event.getTime();
    				event.getHandler().handle(event);
    			}
    			
    			//use AllReduce on running to determine if all LP has stopped running.
    			int[] all_running = new int[1];
    			MPI.COMM_WORLD.Allreduce(running, 0, all_running, 0, 1, MPI.INT, MPI.PROD);
    			if (all_running[0] == 1) break;
    			
    			updateRecvBuf();
    			MPIUtil.allToAll(sendBuf, 0, sendCount, sendDispls, MPI.DOUBLE, 
    					recvBuf, 0, recvCount, recvDispls, MPI.DOUBLE);
    			
    			for (int i = 0; i < sizeOfRecvBuf; i += 5) {
    				double currentTime = Simulator.getCurrentTime();
    				double eventStart = recvBuf[i];
    				double eventDelay = recvBuf[i + 1];
    				
    				//According to YAWNS algorithm, correctDelay must be >= 0
    				double correctDelay = eventStart + eventDelay - currentTime;
    				int destination = (int)recvBuf[i + 2];
    				int airplaneType = (int)recvBuf[i + 3];
    				int passengerNum = (int)recvBuf[i + 4];
    				Airplane curAirplane;
    				if (airplaneType == 0) 
    					curAirplane = new Airplane("Boe747", 614, 416);
    				else 
    					curAirplane = new Airplane("A380_1", 634, 853);
    				AirportEvent landingEvent = new AirportEvent(correctDelay,  AirportSim.airportList[destination],
                            AirportEvent.PLANE_ARRIVES, curAirplane, passengerNum, eventStart);
    				Simulator.schedule(landingEvent);
    			}
    		}
    }
    
    void runNull() {
    	
    }
    
    void nullLoop() {
        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();
        
    	m_running = true;
        while(m_running && !m_eventList.isEmpty()) {
            Event ev = m_eventList.pollFirst();
            m_currentTime = ev.getTime();
            ev.getHandler().handle(ev);
        }
        
        mpi.Request req[] = new mpi.Request[size];
        double[][] recvBuf = new double[size][6];
        for (int i = 0; i < size; i++) {
        	if (i == rank) continue;
        	
        	//if the last receive has finished, the event should be scheduled now
        	if (req[i].Test() != null) {
        		//put event in the incomingQueue and update incomingQueue and corresponding count
        		Message m = new Message(recvBuf[i]);
        		incomingQueue.add(m);
        		queueCount[i]++;
        	}
        	
        	//if incoming queue for this LP is empty and non-blocking receive hasn't finished receiving, this means it 
        	//should be changed to blocking receive in the below blocking loop
        	else if (queueCount[i] == 0) {
        		req[i].Cancel();	
        		continue;
        	}
        	
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
        	Event nextIncomingEvent = createEvent(m.message);
        	schedule(nextIncomingEvent);
        	
        	double LBTS = nextIncomingEvent.getTime();
    		while(m_running && m_eventList.first().getTime() <= LBTS){
    			Event event = m_eventList.pollFirst();
    	        m_currentTime = event.getTime();
    			event.getHandler().handle(event);
    		}
        } else {
        	for (int i = 0; i < size; i++) {
        		if (i == rank) continue;
        		
        		//send null message here
        		MPI.COMM_WORLD.Isend(arg0, arg1, arg2, arg3, arg4, arg5);
        	}
        	for (int i : blockedList) {
        		MPI.COMM_WORLD.Recv(recvBuf[i], 0, 6, MPI.DOUBLE, i, 0);
        		//put event in the incomingQueue and update incomingQueue and corresponding count
        		
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


    public void updateSendBuf(double startTime, double delay, double airportId, 
			double airplaneType, double passengerNum) {
    		sendBuf[sizeOfSendBuf] = startTime;
    		sendBuf[sizeOfSendBuf + 1] = delay;
    		sendBuf[sizeOfSendBuf + 2] = airportId;
    		sendBuf[sizeOfSendBuf + 3] = airplaneType;
    		sendBuf[sizeOfSendBuf + 4] = passengerNum;
    		sizeOfSendBuf += 5;
    		sendCount[(int)airportId] += 5;
    }
    
    public void updateRecvBuf() {
    		int size = MPI.COMM_WORLD.Size();
    		int displs = 0;
    		
    		//update sendDispls[]
    		for (int i = 0; i < size; i++) {
    			sendDispls[i] = displs;
    			displs += sendCount[i];
    		}
    		
    		//use alltoall to update recvCount[]
    		MPI.COMM_WORLD.Alltoall(sendCount, 0, 1, MPI.INT, recvCount, 0, 1, MPI.INT);
    		
    		displs = 0;
    		//update recvDispls[]
    		for (int i = 0; i < size; i++) {
    			recvDispls[i] = displs;
    			displs += recvCount[i];
    			sizeOfRecvBuf += recvCount[i];
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
    
	public void setLookAhead(double lookAhead) {
		m_lookAhead = lookAhead;
	}
}
