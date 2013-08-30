// TODO 1* make EOT_MODE work as expected in Send() (488.1 and 488.2 function calls)

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
import com.sun.jna.Pointer;
import com.sun.jna.ptr.ByteByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.ShortByReference;
import icontrol.IcontrolView;
import icontrol.iC_Properties;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class provides access to the native code to access the National
 * Instruments GPIB cards and also Agilent's GPIB cards when this special
 * emulate NI is enabled.<p>
 *
 * It uses Java Native Access (JNA) package to access the driver (for instance
 * gpib-32.dll on Windows operating systems). Due to a possible bug in the native
 * code, it is possible to select to use 488.1 or 488.2 function calls to send and
 * receive with the constant <code>USE_4882</code> (488.2 function calls are
 * still used in <code>ClearDevice</code> and <code>ClearAllDevices</code>).<p>
 *
 * All GPIB calls are addressed to GPIB Board defined in iC.properties. The
 * default value is GPIB0 at address 0. Before being able to send and receive
 * the method <code>Open</code> must be called, or else the behavior is
 * undefined.<p>
 * 
 * This class is Deprecated, use <code>GPIB_NI64</code> instead. The new version
 * uses NI4882.dll instead of gpib-32.dll to access NI hardware on Windows 
 * machines. NI4882.dll is nominally backwards compatible with 32-bit machines,
 * hence, this class will not be further developed and should not be used.
 * It is kept as a backup and can be used by setting the GPIB_NI.Use64BitDriver
 * switch in the iC.properties.<p>
 *
 * <h3>Usage:</h3>
 * <ul>
 *  <li>Instantiate the class. This calls <code>LoadNative</code> to establish
 *      access to the native code (for instance gpib-32.dll on Windows OS).
 *  <li>[Optional]: Call <code>ClearAllDevices</code> to send the Selected Device
 *      Clear (SDC) GPIB command to all Listeners on the bus. Upon opening an
 *      Instrument at a specific GPIB address, the SDC command is sent to
 *      this Instrument regardless. To use this method, change it's visibility
 *      from private to protected.
 *  <li>Open an Instrument at a specific GPIB address using <code>Open</code>,
 *      which calls <code>ClearDevice</code>. It assigns a proper value to
 *      <code>m_UnitDescriptor</code> that can be used in 488.1 and 488.2
 *      function calls.
 *  <li>To send/receive data use <code>Send</code> and <code>Receive</code>.
 *  <li>To close the connection to an Instrument call <code>CloseInstrument</code>.
 *  <li>Once done with all GPIB communication, close the connection to the
 *      GPIB controller with <code>CloseController</code>.
 *  <li>Each GPIB communication checks the status of the last GPIB call using
 *      <code>checkErrorGPIB</code> which throws an <code>IOException</code>
 *      if an error occurred.
 *  <li>To receive the Status Byte - especially of older equipment that is not
 *      488.2 compliant but implements 488 only, hence, no *STB? command -
 *      <code>ReadStatusByte</code> can be used.
 *  <li><code>getStatus</code> is a convenience method that returns a nicely
 *      formated String of the error that occurred during the last GPIB
 *      transaction or an empty String if no error occurred.
 *  <li><code>setTimeOut</code> sets the TimeOut value for (only useful when
 *      488.2 function calls are being used) the GPIB controller.
 * </ul>
 *
 * @author KPP (Kurt Pernstich: pernstich@alumni.ethz.ch)
 * @version 0.9
 *
 */
@Deprecated
public class GPIB_NI extends GPIB_Driver {

    ///////////////////
    // member variables

    /** Handle to access the native code */
    private static gpib32    m_gpib32 = null;

    /** The Logger for this class */
    private static final Logger m_Logger = Logger.getLogger("iC.Instruments.GPIB_NI");

    /** Receive Buffer */
    private static ByteBuffer m_ReceiveBuffer = null;

    /** 
     * Unit Descriptor for NI 488 calls. Note that <code>m_UnitDescriptor</code>
     * is actually a 16-bit number, so it can safely be cast to a (short).
     */
    private int m_UnitDescriptor;

    /**
     * The 488.2 function calls are corrupted as shown in this example with
     * an Agilent 3606A Multimeter (GPIB 6) and a Lakeshore 340 Temperature
     * Controller (GPIB 4):
     *
     * Using the 488.2 function calls in gpib-32.dll (called from a JAVA program using Java Native Access JNA)
     * m_gpib32.Send(BOARD_NR, MakeAdr(GPIB_Adr, 0), BBuffer, Msg.length(), EOT_MODE);
     * m_gpib32.Receive(BOARD_NR, MakeAdr(GPIB_Adr, 0), m_ReceiveBuffer, m_ReceiveBuffer.limit(), RECEIVE_TERMINATION);
     *
     * causes the following output recorded with NI Spy
     * Send(0, 0x0006, "*IDN?", 5 (0x5), NULLend (0x00)) [Although I specified NLend as EOT_MODE, NULLend is used]
     * Receive(0, 0x0006, "", 150000 (0x249F0), 0x0) [iberr=6 because the newline is missing and the instrument is not ready to send when addressed to talk]
     *
     * The software handles this case and proceeds with a query of the temperature (on the instrument that did not cause an error)
     * Send(0, 0x0004, "KRDG? A", 7 (0x7), NULLend (0x00))
     * Receive(0, 0x0004, "LSCI,MODEL340...", 150000 (0x249F0), 0x0) [interestingly, the IDN is returned instead of the temperature !!]
     *
     * When the same steps are repeated with 488.1 function calls
     * m_gpib32.ibwrt(m_UnitDescriptor, BBuffer, Msg.length());
     * m_gpib32.ibrd(m_UnitDescriptor, m_ReceiveBuffer, m_ReceiveBuffer.limit());
     *
     * NI Spy records:
     * ibwrt(UD3, "*IDN?", 5 (0x5))
     * ibrd(UD3, "", 150000 (0x249F0)) [iberr=6 as expected]
     *
     * ibwrt(UD2, "KRDG? A", 7 (0x7))
     * ibrd(UD2, "+227.746E+0..", 150000 (0x249F0)) [now the actual temperature is returned as desired]
     *
     * I would expect that calls into the dll from JAVA might be the cause, but
     * since NI Spy is recording the GPIB traffic, I think it might be a bug in the
     * gpib-32.dll. Any suggestions/thoughts are highly appreciated although I
     * can use the 488.1 function calls to solve the problem, I would be interested
     * in understanding what went wrong.
     */
    private final boolean USE_4882 = false;

    /**
     * Grants access to global properties of the iC application in a resource
     * bundle file 'iC.properties'
     */
    protected static iC_Properties m_iC_Properties;

    /** GPIB Address of the Instrument */
    private int m_GPIB_Adr;
    
    
    //////////////////////////////////////////////
    // Global variables defined in the native code
    // these are common to all derived classes

    /** Status of the last GPIB call */
    private static IntByReference m_ibsta = new IntByReference();
    
    /** Error code of the last GPIB call */
    private static IntByReference m_iberr = new IntByReference();

    /** Counter variable associated with the last GPIB call (int) */
    private static IntByReference m_ibcnt = new IntByReference();

    /** Counter variable associated with the last GPIB call (long) */
    private static LongByReference m_ibcntl = new LongByReference();


