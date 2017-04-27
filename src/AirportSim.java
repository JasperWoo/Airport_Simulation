//Peijun Wu
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

import com.sun.prism.Material;

import mpi.*;

public class AirportSim {
	public static int airportTotalNum = 50;
    public static Airport[] airportList = new Airport[airportTotalNum];
    public static final double[][] distanceMatrix = new double[airportList.length][airportList.length];
    //static Datatype typeAirEvent;
    public static void main(String[] args) {
        //Implemented different airplanes air airports
        int numInitials = 1;
        
        MPI.Init(args);
        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();
        
/*        //define new datatype for event send&recv
        int typeAirEventStructblocklengths[] = new int[6];
        int typeAirEventStructdisplacements[] = new int [6];
        Datatype typeAirEventStructOldType[] = new Datatype[6];
        typeAirEventStructOldType[0] = MPI.CHAR; //for airplane name
        typeAirEventStructblocklengths[0] = 1;
        typeAirEventStructdisplacements[0] = 0;
        typeAirEventStructOldType[1] = MPI.INT; //for airplane speed
        typeAirEventStructblocklengths[1] = 1;
        typeAirEventStructdisplacements[1] = 1;
        typeAirEventStructOldType[2] = MPI.INT; //for passenger number, not capacity
        typeAirEventStructblocklengths[2] = 1;
        typeAirEventStructdisplacements[2] = 2;
        typeAirEventStructOldType[3] = MPI.INT; //for des airport Index
        typeAirEventStructblocklengths[3] = 1;
        typeAirEventStructdisplacements[3] = 3;
        typeAirEventStructOldType[4] = MPI.DOUBLE; //for take off event current time
        typeAirEventStructblocklengths[4] = 1;
        typeAirEventStructdisplacements[4] = 4;
        typeAirEventStructOldType[5] = MPI.DOUBLE; //for delay time
        typeAirEventStructblocklengths[5] = 1;
        typeAirEventStructdisplacements[5] = 5;
        typeAirEvent =  Datatype.Struct(typeAirEventStructblocklengths, typeAirEventStructdisplacements, typeAirEventStructOldType);
        typeAirEvent.Commit();*/
        
        String csvAirports = "data_airports.csv";
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
        
        //airportList[0] = new Airport("LAX", 0.1, 0.1, 0.1, 20, 10, 5, true, 0, -118.4, 33.9); //Los Angelas
        //airportList[1] = new Airport("AUS", 0.1, 0.1, 0.1, 20, 10, 5, true, 1, -97.6, 30.1); //Austin
        
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
            for (int j = i; j < airportList.length; j++) {
                if (i == j) {
                    distanceMatrix[i][j] = 0;
                }
                distanceMatrix[i][j] = distanceMatrix[j][i] = 3959.0 * Math.acos(Math.sin(Math.toRadians(airportList[i].getM_Lat())) *
                        Math.sin(Math.toRadians(airportList[j].getM_Lat())) +
                        Math.cos(Math.toRadians(airportList[i].getM_Lat())) * Math.cos(Math.toRadians(airportList[j].getM_Lat()))
                                * Math.cos(Math.toRadians(airportList[i].getM_Long() - airportList[j].getM_Long())));
            }
        }
        Simulator.stopAt(50);
        //In each loop, new planes will depart at every airport
        for (int i = 0; i < numInitials; i++) {
            Airplane boe747_1 = new Airplane("Boe707", 614, 416);
            Airplane boe747_2 = new Airplane("Boe717", 614, 416);
            //Airplane boe747_3 = new Airplane("Boe747", 614, 416);
            //Airplane boe747_4 = new Airplane("Boe747", 614, 416);
            //Airplane a380 = new Airplane("A380_1", 634, 853);
            
            // The departure event, when handled, will automatically assign a new number of passengers randomly. Thus no need to assign randomly here.
            if (rank == 0) {
            		AirportEvent departureEvent_1 = new AirportEvent(0, airportList[0], AirportEvent.PLANE_DEPARTS, boe747_1, 0, 0);
                Simulator.schedule(departureEvent_1);
            }
            else if (rank == 1){
            		AirportEvent departureEvent_2 = new AirportEvent(0, airportList[1], AirportEvent.PLANE_DEPARTS, boe747_2, 0, 0);
            		Simulator.schedule(departureEvent_2);
            }
            
        }
        Simulator.setLookAhead(1.0);
        Simulator.runYAWNS();
        
        MPI.Finalize();
        System.out.println("The end!");
    }
}

