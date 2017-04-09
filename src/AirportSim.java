//Peijun Wu
import java.util.concurrent.ThreadLocalRandom;

public class AirportSim {
    public static Airport[] airportList = new Airport[2];
    public static final double[][] distanceMatrix = new double[airportList.length][airportList.length];

    public static void main(String[] args) {
        //Implemented different airplanes air airports
        int numInitials = 100;
        airportList[0] = new Airport("LAX", 0.1, 0.1, 0.1, 20, 10, 5, true, -118.4, 33.9); //Los Angelas
        airportList[1] = new Airport("AUS", 0.1, 0.1, 0.1, 20, 10, 5, true, -97.6, 30.1); //Austin
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
        Simulator.stopAt(1000);
        //In each loop, new planes will depart at every airport
        for (int i = 0; i < numInitials; i++) {
            Airplane boe747_1 = new Airplane("Boe747", 614, 416);
            Airplane boe747_2 = new Airplane("Boe747", 614, 416);
            Airplane boe747_3 = new Airplane("Boe747", 614, 416);
            Airplane boe747_4 = new Airplane("Boe747", 614, 416);
            Airplane a380 = new Airplane("A380_1", 634, 853);
            // The departure event, when handled, will automatically assign a new number of passengers randomly. Thus no need to assign randomly here.
            AirportEvent departureEvent_1 = new AirportEvent(0, airportList[0], AirportEvent.PLANE_DEPARTS, boe747_1, 0, 0);
            AirportEvent departureEvent_2 = new AirportEvent(0, airportList[1], AirportEvent.PLANE_DEPARTS, boe747_2, 0, 0);

            Simulator.schedule(departureEvent_1);
            Simulator.schedule(departureEvent_2);
            
        }
        Simulator.run();
    }
}

