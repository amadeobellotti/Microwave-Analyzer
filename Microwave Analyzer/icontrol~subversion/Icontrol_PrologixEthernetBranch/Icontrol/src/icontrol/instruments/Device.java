// TODO 1* implement argument-type-check in findScriptMethod to enable true overloading
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
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import icontrol.Dispatcher;
import icontrol.IcontrolView;
import icontrol.ScriptMethod;
import icontrol.dialogs.JythonPrompt;
import icontrol.dialogs.WrongIDN;
import icontrol.iC_Annotation;
import icontrol.iC_Properties;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.DataFormatException;
import javax.script.ScriptException;
import javax.swing.JOptionPane;
import org.python.core.PyException;
import org.python.core.PyInteger;
import org.python.util.PythonInterpreter;


/**
 * This is the base class from which all other Instrument-Classes
 * should be derived. It implements basic functionality such as
 * opening the instrument via the GPIB bus.<p>
 * 
 * Note: After instantiating this class, use <code>setInstrumentClassName</code>
 * to ensure generically defined methods can be found. See also the comment at the 
 * constructor.<p>
 *
 * Consult the main page of the javadoc for instructions on how to write new
 * Instrument-Classes.<p>
 *
 * <h3>Instrument Commands:</h3>
 * <ul>
 *  <li>{@link #Send(String) }
 *  <li>{@link #SpeedTest(int) }
 *  <li>{@link #Query(String) }
 * </ul>
 *
 * <h3>Utility Methods:</h3>
 * <ul>
 *  <li>{@link #GenerateValues(double, double, double, boolean, java.util.ArrayList, int)  }
 *  <li>{@link #getBoolean(java.lang.String) }
 *  <li>{@link #getDouble(java.lang.String) }
 *  <li>{@link #getFloat(java.lang.String) }
 *  <li>{@link #getInteger(java.lang.String) }
 * </ul>
 *
 *
 * @author KPP (Kurt Pernstich: pernstich@alumni.ethz.ch)
 * @version 0.4
 */
public class Device {

    //////////////////////
    // my member variables

    /** The Instrument-Name of this instance */
    protected String    m_InstrumentName;

    /**
     * Holds the instances of the Instruments according to their name. It is 
     * shared between all instruments derived from Device. This is actually only
     * a "reference" to <code>Dispatcher.m_UsedInstruments</code> and it is
     * assigned in the <code>Dispatcher</code>'s constructor.
     */
    protected static HashMap<String, Device> m_UsedInstruments = null;

    /**
     * Holds a reference to the <code>IcontrolView</code> class. So far only
     * used by the virtual instrument <code>iC_Instrument</code> to "press"
     * the Pause button via script.
     */
    protected static IcontrolView m_GUI;
    

    /** Handle for GPIB_NI and GPIB_Prologix to access the GPIB controllers (via JNA) */
    protected GPIB_Driver m_GPIB_Driver;


    // stores the GPIB Address
    private int m_GPIBAddress;
    
    /** Handle to access the RS232 driver */
    protected RS232_Driver m_RS232_Driver;
    
    /** Handle to access the network driver for LAN access */
    protected LAN_Driver m_LAN_Driver;
    

    /**
     * When set to <code>true</code>, the instrument of this instance is set
     * into a no-communication-mode where no I/O communication takes place.
     * Can only be set from <code>OpenXYZ</code> when, for instance,
     * <code>OpenGPIB</code> failed and the user chose to enter a No-Communication-Mode.
     */
    private boolean m_NoCommunicationModeLocal;


    /**
     * When set to <code>true</code>, all Instruments derived from <code>Device</code>
     * are set into a no-communication-mode where no GPIB communication
     * takes place. Can be set/reset using <code>setNoCommunicationModeGlobal</code>.
     */
    private static boolean  m_NoCommunicationModeGlobal;

    /**
     * When set to <code>true</code>, no methods in Instruments classes
     * derived from <code>Device</code> are called, unless they bare an
     * <code>iC_Annotation</code> with <code>MethodChecksSyntax</code> set to
     * true. This value is set in {@link icontrol.Dispatcher#setSyntaxCheckMode(boolean)}
     */
    private static boolean  m_SyntaxCheckMode;


    /**
     * Defines all implemented communication ports/protocols in any of
     * the Instrument classes. As of now, only GPIB, RS232 and LAN (TCP/IP) are implemented.
     *
     * The <code>OpenInstrument</code> method of <code>Device</code> stores the chosen mode,
     * and every method in the Instrument classes should format the message to
     * the instrument according to the chosen protocol.
     */
    public enum CommPorts {GPIB, RS232, LAN, none}

    /**
     * Stores the chosen communication port/protocol
     */
    protected CommPorts m_UsedCommPort;


    /**
     * Defines the supported GPIB controllers.
     * NI = National Instruments GPIB controller with NI 488.2 drivers
     * Prologix = Prologix GPIB-USB controller with D2XX Direct Driver
     * Prologix_Ethernet = Prologix GPIB-Ethernet controller using Java's Ethernet capabilities
     * IOtech = IOtech RS232-GPIB controller using <code>RS232_Driver</code> class
     */
    public enum GPIBcontroller {NI, Prologix, Prologix_Ethernet, IOtech}

    /** Stores the chosen GPIB controller. It is set from <code>IcontrolView.Start</code> */
    protected static GPIBcontroller m_GPIBcontroller;

    /** is true when processing of the script should be stopped */
    protected static boolean m_StopScripting;

    /** remembers the start time of processing the script */
    protected static long m_tic;

    /** The Logger for this class */
    private static final Logger m_Logger = Logger.getLogger("iC.Instruments.Device");

    /** The GPIB Logger */
    private static final Logger m_GPIB_Logger = Logger.getLogger("GPIB");


    /**
     * Grants access to global properties of the iC application in a resource
     * bundle file 'iC.properties'
     */
    protected static iC_Properties m_iC_Properties;

    
    /** Stores the response to a *IDN? query */
    protected String m_IDN = "not set";

    /**
     * Stores the return value of the method implementing the script-command.
     * Can be used in successive script-commands. An example is given in
     * <code>NewInstrument.DisplayPreviousResult</code>
     */
    protected static Object m_LastReturnValue;


    /**
     * Stores the Lock for the GPIB controller.
     * Using a Lock on a per GPIB address makes no sense, because who knows what
     * the GPIB controller behaves when two Instruments are addresses interleaved.
     * For the NI driver the single thread methods are used anyways.
     */
    protected static ReentrantLock m_LockGPIBController = new ReentrantLock(true);
    //OLD: protected static Map<Integer, ReentrantLock> m_Locks =
    //        Collections.synchronizedMap( new HashMap<Integer, ReentrantLock>(4, 1) );
    // also enable in OpenGPIB, ReadViaGPIB, QueryViaGPIB, CloseController
    
    
    /** Stores the Lock for the RS232 port. */
    protected static ReentrantLock m_LockRS232Port = new ReentrantLock(true);

    /** Defines the format of the time stamp which precedes lines in the GPIB logger
     * Could be changed to HH:mm:ss.S */
    private static final SimpleDateFormat m_TimeStampFormat = new SimpleDateFormat("HH:mm:ss");

       
    /** Stores all generically defined GPIB Instruments. The Instrument-Class-Name 
     * is used as a key. This map is populated in <code>loadGenericGPIBInstruments</code>
     * and is used in conjunction with <code>m_InstrumentClassName</code> in 
     * <code>listAllScriptMethods</code> to find all Script-Methods (a public method
     * in an Instrument-Class) for a given Instrument. */
    protected static HashMap<String, GenericGPIBInstrument> m_GenericGPIBInstruments =
            new HashMap<String, GenericGPIBInstrument>();

    
    /** Stores the Instrument-Class-Name for a particular instance of
     * <code>Device</code>. It is set in <code>HandleMakeCommand</code> after
     * instantiating an object and is used to access the identify the proper
     * Instrument in all the generic GPIB Instrument defined in
     * <code>m_GenericGPIBInstruments</code>. */
    protected String m_InstrumentClassName = "";
    
    /** Handle to the Jython/Python Interpreter. Is null if no Python statement
     * has been issued. It's static because only one PythonInterpreter exists
     * for all instances of Device. */ 
    private static PythonInterpreter m_PythonInterpreter;
    
    /** Handle to the Form (Window) that shows the Python output and the Python
     * command line. Must be static so that the only one Jython Prompt Window 
     * is accessible in all instances of Device. */
    private static JythonPrompt m_JythonPromptWindow;
    
    
    
    
    /**
     * Class that defines a generic GPIB Instrument. It is static so that it can 
     * be accessed from <code>RegisterGenericDeviceClasses</code>, which is also static.
     */
    // <editor-fold defaultstate="collapsed" desc="Generic GPIB Instrument class">
    protected class GenericGPIBInstrument {

        ///////////////////
        // member variables

        /* The Instrument-Class-Name (e.g. SRS DS345). It is not really used for 
         * anything but I did not yet want to remove it. */
        //private String m_InstrumentClassName;
        
        /** A list of commands the Instrument supports. */
        private ArrayList<ScriptMethod> m_ScriptMethods;

        /* The Instrument Name Identifier is part of the response to a *IDN? query */
        //private String m_InstrumentNameIdentifier;
        
        
        /**  
         * Default constructor. Initializes data structures.
         */
        // <editor-fold defaultstate="collapsed" desc="Constructor">
        public GenericGPIBInstrument() {

            // instantiate the structure to hold the commands for this Instrument
            m_ScriptMethods = new ArrayList<ScriptMethod>();

            // assign the Instrument Class Name
            //this.m_InstrumentClassName = InstrumentClassName;
            
            // assign the Instrument Name Identifier
            //this.m_InstrumentNameIdentifier = InstrumentNameIdentifier;
        }//</editor-fold>

        
        /** add a generic GPIB command in the form of a <code>ScriptMethod</code>
         * command to the list of supported commands of this Instrument */
        // <editor-fold defaultstate="collapsed" desc="add Command">
        public void addCommand(ScriptMethod met) {
            // TODO 3* ensure that not two commands have the same name!
            
            // add the new method
            this.m_ScriptMethods.add(met);
        }//</editor-fold>
        
        
        /** @return Returns the generically defined GPIB commands this Instrument
         * implements. It can return an empty list but never <code>null</code>. */
        // <editor-fold defaultstate="collapsed" desc="get Commands">
        ArrayList<ScriptMethod> getCommands() {
            return m_ScriptMethods;
        }//</editor-fold>

    }//</editor-fold>
    
   

    /**
     * Instrument Name Identifier:<br>
     * If the response to *IDN? contains the Instrument Name Identifier String
     * as returned from this method, the instrument is accepted, otherwise 
     * an Exception is thrown. If the returned String is empty, no *IDN? query
     * will be performed (some older instruments do not support *IDN?). <p>
     *
     * To check if the addressed instrument is really of the expected type
     * (for instance when an instance of class Lakeshore340 is created but
     * a Agilent 4155 has the specified GPIB address assigned) the
     * response to *IDN? is evaluated against the returned String. <p>
     *
     * Instrument classes implementing Instruments that support IEEE 488.2 should
     * override this method.<p>
     *
     * It can be advantageous to define the Instrument Name Identifier in the
     * resource iC.properties, to enable changing this value without having
     * to recompile the program. See the example linked to below.<p>
     *
     * @return The string that identifies the generic Instrument type upon an
     * '*IDN?' query. See the Agilent4155 class for an example.
     *
     * @see Agilent4155#getInstrumentNameIdentifier()
     * {@link NewInstrument#getInstrumentNameIdentifier()}
     */
    // <editor-fold defaultstate="collapsed" desc="get Instrument Name Identifier">
    protected String getInstrumentNameIdentifier() {

        // get class name
        String Key = this.getClass().getName();

        // strip of the package name
        Key = Key.substring( Key.lastIndexOf ('.') + 1 );

        // add the second part of the key
        Key += ".InstrumentNameIdentifier";

        // return the Instrument Response or an empty String
        return m_iC_Properties.getString(Key, "");
    }//</editor-fold>

    
    
    /**
     * Enables of disables the Syntax-check mode for all Instruments
     * derived from class <code>Device</code>. This is the setter method
     * for <code>m_SyntaxCheckMode</code>. It is called in the <code>run</code>
     * method of the <code>Dispatcher</code> class.
     */
    // <editor-fold defaultstate="collapsed" desc="set Syntax check Mode">
    public static void setSyntaxCheckMode(boolean SyntaxCheckMode) {
        m_SyntaxCheckMode = SyntaxCheckMode;
    }//</editor-fold>


     /**
     * Get the flag for the Syntax-check mode.
     *
     * @return <code>true</code> if the Syntax-check mode is enabled, and no
     * IO-communication or other operations should be performed.
     */
    // This method is public so that it can be accessed from Python
    // <editor-fold defaultstate="collapsed" desc="in Syntax Check Mode?">
    public final boolean inSyntaxCheckMode() {
        return m_SyntaxCheckMode;
    }//</editor-fold>


