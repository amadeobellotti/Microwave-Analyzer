
// TODO 5* add linear interpolation so that T range is +- 1K outside calibration Temperatures
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

package icontrol.drivers.instruments.lakeshore;

import icontrol.AutoGUIAnnotation;
import icontrol.IcontrolView;
import static icontrol.Utilities.getFloat;
import icontrol.drivers.Device;
import icontrol.iC_Annotation;
import icontrol.iC_ChartXY;
import icontrol.iC_ChartXY.SeriesIdentification;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.ScriptException;
import icontrol.drivers.Device.CommPorts;
import icontrol.drivers.instruments.iC_Instrument;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.LoessInterpolator;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;
import org.apache.commons.math3.exception.MathIllegalArgumentException;


/**
 * Lakeshore 340 Temperature Controller "driver" class.<p>
 *
 * All device commands that the Lakeshore 340 understands are
 * implemented here.<p>
 *
 * <h3>Instrument Commands:</h3>
 * <ul>
 *  <li>some commands are inherited from {@link icontrol.drivers.instruments.lakeshore.LakeshoreTC}
 *  <li>{@link #addCalibrationPoint(boolean, boolean, String, boolean, String) }
 *  <li>{@link #configControlLoopLimit(int, float, float, float, int, int) }
 *  <li>{@link #configLoopParameters(int, String, String)}
 *  <li>{@link #configRelay(String, String, boolean) }
 *  <li>{@link #readCalibrationPoints(String, String) }
 *  <li>{@link #setHeaterCurrentLimit(float)}
 *  <li>{@link #setHeaterRange(int) } (only to redefine the <code>@AutoGUIAnnotation</code>)
 *  <li>{@link #setTempCalibrate(float, int, float, String, float, int, boolean) }
 * </ul>
 *
 * @author KPP (Kurt Pernstich: pernstich@alumni.ethz.ch)
 * @version 0.5
 *
 */


// promise that this class supports GPIB communication
@iC_Annotation( CommPorts=CommPorts.GPIB,
                InstrumentClassName="Lakeshore 340")
public class Lakeshore340 extends LakeshoreTC {

    ///////////////////
    // member variables

    /**
     * The Logger for this class. Note that the Logger Level is set in the
     * constructor of the <code>Device</code> class.
     */
    private static final Logger m_Logger = Logger.getLogger("iC.Instruments.Lakeshore340");




    /**
     * Anonymous inner class defining Temperature / Sensor Unit value pairs.
     * It overrides <code>toString</code> and provides two constructors. It also
     * implements the <code>Comparable<TSUpair></code> interface.
     */
    // <editor-fold defaultstate="collapsed" desc="TSUpair">
    private class TSUpair implements Comparable<TSUpair> {
        private float Temperature;
        private float SensorUnit;

        /** default (empty) constructor */
        TSUpair() {
            Temperature = -1.2f;
            SensorUnit = -1.2f;
        }

        /** alternative constructor */
        TSUpair(float T, float SU) {
            Temperature = T;
            SensorUnit = SU;
        }

        /**
         * Overridden <code>toString</code> method.
         *
         * @return A string containing Temperature and Sensor Unit values
         */
        // <editor-fold defaultstate="collapsed" desc="toString">
        @Override
        public String toString() {
            return String.format(Locale.US, "T=%.3f SU=%.3f", Temperature, SensorUnit);
        }//</editor-fold>

        /**
         * Specialized <code>compareTo</code> method for <code>TSUpairs</code>.
         * It is used in <code>UpdateSensorCurve</code> which uses the
         * <code>Collections.sort</code> method. It compares two
         * <code>TSUpair</code>s, first for Temperature, then for SensorUnits.
         * The fitting algorithm requires increasing x-axis units (Temperature).
         *
         * @param o The TSUpair to be compared
         * @return The same value as {@link Comparable#compareTo(Object)}.
         */
        // <editor-fold defaultstate="collapsed" desc="compareTo">
        public int compareTo(TSUpair o) {
            // first compare Sensor Units
            int ret = (  (Comparable<Float>) this.Temperature  ).compareTo(
                                                o.Temperature );

            // check if equal
            if (ret == 0) {
                // if equal, compare Temperature
                ret = (  (Comparable<Float>) this.SensorUnit  ).compareTo(
                                                o.SensorUnit );
            }

            return ret;
        }//</editor-fold>
        
    };//</editor-fold>


    /**
     * List of temperature / Sensor Units (resistance) calibration points.
     *
     * This needs to be a <code>List</code> because a <code>Map</code> does not
     * allow identical entries.
     * @see LinkedHashMap
     */
    private ArrayList<TSUpair> m_CalibrationDataChannelA;
    private ArrayList<TSUpair> m_CalibrationDataChannelB;



    /** Handle to the JFreeChart Graph used to plot the calibration data */
    private iC_ChartXY m_SensorCurveChart = null;
    
    /** HashMap that holds CurveNumber-SeriesIdentification pairs and is used
     * to plot the measured data points for <code>addCalibrationPoint</code>
     * as well as the interpolation. */
    HashMap<Integer, SeriesIdentification> m_SensorCurveSeries;

    

    /**
     * Default constructor. It assigns proper values to <code>m_AvailableInputChannels</code>,
     * because the Lakeshore 340 Temperature controller has 4 Input Channels as
     * opposed to 2 defined in the generic implementation of <code>LakeshoreTC</code>. It
     * also updates the largest Input-Curve-Number to 60 and the Heater Range.
     */
    // <editor-fold defaultstate="collapsed" desc="Constructor">
    public Lakeshore340() {
        
        // invoke the base class' constructor
        super();

        // add Input Channels C and D (must be uppercase letters)
        m_AvailableInputChannels.add("C");
        m_AvailableInputChannels.add("D");

        // define the largest available Input Curve Number
        m_MaxInputCurveNumber = 60;

        // define the largest available Heater Range
        m_MaxHeaterRange = 5;
        
        
        //////////////////////////
        // Input Curve Calibration
        
        // handles to plot Temperature/Senos unit data
        m_SensorCurveSeries = new HashMap<Integer, SeriesIdentification>();
        
        // holds the calibration data
        m_CalibrationDataChannelA = new ArrayList<TSUpair>();
        m_CalibrationDataChannelB = new ArrayList<TSUpair>();
    }//</editor-fold>

    /**
     * Overridden <code>Close</code> method that puts the Instrument in a
     * local mode and clears the interface, just in case.
     *
     * @throws IOException re-thrown from <code>SendToInstrument</code>
     * @see Device#Close
     */
    // <editor-fold defaultstate="collapsed" desc="Close">
    @Override
    public void Close() throws IOException {

        // using GPIB do ...
        if (m_UsedCommPort == CommPorts.GPIB) {

            // set to local
            SendToInstrument("MODE 1");

            // clear the interface, just in case
            SendToInstrument("*CLS");
        }
    }// </editor-fold>


    
    /////////////////////////////////////////
    // Methods that implement a function that
    // can be called from the script
    // (methods that carry a AutoGUIAnnotation)


