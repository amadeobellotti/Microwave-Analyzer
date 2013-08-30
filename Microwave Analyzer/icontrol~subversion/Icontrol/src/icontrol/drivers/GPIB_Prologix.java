// TODO 1* dont always send ++addr (speed wise this doesn't help, so skip it)
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

package icontrol.drivers;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Platform;
import com.sun.jna.ptr.IntByReference;
import icontrol.IcontrolView;
import icontrol.iC_Properties;
import static icontrol.Utilities.getInteger;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.ScriptException;

/**
 * This class provides access to the native code to access the Prologix's
 * GPIB-USB.<p>
 *
 * It uses Java Native Access (JNA) package to access the driver (for instance
 * ftd2xx.dylib on Mac operating systems).<p>
 *
 * Before being able to send and receive the method <code>Open</code> must be
 * called, or else the behavior is undefined.<p>
 *
 * <h3>Usage / How this class works:</h3>
 * <ul>
 *  <li>Instantiate the class. The constructor calls <code>LoadNative</code>.
 *  <li><code>LoadNative</code> establish access to the native code (for instance 
 *      ftdi2xx.dylib on Mac OS), and then calls <code>OpenFTDI</code>. If the
 *      native library has been loaded before, this method does nothing but
 *      return.
 *  <li><code>OpenFTDI</code> establishes the connection to the FTDI chip inside
 *      the Prologix controller before calling <code>InitPrologix</code>.
 *  <li><code>InitPrologix</code> initializes the Prologix GPIB-USB controller;
 *      that is it sets parameters like the End-Of-Transmission mode or the
 *      GPIB Time-Out.
 *  <li>Open an Instrument at a specific GPIB address using <code>Open</code>,
 *      which stores the GPIB address and sends a Selected-Device-Clear to the
 *      Instrument at the specified GPIB address.
 *  <li>To send/receive data use <code>SendCommand</code> and <code>Receive</code>.
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
public class GPIB_Prologix extends GPIB_Driver {

    ///////////////////
    // member variables

    /** The Logger for this class */
    private static final Logger m_Logger = Logger.getLogger("iC.Instruments.GPIB_Prologix");

    /** Grants access to global properties in a resource bundle file 'iC.properties' */
    protected static iC_Properties m_iC_Properties;

    /** GPIB Address of the Instrument */
    private int m_GPIB_Adr;

    /** Receive Buffer */
    private static ByteBuffer m_ReceiveBuffer = null;

    /** Handle to access the native code */
    private static ftd2xx       m_ftd2xx = null;

    /** The ftHandle to access the Prologix GPIB-USB controller. Assigned in
     * <code>OpenFTDI</code> and used in communicating with the controller. */
    private static int m_ftPrologix;

    /** The Status of the last invocation of a native function (FT_...) */
    private int m_Status;

    /** Storage for the number of bytes used in many different methods.
     * Defined central to avoid multiple creations/destructions */
    private IntByReference m_NrBytes = new IntByReference();



    /////////////////////////////////
    // ftd2xx.dll specific CONSTANTS
    // <editor-fold defaultstate="collapsed" desc="ftd2xx.dll CONSTANTS">


    /** Define meaning of Device Status. See FTDI Programmers Guide. */
    private final String[] FT_STATUS_DESCRIPTION = {
        "FT_OK",
        "FT_INVALID_HANDLE",
        "FT_DEVICE_NOT_FOUND",
        "FT_DEVICE_NOT_OPENED",
        "FT_IO_ERROR",
        "FT_INSUFFICIENT_RESOURCES",
        "FT_INVALID_PARAMETER",
        "FT_INVALID_BAUD_RATE",	//7
        "FT_DEVICE_NOT_OPENED_FOR_ERASE",
        "FT_DEVICE_NOT_OPENED_FOR_WRITE",
        "FT_FAILED_TO_WRITE_DEVICE",
        "FT_EEPROM_READ_FAILED",
        "FT_EEPROM_WRITE_FAILED",
        "FT_EEPROM_ERASE_FAILED",
        "FT_EEPROM_NOT_PRESENT",
        "FT_EEPROM_NOT_PROGRAMMED",
        "FT_INVALID_ARGS",
        "FT_NOT_SUPPORTED",
        "FT_OTHER_ERROR"};

    // Device Status OK
    private final int FT_OK = 0;

    // FT_OpenEx Flags
    private final int FT_OPEN_BY_SERIAL_NUMBER =    1;
    private final int FT_OPEN_BY_DESCRIPTION =      2;
    private final int FT_OPEN_BY_LOCATION =         4;

    // FT_ListDevices Flags (used in conjunction with FT_OpenEx Flags
    private final int FT_LIST_NUMBER_ONLY = 0x80000000;
    private final int FT_LIST_BY_INDEX =    0x40000000;
    private final int FT_LIST_ALL =         0x20000000;

    private final int FT_LIST_MASK = (FT_LIST_NUMBER_ONLY|FT_LIST_BY_INDEX|FT_LIST_ALL);


    // Baud Rates
    private final int FT_BAUD_300 =     300;
    private final int FT_BAUD_600 =     600;
    private final int FT_BAUD_1200 =    1200;
    private final int FT_BAUD_2400 =    2400;
    private final int FT_BAUD_4800 =    4800;
    private final int FT_BAUD_9600 =    9600;
    private final int FT_BAUD_14400 =   14400;
    private final int FT_BAUD_19200 =   19200;
    private final int FT_BAUD_38400 =   38400;
    private final int FT_BAUD_57600 =   57600;
    private final int FT_BAUD_115200 =  115200;
    private final int FT_BAUD_230400 =  230400;
    private final int FT_BAUD_460800 =  460800;
    private final int FT_BAUD_921600 =  921600;


    // Word Lengths
    private final byte FT_BITS_8 =  8;
    private final byte FT_BITS_7 =  7;


    // Stop Bits
    private final byte FT_STOP_BITS_1 = 0;
    private final byte FT_STOP_BITS_2 = 2;

    // Parity
    private final byte FT_PARITY_NONE =     0;
    private final byte FT_PARITY_ODD =      1;
    private final byte FT_PARITY_EVEN =     2;
    private final byte FT_PARITY_MARK =     3;
    private final byte FT_PARITY_SPACE =    4;

    // Flow Control
    private final int FT_FLOW_NONE =        0x0000;
    private final int FT_FLOW_RTS_CTS =     0x0100;
    private final int FT_FLOW_DTR_DSR =     0x0200;
    private final int FT_FLOW_XON_XOFF =    0x0400;

    // Purge rx and tx buffers
    private final int FT_PURGE_RX = 1;
    private final int FT_PURGE_TX = 2;

    // Events
    private final int FT_EVENT_RXCHAR =         1;
    private final int FT_EVENT_MODEM_STATUS =   2;
    private final int FT_EVENT_LINE_STATUS =    4;

    // Timeouts
    private final int FT_DEFAULT_RX_TIMEOUT = 300;
    private final int FT_DEFAULT_TX_TIMEOUT = 300;

    // Device types
