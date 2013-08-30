// TODO 5* calc Rshunt, Rseries
// TODO 3* add a skip button to SweepVmeasureI (could be in iC_Utilities if possible)
// TODO 6* add proper reference to LevenbergMarquardtOptimizer in javadoc if it works
// TODO 6* what can I do to ensure the PolynomialFitter does not stall?


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
package icontrol.drivers.instruments.keithley;

import icontrol.AutoGUIAnnotation;
import icontrol.IcontrolView;
import icontrol.Utilities;
import static icontrol.Utilities.getDouble;
import icontrol.drivers.Device;
import icontrol.iC_Annotation;
import icontrol.iC_ChartXY;
import icontrol.iC_ChartXY.SeriesIdentification;
import icontrol.drivers.Device.CommPorts;
import java.awt.Toolkit;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.ListIterator;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;
import javax.script.ScriptException;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.LoessInterpolator;
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.analysis.solvers.PegasusSolver;
import org.apache.commons.math3.optimization.fitting.PolynomialFitter;
import org.apache.commons.math3.optimization.general.GaussNewtonOptimizer;
import org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer;


/**
 * This class implements functionality to communicate with a Keithley 2600A System
 * SourceMeter. It was tested with a Keithley 2636 and should work for the entire
 * 2600 class instruments.<p>
 * 
 * It is recommended to call <code>checkErrorQueue</code> when implementing new
 * methods.<p>
 *
 * <h3>Instrument Commands:</h3>
 * <ul>
 *  <li>{@link #AutoZero(java.lang.String, java.lang.String) }
 *  <li>{@link #configDelayTime(String, double, double) }
 *  <li>{@link #configFilters(String, String, int, boolean, boolean) }
 *  <li>{@link #configOffLimit(String, String, float) }
 *  <li>{@link #configSMUChannel(String, String) }
 *  <li>{@link #MeasureI(String) }
 *  <li>{@link #MeasureOPV(String, double, double, double, boolean, String) }
 *  <li>{@link #OutputI(String, float) }
 *  <li>{@link #OutputState(String, String, double)  }
 *  <li>{@link #OutputV(String, double) }
 *  <li>{@link #SweepVmeasureI(String, double, double, double, boolean, String) }
 *  <li>{@link #SweepVmeasureIconfig(java.lang.String, int, int, int, int, boolean) }
 *  <li>The following commands are implemented as generic GPIB instruments:
 *  <li>configAnalogFilters
 *  <li>configNPLC
 * </ul>
 *
 * @author KPP (Kurt Pernstich: pernstich@alumni.ethz.ch)
 * @version 0.1
 *
 */

// promise that this class supports GPIB communication
@iC_Annotation(CommPorts=CommPorts.GPIB,
                InstrumentClassName="Keithley 2600")
public class Keithley2600 extends Device {


    ///////////////////
    // member variables

    /**
     * The Logger for this class. Note that the Logger Level is set in the
     * constructor of the <code>Device</code> class.
     */
    // change the Logger name to the name of your class
    private static final Logger m_Logger = Logger.getLogger("iC.Instruments.Keithley2600");


    /**
     * Private inner class that saves the configuration parameters for 
     * <code>SweepVmeasureI</code>. The parameters are set in <code>SweepVmeasureIconfig</code>.
     */
    // <editor-fold defaultstate="collapsed" desc="SVMIconfig struct">
    private class SVMIconfig {
        public String SMUChannel;
        public int HoldTime;
        public int DelayTime;
        public int NrMeasurements;
        public int NrAveraging;
        public boolean AutoStabilize;
    }//</editor-fold>
    
    
    /** Configuration parameters for channel A, B used in <code>SweepVmeasureI</code> */
    private SVMIconfig  m_SVMIconfig_A = null;
    private SVMIconfig  m_SVMIconfig_B = null;
    
    /** 
     * Handle to the chart that shows the I-V plots made in <code>SweepVMeasureI</code>
     * or <code>MeasureOPV</code>. It's a bit of a work around to define that variable
     * as a class member to be able to access the chart from both methods and
     * not having to duplicate code.
     */
    // TODO make private again if a solution was found for Unit Testing
    protected iC_ChartXY m_IV_Chart = null;

    /**
     * Overwritten <code>Device.Open</code> method that additional to what's
     * done in <code>Device.Open</code> clears the error queue.
     * @throws IOException see <code>Device.Open</code>
     * @throws ScriptException see <code>Device.Open</code>
     */
    // <editor-fold defaultstate="collapsed" desc="Open">
    @Override
    public void Open() throws IOException, ScriptException {
        super.Open();
                   
        // clear error queue
        SendToInstrument("errorqueue.clear()");

    }// </editor-fold>
    
    
    
    /**
     * Queries the oldest entry of the 2600's Error Queue. The error is removed from the
     * error queue. This method simply returns when no error was found in the
     * queue or throws a ScriptException with the Error Number(s) and Message(s) if
     * an error occurred. It also returns if in NoCommunicationMode.
     * 
     * @param Message This String is appended to the Error Message in case an
     * error occurred.
     *
     * @throws IOException when GPIB communication failed (bubbles up from 
     * <code>QueryInstrument</code>.
     * 
     * @throws ScriptException if 1) the returned number of errors in the queue
     * could not be converted into an Integer (should never happen unless GPIB 
     * transmission introduces an error), or 3) at least one error was found in 
     * the error queue.
     */
    // <editor-fold defaultstate="collapsed" desc="checkErrorQueue">
    private void checkErrorQueue(String Message) 
            throws ScriptException, IOException {

        // return without indicating an error when in NoCommunicatin Mode
        if (inNoCommunicationMode())
            return;
        
            
        // get the number of errors in the queue
        String ans = QueryInstrument("print(errorqueue.count)");

        // convert to a number
        double NrOfErrors = getDouble(ans);

        // return if no errors were found
        if (NrOfErrors == 0) {
            return;
        }

        // add a description to the Error Message
        Message += "The following errors were found in the Keithley 2600's error queue:\n";

        // iterate through all error messages in the queue
        for (int i=1; i<= NrOfErrors; i++) {

            // get error description
            SendToInstrument("errorCode, message, severity, errorNode = errorqueue.next()");


            // append error description to Error Message
            Message += "Error #" + i + ": "
                    + QueryInstrument("print(message)") 
                    + " (" + QueryInstrument("print(errorCode)") + ")\n";
        }

        // beep
        Toolkit.getDefaultToolkit().beep();

        // throw ScriptException with the Error Message
        throw new ScriptException(Message);        
        
    }//</editor-fold>
    

