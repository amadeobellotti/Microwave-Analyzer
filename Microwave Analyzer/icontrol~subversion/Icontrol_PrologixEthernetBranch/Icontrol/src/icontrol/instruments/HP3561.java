//////////////////////////////////////////////////////////
// This is WORK IN PROGRESS with all it's consequences. //
//////////////////////////////////////////////////////////

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

import icontrol.AutoGUIAnnotation;
import icontrol.iC_Annotation;
import icontrol.instruments.Device.CommPorts;
import java.io.IOException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;

/**
 * Hewlett-Packard HP 3561A Dynamic Signal Analyzer.<p>
 *
 * All device commands that the NewInstrument understands are
 * implemented here.<p>
 *
 * <h3>Instrument Commands:</h3>
 * <ul>
 *  <li>{@link #Init(int, boolean, int)  }
 * </ul>
 *
 * @author KPP (Kurt Pernstich: pernstich@alumni.ethz.ch)
 * @version 0.1
 *
 */

// promise that this class supports GPIB communication
@iC_Annotation( CommPorts=CommPorts.GPIB,
                InstrumentClassName="HP 3561")
public class HP3561 extends Device {


    ///////////////////
    // member variables

    /**
     * The Logger for this class. Note that the Logger Level is set in the
     * constructor of the <code>Device</code> class.
     */
    private static final Logger m_Logger = Logger.getLogger("iC.Instruments.HP3561");
    
    // define a new data type for the returned answer
    // Apache's Common Math would also offer complex numbers
    // TODO delme
    // <editor-fold defaultstate="collapsed" desc="MValue">
    private class MValue {

        // complex number
        private float   Z = Float.NaN;
        private float   Phi = Float.NaN;

    }//</editor-fold>

    // flag to remember if Init() was called
    // TODO delme
    private boolean m_Initialized = false;


    /**
     * Override the default <code>Open</code> method in the <code>Device</code>
     * class because this Instrument does not support IEEE 488.2 calls which
     * are used in the default implementation.<p>
     * 
     * It is convenient to clear the queue here.
     * 
     * @throws IOException if the queue could not be cleared; bubbles up from
     * <code>ClearQueue</code>.
     */
    // <editor-fold defaultstate="collapsed" desc="Open">
    @Override
    public void Open()
           throws IOException {

        // clear the queue, just in case
        ClearQueue();
        
        // try to get the ID to
        String ID = QueryViaGPIB("ID?");
        icontrol.IcontrolView.DisplayStatusMessage("The ID of the HP3561 is: " + ID + "\n", false);
        
    }//</editor-fold>

