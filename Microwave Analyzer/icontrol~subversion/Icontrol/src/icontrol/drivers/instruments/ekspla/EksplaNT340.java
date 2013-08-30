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
package icontrol.drivers.instruments.ekspla;

import icontrol.AutoGUIAnnotation;
import icontrol.IcontrolView;
import icontrol.drivers.Device;
import icontrol.iC_Annotation;
import icontrol.drivers.Device.CommPorts;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;

/**
 * Ekspla NT340 Series Tuneable Nd:YAG Pulsed Laser System. The default RS232 
 * parameters are 19200 baud, 8 data bits, 1 stop bit, no parity. Note that a 
 * straight RS232 cable must be used, i.e. pin 2 goes to pin 2, and pin 3 to 
 * pin 3 and pin 5 to pin 5.<p>
 *
 * All device commands that the Ekspla NT340 understands are implemented here.<p>
 *
 * <h3>Instrument Commands:</h3>
 * <ul>
 *  <li>{@link #getStatus() }
 *  <li>{@link #NullmodemTest() }
 *  <li>{@link #OutputPower(String) }
 *  <li> Note that more commands are defined as generic commands
 * </ul>
 *
 * @author KPP (Kurt Pernstich: pernstich@alumni.ethz.ch)
 * @version 0.1
 *
 */

// promise that this class supports RS232 communication
@iC_Annotation(CommPorts=CommPorts.RS232,
                InstrumentClassName="Ekspla NT340")
public class EksplaNT340 extends Device {


    ///////////////////
    // member variables

    /**
     * The Logger for this class. Note that the Logger Level is set in the
     * constructor of the <code>Device</code> class.
     */
    // change the Logger name to the name of your class
    private static final Logger m_Logger = Logger.getLogger("iC.Instruments.EksplaNT340");


                  
    /**
     * Query the Status of the Laser System [NL] and displays the Laser Status 
     * in the GUI.
     * 
     * @throws IOException Bubbles up from <code>QueryInstrument</code>
     */
    // <editor-fold defaultstate="collapsed" desc="getStatus">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Get the Status of the Laser System.</html>",
        ParameterNames = {},
        DefaultValues = {},
        ToolTips = {})
    @iC_Annotation(  MethodChecksSyntax = false )
    public void getStatus()
           throws IOException {

        
        // return if in Syntax-check mode
        if (inSyntaxCheckMode())
            return;

        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return;


        // query the Laser
        String ans = QueryInstrument("[NL:SAY\\iC]");
        
        // display answer
        m_GUI.DisplayStatusMessage("Laser Status: " + ans + "\n");

    }//</editor-fold>


    /** 
     * Performs a Null-Modem test, that is, it sends byte values ranging from
     * 0 to 255 via the RS232 port, reads them back, and checks whether they
     * were transmitted properly. In order for this test to work, pin 2 & 3
     * have to be shorted (null-modem). The test is performed first by sending
     * a byte[], then by converting the byte[] into a String.
     * @throws IOException bubbles up from <code>QueryViaRS232</code>, 
     * <code>QueryInstrument</code>, or when the null-modem test failed.
     */
    // <editor-fold defaultstate="collapsed" desc="NullmodemTest">
    public void NullmodemTest() 
              throws IOException {
        
        // thorough test with all values from 0 to 255
        byte[] AllValues = new byte[256];
        for (int i = 0; i <= 255; i++) {
            AllValues[i] = (byte) i;
        }
        
        // query them as byte stream
        byte[] ans = QueryViaRS232( ByteBuffer.wrap(AllValues), 0 ).array();
        
        // check length
        if (ans.length != AllValues.length) {
            String str = "The length of the received answer was incorrect.\n";
            throw new IOException(str);
        }
        
        // compare send with received
        for (int i = 0; i <= 255; i++) {
            if (AllValues[i] != ans[i]) {
                String str = "The received data bytes were incorrect (comparing byte[]).\n";
                throw new IOException(str);
            }
        } 
        
        
        try {
            // convert to string
            String AllValuesStr = new String(AllValues, CHARACTER_ENCODING);
            
            // query as String
            String ansStr = QueryInstrument(AllValuesStr);

            // convert back to byte[]
            byte[] ConvertedBack = ansStr.getBytes(CHARACTER_ENCODING);
            
            for (int i = 0; i <= 255; i++) {
                if (AllValues[i] != ConvertedBack[i]) {
                    String str = "The received data bytes were incorrect (comparing Sting).\n";
                    throw new IOException(str);
                }
            }    
        } catch (UnsupportedEncodingException ex) {
            String str = "Invalid Character Encoding.\n";
            throw new IOException(str);
        }
        
        // display success message
        m_GUI.DisplayStatusMessage("RS232 Nullmodem test successful.\n", false);
    }// </editor-fold>
    
    
    /**
     * Sets the Output Power to off, adjustment mode, or max. output mode.
     * 
     * @param OutputMode The desired output power; can be Off, Adjust, Max (case
     * insensitive).
     * 
     * @throws DataFormatException When the Syntax-Check failed.
     * @throws IOException Bubbles up from <code>SendToInstrument</code>
     */
    // <editor-fold defaultstate="collapsed" desc="OutputPower">
    @AutoGUIAnnotation(DescriptionForUser = "Sets the Output Power.",
        ParameterNames = {"Power Level"},
        ToolTips = {"Can be Off, Adjust, Max (case insensitive)"},
        DefaultValues = {"Off"})
    @iC_Annotation(MethodChecksSyntax = true)
    public void OutputPower(String OutputMode) 
           throws DataFormatException, IOException {
        
        ///////////////
        // Syntax-Check
        // also assigns the Command String
        
        // build the String
        String cmd = "[NL:E0/S";
        if (OutputMode.equalsIgnoreCase("Off")) {
            cmd += "0\\iC]";
        } else if (OutputMode.equalsIgnoreCase("Adjust")) {
            cmd += "1\\iC]";
        } else if (OutputMode.equalsIgnoreCase("Max")) {
            cmd += "2\\iC]";
        } else {
            String str = "The Power Level '" + OutputMode + "' is not valid."
                    + "Please select a value from: Off, Adjust, Max\n ";
            throw new DataFormatException(str);
        }

        
        // return if in Syntax-check mode
        if (inSyntaxCheckMode()) {
            return;
        }

        // exit if in No-Communication-Mode
        if (inNoCommunicationMode()) {
            return;
        }
        
        // send the command
        SendToInstrument(cmd);
    }// </editor-fold>
    
}
