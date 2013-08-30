// TODO ask for docu about:
// TmcGetInitializeError, TmcGetDetailLastError, TmcCheckError, TmcGetStatusByte
// TmcGotoLocal
// ask about xTmc... methods and L2 Error Numbers
// DL9000 what does :WAVeform:SIGN? mean?
// [DL9000 difference between :Acquire:Average:count and :EWeight]
// DL9000 is it enough to set :Waveform:Record AVERAGE, :History:Current:Mode AVERAGE,
// :History:Current:Display 0,-15 once or is it required to
// be set for each waveform (set with :Waveform:Trace)
// DL9000 who can I find out how many waveforms are available?

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
import icontrol.iC_Properties;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class provides access to the native code to access Yokogawa's TMCTL
 * library.<p>
 *
 * It uses Java Native Access (JNA) package to access the driver (tmctl.dll). 
 * The TMCTL library is only available for Windows platform.<p>
 *
 * <h3>Usage:</h3>
 * <ul>
 *  <li>Instantiate the class. This calls <code>LoadNative</code> to establish
 *      access to the native code (tmctl.dll).
 *  <li>Open the connection to an Instrument using <code>Open</code>. It clears
 *      the device (only for GPIB communication), assigns a proper value to
 *      <code>m_DeviceID</code>, and sets the TimeOut value.
 *  <li>To send/receive data use <code>Send</code> and <code>Receive</code>.
 *  <li>To receive Block-Data (e.g. to receive a waveform from an oscilloscope)
 *      use <code>ReceiveBlockData</code>
 *  <li>To close the connection to an Instrument call <code>CloseInstrument</code>.
 *  <li>Each communication with the TMCTL library checks the status of the last 
 *      transaction using <code>checkErrorTMCTL</code> which throws an 
 *      <code>IOException</code> if an error occurred.
 * </ul>
 *
 * @author KPP (Kurt Pernstich: pernstich@alumni.ethz.ch)
 * @version 0.1
 *
 */
public class TMCTL_Driver {

    ///////////////////
    // member variables

    /** Handle to access the native code */
    private static TMCTL    m_TMCTL = null;

    /** The Logger for this class */
    private static final Logger m_Logger = Logger.getLogger("iC.Instruments.TMCTL_Driver");
    
    /** The Comm Logger */
    protected static final Logger m_Comm_Logger = Logger.getLogger("Comm");
    
    /** Receive Buffer */
    private static ByteBuffer m_ReceiveBuffer = null;


    /**
     * Grants access to global properties of the iC application in a resource
     * bundle file 'iC.properties' */
    protected static iC_Properties m_iC_Properties;
    
    
    /** Device ID; obtained in <code>Open</code> */
    private int m_DeviceID;

    ///////////////////////////////
    // tmctl.dll specific CONSTANTS
    // VERY IMPORTANT REMARK:
    // Because there is no unsigned short in Java, the constants are defined
    // as int's. Casting a short to an int keeps to lower 16 bits as expected, but
    // the int-value is interpreted wrong, for instance as -1 instead of 0xFFFF.
    // <editor-fold defaultstate="collapsed" desc="tmctl.dll CONSTANTS">
    
    /* Control setting (Wire parameter) */
    private final int TM_NO_CONTROL =   0;
    private final int TM_CTL_GPIB =     1;
    private final int TM_CTL_RS232 =    2;
    private final int TM_CTL_USB =      3;
    private final int TM_CTL_ETHER =    4;
    private final int TM_CTL_USBTMC =   5;  // for DL9000
    private final int TM_CTL_ETHERUDP = 6;
    private final int TM_CTL_USBTMC2 =  7;  // excluding DL9000
    private final int TM_CTL_VXI11 =    8;
    private final int TM_CTL_USB2 =     9;  // not explained in tmctl read me(ENG).txt
    

    /* GPIB */

