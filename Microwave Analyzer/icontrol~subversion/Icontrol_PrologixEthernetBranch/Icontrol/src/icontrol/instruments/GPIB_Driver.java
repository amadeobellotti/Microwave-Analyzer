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

import icontrol.IcontrolView;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract class defining the required methods for a derived class to be
 * recognized as a GPIB driver. At the time of writing, two derived classes
 * (<code>GPIB_NI</code> and <code>GPIB_Prologix</code>) implement this
 * 'interface'. This class is defined abstract, as opposed to be defined as an
 * Interface, because 1) an Interface only allows public methods, and 2) it
 * is unlikely that a derived class would like to extend an other class as well.
 *
 * @author KPP (Kurt Pernstich: pernstich@alumni.ethz.ch)
 * @version 0.1
 *
 */
abstract class GPIB_Driver {

    /** The GPIB Logger */
    protected static final Logger m_GPIB_Logger = Logger.getLogger("GPIB");


    /**
     * Opens the connection to the Instrument at the specified GPIB address.
     *
     * @param GPIB_Address The primary GPIB address of the Instrument
     *
     * @throws IOException When a GPIB transaction caused a GPIB error (bubbles
     * up from <code>checkErrorGPIB</code>.
     *
     */
    abstract protected void Open(int GPIB_Address) throws IOException;

    /**
     * Sends the given String via the GPIB bus to the Instrument specified in
     * <code>Open</code> and returns the number of bytes actually sent.<p>
     *
     * @param Msg Data to be sent
     * @return Number of data bytes actually sent
     * @throws IOException When the transmission caused a GPIB error
     */
    abstract protected long Send(String Msg) throws IOException;

    /**
     * Receives data from the Instrument specified in <code>Open</code>.
     * The number of Bytes that are requested from the Instrument should be specified
     * by <code>RECEIVE_BUFFER_SIZE</code>, which, in turn, should be specified in the
     * iC.property file. Currently this value is 150000 which has proven useful
     * in my personal experience. Increase this value if an Instrument places
     * more data in the output queue. A number which is too small will most
     * likely result in an erroneous transmission (<code>Receive</code> does
     * not check if all data bytes have been read).
     *
     * @param Trim If <code>true</code>, newline (\n) and carriage return (\r)
     * characters are removed from the end of the returned String.
     * @return The data bytes sent by the Instrument wrapped in a String.
     * @throws IOException When the transmission caused a GPIB error
     */
    abstract protected String Receive(boolean Trim) throws IOException;


    /**
     * Gets the Status of the last GPIB call. After each GPIB call, the
     * error flag in the Status Byte should, ideally, be checked. If an error
     * occurred, this method returns a String with a short description of what
     * went wrong. If an error occurred, it should be logged to the GPIB Logger.
     *
     * @return When an error occurred during the last GPIB call, the returned
     * String contains a somewhat helpful explanation of what went
     * wrong. An empty String is returned if no error occurred.
     */
    abstract protected String getGPIBStatus();

    /**
     * Reads the Status Byte of the Instrument defined in <code>Open</code>
     * by serially polling the Instrument. <code>Open</code> must have been
     * called before calling this method.
     *
     * @return The Status Byte of the Instrument
     * @throws IOException When the transmission caused a GPIB error
     */
    abstract protected int ReadStatusByte() throws IOException;

    /**
     * Closes the connection to the Instrument specified in <code>Open</code>.
     * This method is called from <code>Dispatcher.run</code> after the Script
     * has been processed.
     *
     * @throws IOException When the closing caused a GPIB error
     */
    abstract protected void CloseInstrument() throws IOException;

    /**
     * Closes the connection to the GPIB controller. This method is called from
     * <code>Dispatcher.run</code> after the Script has been processed.
     *
     * @throws IOException When the closing caused a GPIB error
     */
    abstract protected void CloseController() throws IOException;


    /**
     * Used to test the speed of the GPIB controller. Enable the Speed Test in
     * the <code>Open</code> method respectively issue the ScriptCommand
     * SpeedTest NrIterations (no Auto GUI is specified for this Script Command).
     *
     * Initial tests with a very limited number of test cases gave the following
     * results:<br>
     * Upon testing the speed with the National Instruments PCI-GPIB controller
     * on WinXP (rack computer) the average time between successive IDN queries
     * of a Lakeshore 340 was 31 ms. It never exceeded 32 ms, and sometimes it
     * was as short as 16 ms. Again, only a very limited number of test cases was
     * considered.<p>
     *
     * Upon testing the speed with a Prologix GPIB-USB controller on WinXP (rack
     * computer, Lakeshore 340) it was found necessary to set a minimal USB
     * TimeOut value of 35 ms. While 50 ms worked very often, long time tests
     * indicated that 50 ms might be too short, hence, 75 ms is recommended. The
     * maximum total turn-around time was 47 ms (for 35 ms delay), 63 ms (for
     * 50 ms delay) and 79 ms (for 75 ms delay).<p>
     * In the initial tests only a very low number of test cases were considered.
     * See also iC_Test_Protocol.txt.<p>
     *
     * Using a National Instruments GPIB-USB-HS on a Mac resulted in average
     * turn around times of 16 ms (maximum 26 ms) for a SRS DS345.<p>
     */
    // <editor-fold defaultstate="collapsed" desc="Speed Test">
    protected void SpeedTest(int NrIterations) {

        Logger myLogger = Logger.getLogger("iC.Instruments.GPIB_Driver");

        // initial wait
        try { Thread.sleep(250); } catch (InterruptedException ex) {}
        myLogger.log(Level.FINER, "Waited 250ms\n");

        // display status msg
        IcontrolView.DisplayStatusMessage("Starting GPIB_Driver.SpeedTest().\n", false);


        // init Timer
        long T = System.currentTimeMillis();
        long max_dT = 0;

        // test it multiple times
        for (int i=0; i<NrIterations; i++) {
            try {

                //Write *idn?
                Send("*IDN?");

                //Read response
                String idn = Receive(true);

                // calc dT
                long dummy = System.currentTimeMillis();
                long dT = dummy - T;
                T = dummy;
                if (max_dT < dT)
                    max_dT = dT;

                // show (and log) results
                IcontrolView.DisplayStatusMessage(String.format("%3d (%4dms): %s\n", i, dT, idn), false);

            } catch (IOException ex) {
                // show and log error
                IcontrolView.DisplayStatusMessage("GPIB_Driver.SpeedTest: IO failed.\n", false);
                return;
            }
        }

        // display status msg
        IcontrolView.DisplayStatusMessage("max dT = " + max_dT + "\n", false);
        IcontrolView.DisplayStatusMessage("The GPIB_Driver.SpeedTest is done.\n", false);
    }//</editor-fold>
}
