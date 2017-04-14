//Peijun Wu

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;

public class AirportSim {
    public static final int NUM_AIRPORTS = 50;
    public static Airport[] airportList = new Airport[NUM_AIRPORTS];
    public static final double[][] distanceMatrix = new double[airportList.length][airportList.length];

    public static void main(String[] args) {
        //Implemented different airplanes air airports
        int numInitials = 10;

        String csvAirports = "data_airports.csv";
        BufferedReader br = null;
        String line = "";
        String csvSeparator = ",";

        // Read airport information from .csv and initialize airports.
        try {
            br = new BufferedReader(new FileReader(csvAirports));
            line = br.readLine(); // Skip the first line
            for (int i=0;i<NUM_AIRPORTS;i++){
                line = br.readLine();
                // use comma as separator
                String [] values = line.split(csvSeparator)  ;
                airportList[i] = new Airport(values[0], Double.valueOf(values[1]), Double.valueOf(values[2]), Double.valueOf(values[3]), Integer.valueOf(values[4]), Integer.valueOf(values[5]), Integer.valueOf(values[6]), Boolean.valueOf(values[7]), Double.valueOf(values[8]), Double.valueOf(values[9])); //Los Angelas
            }
        }  catch (FileNotFoundException e){
            e.printStackTrace();
        }  catch (IOException e){
            e.printStackTrace();
        }  finally {
            if(br!=null){
                try{
                    System.out.println("The number of airports specified in the code may exceed the data available!");
                    br.close();
                } catch(IOException e){
                    e.printStackTrace();
                }
            }
        }

        // calculate the pairwise distances
        for (int i = 0; i < NUM_AIRPORTS; i++) {
            for (int j = i; j < NUM_AIRPORTS; j++) {
                if (i == j) {
                    distanceMatrix[i][j] = 0;
                }
                distanceMatrix[i][j] = distanceMatrix[j][i] = 3959.0 * Math.acos(Math.sin(Math.toRadians(airportList[i].getM_Lat())) *
                        Math.sin(Math.toRadians(airportList[j].getM_Lat())) +
                        Math.cos(Math.toRadians(airportList[i].getM_Lat())) * Math.cos(Math.toRadians(airportList[j].getM_Lat()))
                                * Math.cos(Math.toRadians(airportList[i].getM_Long() - airportList[j].getM_Long())));
            }
        }
        // Set up simulation stop time.
        Simulator.stopAt(1000);
        
        //In each loop, new planes will depart at every airport.
        for (int i = 0; i < numInitials; i++) {
            for(int j = 0; j<NUM_AIRPORTS; j++) {
                Airplane boe747 = new Airplane("Boe747", 614, 416);
                AirportEvent departureEvent_1 = new AirportEvent(0, airportList[0], AirportEvent.PLANE_DEPARTS, boe747, 0, 0);
                Simulator.schedule(departureEvent_1);

                if (airportList[j].isSupportA380()) {
                    Airplane a380 = new Airplane("A380", 634, 853);
                    AirportEvent departureEvent_2 = new AirportEvent(0, airportList[1], AirportEvent.PLANE_DEPARTS, a380, 0, 0);
                    Simulator.schedule(departureEvent_2);
                    // The departure event, when handled, will automatically assign a new number of passengers randomly. Thus no need to assign randomly here.
                }
            }
        }

        // Start the simulation
        Simulator.run();
    }
}

