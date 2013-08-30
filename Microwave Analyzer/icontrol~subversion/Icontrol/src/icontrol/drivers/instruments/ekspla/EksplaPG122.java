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
 * Ekspla PG122 Optical Parametric Oscillator (part of a Ekspla NT340 Series 
 * Tuneable Nd:YAG Pulsed Laser System). The default RS232 parameters are 38400 baud,
 * 8 data bits, 1 stop bit, no parity. Note that a straight RS232 cable must be 
 * used, i.e. pin 2 goes to pin 2, and pin 3 to pin 3 and pin 5 to pin 5.<p>
 *
 * All device commands that the Ekspla PG122 understands are implemented here.<p>
 *
 * <h3>Instrument Commands:</h3>
 * <ul>
 *  <li>{@link #getStatus() }
 *  <li> Note that more commands are defined as generic commands
 * </ul>
 *
 * @author KPP (Kurt Pernstich: pernstich@alumni.ethz.ch)
 * @version 0.1
 *
 */

// promise that this class supports RS232 communication
@iC_Annotation(CommPorts=CommPorts.RS232,
                InstrumentClassName="Ekspla PG122")
public class EksplaPG122 extends Device {


    ///////////////////
    // member variables

    /**
     * The Logger for this class. Note that the Logger Level is set in the
     * constructor of the <code>Device</code> class.
     */
    // change the Logger name to the name of your class
    private static final Logger m_Logger = Logger.getLogger("iC.Instruments.EksplaPG122");


                  
    /**
     * Query the Status of the Optical Parametric Oscillator [D1] and displays 
     * it's Status in the GUI.
     * 
     * @throws IOException Bubbles up from <code>QueryInstrument</code>
     */
    // <editor-fold defaultstate="collapsed" desc="getStatus">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Get the Status of the Optical Parametric Oscillator.</html>",
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
        
        // query the Optical Parametric Oscillator
        String ans = QueryInstrument("[D1:SAY\\iC]");
        
        // display answer
        m_GUI.DisplayStatusMessage("OPO Status: " + ans + "\n");
    }//</editor-fold>  
    
}
