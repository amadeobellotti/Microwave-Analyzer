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
import icontrol.iC_Properties;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.ScriptException;

/**
 * This class provides access to the IOtech Micro488/A-901 RS232-GPIB 
 * controller.<p>
 * 
 * Note: The at-character (@) has a special meaning (clear the serial input). If 
 * the commands/data contain the at-character, it should be disabled using the 
 * ID command.<p>
 * 
 * Note on DIP switch settings in the Micro 488:<br>
 * The Micro488 is assumed to be the System Controller (SW2-1,2: on, on)<br>
 * Serial echoing should be disabled (SW 2-5: on)<br>
 * Serial handshaking can likely be set to hardware (SW 1-5: on) if a 5-wire
 * cable is used.<br>
 * IEEE terminator selection depends on the instruments, CR+LF and EOI disabled
 * is recommended (SW3-6,7,8: off, off, on)<br>
 * A baud rate of 9600 is recommended to begin with (SW 1-1,2,3,4: off, on, off, off<br>
 * 8 data bits are recommended (SW 1-6: on)<br>
 * 2 stop bit are recommended (SW 1-8: off)<br>
 * no parity is recommended (SW 2-6: on)<br>
 * IEEE address can be chosen arbitrarily, but address 30 is recommended 
 * (SW 3-1,2,3,4,5: off, on, on, on, on)<br>
 * Choose the proper signal levels (RS232 or RS422), most likely you have RS232,
 * so the jumper needs to be in J206<br>
 * <p>
 * 
 *
 * <h3>Usage / How this class works:</h3>
 * <ul>
 *  <li>Instantiate the class. The constructor calls <code>OpenRS232</code>.
 *  <li><code>OpenRS232</code> establish access to the RS232 serial port, and 
 *      then calls <code>InitIOtech</code>.
 *  <li>Open an Instrument at a specific GPIB address using <code>Open</code>,
 *      which stores the GPIB address and sends a Selected-Device-Clear to the
 *      Instrument at the specified GPIB address.
 *  <li>To send/receive data use <code>Send</code> and <code>Receive</code>.
 *  <li>To close the connection to an Instrument call <code>CloseInstrument</code>.
 *  <li>Once done with all GPIB communication, close the connection to the
 *      GPIB controller with <code>CloseController</code>.
 *  <li>To receive the Status Byte - especially of older equipment that is not
 *      488.2 compliant but implements 488 only, hence, no *STB? command -
 *      <code>ReadStatusByte</code> can be used.
 * </ul>
 *
 *
 * @author KPP (Kurt Pernstich: pernstich@alumni.ethz.ch)
 * @version 0.1
 *
 */
public class GPIB_IOtech extends GPIB_Driver {

    ///////////////////
    // member variables

    /** The Logger for this class */
    private static final Logger m_Logger = Logger.getLogger("iC.Instruments.GPIB_IOtech");

    /** Grants access to global properties in a resource bundle file 'iC.properties' */
    protected static iC_Properties m_iC_Properties;

    /** GPIB Address of the Instrument */
    private int m_GPIB_Adr;

    /** Handle to access the RS232 port */
    private RS232_Driver m_RS232Driver = null;
    
    /** Stores the status (error) of the last communication with the IOtech
     * controller. Assigned in <code>checkCommunicationError</code> and used
     * in <code>getGPIBStatus</code>. */
    private String m_LastError = "";
    
    /* for development without RS232 port and IOtech controller; it can be
     * set in iC.properties/GPIB_IOtech.Debug */
    private final boolean m_IOtechDebug;


    /**
     * Constructor. Does some initializations and calls <code>OpenRS232</code>
     * to open the RS232 port. <code>OpenRS232</code> calls <code>InitIOtech</code>
     * to initializes the Micro 488 GPIB controller.
     *
     * @throws IOException bubbles up from <code>OpenRS232</code>.
     */
    // <editor-fold defaultstate="collapsed" desc="Constructor">
    protected GPIB_IOtech()
           throws IOException {

        // init Logger to inherit Logger level from Parent Logger
        m_Logger.setLevel(null);

        // instantiate iC_Properties
        m_iC_Properties = new iC_Properties();
        
        // assign debug mode
        m_IOtechDebug = Boolean.parseBoolean(
            m_iC_Properties.getString("GPIB_IOtech.DebugNoRS232", "false") );
        
        // open the RS232 port
        OpenRS232();

    }//</editor-fold>


