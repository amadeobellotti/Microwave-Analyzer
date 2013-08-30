// TODO 4* add method to set constant values (:page:meas:swe:cons:smu:sour and :compl
// TODO 3* add method to set 
// TODO 2* reset AutoCalibration in Close if it was set to off in the script
// TODO 2* check usage of SendViaGPIB versus WaitAndSendViaGPIB

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
 * other characteristic. We would appreciate acknowledgement if the software
 * is used. The publication introducing this program is planned to be published
 * in 2011/12 with K. P. Pernstich as first author.
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

package icontrol.instruments;

import icontrol.AutoGUIAnnotation;
import icontrol.IcontrolView;
import icontrol.iC_Annotation;
import icontrol.iC_ChartXY;
import icontrol.iC_ChartXY.SeriesIdentification;
import icontrol.instruments.Device.CommPorts;
import java.awt.Toolkit;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;
import javax.script.ScriptException;
import javax.swing.JOptionPane;


/**
 * This class implements functionality to communicate with an Agilent 4155
 * Semiconductor Parameter Analyzer. It was tested with a 4155C but should also
 * work with 4155A/B/C and a 4156B/C. It uses the SCPI commands and ASCII transfer.
 *
 * <h3>Instrument Commands:</h3>
 * <ul>
 *  <li>{@link #AutoCalibration(boolean) }
 *  <li>{@link #ButtonStart() }
 *  <li>{@link #ButtonStop() }
 *  <li>{@link #DisplayUpdate(boolean) }
 *  <li>{@link #Measure(String, String, String, String) }
 *  <li>{@link #PlotData(String, String,boolean,boolean) }
 *  <li>{@link #SaveData(String, String, boolean) }
 *  <li>{@link #configDisplayAxis(String, String, String) }
 *  <li>{@link #configMeasurementMode(String) }
 *  <li>{@link #configSMU(int, String, String, String, String) }
 *  <li>{@link #configSweep(String, boolean, String, float, float, float, float, float, float) }
 *  <li>{@link #configVMU(int, String, String) }
 * </ul><p>
 *
 * <h3>Some peculiarities of this Instrument-Class:</h3>
 * <ul>
 *  <li>Because measurements can take a few 10's of seconds, and because the 4155
 *      performs automatic calibrations from time to time, it was necessary to
 *      implement methods that check the status of the 4155 and if it is ready
 *      to receive commands. This functionality has been implemented in the
 *      methods <code>WaitAndSendViaGPIB</code> and <code>WaitAndQueryViaGPIB</code>,
 *      respectively, in <code>WaitUntilReady</code> and <code>isReady</code>. It
 *      is advisable to use these wrapper methods to communicate with the 4155.
 * <li> Because there is the need to wait until the 4155 becomes ready to receive
 *      commands, is is possible to define a default time-out for these methods.
 *      The default time-out value can be set in iC.properties, which is also
 *      accessible without having to recompile the iC program.
 * <li> The maximum number of points that can be measured in sweep mode is 1001, and
 *      the GPIB buffer size to read the response has been set 
 *      to 150000 to accommodate such responses from the 4155. If for some reason
 *      the 4155's response becomes longer, an error is generated, and the GPIB buffer
 *      size needs to be increased (or implement a method that reads twice from GPIB
 *      without sending a command first).
 * <li> The 4155 is a delicate Instrument to handle, which is why a
 *      <code>checkErrorQueue</code> method is implemented. It is recommended to use
 *      it wisely, and better once to often than once to few.
 * </ul><p>
 *
 * @author KPP (Kurt Pernstich: pernstich@alumni.ethz.ch)
 * @version 0.4
 */

// promise that this class supports GPIB communication
@iC_Annotation( CommPorts=CommPorts.GPIB,
                InstrumentClassName="Agilent 4155")
public class Agilent4155 extends Device {

    ///////////////////
    // member variables

    /**
     * The Logger for this class. Note that the Logger Level is set in the
     * constructor of the <code>Device</code> class.
     */
    private static final Logger m_Logger = Logger.getLogger("iC.Instruments.Agilent4155");


    /** Allowed Measurement Modes. Must be specified here in capital letters. */
    private static final List<String> MEASUREMENT_MODES =
            Arrays.asList("SWEEP", "SAMPLING", "QSCV");

    /** Allowed modes for SMUs. Must be specified here in capital letters. */
    private static final List<String> SMU_MODES =
            Arrays.asList("V", "I", "VPULSE", "IPULSE", "COMMON");

    
    /** Allowed functions for SMUs. Must be specified here in capital letters. */
    private static final List<String> SMU_FUNCTIONS =
            Arrays.asList("CONSTANT", "VAR1", "VAR2", "DISABLE");



    
    /**
     * Overridden <code>Close</code> method that puts the Instrument in a
     * defined state and clears the interface, just in case.
     *
     * @throws IOException re-thrown from <code>SendViaGPIB</code>
     * @see Device#Close
     */
    // <editor-fold defaultstate="collapsed" desc="Close()">
    @Override
    public void Close() throws IOException {

        // using GPIB do ...
        if (m_UsedCommPort == CommPorts.GPIB) {

            // don't use predefined functions because they use WaitAndSendXYZ
            // which checks m_StopScripting, which is always set at this point
            // while the 4155 might just not be ready to receive a command yet
            // (but will be in 1 sec or so).
            // just send the commands

            // re-enable auto calibration
            //SendViaGPIB("cal:auto on");
            
            // re-enable auto-update of the display
            //SendViaGPIB(":display:window:state 1");

            // clear interface
            SendViaGPIB("*CLS");
        }
    }//</editor-fold>
			


    /**
     * Extra initialization of the Agilent 4155 after establishing the
     * IO Connection via GPIB. It invokes the base class' <code>Open</code>, and
     * sets the data format to ascii.<p>
     *
     * The description of the parameters and exceptions was copied from the
     * implementation in the super class.
     * See {@link icontrol.instruments.Device#OpenGPIB(int) } for details.<p>
     *
     * @throws IOException when an sending / receiving data caused a GPIB error
     * @throws ScriptException when the response to a *IDN? query does not
     * match the expected result; bubbles up from <code>checkIDN</code>.
     */
    // <editor-fold defaultstate="collapsed" desc="OpenGPIB()">
    @Override
    public void Open()
            throws IOException, ScriptException {

        // invoke the Open method of the base class
        super.Open();

 
        // use Ascii format to transfer data to and from the 4155
        // see manual page 5-32
        try {
            WaitAndSendViaGPIB(":Format:Data Ascii");

        } catch (InterruptedException ex) {
            // it is safe to ignore this Exception because there is no need
            // to init Ascii transfer after the user pressed the Stop button
        }
    }//</editor-fold>


    /////////////////
    // helper methods


    /**
     * Waits until the 4155 is ready to receive a command, and then sends
     * the command. A maximum wait time can be specified, and in the moment
     * it is set to infinity, so this method returns only when the 4155 is ready
     * to receive a command. The maximum wait time is specified in the
     * iC.properties resource.
     *
     * @param Message this String is sent to the Instrument
     *
     * @throws IOException re-thrown from <code>WaitUntilReady</code> or when
     * the 4155 did not become ready within the maximum wait time.
     *
     * @throws InterruptedException when the user pressed the stop button (the
     * <code>m_StopScripting</code> flag is set to true) while the 4155 was not
     * ready to receive a command (bubbles up from <code>WaitUntilReady</code>).
     *
     * @see #WaitUntilReady(int)
     *
     */
    // <editor-fold defaultstate="collapsed" desc="WaitAndSendViaGPIB">
    void WaitAndSendViaGPIB(String Message) 
            throws IOException, InterruptedException {

        // get default maximum wait time from the iC properties
        int WaitTime = m_iC_Properties.getInt("Agilent4155.MaxWaitTime", 0);

        // wait max. WaitTime-sec until the 4155 becomes ready
        boolean ans = WaitUntilReady(WaitTime);

        if (ans != true) {
            // throw an IOException
            String str = "The Agilent 4155 did not accept commands for the last ";
            str += Integer.toString(WaitTime) + " sec.\n";

            throw new IOException(str);
        }

        // the 4155 is ready to receive a command, so send it
        SendViaGPIB(Message);
    }//</editor-fold>



    /**
     * Waits until the 4155 is ready to receive a command, and then queries
     * the command. The maximum time this method waits for the 4155 to become
     * ready is specified in the iC.properties resource.
     *
     * @param Message this String is sent to the Instrument
     *
     * @return The response of the Instrument
     *
     * @throws IOException re-thrown from <code>WaitUntilReady</code> or when
     * the 4155 did not become ready within a certain time, typically 50 sec.
     *
     * @throws InterruptedException when the user pressed the stop button (the
     * <code>m_StopScripting</code> flag is set to true) while the 4155 was not
     * ready to receive a command (bubbles up from <code>WaitUntilReady</code>).
     *
     */
    // <editor-fold defaultstate="collapsed" desc="WaitAndQueryViaGPIB">
    String WaitAndQueryViaGPIB(String Message) 
            throws IOException, InterruptedException {

        // get default maximum wait time from the iC properties
        int WaitTime = m_iC_Properties.getInt("Agilent4155.MaxWaitTime", 0);

        // wait max. WaitTime-sec until the 4155 becomes ready
        boolean ans = WaitUntilReady(WaitTime);

        if (ans != true) {
            // throw an IOException
            String str = "The Agilent 4155 did not accept commands for the last ";
            str += Integer.toString(WaitTime) + " sec.\n";

            throw new IOException(str);
        }

        // the 4155 is ready to receive a command, so query it
        return QueryViaGPIB(Message);
    }//</editor-fold>



