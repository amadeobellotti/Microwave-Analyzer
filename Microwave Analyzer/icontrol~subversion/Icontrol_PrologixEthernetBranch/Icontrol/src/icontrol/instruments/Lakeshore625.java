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
import icontrol.iC_Annotation;
import java.io.IOException;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;
import javax.script.ScriptException;
import javax.swing.JOptionPane;

/**
 * Lakeshore 625 Superconducting Magnet Power Supply "driver" class.<p>
 *
 * All device commands that the Lakeshore 625 understands are
 * implemented here.<p>
 *
 * <h3>Instrument Commands:</h3>
 * <ul>
 *  <li>{@link #getField() }
 *  <li>{@link #setField(float, boolean)}
 *  <li>{@link #setRampRate(float) }
 * </ul>
 *
 * @author KPP (Kurt Pernstich: pernstich@alumni.ethz.ch)
 * @version 0.3
 *
 */

// promise that this class supports GPIB communication
@iC_Annotation( CommPorts=Device.CommPorts.GPIB,
                InstrumentClassName="Lakeshore 625")
public class Lakeshore625 extends Device {

    ///////////////////
    // member variables

    /**
     * The Logger for this class. Note that the Logger Level is set in the
     * constructor of the <code>Device</code> class.
     */
    private static final Logger m_Logger = Logger.getLogger("iC.Instruments.Lakeshore625");


    /**
     * Overridden <code>Close</code> method that puts the Instrument in a
     * local mode and clears the interface, just in case.
     *
     * @throws IOException re-thrown from <code>SendViaGPIB</code>
     * @see Device#Close
     */
    // <editor-fold defaultstate="collapsed" desc="Close">
    @Override
    public void Close() throws IOException {

        // using GPIB do ...
        if (m_UsedCommPort == CommPorts.GPIB) {

            // clear the interface, just in case
            SendViaGPIB("*CLS");

            // set to local
            SendViaGPIB("MODE 1");
        }
    }// </editor-fold>

    /**
     * Ramps the magnetic field to the specified value and waits until the field
     * is within an Epsilon value of the Setpoint. The parameter Epsilon is
     * defined in iC.properties (Lakeshore625.Epsilon).
     *
     * @param Field Magnetic field Setpoint
     *
     * @param Wait When <code>true</code> this method waits until the mag. field
     * is within a value of Epsilon to the Setpoint before it returns, else it
     * returns after setting the new Setpoint.
     *
     * @throws IOException bubbles up from <code>SendViaGPIB</code> and from
     * <code>getField</code>.
     *
     * @throws ScriptException when parsing the String was unsuccessful (bubbles
     * up from <code>getField</code>).
     */
    // <editor-fold defaultstate="collapsed" desc="set Field">
    @AutoGUIAnnotation(
        DescriptionForUser="Sets the magnetic field and waits until field is reached",
        ParameterNames={"Mag Field Setpoint [Field Units]", "Wait until reached"},
        DefaultValues={"0.1", "true"},
        ToolTips={"", "<html>When true, this command waits until the mag. Field is within<br>"
                + "an Epsilon value of the Setpoint before it returns.</html>"})
    public void setField(float Field, boolean Wait)
            throws IOException, ScriptException {

        // field equality epsilon
        float eps = (float) m_iC_Properties.getDouble("Lakeshore625.Epsilon", 1e-3);


        // return if in No-Communication Mode
        if (inNoCommunicationMode())
            return;


        // GPIB communication?
        if (m_UsedCommPort == CommPorts.GPIB) {

            // build the command String
            String cmd = String.format(Locale.US, "SETF %.3E", Field);

            // send the command
            SendViaGPIB(cmd);


            ////////////////////////////////////////////////
            // wait for the magnet to reach the target field
            if (Wait) {
                // init current field to something else than target field
                float CurrentField = Field - 2*eps;

                // wait until target field is reached
                while ( java.lang.Math.abs(CurrentField-Field) > eps &&
                        m_StopScripting == false ) {

                    // check for pause button
                    isPaused(true);

                    // get current field
                    CurrentField = getField();

                    // wait a bit
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ex) {/*ignore*/}
                }
            }
        }
    }//</editor-fold>



    /**
     * Read the current magnetic field.
     *
     * @return The calculated output field (from field constant and current)
     * or 123.45 if in No-Communication Mode. The unit is in Gauss or
     * Tesla depending on the setting in the FLDS command.
     *
     * @throws IOException thrown by <code>QueryViaGPIB</code>
     *
     * @throws ScriptException thrown by <code>getFloat</code> if the Instrument's
     * response could not be converted into a <code>float</code>
     */
    // <editor-fold defaultstate="collapsed" desc="get Field">
    @AutoGUIAnnotation(
        DescriptionForUser="Reads the magnetic field.",
        ParameterNames={},
        DefaultValues={},
        ToolTips={})
    public float getField()
                 throws IOException, ScriptException {

        // local variables
        Float ret = 0.0f;

        // it is convenient to return a (unphysical) number in No-Communication-Mode
        // to test the rest of the program, so return 123.45 T if in No-Communication-Mode
        if ( inNoCommunicationMode() )
            return 123.45f;


        ///////////////////////
        // GPIB communication ?
        if (m_UsedCommPort == CommPorts.GPIB) {

            // query Kelvin Reading of selected channel and store the answer
            String ans = QueryViaGPIB("RDGF?");

            // convert
            ret = getFloat(ans);

            // debug
            m_GUI.DisplayStatusMessage("Lakeshre 625: mag. Field = " + Float.toString(ret));
        }

	return ret;
    }//</editor-fold>


    /**
     * Set the rate for ramping the magnetic field.
     *
     * @param RampRate The new ramp rate.
     *
     * @throws IOException bubbles up from <code>SendViaGPIB</code> and from
     * <code>getField</code>.
     *
     * @throws DataFormatException if Syntax-Check fails
     */
    // <editor-fold defaultstate="collapsed" desc="set Ramp Rate">
    @AutoGUIAnnotation(
        DescriptionForUser="Sets the output current ramp rate.",
        ParameterNames={"Ramp Rate [A/sec]"},
        DefaultValues={"1"},
        ToolTips={""})
    @iC_Annotation(MethodChecksSyntax=true)
    public void setRampRate(float RampRate)
            throws IOException, DataFormatException {

        ///////////////
        // Syntax-Check

        if ( RampRate < 0.0001 || RampRate > 99.999 )
            throw new DataFormatException("The Ramp Rate must be between 0.0001 and 99.999.");


        // return from Syntax-Check mode
        if (inSyntaxCheckMode())
            return;


        // return if in No-Communication Mode
        if (inNoCommunicationMode())
            return;


        // GPIB communication?
        if (m_UsedCommPort == CommPorts.GPIB) {

            // build the command String
            String cmd = String.format(Locale.US, "RATE %.3f", RampRate);

            // send the command
            SendViaGPIB(cmd);
        }
    }//</editor-fold>
}