//    enum DeviceTypes {
//        FT_DEVICE_BM,
//        FT_DEVICE_AM,
//        FT_DEVICE_100AX,
//        FT_DEVICE_UNKNOWN,
//        FT_DEVICE_2232C,
//        FT_DEVICE_232R,
//        FT_DEVICE_2232H,
//        FT_DEVICE_4232H
//     };

    // Bit Modes
    private final int FT_BITMODE_RESET =            0x00;
    private final int FT_BITMODE_ASYNC_BITBANG =    0x01;
    private final int FT_BITMODE_MPSSE =            0x02;
    private final int FT_BITMODE_SYNC_BITBANG =     0x04;
    private final int FT_BITMODE_MCU_HOST =         0x08;
    private final int FT_BITMODE_FAST_SERIAL =      0x10;
    private final int FT_BITMODE_CBUS_BITBANG =     0x20;
    private final int FT_BITMODE_SYNC_FIFO =        0x40;
            
    // Events
    private final int EV_RXCHAR =   0x0001;  // Any Character received
    private final int EV_RXFLAG =   0x0002;  // Received certain character
    private final int EV_TXEMPTY =  0x0004;  // Transmitt Queue Empty
    private final int EV_CTS =      0x0008;  // CTS changed state
    private final int EV_DSR =      0x0010;  // DSR changed state
    private final int EV_RLSD =     0x0020;  // RLSD changed state
    private final int EV_BREAK =    0x0040;  // BREAK received
    private final int EV_ERR =      0x0080;  // Line status error occurred
    private final int EV_RING =     0x0100;  // Ring signal detected
    private final int EV_PERR =     0x0200;  // Printer error occurred
    private final int EV_RX80FULL = 0x0400;  // Receive buffer is 80 percent full
    private final int EV_EVENT1 =   0x0800;  // Provider specific event 1
    private final int EV_EVENT2 =   0x1000;  // Provider specific event 2

    // Escape Functions
    private final int SETXOFF =     1;       // Simulate XOFF received
    private final int SETXON =      2;       // Simulate XON received
    private final int SETRTS =      3;       // Set RTS high
    private final int CLRRTS =      4;       // Set RTS low
    private final int SETDTR =      5;       // Set DTR high
    private final int CLRDTR =      6;       // Set DTR low
    private final int RESETDEV =    7;       // Reset device if possible
    private final int SETBREAK =    8;       // Set the device break line.
    private final int CLRBREAK =    9;       // Clear the device break line.

    // PURGE function flags.
    private final int PURGE_TXABORT = 0x0001;  // Kill the pending/current writes to the comm port.
    private final int PURGE_RXABORT = 0x0002;  // Kill the pending/current reads to the comm port.
    private final int PURGE_TXCLEAR = 0x0004;  // Kill the transmit queue if there.
    private final int PURGE_RXCLEAR = 0x0008;  // Kill the typeahead buffer if there.

    //</editor-fold>



    ///////////////////////////
    // class specific constants

    /** size of the Receive buffer */
    private final int RECEIVE_BUFFER_SIZE =
            (new iC_Properties()).getInt("GPIB_Prologix.ReceiveBufferSize", 150000);

    /** USB Time-Out value
     * The <code>FT_Read</code> method reads a certain number of bytes or until
     * a USB TimeOut occurs. Because <code>ReadFromPrologix</code> does not know
     * in advance how many bytes to read, it is necessary so set a proper USB
     * TimeOut value. Larger numbers slow down communication, and smaller numbers
     * might lead to communication errors.<p>
     * See the additional comments at <code>SpeedTest</code> in the
     * <code>GPIB_Driver</code> class.
     */
    private final int TIMEOUT_USB = 
            (new iC_Properties()).getInt("GPIB_Prologix.TimeOutUSB", 50);

    /** GPIB Time-Out value */
    private final int TIMEOUT_GPIB = 
            (new iC_Properties()).getInt("GPIB_Prologix.TimeOutGPIB", 3000);

    // Used during development
    // with ++read eoi no wait time appears to be necessary anymore
    // TODO delete mid 2012)
    private final int DEBUG_PROLOGIX_RW_DELAY = 0;


    /**
     * Defines the interface required by JNA to invoke the native library.<p>
     * This interface defines all function calls 'into' the native
     * code (.dll / .dylib). It is the standard, stable way of mapping, which
     * supports extensive customization and mapping of Java to native types.<p>
     *
     * Please note that not all function declaration have been imported from
     * ftd2xx.h while most constants should have been included.
     */
    // <editor-fold defaultstate="collapsed" desc="ftd2xx Interface Declarations">
    private interface ftd2xx extends Library {

        /* Employed data type conversions:
         * Note: Unsigned values may be passed by assigning the corresponding
         * two's-complement representation to the signed type of the same size.
         *
         * type         native          JNA
         * ----------------------------------
         * FT_HANDLE    DWORD           int ___see Appendix A, page 92
         * FT_HANDLE*   DWORD*          IntByReference()
         * FT_STATUS    DWORD           int
         *              LPDWORD         IntByReference()
         * PVOID        LPVOID          Pointer() ___ should be generally valid
         *              --"--           String ___depending on the function
         *              --"--           ByteBuffer() ___depending on the function
         * PCHAR        char*           byte[]
         */


        ////////////////////////////////////////////////////
        // Function declaration (see D2XX_Programmers_Guide)


        /** This function returns D2XX DLL version number, page 32 */
        int FT_GetLibraryVersion(IntByReference DLLVersion);
        //LPDWORD lpdwDLLVersion

        /**
         * This function builds a FTDI device information list and returns the number
         * of D2XX devices connected to the system. The list contains information
         * about both unopened and opened devices. See page 7
         */
        int FT_CreateDeviceInfoList(IntByReference NumberOfDevices);
        //LPDWORD lpdwNumDevs

        /** 
         * This function returns an entry from the FTDI device information list. Ensure
         * that <code>FT_CreateDeviceInfoList</code> has been called before invoking
         * this method.
         */
        int FT_GetDeviceInfoDetail(int Index, IntByReference Flags,
                IntByReference Type, IntByReference ID, IntByReference LocationID,
                byte[] SerialNr, byte[] Description, IntByReference ftHandle);
        //FT_STATUS FT_GetDeviceInfoDetail (DWORD dwIndex, LPDWORD lpdwFlags, LPDWORD lpdwType,
        //LPDWORD lpdwID, LPDWORD lpdwLocId, PCHAR pcSerialNumber, PCHAR pcDescription, FT_HANDLE *ftHandle)

        /**
         * Open the specified FTDI device and return a handle that will be used for
         * subsequent accesses. Note that with Arg1 defined as String, FT_OPEN_BY_LOCATION
         * can not be used.
         */
        int FT_OpenEx(String Arg1, int Flags, IntByReference ftHandle);
        //FT_STATUS FT_OpenEx (PVOID pvArg1, DWORD dwFlags, FT_HANDLE *ftHandle)

        /** Close the connection to the FTDI device (that is the Prologix GPIB-USB controller */
        int FT_Close(int ftHandle);
        //FT_STATUS FT_Close (FT_HANDLE ftHandle)
        
        /** Write data to the FTDI device. Page 19 */
        int FT_Write(int ftHandle, String Buffer,
                int BytesToWrite, IntByReference BytesWritten);
        // FT_STATUS FT_Write (FT_HANDLE ftHandle, LPVOID lpBuffer, DWORD dwBytesToWrite,
        // LPDWORD lpdwBytesWritten)

        /** Read data from the FTDI device. Page 17 */
        int FT_Read(int ftHandle, ByteBuffer Buffer, int BytesToRead,
                IntByReference BytesRead);
        //FT_STATUS FT_Read (FT_HANDLE ftHandle, LPVOID lpBuffer, DWORD dwBytesToRead,
        //LPDWORD lpdwBytesReturned)

        /** Purge the receive and transmit buffers in the FTDI chip. Page 39 */
        int FT_Purge(int ftHandle, int Mask);
        //FT_STATUS FT_Purge (FT_HANDLE ftHandle, DWORD dwMask)

        /** Set the read and write timeouts for communication with the FTDI
         * chip over the USB bus. This is not the TimeOut for GPIB communication. */
        int FT_SetTimeouts(int ftHandle, int ReadTimeOut, int WriteTimeOut);
        //FT_STATUS FT_SetTimeouts (FT_HANDLE ftHandle, DWORD dwReadTimeout, DWORD dwWriteTimeout)

        /** Gets the device status including number of characters in the receive
         *  queue, number of characters in the transmit queue, and the current
         * event status.
         */
        int FT_GetStatus(int ftHandle, IntByReference AmountInRxQueue,
                         IntByReference AmountInTxQueue, IntByReference EventStatus);
        //FT_STATUS FT_GetStatus (FT_HANDLE ftHandle, LPDWORD lpdwAmountInRxQueue,
        //LPDWORD lpdwAmountInTxQueue, LPDWORD lpdwEventStatus)

        /** This function sets the baud rate for the communication between FTDI chip
         * and the Prologix GPIB-USB controller */
        int FT_SetBaudRate(int ftHandle, int BaudeRate);
        // FT_STATUS FT_SetBaudRate (FT_HANDLE ftHandle, DWORD dwBaudRate)
    }//</editor-fold>



    /**
     * Constructor.<p>
     * It calls <code>LoadNative</code> to load the native library, which in turn
     * calls <code>OpenFDTI</code> to establish and initialize connection to the
     * FTDI chip inside the Prologix GPIB-USB controller. <code>OpenFTDI</code>
     * then calls <code>InitPrologix</code> to initialize parameters in the
     * Prologix controller.<p>
     *
     * If the native library has already been loaded, <code>LoadNativ</code>
     * returns without starting the cascade of initializations.
     *
     * @throws IOException When 1) loading the native library failed (bubbles up
     * from <code>LoadNative</code>), or 2) the connection to the FTDI chip could
     * not be established (bubbles up from <code>OpenFTDI</code>), or 3) when
     * the Prologix controller could not be initialized (bubbles up from
     * <code>OpenPrologix</code>).
     */
    // <editor-fold defaultstate="collapsed" desc="Constructor">
    protected GPIB_Prologix()
           throws IOException {

        // init Logger to inherit Logger level from Parent Logger
        m_Logger.setLevel(null);

        // instantiate iC_Properties
        m_iC_Properties = new iC_Properties();


        // load the native library
        LoadNative();

    }//</editor-fold>


    /**
     * Checks the Status of the last communication with the FTDI chip in the
     * Prologix GPIB-USB controller and throws an Exception if an error occurred.
     * If an error occurred, this event is also Logged with level SEVERE. The
     * member variable <code>m_Status</code> is used to determine if an error
     * occurred.
     *
     * @param Msg This String is appended to the message thrown as an
     * Exception when an error occurred.
     * @throws IOException when an error occurred during the last GPIB operation
     */
    // <editor-fold defaultstate="collapsed" desc="check Error TxRx">
    private void checkCommunicationError(String Msg)
            throws IOException {

        // check for communication errors
        if (m_Status != FT_OK) {
            String str = Msg + "Status: " + FT_STATUS_DESCRIPTION[m_Status] + "\n";

            // log event
            m_Logger.severe(str);
            m_Comm_Logger.severe(str);

            throw new IOException(str);
        }
    }//</editor-fold>



    /**
     * Loads the native library if the library was not already loaded. In this 
     * case, it also calls <code>OpenFTDI</code> to establish a connection to the
     * FTDI chip inside the Prologix controller. <code>OpenFTDI</code> also
     * calls <code>InitPrologix</code> to initialize the Prologix controller.<p>
     *
     * If the library has been loaded before this method returns without calling
     * <code>OpenFTDI</code>.<p>
     *
     * @throws IOException If 1) the native library could not be loaded, or 2)
     * the connection to the FTDI chip could not be established (bubbles up
     * from <code>OpenFTDI</code>), or 3) when the Prologix controller could
     * not be initialized (bubbles up from <code>OpenPrologix</code>).
     */
    // <editor-fold defaultstate="collapsed" desc="Load Native">
    private void LoadNative()
            throws IOException {

        // local variables
        String  LibraryName = "no lib specified";

        // get library name
        if (Platform.isWindows())
            LibraryName = "ftd2xx";
        else if (Platform.isMac())
            LibraryName = "ftd2xx";

        // log platform name
        // see https://jna.dev.java.net/nonav/javadoc/index.html
        m_Logger.log(Level.FINE, "JNA: Platform name = {0}\n",
                Integer.toString(Platform.getOSType()));

        // return if the native library has already been loaded
        if (m_ftd2xx != null) {
            // log already loaded
            m_Logger.log(Level.FINE, "JNA: The library {0} was already loaded.\n", LibraryName);

            // return
            return;
        }

        
        // enable Java Virtual Machine crash protection
        if ( !Native.isProtected() ) {
            Native.setProtected(true);

            // check if VM crash protection is supported on the Platform
            if ( !Native.isProtected() ) {
                m_Logger.config("JNA: Java VM Crash Protection is not supported on this platform.\n");
            } else {
                m_Logger.config("JNA: Enabled Java VM Crash Protection.\n");
            }
        }


        // add an additional search path to load the nativ library
        String Path = m_iC_Properties.getPath("GPIB_Prologix.AdditionalLibraryPath", "");
        // Remark: getPath was changed to include a file separator at the end. If 
        // this causes problems here, remove the last character
        
        if ( !Path.isEmpty() ) {
            // add the additional search path
            NativeLibrary.addSearchPath(LibraryName, Path);

            // log event
            m_Logger.log(Level.CONFIG, "JNA: Adding the additional search path {0}\n", Path);
        }

        try {
            // load the native library
            m_ftd2xx = (ftd2xx) Native.loadLibrary(LibraryName, ftd2xx.class);

            // log success
            m_Logger.log(Level.FINE, "JNA: Loaded library {0}.\n", LibraryName);

        } catch (UnsatisfiedLinkError ex) {
            String str = "Could not load the native library for the Prologix GPIB-USB controller.\n";
            str += "Please ensure that the direct drivers are installed properly.\n";
            str += "On WinXP, ftd2xx.dll is typically installed in C:\\Windows\\System32.\n";
            str += "On MacOS, libftd2xx.dylib is typically installed in /usr/local/lib/.\n";
            str += "Java Native Access' response:\n";
            str += ex.getMessage();

            m_Logger.severe(str);
            throw new IOException(str);
        }


        /* When converting Java unicode characters into an array of char, the
         * default platform encoding is used, unless the system property 
         * jna.encoding is set to a valid encoding. This property may be set to 
         * "ISO-8859-1", for example, to ensure all native strings use that encoding.
         * 
         * The charqacter encoding used by JNA is set in IcontrolView.myInit
         */

        // establish the connection to the FTDI chip
        OpenFTDI();

    }//</editor-fold>


    /**
     * Opens the FTDI device with the Product Description String specified in
     * the iC.properties (which is a Prologix GPIB-USB controller).
     * The ftHandle (<code>m_ftPrologix</code>) to address the Prologix controller is
     * assigned here. After opening, the send and receive buffers are
     * purged, the TimeOut value for communication with the FTDI chip over the 
     * USB bus is set (this is different from the GPIB TimeOut value), and the
     * controller is initialized by calling <code>InitPrologix</code>.
     *
     * @throws IOException When 1) the Device Info List could not be
     * created, or 2) no FTDI devices were found to be attached to the USB bus,
     * or 3) the controller could not be opened from the Product Description, or
     * 4) the transmit and receive buffers in the FTDI chip could not be purged, or
     * 5) setting the USB-TimeOut value failed, or 5) if the controller could
     * not be initialized (bubbles up from <code>InitPrologix</code>).
     */
    // <editor-fold defaultstate="collapsed" desc="Open FTDI">
    protected void OpenFTDI()
              throws IOException {


        ///////////////////////////////////////////////////
        // get number of FTDI chips attached to the USB bus
        
        // number of FTDI devices attached to the USB bus
        IntByReference NrDevices = new IntByReference(0);
        
        // create the device info, and get the number of attached FTDI chips
        m_Status = m_ftd2xx.FT_CreateDeviceInfoList(NrDevices);

        // throw Exception if error occurred
        String str = "Could not create the Device Info List.\n";
        checkCommunicationError(str);
            


        // throw Exception if no FTDI devices were found
        if (NrDevices.getValue() == 0) {
            str = "No FTDI devices were found to be attached to the USB bus.\n";
            str += "Ensure the Prologix GPIB-USB controller is attached and operating.\n";

            // log event
            m_Logger.severe(str);

            throw new IOException(str);
        } else {
            // log Nr of devices on bus
            m_Logger.log(Level.FINE, "Found {0} FTDI devices connected to the USB bus.\n",
                    Integer.toString(NrDevices.getValue()));
        }



        ///////////////////////////////
        // open Prologix (using OpenEx)
        
        // get Prologix Product description
        String PrologixProductDescription =
                m_iC_Properties.getString("GPIB_Prologix.ProductDescription", "Prologix GPIB-USB Controller");


        // receive info for the device
        IntByReference ftHandle = new IntByReference(0);
        m_Status = m_ftd2xx.FT_OpenEx(PrologixProductDescription,
                FT_OPEN_BY_DESCRIPTION, ftHandle);
        m_ftPrologix = ftHandle.getValue();

        /* Test error code
        m_Status = m_ftd2xx.FT_Close(m_ftPrologix);

        // receive info for the device
        ftHandle = new IntByReference(0);
        m_Status = m_ftd2xx.FT_OpenEx(PrologixProductDescription,
                FT_OPEN_BY_DESCRIPTION, ftHandle);
        m_ftPrologix = ftHandle.getValue();*/


        // throw Exception if error occurred
        if (m_Status != FT_OK) {
            str = "Could not open the Prologix controller with the Product Description\n";
            str += PrologixProductDescription;
            str += " (" + FT_STATUS_DESCRIPTION[m_Status] + ").\n\n";
            str += (m_Status==1) ? "The Prologix controler might already be opened.\n\n" : "";
            str += "Product Descriptions of FTDI chips found on the USB bus:\n";


            /////////////////////////////////////
            // list FTDI chips present on the bus
            byte[]         Description = new byte[64];
            for (int i=0; i < NrDevices.getValue(); i++) {

                // receive info for the device
                m_Status = m_ftd2xx.FT_GetDeviceInfoDetail(i, new IntByReference(),
                        new IntByReference(), new IntByReference(), new IntByReference(),
                        new byte[64], Description, new IntByReference());

                // throw Exception if error occurred
                if (m_Status != FT_OK) {
                    str += "Could not obtain the Product Description ("
                            + FT_STATUS_DESCRIPTION[m_Status] + ")\n";
                }

                // make a nice String for the Product Description
                String PD = new String(Description);
                int end = PD.indexOf(0);
                if (end != -1)
                    str += PD.substring(0, end) + "\n";
            }

            // log event
            m_Logger.severe(str);

            throw new IOException(str);
        }


        /////////////////////////////
        // purge send/receive buffers
        m_Status = m_ftd2xx.FT_Purge(m_ftPrologix, FT_PURGE_RX & FT_PURGE_TX);

        // throw Exception if error occurred
        str = "Could not purge FTDI's receive and transmit buffers.\n";
        checkCommunicationError(str);
                    

        //////////////////
        // set USB TimeOut
        // important for WriteToPrologix and ReadFromPrologix
        m_Status = m_ftd2xx.FT_SetTimeouts(m_ftPrologix, TIMEOUT_USB, TIMEOUT_USB);
        m_Logger.log(Level.CONFIG, "USB TimeOut = {0}", TIMEOUT_USB);

        // throw Exception if error occurred
        str = "Could not set the USB-TimeOut value for the FTDI chip.\n";
        checkCommunicationError(str);


        //////////////////
        // init controller
        InitPrologix();

    }//</editor-fold>



    /**
     * Initializes the Prologx GPIB-USB controller. It sets parameters for:<br>
     * ++auto, ++eoi, ++eos, ++eot_enable, ++eot_char, ++mode, ++ifc,
     * ++read_tmo_ms
     *
     * @throws IOException When communicating with the FTDI chip in the Prologix
     * controller failed.
     */
    // <editor-fold defaultstate="collapsed" desc="InitController">
    private void InitPrologix()
            throws IOException {
        
        // commands that address the Prologix controller start with ++
        // and must be terminated with \n to be executed


        ////////////////////////////////////////
        // allocate memory for the ReceiveBuffer
        int dummy = RECEIVE_BUFFER_SIZE;

        // prevent runtime errors
        if (dummy < 1) {
            dummy = 1;
            m_Logger.severe("RECEIVE_BUFFER_SIZE was smaller than 1.\n");
        }

        // allocate the memory
        m_ReceiveBuffer = ByteBuffer.allocate(dummy);

        // The Buffer created with allocate will be backed by an array
        // so the following should never be true
        if ( !m_ReceiveBuffer.hasArray() ) {
            m_Logger.severe("m_ReceiveBuffer is NOT backed by an array.\n");
        }


        // suggested from Prologix' support
        try {
            Thread.sleep(250);//DEBUG_PROLOGIX_RW_DELAY
            m_Logger.log(Level.FINER, "Waited 250ms\n");
            //m_Logger.log(Level.FINER, "Waited {0}ms\n", Integer.toString(DEBUG_PROLOGIX_RW_DELAY));
        } catch (InterruptedException ex) {}

        // *** First attempt
        // get a list of supported commands
        // to find if the firmware supports the ++savecfg command
        WriteToPrologix("++help");
        String Response = ReadFromPrologix();

        // interestingly, the Prologix sometimes does not send
        // any response. upon a second query, it does however
        m_GUI.DisplayStatusMessage("First Attempt on ++ help read " + Integer.toString(Response.length()) + " bytes\n", false);

        // try a second time if first query failed
        if (Response.length() < 1700) {
            // *** Second attempt
            WriteToPrologix("++help");
            Response = ReadFromPrologix();
            m_GUI.DisplayStatusMessage("Second Attempt on ++ help read " + Integer.toString(Response.length()) + " bytes\n", false);
        }


        // -----
        // disable automatic saving of configuration parameters in EPROM
        // to help save wear out of the EPROM if it is supported by the
        // installed firmware
        if (Response.toLowerCase().contains("++savecfg")) {
            
            // get current value
            WriteToPrologix("++savecfg");
            String ans = ReadFromPrologix().trim();
            m_Logger.log(Level.FINER, "++savecfg was {0}\n", ans);

            // get preference whether or not to set ++savecfg
            String cfg = m_iC_Properties.getString("GPIB_Prologix.Savecfg", "0");

            // syntax check
            if ( !cfg.equals("1") )
                cfg = "0";

            // check if current value differs from desired
            if ( !ans.equals(cfg) ) {
                WriteToPrologix("++savecfg " + cfg);
                m_Logger.log(Level.CONFIG, "++savecfg set to {0}\n", cfg);
            }
        } else {
            m_Logger.finer("++savecfg is NOT supported.\n");
        }

        // -----
        // disable to automatically address instruments to talk after
        // sending them a command in order to read their response

        // get current value
        WriteToPrologix("++auto");
        String ans = ReadFromPrologix().trim();
        m_Logger.log(Level.FINER, "++auto was {0}\n", ans);

        // check current value
        if ( !ans.equals("0") ) {
            WriteToPrologix("++auto 0");
            m_Logger.finer("++auto set to 0\n");
        }

        // -----
        // enable the assertion of the EOI signal with the last character
        // of any command sent over GPIB port

        // get current value
        WriteToPrologix("++eoi");
        ans = ReadFromPrologix().trim();
        m_Logger.log(Level.FINER, "++eoi was {0}\n", ans);

        // check current value
        if ( !ans.equals("1") ) {
            WriteToPrologix("++eoi 1");
            m_Logger.finer("++eoi set to 1\n");
        }

        // -----
        // specify GPIB termination character appended by the
        // Prologix controller (default is CR+LF)

        // get current value
        WriteToPrologix("++eos");
        ans = ReadFromPrologix().trim();
        m_Logger.log(Level.FINER, "++eos was {0}\n", ans);

        // check current value
        int EOS = m_iC_Properties.getInt("GPIB_Prologix.EOS", 0);
        if ( !ans.equals(Integer.toString(EOS)) ) {
            WriteToPrologix("++eos " + EOS);
            m_Logger.log(Level.FINER, "++eos set to {0}\n", EOS);
        }

        // -----
        // disables the appending of a user specified character (see eot_char)
        // to USB output whenever EOI is detected while reading a
        // character from the GPIB port

        // get current value
        WriteToPrologix("++eot_enable");
        ans = ReadFromPrologix().trim();
        m_Logger.log(Level.FINER, "++eot_enable was {0}\n", ans);

        // check current value
        if ( !ans.equals("0") ) {
            WriteToPrologix("++eot_enable 0");
            m_Logger.finer("++eot_enable set to 0\n");
        }

        // test
//        WriteToPrologix("++eot_enable 1");
//        WriteToPrologix("++eot_char 10");

        // -----
        // configure the Prologix GPIB-USB controller to be a CONTROLLER

        // get current value
        WriteToPrologix("++mode");
        ans = ReadFromPrologix().trim();
        m_Logger.log(Level.FINER, "++mode was {0}\n", ans);

        // check current value
        if ( !ans.equals("1") ) {
            WriteToPrologix("++mode 1");
            m_Logger.finer("++mode set to 1\n");
        }


        // make the Prologix GPIB-USB controller the Controller-In-Charge
        // on the GPIB bus
        WriteToPrologix("++ifc");

        // -----
        // set time-out value for read and spoll in milliseconds

        // get current value
        WriteToPrologix("++read_tmo_ms");
        ans = ReadFromPrologix().trim();
        m_Logger.log(Level.FINER, "++read_tmo_ms was {0} (GPIB TimeOut)\n", ans);

        // check current value
        if ( !ans.equals( Integer.toString(TIMEOUT_GPIB)) ) {
            WriteToPrologix("++read_tmo_ms " + TIMEOUT_GPIB);
            m_Logger.log(Level.FINER, "++read_tmo_ms set to {0}\n", TIMEOUT_GPIB);
        }


        // ask Prologix for firmware version
        WriteToPrologix("++ver");
        Response = ReadFromPrologix().trim();
        m_Logger.log(Level.FINER, "++ver= {0}\n", Response);


        // suggested from Prologix' support
        try {
            Thread.sleep(DEBUG_PROLOGIX_RW_DELAY);
            m_Logger.log(Level.FINER, "Waited {0}ms\n", Integer.toString(DEBUG_PROLOGIX_RW_DELAY));
        } catch (InterruptedException ex) {}

    }//</editor-fold>



    /**
     * Closes the connection to the GPIB controller. This method is called from
     * <code>Dispatcher.run</code> after the Script has been processed.<p>
     *
     * This method switches the Prologix GPIB-USB controller in a device mode
     * (rather than a controller mode) and addresses it to listen to GPIB address
     * specified in the <code>iC.properties</code>. This allows multiple GPIB
     * controllers to be connected at the same time.
     *
     * @throws IOException When the closing caused a GPIB error
     */
    @Override
    protected void CloseController() 
              throws IOException {

        // log
        m_Logger.fine("Closing connection to the GPIB controller (GPIB_Prologix).\n");

        // make the controller a Device, not a Controller
        // this might help if more than one controller are attched to the GPIB bus
        // it does not help
        WriteToPrologix("++mode 0");

        // check for communication errors
        String str = "Could not set ++mode 0 (make the Prologix a device rather than a controller).\n";
        checkCommunicationError(str);
        m_Logger.finer("Just set ++mode 0 (device, not controller).\n");



        // make it listen to address 31
        String adr = m_iC_Properties.getString("GPIB_Prologix.DeviceGPIBaddress", "31");
        WriteToPrologix("++add " + adr);

        // check for communication errors
        str = "Could not set the GPIB address " + adr + ".\n";
        checkCommunicationError(str);
        m_Logger.log(Level.FINER, "Just set the address to {0}.\n", adr);


        
        // close communication to the FTDI chip (built into the Prologix controller)
        int Status = m_ftd2xx.FT_Close(m_ftPrologix);
        
        // check for communication errors
        str = "An error occurred upon closing the connection to the FTDI chip inside the Prologix GPIB-USB controller.\n";
        checkCommunicationError(str);
        
        // assign null to detect any calls after I don't expect any
        m_ftd2xx = null;
    }



    /**
     * Returns the library version of the native FTD2xx driver. Only
     * Windows operating Systems are supported. The response on MacOS
     * is close to 65538.
     *
     * @return The library version of the FTD2xx native driver
     * @throws IOException When a communication error occurred upon invoking
     * the native code
     */
    // <editor-fold defaultstate="collapsed" desc="get Library Version">
    private int getLibraryVersion() 
            throws IOException {

        // this is the Java data type equivalent to LPDWORD
        IntByReference LibVer = new IntByReference();


        // get lib version from FTD2xx .dll/.dylib
        m_Status = m_ftd2xx.FT_GetLibraryVersion(LibVer);

        // check for Errors
        String str = "Could not obtain the library version.\n";
        checkCommunicationError(str);

        return LibVer.getValue();
        
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

        // send Selected Device Clear
        Send("++clr\n");


        // Start Speed Tests (used during debugging)
        // SpeedTestV2(10);
        // SpeedTest(10);
    }//</editor-fold>




    /**
     * Checks if the Transmit-Queue of the FTDI chip in the Prologix controller
     * is empty. If it is not empty, this method waits a bit and tries again
     * for a certain number of times. This method was used during development
     * and it should not be necessary to use it again.
     * @throws IOException hmm ...
     */
    // <editor-fold defaultstate="collapsed" desc="wait For Empty Tx Queue">
    private void waitForEmptyTxQueue()
            throws IOException {

        // constants
        final int TIME_INBETWEEN = 100;

        // local variables
        IntByReference NrBytesRx = new IntByReference();
        IntByReference NrBytesTx = new IntByReference();
        IntByReference EventStatus = new IntByReference();

        boolean Done = false;

        // get number of tries
        final int NR_TRIES = TIMEOUT_USB / TIME_INBETWEEN;

        // check for an empty queue multiple times
        for (int i=0; i < NR_TRIES; i++) {

            // get status of the communication to the FTDI chip in the
            // Prologix controller
            m_Status = m_ftd2xx.FT_GetStatus(m_ftPrologix, NrBytesRx,
                                NrBytesTx, EventStatus);

            // check for communicaton errors
            String str = "Could not obtain the status of the Rx/Tx queues.\n";
            checkCommunicationError(str);

            // is Tx queue empty?
            if ( NrBytesTx.getValue() == 0 ) {
                // remember that it was empty
                Done = true;

                // log that it was empty
                m_Logger.fine("FTDI's Tx queue is empty\n");

                // exit
                break;
            } else {
                // log event
                m_Logger.log(Level.FINER, "FTDI''s Tx queue contains {0} bytes\n",
                        Integer.toString(NrBytesTx.getValue()));
            }

            // wait a bit
            try { Thread.sleep(TIME_INBETWEEN);} catch (InterruptedException ex) {}
        }

        if ( !Done ) {
            String str = "FTDI's Tx queue did not become empty within the specified TimeOut value\n";

            // log event
            m_Logger.severe(str);

            throw new IOException(str);
        }
    }//</editor-fold>


    /**
     * Sends the given String to the Prologix GPIB-USB controller. The String should
     * end with \r\n or without special termination, in which case \r\n is appended
     * automatically. The \r\n is requested by the Prologix controller.
     *
     * @param Msg The data to be sent
     * @return The number of bytes that have been sent
     * @throws IOException When the transmission caused an error
     */
    // <editor-fold defaultstate="collapsed" desc="Write To Prologix">
    private synchronized int WriteToPrologix(String Msg)
                         throws IOException {


        // wait until transmit buffer is empty
        //waitForEmptyTxQueue();

        // log what's about to be sent
        m_Logger.log(Level.FINEST, "WriteToPrologix will send: {0}", Msg);


        // append \r\n so that the Prologix controller recognizes the end of transmission
        final String append = "\r\n";
        if ( !Msg.endsWith(append) )
            Msg += append;


        // send to the Instrument
        m_Status = m_ftd2xx.FT_Write(m_ftPrologix, Msg, Msg.length(), m_NrBytes);

        // check for communication errors
        String str = "An error occurred upon sending data to the Prologix GPIB-USB controller.\n";
        checkCommunicationError(str);

        // check for TimeOut
        if (Msg.length() != m_NrBytes.getValue()) {
            str = "A GPIB TimeOut occurred upon sending data to the Prologix GPIB-USB controller.\n";
            str += String.format("Only %d characters out of %d characters were sent.\n", m_NrBytes.getValue(), Msg.length());

            // log event
            m_Logger.severe(str);
            m_Comm_Logger.severe(str);

            throw new IOException(str);
        }

        // return the number of bytes that have been sent
        return m_NrBytes.getValue();
    }//</editor-fold>


    /**
     * Sends the given String via the GPIB bus to the Instrument specified in
     * <code>Open</code> and returns the number of bytes actually sent. If
     * <code>Msg</code> is empty, the method returns immediately.<p>
     *
     * @param Msg Data to be sent
     * @return Number of data bytes actually sent.
     * @throws IOException When the transmission caused a GPIB error
     */
    // <editor-fold defaultstate="collapsed" desc="SendCommand">
    @Override
    protected long Send(String Msg)
              throws IOException {

        // commands that address the Prologix controller start with ++
        // and must be terminated with \n to be executed
        
        
        // do nothing if Msg is empty. See comment in GPIB_NI.SendCommand
        if (Msg.isEmpty()) {
            return 0;
        }

        // suggested from Prologix' support (not necessary any more it seems)
        if (DEBUG_PROLOGIX_RW_DELAY > 0 ) {
            try {
                Thread.sleep(DEBUG_PROLOGIX_RW_DELAY);
                m_Logger.log(Level.FINEST, "Waited {0}ms\n", Integer.toString(DEBUG_PROLOGIX_RW_DELAY));
            } catch (InterruptedException ex) {}
        }

        // tell Prologix controller to address the right Instrument
        String Adr = String.format(Locale.US, "++addr %d", m_GPIB_Adr);
        WriteToPrologix(Adr);

        // suggested from Prologix' support (not necessary any more it seems)
        if (DEBUG_PROLOGIX_RW_DELAY > 0 ) {
            try {
                Thread.sleep(DEBUG_PROLOGIX_RW_DELAY);
                m_Logger.log(Level.FINEST, "Waited {0}ms\n", Integer.toString(DEBUG_PROLOGIX_RW_DELAY));
            } catch (InterruptedException ex) {}
        }

        // send the message
        int NrBytes = WriteToPrologix(Msg);

        // return the number of bytes sent
        return NrBytes;

    }//</editor-fold>



    /**
     * Gets the Status of the last GPIB call. After each GPIB call, the
     * error flag in the Status Byte should, ideally, be checked. If an error
     * occurred, this method returns a String with a short description of what
     * went wrong. If an error occurred, it should be logged to the Comm Logger.
     *
     * @return When an error occurred during the last GPIB call, the returned
     * String contains a somewhat helpful explanation of what went
     * wrong. An empty String is returned if no error occurred.
     */
    // <editor-fold defaultstate="collapsed" desc="get GPIB Status">
    @Override
    protected String getGPIBStatus() {

        // check for communication errors
        if (m_Status != FT_OK) {
            // make a nice String with the GPIB Status
            String str = String.format(Locale.US, "GPIB Status: Status = %d (%s)\n",
                    m_Status,
                   (m_Status<FT_STATUS_DESCRIPTION.length ? FT_STATUS_DESCRIPTION[m_Status] : "Error number not recognized"));

            // log event
            m_Logger.finer(str);
            //m_Comm_Logger.finer(str);

            return str;
        } else {
            return "";
        }
    }//</editor-fold>


    /**
     * Read data from the Prologix GPIB-USB controller.
     * 
     * @return The data read from the controller. The String's length equals
     * the number of bytes received from the Prologix controller.
     * @throws IOException When the transmission caused an error or the Receive
     * Buffer is too small.
     */
    // <editor-fold defaultstate="collapsed" desc="Read From Prologix">
    private synchronized String ReadFromPrologix()
                         throws IOException {

        // local variables
        String ret = "";
        String dummy = "";
        IntByReference AmountInRxQueue = new IntByReference();
        IntByReference AmountInTxQueue = new IntByReference();
        IntByReference EventStatus = new IntByReference();

        // check RECEIVE_BUFFER_SIZE vs. limit
        if (RECEIVE_BUFFER_SIZE != m_ReceiveBuffer.limit())
            m_Logger.warning("RECEIVE_BUFFER_SIZE != m_ReceiveBuffer.limit()\n");

        // optimizing Prologix's FT_Read() performance by reading the number of 
        // bytes in the queue instead of letting a USB Timeout occur.
        // nice try, but doesn't work because the queue is often beeing filled after
        // it's size is queries.
        if (false) {

            // wait a bit
            try {Thread.sleep(30);} catch (InterruptedException ex) {}


            // get nr of bytes in the receive queue
            m_Status = m_ftd2xx.FT_GetStatus(m_ftPrologix, AmountInRxQueue,
                      AmountInTxQueue, EventStatus);

            // check for communication errors
            String str = "Could not determine the number of availble bytes in the RxQueue.\n";
            checkCommunicationError(str);

            // check if Buffer is big enough
            if (m_ReceiveBuffer.limit() <= AmountInRxQueue.getValue()) {
                str = "The Receive Buffer Size is too small; please increase the size in iC.properties.\n";
                throw new IOException(str);
            }

            // log number of available bytes
            m_Comm_Logger.log(Level.FINEST, "ReadFromPrologix: {0} bytes in RxQueue\n",
                    Integer.toString(AmountInRxQueue.getValue()));


            
            // do as long as there are bytes available to read
            while (AmountInRxQueue.getValue() > 0) {

                // read from Prologix
                m_Status = m_ftd2xx.FT_Read(m_ftPrologix, m_ReceiveBuffer,
                            AmountInRxQueue.getValue(), m_NrBytes);

                // check for communication errors
                str = "Error receiving data from the Prologix GPIB-USB controller.\n";
                checkCommunicationError(str);

                // check for TimeOut
                if (m_NrBytes.getValue() != AmountInRxQueue.getValue()) {
                    str = "Error: A USB TimeOut occurred during reading from the Prologix GPIB-USB controller.\n"
                        + "The received data is likely incorrect!\n";
                    m_GUI.DisplayStatusMessage(str);
                }

                // convert to a String
                dummy = new String(m_ReceiveBuffer.array());

                // set the correct size
                ret += dummy.substring(0, m_NrBytes.getValue());

                // log number of bytes read
                m_Comm_Logger.log(Level.FINEST, "ReadFromPrologix just read {0} bytes\n",
                        Integer.toString(m_NrBytes.getValue()));

                // wait a bit
                try {Thread.sleep(30);} catch (InterruptedException ex) {}


                // get nr of bytes in the receive queue
                m_Status = m_ftd2xx.FT_GetStatus(m_ftPrologix, AmountInRxQueue,
                          AmountInTxQueue, EventStatus);

                // check for communication errors
                str = "Could not determine the number of availble bytes in the RxQueue.\n";
                checkCommunicationError(str);

                // check if Buffer is big enough
                if (m_ReceiveBuffer.limit() <= AmountInRxQueue.getValue()) {
                    str = "The Receive Buffer Size is too small; please increase the size in iC.properties.\n";
                    throw new IOException(str);
                }

                // log number of bytes read
                m_Comm_Logger.log(Level.FINEST, "ReadFromPrologix: {0} bytes in RxQueue\n",
                        Integer.toString(AmountInRxQueue.getValue()));
            }

        } else {


            // read from prologix
            m_Status = m_ftd2xx.FT_Read(m_ftPrologix, m_ReceiveBuffer,
                        m_ReceiveBuffer.limit(), m_NrBytes);
            
            /* TODO 3* fix Prologix on a Mac
             * On a Mac, the returned number of bytes is correct, but the ReceiveBuffer
             * only contains 0's. On Windows, the same code works correctly. Using
             * C to call the FTDI methods directly works, so it seems to be related
             * to Java/JNA. Interestingly, using C it's nessesary to send ++help\n\r (10, 13)
             * and using \r\n reads 0 bytes. In Java on Win, I send \r\n and using
             * \n\r on Mac doesn't work either.
             * Installed the 64 bit version of the D2XX drivers (which looks the
             * same as the 32 bit version) and starting java without -d32 option
             * gives an error of an invalid FT_Handle. check what could cause this.
             */

            // check for communication errors
            String str = "Error receiving data from the Prologix GPIB-USB controller.\n";
            checkCommunicationError(str);


            // convert to a String
            ret = new String(m_ReceiveBuffer.array());

            // set the correct size
            ret = ret.substring(0, m_NrBytes.getValue());

            // log number of bytes read
            m_Logger.log(Level.FINEST, "ReadFromPrologix just read {0} bytes\n",
                    Integer.toString(m_NrBytes.getValue()));
        }

        /*
         * There is no way to detect TimeOuts unless the number of bytes to
         * be read is known because even when a TimeOut occurred, the Status
         * will be FT_OK (see page 17).
         */

        return ret;
    }//</editor-fold>



    /**
     * Receives data from the Instrument specified in <code>Open</code>.
     * The number of Bytes that are requested from the Instrument are specified
     * by <code>RECEIVE_BUFFER_SIZE</code>, which, in turn, is specified in the
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
    // <editor-fold defaultstate="collapsed" desc="Receive">
    @Override
    protected String Receive(boolean Trim)
              throws IOException {
        
        // commands that address the Prologix controller start with ++
        // and must be terminated with \n to be executed

        // suggested from Prologix' support (not necessary any more it seems)
        if (DEBUG_PROLOGIX_RW_DELAY > 0 ) {
            try {
                Thread.sleep(DEBUG_PROLOGIX_RW_DELAY);
                m_Logger.log(Level.FINEST, "Waited {0}ms\n", Integer.toString(DEBUG_PROLOGIX_RW_DELAY));
            } catch (InterruptedException ex) {}
        }

        
        // tell Prologix controller to address the right Instrument
        String Adr = String.format(Locale.US, "++addr %d", m_GPIB_Adr);
        WriteToPrologix(Adr);

        // suggested from Prologix' support (not necessary any more it seems)
        if (DEBUG_PROLOGIX_RW_DELAY > 0 ) {
            try {
                Thread.sleep(DEBUG_PROLOGIX_RW_DELAY);
                m_Logger.log(Level.FINEST, "Waited {0}ms\n", Integer.toString(DEBUG_PROLOGIX_RW_DELAY));
            } catch (InterruptedException ex) {}
        }

        // tell the Prologix controller to address the Instrument
        // to send data (become talker)
        WriteToPrologix("++read eoi");  // was ++read

        // suggested from Prologix' support (not necessary any more it seems)
        if (DEBUG_PROLOGIX_RW_DELAY > 0 ) {
            try {
                Thread.sleep(DEBUG_PROLOGIX_RW_DELAY);
                m_Logger.log(Level.FINEST, "Waited {0}ms\n", Integer.toString(DEBUG_PROLOGIX_RW_DELAY));
            } catch (InterruptedException ex) {}
        }

        // read the data
        String ret = ReadFromPrologix();

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
    public int ReadStatusByte()
           throws IOException {

        // ask for the Status Byte by serially poplling
        String poll = String.format(Locale.US, "++spoll %d", m_GPIB_Adr);
        WriteToPrologix(poll);
        

        // read the Status Byte
        String StatusByte = ReadFromPrologix();

        // convert the Status Byte
        int Status;
        try {
            Status = getInteger(StatusByte);
        } catch (ScriptException ex) {
            String str = "Could not cenvert the received Status Byte into a valid number.\n";
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
        WriteToPrologix("++loc");

        // check for communication errors
        // error check is already in WriteToPrologix

    }//</editor-fold>


    /** 
     * Used to test the speed of the GPIB controller. Enable the Speed Test in
     * the <code>Open</code> method respectively issue the ScriptCommand
     * SpeedTest NrIterations (no Auto GUI is specified for this Script Command).<p>
     *
     * This is a special implementation of the general <code>SpeedTest</code> method
     * in the <code>GPIB_Driver</code> class which communicates directly with the
     * FTDI/Prologix chip without (re-)addressing the instrument. The time obtained
     * here can be viewed as minimal achievable time, while the time obtained
     * from <code>SpeedTest</code> might be slightly larger due to overhead that
     * needs, in general, to be sent.
     */
    // <editor-fold defaultstate="collapsed" desc="Speed Test V2">
    protected void SpeedTestV2(int NrIterations) {

        // initial wait
        try { Thread.sleep(250); } catch (InterruptedException ex) {}
        m_Logger.log(Level.FINER, "Waited {0}ms\n", Integer.toString(250));

        // display status msg
        m_GUI.DisplayStatusMessage("Starting GPIB_Prologix.SpeedTestV2().\n", false);


        // init Timer
        long T = System.currentTimeMillis();
        long max_dT = 0;

        // test it multiple times
        for (int i=0; i<NrIterations; i++) {
            try {

                //Write *idn?
                WriteToPrologix("*IDN?");

                //Write ++read eoi
                WriteToPrologix("++read eoi");

                //Wait
                //final int WaitTime = 5;
                //try { Thread.sleep(WaitTime); } catch (InterruptedException ex) {}
                //m_Logger.log(Level.FINER, "Waited {0}ms\n", Integer.toString(WaitTime));

                //Read response
                String idn = ReadFromPrologix().trim();

                // calc dT
                long dummy = System.currentTimeMillis();
                long dT = dummy - T;
                T = dummy;
                if (max_dT < dT)
                    max_dT = dT;

                // show (and log) results
                m_GUI.DisplayStatusMessage(String.format("%3d (%4dms): %s\n", i, dT, idn), false);

            } catch (IOException ex) {
                // show and log error
                m_GUI.DisplayStatusMessage("GPIB_Prologix.SpeedTestV2: IO failed.\n", false);
                return;
            }
        }

        // display status msg
        m_GUI.DisplayStatusMessage("max dT = " + max_dT + "\n", false);
        m_GUI.DisplayStatusMessage("The GPIB_Prologix.SpeedTestV2 is done.\n", false);
    }//</editor-fold>
}
