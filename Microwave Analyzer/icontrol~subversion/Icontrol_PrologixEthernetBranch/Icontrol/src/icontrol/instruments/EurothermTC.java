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
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;

/**
 * Eurotherm Temperature Controller. This class was tailored for a Eurotherm 3200
 * controller but might also serve as base class for other models.<p>
 * 
 * To set parameters in the Eurotherm controller, use <code>WriteA32bitValue</code>, 
 * which is implemented in this class.<p>
 * 
 * All device commands that the EurothermTC understands are
 * implemented here.<p>
 *
 * <h3>Instrument Commands:</h3>
 * <ul>
 *  <li>{@link #getProcessValue() }
 *  <li>{@link #setModbusAddress(int) }
 *  <li>{@link #setRampRate(float, int) }
 *  <li>{@link #setSP1(float) }
 *  <li>{@link #ReadA32bitValue(short, Class) }
 *  <li>{@link #WriteA32bitValue(short, Number) }
 * </ul>
 * 
 * The following is an excerpt of a log of iTools when changing SetPoint1:
<pre>
log from iTools OPC Server, Network Monitor ON, COM1 Statistics
enable Monitor, log to file, <user>\Local Settings\Temp\EuroMBus.log

           Adr = 1 (okay)
           | Fct code 10 = Write N words
           | |  Adr of first word (80 30 = 0x8000 + 2*24; adr 24=Setpoint1)
           | |  |     Write 2 words
           | |  |     |     Nr data bytes
           | |  |     |     |  Data (I set SP1 to 0)
           | |  |     |     |              CRC
29:TX-OK: 01 10 80 30 00 02 04 00 00 00 00 91 7D
29:RX-OK: 01 10 80 30 00 02 68 07

             Fct code 3 = Read N bytes
             |  Adr of first word
             |  |     Nr of words to read
             |  |     |     CRC
2A:TX-OK: 01 03 80 30 00 02 ED C4
2A:RX-OK: 01 03 04 00 00 00 00 FA 33
         
                read Instrument Mode (register 199)
2B:TX-OK: 01 03 81 8E 00 02 8C 1C
2B:RX-OK: 01 03 04 00 01 80 00 CA 33
         
                read register 16128 (it's not listed in the manual)
2C:TX-OK: 01 03 3F 00 00 0F 09 DA
2C:RX-OK: 01 03 1E 00 20 20 32 B5 33 80 12 00 20 80 12 00 00 00 00 FF 03 00 09 02 FF 00 01 01 11 00 00 00 A0 1A D7

                               change SP1 to 12.34 as IEEE float
3E:TX-OK: 01 10 80 30 00 02 04 41 45 70 A4 B0 EF
3E:RX-OK: 01 10 80 30 00 02 68 07
</pre>
 *
 * @author KPP (Kurt Pernstich: pernstich@alumni.ethz.ch)
 * @version 0.1
 */

// promise that this class supports GPIB communication
@iC_Annotation(CommPorts=CommPorts.RS232,
               InstrumentClassName="Eurotherm TC")
public class EurothermTC extends Device {


    ///////////////////
    // member variables
    
    /* The MODBUS address of the Eurotherm controller */
    private byte m_ModbusAddress;
    
    /* Wait time that signals end-of-transmission */
    private int m_WaitEOT = 4;
    

    /**
     * The Logger for this class. Note that the Logger Level is set in the
     * constructor of the <code>Device</code> class.
     */
    // change the Logger name to the name of your class
    private static final Logger m_Logger = Logger.getLogger("iC.Instruments.EurothermTC");
    
    /** Register addresses for certain functions */
    final short   SP1 = 24;         // SP1
    final short   RAMP_UNITS = 531; // RAMPU
    final short   SP_RATE = 35;     // SP.RAT (Setpoint rate limit)
    final short   PV_IN = 1;        // PV.IN (process value)
    //final short   TIMER_TYPE = 320;
    
    /**
     * Constructor, Assigns the default Modbus Address.
     */
    // <editor-fold defaultstate="collapsed" desc="Constructor">
    public EurothermTC() {
        // init Modbus address
        m_ModbusAddress = (byte)m_iC_Properties.getInt("EurothermTC.ModbusAddress", 1);

    }//</editor-fold>
    
