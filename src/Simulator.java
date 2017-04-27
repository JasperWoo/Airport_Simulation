//Peijun Wu

//singleton
public class Simulator {

    //singleton
    private static SimulatorEngine instance = null;
    // Make the construct unaccessible.
    private Simulator(){};
    public static SimulatorEngine getSim() {
        if(instance == null) {
            instance = new SimulatorEngine();
        }
        return instance;
    }

    public static void stopAt(double time) {
        Event stopEvent = new SimulatorEvent(time, getSim(), SimulatorEvent.STOP_EVENT);
        schedule(stopEvent);
    }

    public static void run() {
        getSim().run();
    }

    public static double getCurrentTime() {
        return getSim().getCurrentTime();
    }

    public static void schedule(Event event) {
        event.setTime(event.getTime() + getSim().getCurrentTime());
        getSim().schedule(event);
    }
    
	public static void setLookAhead(double time) {
		getSim().setLookAhead(time);
	}
	
}