    /////////////////////////////////
    // gpib-32.dll specific CONSTANTS
    // VERY IMPORTANT REMARK:
    // Because there is no unsigned short in Java, the constants are defined
    // as int's. Casting a short to an int keeps to lower 16 bits as expected, but
    // the int-value is interpreted wrong, for instance as -1 instead of 0xFFFF.
    // <editor-fold defaultstate="collapsed" desc="gpib-32.dll CONSTANTS">
    private final int UNL=  0x3f;  /* GPIB unlisten command                 */
    private final int UNT=  0x5f;  /* GPIB untalk command                   */
    private final int GTL=  0x01;  /* GPIB go to local                      */
    private final int SDC=  0x04;  /* GPIB selected device clear            */
    private final int PPC=  0x05;  /* GPIB parallel poll configure          */
    private final int GET=  0x08;  /* GPIB group execute trigger            */
    private final int TCT=  0x09;  /* GPIB take control                     */
    private final int LLO=  0x11;  /* GPIB local lock out                   */
    private final int DCL=  0x14;  /* GPIB device clear                     */
    private final int PPU=  0x15;  /* GPIB parallel poll unconfigure        */
    private final int SPE=  0x18;  /* GPIB serial poll enable               */
    private final int SPD=  0x19;  /* GPIB serial poll disable              */
    private final int PPE=  0x60;  /* GPIB parallel poll enable             */
    private final int PPD=  0x70;  /* GPIB parallel poll disable            */

    /* GPIB status bit vector :            */
    /* global variable ibsta and wait mask */
    private final int ERR=     (1<<15); /* Error detected                   */
    private final int TIMO=    (1<<14); /* Timeout                          */
    private final int END=     (1<<13); /* EOI or EOS detected              */
    private final int SRQI=    (1<<12); /* SRQ detected by CIC              */
    private final int RQS=     (1<<11); /* Device needs service             */
    private final int CMPL=    (1<<8);  /* I/O completed                    */
    private final int LOK=     (1<<7);  /* Local lockout state              */
    private final int REM=     (1<<6);  /* Remote state                     */
    private final int CIC=     (1<<5);  /* Controller-in-Charge             */
    private final int ATN=     (1<<4);  /* Attention asserted               */
    private final int TACS=    (1<<3);  /* Talker active                    */
    private final int LACS=    (1<<2);  /* Listener active                  */
    private final int DTAS=    (1<<1);  /* Device trigger state             */
    private final int DCAS=    (1<<0);  /* Device clear state               */

    /* Error messages returned in global variable iberr */
    private final int EDVR=  0;  /* System error                            */
    private final int ECIC=  1;  /* Function requires GPIB board to be CIC  */
    private final int ENOL=  2;  /* Write function detected no Listeners    */
    private final int EADR=  3;  /* Interface board not addressed correctly */
    private final int EARG=  4;  /* Invalid argument to function call       */
    private final int ESAC=  5;  /* Function requires GPIB board to be SAC  */
    private final int EABO=  6;  /* I/O operation aborted                   */
    private final int ENEB=  7;  /* Non-existent interface board            */
    private final int EDMA=  8;  /* Error performing DMA                    */
    private final int EOIP= 10;  /* I/O operation started before previous   */
                                 /* operation completed                     */
    private final int ECAP= 11;  /* No capability for intended operation    */
    private final int EFSO= 12;  /* File system operation error             */
    private final int EBUS= 14;  /* Command error during device call        */
    private final int ESTB= 15;  /* Serial poll status byte lost            */
    private final int ESRQ= 16;  /* SRQ remains asserted                    */
    private final int ETAB= 20;  /* The return buffer is full.              */
    private final int ELCK= 21;  /* Address or board is locked.             */
    private final int EARM= 22;  /* The ibnotify Callback failed to rearm   */
    private final int EHDL= 23;  /* The input handle is invalid             */
    private final int EWIP= 26;  /* Wait already in progress on input ud    */
    private final int ERST= 27;  /* The event notification was cancelled    */
                                 /* due to a reset of the interface         */
    private final int EPWR= 28;  /* The system or board has lost power or   */
                                 /* gone to standby                         */

    /* Warning messages returned in global variable iberr  */
    private final int WCFG= 24;  /* Configuration warning                   */
    private final int ECFG= WCFG;

    /* EOS mode bits  */
    private final int BIN=  (1<<12); /* Eight bit compare                   */
    private final int XEOS= (1<<11); /* Send END with EOS byte              */
    private final int REOS= (1<<10); /* Terminate read on EOS               */

    /* Timeout values and meanings  */
    private final int TNONE=    0;   /* Infinite timeout (disabled)         */
    private final int T10us=    1;   /* Timeout of 10 us (ideal)            */
    private final int T30us=    2;   /* Timeout of 30 us (ideal)            */
    private final int T100us=   3;   /* Timeout of 100 us (ideal)           */
    private final int T300us=   4;   /* Timeout of 300 us (ideal)           */
    private final int T1ms=     5;   /* Timeout of 1 ms (ideal)             */
    private final int T3ms=     6;   /* Timeout of 3 ms (ideal)             */
    private final int T10ms=    7;   /* Timeout of 10 ms (ideal)            */
    private final int T30ms=    8;   /* Timeout of 30 ms (ideal)            */
    private final int T100ms=   9;   /* Timeout of 100 ms (ideal)           */
    private final int T300ms=  10;   /* Timeout of 300 ms (ideal)           */
    private final int T1s=     11;   /* Timeout of 1 s (ideal)              */
    private final int T3s=     12;   /* Timeout of 3 s (ideal)              */
    private final int T10s=    13;   /* Timeout of 10 s (ideal)             */
    private final int T30s=    14;   /* Timeout of 30 s (ideal)             */
    private final int T100s=   15;   /* Timeout of 100 s (ideal)            */
    private final int T300s=   16;   /* Timeout of 300 s (ideal)            */
    private final int T1000s=  17;   /* Timeout of 1000 s (ideal)           */

    /*  IBLN Constants                                          */
    private final int NO_SAD=   0;
    private final int ALL_SAD= -1;

