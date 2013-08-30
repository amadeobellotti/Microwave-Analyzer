// TODO add to list of supported instruments in overview.html

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

import gnu.io.SerialPort;
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
 * Eksplan NT340 Tuneable Nanosecond Laser System.<p>
 *
 * All device commands that the NewInstrument understands are
 * implemented here.<p>
 *
 * <h3>Instrument Commands:</h3>
 * <ul>
 *  <li>{@link #getStatus() }
 * </ul>
 *
 * @author KPP (Kurt Pernstich: pernstich@alumni.ethz.ch)
 * @version 0.1
 *
 */

// promise that this class supports GPIB communication
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
    private static final Logger m_Logger = Logger.getLogger("iC.Instruments.NewInstrument");


                  
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


        // query command
        String ans = QueryViaRS232("[NL:SAY\\iC]");
        
        // display answer
        IcontrolView.DisplayStatusMessage("Status: " + ans + "\n");
    }//</editor-fold>



    
}
