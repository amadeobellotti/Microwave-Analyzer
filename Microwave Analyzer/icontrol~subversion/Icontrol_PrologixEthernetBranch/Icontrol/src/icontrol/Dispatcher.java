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
package icontrol;

import icontrol.instruments.Device;
import icontrol.instruments.Device.CommPorts;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.script.ScriptException;
import javax.swing.JOptionPane;
import org.python.core.PyException;



/**
 *
 * Dispatcher class of Instrument Control.<p>
 *
 * This class  obtains a list of Script-Commands and dispatches the Script-Commands
 * to the Device class which in turn dispatches them to the appropriate
 * instances of the Instrument-Classes. Processing of the script is done in a
 * separate task.<p>
 *
 * In essence, this class iterates through all Command-Lines in the Script and
 * <ul>
 *  <li> ignores comment lines marked by either '%' (Matlab style) or '//' (C style)
 *  <li> handles the Framework-Commands listed below
 *  <li> dispatches the Command-Lines to <code>Device.DispatchCommand</code>,
 *       which in turn dispatches the Device-Command to the appropriate
 *       instrument object
 *  <li> keeps a HashMap of all Used Instruments (<code>m_UsedInstruments</code>)
 *  <li> Shows error messages to the user if an error occurred during processing the
 *       Script
 * </ul><p>
 *
 *
 * <h3>Supported Framework-Commands:</h3>
 * <ul>
 * <li> A whitespace separates the Framework-Command from it's Argument-List, and
 *      individual entries in the Argument-List are separated by
 *      <code>DELIMITER</code>, which is a semicolon (;).
 * </li>

 * <li> MAKE Instrument-Name; Instrument-Class; GPIB_Address
 * </li>
 *      <ul>
 *      <li>Instantiates a new instrument.</li>
 *      <li>Instrument-Name: with this name the instrument can be accessed from the script.</li>
 *      <li>Instrument-Class: this specified the 'type' of instrument (for instance a
 *          Lakeshore 340 temperature controller or a Agilent 4155 parameter analyzer).</li>
 *      <li>GPIB_Address: this is the GPIB address that is set at the instrument.<br>
 *          Note that the GPIB_Address starts with the String "GPIB" followed by
 *          the Integer valued GPIB Address (range: 0..31). Any additional non-digit
 *          letters and symbols are disregarded, allowing to write for instance
 *          "GPIB @9" instead of only "GPIB9".</li>
 *      </ul>
 *  <li> INCLUDE FileName</li>
 *      <ul>
 *      <li>Includes an external file as a Sub-Script in the current script</li>
 *      <li>If FileName does not specify a path (no file separator (e.g. '\') is
 *          specified, the default path specified in
 *          <code>iC_Properties.getPath("iC.DefaultPath", "$user.home$/iC")</code>
 *          is used.</li>
 *      <li>If no file extension was specified (FileName does not contain
 *          a '.', the default extension '.iC' is added.</li>
 *      <li>Sub-Scripts can contain MAKE commands, but these Instrument-Names are not
 *          shown in the GUI's Instrument-List.</li>
 *      <li>Sub-Scripts can INCLUDE other Sub-Scripts.</li>
 *      </ul>
 * </ul>
 *
 * @see Dispatcher#DELIMITER
 *
 * @author KPP (Kurt Pernstich: pernstich@alumni.ethz.ch)
 * @version 0.4
 *
 */
public final class Dispatcher extends Thread {

    //////////////////////
    // my member variables

    /** Holds a "reference" to the instance of the View-class to enable access 
     * it's methods. */
    private IcontrolView m_GUI;


    /** Holds the list of command strings to process. This list is not changed
     * and assigned from <code>IcontrolView</code>. */
    private ArrayList<String> m_CommandList;

    /**
     * Instrument-Name / Instrument-Instance pairs of the Instruments used in the
     * script. The Instrument-Name (or variable name, for instance Tsample, PA, 
     * srs ...) used to access the instrument from the script is used as a key 
     * to the HashMap. It is populated in <code>HandleMakeCommand</code>.
     */
    private HashMap<String, Device> m_UsedInstruments;

    /**
     * Instrument-Class-Name / Instrument-Class pairs of all available
     * Instruments, for example: Lakeshore 332 / icontrol.instruments.Lakeshore332, 
     * SRS DS345 / icontrol.instruments.iC_GenericGPIB. Every new Device-class 
     * needs to be registered here to be accessible from the script. 
     *
     * The Instrument-Class-Name is used to populate the ComboBox that allows to 
     * add MAKE commands. It is also used in <code>Device.DispatchCommand</code> 
     * to identify the instances the Script-Command is passed to, and in 
     * <code>IcontrolView.jInstrumentNameSelected</code> to search all available
     * Script-Methods.<p>
    
     * The Instrument-Class is a String that contains the actual class name
     * of the class that implements the functionality of that Instrument. Note 
     * that it must contain the full class name including package, that is 
     * icontrol.Lakeshore340 not only Lakeshore340. Generic GPIB Instruments register
     * as class <code>iC_GenericGPIB</code>.<p>
     * 
     * A TreeMap is a sorted HashMap.
     */
    private TreeMap<String, String> m_RegisteredInstruments;


    /** character used to separate individual arguments in a Script-Command.
     * It is also accessed from <code>IcontrolView</code>, so it is made static */
    protected static final char DELIMITER = ';';

    // when true, the dispatcher runs in SyntaxCheckMode
    private boolean m_SyntaxCheckMode;
    
    // is true when an error occurred during processing the script
    private boolean m_ErrorOccurred;

    // is true when the sequencing should be stopped
    private boolean m_StopSequencing;

    // holds the number of lines INCLUDED from Sub-Scripts.
    private int     m_IncludedLines;


    // Convenient access to application wide properties defined in iC.properties
    private iC_Properties m_iC_Properties;

    /** The Logger for this class */
    private static final Logger m_Logger = Logger.getLogger("iC.Dispatcher");
    


    /**
     * Default constructor that assigns the given parameters.
     * Creates a new HashMap that stores the Instruments used by the script.
     * Also calls <code>RegisterGenericDeviceClasses</code> which registers all
     * available Instrument-Classes.
     *
     * @param GUI is used to reference methods in the View-class (e.g. display status messages)
     */
    // <editor-fold defaultstate="collapsed" desc="Default constructor">
    Dispatcher( IcontrolView GUI) {

        // set Logger level to inherit level of parent logger
        m_Logger.setLevel(null);

        // store access to the GUI
        m_GUI = GUI;

        // also grant the Device classes access to the GUI
        Device.setGUI(GUI);

        // instantiate iC_Properties
        m_iC_Properties = new iC_Properties();
        
        // make a new HashMap for the used instruments
        m_UsedInstruments = new HashMap<String, Device>();

        // make a new HashMap for the registered Instruments
        m_RegisteredInstruments = new TreeMap<String, String>();

        // register all available Instrument-Classes
        RegisterDeviceClasses();


        // pass a reference to the list of used instruments to the Device class
        // at this point the list is empty but will be filled in
        // HandleMakeCommand().
        Device.setUsedInstruments(m_UsedInstruments);

        // init setSyntaxCheckMode
        m_SyntaxCheckMode = true;

        // init Error flag
        m_ErrorOccurred = false;

    }//</editor-fold>