    /*  The following constants are used for the second parameter of the
     *  ibconfig function.  They are the "option" selection codes. */
    private final int  IbcPAD=        0x0001;      /* Primary Address                      */
    private final int  IbcSAD=        0x0002;      /* Secondary Address                    */
    private final int  IbcTMO=        0x0003;      /* Timeout Value                        */
    private final int  IbcEOT=        0x0004;      /* Send EOI with last data byte?        */
    private final int  IbcPPC=        0x0005;      /* Parallel Poll Configure              */
    private final int  IbcREADDR=     0x0006;      /* Repeat Addressing                    */
    private final int  IbcAUTOPOLL=   0x0007;      /* Disable Auto Serial Polling          */
    private final int  IbcCICPROT=    0x0008;      /* Use the CIC Protocol?                */
    private final int  IbcIRQ=        0x0009;      /* Use PIO for I/O                      */
    private final int  IbcSC=         0x000A;      /* Board is System Controller?          */
    private final int  IbcSRE=        0x000B;      /* Assert SRE on device calls?          */
    private final int  IbcEOSrd=      0x000C;      /* Terminate reads on EOS               */
    private final int  IbcEOSwrt=     0x000D;      /* Send EOI with EOS character          */
    private final int  IbcEOScmp=     0x000E;      /* Use 7 or 8-bit EOS compare           */
    private final int  IbcEOSchar=    0x000F;      /* The EOS character.                   */
    private final int  IbcPP2=        0x0010;      /* Use Parallel Poll Mode 2.            */
    private final int  IbcTIMING=     0x0011;      /* NORMAL, HIGH, or VERY_HIGH timing.   */
    private final int  IbcDMA=        0x0012;      /* Use DMA for I/O                      */
    private final int  IbcReadAdjust= 0x0013;      /* Swap bytes during an ibrd.           */
    private final int  IbcWriteAdjust= 0x014;      /* Swap bytes during an ibwrt.          */
    private final int  IbcSendLLO=    0x0017;      /* Enable/disable the sending of LLO.      */
    private final int  IbcSPollTime=  0x0018;      /* Set the timeout value for serial polls. */
    private final int  IbcPPollTime=  0x0019;     /* Set the parallel poll length period.    */
    private final int  IbcEndBitIsNormal= 0x001A;  /* Remove EOS from END bit of IBSTA.       */
    private final int  IbcUnAddr=         0x001B;  /* Enable/disable device unaddressing.     */
    private final int  IbcSignalNumber=   0x001C;  /* Set UNIX signal number - unsupported */
    private final int  IbcBlockIfLocked=  0x001D;  /* Enable/disable blocking for locked boards/devices */
    private final int  IbcHSCableLength=  0x001F;  /* Length of cable specified for high speed timing.*/
    private final int  IbcIst=        0x0020;      /* Set the IST bit.                     */
    private final int  IbcRsv=        0x0021;      /* Set the RSV byte.                    */
    private final int  IbcLON=        0x0022;      /* Enter listen only mode               */

    /* Constants that can be used (in addition to the ibconfig constants)
     * when calling the ibask() function. */
    private final int  IbaPAD=            IbcPAD;
    private final int  IbaSAD=            IbcSAD;
    private final int  IbaTMO=            IbcTMO;
    private final int  IbaEOT=            IbcEOT;
    private final int  IbaPPC=            IbcPPC;
    private final int  IbaREADDR=         IbcREADDR;
    private final int  IbaAUTOPOLL=       IbcAUTOPOLL;
    private final int  IbaCICPROT=        IbcCICPROT;
    private final int  IbaIRQ=            IbcIRQ;
    private final int  IbaSC=             IbcSC;
    private final int  IbaSRE=            IbcSRE;
    private final int  IbaEOSrd=          IbcEOSrd;
    private final int  IbaEOSwrt=         IbcEOSwrt;
    private final int  IbaEOScmp=         IbcEOScmp;
    private final int  IbaEOSchar=        IbcEOSchar;
    private final int  IbaPP2=            IbcPP2;
    private final int  IbaTIMING=         IbcTIMING;
    private final int  IbaDMA=            IbcDMA;
    private final int  IbaReadAdjust=     IbcReadAdjust;
    private final int  IbaWriteAdjust=    IbcWriteAdjust;
    private final int  IbaSendLLO=        IbcSendLLO;
    private final int  IbaSPollTime=      IbcSPollTime;
    private final int  IbaPPollTime=      IbcPPollTime;
    private final int  IbaEndBitIsNormal= IbcEndBitIsNormal;
    private final int  IbaUnAddr=         IbcUnAddr;
    private final int  IbaSignalNumber=   IbcSignalNumber;
    private final int  IbaBlockIfLocked=  IbcBlockIfLocked;
    private final int  IbaHSCableLength=  IbcHSCableLength;
    private final int  IbaIst=            IbcIst;
    private final int  IbaRsv=            IbcRsv;
    private final int  IbaLON=            IbcLON;
    private final int  IbaSerialNumber=   0x0023;

    private final int  IbaBNA=            0x0200;   /* A device's access board. */


    /* Values used by the Send 488.2 command. */
    private final int  NULLend= 0x00;  /* Do nothing at the end of a transfer.*/
    private final int  NLend=   0x01;  /* Send NL with EOI after a transfer.  */
    private final int  DABend=  0x02;  /* Send EOI with the last DAB.         */

    /* Value used by the 488.2 Receive command. */
    private final int  STOPend=     0x0100;

    /* This value is used to terminate an address list.  It should be
     * assigned to the last entry. */
    private final int NOADDR=    0xFFFF;//Short.parseShort("1111111111111111", 2);


    /* iblines constants */
    private final int  ValidEOI=   0x0080;
    private final int  ValidATN=   0x0040;
    private final int  ValidSRQ=   0x0020;
    private final int  ValidREN=   0x0010;
    private final int  ValidIFC=   0x0008;
    private final int  ValidNRFD=  0x0004;
    private final int  ValidNDAC=  0x0002;
    private final int  ValidDAV=   0x0001;
    private final int  BusEOI=     0x8000;//Short.parseShort("1000000000000000", 2);
    private final int  BusATN=     0x4000;
    private final int  BusSRQ=     0x2000;
    private final int  BusREN=     0x1000;
    private final int  BusIFC=     0x0800;
    private final int  BusNRFD=    0x0400;
    private final int  BusNDAC=    0x0200;
    private final int  BusDAV=     0x0100;

    /** A user friendly description of the error code */
    private final String[] ErrorMnemonic = {
        "EDVR System error",
        "ECIC Function requires GPIB interface to be CIC",
        "ENOL No Listeners on the GPIB",
        "EADR GPIB interface not addressed correctly",
        "EARG Invalid argument to function call",
        "ESAC GPIB interface not System Controller as required",
        "EABO I/O operation aborted (timeout)",
        "ENEB Nonexistent GPIB interface",
        "EDMA DMA error", //8
        "", "EOIP Asynchronous I/O in progress",
        "ECAP No capability for operation",
        "EFSO File system error",//12
        "", "EBUS GPIB bus error/Command error during device call",
        "ESTB Serial poll status byte lost",//15
        "ESRQ SRQ remains asserted",
        "", "", "", "ETAB Table problem/The return buffer is full",//20
        "ELCK GPIB interface is locked and cannot be accessed", //21
        "EARM ibnotify callback failed to rearm",
        "EHDL Input handle is invalid", //23
        "", "", "EWIP Wait in progress on specified input handle",
        "ERST The event notification was cancelled due to a reset of the interface",
        "EPWR The system or board has lost power or gone to standby"};
    //</editor-fold>



    ///////////////////////////
    // class specific constants

    /** Defines the board number where all commands are addressed to */
    private final int BOARD_NR = (new iC_Properties()).getInt("GPIB_NI.BoardNr", 0);

    /** Behavior after sending data (end of transmission).
     * The last byte is sent with the EOI line asserted if eotmode is DABend.
     * The last byte is sent without the EOI line asserted if eotmode
     * is NULLend. If eotmode is NLend, a new line character ('\n') is sent with
     * the EOI line asserted after the last byte of buffer.<p>
     *
     * NOTE: While this driver class passes the proper value for EOT_MODE to
     * the native code, NI Spy reports an EOT mode of NULLend for the transmission
     * irrespective of the chosen EOT mode. That no newline character is added
     * by the native code although NLend was specified was confirmed with
     * an Agilent 3606A Multimeter which requires a newline character at the end.
     * For now, the newline character is manually added in <code>Send</code> if
     * NLend is chosen.
     */
    private final int EOT_MODE = (new iC_Properties()).getInt("GPIB_NI.EOT_Mode", NLend);