    /**
     * Checks the Status of the last communication with the IOtech controller
     * and throws an Exception if an error occurred.<p>
     * 
     * If an error occurred, this event is also Logged with level SEVERE. IOtech's
     * system command STATUS 1 is used to determine if an error occurred. It is 
     * recommended to call this method after each invocation of
     * <code>WriteToIOtech</code>.<p>
     * 
     * The error message is also stored in a member variable and used in 
     * <code>getGPIBStatus</code> to return the GPIB status of the last
     * GPIB communication.
     *
     * @param Msg This String is appended to the message thrown as an
     * Exception when an error occurred.
     * @throws IOException when an error occurred during the last GPIB operation
     * or the reported Status of the IOtech controller was invalid.
     */
    // <editor-fold defaultstate="collapsed" desc="check Communication Error">
    private void checkCommunicationError(String Msg)
            throws IOException {
        
        // return during development
        if (m_IOtechDebug)
            return;
        
        // ask for the Status of the IOtech
        WriteToIOtech("STATUS 1");
        
        // receive Status
        String Status = ReadFromIOtech();
        
        // log Status
        m_Logger.log(Level.FINEST, "Status: {0}\n", Status);
        
        // get error number
        int ErrorNumber;
        int ServiceRequest;
        try {
            //Status = "C 10 G0 I S1 E01 T0 C0 notOK";
            //Status = " notOK";
            ErrorNumber = Device.getInteger(Status.substring(14, 16));
            ServiceRequest = Device.getInteger(Status.substring(11, 12));
            
        } catch (Exception ex) {
            // catches ScriptExceptions from getInterger and 
            // IndexOutOfBounds exceptions if the String was not long enough
            String str = "Could not extract the error number from the Status of the IOtech GPIB controller\n"
                    + "because the received Status was invalid:\n" + Status + ".\n";
            
            throw new IOException(str);
        }

        // check for communication errors
        if (ErrorNumber != 0) {
            // build the string
            String str = Msg + "Error message: " + Status.substring(23) + "\n"
                    + "Service Request: " + (ServiceRequest == 0 ? "no\n" : "yes\n");

            // log event
            m_Logger.severe(str);
            m_GPIB_Logger.severe(str);
            
            // remember error number and message
            m_LastError = "Error " + ErrorNumber + ": " + Status.substring(23) + "\n";

            throw new IOException(str);
        } else {
            // remember that no error occurred
            m_LastError = "";
        }
    }//</editor-fold>



    /**
     * Opens the RS232 port using the parameters defined in 
     * <code>iC.Properties</code>. It also calls <code>InitIOtech</code> which
     * initializes the GPIB controller (e.g. to make it controller in charge etc.)
     *
     * @throws IOException When 1) opening the RS232 port with the specified
     * communication parameters failed, or 2) an exception was thrown in 
     * <code>initIOtech</code>.
     */
    // <editor-fold defaultstate="collapsed" desc="Open IOtech">
    final protected void OpenRS232()
              throws IOException {
        
        // make a new object for RS232 communication
        m_RS232Driver = new RS232_Driver();
        

        // get the Communication parameters
        String PortName = m_iC_Properties.getString("GPIB_IOtech.PortName", "COM1");
        int BaudRate = m_iC_Properties.getInt("GPIB_IOtech.BaudRate", 9600);
        int NrDataBits = m_iC_Properties.getInt("GPIB_IOtech.NrDataBits", 8);
        int NrStopBits = m_iC_Properties.getInt("GPIB_IOtech.NrStopBits", 1);
        String Parity = m_iC_Properties.getString("GPIB_IOtech.Parity", "none");

        // don't try to open port during development
        if (m_IOtechDebug)
            return;
        
        // open RS232 port
        try {
            m_RS232Driver.OpenPort(PortName, BaudRate, NrDataBits, NrStopBits, Parity);
        } catch (ScriptException ex) {
            // re-throw as IOException
            throw new IOException(ex.getMessage());
        }

        
        //////////////////
        // init controller
        InitIOtech();

    }//</editor-fold>