    // <editor-fold defaultstate="collapsed" desc="set NoCommunicationModeGlobal">
    /**
     * Setter method for <code>m_NoCommunicationModeGlobal</code>
     *
     * @param NoCommunicationModeGlobal if <code>true</code>, no-communication-mode
     * is enabled and no GPIB communication takes place.
     */
    public static void setNoCommunicationModeGlobal(boolean NoCommunicationModeGlobal) {
        Device.m_NoCommunicationModeGlobal = NoCommunicationModeGlobal;
    }//</editor-fold>

    
    /**
     * Setter method for the used GPIB controller. It is called from
     * <code>IcontrolView.Start</code>. Later implementations might allow more
     * than one GPIB controller which is the reason why the member variable
     * used to store the selection is called 'default'.<p>
     *
     * This method is called after the user pressed Start, and must not be called
     * after the <code>Device</code> class has been instantiated or a
     * <code>NullPointerException</code> might occur.<p>
     *
     * @param controler The GPIB controller to be used to communicate with
     * the Instruments.
     */
    // <editor-fold defaultstate="collapsed" desc="set GPIB controller">
    public static void setGPIBcontroller(GPIBcontroller controler) {
        m_GPIBcontroller = controler;
    }//</editor-fold>


    /**
     * Gets the current time in milliseconds and stores it in <code>m_tic</code>.
     */
    // <editor-fold defaultstate="collapsed" desc="tic">
    public static void tic() {

        // remember the current time
        m_tic = System.currentTimeMillis();

        // Remark: this is actually called twice, once in Syntax-Check-Mode and
        // once in the real run, but this should not matter
    }//</editor-fold>


    /**
     * Setter method for <code>m_GUI</code> to grant objects of the Device class
     * access to the GUI. Access to the GUI is thus far only needed by
     * <code>iC_Instrument</code> to virtually press the 'Pause' button.
     * @param GUI The <code>IcontrolView</code> object
     */
    // <editor-fold defaultstate="collapsed" desc="set GUI">
    public static void setGUI(IcontrolView GUI) {
        m_GUI = GUI;
    }//</editor-fold>

  
   
    /**
     * Default constructor that initializes some member variables.
     * Call <code>OpenInstrument</code> to establish a connection with the Instrument.<p>
     *
     * Note: Most of the <code>set...</code> methods should be called once from
     * the program 'above' to ensure proper function of the <code>Device</code> class.<p>
     * 
     * Note: If the arguments set by the above mentioned <code>set...</code> methods
     * were to be passed to the constructor, derived classes would be required
     * to override this constructor, which makes extending the functionality
     * more complex. Therefore, it was chosen to put the effort not to forget to
     * call any of the <code>set...</code> methods into programming the iC-Framework.<p>
     * TODO 1* understand why this is.
     *
     * Derived classes that override the default constructor should call
     * this constructor of the base class for consistency in case something
     * is added later.
     */
    // <editor-fold defaultstate="collapsed" desc="default constructor">
    public Device() {

        // Set Logger level to inherit level of parent logger
        // By using this.m_Logger it is made sure that the m_Logger instance
        // of the derived class is initialized, not m_Logger of the Device class
        this.m_Logger.setLevel(null);

        // init member variables
        m_StopScripting = false;

        // instantiate iC_Properties
        m_iC_Properties = new iC_Properties();
        
    }// </editor-fold>


    /**
     * Call this to stop processing the script. This 'signal' should be used
     * to stop any running threads (for instance in <code>iC_Instrument.MonitorChart</code>.
     *
     * {@link iC_Instrument#MonitorChart}
     * {@link Agilent4155#WaitUntilReady}
     */
    // <editor-fold defaultstate="collapsed" desc="Stop Scripting">
    public static void StopScripting() {

        // remember to stop sequencing in the Dispatcher
        m_StopScripting = true;

        /* Future implementations might traverse all Used Instruments and call
         * a method 'Stop'. This 'Stop' would be implementd in Device without
         * doing anything, and which could be overridden by Instruments that would
         * like to react _immediately_ to the Stop signal of the user instead
         * of having to poll the m_StopScripting flag.
         */
    }//</editor-fold>



    /**
     * After instantiating an object, this method is called from
     * <code>Dispatcher.HandleMakeCommand</code> to establish a connection to 
     * the Instrument. Currently, only GPIB, RS232 and LAN (TCP) communication is
     * implemented.<p>
     *
     * If this method is called while in Syntax-Check-Mode, the specialized 'OpenPort'
     * method for the chosen communication protocol (for instance <code>OpenGPIB</code>
     * is not called, because there is no need to check this syntax. However, the
     * chosen communication protocol is stored, in case some Instrument methods
     * perform a Syntax-Check which depends on the chosen communication protocol.<p>
     *
     * The specialized 'OpenPort' method is also not called if in No-Communication-Mode.<p>
     *
     * If a derived class needs extra initialization after IO communication was
     * established then override the <code>Open</code> method, which is also called
     * by <code>Dispatcher.HandleMakeCommand</code>; see the javadoc there.
     *
     * @param InstrumentName The Instrument-Name of this instance, for
     * instance 'Tsample', or 'PA'. At the time of writing it's only used in 
     * <code>LakeshoreTC.MonitorTemp</code>
     * @param CommPort Defines the port/protocol used to communicate with the
     * Instrument.
     * @param GPIBAddress The GPIB address of the instrument. For RS232 communication
     * this parameter is ignored.
     * 
     * @param ComPortName The name of the RS232 port, for instance 'COM1'. For
     * GPIB communication this parameter is ignored.
     * @param BaudRate The baud rate for RS232 communication. For GPIB communication
     * this parameter is ignored.
     * @param DataBits The number of data bits for RS232 communication. For GPIB communication
     * this parameter is ignored.
     * @param StopBits The number of stop bits for RS232 communication. For GPIB communication
     * this parameter is ignored.
     * @param Parity The parity for RS232 communication. For GPIB communication
     * this parameter is ignored.
     * 
     * @param InstrumentURL The URL (including protocol) of the instrument. Will be 
     * null if an other protocol than LAN has been selected
     *
     * @throws ScriptException when the selected communication port/protocol
     * has not been implemented; this should actually never occur
     *
     * @see CommPorts
     * @see Device#OpenGPIB
     */
    // <editor-fold defaultstate="collapsed" desc="OpenInstrument">
    public final void OpenInstrument(String InstrumentName, CommPorts CommPort, int GPIBAddress,
            String ComPortName, int BaudRate, int DataBits, int StopBits, String Parity,
            URL InstrumentURL)
            throws ScriptException {

        // remember the Instrument-Name
        m_InstrumentName = InstrumentName;

        // remember the communication port/protocol
        m_UsedCommPort = CommPort;
        
        // establish the connection to the instrument
        // if Syntax check is done and IO-communication desired
        if ( !inNoCommunicationMode() &&
             !m_SyntaxCheckMode) {
            switch (CommPort) {
                case none:
                    break;

                case GPIB:
                    OpenGPIB(GPIBAddress);
                    break;
                    
                case RS232:
                    OpenRS232(ComPortName, BaudRate, DataBits, StopBits, Parity);
                    break;
                    
                case LAN:
                    OpenLAN(InstrumentURL);
                    break;
                    
                default:
                    String str = "The chosen communication port/protocol is not supported.\n";
                    str += "Please select GPIB, RS232, LAN or implement other protocols.\n";
                    throw new ScriptException(str);
            }
        }
    }//</editor-fold>



    /** 
     * Establishes the connection to the instrument via GPIB bus using a class
     * derived from <code>GPIB_Driver</code>, that is <code>GPIB_NI</code>, 
     * <code>GPIB_Prologix</code> or <code>GPIB_IOtech</code>.<p>
     *
     * This specialized 'OpenPort' method is called from <code>OpenInstrument</code>
     * only if not in Syntax-Check Mode and if not in No-Communication Mode.<p>
     *
     * If additional initialization is required after IO communication has been
     * established, override <code>Device.Open</code>; see the javadoc there for more
     * details.<p>
     *
     * If the String returned by <code>getInstrumentNameIdentifier</code> is not empty, then the
     * response to a *IDN? query is used to ensure that the addressed instrument
     * is of the expected type (for instance it is a Lakeshore 340 and not an
     * Agilent 4155). <p>
     *
     *
     * @param GPIBAddress the GPIB address of the instrument
     *
     * @throws ScriptException when 1) the Instrument did not respond to a *IDN? query
     * with the expected answer (specified in <code>m_InstrumentNameIdentifier</code>)
     * and the user has chosen to change the script (not to enter No-Communication-Mode).
     *
     * @see Device#getInstrumentNameIdentifier()
     */
    // <editor-fold defaultstate="collapsed" desc="OpenGPIB()">
    protected final void OpenGPIB(int GPIBAddress)
              throws  ScriptException {

        // store GPIB Address
        m_GPIBAddress = GPIBAddress;


        ///////////////////
        // init GPIB_Driver
        // GPIB_NI and GPIB_Prologix
        try {
            if (m_GPIBcontroller == GPIBcontroller.NI) {
                // instantiate the GPIB driver
                // Note: m_GPIB_Driver will be null if an Exception is thrown in the constructor
                m_GPIB_Driver = new GPIB_NI();

            } else if (m_GPIBcontroller == GPIBcontroller.Prologix) {
                
                // instantiate the GPIB driver (using native drivers)
                // Note: m_GPIB_Driver will be null if an Exception is thrown in the constructor
                m_GPIB_Driver = new GPIB_Prologix(false);
                
            } else if (m_GPIBcontroller == GPIBcontroller.Prologix_Ethernet) {
                
                // instantiate the GPIB driver (using Ethernet access)
                // Note: m_GPIB_Driver will be null if an Exception is thrown in the constructor
                m_GPIB_Driver = new GPIB_Prologix(true);
                
            } else if (m_GPIBcontroller == GPIBcontroller.IOtech) {
                // instantiate the GPIB driver
                // Note: m_GPIB_Driver will be null if an Exception is thrown in the constructor
                m_GPIB_Driver = new GPIB_IOtech();
            }

            /* When an error occurred in the Constructor that does not throw
             * an Exception (for instance a static variable is not initialized
             * correctly) m_GPIB_Driver will be null and NullPointer Exception
             * is thrown below. Such errors are difficult to find, hence this
             * extra error checking. It turns out that the error check does not
             * really help, but well, it's there now ... And the default
             * Exception Handler doesn't work either...  */
            if (m_GPIB_Driver == null) {
                String str = "An unexpected error uccurred in the constructor of "
                        + "the GPIB_Driver.\n"
                        + "This event should only occur during the development phase.\n";
                throw new IOException(str);
            }


            // open the device
            m_GPIB_Driver.Open(GPIBAddress);


            // OLD: add a new lock if no instrument at this GPIB adr has already made a lock

            /* Remark: One Communication Driver is needed for every Instrument
             * as these instances store the GPIB address of the device.
             * m_GPIB_NI must, therefore, not be static */

            // log event in GPIB logger
            m_GPIB_Logger.log(Level.CONFIG, "Opened {0} @GPIB {1}\n",
                    new Object[]{m_InstrumentName, Integer.toString(m_GPIBAddress)});

        } catch (IOException ex) {

            // show a dialog to ask the user what to do
            String str = "The GPIB_Driver could not open the GPIB instrument at address ";
            str += Integer.toString(m_GPIBAddress) + ".\n\n";
            str += "GPIB_Driver's response:\n" + ex.getMessage() + "\n\n";
            str += "Would you like to enable the simulation mode without GPIB\n";
            str += "communication for this instrument?";

            m_Logger.log(Level.SEVERE, str);

            Integer ret = JOptionPane.showConfirmDialog(m_GUI.getComponent(), str,
                                "GPIB_Driver Error", JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE, m_iC_Properties.getLogoSmall()) ;

            // log user's response
            m_Logger.log(Level.SEVERE, "User selected option #{0}", Integer.toString(ret));

            if ( ret == 0) {
                // the user choose to enter simulation mode
                // so enable 'local' simulation mode (for this instrument only)
                m_NoCommunicationModeLocal = true;

            } else {
                str = "The GPIB_Driver could not open the GPIB instrument,\n"
                    + "and you did not enable No-Communication mode.\n"
                    + "Therefore, processing the script will be stoped now.\n"
                    + "Please establish communication with the Instrument and try again.\n";

                throw new ScriptException(str);
            }
        }
    }//</editor-fold>

    
    /** 
     * Establishes the connection to the instrument via RS232 using the class
     * <code>RS232_Driver</code>.<p>
     *
     * This specialized 'OpenPort' method is called from <code>OpenInstrument</code>
     * only if not in Syntax-Check Mode and if not in No-Communication Mode.<p>
     *
     * If additional initialization is required after IO communication has been
     * established, override <code>Device.Open</code>; see the javadoc there for more
     * details.<p>
     *
     * @param ComPortName The name of the RS232 port, for instance 'COM1'.
     * @param BaudRate The baud rate for RS232 communication.
     * @param DataBits The number of data bits for RS232 communication.
     * @param StopBits The number of stop bits for RS232 communication.
     * @param Parity The parity for RS232 communication. Can be 'none', 'even', 
     * 'odd', or 'mark' (case insensitive).
     * 
     * @throws ScriptException When the RS232 port could not be opened (bubbles up
     * from <code>OpenPort</code>).
     */
    // <editor-fold defaultstate="collapsed" desc="OpenRS232">
    protected final void OpenRS232(String ComPortName, int BaudRate, int DataBits,
                                   int StopBits, String Parity)
                    throws  ScriptException {
        
        // instantiate the RS232 driver and open the serial port
        // Note: m_RS232_Driver will be null if an Exception is thrown in the constructor
        m_RS232_Driver = new RS232_Driver();
        
        try {
        
            // open the serial port
            m_RS232_Driver.OpenPort(ComPortName, BaudRate, DataBits, StopBits, Parity);

            // log event in GPIB logger
            m_GPIB_Logger.log(Level.CONFIG, "Opened {0} @RS232 {1},{2},{3},{4},{5}\n",
                    new Object[]{m_InstrumentName, ComPortName, BaudRate, DataBits, StopBits, Parity});
            
        } catch (ScriptException ex) {

            // show a dialog to ask the user what to do
            String str = "The RS232_Driver could not open the instrument at" + ComPortName + "\n\n"
                + "RS232_Driver's response:\n" + ex.getMessage() + "\n\n"
                + "Would you like to enable the simulation mode without\n"
                + "communication for this instrument?";

            m_Logger.log(Level.SEVERE, str);

            Integer ret = JOptionPane.showConfirmDialog(m_GUI.getComponent(), str,
                                "RS232_Driver Error", JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE, m_iC_Properties.getLogoSmall()) ;

            // log user's response
            m_Logger.log(Level.SEVERE, "User selected option #{0}", Integer.toString(ret));

            if ( ret == 0) {
                // the user choose to enter simulation mode
                // so enable 'local' simulation mode (for this instrument only)
                m_NoCommunicationModeLocal = true;

            } else {
                str = "The RS232_Driver could not establish the network connection to the instrument,\n"
                    + "and you did not enable No-Communication mode.\n"
                    + "Therefore, processing the script will be stoped now.\n"
                    + "Please establish communication with the Instrument and try again.\n";

                throw new ScriptException(str);
            }
        }
    }//</editor-fold>
    
    
    /** 
     * Establishes the connection to the instrument via LAN (TCP) using Java's
     * support for TCP-IP communication.<p>
     *
     * This specialized 'OpenPort' method is called from <code>OpenInstrument</code>
     * only if not in Syntax-Check Mode and if not in No-Communication Mode.<p>
     *
     * If additional initialization is required after IO communication has been
     * established, override <code>Device.Open</code>; see the javadoc there for more
     * details.<p>
     *
     * @param InstrumentURL the URL of the instrument (IP address or host name). The
     * protocol, port number and file are defined in the iC.properties.
     *
     * @throws ScriptException when the network connection could not be established.
     *
     */
    // <editor-fold defaultstate="collapsed" desc="Open LAN">
    protected final void OpenLAN(URL InstrumentURL)
              throws  ScriptException {

        //////////////////
        // init LAN driver
        
        try {    
            // instantiate the LAN driver
            m_LAN_Driver = new LAN_Driver();

            // open the network connection
            m_LAN_Driver.OpenConnection(InstrumentURL);


            // log event in GPIB logger
            m_GPIB_Logger.log(Level.CONFIG, "Opened {0} @URL {1}\n",
                    new Object[]{m_InstrumentName, InstrumentURL});

        } catch (ScriptException ex) {

            // show a dialog to ask the user what to do
            String str = "The LAN_Driver could not open the instrument at\n"
                + InstrumentURL + "\n\n"
                + "LAN_Driver's response:\n" + ex.getMessage() + "\n\n"
                + "Would you like to enable the simulation mode without network\n"
                + "communication for this instrument?";

            m_Logger.log(Level.SEVERE, str);

            Integer ret = JOptionPane.showConfirmDialog(m_GUI.getComponent(), str,
                                "LAN_Driver Error", JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE, m_iC_Properties.getLogoSmall()) ;

            // log user's response
            m_Logger.log(Level.SEVERE, "User selected option #{0}", Integer.toString(ret));

            if ( ret == 0) {
                // the user choose to enter simulation mode
                // so enable 'local' simulation mode (for this instrument only)
                m_NoCommunicationModeLocal = true;

            } else {
                str = "The LAN_Driver could not establish the network connection to the instrument,\n"
                    + "and you did not enable No-Communication mode.\n"
                    + "Therefore, processing the script will be stoped now.\n"
                    + "Please establish communication with the Instrument and try again.\n";

                throw new ScriptException(str);
            }
        }
    }//</editor-fold>