    /**
     * Checks the status of the 4155 and returns true if the Instrument is ready
     * to receive a command (= the measurement is done).<p>
     * 
     * The GPIB answer of the 4155 comes typically instantaneous, but can also be
     * delayed if the 4155 is pulsing, updating the display (which may take some
     * seconds), or calibrating (which takes 45 sec), at least in some older 4155B's.
     * Any read function, can therefore cause a GPIB Time-Out. This method tries
     * to get an answer if the Instrument is ready to receive
     * a command by reading the Operation Status Register (p. 3-10).
     * 
     * @return true if the measurement is done and the 4155 is ready to receive a GPIB command
     *
     * @throws IOException re-thrown from <code>QueryViaGPIB</code>
     * or <code>setTimeout</code>
     */
    // <editor-fold defaultstate="collapsed" desc="isReady()">
    private boolean isReady() throws IOException {

        // local variables
        String ans = "";
        boolean ret = false;

        // if GPIB is the chosen protocol, do ...
        if (m_UsedCommPort == CommPorts.GPIB) {

            // remember present GPIB Time-Out
            //int TimeOut = m_JPIB_Device.getTimeout();

            // set new Time-Out in ms
            //m_JPIB_Device.setTimeout(500);

            // query the Operation Status Register (see manual page 3-10)
            try {
                ans = QueryViaGPIB("status:operation:condition?");

            } catch (IOException ex) {
                // some IO error occurred, most likely a Time-Out, in which case
                // the instrument is apparently not ready to receive a command

                // so restore the original Time-Out
                //m_JPIB_Device.setTimeout(TimeOut);

                // and return false
                return false;
            }

            // the 4155 responded, so let's check if it is ready to receive a
            // command. If bit 8 is set, the 4155 is in standby, hence, the
            // measurement is done. Refer to pages 3-11 and 5-293 in the manual
            int dummy = 0;
            try {
                // convert to integer
                dummy = getInteger(ans);

            } catch (ScriptException ex) {
                // conversion failed, so 4155 appears to not to be ready
                dummy = 0;
            }

            // check bit 8
            if ( (dummy & 65279) == 1) {
                // bit 8 is set, hence, the Instrument is ready
                ret = true;
            } else {
                ret = false;
            }

            // restore the original Time-Out
            //m_JPIB_Device.setTimeout(TimeOut);

            // return
            return ret;
        }

        // no supported communication protocol has been selected
        // this should not be able to occur, but if it does, the instrument
        // is obviously not ready to receive a command
        return false;
    }//</editor-fold>


    /**
     * Queries the head of the 4155's Error Queue. The error is removed from the
     * error queue. This method simply returns when no error was found in the
     * queue or throws a ScriptException with the Error Number and Message if
     * an error occurred. It also returns if in NoCommunicationMode.
     * 
     * @param Message This String is appended to the Error Message in case an
     * error occurred.
     *
     * @throws ScriptException if 1) the error queue could not be queried (when an
     * IOException occurred in <code>WaitAndQueryViaGPIB</code>, or 2) the returned error
     * number from the 4155 could not be converted into an Integer (should never
     * happen unless GPIB transmission introduces an error), or 3) an error was
     * found in the error queue.
     */
    // <editor-fold defaultstate="collapsed" desc="check Error Queue">
    private void checkErrorQueue(String Message) 
            throws ScriptException {

        // TODO 2* check all errors in the queue

        // return without indicating an error when in NoCommunicatin Mode
        if (inNoCommunicationMode())
            return;
        
        String ans = "";
        try {
            // query the error queue
            ans = WaitAndQueryViaGPIB(":system:error?");

        } catch (IOException ex) {
            String str = "Could not query the error queue of the Agilent 4155 (:system:error?).\n";
            str += ex.getMessage() + "\n";

            // log event
            m_Logger.severe(str);

            throw new ScriptException(str);
        } catch (InterruptedException ex) {

            String str = "Could not query the error queue of the Agilent 4155\n";
            str += "because the user pressed the stop button.\n";

            // log event
            m_Logger.info(str);

            throw new ScriptException(str);
        }
        
        // make a pattern and matcher
        Pattern p = Pattern.compile("([+-]?\\d*),\"(.*)\"");
        Matcher m = p.matcher(ans);
        
        // check if a match is found
        if ( !m.matches() ) {
            String str = "Could not interpret the answer of the 4155 upon\n";
            str += "reading the error queue.\n";

            // log event
            m_Logger.severe(str);

            throw new ScriptException(str);
        }


        // convert error number to an integer
        int ErrorNumber = getInteger( m.group(1) );

        // extract the error message (remove the "")
        String ErrorMessage = m.group(2);

        // throw an error if an error was found in the error queue and beep
        if (ErrorNumber != 0) {
            // beep
            Toolkit.getDefaultToolkit().beep();
            
            String str = "The Error Queue of the Agilent 4155 contained an error.\n"
                + "Error Number: " + ErrorNumber + "\n"
                + "Error Message: " + ErrorMessage + "\n"
                + (Message.isEmpty() ? "" : "Additional Message: " + Message);

            // log event
            m_Logger.severe(str);

            throw new ScriptException(str);
        }
    }//</editor-fold>

    /**
     * Waits until the 4155 is ready to receive a GPIB command (= the measurement
     * is done, the 4155 is not calibrating, or the 4155 is not otherwise busy with
     * some task) or the maximum wait time has been reached. The method also
     * exits and returns false if scripting has been stopped. This method is also
     * interrupted when scripting is paused.<p>
     *
     * The GPIB answer of the 4155 comes typically instantaneous, but can also be
     * delayed if the 4155 is pulsing, updating the display (which may take some
     * seconds), or calibrating (which takes 45 sec). Any read function, can
     * therefore cause a GPIB Time-Out. This method sets the Time-Out of the GPIB
     * bus to 0.5 sec, tries to get an answer if the Instrument is ready to receive
     * a command by reading the Operation Status Register (p. 3-10). Once the
     * 4155 is ready or the maximum wait time has been reached, the previous GPIB
     * Time-Out is restored.
     *
     * @param MaxWaitTime Maximum wait time in seconds. If <code>MaxWaitTime</code>
     * is 0 then this method waits infinitely long for the 4155 to become ready.
     *
     * @return true if the measurement is done and the 4155 is ready to receive
     * a GPIB command. Also returns true if in No-Communication-Mode. Returns
     * false otherwise.
     *
     * @throws IOException re-thrown from <code>QueryViaGPIB</code>
     * or <code>setTimeout</code>
     *
     * @throws InterruptedException when the user pressed the stop button (the
     * <code>m_StopScripting</code> flag is set to true) while the 4155 was not
     * ready to receive a command.
     *
     * @see Device#inNoCommunicationMode
     */
    // <editor-fold defaultstate="collapsed" desc="WaitUntilReady()">
    private boolean WaitUntilReady(int MaxWaitTime) 
            throws IOException, InterruptedException {

        // local variables
        String  ans = "";
        boolean ret = false;

        // for beeping to alert the user
        long    BeepTic = System.currentTimeMillis();
        double  lastBeep = 0;

        // when this method needs to wait longer than the specified time in
        // milliseconds then a status message is displayed to the user
        final long DisplayStatusMessageDelayTime = 750;


        // always return true (for sucess) if in No-Communication-Mode
        // or if in SyntaxCheck Mode
        if (inNoCommunicationMode())
            return true;
        

        // if GPIB is the chosen protocol, do ...
        if (m_UsedCommPort == CommPorts.GPIB) {
            // flag that stores when the first part of the status message to the
            // user is shown when the response of the 4155 took longer than 250 ms
            boolean DisplayWaitTime = false;

            // remember present GPIB Time-Out
            // set new Time-Out in ms

            // remember the current time
            long tic = System.currentTimeMillis();

            while ( System.currentTimeMillis() - tic < MaxWaitTime*1000 ||
                    MaxWaitTime == 0 ) {

                // check if scripting has been paused
                isPaused(true);

                // query the Operation Status Register (see manual page 3-10)
                try {
                    ans = QueryViaGPIB("status:operation:condition?");
                    // debug: display in StatusDisplay
                    //m_GUI.DisplayStatusMessage(ans+"\n");

                } catch (IOException ex) {
                    // some IO error occurred, most likely a Time-Out, in which case
                    // the instrument is apparently not ready to receive a command,
                    // so ignore this Exception and try again
                }

                // the 4155 responded, so let's check if it is ready to receive a
                // command. Refer to pages 3-11 and 5-293 in the manual
                int dummy;
                try {
                    // convert to integer
                    dummy = getInteger(ans);

                } catch (ScriptException ex) {
                    // conversion failed, so 4155 appears to not to be ready
                    dummy = 0xFFFF;
                }

                // check bits
                int BitsToCompare = 0;  // = 65535;
                //BitsToCompare = 128;  // bit 8=true: Instrument is busy
                BitsToCompare += 16;    // bit 4=true: Instrument is measuring
                BitsToCompare += 1;     // bit 0=true: calibration in progress
                if ( (dummy & BitsToCompare) == 0) {
                    // Instrument is ready, so remember this
                    ret = true;

                    // and return from this method
                    break;
                }

                // Display a Status Message that the 4155 is not ready if it did
                // not respond within, say, 250 ms.

                if (    DisplayWaitTime == false &&
                        System.currentTimeMillis() - tic > DisplayStatusMessageDelayTime) {
                    IcontrolView.DisplayStatusMessage("Need to wait for Agilent 4155 (OSR " + ans + "): ");
                    DisplayWaitTime = true;
                }

                // check if scripting has been stopped
                // when called from Close(), m_StopScripting is set to true
                // but it would still be nice to execute commands in Close(),
                // so put it at the end to give the 4155 a chance to respond.
                if (m_StopScripting) {
                    throw new InterruptedException("Agilent 4155: WaitUntilReady was interrupted by the user.\n");
                }

                // Beep every 10 seconds after waiting for 50 sec
                // to alert the user that 4155 did not respond
                double deltaT = (System.currentTimeMillis() - BeepTic ) / 1000;
                if ( (deltaT > 50) && (deltaT > lastBeep + 10) ) {
                    // remember current time of the last beep
                    lastBeep = deltaT;

                    // beep
                    Toolkit.getDefaultToolkit().beep();
                }

                
                try {
                    Thread.sleep(250);
                } catch (InterruptedException ex) {}
            }

            // restore the original Time-Out

            // Display the total time waited for the 4155 to become ready if it did
            // not respond within, say, 250 ms.
            if (DisplayWaitTime) {
                IcontrolView.DisplayStatusMessage(
                        String.format(Locale.US, "%.1f sec\n", (System.currentTimeMillis() - tic)/1000.0), false);
            }

            // return
            return ret;
        }

        // no supported communication protocol has been selected
        // this should not be able to occur, but if it does, the instrument
        // is obviously not ready to receive a command
        return false;
    }//</editor-fold>
    

