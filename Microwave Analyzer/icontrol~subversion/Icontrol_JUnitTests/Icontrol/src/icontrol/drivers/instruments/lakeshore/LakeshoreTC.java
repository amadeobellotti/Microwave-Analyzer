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
import static icontrol.Utilities.getInteger;
import static icontrol.Utilities.getFloat;
import icontrol.drivers.Device;
import icontrol.iC_Annotation;
import icontrol.iC_ChartXY;
import icontrol.iC_ChartXY.SeriesIdentification;
import icontrol.drivers.Device.CommPorts;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;
import javax.script.ScriptException;
import javax.swing.JOptionPane;
import org.jfree.chart.axis.NumberAxis;


/**
 * Generic Temperature Controller class for Lakeshore temperature controllers
 * based on a Lakeshore 331 model.<p>
 *
 * Usage: Derived classes should override the default constructor to initialize
 * the Instrument specific parameters, such as the maximum number of Heater Ranges
 * appropriately.<p>
 *
 * Unfortunately, there seems to be no way the Annotations can contain variables,
 * hence, if a derived class is not happy with the provided annotations, the
 * method in question can be overridden and supplied with a proper annotation,
 * and by calling the super class' method save to duplicate the method body.<p>
 * 
 * Note: Tests showed that the set point temperature is not changed when the command is 
 * sent directly after changing the ramp rate on a Lakeshore 332. (see Change Log
 * 110919 for details). No error was generated. This was confirmed by sending 
 * RAMP 2,0,10.000\n\rSETP 2, 70 using NI Measurement and Automation Explorer. 
 * Adding a 100ms delay after setting the ramp rate helped. Some other commands
 * also wait for 100 ms before exiting.
 *
 * All device commands that the generic Lakeshore temperature controller
 * understands are implemented here.<p>
 *
 * <h3>Instrument Commands:</h3>
 * <ul>
 *  <li>{@link #configDefaults(String, int) }
 *  <li>{@link #configInputCurve(java.lang.String, int) }
 *  <li>{@link #enableHeaterOuput(boolean) }
 *  <li>{@link #getTemp(String) }
 *  <li>{@link #monitorTemp(String, float, String) }
 *  <li>{@link #setHeaterRange(int) }
 *  <li>{@link #setRampRate(int, boolean, double) }
 *  <li>{@link #setTemp(int, float, boolean, float, float) }
 * </ul>
 *
 * @author KPP (Kurt Pernstich: pernstich@alumni.ethz.ch)
 * @version 0.3
 *
 */


// promise that this class supports GPIB communication
@iC_Annotation(CommPorts=CommPorts.GPIB)
public class LakeshoreTC extends Device {

    ///////////////////
    // member variables

    /** Stores the default Input Channel. Initialized with a non-valid String to
     * indicate that the default Input Channel has yet to be assigned. */
    protected String m_DefaultInputChannel = "not assigned";


    /** Stores the default loop#. Initialized with -1 to indicate that the
     * default channel has not yet been assigned. */
    protected int m_DefaultLoop = -1;


    /**
     * Defines the available loop numbers for this temperature controller.
     * The list must contain the loop number 0 used to address the default loop.
     * If a derived class supports an Instrument with more Loops, this variable
     * should be updated in the derived class' constructor.
     */
    protected HashSet<Integer> m_AvailableLoops;


    /** Defines the available input channels for this temperature controller.
     * The list must contain the input channel 'default' to address the default
     * input channel. Note that the channel designation specified in
     * <code>m_AvailableInputChannels</code> must be in upper
     * case letters; this does not influence the case insensitivity in the Script.
     * If a derived class supports an Instrument with more Input Channels, this 
     * variable should be updated in the derived class' constructor (see 
     * Lakeshore340 for an example.
     */
    protected HashSet<String> m_AvailableInputChannels;


    /** Defines the String used in <code>String.format</code> to append certain
     * values (for instance the temperature) to be sent via GPIB bus. If derived
     * classes are not allowed to change this constant, the 'final' restriction
     * might be lifted. */
    protected final String PRECISION = "%.3f";

    /** Defines the largest Input-Curve-Number */
    protected int m_MaxInputCurveNumber;
    
    /** Defines the maximum heater range for this temperature controller */
    protected int m_MaxHeaterRange;

    /** Store last Heater Range if Heater is switched off with <code>setHeaterOutput</code> */
    private int m_LastHeaterRange = 1;

    /**
     * The Logger for this class. Note that the Logger Level is set in the
     * constructor of the <code>Device</code> class.
     */
    private static final Logger m_Logger = Logger.getLogger("iC.Instruments.LakeshoreTC");
    