    /**
     * A method for additional initializations after establishing the connection
     * to the Instrument. This method is called from <code>Dispatcher.HandleMakeCommand</code>
     * after calling <code>Openinstrument</code>.<p>
     *
     * Note that this method is not called when in Syntax-Check mode or when in
     * No-Communication mode!<p>
     *
     * When GPIB communication is selected, the default implementation assumes 
     * that the Instrument implements IEEE 488.2 (which is the case for most modern 
     * instruments) and sends *CLS to clear the Instrument's interface, and checks 
     * the type of Instrument by calling <code>checkIDN</code>. If a GPIB Instrument 
     * does not support IEEE 488.2 functions this method should be overridden in 
     * the derived class to avoid communication errors.<p>
     * 
     * If communication via RS232 is selected, the default implementation does 
     * nothing.
     *
     * @throws IOException when an sending / receiving data caused a GPIB error
     * @throws ScriptException when the response to a *IDN? query does not
     * match the expected result; bubbles up from <code>checkIDN</code>.
     */
    // <editor-fold defaultstate="collapsed" desc="Open">
    public void Open()
           throws IOException, ScriptException {

        ///////////////////////
        // GPIB communication ?
        if (m_UsedCommPort == CommPorts.GPIB) {

            // send Clear Interface command
            SendViaGPIB("*CLS");
            
            // check if the intended Instrument is addressed
            checkIDN();
        }       
    }//</editor-fold>


    /**
     * Reads the response to a *IDN? query if not in No-Communication mode and
     * stores it in the member variable <code>m_IDN</code>. If the global switch
     * <code>iC.EnableIDNcheck</code> in <code>iC.properties</code> is set to '1' 
     * then this method checks if the response to the *IDN? query matches the
     * expected response for the Instrument returned from
     * <code>getInstrumentNameIdentifier</code>. If this expected response is an
     * empty String (no InstrumentNameIdentifier has been defined in
     * <code>iC.properties</code> this method returns.<p>
     *
     * When the response to *IDN? does not match the expected result, the user
     * is presented with a dialog to choose an action.
     *
     * @throws ScriptException
     * @see icontrol.dialogs.WrongIDN
     */
    // <editor-fold defaultstate="collapsed" desc="check IDN">
    protected void checkIDN()
              throws ScriptException {


        ///////////////////////
        // GPIB communication ?
        if (m_UsedCommPort == CommPorts.GPIB) {
          
            try {

                // get *IDN? from the instrument
                if ( !inNoCommunicationMode() ) {
                    m_IDN = QueryViaGPIB("*IDN?");
                }

                // get global enable/disable IDN check switch
                boolean IDNcheck;
                if ( m_iC_Properties.getString("iC.EnableIDNcheck", "1").equals("1") ) {
                    IDNcheck = true;
                } else {
                    IDNcheck = false;
                }


                // return if there is nothing to do
                if ( getInstrumentNameIdentifier().isEmpty() ||
                     inNoCommunicationMode() ||
                     IDNcheck == false  ) {
                    return;
                }

                

            } catch (IOException ex) {
                m_Logger.log(Level.SEVERE, "Querying *IDN? failed in checkIDN\n", ex);

                // querying the Instrument for it's name failed, hence
                // assign a meaningful String to variable idn, which is
                // recognized by an error in the check below
                m_IDN = "The Instrument did not respond to a *IDN? query";
            }

            // check if it contains the Instrument Identifier
            if (!m_IDN.contains(getInstrumentNameIdentifier())) {
                String str = "The instrument at GPIB address " + m_GPIBAddress;
                str += " does not appear to be of the correct type, or ";
                str += "it did not respond to '*IDN?' in time.\n\n";
                str += "Expected response: " + getInstrumentNameIdentifier() + "\n";
                str += "Received response: " + m_IDN + "\n\n";
                str += "Please correct the GPIB address or the Instrument type, and\n";
                str += "make sure it is accessible.";

                // make a new WrongIDN object
                WrongIDN dlg = new WrongIDN(m_GUI.getFrame(), str, m_IDN,
                                            getInstrumentNameIdentifier(),
                                            this.getClass().toString());

                // ask the user what to do
                int ret = dlg.AskUser();

                // check the User's answer
                if ( ret == dlg.ACTION_STOP) {
                    // the user choose to change the script, hence, exit by
                    // throwing a ScriptException
                    throw new ScriptException("Please correct the GPIB Address or the Instrument Type, and\n make sure it is accessible.\n");
                } else if (ret == dlg.ACTION_NO_COMMUNICATION) {
                    // the user choose to enter simulation mode
                    // so enable 'local' simulation mode (for this instrument only)
                    m_NoCommunicationModeLocal = true;
                }
                // the user has selected to proceed with the present instrument
            }
        }
    }//</editor-fold>


    /**
     * This method is called one time after the Script has been processed (from
     * <code>Dispatcher.run</code>), and is used to close the connection to
     * the Communication driver. If new drivers (GPIB, USB, Ethernet, ...) are added to
     * iC, this method should be updated, but typically this method does not
     * need to be overridden, so it is declared final.
     *
     * @throws IOException Bubbles up from the Communication driver's
     * <code>CloseController</code> method (GPIB_NI or GPIB_Prologix or FTD2xxj)
     */
    // <editor-fold defaultstate="collapsed" desc="Close Controller">
    public final void CloseController()
                 throws IOException {

        // close connection to the GPIB driver (GPIB_NI and GPIB_Prologix)
        if (m_GPIB_Driver != null) {
            m_GPIB_Driver.CloseController();
            IcontrolView.DisplayStatusMessage("Closed the connection to GPIB_Driver.\n");
        }
    }//</editor-fold>


    /**
     * This method is called for all the used Instruments after processing
     * the script. It can be used to "clean up" the Instrument state. The 
     * connection to the Instrument is closed in <code>CloseInstrument</code>,
     * which is called from <code>Dispatcher.run</code> after processing the
     * script has been finished. <code>CloseInstrument</code> tries to switch 
     * the Instrument to 'local' mode.<p>
     * 
     * Note that this method is also called when an error occurred during opening
     * of the instrument. It might therefore be necesary to check if the driver 
     * used to communicate with the instrument is valid.
     *
     * Remark: This method is not called when in Syntax-Check mode or when in
     * NoCommunication Mode.
     *
     * @throws IOException When a GPIB error occurred during closing the connection
     * to the Instrument
     *
     * @see Device#CloseInstrument()
     */
    // <editor-fold defaultstate="collapsed" desc="Close">
    public void Close()
           throws IOException {

        // the default implementation performes no task and simply returns
    }//</editor-fold>


    /**
     * This method is called from <code>Dispatcher.run</code> for all the used
     * Instruments after processing the script. It closes the connection
     * to Instrument. It is usually not necessary to override this method,
     * therefore it is defined final. If a new GPIB/USB/Ethernet/... driver
     * is added to iC, this method should also be updated.<p>
     *
     * This method calls <code>CloseInstrument</code> in the driver class (for
     * instance <code>GPIB_NI</code> and <code>GPIB_Prologix</code>). These
     * methods try to switch the Instrument to 'local' mode and then close
     * the connection. For RS232 communication it closes the serial port, and for
     * LAN communication it closes the input/output streams to the URL.<p>
     *
     * Remark: This method is not called when in Syntax-Check mode or when in
     * NoCommunication Mode.
     *
     * @throws IOException When a GPIB error occurred during closing the connection
     * to the Instrument
     *
     * @see Device#CloseInstrument()
     */
    // <editor-fold defaultstate="collapsed" desc="Close Instrument">
    public void CloseInstrument()
           throws IOException {

        // is a GPIB driver used?
        if (m_GPIB_Driver != null) {

            // close connection to the Instrument
            m_GPIB_Driver.CloseInstrument();
        }
        
        // is a RS232 driver used?
        if (m_RS232_Driver != null) {

            // close the serial port
            m_RS232_Driver.ClosePort();
        }
        
        // is a LAN connection used?
        if (m_LAN_Driver != null) {
            
            // close network connection (respectively the in/out streams)
            m_LAN_Driver.CloseConnection();
        }
    }//</editor-fold>


    /**
     * Setter method for the HashMap that stores all used instruments.
     *
     * The HashMap <code>UsedInstruments</code> is created in Dispatcher.
     * Some instruments might require access to other instruments, when
     * they, for instance, save data to a file and want to append the
     * actual temperature measured by an other instrument to the file name.
     * This
     *
     * @param UsedInstruments Holds the instances of all used Instruments
     */
    // <editor-fold defaultstate="collapsed" desc="set Used Instruments">
    public static void setUsedInstruments(HashMap<String, Device> UsedInstruments) {
        Device.m_UsedInstruments = UsedInstruments;
    } //</editor-fold>