    /**
     * Initializes the IOtech Micro 488 GPIB-RS232 controller to it's power-on
     * state (command @@), becomes controller-in-charge (ABORT). The response to the
     * HELLO-command is logged. It also sets the serial termination character 
     * (STERM, p. 4-65) to none (the data can contain any value between 0-255), 
     * and sets the time-out value.
     *
     * @throws IOException When communicating with the IOtech controller failed.
     */
    // <editor-fold defaultstate="collapsed" desc="Init IOtech">
    private void InitIOtech()
            throws IOException {        
        
        // reset to power-on state
        WriteToIOtech("@@");
        
        // wait a bit
        try{Thread.sleep(3000);} catch (InterruptedException ignore) {}
        
        // get the name of the controller
        WriteToIOtech("HELLO");
        String Response = ReadFromIOtech();
        
        // log response
        m_Logger.log(Level.CONFIG, "IOtech HELLO''d: {0}\n", Response);

        // display the controller's name
        // TODO delme when HELLO works
        IcontrolView.DisplayStatusMessage("IOtech's name: " + Response, false);
        
        // become controller-in-charge (active controller)
        WriteToIOtech("ABORT");        
        String str = "An error occurred when trying to become Controller-In-Charge.\n";
        checkCommunicationError(str);
        
        // do not send Device-Clear to all devices on the bus, just to the ones used in the
        // script in Open (command CLEAR)
        
        // Do not send any serial output termination character
        // could be mistaken as a data byte
        WriteToIOtech("STERM NONE");
        str = "Could not set serial termination character (STERM) to none.\n";
        checkCommunicationError(str);
        
        // Maybe set IEEE terminator (make it an iC.properties)
        
        // set Time-Out value
        String to = m_iC_Properties.getString("GPIB_IOtech.TimeOut", "3");
        WriteToIOtech("TIME OUT " + to);
        str = "Could not set IOtech's time-out value.\n";
        checkCommunicationError(str);
        
        // TODO 2* test to disable ID character
        // disable ID character
        // the symbol '@' can be part of the data stream, so do not use it as
        // system command. See ID command on page 4.9
//        WriteToIOtech("ID;\"\"");
//        str = "Could not set IOtech's time-out value.\n";
//        checkCommunicationError(str);

    }//</editor-fold>



    /**
     * Closes the connection to the GPIB controller by closing the RS232 port. 
     * This method is called from <code>Dispatcher.run</code> after the Script 
     * has been processed.<p>
     * 
     * The IOtech GPIB controller cannot be switched into a passive mode like
     * the NI or the Prologix GPIB controllers. Hence, it is not possible to
     * not be the active System Controller and allow for multiple GPIB controllers
     * to be attached at the same time. The command would be PASS CONTROL on 
     * page 4-45.
     *
     * @throws IOException When the closing caused a GPIB error
     */
    // <editor-fold defaultstate="collapsed" desc="CloseController">
    @Override
    protected void CloseController() 
              throws IOException {

        // log
        m_Logger.fine("Closing connection to the GPIB controller (GPIB_IOtech).\n");
        
        // close communication to the RS232 port
        m_RS232Driver.ClosePort();
        
        // assign null to detect any calls after I don't expect any
        m_RS232Driver = null;
    }//</editor-fold>




    /**
     * Opens the connection to the Instrument at the specified GPIB address.<p>
     *
     * Because there are no special requirements to open an instrument, only a
     * Selected Device Clear is sent.
     *
     * @param GPIB_Address The primary GPIB address of the Instrument
     *
     * @throws IOException When a GPIB transaction caused a GPIB error (bubbles
     * up from <code>checkErrorGPIB</code>.
     *
     */
    // <editor-fold defaultstate="collapsed" desc="Open">
    @Override
    protected void Open(int GPIB_Address) throws IOException {

        // store GPIB address
        m_GPIB_Adr = GPIB_Address;

        // send Device Clear to selected device
        WriteToIOtech( String.format("CLEAR %02d", m_GPIB_Adr) );
        String str = "Could not send Device-Clear to the instrument at GPIB address " + m_GPIB_Adr + ".\n";
        checkCommunicationError(str);

    }//</editor-fold>