    /** size of the Receive buffer */
    private final int RECEIVE_BUFFER_SIZE = (new iC_Properties()).getInt("GPIB_NI.ReceiveBufferSize", 150000);

    /** Termination condition for the Receive operation.
     * If the termination condition is STOPend, the read is stopped when a byte is
     * received with the EOI line asserted. Otherwise, the read is stopped when an
     * 8-bit EOS character is detected. 
     * NOTE: The proper functioning of the Receive Termination has not been
     * thoroughly tested; please use with caution.
     */
    private final int RECEIVE_TERMINATION = (new iC_Properties()).getInt("GPIB_NI.ReceiveTermination", 0);

    
    /** Character encoding used to convert between String and byte[] */
    private final String CHARACTER_ENCODING = (new iC_Properties()).getString("iC.CharacterEncoding", "ISO-8859-1");
    
    
    /**
     * Defines the interface required by JNA to invoke the native library.<p>
     * This interface defines all function calls 'into' the native
     * code (.dll / .dylib). It is the standard, stable way of mapping, which
     * supports extensive customization and mapping of Java to native types.<p>
     *
     * Please note that all function declaration have been imported from ni488.h,
     * but only the uncommented method declarations have been tested. To use
     * a native function that is currently commented, uncomment it, ensure the
     * data type conversion is correct, and test it.
     */
    // <editor-fold defaultstate="collapsed" desc="gpib32 Interface Declarations">
    private interface gpib32 extends Library {
        
        /* Employed data type conversions:
         *
         * native          JNA
         * --------------------------------
         * LPCSTR           String
         * LPWSTR           WString
         * PINT             IntByReference
         * PVOID            ByteBuffer
         * PCHAR            ByteByReference
         * PSHORT           ShortByReference, might also be short[] -- not sure
         * void*            Pointer
         * Addr4882_t       short (must be 16 bits)
         * Addr4882_t*      short[]
         */

   
        //////////////////////////////////////////////
        // Function declaration (adapted from ni488.h)


        /********************************/
        /*  NI-488 Function Prototypes  */
        // for the 488 calls, the ud needs to be aquired from ibdev
//        int ibfindA   (String udname);//LPCSTR udname
//        int ibbnaA    (int ud, String udname);//LPCSTR udname
//        int ibrdfA    (int ud, String filename);//LPCSTR udname
//        int ibwrtfA   (int ud, String filename);//LPCSTR udname

//        int ibfindW   (WString udname);//LPCWSTR udname
//        int ibbnaW    (int ud, WString udname);//LPCWSTR udname
//        int ibrdfW    (int ud, WString filename);//LPCWSTR udname
//        int ibwrtfW   (int ud, WString filename);//LPCWSTR udname

//        int ibask    (int ud, int option, IntByReference v);//PINT v
//        int ibcac    (int ud, int v);

        /** Clear a specific device */
        int ibclr    (int ud);
        
//        int ibcmd    (int ud, ByteBuffer buf, long cnt);//PVOID buf
//        int ibcmda   (int ud, ByteBuffer buf, long cnt);//PVOID buf
        
        /** Change the configuration parameters */
        int ibconfig (int ud, int option, int v);

        /** Open and initialize a device descriptor */
        int ibdev    (int boardID, int pad, int sad, int tmo, int eot, int eos);

//        int ibdiag   (int ud, ByteBuffer buf, long cnt);//PVOID buf
//        int ibdma    (int ud, int v);
//        int ibexpert (int ud, int option, Pointer Input, Pointer Output);//void * Input, void * Output
//        int ibeos    (int ud, int v);
//        int ibeot    (int ud, int v);
//        int ibgts    (int ud, int v);
//        int ibist    (int ud, int v);
//        int iblck    (int ud, int v, int LockWaitTime, Pointer Reserved);//unsigned int LockWaitTime, void * Reserved
//        int iblines  (int ud, ShortByReference result);//PSHORT result
//        int ibln     (int ud, int pad, int sad, ShortByReference listen);//PSHORT listen
//        int ibloc    (int ud);

        //typedef int (__stdcall * GpibNotifyCallback_t)(int, int, int, long, PVOID);
//        int ibnotify (int ud, int mask, GpibNotifyCallback_t Callback, PVOID RefData);//GpibNotifyCallback_t Callback, PVOID RefData

        /** Place device online or offline */
        int ibonl    (int ud, int v);

//        int ibpad    (int ud, int v);
//        int ibpct    (int ud);
//        int ibpoke   (int ud, long option, long v);
//        int ibppc    (int ud, int v);

        /** Read data from a device into a user buffer */
        int ibrd     (int ud, ByteBuffer buf, long cnt);//PVOID buf

//        int ibrda    (int ud, ByteBuffer buf, long cnt);//PVOID buf
//        int ibrpp    (int ud, CharBuffer ppr);//PCHAR spr
//        int ibrsc    (int ud, int v);

        /** Conduct a serial poll to aquire the Status Byte */
        int ibrsp    (int ud, ByteByReference spr);//PCHAR spr

//        int ibrsv    (int ud, int v);
//        int ibsad    (int ud, int v);
//        int ibsic    (int ud);
//        int ibsre    (int ud, int v);
//        int ibstop   (int ud);
//        int ibtmo    (int ud, int v);
//        int ibtrg    (int ud);
//        int ibwait   (int ud, int mask);

        /** Write data to a device from a user buffer */
        int ibwrt    (int ud, ByteBuffer buf, long cnt);//PVOID buf

//        int ibwrta   (int ud, ByteBuffer buf, long cnt);//PVOID buf

        /***********************************************************************/
        /*  Functions to access Thread-Specific copies of the GPIB global vars */

//        int  ThreadIbsta ();//void
//        int  ThreadIberr ();//void
//        int  ThreadIbcnt ();//void
//        long ThreadIbcntl ();//void


        /**********************************/
        /*  NI-488.2 Function Prototypes  */

//        void AllSpoll      (int boardID, short[] addrlist, ShortByReference results);//Addr4882_t * addrlist, PSHORT results

        /** Sends the GPIB Selected Device Clear (SDC) command message to the selected devices. */
        void DevClear      (int boardID, short addr);//Addr4882_t addr

        /** Sends the GPIB Selected Device Clear (SDC) command message to all devices on the bus. */
        void DevClearList  (int boardID, short[] addrlist);//Addr4882_t * addrlist

        /** Enable operations from the front panel of devices (leave remote programming mode) */
        void EnableLocal   (int boardID, short[] addrlist);//Addr4882_t * addrlist

        /** Enable remote GPIB programming for devices */
        void EnableRemote  (int boardID, short[] addrlist);//Addr4882_t * addrlist
        
