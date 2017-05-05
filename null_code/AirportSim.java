//Peijun Wu
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import com.sun.prism.Material;

import mpi.*;

public class AirportSim {
	public static int airportTotalNum = 100;
    public static Airport[] airportList = new Airport[airportTotalNum];
    public static String[] airplaneNameList = new String[10];
    public static final double[][] distanceMatrix = new double[airportList.length][airportList.length];
    
    //static Datatype typeAirEvent;
    public static void main(String[] args) {
        //Implemented different airplanes air airports
        int numInitials = 100;
        int stopTime = 50;
        
        MPI.Init(args);
        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();
        
        Simulator.getSim().setFastestSpeed(new double[size]);
        Simulator.getSim().setShortestDistance(new double[size]);
        Simulator.getSim().setLookaheadTable(new double[size]);
        
        double fastestSpeed = 707;
        double[] shortestDistance = Simulator.getSim().getShortestDistance();
        double[] lookaheadTable = Simulator.getSim().getLookaheadTable();
        Arrays.fill(shortestDistance, Double.MAX_VALUE);
        
        String csvAirports = "../data_airports.csv";
        BufferedReader br = null;
        String line = "";
        String csvSeparator = ",";

        // Read airport information from .csv and initialize airports.
        try {
            br = new BufferedReader(new FileReader(csvAirports));
            line = br.readLine(); // Skip the first line
            for (int i=0;i<airportTotalNum;i++){
                line = br.readLine();
                // use comma as separator
                String [] values = line.split(csvSeparator)  ;
                airportList[i] = new Airport(values[0], Double.valueOf(values[1]), 
                        Double.valueOf(values[2]), Double.valueOf(values[3]), Integer.valueOf(values[4]), 
                        Integer.valueOf(values[5]), Integer.valueOf(values[6]), Boolean.valueOf(values[7]), 
                        Double.valueOf(values[8]), Double.valueOf(values[9])); //Los Angelas
            }
        }  catch (FileNotFoundException e){
            e.printStackTrace();
        }  catch (IOException e){
            e.printStackTrace();
        }  finally {
            if(br!=null){
                try{
                    //System.out.println("The number of airports specified in the code may exceed the data available!");
                    br.close();
                } catch(IOException e){
                    e.printStackTrace();
                }
            }
        }
        
        //distribute airport total n, total LP is p
        int currentNtoSetLP = 0;
        int ceilLPnumber = Math.floorMod(airportTotalNum, size);
        int ceilAirportNumber = (int)Math.ceil(((double)airportTotalNum)/size);
        int floorLPnumber = size - ceilLPnumber; 
        int floorAirportNumber = (int)Math.floor(((double)airportTotalNum)/size);
        for (int i = 0; i < ceilAirportNumber*ceilLPnumber; i++){
        	airportList[i].setM_LPid((int)(Math.floorDiv(i, ceilAirportNumber)));
        	currentNtoSetLP = i;
        }
        for (int i = currentNtoSetLP+1; i<airportTotalNum; i++){
        	airportList[i].setM_LPid((int)Math.floor((i-ceilAirportNumber*ceilLPnumber)/floorAirportNumber)+ceilLPnumber);
        }
        
        
        for (int i = 0; i < airportList.length; i++) {
            for (int j = 0; j < airportList.length; j++) {
                if (i == j) {
                    distanceMatrix[i][j] = 0;
                    continue;
                }
                int LPid = airportList[i].getM_LPid();
                double distance = distanceMatrix[i][j] = distanceMatrix[j][i] = 3959.0 * Math.acos(Math.sin(Math.toRadians(airportList[i].getM_Lat())) *
                        Math.sin(Math.toRadians(airportList[j].getM_Lat())) +
                        Math.cos(Math.toRadians(airportList[i].getM_Lat())) * Math.cos(Math.toRadians(airportList[j].getM_Lat()))
                                * Math.cos(Math.toRadians(airportList[i].getM_Long() - airportList[j].getM_Long())));
                if (distance < shortestDistance[LPid]) 
                	shortestDistance[LPid] = distance;
            }
        }
        
        for (int i = 0; i < size; i++) {
        	if (i == rank) continue;
        	lookaheadTable[i] = shortestDistance[i] / fastestSpeed;
        }
        
        Simulator.stopAt(stopTime);
        
        //Initiate airplane with name Boe707, 717， 727， 737， 747， 757， 767， 777， 787，A380
        airplaneNameList[0]="Boe707";
        airplaneNameList[1]="Boe717";
        airplaneNameList[2]="Boe727";
        airplaneNameList[3]="Boe737";
        airplaneNameList[4]="Boe747";
        airplaneNameList[5]="Boe757";
        airplaneNameList[6]="Boe767";
        airplaneNameList[7]="Boe777";
        airplaneNameList[8]="Boe787";
        airplaneNameList[9]="A380";
        int startAirportID;
        int endAirportID;
        if (rank < ceilLPnumber){
            startAirportID = rank*ceilAirportNumber;
            endAirportID = startAirportID + ceilAirportNumber;
        }else{
            startAirportID = ceilLPnumber*ceilAirportNumber + (rank-ceilLPnumber)*floorAirportNumber;
            endAirportID = startAirportID + floorAirportNumber;
        }
        for (int i = startAirportID; i< endAirportID; i++){
            if (airportList[i].m_supportA380 == true){
                Airplane newAirplane = new Airplane(airplaneNameList[9], 707, 466);
                AirportEvent departureEvent = new AirportEvent(0, airportList[i], AirportEvent.PLANE_DEPARTS, newAirplane, 0, 0);
                Simulator.schedule(departureEvent);
                if (numInitials > 1){
                    for (int j = 1; j<numInitials;j++){
                        Airplane newAirplane2 = new Airplane(airplaneNameList[Math.floorMod(j-1, 9)], 614, 416);
                        AirportEvent departureEvent2 = new AirportEvent(0, airportList[i], AirportEvent.PLANE_DEPARTS, newAirplane2, 0, 0);
                        Simulator.schedule(departureEvent2);
                    }
                }
            }else{
                if (numInitials > 1){
                    for (int j = 0; j<numInitials;j++){
                        Airplane newAirplane2 = new Airplane(airplaneNameList[Math.floorMod(j, 9)], 614, 416);
                        AirportEvent departureEvent2 = new AirportEvent(0, airportList[i], AirportEvent.PLANE_DEPARTS, newAirplane2, 0, 0);
                        Simulator.schedule(departureEvent2);
                    }
                }
            }
        }
        
        Simulator.runNull();
        
        MPI.Finalize();
        System.out.println("The end!");
    }
}