    /**
     * Sends the given String to the IOtech GPIB-RS232 controller. A newline
     * character (\n) is appended to <code>Msg</code> if none is present already
     * to terminate serial transmission (see STERM in IOtech's manual).<p>
     * 
     * To check for communication errors, use <code>checkCommunicationError</code> 
     * after each use of <code>WriteToIOtech</code> (with a few exceptions in 
     * <code>InitIOtech</code> for instance).
     *
     * @param Msg The data to be sent. A newline character will be appended if none
     * is present to indicate the end of transmission via RS232.
     * @throws IOException When the transmission caused an error
     */
    // <editor-fold defaultstate="collapsed" desc="Write To IOtech">
    private synchronized void WriteToIOtech(String Msg)
                         throws IOException {

        // append \r or \n so that the IOtech controller recognizes the end of transmission
        final String append = "\n";
        if ( !Msg.endsWith(append) )
            Msg += append;
        
        
        // log what's about to be sent
        m_Logger.log(Level.FINEST, "WriteToIOtech will send: {0}", Msg);


        // send to the Instrument via RS232
        if ( !m_IOtechDebug )
            m_RS232Driver.Send(Msg.getBytes(Charset.forName("UTF-8")));
        
        
        // cannot check for communication errors here because checkCommunicationError 
        // uses WriteToIOtech which would cause an infinite loop
        // P.S. The Prologix's checkCommErr does not use WriteToPrologix.

    }//</editor-fold>


    /**
     * Sends the given String via the GPIB bus to the Instrument specified in
     * <code>Open</code>. If <code>Msg</code> is empty, the method returns immediately.<p>
     *
     * @param Msg Data to be sent
     * @return The length of <code>Msg</code> because the actual number of bytes
     * sent is not reported by the IOtech controller.
     * @throws IOException When the transmission caused a GPIB error
     */
    // <editor-fold defaultstate="collapsed" desc="Send">
    @Override
    protected long Send(String Msg)
              throws IOException {
        
        // do nothing if Msg is empty. See comment in GPIB_NI.Send
        if (Msg.isEmpty()) {
            return 0;
        }
        
        // build the commands string
        // could use ("OUTPUT %2d#%d;%s", m_GPIB_Adr, Msg.length(), Msg);
        // but then the IEEE terminator needs to be appended too (see TERM, p. 4-66)
        String cmd = String.format("OUTPUT %02d;%s", 
                m_GPIB_Adr, Msg);


        // send the message
        WriteToIOtech(cmd);
        
        // wait a bit (500 might be too long, 250 is too short)
        try {Thread.sleep(500);} catch (InterruptedException ignore) {};
        
        String str = "An error occurred upon sending data to the IOtech GPIB-RS232 controller.\n";
        checkCommunicationError(str);

        // return the number of bytes sent
        return Msg.length();

    }//</editor-fold>



    /**
     * Returns the status of the last communication with the IOtech controller.
     * The last status is stored in a member variable in method
     * <code>checkCommunicationError</code>, which should be called each time
     * after invoking <code>WriteToIOtech</code>.
     *
     * @return When an error occurred during the last GPIB call, the returned
     * String contains a somewhat helpful explanation of what went
     * wrong. An empty String is returned if no error occurred.
     */
    // <editor-fold defaultstate="collapsed" desc="get GPIB Status">
    @Override
    protected String getGPIBStatus() {

        // log event
        m_Logger.finer(m_LastError);
        //m_GPIB_Logger.finer(str);

        return m_LastError;
    }//</editor-fold>