    // <editor-fold defaultstate="collapsed" desc="set No-Communication Mode Global()">
    /**
     * Sets the simulation mode of all instruments derived from <code>Device</code>.
     * 
     * @param SimulationMode if <code>true</code>, all instruments are set into
     * a simulation mode where no GPIB communication takes place
     */
    public void setSimulationModeGlobal(boolean SimulationMode) {

        // set the simulation mode in class Device
        Device.setNoCommunicationModeGlobal(SimulationMode);

        // display status message for the user
        if (SimulationMode) {
            IcontrolView.DisplayStatusMessage("Entering simulation mode for all instruments.\n");
        }
    }//</editor-fold>


    /**
     * Sets the value of the setSyntaxCheckMode. In setSyntaxCheckMode
     * the InstrumentCommands (methods in the Instrument classes)
     * are not called by the dispatcher, unless the bare an
     * <code>iC_Annotation</code> with <code>ChecksSyntax</code>=true
     *
     * @param SyntaxCheckMode the new value of the <code>SyntaxCheckMode</code>
     */
    // <editor-fold defaultstate="collapsed" desc="set Syntax Check Mode">
    public void setSyntaxCheckMode(boolean SyntaxCheckMode) {
        m_SyntaxCheckMode = SyntaxCheckMode;

        // also set it in the Devices
        Device.setSyntaxCheckMode(SyntaxCheckMode);
    }//</editor-fold>


    /**
     * Returns the Error Flag of the Dispatcher. When true, then an error occurred
     * during processing the script, for instance in SyntaxCheckMode
     *
     * @return true if an error occurred during dispatching the commands or in
     * SyntaxCheckMode
     */
    // <editor-fold defaultstate="collapsed" desc="get Error Flag">
    public boolean getErrorFlag() {
        return m_ErrorOccurred;
    }//</editor-fold>


    // <editor-fold defaultstate="collapsed" desc="get Registered Instruments()">
    /**
     * Default Getter method for the HashMap containing all the 
     * registered Instruments. Make sure the returned value is not changed because
     * the compiler can't.
     * 
     * @return A HashMap of all registered Instruments
     */
    public TreeMap<String, String> getRegisteredInstruments() {
        return m_RegisteredInstruments;
    }//</editor-fold>

    
    // <editor-fold defaultstate="collapsed" desc="get Used Instruments()">
    /**
     * Default Getter method for the HashMap containing all the 
     * used Instruments. Make sure the returned value is not changed because
     * the compiler can't.
     * 
     * @return A HashMap of all registered Instruments
     */
    public HashMap<String, Device> getUsedInstruments() {
        return m_UsedInstruments;
    }//</editor-fold>

