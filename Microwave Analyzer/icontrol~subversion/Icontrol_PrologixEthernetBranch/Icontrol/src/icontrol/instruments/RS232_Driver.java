// TODO 4* would it make sense to rename GPIB_Logger to Comm_Logger

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

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import icontrol.iC_Properties;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.ScriptException;

/**
 * "Driver" class to support RS232 communication in Instrument Control (iC).<p>
 * 
 * To convert between <code>String</code> and <code>byte[]</code> use the
 * UTF-8 character set, for instance String.getBytes(Charset.forName("UTF-8"))<p>
 * 
 * How to use the RS232_Driver class:<br>
 * Instantiate this class and call <code>OpenPort</code>. To send/receive data
 * use <code>Send</code> and <code>Receive</code>. Don't forget to 
 * call <code>ClosePort</code> to close the port after use.
 * 
 * @author KPP (Kurt Pernstich: pernstich@alumni.ethz.ch)
 * @version 0.1
 *
 */
public class RS232_Driver {
    
    /** The Logger for this class */
    private static final Logger m_Logger = Logger.getLogger("iC.Instruments.RS232_Driver");
    
    /**
     * Grants access to global properties of the iC application in a resource
     * bundle file 'iC.properties'
     */
    protected static iC_Properties m_iC_Properties;
    
    /** Handle to access the RS232 port */
    private SerialPort m_Port;
    
    /** Stores the port name (for instance COM1) */
    private String m_PortName;
    
    /** The input stream of the RS232 port */
    private InputStream m_InStream;
    
    /** The output stream of the RS232 port */
    private OutputStream m_OutStream;
    
    /** Time in ms it takes to transmit one byte; used in <code>Send</code> */
    private float m_TimeToTransmit = 0;
    
    
    
    /**
     * Constructor. Initializes the Logger for this class. To open the serial
     * port call <code>OpenPort</code>.<p>
     */
    // <editor-fold defaultstate="collapsed" desc="Constructor">
    public RS232_Driver() {
        
        // init Logger to inherit Logger level from Parent Logger
        m_Logger.setLevel(null);
        
        // instantiate iC_Properties
        m_iC_Properties = new iC_Properties();
    }//</editor-fold>
    
    
    /**
     * Opens the specified serial port using the specified parameters for baud rate,
     * number of data and stop bits, and parity.
     * 
     * @param ComPortName The name of the RS232 port, for instance 'COM1'.
     * @param BaudRate The baud rate for RS232 communication. For GPIB communication
     * this parameter is ignored.
     * @param DataBits The number of data bits for RS232 communication. For GPIB communication
     * this parameter is ignored.
     * @param StopBits The number of stop bits for RS232 communication. For GPIB communication
     * this parameter is ignored.
     * @param Parity The parity for RS232 communication. Can be 'none', 'even', 
     * 'odd', or 'mark' (case insensitive). For GPIB communication this parameter is ignored.
     * 
     * @throws ScriptException when the specified COM port 1) was already opened 
     * and, hence, cannot be opened again, or 2) could not be opened for some
     * (unspecified) reason, or 3) if the specified COM port was not found on the
     * system, 4) if the in-out streams could not be assigned, or an <code>IOException</code>
     * was thrown from <code>configRS232</code> because the specified serial port
     * parameters could not be set.
     */
    // <editor-fold defaultstate="collapsed" desc="Open Port">
    public void OpenPort(String ComPortName, int BaudRate, int DataBits, 
                         int StopBits, String Parity) 
           throws ScriptException {
        
        // init
        m_Port = null;
        
        // store Port Number
        m_PortName = ComPortName;
        
        // obtain all ports present on the system
        m_Logger.finer("Trying to get Port Identifiers...\n");
        Enumeration AllPorts = CommPortIdentifier.getPortIdentifiers();
        m_Logger.finer("Port Identifiers obtained.\n");
        
        // Remark: just found in RxTx's code examples an other way to open using:
        // CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
        
        // iterate through all ports
        while (AllPorts.hasMoreElements()) {
            
            // get "current" port
            CommPortIdentifier com = (CommPortIdentifier) AllPorts.nextElement();
            
            // log port
            m_Logger.log(Level.FINER, "Found port: {0}\n", com.getName());
                
            // is it a serial port with the specified name?
            if (com.getPortType() == CommPortIdentifier.PORT_SERIAL &&
                com.getName().equalsIgnoreCase(ComPortName)) {
                
                // log that the specified port was found
                m_Logger.log(Level.FINER, "{0} port was found.\n", ComPortName);
                
                try {
                    // open port
                    m_Port = (SerialPort) com.open("Instrument_Control_iC", 50);

                    // log success
                    m_Logger.log(Level.CONFIG, "{0} port was opened.\n", ComPortName);
                    
                    // assign in-out streams
                    m_InStream = m_Port.getInputStream();
                    m_OutStream = m_Port.getOutputStream();
                    
                    // just to be sure: check for null
                    if (m_InStream == null || m_OutStream == null) {
                        String str = "Could not obtain in/out streams for the RS232 port " 
                                + ComPortName + "\n" + "I don't have any suggestions.\n";
                        throw new ScriptException(str);
                    }

                } catch (PortInUseException e) {
                    
                    // throw exception if the port is in use
                    String str = "The serial port "  + com.getName() + " is in use.\n"
                            + "Please close the port and try again.\n"
                            + e.getMessage() + "\n";
                    
                    throw new ScriptException(str);

                } catch (Exception e) {
                    // throw exception
                    String str = "Failed to open serial port " +  com.getName() + "\n"
                            + "No reason was specified in RxTx, was it?\n"
                            + e.getMessage() + "\n";

                    throw new ScriptException(str);
                }
            }
            
            // check if a port was opened
            if (m_Port == null) {
                String str = "The specified RS232 port (" + ComPortName + ")\n" +
                        "was not found on the system.\n" +
                        "A list of serial port present on the system was logged.\n";
                throw new ScriptException(str);
            }
            
            // set port parameters
            try {
                configRS232(BaudRate, DataBits, StopBits, Parity);
                
            } catch (IOException ex) {
                // rethrow as ScriptException
                throw new ScriptException(ex);
            }
        }
    }//</editor-fold>
    
    
    /** 
     * Returns the Baud rate of the serial port.
     * @return The actual baud rate or 0 if the port was not opened before.
     */
    // <editor-fold defaultstate="collapsed" desc="get Baud Rate">
    int getBaudRate() { 
        
        if (m_Port != null) {
            return m_Port.getBaudRate(); 
        } else {
            return 0;
        }
    }//</editor-fold>
    