        /** Find all Listeners on the GPIB bus */
        void FindLstn      (int boardID, short[] addrlist, short[] results, int limit);//Addr4882_t * addrlist, Addr4882_t * results

//        void FindRQS       (int boardID, ShortByReference addrlist, ShortByReference dev_stat);//Addr4882_t * addrlist, PSHORT dev_stat
//        void PPoll         (int boardID, ShortByReference result);//PSHORT result
//        void PPollConfig   (int boardID, short addr, int dataLine, int lineSense);//Addr4882_t addr
//        void PPollUnconfig (int boardID, short[] addrlist);//Addr4882_t * addrlist
//        void PassControl   (int boardID, short addr);//Addr4882_t addr
//        void RcvRespMsg    (int boardID, ByteBuffer buffer, long cnt, int Termination);//PVOID buffer

        /** Read the Status Byte */
        void ReadStatusByte(int boardID, short addr, ShortByReference result);//Addr4882_t addr, PSHORT result

        /** Read data bytes from a device */
        void Receive       (int boardID, short addr, ByteBuffer buffer, long cnt, int Termination);//Addr4882_t addr, PVOID buffer

//        void ReceiveSetup  (int boardID, short addr);//Addr4882_t addr
//        void ResetSys      (int boardID, short[] addrlist);//Addr4882_t * addrlist

        /** Send data bytes to a device */
        void Send          (int boardID, short addr, ByteBuffer databuf, long datacnt, int eotMode);//Addr4882_t addr, PVOID databuf

//        void SendCmds      (int boardID, ByteBuffer buffer, long cnt);//PVOID buffer
//        void SendDataBytes (int boardID, ByteBuffer buffer, long cnt, int eot_mode);//PVOID buffer

        /** Send interface clear */
        void SendIFC       (int boardID);

//        void SendLLO       (int boardID);
//        void SendList      (int boardID, short[] addrlist, ByteBuffer databuf, long datacnt, int eotMode);//Addr4882_t * addrlist, PVOID databuf
//        void SendSetup     (int boardID, short[] addrlist);//Addr4882_t * addrlist
//        void SetRWLS       (int boardID, short[] addrlist);//Addr4882_t * addrlist
//        void TestSRQ       (int boardID, ShortByReference result);//PSHORT result
//        void TestSys       (int boardID, short[] addrlist, ShortByReference results);//Addr4882_t * addrlist, PSHORT results
//        void Trigger       (int boardID, short addr);//Addr4882_t addr
//        void TriggerList   (int boardID, short[] addrlist);//Addr4882_t * addrlist
//        void WaitSRQ       (int boardID, ShortByReference result);//PSHORT result

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
    protected GPIB_NI() throws IOException {

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
     * Loads the native library, and establishes access to the global
     * variables defined in the native code. Does nothing if the native
     * library has already been loaded. Also sets the character encoding
     * used by JNA to the value defined in the iC.properties.
     *
     * @throws IOException when 1) the native library could not be loaded, or
     * 2) the global status variables (iberr, ...) could not be accessed.
     */
    // <editor-fold defaultstate="collapsed" desc="Load Native">
    private void LoadNative()
            throws IOException {

        // local variables
        String  LibraryName = "NI gpib";

        // return if the native library has already been loaded
        if (m_gpib32 != null) {
            // log already loaded
            m_Logger.log(Level.FINE, "JNA: The library {0} was already loaded.\n", LibraryName);

            // return
            return;
        }

        // log some parameters
        // (done only once for this class and not for every instance)
        m_Logger.log(Level.CONFIG, "EOT mode = {0}\n\t"
                + "Receive Termination = 0x{1}\n\t"
                + "Receive Buffer size = {2}", new Object[]{
            Integer.toString(EOT_MODE), Integer.toHexString(RECEIVE_TERMINATION),
            Integer.toString(RECEIVE_BUFFER_SIZE)});


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


        // get library name
        if (Platform.isWindows()) {
            LibraryName = "gpib-32.dll";

            // get the additional search path
            String Path = m_iC_Properties.getPath("GPIB_NI.AdditionalLibraryPathWin", "");

            // add additional search path
            NativeLibrary.addSearchPath(LibraryName, Path);

        } else if (Platform.isLinux()) {

            LibraryName = "gpibapi";    // libgpibapi.so
            
            // get the additional search path 
            String Path = m_iC_Properties.getPath("GPIB_NI.AdditionalLibraryPathLinux", "");
            
            // add additional search path
            NativeLibrary.addSearchPath(LibraryName, Path);

        } else if (Platform.isMac()) {

            // to load a Framework from a dir other than /System/Library/Frameworks/
            // it is necessary to specify the fully quantified path as LibraryName
            // as NativeLibrary.addSearchPath is not effective for Frameworks it seems

            // get the additional path
            String Path = m_iC_Properties.getPath("GPIB_NI.AdditionalLibraryPathMac", "/Library/Frameworks/NI488.framework/");

            // add an additional search path to the LibraryName
            LibraryName = Path + "NI488";
        }
        

        // log platform name
        // see https://jna.dev.java.net/nonav/javadoc/index.html
        m_Logger.log(Level.FINE, "JNA: Platform name = {0}\n",
                Integer.toString(Platform.getOSType()));


        try {
            // load the native library
            m_gpib32 = (gpib32) Native.loadLibrary(LibraryName, gpib32.class);

            // log success
            m_Logger.log(Level.FINE, "JNA: Loaded library {0}.\n", LibraryName);

        } catch (UnsatisfiedLinkError ex) {
            String str = "Could not load the native library for the NI/Agilent GPIB-USB controller.\n";
            str += "Please ensure that the drivers are installed properly.\n";
            str += "On WinXP, gpib-32.dll is typically installed in C:\\Windows\\System32.\n";
            str += "On MacOS, the NI488 Framework is typically installed in /Library/Frameworks/.\n"
                + "On Mac, it is necessary to run iC in 32 bit mode (-d32).\n";
            str += "See Java Native Access (JNA) on how native libraries are loaded.\n\n";
            str += "Java Native Access' response:\n";
            str += ex.getMessage();

            m_Logger.severe(str);
            throw new IOException(str);
        }

        
        /*  Set up access to the user variables (ibsta, iberr, ibcnt, ibcntl).
         *  These are declared and exported by the 32-bit DLL.  Separate copies
         *  exist for each process that accesses the DLL.  They are shared by
         *  multiple threads of a single process. The variable names in the DLL
         *  (Windows) have different names than the variables in the Framework (Mac) */
        try {
            // load variables on a Mac
            if (Platform.isMac()) {
                // ibsta
                Pointer dummy =  NativeLibrary.getInstance(LibraryName).getGlobalVariableAddress("ibsta");
                m_ibsta.setPointer(dummy);

                //iberr
                dummy =  NativeLibrary.getInstance(LibraryName).getGlobalVariableAddress("iberr");
                m_iberr.setPointer(dummy);

                // ibcnt
                dummy =  NativeLibrary.getInstance(LibraryName).getGlobalVariableAddress("ibcnt");
                m_ibcnt.setPointer(dummy);

                // ibcntl
                dummy =  NativeLibrary.getInstance(LibraryName).getGlobalVariableAddress("ibcntl");
                m_ibcntl.setPointer(dummy);

            } else { // load variables on Windows
                // ibsta
                Pointer dummy =  NativeLibrary.getInstance(LibraryName).getGlobalVariableAddress("user_ibsta");
                m_ibsta.setPointer(dummy);

                //iberr
                dummy =  NativeLibrary.getInstance(LibraryName).getGlobalVariableAddress("user_iberr");
                m_iberr.setPointer(dummy);

                // ibcnt
                dummy =  NativeLibrary.getInstance(LibraryName).getGlobalVariableAddress("user_ibcnt");
                m_ibcnt.setPointer(dummy);

                // ibcntl
                dummy =  NativeLibrary.getInstance(LibraryName).getGlobalVariableAddress("user_ibcntl");
                m_ibcntl.setPointer(dummy);
            }
            
        } catch (UnsatisfiedLinkError ex) {
            String str = "Could not access the global variables in the native library for the NI/Agilent GPIB-USB controller.\n";
            str += "Java Native Access' response:\n";
            str += ex.getMessage();

            m_Logger.severe(str);
            throw new IOException(str);
        }

        // it does not help to set the EOT mode here
        //m_gpib32.ibconfig(0, IbcEOT, NLend);
        

        /* When converting Java unicode characters into an array of char, the
         * default platform encoding is used, unless the system property 
         * jna.encoding is set to a valid encoding. This property may be set to 
         * "ISO-8859-1", for example, to ensure all native strings use that encoding.
         * 
         * The charqacter encoding used by JNA is set in IcontrolView.myInit
         */
        
    }//</editor-fold>