    /**
     * Performs a Syntax-Check for the correct Name. A correct name can contain
     * 6 alphanumeric letters and must start with an alphabet character.
     *
     * @param Name The Name to check
     * @throws DataFormatException If Syntax-Check failed.
     */
    // <editor-fold defaultstate="collapsed" desc="check Name">
    private void checkName(String Name)
            throws DataFormatException {

        // Syntax Check for length and first character
        if ( Name.isEmpty() || Name.length() > 6 || Name.matches("^[^a-zA-Z].*")) {
            String str = "The Name '" + Name + "' is not valid.\n"
                    + "A correct name can contain up to 6 alphanumeric characters\n"
                    + "and must start with an alphabet character.";
            throw new DataFormatException(str);
        }
    }//</editor-fold>

    /**
     * Performs a Syntax-Check for the correct SMU Mode.
     *
     * @param Mode The SMU mode to check
     * @throws DataFormatException If Syntax-Check failed.
     */
    // <editor-fold defaultstate="collapsed" desc="check SMU mode">
    private void checkSMU_Modes(String Mode) throws DataFormatException {

        // Syntax Check for correct SMU Mode
        if ( !SMU_MODES.contains(Mode.toUpperCase()) ) {
            String str = "The SMU Mode '" + Mode + "' is not valid.";
            str += "Please select a SMU mode from:\n " + SMU_MODES.toString() + ".\n";
            throw new DataFormatException(str);
        }
    }//</editor-fold>

    /**
     * Performs a Syntax-Check for the correct SMU Function. The Functions for
     * the VMUs are the same.
     *
     * @param Function The SMU Function to check
     * @throws DataFormatException If Syntax-Check failed.
     */
    // <editor-fold defaultstate="collapsed" desc="check SMU Function">
    private void checkSMU_Function(String Function)
            throws DataFormatException {

        // Syntax Check for correct Heater Range
        if ( !SMU_FUNCTIONS.contains(Function.toUpperCase()) ) {
            String str = "The SMU Function '" + Function + "' is not valid.";
            str += "Please select a SMU/VMU Function from:\n " + SMU_FUNCTIONS.toString() + ".\n";
            throw new DataFormatException(str);
        }
    }//</editor-fold>


    /////////////////
    // Script methods


    /**
     * Loads a measurement setup from the 4155'es internal memory or from the
     * disk drive. This method returns if it is interrupted by the user
     * before the 4155 becomes ready to receive a command.<p>
     *
     * Remark: Make this method public if you like to access it from a script.<p>
     *
     * Remark: If a Syntax-Check would be added to this method, then it should
     * be called from <code>Measure</code> to check this parameter there too.
     *
     * @param FileName has to be 'MEM1'-'MEM4' or 'xxx.mes', 'xxx.str', or
     * 'xxx.cst' see manual page 5-70
     */
    // <editor-fold defaultstate="collapsed" desc="Load Measurement Setup">
    @AutoGUIAnnotation(
        DescriptionForUser = "Load a measurement setup from internal memory or from the disk drive.",
        ParameterNames = "File Name",
        ToolTips = "Choose '1'-'4' to load from internal memory or 'xxx.MES', 'xxx.STR', or 'xxx.CST' to load from the disk drive")
    private void LoadMeasurementSetup(String FileName) throws IOException {


        // if GPIB is used ...
        if (m_UsedCommPort == CommPorts.GPIB) {
            
            // build the command string
            String str = "MMemory:Load:State 0,";

            // check if user selected to load from disk or internal memory
            if (    FileName.toUpperCase().contains(".MES") ||
                    FileName.toUpperCase().contains(".STR") ||
                    FileName.toUpperCase().contains(".CST")) {
                
                // user has selected to load a file from disk
                str += "'" + FileName + "','DISK'";
            } else {
                // user has selected to load from internal memory
                str += "'MEM" + FileName + "','MEMORY'";
            }

            try {
                // send it to the instrument
                WaitAndSendViaGPIB(str);

            } catch (InterruptedException ex) {/* do nothing when interrupted by the user */}
        }
    }//</editor-fold>


    /**
     * Sets whether or not the display of the 4155 is updated or not. Not updating
     * the 4155's display can speed up a measuring cycle significantly (up to
     * approximately 1-20 sec). This method returns if it is interrupted by the user
     * before the 4155 becomes ready to receive a command.
     *
     * @param UpdateDisplay if true, the 4155 updates it's display
     *
     * @throws IOException re-thrown from <code>WaitAndSendViaGPIB</code>
     */
    // <editor-fold defaultstate="collapsed" desc="Display Update">
    @AutoGUIAnnotation(
        DescriptionForUser = "Enable/Disable updating the display of the 4155.",
        ParameterNames = "Update Display",
        ToolTips = "When true the display is updated, when false, it's not updated.")
    public void DisplayUpdate(boolean UpdateDisplay) throws IOException {

        try {
            if (UpdateDisplay == true) {
                WaitAndSendViaGPIB(":display:window:state 1");
            } else {
                WaitAndSendViaGPIB(":display:window:state 0");
            }
        } catch (InterruptedException ex) {/* do nothing when interrupted by the user */}
    }//</editor-fold>


    /**
     * Enable/Disable Auto Calibration every 30 min. During a calibration, the
     * 4155 should be disconnected from the device-under-test (see manual page 5-6),
     * which I personally have never done because I was not aware of that.<p>
     * This method returns if it is interrupted by the user
     * before the 4155 becomes ready to receive a command.
     *
     * @param AutoCalibration if true, the 4155 performs an auto calibration every
     * 30 min.
     *
     * @throws IOException re-thrown from <code>WaitAndSendViaGPIB</code>
     */
    // <editor-fold defaultstate="collapsed" desc="Auto Calibration">
    @AutoGUIAnnotation(
        DescriptionForUser = "Enable/Disable Auto-Calibration of the 4155.",
        ParameterNames = "Update Display",
        ToolTips = "true/false turnes on/off Auto Calibration of the 4155.")
    public void AutoCalibration(boolean AutoCalibration) throws IOException {

        try {
            if (m_UsedCommPort == CommPorts.GPIB) {
                if (AutoCalibration == true) {
                    WaitAndSendViaGPIB("cal:auto on");
                } else {
                    WaitAndSendViaGPIB("cal:auto off");
                }
            }
        } catch (InterruptedException ex) {/* do nothing when interrupted by the user */}
    }//</editor-fold>


