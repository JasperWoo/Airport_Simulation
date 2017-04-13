import java.awt.EventQueue;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JLabel;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.awt.event.ActionEvent;
import javax.swing.JCheckBox;
import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.Color;
import java.awt.Font;
import javax.swing.JComboBox;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
public class AirportSim extends JFrame{

    private JFrame frame;
    private JTextField AirportName;
    private JTextField RunTimetoLand;
    private JTextField Requiredtimeonground;
    private JTextField RunwayTimeToTakeoff;
    private JTextField groundCapacity;
    private JTextField airCapacity;
    private JTextField NumberOfRunway;
    private JTextField Longitude;
    private JTextField Latitude;
    private JTextField NumberOfAirportFromFile;
    private JTextField NumberOfAirplaneFromFile;
    private JLabel lblNumberOfAirportFromFile;
    private JLabel lblTimeonground;
    private JLabel lblTimetotakeoff;
    private JLabel lblGroundcapacity;
    private JLabel lblAircapacity;
    private JLabel lblNumberofrunway;
    private JLabel lblSupportAtf;
    private JLabel lblLongitude;
    private JLabel lblLattitude;
    private JLabel airplaneEvent;
    public static int NumberOfAirport = 1;
    static List<Airport> airportArrayList = new ArrayList<Airport>();
    private JLabel lblCurrentAirportNumber;
    private JLabel CurrentAirportNumber;
    private boolean IFsupportA380 = false;
    private final Action action = new SwingAction();
    private Airport selectedAirport;
    private String selectedEvent;
    private JTextField airplaneName;
    private JTextField airplaneSpeed;
    private JTextField airplaneCapacity;
    private JTextField airplaneEventText;
    private JTextField airplaneDelaytime;
    private String newAirplaneName;
    private int CurrentAirplaneNumber = 0;
    public static Airport[] airportList;
    public static double[][] distanceMatrix;

    Vector AirportNameList = new Vector();
    Vector EventList = new Vector();
    final DefaultComboBoxModel SelectAirportModel = new DefaultComboBoxModel(AirportNameList);
    final DefaultComboBoxModel EventListModel = new DefaultComboBoxModel(EventList);

