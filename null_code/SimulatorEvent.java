//Peijun Wu


public class SimulatorEvent extends Event {
    public static final int STOP_EVENT = 4;
    public static final int NULL_MESSAGE = 5;
    

    SimulatorEvent(double delay, EventHandler handler, int eventType) {
        super(delay, handler, eventType);
    }
}