    /** Default wait time after setting certain parameters in the Lakeshore */
    private int m_DefaultWaitTime;


    /**
     * Default constructor. Initializes the parameter range for this temperature 
     * controller, such as maximum heater range, maximum input curve number, etc.<p>
     * 
     * Derived classes should copy this constructor and initialize the variables
     * according to the Instrument's capabilities. They also need to invoke this
     * constructor (for creation of the HashSets).<p>
     *
     * Note: If this constructor is updated, the constructor of derived classes
     * should also be updated!
     */
    // <editor-fold defaultstate="collapsed" desc="Constructor">
    public LakeshoreTC() {

        // define available Loop Numbers
        m_AvailableLoops = new HashSet<Integer>( Arrays.asList(0, 1, 2) );

        // define available Input Channels
        m_AvailableInputChannels = new HashSet<String>( Arrays.asList("A", "B", "DEFAULT"));

        // define the largest available Input Curve Number
        m_MaxInputCurveNumber = 41;

        // define the largest available Heater Range
        m_MaxHeaterRange = 3;
        
        // retrieve default wait time
        m_DefaultWaitTime = m_iC_Properties.getInt("LakeshoreTC.DefaultWaitTime", 100);
    }//</editor-fold>


    /**
     * Performs a Syntax-Check for the correct Control Loop number and assigns
     * the chosen default Control Loop if necessary.
     *
     * @param Loop The Loop number to check
     * @return The corrected Loop number; might be the same as Loop or it is the
     * default Loop number. In any case, it is guaranteed a valid Loop number.
     * @throws DataFormatException If Syntax-Check failed.
     */
    // <editor-fold defaultstate="collapsed" desc="check Loop">
    protected int checkLoop(int Loop)
              throws DataFormatException {

        // Syntax Check for correct loop number
        if ( !m_AvailableLoops.contains(Loop) ) {
            String str = "The Control Loop number must have a value from "
                    + m_AvailableLoops.toString() + ".\n";
            throw new DataFormatException(str);
        }

        // assign default loop and Syntax-Check
        if ( Loop == 0 ) {

            // assign default loop if selected
            Loop = m_DefaultLoop;

            // check if a valid default loop was selected in configDefaults
            if (Loop == -1) {
                String str = "You have addressed the default Control Loop\n"
                        + "without having assigned a valid default Loop.\n";
                str += "Please use 'configDefaults' first.\n";
                throw new DataFormatException(str);
            }
        }

        // return the correct Loop number
        return Loop;
    }//</editor-fold>


    /**
     * Performs a Syntax-Check for the correct Control Loop number and assigns
     * the chosen default Control Loop if necessary.
     *
     * @param InputChannel The Input Channel to check
     * @return The corrected Input Channel; might be the same as InputChannel or
     * it is the default Input Channel. In any case, it is guaranteed a valid Input Channel.
     * @throws DataFormatException If Syntax-Check failed.
     */
    // <editor-fold defaultstate="collapsed" desc="check Input Channel">
    protected String checkInputChannel(String InputChannel)
              throws DataFormatException {

        // Syntax Check for correct input channel
        if ( !m_AvailableInputChannels.contains(InputChannel.toUpperCase()) ) {
            String str = "The selected Input Channel is not valid.";
            str += "Please select a value from " + m_AvailableInputChannels.toString() + ".\n";
            throw new DataFormatException(str);
        }

        // assign default loop and Syntax-Check
        if ( InputChannel.equalsIgnoreCase("default") ) {

            // assign default loop if selected
            InputChannel = m_DefaultInputChannel;

            // check if a valid default loop was selected in configDefaults
            if ( !m_AvailableInputChannels.contains(InputChannel.toUpperCase()) ) {
                String str = "You have addressed the default Input Channel\n"
                        + "without having assigned a valid default Input Channel.\n";
                str += "Please use 'configDefaults' first.\n";
                throw new DataFormatException(str);
            }
        }

        // return the correct Input Channel
        return InputChannel;
    }//</editor-fold>

    /**
     * Performs a Syntax-Check for the correct Heater Range.
     *
     * @param Range The Heater Range to check
     * @throws DataFormatException If Syntax-Check failed.
     */
    // <editor-fold defaultstate="collapsed" desc="check Heater Range">
    protected void checkHeaterRange(int Range)
              throws DataFormatException {

        // Syntax Check for correct Heater Range
        if ( Range < 0 || Range > m_MaxHeaterRange ) {
            String str = "The Heater Range is not valid.";
            str += "Please select a Heater Range between 0 and " + m_MaxHeaterRange + ".\n";
            throw new DataFormatException(str);
        }
    }//</editor-fold>