    /**
     * Sets the mode and state of the relay output of the Lakeshore 340.
     *
     * @param HighLow Specify which of the two relays to address. Can be 'high' or
     * 'low' (case insensitive).
     *
     * @param Mode Specify the operation mode of the relay. Can be 'off',
     * 'alarms' or 'manual' (case insensitive).
     *
     * @param OnOff Sets the state of the relay.
     *
     * @throws ScriptException When a wrong parameter was passed in <code>HighLow</code>
     * or <code>Mode</code>.
     *
     * @throws IOException When sending the command failed - bubbles up from
     * <code>SendToInstrument</code>
     */
    // <editor-fold defaultstate="collapsed" desc="configRelay">
    @AutoGUIAnnotation(
        DescriptionForUser = "Set the mode and state of the relay output.",
        ParameterNames = {"Which relay?", "Mode of operation", "Relay State"},
        ToolTips = {"Can be 'high' or 'low' (case insensitive).", "'off', 'alarms', or 'manual'", "'true'=ON, 'false'=OFF"},
        DefaultValues = {"low", "manual", "false"})
    public void configRelay(String HighLow, String Mode, boolean OnOff)
            throws ScriptException, IOException {

        // Syntax check
        if ( !HighLow.equalsIgnoreCase("high") &&
             !HighLow.equalsIgnoreCase("low")) {
            throw new ScriptException("Wrong parameter, please specify 'high' or 'low' in setRelay\n");
        }

        if ( !Mode.equalsIgnoreCase("off") &&
             !Mode.equalsIgnoreCase("alarms") &&
             !Mode.equalsIgnoreCase("manual")) {
            throw new ScriptException("Wrong parameter, please specify 'off', 'alarm', or 'manual' in setRelay\n");
        }


        // build the GPIB command
        String str = "RELAY ";

        // address the right relay
        str += (HighLow.equalsIgnoreCase("high") ? "1," : "2,");

        // add the mode
        if (Mode.equalsIgnoreCase("off")) str += "0,";
        if (Mode.equalsIgnoreCase("alarms")) str += "1,";
        if (Mode.equalsIgnoreCase("manual")) str += "2,";

        // add state
        str += OnOff ? "1" : "0";

        // send the command
        SendToInstrument(str);

    }// </editor-fold>

    
    /**
     * Set the Control Loop Parameters (see manual p. 9-31). The control loop is
     * always set to ON and power up is always disabled.<p>
     *
     * This method performs a SyntaxCheck.
     *
     * @param Loop The number of the loop to configure. Can be 1 or 2.
     *
     * @param InputChannel Selects the input channel for the loop; can be 'A' or 'B'.
     *
     * @param Unit Selects the unit; can be K for Kelvin, C for Celsius or SU for
     * Sensor Units (case insensitive).
     *
     * @throws DataFormatException when the Syntax check fails
     *
     * @throws IOException when GPIB communication fails (bubbles up from
     * <code>SendToInstrument</code>.
     *
     * {@link iC_Annotation#MethodChecksSyntax() }
     */
    // <editor-fold defaultstate="collapsed" desc="Configure Control Loop Parameters">
    @AutoGUIAnnotation(
        DescriptionForUser = "Configure Control Loop Parameters.",
        ParameterNames = {"Loop# {1 or 2}", "Input Channel {A or B}", "Unit {K or C or SU}" },
        DefaultValues = {"1", "A", "K"},
        ToolTips = {"", "", "Kelvin, Celcius or Sensor Units"})
    @iC_Annotation(MethodChecksSyntax=true)
    public void configLoopParameters (int Loop, String InputChannel, String Unit)
            throws DataFormatException, IOException {

        // Syntax Check for correct loop number
        checkLoop(Loop);
        
        // Syntax Check for correct Input channel
        checkInputChannel(InputChannel);
        

        // Syntax Check for correct Unit
        if ( !Unit.equalsIgnoreCase("K") &&
             !Unit.equalsIgnoreCase("C") &&
             !Unit.equalsIgnoreCase("SU")) {

            // the given Input Channel was incorrect
            String str = "The given Unit (\"" + Unit + "\") is incorrect.\n";
            str += "Please select either 'K', 'C', or 'SU' in Lakeshore340.ControlLoopParameters.";

            // throw an Exception to indicate SyntaxCheck error
            throw new DataFormatException(str);
        }


        // return if in Syntax-check mode
        if (inSyntaxCheckMode())
            return;

        // return if in No-Communication Moe
        if (inNoCommunicationMode())
            return;

        // build the GPIB command
        String str = String.format(Locale.US, "CSET %d,", Loop);

        // add input channel
        str += InputChannel + ",";

        // add units
        if (Unit.equalsIgnoreCase("K")) str += "1";
        if (Unit.equalsIgnoreCase("C")) str += "2";
        if (Unit.equalsIgnoreCase("SU")) str += "3";

        // add the rest
        str += ",1,0";

        // send the command
        SendToInstrument(str);

        // for debugging
       // m_GUI.DisplayStatusMessage(str + "\n", false);

    }// </editor-fold>