    /* RS232 */
//    private final String TM_RS_1200 =	"0";
//    private final String TM_RS_2400 =	"1";
//    private final String TM_RS_4800 =	"2";
//    private final String TM_RS_9600 =	"3";
//    private final String TM_RS_19200 =	"4";
//    private final String TM_RS_38400 =  "5";
//    private final String TM_RS_57600 =	"6";
//    private final String TM_RS_115200 = "7";
//
//    private final String TM_RS_8N =    "0";
//    private final String TM_RS_7E =    "1";
//    private final String TM_RS_7O =    "2";
//    private final String TM_RS_8O =    "3";
//    private final String TM_RS_7N5 =   "4";
//
//    private final String TM_RS_NO =    "0";
//    private final String TM_RS_XON =   "1";
//    private final String TM_RS_HARD =  "2";

    /* USB */
//    private final int TM_USB_CHECK_OK = 0;
//    private final int TM_USB_CHECK_NOTOPEN = 1;
//    private final int TM_USB_CHECK_NODEVICE = 2;
//
//    private final int TM_USB_READPIPE = 0;
//    private final int TM_USB_WRITEPIPE = 1;
//    private final int TM_USB_STATUSPIPE = 2;

    /* Error Number */
    private final int TMCTL_NO_ERROR =              0x00000000;		/* No error */
    private final int TMCTL_TIMEOUT =               0x00000001;		/* Timeout */
    private final int TMCTL_NO_DEVICE =             0x00000002;		/* Device Not Found */
    private final int TMCTL_FAIL_OPEN =             0x00000004;		/* Open Port Error */
    private final int TMCTL_NOT_OPEN =              0x00000008;		/* Device Not Open */
    private final int TMCTL_DEVICE_ALREADY_OPEN =   0x00000010;		/* Device Already Open */
    private final int TMCTL_NOT_CONTROL =           0x00000020;		/* Controller Not Found */
    private final int TMCTL_ILLEGAL_PARAMETER =     0x00000040;		/* Parameter is illegal */
    private final int TMCTL_SEND_ERROR =            0x00000100;		/* Send Error */
    private final int TMCTL_RECV_ERROR =            0x00000200;		/* Receive Error */
    private final int TMCTL_NOT_BLOCK =             0x00000400;		/* Data is not Block Data */
    private final int TMCTL_SYSTEM_ERROR =          0x00001000;		/* System Error */
    private final int TMCTL_ILLEGAL_ID =            0x00002000;		/* Device ID is Illegal */
    private final int TMCTL_NOT_SUPPORTED =         0x00004000;		/* this feature not supportred */
    private final int TMCTL_INSUFFICIENT_BUFFER =   0x00008000;		/* unsufficient buffer size */

//    /* L2 Error Number */
//    // not sure if this is the right data type. In tmctl.h the numbers were in ()
//    private final int TMCTL2_NO_ERROR =             00000;   /* No error */
//    private final int TMCTL2_TIMEOUT =              10001;   /* Timeout */
//    private final int TMCTL2_NO_DEVICE =            10002;   /* Device Not Found */
//    private final int TMCTL2_FAIL_OPEN =            10003;   /* Open Port Error */
//    private final int TMCTL2_NOT_OPEN =             10004;   /* Device Not Open */
//    private final int TMCTL2_DEVICE_ALREADY_OPEN =  10005;   /* Device Already Open */
//    private final int TMCTL2_NOT_CONTROL =          10006;   /* Controller Not Found */
//    private final int TMCTL2_ILLEGAL_PARAMETER =    10007;   /* Parameter is illegal */
//    private final int TMCTL2_SEND_ERROR =           10008;   /* Send Error */
//    private final int TMCTL2_RECV_ERROR =           10009;   /* Receive Error */
//    private final int TMCTL2_NOT_BLOCK =            10010;   /* Data is not Block Data */
//    private final int TMCTL2_SYSTEM_ERROR =         10011;   /* System Error */
//    private final int TMCTL2_ILLEGAL_ID =           10012;   /* Device ID is Illegal */
//    private final int TMCTL2_NOT_SUPPORTED =        10013;   /* this feature not supportred */
//    private final int TMCTL2_INSUFFICIENT_BUFFER =  10014;   /* unsufficient buffer size */

//    private final int ADRMAXLEN = 64;    
    
//    typedef	struct _Devicelist
//    {
//            char	adr[ADRMAXLEN] ;
//    } DEVICELIST ;
//
//    typedef	struct _DevicelistEx
//    {
//            char		adr[ADRMAXLEN] ;
//    unsigned short	vendorID ;
//    unsigned short	productID ;
//            char		dummy[188] ;
//    } DEVICELISTEX ;

//    // ÉRÅ[ÉãÉoÉbÉNÉãÅ[É`Éì
//    typedef void(__stdcall *Hndlr)(int, UCHAR, ULONG, ULONG) ;
    