    /**
     * Assigns the default InputChannel and the default Control Loop number.
     * This method has be called before the default input/loop can be used in
     * other methods, such as <code>setTemp</code>.
     *
     * @param InputChannel The Input Channel to be used as the default Input Channel.
     * @param Loop The Loop number to be used as the default loop#.
     * @throws DataFormatException When Syntax-Check fails
     */
    // <editor-fold defaultstate="collapsed" desc="config Defaults">
    @AutoGUIAnnotation(
        DescriptionForUser = "Configure the default Input Channel and the default Control Loop.",
        ParameterNames = {"Input Channel {default, A, B, (C, ...)}", "Loop# {0, 1, 2, (3, ...)}"},
        DefaultValues= {"A", "1"},
        ToolTips = {"<html>'default': addresses the Input Channel previously defined in 'configDefaults'<br>"
                    + "'A', 'B', ...: the Input Channel as displayed on the Instrument<br>"
                    + "The number of available Input Channels depends on the Instrument.</html>",
                    "<html>'0': addresses the Control Loop previously defined in 'configDefaults'<br>"
                    + "'1', '2', ...: the Control Loop as displayed on the Instrument<br>"
                    + "The number of available Loops depends on the Instrument.</html>"})
    @iC_Annotation(  MethodChecksSyntax = true )
    public void configDefaults(String InputChannel, int Loop)
           throws DataFormatException {

        ///////////////
        // Syntax-Check

        // Syntax-check InputChannel
        if ( !m_AvailableInputChannels.contains(InputChannel) ) {
            String str = "The given Input Channel (\"" + InputChannel + "\") is incorrect.\n";
            str += "Please select a Channel from: " + m_AvailableInputChannels.toString() + "\n";

            throw new DataFormatException(str);
        }

        // Syntax-check Loop
        if ( !m_AvailableLoops.contains(Loop) ) {
            String str = "The given Control Loop (\"" + Integer.toString(Loop) + "\") is incorrect.\n";
            str += "Please select a Loop from: " + m_AvailableLoops.toString() + "\n";

            throw new DataFormatException(str);
        }


        // Syntax check sucsessful, so remember the choice
        // this is done even when in NoCommunicationMode
        m_DefaultInputChannel = InputChannel;
        m_DefaultLoop = Loop;

    }//</editor-fold>


    
    