    private final Action action_1 = new SwingAction_1();
    private JButton btnRun;
    private JTextField textSetRuntime;
    private JLabel lblNewLabel_3;
    private double totalRunTime;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    AirportSim window = new AirportSim();
                    window.frame.setVisible(true);
                    PrintStream out = new PrintStream(new FileOutputStream("output.txt"));
                    System.setOut(out);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        });

    }

    /**
     * Create the application.
     */
    public AirportSim() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initialize();

    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {

        EventListModel.addElement(new String("PLANE_ARRIVES"));//, new String("PLANE_LANDED"),
        EventListModel.addElement(new String("PLANE_DEPARTS"));
        EventListModel.addElement(new String("PLANE_TAKEOFF"));
        EventListModel.addElement(new String("PLANE_LANDED"));

        frame = new JFrame();
        frame.setBounds(100, 100, 1000, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(null);

        AirportName = new JTextField();
        AirportName.setBounds(20, 50, 60, 26);
        frame.getContentPane().add(AirportName);
        AirportName.setColumns(10);

        RunTimetoLand = new JTextField();
        RunTimetoLand.setBounds(104, 50, 40, 26);
        frame.getContentPane().add(RunTimetoLand);
        RunTimetoLand.setColumns(10);

        Requiredtimeonground = new JTextField();
        Requiredtimeonground.setBounds(201, 50, 40, 26);
        frame.getContentPane().add(Requiredtimeonground);
        Requiredtimeonground.setColumns(10);

        RunwayTimeToTakeoff = new JTextField();
        RunwayTimeToTakeoff.setBounds(322, 50, 40, 26);
        frame.getContentPane().add(RunwayTimeToTakeoff);
        RunwayTimeToTakeoff.setColumns(10);

        groundCapacity = new JTextField();
        groundCapacity.setBounds(448, 50, 40, 26);
        frame.getContentPane().add(groundCapacity);
        groundCapacity.setColumns(10);

        airCapacity = new JTextField();
        airCapacity.setBounds(20, 115, 40, 26);
        frame.getContentPane().add(airCapacity);
        airCapacity.setColumns(10);

        NumberOfRunway = new JTextField();
        NumberOfRunway.setBounds(104, 115, 40, 26);
        frame.getContentPane().add(NumberOfRunway);
        NumberOfRunway.setColumns(10);

        Longitude = new JTextField();
        Longitude.setBounds(322, 115, 80, 26);
        frame.getContentPane().add(Longitude);
        Longitude.setColumns(10);

        Latitude = new JTextField();
        Latitude.setBounds(438, 115, 80, 26);
        frame.getContentPane().add(Latitude);
        Latitude.setColumns(10);

        JLabel lblName = new JLabel("Name");
        lblName.setBounds(20, 20, 40, 30);
        frame.getContentPane().add(lblName);

        JLabel lblTimetoland = new JLabel("TimeToLand");
        lblTimetoland.setBounds(84, 27, 83, 16);
        frame.getContentPane().add(lblTimetoland);

        lblTimeonground = new JLabel("TimeOnGround");
        lblTimeonground.setBounds(191, 27, 97, 16);
        frame.getContentPane().add(lblTimeonground);

        lblTimetotakeoff = new JLabel("TimeToTakeoff");
        lblTimetotakeoff.setBounds(312, 27, 102, 16);
        frame.getContentPane().add(lblTimetotakeoff);

        lblGroundcapacity = new JLabel("GroundCapacity");
        lblGroundcapacity.setBounds(438, 27, 110, 16);
        frame.getContentPane().add(lblGroundcapacity);

        lblAircapacity = new JLabel("AirCapacity");
        lblAircapacity.setBounds(20, 87, 97, 16);
        frame.getContentPane().add(lblAircapacity);

        lblNumberofrunway = new JLabel("NumberOfRunway");
        lblNumberofrunway.setBounds(104, 87, 123, 16);
        frame.getContentPane().add(lblNumberofrunway);

        lblSupportAtf = new JLabel("Support A380");
        lblSupportAtf.setBounds(230, 87, 132, 16);
        frame.getContentPane().add(lblSupportAtf);

        lblLongitude = new JLabel("Longitude");
        lblLongitude.setBounds(332, 87, 80, 16);
        frame.getContentPane().add(lblLongitude);

        lblLattitude = new JLabel("Lattitude");
        lblLattitude.setBounds(438, 87, 70, 16);
        frame.getContentPane().add(lblLattitude);

        NumberOfAirportFromFile = new JTextField();
        NumberOfAirportFromFile.setBounds(690, 50, 80, 26);
        frame.getContentPane().add(NumberOfAirportFromFile);
        NumberOfAirportFromFile .setColumns(10);

        JLabel lblCurrentAirplaneNumber = new JLabel("Current Airplane number is ");
        lblCurrentAirplaneNumber.setBounds(20, 282, 189, 16);
        frame.getContentPane().add(lblCurrentAirplaneNumber);

        JLabel lblAirplaneNumber = new JLabel("0");
        lblAirplaneNumber.setBounds(201, 282, 61, 16);
        frame.getContentPane().add(lblAirplaneNumber);

        JLabel lblNumberOfAirportFromFile = new JLabel("Airport number to read from file (<100)");
        lblNumberOfAirportFromFile.setBounds(690, 20, 270, 30);
        frame.getContentPane().add(lblNumberOfAirportFromFile);

        NumberOfAirplaneFromFile = new JTextField();
        NumberOfAirplaneFromFile.setBounds(690, 210, 80, 26);
        frame.getContentPane().add(NumberOfAirplaneFromFile);
        NumberOfAirplaneFromFile .setColumns(10);

        JLabel lblNumberOfAirplaneFromFile = new JLabel("Airplane number per airport read from file (<10)");
        lblNumberOfAirplaneFromFile.setBounds(690, 180, 330, 30);
        frame.getContentPane().add(lblNumberOfAirplaneFromFile);

        JButton btnAdd50Airport = new JButton("Add Airports From File");
        btnAdd50Airport.setFont(new Font("Lucida Grande", Font.BOLD, 14));
        btnAdd50Airport.setForeground(Color.GREEN);
        btnAdd50Airport.setBackground(Color.GREEN);
        btnAdd50Airport.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                //Airport newAirport = new Airport(newAirport_name, newAirport_RuntimeToLand,
                //        newAirport_Requiredtimeonground, newAirport_RunwayTimeToTakeoff, newAirport_groundCapacity,
                //        newAirport_airCapacity, newAirport_NumberOfRunway, IFsupportA380, newAirport_Longitude,
                //        newAirport_Latitude);
                int currentAirportNumber = airportArrayList.size();

                int NUM_addAIRPORTS = Integer.parseInt(NumberOfAirportFromFile.getText());;
                String csvAirports = "data_airports.csv";
                BufferedReader br = null;
                String line = "";
                String csvSeparator = ",";

                try {
                    br = new BufferedReader(new FileReader(csvAirports));
                    line = br.readLine(); // Skip the first line
                    for (int i=0;i<NUM_addAIRPORTS;i++){
                        line = br.readLine();
                        // use comma as separator
                        String [] values = line.split(csvSeparator)  ;
                        airportArrayList.add(new Airport(values[0], Double.valueOf(values[1]), Double.valueOf(values[2]), Double.valueOf(values[3]), Integer.valueOf(values[4]), Integer.valueOf(values[5]), Integer.valueOf(values[6]), Boolean.valueOf(values[7]), Double.valueOf(values[8]), Double.valueOf(values[9]))); //Los Angelas
                    }
                }  catch (FileNotFoundException eread){
                    eread.printStackTrace();
                }  catch (IOException eread){
                    eread.printStackTrace();
                }  finally {
                    if(br!=null){
                        try{
                            System.out.println("The number of airports specified in the code may exceed the data available!");
                            br.close();
                        } catch(IOException eread){
                            eread.printStackTrace();
                        }
                    }
                }


                CurrentAirportNumber.setText(Integer.toString(airportArrayList.size()));
                // @Override
                // public void actionPerformed(ActionEvent ae) {
                for (int i=currentAirportNumber; i < currentAirportNumber+NUM_addAIRPORTS;i++) {
                    SelectAirportModel.addElement(airportArrayList.get(i).getName());
                }
                // }


            }
        });
        btnAdd50Airport.setBounds(690, 80, 200, 44);
        frame.getContentPane().add(btnAdd50Airport);
        //add airplane from file
        //this is for option reading airplanes from file
        //then this function will add certain number of airplanes on airports read from file
        //set the number of airplane n per airport
        //if the airport support A380, add n general planes and n A380 planes
        JButton btnAdd50Airplane = new JButton("Add Airplanes From File");
        btnAdd50Airplane.setFont(new Font("Lucida Grande", Font.BOLD, 14));
        btnAdd50Airplane.setForeground(Color.ORANGE);
        btnAdd50Airplane.setBackground(Color.ORANGE);
        btnAdd50Airplane.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int numInitials = Integer.parseInt(NumberOfAirplaneFromFile.getText());
                for (int i = 0; i < numInitials; i++) {
                    for(int j = 0; j<Integer.parseInt(NumberOfAirportFromFile.getText()); j++) {
                        Airplane boe747 = new Airplane("Boe747", 614, 416);
                        AirportEvent departureEvent_1 = new AirportEvent(0, airportArrayList.get(j), AirportEvent.PLANE_DEPARTS, boe747, 0, 0);
                        Simulator.schedule(departureEvent_1);
                        CurrentAirplaneNumber = CurrentAirplaneNumber + 1;
                        lblAirplaneNumber.setText(Integer.toString(CurrentAirplaneNumber));

                        if (airportArrayList.get(j).isSupportA380()) {
                            Airplane a380 = new Airplane("A380", 634, 853);
                            AirportEvent departureEvent_2 = new AirportEvent(0, airportArrayList.get(j), AirportEvent.PLANE_DEPARTS, a380, 0, 0);
                            Simulator.schedule(departureEvent_2);
                            CurrentAirplaneNumber = CurrentAirplaneNumber + 1;
                            lblAirplaneNumber.setText(Integer.toString(CurrentAirplaneNumber));
                            // The departure event, when handled, will automatically assign a new number of passengers randomly. Thus no need to assign randomly here.
                        }
                    }
                }
            }
        });
        btnAdd50Airplane.setBounds(690, 240, 200, 44);
        frame.getContentPane().add(btnAdd50Airplane);

        JButton btnAddAirport = new JButton("Add Airport");
        btnAddAirport.setFont(new Font("Lucida Grande", Font.BOLD, 14));
        btnAddAirport.setForeground(Color.GREEN);
        btnAddAirport.setBackground(Color.GREEN);
        btnAddAirport.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String newAirport_name = AirportName.getText();
                double newAirport_RuntimeToLand = Double.parseDouble(RunTimetoLand.getText());
                double newAirport_Requiredtimeonground = Double.parseDouble(Requiredtimeonground.getText());
                double newAirport_RunwayTimeToTakeoff = Double.parseDouble(RunwayTimeToTakeoff.getText());
                int newAirport_groundCapacity = Integer.parseInt(groundCapacity.getText());
                int newAirport_airCapacity = Integer.parseInt(airCapacity.getText());
                int newAirport_NumberOfRunway = Integer.parseInt(NumberOfRunway.getText());
                // IfsupportA380
                double newAirport_Longitude = Double.parseDouble(Longitude.getText());
                double newAirport_Latitude = Double.parseDouble(Latitude.getText());

                Airport newAirport = new Airport(newAirport_name, newAirport_RuntimeToLand,
                        newAirport_Requiredtimeonground, newAirport_RunwayTimeToTakeoff, newAirport_groundCapacity,
                        newAirport_airCapacity, newAirport_NumberOfRunway, IFsupportA380, newAirport_Longitude,
                        newAirport_Latitude);
                airportArrayList.add(newAirport);
                CurrentAirportNumber.setText(Integer.toString(airportArrayList.size()));
                // @Override
                // public void actionPerformed(ActionEvent ae) {
                SelectAirportModel.addElement(newAirport_name);
                // }
                AirportName.setText("");
                RunTimetoLand.setText("");
                Requiredtimeonground.setText("");
                RunwayTimeToTakeoff.setText("");
                groundCapacity.setText("");
                airCapacity.setText("");
                NumberOfRunway.setText("");
                Longitude.setText("");
                Latitude.setText("");




            }
        });
        btnAddAirport.setBounds(530, 62, 97, 44);
        frame.getContentPane().add(btnAddAirport);

        lblCurrentAirportNumber = new JLabel("Current Airport Number is");
        lblCurrentAirportNumber.setBounds(20, 157, 169, 16);
        frame.getContentPane().add(lblCurrentAirportNumber);

        CurrentAirportNumber = new JLabel("0");
        CurrentAirportNumber.setBounds(201, 157, 61, 16);
        frame.getContentPane().add(CurrentAirportNumber);

        JCheckBox SupportA380 = new JCheckBox("");
        SupportA380.setAction(action);
        SupportA380.setSelected(true);
        SupportA380.setBounds(240, 115, 28, 23);
        frame.getContentPane().add(SupportA380);

        airplaneName = new JTextField();
        airplaneName.setBounds(129, 209, 80, 26);
        frame.getContentPane().add(airplaneName);
        airplaneName.setColumns(10);

        airplaneSpeed = new JTextField();
        airplaneSpeed.setBounds(269, 218, 40, 26);
        frame.getContentPane().add(airplaneSpeed);
        airplaneSpeed.setColumns(10);

        airplaneCapacity = new JTextField();
        airplaneCapacity.setBounds(383, 218, 40, 26);
        frame.getContentPane().add(airplaneCapacity);
        airplaneCapacity.setColumns(10);

        airplaneEvent = new JLabel("AirplaneEvent");
        airplaneEvent.setBounds(440, 200, 169, 16);
        frame.getContentPane().add(airplaneEvent);




        JButton btnNewButton = new JButton("Add Airplane");
        btnNewButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // add new airplane here -------------
                int eventIndex = 2;
                if (airplaneName.isEnabled()) {
                    newAirplaneName = airplaneName.getText();
                } else {
                    newAirplaneName = "A380";
                }
                int newairplaneSpeed = Integer.parseInt(airplaneSpeed.getText());
                int newairplaneCapacity = Integer.parseInt(airplaneCapacity.getText());
                double newairplaneDelayTime = Double.parseDouble(airplaneDelaytime.getText());
                // selectedAirport
                switch (selectedEvent) {
                    case "PLANE_ARRIVES":
                        eventIndex = 0;
                    case "PLANE_LANDED":
                        eventIndex = 1;
                    case "PLANE_DEPARTS":
                        eventIndex = 2;
                    case "PLANE_TAKEOFF":
                        eventIndex = 3;
                }

                Airplane newAirplane = new Airplane(newAirplaneName, newairplaneSpeed, newairplaneCapacity);
                AirportEvent newDepartureEvent = new AirportEvent(newairplaneDelayTime, selectedAirport,
                        eventIndex, newAirplane, 0, 0);
                Simulator.schedule(newDepartureEvent);
                CurrentAirplaneNumber = CurrentAirplaneNumber + 1;
                lblAirplaneNumber.setText(Integer.toString(CurrentAirplaneNumber));

                airplaneName.setText("");
                airplaneSpeed.setText("");
                airplaneCapacity.setText("");
                airplaneDelaytime.setText("");


            }
        });
        btnNewButton.setForeground(Color.ORANGE);
        btnNewButton.setBounds(510, 260, 117, 44);
        frame.getContentPane().add(btnNewButton);

        JLabel lblNewLabel = new JLabel("New Airport Information");
        lblNewLabel.setFont(new Font("Lucida Grande", Font.ITALIC, 13));
        lblNewLabel.setBounds(20, 6, 208, 16);
        frame.getContentPane().add(lblNewLabel);

        JLabel lblNewLabel_1 = new JLabel("New Airplane Information");
        lblNewLabel_1.setFont(new Font("Lucida Grande", Font.ITALIC, 13));
        lblNewLabel_1.setBounds(20, 186, 170, 16);
        frame.getContentPane().add(lblNewLabel_1);

        JLabel lblAirplaneName = new JLabel("Type the name ");
        lblAirplaneName.setBounds(20, 214, 124, 16);
        frame.getContentPane().add(lblAirplaneName);

        JCheckBox airplaneIf380 = new JCheckBox("A380");
        airplaneIf380.setAction(action_1);
        airplaneIf380.setBounds(129, 229, 128, 23);
        frame.getContentPane().add(airplaneIf380);

        JLabel lblSpeed = new JLabel("Speed");
        lblSpeed.setBounds(221, 223, 46, 16);
        frame.getContentPane().add(lblSpeed);

        JLabel lblCapacity = new JLabel("Capacity");
        lblCapacity.setBounds(322, 219, 61, 16);
        frame.getContentPane().add(lblCapacity);

        JLabel lblTypeTheName = new JLabel("or choose A380");
        lblTypeTheName.setBounds(19, 233, 222, 16);
        frame.getContentPane().add(lblTypeTheName);

        JLabel lblNewLabel_2 = new JLabel("Delay time");
        lblNewLabel_2.setBounds(19, 261, 98, 16);
        frame.getContentPane().add(lblNewLabel_2);

        airplaneDelaytime = new JTextField();
        airplaneDelaytime.setBounds(97, 256, 46, 26);
        frame.getContentPane().add(airplaneDelaytime);
        airplaneDelaytime.setColumns(10);

        JLabel lblInitialDepartingAirport = new JLabel("Initial departing Airport");
        lblInitialDepartingAirport.setBounds(155, 261, 154, 16);
        frame.getContentPane().add(lblInitialDepartingAirport);

        JComboBox SelectAirport = new JComboBox(SelectAirportModel);
        SelectAirport.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectedAirport = airportArrayList.get(SelectAirport.getSelectedIndex());
            }
        });

        JComboBox SelectEvent = new JComboBox(EventListModel);
        SelectAirport.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectedEvent = (String)SelectEvent.getSelectedItem();
            }
        });

        SelectAirport.setBounds(322, 257, 92, 27);
        frame.getContentPane().add(SelectAirport);

        SelectEvent.setBounds(430, 218, 160, 26);
        frame.getContentPane().add(SelectEvent);

        textSetRuntime = new JTextField();
        textSetRuntime.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                totalRunTime = Double.parseDouble(textSetRuntime.getText());
            }
        });
        textSetRuntime.setBounds(201, 380, 130, 26);
        frame.getContentPane().add(textSetRuntime);
        textSetRuntime.setColumns(10);

        lblNewLabel_3 = new JLabel("Set total runtime");
        lblNewLabel_3.setBounds(84, 380, 126, 16);
        frame.getContentPane().add(lblNewLabel_3);

        btnRun = new JButton("RUN");
        btnRun.setFont(new Font("Lucida Grande", Font.BOLD, 24));
        btnRun.setForeground(Color.RED);
        btnRun.setBackground(Color.RED);
        btnRun.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // run the simulation here!!!
                distanceMatrix = new double[airportArrayList.size()][airportArrayList.size()];
                for (int i = 0; i < airportArrayList.size(); i++) {
                    for (int j = i; j < airportArrayList.size(); j++) {
                        if (i == j) {
                            distanceMatrix[i][j] = 0;
                        }
                        distanceMatrix[i][j] = distanceMatrix[j][i] = 3959.0
                                * Math.acos(Math.sin(Math.toRadians(airportArrayList.get(i).getM_Lat()))
                                * Math.sin(Math.toRadians(airportArrayList.get(j).getM_Lat()))
                                + Math.cos(Math.toRadians(airportArrayList.get(i).getM_Lat()))
                                * Math.cos(Math.toRadians(airportArrayList.get(j).getM_Lat())) * Math
                                .cos(Math.toRadians(airportArrayList.get(i).getM_Long()
                                        - airportArrayList.get(j).getM_Long())));
                    }
                }
                airportList = airportArrayList.toArray(new Airport[airportArrayList.size()]);
                Simulator.stopAt(Double.parseDouble(textSetRuntime.getText()));
                Simulator.run();
            }
        });
        btnRun.setBounds(371, 380, 117, 40);
        frame.getContentPane().add(btnRun);
    }

    private class SwingAction extends AbstractAction {
        public SwingAction() {
            putValue(NAME, "SwingAction");
            putValue(SHORT_DESCRIPTION, "Some short description");
        }

        public void actionPerformed(ActionEvent e) {
            JCheckBox cbA380 = (JCheckBox) e.getSource();
            if (cbA380.isSelected()) {
                IFsupportA380 = true;
            } else {
                IFsupportA380 = false;
            }

        }
    }

    private class SwingAction_1 extends AbstractAction {
        public SwingAction_1() {
            putValue(NAME, "A380");
            putValue(SHORT_DESCRIPTION, "Some short description");
        }

        public void actionPerformed(ActionEvent e) {
            JCheckBox cb_airplaneIf380 = (JCheckBox) e.getSource();
            if (!cb_airplaneIf380.isSelected()) {
                airplaneName.setEnabled(true);
            }

        }
    }
}