    /**
     * Setter method for the Instrument-Class-Name.
     *
     * @param InstrumentClassName The Instrument-Class-Name for the particular
     * instance.
     */
    // <editor-fold defaultstate="collapsed" desc="set Instrument Class Name">
    public void setInstrumentClassName(String InstrumentClassName) {
        m_InstrumentClassName = InstrumentClassName;
    }//</editor-fold>
    
  
    /**
     * Determines if this instance of the instrument is in simulation mode
     * where no GPIB communication takes place. An instrument can be in
     * simulation mode when either the global simulation mode is enabled,
     * or this instrument is specifically set to simulation mode during
     * instantiation.
     *
     * @return <code>true</code> if this instrument is in simulation mode
     */
    // <editor-fold defaultstate="collapsed" desc="inNoCommunicationMode() ?">
    public final boolean inNoCommunicationMode() {
        if (    m_NoCommunicationModeLocal == true ||
                m_NoCommunicationModeGlobal == true ) {
            return true;
        } else
            return false;
    }//</editor-fold>


    /**
     * This method calls @link{IcontrolView.isPaused}. See the javadoc
     * for this method.
     *
     * @param WaitUntilNotPaused see @link{IcontrolView.isPaused}
     * @return see @link{IcontrolView.isPaused}
     */
    // <editor-fold defaultstate="collapsed" desc="isPaused()">
    public boolean isPaused(boolean WaitUntilNotPaused) {

        // call corresponding method in IcontrolView
        return m_GUI.isPaused(WaitUntilNotPaused);

    }//</editor-fold>
    
    
    /**
     * Utility method:
     * Fills an ArrayList with values starting for <code>Start</code> to <code>Stop</code>
     * with either equidistant or logarithmically spaced values. If
     * <code>AppendToList==null</code> a new list is created, otherwise the new
     * values are appended to the specified list.
     *
     * @param Start The start value of the new data series.
     * @param Stop The stop value of the new data series.
     *
     * @param Increment When <code>Logarithmic==false</code> this value is the step
     * size used to increment the equally spaced values; if logarithmic spacing
     * is chosen, this value determines the number of values per decade. The sign
     * of <code>Increment</code> is automatically adjusted to fit the start and
     * stop values.
     *
     * @param Logarithmic When set to <code>false</code>, linearly spaced values
     * are generated, else the values are logarithmically spaced.
     *
     * @param AppendToList When different from <code>null</code> the generated
     * values are appended to this <code>ArrayList</code>.
     *
     * @param DecimalPlaces If this number is different from 0, the generated values
     * are rounded to the number of digits after the comma specified in
     * <code>DecimalPlaces</code>.
     *
     * @return The <code>ArrayList<Double></code> with the generated values.
     *
     * @throws ScriptException When 1) <code>Increment</code> is 0, or 2) the sign
     * of <code>Start</code> differs from the one of <code>Stop</code> when
     * logarithmic spacing is chosen.
     */
    // <editor-fold defaultstate="collapsed" desc="GenerateValues">
    protected final ArrayList<Double> GenerateValues( double Start, double Stop, double Increment,
                                                boolean Logarithmic,
                                                ArrayList<Double> AppendToList,
                                                int DecimalPlaces)
                    throws ScriptException {

        // local variables
        ArrayList<Double> ret;

        // make new ArrayList if not specified to append to an existing one
        if (AppendToList == null) {
            ret = new ArrayList<Double>();
        } else
            ret = AppendToList;

        // calculate the factor used to round the values to the specified
        // number of digits after the comma
        double  RoundingFactor = Math.pow(10, DecimalPlaces);


        // return Start if Start == Stop
        if (Start == Stop) {
            if (DecimalPlaces > 0) {
                    // add the rounded value
                    ret.add( Math.floor(Start*RoundingFactor) / RoundingFactor );
                } else {
                    // add the 'full-length' value
                    ret.add(Start);
                }
            return ret;
        }

        // throw an exception if Increment is 0
        if (Increment == 0) {
            String str = "GenerateValues: The parameter 'Increment' must not be 0.";
            throw new ScriptException(str);
        }

        
        // lin or log ?
        if (Logarithmic == false) {
            /////////////////
            // Linear spacing

            // ensure sign of Increment is correct
            if ( Start < Stop ) {
                if ( Math.signum(Increment) < 0 )
                    Increment *= -1.0;
            } else {
                if ( Math.signum(Increment) > 0 )
                    Increment *= -1.0;
            }

            // add the new values
            for (double t=Start; t<=Stop; t += Increment) {
                if (DecimalPlaces > 0) {
                    // add the rounded value
                    ret.add( Math.floor(t*RoundingFactor) / RoundingFactor );
                } else {
                    // add the 'full-length' value
                    ret.add(t);
                }
            }
        } else {
            //////////////
            // log spacing

            // ensure sign of Increment is positive
            if ( Math.signum(Increment) < 0 )
                    Increment *= -1.0;

            // ensure the sign of start and stop are the same
            if ( Math.signum(Start) != Math.signum(Stop) ) {
                String str = "For logarithmic spacing, the sign of Start and Stop must be the same.\n";
                throw new ScriptException(str);
            }

            // calc the scaling factor
            double q = Math.pow(10, 1.0/Increment);

            /* calc the correction factor:
             * due to rounding error, it can happen that the last data point is not
             * added (e.g. 100.00000000006 is larger than 100.0). To avoid this,
             * an epsilon value is added to the Stop value.
             */
            double eps = 0.0001;
            if (DecimalPlaces > 0) {
                // make eps one order of magnitude lower than the desired accuracy
                eps = 1.0 / Math.pow(10, DecimalPlaces+1);
            }


            // add the new values
            for (double t=Start; t <= (Stop+eps); t *= q) {
                if (DecimalPlaces > 0) {
                    // add the rounded value
                    ret.add( Math.floor(t*RoundingFactor) / RoundingFactor );
                } else {
                    // add the 'full-length' value
                    ret.add(t);
                }
            }
        }

        return ret;
    }//</editor-fold>


    /**
     * Processes a Command-Line that addresses an Instrument. It first identifies
     * the Instance of the addressed Instrument, then finds the method implementing
     * the command, and calls this method with the appropriate parameters.<p>
     *
     * The <code>DispatchCommand</code> of this base class for other
     * Instrument classes uses Reflection to find a method (in the base and in derived
     * classes) with a name and the number of parameters as specified in the
     * Script (Device Command). If such a method is found, it tries to
     * convert the specified parameters into the appropriate variable types
     * and calls the method. The method name (Device Command) is case sensitive.
     * Two methods with the same name can exist as long as the number of
     * parameters is different.<p>
     *
     * Note that only those device commands can be handled that take arguments
     * of type <code>String</code>, <code>Integer</code>, <code>Double</code>,
     * <code>Float</code>, or <code>Boolean</code>. <p>
     *
     * Remark: This method can also be invoked from within methods in Instrument
     * Classes to, for instance, get the current temperature of an Instrument to
     * append this value to the file name.<p>
     *
     * Remark: Only methods of classes derived from <code>Device</code> are
     * considered here. This can be useful, because the Reflection mechanism
     * also lists methods such as, for instance, java.lang.Object wait(), while
     * an Instrument class might also implement a wait() method. If this requirement
     * should be lifted, see the remarks in <code>Dispatcher</code> at the
     * bookmark BM_LiftRequirement. <p>
     *
     * Remark: Any exception thrown by the invoked method (the method in the
     * Instrument Class which handles the Device Command) should also be handled
     * by the invoked method, because no code exists 'above' that which could
     * handle the Exception better.<p>
     *
     * Remark: This method is <code>static</code> because it is called from the
     * <code>Dispatcher</code> class without a specific object. It would of course
     * also be possible to create a dummy object in the Dispatcher. I don't know
     * which way is more elegant, so I chose to make this method <code>static</code>.
     * This also required to make <code>getInteger</code> etc. static.<p>
     *
     * Developing Remark: Originally this method was made static, but when multiple
     * Command Lines are dispatched it's probably safer not to overwrite the variables
     * used in <code>DispatchCommand</code>. I did not quite get my head around that,
     * but I remove the static for now, and keep thinking. See also the remark in
     * javadoc (How to write new Instrument-Classes).<p>
     *
     * @param CommandLine to be dispatched and processed. The first Token in the
     * Command Line contains the Instrument Name, and the remainder contains the
     * Instrument Command and the Parameters. An example would be the String
     * "Tstage SetTemp; 325.0" or "PA Measure; 0; Id,Vd,Ig,Vg; .trans"
     *
     * @return The <code>Object</code> that is returned by the method called by the
     * Reflection mechanism. See remark above.
     * IMPORTANT: The returned <code>Object</code> is <code>null</code> when in
     * Syntax-Check mode or when the returned <code>Object</code> is null. Always
     * perform a check for <code>!null</code> when using this return value!
     *
     * @throws ScriptException is thrown when 
     * 1) the specified Instrument Name was not recognized, or
     * 2) the Script-Command did not contain a Device-Command, or
     * 3) none or more than one functions with the same name and number of arguments are found, or
     * 4) when an error occurred when the strings were converted into the appropriate
     * variable type, or
     * 5) when the Syntax-check failed, or
     * 6) when the invoked method threw an Exception.
     *
     */
    // <editor-fold defaultstate="collapsed" desc="Dispatch Command">
    public final Object DispatchCommand(String CommandLine)
                 throws ScriptException  {

        //////////////////
        // local variables

        Device      DeviceInstance;         // the instance of the addressed Instrument
        boolean     MethodCalled = false;   // flag to remember if a method was invoked
        

        // split the Command Line into it's Tokens
        ArrayList<String> Tokens = Dispatcher.Tokenizer( CommandLine );

        
        ////////////////////////////////
        // let's use nice variable names
        
        // the Instrument Name (e.g. Tsample)
        String InstrumentName = Tokens.get(0);

        // optional Arguments (all other Tokens)
        ArrayList<String> Arguments = new ArrayList<String>(Tokens);
        Arguments.remove(0);
        
        // the name of the Script-Method
        String  ScriptMethodName;
        if (Arguments.size() > 0) {
            ScriptMethodName = Arguments.get(0);
        } else {
            // the Argument List does not contain a Device-Command
            String str = "The Script-Command is invalid.\n"
                    + "Please provide a Device-Command (e.g. SetTemp).\n";
            
            throw new ScriptException(str);
        }
        
        // number of arguments to pass to the Script-Method
        int NrArguments = Arguments.size() - 1;



        //////////////////////////////////////////
        // does the Command address an Instrument?
        if(m_UsedInstruments.containsKey(InstrumentName)) {

            // get the object referred to by the Instrument-Name
            DeviceInstance = m_UsedInstruments.get(InstrumentName);

        } else {
            // the command line was not recognized, so throw a ScriptException
            String str = "The Instrument Name '" + InstrumentName +"' is not recognized.\n";
            str += "Please check the spelling.\n";

            throw new ScriptException (str);
        }

        
        /////////////////////////
        // find the Script-Method
        
        // this throws an Exception if not exacly one suitable method was found
        ScriptMethod met = DeviceInstance.findScriptMethod(ScriptMethodName, NrArguments);


        //////////////////////////
        // generate parameter list
        
        // use shorter variable names
        Class<?>[] Types = met.ParameterTypes;
        
        // make an array of actual parameters having the correct types
        Object[] ConvertedParameters= new Object[ Types.length ];


        // for all types convert the passed parameters from String to the
        // appropriate type.
        for (int i=0; i<Types.length; i++) {

            // is type an Integer?
            if ( Types[i] == Integer.TYPE ) {
                ConvertedParameters[i] = getInteger(Arguments.get(i+1)) ;
            }

            // is type an Double?
            else if(Types[i] == Double.TYPE) {
                ConvertedParameters[i] = getDouble(Arguments.get(i+1)) ;
            }

            // is type an Float?
            else if(Types[i] == Float.TYPE) {
                ConvertedParameters[i] = getFloat(Arguments.get(i+1)) ;
            }

            // is type a Boolean?
            else if(Types[i] == Boolean.TYPE) {
                ConvertedParameters[i] = getBoolean(Arguments.get(i+1)) ;
            }

            // is type an String?
            else if(Types[i] == String.class) {
                ConvertedParameters[i] = Arguments.get(i+1) ;
            }

            else {
                // no supported data type was found
                String str = "An unexcepted error occurred in Device.DispatchCommand().\n"
                    + "The selected data type '" + Types[i].toString() + "' is not supported.\n"
                    + "Tried to call '" + ScriptMethodName + "'.\n"
                    + "Please consider reporting this incident to the developer and\n"
                    + "include the log file and your script.\n";

                throw new ScriptException(str);
            }
        }

        
        try {
            // call the method when not in SyntaxCheckMode, or the
            // method performes a syntax ckeck
            //if (m_SyntaxCheckMode == false || MethodChecksSyntax==true) {
            if (m_SyntaxCheckMode == false || met.MethodChecksSyntax == true) {

                //////////////////////
                // now call the method
                
                if ( met.isGenericGPIB ) {
                    // execute generic command
                    m_LastReturnValue = DeviceInstance.invokeGenericScriptMethod(
                            met, ConvertedParameters);
                    
                } else {
                    // use Reflection
                    m_LastReturnValue = met.ReferenceToMethod.invoke(
                            DeviceInstance, ConvertedParameters);
                }
            }

            // remember that a method was called
            MethodCalled = true;

        } catch (InvocationTargetException ex) {

            // check if a SyntaxCheck Error occurred and re-wrap it as ScriptException
            if (ex.getCause().getClass() == DataFormatException.class) {
                // The method threw a DataFormatException (while in SyntaxCheckMode),
                // so the syntax-check went wrong. Throw a ScriptException with the
                // message from the DataFormatException to display the message
                // and stop processing the script.

                throw new ScriptException(ex.getCause().getMessage() + "\n");
            }

            /*
             * the underlying method threw an exception, hence, re-throw it as
             * a ScriptException. Because this exception cannot really be
             * handled by any code 'above' that recievs this exception, any exception
             * should ideally be handled in the method which was just called.
             */
            
            // log stack trace
            m_Logger.severe(Dispatcher.printStackTrace(ex));

            String str = "The device command '" + Arguments.get(0) + "' caused an error.\n"
                + "Please check the error message and/or the log-file\n"
                + "and try to correct the error.\n\n"
                + ex.getCause().getMessage() + "\n"
                + ex.toString() + "\n";

            ScriptException dummy = new ScriptException(str);
            dummy.initCause(ex);

            // re-throw as ScriptException
            throw dummy;

        } catch (Exception ex) {
            // log stack trace
            m_Logger.severe(Dispatcher.printStackTrace(ex));

            String str = "An Exception occurred in Device.DispatchCommand().\n"
                + "Please report this incident to the developer and\n"
                + "include the log file and your script.\n";

            // re-throw as ScriptException
            throw new ScriptException(str);
        }


        // no method was called
        if (MethodCalled == false) {
            
            String str = "The instrument '" + InstrumentName + "' does not\n"
                + "handle the command '" + Arguments.get(0) + "'.\n"
                + "Choose an other instrument, a different command, or\n"
                + "implement the command.\n";

            throw new ScriptException(str);
        }

        return m_LastReturnValue;
    }//</editor-fold>


    
    /**
     * Converts a String into an Integer.
     * If a conversion error occurs it generates a ScriptException which is easy
     * to understand for the user.<p>
     *
     * Remark: This method needs to be static because it is called from
     * <code>DispatchCommand</code>, which needs to be static because it
     * is called from the <code>Dispatcher</code> class without a specific
     * object.
     *
     * @param str - String to convert
     * @return The corresponding Integer value
     * @throws ScriptException when parsing the String was unsuccessful
     *
     */
    // <editor-fold defaultstate="collapsed" desc="getInteger">
    static public int getInteger(String str)
                throws ScriptException {

        // some Instruments return a string with a leading '+' sign, which
        // is not recognized by parseInt, so remove it
        str = str.replaceFirst("[+]", "");

        try {
            return Integer.parseInt(str);

        } catch (NumberFormatException ex) {
            String dummy = "Error converting the argument " + str + "\n" + "into an Integer value.\n";
            dummy += "Please correct this parameter and try again.\n";

            // log event
            m_Logger.severe(str);

            throw new ScriptException(dummy);
        }
    }//</editor-fold>

