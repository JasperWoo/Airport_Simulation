//Peijun Wu

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.TreeSet;
import mpi.*;
import mpjdev.natmpjdev.Intracomm;


public class SimulatorEngine implements EventHandler {

    private double m_currentTime;
    private TreeSet<Event> m_eventList;
    private boolean m_running;
    public double m_lookAhead;
    
    private double[] sendBuf;
    private int sizeOfSendBuf;
    private int sendCount[];
    private int sendDispls[];
    private double[] recvBuf;
    private int sizeOfRecvBuf;
    private int recvCount[];
    private int recvDispls[];
    private double[] LBTS = new double[1];//global LBTS



    SimulatorEngine() {
        m_running = false;
        m_currentTime = 0.0;
        m_eventList = new TreeSet<Event>();
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
        	int rank = MPI.COMM_WORLD.Rank();
        	int size = MPI.COMM_WORLD.Size();
        	
    		m_running = true;
    		int[] running = new int[] {0};
    		while (!m_eventList.isEmpty()) {
    			//reset alltoall buffers
    			allToAllInitialize();
    			double[] LocalLBTS = new double[1];//Local LBTS
    			LocalLBTS[0] = getLocalLBTS();
    			//LocalLBTS[0] = m_eventList.first().getTime() + m_lookAhead;
    			
    			MPI.COMM_WORLD.Allreduce(LocalLBTS, 0, LBTS, 0, 1, MPI.DOUBLE, MPI.MIN);
    			if (rank == 0) {System.out.println("LBTS = "+ LBTS[0]);}
    			while(running[0] == 0 && m_eventList.first().getTime() <= LBTS[0]){
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
    			if (all_running[0] == 1){
    				break;
    			}
    			
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
    				if (airplaneType == 9) 
    					curAirplane = new Airplane(AirportSim.airplaneNameList[9], 707, 466);
    				else 
    					curAirplane = new Airplane(AirportSim.airplaneNameList[airplaneType], 614, 416);
    				AirportEvent landingEvent = new AirportEvent(correctDelay,  AirportSim.airportList[destination],
                            AirportEvent.PLANE_ARRIVES, curAirplane, passengerNum, eventStart);
    				Simulator.schedule(landingEvent);
    			}
    		}
    }
    
    public double getLocalLBTS(){
    	double LB = 0;
    	for (Event c_Event : m_eventList){
    		if (c_Event.getType() == SimulatorEvent.STOP_EVENT){
    			LB = c_Event.getTime();  //get stopping time, this is the max time
    		}
    	}
    	
    	for (Event c_Event : m_eventList){
    		if (!(c_Event.getType() == SimulatorEvent.STOP_EVENT)){
    			AirportEvent airEvent = (AirportEvent)c_Event;
    			if (airEvent.getType() == AirportEvent.PLANE_TAKEOFF){
    				double LBi = airEvent.getTime() ; //+ airEvent.getLastEventTime();
	    			if (LBi < LB){
	    				LB = LBi;
	    			}
    			}else{
    				//calculate lookahead time
    				int speed = airEvent.getPlane().getSpeed();
    				int curAirport = Arrays.asList(AirportSim.airportList).indexOf(airEvent.getHandler());
    				for (int i = 0; i < AirportSim.airportTotalNum; i += 1 ){
    					if (!(i==curAirport)){
    						double dist = AirportSim.distanceMatrix[curAirport][i];
    	                    double m_flightTime = dist / speed;
    	                    //double LBi = airEvent.getTime() + airEvent.getLastEventTime()+m_flightTime;
    	                    double LBi = airEvent.getTime() + m_flightTime;
    		    			if (LBi < LB){
    		    				LB = LBi;
    		    			}
    					}
    				}
    			}
    		}
    	}
    	
    	return LB;
    }

    public void updateSendBuf(double startTime, double delay, double airportId, 
			double airplaneType, double passengerNum) {
    		sendBuf[sizeOfSendBuf] = startTime;
    		sendBuf[sizeOfSendBuf + 1] = delay;
    		sendBuf[sizeOfSendBuf + 2] = airportId;
    		sendBuf[sizeOfSendBuf + 3] = airplaneType;
    		sendBuf[sizeOfSendBuf + 4] = passengerNum;
    		sizeOfSendBuf += 5;
    		//sendCount[(int)airportId] += 5;
    		sendCount[AirportSim.airportList[(int)airportId].getM_LPid()] += 5;
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
                int rank = MPI.COMM_WORLD.Rank();
                System.out.println("Simulator at LP "+rank+" stopping at time: " + ev.getTime());
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