    /**
     * Opens the connection to the Instrument at the specified GPIB address.<p>
     *
     * If NI 488.2 functions are enabled (<code>USE_4882=true</code>):<br>
     * This method checks if an Instrument is listening on the specified
     * GPIB address, and if so, a Selected Device Clear (SDC) GPIB command
     * is issued. The timeout value (defined in iC.properties) is set by
     * calling <code>setTimeOut</code>.<p>
     *
     * If NI 488.1 functions are used for Send/Receive/ReadStatusByte:<br>
     * The Unit Descriptor is obtained using <code>ibdev</code>, which allows
     * to set the default timeout (defined in iC.properties) as well as
     * EOT and EOS mode. The device is also cleared.<p>
     *
     * Both ways assign a proper value to <code>m_UnitDescriptor</code>, so every
     * function call, be it 488.1 or 488.2, can use <code>m_UnitDescriptor</code>
     * to access the current Instrument.
     *
     * @param GPIB_Address The primary GPIB address of the Instrument
     *
     * @throws IOException When a GPIB transaction caused a GPIB error (bubbles
     * up from <code>checkErrorGPIB</code>.
     *
     */
    // <editor-fold defaultstate="collapsed" desc="Open">
    protected void Open(int GPIB_Address)
           throws IOException {

        // remember GPIB Address
        m_GPIB_Adr = GPIB_Address;

        // get time out value
        int TimeOut = m_iC_Properties.getInt("GPIB_NI.TimeOut", T10s);

        // 488.1 or 488.2 function calls ?
        if (USE_4882 == true) {
            // send Selected Device Clear to the Instrument
            ClearDevice();

            // check for errors
            String str = "Could not clear the Instrument at address "
                    + Integer.toString(GPIB_Address) + ".\n";
            checkErrorGPIB(str, false);

            // assign the Unit Descriptor
            m_UnitDescriptor = MakeAdr(m_GPIB_Adr, 0);

            // set TimeOut value
            // because this might not always work, a possible exception
            // is treated differently
            try {
                setTimeOut(TimeOut);
            } catch (IOException ex) {
                str = "Could not set the GPIB TimeOut value; using default instead.\n";

                // log event
                m_Logger.warning(str);

                // show it to the user
                m_GUI.DisplayStatusMessage("Warning: " + str, false);
            }
            
        } else {
            
            // init device
            m_UnitDescriptor = m_gpib32.ibdev(BOARD_NR, GPIB_Address, 0,
                    TimeOut, EOT_MODE, RECEIVE_TERMINATION);

            // log eot mode
            m_Logger.log(Level.FINER, "eot mode= {0}\n", EOT_MODE);
            m_Logger.log(Level.FINER, "Receive Termination= {0}\n", RECEIVE_TERMINATION);

            // check for errors
            String str = "Could not configure the Instrument at address "
                    + Integer.toString(GPIB_Address) + ".\n";
            checkErrorGPIB(str, false);

            // clear device
            m_gpib32.ibclr(m_UnitDescriptor);

            // check for errors
            str = "Could not clear the Instrument at address "
                    + Integer.toString(GPIB_Address) + ".\n";
            checkErrorGPIB(str, true);
        }

        // Start Speed Test (used during debugging)
        //SpeedTest(10);
    }//</editor-fold>



    /**
     * Sends the given String via the GPIB bus to the Instrument specified in
     * <code>Open</code> and returns the number of bytes actually sent. If
     * <code>Msg</code> is empty, the method returns immediately. The character set
     * specified in the iC.properties (ISO-8859-1) is used to convert between 
     * <code>Msg</code> and the stream of bytes (byte[] using <code>String.getBytes</code>.<p>
     *
     * NOTE: It seems as if the EOT_Mode parameter has no effect on the
     * GPIB transmission when using 488.1 and 488.2 function calls.
     * For now, the newline character is added manually.
     *
     * @param Msg Data to be sent
     * @return Number of data bytes actually sent
     * @throws IOException When the transmission caused a GPIB error
     */
    // <editor-fold defaultstate="collapsed" desc="Send">
    protected long Send(String Msg)
              throws IOException {
        
        // do nothing if Msg is empty. This is required because 
        // Device.invokeGenericScriptMethod uses QueryInstrument("") to query the
        // Instrument, and if NLend is selected, a \n is added and sent to the
        // Instrument, which can cause a error in some insttruments, e.g. Agilent E4980
        if (Msg.isEmpty()) {
            return 0;
        }

        // add newline if not already there (because gpib-32.dll does not
        // do that for me although EOT_MODE of NLend is specified
        if ( (EOT_MODE == NLend) && (!Msg.endsWith("\n")) )
            Msg += "\n";

        // make a ByteBuffer to hold the Msg String
        ByteBuffer BBuffer = ByteBuffer.wrap(Msg.getBytes(CHARACTER_ENCODING));
        
        // send it
        if (USE_4882 == true) {
            m_gpib32.Send(BOARD_NR, (short)m_UnitDescriptor,
                    BBuffer, Msg.length(), EOT_MODE);
        } else {
            m_gpib32.ibwrt(m_UnitDescriptor, BBuffer, Msg.length());
        }

               
        // check for errors
        String str = "A GPIB error occurred during sending the last command.\n";
        checkErrorGPIB(str, false);

        // return the number of Bytes that have been send
        return m_ibcntl.getValue();
    }//</editor-fold>



