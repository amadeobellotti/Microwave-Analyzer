/**
 * EXPERIMENTAL - DO NOT USE
 */



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
 * EXPERIMENTAL - DO NOT USE
 *
 * <h3>Instrument Commands:</h3>
 * <ul>
 *  <li>{@link #DemoCommand() }
 * </ul>
 *
 * @author KPP (Kurt Pernstich: pernstich@alumni.ethz.ch)
 * @version 0.3
 *
 */
// TODO @LAN update javadoc section above
// you might also want to rename the class using Netbeans (or else SVN becomes a mess!!)

// promise that this class supports LAN (TCP) communication
@iC_Annotation(CommPorts=CommPorts.LAN,
                InstrumentClassName="LAN_Instrument")
public class LAN_Instrument extends Device {


    ///////////////////
    // member variables

    /**
     * The Logger for this class. Note that the Logger Level is set in the
     * constructor of the <code>Device</code> class.
     */
    // change the Logger name to the name of your class
    // TODO @LAN update name once the class is renamed
    private static final Logger m_Logger = Logger.getLogger("iC.Instruments.LAN_Insrument");


    // define your member variables here, use m_ to mark them as such, at least
    // if you like to stick to the convention used in the iC Framework.
    private int m_NewVariable;


    /**
     * To make use of the mechanism that checks the Instrument's response to a
     * *IDN? query to check if the Instrument is of the correct type, add an
     * appropriate entry to the file iC.properties found in the icontrol.resources
     * directory. The entry should consist of this class's name followed by
     * '.InstrumentNameIdentifier' (without quotes). When this nomenclature is
     * used, the default implementation of
     * <code>Device.getInstrumentNameIdentifier</code> checks if the value
     * to this key is contained in the Instrument's response to an *IDN? query.
     * Only if this String is found the Instrument is accepted, otherwise the User
     * is warned. The User can then decide whether or not to accept the Instrument
     * and also if this selection should be persistently stored in the User
     * properties. If the InstrumentNameIdentifier String is empty no *IDN?
     * query is performed. This is important for older Instruments that do not
     * implement the *IDN? command.
     * This whole mechanism is helpful if, as an example, a Lakeshore 334
     * temperature controller is used with a "driver" for a Lakeshore 340
     * temperature controller. The two command sets might be similar but because
     * they are not necessarily identical, it is the User's responsibility to accept
     * a different Instrument than the driver was designed for, and that only
     * supported Device Commands are used.
     */



    /**
     * This is an exemplary implementation of a new device command to help understanding
     * how to write new Instrument Commands. Use the reference implementation to
     * implement new Instrument Commands.<p>
     *
     * This method shows a message to the user when in SyntaxCheck mode and also
     * when the 'real' action is performed.<p>
     *
     * This method implements the Syntax-Check mode.
     *
     * @throws ScriptException when conversion of the Instrument's answer to a
     * float fails.
     *
     * @throws IOException re-thrown from {@link Device#QueryViaGPIB}
     *
     * @throws DataFormatException when the Syntax Check failed.
     *
     */
    // TODO @LAN test your implementation here
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Just send something via LAN</html>",
        ParameterNames = {},
        DefaultValues = {},
        ToolTips = {})
    @iC_Annotation(  MethodChecksSyntax = false )
    public void DemoCommand()
           throws IOException {


        ///////////////////////
        // LAN communication ?
        // this check is not really necessary as long as a class only supports one 
        // protocoll. At some point I will make a change that only one comm protocol
        // is allowed. For now, let's just use the check to be safe
        if (m_UsedCommPort == CommPorts.LAN) {
            
            // query the command
            String cmd = "Send something";
            String ans = QueryViaLAN(cmd);
            
            // display anser in Status field
            IcontrolView.DisplayStatusMessage("LAN test returned: " + ans + "\n");

        }
    }


   
}
