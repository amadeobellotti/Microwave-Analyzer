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
import icontrol.instruments.Device.CommPorts;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;
import javax.script.ScriptException;
import javax.swing.JOptionPane;

/**
 * This class implements functionality to communicate with a Keithley 2600A System
 * SourceMeter. It was tested with a Keithley 2636 and should work for the entire
 * 2600 class instruments.<p>
 *
 * <h3>Instrument Commands:</h3>
 * <ul>
 *  <li>{@link #configSMUChannel(String, String)  }
 *  <li>The following commands are implemented as generic GPIB instruments:
 *  <li>configAnalogFilters
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
     * Configures the SMU Channel to act as voltage or current source.<p>
     *
     * This method performs a Syntax-Check.
     *
     * @param SMUChannel The SMU Channel, can be A, B (case insensitive)
     * @param Mode The operation mode, can be V, I (case insensitive)
     *
     * @throws IOException re-thrown from {@link Device#SendViaGPIB}
     *
     * @throws DataFormatException when the Syntax Check failed.
     */
    // <editor-fold defaultstate="collapsed" desc="configSMUChannel">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Select if the SMU Channel operates as current or voltage source</html>",
        ParameterNames = {"SMU Channel Name {A, B}", "Operation Mode {V, I}"},
        DefaultValues = {"A", "V"},
        ToolTips = {"", "<html>V ... Voltage source (force Voltage measure Current)<br>I ... Current source (force Current measure Voltage)</html>", "The command to execute"})
    @iC_Annotation(  MethodChecksSyntax = true )
    public void configSMUChannel(String SMUChannel, String Mode)
           throws IOException, DataFormatException {

        // local variables
        float dummy = 0;

        // convert to lower case
        SMUChannel = SMUChannel.toLowerCase();
        
        

        ///////////////
        // Syntax-Check

        // check channel 
        checkChannelName(SMUChannel);
        
        // check Mode
        if ( !Mode.equalsIgnoreCase("V") && !Mode.equalsIgnoreCase("I") ) {
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



        ///////////////////////
        // GPIB communication ?
        if (m_UsedCommPort == CommPorts.GPIB) {

            // build the GPIB command
            String cmd = String.format(Locale.US, "smu%s.source.func = %s", SMUChannel, 
                    (Mode.equalsIgnoreCase("V") ? "1" : "0") );
            
            // send the command
            SendViaGPIB(cmd);

        }
    }//</editor-fold>
    
    
    /**
     * Measures Current.<p>
     *
     * This method performs a Syntax-Check.
     *
     * @param SMUChannel The SMU Channel, can be A, B (case insensitive)
     *
     * @throws IOException re-thrown from <code>SendViaGPIB</code> or 
     * <code>QueryViaGPIB</code>
     * 
     * @throws ScriptException if the returned answer from the Instrument could
     * not be interpreted as a Double value (bubbles up from <code>getDouble</code>.
     * 
     * @throws DataFormatException when the Syntax Check failed.
     */
    // <editor-fold defaultstate="collapsed" desc="measureI">
    @iC_Annotation(  MethodChecksSyntax = true )
    public double measureI(String SMUChannel)
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



        ///////////////////////
        // GPIB communication ?
        if (m_UsedCommPort == CommPorts.GPIB) {

            // build the GPIB command
            String cmd = String.format(Locale.US, "I = smu%s.measure.i()", SMUChannel);
            
            // send the command
            SendViaGPIB(cmd);
            
            // query answer
            String ans = QueryViaGPIB("print(I)");
            
            // try to convert to a double
            ret = getDouble(ans);
            
            // TODO delme
            IcontrolView.DisplayStatusMessage("I = " + ret + "\n");

        }
        
        // return value
        return ret;
    }

    
    /**
     * Performs a Syntax-Check for the correct Channel Name.
     *
     * @param Mode The Channel name to test
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

}
