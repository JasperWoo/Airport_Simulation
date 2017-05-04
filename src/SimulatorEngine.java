//Peijun Wu

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.TreeSet;
import mpi.*;


public class SimulatorEngine implements EventHandler {

    private double m_currentTime;
    private TreeSet<Event> m_eventList;
    private boolean m_running;
    public double m_lookAhead;
    private int bufLength = 20000;
    private double[] sendBuf;
    private int sizeOfSendBuf;
    private int sendCount[];
    private int sendDispls[];
    private double[] recvBuf;
    private int sizeOfRecvBuf;
    private int recvCount[];
    private int recvDispls[];
    private double[] LBTS = new double[1];//global LBTS
    private double[] fastestSpeed;
    private double[] shortestDistance;
    private double[] lookaheadTable;
    private double minLocalLB;




    SimulatorEngine() {
        m_running = false;
        m_currentTime = 0.0;
        m_eventList = new TreeSet<Event>();
    }
    
    public void allToAllInitialize() {
		int size = MPI.COMM_WORLD.Size();
		sendBuf = new double[bufLength];
		sizeOfSendBuf = 0;
		sendCount = new int[size];
		sendDispls = new int[size];
		recvBuf = new double[bufLength];
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
        	minLocalLB = getMinLBTSforCurrentLP();
    		m_running = true;
    		int[] running = new int[] {0};
            double startTime = System.nanoTime();
    		while (!m_eventList.isEmpty()) {
    			//reset alltoall buffers
    			allToAllInitialize();
    			double[] LocalLBTS = new double[1];//Local LBTS
    			LocalLBTS[0] = Simulator.getCurrentTime()+minLocalLB;
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
    			
    			organizeSendBuf();
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

            //only the 0 LP print the elapsed time
            if (rank == 0) {
                double endTime = System.nanoTime();
                //elapsed time in seconds
                double elapsedTime = (endTime - startTime) / 1000000000.0;
                System.out.println("Elapsed Time for the " + rank + "th LP: " + elapsedTime);
            }
    }
    
    public double getMinLBTSforCurrentLP(){
    	double minLB;
    	//int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();
        minLB=lookaheadTable[0];
        for (int i=1; i<size; i++){
        	if(lookaheadTable[i]<minLB) minLB = lookaheadTable[i];
        }
        
    	return minLB;
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

    				int speed = airEvent.getPlane().getSpeed();
    				int curAirport = Arrays.asList(AirportSim.airportList).indexOf(airEvent.getHandler());
    				for (int i = 0; i < AirportSim.airportTotalNum; i += 1 ){
    					if (!(i==curAirport)){
    						double dist = AirportSim.distanceMatrix[curAirport][i];
    	                    double m_flightTime = dist / speed;
    	                    double extraDelay = 0;
    	                    switch(airEvent.getType()) {
    	                    	case AirportEvent.PLANE_ARRIVES:
    	                    		extraDelay = AirportSim.airportList[curAirport].m_runwayTimeToLand+
    	                    			AirportSim.airportList[curAirport].m_requiredTimeOnGround+
    	                    			AirportSim.airportList[curAirport].m_runwayTimeToTakeoff+
	                    				AirportSim.airportList[curAirport].m_checkDestinationPeriod;
    	                    		break;
    	                    	case AirportEvent.PLANE_DEPARTS:
    	                    		extraDelay = AirportSim.airportList[curAirport].m_runwayTimeToTakeoff+
    	                    			AirportSim.airportList[curAirport].m_checkDestinationPeriod;
    	                    		break;
    	                    	case AirportEvent.PLANE_LANDED:
    	                    		extraDelay = AirportSim.airportList[curAirport].m_requiredTimeOnGround+
        	                    		AirportSim.airportList[curAirport].m_runwayTimeToTakeoff+
	                    				AirportSim.airportList[curAirport].m_checkDestinationPeriod;
    	                    		break;
    	                    	case AirportEvent.PLANE_TAKEOFF:
    	                    		//takeoff event has determined destination
    	                    		//the LB is calculated for next possible takeoff
    	                    		int destination = airEvent.getPlane().destination;                    		
    	                            double distance = AirportSim.distanceMatrix[curAirport][destination];
    	                            double takeoffeventflightTime = distance / speed;
    	                            
    	                    		extraDelay = AirportSim.airportList[curAirport].m_runwayTimeToTakeoff+
    	                    			takeoffeventflightTime+
    	                    			AirportSim.airportList[curAirport].m_runwayTimeToLand+
	                    				AirportSim.airportList[curAirport].m_requiredTimeOnGround+
	                    				AirportSim.airportList[curAirport].m_runwayTimeToTakeoff+
	                    				AirportSim.airportList[curAirport].m_checkDestinationPeriod;
    	                    		break;
    	                    		
    	                    
    	                    }
    	                    double LBi = airEvent.getTime() + m_flightTime + extraDelay;
    		    			if (LBi < LB){
    		    				LB = LBi;
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
    
    public void organizeSendBuf(){
    	//this method organize the send buffer in the order of LP
    	//each event has 5 double value, event number to LP is stored in sendCount[]
        int size = MPI.COMM_WORLD.Size();
    	double[][] tmpsendBuf = new double[size][bufLength];
    	int[] tmpSendCount = new int[size];
    	for (int i = 0; i < size; i ++){
    		tmpSendCount[i] = 0;
    	}
    	for (int i = 0; i < sizeOfSendBuf; i +=5){
    		int airportID = (int)sendBuf[i+2];
    		int curLPforCurEvent =AirportSim.airportList[airportID].getM_LPid();
    		tmpsendBuf[curLPforCurEvent][tmpSendCount[curLPforCurEvent]  ] = sendBuf[i  ];
    		tmpsendBuf[curLPforCurEvent][tmpSendCount[curLPforCurEvent]+1] = sendBuf[i+1];
    		tmpsendBuf[curLPforCurEvent][tmpSendCount[curLPforCurEvent]+2] = sendBuf[i+2];
    		tmpsendBuf[curLPforCurEvent][tmpSendCount[curLPforCurEvent]+3] = sendBuf[i+3];
    		tmpsendBuf[curLPforCurEvent][tmpSendCount[curLPforCurEvent]+4] = sendBuf[i+4];
    		tmpSendCount[curLPforCurEvent] += 5;
    	}
    	int curDisp = 0;
    	for (int i = 0; i < size; i ++){
    		for (int j = 0; j<sendCount[i]; j +=5){
    			sendBuf[curDisp] = tmpsendBuf[i][j];
    			sendBuf[curDisp + 1] = tmpsendBuf[i][j + 1];
    			sendBuf[curDisp + 2] = tmpsendBuf[i][j + 2];
    			sendBuf[curDisp + 3] = tmpsendBuf[i][j + 3];
    			sendBuf[curDisp + 4] = tmpsendBuf[i][j + 4];
    			curDisp += 5;
    		}
    	}
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