    // <editor-fold defaultstate="collapsed" desc="getDouble">
    /**
     * Converts a String into a Double.
     * If a conversion error occurs it generates a ScriptException which is easy
     * to understand for the user.<p>
     * 
     * This method is public to be accessible from <code>scriptMethod</code>.
     *
     * @param str - String to convert
     * @return The corresponding double value
     * @throws ScriptException when parsing the String was unsuccessful
     *
     */
    static public double getDouble(String str)
                throws ScriptException {

        try {
            return Double.parseDouble(str);

        } catch (NumberFormatException ex) {
            String dummy = "Error converting the argument " + str + "\n" + "into a Double value.\n";
            dummy += "Please correct this parameter and try again.\n";

            // log event
            m_Logger.severe(str);

            throw new ScriptException(dummy);
        }
    }//</editor-fold>

    // <editor-fold defaultstate="collapsed" desc="getFloat">
    /**
     * Converts a String into a Float.
     * If a conversion error occurs it generates a ScriptException which is easy
     * to understand for the user.
     *
     * @param str - String to convert
     * @return The corresponding double value
     * @throws ScriptException when parsing the String was unsuccessful
     *
     */
    static protected float getFloat(String str)
                throws ScriptException {

        try {
            return Float.parseFloat(str);

        } catch (NumberFormatException ex) {
            String dummy = "Error converting the argument " + str + "\n" + "into a Float value.\n";
            dummy += "Please correct this parameter and try again.\n";
            dummy += "This error might also be caused by an unexpected response from an Instrument.";

            // log event
            m_Logger.severe(str);

            throw new ScriptException(dummy);
        }
    }//</editor-fold>

    // <editor-fold defaultstate="collapsed" desc="getBoolean">
    /**
     * Converts a String into a Boolean.
     * If a conversion error occurs it generates a ScriptException which is easy
     * to understand for the user.
     *
     * @param str - String to convert
     * @return The corresponding Boolean value
     * @throws ScriptException when parsing the String was unsuccessful
     *
     */
    static protected boolean getBoolean(String str)
                throws ScriptException {

        try {
            return Boolean.parseBoolean(str);

        } catch (NumberFormatException ex) {
            String dummy = "Error converting the argument " + str + "\n" + "into a Boolean value.\n";
            dummy += "Please correct this parameter and try again.\n";

            // log event
            m_Logger.severe(str);

            throw new ScriptException(dummy);
        }
    }//</editor-fold>



    
    /**
     * Sends a string via GPIB to the instrument if it is not in
     * No-Communication-Mode. The String to send is in any case logged
     * to <code>m_GPIB_Logger</code>.<p>
     *
     * This method uses a Lock on the GPIB controller to ensure thread safety.
     * Note: Declaring the method as synchronized resulted in communication errors when
     * different threads tried to access the same instrument.<p>
     *
     * Remark: To prevent unintended behavior on non-English environments (where
     * the decimal point might not be '.' but ',') it is recommended to use the
     * <code>Locale.US</code>, for instance like <code>String.format(Locale.US, ...</code>.
     *
     * @param Message is sent to the GPIB
     * @throws IOException if the transmission caused a GPIB error; bubbles
     * up from <code>GPIB_Driver.Send</code> or if a driver not derived from
     * <code>GPIB_Driver</code> was used (should only occur during development).
     */
    // <editor-fold defaultstate="collapsed" desc="SendMessage GPIB">
    protected final void SendViaGPIB(String Message)
                    throws IOException {

        // log message and return if in No-Communication Mode
        if (inNoCommunicationMode()) {
            // log message
            m_GPIB_Logger.log(Level.FINE, "{0} <n {1}: {2}", new Object[]{
                m_TimeStampFormat.format(Calendar.getInstance().getTime()) , m_InstrumentName, Message});

            return;
        }

        // send via driver derived from GPIB_Driver
        if (m_GPIB_Driver instanceof GPIB_Driver) {
        //if (m_GPIB_Driver.getClass().getSuperclass() == GPIB_Driver.class) {

            // lock the Lock
            m_LockGPIBController.lock();
            
            try {
                // log the message
                m_GPIB_Logger.log(Level.FINE, "{0} <- {1}: {2}", new Object[]{
                    m_TimeStampFormat.format(Calendar.getInstance().getTime()), m_InstrumentName, Message});

                // send the message
                m_GPIB_Driver.Send(Message);

            } catch (IOException ex) {

                String str = "An error occurred during GPIB communication.\n";
                str += ex.getMessage() + "\n";

                // log event
                m_Logger.severe(str);

                // log GPIB Status to GPIB logger
                m_GPIB_Logger.severe(m_GPIB_Driver.getGPIBStatus());

                // TODO 4* ask User what to do. retry, ignore, stop

                throw new IOException(str, ex);
            } finally {
                // finally is also called when a new Exception is thrown in catch{}

                // release the Lock
                m_LockGPIBController.unlock();
            }
        } else {
            String str = "The employed GPIB driver was not derived from GPIB_Driver.\n"
                    + "Please adapt Device.SendViaGPIB accordingly.\n"
                    + "This error should only occur during develoment.\n";
            
            throw new IOException(str);
        }
    }//</editor-fold>



    /**
     * Sends <code>ByteBuffer Message</code> via RS232 to the instrument if it is not in
     * No-Communication-Mode. The bytes to send are in any case logged
     * to <code>m_GPIB_Logger</code>.<p>
     * 
     * This method uses a Lock on the RS232 controller to ensure thread safety.
     * See remark in <code>SendViaGPIB</code>.<p>
     *
     * @param Message is sent over the RS232.
     * 
     * @throws IOException if the transmission caused an error; bubbles
     * up from <code>RS232_Driver.Send</code>.
     */
    // <editor-fold defaultstate="collapsed" desc="Send Via RS232 (ByteBuffer)">
    protected final void SendViaRS232(ByteBuffer Message) 
                    throws IOException {

        // build a string representation of Message
        String MessageString = RS232_Driver.ByteBufferToString(Message);

        
        // log message and return if in No-Communication Mode
        if (inNoCommunicationMode()) {
            // log message
            m_GPIB_Logger.log(Level.FINE, "{0} <n {1}: {2}", new Object[]{
                m_TimeStampFormat.format(Calendar.getInstance().getTime()) , 
                m_InstrumentName, MessageString});

            return;
        }

        // lock the Lock
        m_LockRS232Port.lock();
        
        try {
            // log the message
            m_GPIB_Logger.log(Level.FINE, "{0} <- {1}: {2}", new Object[]{
                m_TimeStampFormat.format(Calendar.getInstance().getTime()), m_InstrumentName, MessageString});

            // send the message
            m_RS232_Driver.Send(Message.array());

        } catch (IOException ex) {

            // log event
            m_Logger.severe(ex.getMessage());

            // log GPIB Status to GPIB logger
            m_GPIB_Logger.severe(ex.getMessage());

            // TODO 4* ask User what to do. retry, ignore, stop

            throw ex;
            
        } finally {
            // finally is also called when a new Exception is thrown in catch{}

            // release the Lock
            m_LockRS232Port.unlock();   
        }
    }//</editor-fold>
    
      
    /**
     * Sends a string via RS232 to the instrument if it is not in
     * No-Communication-Mode. The String to send is in any case logged
     * to <code>m_GPIB_Logger</code>. To convert the string into 8-bit values
     * the character set UTF-8 is used.<p>
     *
     * Remark: To prevent unintended behavior on non-English environments (where
     * the decimal point might not be '.' but ',') it is recommended to use the
     * <code>Locale.US</code>, for instance like <code>String.format(Locale.US, ...</code>.
     *
     * @param Message is sent over the RS232. Conversion from <code>String</code>
     * to <code>byte[]</code> is done using the UTF-8 character set.
     * 
     * @throws IOException if the transmission caused an error; bubbles
     * up from <code>RS232_Driver.Send</code>.
     */
    // <editor-fold defaultstate="collapsed" desc="Send via RS232 (String)">
    protected final void SendViaRS232(String Message) 
                    throws IOException {
        
        // Because the format of the log message is different when Message
        // is a String versus a byte[], this code is somewhat doulbed instead
        // of calling SendViaRS232(byte[])
        
        // log message and return if in No-Communication Mode
        if (inNoCommunicationMode()) {
            // log message
            m_GPIB_Logger.log(Level.FINE, "{0} <n {1}: {2}", new Object[]{
                m_TimeStampFormat.format(Calendar.getInstance().getTime()) , 
                m_InstrumentName, Message});

            return;
        }

        // lock the Lock
        m_LockRS232Port.lock();
        
        try {
            // log the message
            m_GPIB_Logger.log(Level.FINE, "{0} <- {1}: {2}", new Object[]{
                m_TimeStampFormat.format(Calendar.getInstance().getTime()), m_InstrumentName, Message});

            // send the message
            m_RS232_Driver.Send( Message.getBytes(Charset.forName("UTF-8")) );

        } catch (IOException ex) {

            // log event
            m_Logger.severe(ex.getMessage());

            // log GPIB Status to GPIB logger
            m_GPIB_Logger.severe(ex.getMessage());

            // TODO 4* ask User what to do. retry, ignore, stop

            throw ex;
            
        } finally {
            // finally is also called when a new Exception is thrown in catch{}

            // release the Lock
            m_LockRS232Port.unlock();   
        }
    }//</editor-fold>

    
    /**
     * Sends a string via LAN (TCP) to the instrument if it is not in
     * No-Communication-Mode. The String to send is in any case logged
     * to <code>m_GPIB_Logger</code>.<p>
     *
     * This method uses a Lock on the GPIB controller to ensure thread safety.
     * Note: Declaring the method as synchronized resulted in communication errors when
     * different threads tried to access the same instrument.<p>
     *
     * Remark: To prevent unintended behavior on non-English environments (where
     * the decimal point might not be '.' but ',') it is recommended to use the
     * <code>Locale.US</code>, for instance like <code>String.format(Locale.US, ...</code>.
     *
     * @param Message is sent to the GPIB
     * @throws IOException if the transmission caused a GPIB error; bubbles
     * up from <code>GPIB_Driver.Send</code>.
     */
    // TODO @LAN implement SendViaLAN()
    // I recommend to log to the GPIB logger although the name is misleading.
    // not sure if String is the best data type since I never used LAN communication for my instruments
    // you might not need the Lock-ing mechanism
    // <editor-fold defaultstate="collapsed" desc="Send Via LAN">
    protected final void SendViaLAN(String Message)
                    throws IOException {
        
        // just display an "error" message
        IcontrolView.DisplayStatusMessage("Sending via LAN is not yet supported.", false);
        
        // see SendViaGPIB for an example code

        
    }//</editor-fold>

    
    /**
     * Sends a string via GPIB to the Instrument, and reads the Instrument's
     * reply. Does that only if the Instrument is not in No-Communication-Mode.
     * The String to send and a possible answer is logged to
     * <code>m_GPIB_Logger</code>.<p>
     *
     * This method uses a Lock on the GPIB controller to ensure thread safety.
     * Note: Declaring the method as synchronized resulted in communication errors when
     * different threads tried to access the same instrument.<p>
     *
     * Remark: To prevent unintended behavior on non-English environments (where
     * the decimal point might not be '.' but ',') it is recommended to use the
     * <code>Locale.US</code>, for instance like <code>String.format(Locale.US, ...</code>.
     *
     * @param Message is sent over the GPIB.
     * @return Returns the string read from GPIB, or an empty String if
     * in No-Communication-Mode.
     * @throws IOException if the transmission caused a GPIB error; bubbles
     * up from <code>GPIB_Driver.Send</code> or <code>GPIB_Driver.Receive</code>.
     */
    // <editor-fold defaultstate="collapsed" desc="Query Via GPIB">
    protected final String QueryViaGPIB(String Message)
                    throws IOException {

        // returned value if in No-Communication Mode
        String ret = "";

        // log message and return if in No-Communication Mode
        if (inNoCommunicationMode()) {
            // log message
            m_GPIB_Logger.log(Level.FINE, "{0} <n {1}: {2}", new Object[]{
                m_TimeStampFormat.format(Calendar.getInstance().getTime()), m_InstrumentName, Message});

            return ret;
        }

        // send via driver derived from GPIB_Driver
        if (m_GPIBcontroller == GPIBcontroller.NI ||
            m_GPIBcontroller == GPIBcontroller.Prologix) {

            // lock the Lock
            m_LockGPIBController.lock();
            
            try {
                // log message
                m_GPIB_Logger.log(Level.FINE, "{0} <- {1}: {2}", new Object[]{
                    m_TimeStampFormat.format(Calendar.getInstance().getTime()), m_InstrumentName, Message});

                // write to Instrument
                m_GPIB_Driver.Send(Message);

                // read from Instrument
                ret = m_GPIB_Driver.Receive(true);

                // log received GPIB traffic
                m_GPIB_Logger.log(Level.FINE, "{0} -> {1}: {2}", new Object[]{
                    m_TimeStampFormat.format(Calendar.getInstance().getTime()), m_InstrumentName, ret});

            } catch (IOException ex) {

                String str = "An error occurred during GPIB communication.\n";
                str += ex.getMessage() + "\n";

                // log event
                m_Logger.severe(str);

                // log GPIB Status to GPIB logger
                m_GPIB_Logger.severe(m_GPIB_Driver.getGPIBStatus());

                // TODO 4* ask User what to do. retry, ignore, stop

                throw new IOException(str, ex);
            } finally {
                // finally is also called when a new Exception is thrown in catch{}

                // release the lock
                m_LockGPIBController.unlock();
            }
        }

        return ret;
    }//</editor-fold>