    //</editor-fold>



    ///////////////////////////
    // class specific constants


    /** Size of the Receive buffer. Note that the entire response is read from the
     * Instrument, this is just the size of one chunk of the Instrument's response */
    private final int RECEIVE_BUFFER_SIZE = 
            (new iC_Properties()).getInt("TMCTL_Driver.ReceiveBufferSize", 100000);;

    
    /**
     * Defines the interface required by JNA to invoke the native library.<p>
     * This interface defines all function calls 'into' the native
     * code (.dll). It is the standard, stable way of mapping, which
     * supports extensive customization and mapping of Java to native types.<p>
     *
     * Please note that all function declaration have been imported from tmctl.h,
     * but only the uncommented method declarations have been tested. To use
     * a native function that is currently commented, uncomment it, ensure the
     * data type conversion is correct, and test it. The tmctl.h file defines
     * also functions that start with x... but seem otherwise to be identical.
     * Could not find what the difference is, so the 'regular' functions are
     * used.
     */
    // <editor-fold defaultstate="collapsed" desc="tmctl Interface Declarations">
    private interface TMCTL extends Library {
        
        /* Employed data type conversions:
         * native          JNA
         * --------------------------------
         * int              int
         * char*            ByteBuffer
         * int*             IntByReference
         * ULONG            int (the dll is only 32 bit anyways); could be NativeLong
         */

   
        //////////////////////////////////////////////
        // Function declaration (adapted from tmctl.h)

        /** Initializes and opens a connection to the specified device. */
        int TmcInitialize(int Wire, String Address, IntByReference DeviceID );// char*, int*
        
        
//        int TmcSetIFC( int id, int tm ) ;
        
        /** Executes a selected device clear (SDC). This is a GPIB-specific command. 
         * The tmctl read me(ENG).txt is not so clear, but it appears harmless if called
         * for non-GPIB ports. */
        int TmcDeviceClear(int DeviceID);
        
        /** Sends a message to a device */
        int TmcSend(int DeviceID, String Message) ; //char*

//        int TmcSendByLength( int DeviceID, char* msg, int len ) ;
//        int TmcSendSetup( int DeviceID ) ;
//        int TmcSendOnly( int DeviceID, char* msg, int len, int end ) ;
        
        /** Receives a message from a device */
        int TmcReceive(int DeviceID, ByteBuffer Buffer, int BufferLength, 
                IntByReference NrBytesRead ) ; //char*, int*

        /** Prepares the PC to receive a message from a device */
        int TmcReceiveSetup(int DeviceID) ;
        
        /** Receives a message (after preparation) from a device. */
        int TmcReceiveOnly(int DeviceID, ByteBuffer Buffer, int BufferLength,
                IntByReference NrBytesRead) ;// char*, int*
        
        /** Receives the header portion of the Block Data sent from the device, 
         * and returns the number of bytes after the header. This is the first 
         * command to be used before receiving Block Data. The number of bytes in 
         * the data after the header is returned as the length, so that	number 
         * plus 1 (for the terminator) is assigned to "TmcReceiveBlockData" and 
         * then the data is received. */
        int TmcReceiveBlockHeader(int DeviceID, IntByReference NrDataBytes);
        