    /**
     * Read data from the IOtech GPIB-RS232 controller until no more data was 
     * received via RS232 for a time-out period specified in 
     * <code>iC.properties / RS232_Driver.TimeOut</code>.
     * 
     * @return The data read from the controller. The String's length equals
     * the number of bytes received from the IOtech controller.
     * @throws IOException When the transmission caused an error (bubbles up from
     * <code>RS232_Driver.Receive</code>.
     */
    // <editor-fold defaultstate="collapsed" desc="Read From IOtech">
    private synchronized String ReadFromIOtech()
                         throws IOException {

        // local variables
        String ret = "";
        
        // return when developing
        if (m_IOtechDebug)
            return "IOtechDebug mode enabled";

        // read from the serial port
        byte[] ans = m_RS232Driver.Receive();
        
        // convert to a String
        ret = new String(ans, Charset.forName("UTF-8"));
        
        // log number of bytes read
        m_Logger.log(Level.FINEST, "ReadFromIOtech just read {0} bytes\n",
                Integer.toString(ret.length()));


        return ret;
    }//</editor-fold>



    /**
     * Receives data from the Instrument specified in <code>Open</code>. This 
     * method returns when no data was received via the RS232 port for a time-out
     * value specified in RS232_Driver.TimeOut in the iC.properties.
     *
     * @param Trim If <code>true</code>, newline (\n) and carriage return (\r)
     * characters are removed from the end of the returned String.
     * @return The data bytes sent by the Instrument wrapped in a String.
     * @throws IOException When a transmission error occurred (bubbles up from
     * <code>WriteToIOtech</code> or <code>ReadFromIOtech</code>
     */
    // <editor-fold defaultstate="collapsed" desc="Receive">
    @Override
    protected String Receive(boolean Trim)
              throws IOException {
        
        // address the instrument to talk
        WriteToIOtech(String.format("ENTER %02d", m_GPIB_Adr));
        
        // wait a bit (500 might be too long, 250 is too short)
        try {Thread.sleep(500);} catch (InterruptedException ignore) {};
        
        String str = "An error occurred when trying to receive data via the IOtech GPIB-RS232 controller.\n";
        checkCommunicationError(str);

        // read the data
        String ret = ReadFromIOtech();

        // remove trailing newline & carriage return
        if (Trim)
            ret = ret.replaceFirst("[\\n[\\r]]+$", "");

        return ret;
    }//</editor-fold>



    /**
     * Reads the Status Byte of the Instrument defined in <code>Open</code>
     * by serially polling the Instrument. <code>Open</code> must have been
     * called before calling this method.
     *
     * @return The Status Byte of the Instrument.
     * @throws IOException When the transmission caused a GPIB error or the
     * received Status Byte could not be converted into an Integer value.
     */
    // <editor-fold defaultstate="collapsed" desc="Read Status Byte">
    @Override
    protected int ReadStatusByte()
              throws IOException {

        // ask for the Status Byte by serially polling
        String poll = String.format("SPOLL %02d", m_GPIB_Adr);
        WriteToIOtech(poll);
      

        // read the Status Byte
        String StatusByte = ReadFromIOtech();

        // convert the Status Byte
        int Status;
        try {
            Status = Device.getInteger(StatusByte);
        } catch (ScriptException ex) {
            String str = "Could not convert the received Status Byte into a valid number.\n";
            str += ex.getMessage();

            throw new IOException(str);
        }
        
        // return the Status Byte
        return Status;
    }//</editor-fold>
    

    /**
     * Closes the connection to the Instrument specified in <code>Open</code>.
     * This method is called from <code>Dispatcher.run</code> after the Script
     * has been processed.<p>
     *
     * This method switches the Instrument to a local mode.
     *
     * @throws IOException When the closing caused a GPIB error
     */
    // <editor-fold defaultstate="collapsed" desc="Close Instrument">
    @Override
    protected void CloseInstrument() 
              throws IOException {

        // log
        m_Logger.log(Level.FINE, "Closing connection to the Instrument at address {0}.\n",
                Integer.toString(m_GPIB_Adr));

        // put instrument to local mode
        String dummy = String.format("LOCAL %02d", m_GPIB_Adr);
        WriteToIOtech(dummy);

        // check for communication errors
        String str = "Could not put the Instrument into local mode.\n";
        checkCommunicationError(str);

    }//</editor-fold>
}
