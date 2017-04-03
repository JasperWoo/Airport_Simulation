//Peijun Wu
import java.util.concurrent.ThreadLocalRandom;

public class AirportSim {
    public static Airport[] airportList = new Airport[5];
    public static final double[][] distAirports = {
        { 0, 1239.17, 1942.17, 954.95, 2170.93},  //LAX -> LAX, AUS, ATL, SEA, YYZ
        { 1239.17, 0, 811.47, 1769.19, 1358.23},  //AUS -> ...
        { 1942.17, 811.47, 0, 2177.79, 740.07},  //ATL -> ...
        { 954.95, 1769.19, 2177.79, 0, 2054.55},
        { 2170.93, 1358.23, 740.07, 2054.55, 0}
    };
    public static void main(String[] args) {
        //Implemented different airplanes air airports
        int numInitials = 30;
        airportList[0] = new Airport("LAX", 0.1, 0.1, 0.1); //Los Angelas
        airportList[1] = new Airport("AUS", 0.1, 0.1, 0.1); //Austin
        airportList[2] = new Airport("ATL", 0.1, 0.1, 0.1); // Atlanta
        airportList[3] = new Airport("SEA", 0.1, 0.1, 0.1); //Seattle
        airportList[4] = new Airport("YYZ", 0.1, 0.1, 0.1); //Toronto

        Simulator.stopAt(10);
        //In each loop, new planes will depart at every airport
        for (int i=0; i<numInitials; i++ ){
            Airplane boe747_1 = new Airplane("Boe747", 614,416);
            Airplane boe747_2 = new Airplane("Boe747", 614,416);
            Airplane boe747_3 = new Airplane("Boe747", 614,416);
            Airplane boe747_4 = new Airplane("Boe747", 614,416);
            Airplane a380 = new Airplane("A380", 634, 853);
            // The departure event, when handled, will automatically assign a new number of passengers randomly. Thus no need to assign randomly here.
            AirportEvent departureEvent_1 = new AirportEvent(0, airportList[0], AirportEvent.PLANE_DEPARTS, boe747_1, 0, 0);
            AirportEvent departureEvent_2 = new AirportEvent(0, airportList[1], AirportEvent.PLANE_DEPARTS, boe747_2, 0, 0);
            AirportEvent departureEvent_3 = new AirportEvent(0, airportList[2], AirportEvent.PLANE_DEPARTS, boe747_3, 0, 0);
            AirportEvent departureEvent_4 = new AirportEvent(0, airportList[3], AirportEvent.PLANE_DEPARTS, boe747_4, 0, 0);
            AirportEvent departureEvent_5 = new AirportEvent(0, airportList[4], AirportEvent.PLANE_DEPARTS, a380, 0, 0);
            Simulator.schedule(departureEvent_1);
            Simulator.schedule(departureEvent_2);
            Simulator.schedule(departureEvent_3);
            Simulator.schedule(departureEvent_4);
            Simulator.schedule(departureEvent_5);
        }
        Simulator.run();
    }
}
