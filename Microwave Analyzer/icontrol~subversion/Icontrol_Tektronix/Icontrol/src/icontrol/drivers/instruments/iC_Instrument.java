// TODO 5* manually sending WaitForUser afer WaitForStop deadlocks the program

/*
 * This software was developed at the National Institute of Standards and
 * Technology by a guest researcher in the course of his official duties and
 * with the partial support of the Swiss National Science Foundation. Pursuant
 * to title 17 Section 105 of the United States Code this software is not
 * subject to copyright protection and is in the public domain. The
 * Instrument-Control (iC) software is an experimental system. Neither NIST, nor
 * the Swiss National Science Foundation nor any of the authors assumes any
 * responsibility whatsoever for its use by other parties, and makes no
 * guarantees, expressed or implied, about its quality, reliability, or any
 * other characteristic. We would appreciate your citation if the software
 * is used: http://dx.doi.org/10.6028/jres.117.010 .
 *
 * This software can be redistributed and/or modified freely under the terms of
 * the GNU Public Licence and provided that any derivative works bear some
 * notice that they are derived from it, and any modified versions bear some
 * notice that they have been modified.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Public License for more details. http://www.fsf.org
 *
 * This software relies on other open source projects; please see the accompanying
 * _ReadMe_iC.txt for a list of included packages. Thank's very much to those
 * developers !! Without your effort, iC would not have been possible!
 *
 */

package icontrol.drivers.instruments;


import icontrol.AutoGUIAnnotation;
import icontrol.IcontrolView;
import static icontrol.Utilities.getDouble;
import icontrol.drivers.Device;
import icontrol.iC_Annotation;
import icontrol.iC_ChartXY;
import icontrol.iC_ChartXY.SeriesIdentification;
import icontrol.drivers.Device.CommPorts;
import java.awt.Toolkit;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;
import javax.script.ScriptException;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import org.jfree.chart.axis.NumberAxis;

/**
 * A virtual Instrument without Communication that provides mostly control functions
 * for processing the script and the program itself.
 *
 * <h3>Scriptable Instrument-Commands:</h3>
 * <ul>
 *  <li>{@link #DisplayStatusMessage(String) }
 *  <li>{@link #MonitorChart(float, String, String, String, String, String, String, String) }
 *  <li>{@link #Pause() }
 *  <li>{@link #Wait(float) }
 *  <li>{@link #WaitDate(String) }
 *  <li>{@link #WaitForStop() }
 *  <li>{@link #WaitForUser(String) }
 *  <li>{@link #getFreeMemory() }
 * </ul>
 *
 */
// declare that this class does not need a communication port
@iC_Annotation( CommPorts=CommPorts.none,
                InstrumentClassName="iC-control")
public class iC_Instrument extends Device {


    /**
     * The Logger for this class. Note that the Logger Level is set in the
     * constructor of the <code>Device</code> class.
     */
    private static final Logger m_Logger = Logger.getLogger("iC.Instruments.iC_Instrument");


    /**
     * Puts the processing of the script into pause mode.
     * To un-pause, the user must press the "Continue" button in the GUI
     */
    // <editor-fold defaultstate="collapsed" desc="Pause">
    @AutoGUIAnnotation(
        DescriptionForUser = "Pause Scripting until button 'Continue' is pressed.",
        ParameterNames = {})
    public void Pause() {
        
        // simulate pressing the "Pause" button in the GUI
        m_GUI.Pause();

        // beep
        Toolkit.getDefaultToolkit().beep();
        
    }//</editor-fold>


    /**
     * Interrupts processing of the script by a certain amount of time. Because it
     * uses <code>Thread.sleep</code> the sleep method might get interrupted by
     * some other thread (which I don't think should occur) in which case the
     * actual time spend waiting might be shorter.
     */
    // <editor-fold defaultstate="collapsed" desc="Wait xx-seconds">
    @AutoGUIAnnotation(
        DescriptionForUser = "Pause Scripting for specified amount of time.",
        ParameterNames = "Wait time [sec]",
        DefaultValues="5")
    public void Wait(float WaitTimeInSeconds) {
        try {
            Thread.sleep( Math.round(WaitTimeInSeconds*1000) );
        } catch (InterruptedException ex) { }
    }//</editor-fold>