    /**
     * Retrieves the specified Data Variables from the 4155 and saves them in the
     * specified file in a text format. This method returns if it is interrupted by the user
     * before the 4155 becomes ready to receive a command.<p>
     *
     * During No-Communication-Mode, no data is saved, and the method just returns.
     *
     * @param DataVariables A comma-separated list of Data Variables that will be
     * retrieved form the 4155 and saved in the file. The Name of the Data
     * Variables must of course correspond exactly to the names specified in the
     * 4155. Extra white spaces are ignored, therefore the Data Variables must not
     * contain spaces (which is anyway not allowed by the 4155 as far as I know).
     *
     * @param FileExtension Specifies the extension that will be added to the 
     * filename. The path and filename are obtained from the GUI. If the
     * filename already exists, a new file with a counter is inserted between the 
     * FileName and the FileExtension.
     * 
     * @param DontSaveInvalidData If true, saving the data to the file stops as
     * soon as all data variables for one measurement point contained +9.91E+307
     * (which marks invalid data). This can occur, for instance, if the user
     * pressed 'Local' and then quickly 'Stop' at the 4155 after the script has
     * executed a <code>Measure</code> command. A Status message is shown to the
     * user in this case.
     * 
     * @return null if an error of any kind occurred or a structure similar to
     * a matrix that contains the saved data. This can be used in the successive
     * script command to, for instance, chart the data.
     *
     * @throws IOException This exception is bubbled-up from <code>WaitAndQueryViaGPIB</code>
     *
     * @throws ScriptException 1) When a specified data variable is not available from
     * the 4155 (not measured or measured and not stored (add it to the list)),
     * or 2) the response of the 4155 could not be converted into the expected number
     * format, or 3) the file could not be opened (IOException from FileWriter()), or
     * 4) an error occurred after reading the 4155's response, or 5) writing to the
     * file failed.
     *
     *
     * @see Agilent4155#WaitAndQueryViaGPIB(String)
     * @see Device#getInteger(String)
     *
     */
    // <editor-fold defaultstate="collapsed" desc="Save Data">
    @AutoGUIAnnotation(
        DescriptionForUser = "Saves the given Data Variables in a Text File.",
        ParameterNames = {"Data Variables", "File Extension", "Don't save invalid data"},
        ToolTips = {"Comma separated list of Data Variables defined in the 4155.", "", 
            "<html>If a measurement is interrupted, the 4155's buffer<br>contains invalid data. Check this box<br>to save only the valid data.</html>"},
        DefaultValues={"Id,Vd,Ig,Vg,Is", ".trans", "true"})
    public ArrayList<String>[] SaveData(String DataVariables, String FileExtension, boolean DontSaveInvalidData)
            throws IOException, ScriptException {

        // handle No-Communication-Mode separately, and just return
        if (inNoCommunicationMode())
            return null;
        

        // remove whitespaces from the DataVariables string
        DataVariables = DataVariables.replaceAll("\\s+", "");

        // split at the comma (',') into individual data variables
        String[] VarNamesToSave = DataVariables.split("[,]");

        // do nothing if no variables were specified
        if (VarNamesToSave.length == 0) {
            // display a status message
            IcontrolView.DisplayStatusMessage("Agilent4155.SaveData: No Data Variables were specified.\n");

            // do nothing and return
            return null;
        }

        /////////////////////////
        // when GPIB was selected
        if (m_UsedCommPort == CommPorts.GPIB) {
            String dummy;


            // Get DataVariable Names from 4155
            String VarNamesAvailable;
            try {    
                VarNamesAvailable = WaitAndQueryViaGPIB("data:cat?");

            } catch (InterruptedException ex) {
                // return if interrupted by the user
                return null;
            }



            // Check if all Names are in List of 4155
            for (String var : VarNamesToSave) {
                if ( !VarNamesAvailable.contains(var) ) {
                    // requested data variable name not found, so throw an exception
                    String str = "The Data Variable \"" + var + "\" is not available in the current measurement setup.\n";
                    str += "Check the spelling, or add the variable to the list.\n";
                    str += "(Check 4155's List-view to see the available data variables.\n";

                    throw new ScriptException(str);
                }
            }

            // get the number of data points
            //String dummy = ":data:points? '" + VarNamesToSave[0] + "'";
            //int NrOfPoints = getInteger( WaitAndQueryViaGPIB(dummy) );

            // make a new ArrayList that will 'cache' the data read from the 4155
            // the 'width' is equal to the number of data variables to save
            // the 'length' is (variable) and equals the number of data points to save
            @SuppressWarnings("unchecked")
            ArrayList<String> Cache[] = (ArrayList<String>[]) new ArrayList[ VarNamesToSave.length ];


            // get the data: for-each data variable to save
            // save it in a 'cache' variable (sort of a matrix)
            for (int i=0; i<VarNamesToSave.length; i++) {

                // thus far, Cache[i] equals to null, so make new ArrayLists
                // because <String> is chosen here, the @SuppressWarnings("unchecked")
                // above is okay.
                Cache[i] = new ArrayList<String>();

                // receive the data
                String DataAsString;
                try {
                    dummy = ":data? '" + VarNamesToSave[i] + "'";
                    DataAsString = WaitAndQueryViaGPIB(dummy);


                    // check the error queue to ensure that all measurement
                    // points could be read (GPIB read buffer size) and no
                    // -410,"Query INTERRUPTED" error occurred
                    try {
                        checkErrorQueue("");

                    } catch (ScriptException ex) {
                        // re-throw with an extended description
                        String str = "Your data could not be saved !!\n";
                        str += "Very likely the GPIB read buffer size is too small.\n";
                        str += "Decrease the number of points measured or\n";
                        str += "increase the GPIB buffer size in class GPIB_NI.\n\n";
                        str += "The returned error message from the 4155 is:\n";
                        str += ex.getMessage();

                        throw new ScriptException(str);
                    }

                } catch (InterruptedException ex) {
                    // return if interrupted by the user
                    return null;
                }

                // split at \n
                String[] splitted = DataAsString.split("[,]");

                // add DataVariable Name as the first element of 'cache'
                Cache[i].add(VarNamesToSave[i]);
                
                // store the data in the 'cache' variable
                Cache[i].addAll(Arrays.asList(splitted));
            }


            // get the filename
            String FileName = m_GUI.getFileName(FileExtension);
        
            // open the file for writing
            BufferedWriter file;
            try {
                file = new BufferedWriter( new FileWriter(FileName) );

            } catch (IOException ex) {
                String str = "Could not open the file\n" + FileName + "\n"
                    + "Make sure it is not a directory and that it can be opened or created.\n";
                throw new ScriptException(str);
            }

            try {
                // add date and time in the file's Header as a comment
            
                // reformat the current date and time
                SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd_HHmmss");
                String DateString = sdf.format(Calendar.getInstance().getTime());

                // write to file
                file.write("% iC: date and time measured:" + DateString);
                file.newLine();
            

                // now write the data line by line
                int MeasurementInterrupted;
                for (int row=0; row<Cache[0].size(); row++) {

                    dummy = "";
                    MeasurementInterrupted = 0;

                    // add all variables to save
                    for (int col=0; col<VarNamesToSave.length; col++) {
                        // add \t if it's not the first entry in the line
                        if (col>0)
                            dummy += "\t";

                        // add the data
                        dummy += Cache[col].get(row);

                        // when a measurement gets interrrupted, not all data points
                        // contain reasonable data but +9.91E+307
                        // remember that any data variable contained this number
                        if (Cache[col].get(row).equals("+9.91E+307"))
                            MeasurementInterrupted++;
                    }

                    // done writing to file when the measurement has been
                    // interrupted (by the user), hence, the answer of the
                    // 4155 was +9.91E+307
                    if (    DontSaveInvalidData &&
                            MeasurementInterrupted == VarNamesToSave.length) {
                        // display a status message
                        IcontrolView.DisplayStatusMessage("Warning from Agilent "
                                + "4155: Saved data has been truncated after all "
                                + "Data Variables contained +9.91E+307 (invalid "
                                + "data/measurement interrupted)!\n");

                        // and exit saving
                        break;
                    }

                    // write to file
                    file.write(dummy);
                    file.newLine();
                }

            } catch (IOException ex) {
                String str = "Could not save the data in file\n" + FileName + "\n"
                        + ex.getMessage();
                
                throw new ScriptException(str);
            }
            
            try {
                // close the file
                if (file != null)
                    file.close();
                
            } catch (IOException ex) {
                IcontrolView.DisplayStatusMessage("Could not close the file " + FileExtension + "\n");
            }
            
            // display Status message
            IcontrolView.DisplayStatusMessage("Agilent 4155: Data saved in " +
                    FileExtension + "\n");

            // return the data
            return Cache;
        }

        // nothing was saved, so return null
        return null;
        
    }//</editor-fold>


    /**
     * Starts a measurement by 'virtually' pressing the 'Start" button on
     * the 4155.
     *
     * @throws IOException bubbles up from <code>WaitAndSendViaGPIB</code>
     * @throws ScriptException when the 4155 is not measuring after pressing
     * 'Start' (most likely due to a wrong channel setup (for instance power
     * compliance is set too high).
     */
    // <editor-fold defaultstate="collapsed" desc="Button Single">
    @AutoGUIAnnotation(
        DescriptionForUser = "Press the 'Start' button.",
        ParameterNames = {})
    public void ButtonStart() 
            throws IOException, ScriptException {

        /////////////////////////
        // when GPIB was selected
        if (m_UsedCommPort == CommPorts.GPIB) {

            // press the button
            try {
                WaitAndSendViaGPIB("Page:Scontrol:Single");
            } catch (InterruptedException ex) {
                // just return if interrupted by the user
                return;
            }


            /* Check for errors.
             *
             * It can happen that after pressing the Start Button remotely the
             * 4155 does not start measuring when, for instance, the current
             * compliance is set too high for the maximum output voltage. When
             * pressing the Start button maually, the 4155 beeps indicating an
             * error, but when pressing the Start button remotely, it doesn't beep.
             */
            checkErrorQueue("");
        }

    }//</editor-fold>


    /**
     * Stops a measurement by 'virtually' pressing the 'Stop" button on
     * the 4155.
     *
     * @throws IOException bubbles up from <code>WaitAndSendViaGPIB</code>
     */
    // <editor-fold defaultstate="collapsed" desc="Button Stop">
    @AutoGUIAnnotation(
        DescriptionForUser = "Press the 'Stop' button.",
        ParameterNames = {})
    public void ButtonStop() throws IOException {

        /////////////////////////
        // when GPIB was selected
        if (m_UsedCommPort == CommPorts.GPIB) {

            // press the button
            //WaitAndSendViaGPIB("Page:Scontrol:Stop");
            SendViaGPIB("Page:Scontrol:Stop");
        }
    }//</editor-fold>