        /** Receives the data portion of the Block Data sent from a device. Returns 
         * whether all of the number of bytes of data returned by "TmcReceiveBlockHeader" 
         * are finished being received.	The value is 1 at the end of reception, 
         * or 0 if reception is to continue. */
        int TmcReceiveBlockData(int DeviceID, ByteBuffer Buffer, int BufferLength,
                IntByReference NrDataBytes, IntByReference EndFlag ) ;//char*,int*,int*

        /** Returns whether the message from the device is finished. Can be used 
         * with the GPIB, USB, Ethernet, USBTMC, and VXI-11 interfaces. */
        int TmcCheckEnd( int DeviceID ) ;
        
//        int TmcSetCmd( int DeviceID, char* cmd ) ;
//        int TmcSetRen( int DeviceID, int flag ) ;
        
        /** Returns the number of the last error that occurred. */
        int TmcGetLastError(int DeviceID) ;
        
//        int TmcGetDetailLastError( int DeviceID ) ;
        
        /** Undocumented */
//        int TmcCheckError(int DeviceID, int Status, String Msg, String Error);//char*,char*
        
//        int TmcSetTerm( int DeviceID, int eos, int eot ) ;
//        int TmcSetEos( int DeviceID, unsigned char eos ) ;
        
        /** Sets the timeout time for communications (100-6553600 ms)*/
        int TmcSetTimeout(int DeviceID, int TimeOut ) ;
        
//        int TmcSetDma( int DeviceID, int flg ) ;
//        int TmcGetStatusByte( int DeviceID, unsigned char* sts ) ;
        
        /** Closes the connection to a device. */
        int TmcFinish( int DeviceID ) ;
        
//        int TmcSearchDevices(int wire, DEVICELIST* list, int max, int* num,char* option) ;
//        int TmcSearchDevicesEx(int wire, DEVICELISTEX* list, int max, int* num,char* option) ;
//        int TmcWaitSRQ(int DeviceID, char* stsbyte, int tout) ;
//        int TmcAbortWaitSRQ(int DeviceID) ;
//        int TmcSetCallback(int DeviceID,Hndlr func, ULONG p1, ULONG p2) ;
//        int TmcResetCallback(int DeviceID) ;
//        int TmcSendTestData(int DeviceID, char* msg, int len ) ;
//        int TmcReceiveTestData( int DeviceID, char* buff, int BufferLength, int* rlen ) ;
//        int TmcInitiateAbortBulkIn(int DeviceID, UCHAR tagNo) ;
//        int TmcInitiateAbortBulkOut(int DeviceID, UCHAR tagNo) ;
//        int TmcCheckAbortBulkInStatus(int DeviceID) ;
//        int TmcCheckAbortBulkOutStatus(int DeviceID) ;
//        int TmcEncodeSerialNumber(char* encode,size_t len,char* src) ;
//        int TmcDecodeSerialNumber(char* decode,size_t len,char* src) ;
//        int TmcGotoLocal( int DeviceID ) ;
//        int TmcLocalLockout(int DeviceID) ;
//        int TmcAbortPipe(int DeviceID,long pipeNo) ;
//        int TmcResetPipe(int DeviceID,long pipeNo) ;
//        int TmcWriteHeader(int DeviceID, int BufferLength) ;
//        int TmcReceiveWithoutWriteHeader(int DeviceID, char* buff, int BufferLength, int* rlen, int* end ) ;
//        int TmcGetTagNo(int DeviceID, UCHAR* tag) ;
//        int TmcSendByLength2( int DeviceID, char* msg, int msgSize, int len, CHAR eof) ;
//        int TmcDeviceChangeNotify(HWND hWnd, BOOL bStart) ;
//        int TmcCheckUSB(int DeviceID) ;
//        int TmcGetPipeNo(int DeviceID,int type,int* pipeNo) ;
//        int TmcCheckGUID(int lParam) ;
        
        /** This method is undocumented but it sounds like it would return the
         * error number defined in the constants after TmcInitialize */
        //int TmcGetInitializeError() ;   // ULONG


    }//</editor-fold>