    /**
     * Interrupts processing of the script in a way that allows background processes
     * (for instance MonitorChart) to continue. This method displays a modeless 
     * dialog to the user which includes the additional message.
     * @param Message The additional message displayed to the user
     */
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Wait until the User presses the OK button in a popup window.\nBackground processes will continue meanwhile.</html>",
        ParameterNames = "Additional Message",
        DefaultValues="Waiting ...")
    // <editor-fold defaultstate="collapsed" desc="Wait For User">
    public void WaitForUser(String Message) {
        
        String str = "Processing of the script has been interrupted.\n"
            + "Background process will continue to work.\n";
        if (!Message.isEmpty()) {
            str += "\n" + Message + "\n\n";
        }
        str += "Press 'Okay' to continue.";
        
        // log event
        m_Logger.finer("in WaitForUser\n");
        
        // beep
        Toolkit.getDefaultToolkit().beep();
        
        // http://java.about.com/od/UsingDialogBoxes/a/How-To-Make-A-Modeless-Dialog-Box.htm
        // http://java.sun.com/developer/technicalArticles/J2SE/Desktop/javase6/modality/
        
        // make a new JDialog to display a non-modal (modeless) JOptionPane
        final JDialog MessageDialog = new JDialog(m_GUI.getTheFrame(),"Wait For User", false);
        
        // set action on closing the window
        MessageDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        
        // Make a new instance of a JOptionPane 
        JOptionPane optPane = new JOptionPane(str, JOptionPane.INFORMATION_MESSAGE, 
                JOptionPane.OK_OPTION, m_iC_Properties.getLogoSmall(), new Object[] {"Okay"});

        //Listen for the JOptionPane button click. It comes through as property change 
        //event with the property called "value". 
        optPane.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                if (e.getPropertyName().equals("value")) {
                    // any click releases screen-resources of the MessageDialog,
                    // hence, it becomes not displayable any more
                    MessageDialog.dispose();   
                }    
            }    
        });
        

        // Set size and location of the MessageDialog, and make it visible
        MessageDialog.setContentPane(optPane);
        MessageDialog.pack();
        MessageDialog.setLocationRelativeTo(m_GUI.getTheComponent());
        MessageDialog.setVisible(true);
            // using invoke and invokeAndWait did not help
        
        
        // wait until the user pressed okay (screen resources are released)
        while (MessageDialog.isDisplayable()) {
            try {Thread.sleep(100);} catch (InterruptedException ignore) {}
        }
        
        // log
        m_Logger.finer("exiting WaitForUser\n");
    }//</editor-fold>
    
    
    
    /**
     * Interrupts processing of the script in a way that allows background processes
     * (for instance MonitorChart) to continue. This method displays a modeless 
     * dialog to the user which includes the additional message. When the user
     * selects 'Stop' then processing of the script is stopped, and when the user
     * selects 'Continue' processing of the script is continued.
     * 
     * @param Message The additional message displayed to the user
     */
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Ask user to Stop or Continue processing the script\nBackground processes will continue meanwhile.</html>",
        ParameterNames = "Additional Message",
        DefaultValues="Waiting ...")
    // <editor-fold defaultstate="collapsed" desc="StopOrGo">
    public void StopOrGo(String Message) {
        
        // build displayed string   
        String str = Message + "\n\n"
                + "Please select if you want to Stop processing of the script\n"
                + "or you want to continue with the script.\n\n"
                + "Processing of the script has been interrupted.\n"
                + "Background process will continue to work.\n";
        
        // log event
        m_Logger.finer("in StopOrGo\n");
        
        // beep
        Toolkit.getDefaultToolkit().beep();
        
        // http://java.about.com/od/UsingDialogBoxes/a/How-To-Make-A-Modeless-Dialog-Box.htm
        // http://java.sun.com/developer/technicalArticles/J2SE/Desktop/javase6/modality/
        
        // make a new JDialog to display a non-modal (modeless) JOptionPane
        final JDialog MessageDialog = new JDialog(m_GUI.getTheFrame(),"Stop Or Go", false);
        
        // set action on closing the window
        MessageDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        
        // Make a new instance of a JOptionPane 
        JOptionPane optPane = new JOptionPane(str, JOptionPane.INFORMATION_MESSAGE, 
                JOptionPane.OK_OPTION, m_iC_Properties.getLogoSmall(), new Object[] {"Stop", "Continue"});

        //Listen for the JOptionPane button click. It comes through as property change 
        //event with the property called "value". 
        optPane.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                if (e.getPropertyName().equals("value")) {
                    // any click releases screen-resources of the MessageDialog,
                    // hence, it becomes not displayable any more
                    MessageDialog.dispose();   
                }    
            }    
        });
        

        // Set size and location of the MessageDialog, and make it visible
        MessageDialog.setContentPane(optPane);
        MessageDialog.pack();
        MessageDialog.setLocationRelativeTo(m_GUI.getTheComponent());
        MessageDialog.setVisible(true);
            // using invoke and invokeAndWait did not help
        
        
        // wait until the user pressed okay (screen resources are released)
        while (MessageDialog.isDisplayable()) {
            try {Thread.sleep(100);} catch (InterruptedException ignore) {}
        }
        
        // get the user's selection
        String Selection = (String)optPane.getValue();
        
        // react on stop
        if (Selection.equalsIgnoreCase("Stop")) {
            // stop scripting
            m_GUI.Stop();
        }
        
        // continue with scripting by exiting this method
        
        // log
        m_Logger.finer("exiting Stop Or Go\n");
    }//</editor-fold>

    
    /**
     * Interrupts processing of the script until the specified date and time in 
     * a way that allows background processes (for instance MonitorChart) to continue.
     * During Syntax-Check mode, the date and time is echoed on the Status
     * Area of the GUI.
     * 
     * @param DateAndTime The date and time after which processing of the 
     * script will be continued.
     * 
     * @throws DataFormatException when the Syntax check fails
     */
    // <editor-fold defaultstate="collapsed" desc="Wait Date">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Wait until the specified date and time before continuing. \nBackground processes will continue meanwhile.</html>",
        ParameterNames = "Date and Time",
        DefaultValues="23.11 17:35",
        ToolTips="Format: MM.dd HH:mm (24-h notation)")
    @iC_Annotation(MethodChecksSyntax=true)
    public void WaitDate(String DateAndTime) 
           throws DataFormatException {
        
        // define the format of the Date and Time
        DateFormat myDateFormat = new SimpleDateFormat("MM.dd HH:mm", Locale.US);
        
        // disable lenient parsing to parse the date more strict and catch more errors
        myDateFormat.setLenient(false);
        
        // convert the specified String into a date and time
        GregorianCalendar Time = (GregorianCalendar)Calendar.getInstance();
        try {
            Time.setTime(myDateFormat.parse(DateAndTime));
            
            // assign current year
            Time.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR));
        } catch (ParseException ex) {
            String str = "Could not interpret the date and time string\n"
                    + DateAndTime + "\n"
                    + "Detailed error message:\n" + ex.getMessage() + "\n\n"
                    + "Please check the spelling and try again.\n"
                    + "See the ToolTips for an example of a date and time.\n";
                    
            throw new DataFormatException(str);
        }
        
        // make a nice String representation of the converted date and time
        String[] Months = new DateFormatSymbols().getShortMonths();
        String TimeString = Time.get(Calendar.DAY_OF_MONTH) + " "
                + Months[Time.get(Calendar.MONTH)] + " "
                + Time.get(Calendar.YEAR) + " at "
                + Time.get(Calendar.HOUR_OF_DAY) + ":"
                + Time.get(Calendar.MINUTE);
        
        // return if in Syntax-check mode
        if (inSyntaxCheckMode()) {
            // display the (converted) date and time in the status area
            m_GUI.DisplayStatusMessage("Will be waiting until " + TimeString + "(still in Syntax-Check mode)\n", false);
            
            return;
        }        

        // show Status Message
        m_GUI.DisplayStatusMessage("Waiting until " + TimeString +"\n");
        
        // log event
        m_Logger.log(Level.FINEST, "Starting to wait for {0}\n", TimeString);
        
        // check if time has already passed
        while (Calendar.getInstance().compareTo(Time) <= 0 &&
               m_StopScripting == false) {
            
            // time not reached yet, so wait a bit
            try {Thread.sleep(1000);} catch (InterruptedException ex) {/*ignore*/}
            
            // is scripting paused?
            isPaused(true);
        }
        
        // log
        m_Logger.log(Level.FINEST, "Done waiting for {0}\n", TimeString);
    }//</editor-fold>
    
    /**
     * Waits until the user presses Stop in the GUI. Bckground processes continue
     * to work.
     */
    // <editor-fold defaultstate="collapsed" desc="Wait For Stop">
    @AutoGUIAnnotation(
        DescriptionForUser = "Wait until the User presses Stop.",
        ParameterNames = {})
    public void WaitForStop() {
        
        // do until the user presses stop
        while ( m_StopScripting == false) {

            // wait a bit and check the stop button again
            try { Thread.sleep(500); } catch (InterruptedException ex) { }
        }
    }//</editor-fold>
    
    
    
    /**
     * Starts a new thread that executes the passed Command Lines at the given
     * time interval, and displays the returned value in an XY-chart. A separate
     * XY Series is used for the different Command Lines.  The values are stored
     * in a file together with the number of seconds elapsed since processing
     * of the script started. The chart is saved as a .png file at the end.<p>
     * 
     * Note: JFreeChart was not designed for speed, and it occurs frequently, that
     * events to refresh the chart get queued, which slows down the response. There
     * is no simple way to prevent queuing, and to find a work around is on the 
     * to do list.<p>
     *
     * This methods performs a syntax check.
     *
     * @param TimeIntervall The temperature is monitored every TimeInterval-seconds.
     * @param YLabel The label for the Y-axis
     * @param FileExtension is appended to the FileName used to store the monitored
     * temperatures. If it contains no '.' a '.' is automatically inserted. If the
     * filename already exists, a new file with a counter is inserted between the 
     * FileName and the FileExtension.
     * @param CommandLine1 A String containing the command line that is passed
     * to <code>Device.DispatchCommand</code> to obtain the desired value to be
     * displayed in the chart.
     * @param CommandLine2 same as CommandLine1
     * @param CommandLine3 same as CommandLine1
     * @param CommandLine4 same as CommandLine1
     * @param CommandLine5 same as CommandLine1
     * @throws ScriptException when executing the Command Line failed (bubbles up
     * from <code>Device.DispatchCommand</code> or from <code>getDouble</code>).
     */
    // <editor-fold defaultstate="collapsed" desc="Monitor Chart">
    @iC_Annotation(MethodChecksSyntax=true)
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Monitors up to 5 variables returned from execupting the passed Command Lines.</html>",
        ParameterNames = {"Time Intervall [sec]", "Y axis label","File Extension",
                          "Command Line 1", "Command Line 2", "Command Line 3", "Command Line 4", "Command Line 5"},
        DefaultValues = {"1", "Temperature [K]", ".monT", "\"\"", "\"\"", "\"\"", "\"\"", "\"\""},
        ToolTips = {"Time between two measurements.", "If the FileExtension does not contain a '.' a '.' is automatically added.",
                        "The Command Line must be enclosed in double quotes (\")",
                        "The Command Line must be enclosed in double quotes (\")",
                        "The Command Line must be enclosed in double quotes (\")",
                        "The Command Line must be enclosed in double quotes (\")",
                        "The Command Line must be enclosed in double quotes (\")"})
    public void MonitorChart(float TimeIntervall, String YLabel, String FileExtension,
                             String CommandLine1, String CommandLine2,
                             String CommandLine3, String CommandLine4,
                             String CommandLine5)
            throws ScriptException {

        //////////////////
        // local variables

        // make a new Device object to call it's DispatchCommand method
        // see Remark in javadoc (How to write new Instrument-Classes)
        Device dev = new Device();


        /**
         * Implement a new class from an anonymous inner class that extends
         * Thread and implements run(), in which the actual job is done.<p>
         *
         * The thread pauses, respectively, stops when <code>m_Paused</code>, respectively,
         * <code>m_StopScripting</code> is true.
         */
        // <editor-fold defaultstate="collapsed" desc="myThread as anonymous inner class">
        class myThread extends Thread {
            /** The command lines to execute to obtain the values to be displayed */
            public ArrayList<String> m_CommandLines;

            /** The file with the monitored temperature as text */
            public BufferedWriter m_FileWriter;

            /** The file with the monitored temperature as png */
            public File m_FileForChart;

            /** The time between two temperature measurements in sec */
            public float m_TimeInterval;

            // Identification of the Data Series (handle to the series)
            private SeriesIdentification[] m_SeriesIDs;

            // holds the chart
            private iC_ChartXY m_Chart;
            
            // is set to true when the thread has been startted
            public boolean m_ThreadStarted = false;

            /**
             * Default constructor. Prepares the XY chart
             * @param YLabel The label for the Y axis
             */
            public myThread(String YLabel) {

                // make a new XYChart object
                m_Chart = new iC_ChartXY("Monitor",
                                         "Time since Start [ sec ]",
                                         YLabel,
                                         true  /*legend*/,
                                         1024, 480);

                // use no decimal places on the X axis (time)
                m_Chart.getXAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());
                //m_Chart.getXAxis().setNumberFormatOverride(new DecimalFormat("0"));

                // don't include zero when auto ranging (default left axis)
                m_Chart.getYAxis(0).setAutoRangeIncludesZero(false);

                // switch off AntiAliasing for faster updates of the chart
                // Not recommended; see javadoc to iC_ChartXY.setAntiAlias
                //boolean old = m_Chart.setAntiAlias(false);
            }


            @Override
            public void run() {
                // local variables
                double Result = 0.0;
                Object obj=null;

                // add the new data series' to the chart
                m_SeriesIDs = new SeriesIdentification[m_CommandLines.size()];
                for (int i=0; i<m_CommandLines.size(); i++) {
                    m_SeriesIDs[i] = m_Chart.AddXYSeries(
                            m_CommandLines.get(i),  // the name
                            0,  // Y axis index (0=default left axis)
                            false, true, m_Chart.LINE_SOLID,
                            m_Chart.MARKER_CIRCLE);
                }

                // make a new Device object to call it's DispatchCommand method
                // see Remark in javadoc (How to write new Instrument-Classes)
                // TODO why did I not use 'this'? and why does it crash sometimes in test LM-500
                Device dev = new Device();
                
                // set that the Thread has started
                m_ThreadStarted = true;
                
                // do until the thread should be stopped
                while ( m_StopScripting == false) {

                    // check for pause button
                    isPaused(true);

                    // get elapsed time in seconds
                    double Time = (System.currentTimeMillis() - m_tic) / 1000.0;

                    // prepare the line to write in the file
                    // use Local.US to ensure a '.' is used as comma
                    // (in german a ',' is used as decimal point, which is sometimes
                    // not recognized as decimal point by some other programs like
                    // Origin, IgorPro, Matlab, Excel, ...
                    String line = String.format(Locale.US, "%.3f", Time);

                    
                    // disable event notification upon adding new data points
                    m_Chart.setNotify(false);


                    // do for all command lines
                    for (int i=0; i<m_CommandLines.size(); i++) {

                        try {
                            
                            // getting and displaying data can take longer than
                            // the default wait after the Stop button is pressed.
                            // With the Eurother it happened that the instrument
                            // was closed before this method exited, so check
                            // Stop signal more often (directly before communication)
                            if (m_StopScripting) {
                                break;
                            }

                            // execute the command line
                            obj = dev.DispatchCommand(m_CommandLines.get(i));

                            // convert the Instrument's answer to a double
                            Result = getDouble(obj.toString());
                            

                            // append to the line to write into the file
                            line += String.format(Locale.US, "\t%e", Result);

                            // add the datapoint to the graph
                            m_Chart.AddXYDataPoint(m_SeriesIDs[i], Time, Result);

                        } catch (Exception ex) {
                                String str = "Executing the Command Line " + m_CommandLines.get(i) + "\n"
                                    + "did not return a valid object.\n"
                                    + "Returned object: " + (obj==null?"null":obj.toString()) + "\n"
                                    + "Please check the Command Line, the connection to the Instrument,\n"
                                    + "and the CPU load (which might cause sporadic communication errors).\n\n"
                                    + ex.getMessage() + "\n";

                                // log event
                                m_Logger.severe(str);

                                // display the event
                                m_GUI.DisplayStatusMessage(str);
                        }
                    }

                    // re-enable event notification upon adding new data points
                    // also sends a trigger to update the chart
                    m_Chart.setNotify(true);
               

                    // write the line into the file
                    try {
                        m_FileWriter.write(line);
                        m_FileWriter.newLine();

                    } catch (IOException ex) {
                        String str = "Error writing to file in MonitorChart.\n";
                        m_Logger.log(Level.SEVERE, "{0}{1}\n", new Object[]{str, ex.getMessage()});
                        m_GUI.DisplayStatusMessage(str);
                    }


                    // wait the desired time
                    try {
                        Thread.sleep( Math.round(m_TimeInterval) );
                    } catch (InterruptedException ex) {}
                }

                // close the file
                try {
                    m_FileWriter.close();

                } catch (IOException ex) {
                    m_Logger.log(Level.SEVERE, "iC_Instrument.MonitorChart: Could not close the file", ex);

                    m_GUI.DisplayStatusMessage("Could not close the file in MonitorChart.\n");
                }


                ////////////////////////
                // save the chart as png
                // after the thread has finished
                try {
                    m_Chart.SaveAsPNG(m_FileForChart, 1024, 480);
                } catch (IOException ex) {
                    m_GUI.DisplayStatusMessage(ex.getMessage());
                }


                // Display a status message
                m_GUI.DisplayStatusMessage("MonitorChart has been stopped.\n");
            }
        }//</editor-fold>


        // make an ArrayList for further processing
        // NOTE: also change header line if adding a new Command Line
        ArrayList<String> CommandLines = new ArrayList<String>();
        CommandLines.add(CommandLine1);
        CommandLines.add(CommandLine2);
        CommandLines.add(CommandLine3);
        CommandLines.add(CommandLine4);
        CommandLines.add(CommandLine5);

        // prepare the command lines
        // run index backwards so it is correct if a line is removed
        // subtracting 1 from the index once a line is removed would also do
        for (int i=CommandLines.size()-1; i>=0; i--) {

            // remove double-quotes from the beginning and end of the CommandLine
            CommandLines.set(i, CommandLines.get(i).replaceFirst("^\"", "").replaceFirst("\"$", ""));

            // drop (remove) empty command lines
            if (CommandLines.get(i).isEmpty())
                CommandLines.remove(i);
        }


        // return if all Command Lines are empty
        if (CommandLines.isEmpty()) {
            m_Logger.warning("No Command Lines have been passed MonitorChart\n");
            return;
        }


        // Syntax Check
        if (inSyntaxCheckMode()) {

            // check all non-empty
            for (String str : CommandLines) {
                // check Syntax (throws an Exception if Syntax is not correct)
                dev.DispatchCommand(str);
            }
            
            // return from Syntax-Check mode
            return;
        }

        // get the file name and add the extension
        String FileName = m_GUI.getFileName(FileExtension);


        // open the file for writing
        // and write the headerline
        BufferedWriter fw;
        try {
            fw = new BufferedWriter(new FileWriter(FileName));

            for (int i=0; i<CommandLines.size(); i++) {
                // make the headerline
                String dummy = String.format(Locale.US, "Command Line #%d: %s",
                        i+1, CommandLines.get(i));

                // write the headerline
                fw.write(dummy);
                fw.newLine();
            }
            fw.newLine();
            fw.write("Time_[sec]\tcmd1\tcmd2\tcmd3\tcmd4\tcmd5");
            fw.newLine();

        } catch (IOException ex) {

            // message to the user
            String str = "Could not open the file\n"
                + FileName + "\n\n"
                + ex.getMessage() + "\n";

            // log event
            m_Logger.severe(str);

            // show a dialog
            JOptionPane.showMessageDialog(m_GUI.getTheFrame(),
                    str, "File open Error", JOptionPane.ERROR_MESSAGE, m_iC_Properties.getLogoSmall());

            return;
        }


        /////////////////////
        // prepare the thread

        // make a new thread object
        myThread myT = new myThread(YLabel);

        // set the input channel in the thread object
        myT.m_CommandLines = CommandLines;

        // pass the FileWriter to the thread
        myT.m_FileWriter = fw;

        // pass the Time Interval in sec
        myT.m_TimeInterval = 1000 * TimeIntervall;

        // pass a new file for saving the graph as png
        myT.m_FileForChart = new File(FileName + ".png");



        // Display a status message
        m_GUI.DisplayStatusMessage("Now starting MonitorChart.\n");

        // start the thread and return
        myT.start();
        
        // allow the thread to start
        // Apparently it can happen that the stop signal is propagated to the
        // Device class before the thread started, and the stop signal got
        // lost somehow on the way (TODO 3* understand this)
        // Test with 'test LM-500.iC' without the iC WaitForStop
        // TODO wait here until the thread is running.
        // 10ms delay help to propagate the StopSignal but the LM500
        // is cleaned up before the thread can stop and the thread
        // accesses it once when it is null
        //try {Thread.sleep(10);} catch (InterruptedException ignore) {}
        
        // wait until the thread has started
        // TODO 4* understand this and add it to the example code
        while (myT.m_ThreadStarted == false) {
            try {Thread.sleep(10);} catch (InterruptedException ignore) {}
        }
        
    }//</editor-fold>
    
    
    /**
     * This method displays <code>Message</code> in the Status Text Area of the
     * GUI. It's mainly used during development and testing.
     * @param Message The String to be shown in the Status Area of the GUI
     */
    // <editor-fold defaultstate="collapsed" desc="Display Status Message">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Displays a Status Message in the GUI.</html>",
        ParameterNames = {"Message"},
        DefaultValues = {"..."})
    public void DisplayStatusMessage(String Message) {
        
        // show the message
        m_GUI.DisplayStatusMessage(Message+"\n");
    }//</editor-fold>
    
    
    /**
     * 
     * @return The the free memory in kByte that is available for this 
     * application on the heap.
     */
    public long getFreeMemory() {
        
        Runtime RT = Runtime.getRuntime();
               
        // get free memory
        long FreeMem = RT.freeMemory();
        //m_GUI.DisplayStatusMessage("Free Memory= " + FreeMem/1024 + " kB\n", false);
        
        
        // use MXBean not System
        // yields the same values for free memory as Runtime.freeMemory
//        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
//        MemoryUsage MemUsage = memoryMXBean.getHeapMemoryUsage();
//        long FreeMem2 = MemUsage.getCommitted() - MemUsage.getUsed();
//        
//        m_GUI.DisplayStatusMessage("Available Heap= " + FreeMem2/1024 + "kB\n", false);
        
        // return result
        return FreeMem/1024;
    }
}