    /**
     * Specialized method that allows to calibrate a sensor with a known sensor.
     * This is achieved by first executing the <code>CommandLine</code> to query
     * the actual temperature of a known temperature sensor (for instance that of
     * a Si Diode mounted to the sample stage), then the value of the sensor
     * to calibrate is queried (in my case it's a Platinum thermistor fabricated
     * on a MEMS chip). Both values are stored in a List, then these points are
     * smoothed with a smoothing spline, interpolated with a denser mesh of
     * breakpoints, and sent to the Lakeshore 340's user curve. The
     * readings for calibration temperature and sensor units are averaged
     * as specified in the iC.properties. The calibration data is saved in files.<p>
     *
     * The user curve associated with input channel A/B is specified in the
     * iC.properties.<p>
     *
     * This method performs a Syntax Check where the <code>CommandLine</code> is
     * Syntax-checked (by dispatching it in Syntax-Check mode).<p>
     *
     * @param ChannelA When <code>true</code> the sensor attached to input
     * channel A will be calibrated, and the associated user curve will be
     * updated in the Lakeshore 340.
     *
     * @param ChannelB When <code>true</code> the sensor attached to input
     * channel B will be calibrated, and the associated user curve will be
     * updated in the Lakeshore 340.
     *
     * @param CommandLine This Command Line is dispatched and the result is interpreted
     * as the reference temperature. The return value of the invoked method must
     * be cast-able to a Float value.
     *
     * @param BurnToFlash When <code>true</code> the newly programmed user curve
     * is burnt into the internal Flash memory to preserve the curve after switching
     * off the instrument.
     *
     * @param FileExtension When this String is not empty, the calibration data
     * will be saved to a file with this file extension followed by '_A' or
     * '_B' for the chosen input channel. If the file cannot be opened, an Error
     * Status Message is shown in the GUI (the background should become red), and
     * the program continues without saving the calibration data. If the
     * filename already exists, a new file with a counter is inserted between the 
     * FileName and the FileExtension.
     *
     * @throws ScriptException Bubbles up from <code>Device.DispatchCommand</code>, from 
     * dispatching the <code>CommandLine</code>, from <code>getFloat</code>, or
     * gets thrown when the returned object from dispatching
     * <code>CommandLine</code> is <code>null</code> or not of type <code>float</code>.
     * @throws DataFormatException when the Syntax Check failed (no Command Line 
     * was specified)
     * @throws IOException Bubbles up from <code>QueryInstrument</code>, from
     * <code>UpdateSensorCurve</code>, or from writing to the <code>BufferedWriter</code>.
     * 
     */
    // <editor-fold defaultstate="collapsed" desc="Add Calibration Point">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Reads the current temperature from the specified Instrument and<br>uses this temperature as a calibration point for the specified Input Channel(s).</html>",
        ParameterNames = {"Calibrate Channel A", "Calibrate Channel B", "Reference Temperature Command", "Burn to Flash", "File Extension"},
        DefaultValues = {"true", "true", "\"Tstage getTemp default\"", "false", ".cal"},
        ToolTips = {"",
                    "",
                    "<html>This Command Line is executed and the result is interpreted as a temperature which<br> is then used to calibrate the resistance values measured in Channel A/B.<br>This String needs to be enclosed inside double-quotes (\") !!<br>Use with caution.</html>",
                    "<html>When true, the new User Curve is burnt into the internal Flash memory.<br>Only a burnt User curve is preserved after a switching off the instrument.</html>",
                    "When a non-empty File Extension is specified, the calibration data is saved to a file."})
    @iC_Annotation(MethodChecksSyntax=true)
    public void addCalibrationPoint(boolean ChannelA, boolean ChannelB,
                                    String CommandLine, boolean BurnToFlash,
                                    String FileExtension)
                throws ScriptException, DataFormatException, IOException {
        
        // local variables to the files used to save the calibration data
        BufferedWriter  FileChannelA = null;
        BufferedWriter  FileChannelB = null;
        File            FileChart = null;

        
        // remove double-quotes from the beginning and end of the CommandLine
        CommandLine = CommandLine.replaceFirst("^\"", "").replaceFirst("\"$", "");
        
        // make a new Device object to call it's DispatchCommand method
        // see Remark in javadoc (How to write new Instrument-Classes)
        Device dev = new Device();


        ///////////////
        // Syntax-Check
        
        // was a command line specified?
        if ( CommandLine.isEmpty() ) {
            String str = "No command line to determine the current temperature was provided.\n"
                    + "Please add a command line and try again.\n";
            throw new DataFormatException(str);
        }

        // Execute the Command Line
        // in Syntax-Check mode the returned object is null
        // when not in Syntax-Check mode, the object should be valid
        Object obj = dev.DispatchCommand(CommandLine);

        // check if returned object is of 'type' Float
        // during syntaxCheck the returned object can be null
        if ( obj == null && !inSyntaxCheckMode() ||
             obj != null && !(obj instanceof Float) ) {
            // returned object is not recognized, so throw an Exception
            String str = "Executing the Command Line\n" + CommandLine + "\n";
            str += "did not return a valid object (of 'type' Float). Please check the Command Line.\n";

            throw new ScriptException(str);
        }

        // return if in Syntax-check mode
        if (inSyntaxCheckMode())
            return;

        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return;


        // TODO delme (for development)
        if (true) {
            // read cal data from file and update defualt input curve in Lakeshore
            dev.DispatchCommand("Therm1 readCalibrationPoints 01 Calibrate._cal_A.txt; A");
            
//            dev.DispatchCommand("Therm1 readCalibrationPoints 01 Calibrate._cal_B.txt; B");
            
            return;
        }
        
        // TODO delme (for development)
        if (false) {
            m_CalibrationDataChannelA.add(new TSUpair(298.63974f,110.77699f));
            m_CalibrationDataChannelA.add(new TSUpair(298.63562f,110.77988f));
            m_CalibrationDataChannelA.add(new TSUpair(298.94897f,110.82775f));
            m_CalibrationDataChannelA.add(new TSUpair(299.73026f,111.054504f));
            m_CalibrationDataChannelA.add(new TSUpair(300.0235f,111.08837f));
            m_CalibrationDataChannelA.add(new TSUpair(300.49487f,111.16875f));
            m_CalibrationDataChannelA.add(new TSUpair(300.98938f,111.27013f));
            BufferedWriter bw = new BufferedWriter(new FileWriter(m_GUI.getFileName(".cal_Adevelop.txt")));
            UpdateSensorCurve(49, m_CalibrationDataChannelA, false, bw);
            
            
            m_CalibrationDataChannelB.add(new TSUpair(288.63974f,114.77699f));
            m_CalibrationDataChannelB.add(new TSUpair(288.63562f,114.77988f));
            m_CalibrationDataChannelB.add(new TSUpair(288.94897f,114.82775f));
            m_CalibrationDataChannelB.add(new TSUpair(289.73026f,115.054504f));
            m_CalibrationDataChannelB.add(new TSUpair(290.0235f,115.08837f));
            m_CalibrationDataChannelB.add(new TSUpair(290.49487f,115.16875f));
            m_CalibrationDataChannelB.add(new TSUpair(290.98938f,115.27013f));
            bw = new BufferedWriter(new FileWriter(m_GUI.getFileName(".cal_Bdevelop.txt")));
            UpdateSensorCurve(50, m_CalibrationDataChannelB, false, bw);
            
//            m_CalibrationDataChannelA.add(new TSUpair(30f,0.16f));
//            m_CalibrationDataChannelA.add(new TSUpair(10f,0.0029f));
//            m_CalibrationDataChannelA.add(new TSUpair(20f,0.0359f));
//
//            m_CalibrationDataChannelA.add(new TSUpair(40f,0.396f));
//            m_CalibrationDataChannelA.add(new TSUpair(50f,0.719f));
//            m_CalibrationDataChannelA.add(new TSUpair(50f,0.718f));
//
//            m_CalibrationDataChannelA.add(new TSUpair(60f,1.09f));
//            m_CalibrationDataChannelA.add(new TSUpair(70f,1.49f));
//            m_CalibrationDataChannelA.add(new TSUpair(80f,1.9f));
//            m_CalibrationDataChannelA.add(new TSUpair(90f,2.32f));
//            m_CalibrationDataChannelA.add(new TSUpair(100f,2.74f));
//            m_CalibrationDataChannelA.add(new TSUpair(120f,3.56f));
//            m_CalibrationDataChannelA.add(new TSUpair(140f,4.37f));
//
//            m_CalibrationDataChannelA.add(new TSUpair(50f,0.717f));
//
//            m_CalibrationDataChannelA.add(new TSUpair(160f,5.18f));
//            m_CalibrationDataChannelA.add(new TSUpair(180f,5.97f));
//            m_CalibrationDataChannelA.add(new TSUpair(200f,6.67f));
//            m_CalibrationDataChannelA.add(new TSUpair(220f,7.54f));
//            m_CalibrationDataChannelA.add(new TSUpair(250f,8.7f));
//            m_CalibrationDataChannelA.add(new TSUpair(273f,9.59f));
//            m_CalibrationDataChannelA.add(new TSUpair(295f,10.42f));
//
//            BufferedWriter bw = new BufferedWriter(new FileWriter(m_GUI.getFileName(".cal_develop")));
//            UpdateSensorCurve(50, m_CalibrationDataChannelA, true, bw);
            return;
        }



        // open a file if desired
        // <editor-fold defaultstate="collapsed" desc="open files">
        if ( !FileExtension.isEmpty() ) {

            // get the file name and add the extension
            String FileName = m_GUI.getFileName(FileExtension);


            try {
                // open the file for Channel A
                if (ChannelA) {
                    FileChannelA = new BufferedWriter(new FileWriter(FileName + "_A.txt"));
                    
                    // write the command line in the file's header
                    FileChannelA.write("% Command line: " + CommandLine + "\n");
                }

                // open the file for Channel B
                if (ChannelB) {
                    FileChannelB = new BufferedWriter(new FileWriter(FileName + "_B.txt"));
                    
                    // write the command line in the file's header
                    FileChannelB.write("% Command line: " + CommandLine + "\n");
                }
                
                // open file to save the chart as png
                FileChart = new File(FileName + ".png");
            } catch (IOException ex) {
                // show a dialog
                String str = "Error: Could not open the file. Calibration Data will not be saved !!\n";
                m_GUI.DisplayStatusMessage(str);
            }
        }//</editor-fold>
        
        
        ////////////////////////////////////////
        // average over a few measurement points
        int Average = m_iC_Properties.getInt("Lakeshore340.Average", 8);
        int AverageWaitTime = m_iC_Properties.getInt("Lakeshore340.AverageWaitTime", 250);
        Float Tavg = new Float(0);
        Float TchannelAavg = new Float(0);
        Float TchannelBavg = new Float(0);
        Float Tdummy = new Float(0);
        String LogString = "";
        
        for (int i=1; i<=Average; i++) {

            // dispatch command line to get calibration Temperature
            // obj should never be null or of an other type than float
            obj = dev.DispatchCommand(CommandLine);
            Tdummy = (Float) obj;
            Tavg += Tdummy;
            LogString += i + ": T" + Tdummy;
            
            // get sensor units of Channel A
            if (ChannelA) {
                // query current resistance value for Channel A
                String dummy = QueryInstrument("SRDG? A");

                // convert to float and add to average building and log string
                Tdummy = getFloat(dummy);
                TchannelAavg += Tdummy;
                LogString += "  A" + Tdummy;
            }
            
            // get sensor units of Channel B
            if (ChannelB) {
                // query current resistance value for Channel B
                String dummy = QueryInstrument("SRDG? B");

                // convert to float and add to average building and log string
                Tdummy = getFloat(dummy);
                TchannelBavg += Tdummy;
                LogString += "  B" + Tdummy;
            }
            
            // wait a bit
            try {Thread.sleep(AverageWaitTime);} catch (InterruptedException ex) {/*ignore*/}
        }
        
        // calculate average values
        Tavg /= Average;
        TchannelAavg /= Average;
        TchannelBavg /= Average;
        LogString += "   Tavg=" + Tavg + " SU A=" + TchannelAavg   
                + " SU B=" + TchannelBavg + "\n";
        
        // log averaging
        m_Logger.fine(LogString);
        
        
        ////////////////////
        // process Channel A
        if (ChannelA) {
            
            // make a new TSUpair
            // this is necessary for each data pair as List.add does not
            // copy the object, but only stores a reference to it
            TSUpair TSUvalue = new TSUpair(Tavg, TchannelAavg);
            
            // add to the list of calibration points
            m_CalibrationDataChannelA.add(TSUvalue);

            // get the User Curve Number from the iC.properties
            int Curve_A = m_iC_Properties.getInt("Lakeshore340.UserCurve_A", 51);
            
            // log data point
            m_Logger.log(Level.FINE, "Adding Calibration Point to {0}/Curve_A(#{1}): T={2}\tSU={3}",
                new Object[] {m_InstrumentName, Curve_A, TSUvalue.Temperature, TSUvalue.SensorUnit});

            // update the Sensor curve
            UpdateSensorCurve(Curve_A, m_CalibrationDataChannelA, BurnToFlash, FileChannelA);
            
            // re-assign sensor curve (curve is set to 0 when chosen curve is being programmed)
            SendToInstrument("INCRV A," + Curve_A);
        }


        ////////////////////
        // process Channel B
        if (ChannelB) {

            // make a new TSUpair
            TSUpair TSUvalue = new TSUpair(Tavg, TchannelBavg);
            
            // add to the list of calibration points
            // if no new TSUvalue is created, m_CalibrationDataChannelA would also be altered
            m_CalibrationDataChannelB.add(TSUvalue);

            // get the User Curve Number form the iC.properties
            int Curve_B = m_iC_Properties.getInt("Lakeshore340.UserCurve_B", 52);
            
            // log data point
            m_Logger.log(Level.FINE, "Adding Calibration Point to {0}/Curve_B(#{1}): T={2}\tSU={3}",
                new Object[] {m_InstrumentName, Curve_B, TSUvalue.Temperature, TSUvalue.SensorUnit});

            // update the Sensor curve
            UpdateSensorCurve(Curve_B, m_CalibrationDataChannelB, BurnToFlash, FileChannelB);
            
            // re-assign sensor curve (curve is set to 0 when chosen curve is being programmed)
            SendToInstrument("INCRV B," + Curve_B);
        }
        
        
        // re-assing Kelvin unit
        // as control parameter of Loop 1 (is set to Ohms when reprograming the input curve)
        SendToInstrument("CSET 1,,1");
        
        // save the Chart as png
        if (m_SensorCurveChart != null) {
            try {
                m_SensorCurveChart.SaveAsPNG(FileChart, 0, 0);
            } catch (IOException ex) {
                m_GUI.DisplayStatusMessage("Warning: Could not save the Chart with the calibration data.\n");
            }
        }
        
        
        // close the files
        try {
            if (FileChannelA != null) 
                FileChannelA.close();
            
            if (FileChannelB != null) 
                FileChannelB.close();
            
        } catch (IOException ex) {

            // display status message
            m_GUI.DisplayStatusMessage("Error: Could not close the calibration data files.\n");
        }
    }//</editor-fold>


    /**
     * Specialize method for Thermo-Electrical Measurements. It reads previously 
     * recorded calibration points for the Temperature controller's input curve
     * from a file as it was created by <code>UpdateSensorCurve</code> and
     * sends the generated Input Curve to the Temperature Controller using the
     * predefined Curve Numbers (Lakeshore340.UserCurve_A). The curves are
     * not saved to Flash memory.<p>
     * 
     * The data is read from the file also during Syntax-Check, but it's not
     * added to the calibration points.
     * 
     * @param FileName The name of the file from which the calibration data is to
     * be read. The file is searched only in the Project Directory specified in the GUI.
     * 
     * @throws ScriptException If opening/reading/closing from the file failed, or
     * <code>getFloat</code> could not convert the number (which should not happen 
     * after Regex identified a float, or if neither A or B was specified as Input Channel.
     * 
     * @throws DataFormatException If the Syntax-Check fails (only when a wrong
     * InputChannel was specified)
     */
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Specialized method for Thermo-Electric measurements.<br>"
                + "Reads previously measured calibration data for the input curve from a file.<br>"
                + "Previous calibration data is overwritten!</html>",
        ParameterNames = {"File Name", "Input Channel {default, A, B, (C, ...)}"},
        DefaultValues = {"Thermistor Calibration.cal_A.txt", "A"},
        ToolTips = {"The file must be located in the project directory", ""})
    @iC_Annotation(MethodChecksSyntax=true)
    public void readCalibrationPoints(String FileName, String InputChannel) 
           throws DataFormatException, ScriptException {
        
        ///////////////
        // Syntax-Check

        // Syntax Check for correct input channel
        InputChannel = checkInputChannel(InputChannel);
        
        // make File Name
        FileName = m_GUI.getProjectPath() + FileName;
        
        try {
            String line;
            
            // open the file
            BufferedReader fr = new BufferedReader(new FileReader(FileName));
            
            // make a pattern for 4 floats separated by \t
            Pattern p = Pattern.compile(
                    "([-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?)\\t"
                  + "([-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?)\\t"
                  + "([-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?)\\t"
                  + "([-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?)");
            
            // delete old data
            if (InputChannel.equalsIgnoreCase("A")) {
                m_CalibrationDataChannelA.clear();
                
            } else if (InputChannel.equalsIgnoreCase("B")) {            
                m_CalibrationDataChannelB.clear();    
            }
            
            // read line by line
            while ( (line = fr.readLine()) != null) {
    
                // make a matcher
                Matcher m = p.matcher(line);
                
                // check if a match is found
                if ( m.matches() ) {
                    // devel
                    float f1 = getFloat(m.group(5));
                    float f2 = getFloat(m.group(7));
                    
                    // make a new TSU pair
                    TSUpair dummy = new TSUpair(f1, f2);
                    
                    // append to calibration data points
                    if (!inSyntaxCheckMode()) {
                        if (InputChannel.equalsIgnoreCase("A")) {
                            m_CalibrationDataChannelA.add(dummy);
                        } else if (InputChannel.equalsIgnoreCase("B")) {
                            m_CalibrationDataChannelB.add(dummy);
                        } else {
                            String str = "The specified Input Channel (" + InputChannel 
                                    + ") is invalid.\nIt must either be A, or B.\n";

                            throw new ScriptException(str);
                        }
                    } else {
                        m_GUI.DisplayStatusMessage("Would have added " + f1 + "  " + f2 + "\n", false);
                    }                    
                }                
            }
            
            // decide which channel to update (InputChannel is already A or B)
            if ( !inSyntaxCheckMode() ) {
                if (InputChannel.equalsIgnoreCase("A")) {

                    // get curve number to update
                    int Curve = m_iC_Properties.getInt("Lakeshore340.UserCurve_A", 51);

                    // update the curves in the Lakeshore
                    UpdateSensorCurve(Curve, m_CalibrationDataChannelA, false, null);

                    // select the new input curve
                    SendToInstrument("INCRV A," + Curve);
                } else {
                    // get curve number to update
                    int Curve = m_iC_Properties.getInt("Lakeshore340.UserCurve_B", 52);

                    // update the curves in the Lakeshore
                    UpdateSensorCurve(Curve, m_CalibrationDataChannelB, false, null);

                    // select the new input curve
                    SendToInstrument("INCRV B," + Curve);
                }
            }
                        
            // close file
            fr.close();
        
        } catch (IOException ex) {
            // IOException includes FileNotFoundException
            String str = "Could not read from file\n"
                    + FileName + "\n\n" + ex.getMessage();
            
            throw new ScriptException(str);
        }
    }
    
    /**
     * Specialized method for Thermo-Electric measurements. The specified 
     * temperature will be approached and at certain temperatures above, both 
     * channels of "Calibrated Instrument" will be calibrated against the
     * temperature of "Reference Instrument".
     * 
     * @param Temperature SetPoint temperature after executing this method
     * @param NrOfPoints The number of calibration points above the new SetPoint
     * temperature at which both channels of the "Calibrated Instrument" are 
     * calibrated. An additional calibration point will be inserted at the SP 
     * temperature itself.
     * @param DeltaT The temperature difference between the individual calibration points
     * @param ReferneceInstrument The name of the Instrument used to determine 
     * the reference temperature
     * @param TemperatureTolerance Defines the allowed temperature variation for 
     * viewing the temperature as stable
     * @param StabilizationTime The time during which the temperature needs to be 
     * within tolerance for viewing the temperature as stable
     * @param BurnToFlash When true, the new User Curve is burnt into the internal 
     * Flash memory after adding the last calibration point. Only a burnt User 
     * curve is preserved after a switching off the instrument.
     * 
     * @throws DataFormatException When the Syntax-Check failed
     * @throws ScriptException When executing the Commands to set the new SetPoint
     * or adding a calibration point failed.
     */
    // <editor-fold defaultstate="collapsed" desc="setTempCalibrate">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Specialized method for Thermo-Electric measurements.<br>"
            + "The specified temperature will be approached and at certain temperatures above,<br>"
            + "both channels of this Instrument will be calibrated against 'Reference Instrument'.</html>",
        ParameterNames = {"Temperature", "# of calibration points", "Temperature Difference", 
            "Reference Instrument Name", "Temperature Tolerance", 
            "Stabilization Time", "Burn Input Curve To Flash?"},
        DefaultValues = {"295", "2", "1", "Tstage", "0.1", "60"},
        ToolTips = {"SetPoint temperature after executing this method",      
            "<html>The number of calibration points above the new SetPoint temperature<br>"
                + "at which both channels of the 'Calibrated Instrument' are calibrated.<br>"
                + "An additional calibration point will be inserted at the SP temperature itself.</html>",
            "<html>The temperature difference between the individual calibration points.</html>",
            "The name of the Instrument used to determine the reference temperature.",
            "Defines the allowed temperature variation for viewing the temperature as stable",
            "The time during which the temperature needs to be within tolerance for viewing the temperature as stable",
            "<html>When true, the new User Curve is burnt into the internal Flash memory after adding the last calibration point.<br>"
                + "Only a burnt User curve is preserved after a switching off the instrument.</html>"})
    @iC_Annotation(MethodChecksSyntax=true)
    // TODO 5* add file extension to argument list
    public void setTempCalibrate(float Temperature, int NrOfPoints, float DeltaT,     
                        String ReferneceInstrument,
                        float TemperatureTolerance, int StabilizationTime, boolean BurnToFlash) 
           throws DataFormatException, ScriptException {
        
        // make a new Device object to call it's DispatchCommand method
        // see Remark in javadoc (How to write new Instrument-Classes)
        Device dev = new Device();


        ///////////////////////////
        // generate command strings
        
        // set Stage Temperature
        // e.g. Tstage setTemp 0; 298.5; true; 0.1; 60
        String cmdSetStageTemp = String.format(Locale.US,
                "%s setTemp 0; %.3f; true; %.3f; %d", ReferneceInstrument, 
                Temperature, TemperatureTolerance, StabilizationTime);
        
        // add Calibration Point
        // e.g. Therm addCalibrationPoint true; true; "Tstage getTemp default"; false; .cal
        String cmdAddCalibrationPoint = String.format(Locale.US,
                "%s addCalibrationPoint true; true; \"%s getTemp default\"; %b; _cal", 
                m_InstrumentName, ReferneceInstrument, BurnToFlash);

        
        ///////////////
        // Syntax-Check
        
        // ensure NrPoints is positive
        if (NrOfPoints <=0) {
            throw new DataFormatException("The number of points must be >0\n");
        }
        
        // syntax-check command lines
        if (inSyntaxCheckMode()) {
            // Execute the Command Lines
            dev.DispatchCommand(cmdSetStageTemp);
            dev.DispatchCommand(cmdAddCalibrationPoint);
        }


        // return if in Syntax-check mode
        if (inSyntaxCheckMode())
            return;

        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return;
        
        // store current content of Status bar
        String StatusBar = m_GUI.DisplayStatusLine("", false);
        
        
        //////////////////////////////
        // go through all temperatures
        for (int i=NrOfPoints; i>=0; i--) {
            
            // calc current Temperature
            float T = Temperature + i*DeltaT;
            
            // update command strings to set Stage Temperature
            cmdSetStageTemp = String.format(Locale.US,
                    "%s setTemp 0; %.3f; true; %.3f; %d", ReferneceInstrument,
                    T, TemperatureTolerance, StabilizationTime);
            
            // update command string to add Calibration Point
            cmdAddCalibrationPoint = String.format(Locale.US,
                    "%s addCalibrationPoint true; true; \"%s getTemp default\"; %b; _cal", 
                    m_InstrumentName, ReferneceInstrument, (i==0?BurnToFlash:false) );
            
            // update Status Bar
            m_GUI.DisplayStatusLine(StatusBar + " - approaching " + T + "K", false);
            
            // change SetPoint and wait until stable by executing the Command Line
            dev.DispatchCommand(cmdSetStageTemp);
            
            // if the user pressed the Stop button while waiting for the 
            // temperature to stabilize, this loop must end
            if ( m_StopScripting ) {
                break;
            }
                            
            // add the calibration point
            dev.DispatchCommand(cmdAddCalibrationPoint);
        }
    }//</editor-fold>
    
    /**
     * Writes the temperature (T) and resistance (R) values into the User Curves.
     * It also writes a curve header, so when the sensor unit is changed from
     * Ohm to some other sensor unit (SU), the header info needs to be adapted to.
     * The Sensor Curve is not programmed into the 340 if less than two data points
     * have been added.<p>
     *
     * This methods sorts the T/SU lists from low temperatures to high temperatures,
     * then smooths the data points with a Loess Interpolation (thanks to Apache 
     * Commons Math) and interpolates the spline with a specified number of points. 
     * The interpolated points are sent to the User Curve, which can also be 
     * burnt into the internal Flash memory.<p>
     *
     * The smoothing parameters are defined in iC.properties under section Lakeshore340.<p>
     *
     * If a valid <code>BufferedWriter</code> object (not <code>null</code>) is
     * specified, the calibration data will be saved in this file.<p>
     *
     * Remark: The interpolation algorithm requires the X values (temperature)
     * to be in increasing order. The Lakeshore 340 requires an increasing value
     * of the sensor units with increasing point number. Because the method is
     * written for a thermistor with a positive temperature coefficient, an
     * increasing temperature results in increasing sensor units. If a sensor
     * with negative temperature coefficient should be calibrated, change the
     * Lakeshore340.TemperatureCoefficient property in iC.properties.<p>
     *
     * Remark: The interpolation algorithm does not allow multiple entries
     * with the same X value. This method checks for this possibility and calculates
     * an average of the Sensor Unit when multiple entries at the same temperature
     * exist.<p>
     *
     * Remark: The calibration points are written with 3 digit precision. The
     * maximum allowed precision would be 6 digits, but 1 mK resolution appears
     * fine for now.<p>
     * 
     * Remark: If no interpolation was selected with Lakeshore240.InterpolationType
     * then the measured calibration points are directly written to the instrument
     * which does, as far as I remember, a linear interpolation. When more than 
     * 200 cal. points have been taken, this method issues a WaitForUser command
     * to offer the possibility to manually thin-out the data points and re-take
     * the calibration data point again (using readCalibrationPoint, addCalibrationPoint,
     * of setTempCalibrate).<p>
     *
     * @param CurveNumber The number of the curve to program. Must be between 21
     * and 60 - no error checking is done.
     *
     * @param CalibrationData List of temperature / sensor units (resistance) value
     * pairs for the new user curve. The content of this list is not altered.
     *
     * @param BurnToFlash when <code>true</code> the new user curve will be
     * programmed into the instrument's internal flash memory
     *
     * @param File If <code>File != null</code> then the Calibration data will be
     * saved into this file, and the file will be closed afterwards.
     *
     * @throws IOException Bubbles up from <code>SendToInstrument</code>
     *
     * @throws ScriptException When the interpolating curve could not be generated
     * or evaluated, or an invalid interpolation type was selected in iC.properties
     */
    // <editor-fold defaultstate="collapsed" desc="Update Sensor Curve">
    private void UpdateSensorCurve( int     CurveNumber,
                                    final   ArrayList<TSUpair> CalibrationData,
                                    boolean BurnToFlash,
                                    BufferedWriter    File)
                    throws IOException, ScriptException {
        
        // check if a Chart has already been created
        if (m_SensorCurveChart == null) {
            // create a new Chart
            m_SensorCurveChart = new iC_ChartXY("Update Sensor Curves",
                    "T [K]", "Sensor Unit [Ohm]", true, 0, 0);
            
            // do not include 0 in AutoScaling of left y-axis
            m_SensorCurveChart.getYAxis(0).setAutoRangeIncludesZero(false);
        }
        
        ////////////////////////////////////
        // check if the series already exist
        // The key to the HashMap for the measured sensor curve is equal to the CurveNumber
        // The key to the interpolated curve is CurveNumber+1000 to make things easier (HashMap cannot have two keys)
        SeriesIdentification SeriesID;
        
        // check for measured data
        if ( !m_SensorCurveSeries.containsKey(CurveNumber) ) {
            // make a new series for the measured data
            SeriesID = m_SensorCurveChart.AddXYSeries("Measured #" + CurveNumber, 0, false, true,
                    m_SensorCurveChart.LINE_NONE, m_SensorCurveChart.MARKER_CIRCLE);
            
            // store series idenentification in the HasMap
            m_SensorCurveSeries.put(CurveNumber, SeriesID);
        }
        
        // check for interpolation data
        if ( !m_SensorCurveSeries.containsKey(CurveNumber + 1000) ) {
            // make a new series for the interpolated data
            SeriesID = m_SensorCurveChart.AddXYSeries("Interpolated #" + CurveNumber, 0, false, true,
                    m_SensorCurveChart.LINE_SOLID, m_SensorCurveChart.MARKER_NONE);
            
            // store series idenentification in the HasMap
            m_SensorCurveSeries.put(CurveNumber + 1000, SeriesID);
        }
        
 
        // duplicate CalibrationData before manipulating it
        ArrayList<TSUpair> CalData = new ArrayList<TSUpair>(CalibrationData);


        // sort the Calibration Data
        // first for increasing temperature then for increasing SensorUnit
        // increasing Temperatures are required by the fitting algorithm
        // http://stackoverflow.com/questions/109383/how-to-sort-a-mapkey-value-on-the-values-in-java
        Collections.sort(CalData);


        //////////////////////////////
        // check for identical entries
        // (multiple sensor unit entries at the same temperature)
        // <editor-fold defaultstate="collapsed" desc="check for identical entries">
        // iterate through all elements
        for (int FirstIndex=0; FirstIndex < CalData.size(); FirstIndex++) {

            // get current temperature/sensor unit value
            float ThisTemperature = CalData.get(FirstIndex).Temperature;

            // find last index with the same temperature
            int LastIndex;
            for (LastIndex=CalData.size()-1; LastIndex >= FirstIndex; LastIndex--) {
                // end the for loop as soon as the same temperature is found
                if (CalData.get(LastIndex).Temperature == ThisTemperature)
                    break;
            }

            // check if there are identical temperature values
            if ( FirstIndex != LastIndex ) {
                // yes, so calc the mean, remove the original entries
                // and add the mean value of the Sensor Unit for the given temperature

                // iterate through the identical temperatures
                // and calc the sum of the sensor units
                float mean = 0;
                for (int t=FirstIndex; t<= LastIndex; t++) {
                    mean += CalData.get(t).SensorUnit;
                }

                // now calc the mean value
                mean = mean / (LastIndex - FirstIndex + 1);

                // remove the original entries from the list starting from the last
                for (int i=LastIndex; i>=FirstIndex; i--) {
                    CalData.remove(i);
                }

                // add new mean value
                CalData.add(FirstIndex, new TSUpair(ThisTemperature, mean));
            }
        }//</editor-fold>

        
        // convert the ArrayList into two double arrays
        // maybe there is a more elegant way using toArray
        // please inform me if you do know this elegant way
        double[] X = new double[CalData.size()];
        double[] Y = new double[CalData.size()];

        for (int i=0; i<CalData.size(); i++) {
            X[i] = CalData.get(i).Temperature;
            Y[i] = CalData.get(i).SensorUnit;
        }
        
        
        ///////////////////////////
        // plot measured T/SU pairs

        // get Series ID for the chart
        SeriesID = m_SensorCurveSeries.get(CurveNumber);
        
        // plot the data
        if (SeriesID != null) {
            // clear series first
            m_SensorCurveChart.ClearSeries(SeriesID);
            
            // plot measured data
            m_SensorCurveChart.AddXYDataPoint(SeriesID, X, Y);            
        } else {
            m_GUI.DisplayStatusMessage("Develop Error: no SeriesID for CurveNumber\n");
        }

        // to store the interpolated data
        double[] Xp;
        double[] Yp;
        int NrOfPoints;
        
        // get interpolation type
        String InterpolationType = m_iC_Properties.getString("Lakeshore340.InterpolationType", "Linear");
        
        // continue depending on the chosen interpolation type
        if (InterpolationType.equalsIgnoreCase("Loess")) {
        
            // return if only one or two data points are available
            if ( X.length < 3 )
                return;


            // interpolation points
            // max. 200 points are accepted by the Lakeshore 340
            // To avoid misinterpretation of the data by the Lakeshore 340 when only
            // a small number of calibration points have been measured (which might
            // result in identical interpolated SU values) only a 5x denser interpolation
            // is used
            NrOfPoints = 5*CalData.size();
            if (NrOfPoints > 200) {
                NrOfPoints = 200;
            }
            Xp = new double[NrOfPoints]; // Xp, Yp are interpolation points
            Yp = new double[NrOfPoints];


            ////////////////////////////////
            // fit using Apache Commons Math

            UnivariateInterpolator interpolator;
            UnivariateFunction function;

            double Bandwidth = m_iC_Properties.getDouble("Lakeshore340.LoessBandwidth", 1);
            int RobustnessIters = m_iC_Properties.getInt("Lakeshore340.LoessRobustnessIterations", 2);
            double Accuracy = m_iC_Properties.getDouble("Lakeshore340.LoessAccuracy", 1e-12);

            /* adjust Bandwidth for low number of data points to prevent a MathException
             * when interpolating. As far as I can tell, the Bandwidth must be larger
             * than 2/#data points and smaller than 1 */
            if (Bandwidth < (2.0/X.length)) {
                Bandwidth = 2.0/X.length;
            }

            // TODO 9* why does the linear interpoaltor not work. It should, then I do not need the 
            // complicated WaitForUser when more than 200 cal points were taken
            try {
                // make the interpolator
                // http://commons.apache.org/math/userguide/analysis.html#a12.6_Curve_Fitting
                if (InterpolationType.equalsIgnoreCase("Loess")) {
                    interpolator = new LoessInterpolator(Bandwidth, RobustnessIters, Accuracy);
                } else if (InterpolationType.equalsIgnoreCase("Linear")) {
                    interpolator = new LinearInterpolator();
                } else {
                    throw new ScriptException("The value of Lakeshore340.InterpolationType in iC.properties is not recognized.\n");                
                }

                // generate the interpolating function
                function = interpolator.interpolate(X, Y);

                // in SSE: http://www.iro.umontreal.ca/~simardr/ssj/indexe.html
                // SmoothingCubicSpline fit = new SmoothingCubicSpline(X, Y, rho); // with rho=0.5

            } catch (MathIllegalArgumentException ex) {

                String str = "Error: Could not generate the interpolating curve.\n";
                str += ex.getMessage() + "\n";

                // just show a Status Message and end updating the curve
                // maybe with the addition of the next point the fitting works
                m_GUI.DisplayStatusMessage(str);

                return;
                //throw new ScriptException(str);
            }


            // interpolate with a specified number of points
            double Step = (X[ X.length-1 ] - X[0]) / (NrOfPoints - 1);

            for (int i = 0; i < NrOfPoints; i++) {
                // calc new x value
                double x = X[0] + i * Step;

                // store interpolated values
                Xp[i] = x;
                try {
                    Yp[i] = function.value(x); // evaluate spline at z
                    // in SSE: fit.evaluate(x);

                } catch (MathIllegalArgumentException ex) {
                    String str = "Could not evaluate the interpolating function.\n";
                    str += ex.getMessage() + "\n";

                    throw new ScriptException(str);
                }
            }
        } else if (InterpolationType.equalsIgnoreCase("none")) {
            
            // get the number of calibration points and make sure less than 200 are present
            NrOfPoints = X.length;
            if (NrOfPoints > 200) {
                // interrupts scripting to let user load new calibration data manually
                final String str = "More than 200 calibration points were taken and\n"
                        + "no interpolation was selected. Scripting is now interrupted to\n"
                        + "give you the opportunity to manually load thinned-out calibration\n"
                        + "data using readCalibrationPoints and re-take the current calibration\n"
                        + "point by manually issuing addCalibrationPoint or setTempCalibrate.\n"
                        + "If you continue with more than 200 data points, scripting will stop\n"
                        + "because the Lakeshore will throw an error.\n";
                
                // make a new iC_Instrument
                iC_Instrument inst = new iC_Instrument();

                // execute the WaitForUser method
                inst.WaitForUser(str);
                
                // stop processing
                return;                
            }
            
            // just copy the data from X,Y to Xp,Yp and let the Lakeshore do 
            // the interpolation
            Xp = new double[NrOfPoints]; // Xp, Yp are interpolation points
            Yp = new double[NrOfPoints];
            
            for (int i=0; i<NrOfPoints; i++) {
                Xp[i] = X[i];
                Yp[i] = Y[i];
            }
            
        } else {
            String str = "The chosen interpolation type is not supported";
            throw new ScriptException(str);
        }
        
        // delete old input curve
        SendToInstrument("CRVDEL " + CurveNumber);
        
  
        // get current date
        Calendar cal = Calendar.getInstance();
        
        // write Curve header
        String str = String.format("CRVHDR %d,iC_cal,%02d%02d", 
                CurveNumber, cal.get(Calendar.MONTH)+1, cal.get(Calendar.DAY_OF_MONTH));
        str += ",3";    // Ohms / K
        str += ",375";  // max Temp
        str += ",2";    // positive temperature coefficient

        SendToInstrument(str);


        // get Temperature Coefficient
        double TemperatureCoefficient = m_iC_Properties.getDouble("Lakeshore340.TemperatureCoefficient", 1);
        
        // write Curve points
        // Sensor Units must be increasing with point number
        for (int i=0; i < Xp.length; i++) {
            
            // The Lakeshore 340 requires increasing SensorUnits with increasing 
            // point numbers. For positive temperature coefficients, the order
            // of the data points need to be reversed (page 8-3)
            int ii;
            
            if (TemperatureCoefficient > 0) {
                ii = i;
            } else {
                ii = Xp.length - i - 1;
            }

            // build string
            str = String.format(Locale.US, "CRVPT %d,%d,%.3f,%.3f", CurveNumber,
                    i+1, Yp[ii], Xp[ii]);

            // send to instrument
            SendToInstrument(str);
        }
        

        ///////////////////////////////
        // plot the interpolation curve
        
        // get Series ID for the chart
        SeriesID = m_SensorCurveSeries.get(CurveNumber + 1000);
        
        // plot the data
        if (SeriesID != null) {
            // clear, then update the chart for the interpolated values
            m_SensorCurveChart.ClearSeries(SeriesID);
            m_SensorCurveChart.AddXYDataPoint(SeriesID, Xp, Yp);
        } else {
            m_GUI.DisplayStatusMessage("Develop Error: no SeriesID for CurveNumber\n");
        }
        


        ////////////////
        // write to file
        if (File != null) {
            try {
                // write header line
                File.write("% Instrument Control (iC)"); File.newLine();
                File.write("% Temperature / Sensor Unit Calibration"); File.newLine();
                File.write("% T should be in Kelvin, SU in Ohm"); File.newLine();
                File.newLine();
                File.write("Tinterpolated\tSUinterpolated\tTmeasured\tSUmeasured"); File.newLine();

                // write data points
                for (int i = 0; i < NrOfPoints; i++) {
                    // check if the line to write contains also measured data
                    if ( i < X.length ) {
                        str = String.format(Locale.US, "%f\t%f\t%f\t%f", Xp[i], Yp[i], X[i], Y[i]);
                    } else {
                        str = String.format(Locale.US, "%f\t%f", Xp[i], Yp[i]);
                    }

                    // write the line to the file
                    File.write(str); File.newLine();
                }

                // close the file
                File.close();

                // display status message
                m_Logger.fine("Lakeshore 340: New Calibration data was saved to file.\n");
//                m_GUI.DisplayStatusMessage("Lakeshore 340: New Calibration data "
//                        + "was saved to file.\n");

            } catch (IOException ex) {
                m_GUI.DisplayStatusMessage("Error writing Calibration data to the file!");

                // close the file
                try{File.close();} catch (IOException ex2) {}

            }
        }
        
        //////////////////
        // write to flash?
        if (BurnToFlash) {
            // display status message
            m_GUI.DisplayStatusMessage("Writing calibration curve to flash memory. Takes a few seconds...\n");

            // send command to save curve
            SendToInstrument("CRVSAV");
                        
            // Wait until curve is saved
            // initially I tried to use *ESE 1, *SRE 32, *OPC? and *STB? respectively *ESR?
            // to check for completion. Then I contacted Lakeshore and they told me
            // to read the manual where the use of BUSY? is recommended in the CRVSAV command
            String BusyStatus;
            do {
                try {Thread.sleep(750);} catch (InterruptedException ex) {/* ignore */}
                
                // query busy status
                BusyStatus = QueryInstrument("BUSY?");
                
            } while ( !BusyStatus.equalsIgnoreCase("0") );
            

            // display status message
            m_GUI.DisplayStatusMessage("Calibration curve saved to flash memory.\n");
        }
        
    }//</editor-fold>


    /**
     * Sets the Heater Range (see manual chapter 6.12.1). When Loop 1 is configured
     * to use a user defined current compliance (CLIMIT), this current compliance
     * can be set with <code>setHeaterCurrentLimit</code>.<p>
     *
     * This method performs a Syntax-Check.
     *
     * @param Range The Heater range.
     *
     * @throws DataFormatException when the Syntax Check failed.
     * @throws IOException re-thrown from {@link Device#SendToInstrument}
     */
    // <editor-fold defaultstate="collapsed" desc="set Heater Range">
    @AutoGUIAnnotation(
        DescriptionForUser = "Set the Heater Range.",
        ParameterNames = {"Range { [0,5] }"},
        DefaultValues = {"1"},
        ToolTips = {"0 turns off the Heater. Use in combination with setUserCurrentLimit. See manual 6.12.1."})
    @iC_Annotation(  MethodChecksSyntax = true )
    @Override
    public void setHeaterRange(int Range)
                throws IOException, DataFormatException {

        // just call the base class's method
        super.setHeaterRange(Range);
    }//</editor-fold>

    /**
     * Sets the maximum heater current to a user defined level. This current compliance
     * is only available on Firmware version 01.03.08 and later (see Lakeshore manual
     * on page 9-28. This current compliance is only applied when Loop 1 is
     * appropriately set-up (see CLIMIT command or chapter 6.12.1 in the manual).<p>
     *
     * This method performs a Syntax-Check.
     *
     * @param MaxHeaterCurrent Current compliance for the heater.
     *
     * @throws DataFormatException when the Syntax Check failed.
     * @throws IOException re-thrown from {@link Device#SendToInstrument}
     */
    // <editor-fold defaultstate="collapsed" desc="set Heater Current Limit">
    @AutoGUIAnnotation(
        DescriptionForUser = "Sets the maximum heater current output for Loop 1.",
        ParameterNames = {"Max. Heater Current { [0.1,2]A }"},
        DefaultValues = {"0.1"},
        ToolTips = {"<html>Max. Current output for Loop 1 when operating in<br>"
                + "the User setting for the Max Current Limit (see ControlLoopLimit)."})
    @iC_Annotation(  MethodChecksSyntax = true )
    public void setHeaterCurrentLimit(float MaxHeaterCurrent)
                throws DataFormatException, IOException {

        ///////////////
        // Syntax-Check

        // Syntax check for max. heater current
        if ( MaxHeaterCurrent < 0.1 || MaxHeaterCurrent > 2.0f ) {
            String str = "The max. heater current must be between 0.1 and 2.0.\n";

            throw new DataFormatException(str);
        }


        // return if in Syntax-check mode
        if (inSyntaxCheckMode())
            return;


        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return;


        // build the GPIB command
        String cmd = String.format(Locale.US, "CLIMI %.3f", MaxHeaterCurrent);

        // send via GPIB
        SendToInstrument(cmd);

    }//</editor-fold>


    

    /**
     * Configures the Control Loop Limit Parameters. See Lakeshore manual p. 9-28
     * and chapter 6.13.<p>
     *
     * This method performs a Syntax-Check.
     *
     *
     * @param Loop The number of the loop to change the ramp rate. Can be 1 or 2.
     *
     * @param SetPointLimit The heater output is switched off when this value is reached.
     *
     * @param PositiveSlope Max. positive change of output in %/sec - 0 disables this slope.
     * @param NegativeSlope Max. change in output when the output goes down. 0 disables this slope.
     *
     * @param MaxCurrent Max. current for Loop 1 heater output (see ToolTips for details).
     *
     * @param MaxHeaterRange Max. Loop #1 heater range.
     *
     * @throws DataFormatException when the Syntax check fails
     *
     * @throws IOException when GPIB communication fails (bubbles up from
     * <code>SendToInstrument</code>.
     */
    // <editor-fold defaultstate="collapsed" desc="set Heater Current Limit">
    @AutoGUIAnnotation(
        DescriptionForUser = "Configure the Control Loop Limit Parameters.",
        ParameterNames = {"Loop# {1 or 2}", "Set Point Limit [K]",
                          "Positive Slope [%/sec]", "Negative Slope [%/sec]",
                          "Max. Current Setting { [1,5] }", "Max. Heater Range { [0,5] }"},
        DefaultValues = {"1", "350", "10", "50", "5", "2"},
        ToolTips = {"", "The heater output is switched off when this value is reached.",
                    "Max. positive change of output - 0 disables this slope.", "Max. change in output when the output goes down. 0 disables this slope.",
                    "<html>Max. current for Loop #1 heater output:<br>1 = 0.25 A<br>2 = 0.5 A<br>3 = 1 A<br>4 = 2 A<br>5 = User (see setHeaterCurrentLimit)</html>",
                    "Max. heater range of Loop #1."})
    @iC_Annotation(  MethodChecksSyntax = true )
    public void configControlLoopLimit(int Loop, float SetPointLimit,
                            float PositiveSlope, float NegativeSlope,
                            int MaxCurrent, int MaxHeaterRange)
                throws DataFormatException, IOException {

        ///////////////
        // Syntax-Check
        // <editor-fold defaultstate="collapsed" desc="Syntax Checks">

        // Syntax Check for correct loop number
        checkLoop(Loop);

        // Syntax Check for Set Point Limit
        if ( SetPointLimit < 0)
            throw new DataFormatException("The Set Point Limit must be greater than 0.");

        // Syntax check for pos/neg Slope
        if (PositiveSlope < 0 || PositiveSlope > 100)
            throw new DataFormatException("The positive slope must be between 0 and 100.");

        if (NegativeSlope < 0 || NegativeSlope > 100)
            throw new DataFormatException("The negative slope must be between 0 and 100.");

        // Syntax check for Max Current
        if (MaxCurrent < 1 || MaxCurrent > 5)
            throw new DataFormatException("The Max. Current Setting must be between 1 and 5.");

        // Syntax check for Max Heater Range
        // TODO 2* should m_MaxHeaterRange be updated ??
        checkHeaterRange(MaxHeaterRange);
            //</editor-fold>

        // return if in Syntax-check mode
        if (inSyntaxCheckMode())
            return;

        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return;

        // build the GPIB command
        String cmd = String.format(Locale.US, "CLIMIT %d,%.3f,%.1f,%.1f,%d,%d", Loop,
                SetPointLimit, PositiveSlope, NegativeSlope, MaxCurrent, MaxHeaterRange);

        // send via GPIB
        SendToInstrument(cmd);
    }//</editor-fold>
}