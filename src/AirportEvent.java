//Peijun Wu

public class AirportEvent extends Event {
    public static final int PLANE_ARRIVES = 0;
    public static final int PLANE_LANDED = 1;
    public static final int PLANE_DEPARTS = 2;
    public static final int PLANE_TAKEOFF = 3;

    private Airplane m_plane;
    private int m_numPassengers;
    private double m_lastEventTime;

    AirportEvent(double delay, EventHandler handler, int eventType, Airplane plane, int numPassengers, double lastEventTime) {
        super(delay, handler, eventType);
        m_plane = plane;
        m_numPassengers = numPassengers;
        m_lastEventTime = lastEventTime;
    }

    public Airplane getPlane(){ return m_plane;}
    public int getNumPassengers(){ return m_numPassengers;}
    public double getLastEventTime(){ return m_lastEventTime;}
}