    /**
     * Sets the SetPoint temperature in units of the setpoint. If <code>Wait</code><p>
     * is set to false, this method returns immediately, if set to true, the
     * temperature needs to be within <code>Tolerance</code> for the duration
     * <code>Time</code> before the method returns.<p>
     *
     * This method performs a (crude) Syntax Check.<p>
     *
     * @param Loop The number of the loop to change the setpoint. Allowed values
     * are defined in <code>m_AvailableLoops</code>.
     * @param Temperature The new SetPoint temperature in units of the SetPoint.
     * @param Wait If set to false the method returns immediately, if set to 
     * true, the temperature must be stable before the method returns
     * @param Tolerance Specify the maximum allowed temperature variation to 
     * qualify for a stable temperature
     * @param Time The time in seconds the temperature must be within the
     * tolerance in order for the temperature to be stable
     *
     * @throws DataFormatException when the Syntax check fails
     * @throws IOException re-thrown from <code>SendToInstrument</code>
     */
    // <editor-fold defaultstate="collapsed" desc="SetTemp">
    @AutoGUIAnnotation(
        DescriptionForUser = "Sets the SetPoint temperature.",
        ParameterNames = {"Loop# {0,1,2,(3,...)}", "Temperature [unit of SetPoint]", "Wait until SP reached?",
                          "Temperature Tolerance [unit of SP]", "Stabilization Time [s]"},
        DefaultValues= {"0", "295", "false", "0.1", "10"},
        ToolTips = {"<html>'0': addresses the Control Loop previously defined in 'configDefaults'<br>"
                    + "'1', '2': the Control Loop as displayed on the Instrument<br>"
                    + "The number of available Loops depends on the Instrument.</html>",
                "The unit is the same as for the SetPoint (as displayed on the Temperature Controller).",
                "When selected, this method waits until the temperature is within a small tolerance band of the set point.",
                "The allowed temperature variation for the command to continue",
                "The time during which the temperature toleracne is checked before continuing processing the script"})
    @iC_Annotation(MethodChecksSyntax = true )
    public void setTemp(int Loop, float Temperature, boolean Wait, float Tolerance, float Time)
                throws IOException, DataFormatException, ScriptException {

        // local variables
        float T;


        ///////////////
        // Syntax-Check

        // Syntax Check for correct loop number
        Loop = checkLoop(Loop);
        
        // return if in Syntax-check mode
        if (inSyntaxCheckMode())
            return;

        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return;

        // build the GPIB command
        String cmdstr = "SETP %d, " + PRECISION;
        //String str = String.format(Locale.US, "SETP %d, %.3f", Loop, Temperature);
        String str = String.format(Locale.US, cmdstr, Loop, Temperature);

        // send the command
        SendToInstrument(str);

        // wait a bit
        DefaultWait();

        // wait until setpoint is reached
        if (Wait) {
            long TimeDifference = 0;
            long StatusBarTimeDiff = 0;

            // get current Status Bar String (need to display it again)
            String StatusBarText = m_GUI.DisplayStatusLine("", false);
            m_GUI.DisplayStatusLine(StatusBarText + " - T not yet reached", false);

            // get current time
            long StartTime = System.currentTimeMillis();

            do {
                // if scripting is paused, wait wait until un-paused
                while( isPaused(false) && m_StopScripting == false) {
                    // wait a bit
                    try { Thread.sleep(300); } catch (InterruptedException ex) {/* ignore */}

                    // reset StartTime if scripting was paused
                    StartTime = System.currentTimeMillis();
                }

                // wait a bit
                try { Thread.sleep(250); } catch (InterruptedException ex) {}

                // get current temperature
                str = QueryInstrument("KRDG? A");

                // convert to a float value
                T = getFloat(str);

                // reset start time if outside Tolerance
                if (Math.abs(T - Temperature) > Tolerance) {
                    // re-set start time to current time
                    StartTime = System.currentTimeMillis();

                    // log event
                    //m_Logger.log(Level.FINEST, "Resetting start time in SetTemp with wait: SP= {0}\tCurrent T= {1}\t old TimeDiff={2}\n", new Object[]{Temperature, T, TimeDifference});
                }

                // calc time since start respectively since Tolerance was ast met
                TimeDifference = System.currentTimeMillis() - StartTime;

                // update StatusBar text every 5 sec
                if (Math.abs(TimeDifference - StatusBarTimeDiff) > 5000) {
                    // remember new Time Difference
                    StatusBarTimeDiff = TimeDifference;

                    // update Status Bar Text
                    m_GUI.DisplayStatusLine(StatusBarText + " - ETA " 
                            + Math.round(Time-TimeDifference/1000) + " sec", false);
                }

            } while ( TimeDifference < Time * 1000 &&
                      m_StopScripting == false);
        }
    }//</editor-fold>
    
    
    /**
     * Sets the SetPoint to the current temperature by temporarily disabling
     * ramping. The previous Ramp settings are restored..<p>
     *
     * This method performs a (crude) Syntax Check.<p>
     *
     * @param InputChannel for which the temperature should be read (defined in
     * <code>m_AvailableInputchannels</code>)
     * 
     * @param Loop The number of the loop to change the setpoint. Allowed values
     * are defined in <code>m_AvailableLoops</code>.
     *
     * @throws DataFormatException when the Syntax check fails
     * @throws IOException re-thrown from <code>SendToInstrument</code>
     */
    // <editor-fold defaultstate="collapsed" desc="autoSetPoint">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Disables Ramping, set the Set Point to the current Temperature,<br>and re-sets Ramping to the previous setting.</html>",
        ParameterNames = {"Input Channel {default, A, B, (C, ...)}", "Loop# {0,1,2,(3,...)}"},
        DefaultValues= {"default", "0"},
        ToolTips = {"<html>'default': addresses the input channel previously defined in 'configDefaults'<br>"
                    + "'A', 'B', ...: the Input Channels as displayed on the Instrument<br>"
                    + "The number of available Input Channels depends on the Instrument.</html>",
                    "<html>'0': addresses the Control Loop previously defined in 'configDefaults'<br>"
                    + "'1', '2': the Control Loop as displayed on the Instrument<br>"
                    + "The number of available Loops depends on the Instrument.</html>"})
    @iC_Annotation(MethodChecksSyntax = true )
    public void autoSetPoint(String InputChannel, int Loop)
                throws IOException, DataFormatException, ScriptException {

        // local variables
        float T;

        ///////////////
        // Syntax-Check

        // Syntax Check for correct input channel
        InputChannel = checkInputChannel(InputChannel);
        
        // Syntax Check for correct loop number
        Loop = checkLoop(Loop);
        
        // return if in Syntax-check mode
        if (inSyntaxCheckMode())
            return;

        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return;

        // query the Ramp parameters
        String RampParams = QueryInstrument("RAMP? " + Loop);

        // disable Ramping
        setRampRate(Loop, false, 0);

        // get current Temperature
        T = getTemp(m_DefaultInputChannel);

        // set new SetPoint
        setTemp(Loop, T, false, 1, 1);

        // restore previous Ramp parameters
        SendToInstrument("RAMP " + Loop + "," + RampParams);

        // wait a bit
        DefaultWait();

    }//</editor-fold>


    /**
     * Set the Input-Curve-Number for the specified Input Channel (see
     * manual page 9-33).<p>
     *
     * This method performs a SyntaxCheck.
     *
     * @param InputChannel Selects the input channel for the loop; can be 'A' or 'B'.
     * @param CurveNumber Selects the Input Curve Number. Must be within [0...60].
     * 
     * @throws DataFormatException when the Syntax check fails
     * @throws IOException when GPIB communication fails (bubbles up from
     * <code>SendToInstrument</code>.
     */
    // <editor-fold defaultstate="collapsed" desc="configInputCurve">
    @AutoGUIAnnotation(
        DescriptionForUser = "Configure Input Curve Number.",
        ParameterNames = {"Input Channel {A or B}", "Input Curve Number { [0, 41] }" },
        DefaultValues = {"A", "21"},
        ToolTips = {"", ""})
    @iC_Annotation(MethodChecksSyntax=true)
    public void configInputCurve(String InputChannel, int CurveNumber)
            throws DataFormatException, IOException {

        ///////////////
        // Syntax check

        // Syntax Check for correct Input channel
        InputChannel = checkInputChannel(InputChannel);


        // Syntax-Check for Curve Number
        if (CurveNumber < 0 || CurveNumber > m_MaxInputCurveNumber) {

            // the selected Curve Number is incorrect
            String str = "The given Curve Number is incorrect.\n";
            str += "Please choose a number between 0 and "
                    + m_MaxInputCurveNumber + ".\n";

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
        //String str = "INCRV" + InputChannel;
        String str = String.format(Locale.US, "INCRV %s,%d", InputChannel, CurveNumber);

        // send the command
        SendToInstrument(str);

        // wait a bit
        DefaultWait();
    }// </editor-fold>



    /**
     * Returns the temperature in Kelvin from the specified Input Channel (A, B, ...)
     * or the default Input Channel defined in <code>configDefaults</code>.
     * This method performs a Syntax-Check. This method also checks if the retrieved
     * temperature reading is valid by querying RDGST? (see page 9-41) and returns
     * <code>Float.NaN</code> for invalid readings. Note that a bug in current
     * Lakeshore's firmware, the status is reported as okay even if no input
     * curve was selected.
     *
     * @param InputChannel for which the temperature should be read (defined in
     * <code>m_AvailableInputchannels</code>)
     *
     * @return The Temperature in Kelvin from the chosen Input Channel, or 
     * -1.1+0.1*random() if in No-Communication-Mode. Tho return a (unphysical)
     * temperature is helpful to test the rest of the program. If the instrument
     * returned an invalid reading, the returned value is set to <code>Float.NaN</code>
     *
     * @throws IOException thrown by <code>QueryInstrument</code>
     *
     * @throws ScriptException thrown by <code>getFloat</code> if the Instrument's
     * response could not be converted into a <code>float</code>
     *
     * @throws DataFormatException if Syntax-Check failed
     */
    // <editor-fold defaultstate="collapsed" desc="Read Temperature">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Reads the Temperature of the given input channel.</html>",
        ParameterNames = {"Input Channel {default, A, B, (C, ...)}"},
        ToolTips = {"<html>'default': addresses the input channel previously defined in 'configDefaults'<br>"
                + "'A', 'B', ...: the Input Channels as displayed on the Instrument<br>"
                + "The number of available Input Channels depends on the Instrument.</html>"},
        DefaultValues = {"default"})
    @iC_Annotation(MethodChecksSyntax=true)
    public float getTemp(String InputChannel)
            throws IOException, ScriptException, DataFormatException {

        // local variables
        Float ret = 0.0f;

        ///////////////
        // Syntax-Check

        // Syntax Check for correct input channel
        InputChannel = checkInputChannel(InputChannel);

        // return from Syntax-Check mode
        if (inSyntaxCheckMode())
            return -1.2f;

        
        // it is convenient to return a (unphysical) number in No-Communication-Mode
        // to test the rest of the program, so return -1.1 K if in No-Communication-Mode
        if ( inNoCommunicationMode() )
            return -1.1f + (0.2f * (float)Math.random() - 0.1f);

        // query Kelvin Reading of selected channel and store the answer
        String ans = QueryInstrument("KRDG? " + InputChannel);

        // convert
        ret = getFloat(ans);

        // get status of the input channel
        String Status = QueryInstrument("RDGST? " + InputChannel);

        // get input curve number
        // due to a bug in the Lakeshore which reports an okay status of
        // the Kelvin reading even if no Input Curve was selected, checking
        // of the input curve is necessary.
        String InputCurve = QueryInstrument("INCRV? " + InputChannel);

        // check if Kelvin reading is valid
        // Lakeshore bug: If no curve was selected, the reported status is okay...
        if ( getInteger(Status) > 0 ||
             getInteger(InputCurve)  <= 0) {
            // invalid answer, so log event
            m_Logger.log(Level.FINEST, "Kelvin reading of {0} channel {1} is invalid ({2})\n", 
                    new Object[]{m_InstrumentName, InputChannel, ans});

            // assign invalid result
            ret = Float.NaN;
        }

	return ret;
    }//</editor-fold>



    /**
     * Set the Ramp rate and allows to enable/disable ramping. This method
     * performs a SyntaxCheck.
     *
     * @param Loop The number of the loop to change the ramp rate. Can be 1 or 2.
     * @param OnOff When <code>true</code>, ramping is enabled.
     * @param RampRate The ramp rate in K/minutes
     *
     * @throws DataFormatException when the Syntax check fails
     * @throws IOException when GPIB communication fails (bubbles up from
     * <code>SendToInstrument</code>.
     */
    // <editor-fold defaultstate="collapsed" desc="setRampRate">
    @AutoGUIAnnotation(
        DescriptionForUser = "Sets the ramp rate and enables/disables ramping.",
        ParameterNames = {"Loop# {0, 1, 2, (3, ...)}", "On/Off", "Ramp Rate [K/min]"},
        DefaultValues = {"0", "true", "10"},
        ToolTips = {"<html>'0': addresses the Control Loop previously defined in 'configDefaults'<br>"
                    + "'1', '2', ...: the Control Loop as displayed on the Instrument<br>"
                    + "The number of available Loops depends on the Instrument.</html>",
                    "", ""})
    @iC_Annotation(MethodChecksSyntax=true)
    public void setRampRate (int Loop, boolean OnOff, double RampRate)
            throws DataFormatException, IOException {

        // Syntax Check for correct loop number
        Loop = checkLoop(Loop);

        // return if in Syntax-check mode
        if (inSyntaxCheckMode())
            return;

        // build the GPIB command
        String str = String.format(Locale.US, "RAMP %d,", Loop);

        // add On/Off
        str += OnOff ? "1," : "0,";

        // add ramp rate
        str += String.format(Locale.US, PRECISION, RampRate);

        // send the command
        SendToInstrument(str);

        // wait a bit
        DefaultWait();

    }//</editor-fold>


    /**
     * Enables or disables the Heater output. Before the Heater output is disabled,
     * the present Heater Range is stored. When the Heater output is enabled again,
     * the previously stored Range is used. If this range was 0 (off), the lowest
     * range (1) is used.
     *
     * @param HeaterState Choose to enable or disable the Heater output.
     *
     * @throws IOException re-thrown from <code>QueryInstrument</code> or
     * <code>SendToInstrument</code>.
     *
     * @throws ScriptException when <code>getInteger</code> fails because the
     * response of the Lakeshore TC to RANGE? could not be converted to an integer.
     */
    // <editor-fold defaultstate="collapsed" desc="enableHeaterOutput">
    @AutoGUIAnnotation(
        DescriptionForUser = "Enables/Disables the Heater output.",
        ParameterNames = {"Heater on/off"},
        DefaultValues = {"true"},
        ToolTips = {"<html>If enabled, the Heater Range is set to the lowest range,<br>"
                + "respectively the range used when it was disabled with this command.</html>"})
    public void enableHeaterOuput(boolean HeaterState)
                throws IOException, ScriptException {

        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return;

        if (HeaterState == true) {
            // switch heater on:
            try {
                // set Heater range to previous or lowest value
                setHeaterRange(m_LastHeaterRange);
            } catch (DataFormatException ex) {
                // ignored, because the passed values will always be
                // allowed (either 1 or what was returned from RANGE?
            }

        } else {
            // switch heater off:

            // query the present heater range
            m_LastHeaterRange = getInteger( QueryInstrument("RANGE?") );

            // check if Heater was already switched off
            if (m_LastHeaterRange == 0) {
                // set to lowest Heater Range
                m_LastHeaterRange = 1;
            }

            // set Heater range to 0 to switch it off
            try {
                setHeaterRange(0);
            } catch (DataFormatException ex) {/*ignored, because 0 is allowed*/}
        }
    }//</editor-fold>


    /**
     * Sets the Heater Range (see Lakeshore 331 manual p. 6-37 or for model 340
     * see chapter 6.12.1).<p>
     *
     * This method performs a Syntax-Check.
     *
     * @param Range The Heater range.
     *
     * @throws DataFormatException when the Syntax Check failed.
     * @throws IOException re-thrown from <code>SendToInstrument</code>
     */
    // <editor-fold defaultstate="collapsed" desc="set Heater Range">
    @AutoGUIAnnotation(
        DescriptionForUser = "Set the Heater Range.",
        ParameterNames = {"Range { 0,1,2,... }"},
        DefaultValues = {"1"},
        ToolTips = {"0 turns off the Heater."})
    @iC_Annotation(  MethodChecksSyntax = true )
    public void setHeaterRange(int Range)
                throws IOException, DataFormatException {

        ///////////////
        // Syntax-Check

        // Syntax Check for correct Heater Range
        checkHeaterRange(Range);
        

        // return if in Syntax-check mode
        if (inSyntaxCheckMode())
            return;

        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return;

        // build the GPIB command
        String cmd = String.format(Locale.US, "RANGE %d", Range);

        // send via GPIB
        SendToInstrument(cmd);

    }//</editor-fold>


    /**
     * Starts a new thread that reads the temperature of the given input channel
     * at the given time interval and stores it in a file together with the
     * number of seconds elapsed since processing of the script started.<p>
     *
     * This methods performs a crude syntax check, hence it is called during
     * Syntax-Check mode, and needs to return without IO communication if it
     * was successful.
     *
     * @param InputChannel The temperature of this input channel is monitored;
     * can be "A" or "B".
     *
     * @param TimeIntervall The temperature is monitored every TimeInterval-seconds.
     *
     * @param FileExtension is appended to the FileName used to store the monitored
     * temperatures. If it contains no '.' a '.' is automatically inserted. If the
     * filename already exists, a new file with a counter is inserted between the 
     * FileName and the FileExtension.
     *
     * @throws DataFormatException when the Syntax Check failed.
     */
    // <editor-fold defaultstate="collapsed" desc="monitorTemp">
    @iC_Annotation(MethodChecksSyntax=true)
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Monitors the temperature at the given intervall<br>Stores the temperatures in a file with the given extension.</html>",
        ParameterNames = {"Input channel {A or B}", "Time Intervall [sec]", "File Extension"},
        DefaultValues = {"A", "1"},
        ToolTips = {"", "Time between two measurements.", "If the FileExtension does not contain a '.' a '.' is automatically added."})
    public void monitorTemp(String InputChannel, float TimeIntervall, String FileExtension)
            throws DataFormatException {

        /**
         * Implement a new class from an anonymous inner class that extends
         * Thread and implements run(), in which the actual job is done.<p>
         *
         * The thread pauses, respectively, stops when <code>m_Paused</code>, respectively,
         * <code>m_StopScripting</code> is true.
         */
        // <editor-fold defaultstate="collapsed" desc="myThread as anonymous inner class">
        class myThread extends Thread {
            public String       m_InputChannel;

            /** The file with the monitored temperature as text */
            public BufferedWriter   m_FileWriter;

            /** The file with the monitored temperature as png */
            public File         m_FileForChart;

            /** The time between two temperature measurements in sec */
            public float        m_TimeInterval;


            // Identification of the Data Series
            private SeriesIdentification    m_SeriesID;

            // holds the chart
            private iC_ChartXY  m_Chart;


            /**
             * Default constructor. Prepares the XY chart
             */
            public myThread() {

                // make a new XYChart object
                m_Chart = new iC_ChartXY("Monitoring '" + m_InstrumentName + "'",
                                         "Time since Start [ sec ]",
                                         "Temperature [ K ]",
                                         false  /*legend*/,
                                         1024, 480);

                // add a new data series
                m_SeriesID = m_Chart.AddXYSeries("Measured", 0,    // default left axis
                                                 false, true,
                                                 m_Chart.LINE_SOLID,
                                                 m_Chart.MARKER_CIRCLE);

                // use no decimal places on the X axis (time)
                m_Chart.getXAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());
            }


            @Override
            public void run() {
                // local variables
                float Temp = 0.0f;

                // do until the thread should be stopped
                while ( m_StopScripting == false) {

                    // check for pause button
                    isPaused(true);

                    // get temperature
                    try {
                        Temp = getTemp(m_InputChannel);

                    } catch (Exception ex) {
                        // an error is (not anymore) expected when in No-Communication-Mode
                        // ignore it anyways. (ReadTemp now return -1.1 when in N-C-M)
                        if ( !inNoCommunicationMode() ) {
                            m_Logger.log(Level.SEVERE, "Lakeshore340#MonitorTemp: Could not read the temperature.", ex);

                            m_GUI.DisplayStatusMessage("Error reading temperature in MonitorTemp.\n");

                        } else {
                            m_GUI.DisplayStatusMessage("Monitoring temperature in No-Communication-Mode.\n");
                        }
                    }

                    // get elapsed time in seconds
                    double Time = (System.currentTimeMillis() - m_tic) / 1000.0;


                    // make the line to write in the file
                    // use Local.US to ensure a '.' is used as comma
                    // (in german a ',' is used as decimal point, which is sometimes
                    // not recognized as decimal point by some other programs like
                    // Origin, IgorPro, Matlab, Excel, ...
                    String line =
                            String.format(Locale.US, "%.3f\t%f\n", Time, Temp);

                    // write into the file
                    try {
                        m_FileWriter.write(line);

                    } catch (IOException ex) {
                        m_Logger.log(Level.SEVERE, "MonitorTemp.run(): could not write to FileWriter.", ex);

                        m_GUI.DisplayStatusMessage("Error writing to file in MonitorTemp.\n");
                    }

                    // add the datapoint to the graph
                    m_Chart.AddXYDataPoint(m_SeriesID, Time, Temp);

                    // wait the desired time
                    try {
                        Thread.sleep( Math.round(m_TimeInterval) );
                    } catch (InterruptedException ex) {}
                }

                // close the file
                try {
                    m_FileWriter.close();

                } catch (IOException ex) {
                    m_Logger.log(Level.SEVERE, "Lakeshore340#MonitorTemp: Could not close the file", ex);

                    m_GUI.DisplayStatusMessage("Could not close the file in MonitorTemp.\n");
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
                m_GUI.DisplayStatusMessage("Stop monitoring the temperature.\n");
            }
        }//</editor-fold>


        // Syntax Check for correct channel assignment
        checkInputChannel(InputChannel);
        
        // return if in Syntax-check mode
        if (inSyntaxCheckMode())
            return;

        // get the file name and add the extension
        String FileName = m_GUI.getFileName(FileExtension);


        // open the file for writing
        // and write the headerline
        BufferedWriter fw;
        try {
            fw = new BufferedWriter(new FileWriter(FileName));

            fw.write("Time [sec]\tTemperatur [K]\n");

        } catch (IOException ex) {
            m_Logger.log(Level.SEVERE, "LakeshoreTC.MonitorTemp(): new FileWriter", ex);

            // show a dialog
            String str = "Could not open the file\n";
            str += FileName + "\n\n";
            str += "Maybe Java has not write permission for that folder.\n";
            str += "Please try making this folder manually.\n\n";
            str += "MonitorTemp will not be executed.";

            JOptionPane.showMessageDialog(m_GUI.getTheFrame(),
                    str, "File open Error", JOptionPane.ERROR_MESSAGE, m_iC_Properties.getLogoSmall());

            return;
        }


        /////////////////////
        // prepare the thread

        // make a new thread object
        myThread myT = new myThread();

        // set the input channel in the thread object
        myT.m_InputChannel = InputChannel;

        // pass the FileWriter to the thread
        myT.m_FileWriter = fw;

        // pass the Time Interval in sec
        myT.m_TimeInterval = 1000 * TimeIntervall;

        // pass a new file for saving the graph as png
        myT.m_FileForChart = new File(FileName + ".png");



        // Display a status message
        m_GUI.DisplayStatusMessage("Now starting to monitor the temperature.\n");

        // start the thread and return
        myT.start();
    }//</editor-fold>

    /** 
     * Waits the default time period (100 ms) before it returns. It appears 
     * advisable to call this method each time some parameter is set on an
     * (older?) Lakeshore controller.<p>
     * From the Change Log 110919: Apparently the set point temperature is not 
     * changed when the command is sent directly after changing the ramp rate on
     * a Lakeshore 332. No error was generated. This was confirmed by sending 
     * RAMP 2,0,10.000\n\rSETP 2, 70 using NI Measurement and Automation Explorer.<p>
     * 
     * Note: When checking completion of CRVSAV in <code>Lakeshore340.UpdateSensorCurve</code>
     * I tried to use *ESE 1, *SRE 32, *OPC? and *STB? respectively *ESR?
     * to check for completion. Now I use BUSY? instead, but sending the *OPC
     * command might be useful in <code>DefaultWait</code>.
     */
    // <editor-fold defaultstate="collapsed" desc="Default Wait">
    private void DefaultWait() {
        // wait a bit
        // see remark to 110919 in the Change Log
        try {Thread.sleep(m_DefaultWaitTime);} catch (InterruptedException ex) {/* ignore */}
    }//</editor-fold>
}