    /**
     * Sends a string via RS232 to the Instrument, and reads the Instrument's
     * reply. Does that only if the Instrument is not in No-Communication-Mode.
     * The String to send and a possible answer is logged to
     * <code>m_GPIB_Logger</code>.<p>
     *
     * Remark: To prevent unintended behavior on non-English environments (where
     * the decimal point might not be '.' but ',') it is recommended to use the
     * <code>Locale.US</code>, for instance like <code>String.format(Locale.US, ...</code>.<p>
     * 
     * @param Message is sent over the RS232. To convert the string to and from 
     * 8-bit values, the character set UTF-8 is used.
     * 
     * @return Returns the string read from RS232, or an empty String if
     * in No-Communication-Mode.
     * 
     * @throws IOException if the transmission caused a RS232 error; bubbles
     * up from <code>RS232_Driver.Send</code> or <code>RS232_Driver.Receive</code>.
     */
    // <editor-fold defaultstate="collapsed" desc="Query Via RS232 (String)">
    protected final String QueryViaRS232(String Message)
                    throws IOException {
        
        // Because Message is logged differently when it is passed as a String
        // versus being passed as a byte[], the code is somewhat doubled and does
        // not only call QueryViaRS232(byte[])
        
        // returned value if in No-Communication Mode
        String ret = "";

        // log message and return if in No-Communication Mode
        if (inNoCommunicationMode()) {
            // log message
            m_GPIB_Logger.log(Level.FINE, "{0} <n {1}: {2}", new Object[]{
                m_TimeStampFormat.format(Calendar.getInstance().getTime()), m_InstrumentName, Message});

            return ret;
        }

        // lock the Lock
        m_LockRS232Port.lock();
        
        try {
            // log message
            m_GPIB_Logger.log(Level.FINE, "{0} <- {1}: {2}", new Object[]{
                m_TimeStampFormat.format(Calendar.getInstance().getTime()), m_InstrumentName, Message});

            // write to Instrument
            m_RS232_Driver.Send( Message.getBytes(Charset.forName("UTF-8")) );

            // read from Instrument
            byte[] ans = m_RS232_Driver.Receive();
            
            // convert to a String
            ret = new String(ans, Charset.forName("UTF-8"));

            // log received GPIB traffic
            m_GPIB_Logger.log(Level.FINE, "{0} -> {1}: {2}", new Object[]{
                m_TimeStampFormat.format(Calendar.getInstance().getTime()), m_InstrumentName, ret});

        } catch (IOException ex) {

            // log event
            m_Logger.severe(ex.getMessage());

            // log GPIB Status to GPIB logger
            m_GPIB_Logger.severe(ex.getMessage());

            // TODO 4* ask User what to do. retry, ignore, stop

            throw ex;
        } finally {
            // finally is also called when a new Exception is thrown in catch{}

            // release the lock
            m_LockRS232Port.unlock();
        }

        return ret;
    }//</editor-fold>
    
    
    
    /**
     * Sends the <code>ByteBuffer</code> via RS232 to the Instrument, and reads the Instrument's
     * reply. Does that only if the Instrument is not in No-Communication-Mode.
     * The <code>ByteBuffer</code> to send and a possible answer are logged to
     * <code>m_GPIB_Logger</code>.<p>
     *
     * @param Message is sent over the RS232. To query with an empty argument
     * use <code>QueryViaRS232(ByteBuffer.allocate(0))</code>.   
     * 
     * @return Returns the data read from RS232, or a <code>ByteBuffer</code> with a 
     * limit of 0 if in No-Communication-Mode. The return value should never be null.
     * 
     * @throws IOException if the transmission caused a RS232 error; bubbles
     * up from <code>RS232_Driver.Send</code> or <code>RS232_Driver.Receive</code>.
     */
    // <editor-fold defaultstate="collapsed" desc="Query Via RS232 (ByteBuffer)">
    protected final ByteBuffer QueryViaRS232(ByteBuffer Message)
                    throws IOException {
        
        // returned value if in No-Communication Mode
        ByteBuffer ret = ByteBuffer.allocate(0);
        
        // build a string representation of Message
        String MessageString = RS232_Driver.ByteBufferToString(Message);

        // log message and return if in No-Communication Mode
        if (inNoCommunicationMode()) {
            // log message
            m_GPIB_Logger.log(Level.FINE, "{0} <n {1}: {2}", new Object[]{
                m_TimeStampFormat.format(Calendar.getInstance().getTime()), m_InstrumentName, MessageString});

            return ret;
        }

        // lock the Lock
        m_LockRS232Port.lock();
        
        try {
            // log message
            m_GPIB_Logger.log(Level.FINE, "{0} <- {1}: {2}", new Object[]{
                m_TimeStampFormat.format(Calendar.getInstance().getTime()), m_InstrumentName, MessageString});

            // write to Instrument
            m_RS232_Driver.Send(Message.array());

            // read from Instrument
            ret = ByteBuffer.wrap( m_RS232_Driver.Receive() );
            
            // build a string representation of the answer
            MessageString = RS232_Driver.ByteBufferToString(ret);

            // log received GPIB traffic
            m_GPIB_Logger.log(Level.FINE, "{0} -> {1}: {2}", new Object[]{
                m_TimeStampFormat.format(Calendar.getInstance().getTime()), m_InstrumentName, MessageString});

        } catch (IOException ex) {

            // log event
            m_Logger.severe(ex.getMessage());

            // log GPIB Status to GPIB logger
            m_GPIB_Logger.severe(ex.getMessage());

            // TODO 4* ask User what to do. retry, ignore, stop

            throw ex;
        } finally {
            // finally is also called when a new Exception is thrown in catch{}

            // release the lock
            m_LockRS232Port.unlock();
        }

        return ret;
    }//</editor-fold>

    
    /**
     * Sends a string via GPIB to the Instrument, and reads the Instrument's
     * reply. Does that only if the Instrument is not in No-Communication-Mode.
     * The String to send and a possible answer is logged to
     * <code>m_GPIB_Logger</code>.<p>
     *
     * This method uses a Lock on the GPIB controller to ensure thread safety.
     * Note: Declaring the method as synchronized resulted in communication errors when
     * different threads tried to access the same instrument.<p>
     *
     * Remark: To prevent unintended behavior on non-English environments (where
     * the decimal point might not be '.' but ',') it is recommended to use the
     * <code>Locale.US</code>, for instance like <code>String.format(Locale.US, ...</code>.
     *
     * @param Message is sent over the GPIB.
     * @return Returns the string read from GPIB, or an empty String if
     * in No-Communication-Mode.
     * @throws IOException if the transmission caused a GPIB error; bubbles
     * up from <code>GPIB_Driver.Send</code> or <code>GPIB_Driver.Receive</code>.
     */
    // TODO @LAN implement QueryViaLAN - see remarks to SendViaLAN()
    