     /**
     * Starts a measurement by 'virtually' pressing the 'Stress" button on
     * the 4155.
     *
     * Remark: Make this method public if you like to access it from a script.
     *
     * @throws IOException bubbles up from <code>WaitAndSendViaGPIB</code>
     */
    // <editor-fold defaultstate="collapsed" desc="Button Stress">
    @AutoGUIAnnotation(
        DescriptionForUser = "Press the 'Stress' button.",
        ParameterNames = {})
    private void ButtonStress() throws IOException {

        /////////////////////////
        // when GPIB was selected
        if (m_UsedCommPort == CommPorts.GPIB) {

            // press the button
            try {
                WaitAndSendViaGPIB("Page:Scontrol:stress");
            } catch (InterruptedException ex) {/* do nothing when interrupted by the user */}
        }
    }//</editor-fold>


    /**
     * Measures a transfer characteristic of a transistor.<br>
     * First, a measurement setup is loaded (from internal memory or from disk), then
     * the measurement is started, and after it is completed, the data is saved
     * into a text file. If this method is interrupted because the user pressed
     * the Stop button before the 4155 became ready to receive a command, then 
     * this method calls <code>ButtonStop</code> and returns.<p>
     * 
     * Note: All data variables that are to be saved are added to the List 
     * (button Disp/List on the 4155) to ensure that the 4155 saves them. If more
     * than 8 data variables are specified, the implemented automatic mechanism 
     * fails and the user needs to choose which data variables are automatically saved
     * (e.g. constant values) and add the remaining data variables manually. If more
     * than 8 data variables are specified, this method shows a message to the user
     * but continues to measure.
     *
     * This method performs a Syntax-Check.
     *
     * @param MeasurementSetup Specifies what measurement setup to loads: '0' does load nothing,
     * '1'-'4' loads from the internal memory (MEM1-MEM4), and other string loads from
     * disk and must contain an extension '.MES', '.STR' or '.CST'
     *
     * @param DataVariables see {@link Agilent4155#SaveData(String, String, boolean)}
     *
     * @param FileExtension This String is appended to the File Name
     *
     * @param CommandLine This String needs to be enclosed in double-quote (") and is
     * sent to <code>DispatchCommand</code> before the measurement is started. The result
     * of this command is converted to a String using the objects <code>toString</code>
     * method and appended to the File Name.
     * 
     * @return null if an error of any kind occurred or a structure similar to
     * a matrix that contains the saved data. This can be used in the successive
     * script command to, for instance, chart the data. The returned data is 
     * "passed on" from <code>SaveData</code>.
     *
     * @throws IOException bubbles up from <code>Agilent4155#LoadMeasurementSetup</code>, <code>ButtonStart</code>,
     * or <code>WaitUntilReady</code>.
     *
     * @throws ScriptException bubbles up from <code>SaveData</code> or from dispatching
     * the <code>CommandLine</code> or when an error occurred when writing the data
     * variables to the List from <code>checkErrorQueue</code>.
     *
     * @throws DataFormatException when Syntax-Check fails. The Syntax-Check includes
     * a simple check if <code>DataVariables</code> is not empty, and also if the
     * <code>CommandLine</code> can be interpreted (this is done by sending it to
     * <code>DispatchCommand</code> which performs a full Syntax-Check on the
     * <code>CommandLine</code>.
     *
     * @see Agilent4155#LoadMeasurementSetup(String)
     * @see Agilent4155#ButtonStart()
     * @see Agilent4155#WaitUntilReady(int)
     * @see Agilent4155#SaveData(String, String, boolean)
     */
    // <editor-fold defaultstate="collapsed" desc="Measure">
    @AutoGUIAnnotation(
        DescriptionForUser = "Loads a measurement setup, starts the measurement, and saves the data.",
        ParameterNames = {"Measurement Setup { [0,4] }", "Data Variables", "File Extension", "Additional Command Line"},
        DefaultValues = {"0", "Id, Vd, Ig, Vg, Is", ".trans", "\"\""},
        ToolTips = {"Choose '0' to skip loading, or choose '1'-'4' to load from internal memory or 'xxx.MES', 'xxx.STR', or 'xxx.CST' to load from the disk drive",
                    "Comma separated list of Data Variables defined (and recorded) in the 4155.",
                    "The File Extension (with or without '.').",
                    "<html>This Command Line is executed before the measurement is started,<br> and the result is appended to the File Name.<br>This String needs to be enclosed inside double-quotes (\") !!<br>Use with caution.</html>"})
    @iC_Annotation(MethodChecksSyntax=true)
    public ArrayList<String>[] Measure(String MeasurementSetup, String DataVariables,
                        String FileExtension, String CommandLine)
            throws  IOException, ScriptException, DataFormatException {

        // local variables
        String  ResultToAppend = "";

        // remove double-quotes from the beginning and end of the CommandLine
        CommandLine = CommandLine.replaceFirst("^\"", "").replaceFirst("\"$", "");


        ///////////////
        // Syntax-Check

        // if LoadMeasurementSetup would perform a Syntax-Check, it could be called now

        // chekc if any DataVariables are given
        if (DataVariables.isEmpty()) {

            // there must be some Variables to save
            String str = "The DataVariables must not be empty.\n";

            // throw the Exception
            throw new DataFormatException(str);
        }


        // Execute the Command Line
        // in Syntax-Check mode the returned object is null
        // when not in Syntax-Check mode, the object should be valid
        if ( !CommandLine.isEmpty() ) {

            // make a new Device object to call it's DispatchCommand method
            // see Remark in javadoc (How to write new Instrument-Classes)
            Device dev = new Device();

            Object obj = dev.DispatchCommand(CommandLine);

            // convert to String if returned object is valid
            if (obj != null) {
                ResultToAppend = obj.toString();
            } else {
                ResultToAppend = "";
            }
        }
        
        // ------
        // Check if more than 8 data variables are to be saved
        
        // remove whitespaces from the DataVariables string
        DataVariables = DataVariables.replaceAll("\\s+", "");

        // split at the comma (',') into individual data variables
        String[] VarNamesToSave = DataVariables.split("[,]");

        // check number of variables specified
        if ( VarNamesToSave.length > 8 && inSyntaxCheckMode() ) {
            // display a message
            String str = "You have specified to save more than 8 data variables\n"
                    + "(" + DataVariables + "). iC cannot automatically determine which\n"
                    + "data variables need to be added to the 4155's List (Disp/List).\n\n"
                    + "Data variables that are not displayed, or are not in this List, or are\n"
                    + "not defined as data variables for Markers might not be stored in the 4155.\n\n"
                    + "Please ensure that all data variables are stored in the 4155 so that iC\n"
                    + "can retrieve their value. It is highly recommended to inspect the saved file\n"
                    + "to ensure that proper values are saved.";
            
            JOptionPane.showMessageDialog(m_GUI.getComponent(), str,
                    "Too many data variables", JOptionPane.ERROR_MESSAGE, m_iC_Properties.getLogoSmall());
        }


        // return if in Syntax-check mode
        if (inSyntaxCheckMode())
            return null;



        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return null;



        // load the measurement setup if desired
        if ( !MeasurementSetup.startsWith("0") ) {
            LoadMeasurementSetup(MeasurementSetup);
        }

        /////////////////////////////////
        // add data variables to the List so that they are stored in the 4155
        if (VarNamesToSave.length <= 8) {
            
            // delete all variable names in the list
            SendViaGPIB(":PAGE:DISP:LIST:DEL:ALL");
            
            // check error queue
            checkErrorQueue("Could not clear the data variable list.\n");
            
            // add the variable names
            for (String str : VarNamesToSave) {
                SendViaGPIB(":PAGE:DISP:LIST:SEL '" + str + "'");
                
                // check error queue
                checkErrorQueue("Could not add data variable >" + str + "< to the data variable list.\n");
            }
        }
        

        ////////////////////////
        // Start the measurement
        ButtonStart();

        // Wait until done measuring, calibrating or an other situation that puts
        // the 4155 in a busy state
        try {
            WaitUntilReady(0);

        } catch (InterruptedException ex) {
            // the user pressed Stop, so press Stop on the 4155
            ButtonStop();

            // return when interrupted by the user
            return null;
        }


        ////////////////
        // save the file

        // append the result from executing the CommandLine
        if ( !ResultToAppend.isEmpty() )
            FileExtension += ".T" + ResultToAppend;

        // precede the FileExtension with a '.' if no '.' was included
        if ( !FileExtension.startsWith(".") )
            FileExtension = "." + FileExtension;


        // save the data (don't save invalid data)
        // TODO 3* add Agilent4155.TrunkateInvalidData to iC.properties
        ArrayList<String> Cache[] = SaveData(DataVariables, FileExtension, true);

        return Cache;
    }//</editor-fold>



