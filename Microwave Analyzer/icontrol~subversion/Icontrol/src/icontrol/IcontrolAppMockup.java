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
package icontrol;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;


/**
 * Mockup for the test environment. Instantiate this class and it's constructor
 * initializes the MockupView as well as the Dispatcher. All classes of the 
 * iC-framework should now be able to run.<p>
 * 
 * It would be nicer if this class were in the test/ directory, but becuase the
 * <code>Utilities.getView</code> needs access <code>IcontrolAppMockup.getView</code>,
 * it needs to be in the source directory, where it also gets distributed, but well.
 * 
 * @author KPP (Kurt Pernstich: pernstich@alumni.ethz.ch)
 * @version 0.1
 *
 */
public class IcontrolAppMockup {
    
    ///////////////////
    // member variables
    
    /** The GUI Mockup */
    private static GUI_InterfaceMockup m_GUI = null;
    
    /** The Dispatcher instance for JUnit tests */
    private Dispatcher m_Dispatcher;
    
    /**
     * Define the iC_Logger and the iC_Comm_Logger here to
     * keep a strong reference to avoid that the Logger is being garbage collected. */
    private static final Logger m_Logger = Logger.getLogger("iC");
    private static final Logger m_Comm_Logger = Logger.getLogger("Comm");
    
    /** Convenient access to application wide properties defined in iC.properties */
    private iC_Properties m_iC_Properties;
    
    
    
    /**
     * @return Returns the <code>GUI_Interface</code> (aka, the GUI) when iC
     * is run as a Unit Test and not as a "regular" application.
     */
    // <editor-fold defaultstate="collapsed" desc="getView">
    public static GUI_Interface getView() {
        return m_GUI;
    }//</editor-fold>


    /**
     * Constructor. Enables No-Communication-Mode and enables Syntax-Check Mode.
     * Sets up the Loggers to write to a file in the standard $user home$/iC/iC log
     * directory with Log level of ALL. If the directory does not exist, run the
     * program once, as this ask to create the directory for you.
     */
    // <editor-fold defaultstate="collapsed" desc="Constructor">
    public IcontrolAppMockup() {
        
        // do nothing if the GUI has been initialized before
        if (m_GUI != null) {
            return;
        }
        
            
        // instantiate the Mockup View
        m_GUI = new GUI_InterfaceMockup();
        
        // instantiate iC_Properties
        m_iC_Properties = new iC_Properties();

        
        /////////////////////
        // prepare Dispatcher

        // make a new Dispatcher instance
        // this also calls RegisterGenericDeviceClasses() which registers all available
        // Instrument Classes
        m_Dispatcher = new Dispatcher(m_GUI);
        
        
        // set No-Communication Mode
        setSimulationMode(true);

        // enable Syntax-Check mode
        setSyntaxCheckMode(true);
        
            
        ////////////
        // iC Logger
        // <editor-fold defaultstate="collapsed" desc="iC Logger">
        // set Logger level
        m_Logger.setLevel(Level.parse("ALL"));


        // reformat the current date and time
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd_HHmmss");
        String DateString = sdf.format(Calendar.getInstance().getTime());
        
        // get default iC path from iC properties and make the file name
        String Default_iCPath = m_iC_Properties.getPath("iC.DefaultPath", "$user.home$/iC");
        String LogFileName = Default_iCPath + "iC log" 
                + System.getProperty("file.separator") + DateString + "_iC_Log.txt";

        // define a new Formatter-class
        // <editor-fold defaultstate="collapsed" desc="myFormatter">
        class myFormatter extends java.util.logging.Formatter {
            
            // constructor is probably not necessary

            @Override
            public String format(LogRecord record) {
                
                // add LoggerName
                String str = record.getLoggerName() + ":\t";
                
                // add Message
                str += formatMessage(record);

                // add a newline if non was given
                if ( !str.endsWith("\n") )
                        str += "\n";

                // replace every \n with a \r\n
                str = str.replaceAll("[\n]", "\r\n");

                return str;
            }
        }//</editor-fold>
        
        // make the Formatter
        myFormatter form = new myFormatter();

        // direct Logger output to file
        try {
            // make new file handler
            FileHandler fh = new FileHandler(LogFileName, true);

            // attach to logger
            m_Logger.addHandler(fh);

            // attach the Formatter to the FileHandler
            fh.setFormatter(form);

        } catch (IOException ex) {
            m_GUI.DisplayStatusMessage("Error: Could not open the Logger's FileHander.\n");
        } catch (SecurityException ex) {
            m_GUI.DisplayStatusMessage("Error: Insufficient rights to configure the Logger:\n"
                    + ex.getMessage() + "\n");
        }

        // log Logger level
        m_Logger.log(Level.CONFIG, "Logger Level = {0}\n", m_Logger.getLevel().toString());
        //</editor-fold>

        
        //////////////
        // Comm Logger
        // <editor-fold defaultstate="collapsed" desc="Comm Logger">
        
        // set Logger level
        m_Comm_Logger.setLevel(Level.parse("ALL"));


        // get default iC path from iC properties and make the file name
        LogFileName = Default_iCPath + "iC log" 
                + System.getProperty("file.separator") + DateString + "_Comm_Log.txt";

        // define a new Formatter-class
        // <editor-fold defaultstate="collapsed" desc="myCommFormatter">
        class myCommFormatter extends java.util.logging.Formatter {

            // local variables
            //SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.S");


            @Override
            public String format(LogRecord record) {

                // add Message
                String str = formatMessage(record);

                // add a newline if non was given
                if ( !str.endsWith("\n") )
                        str += "\n";

                // replace every \n with a \r\n
                str = str.replaceAll("[\n]", "\r\n");

                // precede string with a time stamp
                //str = sdf.format(Calendar.getInstance().getTime()) + " " + str;


                return str;
            }
        }//</editor-fold>


        // direct Logger output to file
        try {
            // make new file handler
            FileHandler fh = new FileHandler(LogFileName, true);

            // attach to logger
            m_Comm_Logger.addHandler(fh);

            // attach the Formatter to the FileHandler
            fh.setFormatter( new myCommFormatter() );

        } catch (IOException ex) {
            m_GUI.DisplayStatusMessage("Error: Could not open the Comm-Logger's FileHander.\n");
        } catch (SecurityException ex) {
            m_GUI.DisplayStatusMessage("Error: Insufficient rights to configure the Comm-Logger:\n"
                    + ex.getMessage() + "\n");
        }

        // log Logger level
        m_Comm_Logger.log(Level.CONFIG, "Comm-Logger Level = {0}\n", m_Logger.getLevel().toString());
        //</editor-fold>
        
    }//</editor-fold>
    

    /**
     * Sets the global Simulation Mode during Tests.
     *
     * @param SimulationMode When true, the global Simulation Mode is enabled.
     */
    // <editor-fold defaultstate="collapsed" desc="setSimulationMode">
    public final void setSimulationMode(boolean SimulationMode) {
        m_Dispatcher.setSimulationModeGlobal(SimulationMode);
    }//</editor-fold>

    /**
     * Sets the Syntax-Check mode
     *
     * @param SyntaxCheckMode When true, the Syntax-Check mode is enabled
     */
    // <editor-fold defaultstate="collapsed" desc="setSyntaxCheckMode">
    public final void setSyntaxCheckMode(boolean SyntaxCheckMode) {
        m_Dispatcher.setSyntaxCheckMode(SyntaxCheckMode);
    }//</editor-fold>
    
    
}