    // <editor-fold defaultstate="collapsed" desc="Query Via LAN">
    protected final String QueryViaLAN(String Message)
                    throws IOException {

        // returned value if in No-Communication Mode
        String ret = "LAN communication is not yet supported";
        
        // just display an "error" message
        IcontrolView.DisplayStatusMessage("Querying via LAN is not yet supported.", false);

        // see QueryViaGPIB for an example code

        return ret;
    }//</editor-fold>

    
    /**
     * Handles the SEND command.<p>
     *
     * The SEND command sends the given string directly to the chosen instrument.
     * Supports GPIB and RS232 communication.
     *
     * @param Message is the String that is sent to the instrument.
     *
     * @throws ScriptException bubbles up from <code>SendViaGPIB</code> or from
     * <code>SendViaRS232</code>.
     *
     */
    // <editor-fold defaultstate="collapsed" desc="Send">
    @AutoGUIAnnotation(
        DescriptionForUser = "Sends a String to the Instrument (via GPIB or RS232)",
        ParameterNames = "String to send",
        ToolTips = "")
    public final void Send(String Message) throws ScriptException {
        
        // send message to the instrument
        try {
            // GPIB communication?
            if (m_UsedCommPort == CommPorts.GPIB) {
                SendViaGPIB(Message);
            } 
            // RS232 communication?
            else if (m_UsedCommPort == CommPorts.RS232) {
                SendViaRS232(Message);
            }

        } catch (IOException ex) {
            String str = "The Send command caused an error.\n ";
            str += "Please check the error message and log-dump and try to correct it.\n\n";
            str += ex.toString() + "\n";
            str += ex.getMessage() + "\n";

            // log event
            m_Logger.severe(str);

            // re-throw the exception as ScriptException
            ScriptException dummy = new ScriptException(str);
            dummy.initCause(ex);                
            throw dummy;
        }
    }//</editor-fold>

    
    /**
     * Handles the QUERY command.<p>
     *
     * The QUERY command sends the given string directly to the chosen instrument,
     * and addresses the instrument to talk. The received answer is made available
     * to the subsequent script-command.Supports GPIB and RS232 communication.
     *
     * @param Message is the String that is sent to the instrument.
     * @return The Instrument's response
     *
     * @throws ScriptException bubbles up from <code>QueryViaGPIB</code>, or 
     * from <code>QueryViaGPIB</code>.
     *
     */
    // <editor-fold defaultstate="collapsed" desc="Query">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Sends a String to the Instrument (via GPIB or RS232) and addresses the Instrument<br>"
                    + " to talk. The result is made available to the subsequent script command",
        ParameterNames = "String to send",
        ToolTips = "")
    public final String Query(String Message) throws ScriptException {
        
        // return value
        String ret = null;
        
        // send message to the instrument
        try {
            // GPIB communication?
            if (m_UsedCommPort == CommPorts.GPIB) {
                ret = QueryViaGPIB(Message);
            } 
            // RS232 communication?
            else if (m_UsedCommPort == CommPorts.RS232) {
                ret = QueryViaRS232(Message);
            }

        } catch (IOException ex) {
            String str = "The Query command caused an error.\n ";
            str += "Please check the error message and log-dump and try to correct it.\n\n";
            str += ex.toString() + "\n";
            str += ex.getMessage() + "\n";

            // log event
            m_Logger.severe(str);

            // re-throw the exception as ScriptException
            ScriptException dummy = new ScriptException(str);
            dummy.initCause(ex);                
            throw dummy;
        }
        
        // return the result
        return ret;
        
    }
    
    /**
     * Performs a speed test of the currently selected GPIB controller by
     * requesting the Instrument to send the Identification query. The ID of the
     * instrument is shown in the Status Text together with the time between
     * successive queries.<p>
     * Note that no Auto GUI is supplied as this method is intended for debugging
     * only.<br>
     *
     * @param NrIterations Specifies how many times the *IDN? query should be
     * performed.
     * @see GPIB_Driver#SpeedTest(int) 
     */
    // <editor-fold defaultstate="collapsed" desc="Speed Test">
    public void SpeedTest(int NrIterations) {
        
        // perform standard speed test
        m_GPIB_Driver.SpeedTest(NrIterations);

        // perform specialized speed test for Prologix controllers
        if (m_GPIB_Driver instanceof GPIB_Prologix) {
            ((GPIB_Prologix)m_GPIB_Driver).SpeedTestV2(NrIterations);
        }
    }//</editor-fold>
    
     
    /**
     * Finds the specified method from the available programmatic and generic
     * script-methods. The returned object contains all information available
     * about this method.<p>
     * 
     * Note: At present, only the method's name and number of arguments are used
     * to identify the method. Because the structure of iC has been changed, it
     * should not be difficult to implement a precise check of the parameter types
     * instead, thereby enabling truly overloaded methods. It's on the to do list.
     * 
     * @param Name The name of the method to look for.
     * @param NrOfArguments The number of arguments the method accepts.
     * 
     * @return A <code>ScriptMethod</code> object identifying the method.
     *
     * @throws ScriptException If none or more than one method was found.
     */
    // <editor-fold defaultstate="collapsed" desc="find Script Method">
    protected ScriptMethod findScriptMethod(String Name, int NrOfArguments)
              throws ScriptException {
        
        // local variables
        int             MethodCounter = 0;
        String          MultipleMethodsFound = "";
        ScriptMethod    ret = null;
        

        // get all methods
        ArrayList<ScriptMethod> AllScriptMethods = listAllScriptMethods();
        
        // identify the proper method to call
        for (ScriptMethod met : AllScriptMethods) {

            // first, match the names (case sensitive)
            if ( !met.DeviceCommandName.equals(Name) ) {
                continue;
            }

            // second, check if the class is a descendent from class Device
            // this is now done in listAllScriptMethods

            // third, match the number of arguments
            if ( met.ParameterTypes.length != NrOfArguments ) {
                // wrong number of arguments, so search the next method
                continue;
            }
                                
            // remember the Method to invoke
            ret = met;


            // found a method, so count it
            MethodCounter++;

            // remember the method's name for the error message, if any
            MultipleMethodsFound += met.DeviceCommandName + "(";// + met.ParameterTypes + "\n";
            for (int i=0; i<met.ParameterTypes.length; i++) {
                MultipleMethodsFound += (i>0?", ":"") + met.ParameterTypes[i].getSimpleName();
            }
            MultipleMethodsFound += ")\n";
            
        }
        
        
        
        /////////////////
        // error checking
        
        // was no method found?
        if (MethodCounter < 1) {
            String str = "The Script-Command is not supported.\n"
                + "Did not find a method '" + Name + "' which accepts " 
                + NrOfArguments + " arguments in the instrument class\n"
                + this.getClass().getSimpleName()
                + ".\nPlease check the spelling (case sensitive) or implement this method.\n";

            throw new ScriptException(str);
        }
        
        // was more than one method found?
        if (MethodCounter > 1) {
            String str = "More than one method with the name \"" + Name;
            str += "\" was found in the instrument class.\n";
            str += "Overloaded methods with the same number of arguments are not allowed.\n";
            str += "Please rename the methods.\n";
            str += "These methods were found:\n";
            str += MultipleMethodsFound;
            
            throw new ScriptException(str);
        }

        // return the method
        return ret;
    }//</editor-fold>
    
    
    
    /**
     * Invokes a genericaly defined Script Method. The <code>ConvertedParameters</code>
     * are 'filled' into the GPIB String, which is then sent to the Instrument. If
     * the method name starts with 'get', the Instrument is addressed to talk and
     * the result of this query is returned. If the method name starts with 'save'
     * the result of a query is additionally save to a file. In this case, the last
     * <code>ConvertedParameter</code> is interpreted as a file name.<p>
     * 
     * The Syntax-check mode works the same way as for Script Methods defined in
     * Java, and a Range Check on the <code>ConvertedParameters</code> is also 
     * performed if a range was defined in the generic Script Method.
     * 
     * @param Method The <code>ScriptMethod</code> to call.
     * 
     * @param ConvertedParameters The parameters of the method to call. The type
     * of the parameters must match the format specifiers supplied in 
     * <code>Method</code>.
     * 
     * @return The return value of the invoked method if <code>Method</code>'s
     * name starts with 'get'. Can be null.
     * 
     * @throws InvocationTargetException when 1) one of the <code>ConvertedParameters</code> 
     * is out of range or has an incorrect type (then the cause of 
     * <code>InvocationTargetException</code> is a <code>DataFormatException</code>), 
     * of 2) when sending or querying the GPIB command caused an error (bubbles up
     * from <code>SendViaGPIB</code> or <code>QueryViaGPIB</code>).
     */
    private Object invokeGenericScriptMethod(ScriptMethod Method, Object[] ConvertedParameters) 
           throws InvocationTargetException {
        
        // local variables
        String  FileExtension = "";
        String  ret = "";
        boolean QueryRequested = false;
        boolean SaveRequested = false;
        
        // make an ArrayList for easier handling
        ArrayList<Object> Parameters = new ArrayList<Object>(Arrays.asList(ConvertedParameters));
        
        
        // check if method name starts with 'get'
        if ( Method.DeviceCommandName.toLowerCase().startsWith("get") ) {
            
            // remember to query the Instrument
            QueryRequested = true;
        }
        
        // check if method name starts with 'save'
        else if ( Method.DeviceCommandName.toLowerCase().startsWith("save") ) {
            
            // remember to query the Instrument and save the result
            QueryRequested = true;
            SaveRequested = true;
            
            // yes, so get the File Extension and remove it from the Parameter List
            Object dummy = Parameters.remove(Parameters.size()-1);
            
            // check for correct Type (String)
            if ( !String.class.isAssignableFrom(dummy.getClass()) ) {
                String str = "Expected a File Name but did not find a String\n"
                        + "value as the last Parameter.\n";
                throw new InvocationTargetException(new DataFormatException(str));
            }
            
            // assign the File Extension
            FileExtension = (String) dummy;
        }
        
        // build the GPIB command string
        // in Syntax-Check mode and during the real run
        String GPIBString;
        try {
            GPIBString = String.format(Locale.US, Method.GPIBString, Parameters.toArray());
            
        } catch (Exception ex) {
            String str = "Could not generate the GPIB String.\n"
                    + "The format specifiers did not match the specified parameters.\n"
                    + ex.getMessage();
            throw new InvocationTargetException(new DataFormatException(str));
        }
        
        
        //////////////
        // Range Check
        if ( inSyntaxCheckMode() && Method.MethodChecksSyntax) {
        
            // iterate through all Parameters
            for (int i=0; i<Method.ParameterNames.length; i++) {
                
                // was a range defined for that Parameter Name
                // treat all numbers as double
                if (Method.ParameterRangeCheck[i]) {
                    
                    // check for correct Type (Number)
                    if ( !Number.class.isAssignableFrom(Parameters.get(i).getClass())) {
                        String str = "Expected a Number object and found a " + Parameters.get(i).getClass() + "\n"
                                + "for Parameter '" + Method.ParameterNames[i] + "'.\n";
                        throw new InvocationTargetException(new DataFormatException(str));
                    }
            
                    // get the current value
                    Number value = (Number) Parameters.get(i);
                    
                    // check range
                    if ( value.doubleValue() < Method.ParameterMinValue[i].doubleValue() ||
                         value.doubleValue() > Method.ParameterMaxValue[i].doubleValue() ) {
                        String str = "The value for '" + Method.ParameterNames[i] 
                                + "' (" + value + ")\n"
                                + "is out of the allowed range [" + Method.ParameterMinValue[i]
                                + ", " + Method.ParameterMaxValue[i] + "].";
                        throw new InvocationTargetException(new DataFormatException(str));
                    }
                }   
            }    
        }
        
        
        // return if in Syntax-check mode
        if (inSyntaxCheckMode())
            return ret;

        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return ret;

        
        try {
            // send the command
            SendViaGPIB(GPIBString);
            
            // is a query desired?
            if (QueryRequested) {
                // query the Instrument
                ret = QueryViaGPIB("");
            }
            
        } catch (Exception ex) {
            // re-throw as InvocationTargetException
            throw new InvocationTargetException(ex);
        }
        
        // is saving requested?
        if (SaveRequested) {
            
            // get the File Name
            String FileName = m_GUI.getFileName(FileExtension);
            
            // open the file for writing
            FileWriter fw;
            try {
                fw = new FileWriter(FileName);

            } catch (IOException ex) {
                String str = "Could not open the file " + FileName + "\n"
                    + "Make sure it is not a directory and that it can be opened or created.\n";
                throw new InvocationTargetException(new ScriptException(str));
            }

            try {
                // write to file
                fw.write(ret);

            } catch (IOException ex) {
                String str = "Could not save the data in file" + FileName + "\n"
                        + ex.getMessage();
                
                throw new InvocationTargetException(new ScriptException(str));
            }
            
            try {
                // close the file
                if (fw != null)
                    fw.close();
                
            } catch (IOException ex) {
                IcontrolView.DisplayStatusMessage("Could not close the file " + FileName + "\n");
            }
        }        
        
        return ret;
    }
    

    /**
     * Loads the generically defined Instruments from the specified file. The
     * File Name without extension (that is all characters before the first . (dot))
     * is used as the Instrument-Class-Name. The Instrument-Class-Name ((the name 
     * that shows up in the ComboBox of the MAKE section of the GUI) may contain 
     * spaces.<p>
     * 
     * Note that once a parsing error occurs, the remainder of file is not parsed,
     * hence, the entire file needs to be free of errors for all methods to be
     * available.
     * 
     * @throws ScriptException if parsing the file failed. 
     */
    // <editor-fold defaultstate="collapsed" desc="set Instrument Class Name">
    public void loadGenericGPIBInstruments(File FileToLoad)
           throws ScriptException {
        
        // tic
        long tic = System.currentTimeMillis();
              

        // open the file for reading
        BufferedReader fr;
        try {
            fr = new BufferedReader(new FileReader(FileToLoad));

        } catch (FileNotFoundException ex) {
            
            // should never happen, but well ...
            String str = "Error laoding generic GPIB Instrument: Could not find the file\n"
                    + FileToLoad.getAbsolutePath() + "\n";
            
            // display to the user (and log it)
            IcontrolView.DisplayStatusMessage(str, false);

            // nothing to load, so return
            return;
        }
        
        
        // import the definitions
        String Line = "";
        try {
            // process all lines in the file
            while ( (Line = fr.readLine()) != null ) {

                // remove all characters after a comment (% or //)
                // original Regex was ^\\s*[//[%]].*  (it's wrong)
                // updated Regex was (//|%).+   (it's also wrong, strips off everything after % (e.g. in FR %.2f))
                // up-updated Regex is ^\\s*(//|%).+
                Line = Line.replaceAll("^\\s*(//|%).+", "");

                // remove leading and trailing whitespaces
                Line = Line.replaceAll("^\\s+", "").replaceAll("\\s+$", "");
                
                // go to the next line if Line is empty
                if (Line.isEmpty())
                    continue;              

                /////////////////////
                // get the Instrument

                GenericGPIBInstrument Inst;

                // get the Instrument-Name
                String InstrumentClassName = FileToLoad.getName().replaceAll("\\..*", "");

                if ( m_GenericGPIBInstruments.containsKey(InstrumentClassName) ) {
                    // yes, so go and get it
                    Inst = m_GenericGPIBInstruments.get(InstrumentClassName);

                } else {
                    // no, make a new one
                    Inst = new GenericGPIBInstrument();

                    // put it in the list
                    m_GenericGPIBInstruments.put(InstrumentClassName, Inst);
                }
                
               
                
                /////////////////
                // parse the line
                
                // make a new ScriptMethod object
                ScriptMethod met = new ScriptMethod();
                
                try {
                    // parse the line and fill the ScriptMethod
                    met.parseDefinition(Line);
                    
                } catch (ScriptException ex) {
                    // append the line to the exception's message
                    String str = "Parsing the definition the of generic GPIB Instrument in file\n"
                            + FileToLoad.getAbsolutePath() + "\n"
                            + "caused an error:\n"
                            + ex.getMessage() + "\n"
                            + "The line containing the error is:\n"
                            + Line.replaceAll(".{100}", "$0\n") + "\n\n"   // restrict line to 80 characters
                            + "Not all generic commands defined for this Instrument will be available until\n"
                            + "the file can be parsed entirely without errors. Please correct the line and try again.\n";
                    
                    // log event
                    m_Logger.severe(str);
                    
                    // re-throw
                    throw new ScriptException(str);
                }
                
                // add method
                Inst.addCommand(met);
            }
        } catch (IOException ex) {
            String str = "An IO error occurred during reading the file\n"
                    + FileToLoad.getName() + "\n"
                    + "which defines a generic Instrument. Please remove the source of "
                    + "this error and try again. The last line sucessfully read was\n"
                    + Line + "\n";
            
            throw new ScriptException(str);
        }
        
        // toc
        long dT = System.currentTimeMillis() - tic;
        
        // log time to load
        if ( m_iC_Properties.getInt("iC.Debug", 0) == 1 )
            IcontrolView.DisplayStatusMessage(dT + " ms (" + FileToLoad.getName() + ")\n", false);
        else
            m_Logger.log(Level.FINE, "{0} ms ({1})\n", new Object[]{dT, FileToLoad.getName()});
     
    }//</editor-fold>
    
    
    /**
     * Registers all generic GPIB Instruments that have not yet been registered, 
     * that is, every Instrument-Class-Name defined in <code>m_GenericGPIBInstruments</code>
     * that is not included in <code>Dispatcher.m_RegisteredInstruments</code> is 
     * added to the list with <code>iC_GenericGPIB</code> as Instrument-Class.
     
     * @param RegisteredInstruments The generic GPIB Instruments will be added to this
     * list.
     */
    // <editor-fold defaultstate="collapsed" desc="register Generic GPIB Instruments">
    public void registerGenericGPIBInstruments(TreeMap<String, String> RegisteredInstruments) {
        
        // get the keys of all registered Instruments
        Set<String> RegisteredInstrumentClassNames = RegisteredInstruments.keySet();
        
        // iterate through all generic Instruments
        for (String InstrumentClassName : m_GenericGPIBInstruments.keySet()) {
            
            // already registered?
            if ( !RegisteredInstrumentClassNames.contains(InstrumentClassName) ) {
                // no, so register it
                RegisteredInstruments.put(InstrumentClassName, iC_GenericGPIB.class.getName());
            }
        }
    }//</editor-fold>
    
    /**
     * Finds all Script-Methods, that is, all public methods defined in class
     * <code>Device</code> or it's descendants. All public methods can be invoked
     * from the Script, but note that not all returned methods have to bare an 
     * <code>@AutoGUIAnnotation</code>. This method also returns generically defined
     * methods.
     * 
     * @return A list of methods that can be invoked via script commands. The
     * returned list might be empty but is never null.
     */
    // <editor-fold defaultstate="collapsed" desc="list All Script Methods">
    public ArrayList<ScriptMethod> listAllScriptMethods() {
        
        // holds the return value
        ArrayList<ScriptMethod> ret = new ArrayList<ScriptMethod>();
       
        
        //////////////////////
        // programmatic methods
        
        // get all (programmatic) methods from this object
        Method[] allMethods = this.getClass().getMethods();

        
        // identify all Script-Methods (as opposed to methods not availalbe to scripts)
        for (Method met : allMethods) {

            // check if the class is derived from class Device or
            // the Device class itself. (see comment in Dispatcher.HandleMakeCommand)
            // Remark: methods from, say, Object, are also listed by getMethods()
            Class DeclaringClass = met.getDeclaringClass();

            // the != null check is probably not necessary, as a NullPointerException would be thrown above
            if (met != null && !Device.class.isAssignableFrom(DeclaringClass)) {
                // no, it's not derived from Device, so search the next method
                continue;
            }
            
            
            // get the AutoGUIAnnotation
            AutoGUIAnnotation AGanno = met.getAnnotation(AutoGUIAnnotation.class);
            
            // get iC_Annotation
            iC_Annotation iCanno = met.getAnnotation(iC_Annotation.class);

            
            // make a new ScriptComand instance
            ScriptMethod sm = new ScriptMethod();

            
            
            // *** the method itself
            
            // remember the method's name
            sm.DeviceCommandName = met.getName();
            
            // remember the Method to invoke
            sm.ReferenceToMethod = met;

            // remember the parameter Types
            sm.ParameterTypes = met.getParameterTypes();
            
            // remember that it is not a generic method (would not be necessary)
            sm.isGenericGPIB = false;
            
            
            // *** AutoGUI
            
            // remember if Method bares an AutoGUIAnnotation (is to be included
            // in the AutoGUI)
            sm.AutoGUI = (AGanno != null) ? true : false;
            
            // assign Description for user
            sm.DescriptionForUser = (AGanno == null) ? 
                    "no AutoGUIAnnotation present" : AGanno.DescriptionForUser();

            // remember the parameter Names
            String noAG[] = {"no AutoGUIAnnotation present"};
            sm.ParameterNames = (AGanno == null) ? noAG : AGanno.ParameterNames();

            // remember the tooltip texts
            sm.ToolTips = (AGanno == null) ? noAG : AGanno.ToolTips();
            
            // remember the default values
            sm.ParameterDefaultValues = (AGanno == null) ? noAG : AGanno.DefaultValues();

            
            // *** syntax-check
            
            // remember value of MethodChecksSyntax
            sm.MethodChecksSyntax = (iCanno != null) ? iCanno.MethodChecksSyntax() : false;          
            

            // found a method, so remember it
            ret.add( sm );
        }
        
        
        //////////////////
        // generic methods
        
        // get the generic GPIB Instrument
        GenericGPIBInstrument Inst = m_GenericGPIBInstruments.get(m_InstrumentClassName);
        
        // get all generic methods
        if (Inst != null) {
            ret.addAll(Inst.getCommands());
            
        }

        return ret;
    }//</editor-fold>
    
    
    
    /**
     * Executes a Python command. If it is the first time a Python command is
     * executed, this method also creates a new instance of the Python 
     * Interpreter by calling <code>resetPythonInterpreter</code> and shows the 
     * JythonPrompt window.<p>
     * 
     * Remark to Developers: The MAKE command is not supported from Python 
     * because MAKE commands are handled by the Dispatcher, and the Device class 
     * has no access to the Dispatcher. It would be easy to assign a static 
     * Dispatcher object to the Device class, but it's not elegant, and it is easy 
     * to MAKE new instruments from iC, hence, it's not necessary to use Python. 
     * Especially because it would be a big effort that the instruments made in 
     * Python would also show up in the GUI. A second way to export the 
     * Dispatcher to Python would be to have <code>resetPythonInterpreter</code> 
     * call a method in <code>IcontrolView</code> which uses 
     * <code>Device.getPythonInterpreter</code> to export <code>m_Dispatcher</code>.
     * Maybe there is a third way.
     * 
     * @param PythonCommand This command will be executed by the Python 
     * Interpreter. To execute multi-line Python commands, the lines must be sent
     * in one String separated with the newline (\n) character.
     * 
     * @throws ScriptException is thrown when 1) executing a Python command 
     * (either <code>PythonCommand</code> or a command in Startup.py) caused
     * a Python error, or 2) an exception bubbles up from 
     * <code>resetPythonInterpreter</code>.
     */
    // <editor-fold defaultstate="collapsed" desc="exec Python">
    public void execPython(String PythonCommand) 
           throws ScriptException {
        
        // local variable
        String Message = "";
        
        try {
            
            // TODO 2* I could check if m_JythonPromptWindow is null and make a 
            // new one. Then change from HIDE to DISPOSE_ON_EXIT to be able to
            // recreate the Python window from the GUI with the Py button.
            // Would need to test for != null in JythonPrompt.TextAreaWriter
                 
            // start a new Python Interpreter if necessary
            if (m_PythonInterpreter == null) {

                // show the Jython Prompt - Window
                m_JythonPromptWindow = new JythonPrompt();

                // tic
                long tic = System.currentTimeMillis();

                // Create an instance of the PythonInterpreter, 
                // assign Python's standard output stream, and
                // execute the default Python commands (iC/Startup.py)
                resetPythonInterpreter("Starting Python Interpreter.");

                // toc
                int dT = (int) (System.currentTimeMillis() - tic);

                // show time it took to start Jython
                m_PythonInterpreter.set("StartupTime", new PyInteger(dT));
                m_PythonInterpreter.exec("print 'Jython Interpreter started in %d ms.' % StartupTime");

                // display Python version
                //m_PythonInterpreter.exec("import sys");
                //m_PythonInterpreter.exec("print sys.version");

                Message = "An error occurred when starting Python.\n";
            }
        
            // export the result of the last command to the Python environment
            m_PythonInterpreter.set("_ans", Device.m_LastReturnValue);
            
            
            // now execute the intended Python command
            m_PythonInterpreter.exec(PythonCommand);
            
        } catch (PyException ex) {
            
            // ex.toString is much better than me formating the ErrorMessage
            // could use:
            // ex.normalize();
            // ex.value.__findattr__("lineno");
            // or other fields in the values or type field of ex
            // see javadoc to PyException

            // normalize the Exception, whatever that means
            ex.normalize();
            
            // build the String
            Message += "The Python Interpreter returned an error:\n"
                    + ex.toString() + "\n";
            
            // re-throw as Script Exception
            throw new ScriptException(Message);            
        }
    }//</editor-fold>

    
    /**
     * Resets the Python Interpreter (PI) by discarding the old PI and making
     * a new PI. After the new PI is instantiated, the file Startup.py residing
     * in the iC directory is executed. Needs to be public so that it can be
     * called from the Dispatcher.
     * 
     * @param Message will be printed in the Python output
     * @throws ScriptException when a Python Error occurred
     */
    // <editor-fold defaultstate="collapsed" desc="reset Python Interpreter">
    public void resetPythonInterpreter(String Message) 
           throws ScriptException {
        
        /* Remark: Instead of creating a new PI instance, I might also be able
         * to pass a PyStringMap to the PI (new PythonInterpreter(new PyStringMap).
         * As far as I understand, this Python object acts as global namespace.
         * Upon reset I would just create a new PyStringMap, which brings us
         * actually back to square one.
         * http://old.nabble.com/PythonInterpreter-question-td27760276.html
         */
        
        
        // make a new Python Interpreter
        // to reset all variables declared in a previous run or during Syntax-check
        m_PythonInterpreter = new PythonInterpreter();
        
        // assign new Python Interpreter to the JythonPrompt Form
        m_JythonPromptWindow.setPythonInterpreter(m_PythonInterpreter);

        
        ///////////////////////////////////
        // export useful iC stuff to Python

        // make a new Device dummy object
        Device dev = new Device();
        
        // get the iC directory
        String iC_Dir = m_iC_Properties.getPath("iC.DefaultPath", "$user.home$/iC")
                + System.getProperty("file.separator");
        
        // make full file name for Startup.py
        String FileName = iC_Dir + "Startup.py";

        
        try {
            // make the Device instance accessible in Python
            // use _ to mark it as a instance variable; see Python style guide
            // http://www.python.org/dev/peps/pep-0008 (Method Names and Instance Variables)
            m_PythonInterpreter.set("_device", dev);

            // export the Project Directory
            m_PythonInterpreter.set("project_directory", m_GUI.getProjectPath());

            // export the iC directory       
            m_PythonInterpreter.set("iC_directory", iC_Dir);
            
            
            // display the Message
            if ( !Message.isEmpty() ) {
                execPython("print '" + Message + "'");
            }
        
            // check if the file exists
            if ( (new File(FileName)).exists() ) {
                
                // yes, so execute it
                m_PythonInterpreter.execfile(FileName);
                
            } else {
                
                // no, show a Status message
                IcontrolView.DisplayStatusMessage("Error: Could not find " + FileName + "\n", false);
            }
       
        } catch (PyException ex) {
            // re-throw as ScriptException
            String str = "Python error:\n" + ex.toString();
            throw new ScriptException(str);
        }
    }//</editor-fold>
   
    
    /** 
     * Getter method for <code>m_StopScripting</code>, the flag which indicates
     * that the user has pressed the Stop button. Used to access this flag from
     * Python.
     * 
     * @return <code>true</code> if the user has pressed the Stop button, false
     * otherwise.
     */
    // <editor-fold defaultstate="collapsed" desc="getStopScripting">
    public boolean getStopScripting() {
        return m_StopScripting;
    }//</editor-fold>
    
    /**
     * Getter method for the Python Interpreter
     * @return Returns the current PythonInterpreter or null if the Python
     * Interpreter has not been started (because no Python commands were executed)
     */
    // <editor-fold defaultstate="collapsed" desc="get Python Interpreter">
    public PythonInterpreter getPythonInterpreter() {
        return m_PythonInterpreter;
    }//</editor-fold>
    
}