    /**
     * Displays the two Data Variables in an X-Y plot. This command must follow
     * immediately to a <code>Measure</code>, <code>SaveData</code>, or 
     * <code>PlotData</code> script command because only those methods return
     * the required data structure.<p>
     * 
     * If the method is called with invalid parameters or a invalid data is
     * returned from the previous script command, nothing is plotted and the
     * method does not interrupt scripting.
     * 
     * @param XDataVariable The name of the data variable to use as the X-Axis
     * @param YDataVariable The name of the data variable to use as the Y-Axis
     * @return null if an error of any kind occurred or a structure similar to
     * a matrix that contains the saved data. This can be used in the successive
     * script command to, for instance, chart the data. The returned data is 
     * "passed on" from <code>SaveData</code>.
     */
    // <editor-fold defaultstate="collapsed" desc="Plot Data">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Plots the specified data variables in a XY chart<br>This command must immediately follow a Measure, SaveData, or PlotData script command.</html>",
        ParameterNames = {"X Data Variable", "Y Data Variable", "log X-axis", "log Y-axis"},
        DefaultValues = {"Vg", "Id"},
        ToolTips = {"", "", "If true, the ABSOLUTE values will be plotted on a log-scale.", 
            "If true, the ABSOLUTE values will be plotted on a log-scale."})
    public ArrayList<String>[] PlotData(String XDataVariable, String YDataVariable,
                                        boolean logX, boolean logY) {
        
        // check for the correct data type
        // TODO 1* is there a more elegant way of type checking (cannot use ArrayList<String>[])
        if ( !(m_LastReturnValue instanceof ArrayList<?>[]) ) {
            return null;
        } 
        
        // check size of array
        if ( ((ArrayList<?>[])m_LastReturnValue).length < 2) {
            // array is too short
            return null;
        }
        
        // check if of type String
        if ( !( ((ArrayList<?>[])m_LastReturnValue)[0].get(0) instanceof String ) ) {
            return null;
        }
        
        // use nice variable names
        @SuppressWarnings("unchecked")
        ArrayList<String>[] Data = (ArrayList<String>[])m_LastReturnValue;
        
        int IndexX = -1;
        int IndexY = -1;
        
        // find index for X/Y Data Variable
        // the first entry in each col contains the name of the DataVariable
        for (int i=0; i<Data.length; i++) {
            
            // X Data Variable?
            if (Data[i].get(0).equals(XDataVariable)) {
                IndexX = i;
            }
            
            // Y Data Variable?
            if (Data[i].get(0).equals(YDataVariable)) {
                IndexY = i;
            }
        }
        
        // were both indices found?
        if (IndexX < 0 || IndexY < 0) {
            // no Data Variables found, so return
            return Data;
        }
        
        // reserve space for data
        double x[] = new double[Data[0].size()-1];
        double y[] = new double[Data[0].size()-1];
        
        
        // convert string data to double
        try {
            for (int i=1; i<Data[0].size(); i++) {
                x[i-1] = getDouble(Data[IndexX].get(i));
                y[i-1] = getDouble(Data[IndexY].get(i));
                
                // log plot?
                if (logX) {
                    x[i-1] = Math.abs(x[i-1]);
                }
                
                if (logY) {
                    y[i-1] = Math.abs(y[i-1]);
                }
            }
        } catch (ScriptException ex) {
            // conversion into a double failed
            IcontrolView.DisplayStatusMessage("Warning: Agilent4155.PlotData: conversion into a double failed.");
            return Data;
        }
        
        
        ///////////////
        // do the chart
        
        // make the name of the chart
        String str = "Plot " + (logY ? "|"+YDataVariable+"|" : YDataVariable) + "("
                             + (logX ? "|"+XDataVariable+"|" : XDataVariable) + ")";
        
        // make a new XYChart object
        iC_ChartXY Chart = new iC_ChartXY(str, XDataVariable, YDataVariable,
                                 false  /*legend*/,
                                 640, 480);
        
        // add a new trace (series)
        SeriesIdentification Series1 = Chart.AddXYSeries("Sweep1", 0, false, true,
                                             Chart.LINE_SOLID, Chart.MARKER_CIRCLE);
        
        // set lin/log scale of the axis
        Chart.LogXAxis(logX);
        Chart.LogYAxis(logY);
        
        // add the datapoints to the graph
        Chart.AddXYDataPoint(Series1, x, y);

        // return the result for the next script command
        return Data;        
    } //</editor-fold>
    
    
    /**
     * Configures the SMUs.<p>
     *
     * This method performs a Syntax-Check.
     *
     * @param SMU_Number The number of the SMU to configure
     * @param Vname The variable name of the voltage
     * @param Iname The variable name of the current
     * @param Mode The operation mode of this SMU
     * @param Function The Function of this SMU. The function 'Disable' deletes
     * the settings of the SMU.
     * 
     * @throws IOException re-thrown from {@link Device#SendViaGPIB}
     * @throws DataFormatException when the Syntax Check failed.
     * @throws ScriptException when the error queue contains an error (bubbles
     * up from <code>checkErrorQueue</code>).
     */
    // <editor-fold defaultstate="collapsed" desc="config SMU">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Configure a SMU</html>",
        ParameterNames = {"SMU# { [1,4] }", "Vname", "Iname", "Mode", "Function"},
        DefaultValues = {"1", "V1", "I1", "V", "Var1"},
        ToolTips = {"", "", "", "Can be V, I, Vpulse, Ipulse, Common",
                    "Can be Constant, Var1, Var2, Disable"})
    @iC_Annotation(  MethodChecksSyntax = true )
    public void configSMU(  int SMU_Number,
                            String Vname, String Iname,
                            String Mode, String Function)
           throws IOException, DataFormatException, ScriptException {


        ///////////////
        // Syntax-check

        // check SMU number
        if (SMU_Number < 1 || SMU_Number > 4) {
            String str = "The SMU Number must be between 1 and 4.";
            throw new DataFormatException(str);
        }

        // check SMU Mode
        checkSMU_Modes(Mode);

        // check SMU Function
        checkSMU_Function(Function);

        // disregard Vname, Iname is function is DISABLE
        if ( !Function.toUpperCase().equals("DISABLE")) {
            // check Vname
            checkName(Vname);

            // check Iname
            checkName(Iname);
        }

        // return if in Syntax-check mode
        if (inSyntaxCheckMode())
            return;

        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return;


        ///////////////////////
        // GPIB communication ?
        if (m_UsedCommPort == CommPorts.GPIB) {
            
            // check error queue
            checkErrorQueue("The previous command caused this error.\n");
            
            // disable the SMU?
            if ( Function.toUpperCase().equals("DISABLE")) {
                // disbale the SMU
                String cmd = String.format(Locale.US, 
                    "PAGE:CHAN:CDEF:SMU%d:DISABLE", SMU_Number);
                SendViaGPIB(cmd);
                
                // check error queue
                checkErrorQueue("Setting the Vname caused this error.\n");
                
                // exit
                return;
            }

            // set the Vname
            String cmd = String.format(Locale.US, 
                    "PAGE:CHAN:CDEF:SMU%d:VNAME '%s'", SMU_Number, Vname);
            SendViaGPIB(cmd);
            
            // check error queue
            checkErrorQueue("Setting the Vname caused this error.\n");

            // set the Iname
            cmd = String.format(Locale.US, 
                    "PAGE:CHAN:CDEF:SMU%d:INAME '%s'", SMU_Number, Iname);
            SendViaGPIB(cmd);
            
            // check error queue
            checkErrorQueue("Setting the Iname caused this error.\n");

            // set the mode
            cmd = String.format(Locale.US, 
                    "PAGE:CHAN:CDEF:SMU%d:MODE %s", SMU_Number, Mode);
            SendViaGPIB(cmd);
            
            // check error queue
            checkErrorQueue("Setting the Mode caused this error.\n");

            // set the Functions
            cmd = String.format(Locale.US, 
                    "PAGE:CHAN:CDEF:SMU%d:FUNC %s", SMU_Number, Function);
            SendViaGPIB(cmd);
            
            // check error queue
            checkErrorQueue("Setting the Function caused this error.\n");
            
            // add a reminder for the user
            //IcontrolView.DisplayStatusMessage("Info: Please ensure that the variables are either displayed or contained in the List when you want to save them.\n", false);
        }

    }//</editor-fold>

    /**
     * Configures the VMUs. After each step the error queue of the 4155 is checked.<p>
     *
     * This method performs a Syntax-Check.
     *
     * @param VMU_Number The number of the VMU to configure
     * @param Vname The variable name of the voltage
     * @param Mode The operation mode of this VMU
     *
     * @throws IOException re-thrown from {@link Device#SendViaGPIB}
     * @throws DataFormatException when the Syntax Check failed.
     * @throws ScriptException when the error queue contains an error (bubbles
     * up from <code>checkErrorQueue</code>).
     */
    // <editor-fold defaultstate="collapsed" desc="config VMU">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Configure a VMU</html>",
        ParameterNames = {"VMU# { [1,2] }", "Vname", "Mode"},
        DefaultValues = {"1", "Vm1", "V"},
        ToolTips = {"", "", "", "Can be V, DVolt, DeleteRow"})
    @iC_Annotation(  MethodChecksSyntax = true )
    public void configVMU( int VMU_Number, String Vname, String Mode )
           throws IOException, DataFormatException, ScriptException {


        ///////////////
        // Syntax-check

        // check VMU number
        if (VMU_Number < 1 || VMU_Number > 2) {
            String str = "The VMU Number must be between 1 and 2.";
            throw new DataFormatException(str);
        }

        // Syntax Check for correct VMU Mode
        final List<String> VMU_MODES = Arrays.asList("V", "DVOLT", "DELETEROW");
        if ( !VMU_MODES.contains(Mode.toUpperCase()) ) {
            String str = "The VMU Mode is not valid.";
            str += "Please select a VMU mode from:\n " + VMU_MODES.toString() + ".\n";
            throw new DataFormatException(str);
        }

        // check Vname
        checkName(Vname);

        // return if in Syntax-check mode
        if (inSyntaxCheckMode())
            return;

        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return;


        ///////////////////////
        // GPIB communication ?
        if (m_UsedCommPort == CommPorts.GPIB) {
            
            // check error queue
            checkErrorQueue("The previous command caused this error.\n");

            // set the Vname
            String cmd = String.format(Locale.US,
                    "PAGE:CHAN:CDEF:VMU%d:VNAME '%s'", VMU_Number, Vname);
            SendViaGPIB(cmd);
            
            // check error queue
            checkErrorQueue("Setting the Vname caused this error.\n");

            // set the mode
            cmd = String.format(Locale.US,
                    "PAGE:CHAN:CDEF:VMU%d:MODE %s", VMU_Number, Mode);
            SendViaGPIB(cmd);
            
            // check error queue
            checkErrorQueue("Setting the Mode caused this error.\n");
        }
    }//</editor-fold>


    /**
     * Configures the Measurement Mode. After each step the error queue of the 
     * 4155 is checked.<p>
     *
     * This method performs a Syntax-Check.
     *
     * @param Mode The Measurement Mode.
     *
     * @throws IOException re-thrown from {@link Device#SendViaGPIB}
     * @throws DataFormatException when the Syntax Check failed.
     * @throws ScriptException when the error queue contains an error (bubbles
     * up from <code>checkErrorQueue</code>).
     */
    // <editor-fold defaultstate="collapsed" desc="config MeasurementMode">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Choose a measurement mode.</html>",
        ParameterNames = {"Mode"},
        DefaultValues = {"Sweep"},
        ToolTips = {"Can be Sweep, Sampling, QSCV"})
    @iC_Annotation(  MethodChecksSyntax = true )
    public void configMeasurementMode(String Mode)
           throws IOException, DataFormatException, ScriptException {


        ///////////////
        // Syntax-check

        // Syntax Check for correct Mode
        if ( !MEASUREMENT_MODES.contains(Mode.toUpperCase()) ) {
            String str = "The Measurement Mode is not valid.";
            str += "Please select a measurement mode from:\n " + MEASUREMENT_MODES.toString() + ".\n";
            throw new DataFormatException(str);
        }

        // return if in Syntax-check mode
        if (inSyntaxCheckMode())
            return;

        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return;


        ///////////////////////
        // GPIB communication ?
        if (m_UsedCommPort == CommPorts.GPIB) {
            
            // check error queue
            checkErrorQueue("The previous command caused this error.\n");

            // set the measurement mode
            String cmd = String.format(Locale.US, "PAGE:CHAN:CDEF:MODE %s", Mode);
            SendViaGPIB(cmd);
            
            // check error queue
            checkErrorQueue("");
        }
    }//</editor-fold>

    /**
     * Configures the parameters for a Sweep measurement.<p>
     *
     * This method performs a Syntax-Check. Note that the Syntax-Check does not
     * guarantee 100% correctness. Some values, for instance the Compliance value,
     * depend on the chosen measurement range, and this sophisticated level of
     * correctness is not implemented in the Syntax-Check.<p>
     * 
     * After each step the error queue of the 4155 is checked.
     *
     * @throws IOException re-thrown from {@link Device#SendViaGPIB}
     * @throws DataFormatException when the Syntax Check failed.
     * @throws ScriptException when the error queue contains an error (bubbles
     * up from <code>checkErrorQueue</code>).
     */
    // <editor-fold defaultstate="collapsed" desc="config Sweep">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Configure a Sweep measurement.</html>",
        ParameterNames = {"Sweep Variable", "Double Sweep?", "Spacing",
                          "Start", "Stop", "Step", "Compliance",
                          "Hold Time [sec]", "Delay Time [sec]"},
        DefaultValues = {"Var1", "true", "Lin", "0", "5", "0.1", "100e-6",
                         "1", "0"},
        ToolTips = {"Can be Var1 or Var2", "Single or Double sweeps? (only valid for Var1)",
                    "Can be Lin, L10, L25, or L50", "Range depends on the setup",
                    "Range depends on the setup", "Range depends on the setup",
                    "Range depends on the setup", "Wait time before making 1st measurement of each Var1 sweep.", 
                    "Delay time after each step before making a measurement."})
    @iC_Annotation(  MethodChecksSyntax = true )
    public void configSweep(String SweepVariable, boolean DoubleSweep, String Spacing,
                            float Start, float Stop, float Step,
                            float Compliance, float HoldTime, float DelayTime)
           throws IOException, DataFormatException, ScriptException {


        ///////////////
        // Syntax-check

        // Syntax Check for correct SweepVariable
        final List<String> SWEEP_VARIABLE =
                Arrays.asList("VAR1", "VAR2");
        if ( !SWEEP_VARIABLE.contains(SweepVariable.toUpperCase()) ) {
            String str = "The Sweep-Variable '" + SweepVariable + "' is not valid.";
            str += "Please select a Sweep-Variable from:\n " + SWEEP_VARIABLE.toString() + ".\n";
            throw new DataFormatException(str);
        }

        // Syntax Check for correct Spacing
        final List<String> SPACING =
                Arrays.asList("LIN", "L10", "L25","L50");
        if ( !SPACING.contains(Spacing.toUpperCase()) ) {
            String str = "The Spacing-parameter is not valid.";
            str += "Please select a Spacing-parameter from:\n " + SPACING.toString() + ".\n";
            throw new DataFormatException(str);
        }
        
        // Range check for Start
        // TODO 3* define in iC_Properties
        final List<Float> START_RANGE = Arrays.asList(-100f, 100f);
        if (Start < START_RANGE.get(0) || Start > START_RANGE.get(1)) {
            String str = "The Start value is out of range.\n"
                    + "Please select a value between " + START_RANGE.toString() + ".\n";
            throw new DataFormatException(str);
        }
        
        // Range check for Stop
        // TODO 3* define in iC_Properties
        if (Stop < -100 || Stop > 100) {
            String str = "The Stop value is out of range.\n"
                    + "Please select a value between -100 and 100.\n";
            throw new DataFormatException(str);
        }
        
        // Range check for Step
        // TODO 3* define in iC_Properties and depending on measurement mode
        // the range depends on the measurement mode 
        // (+-100 for V, +-1 for I - also depends on the type of SMU built in (HRSMU, HPSMU, ...)
        if (Step < -100 || Step > 100) {
            String str = "The Start value is out of range.\n"
                    + "Please select a value between -100 and 100.\n";
            throw new DataFormatException(str);
        }
        
        // Range check for Compliance
        // TODO 3* define in iC_Properties
        if (Compliance < -0.1 || Compliance > 0.1) {
            String str = "The Compliance value is out of range.\n"
                    + "Please select a value between -100 and 100.\n";
            throw new DataFormatException(str);
        }

        // Range check for HoldTime
        if (HoldTime < 0 || HoldTime > 655.35) {
            String str = "The Hold Time is out of range.\n"
                    + "Please select a value between 0 and 635.35 .\n";
            throw new DataFormatException(str);
        }

        // Range check for DelayTime
        if (DelayTime < 0 || DelayTime > 655.35) {
            String str = "The Delay Time is out of range.\n"
                    + "Please select a value between 0 and 635.35 .\n";
            throw new DataFormatException(str);
        }
        


        // return if in Syntax-check mode
        if (inSyntaxCheckMode())
            return;

        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return;


        ///////////////////////
        // GPIB communication ?
        if (m_UsedCommPort == CommPorts.GPIB) {
            
            // check error queue
            checkErrorQueue("The previous command caused this error.\n");

            // set the measurement mode
            String cmd = String.format(Locale.US, 
                    "PAGE:MEAS:SWE:%s:MODE %s", SweepVariable,
                                                  DoubleSweep ? "DOUBLE" : "SINGLE");
            // send it only for VAR1
            if (SweepVariable.toUpperCase().equals("VAR1")) {
                SendViaGPIB(cmd);
                
                // check error queue
                checkErrorQueue("Setting the single/double sweep caused this error.\n");
            } else {
                // log Info message
                //IcontrolView.DisplayStatusMessage("Info: single/double Sweep for VAR2 was ignored.\n", false);
                m_Logger.info("single/double Sweep for VAR2 was ignored.");
            }
                       

            // set the Spacing
            cmd = String.format(Locale.US,
                    "PAGE:MEAS:SWE:%s:SPAC %s", SweepVariable, Spacing);
            
            // send it only for VAR1
            if (SweepVariable.toUpperCase().equals("VAR1")) {
                SendViaGPIB(cmd);
                
                // check error queue
                checkErrorQueue("Setting the Spacing caused this error.\n");
            } else {
                // log Info message
                //IcontrolView.DisplayStatusMessage("Info: setting the Spacing for VAR2 was ignored.\n", false);
                m_Logger.info("Setting the Spacing for VAR2 was ignored.");
            }


            // set the Start
            cmd = String.format(Locale.US,
                    "PAGE:MEAS:SWE:%s:STAR %s", SweepVariable, Start);
            SendViaGPIB(cmd);
            
            // check error queue
            checkErrorQueue("Setting the Start value caused this error.\n");

            
            // set the Stop
            cmd = String.format(Locale.US,
                    "PAGE:MEAS:SWE:%s:STOP %s", SweepVariable, Stop);
            
            // send it only for VAR1
            if (SweepVariable.toUpperCase().equals("VAR1")) {
                SendViaGPIB(cmd);
                
                // check error queue
                checkErrorQueue("Setting the Stop value caused this error.\n");
            } else {
                // for VAR2, set the number of points instead
                int NrPoints = Math.abs(Math.round(Math.abs(Stop-Start)/Step));
                
                // measure at least one data point
                if (NrPoints < 1) {
                    NrPoints = 1;
                }
                
                cmd = String.format(Locale.US,
                    "PAGE:MEAS:SWE:%s:POINTS %s", SweepVariable, NrPoints);
                
                SendViaGPIB(cmd);
                
                // check error queue
                checkErrorQueue("Setting the number of Points (no Stop value for Var2) caused this error.\n");
            }

            // set the Step
            cmd = String.format(Locale.US,
                    "PAGE:MEAS:SWE:%s:STEP %s", SweepVariable, Step);
            SendViaGPIB(cmd);
            
            // check error queue
            checkErrorQueue("Setting the Step value caused this error.\n");

            // set the Compliance
            cmd = String.format(Locale.US,
                    "PAGE:MEAS:SWE:%s:COMP %s", SweepVariable, Compliance);
            SendViaGPIB(cmd);
            
            // check error queue
            checkErrorQueue("Setting the Compliance caused this error.\n");

            // set the Hold Time
            cmd = String.format(Locale.US,
                    "PAGE:MEAS:SWE:HTIME %E", HoldTime);
            SendViaGPIB(cmd);
            
            // check error queue
            checkErrorQueue("Setting the Hold Time caused this error.\n");

            // set the Delay Time
            cmd = String.format(Locale.US,
                    "PAGE:MEAS:SWE:DEL %E", DelayTime);
            SendViaGPIB(cmd);
            
            // check error queue
            checkErrorQueue("Setting the Delay Time caused this error.\n");
        }

    }//</editor-fold>
 
    
    /**
     * Configures the data variables used to display data on the graph page of 
     * the 4155.<p>
     * 
     * This method performes a Syntax-Check. Any error that occurs during the real
     * run is ignored so that scripting is not disturbed by this minor mistake.
     * 
     * @param Axis Specifies which axis to change; can be X, Y1, or Y2
     * @param Scale Sets the scale with which the data axis is drawn; can be Linear or Log
     * @param Min Sets the minimum value shown on the axis
     * @param Max Sets the maximum value shown on the axis
     * 
     * @throws ScriptException when the error queue contains an error (bubbles
     * up from <code>checkErrorQueue</code>).
     * @throws IOException bubbles up from <code>WaitAndSendViaGPIB</code>
     * @throws DataFormatException when Syntax-check fails
     */
    // <editor-fold defaultstate="collapsed" desc="configDisplayAxis">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Configures which data variables to display as X, Y1, or Y2 axis.</html>",
        ParameterNames = {"Axis Name", "Scale", "Min", "Max"},
        DefaultValues = {"X", "Linear", "0", "1"},
        ToolTips = {"Can be X, Y1, or Y2", "Can be Linear or Log"})
    @iC_Annotation(MethodChecksSyntax=true)
    public void configDisplayAxis(String Axis, String Scale, float Min, float Max) 
           throws ScriptException, IOException, DataFormatException {
        
        
        ///////////////
        // Syntax-check
        // also builds the Base GPIB String
        
        // build base GPIB String
        String BaseCmd = ":PAGE:DISP:SET:GRAP:";

        // add axis name
        Axis = Axis.trim();
        if (Axis.equalsIgnoreCase("X")) {
            BaseCmd += "X";
        } else if (Axis.equalsIgnoreCase("Y1")) {
            BaseCmd += "Y1";
        } else if (Axis.equalsIgnoreCase("Y2")) {
            BaseCmd += "Y2";
        } else {
            String str = "The specified axis name '" + Axis + "' is not valid.\n"
                    + "Please select either X, Y1, or Y2";
            
            throw new DataFormatException(str);
        }
        
        // check Scale
        if ( !Scale.equalsIgnoreCase("Linear") && !Scale.equalsIgnoreCase("Log")) {
            String str = "The Scale '" + Scale + "' is invalid.\n"
                    + "Please choose either Linear or Log";
            
            throw new DataFormatException(str);
        }
        
        
        // exit if in Syntax-check mode
        if (inSyntaxCheckMode())
            return;
        
        
        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return;


        ///////////////////////
        // GPIB communication ?
        if (m_UsedCommPort == CommPorts.GPIB) {
            
            // check error queue
            checkErrorQueue("The previous command caused this error.\n");            

            // catch all errors and just display them to not interrrupt scripting
            try {
                ////////
                // set data variable to display
                String cmd = BaseCmd + String.format(Locale.US, ":NAME '%s'", Axis);

                // send the command
                WaitAndSendViaGPIB(cmd);
                    
                // check error queue
                checkErrorQueue("Setting the data variable to display on the " + Axis + " axis failed.\n");
                

                ////////
                // set scale
                cmd = BaseCmd + String.format(Locale.US, ":SCALe '%s'", Scale);

                // send the command
                WaitAndSendViaGPIB(cmd);
                    
                // check error queue
                checkErrorQueue("Setting the scale to " + Scale + " on the " + Axis + " axis failed.\n");
                
                
                ////////
                // set min value
                cmd = BaseCmd + String.format(Locale.US, ":MIN %e", Min);

                // send the command
                WaitAndSendViaGPIB(cmd);
                    
                // check error queue
                checkErrorQueue("Setting the MIN value of " + Min + " on the " + Axis + " axis failed.\n");
                
                
                ////////
                // set max value
                cmd = BaseCmd + String.format(Locale.US, ":MAX %e", Max);

                // send the command
                WaitAndSendViaGPIB(cmd);
                    
                // check error queue
                checkErrorQueue("Setting the MAX value of " + Max + " on the " + Axis + " axis failed.\n");
                
                
            } catch (InterruptedException ex) {
                /* user pressed stop, so return */
                return;
            } catch (ScriptException ex) {
                /* setting one of the data variables failed
                 * Display a warning and return */
                IcontrolView.DisplayStatusMessage("Error: " + ex.getMessage());
            }
        }
    }//</editor-fold>
    
    // TODO delme
    /**
     * Configures which data variables are displayed on the X, Y, and Y2 axis
     * on the display of the 4155.<p>
     * 
     * If the specified data variable names do not exist, the corresponding
     * axis is not changed and the error is ignored so that scripting is not
     * disturbed by this minor mistake.
     * 
     * @param Xaxis Data variable name to display on the X axis
     * @param Y1axis Data variable name to display on the Y1 axis
     * @param Y2axis Data variable name to display on the Y2 axis
     * 
     * @throws ScriptException when the error queue contains an error (bubbles
     * up from <code>checkErrorQueue</code>).
     * @throws IOException bubbles up from <code>WaitAndSendViaGPIB</code>
     */
    /*
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Configure which data variables to display on the 4155.</html>",
        ParameterNames = {"X Data Variable", "Y1 Data Variable", "Y2 Data Variable"},
        DefaultValues = {"Vg", "Id", "Ig"},
        ToolTips = {"Can be empty", "Can be empty",
                    "Can be an empty String if no Y2 axis is needed."})
    public void configDisplayAxis(String Xaxis, String Y1axis, String Y2axis) 
           throws ScriptException, IOException {
        
        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return;


        ///////////////////////
        // GPIB communication ?
        if (m_UsedCommPort == CommPorts.GPIB) {
            
            // check error queue
            checkErrorQueue("The previous command caused this error.\n");

            try {
                // set X axis
                if ( !Xaxis.trim().isEmpty() ) {
                    
                    // form the GPIB command
                    String cmd = String.format(Locale.US, 
                        ":PAGE:DISP:SET:GRAP:X:NAME '%s'", Xaxis.trim());
                    
                    // send the command
                    WaitAndSendViaGPIB(cmd);
                    
                    // check error queue
                    checkErrorQueue("Seting the data variable to show on the X axis failed.\n");
                }
                
                // set Y1 axis
                if ( !Y1axis.trim().isEmpty() ) {
                    
                    // form the GPIB command
                    String cmd = String.format(Locale.US, 
                        ":PAGE:DISP:SET:GRAP:Y1:NAME '%s'", Y1axis.trim());
                    
                    // send the command
                    WaitAndSendViaGPIB(cmd);
                    
                    // check error queue
                    checkErrorQueue("Seting the data variable to show on the Y1 axis failed.\n");
                }
                
                // set Y2 axis
                if ( !Y2axis.trim().isEmpty() ) {
                    
                    // form the GPIB command
                    String cmd = String.format(Locale.US, 
                        ":PAGE:DISP:SET:GRAP:Y2:NAME '%s'", Y2axis.trim());
                    
                    // send the command
                    WaitAndSendViaGPIB(cmd);
                    
                    // check error queue
                    checkErrorQueue("Seting the data variable to show on the Y2 axis failed.\n");
                }
                
                
            } catch (InterruptedException ex) {
                /* user pressed stop, so return */
//                return;
//            } catch (ScriptException ex) {
//                /* setting one of the data variables failed
//                 * Display a warning and return */
//                IcontrolView.DisplayStatusMessage("Error: " + ex.getMessage());
//            }
//        }
//    }//</editor-fold>*/
    
}