    /**
     * Closes the serial port.
     * 
     * @throws IOException When closing the in/out streams caused an error.
     */
    // <editor-fold defaultstate="collapsed" desc="Close Port">
    public void ClosePort() 
           throws IOException {
        
        // close in-out streams
        try {
            if (m_InStream != null) {
                m_InStream.close();
            }
            
            if (m_OutStream != null) {
                m_OutStream.close();
            }
        } catch (IOException ex) {
            String str = "Closing the in-out streams caused the error:\n"
                    + ex.getMessage() + "\n";
            throw new IOException(str, ex);
        }
        
        // close the port
        if (m_Port != null) {
            
            m_Port.close();
            
            // log event
            m_Logger.log(Level.INFO, "Closed RS232 port {0}.\n", m_PortName);
        } else {
            // log event
            m_Logger.log(Level.INFO, "Tried to close RS232 port {0} although it was not opened.\n", m_PortName);
        }
    }//</editor-fold>
    
    
    /**
     * Sends the given <code>byte[]</code> via the RS232 port (and flushes
     * the output stream to make sure the data is sent). Returns immediately
     * if <code>Message</code> is empty. After sending the data to the output
     * stream, this method waits the time it takes theoretically to send the data.
     * This is to ensure that the data could have been received before the program
     * proceeds (and attempts to read).<p>
     * 
     * @param Message The data bytes to be sent.
     * 
     * @throws IOException When an IO error occurred during sending to the
     * output stream. This should actually not occur.
     */
    // <editor-fold defaultstate="collapsed" desc="Send">
    protected void Send(byte[] Message) 
              throws IOException {
        
        // do nothing if Message is empty
        if (Message.length == 0) {
            return;
        }
        
        try {
            // send the message
            m_OutStream.write(Message);
            
            // flush output stream
            m_OutStream.flush();
            
            // to be save, wait until the data has been sent before proceeding
            // TODO 8* test TimeToTransmit
            if (m_TimeToTransmit > 0) {
                try { 
                    Thread.sleep(Math.round(Message.length * m_TimeToTransmit));
                } catch (InterruptedException ignore) {}
            }
            
            
        } catch (IOException ex) {
            String str = "An unexpected IO error occurred when sending to\n"
                    + "the RS232 port " + m_PortName + ":\n"
                    + ex.getMessage() + "\n";
            
            throw new IOException(str);
        }
    }//</editor-fold>
    
    
    /**
     * Reads from the serial port until no more data was received for a specified
     * time out value. Because serial transmission can be rather slow, it is assumed
     * that this method can be called before the receive buffer is entirely filled.
     * Therefore, the receive buffer is repeatedly read until no data was received within
     * the time out value defined in the iC_Properties.<p>
     * 
     * Note: In theory one could end transmission upon receiving a end-of-transmission
     * character, but because, in general, the data stream can contain any character
     * including this eot-character this is not recommended.
     * 
     * @return The data bytes received from the RS232
     * 
     * @throws IOException If an IO error occurred during reading the input
     * stream. This should actually not occur.
     */
    // <editor-fold defaultstate="collapsed" desc="Receive">
    protected byte[] Receive() 
              throws IOException {
        
        // local variables
        ArrayList<Byte> ans = new ArrayList<Byte>();
        int Attempts = 0;
        byte[] buffer = new byte[108];
        
        
        // time out value
        int TimeOut = m_iC_Properties.getInt("RS232_Driver.TimeOut", 250);
        
        try {
            do {
                // get number of available bytes
                int len = m_InStream.available();

                if (len > 0) {
                    // read from input stream
                    len = m_InStream.read(buffer);
                    
                    // append to return value
                    for (int i=0; i<len; i++) {
                        ans.add(buffer[i]);
                    }
                    
                    // wait short time (might speed up transmission in some cases)

                    // reset attempts
                    Attempts = 0;
                } else {
                    // no data is in the queue

                    // count attempts
                    Attempts++;

                    // if not waited long before, wait long
                    if (Attempts < 2) {
                        // wait for time-out period
                        try{ Thread.sleep(TimeOut); } catch (InterruptedException ex) {/*ignore*/}                        
                    }
                }
            } while (Attempts < 2);
            
        } catch (IOException ex) {
            String str = "An unexpected IO error occurred when reading from\n"
                    + "the RS232 port " + m_PortName + "\n"
                    + "The reason was:\n"
                    + ex.getMessage() + "\n";
            
            throw new IOException(str);
        }
        
        // convert to a byte[]
        byte[] ret = new byte[ans.size()];
        for (int i=0; i<ans.size(); i++) {
            ret[i] = ans.get(i);
        }
        
        // return the answer
        return ret;
    }//</editor-fold>
    
      
    