    /**
     * Configures the SMU Channel to act as voltage or current source. It also sets
     * the display to show the corresponding value (current or voltage).<p>
     *
     * This method performs a Syntax-Check.
     *
     * @param SMUChannel The SMU Channel, can be A, B (case insensitive)
     * @param Mode The operation mode, can be V, I (case insensitive)
     *
     * @throws IOException re-thrown from {@link Device#SendToInstrument}
     * @throws ScriptException When the 2600's error queue contained an error
     * @throws DataFormatException when the Syntax Check failed.
     */
    // <editor-fold defaultstate="collapsed" desc="configSMUChannel">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Select if the SMU Channel operates as current or voltage source</html>",
        ParameterNames = {"SMU Channel Name {A, B}", "Operation Mode {V, I}"},
        DefaultValues = {"A", "V"},
        ToolTips = {"", "<html>V ... Voltage source (force Voltage measure Current)<br>I ... Current source (force Current measure Voltage)</html>"})
    @iC_Annotation(  MethodChecksSyntax = true )
    public void configSMUChannel(String SMUChannel, String Mode)
           throws IOException, DataFormatException, ScriptException {

        // convert to lower case
        SMUChannel = SMUChannel.toLowerCase();
        

        ///////////////
        // Syntax-Check

        // check channel 
        checkChannelName(SMUChannel);
        
        
        // translate Mode
        String ModeCmd;
        if (Mode.equalsIgnoreCase("V")) {
            ModeCmd = String.format(Locale.US, "smu%s.OUTPUT_DCVOLTS", SMUChannel);
        } else if (Mode.equalsIgnoreCase("I")) {
            ModeCmd = String.format(Locale.US, "smu%s.OUTPUT_DCAMPS", SMUChannel);
        } else {
            String str = "The Mode '" + Mode + "' is not valid.\n";
            str += "Please select either 'V' or 'I'.\n";
            throw new DataFormatException(str);
        }
              

        // return if in Syntax-check mode
        if (inSyntaxCheckMode())
            return;

        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return;



        // build the GPIB command
        String cmd = String.format(Locale.US, "smu%s.source.func = %s", 
                                                SMUChannel, ModeCmd );

        // check the error queue
        checkErrorQueue("The previous command to configSMUChannel caused an error.\n");

        // send the command
        SendToInstrument(cmd);

        // check the error queue
        checkErrorQueue("configSMUChannel caused an error.\n");


        // build sommand to set display function
        if (Mode.equalsIgnoreCase("V")) {
            cmd = String.format(Locale.US, 
                    "display.smu%s.measure.func=display.MEASURE_DCAMPS", SMUChannel);
        } else {
            cmd = String.format(Locale.US, 
                    "display.smu%s.measure.func=display.MEASURE_DCVOLTS", SMUChannel);
        }

        // set display function
        SendToInstrument(cmd);

        // check the error queue
        checkErrorQueue("configSMUChannel could not set the Display function.\n");

    }//</editor-fold>
    
    
    /**
     * Configures the Settling Delay Time and the Delay factor before taking a
     * measurement.<p>
     *
     * This method performs a Syntax-Check.
     *
     * @param SMUChannel The SMU Channel, can be A, B (case insensitive)
     * @param DelayTime The Settling Delay Time in Seconds, or 0 to disable the
     * default delay times, or -1 to enable Automatic Delay Times
     * @param DelayFactor Multiplier to the Delay Time when DelayTime is set to 
     * Automatic (-1).
     *
     * @throws IOException re-thrown from {@link Device#SendToInstrument}
     * @throws ScriptException When the 2600's error queue contained an error
     * @throws DataFormatException when the Syntax Check failed.
     */
    // <editor-fold defaultstate="collapsed" desc="configDelayTime">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Select the Settling Delay Time</html>",
        ParameterNames = {"SMU Channel Name {A, B}", "Delay Time [sec]", "Delay Factor"},
        DefaultValues = {"A", "-1", "1.3"},
        ToolTips = {"", "<html>-1 ... Automatic delay value<br>0 ... No delay<br>>0 ... Delay Time in seconds</html>",
                    "Multiplier to the Delay Time when DelayTime is set to Automatic (-1)"})
    @iC_Annotation(  MethodChecksSyntax = true )
    public void configDelayTime(String SMUChannel, double DelayTime, double DelayFactor)
           throws IOException, DataFormatException, ScriptException {

        ///////////////
        // Syntax-Check

        // convert to lower case
        SMUChannel = SMUChannel.toLowerCase();

        // check channel 
        checkChannelName(SMUChannel);
        
        // check Delay Time
        if (DelayTime < 0 && DelayTime != -1) {
            String str = "The DelayTime must be >0 or -1.";
            throw new DataFormatException(str);
        }
        
        // check Delay Factor
        if (DelayFactor < 0) {
            String str = "The DelayFactor must be >0.";
            throw new DataFormatException(str);
        }
                     

        // return if in Syntax-check mode
        if (inSyntaxCheckMode())
            return;

        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return;


        // check the error queue
        checkErrorQueue("The previous command to configDelayTime caused an error.\n");


        // build the GPIB command for the Delay Time
        String cmd = String.format(Locale.US, "smu%s.measure.delay = %f", 
                                                SMUChannel, DelayTime );       

        // send the command
        SendToInstrument(cmd);

        // check the error queue
        checkErrorQueue("configuring the DelayTime caused an error.\n");


        // build the GPIB command for the Delay Factor
        cmd = String.format(Locale.US, "smu%s.measure.delayfactor = %f", 
                                                SMUChannel, DelayFactor );       

        // send the command
        SendToInstrument(cmd);

        // check the error queue
        checkErrorQueue("configuring the DelayFactor caused an error.\n");

    }//</editor-fold>
    
    
    
    /**
     * Configures the digital and analog Filters.<p>
     *
     * This method performs a Syntax-Check.
     *
     * @param SMUChannel The SMU Channel, can be A, B (case insensitive)
     * @param FilterType Selects the Moving Average, Repeat Average, or the
     * Median digital Filter
     * @param FilterCount The Number of measured readings required to yield one 
     * filtered measurement
     * @param DigitalFilterEnable Enables or disables the digital Filter
     * @param AnalogFilterEnable Controls the use of an analog filter when 
     * measuring on the lowest current ranges (Models 2635A/2636A only).
     *
     * @throws IOException re-thrown from <code>SendToInstrument</code>
     * @throws ScriptException When the 2600's error queue contained an error
     * @throws DataFormatException when the Syntax Check failed.
     */
    // <editor-fold defaultstate="collapsed" desc="configFilters">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Configures the digital and analog Filters</html>",
        ParameterNames = {"SMU Channel Name {A, B}", "Digitial Filter Type", "Filter Count",
                          "Digital Filter Enable", "Analog Filter Enable"},
        DefaultValues = {"A", "Repeat", "3", "true", "true"},
        ToolTips = {"", "<html>Moving ... Selects the Moving Average filter<br>Repeat ... Selects the Repeat filter<br>Median ... Selects the Median filter</html>",
                    "Number of measured readings required to yield one filtered measurement",
                    "Enables or disables digitally filtered measurements",
                    "Controls the use of an analog filter when measuring on the lowest current ranges (Models 2635A/2636A only)"})
    @iC_Annotation(  MethodChecksSyntax = true )
    public void configFilters(String SMUChannel, String FilterType, int FilterCount, 
                              boolean DigitalFilterEnable, boolean AnalogFilterEnable )
           throws IOException, DataFormatException, ScriptException {

        ///////////////
        // Syntax-Check

        // convert to lower case
        SMUChannel = SMUChannel.toLowerCase();

        // check channel 
        checkChannelName(SMUChannel);
        
             
        // translate and Syntax-check FilterType
        String FilterTypeCmd;
        if (FilterType.equalsIgnoreCase("Moving")) {
            FilterTypeCmd = String.format("smu%1$s.measure.filter.type = smu%1$s.FILTER_MOVING_AVG", SMUChannel);
        } else if (FilterType.equalsIgnoreCase("Repeat")) {
            FilterTypeCmd = String.format("smu%1$s.measure.filter.type = smu%1$s.FILTER_REPEAT_AVG", SMUChannel);
        } else if (FilterType.equalsIgnoreCase("Median")) {
            FilterTypeCmd = String.format("smu%1$s.measure.filter.type = smu%1$s.FILTER_MEDIAN", SMUChannel);
        } else {
            String str = "The Filter Type '" + FilterType + "' is not recognized.\n"
                    + "Please select Moving, Repeat, or Median.\n";
            throw new DataFormatException(str);
        }
        
        // check Filter Count
        if (FilterCount < 1 || FilterCount >100) {
            String str = "The Filter Count must be between 1 and 100.";
            throw new DataFormatException(str);
        }
        
        // translate Digital Filter Enable
        String DigitalFilterEnableCmd;
        if (DigitalFilterEnable) {
            DigitalFilterEnableCmd = String.format("smu%1$s.measure.filter.enable = smu%1$s.FILTER_ON", SMUChannel);
        } else {
            DigitalFilterEnableCmd = String.format("smu%1$s.measure.filter.enable = smu%1$s.FILTER_OFF", SMUChannel);
        }
        
        // translate Analog Filter Enable
        String AnalogFilterEnableCmd;
        if (DigitalFilterEnable) {
            AnalogFilterEnableCmd = String.format("smu%1$s.measure.analogfilter = 1", SMUChannel);
        } else {
            AnalogFilterEnableCmd = String.format("smu%1$s.measure.analogfilter = 0", SMUChannel);
        }
                     

        // return if in Syntax-check mode
        if (inSyntaxCheckMode())
            return;

        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return;


        // check the error queue
        checkErrorQueue("The previous command to configFilters caused an error.\n");           

        // send the command to set FilterType
        SendToInstrument(FilterTypeCmd);

        // check the error queue
        checkErrorQueue("configuring the Filter Type caused an error.\n");



        // build the GPIB command for the Filter Count
        String cmd = String.format(Locale.US, "smu%s.measure.filter.count = %d", 
                                                SMUChannel, FilterCount );       

        // send the command
        SendToInstrument(cmd);

        // check the error queue
        checkErrorQueue("configuring the Filter Count caused an error.\n");


        // send the command to set DigitalFilterEnable
        SendToInstrument(DigitalFilterEnableCmd);

        // check the error queue
        checkErrorQueue("configuring the Digital Filter Enable caused an error.\n");


        // send the command to set AnalogFilterEnable
        SendToInstrument(AnalogFilterEnableCmd);

        // check the error queue
        checkErrorQueue("configuring the Analog Filter Enable caused an error.\n");

    }//</editor-fold>
    
    
    /**
     * Configures the voltage or current limit when the SMU's output state is
     * set to off.<p>
     *
     * This method performs a Syntax-Check.
     *
     * @param SMUChannel The SMU Channel, can be A, B (case insensitive)
     * @param Function Selects which limit to set, can be V, I (case insensitive)
     * @param LimitValue The new value for the voltage/current limit
     *
     * @throws IOException re-thrown from {@link Device#SendToInstrument}
     * @throws ScriptException When the 2600's error queue contained an error
     * @throws DataFormatException when the Syntax Check failed.
     */
    // <editor-fold defaultstate="collapsed" desc="configOffLimit">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Configures the voltage/current limit when the output is switched off</html>",
        ParameterNames = {"SMU Channel Name {A, B}", "Which Limit? {V, I}", "Limit Value"},
        DefaultValues = {"A", "I", "100e-6"},
        ToolTips = {"", "<html>V ... set the Voltage limit<br>I ... set the Current limit</html>"})
    @iC_Annotation(  MethodChecksSyntax = true )
    public void configOffLimit(String SMUChannel, String Function, float LimitValue)
           throws IOException, DataFormatException, ScriptException {

        // convert to lower case
        SMUChannel = SMUChannel.toLowerCase();
        Function = Function.toLowerCase();
        

        ///////////////
        // Syntax-Check

        // check channel 
        checkChannelName(SMUChannel);

        
        // check Mode
        if ( !Function.equalsIgnoreCase("V") && !Function.equalsIgnoreCase("I") ) {
            String str = "The Mode '" + Function + "' is not valid.\n";
            str += "Please select either 'V' or 'I'.\n";
            
            throw new DataFormatException(str);
        }
        

        // return if in Syntax-check mode
        if (inSyntaxCheckMode())
            return;

        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return;


        // build the GPIB command
        String cmd = String.format(Locale.US, "smu%s.source.limit%s = %f", 
                                                SMUChannel, Function, LimitValue );

        // check the error queue
        checkErrorQueue("the previous command to configOffLimit caused an error.\n");

        // send the command
        SendToInstrument(cmd);

        // check the error queue
        checkErrorQueue("configOffLimit caused an error.\n");

    }//</editor-fold>
    
    
    
    /**
     * Sets the Output State of the SMU to either on, off, or high_Z. When on is
     * selected, the SMU's mode (force V or I) is determined by <code>configSMUChannel</code>,
     * when off is selected, the SMU is turned off and smuX.source.offmode determines
     * whether 0V or 0A are sourced, and additionally, the output voltage is set to
     * 0 using <code>OutputV</code>. High_Z turns off the output and opens the
     * output relay. This method performs a Syntax-Check. This method can optionally
     * wait until a threshold is met before returning.
     * 
     * @param SMUChannel The SMU Channel, can be A, B (case insensitive)
     * @param OutputState The Output State of the SMU, can be on, off, or highZ
     * (case insensitive)
     *
     * @throws IOException re-thrown from {@link Device#SendToInstrument}
     * @throws ScriptException if the returned answer from the Instrument could
     * not be interpreted as a Double value (bubbles up from <code>getDouble</code>.
     * @throws DataFormatException when the Syntax Check failed.
     */
    // <editor-fold defaultstate="collapsed" desc="OutputState">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Select Output State of the SMU</html>",
        ParameterNames = {"SMU Channel Name {A, B}", "Output State {on, off, highZ}"},
        DefaultValues = {"A", "off"},
        ToolTips = {"", "<html>When 'highZ' is selected, the SMU will open the output relay.</html>",
                    "<html>A number greater than zero causes this method to wait until the<br>present output is below this threshold before returning<br>when the output state off is selected.</html>"})
    @iC_Annotation(  MethodChecksSyntax = true )
    public void OutputState(String SMUChannel, String OutputState)
           throws IOException, DataFormatException, ScriptException {
        
        ///////////////
        // Syntax-Check
        
        // convert to lower case
        SMUChannel = SMUChannel.toLowerCase();
        

        // check channel 
        checkChannelName(SMUChannel);
        
        // translate OutputState
        String OutputStateCmd;
        if (OutputState.equalsIgnoreCase("off")) {
            OutputStateCmd = String.format("smu%s.OUTPUT_OFF", SMUChannel);
        } else if (OutputState.equalsIgnoreCase("on")) {
            OutputStateCmd = String.format("smu%s.OUTPUT_ON", SMUChannel);
        } else if (OutputState.equalsIgnoreCase("highZ")) {
            OutputStateCmd = String.format("smu%s.OUTPUT_HIGH_Z", SMUChannel);
        } else {
            String str = "The Output State '" + OutputState + "' is not valid.";
            str += "Please select either on, off, or highZ.\n";
            throw new DataFormatException(str);
        }
                

        // return if in Syntax-check mode
        if (inSyntaxCheckMode())
            return;

        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return;

          
        // build the GPIB command
        String cmd = String.format(Locale.US, "smu%s.source.output = %s", 
                                                SMUChannel, OutputStateCmd );

        // check the error queue
        checkErrorQueue("The previous command to OutputState caused an error.\n");

        // send the command
        SendToInstrument(cmd);

        // check the error queue
        checkErrorQueue("OutputState caused an error.\n");

        // is Output switched off?
        if (OutputState.equalsIgnoreCase("off")) {
            // yes, so set output voltage to 0
            // otherwise the user might enable the output when a high voltage is set
            OutputV(SMUChannel, 0);
        }

        // check if Waiting for threshold is desired
//        if (OutputState.equalsIgnoreCase("off") && WaitThreshold > 0) {
//
//            Double CurrentValue;
//
//            // TODO 9* test me
//            m_GUI.DisplayStatusMessage("Warning: Waiting for a Threshold in OutputState has not been tested.\n", false);
//
//            // build query string
//            cmd = String.format(Locale.US, "print(smu%s.source.func)", SMUChannel);
//
//            // get current SMU Function (voltage, current, resistance, power)
//            String SMUFunction = QueryInstrument(cmd);
//
//
//            // yes, so let's wait
//            do {
//
//                // check for Stop Button
//                if (m_StopScripting) {
//                    break;
//                }
//
//                // check for Pause Button
//                isPaused(true);
//
//                // build query string
//                cmd = String.format(Locale.US, "print(smu%s.measure.%s())", 
//                                                SMUChannel, SMUFunction);
//
//                // get current reading  (can be voltage, current, power, resistance)
//                String ans = QueryInstrument(cmd);
//
//                // TODO delme after testing
//                m_GUI.DisplayStatusMessage("develop: ans=" + ans + "\n");
//
//                // check the error queue
//                checkErrorQueue("OutputState caused an error.\n");
//
//                // convert to double
//                CurrentValue = getDouble(ans);
//
//            } while (Math.abs(CurrentValue) > Math.abs(WaitThreshold));
//        }
    }//</editor-fold>
    
    
    
    /**
     * Switches the Auto-Ranging for the SMU Output on or off, and sets the voltage
     * source range if Auto-Ranging is switched off.
     * 
     * @param SMUChannel The SMU Channel, can be A, B (case insensitive)
     * @param AutoRanging If <code>true</code> Auto-Ranging will be switched on
     * for the specified SMU output
     * @param VoltageRange Sets the SMU to a fixed range large enough to source 
     * the assigned value. The  instrument will select the best range for sourcing
     * this value.
     *
     * @throws IOException re-thrown from {@link Device#SendToInstrument}
     * @throws DataFormatException when the Syntax Check failed.
     * @throws ScriptException When the 2600's error queue contained an error.
     */
    // <editor-fold defaultstate="collapsed" desc="configVoltageSourceRange">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Switches the Auto-Ranging for the SMU Output on or off, and<br>"
            + "sets the Source Range if Auto-Ranging is switched off</html>",
        ParameterNames = {"SMU Channel Name {A, B}", "Auto-Ranging?", "Voltage Range"},
        DefaultValues = {"A", "true", "5"},
        ToolTips = {"", "<html>Switching off Auto-Ranging puts the SMU into a fixed source range<br>"
                + "as specified with Voltage Range.</html>",
                "Set this value to the maximum expected voltage to be sourced"})
    @iC_Annotation(  MethodChecksSyntax = true )
    public void configVoltageSourceRange(String SMUChannel, boolean AutoRanging, double VoltageRange)
           throws IOException, DataFormatException, ScriptException {
        
        ///////////////
        // Syntax-Check
        
        // convert to lower case
        SMUChannel = SMUChannel.toLowerCase();
        

        // check channel 
        checkChannelName(SMUChannel);
        
        // translate OutputState
        String AutoRangeCmd;
        if (AutoRanging) {
            AutoRangeCmd = String.format("smu%s.AUTORANGE_ON", SMUChannel);
        } else {
            AutoRangeCmd = String.format("smu%s.AUTORANGE_OFF", SMUChannel);
        }
        
        // check if Voltage Range is within limits
        VoltageRange = Math.abs(VoltageRange);
        if (VoltageRange > 200) {
            String str = "The Voltage Range must be below 200 V.";
            throw new DataFormatException(str);
        }
                

        // return if in Syntax-check mode
        if (inSyntaxCheckMode())
            return;

        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return;

          
        // build the GPIB command
        String cmd = String.format(Locale.US, "smu%s.source.autorangev = %s", 
                                                SMUChannel, AutoRangeCmd );

        // check the error queue
        checkErrorQueue("The previous command to configVoltageSourceRange caused an error.\n");

        // send the command
        SendToInstrument(cmd);

        // check the error queue
        checkErrorQueue("Setting to Auto-Range the SMU caused an error.\n");

        // is Auto-Ranging disabled?
        if ( !AutoRanging) {
            // yes, so set the voltage source range
            
            // build the GPIB command
            cmd = String.format(Locale.US, "smu%s.source.rangev = %f", 
                                                SMUChannel, VoltageRange );
            
            // send the command
            SendToInstrument(cmd);

            // check the error queue
            checkErrorQueue("Setting the Voltage Source Range caused an error.\n");
            
        }

        
    }//</editor-fold>
    
    /**
     * Measures Current.<p>
     *
     * This method performs a Syntax-Check.
     *
     * @param SMUChannel The SMU Channel, can be A, B (case insensitive)
     *
     * @throws IOException re-thrown from <code>SendToInstrument</code> or 
     * <code>QueryInstrument</code>
     * 
     * @throws ScriptException if the returned answer from the Instrument could
     * not be interpreted as a Double value (bubbles up from <code>getDouble</code>.
     * 
     * @throws DataFormatException when the Syntax Check failed.
     */
    // <editor-fold defaultstate="collapsed" desc="MeasureI">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Measures and returns the current measured by the SMU.</html>",
        ParameterNames = {"SMU Channel Name {A, B}"},
        DefaultValues = {"A"},
        ToolTips = {""})
    @iC_Annotation(  MethodChecksSyntax = true )
    public double MeasureI(String SMUChannel)
           throws IOException, DataFormatException, ScriptException {

        // local variables
        double ret = Double.NaN;

        // convert to lower case
        SMUChannel = SMUChannel.toLowerCase();     

        ///////////////
        // Syntax-Check

        // check channel 
        checkChannelName(SMUChannel);
        
        
        // return if in Syntax-check mode
        if (inSyntaxCheckMode())
            return ret;

        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return ret;

        // build the GPIB command
        String cmd = String.format(Locale.US, "print(smu%s.measure.i())", SMUChannel);

        // query answer
        String ans = QueryInstrument(cmd);

        // try to convert to a double
        ret = getDouble(ans);
        
        // return value
        return ret;
    }//</editor-fold>

    
    /**
     * Sets the Voltage Output Level. This value is sourced at the specified
     * SMU when the force voltage mode was selected and the output state is
     * set to on.<p>
     *
     * This method performs a Syntax-Check.
     *
     * @param SMUChannel The SMU Channel, can be A, B (case insensitive)
     *
     * @throws IOException re-thrown from <code>SendToInstrument</code> or 
     * <code>QueryInstrument</code>
     * @throws ScriptException When the 2600's error queue contained an error.
     * @throws DataFormatException when the Syntax Check failed.
     */
    // <editor-fold defaultstate="collapsed" desc="OutputV">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Set the new Voltage Output Level of the SMU</html>",
        ParameterNames = {"SMU Channel Name {A, B}", "Output Voltage {[+-200]}"},
        DefaultValues = {"A", "0"},
        ToolTips = {"", "</html>"})
    @iC_Annotation(  MethodChecksSyntax = true )
    public void OutputV(String SMUChannel, double Voltage)
           throws IOException, DataFormatException, ScriptException {
        
        // convert to lower case
        SMUChannel = SMUChannel.toLowerCase();     

        ///////////////
        // Syntax-Check

        // check channel 
        checkChannelName(SMUChannel);
        
        
        // return if in Syntax-check mode
        if (inSyntaxCheckMode())
            return;

        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return;

        // build the GPIB command
        String cmd = String.format(Locale.US, "smu%s.source.levelv = %f", 
                                                SMUChannel, Voltage);

        // check the error queue
        checkErrorQueue("The previous command to OutputV caused an error.\n");

        // send the command
        SendToInstrument(cmd);

        // check the error queue
        checkErrorQueue("OutputV caused an error.\n");

    }//</editor-fold>
    
    
    /**
     * Sets the Current Output Level. This value is sourced at the specified
     * SMU when the force current mode was selected and the output state is
     * set to on.<p>
     *
     * This method performs a Syntax-Check.
     *
     * @param SMUChannel The SMU Channel, can be A, B (case insensitive)
     *
     * @throws IOException re-thrown from <code>SendToInstrument</code> or 
     * <code>QueryInstrument</code>
     * @throws ScriptException When the 2600's error queue contained an error.
     * @throws DataFormatException when the Syntax Check failed.
     */
    // <editor-fold defaultstate="collapsed" desc="OutputI">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Set the new Current Output Level of the SMU</html>",
        ParameterNames = {"SMU Channel Name {A, B}", "Output Current {[+-1.5]}"},
        DefaultValues = {"A", "0"},
        ToolTips = {"", "</html>"})
    @iC_Annotation(  MethodChecksSyntax = true )
    public void OutputI(String SMUChannel, float Current)
           throws IOException, DataFormatException, ScriptException {
        
        // convert to lower case
        SMUChannel = SMUChannel.toLowerCase();     

        ///////////////
        // Syntax-Check

        // check channel 
        checkChannelName(SMUChannel);
        
        
        // return if in Syntax-check mode
        if (inSyntaxCheckMode())
            return;

        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return;

        // build the GPIB command
        String cmd = String.format(Locale.US, "smu%s.source.leveli = %f", 
                                                SMUChannel, Current);

        // check the error queue
        checkErrorQueue("The previous command to OutputI caused an error.\n");            

        // send the command
        SendToInstrument(cmd);

        // check the error queue
        checkErrorQueue("OutputI caused an error.\n");

    }//</editor-fold>
    
 
    
    /**
     * Configures the Auto-Zero mode and/or performs an A/D internal reference 
     * measurement (autozero).<p>
     *
     * This method performs a Syntax-Check.
     *
     * @param SMUChannel The SMU Channel, can be A, B (case insensitive)
     * @param AutoZero The autozero mode; can be Auto, Off, Once (case insensitive)
     *
     * @throws IOException re-thrown from {@link Device#SendToInstrument}
     * @throws ScriptException When the 2600's error queue contained an error
     * @throws DataFormatException when the Syntax Check failed.
     */
    // <editor-fold defaultstate="collapsed" desc="AutoZero">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Select the AutoZero mode</html>",
        ParameterNames = {"SMU Channel Name {A, B}", "Auto Zero mode"},
        DefaultValues = {"A", "Once"},
        ToolTips = {"", "Can be Auto, Off, or Once. Once performs an autozero measurement and turns off autozeroing.</html>"})
    @iC_Annotation(  MethodChecksSyntax = true )
    public void AutoZero(String SMUChannel, String AutoZero)
           throws IOException, DataFormatException, ScriptException {

        ///////////////
        // Syntax-Check

        // convert to lower case
        SMUChannel = SMUChannel.toLowerCase();

        // check channel 
        checkChannelName(SMUChannel);
        
        // check and translate AutoZero mode
        String cmd = String.format(Locale.US, "smu%1$s.measure.autozero=smu%1$s.", SMUChannel);
        
        if (AutoZero.equalsIgnoreCase("Auto")) {
            cmd += "AUTOZERO_AUTO";
        } else if (AutoZero.equalsIgnoreCase("Off")) {
            cmd += "AUTOZERO_OFF";
        } else if (AutoZero.equalsIgnoreCase("Once")) {
            cmd += "AUTOZERO_ONCE";
        } else {
            String str = "The Autozero mode '" + AutoZero + "' is not valid."
                    + "Please select a value from:\n"
                    + "Auto, Off, Once.\n";
                    
            throw new DataFormatException(str);            
        }
                     

        // return if in Syntax-check mode
        if (inSyntaxCheckMode())
            return;

        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return;


        // check the error queue
        checkErrorQueue("The previous command to AutoZero caused an error.\n");

        // send the command
        SendToInstrument(cmd);

        // check the error queue
        checkErrorQueue("configuring the AutoZero mode caused an error.\n");

    }//</editor-fold>
    
    /**
     * This methods stores parameters controlling <code>SeepVmeasureI</code> (also
     * in syntax-Check mode). These configuration parameters are stored separately 
     * for each SMU channel.
     * 
     * @param SMUChannel The SMU Channel, can be A, B (case insensitive)
     * @param HoldTime The time in ms between applying a new voltage and the first measurement
     * @param DelayTime Additional wait time between two measurements. Note that an
     * additional delay time might be set in the Keithley 2600 (<code>configDelayTime</code>)
     * @param NrMeasurements The number of measurements that will be taken at each voltage
     * @param NrAveraging The return value of <code>SweepVmeasureI</code> is obtained
     * by averaging over the last <code>NrAveraging</code> measurements.
     * @param AutoStabilize If true, the standard deviation and drift of the measurements
     * is evaluated to find when the measurements are stable. To avoid deadlocks,
     * a maximum of 'Nr. of measurements' are taken.
     * 
     * @throws DataFormatException When the Syntax-check fails
     */
    // <editor-fold defaultstate="collapsed" desc="SweepVmeasureIconfig">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Configures parameters controlling the SweepVmeasureI command.<br>Must be called before SweepVmeasureI.</html>",
        ParameterNames = {"SMU Channel Name {A, B}", "Hold Time [ms]", "Additional Delay Time [ms]",
                          "Nr. of measurements", "Nr. of averaging", "Auto Stabilize"},
        DefaultValues = {"A", "1000", "100", "15", "5", "false"},
        ToolTips = {"", "Time between applying a new voltage and the first measurement", 
                    "<html>Time between two measurements.<br>The Keithley's internal Delay Time is also active.</html>",
                    "Number of measurements at each voltage", "Average over the last N measurements as return value",
                    "Wait until readings have stabilized or a maximum of 'Nr. of measurements' points were measured."})
    @iC_Annotation(MethodChecksSyntax=true)
    public void SweepVmeasureIconfig(
                    String SMUChannel, 
                    int HoldTime, int DelayTime,
                    int NrMeasurements, int NrAveraging, boolean AutoStabilize) 
            throws DataFormatException {
        
        // local variables
        SVMIconfig NewConfig;
        
        
        ///////////////
        // Syntax-Check
        // it also stores the configuration parameters

        // convert to lower case
        SMUChannel = SMUChannel.toLowerCase();
        
        // check channel 
        checkChannelName(SMUChannel);
        
        // get SVMIconfig variable
        if (SMUChannel.equals("a")) {
            
            // instantiate configuration for channel A if it does not exist
            if (m_SVMIconfig_A == null) {
                m_SVMIconfig_A = new SVMIconfig();
            }
            
            // remember selection
            NewConfig = m_SVMIconfig_A;
            
        } else {
            
            // instantiate configuration for channel B if it does not exist
            if (m_SVMIconfig_B == null) {
                m_SVMIconfig_B = new SVMIconfig();
            }
            
            // remember selection
            NewConfig = m_SVMIconfig_B;
        }
        
        
        
        // positive HoldTime?
        if ( HoldTime < 0.0) {
            String str = "HoldTime must be greater or equal to 0.";
            throw new DataFormatException(str);   
        }
        
        // positive DelayTime?
        if ( DelayTime < 0.0) {
            String str = "DelayTime must be greater or equal to 0.";
            throw new DataFormatException(str);   
        }
        
        // positive NrMeasurements?
        if ( NrMeasurements < 1) {
            String str = "Number of Measurements must be greater or equal to 1.";
            throw new DataFormatException(str);   
        }
        
        // positive NrAveraging?
        if ( NrAveraging < 1) {
            String str = "Number of Averaging must be greater or equal to 1.";
            throw new DataFormatException(str); 
        } 
            
        // is NrAveraging small enough? 
        if (NrAveraging > NrMeasurements) {
            String str = "Number of Averaging must be less or equal to 'Nr. of measurements'.";
            throw new DataFormatException(str);
        }
        
        
        // remember configuration parameters
        NewConfig.HoldTime = HoldTime;
        NewConfig.DelayTime = DelayTime;
        NewConfig.NrMeasurements = NrMeasurements;
        NewConfig.NrAveraging = NrAveraging;
        NewConfig.AutoStabilize = AutoStabilize;
        
        
        // display warning of unimplemented AutoStabilize
        if (inSyntaxCheckMode() && AutoStabilize) {
            m_GUI.DisplayStatusMessage("Warning: AutoStabilize in SweepVMeasureI is not yet implemented. Parameter will be ignored.\n");
        }
    }//</editor-fold>
    
    
    /**
     * This method sweeps the voltage in the given range and measures the current
     * at each voltage level by invoking <code>private SweepVmeasureI</code> with 
     * a <code>CallerID</code> of "SweepVmeasureI". See the description of 
     * {@link #SweepVmeasureI(String, double, double, double, boolean, String, int) }
     * for more details.
     */
    // <editor-fold defaultstate="collapsed" desc="SweepVmeasureI">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Sweeps the voltage and measures the current at each voltage.</html>",
        ParameterNames = {"SMU Channel Name {A, B}", 
                          "Start Voltage", "Stop Voltage", "Step Size", "Log Sweep",
                          "File Extension"},
        DefaultValues = {"A", "1e-3", "10", "5", "true", "vi.txt"},
        ToolTips = {"", "", "", "Voltage Step or Number of Steps per Decade", "", ""})
    @iC_Annotation(  MethodChecksSyntax = true )
    public double[][] SweepVmeasureI(String SMUChannel,
                                     double Start, double Stop, double Step, boolean Log,
                                     String FileExtension) 
           throws DataFormatException, ScriptException, IOException {
        
        // call the method that does all the work with a CallerID 1
        return SweepVmeasureI(SMUChannel, Start, Stop, Step, Log, FileExtension, "SweepVmeasureI");
    }//</editor-fold>
    
    
    
    /**
     * This is a helper method that actually measures the current at different
     * voltages as described below. It can be called with different <code>CallerID</code>s
     * and, thus, the Chart generated looks slightly different. This is to avoid
     * replication of code as this method is called from <code>SweepVmeasureI</code>
     * as well as from <code>MeasureOPV</code>. Because the chart produced by these
     * methods look different, this approach was chosen.<p>
     * 
     * This method sweeps the voltage in the given range and measures the current
     * at each voltage level. Multiple current measurements are taken at each
     * voltage step, and the return value is averaged over the last few measurements.
     * An automatic stabilization detection can be selected, which evaluates the
     * standard deviation and linear drift to determine when a measurement is stable.
     * The parameters governing this auto-stabilization are defined in the iC.properties.
     * Note that Auto-stabilization is un-commented at this time as it has not been
     * tested. Note that after the applying the first voltage, an additional wait
     * time is introduced, set by Keithley2600.SweepVmeasureI.WaitFirstVoltage in
     * the iC.properties.
     * 
     * @param SMUChannel The SMU Channel, can be A, B (case insensitive)
     * @param Start The start value of the sweep voltage
     * @param Stop The stop value of the sweep voltage
     * @param Step The step size of the sweep voltage if a linear sweep is selected,
     * or the number of steps per decade if a log sweep is selected
     * @param Log Selects a linear or logarithmic scaling of the sweep voltage
     * @param FileExtension This extension will be added to the file name to save
     * the measured data in a file and also the chart as a png (depending on 
     * <code>CallerID</code>).
     * @param CallerID Controlls the chart's appearance; can be 'SweepVMeasureI'
     * or 'MeasureOPV'.
     * 
     * @return A double[][] with voltage values in the first column (ret[:][0]) 
     * and the averaged current in the second (ret[:][1]) or <code>null</code> 
     * if the values have not been measured (e.g. when the user pressed the stop 
     * button or something else went wrong).
     * 
     * @throws ScriptException When the file to save the data could not be opened,
     * or written to.
     * @throws IOException re-thrown from <code>OutputV</code>, or <code>MeasureI</code>
     * @throws DataFormatException When the Syntax-check fails (can also bubble 
     * up from <code>Device.GenerateValues</code>
     */
    // <editor-fold defaultstate="collapsed" desc="SweepVmeasureI">
    private double[][] SweepVmeasureI(String SMUChannel,
                                     double Start, double Stop, double Step, boolean Log,
                                     String FileExtension,
                                     String CallerID) 
           throws DataFormatException, ScriptException, IOException {
        
        //////////////////
        // local variables
        
        // running number of the measurement number for plotting the IT-chart
        int PointNumber = 0;
        
        ArrayList<Double> V = null;
        ArrayList<Double> Iaverage = new ArrayList<Double>();
        SVMIconfig Config;
        
        
        ///////////////
        // Syntax-Check
        
        // convert to lower case
        SMUChannel = SMUChannel.toLowerCase();
        
        // check channel 
        checkChannelName(SMUChannel);
        
        
        
        // has SweepVmeasureIconfig been called for that channel before?
        if (SMUChannel.equals("a")) {
            
            // SMU channel A
            if (m_SVMIconfig_A == null) {
                String str = "Please call SweepVmeasureIconfig for SMU channel A before\n."
                        + "using SweepVmeasureI.";
                throw new DataFormatException(str);
            }
            
            // remember selection
            Config = m_SVMIconfig_A;
            
        } else {
            
            // SMU channel B
            if (m_SVMIconfig_B == null) {
                String str = "Please call SweepVmeasureIconfig for SMU channel B before\n"
                        + "using SweepVmeasureI.";
                throw new DataFormatException(str);
            }
            
            // remember selection
            Config = m_SVMIconfig_B;
        }

        
        // make a new Utilites object
        Utilities util = new Utilities();
        
        // generate Voltage levels; is at the same time a Syntax-Check
        try {
            
            V = util.GenerateValues(Start, Stop, Step, Log, null, 6);
            
        } catch (ScriptException ex) {
            
            String str = "Could not generate the voltage vector:\n" + ex.getMessage();
            throw new DataFormatException(str);
        }
        
        
        // return if in Syntax-check mode
        if (inSyntaxCheckMode())
            return null;

        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return null;
        
        
        
        /////////////////////////
        // open file to save data
        
        // <editor-fold defaultstate="collapsed" desc="open file">
        // get the filename
        String FileName = m_GUI.getFileName(FileExtension);

        // open the file for writing
        BufferedWriter OutputFileWriter;
        try {
            OutputFileWriter = new BufferedWriter( new FileWriter(FileName) );

        } catch (IOException ex) {
            String str = "Could not open the file\n" + FileName + ".\n"
                + "Error message: \n" + ex.getMessage();
            throw new ScriptException(str);
        }
        
        // write file header
        try {

            // reformat the current date and time
            SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd_HHmmss");
            String DateString = sdf.format(Calendar.getInstance().getTime());

            // write to file
            OutputFileWriter.write("% iC: date and time measured:" + DateString);
            OutputFileWriter.newLine();
            
            // column header
            String dummy = "V\tIaverage";
            
            // add NrMeasurements col headers
            // (when AutoStabilize is selected there can be more col headers)
            for (int t=1; t <= Config.NrMeasurements; t++) {
                dummy += String.format(Locale.US, "\tI%02d", t);
            }
            
            // write to file
            OutputFileWriter.write(dummy);
            OutputFileWriter.newLine();
            

        } catch (IOException ex) {
            String str = "Could not write the header to file\n" + FileName + "\n"
                    + ex.getMessage();
            
            // close file
            if (OutputFileWriter!=null) {
                try { OutputFileWriter.close(); } catch (IOException ignore) {}
            }

            throw new ScriptException(str);
        }//</editor-fold>
        
        
        ////////////////////
        // prepare the Chart
        
        // <editor-fold defaultstate="collapsed" desc="prepare charts">
        
        // make a new XYChart to view I(V)
        m_IV_Chart = new iC_ChartXY("ch " + SMUChannel.toUpperCase() + " - I(V)", 
                                "Voltage [V]", "Current [A]",
                                false  /*legend*/,
                                640, 480);
        
        // make a new XYChart to view all I measurements if more than one
        // measurement is to be taken
        iC_ChartXY IT_Chart;
        if (Config.NrMeasurements > 1) {
            IT_Chart = new iC_ChartXY("ch " + SMUChannel.toUpperCase() + " - detail", 
                                    "# of measurement", "Current [I]",
                                    false  /*legend*/,
                                    640, 480);
        } else {
            IT_Chart = null;
        }
        
        // add new trace to IV graph depending on the CallerID
        SeriesIdentification IV_Series; 
        
        if (CallerID.equalsIgnoreCase("MeasureOPV")) {
            
            // style for MeasureOV
            IV_Series = m_IV_Chart.AddXYSeries("IV",
                    0, false, true, m_IV_Chart.LINE_NONE, m_IV_Chart.MARKER_CIRCLE);
            
            // show zero lines
            m_IV_Chart.ShowZeroLines(true, true);
            
        } else {
            
            // default syle, also for SweepVmeasureI
            IV_Series = m_IV_Chart.AddXYSeries("IV",
                    0, false, true, m_IV_Chart.LINE_SOLID, m_IV_Chart.MARKER_DOT);
        }
    
        
        // do not include 0 in autoranging
        m_IV_Chart.getYAxis(0).setAutoRangeIncludesZero(false);
        if (IT_Chart != null) {
            IT_Chart.getYAxis(0).setAutoRangeIncludesZero(false);
        }
        
        
        // set log axis
        m_IV_Chart.LogYAxis(Log);
        
//        // TODO delme development
//        // generate X/Y values to test log plot autoscale
//        IV_Chart.getYAxis(0).setAutoRange(true);
//        
//        double[] xt = new double[6];
//        double[] yt = new double[6];
//        for (int t=0; t<6; t++) {
//            xt[t] = t;
//            yt[t] = (t+1)*1e-6;
//        }
//        // plot
//        IV_Chart.AddXYDataPoint(IV_Series, xt, yt);
//        
//        //IV_Chart.getYAxis(0).setRange(1e-6, 1e-5);
////        LogarithmicAxis la = (LogarithmicAxis)IV_Chart.getYAxis(0);
////        la.autoAdjustRange();
//        
//        if (true) return Double.NaN;
        
        
        // set number formats
        m_IV_Chart.getYAxis(0).setNumberFormatOverride(new DecimalFormat("##0.#####E0"));
        if (IT_Chart != null) {
            // http://docs.oracle.com/javase/6/docs/api/java/text/DecimalFormat.html
            IT_Chart.getYAxis(0).setNumberFormatOverride(new DecimalFormat("##0.#####E0"));
        }
        //</editor-fold>
        
        
        // get value for additional wait after applying the first voltage
        int FirstVoltageWait = m_iC_Properties.getInt("Keithley2600.SweepVmeasureI.WaitFirstVoltage", 250);
        
        ///////////////////////////////
        // iterate through all Voltages
        boolean FirstVoltage = true;
        for (double v : V) {
        
        
            // apply voltage
            OutputV(SMUChannel, v);
            
            // add extra wait if it's the first voltage point
            if (FirstVoltage) {
                // remember that it's not the first voltage point
                FirstVoltage = false;
                
                // wait
                try {Thread.sleep(FirstVoltageWait);} catch (InterruptedException ignore) {}
            }
            
            // add new trace to IT graph
            SeriesIdentification IT_Series = null;
            if (IT_Chart != null) {
                IT_Series = IT_Chart.AddXYSeries("V"+v,
                        0, false, true, IT_Chart.LINE_SOLID, IT_Chart.MARKER_NONE);
            }
            
            // make new measurement buffer
            ArrayList<Double> I_Measurements = new ArrayList<Double>(Config.NrMeasurements);
        
            
            ////////////////////
            // wait for HoldTime
            
            // check for the Stop button
            if (m_StopScripting) 
                return null;
            
            // wait
            try {
                Thread.sleep(Config.HoldTime);
            } catch (InterruptedException ignore) {}
            
            // check for the Stop button again
            if (m_StopScripting) 
                return null;
            
        
            // was Autostabilized selected?
            if ( !Config.AutoStabilize ) {
                
                // no, loop through desired number of measurements per voltage
                for (int t=1; t<=Config.NrMeasurements; t++) {
                    
                    // wait for delay time
                    // Note that the default delay time in the Keithley 2600 is also active
                    try {Thread.sleep(Config.DelayTime);} catch (InterruptedException ignore) {}
        
                    // check for the Stop button again
                    if (m_StopScripting) 
                        return null;
                    
                    // check Pause button
                    isPaused(true);
            
                    // measure the current
                    double I = MeasureI(SMUChannel);
                    // TODO delme development
                    //double I = v + 1 * (Math.random() - 0.5);
                    
                    // store the current
                    I_Measurements.add(I);
                    
                    // increase Point Number
                    PointNumber++;

                    // plot this data point
                    if (IT_Chart != null) {
                        IT_Chart.AddXYDataPoint(IT_Series, PointNumber, I);
                    }
                }
                
            } else {
                throw new UnsupportedOperationException("Auto-Stabilization is not yet implementd.\n");
                
                // wait for Auto-stabilization
                //      (max measurements: NrMeasurements)
                //      once reading is stable, measure NrAveraging more data points
                //      also plot measured data
                
//		double Current = 0.0;
//		int i = 0;
//		int idiv;
//		bool Stable = false;
//		double sy, sy2, sx, sx2, sxy;
//		
//		// init
//		sy = sy2 = sx = sx2 = sxy = 0.0;
//		for (i=0; i<=10; i++)
//			m_sto[i] = 0.0;
//		i = 0;
//
//		do	// do the ExpWait loop
//		{
//			idiv = i%10;
//			if (i >= 10)
//			{
//				sy -= m_sto[idiv];
//				sy2 -= m_sto[idiv] * m_sto[idiv];
//				sxy -= m_sto[idiv] * (i-10);                                        
//				sx2 -= (i-10) * (i-10);
//				sx -= (i-10);
//			}
//
//			// ask to send reading
//			m_pDevice->Write(":data:fres?");	// sense:data:fresh?
//			m_pDevice->Read(temp, 50);
//			ret = atof(temp);
//			m_sto[idiv] = ret;
//			//Sleep(50);		
//				// usleep(50000) = 50000us=50ms
//				// not necessary since one measurement needs 2 powercycles integration time
//
//			// store Measurement range
//			m_pDevice->Write("sense:curr:range?");
//			m_pDevice->Read(temp, 50);
//			Range = atof(temp);
//
//			
//			sy += m_sto[idiv];
//			sy2 += m_sto[idiv] * m_sto[idiv];
//			sxy += m_sto[idiv]*i;
//			sx2 += i*i;
//			sx += i;
//			
//			/* The criteria are: (1) standard deviation less than 5%
//			of end result
//			(2) linear drift term smaller than 1
//			standard deviation per second */
//			
//			if (i >= 9)
//			{
//				double av = 0.1 * sy;
//				double sig = sqrt(0.1*sy2 - av*av);
//				double alin = (sxy - 0.1*sx*sy)/(sx2 - 0.1*sx*sx);
//			
//				if (fabs(sig/av) < 0.05 && fabs(alin) < 0.1*sig)
//				{
//					Stable = true;
//					Current = av;
//				}
//			}
//			i++;
//		} while (!Stable);
                
            }
                      
            ///////////////
            // do averaging
            
            // get a list iterator starting with the first measurement to average
            ListIterator<Double> it = I_Measurements.listIterator(I_Measurements.size() - Config.NrAveraging);
            
            // iteratre through list until the end
            Double Sum = 0.0;
            while (it.hasNext()) {
                Sum += it.next();
            }
            
            // calc the average
            double Iavg = Sum / Config.NrAveraging;
            
            // remember the average
            Iaverage.add(Iavg);
        
            
            
            ///////////////
            // plot average
            
            // plot average as a line in a new trace in the IT plot
            
            // make a new trace in the IT graph
            if (IT_Chart != null) {
                IT_Series = IT_Chart.AddXYSeries("Vavg" + v,
                        0, false, true, IT_Chart.LINE_DASHED, IT_Chart.MARKER_NONE);
            }
            
            // define the two endpoints
            double[] x = new double[2];
            double[] y = new double[2];
            x[0] = PointNumber - Config.NrAveraging;
            x[1] = PointNumber;
            y[0] = Iavg;
            y[1] = Iavg;
            
            // draw the line
            if (IT_Chart != null) {
                IT_Chart.AddXYDataPoint(IT_Series, x, y);
            }
            
            
            // plot average as a single data point in the IV plot
            m_IV_Chart.AddXYDataPoint(IV_Series, v, Iavg);
            
            
            
            ///////////////
            // save average
            // also saves all "inbetween" measurements
            
            try {
                // write voltage
                OutputFileWriter.write(v + "\t");

                // write averaged I
                OutputFileWriter.write( Double.toString(Iavg) );

                // write all "inbetween" measurements
                for (double I : I_Measurements) {
                    OutputFileWriter.write("\t" + I);
                }

                // write a newline
                OutputFileWriter.newLine();
            } catch (IOException ex) {
                String str = "Could not save the data in the file.\n"
                        + ex.getMessage() + "\n";
                
                throw new ScriptException(str);        
            }
            
        } // end iterate through all voltages
                
        
        // close the file
        try { if (OutputFileWriter != null) OutputFileWriter.close();
        } catch (IOException ex) {
            m_GUI.DisplayStatusMessage("Could not close the file " + FileExtension + "\n");
        }
        
             
        // save the chart depending on CallerID
        if ( !CallerID.equalsIgnoreCase("MeasureOPV") ) {
            // make a new file to save the Chart as png
            File f = new File(m_GUI.getFileName(FileExtension + ".png"));

            try {
                // save the chart as PNG
                m_IV_Chart.SaveAsPNG(f, 0, 0);

            } catch (IOException ex) {
                // "ignore" exceptions and just display a warning message
                String str = "Error: Could not save the IV Chart as PNG.\n";
                m_GUI.DisplayStatusMessage(str);
            }
        }
        
        
        /////////////////////////////////
        // build the array to be returned
        
        // check length
        if (V.size() != Iaverage.size()) {
            String str = "Error: Length of V and I is different. Please tell the developer.\n";
            m_GUI.DisplayStatusMessage(str);
        }
        
        // make the new array
        double[][] ret = new double[V.size()][2];
        
        // fill the array
        for(int i=0; i < V.size(); i++) {
            ret[i][0] = V.get(i);
            ret[i][1] = Iaverage.get(i);
        }
        

        // display Status message
        m_GUI.DisplayStatusMessage("Keithley 2600: Data saved in " + FileExtension + "\n");
        
        return ret;
    }//</editor-fold>
    
    
    
    /**
     * Performs a Syntax-Check for the correct Channel Name.
     *
     * @param Channel The Channel name to test
     * @throws DataFormatException If Syntax-Check failed.
     */
    // <editor-fold defaultstate="collapsed" desc="checkChannelName">
    private void checkChannelName(String Channel) 
            throws DataFormatException {

        // Syntax Check for correct Channel name
        if ( !Channel.equals("a") && !Channel.equals("b") ) {
            String str = "The Channel Name '" + Channel + "' is not valid.\n";
            str += "Please select either 'a' or 'b'.\n";
            
            throw new DataFormatException(str);
        }
    }//</editor-fold>

    

    /**
     * Specialized method to measure (Organic) Photovoltaic Cells (OPV).<p>
     * 
     * This method sweeps the voltage in the given range and measures the current
     * at each voltage level by invoking <code>private SweepVmeasureI</code> with 
     * a <code>CallerID</code> of "MeasureOPV. See the description of 
     * {@link #SweepVmeasureI(String, double, double, double, boolean, String, int) }
     * for more details.<p>
     * 
     * After measuring the current, the characteristic of OPV Cell is analysed 
     * by calling <code>EvaluateOPV</code> and the results are written to a file.
     * See {@link #EvaluateOPV(double[][], String) } for more details on what 
     * parameters are extracted and how it's done.
     */
    // <editor-fold defaultstate="collapsed" desc="MeasureOPV">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Specialized command to measure and evaluate (organic) Photovoltaic Cells (OPV).<br>"
                           + "Sweeps the voltage and measures the current at each voltage, and<br>"
                           + "then extracts Voc, Isc, series- and shunt resistance, fill factor, etc.</html>",
        ParameterNames = {"SMU Channel Name {A, B}", 
                          "Start Voltage", "Stop Voltage", "Step Size", "Log Sweep",
                          "File Extension"},
        DefaultValues = {"A", "-0.5", "0.5", "0.05", "false", ".opv1.txt"},
        ToolTips = {"", "", "", "Voltage Step or Number of Steps per Decade", "", ""})
    @iC_Annotation(  MethodChecksSyntax = true )
    public double[][] MeasureOPV(String SMUChannel,
                                 double Start, double Stop, double Step, boolean Log,
                                 String FileExtension)
           throws DataFormatException, ScriptException, IOException {
        
        // return value
        double[][] VI_Data = null;
        
        
        // call the method that does all the work with a CallerID "MeasureOPV"
        VI_Data = SweepVmeasureI(SMUChannel, Start, Stop, Step, Log, FileExtension, "MeasureOPV");
        
        // exit if in Syntax-Check mode
        if (inSyntaxCheckMode())
            return VI_Data;
        
        
        
        
        // exit if in No-Communication Mode (or assign test data)
        if (inNoCommunicationMode()) {
            // for development: assign real data when in No-Communication-Mode
            //VI_Data = testOPV(true);
            return VI_Data;
        }
        
        
        // evaluate the data
        EvaluateOPV(VI_Data, FileExtension);
        
        
        
        // make a new file to save the Chart as png
        File f = new File(m_GUI.getFileName(FileExtension + ".png"));

        try {
            // save the chart as PNG
            m_IV_Chart.SaveAsPNG(f, 0, 0);

        } catch (IOException ex) {
            // "ignore" exceptions and just display a warning message
            String str = "Error: Could not save the IV Chart as PNG.\n";
            m_GUI.DisplayStatusMessage(str);
        }
        
        
        return VI_Data;
    }//</editor-fold>
    
    
    /**
     * Evaluates the characteristic parameters of an (organic) photovoltaic cell.
     * 
     * The parameters extracted from the data are short circuit current Isc, open
     * circuit voltage Voc, the voltage/current for maximum power output Vmaxpower
     * and Imaxpower, as well as Shunt and Series resistance in a one Diode model.<p>
     * 
     * It's probably overkill, but the measured data is first interpolated 
     * using a Loess Interpolator where the fitting parameters are defined in
     * iC.properties. To find Voc, a Pegasus solver (also from Apache Commons
     * Math) is used. Series and shunt resistances are found from the slope of a
     * linear interpolation around Isc and Voc; the region used is also defined
     * in iC.properties. The so calculated Series and Shunt resistances are 
     * estimates only, and there might be better ways of extracting those values,
     * e.g. to fit the data to the analytical formula describing the one diode
     * equivalent circuit model (http://en.wikipedia.org/wiki/Theory_of_solar_cell).
     * Note: In particular, the Series resistance might be more accurately extracted
     * from the largest V, not from Voc as in this region the I(V) curve should
     * be most linear [Olson 2009 - PhD thesis]. Also refer to [Bowden and Rohatgi]
     * and [Nelson03 - The Physics of Solar Cells], [Pysch07], [Zhong12].<p>
     * 
     * The extracted parameters are displayed on the chart defined in the member
     * variable <code>m_IV_Chart</code>. This chart is generated by 
     * <code>SweepVmeasureI</code> with the CallerID of "MeasureOPV". It's not
     * the cleanest solution to pass the chart around this way, but it was the
     * easiest.
     * 
     * This method has protected access to be able to test it, otherwise it could
     * be, and should be, private (see http://onjava.com/pub/a/onjava/2003/11/12/reflection.html?page=2
     * how to access private methods.
     * 
     * @param VI_Data The measured data as returned from 
     * {@link #SweepVmeasureI(String, double, double, double, boolean, String, String) }
     * 
     * @param FileExtension This extension with an appended _eval will be added 
     * to the file name to save the extracted parameters.
     * 
     * @return <code>True</code> when the evaluation could be completed; does
     * not mean that it is correct, just that no unexpected situation occurred.
     */
    // <editor-fold defaultstate="collapsed" desc="EvaluateOPV">
    protected boolean EvaluateOPV(double[][] VI_Data, String FileExtension) {
                
        // log event
        m_Logger.finer("Starting EvaluateOPV\n");
        
        // return if not more than two data points are available
        if ( VI_Data == null || VI_Data.length < 3 )
            return false;
        
        // check if user pressed Stop
        if (m_StopScripting)
            return false;
        
        
        // local variables
        double Vmax = VI_Data[0][0];
        double Vmin = VI_Data[0][0];
        
        double[] V = new double[VI_Data.length];
        double[] I = new double[VI_Data.length];
        
        double Vmaxpower = 0;    // voltage at maximum power
        double Imaxpower = 0;    // current at maximum power
        
        // for interpolation the x-values need to be strictly increasing,
        // hence, check if reversing is required
        boolean ReverseX = false;
        if (VI_Data[0][0] > VI_Data[VI_Data.length-1][0]) {
            
            // yes, reversing of x-values is required
            ReverseX = true;
            
            // log event
            m_Logger.finer("EvaluateOPV: need to reverse data\n");
        }
               
        // fill those arrays with the measured data
        // and find Vmax/Vmin
        for (int i=0; i < VI_Data.length; i++) {
            
            // copy to two arrays in increasing x-values
            if (ReverseX) {
                V[i] = VI_Data[VI_Data.length - 1 - i][0];
                I[i] = VI_Data[VI_Data.length - 1 - i][1];
            } else {
                V[i] = VI_Data[i][0];
                I[i] = VI_Data[i][1];
            }
            
            // find Vmax
            if (VI_Data[i][0] > Vmax)
                Vmax = VI_Data[i][0];
            
            // find Vmin
            if (VI_Data[i][0] < Vmin)
                Vmin = VI_Data[i][0];
        }
        
        
        ////////////////////////////////
        // fit using Apache Commons Math

        UnivariateInterpolator interpolator;
        UnivariateFunction function;
        

        double Bandwidth = m_iC_Properties.getDouble("Keithley2600.MeasureOPV.LoessBandwidth", 0.01);
        int RobustnessIters = m_iC_Properties.getInt("Keithley2600.MeasureOPV.LoessRobustnessIterations", 2);
        double Accuracy = m_iC_Properties.getDouble("Keithley2600.MeasureOPV.LoessAccuracy", 1e-12);
//        Accuracy=1e-30;
//        RobustnessIters=1000;
//        Bandwidth=0.001;
        
        /* adjust Bandwidth for low number of data points to prevent a MathException
         * when interpolating. As far as I can tell, the Bandwidth must be larger
         * than 2/#data points and smaller than 1 */
        if (Bandwidth < (2.0/VI_Data.length)) {
            Bandwidth = 2.0/VI_Data.length;
            
            // inform the user
            String str = "Infor: Had to adjust Loess Bandwith (must be < 2/NrDataPoints)\n";
            m_GUI.DisplayStatusMessage(str, false);
        }
        
        
        try {
            // make the interpolator
            // http://commons.apache.org/math/userguide/analysis.html#a12.6_Curve_Fitting
            interpolator = new LoessInterpolator(Bandwidth, RobustnessIters, Accuracy);
            
            // generate the interpolating function
            function = interpolator.interpolate(V, I);
            
            
            // When experimenting with real measurement data, I found that the Loess
            // interpolation sometimes works with a bandwidth of 0.1, sometimes 0.01
            // is required. When the interpolation did not work, the function evaluation
            // returned NaN, so I test for NaN here
            if (function.value(0) == Double.NaN) {
                String str = "Info: The Loess interpolation seems to have failed. Try decreasing the Loess Bandwidth\n"
                        + "in the iC_User.properties using Keithley2600.MeasureOPV.LoessBandwidth = 0.01.\n";
                m_GUI.DisplayStatusMessage(str);
            }
            
            /* Catch all Exceptions as Commons.Math might throw different Exceptions
             * that have not a common Math.Exception as their root, and the
             * javadoc is not 100% correct. */
        } catch (Exception ex) {

            String str = "Error: Could not generate the interpolating curve.\n";
            str += ex.getMessage() + "\n";

            // just show a Status Message and end evaluating the measured data
            m_GUI.DisplayStatusMessage(str);
            return false;
        }
        
        
        //////////
        // get Isc
        double Isc;
        try {
            
            // evaluate interpolation at V=0
            Isc = function.value(0); 
            
            // log it
            m_Logger.log(Level.FINE, "EvaluateOPV: Isc = {0}\n", Isc);
            
        } catch (Exception ex) {
            String str = "Error: Could not evaluate the interpolating function.\n";
            str += ex.getMessage() + "\n";
            
            // just show a Status Message and end evaluating the measured data
            m_GUI.DisplayStatusMessage(str);
            return false;
        }
        
        
        //////////
        // get Voc
        
        // make a new Pegasus solver
        PegasusSolver Pegasus = new PegasusSolver();
        
        double Voc = 0;
        try {
            // solve for I(V)=0
            Voc = Pegasus.solve(100, function, Vmin, Vmax);
            
            // log it
            m_Logger.log(Level.FINE, "EvaluateOPV: Voc = {0}\n", Voc);
            
        } catch (Exception ex) {
            String str = "Error: Could not determine Voc:\n" + ex.getMessage() + "\n";
            
            // just show a Status Message and end evaluating the measured data
            m_GUI.DisplayStatusMessage(str);
            return false;
        }
        
        
        ///////////////////////////////////////
        // get Pmax + calc interpolation values
        
        // reserve space for the interpolation valus with 1 mV resolution
        final double dV = 1e-3;
        
        // variables definitions
        int NrInterpolationPoints = (int)Math.floor( (Vmax - Vmin) / dV );
        double[] V_int = new double[NrInterpolationPoints];
        double[] I_int = new double[NrInterpolationPoints];
        double Pmax = 0;
        
        
        // calc all interpolation values
        // also calculate power as a function of voltage P(V)
        for (int i=0; i<NrInterpolationPoints; i++) { 

            try {
                // calc V and I
                V_int[i] = Vmin + i * dV;
                I_int[i] = function.value(V_int[i]);
                
                // is current point in 3rd quadrant?
                if (V_int[i] > 0 && I_int[i] < 0) {
                
                    // calc power
                    double P = Math.abs(V_int[i] * I_int[i]);

                    // is it max power?
                    if (P > Pmax) {
                        // remember Vmax, Imax
                        Vmaxpower = V_int[i];
                        Imaxpower = I_int[i];
                        
                        // remember Pmax
                        Pmax = P;
                    }
                } 
            } catch (Exception ex) {
                String str = "Error: Could not evaluate the interpolating function.\n";
                str += ex.getMessage() + "\n";

                // just show a Status Message and end evaluating the measured data
                m_GUI.DisplayStatusMessage(str);
                //return;
            }
        }
        
        // log Vmax, Imax
        m_Logger.log(Level.FINE, "EvaluateOPV: Vmax = {0}\n", Vmaxpower);
        m_Logger.log(Level.FINE, "EvaluateOPV: Imax = {0}\n", Imaxpower);
        
        
        // calc Fill Factor
        // java does not rise an Exception for division by 0 !
        double FillFactor = Vmaxpower * Imaxpower / Isc / Voc;
        
        
        // get device area
        final double DeviceArea = m_iC_Properties.getDouble("Keithley2600.MeasureOPV.DeviceArea", 0.04);
        
        // incident light power density
        final double LightIntensity = m_iC_Properties.getDouble("Keithley2600.MeasureOPV.LightIntensity", 0.1);
        
        // calc power conversion efficiency
        double PCE = FillFactor * Isc * Voc / (DeviceArea * LightIntensity);
        
        
        
        ///////////////////////
        // calc Rshunt, Rseries
        
        double Rseries = Double.NaN;
        double Rshunt = Double.NaN;
        
        // get range for interpolation
        double InterpolationRange = m_iC_Properties.getDouble("Keithley2600.MeasureOPV.R_InterpoaltionRange", 0.025);
        
        // check if Interpolation Range is bigger than step size
        try {
            if (InterpolationRange < 2*(V[1]-V[0])) {
                InterpolationRange = 2*(V[1]-V[0]);
                
                //display event
                m_GUI.DisplayStatusMessage("Info: Increased InterpolationRange because it was smaller than 2*Step size\n", false);
            }
        } catch (IndexOutOfBoundsException ex) {
            // there are not enough data points, so exit
            return false;
        }
        
        
        // make a new Polynomial fitters of degree 1 (y=k*x+d)
        // using a GaussNewtonOptimizer sometimes stalled the evaluation without
        // throwing an exception or any other means I know of of detecting this condition
        PolynomialFitter Rsc = new PolynomialFitter(1, new GaussNewtonOptimizer());
        PolynomialFitter Roc = new PolynomialFitter(1, new GaussNewtonOptimizer());
//        PolynomialFitter Rsc = new PolynomialFitter(1, new LevenbergMarquardtOptimizer());
//        PolynomialFitter Roc = new PolynomialFitter(1, new LevenbergMarquardtOptimizer());
        
        // get V and I within InterpoaltionRange (25 mV) of Voc and 0V
        for (int i=0; i < V.length; i++) {
            
            // near 0V / Isc ?
            if (V[i] >= 0-InterpolationRange && V[i] <= 0+InterpolationRange) {
                
                // use this data point
                Rsc.addObservedPoint(V[i], I[i]);
            }
            
            // near Voc?
            if (V[i] >= Voc-InterpolationRange && V[i] <= Voc+InterpolationRange) {
                
                // log for submitting question to apache commons math
                //m_Logger.finer("RshuntFitter.addObservedPoint(" + V[i] + ", " + I[i] + ");\n");
                
                // use this data point
                Roc.addObservedPoint(V[i], I[i]);
            }
        }
        
        // log event
        m_Logger.finer("EvaluateOPV: will be fitting R @ Voc (Rseries)\n");
        
        // calc and plot R @ Voc (Rseries)
        try {
            
            // find the fit
            PolynomialFunction RocFit = new PolynomialFunction(Roc.fit());
            
            // define voltage endpoints (can now be outside fitted range)
            double v1 = Voc - 5 * InterpolationRange;
            double v2 = Voc + 5 * InterpolationRange;
                        
            // calc current endpoints  
            double i1 = RocFit.value(v1);
            double i2 = RocFit.value(v2);
            
            // calc Rseries
            Rseries = (v2 - v1) / (i2 - i1);
            
            // add a new trace to the chart
            SeriesIdentification Rseries_Series = m_IV_Chart.AddXYSeries("Rseries",
                    0, false, true, m_IV_Chart.LINE_DASHED, m_IV_Chart.MARKER_NONE);

            // add the data
            m_IV_Chart.AddXYDataPoint(Rseries_Series, v1, i1);
            m_IV_Chart.AddXYDataPoint(Rseries_Series, v2, i2);
            
        } catch (Exception ex) {
            String str = "Error: Could not evaluate the interpolation for extracting R @ Voc (Rseries).\n";
            str += ex.getMessage() + "\n";
            
            // just show a Status Message and end evaluating the measured data
            m_GUI.DisplayStatusMessage(str);
        }

                
        // log event
        m_Logger.finer("EvaluateOPV: will be fitting R @ Isc (Rshunt)\n");
        
        
        // calc and plot R @ Isc Rshunt ( I think it should be Rshunt//Rseries)
        try {
            // find the fit
            PolynomialFunction RscFit = new PolynomialFunction(Rsc.fit());
            
            // define voltage endpoints (can now be outside fitted range)
            double v1 = 0 - 5 * InterpolationRange;
            double v2 = 0 + 5 * InterpolationRange;
                        
            // calc current endpoints  
            double i1 = RscFit.value(v1);
            double i2 = RscFit.value(v2);
            
            // calc total Rshunt
            Rshunt = (v2 - v1) / (i2 - i1);
            
            // add a new trace to the chart
            SeriesIdentification Rshunt_Series = m_IV_Chart.AddXYSeries("Rshunt",
                    0, false, true, m_IV_Chart.LINE_DASHED, m_IV_Chart.MARKER_NONE);

            // add the data
            m_IV_Chart.AddXYDataPoint(Rshunt_Series, v1, i1);
            m_IV_Chart.AddXYDataPoint(Rshunt_Series, v2, i2);
            
        } catch (Exception ex) {
            String str = "Error: Could not evaluate the interpolation for extracting R @ Isc (Rshunt//Rseries).\n";
            str += ex.getMessage() + "\n";
            
            // just show a Status Message and end evaluating the measured data
            m_GUI.DisplayStatusMessage(str);
        }
        
        
        // log event
        m_Logger.finer("EvaluateOPV: done fitting Rseries, Rshunt\n");
        
        
        ///////////////////////////
        // plot interpolation curve
        
        // add a new trace to the chart
        SeriesIdentification Interpoaltion_Series = m_IV_Chart.AddXYSeries("IV_interpolated",
                0, false, true, m_IV_Chart.LINE_SOLID_FINE, m_IV_Chart.MARKER_NONE);
        
        // add the data
        m_IV_Chart.AddXYDataPoint(Interpoaltion_Series, V_int, I_int);
        

        // OLD: an artifact which is good to keep in mind !!
        //for (double v=Vmin; v<=Vmax; v+=0.001) { 
           /* Interestingly, the last data point isn't plotted, becuase
            * after the few additions, the second last value is 0.6900000000000005
            * instead of 0.69, hence the next value is larger than 0.7
            */
                           
        
        
        //////////////////////////
        // plot Isc, Voc, maxPower
        
        // add a new trace to the chart
        SeriesIdentification Isc_Series = m_IV_Chart.AddXYSeries("CalculatedPoints",
                0, false, true, m_IV_Chart.LINE_NONE, m_IV_Chart.MARKER_DIAMOND_15x);
        
        // plot Isc
        m_IV_Chart.AddXYDataPoint(Isc_Series, 0, Isc);
        
        // plot Voc
        m_IV_Chart.AddXYDataPoint(Isc_Series, Voc, 0);
        
        // plot max power
        m_IV_Chart.AddXYDataPoint(Isc_Series, Vmaxpower, Imaxpower);
        
        
        
        /////////////////////////
        // open file to save data
        
        // make a Regex pattern & matcher to add .eval to FileExtension
        Pattern p = Pattern.compile("(.*)(.[tT][xX][tT].*)");
        Matcher m = p.matcher(FileExtension);
        
        // match the pattern
        String NewFileExtension;
        if (m.matches()) {
            // add "_eval" before .txt if .txt was specified
            NewFileExtension = m.group(1) + "_eval"
                    + (m.group(2)!=null ? m.group(2) : ".txt");
        } else {
            // fallback solution
            NewFileExtension = FileExtension + "_eval.txt";
        }
        
        
        // get the filename
        String FileName = m_GUI.getFileName(NewFileExtension);

        // get platform specific newline character
        final String NewLine = System.getProperty("line.separator");
        
        // reformat the current date and time
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd_HHmmss");
        String DateString = sdf.format(Calendar.getInstance().getTime());

        // build the String
        String str = "% iC: date and time of the evaluation: " + DateString + NewLine + NewLine;
        
        str += String.format("Isc = %.4E V", Isc) + NewLine;
        str += String.format("Voc = %.4E A", Voc) + NewLine + NewLine;
        
        str += String.format("V @ max power = %.4E V", Vmaxpower) + NewLine;
        str += String.format("I @ max power = %.4E A", Imaxpower) + NewLine + NewLine;
        
        str += String.format("Fill Factor = %.4E", FillFactor) + NewLine
             + "The Fill Factor is calculated with respect to V=I=0, not the dark curve." + NewLine + NewLine;
        

        str += "PLEASE TEST THE CALCULATION OF Rseries & Rshunt AND LET ME KNOW IF THEY ARE CORRECT." + NewLine;
        str += String.format("Estimated Rseries = %.4E Ohm (from the slope of I(V) @ Voc)", Rseries) + NewLine;
        str += String.format("Estimated Rshunt = %.4E Ohm (from the slope of I(V) @ Isc)", Rshunt) + NewLine + NewLine;
        
        str += String.format("Assuming a device area of %.4E cm^2", DeviceArea) + NewLine
             + String.format("and a light intensity of %.4E W/cm^2", LightIntensity) + NewLine
             + String.format("the Power Conversion Efficiency is %.2f %%", PCE) + NewLine;

        try {
            // open the file for writing
            FileWriter fw = new FileWriter(FileName);
            
            // write to file
            fw.write(str);
            
            // close the file
            fw.close();
            
        } catch (IOException ex) {
            String str2 = "Error: Could not open/write to/close the file\n" + FileName + ".\n"
                + "Error message: \n" + ex.getMessage();
            
            // just show a Status Message and end evaluating the measured data
            m_GUI.DisplayStatusMessage(str2);
            return false;         
        }        
        
        // log event
        m_Logger.finer("EvaluateOPV: done\n");
        
        // return with success
        return true;
    }//</editor-fold>
    
    
    /**
     * Overridden <code>Close</code> method that puts the Instrument in a
     * defined state (output is switched to High_Z mode and output voltage and
     * current of both SMUs is set to 0).
     *
     * @throws IOException re-thrown from <code>SendViaGPIB</code>
     * @see Device#Close
     */
    // <editor-fold defaultstate="collapsed" desc="Close">
    @Override
    public void Close() 
           throws IOException {

        try {
            // switch output to highZ
            OutputState("a", "highZ");
            OutputState("b", "highZ");
            
            // set output voltage to 0
            OutputV("a", 0);
            OutputV("b", 0);
            
            // set output current to 0
            OutputI("a", 0);
            OutputI("b", 0);
            
            // check for errors
            checkErrorQueue("After Closing the Instrument.\n");
        
        } catch (Exception ex) {
            
            // just inform the user and proceed
            String str = "Could not put the Keithley2600 in a defined state in Close().\n";
            m_GUI.DisplayStatusMessage(str, false);
            
            // log more details
            m_Logger.severe(ex.getMessage());
        }

        
        
    }//</editor-fold>
    
    /**
     * Simulates a real measurement for testing purposes.
     * 
     * @param PlotData If <code>true</code> then a new chart will be created as
     * does <code>private SweepVmeasureI</code> (in <code>m_IV_Chart</code>).
     * @return A double[][] array with real voltage/current data in 
     * ret[:][0] and ret[:][1]
     */
    // <editor-fold defaultstate="collapsed" desc="testOPV">
    public double[][] testOPV(boolean PlotData) {
        
        // log that I am using test data
        m_Logger.config("ATTENTION: Using test data in MeasureOPV\n");
        
        // James' device
        // works with Loess Bandwidth of 0.1 and gives a decent smooth curve
        double[][] James = 
            {{-200.00000E-3,	-319.54700E-6},{-190.00000E-3,	-319.92200E-6},{-180.00000E-3,	-320.65200E-6},{-170.00000E-3,	-320.22600E-6},
            {-160.00000E-3,	-320.09100E-6},{-150.00000E-3,	-317.16900E-6},{-140.00000E-3,	-317.40900E-6},{-130.00000E-3,	-317.86200E-6},
            {-120.00000E-3,	-316.59500E-6},{-110.00000E-3,	-315.53500E-6},{-100.00000E-3,	-317.06200E-6},{-90.00000E-3,	-316.48900E-6},
            {-80.00000E-3,	-317.85100E-6},{-70.00000E-3,	-316.29600E-6},{-60.00000E-3,	-314.85000E-6},{-50.00000E-3,	-314.23400E-6},
            {-40.00000E-3,	-312.21500E-6},{-30.00000E-3,	-311.86100E-6},{-20.00000E-3,	-314.31500E-6},{-10.00000E-3,	-311.90000E-6},
            {-55.51120E-18,	-310.80200E-6},{10.00000E-3,	-311.15700E-6},{20.00000E-3,	-310.63300E-6},{30.00000E-3,	-307.86300E-6},
            {40.00000E-3,	-307.38300E-6},{50.00000E-3,	-306.52000E-6},{60.00000E-3,	-305.20300E-6},{70.00000E-3,	-303.80100E-6},
            {80.00000E-3,	-303.24100E-6},{90.00000E-3,	-304.20800E-6},{100.00000E-3,	-303.40600E-6},{110.00000E-3,	-302.95100E-6},
            {120.00000E-3,	-301.69700E-6},{130.00000E-3,	-299.11400E-6},{140.00000E-3,	-296.86000E-6},{150.00000E-3,	-293.40000E-6},
            {160.00000E-3,	-292.09200E-6},{170.00000E-3,	-289.80600E-6},{180.00000E-3,	-289.82700E-6},{190.00000E-3,	-289.17900E-6},
            {200.00000E-3,	-286.05000E-6},{210.00000E-3,	-284.92700E-6},{220.00000E-3,	-281.98600E-6},{230.00000E-3,	-278.48600E-6},
            {240.00000E-3,	-274.80700E-6},{250.00000E-3,	-274.20400E-6},{260.00000E-3,	-269.39300E-6},{270.00000E-3,	-267.09200E-6},
            {280.00000E-3,	-264.86100E-6},{290.00000E-3,	-260.22300E-6},{300.00000E-3,	-255.75700E-6},{310.00000E-3,	-251.51900E-6},
            {320.00000E-3,	-247.52200E-6},{330.00000E-3,	-242.14400E-6},{340.00000E-3,	-236.57100E-6},{350.00000E-3,	-230.75100E-6},
            {360.00000E-3,	-225.83100E-6},{370.00000E-3,	-219.39800E-6},{380.00000E-3,	-213.07400E-6},{390.00000E-3,	-205.17300E-6},
            {400.00000E-3,	-198.26500E-6},{410.00000E-3,	-190.30700E-6},{420.00000E-3,	-181.70900E-6},{430.00000E-3,	-174.27200E-6},
            {440.00000E-3,	-165.85100E-6},{450.00000E-3,	-156.88200E-6},{460.00000E-3,	-147.86400E-6},{470.00000E-3,	-137.48200E-6},
            {480.00000E-3,	-127.73300E-6},{490.00000E-3,	-117.40400E-6},{500.00000E-3,	-106.25700E-6},{510.00000E-3,	-94.81660E-6},
            {520.00000E-3,	-83.07180E-6},{530.00000E-3,	-71.77530E-6},{540.00000E-3,	-59.10770E-6},{550.00000E-3,	-46.47390E-6},
            {560.00000E-3,	-33.29240E-6},{570.00000E-3,	-19.72630E-6},{580.00000E-3,	-5.28516E-6},{590.00000E-3,	9.52266E-6},
            {600.00000E-3,	24.76310E-6},{610.00000E-3,	40.81530E-6},{620.00000E-3,	57.42350E-6},{630.00000E-3,	74.78120E-6},
            {640.00000E-3,	93.64270E-6},{650.00000E-3,	112.86400E-6},{660.00000E-3,	133.00800E-6},{670.00000E-3,	153.67100E-6},
            {680.00000E-3,	175.33600E-6},{690.00000E-3,	198.27500E-6},{700.00000E-3,	222.05400E-6}};
        
        // Wei's device
        // only works with Loess Bandwidth of 0.01 and gives not such a good smoothed interpolation
        double[][] Wei = {{-1.0	,-2.78598E-5}, {-0.99	,-2.77114E-5}, {-0.98	,-2.7448E-5	}, {-0.97	,-2.74486E-5}, {-0.96	,-2.73369E-5}, {-0.95	
            ,-2.73513E-5}, {-0.94	,-2.72033E-5}, {-0.93	,-2.71084E-5}, {-0.92	,-2.69713E-5}, {-0.91	,-2.69542E-5}, {-0.9	
            ,-2.68521E-5}, {-0.89	,-2.67774E-5}, {-0.88	,-2.65429E-5}, {-0.87	,-2.63579E-5}, {-0.86	,-2.60967E-5}, {-0.85	
            ,-2.58411E-5}, {-0.84	,-2.56843E-5}, {-0.83	,-2.56284E-5}, {-0.82	,-2.55657E-5}, {-0.81	,-2.56038E-5}, {-0.8	
            ,-2.55192E-5}, {-0.79	,-2.54179E-5}, {-0.78	,-2.54169E-5}, {-0.77	,-2.53152E-5}, {-0.76	,-2.51382E-5}, {-0.75	
            ,-2.5012E-5	}, {-0.74	,-2.49763E-5}, {-0.73	,-2.48979E-5}, {-0.72	,-2.47632E-5}, {-0.71	,-2.44929E-5}, {-0.7	
            ,-2.44019E-5}, {-0.69	,-2.42607E-5}, {-0.68	,-2.42726E-5}, {-0.67	,-2.41539E-5}, {-0.66	,-2.42168E-5}, {-0.65	
            ,-2.41945E-5}, {-0.64	,-2.40965E-5}, {-0.63	,-2.39313E-5}, {-0.62	,-2.37952E-5}, {-0.61	,-2.37298E-5}, {-0.6	
            ,-2.36305E-5}, {-0.59	,-2.35975E-5}, {-0.58	,-2.34759E-5}, {-0.57	,-2.33353E-5}, {-0.56	,-2.32067E-5}, {-0.55	
            ,-2.31281E-5}, {-0.54	,-2.29828E-5}, {-0.53	,-2.28304E-5}, {-0.52	,-2.27718E-5}, {-0.51	,-2.2734E-5	}, {-0.5	
            ,-2.26393E-5}, {-0.49	,-2.25852E-5}, {-0.48	,-2.26022E-5}, {-0.47	,-2.24753E-5}, {-0.46	,-2.23613E-5}, {-0.45	
            ,-2.22146E-5}, {-0.44	,-2.20806E-5}, {-0.43	,-2.19176E-5}, {-0.42	,-2.17885E-5}, {-0.41	,-2.16338E-5}, {-0.4	
            ,-2.15316E-5}, {-0.39	,-2.13955E-5}, {-0.38	,-2.14044E-5}, {-0.37	,-2.13087E-5}, {-0.36	,-2.12255E-5}, {-0.35	
            ,-2.11127E-5}, {-0.34	,-2.10469E-5}, {-0.33	,-2.09738E-5}, {-0.32	,-2.08772E-5}, {-0.31	,-2.07771E-5}, {-0.3	
            ,-2.06422E-5}, {-0.29	,-2.05083E-5}, {-0.28	,-2.03797E-5}, {-0.27	,-2.02258E-5}, {-0.26	,-2.01407E-5}, {-0.25	
            ,-2.00554E-5}, {-0.24	,-1.98411E-5}, {-0.23	,-1.96978E-5}, {-0.22	,-1.95826E-5}, {-0.21	,-1.95474E-5}, {-0.2	
            ,-1.95567E-5}, {-0.19	,-1.94704E-5}, {-0.18	,-1.93697E-5}, {-0.17	,-1.92351E-5}, {-0.16	,-1.91121E-5}, {-0.15	
            ,-1.88752E-5}, {-0.14	,-1.87481E-5}, {-0.13	,-1.87328E-5}, {-0.12	,-1.85662E-5}, {-0.11	,-1.84075E-5}, {-0.1	
            ,-1.82534E-5}, {-0.09	,-1.81987E-5}, {-0.08	,-1.81027E-5}, {-0.07	,-1.80562E-5}, {-0.06	,-1.79015E-5}, {-0.05	
            ,-1.78122E-5}, {-0.04	,-1.7615E-5	}, {-0.03	,-1.74806E-5}, {-0.02	,-1.72845E-5}, {-0.01	,-1.7134E-5	}, {0.00	
            ,-1.69764E-5}, {0.01	,-1.68571E-5}, {0.02	,-1.67272E-5}, {0.03	,-1.66062E-5}, {0.04	,-1.64887E-5}, {0.05	
            ,-1.63169E-5}, {0.06	,-1.62079E-5}, {0.07	,-1.60301E-5}, {0.08	,-1.59027E-5}, {0.09	,-1.57271E-5}, {0.10	
            ,-1.55252E-5}, {0.11	,-1.5359E-5	}, {0.12	,-1.51605E-5}, {0.13	,-1.50174E-5}, {0.14	,-1.47922E-5}, {0.15	
            ,-1.46296E-5}, {0.16	,-1.44069E-5}, {0.17	,-1.42091E-5}, {0.18	,-1.39941E-5}, {0.19	,-1.38147E-5}, {0.20	
            ,-1.36168E-5}, {0.21	,-1.34085E-5}, {0.22	,-1.32285E-5}, {0.23	,-1.30056E-5}, {0.24	,-1.27656E-5}, {0.25	
            ,-1.24878E-5}, {0.26	,-1.22031E-5}, {0.27	,-1.19399E-5}, {0.28	,-1.16701E-5}, {0.29	,-1.14622E-5}, {0.30	
            ,-1.11313E-5}, {0.31	,-1.08531E-5}, {0.32	,-1.05169E-5}, {0.33	,-1.02135E-5}, {0.34	,-9.91881E-6}, {0.35	
            ,-9.61435E-6}, {0.36	,-9.28141E-6}, {0.37	,-8.93795E-6}, {0.38	,-8.6225E-6	}, {0.39	,-8.29242E-6}, {0.40	
            ,-7.90502E-6}, {0.41	,-7.51848E-6}, {0.42	,-7.12991E-6}, {0.43	,-6.73646E-6}, {0.44	,-6.35606E-6}, {0.45	
            ,-5.94898E-6}, {0.46	,-5.54578E-6}, {0.47	,-5.15222E-6}, {0.48	,-4.71162E-6}, {0.49	,-4.26207E-6}, {0.50	
            ,-3.79936E-6}, {0.51	,-3.29926E-6}, {0.52	,-2.78974E-6}, {0.53	,-2.25374E-6}, {0.54	,-1.67325E-6}, {0.55	
            ,-1.06235E-6}, {0.56	,-4.13555E-7}, {0.57	,2.77079E-7	}, {0.58	,1.03397E-6	}, {0.59	,1.83686E-6	}, {0.60	
            ,2.70241E-6 }, {0.61	,3.65319E-6	}, {0.62	,4.67337E-6	}, {0.63	,5.78987E-6	}, {0.64	,6.99645E-6	}, {0.65	
            ,8.31519E-6	}, {0.66	,9.74561E-6	}, {0.67	,1.13022E-5	}, {0.68	,1.29961E-5	}, {0.69	,1.48197E-5	}, {0.70	
            ,1.68074E-5 }, {0.71	,1.89395E-5	}, {0.72	,2.1246E-5	}, {0.73	,2.37295E-5	}, {0.74	,2.64039E-5	}, {0.75	
            ,2.92761E-5	}, {0.76	,3.23403E-5	}, {0.77	,3.56295E-5	}, {0.78	,3.91399E-5	}, {0.79	,4.28893E-5	}, {0.80	
            ,4.68882E-5 }, {0.81	,5.11296E-5	}, {0.82	,5.56566E-5	}, {0.83	,6.04712E-5	}, {0.84	,6.5588E-5	}, {0.85	
            ,7.10005E-5	}, {0.86	,7.67625E-5	}, {0.87	,8.28716E-5	}, {0.88	,8.93567E-5	}, {0.89	,9.62194E-5	}, {0.90	
            ,1.03495E-4 }, {0.91	,1.11199E-4	}, {0.92	,1.19327E-4	}, {0.93	,1.27905E-4	}, {0.94	,1.36979E-4	}, {0.95	
            ,1.46472E-4	}, {0.96	,1.56489E-4	}, {0.97	,1.6702E-4	}, {0.98	,1.78044E-4	}, {0.99	,1.89525E-4	}, {1.00	
            ,2.01571E-4 }};
        
        
        // Polyfit fails
        double[][] PolyFitFails = {{-0.2	,-7.12442E-13	},
                        {-0.199	,-4.33397E-13	},
                        {-0.198	,-2.823E-13	},
                        {-0.197	,-1.40405E-13	},
                        {-0.196	,-7.80821E-15	},
                        {-0.195	,6.20484E-14	},
                        {-0.194	,7.24673E-14	},
                        {-0.193	,1.47152E-13	},
                        {-0.192	,1.9629E-13	},
                        {-0.191	,2.12038E-13	},
                        {-0.19	,2.46906E-13	},
                        {-0.189	,2.77495E-13	},
                        {-0.188	,2.51281E-13	},
                        {-0.187	,2.64001E-13	},
                        {-0.186	,2.8882E-13	},
                        {-0.185	,3.13604E-13	},
                        {-0.184	,3.14248E-13	},
                        {-0.183	,3.1172E-13	},
                        {-0.182	,3.12912E-13	},
                        {-0.181	,3.06761E-13	},
                        {-0.18	,2.8559E-13	},
                        {-0.179	,2.86806E-13	},
                        {-0.178	,2.985E-13	},
                        {-0.177	,2.67148E-13	},
                        {-0.176	,2.94173E-13	},
                        {-0.175	,3.27528E-13	},
                        {-0.174	,3.33858E-13	},
                        {-0.173	,2.97511E-13	},
                        {-0.172	,2.8615E-13	},
                        {-0.171	,2.84624E-13	},
                        {-0.17	,2.62034E-13	},
                        {-0.169	,2.90036E-13	},
                        {-0.168	,2.83527E-13	},
                        {-0.167	,2.87271E-13	},
                        {-0.166	,2.66147E-13	},
                        {-0.165	,3.17526E-13	},
                        {-0.164	,3.27849E-13	},
                        {-0.163	,3.3685E-13	},
                        {-0.162	,3.18944E-13	},
                        {-0.161	,2.90084E-13	},
                        {-0.16	,3.3412E-13	},
                        {-0.159	,2.74539E-13	},
                        {-0.158	,2.62749E-13	},
                        {-0.157	,3.03781E-13	},
                        {-0.156	,2.95794E-13	},
                        {-0.155	,2.85244E-13	},
                        {-0.154	,2.21789E-13	},
                        {-0.153	,2.49207E-13	},
                        {-0.152	,2.87581E-13	},
                        {-0.151	,2.79152E-13	},
                        {-0.15	,2.81632E-13	},
                        {-0.149	,3.2537E-13	},
                        {-0.148	,3.19314E-13	},
                        {-0.147	,2.87581E-13	},
                        {-0.146	,2.9031E-13	},
                        {-0.145	,2.67363E-13	},
                        {-0.144	,2.4296E-13	},
                        {-0.143	,2.80333E-13	},
                        {-0.142	,3.05998E-13	},
                        {-0.141	,2.77829E-13	},
                        {-0.14	,2.86889E-13	},
                        {-0.139	,2.97427E-13	},
                        {-0.138	,2.86555E-13	},
                        {-0.137	,2.6443E-13	},
                        {-0.136	,2.79725E-13	},
                        {-0.135	,2.8156E-13	},
                        {-0.134	,3.25334E-13	},
                        {-0.133	,3.50606E-13	},
                        {-0.132	,3.51083E-13	},
                        {-0.131	,3.54075E-13	},
                        {-0.13	,2.94459E-13	},
                        {-0.129	,2.71142E-13	},
                        {-0.128	,2.59078E-13	},
                        {-0.127	,2.44033E-13	},
                        {-0.126	,2.87819E-13	},
                        {-0.125	,2.85125E-13	},
                        {-0.124	,3.11005E-13	},
                        {-0.123	,3.09527E-13	},
                        {-0.122	,3.25799E-13	},
                        {-0.121	,2.97844E-13	},
                        {-0.12	,3.10111E-13	},
                        {-0.119	,3.02112E-13	},
                        {-0.118	,3.45778E-13	},
                        {-0.117	,3.47054E-13	},
                        {-0.116	,2.93279E-13	},
                        {-0.115	,2.9608E-13	},
                        {-0.114	,2.83253E-13	},
                        {-0.113	,2.90167E-13	},
                        {-0.112	,2.66898E-13	},
                        {-0.111	,3.11971E-13	},
                        {-0.11	,3.14236E-13	},
                        {-0.109	,2.98655E-13	},
                        {-0.108	,2.80917E-13	},
                        {-0.107	,2.689E-13	},
                        {-0.106	,2.67994E-13	},
                        {-0.105	,3.27277E-13	},
                        {-0.104	,3.18456E-13	},
                        {-0.103	,3.23033E-13	},
                        {-0.102	,3.29125E-13	},
                        {-0.101	,3.30293E-13	},
                        {-0.1	,3.02601E-13	},
                        {-0.099	,2.98285E-13	},
                        {-0.098	,2.9434E-13	},
                        {-0.097	,2.86269E-13	},
                        {-0.096	,3.03614E-13	},
                        {-0.095	,2.82455E-13	},
                        {-0.094	,3.28052E-13	},
                        {-0.093	,3.30865E-13	},
                        {-0.092	,3.1656E-13	},
                        {-0.091	,3.17597E-13	},
                        {-0.09	,3.59821E-13	},
                        {-0.089	,3.5826E-13	},
                        {-0.088	,3.34013E-13	},
                        {-0.087	,3.17562E-13	},
                        {-0.086	,3.02052E-13	},
                        {-0.085	,3.03745E-13	},
                        {-0.084	,2.83241E-13	},
                        {-0.083	,2.78437E-13	},
                        {-0.082	,3.17609E-13	},
                        {-0.081	,3.13783E-13	},
                        {-0.08	,3.35884E-13	},
                        {-0.079	,3.29101E-13	},
                        {-0.078	,3.43156E-13	},
                        {-0.077	,2.95913E-13	},
                        {-0.076	,2.99525E-13	},
                        {-0.075	,3.1811E-13	},
                        {-0.074	,2.88582E-13	},
                        {-0.073	,3.4138E-13	},
                        {-0.072	,3.37207E-13	},
                        {-0.071	,2.98023E-13	},
                        {-0.07	,3.30448E-13	},
                        {-0.069	,3.29828E-13	},
                        {-0.068	,3.15487E-13	},
                        {-0.067	,2.82192E-13	},
                        {-0.066	,3.11875E-13	},
                        {-0.065	,3.00717E-13	},
                        {-0.064	,3.36254E-13	},
                        {-0.063	,3.40629E-13	},
                        {-0.062	,3.1718E-13	},
                        {-0.061	,3.17085E-13	},
                        {-0.06	,3.15571E-13	},
                        {-0.059	,3.23009E-13	},
                        {-0.058	,3.3288E-13	},
                        {-0.057	,3.18515E-13	},
                        {-0.056	,3.21746E-13	},
                        {-0.055	,2.93624E-13	},
                        {-0.054	,2.99799E-13	},
                        {-0.053	,3.14236E-13	},
                        {-0.052	,2.88939E-13	},
                        {-0.051	,3.16012E-13	},
                        {-0.05	,3.14629E-13	},
                        {-0.049	,3.24833E-13	},
                        {-0.048	,3.35777E-13	},
                        {-0.047	,3.1054E-13	},
                        {-0.046	,3.10516E-13	},
                        {-0.045	,3.19242E-13	},
                        {-0.044	,2.94256E-13	},
                        {-0.043	,3.12507E-13	},
                        {-0.042	,3.44646E-13	},
                        {-0.041	,3.22652E-13	},
                        {-0.04	,3.14069E-13	},
                        {-0.039	,3.34871E-13	},
                        {-0.038	,3.42608E-13	},
                        {-0.037	,3.07095E-13	},
                        {-0.036	,3.05462E-13	},
                        {-0.035	,3.25024E-13	},
                        {-0.034	,3.08609E-13	},
                        {-0.033	,3.28183E-13	},
                        {-0.032	,3.1538E-13	},
                        {-0.031	,2.91848E-13	},
                        {-0.03	,3.04127E-13	},
                        {-0.029	,3.0961E-13	},
                        {-0.028	,2.97689E-13	},
                        {-0.027	,2.94924E-13	},
                        {-0.026	,2.98965E-13	},
                        {-0.025	,3.40736E-13	},
                        {-0.024	,3.0719E-13	},
                        {-0.023	,3.02911E-13	},
                        {-0.022	,3.19147E-13	},
                        {-0.021	,3.25632E-13	},
                        {-0.02	,3.12614E-13	},
                        {-0.019	,3.2562E-13	},
                        {-0.018	,3.15571E-13	},
                        {-0.017	,3.04425E-13	},
                        {-0.016	,3.14903E-13	},
                        {-0.015	,3.0055E-13	},
                        {-0.014	,2.65968E-13	},
                        {-0.013	,2.5847E-13	},
                        {-0.012	,2.71654E-13	},
                        {-0.011	,3.14927E-13	},
                        {-0.01	,3.06892E-13	},
                        {-0.0090	,3.03245E-13	},
                        {-0.0080	,2.8441E-13	},
                        {-0.0070	,3.18325E-13	},
                        {-0.0060	,3.19171E-13	},
                        {-0.0050	,2.85125E-13	},
                        {-0.0040	,2.9608E-13	},
                        {-0.0030	,2.91729E-13	},
                        {-0.0020	,3.08478E-13	},
                        {-0.0010	,2.8156E-13	},
                        {0.0	,-6.47306E-15	},
                        {0.0010	,7.75814E-14	},
                        {0.0020	,1.30534E-13	},
                        {0.0030	,1.8785E-13	},
                        {0.0040	,1.93548E-13	},
                        {0.0050	,2.60568E-13	},
                        {0.0060	,2.67148E-13	},
                        {0.0070	,2.63786E-13	},
                        {0.0080	,2.66361E-13	},
                        {0.0090	,2.8286E-13	},
                        {0.01	,3.03769E-13	},
                        {0.011	,2.5543E-13	},
                        {0.012	,2.83074E-13	},
                        {0.013	,2.94244E-13	},
                        {0.014	,2.96926E-13	},
                        {0.015	,3.10576E-13	},
                        {0.016	,3.06559E-13	},
                        {0.017	,3.59011E-13	},
                        {0.018	,3.26312E-13	},
                        {0.019	,3.47137E-13	},
                        {0.02	,3.22425E-13	},
                        {0.021	,3.17228E-13	},
                        {0.022	,3.24118E-13	},
                        {0.023	,2.73407E-13	},
                        {0.024	,3.10838E-13	},
                        {0.025	,2.92468E-13	},
                        {0.026	,3.29947E-13	},
                        {0.027	,3.35073E-13	},
                        {0.028	,2.81632E-13	},
                        {0.029	,2.99835E-13	},
                        {0.03	,3.12304E-13	},
                        {0.031	,3.03197E-13	},
                        {0.032	,2.75266E-13	},
                        {0.033	,3.00801E-13	},
                        {0.034	,3.11375E-13	},
                        {0.035	,3.21209E-13	},
                        {0.036	,2.97701E-13	},
                        {0.037	,2.92516E-13	},
                        {0.038	,3.09324E-13	},
                        {0.039	,3.18813E-13	},
                        {0.04	,2.97642E-13	},
                        {0.041	,3.02637E-13	},
                        {0.042	,3.16119E-13	},
                        {0.043	,3.30865E-13	},
                        {0.044	,3.36385E-13	},
                        {0.045	,3.07989E-13	},
                        {0.046	,3.2568E-13	},
                        {0.047	,3.17645E-13	},
                        {0.048	,2.99883E-13	},
                        {0.049	,2.92146E-13	},
                        {0.05	,3.11732E-13	},
                        {0.051	,3.1606E-13	},
                        {0.052	,3.28517E-13	},
                        {0.053	,3.07989E-13	},
                        {0.054	,3.13687E-13	},
                        {0.055	,3.16763E-13	},
                        {0.056	,3.2016E-13	},
                        {0.057	,3.32642E-13	},
                        {0.058	,3.20506E-13	},
                        {0.059	,3.10636E-13	},
                        {0.06	,3.27981E-13	},
                        {0.061	,3.2357E-13	},
                        {0.062	,3.09086E-13	},
                        {0.063	,2.92277E-13	},
                        {0.064	,3.23427E-13	},
                        {0.065	,3.36158E-13	},
                        {0.066	,3.27194E-13	},
                        {0.067	,3.15404E-13	},
                        {0.068	,3.41475E-13	},
                        {0.069	,3.23904E-13	},
                        {0.07	,3.02887E-13	},
                        {0.071	,3.07643E-13	},
                        {0.072	,3.3865E-13	},
                        {0.073	,3.36432E-13	},
                        {0.074	,3.16036E-13	},
                        {0.075	,3.2382E-13	},
                        {0.076	,3.2016E-13	},
                        {0.077	,3.18146E-13	},
                        {0.078	,3.06547E-13	},
                        {0.079	,2.90704E-13	},
                        {0.08	,3.14236E-13	},
                        {0.081	,2.55084E-13	},
                        {0.082	,2.97904E-13	},
                        {0.083	,3.08406E-13	},
                        {0.084	,3.15797E-13	},
                        {0.085	,3.14534E-13	},
                        {0.086	,3.19326E-13	},
                        {0.087	,3.19326E-13	},
                        {0.088	,3.15964E-13	},
                        {0.089	,2.80225E-13	},
                        {0.09	,2.97809E-13	},
                        {0.091	,3.02303E-13	},
                        {0.092	,3.21722E-13	},
                        {0.093	,3.15595E-13	},
                        {0.094	,2.93899E-13	},
                        {0.095	,3.02911E-13	},
                        {0.096	,3.10254E-13	},
                        {0.097	,3.28803E-13	},
                        {0.098	,3.04067E-13	},
                        {0.099	,2.7101E-13	},
                        {0.1	,1.96743E-13	},
                        {0.101	,1.93393E-13	},
                        {0.102	,2.38168E-13	},
                        {0.103	,2.38514E-13	},
                        {0.104	,2.8795E-13	},
                        {0.105	,3.10159E-13	},
                        {0.106	,2.94924E-13	},
                        {0.107	,3.04675E-13	},
                        {0.108	,2.98309E-13	},
                        {0.109	,3.1811E-13	},
                        {0.11	,3.36409E-13	},
                        {0.111	,3.3251E-13	},
                        {0.112	,3.38995E-13	},
                        {0.113	,3.00086E-13	},
                        {0.114	,2.85578E-13	},
                        {0.115	,2.96211E-13	},
                        {0.116	,2.89083E-13	},
                        {0.117	,2.78747E-13	},
                        {0.118	,3.20637E-13	},
                        {0.119	,3.24523E-13	},
                        {0.12	,3.1215E-13	},
                        {0.121	,3.12841E-13	},
                        {0.122	,3.32808E-13	},
                        {0.123	,3.15201E-13	},
                        {0.124	,2.94995E-13	},
                        {0.125	,3.39365E-13	},
                        {0.126	,3.5069E-13	},
                        {0.127	,3.52538E-13	},
                        {0.128	,3.36456E-13	},
                        {0.129	,3.36671E-13	},
                        {0.13	,3.26335E-13	},
                        {0.131	,3.30293E-13	},
                        {0.132	,3.3536E-13	},
                        {0.133	,3.15511E-13	},
                        {0.134	,3.03519E-13	},
                        {0.135	,3.25251E-13	},
                        {0.136	,3.18658E-13	},
                        {0.137	,3.00002E-13	},
                        {0.138	,3.11875E-13	},
                        {0.139	,3.29745E-13	},
                        {0.14	,3.59917E-13	},
                        {0.141	,3.12841E-13	},
                        {0.142	,2.80333E-13	},
                        {0.143	,3.01254E-13	},
                        {0.144	,3.23451E-13	},
                        {0.145	,3.31223E-13	},
                        {0.146	,3.12173E-13	},
                        {0.147	,3.48723E-13	},
                        {0.148	,3.33762E-13	},
                        {0.149	,3.18527E-13	},
                        {0.15	,2.98619E-13	},
                        {0.151	,3.20172E-13	},
                        {0.152	,2.84779E-13	},
                        {0.153	,2.6505E-13	},
                        {0.154	,2.76256E-13	},
                        {0.155	,3.2109E-13	},
                        {0.156	,3.1395E-13	},
                        {0.157	,3.00622E-13	},
                        {0.158	,3.02362E-13	},
                        {0.159	,3.49414E-13	},
                        {0.16	,3.32332E-13	},
                        {0.161	,3.00241E-13	},
                        {0.162	,2.93982E-13	},
                        {0.163	,3.08275E-13	},
                        {0.164	,3.07691E-13	},
                        {0.165	,2.95663E-13	},
                        {0.166	,3.0669E-13	},
                        {0.167	,2.89869E-13	},
                        {0.168	,3.26002E-13	},
                        {0.169	,3.09944E-13	},
                        {0.17	,3.02637E-13	},
                        {0.171	,3.02052E-13	},
                        {0.172	,3.43013E-13	},
                        {0.173	,3.12638E-13	},
                        {0.174	,3.07488E-13	},
                        {0.175	,2.92408E-13	},
                        {0.176	,2.9552E-13	},
                        {0.177	,2.68829E-13	},
                        {0.178	,3.18158E-13	},
                        {0.179	,3.29399E-13	},
                        {0.18	,3.4138E-13	},
                        {0.181	,3.52216E-13	},
                        {0.182	,3.26371E-13	},
                        {0.183	,3.0781E-13	},
                        {0.184	,3.01886E-13	},
                        {0.185	,3.21424E-13	},
                        {0.186	,3.07858E-13	},
                        {0.187	,3.02899E-13	},
                        {0.188	,3.32081E-13	},
                        {0.189	,3.48067E-13	},
                        {0.19	,3.07095E-13	},
                        {0.191	,2.49803E-13	},
                        {0.192	,3.23474E-13	},
                        {0.193	,2.81966E-13	},
                        {0.194	,2.70212E-13	},
                        {0.195	,2.99656E-13	},
                        {0.196	,3.28481E-13	},
                        {0.197	,3.30043E-13	},
                        {0.198	,2.21944E-13	},
                        {0.199	,2.67565E-13	},
                        {0.2	,2.73418E-13	},
                        {0.201	,2.87509E-13	},
                        {0.202	,2.64657E-13	},
                        {0.203	,2.42198E-13	},
                        {0.204	,2.88701E-13	},
                        {0.205	,3.08526E-13	},
                        {0.206	,2.80619E-13	},
                        {0.207	,3.02637E-13	},
                        {0.208	,2.97403E-13	},
                        {0.209	,3.53277E-13	},
                        {0.21	,3.67284E-13	},
                        {0.211	,2.81858E-13	},
                        {0.212	,2.96891E-13	},
                        {0.213	,3.51346E-13	},
                        {0.214	,3.26836E-13	},
                        {0.215	,3.2289E-13	},
                        {0.216	,3.07691E-13	},
                        {0.217	,3.1786E-13	},
                        {0.218	,3.34859E-13	},
                        {0.219	,2.99633E-13	},
                        {0.22	,2.90382E-13	},
                        {0.221	,2.7231E-13	},
                        {0.222	,3.42202E-13	},
                        {0.223	,2.97678E-13	},
                        {0.224	,3.11506E-13	},
                        {0.225	,3.13771E-13	},
                        {0.226	,2.99489E-13	},
                        {0.227	,3.09002E-13	},
                        {0.228	,3.42941E-13	},
                        {0.229	,3.09455E-13	},
                        {0.23	,3.13747E-13	},
                        {0.231	,3.19195E-13	},
                        {0.232	,2.59626E-13	},
                        {0.233	,2.74384E-13	},
                        {0.234	,2.79582E-13	},
                        {0.235	,2.9844E-13	},
                        {0.236	,2.86973E-13	},
                        {0.237	,3.36373E-13	},
                        {0.238	,3.17883E-13	},
                        {0.239	,3.02601E-13	},
                        {0.24	,3.22127E-13	},
                        {0.241	,3.36039E-13	},
                        {0.242	,2.93326E-13	},
                        {0.243	,2.90239E-13	},
                        {0.244	,2.55966E-13	},
                        {0.245	,2.88618E-13	},
                        {0.246	,2.74885E-13	},
                        {0.247	,2.79903E-13	},
                        {0.248	,2.4085E-13	},
                        {0.249	,2.47622E-13	},
                        {0.25	,2.76899E-13	},
                        {0.251	,3.08406E-13	},
                        {0.252	,2.79236E-13	},
                        {0.253	,2.7535E-13	},
                        {0.254	,3.05176E-13	},
                        {0.255	,3.27861E-13	},
                        {0.256	,3.00252E-13	},
                        {0.257	,1.14667E-13	},
                        {0.258	,1.59013E-13	},
                        {0.259	,1.82486E-13	},
                        {0.26	,2.39205E-13	},
                        {0.261	,2.4358E-13	},
                        {0.262	,2.3067E-13	},
                        {0.263	,2.90406E-13	},
                        {0.264	,2.92969E-13	},
                        {0.265	,2.89607E-13	},
                        {0.266	,2.71022E-13	},
                        {0.267	,2.91598E-13	},
                        {0.268	,2.97952E-13	},
                        {0.269	,2.95925E-13	},
                        {0.27	,3.19946E-13	},
                        {0.271	,3.05164E-13	},
                        {0.272	,3.16334E-13	},
                        {0.273	,3.01623E-13	},
                        {0.274	,3.31509E-13	},
                        {0.275	,3.31366E-13	},
                        {0.276	,3.12889E-13	},
                        {0.277	,2.9856E-13	},
                        {0.278	,3.20995E-13	},
                        {0.279	,3.30079E-13	},
                        {0.28	,3.03638E-13	},
                        {0.281	,3.08895E-13	},
                        {0.282	,3.13437E-13	},
                        {0.283	,3.0483E-13	},
                        {0.284	,2.72048E-13	},
                        {0.285	,3.22974E-13	},
                        {0.286	,3.71039E-13	},
                        {0.287	,3.12972E-13	},
                        {0.288	,2.87116E-13	},
                        {0.289	,3.17967E-13	},
                        {0.29	,3.08764E-13	},
                        {0.291	,3.25787E-13	},
                        {0.292	,2.91145E-13	},
                        {0.293	,3.13056E-13	},
                        {0.294	,3.38423E-13	},
                        {0.295	,3.27086E-13	},
                        {0.296	,3.25084E-13	},
                        {0.297	,3.21257E-13	},
                        {0.298	,3.26335E-13	},
                        {0.299	,2.96819E-13	},
                        {0.3	,1.46973E-13	},
                        {0.301	,1.78969E-13	},
                        {0.302	,2.30527E-13	},
                        {0.303	,2.64454E-13	},
                        {0.304	,2.69425E-13	},
                        {0.305	,3.06249E-13	},
                        {0.306	,3.11434E-13	},
                        {0.307	,3.44741E-13	},
                        {0.308	,3.13938E-13	},
                        {0.309	,3.1482E-13	},
                        {0.31	,2.96581E-13	},
                        {0.311	,2.93696E-13	},
                        {0.312	,2.6139E-13	},
                        {0.313	,3.01504E-13	},
                        {0.314	,3.01647E-13	},
                        {0.315	,3.38113E-13	},
                        {0.316	,3.08597E-13	},
                        {0.317	,3.33953E-13	},
                        {0.318	,3.17395E-13	},
                        {0.319	,3.13663E-13	},
                        {0.32	,2.85387E-13	},
                        {0.321	,2.80166E-13	},
                        {0.322	,3.37565E-13	},
                        {0.323	,3.25882E-13	},
                        {0.324	,2.64597E-13	},
                        {0.325	,2.92182E-13	},
                        {0.326	,2.97976E-13	},
                        {0.327	,3.12388E-13	},
                        {0.328	,2.97093E-13	},
                        {0.329	,3.2326E-13	},
                        {0.33	,3.51822E-13	},
                        {0.331	,3.36218E-13	},
                        {0.332	,2.8131E-13	},
                        {0.333	,2.79236E-13	},
                        {0.334	,3.18933E-13	},
                        {0.335	,3.11172E-13	},
                        {0.336	,3.03471E-13	},
                        {0.337	,2.92099E-13	},
                        {0.338	,3.14903E-13	},
                        {0.339	,3.23772E-13	},
                        {0.34	,3.14879E-13	},
                        {0.341	,3.29828E-13	},
                        {0.342	,3.13413E-13	},
                        {0.343	,3.01647E-13	},
                        {0.344	,2.84421E-13	},
                        {0.345	,2.92981E-13	},
                        {0.346	,3.11935E-13	},
                        {0.347	,2.88534E-13	},
                        {0.348	,2.96509E-13	},
                        {0.349	,3.24225E-13	},
                        {0.35	,3.55351E-13	},
                        {0.351	,3.21794E-13	},
                        {0.352	,3.4548E-13	},
                        {0.353	,3.07441E-13	},
                        {0.354	,3.63505E-13	},
                        {0.355	,2.98893E-13	},
                        {0.356	,3.06678E-13	},
                        {0.357	,2.90787E-13	},
                        {0.358	,2.82156E-13	},
                        {0.359	,2.92647E-13	},
                        {0.36	,3.45802E-13	},
                        {0.361	,3.03984E-13	},
                        {0.362	,3.00741E-13	},
                        {0.363	,2.95579E-13	},
                        {0.364	,3.21579E-13	},
                        {0.365	,3.13663E-13	},
                        {0.366	,3.10111E-13	},
                        {0.367	,3.04461E-13	},
                        {0.368	,3.47853E-13	},
                        {0.369	,3.0365E-13	},
                        {0.37	,3.02863E-13	},
                        {0.371	,3.15869E-13	},
                        {0.372	,3.91364E-13	},
                        {0.373	,3.16918E-13	},
                        {0.374	,2.88582E-13	},
                        {0.375	,3.37815E-13	},
                        {0.376	,3.49569E-13	},
                        {0.377	,3.64244E-13	},
                        {0.378	,3.27337E-13	},
                        {0.379	,3.39198E-13	},
                        {0.38	,3.14152E-13	},
                        {0.381	,2.62666E-13	},
                        {0.382	,2.48289E-13	},
                        {0.383	,2.56944E-13	},
                        {0.384	,3.21841E-13	},
                        {0.385	,3.10874E-13	},
                        {0.386	,3.2618E-13	},
                        {0.387	,3.09002E-13	},
                        {0.388	,3.67033E-13	},
                        {0.389	,3.49808E-13	},
                        {0.39	,3.20673E-13	},
                        {0.391	,2.95842E-13	},
                        {0.392	,3.23737E-13	},
                        {0.393	,3.25382E-13	},
                        {0.394	,3.47817E-13	},
                        {0.395	,3.07608E-13	},
                        {0.396	,3.33738E-13	},
                        {0.397	,3.56233E-13	},
                        {0.398	,3.19505E-13	},
                        {0.399	,3.02017E-13	},
                        {0.4	,2.65622E-13	},
                        {0.401	,2.91228E-13	},
                        {0.402	,3.07846E-13	},
                        {0.403	,2.96688E-13	},
                        {0.404	,2.68185E-13	},
                        {0.405	,3.52621E-13	},
                        {0.406	,3.54433E-13	},
                        {0.407	,3.46041E-13	},
                        {0.408	,3.06392E-13	},
                        {0.409	,3.08061E-13	},
                        {0.41	,3.27361E-13	},
                        {0.411	,3.03948E-13	},
                        {0.412	,2.88451E-13	},
                        {0.413	,2.89154E-13	},
                        {0.414	,3.40819E-13	},
                        {0.415	,3.10838E-13	},
                        {0.416	,3.22759E-13	},
                        {0.417	,3.32248E-13	},
                        {0.418	,3.46422E-13	},
                        {0.419	,3.1656E-13	},
                        {0.42	,2.99418E-13	},
                        {0.421	,3.05212E-13	},
                        {0.422	,3.034E-13	},
                        {0.423	,3.08597E-13	},
                        {0.424	,2.88641E-13	},
                        {0.425	,3.08442E-13	},
                        {0.426	,3.26729E-13	},
                        {0.427	,2.5506E-13	},
                        {0.428	,2.97868E-13	},
                        {0.429	,3.35836E-13	},
                        {0.43	,3.09885E-13	},
                        {0.431	,2.99799E-13	},
                        {0.432	,3.13389E-13	},
                        {0.433	,3.29566E-13	},
                        {0.434	,2.9912E-13	},
                        {0.435	,3.00968E-13	},
                        {0.436	,2.93064E-13	},
                        {0.437	,3.43478E-13	},
                        {0.438	,3.22902E-13	},
                        {0.439	,2.97952E-13	},
                        {0.44	,3.1029E-13	},
                        {0.441	,2.99525E-13	},
                        {0.442	,2.94375E-13	},
                        {0.443	,2.65586E-13	},
                        {0.444	,2.89154E-13	},
                        {0.445	,3.09992E-13	},
                        {0.446	,2.96724E-13	},
                        {0.447	,3.2407E-13	},
                        {0.448	,3.54302E-13	},
                        {0.449	,3.6546E-13	},
                        {0.45	,3.13115E-13	},
                        {0.451	,3.33238E-13	},
                        {0.452	,2.97439E-13	},
                        {0.453	,3.17299E-13	},
                        {0.454	,3.01969E-13	},
                        {0.455	,3.15833E-13	},
                        {0.456	,3.10194E-13	},
                        {0.457	,3.29328E-13	},
                        {0.458	,3.26967E-13	},
                        {0.459	,3.27814E-13	},
                        {0.46	,3.33178E-13	},
                        {0.461	,3.31283E-13	},
                        {0.462	,2.97689E-13	},
                        {0.463	,3.31116E-13	},
                        {0.464	,3.25167E-13	},
                        {0.465	,3.37791E-13	},
                        {0.466	,2.7597E-13	},
                        {0.467	,2.59554E-13	},
                        {0.468	,2.82061E-13	},
                        {0.469	,2.76649E-13	},
                        {0.47	,3.09527E-13	},
                        {0.471	,2.89154E-13	},
                        {0.472	,3.24667E-13	},
                        {0.473	,3.24321E-13	},
                        {0.474	,2.97952E-13	},
                        {0.475	,2.99668E-13	},
                        {0.476	,3.06416E-13	},
                        {0.477	,2.78652E-13	},
                        {0.478	,3.02184E-13	},
                        {0.479	,2.82407E-13	},
                        {0.48	,2.91562E-13	},
                        {0.481	,3.00503E-13	},
                        {0.482	,3.03733E-13	},
                        {0.483	,3.11792E-13	},
                        {0.484	,3.02994E-13	},
                        {0.485	,3.34704E-13	},
                        {0.486	,2.96128E-13	},
                        {0.487	,3.2239E-13	},
                        {0.488	,3.14522E-13	},
                        {0.489	,3.20804E-13	},
                        {0.49	,3.38519E-13	},
                        {0.491	,3.01337E-13	},
                        {0.492	,3.22556E-13	},
                        {0.493	,3.15034E-13	},
                        {0.494	,3.20208E-13	},
                        {0.495	,3.1234E-13	},
                        {0.496	,2.91467E-13	},
                        {0.497	,3.10743E-13	},
                        {0.498	,2.90418E-13	},
                        {0.499	,2.98107E-13	},
                        {0.5	,2.83182E-13	},
                        {0.501	,3.05712E-13	},
                        {0.502	,2.8466E-13	},
                        {0.503	,3.21496E-13	},
                        {0.504	,3.27682E-13	},
                        {0.505	,3.51381E-13	},
                        {0.506	,3.28481E-13	},
                        {0.507	,3.38984E-13	},
                        {0.508	,3.00288E-13	},
                        {0.509	,3.00753E-13	},
                        {0.51	,2.80547E-13	},
                        {0.511	,2.92611E-13	},
                        {0.512	,2.97868E-13	},
                        {0.513	,3.02637E-13	},
                        {0.514	,3.33703E-13	},
                        {0.515	,2.84708E-13	},
                        {0.516	,3.31044E-13	},
                        {0.517	,3.07214E-13	},
                        {0.518	,3.08573E-13	},
                        {0.519	,2.81084E-13	},
                        {0.52	,1.99807E-13	},
                        {0.521	,2.19297E-13	},
                        {0.522	,2.38049E-13	},
                        {0.523	,2.39336E-13	},
                        {0.524	,3.12507E-13	},
                        {0.525	,3.00539E-13	},
                        {0.526	,2.67828E-13	},
                        {0.527	,2.58291E-13	},
                        {0.528	,2.78246E-13	},
                        {0.529	,2.81131E-13	},
                        {0.53	,2.55346E-13	},
                        {0.531	,2.93779E-13	},
                        {0.532	,3.09491E-13	},
                        {0.533	,3.60239E-13	},
                        {0.534	,3.11291E-13	},
                        {0.535	,3.30281E-13	},
                        {0.536	,3.30889E-13	},
                        {0.537	,3.28934E-13	},
                        {0.538	,3.02911E-13	},
                        {0.539	,3.17967E-13	},
                        {0.54	,3.05784E-13	},
                        {0.541	,3.13938E-13	},
                        {0.542	,3.29351E-13	},
                        {0.543	,2.83349E-13	},
                        {0.544	,2.85292E-13	},
                        {0.545	,3.05092E-13	},
                        {0.546	,1.91152E-13	},
                        {0.547	,2.5059E-13	},
                        {0.548	,2.80714E-13	},
                        {0.549	,3.07691E-13	},
                        {0.55	,3.16381E-13	},
                        {0.551	,2.9093E-13	},
                        {0.552	,3.22485E-13	},
                        {0.553	,3.24738E-13	},
                        {0.554	,2.72322E-13	},
                        {0.555	,2.87187E-13	},
                        {0.556	,3.09241E-13	},
                        {0.557	,2.70033E-13	},
                        {0.558	,2.64966E-13	},
                        {0.559	,2.66051E-13	},
                        {0.56	,2.99668E-13	},
                        {0.561	,3.07691E-13	},
                        {0.562	,3.08907E-13	},
                        {0.563	,3.01647E-13	},
                        {0.564	,3.4889E-13	},
                        {0.565	,2.97904E-13	},
                        {0.566	,2.80035E-13	},
                        {0.567	,2.70331E-13	},
                        {0.568	,2.59125E-13	},
                        {0.569	,2.64609E-13	},
                        {0.57	,2.82776E-13	},
                        {0.571	,2.75421E-13	},
                        {0.572	,3.25751E-13	},
                        {0.573	,2.9273E-13	},
                        {0.574	,3.407E-13	},
                        {0.575	,3.13568E-13	},
                        {0.576	,3.58546E-13	},
                        {0.577	,2.85625E-13	},
                        {0.578	,3.29816E-13	},
                        {0.579	,3.07274E-13	},
                        {0.58	,3.43132E-13	},
                        {0.581	,3.11756E-13	},
                        {0.582	,3.1234E-13	},
                        {0.583	,3.32439E-13	},
                        {0.584	,3.07226E-13	},
                        {0.585	,2.78616E-13	},
                        {0.586	,3.32975E-13	},
                        {0.587	,3.11685E-13	},
                        {0.588	,3.04353E-13	},
                        {0.589	,3.31783E-13	},
                        {0.59	,3.03531E-13	},
                        {0.591	,3.04055E-13	},
                        {0.592	,2.67816E-13	},
                        {0.593	,3.11351E-13	},
                        {0.594	,2.90024E-13	},
                        {0.595	,3.14486E-13	},
                        {0.596	,3.06094E-13	},
                        {0.597	,2.89857E-13	},
                        {0.598	,3.30329E-13	},
                        {0.599	,3.07024E-13	},
                        {0.6	,3.20923E-13	},
                        {0.601	,3.35407E-13	},
                        {0.602	,3.02839E-13	},
                        {0.603	,3.09277E-13	},
                        {0.604	,2.91085E-13	},
                        {0.605	,3.16215E-13	},
                        {0.606	,2.91562E-13	},
                        {0.607	,3.20697E-13	},
                        {0.608	,3.057E-13	},
                        {0.609	,3.27134E-13	},
                        {0.61	,3.31116E-13	},
                        {0.611	,2.8795E-13	},
                        {0.612	,3.02398E-13	},
                        {0.613	,2.98321E-13	},
                        {0.614	,3.40843E-13	},
                        {0.615	,3.03638E-13	},
                        {0.616	,3.31891E-13	},
                        {0.617	,3.19111E-13	},
                        {0.618	,3.39448E-13	},
                        {0.619	,3.2928E-13	},
                        {0.62	,3.41451E-13	},
                        {0.621	,3.28696E-13	},
                        {0.622	,3.33655E-13	},
                        {0.623	,3.29137E-13	},
                        {0.624	,2.70212E-13	},
                        {0.625	,3.27349E-13	},
                        {0.626	,3.39854E-13	},
                        {0.627	,3.16751E-13	},
                        {0.628	,3.06463E-13	},
                        {0.629	,3.05629E-13	},
                        {0.63	,3.44312E-13	},
                        {0.631	,3.54731E-13	},
                        {0.632	,3.31867E-13	},
                        {0.633	,2.98071E-13	},
                        {0.634	,3.25525E-13	},
                        {0.635	,3.22592E-13	},
                        {0.636	,3.06344E-13	},
                        {0.637	,2.62928E-13	},
                        {0.638	,3.20113E-13	},
                        {0.639	,2.98393E-13	},
                        {0.64	,3.38972E-13	},
                        {0.641	,3.23486E-13	},
                        {0.642	,3.34036E-13	},
                        {0.643	,3.03268E-13	},
                        {0.644	,2.8826E-13	},
                        {0.645	,3.13568E-13	},
                        {0.646	,2.79653E-13	},
                        {0.647	,3.06213E-13	},
                        {0.648	,3.30842E-13	},
                        {0.649	,3.7024E-13	},
                        {0.65	,3.33261E-13	},
                        {0.651	,3.31461E-13	},
                        {0.652	,3.46458E-13	},
                        {0.653	,3.49748E-13	},
                        {0.654	,3.17848E-13	},
                        {0.655	,3.17562E-13	},
                        {0.656	,3.02505E-13	},
                        {0.657	,2.91812E-13	},
                        {0.658	,3.08573E-13	},
                        {0.659	,2.86424E-13	},
                        {0.66	,3.01301E-13	},
                        {0.661	,3.38757E-13	},
                        {0.662	,3.58725E-13	},
                        {0.663	,3.17097E-13	},
                        {0.664	,3.21043E-13	},
                        {0.665	,3.04568E-13	},
                        {0.666	,2.90084E-13	},
                        {0.667	,2.87378E-13	},
                        {0.668	,3.03578E-13	},
                        {0.669	,3.38304E-13	},
                        {0.67	,3.08931E-13	},
                        {0.671	,3.78358E-13	},
                        {0.672	,3.37672E-13	},
                        {0.673	,3.24249E-13	},
                        {0.674	,2.83182E-13	},
                        {0.675	,2.76566E-13	},
                        {0.676	,3.17037E-13	},
                        {0.677	,2.88749E-13	},
                        {0.678	,3.14605E-13	},
                        {0.679	,2.76768E-13	},
                        {0.68	,3.21054E-13	},
                        {0.681	,3.17848E-13	},
                        {0.682	,2.93696E-13	},
                        {0.683	,3.6267E-13	},
                        {0.684	,3.09277E-13	},
                        {0.685	,2.81525E-13	},
                        {0.686	,3.00002E-13	},
                        {0.687	,2.94328E-13	},
                        {0.688	,2.7734E-13	},
                        {0.689	,2.79367E-13	},
                        {0.69	,3.11661E-13	},
                        {0.691	,2.69794E-13	},
                        {0.692	,3.24285E-13	},
                        {0.693	,3.0576E-13	},
                        {0.694	,3.51191E-13	},
                        {0.695	,2.90453E-13	},
                        {0.696	,3.00372E-13	},
                        {0.697	,3.32057E-13	},
                        {0.698	,3.44682E-13	},
                        {0.699	,3.25167E-13	},
                        {0.7	,3.18158E-13	}};
                
        
        
        // select which one to use
        double[][] ret = James;//PolyFitFails;
        
        
        
        if (false) {
            // return generated data
            
            // make the new array
            ret = new double[100][2];

            // fill the array
            for(int i=0; i < 100; i++) {
                ret[i][0] = (i-50)/100;
                ret[i][1] = 1e-12*Math.exp( ret[i][0] / 0.05 - 1);
            }
        }
        
        
        // plot the data?
        if (PlotData) {
            // make a new XYChart to view I(V)
            m_IV_Chart = new iC_ChartXY("develop - I(V)", 
                                    "Voltage [V]", "Current [A]",
                                    false  /*legend*/,
                                    640, 480);
            
            // style for MeasureOV
            SeriesIdentification IV_Series = m_IV_Chart.AddXYSeries("IV",
                    0, false, true, m_IV_Chart.LINE_NONE, m_IV_Chart.MARKER_CIRCLE);
            
            // set number formats
            m_IV_Chart.getYAxis(0).setNumberFormatOverride(new DecimalFormat("##0.#####E0"));
            
            // show zero lines
            m_IV_Chart.ShowZeroLines(true, true);
            
            // add the data
            for (int i=0; i<ret.length; i++) {
                m_IV_Chart.AddXYDataPoint(IV_Series, ret[i][0], ret[i][1]);
            }
            
            
        }
        
        
        return ret;
    }//</editor-fold>
}