    /**
     * Setter method for <code>m_CommandList</code>, which holds the
     * Command Lines to process.
     *
     * @param CommandList holds all command lines (the script)
     */
    // <editor-fold defaultstate="collapsed" desc="set Command List()">
    public void setCommandList(ArrayList<String> CommandList) {
        m_CommandList = CommandList;
    }//</editor-fold>


    
    /**
     * Populates a HashMap of Instrument-Class-Name / Instrument-Class pairs.
     * The Instrument-Class-Name is the String used in a script to identify the
     * Instrument Model (Lakeshore 340, Agilent 4155, ...). It can contain spaces
     * but no <code>DELIMITER</code>, which is semicolon (;). The Instrument-Class-Name
     * is used by the <code>Dispatcher</code> to route commands to the
     * Instrument-Class.
     * Note that an Instrument-Class is not allowed to have multiple
     * Instrument-Class-Names in this HashMap, but it would be allowed to add
     * an additional clause in <code>Dispatcher</code> to enable multiple
     * variant spellings of the same instrument (Lakeshore 340, Lakeshore_340,
     * Lakeshore340, ...).
     *
     */
    // <editor-fold defaultstate="collapsed" desc="RegisterGenericDeviceClasses()">
    //http://stackoverflow.com/questions/176527/how-can-i-enumerate-all-classes-in-a-package-and-add-them-to-a-list
    private void RegisterDeviceClasses() {
        
        
        ////////////////////////////////////////////////////
        // enlist all class names in the instruments package
        
        ArrayList<String> ClassNames = new ArrayList<String>();
        
        
        // The package name to search
        String PackageName = "icontrol.instruments";
        
        // get URL to instruments package
        URL url = ClassLoader.getSystemClassLoader().getResource(PackageName.replace('.', '/'));
        
        // log url
        m_Logger.log(Level.CONFIG, "URL to instruments package = {0}\n", url);

        // check if instruments package was found
        if (url == null) {
            String str = "Could not locate icontrol.instruments package.\n"
                       + "Please tell the developer and include the log files.\n";
            throw new RuntimeException(str);
        }
        
        
        // make a URI to the package dir
        URI uri;
        try {
            
            // get URI to convert special characters like %20 into proper characters
            uri = url.toURI();
        }
        catch (URISyntaxException e) {
            String str = "Could not locate the instruments package directory.\n"
                       + "Please tell the developer and include the log files.\n";

            throw new RuntimeException(str, e);
        }
        
        // get a decoded String representation
        String Path = uri.getSchemeSpecificPart();
            // from terminal file:/Users/kurt/Documents/_shared ww/icontrol~subversion/Icontrol/dist/Icontrol.jar!/icontrol/instruments
            // from NB /Users/kurt/Documents/_shared ww/icontrol~subversion/Icontrol/build/classes/icontrol/instruments

        // log decoded uri
        m_Logger.log(Level.FINE, "decoded URI = {0}\n", uri);
            
        // make a Regex pattern & matcher
        Pattern p = Pattern.compile("file:(.*Icontrol.jar)!(.*)");
        Matcher m = p.matcher(Path);


        // check if it's a jar file
        if ( m.matches() ) {
            // yes, iC was started from the .jar, hence, all classes reside in a jar file

            // get jar file name
            Path = m.group(1);

            // log path
            m_Logger.log(Level.FINE, "path to jar file = {0}\n", Path);


            // make a new JarFile object
            JarFile jarFile;
            try {
                jarFile = new JarFile(Path);    
                
            } catch (IOException e) {
                String str = "Could not access the instruments package.\n"
                        + "The jar file " + Path + "\n"
                        + "appears to be invalid. Please report this incident to the developer\n"
                        + "and include the log files.\n";
                throw new RuntimeException(str, e);
            }

            // get entries in the jar file
            Enumeration<JarEntry> entries = jarFile.entries();
            

            // iterate through all entries
            while (entries.hasMoreElements()) {
                
                // get the next entry name
                String entryName = entries.nextElement().getName();
                // icontrol/instruments/Agilent4155.class - includes other files as well
                
                // log the jar entry
                m_Logger.log(Level.FINEST, "jar entry = {0}\n", entryName);
                
                // convert to a dot-form
                entryName = entryName.replace('/', '.');

                if (entryName.startsWith(PackageName) ) {

                    // remember the class name
                    ClassNames.add(entryName);

                    // log the class name
                    m_Logger.log(Level.FINEST, "class name in jar = {0}\n", entryName);
                }
            }

        } else {
            // no, iC was started from Netbeans IDE, hence, all classes reside in a directory

            // log path
            m_Logger.log(Level.FINE, "path to instruments directory = {0}\n", Path);


            // make a new file to this dir
            File PackageDir = new File( Path );

            // log PackageDir
            m_Logger.log(Level.FINE, "package dir = {0}\n", PackageDir.getPath());

            // Get the list of the files contained in the package
            ClassNames.addAll( Arrays.asList( PackageDir.list() ) );
            
            // add full package name
            ListIterator<String> it = ClassNames.listIterator();
            while (it.hasNext()) {
                it.set( PackageName + "." + it.next() );
            }
        }    
        
        
        ////////////////////
        // filter ClassNames
        
        // Class names to exclude
        // (not really necessary because these classes don't bare the proper iC_Annotation)
        final List<String> ToExclude = Arrays.asList("icontrol.instruments.Device.class",
                              "icontrol.instruments.GPIB_Driver.class", "icontrol.instruments.GPIB_NI.class",
                              "icontrol.instruments.GPIB_Prologix.class", "icontrol.instruments.iC_GenericGPIB.class");

        
        // get an Iterator
        ListIterator<String> it = ClassNames.listIterator();
        
        // iterate through all class names
        while ( it.hasNext() ) {
            
            // get current class name
            String name = it.next();
            
            // check if name is to be excluded
            // Previous comment: Device.class is still included when started from NB
            if ( ToExclude.contains(name) ) {
                    // remove it from the list
                    it.remove();
                    continue;
            }

            
            // exclude inner classes
            if (name.contains("$")) {
                it.remove();
                continue;
            }

            /* It can happen that executing iC with a corrupt
             * ~/.netbeans/version/var/cache directory creates classes
             * <error>.class in the build directory (and apparently
             * also in the dist directory) which leads to an error,
             * so avoid them explicitly.
             */
            if (name.contains("<error>")) {
                IcontrolView.DisplayStatusMessage("Info: <error>.class found.\n"
                        + "Try deleting the build directory and ~/.netbeans/[version]/var/cache\n"
                        + "(close Netbeans first).\n", false);
                
                it.remove();
                continue;
            }

            // does it end with .class?
            if ( !name.endsWith(".class") ) {
                it.remove();
                continue;
            }
            
            // remove .class from the class name
            it.set( name.replace(".class", "") );
        }
        
        
        
        // prevent null-pointer exception
        if (ClassNames.isEmpty()) {
            String str = "Did not find any Class Names in the specified package directory.\n"
                    + "Please tell the developer and include the log files.\n";
            
            throw new RuntimeException(str);
        }



        // log all Class names
        for (String str : ClassNames) {
            //IcontrolView.DisplayStatusMessage(str+"\n", false);

            // log class name
            m_Logger.log(Level.FINEST, "Found Class {0} in package instruments.\n", str);
        }

        
        ///////////////////////
        // register the classes
        
        // make a new dummy Device Instrument to access loadGenericGPIBInstruments
        Device dummyDevice = new Device();
        
        Class c;
        
        // iterate through all class names in the instruments package
        for (String ClassName : ClassNames) {
            try {
                // interestingly my Netbeans crashes when ClassName is <empty>.class
                // catching all Throwable-s below does not help ?!?
                c = Class.forName(ClassName);

            } catch (ClassNotFoundException ex) {
                // if the class is not found, there is not much to do other
                // then check the next class. It should, anyway never occur
                IcontrolView.DisplayStatusMessage("Warning: Class not found ("
                        + ClassName + ")\n", false);

                continue;
            }

            ////////////////////
            // get iC_Annotation
            // see the remark to SupressWarning("unchecked") in HandleMakeCommand()
            @SuppressWarnings("unchecked")
            iC_Annotation anno = (iC_Annotation) c.getAnnotation(iC_Annotation.class);

            // if iC_Annotation is present, check value of InstrumentClassName
            String InstrumentClassName = "";
            if (anno != null) {
                InstrumentClassName = anno.InstrumentClassName();
            }

            /////////////////////////////////////////////
            // register the class if a name was specified
            if ( !InstrumentClassName.isEmpty() ) {

                // register the instrument
                m_RegisteredInstruments.put(InstrumentClassName, ClassName);

                // log the registration
                m_Logger.log(Level.CONFIG, "Registered {0}\n", ClassName);
            }
        }

        // This was the old way:
        //m_RegisteredInstruments.put("Agilent 4155", "icontrol.instruments.Agilent4155");

           

        ////////////////////////////////////////
        // register all Generic GPIB Instruments
        
        // get path to generic GPIB Instrument dir
        String PathGenericGPIB = m_iC_Properties.getPath("iC.DefaultPathGenericInstruments",
                    "$user.home$/iC/Generic GPIB Instruments");
        
        // make a new file to this dir
        File GenericGPIBDir = new File( PathGenericGPIB );

        // log PackageDir
        m_Logger.log(Level.CONFIG, "Generic GPIB dir = {0}\n", GenericGPIBDir.getPath());

        // Get the list of the files contained in the package
        File[] GenericGPIBInstrumentFiles = 
                GenericGPIBDir.listFiles(new FilenameFilter(){
                    public boolean accept(File dir, String name) {
                        return name.toLowerCase().contains(".gpibinstrument");
                    }
                });     
        
              
        // when generic GPIB Instruments were found then (prevent null pointer exception)
        if (GenericGPIBInstrumentFiles != null) {
            try {
                // iterate through all files containing .GPIBinstrument
                for (File f : GenericGPIBInstrumentFiles) {

                    // load the definition and add it to m_GenericGPIBInstruments
                    dummyDevice.loadGenericGPIBInstruments(f);
                }
            } catch (ScriptException ex) {

                // show a dialog
                JOptionPane.showMessageDialog(m_GUI.getComponent(), ex.getMessage(),
                    "Parsing error", JOptionPane.ERROR_MESSAGE, m_iC_Properties.getLogoSmall());
            }
        }
        
        //register all generic GPIB Instruments that have not already been registered
        dummyDevice.registerGenericGPIBInstruments(m_RegisteredInstruments);

    }//</editor-fold>

    /**
     * Call this to stop processing the script. This 'signal' is also
     * propagated to the Device class, hence, all Instruments can stop
     * processing any thread they might run (for instance <code>MonitorTemp</code>.
     * After propagating the Stop signal to the devices, this method waits
     * 200 ms before returning to ensure that the Stop signal is processed in
     * all dependent Devices and threads.
     *
     * @see icontrol.instruments.iC_Instrument#MonitorChart(float, String, String, String, String, String, String, String)
     */
    // <editor-fold defaultstate="collapsed" desc="Stop Scripting">
    public void StopScripting() {

        // remember to stop sequencing in the Dispatcher
        m_StopSequencing = true;

        // propagate the signal to the devices
        Device.StopScripting();

        // wait a bit just in case some devices might take a bit to react on the
        // stop signal. so far there was no problem not waiting, but better save
        // than sorry
        try {Thread.sleep(200);} catch (InterruptedException ex) { /* ignore */ }

    }//</editor-fold>