    /**
     * Constructor. Initializes the Logger for this class and reserves memory
     * for <code>m_ReceiveBuffer</code>. It calls <code>LoadNative</code> to 
     * load the native library.<p>
     *
     * @throws IOException When loading the native library failed (bubbles up
     * from <code>LoadNative</code>).
     */
    // <editor-fold defaultstate="collapsed" desc="Constructor">
    protected TMCTL_Driver() throws IOException {

        // init Logger to inherit Logger level from Parent Logger
        m_Logger.setLevel(null);

        // instantiate iC_Properties
        m_iC_Properties = new iC_Properties();

        // allocate memory for the ReceiveBuffer
        if (m_ReceiveBuffer == null) {
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
        }

        // load the native library
        LoadNative();

    }//</editor-fold>


    /**
     * Loads the native library. Does nothing if the native library has already 
     * been loaded.
     *
     * @throws IOException when the native library could not be loaded.
     */
    // <editor-fold defaultstate="collapsed" desc="Load Native">
    private void LoadNative()
            throws IOException {

        // return if the native library has already been loaded
        if (m_TMCTL != null) {
            // log already loaded
            m_Logger.log(Level.FINE, "JNA: The TMCTL library was already loaded.\n");

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


        // check for non-Windows platforms
        if ( !Platform.isWindows() ) {
            
            String str = "I am sorry to inform you that Yokogawa's TMCTL library\n"
                    + "is only offered for Windows operating systems...\n";
            
            throw new IOException(str);
        }
        
        // define the library name
        String LibName = "tmctl.dll";
        
        
        
        /////////////
        // path issue
        
        /* When starting iC from within Netbeans, the tmcctl.dll is not found,
         * so add the path to the additional search paths in JNA.
         * Because it's nice to store the .dlls in dist/lib, also add the /lib
         * folder to the search path so that they are found when started from
         * the command line (or by double-clicking).
         */
        
        String FileSeparator = System.getProperty("file.separator");
        
        // get Current Working Directory (the directory from which Icontrol.jar was started)
        String WorkingDirectory = System.getProperty("user.dir");
              
        // append /lib
        String Path = WorkingDirectory + FileSeparator + "lib";
        
        // add as additional search path
        NativeLibrary.addSearchPath(LibName, Path);
        
        // log path
        m_Logger.log(Level.CONFIG, "added to JNAs search path for {0}: {1}\n", 
                new Object[]{LibName, Path});
        
        
        // when started from within Netbeans, /dist/lib must be appended
        Path = WorkingDirectory + FileSeparator + "dist" + FileSeparator + "lib";
        
        // add as additional search path
        NativeLibrary.addSearchPath(LibName, Path);
        
        // log path
        m_Logger.log(Level.CONFIG, "added to JNAs search path for {0}: {1}\n", 
                new Object[]{LibName, Path});
        
        
        try {
            // load the native library
            m_TMCTL = (TMCTL) Native.loadLibrary(LibName, TMCTL.class);

            // log success
            m_Logger.log(Level.FINE, "JNA: Loaded library {0}\n", LibName);

        } catch (UnsatisfiedLinkError ex) {
            String str = "Could not load Yokogawa's TMCTL library (" + LibName + ").\n"
                + "Please ensure that the drivers are installed properly.\n"
                + "Note that, at least for the DL9000 Series oscilloscope a USB driver\n"
                + "needs to be installed outside of Instrument Control.\n"
                + "Java Native Access' response:\n"
                + ex.getMessage();

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
    }//</editor-fold>


    /**
     * Opens the connection to the Instrument at the specified address using
     * the specified communication port.<p>
     *
     * @param Wire Chooses the communication protocol (USB, Ethernet, ...). Valid 
     * argument is USBTMC(DL9000). Further communication ports are easy to
     * implement.
     * 
     * @param Address The address of the Instrument. The content depends on the
     * value of <code>Wire</code>. For the DL9000 Series oscilloscope, the Serial
     * Number must be specified.
     *
     * @throws IOException When 1) an invalid <code>Wire</code> parameter is given,
     * or 2) the Instrument could not be opened by <code>TmcInitialize</code>.
     *
     */
    // <editor-fold defaultstate="collapsed" desc="Open">
    protected void Open(String Wire, String Address)
           throws IOException {
        
        int Wire_int;
        
        // find the proper Wire CONSTANT
        if (Wire.equalsIgnoreCase("USBTMC(DL9000)")) {
            Wire_int = TM_CTL_USBTMC;
        
        } // add support for more communication ports here
        else {            
            String str = "The chosen communication port (" + Wire + ")\n"
                    + "is not supported. Please choose USBTMC(DL9000) or"
                    + "contact the developer to implement the port you need.\n";
            
            throw new IOException(str);
        }
        
        
        
        // open communication channel with the Instrument
        IntByReference DeviceID = new IntByReference();
        int Status = m_TMCTL.TmcInitialize(Wire_int, Address, DeviceID);
        
        // error check
        if (Status != 0) {
            
            // get details on why opening failed
            // the example program uses TmcGetLastError and the documentation
            // says this method can be used, although there is an undocumented
            // method TmcGetInitializeError().
            
            String str = "An error occured when trying to open the Instrument "
                    + Address + " using " + Wire + ".\n"
                    + "Please ensure the Instrument is connected properly, that the\n"
                    + "Address specifier (e.g. Serial Number) is correct, and select\n"
                    + "USB in System/Remote Control on the oscilloscope.\n";
            
            checkErrorTMCTL(str, false);
        }
        
        // store Device ID
        m_DeviceID = DeviceID.getValue();
        
        // log Device ID
        m_Logger.log(Level.CONFIG, "Opened {0} ({1}). DeviceID={2}\n", 
                new Object[]{Wire, Address, m_DeviceID});
        
        
        // After opening, these default values are effective:
        // Terminator: LF (LF or EOI for GPIB), Timeout: 30 seconds

        
        // send Selected Device Clear to the Instrument
        // for non-GPIB communication ports, this issues a standard USBTMC InitiateClear,
        // whatever this means
        Status = m_TMCTL.TmcDeviceClear(m_DeviceID);

        // check for errors
        if (Status != 0) {
            String str = "Could not clear the Instrument with address specifier "
                    + Address + ".\n";
            checkErrorTMCTL(str, true);
        }
            
        // set Timout Value
        Status = m_TMCTL.TmcSetTimeout(m_DeviceID, 3000);
        
        // check for errors
        if (Status != 0) {
            String str = "Could not set the TimeOut value the Instrument with\n"
                    + "address specifier " + Address + ".\n";
            checkErrorTMCTL(str, false);
        }

    }//</editor-fold>



    /**
     * Sends the given String via the TMCTL library to the Instrument specified in
     * <code>Open</code> and returns the number of bytes actually sent. If
     * <code>Msg</code> is empty, the method returns immediately.<p>
     *
     * @param Msg Data to be sent
     * @throws IOException When the transmission caused an error
     */
    // <editor-fold defaultstate="collapsed" desc="Send">
    protected void Send(String Msg)
              throws IOException {
        
        // do nothing if Msg is empty. In GPIB_NI this was required, and is added
        // here for safety reasons, but it might not be necessary
        if (Msg.isEmpty()) {
            return;
        }

        // send it
        int Status = m_TMCTL.TmcSend(m_DeviceID, Msg);
        
        // check for errors
        if (Status != 0) {
            String str = "A TMCTL error occurred during sending the last command.\n";
            checkErrorTMCTL(str, false);
        }

    }//</editor-fold>



    /**
     * Receives data from the Instrument specified in <code>Open</code>.
     * The response of the Instrument is read in chunks of size defined by 
     * <code>RECEIVE_BUFFER_SIZE</code>, but all available data is received. 
     * The received data stream is interpreted as a UTF-8 character set.<p>
     * 
     * Note that for oscilloscopes to receive waveform data, the specialized
     * method <code>ReceiveBlockData</code> must be called.
     *
     * @param Trim If <code>true</code>, newline (\n) and carriage return (\r)
     * characters are removed from the end of the returned String.
     * @return The data bytes sent by the Instrument wrapped in a String.
     * @throws IOException When the transmission caused an error
     */
    // <editor-fold defaultstate="collapsed" desc="Receive">
    protected String Receive(boolean Trim)
           throws IOException {
        
        // local variables
        String ret = "";
        IntByReference NrBytesReceived = new IntByReference();
        
        
        // tell Instrument to prepare to receive data
        int Status = m_TMCTL.TmcReceiveSetup(m_DeviceID);
        
        // check for errors
        if (Status != 0) {
            String str = "Could not set up receiving data.\n";
            checkErrorTMCTL(str, false);
        }
        
        
        // Receive the data as long as there is more data available
        int ReadMore = 1;
        while ( ReadMore == 1 ) {
            
            // receive data
            Status = m_TMCTL.TmcReceiveOnly(m_DeviceID, m_ReceiveBuffer, 
                                    m_ReceiveBuffer.limit(), NrBytesReceived);
            
            // check for errors
            if (Status != 0) {
                String str = "An error occurred during receiving data.\n";
                checkErrorTMCTL(str, false);
            }
            
            // convert to a String of the correct size using UTF-8
            ret += new String(m_ReceiveBuffer.array(), 0, 
                    NrBytesReceived.getValue(), Charset.forName("UTF-8"));
            
            
            // more data available to read?
            ReadMore = m_TMCTL.TmcCheckEnd(m_DeviceID);
            
            
        }

        // remove trailing newline & carriage return
        if (Trim)
            ret = ret.replaceFirst("[\\n[\\r]]+$", "");

        return ret;
    }//</editor-fold>


    /**
     * Receives Block-Data from the Instrument. Used for e.g. the DL9000 oscilloscope
     * which sends the waveform data not via the regular <code>TmcReceive</code> method
     * but requires block-transfer of the data. All the data available is read 
     * from the instrument, but it is read in chunks of size<code>RECEIVE_BUFFER_SIZE</code>.
     * 
     * @return A <code>ByteBuffer</code> with the received data.
     * @throws IOException When communication via the TMCTL library failed
     */
    // <editor-fold defaultstate="collapsed" desc="ReceiveBlockData">
    public ByteBuffer ReceiveBlockData() 
              throws IOException {
        
        // local variables
        IntByReference NrDataBytes = new IntByReference();
        IntByReference NrDataBytesReceived = new IntByReference();
        IntByReference EndFlag = new IntByReference();
        
       
        // get Waveform Header
        int Status = m_TMCTL.TmcReceiveBlockHeader(m_DeviceID, NrDataBytes);
        
        // check for errors
        if (Status != 0) {
            String str = "Error receiving the Block Header.\n";
            checkErrorTMCTL(str, false);
        }
        
        // make an appropriate buffer
        ByteBuffer ReturnBuffer = ByteBuffer.allocate(NrDataBytes.getValue() + 1);
        
       
        // read all data
        boolean ReadMore = true;
        while (ReadMore) {
            // get Waveform Data (as Bytes)
            Status = m_TMCTL.TmcReceiveBlockData(m_DeviceID, m_ReceiveBuffer, m_ReceiveBuffer.limit(),
                    NrDataBytesReceived, EndFlag);

            // check for errors
            if (Status != 0) {
                String str = "Error receiving the Block Data.\n";
                checkErrorTMCTL(str, false);
            }
            
            // add received data to the return buffer
            ReturnBuffer.put(m_ReceiveBuffer.array(), 0, NrDataBytesReceived.getValue());
            
            // check Endflag to decide if all the data was read
            if (EndFlag.getValue() == 1) {
                ReadMore = false;
            }
        }
        
        // return result
        return ReturnBuffer;        
    }// </editor-fold>
    


    /**
     * Checks the Status of the last TMCTL transaction (including TmcInitialize)
     * and throws an Exception if an error occurred. If an error occurred, this
     * event is also Logged in the GPIB-Logger.
     *
     * @param Msg This String is appended to the message thrown as an
     * Exception when an error occurred.
     * @param CloseConnection If <code>true</code>, the connection to the
     * GPIB board is closed when an error occurred.
     * @throws IOException when an error occurred during the last TMCTL operation
     */
    // <editor-fold defaultstate="collapsed" desc="checkErrorTMCTL">
    private void checkErrorTMCTL(String Msg, boolean CloseConnection)
            throws IOException {

        // get last error
        int ErrorNr = m_TMCTL.TmcGetLastError(m_DeviceID);
        
        // did an error occur?
        if (ErrorNr != TMCTL_NO_ERROR) {
            
            // yes, so append Error Number, DeviceID and Error Message
            String str = Msg + "Error Number = " + ErrorNr 
                    + " (DeviceID = " + m_DeviceID + "): ";

            switch (ErrorNr) {
                case TMCTL_TIMEOUT:
                    str += "Time Out\n";
                    break;
                case TMCTL_NO_DEVICE:
                    str += "Device not found\n";
                    break;
                case TMCTL_FAIL_OPEN:
                    str += "Opening connection to the device failed.\n";
                    break;
                case TMCTL_NOT_OPEN:
                    str += "Device Not Open\n";
                    break;
                case TMCTL_DEVICE_ALREADY_OPEN:
                    str += "Device Already Open\n";
                    break;
                case TMCTL_NOT_CONTROL:
                    str += "Controller Not Found\n";
                    break;
                case TMCTL_ILLEGAL_PARAMETER:
                    str += "Parameter is illegal\n";
                    break;
                case TMCTL_SEND_ERROR:
                    str += "Send Error\n";
                    break;
                case TMCTL_RECV_ERROR:
                    str += "Receive Error\n";
                    break;
                case TMCTL_NOT_BLOCK:
                    str += "Data is not Block Data\n";
                    break;
                case TMCTL_SYSTEM_ERROR:
                    str += "System Error\n";
                    break;
                case TMCTL_ILLEGAL_ID:
                    str += "Device ID is Illegal\n";
                    break;
                case TMCTL_NOT_SUPPORTED:
                    str += "This feature is not supportred\n";
                    break;
                case TMCTL_INSUFFICIENT_BUFFER:
                    str += "unsufficient buffer size\n";
                    break;
                default:
                    str += "The returned error number is not recognized.\n";
            }
            
            // log event
            m_Logger.severe(str);
            m_Comm_Logger.severe(str);
            
            // close connection to the TMCTL controller
            if (CloseConnection == true) {
                CloseInstrument();
            }

            throw new IOException(str);
        }
    }//</editor-fold>



    /**
     * Closes the connection to the Instrument specified in <code>Open</code>.
     * This method is called from <code>Dispatcher.run</code> after the Script
     * has been processed and from <code>checkErrorTMCTL</code>.
     *
     * @throws IOException When the closing caused a TMCTL error
     */
    // <editor-fold defaultstate="collapsed" desc="CloseInstrument">
    protected void CloseInstrument() 
              throws IOException {

        // log
        m_Logger.log(Level.FINE, "Closing connection to the Instrument with Device ID {0}.\n", Integer.toString(m_DeviceID));

        
        // switch Instrument into local mode
        // it's undocumented, but TmcGotoLocal should do the trick (not required
        // for USB communication
        
//        // check for errors
//        String str = "Could not enable local mode when closing the connection to the instrument\n";
//        checkErrorTMCTL(str, false);
        
        
        
        // Close the connection to the Instrument
        int Status = m_TMCTL.TmcFinish(m_DeviceID);
        
        // check for errors
        if (Status != 0) {
            String str = "Could not close the connection to the Instrument.\n";
            
            // mus be false or else a deadlock might occur as checkErrorTMCTL
            // calls CloseInstrument if true
            checkErrorTMCTL(str, false);
        }
        
        // remember that Instrument was closed
        m_DeviceID = -1;

    }//</editor-fold>

}
