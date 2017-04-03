//Peijun Wu

//TODO add number of passengers, speed
//Added in other places
public class Airplane {
    //Added two properties
    private String m_name;
    private int m_speed;
    private int m_numberPassengers;
    public int runway_number;
    public boolean state;

    //Added parameters
    public Airplane(String name, int speed, int numberPassengers) {
        m_name = name;
        m_speed = speed;
        m_numberPassengers = numberPassengers;
    }
    //Implemented some getters.
    public String getName() {
        return m_name;
    }
    public int getSpeed(){ return m_speed;}
    public int getCapacity(){ return m_numberPassengers; }



}