    /**
     * This is the actual thread that processes the command strings. It splits each
     * Command Line into Tokens, and calls either <code>HandleMakeCommand</code>,
     * <code>HandleIncludeCommand</code>, or dispatches the Command Line to
     * <code>Device#DispatchCommand</code>. It catches ScriptExceptions and
     * IOExceptions and performs some clean up after the Script has been processed.
     */
    // <editor-fold defaultstate="collapsed" desc="run">
    @Override
    public void run() {
        
        /////////////////
        // init variables

        // no error occurred
        m_ErrorOccurred = false;
        
        // don't stop sequencing
        m_StopSequencing = false;

        // no lines have been INCLUDED yet
        m_IncludedLines = 0;

        // no included lines have been processed yet
        int CorrectedLine = 0;

        // use a working copy of the CommandList
        ArrayList<String> CommandList = new ArrayList<String>( m_CommandList );
        
        // make a temporary Device object to be able to call 
        // DisptachCommand and executePython
        Device dev = new Device();
        
        
        // restart Python Interpreter in case Python is still running from 
        // a previous run
        try {
            // build the message shown in the Python prompt
            String str = "--- Restarting Python Interpreter ---\\n";
            if (m_SyntaxCheckMode) {
                str += "--- Starting Syntax check ---\\n";
            } else {
                str += "--- Syntax Check was sucessful ---\\n";
            }
            
            // reset the Interpreter (only done when it was started before)
            if (dev.getPythonInterpreter() != null) {
                dev.resetPythonInterpreter(str);
            }

        } catch (ScriptException ex) {
            // log event
            m_Logger.severe(ex.toString());

            // build the String
            String str = "An error occurred when reseting the Python Interpreter:\n"
                    + ex.toString();
            
            // show a dialog
            JOptionPane.showMessageDialog(m_GUI.getComponent(), ex.getMessage(),
                    "Python Error", JOptionPane.ERROR_MESSAGE, m_iC_Properties.getLogoSmall());

        }


        // start the stopwatch
        Device.tic();
        
        // init Python Command line for consecutive Python commands
        String PythonCommandsLine = "";


        // iterate through all command lines
        for (int i=0; i < CommandList.size(); i++ ) {

            // check for pause button
            m_GUI.isPaused(true);

            // check for Auto-Pause after each script line
            // don't check if in SyntaxCheckMode
            if ( !m_SyntaxCheckMode && m_GUI.isAutoPaused()) {
                // yes, Auto-Pausing is enabled, so process this script line
                // and re-enable Pause
                m_GUI.Pause();

                // also display Command Line which will be processed
                IcontrolView.DisplayStatusMessage("now processing: " + CommandList.get(i) + "\n", false);
            }

            // display command line which will be / is being processed
            m_GUI.DisplayStatusLine(CommandList.get(i), false);


            // exit sequencing when m_StopScripting flag is set
            if (m_StopSequencing)
                break;

            // log current script line
            if ( !m_SyntaxCheckMode ) {
                m_Logger.log(Level.FINE, "now processing: {0}\n", CommandList.get(i));
            }


            /////////////////////////////
            // catch all ScriptExceptions (and others)
            // and shown them to the user
            try {

                // split the Command Line into it's Tokens
                ArrayList<String> Tokens = Tokenizer( CommandList.get(i) );


                ////////////////////////////////
                // let's use nice variable names

                // the string corresponding to the commmand respectively
                // to the VariableName of the Instrument
                String FirstToken = Tokens.get(0);

                // optional Arguments
                ArrayList<String> Arguments = new ArrayList<String>(Tokens);
                Arguments.remove(0);


                /* Correct linenumbers for INCLUDE:
                 * INCLUDE increases m_IncludedLines for each line in the Sub-Script
                 * and inserts them at the position following the INCLUDE command.
                 * As long as not all those included lines have been processed, the
                 * correct line number to present to the user in case of an error is
                 * the one with the INCLUDE command itself, so the m_CorrectedLine
                 * counter needs to be adapted whenever a command line is processed.
                 * There is a bug: if an error is in the last line of an INCLUDED
                 * script, the line number for inside the scipt is not shown.
                 */
                if (m_IncludedLines > 0) {
                    // not all lines that have been INCLUDED as a Sub-Script
                    // have yet been processed, so correct the m_CorrectLines counter
                    m_IncludedLines--;
                    CorrectedLine++;
                }


                ///////////////////
                // process commands

                // empty command string?
                if (FirstToken.isEmpty()) {
                    // just go to the next command string line
                    continue;
                }

                // comment line?
                if ( FirstToken.startsWith("%") || FirstToken.startsWith("//")) {
                    // ignore this line and go to the next command line
                    continue;
                }
                
                /////////////////////////////////////
                // handle consecutive Python commands
                // PythonInterpreter requires that indented command lines are
                // sent as one string
                // http://old.nabble.com/PythonInterpreter-question-td27760276.html
                
                // get the current line
                String line = CommandList.get(i).replaceFirst("\\s*", "");
                
                // does it start with '|'?
                if ( line.startsWith("|")) {
                    
                    // remove the '|' from the CommandLine and
                    // add it to the Python Commands Line
                    PythonCommandsLine += line.replaceFirst("\\|", "") + "\n";
                
                    // does the next line also start with '|' (commented: or is it an empty line?)
                    if ( i+1 < CommandList.size() && 
                         ( CommandList.get(i+1).replaceFirst("\\s*", "").startsWith("|") /*||
                           CommandList.get(i+1).replaceFirst("\\s*", "").isEmpty()*/ ) ) {

                        // yes, so let's get the next line via the for-loop
                        continue;
                    } else {

                        // no, so execute the Python lines
                        dev.execPython(PythonCommandsLine);
                        
                        // reset PythonCommandsLine
                        PythonCommandsLine = "";

                        // go to the next command line
                        continue;
                    }
                }
                

                // for debugging: display the reconstructed Command Line from the Tokens
                if (false) {
                    IcontrolView.DisplayStatusMessage(CommandList.get(i) + "\n", false);
                    IcontrolView.DisplayStatusMessage(Tokens.get(0) + " ", false);
                    for (int ii=1; ii<Tokens.size()-1; ii++) {
                        IcontrolView.DisplayStatusMessage(Tokens.get(ii) + ";", false);
                    }
                    IcontrolView.DisplayStatusMessage(Tokens.get(Tokens.size()-1) + "\n\n", false);
                }


                ////////////////////////
                // is it a MAKE command?
                if ( FirstToken.equalsIgnoreCase("MAKE") ) {
                    // handle what is to be done in a separate method
                    HandleMakeCommand(Arguments);

                    // go to the next command line
                    continue;
                }

                ////////////////////////////
                // is it an INCLUDE command?
                else if(FirstToken.equalsIgnoreCase("INCLUDE")) {
                    // handle what is to be done in a separate method
                    HandleIncludeCommand(CommandList, i, Arguments);
                }


                //////////////////////////////////////////
                // does the Command address an Instrument?
                //
                // A ScriptException is thrown if the command does not address
                // an instrument, hence, this else-branch should be last in the
                // series of this if-else structure
                else {
                    
                    // call Device's DispatchCommand method
                    // see Remark in javadoc (How to write new Instrument-Classes)
                    dev.DispatchCommand(CommandList.get(i));
                }


            /////////////////////////////
            // catch all ScriptExceptions
            // show a message to the user and end processing of script
            } catch ( ScriptException ex ) {

                // stop scripting after an error occurred
                StopScripting();
                
                // get the message of the exception
                String str = ex.getMessage();

                // correct line number for INCLUDED Sub-Scripts
                int Line = i + 1 - CorrectedLine;

                // append line number
                str += "\n" + "Line " + Integer.toString(Line);

                // append line number of Sub-Script if any
                // because of the +1 the line number of an error that occurred in
                // the last line of an INCLUDED script is not shown to the user.
                if ( m_IncludedLines > 0 ) {
                    str += " (line " + Integer.toString(m_IncludedLines+1) + " from the end)";
                }

                // append command line
                str += ": " + CommandList.get(i);

                // append info that scripting will be stopped
                str += "\n\nProcessing of the script will be stopped.";

                // remember that an error occurred
                // used to supress the message to the user
                m_ErrorOccurred = true;

                // log event
                m_Logger.severe(str);

                // log Stack trace
                m_Logger.severe(printStackTrace(ex));
                

                // show the dialog
                JOptionPane.showMessageDialog(m_GUI.getComponent(), str,
                        "Script Error", JOptionPane.ERROR_MESSAGE, m_iC_Properties.getLogoSmall());

                // exit the dispatcher thread by exiting the for-loop
                break;

            } catch (IOException ex) {

                // stop scripting after an error occurred
                StopScripting();                

                // remember that an error occurred
                // used to supress the message to the user
                m_ErrorOccurred = true;

                // log event
                m_Logger.severe(ex.getMessage());

                // log Stack trace
                m_Logger.severe(printStackTrace(ex));

                // show a dialog
                JOptionPane.showMessageDialog(m_GUI.getComponent(), ex.getMessage(),
                        "I/O Error", JOptionPane.ERROR_MESSAGE, m_iC_Properties.getLogoSmall());

                // exit the dispatcher thread by exiting the for-loop
                break;
            }
        } // end iterate through all command lines

        ///////////
        // clean up

        // in Syntax-Check mode there should not be anything that needs to be
        // stopped, but stop it anyways, just in case
        StopScripting();



        // run Close() method of all used Instruments if they are neither in
        // Syntax-Check Mode, nor in No-Cmmunication Mode
        for (Device device : m_UsedInstruments.values()) {
            if ( !m_SyntaxCheckMode && !device.inNoCommunicationMode()) {
                try {
                    // clean-up the Instrument
                    device.Close();
                    
                    // close the connection to the Instrument
                    device.CloseInstrument();
                    
                } catch (IOException ex) {
                    String str = "Error when closing the Instrument " + device.toString() + "\n";
                    str += ex.getMessage();
                    
                    // log event
                    m_Logger.severe(str);

                    // show a dialog
                    JOptionPane.showMessageDialog(m_GUI.getComponent(), str,
                            "I/O Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        // find the first Instrument that it not in No-Communication mode
        // and call CloseController to close the connection to the
        // Communication device (GPIB controller)
        for (Device device : m_UsedInstruments.values()) {
            if ( !m_SyntaxCheckMode && !device.inNoCommunicationMode()) {
                try {
                    // close the connection to the GPIB controller
                    device.CloseController();

                    break;

                } catch (IOException ex) {
                    String str = "Error when closing the connection to the Controller.\n";
                    str += ex.getMessage();

                    // log event
                    m_Logger.severe(str);

                    // show a dialog
                    JOptionPane.showMessageDialog(m_GUI.getComponent(), str,
                            "I/O Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }


        // release all used instruments
        m_UsedInstruments.clear();
        

        // start GarbageCollection
        System.gc();


        // display a Status Message only if in Syntax-check mode
        if (m_SyntaxCheckMode) {
            if (m_ErrorOccurred)
                IcontrolView.DisplayStatusMessage("Syntax check failed.\n");
            else
                IcontrolView.DisplayStatusMessage("Script syntax check successful.\n");
        }

        // call a method in the View-class that cleans up the GUI
        // when an error occurred, or after the script has been processed
        if (!m_SyntaxCheckMode || m_ErrorOccurred) {
            m_GUI.DoneScripting( m_ErrorOccurred );
        }
    }//</editor-fold>



    /**
     * Splits the given String (which represents a Script-Command) into it's Tokens.
     * The Script-Command consists of either 1) a Framework-Command (e.g. MAKE, INCLUDE)
     * followed by an Argument-List or 2) an Instrument-Name separated followed
     * by a Device-Command and an Argument-List.<br>
     * A space character separates the Framework-Command from it's Argument-List,
     * and also the Device-Command from it's Arguments-List. The entries in the
     * Argument-List are separated by <code>DELIMITER</code> which is currently
     * defines as a semicolon (;).<p>
     *
     * Any leading and trailing white spaces are removed from the Tokens. Also, if
     * part of the Command Line is enclosed within double-quotes ("), any DELIMITER
     * found inside this region is disregarded. The double-quotes are not removed from
     * the Tokens, therefore, the double-quotes appear in the Table when the Command
     * Line is selected in the GUI. Enclosing part of the Command Line within
     * double-quotes is important for Instrument Commands that expect a Command
     * Line as input parameter as, for instance, the method
     * <code>Agilent4155.Measure</code>.<p>
     *
     * To use <code>DELIMITER</code> (;) or double-quotes (") in a script-command,
     * use escape those characters (\; or \").<p>
     *
     * Example:<br>
     *  CommandLine = MAKE Tsample  Lakeshore 340; GPIB9<br>
     *  Tokens = "MAKE", "Tsample", "Lakeshore 340", "GPIB9"<br>
     *  Command Line = PA Measure 0;   V3,  I3,V2 , I2; .tra\;ns; "Test ; Test"<br>
     *  Tokens = "PA", "Measure", "0", "V3,  I3,V2 , I2", ".tra;ns", "Test ; Test"<br>
     *
     * @param CommandLine is split into Tokens
     *
     * @return ArrayList of String with the Tokens where leading and trailing
     * white spaces have been removed, double-quotes are still present, escaped
     * semicolons appear as semicolons.
     *
     */
    // <editor-fold defaultstate="collapsed" desc="Tokenizer()">
    static public ArrayList<String> Tokenizer(final String CommandLine) {

        /* define the script-commands that consist of only one word followed
         * by the Argument List.
         */
        final List<String> ONE_WORD_COMMANDS = Arrays.asList("MAKE", "INCLUDE");

        // create the return object
        ArrayList<String> Tokens = new ArrayList<String>();

        // do not overwrite the passed String
        String  Line = CommandLine;

        // remove all characters after a comment (% or //)
        // http://www.regextester.com/
        // old (wrong) Regex was [//[%]].+ (removed Path (/) from INCLUDE /Users/kurtp/iC Test/incl.py
        Line = Line.replaceAll("(//|%).+", "");

        // remove leading and trailing whitespaces of the command string
        //Line.trim(); somehow does not work
        Line = Line.replaceAll("^\\s+", "").replaceAll("\\s+$", "");

        /* I am quite confident that there would be a regular expression
         * able to split the entire Command Line in a very elegant way.
         * Unfortunately, I am not aware of this solution, but would be
         * delighted to see a solution. So if you are, or want to become, a
         * specialist in Regex, please let me know once you found a solution.
         */


        // split string after first whitespace
        String[] Token = Line.split("\\s", 2);
        // Token1[0] contains the Framework-Command or Instrument-Name
        // Token1[1] contains all parameters (the rest of the string)

        // assign first Token to the ArrayList that is returned
        Tokens.add(Token[0]);

        // check if first Token is a one-word Command
        char Delimiter;
        if (ONE_WORD_COMMANDS.contains(Token[0])) {
            // init next delimiter to be DELIMITER
            Delimiter = DELIMITER;
        } else {
            // init next delimiter to be a space
            Delimiter = ' ';
        }


        // if there is a remaining string
        if ( Token.length > 1 ) {
            
            // use nice variable names
            Line = Token[1];

            // trim whitespace
            Line = Line.replaceAll("^\\s+", "").replaceAll("\\s+$", "");

            // Line now contains the Device Command with optional Parameters
            // traverse the Sting character by character
            boolean split = true;
            for (int i=0; i < Line.length(); i++){

                // check if an unescaped double-quote (") is found
                if (Line.charAt(i) == '\"' && (i > 1 ? Line.charAt(i-1) != '\\' : true) ) {
                    // yes, so flip the flag to split the String
                    split = !split;
                }

                // check if a DELIMITER or the End of Line is found when split==true
                if ( split == true && 
                     Line.charAt(i) == Delimiter ) {

                    // don't split if DELIMITER is escaped
                    if (i > 1 && Line.charAt(i-1) == '\\')
                        continue;

                    // get the new Token
                    String NewToken = Line.substring(0, i);

                    // replace escaped DELIMITER with DELIMITER
                    NewToken = NewToken.replaceAll("\\\\"+DELIMITER, Character.toString(DELIMITER));

                    // replace escaped double quotes with double quotes (")
                    NewToken = NewToken.replaceAll("\\\\\"", "\"");

                    // add the new Substring to Tokens
                    Tokens.add( NewToken );

                    // remove that substring from the Line
                    if (Line.length() > i) {
                        // get the remainder of the Command Line
                        Line = Line.substring(i+1);
                    } else {
                        // this is when the semicolon is at the end of the String
                        Line = "";
                    }

                    // start parsing form the beginning of the String
                    // i gets incremented by the for-loop
                    i = -1;

                    // set the next delimiter to be DELIMITER
                    Delimiter = DELIMITER;
                }
            }

            // add the remainder (which does not contain a DELIMITER)
            Tokens.add(Line);
        }

        


        // remove leading and trailing whitespaces of the tokens
        // OLD: and double quotes (") at the beginning and the end
        for (int i=0; i<Tokens.size(); i++) {
            Tokens.set(i, Tokens.get(i) .replaceAll("^\\s*", "")
                                        .replaceAll("\\s*$", "")
                                        //.replaceFirst("^\"", "")
                                        //.replaceFirst("\"$", "")
                                        );
        }




        
        // for development: display the reconstructed Command Line from the Tokens
        // displaying the reconstructed Command Line can also be enabled in run()
        if (false) {
            IcontrolView.DisplayStatusMessage(CommandLine + "\n", false);
            IcontrolView.DisplayStatusMessage(Tokens.get(0) + "|", false);
            for (int ii=1; ii<Tokens.size()-1; ii++) {
                IcontrolView.DisplayStatusMessage(Tokens.get(ii) + "|", false);
            }
            IcontrolView.DisplayStatusMessage(Tokens.get(Tokens.size()-1) + "~\n\n", false);
        }
        
        return Tokens;
    }//</editor-fold>


    /**
     * Handles a 'MAKE' command. <p>
     * General structure: <br>
     * MAKE InstrumentName InstrumentClass; GPIB address or<br>
     * MAKE InstrumentName InstrumentClass; RS232 port parameters or<br>
     * MAKE InstrumentName InstrumentClass; URL (IP address or host name)
     * <p>
     *
     * The Argument string is split into tokens and a new object of the
     * specified Instrument is created and stored in a HashMap.
     * Some error checking if the script command is correct is also done.<p>
     *
     * The InstrumentName is case-sensitive and must not contain spaces.<p>
     *
     * @param Arguments contains the parameters for the MAKE command
     *
     * @throws ScriptException for various reasons. Use <code>getText</code> to
     * see details about what caused the exception.
     * @throws IOException when communication with the Instrument failed.
     *
     * @see icontrol.instruments.Device#OpenGPIB
     */
    // <editor-fold defaultstate="collapsed" desc="HandleMakeCommand()">
    private void HandleMakeCommand(ArrayList<String> Arguments )
            throws ScriptException, IOException {

        // local variables
        CommPorts   CommPort;
        int         GPIBAddress = -1;
        String      ComPortName = "";   // for RS232 communication
        int         BaudRate = 0;    
        int         DataBits = 0;
        int         StopBits = 0;
        String      Parity = "";
        URL         InstrumentURL = null;   // for network connection

        // check number of arguments
        if (Arguments.size() != 3) {
            throw new ScriptException("Expected 3 arguments but found " +
                    Integer.toString(Arguments.size()) + ".\n");
        }


        /////////////////////
        // extract parameters
        // with the proper data type and nice names
        // Also do some error checking


        // Instrument-Name:
        // variable name for the Instrument (used as key to the HashMap)
        // for instance 'Tsample'
        String InstrumentName = Arguments.get(0);

        // error checking
        if (InstrumentName.isEmpty()) {
            String str = "You need to specify a name for the instrument.\n";
            m_Logger.severe(str);
            throw new ScriptException(str);
        }

         // make sure the the Instrument-Name was not used before
        if (m_UsedInstruments.containsKey(InstrumentName)) {
            String str = "The instrument name \"" + InstrumentName + "\" is already in use. ";
            str += "Please choose a different name.\n";
            m_Logger.severe(str);
            throw new ScriptException(str);
        }



        // Instrument class name
        // for instance 'Lakeshore 340'
        String InstrumentClassName = Arguments.get(1);

        // error checking
        if (InstrumentClassName.isEmpty()) {
            String str = "You need to specify an instrument class.\n";
            m_Logger.severe(str);
            throw new ScriptException(str);
        }

        // make sure the the Instrument-Class was registered
        if ( !m_RegisteredInstruments.containsKey(InstrumentClassName)) {
            String str = "The instrument that you have tried to MAKE is not supported.\n";
            str += "Correct the spelling of \"" + InstrumentClassName;
            str += "\" or implement a new instrument driver.\n";
            m_Logger.severe(str);
            throw new ScriptException(str);
        }


        // get the actual name of the class
        String InstrumentClass = m_RegisteredInstruments.get(InstrumentClassName);

        // error check
        if (InstrumentClass == null) {
            String str = "An unexpected error occurred in HandleMakeCommand: InstrumentClassName is null.\n"
                + "The Instrument-Class did not register with a valid class name. "
                + "Check Dispatcher.RegisterDeviceClass().\n"
                + "Please consider reporting this incident to the developer, and"
                + "include the log-file and your script.\n";
                        
            throw new ScriptException(str);
        }



        
        // which protocoll?
        if (Arguments.get(2).startsWith("GPIB")) {
            ///////
            // GPIB

            try {
                // remove all non-digits and get GPIB number
                GPIBAddress = Integer.parseInt(Arguments.get(2).replaceAll("[^0-9]", ""));

            } catch (NumberFormatException ex) {
                throw new ScriptException("Could not interpret the GPIB address. Please check spelling.\n");
            }

            // error check
            if (GPIBAddress < 0 || GPIBAddress > 31) {
                throw new ScriptException("The GPIB address needs to be between 0 and 31.\n");
            }

            // select GPIB protocol
            CommPort = CommPorts.GPIB;

        } else if (Arguments.get(2).startsWith("none")) {

            ///////////////////
            // no communication

            CommPort = CommPorts.none;

        } else if (Arguments.get(2).startsWith("COM")) {

            ////////
            // RS232
            
            // compile the Regex pattern for COM1, 9600, 8, 1, none
            Pattern pattern = Pattern.compile("(\\w+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d)\\s*,\\s*([a-zA-Z]+)");

            // match pattern
            Matcher m = pattern.matcher(Arguments.get(2));

            // correct arguments found?
            if ( m.find() ) {
                ComPortName = m.group(1);
                BaudRate = Device.getInteger(m.group(2));
                DataBits = Device.getInteger(m.group(3));
                StopBits = Device.getInteger(m.group(4));
                Parity = m.group(5);

            } else {
                String str = "Could not extract RS232 port name, baud rate, start/stop bits and parity from\n"
                        + Arguments.get(2) + "\n"
                        + "Please use a format like COM1, 9600, 8, 1, none\n";
                throw new ScriptException(str);
            }
                
            // select RS232 protocol
            CommPort = CommPorts.RS232;

        } else if (Arguments.get(2).startsWith("URL")) {
            ///////////////
            // LAN / TCP-IP

            // get URL (without URL=)
            String HostAdr = Arguments.get(2).replaceFirst("\\s*URL\\s*\\s*=\\s*", "");
            
            // display Status message that networking is not supported by any instruments
            // TODO @LAN delme when an instrument uses LAN communication
            String dummy = "WARNING: You have chosen network communication (URL=...) which is not supported by any instrument yet.\nThe script will likely not work as anticipated.\n";
            IcontrolView.DisplayStatusMessage(dummy, false);
            
                        
            // store the URL
            // TODO @LAN define protocol, port number, and file in Instrument.properties
            try {
                InstrumentURL = new URL("http", HostAdr, -1, "");
                
            } catch (MalformedURLException ex) {
                String str = "The specified URL\n"
                        + HostAdr + "\n is malformed:\n"
                        + ex.getMessage() + "\nPlease correct the URL and try again.\n";
                throw new ScriptException(str);
            }


            // select LAN protocol
            CommPort = CommPorts.LAN;

        }// use else if here for more protocols
        else {
            // an un-supported communication protocol was selected
            // if new protocolls are added, adapt this message
            String str = "An unsupported communication protocol was selected.\n";
            str += "Please select GPIB, RS232 or LAN.\n";

            throw new ScriptException(str);
        }


        ////////////////////////
        // instantiate the class
        try {
            
            // get the class
            Class theClass = Class.forName(InstrumentClass);

            // ensure the class is derived from class Device
            // interestingly, the getSuperclass returns Device, not java.lang.Object
            //if ( theClass.getSuperclass() != Device.class) {
            // updated 110211
            // theClass instanceof Device: instanceof needs an object of the class, but theClass is of type Class
            if ( !Device.class.isAssignableFrom(theClass)) {
                // it appears that the class was not derived from Device
                // so throw a ScriptException
                String str = "The Instrument-Class '" + InstrumentClass
                    + "' is not derived from Device as required.\n"
                    + "Please tell the developer to derive it from Device.\n";

                throw new ScriptException(str);
                /*
                 * this is bookmark BM_LiftRequirement (see also Device class)
                 * I think if the Instrument class is not derived from Device,
                 * and this requirement should be relaxed, then I would implement
                 * an interface, say DeviceInterface, which requires OpenInstrument, etc.
                 * methods, and confirm that class Device as well as the Instrument-class
                 * in question implements this interface.
                 * The code should then check if the class implements that interface
                 * to justify the use of @SuppressWarnings("unchecked") at
                 * the invokation of the constructor below.
                 */
            }
    


            /* For debugging: get all constructors and set them accessible
             *
             * Remark: the return type of getConstructors() is Constructor<?>[] and
             * not Constructor<T>[], hence, a cast is necessary to prevent
             * the compiler warning; see javadoc of getConstructors()
             * Remark: this cast generates a compiler warning which is suppressed,
             * because the above if-satement ensures that 'theClass' is derived from Device.
             */
            //@SuppressWarnings("unchecked")
            //Constructor<Device>[] allc = (Constructor<Device>[])theClass.getConstructors();
            //AccessibleObject.setAccessible(allc, true);


            /* get appropriate constructor
             * the constructor is public and accessible and should also be
             * visible (checked during debugging with allc above)
             *
             * Remark : this cast generates a compiler warning which is manually
             * suppressed, because the above if-satement ensures that 'theClass'
             * is derived from class Device.
             */
            //old: Constructor<Device> c = theClass.getConstructor( ParamType );
            @SuppressWarnings("unchecked")
            Constructor<Device> c = (Constructor<Device>) theClass.getConstructor();
            

            
            // instantiate the device
            Device theDevice = (Device) c.newInstance();
            
            // set the Instrument-Class-Name of the instance to enable the instance
            // to find the generic GPIB Instrument that might implement generic GPIB methods
            theDevice.setInstrumentClassName(InstrumentClassName);


            
            /* find the Annotation with the promise of which
             * communication ports/protocols the class supports
             * 
             * Remark:
             * The compiler generates a warning possibly because it cannot ensure
             * that iC_Annotation.class is indeed an annotation type. Therefore, it
             * should be save to @SuppressWarnings("unchecked") because iC_Annotation
             * is an Annotation (Annotations cannot extend other classes, which would
             * probably be required to tell the compiler that iC_Annotation is
             * an Annotation type)
             */
            @SuppressWarnings("unchecked")
            iC_Annotation anno = (iC_Annotation)theClass.getAnnotation(iC_Annotation.class);

            
            // ensure there is no runtime error when no iC_Annotation was given
            boolean     supported = false;
            if (anno != null) {
                // get the supported communication ports/protocols
                CommPorts SupportedCommPorts[] = anno.CommPorts();

                // check if the chosen port is supported
                for (CommPorts p : SupportedCommPorts) {
                    if (p==CommPort)
                        supported = true;
                }
            }


            // show a message if port is not supported
            if (!supported) {
                String str = "The Instrument class \"" + InstrumentClass;
                str += "\" does not declare to support the \"" + CommPort.toString() + "\" port/protocol.\n";
                str += "Please choose a different port or implement it in the Instrument class.\n";
                m_Logger.severe(str);
                throw new ScriptException(str);
            }


            // open the connection to the Instrument, and
            // tell the Instrument it's own name
            // for GPIB instruments the RS232 parameters are ignored
            theDevice.OpenInstrument(InstrumentName, CommPort, GPIBAddress,
                    ComPortName, BaudRate, DataBits, StopBits, Parity,
                    InstrumentURL);
            

            // call the open method for further initializations
            // if not in Syntax-Check mode and if not in No-Communication mode
            if ( !m_SyntaxCheckMode && !theDevice.inNoCommunicationMode())
                theDevice.Open();

            // put it into the HashMap
            m_UsedInstruments.put(InstrumentName, theDevice);


        } catch (InvocationTargetException ex) {
            m_Logger.log(Level.SEVERE, "Dispatcher.HandleMakeCpmmand: InvokationTargetException", ex);

            // just re-throw the exception as IOException
            throw new IOException(ex.getCause());
            
        } catch (ScriptException ex) {
            // just rethrow ScriptExceptions
            throw ex;
                
        } catch (Exception ex) {
            String str = "An error occurred in Dispatcher.HandleMakeCommand (incl. Open()).\n";
            str += "You might want to report this incident to the developer. If you\n"
                 + "choose to do so, please also send the log-files and the script.\n\n";
            str += ex.toString() + "\n";

            m_Logger.severe(str);
            throw new ScriptException(str);
        }
         
        // display a status message if not in SyntaxCheckMode
        if (!m_SyntaxCheckMode) {
            String str = "Made " + InstrumentName + " (" + InstrumentClassName;
            str += " @" + CommPort.toString() + "#" + Integer.toString(GPIBAddress) + ")\n";

            IcontrolView.DisplayStatusMessage(str, false);
        }
    }//</editor-fold>



    /**
     * Handles an 'INCLUDE' command. <p>
     * General structure: INCLUDE ScriptFile<br>
     * ScriptFile can either be a 'regular' iC script or a Python script.
     * <p>
     *
     * The ScriptFile can contain a fully qualified path. If no path is
     * specified (no file with only ScriptFile as file name is found, the file
     * to include is first searched in the Project Directory, then in the default
     * directory specified in 
     * <code>iC_Properties.getPath("iC.DefaultPath", "$user.home$/iC")</code>).
     * If no file extension was specified (ScriptFile does not contain
     * a '.', the default extension '.iC' is added. Sub-Scripts can MAKE new
     * Instruments and these Instruments are now shown in the GUI's 
     * Instrument-List. If the FileName ends in .py, the script is executed by 
     * the Python Interpreter.<p>
     *
     * The command lines contained in the specified script are included in the
     * script at the line following the INCLUDE command. The new command lines are
     * not shown to the user. In case of an error, the line number in the Sub-Script
     * might not be correct if two INCLUDE commands are closer together than the line count
     * of the first Sub-Script, or if the error occurs in the last line of the
     * INCLUDED script. It's not so important because the line is shown
     * anyways, so I will ignore this for the moment.<p>
     * 
     * 
     * This method is also called from the view to properly handle sub-scripts 
     * on load.
     *
     * @param CommandList contains the Strings comprising the original Script
     *
     * @param Index the index of the INCLUDE command in the CommandList.
     *
     * @param Arguments contains the file name of the script file to include
     *
     * @throws ScriptException when 1) the script file could not be found, or 2)
     * an IO-error occurred during reading from the file, or 3) a Python error
     * occurred during executing a Python Script.
     */
    // <editor-fold defaultstate="collapsed" desc="Handle Include Command">
    protected void HandleIncludeCommand(  ArrayList<String> CommandList,
                                        int Index,
                                        ArrayList<String> Arguments )
            throws ScriptException {

        // check number of arguments
        if (Arguments.size() != 1) {
            throw new ScriptException("Expected 1 argument but found " +
                    Integer.toString(Arguments.size()) + ".\n");
        }


        // use nice variable names
        String IncludeName = Arguments.get(0);
                
        // append default extension if none was specified
        if ( !IncludeName.contains(".")) {
            IncludeName += ".iC";
        }
        
        // start with this file name
        String FileName = IncludeName;

        // add the next command after the Include command itself
        Index++;
        
        
        // start searching for the include file (was full path specified?)
        File f = new File(FileName);
        m_Logger.log(Level.FINE, "Searching sub-script to INCLUDE in path: {0}\n", FileName);
        
        // check if file exists (full path was specified)
        if ( !f.exists() ) {
            // search in Project Path
            FileName = m_GUI.getProjectPath() + IncludeName;
            m_Logger.log(Level.FINE, "Searching sub-script to INCLUDE in path: {0}\n", FileName);
            
            // does the file exist in the project path?
            f = new File(FileName);
            if ( !f.exists() ) {
                // no, search in default iC path
                FileName = m_iC_Properties.getPath("iC.DefaultPath", "$user.home$/iC") + 
                        System.getProperty("file.separator") + IncludeName;
                m_Logger.log(Level.FINE, "Searching sub-script to INCLUDE in path: {0}\n", FileName);
                
                // does the file exist in the default iC path?
                f = new File(FileName);
                if ( !f.exists() ) {
                    // file not found
                    String str = "The script file\n'" + FileName + "'\n"
                            + "could not be found either directly (full path was specified),\n"
                            + "nor in the Project directory or the default iC directory.\n";
                    throw new ScriptException(str);
                }
            }   
        }
        
        
        
        ////////////////////////////////
        // check if it's a Python script
        
        // does the name end in .py?
        if (FileName.toLowerCase().endsWith(".py")) {
            
            // yes, so let's make a new device to get access to the Python Interpreter
            Device dev = new Device();
            
            
            // start Python Interpreter by printing a message
            dev.execPython("print 'Now Starting to execute " + FileName + "'");
            
            try {
                // execute the Python Script
                dev.getPythonInterpreter().execfile(FileName);
                
            } catch (PyException ex) {
                
                // build the string
                String str = "Error executing the INCLUDED Python script\n" 
                        + FileName + "\n" + ex.getMessage();
                
                // re-throw as Script Exception
                throw new ScriptException(str);
                
            }
            
            // finished
            return;
        }


        ///////////////////////
        // Include as iC script
        
        // open file
        BufferedReader fr;
        try {
            fr = new BufferedReader(new FileReader(FileName));
            
            // display status msg to the user
            IcontrolView.DisplayStatusMessage("Included file " + FileName + "\n", false);

        } catch (FileNotFoundException ex) {
            String str = "The script file\n'" + FileName + "'\n could not be opened.\n\n";
            str += "The error message received was:\n";
            str += ex.getMessage() + "\n";

            throw new ScriptException(str);
        }

        // include all command lines
        String line="";
        try {
            while ( (line = fr.readLine()) != null ) {

                // append the line
                CommandList.add(Index, line);

                // remember to add the next line at the next index
                Index++;

                // increase the counter of INCLUDED lines
                m_IncludedLines++;
            }

        } catch (IOException ex) {
            String str = "Could not read from file\n" + FileName + "\n.\n";
            str += "reported cause: " + ex.getMessage() +"\n";

            throw new ScriptException(str);
        }

    }//</editor-fold>


    /**
     * Returns a String with the StackTrace. This method is similar to
     * <code>Throwable.printStackTrace</code> but the output is a String rather
     * than a <code>PrintStream</code>. If the passed <code>Throwable</code> has
     * a cause, then the Stack Trace of the Cause is also listed.<p>
     *
     * The returned String is formatted for use with a Logger, for instance
     * <code>m_Logger</code>.<p>
     *
     * This method is <code>static</code> so that it can also be accessed from
     * <code>IcontrolView</code> where the default uncaught exception handler
     * is defined.
     *
     * @param ex The <code>Thwowable</code> for which the Stack trace is desired.
     *
     * @return The Stack Trace of <code>ex</code> and of <code>ex</code>'s cause 
     * if a cause is present.
     */
    // <editor-fold defaultstate="collapsed" desc="print Stack Trace">
    public static String printStackTrace(Throwable ex) {

        String StackTrace = "";

        // get the Stack trace of the Exception
        if (ex != null) {
            StackTrace = "STACK TRACE:\n";
            StackTraceElement[] ste = ex.getStackTrace();
            for (int t = 0; t < ste.length; t++) {
                StackTrace += "\tat " + ste[t].getClassName() + "." + ste[t].getMethodName()
                           + " (" + ste[t].getFileName() + ":"
                           + Integer.toString(ste[t].getLineNumber()) + ")\n";
            }
        } else {
            StackTrace += "\tNo Throwable present.\n";
        }

        // get the Stack trace of the Cause of the Exception
        if (ex.getCause() != null) {
            StackTrace += "\tSTACK TRACE of the Cause:\n";
            StackTraceElement[] ste = ex.getCause().getStackTrace();
            for (int t = 0; t < ste.length; t++) {
                StackTrace += "\tat " + ste[t].getClassName() + "." + ste[t].getMethodName()
                           + " (" + ste[t].getFileName() + ":"
                           + Integer.toString(ste[t].getLineNumber()) + ")\n";
            }
        } else {
            StackTrace += "\tNo Cause present.\n";
        }

        // return the Stack Trace
        return StackTrace;
    }//</editor-fold>
    
}