    /**
     * Receives data from the Instrument specified in <code>Open</code>.
     * The number of Bytes that are requested from the Instrument are specified
     * by <code>RECEIVE_BUFFER_SIZE</code>, which, in turn, is specified in the
     * iC.property file. Currently this value is 150000 which has proven useful
     * in my personal experience. Increase this value if an Instrument places
     * more data in the output queue. A number which is too small will most
     * likely result in an erroneous transmission (<code>Receive</code> does
     * not check if all data bytes have been read). This method uses either 488.1
     * or 488.2 function calls. The received data stream (byte[]) is converted to
     * a String using the character encoding defined in the iC.properties 
     * (IOS-8859-1 per default).
     *
     * @param Trim If <code>true</code>, newline (\n) and carriage return (\r)
     * characters are removed from the end of the returned String.
     * @return The data bytes sent by the Instrument wrapped in a String.
     * @throws IOException When the transmission caused a GPIB error
     */
    // <editor-fold defaultstate="collapsed" desc="Receive">
    protected String Receive(boolean Trim)
           throws IOException {
        
        // local variables
        String ret = "";

        // check RECEIVE_BUFFER_SIZE vs. limit
        if (RECEIVE_BUFFER_SIZE != m_ReceiveBuffer.limit())
            m_Logger.warning("RECEIVE_BUFFER_SIZE != m_ReceiveBuffer.limit()\n");


        // Receive the data
        if (USE_4882 == true) {
            m_gpib32.Receive(BOARD_NR, (short)m_UnitDescriptor, m_ReceiveBuffer,
                    m_ReceiveBuffer.limit(), RECEIVE_TERMINATION);
        } else {
            m_gpib32.ibrd(m_UnitDescriptor, m_ReceiveBuffer, m_ReceiveBuffer.limit());
        }

        // check for errors
        String str = "A GPIB error occurred during receiving data.\n";
        checkErrorGPIB(str, false);


        // get the number of Bytes that have been recieved
        int NrBytes = (int)m_ibcntl.getValue();


        // convert to a String of the correct size
        ret = new String(m_ReceiveBuffer.array(), 0, NrBytes, CHARACTER_ENCODING);

        // remove trailing newline & carriage return
        if (Trim)
            ret = ret.replaceFirst("[\\n[\\r]]+$", "");

        return ret;
    }//</editor-fold>



    /**
     * Reads the Status Byte of the Instrument defined in <code>Open</code>
     * by serially polling the Instrument. <code>Open</code> must have been
     * called before calling this method. This method uses either 488.1
     * or 488.2 function calls.
     *
     * @return The Status Byte of the Instrument.
     * @throws IOException When the transmission caused a GPIB error
     */
    // <editor-fold defaultstate="collapsed" desc="Read Status Byte">
    @Override
    public int ReadStatusByte()
           throws IOException {

        int StatusByte;
        ShortByReference pShort = new ShortByReference();
        ByteByReference pByte = new ByteByReference();


        // read the status byte
        if (USE_4882 == true) {
            m_gpib32.ReadStatusByte(BOARD_NR, (short)m_UnitDescriptor, pShort);
            StatusByte = (int)pShort.getValue();
        } else {
            m_gpib32.ibrsp(m_UnitDescriptor, pByte);
            StatusByte = (int)pShort.getValue();
        }

        // error check
        String str = "Could not read the Status Byte from the Instrument.\n";
        checkErrorGPIB(str, false);

        // return
        return StatusByte;
    }//</editor-fold>

    /**
     * Send Selected Device Clear (SDC) GPIB command to all Instruments that are
     * Listeners on the GPIBbus. This method uses only or 488.2 function calls.<p>
     *
     * First, the GPIB controller becomes Controller-In-Charge (CIC), then the GPIB
     * Addresses of all Listeners on the bus are obtained, and a Selective Device
     * Clear (SDC) is sent to all Listeners on the bus.<p>
     * The example provided from NI (Dll4882query.c) was closely followed.<p>
     *
     * This method is defined as private because it should not be needed but
     * it was already tested so deleting it would be a waste.
     *
     * @throws IOException When 1) the Controller could not become CIC, or 2) the
     * GPIB command 'FindLstn' could not be issued, or 3) the SDC could not be sent.
     */
    // <editor-fold defaultstate="collapsed" desc="Clear All Devices">
    private void ClearAllDevices()
            throws IOException {

        //////////////////////////////
        // Become Controller-In-Charge

        // The board needs to be the Controller-In-Charge in order to find all
        // listeners on the GPIB.
        m_gpib32.SendIFC(BOARD_NR);

        // clean up if there was an error and throw an exception
        String str = "The NI/Agilent GPIB controller could not become Controller-In-Charge.\n";
        checkErrorGPIB(str, true);


        //////////////////////////////////////////
        // Find a list of all attached Instruments

        // create a list of all primary addresses to search
        short LookUp[] = new short[31];

        // initialize the list; NOADDR signifies the end of the array
        for (int i = 0; i < 30; i++) {
           LookUp[i] = (short)(i + 1);
        }
        LookUp[30] = (short) NOADDR;

        // log starting to look for listeners
        m_Logger.fine("Looking for Listeners.\n");


        // find the Listeners
        // Results will contain the addresses of all listening devices found by FindLstn
        short[] Results = new short[32];
        m_gpib32.FindLstn(BOARD_NR, LookUp, Results, 31);

        // clean up if there was an error and throw an exception
        str = "The NI/Agilent GPIB controller could not issue FindLstn call.\n";
        checkErrorGPIB(str, true);


        // get the number of Listeners found
        int NrListeners = (int)m_ibcntl.getValue();

        // log # of Listeners found
        m_Logger.log(Level.FINE, "Found {0} Listeners.\n", Integer.toString(NrListeners));



        ////////////////
        // Clear Devices

        // add NOADDR to mark the end of the array
        Results[NrListeners] = (short) NOADDR;

        // send the GPIB Selected Device Clear (SDC) command message to all
        // devices on the bus.
        m_gpib32.DevClearList(BOARD_NR, Results);

        // clean up if there was an error and throw an exception
        str = "Unable to clear the Listeners on the GPIB bus.\n";
        checkErrorGPIB(str, true);
        
        // log all devices cleared
        m_Logger.fine("All Listeners have been cleared (SDC).\n");
    }//</editor-fold>


    /**
     * Send Selected Device Clear (SDC) GPIB command to the current Instrument
     * defined in <code>Open</code>. This method uses only 488.2 function calls.<p>
     *
     * First, the GPIB controller becomes Controller-In-Charge (CIC), then it is
     * checked if a Listener is present at the specified GPIB address, and the
     * the SDC command is sent.
     *
     * @throws IOException When 1) the Controller could not become CIC, or 2) the
     * GPIB command 'FindLstn' could not be issued, or 3) the SDC could not be
     * sent, or 4) no Instrument was listening on the specified GPIB address.
     */
    // <editor-fold defaultstate="collapsed" desc="Clear Device">
    private void ClearDevice()
            throws IOException {

        //////////////////////////////
        // Become Controller-In-Charge

        // The board needs to be the Controller-In-Charge in order to find all
        // listeners on the GPIB.
        m_gpib32.SendIFC(BOARD_NR);

        // clean up if there was an error and throw an exception
        String str = "The GPIB_NI driver could not become Controller-In-Charge.\n";
        checkErrorGPIB(str, true);
        

        //////////////////////////////////////
        // Find if an Instrument is listening

        // create a list of all primary addresses to search
        short LookUp[] = new short[2];

        // initialize the list; NOADDR signifies the end of the array
        LookUp[0] = (short) m_GPIB_Adr;
        LookUp[1] = (short) NOADDR;

        // log starting to look for listener
        m_Logger.log(Level.FINE, "Looking for Listener at GPIB address {0}.\n", Integer.toString(m_GPIB_Adr));


        // find the Listener
        // Results will contain the addresses of all listening devices found by FindLstn
        short[] Results = new short[1];
        m_gpib32.FindLstn(BOARD_NR, LookUp, Results, 1);

        // clean up if there was an error and throw an exception
        str = "The GPIB_NI driver could not issue FindLstn call.\n";
        checkErrorGPIB(str, true);
        

        // get the number of Listeners found
        int NrListeners = (int)m_ibcntl.getValue();

        // log # of Listeners found
        m_Logger.log(Level.FINE, "Found {0} Listeners.\n", Integer.toString(NrListeners));


        // Exit if no Listener was found
        if (NrListeners != 1) {
            str = "No Instrument is listening on GPIB address " +
                    Integer.toString(m_GPIB_Adr) + ".\n";
            str += getGPIBStatus();

            // close connection to the Instrument
            CloseInstrument();

            m_Logger.severe(str);
            throw new IOException(str);
        }

        ///////////////
        // Clear Device

        // send the GPIB Selected Device Clear (SDC) command message
        m_gpib32.DevClear(BOARD_NR, (short)m_UnitDescriptor);

        // clean up if there was an error and throw an exception
        str = "Unable to clear the Listener at GPIB addresss " +
                    Integer.toString(m_GPIB_Adr) + ".\n";
        checkErrorGPIB(str, true);
        

        // log all devices cleared
        m_Logger.log(Level.FINE, "Instrument at address {0} cleared (SDC).\n", Integer.toString(m_GPIB_Adr));
    }//</editor-fold>