    /**
     * Configures the Baud rate, number of data and stop bits and the parity
     * of the RS232 port. Use the constants defined in <code>SerialPort<\code>
     * 
     * @param BaudRate The available Baud rates depend on the driver. If an unsupported
     * baud rate is specified, an <code>IOException<\code> is thrown.
     * 
     * @param NrDataBits The number of data bits; can be 5, 6, 7, or 8
     * @param NrStopBits The number of stop bits; can be 1, 2, or 3 (for 1.5 stop bits)
     * @param Parity The parity for the check of a successful transmission; can
     * be 'none', 'even', 'odd', or 'mark' (case insensitive).
     * 
     * @throws IOException When the specified parameters are not supported by the
     * RS232 driver.
     */
    // TODO 5* why is this called twice (also in syntax check mode?) - is it indeed?
    // <editor-fold defaultstate="collapsed" desc="Config RS232">
    protected void configRS232(int BaudRate, int NrDataBits, int NrStopBits, String Parity) 
              throws IOException {
        
        // convert Parity String to integer
        int ParityInt;
        if (Parity.equalsIgnoreCase("none")) ParityInt = SerialPort.PARITY_NONE;
        else if (Parity.equalsIgnoreCase("even")) ParityInt = SerialPort.PARITY_EVEN;
        else if (Parity.equalsIgnoreCase("odd")) ParityInt = SerialPort.PARITY_ODD;
        else if (Parity.equalsIgnoreCase("mark")) ParityInt = SerialPort.PARITY_MARK;
        else if (Parity.equalsIgnoreCase("space")) ParityInt = SerialPort.PARITY_SPACE;
        else {
            String str = "The parity '" + Parity + "'\n" +
                    "is not supported. Please choose from none, even, odd, mark, space.\n";
            throw new IOException(str);
        }

        try {
            // log serial port parameters
            m_Logger.log(Level.CONFIG, "Setting serial port parameters {0}, {1}, {2},{3}\n", 
                    new Object[]{BaudRate, NrDataBits, NrStopBits, Parity});
            
            // set port parameters
            m_Port.setSerialPortParams(BaudRate, NrDataBits, NrStopBits, ParityInt);
            
            // calc time in ms to transmit one byte
            m_TimeToTransmit = (NrDataBits + NrStopBits + (Parity.equalsIgnoreCase("none") ? 0f : 1f) )
                    / BaudRate * 1000;
            
            // log
            m_Logger.log(Level.CONFIG, "Time to Transmit one byte via RS232 is {0} ms", 
                    m_TimeToTransmit);
            
        } catch (UnsupportedCommOperationException ex) {
            String str = "The specified configuration parameters are not supported by\n"
                    + "the RS232 port " + m_PortName + ":\n"
                    + ex.getMessage() + "\n";
            
            throw new IOException(str);
        }
    }//</editor-fold>
    
    
    /**
     * Convenience method to convert the content of a ByteBuffer to a String 
     * with each Byte's hexadecimal value.
     * 
     * @param Message The ByteBuffer to convert. The position of the "iterator" 
     * is changed.
     * @return The String with the hexadecimal values of <code>msg</code> with
     * preceding "0x "
     */
    protected static String ByteBufferToString(ByteBuffer Message) {
        
        // start from the beginning of the ByteBuffer
        Message.rewind();
        
        // the returned message string
        String MessageString = "0x";
        
        // append data bytes as hex values
        while ( Message.hasRemaining() ) {
            MessageString += String.format(" %02X", Message.get());
        }
        
        // return
        return MessageString;
    }
}