    /**
     * Initialize the instrument.<p>
     *
     * This method performs a Syntax-Check.
     *
     * @param Range The Z/Y measurement range. Can be 1 to 8.
     * @param Averaging Determines if averaging should be turned on or off.
     * @param CircuitMode Determines whether serial or parallel circuit mode; see
     * ToolTips for the explanation
     * should be used.
     *
     * @throws IOException re-thrown from {@link Device#SendViaGPIB}
     *
     * @throws DataFormatException when the Syntax Check failed.
     *
     */
    // TODO @HP3561 Init() was just copied from an other HP Instrument and needs to be adapted if at all necessary
    // <editor-fold defaultstate="collapsed" desc="Init">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Initializes the Instrument.<br>Should be called once in every Script before making measurements.</html>",
        ParameterNames = {"Measurement Range { [1,8] }", "Averaging", "Circuit mode {1,2,3} "},
        DefaultValues = {"8", "false", "1"},
        ToolTips = {"<html>1: 1 Ohm/10 S<br>7: 1MOhm/10uS<br>8: Auto</html>", "",
                    "<html>1: Auto<br>2: Serial<br>3: Parallel."})
    @iC_Annotation(  MethodChecksSyntax = true )
    public void Init(int Range, boolean Averaging, int CircuitMode)
           throws IOException, DataFormatException {


        ///////////////
        // Syntax-Check

        // Syntax check for Range
        if ( Range < 1 || Range > 8 ) {
            String str = "The Measurement Range must be between 1 and 8.\n";

            throw new DataFormatException(str);
        }

        // Syntax check for Circuit Mode
        if ( CircuitMode < 1 || CircuitMode > 3 ) {
            String str = "The CircuitMode must be between 1 and 3.\n";

            throw new DataFormatException(str);
        }

        // remember that Init was called
        m_Initialized = true;

        // return if in Syntax-check mode
        if (inSyntaxCheckMode())
            return;

        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return;



        ///////////////////////
        // GPIB communication ?
        if (m_UsedCommPort == CommPorts.GPIB) {

            // clear the output queue (service the Instrument if service is requested)
            ClearQueue();

            // wait a bit (computer is faster than HP4192)
            try { Thread.sleep(350); } catch (InterruptedException ex) {}


            // Display |Z|, phi
            String dummy = "A1B1";

            // *** Bias on
            dummy += "I1";

            // XY Recorder off
            dummy += "X0";

            // log sweep off
            dummy += "G0";

            // circuit mode auto (paralles/serial)
            dummy += "C" + CircuitMode;

            // ZY Range (1..7, 8=Auto)
            dummy += "R" + Integer.toString(Range);

            // High Speed off
            dummy += "H0";

            // Average off(0=normal)/on(1=average)
            dummy += "V" + (Averaging ? "1" : "0");

            // Trigger hold/manual
            dummy += "T3";

            // Data ready (send SRQ)
            dummy += "D1";

            // Data format A/B/C
            dummy += "F1";

            // SelfTest
            //dummy += "S1";

            // debugging log
            m_Logger.finer("Will be sending Init Strings.\n");

            // send to device
            SendViaGPIB(dummy);

            // in the original C++ program I serviced the device here
        }
    }//</editor-fold>



    /**
     * Checks if the Instrument requests service, and if it does, the Instrument
     * is addressed to talk so that the un-fetched data is read to empty the
     * output buffer of the 3561 and end the service request.<p>
     *
     * NOTE: Receiving the Status Byte is only implemented when using driver classes
     * derived from <code>GPIB_Driver</code> and an Exception is thrown if some other
     * driver is used (at the time of writing, no such driver exists).
     *
     * @throws IOException When GPIB communication caused an error
     */
    // TODO @HP3561 ClearQueue() was just copied from an other HP Instrument and needs to be adapted if at all necessary
    // <editor-fold defaultstate="collapsed" desc="Clear Queue">
    private void ClearQueue()
            throws IOException {

        // return if in No-Communication Mode
        if (inNoCommunicationMode())
            return;

        // check if a driver class derived from GPIB_Driver is used
        if (m_GPIB_Driver != null) {

            // debugging log
            m_Logger.finest("Entering ClearQueue.\n");

            // get the Status Byte
            int Status = m_GPIB_Driver.ReadStatusByte();

            // debugging log
            m_Logger.log(Level.FINER, "Status Byte = {0}\n", Integer.toString(Status));

            // check for an error
            if ( (Status & 0x40) > 0) {
                // log event
                m_Logger.fine("The 3561 requested service. Will now read from buffer.\n");

                // Instrument is requesting service, so read from the Instrument
                m_GPIB_Driver.Receive(false);
            } else {
                // log event
                m_Logger.fine("The 3561 did not request service\n");
            }

            // get the Status Byte again
            Status = m_GPIB_Driver.ReadStatusByte();

            // debugging log
            m_Logger.log(Level.FINER, "Status Byte = {0}\n", Integer.toString(Status));

            // check for errors
            if ( (Status & 0x40) > 0) {
                // just log the event
                m_Logger.warning("The 3561 is still requesting service\n");
            }
        } else {
            String str = "It seems a new driver has been implemented but not been\n"
                    + "considered in HP3561.ClearQueue(). Plese tell the developer.\n";

            // log event
            m_Logger.severe(str);

            // show to the user
            //icontrol.IcontrolView.DisplayStatusMessage(str);
            throw new IOException(str);
        }
    }//</editor-fold>


    /**
     * Sets the oscillation amplitude.<p>
     *
     * This method performs a Syntax-Check.
     *
     * @param ACLevel The oscillation (AC) amplitude used to measure the 
     * impedance; only 4 digits after the comma are relevant.
     *
     * @throws IOException re-thrown from {@link Device#SendViaGPIB}
     *
     * @throws DataFormatException when the Syntax Check failed.
     */
    // TODO @HP3561 copied from an other HP Instrument as an example
    // <editor-fold defaultstate="collapsed" desc="set AC Amplitude">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Sets the oscillation amplitude.</html>",
        ParameterNames = {"AC amplitude [V] { [0.005,1.1] }"},
        DefaultValues = {"0.1"},
        ToolTips = {""})
    @iC_Annotation(  MethodChecksSyntax = true )
    public void setAC_Amplitude(double ACLevel)
           throws IOException, DataFormatException {


        ///////////////
        // Syntax-Check

        // Syntax check for max. AC level
        if ( ACLevel < 0.005 || ACLevel > 1.1 ) {
            String str = "The AC amplitude must be between 5 mV and 1.1 V.\n";

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

            // build the string
            String dummy = String.format(Locale.US, "OL%+4.4fEN", ACLevel);

            // send to device
            SendViaGPIB(dummy);
        }
    }//</editor-fold>


   
    /**
     * Triggers the device to make a measurement.<p>
     *
     * @throws IOException re-thrown from {@link Device#SendViaGPIB}
     */
    // TODO @HP3561 copied from an other HP Instrument as an example
    // <editor-fold defaultstate="collapsed" desc="Trigger">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Triggers the device to make a measurement.</html>",
        ParameterNames = {},
        DefaultValues = {},
        ToolTips = {})
    public void Trigger()
           throws IOException {


        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return;


        ///////////////////////
        // GPIB communication ?
        if (m_UsedCommPort == CommPorts.GPIB) {

            // send to device
            SendViaGPIB("EX");
        }
    }//</editor-fold>


    /**
     * "Cleans-up" the Instrument after processing the script has been finished.
     * It sets the Spot-Bias to 0.
     * 
     * @throws IOException 
     */
    // TODO @HP3561 is there anything to clean up?
    @Override
    public void Close() 
           throws IOException {
        
        
    }
}