    /**
     * Sets the TimeOut value for the default GPIB controller (defined
     * with <code>BOARD_NR</code>). [Tested 110126: it works!]<p>
     *
     * @param TimeOutConstant The new TimeOut value; choose a value for the constants
     * defined above (for instance T3s, T10s, ...)
     * @throws IOException When the transmission caused a GPIB error
     */
    // <editor-fold defaultstate="collapsed" desc="set Time Out">
    private void setTimeOut(int TimeOutConstant)
              throws IOException {

        // configure the new time out value for the GPIB controller
        m_gpib32.ibconfig(BOARD_NR, IbaTMO, TimeOutConstant);

        // clean up if there was an error and throw an exception
        String str = "Unable to change the TimeOut value for the default GPIb controller\n";
        checkErrorGPIB(str, true);
    }//</editor-fold>



    /**
     * Gets the Status of the last GPIB call. After each GPIB call, the
     * error flag in the Status Byte <code>m_ibstat</code> should, ideally, be
     * checked. If an error occurred, this method returns a String with a short
     * description of what went wrong.
     *
     * @return When an error occurred during the last GPIB call, the returned
     * String contains a somewhat helpful explanation of what went
     * wrong. An empty String is returned if no error occurred.
     */
    // <editor-fold defaultstate="collapsed" desc="get Status">
    protected String getGPIBStatus() {

        // check for errors
        if ( (m_ibsta.getValue() & ERR) != 0) {
            // return the GPIB Status because an error occurred
            int err = m_iberr.getValue();

            // make a nice String with the GPIB Status
            String str = String.format(Locale.US, "GPIB Status: ibsta = 0x%x;   iberr = %d (%s)\n",
                    m_ibsta.getValue(), err,
                    (err<ErrorMnemonic.length ? ErrorMnemonic[err] : "Error number not recognized"));

            // log event
            m_Logger.finer(str);
            //m_Comm_Logger.finer(str);

            return str;
        } else {
            // return an empty String
            return "";
        }
    }//</editor-fold>


    /**
     * Checks the Status of the last GPIB transaction and throws an
     * Exception if a GPIB error occurred. If an error occurred, this
     * event is also Logged with level FINER and in the GPIB-Logger.
     *
     * @param Msg This String is appended to the message thrown as an
     * Exception when an error occurred.
     * @param CloseConnection If <code>true</code>, the connection to the
     * GPIB board is closed when an error occurred.
     * @throws IOException when an error occurred during the last GPIB operation
     */
    // <editor-fold defaultstate="collapsed" desc="check Error GPIB">
    private void checkErrorGPIB(String Msg, boolean CloseConnection)
            throws IOException {

        // check for errors
        if ( (m_ibsta.getValue() & ERR) != 0) {

            String str = Msg + getGPIBStatus() + "\n";

            // log event
            m_Logger.finer(str);
            m_Comm_Logger.severe(Msg);


            // close connection to the GPIB controller
            if (CloseConnection == true) {
                CloseInstrument();
            }

            throw new IOException(str);
        }
    }//</editor-fold>



    /**
     * Closes the connection to the Instrument specified in <code>Open</code>.
     * This method is called from <code>Dispatcher.run</code> after the Script
     * has been processed.
     *
     * @throws IOException When the closing caused a GPIB error
     */
    // <editor-fold defaultstate="collapsed" desc="CloseInstrument">
    protected void CloseInstrument() 
              throws IOException {

        // log
        m_Logger.log(Level.FINE, "Closing connection to the Instrument at address {0}.\n", Integer.toString(m_GPIB_Adr));

        // Take controller offline
        if (USE_4882 == true) {
            short[] dummy = {(short)m_GPIB_Adr};
            m_gpib32.EnableLocal(BOARD_NR, dummy);
            
        } else {
            // second argument is 0
            m_gpib32.ibonl(m_UnitDescriptor, 0);
        }

        // check for errors
        String str = "Could not enable local mode when closing the connection to the instrument\n";
        checkErrorGPIB(str, false);

    }//</editor-fold>


    /**
     * Closes the connection to the GPIB controller. This method is called from
     * <code>Dispatcher.run</code> after the Script has been processed.
     *
     * @throws IOException When the closing caused a GPIB error
     */
    // <editor-fold defaultstate="collapsed" desc="Close Controller">
    protected void CloseController() 
              throws IOException {

        // log
        m_Logger.fine("Closing connection to the GPIB controller (GPIB_NI).\n");

        // Take controller offline
        // the second argument places the device offline (0) or online (1)
        m_gpib32.ibonl(BOARD_NR, 0);

        // check for errors
        String str = "Could not close the connection to the GPIB controller\n";
        checkErrorGPIB(str, false);

        // assign null to detect any calls after I don't expect any
        m_gpib32 = null;
        
        // just in case, free resources to the ByteBuffer and start GarbageCollection
        m_ReceiveBuffer = null;
        System.gc();

        // In JNA it is not necessary to unload the dll

    }//</editor-fold>


    /**
     * Creates an Address as required by many 488.2 functions. The primary 
     * address goes in the lower 8-bits and the secondary address goes in the 
     * upper 8-bits.
     * 
     * @param PrimaryAdr Primary GPIB address
     * @param SecondaryAdr Secondary GPIB address
     * @return The GPIB address in a format as required by many 488.2 calls
     */
    // <editor-fold defaultstate="collapsed" desc="MakeAdr">
    private short MakeAdr(int PrimaryAdr, int SecondaryAdr) {
        int dummy = (PrimaryAdr & 0xFF) | (SecondaryAdr<<8);
        return (short)dummy;
    }//</editor-fold>

    /*
     *  The following two macros are used to "break apart" an address list
     *  entry.  They take an unsigned integer and return either the primary
     *  or secondary address stored in the integer.
     */
    //#define  GetPAD(val)    ((val) & 0xFF)
    //#define  GetSAD(val)    (((val) >> 8) & 0xFF)
}