    /**
     * Calculates and stores the time signaling the end-of-transmission (3.5 bytes).<p>
     * 
     * Overridden <code>Device.Open</code> for additional initializations after 
     * establishing the connection to the Instrument. This method is not called
     * when in Syntax-Check mode or when in No-Communication mode! For these cases,
     * the wait time is set in the definition of the local variables.<p>
     * 
     * This method also calls <code>checkLoopback</code> depending on the switch
     * EurothermTC.CheckLoopBack in iC.properties.<p>
     */
    // <editor-fold defaultstate="collapsed" desc="Open">
    @Override
    public void Open() 
           throws IOException {
        
        // get baud rate from RS232 driver
        int BaudRate = m_RS232_Driver.getBaudRate();
        
        // store wait time
        if (BaudRate != 0) {
            m_WaitEOT = (int)Math.ceil(1 / BaudRate * 35);  // 3.5 byte * 1+8+1 bits/byte)
            
            // log value
            m_Logger.log(Level.CONFIG, "EOT time= {0}\n", m_WaitEOT);
        } else {
            m_WaitEOT = 4;  // it's the value for 9600 bauds
        }
        
        // get option to check for LoopBack
        if ( m_iC_Properties.getInt("EurothermTC.CheckLoopBack", 1) == 1) {
        
            // check Loopback
            checkLoopback();
            
        } else {
            // log that no check was performed
            m_Logger.config("Did not perform a LoopBack check.\n");
        }
    }//</editor-fold>
    

    
    /**
     * Changes the Modbus address for the Eurotherm controller. The default Modbus
     * address can be set in the iC.properties.
     * 
     * @param ModbusAdr The new Modbus address; can be 1-254 and the value is 
     * cast to a <code>byte</code>.
     * 
     * @throws DataFormatException when the Syntax Check failed.
     */
    // <editor-fold defaultstate="collapsed" desc="set Modbus Address">
    @AutoGUIAnnotation(
        DescriptionForUser = "Sets the MODBUS address of the controller.",
        ParameterNames = {"Modbus address { [1, 254] }"},
        DefaultValues = {"1"},
        ToolTips = {""})
    public void setModbusAddress(int ModbusAdr) 
           throws DataFormatException {
        
        // range check
        if (ModbusAdr < 1 || ModbusAdr > 255) {
            String str = "The Modbus address needs to be between 1 and 255.\n";

            throw new DataFormatException(str);
        }
        
        // store the new adr
        m_ModbusAddress = (byte)ModbusAdr;
    }//</editor-fold>

    /**
     * Changes the SetPoint SP1 (Register address 24).<p>
     * 
     * Note from the manual: Do not write continuously changing values to this 
     * variable. The memory technology used in this product has a limited (100,000) 
     * number of write cycles. If ramped setpoints are required, consider using the
     * internal ramp rate function or the remote comms setpoint (Modbus address 26)
     * in preference.
     * 
     * @param SetPoint The new temperature setpoint. The unit is in display units.
     * 
     * @throws IOException if the transmission via RS232 caused an error; bubbles
     * up from <code>WriteA32bitValue</code>.
     */
    // <editor-fold defaultstate="collapsed" desc="set SP1">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Sets the SetPoint 1 temperature.</html>",
        ParameterNames = {"T [displayed units]"},
        DefaultValues = {"20"},
        ToolTips = {""})
    @iC_Annotation(  MethodChecksSyntax = false )
    public void setSP1(float SetPoint) 
           throws IOException {
              
        // send new value
        WriteA32bitValue(SP1, SetPoint);
        
    }//</editor-fold>
    
    
    
    /**
     * Changes the Ramp Rate (SP.RAT, setpoint rate limit, address 35) and also
     * the unit of the rampe rate (RAMPU, ramp units, address 531).<p>
     * 
     * @param RampRate The new Ramp Rate in units of the TimeBase.
     * @param TimeBase The TimeBase: 0 for ramping in Seconds, 1 for Hours and
     * 2 for ramping in Seconds
     * 
     * @throws IOException if the transmission via RS232 caused an error; bubbles
     * up from <code>WriteA32bitValue</code>.
     */
    // <editor-fold defaultstate="collapsed" desc="set Ramp Rate">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Sets the RampRate 1.</html>",
        ParameterNames = {"Ramp Rate [TimeBase]", "Time Base {0, 1, 2}"},
        DefaultValues = {"6", "0"},
        ToolTips = {"", "<html>Time Base for the Ramp Rate. Can be<br>0 ... Ramp per Minute<br>1 ... Ramp per Hour<br>2 ... Ramp per Second</html>"})
    @iC_Annotation(  MethodChecksSyntax = false )
    public void setRampRate(float RampRate, int TimeBase) 
           throws IOException {
        
        ////////////////
        // set Time Base
        
        // send value
        WriteA32bitValue(RAMP_UNITS, TimeBase);
        
        
        ////////////////
        // set Ramp Rate
        
        // send value
        WriteA32bitValue(SP_RATE, RampRate);
    }//</editor-fold>
    
    
    /**
     * Reads register <code>PV_IN</code> (1) which contains the current process
     * value (aka Temperature).
     *
     * @return Returns the current Process Value (Temperature) as a float.
     * @throws IOException When a communication error occurred (bubbles up from
     * <code>ReadA32bitValue</code>
     */
    // <editor-fold defaultstate="collapsed" desc="get Process Value">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Gets the current Prosses Value (Temperature).</html>",
        ParameterNames = {},
        DefaultValues = {},
        ToolTips = {})
    @iC_Annotation(  MethodChecksSyntax = false )
    public float getProcessValue() 
           throws IOException {
              
        // send new value
        float ret = (Float)ReadA32bitValue(PV_IN, Float.TYPE);
        
        // return the current process value (temperature)
        return ret;
        
    }//</editor-fold>
    
    // TODO 2* would quickStatus be helpful?
    // <editor-fold defaultstate="collapsed" desc="quick Status">
    private void quickStatus() 
            throws IOException {
        
        String Status = "";
        
        // "send" Frame start (3.5 bytes)
        try {Thread.sleep(m_WaitEOT);} catch (InterruptedException ex) {/*ignore*/}
        
        byte[] msg = new byte[4];
        
        msg[0] = m_ModbusAddress;
        msg[1] = 7; // Function Code 7: quick status
        
        // calc and append CRC
        // a wrapped ByteBuffer is transparent for changes
        calcCRC(ByteBuffer.wrap(msg));
        
        // send the message and receive the answer
        SendViaRS232(ByteBuffer.wrap(msg));
        
        // "send" EOT (End-of-transmission) (3.5 bytes)
        try {Thread.sleep(m_WaitEOT);} catch (InterruptedException ignore) {}
        
        
        // check response (without sending something)
        ByteBuffer ans = QueryViaRS232(ByteBuffer.allocate(0));
        
        // check length
        if (ans.limit() != 5) {
            String str = "Received answer is too short. Status is not okay.\n";
            throw new IOException(str);
        }
        
        // check CRC
        if (calcCRC(ans) == false) {
            String str = "The CRC code of the received answer is incorrect. Status is not okay.\n";
            throw new IOException(str);
        }
        
        // check Instrument number
        if (ans.get() != m_ModbusAddress) {
            String str = "Received the wrong device address. Status is not okay.\n";
            throw new IOException(str);
        }

        // do more (page 48)
        
    }//</editor-fold>
    
    
    /**
     * Checks for the presence of a Eurotherm controller by sending the loopback
     * command to a receiver with the specified Modbus address. <p>
     * 
     * @throws IOException When a communication error occurs (bubbles up from 
     * <code>SendViaRS232</code> or <code>QueryViaRS232</code>), or an invalid
     * answer was received via RS232.
     */
    // TODO 1* Could also check the instrument type code (register 122)
    // <editor-fold defaultstate="collapsed" desc="Loop Back">
    private void checkLoopback() 
            throws IOException {
        
        // local variables
        ByteBuffer ans;
        ByteBuffer msg = ByteBuffer.allocate(8);

        // build byte stream to send
        msg.put(m_ModbusAddress);
        msg.put((byte)8);           // Function Code 8: diagnostic loopback        
        msg.putShort((short)0);     // diagnostic code must be 0
        msg.putShort((short)2311);  // loopback data
        
        // calc and append CRC
        calcCRC(msg);
        
        // log the loopback data
        String str = "Sending loopback data: " + RS232_Driver.ByteBufferToString(msg);
        m_Logger.config(str);
        
        
        try {
            // "send" Frame start (3.5 bytes)
            try {Thread.sleep(m_WaitEOT);} catch (InterruptedException ex) {/*ignore*/}

            // send the message
            SendViaRS232(msg);

            // "send" EOT (End-of-transmission) (3.5 bytes)
            try {Thread.sleep(m_WaitEOT);} catch (InterruptedException ex) {/*ignore*/}



            // get response (without sending something)
            ans = QueryViaRS232(ByteBuffer.allocate(0));
            
            // log the answer
            str = "Received loopback data: " + RS232_Driver.ByteBufferToString(ans);
            m_Logger.config(str);
            
        } catch (IOException ex) {
            str = "A communication error occurred during Loopback testing of the Eurotherm TC.\n"
                + "Please check the log file for more details.\n"
                + ex.getMessage();
            throw new IOException(str);
        }
              
        // check validity
        msg.rewind();
        ans.rewind();
        if ( !msg.equals(ans) ) {
            // throw an IOException
            str = "Could not verify the presence of a Eurotherm controller using Loopback.\n"
                + "Please ensure that the Eurotherm controller is connected to the RS232 port,\n"
                + "that the proper communication parameters are selected,\n"
                + "and that the Modbus address is correct.\n";
            throw new IOException(str);
        } else {
            // log success
            m_Logger.config("Loopback was successful\n");
        }
        
    }//</editor-fold>
    
    
    /**
     * Writes a Float or an Integer value as a 32bit value to the specified register 
     * address. It accesses the full resolution data (registers above 0x8000). The
     * response of the Eurotherm is read and checked for validity.<p>
     * 
     * This method is public, so it can be called from the script, but it does not
     * define a <code>AutoGUIAnnotation</code> entry, hence, it is not visible in
     * the GUI.
     * 
     * @param RegisterAddress is the address of the register in the Eurotherm controller
     * that should receive <code>Value</code>. The register address is multiplied by two
     * and 0x8000 is added in order to write full resolution 32 bits to the register
     * (see chapter 7 of Modbus Communications Handbook). Therefore, the original register
     * address as it is listed in the 3200's manual is to be specified here.
     * 
     * @param Value will be written to the register address. The data type of
     * <code>Value</code> must be <code>Float</code> or <code>Integer</code>.
     * 
     * @throws IOException if 1) the transmission via RS232 caused an error (bubbles
     * up from <code>SendViaRS232</code> or <code>QueryViaRS232</code>), or 2) an
     * invalid reply was received from the Eurotherm controller, or 3) the data
     * type of <code>Value</code> is neither <code>Float</code> nor 
     * <code>Integer</code> (this should actually only occur during development.
     */
    // <editor-fold defaultstate="collapsed" desc="Write A 32bit Value">
    public void WriteA32bitValue(short RegisterAddress, Number Value) 
            throws IOException {
              
        // make a new Byte Buffer
        ByteBuffer msg = ByteBuffer.allocate(13);
        
        
        // build the byte stream to send
        msg.put(m_ModbusAddress);   // Eurotherm's ID (address)
        msg.put((byte)0x10);        // Function 10h: WriteNWords
        
        // the register address for the 32bit value
        msg.putShort( (short)(0x8000 + 2*RegisterAddress) );
        
        msg.putShort((short)2); // Number of words to write
        msg.put((byte)4);       // Number of data bytes to write
        
        if (Value instanceof Float) {
            msg.putFloat((Float)Value);    // the new value
        } else if (Value instanceof Integer) {
            msg.putInt((Integer)Value);    // the new value
        } else {
            String str = "A wrong data type was passed to WriteA32bitValue (must be Float or Integer).\n";
            throw new IOException(str);
        }
        
        // add CRC
        calcCRC(msg);
        
        // iTools:
        // SP1 = 0    : 01 10 80 30 00 02 04 00 00 00 00 91 7D
        // SP1 = 12.34: 01 10 80 30 00 02 04 41 45 70 A4 B0 EF       
        
        // send the message
        SendViaRS232(msg);
        
        // "send" EOT (End-of-transmission) (3.5 bytes)
        try {Thread.sleep(m_WaitEOT);} catch (InterruptedException ex) {/*ignore*/}
        
        
        // receive response (without sending something)
        ByteBuffer ans = QueryViaRS232(ByteBuffer.allocate(0));
        
        
        
        ///////////////////////
        // verify communication
        // only the first 6 bytes + CRC are sent by the Eurotherm controller
        
        // check CRC
        boolean CRC = calcCRC(ans);
        
        // check length
        boolean length = ans.limit() == 8 ? true : false;
        
        // "trim" the ByteBuffers of the original message
        msg.limit(6);
        if (length) {
            ans.limit(6);
        }
        
        // compare first 6 bytes
        msg.rewind();
        ans.rewind();
        
        // check validity
        if ( !CRC || !length || !ans.equals(msg) ) {
            String str = "The received answer when writing to register " + RegisterAddress + "\n"
                    + "of the Eurotherm controller '" + m_InstrumentName + "' was invalid.\n"
                    + "sent    : " + RS232_Driver.ByteBufferToString(msg) + "\n"
                    + "received: " + RS232_Driver.ByteBufferToString(ans) + "\n"
                    + "CRC was " + (CRC ? "valid" : "invalid");
            
            throw new IOException(str);
        }     
    }//</editor-fold>
    
    
    /**
     * Reads a Float or an Integer value as a 32bit value from the specified register 
     * address. It accesses the full resolution data (registers above 0x8000). The
     * response of the Eurotherm is read and checked for validity.<p>
     * 
     * This method is public, so it can be called from the script, but it does not
     * define a <code>AutoGUIAnnotation</code> entry, hence, it is not visible in
     * the GUI.
     * 
     * @param RegisterAddress is the address of the register in the Eurotherm controller
     * from which the value should be retrieved. The register address is multiplied by two
     * and 0x8000 is added in order to read full resolution 32 bits from the register
     * (see chapter 7 of Modbus Communications Handbook). Therefore, the original register
     * address as it is listed in the 3200's manual is to be specified here.
     * 
     * @param ReturnType Specifies the data type of the returned value. Valid data
     * types are <code>Float</code> or <code>Integer</code>. Use for instance
     * <code>Float.TYPE</code> to specify <code>ReturnType</code>.
     * 
     * @throws IOException if 1) the transmission via RS232 caused an error (bubbles
     * up from <code>SendViaRS232</code> or <code>QueryViaRS232</code>), or 2) an
     * invalid reply was received from the Eurotherm controller, or 3) the data
     * type of <code>ReturnType</code> is neither <code>Float</code> nor 
     * <code>Integer</code> (this should actually only occur during development.
     */
    // <editor-fold defaultstate="collapsed" desc="Read A 32bit Value">
    public Number ReadA32bitValue(short RegisterAddress, Class<?> ReturnType) 
            throws IOException {
        
        // local variables
        Number ret = null;
              
        // make a new Byte Buffer
        ByteBuffer msg = ByteBuffer.allocate(8);
        
        ////////////////////////////////
        // build the byte stream to send
        msg.put(m_ModbusAddress);   // Eurotherm's ID (address)
        msg.put((byte)0x03);        // Function 03h: ReadNWords
        
        // the register address for the 32bit value
        msg.putShort( (short)(0x8000 + 2*RegisterAddress) );
        
        msg.putShort((short)2); // Number of words to read
        
        // add CRC
        calcCRC(msg);
        
        ///////////////////
        // send the message
        SendViaRS232(msg);
        
        // "send" EOT (End-of-transmission) (3.5 bytes)
        try {Thread.sleep(m_WaitEOT);} catch (InterruptedException ex) {/*ignore*/}
        

        ///////////////////
        // receive response
        ByteBuffer ans = QueryViaRS232(ByteBuffer.allocate(0));
         
        
        //////////////////
        // verify response
        
        // check CRC
        boolean CRC = calcCRC(ans);
        
        // check length
        boolean length = ans.limit() == 9 ? true : false;
        
        // check CRC
        if ( !CRC || !length || ans.get(0) != m_ModbusAddress 
                  || ans.get(1) != (byte)3 // function code
                  || ans.get(2) != 4) {    // number of bytes read 
            String str = "The received answer when reading from register " + RegisterAddress + "\n"
                    + "of the Eurotherm controller '" + m_InstrumentName + "' was invalid.\n"
                    + "sent    : " + RS232_Driver.ByteBufferToString(msg) + "\n"
                    + "received: " + RS232_Driver.ByteBufferToString(ans) + "\n"
                    + "CRC was " + (CRC ? "valid" : "invalid");
            
            throw new IOException(str);
        }
        
        // get the register content with the appropriate data type
        if (ReturnType == Float.TYPE) {
            // return a Float
            ret = ans.getFloat(3);    
            
        } else if (ReturnType == Integer.TYPE) {
            // return an Intteger
            ret = ans.getInt(3);    
        } else {
            String str = "A wrong data type was passed to ReadA32bitValue (must be Float or Integer).\n";
            throw new IOException(str);
        }
        
        // return the answer
        return ret;
        
    }//</editor-fold>
   
    /**
     * Calculates the 16-bit CRC code as required by the Eurotherm controller. For a 
     * definition of the CRC code see Eurotherm's Modbus Digital Communications 
     * Handbook (chapter 3-8, pdf page 32).<p>
     * 
     * Note that the CRC is calculated over all but the last two bytes in 
     * <code>msg</code>. The calculated CRC is compared with the last two bytes
     * in <code>msg</code> and if they agree, this method returns <code>true</code>,
     * otherwise it returns <code>false</code>. The calculated CRC value is stored
     * in the last two bytes of <code>msg</code>.
     * 
     * @param msg The (8-bit) <code>ByteBuffer</code> of which the CRC should be 
     * calculated. See also the note above.
     * 
     * @return <code>True</code> if the last two bytes of <code>msg</code> are
     * equal to the calculated CRC value, and <code>false</code> otherwise.
     * The calculated CRC value is stored in the last two bytes of <code>msg</code>.
     */
    // <editor-fold defaultstate="collapsed" desc="calc CRC">
    private boolean calcCRC(ByteBuffer msg) {
        
        // init crc (exclusive or works on int, not on short)
        int crc = (int)0xFFFF;
                
        // check length of msg
        if (msg.capacity() <= 2) {
            // log event
            m_Logger.severe("calcCRC was called with msg being too short! CRC is invalid.\n");
            
            // return an invalid CRC
            return false;
        }
        
        // rewind the buffer
        msg.rewind();
            
        
        // calc CRC for all but the last two bytes of the message (which will
        // contain the CRC
        while (msg.remaining() > 2) {
            
            // the code to calculate the CRC was adapted from
            // Eurotherm's Modbus Digital Communications Handbook (p. 3-11)
            
            // get next data byte, ensure it's 0x 00 00 00 xy
            int next = 0x000000FF & ((int)msg.get());
            
            /* Here was a nasty bug: 
             * short's hold values between -127 and 128. When the byte value
             * was larger than 128, say 0x80, the int was 0xFFFFFF80 instead of 
             * 0x80, which corrupted the CRC calucluation. Becuase the examples
             * that I got from the Comms Handbook did not contain values larger
             * than 128, the bug was long undetected.
             * => make sure the conversion to int is always correct !! */
            
            crc = crc ^ next;  // exclusive bitwise or returns an int, not a short
            
            for (int n = 0; n < 8; n++) {
                
                // check for carry bit
                int carry = crc & 1; 
                
                // shift right one bit
                crc = crc >>> 1; 
                
                // Exclusive or with 0xA001 if necessary
                if (carry == 1) {
                    crc = crc ^ 0xA001;
                }
            }
        }
        
        // mark current position in the buffer
        msg.mark();
        
        // use nice variable names
        byte msb = (byte)(crc >>> 8);
        byte lsb = (byte)(crc & 0x00FF);
       
        // check if crc is correct, lsb is sent first
        boolean ret;
        if ( msg.get() == lsb &&
             msg.get() == msb ) {
            
            // CRC was correct
            ret = true;
        } else {
            // CRC was incorrect
            ret = false;
            
            // reset buffer position to previous mark
            msg.reset();

            // store calculated CRC value
            msg.put(lsb);
            msg.put(msb);
        }
        
        // return correctness of the CRC
        return ret;
    }//</editor-fold>
    
}
