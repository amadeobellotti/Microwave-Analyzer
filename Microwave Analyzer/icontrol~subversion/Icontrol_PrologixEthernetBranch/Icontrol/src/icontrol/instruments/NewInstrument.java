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
import icontrol.IcontrolView;
import icontrol.iC_Annotation;
import icontrol.instruments.Device.CommPorts;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;
import javax.script.ScriptException;
import javax.swing.JOptionPane;

/**
 * This is the default template for new Instruments. The documentation on how
 * to implement new Instruments can be accessed from the main page of the
 * javadoc. The online version of the javadoc can be found at
 * http://icontrol.kenai.com.<p>
 *
 * Please document your class carefully:
 * NewInstrument ZYX Hypothetical Weather Controller "driver" class.<p>
 *
 * All device commands that the NewInstrument understands are
 * implemented here.<p>
 *
 * <h3>Instrument Commands:</h3>
 * <ul>
 *  <li>{@link #DemoCommand(float, String, boolean) }
 *  <li>{@link #DisplayPreviousResult() }
 *  <li>{@link #ReferenceImplementation(float, boolean, String)}
 *  <li>{@link #setTemp(float)}
 * </ul>
 *
 * @author KPP (Kurt Pernstich: pernstich@alumni.ethz.ch)
 * @version 0.3
 *
 */

// promise that this class supports GPIB communication
@iC_Annotation(CommPorts=CommPorts.GPIB,
                InstrumentClassName="New Instrument")
public class NewInstrument extends Device {


    ///////////////////
    // member variables

    /**
     * The Logger for this class. Note that the Logger Level is set in the
     * constructor of the <code>Device</code> class.
     */
    // change the Logger name to the name of your class
    private static final Logger m_Logger = Logger.getLogger("iC.Instruments.NewInstrument");


    // define your member variables here, use m_ to mark them as such, at least
    // if you like to stick to the convention used in the iC Framework.
    private int m_NewVariable;


    /**
     * To make use of the mechanism that checks the Instrument's response to a
     * *IDN? query to check if the Instrument is of the correct type, add an
     * appropriate entry to the file iC.properties found in the icontrol.resources
     * directory. The entry should consist of this class's name followed by
     * '.InstrumentNameIdentifier' (without quotes). When this nomenclature is
     * used, the default implementation of
     * <code>Device.getInstrumentNameIdentifier</code> checks if the value
     * to this key is contained in the Instrument's response to an *IDN? query.
     * Only if this String is found the Instrument is accepted, otherwise the User
     * is warned. The User can then decide whether or not to accept the Instrument
     * and also if this selection should be persistently stored in the User
     * properties. If the InstrumentNameIdentifier String is empty no *IDN?
     * query is performed. This is important for older Instruments that do not
     * implement the *IDN? command.
     * This whole mechanism is helpful if, as an example, a Lakeshore 334
     * temperature controller is used with a "driver" for a Lakeshore 340
     * temperature controller. The two command sets might be similar but because
     * they are not necessarily identical, it is the User's responsibility to accept
     * a different Instrument than the driver was designed for, and that only
     * supported Device Commands are used.
     */



    /**
     * This is an exemplary implementation of a new device command to help understanding
     * how to write new Instrument Commands. Use the reference implementation to
     * implement new Instrument Commands.<p>
     *
     * This method shows a message to the user when in SyntaxCheck mode and also
     * when the 'real' action is performed.<p>
     *
     * This method implements the Syntax-Check mode.
     *
     * @param Param1 is shown to the user.
     * @param Param2 is shown to the user.
     * @param Param3 when <code>true</code>, the SyntaxCheck will succeed, otherwise
     *        it will fail.
     *
     * @throws ScriptException when conversion of the Instrument's answer to a
     * float fails.
     *
     * @throws IOException re-thrown from {@link Device#QueryViaGPIB}
     *
     * @throws DataFormatException when the Syntax Check failed.
     *
     */
    // <editor-fold defaultstate="collapsed" desc="Demo Command">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>This is an exemplary implementation of a new method.<br>For long ToolTip texts it is helpful to use html and insert<br>line breaks<br>. For shorter texts one can skip the html tags. </html>",
        ParameterNames = {"A float value", "A string", "Syntax Check flag"},
        DefaultValues = {"1.1", "Love All, Serve All", "true"},
        ToolTips = {"for demonstration only", "", "Use this to decide if SyntaxCheck fails or succeeds."})
    @iC_Annotation(  MethodChecksSyntax = true )
    public void DemoCommand(float Param1, String Param2, boolean Param3)
                    throws ScriptException, IOException, DataFormatException {

        // Syntax check for Param 1 (just an illustrative example
        if (Param1 < -1 && Param1 > 1) {

            // Syntax check for Param1 failed
            // (which in this demo is never the case
            String str = "Syntax Check for Param1 failed.\n";
            str += "Please select a value between ...";

            // throw an Exception to indicate SyntaxCheck error
            throw new DataFormatException(str);
        }


        // Syntax check for Param 2 (just an illustrative example
        if (Param2.equalsIgnoreCase("Let's fail the test")) {

            // Syntax check for Param2 failed
            String str = "Syntax Check for Param2 failed.\n";
            str += "Please select a value between ...";

            // throw an Exception to indicate SyntaxCheck error
            throw new DataFormatException(str);
        }


        /* For demonstration purposes only, a dialog is presented when in
         * Syntax-Check Mode which let's the user decide if Syntax-Check should
         * succeed or fail to see the consequences. */
        if (inSyntaxCheckMode()) {
            String str = "Checking Syntax in NewInstrument.NewCommand\n";
            str += "Parameter 3 is " + Boolean.toString(Param3);
            str += ", therefore the Syntax check will ";
            str += (Param3 ? "succeed." : "fail.");

            JOptionPane.showMessageDialog(m_GUI.getComponent(),
                str, "Syntax Check", JOptionPane.INFORMATION_MESSAGE, m_iC_Properties.getLogoSmall());

            if (Param3 == false) {
                throw new DataFormatException("Syntax check in NewInstrument. NewCommand failed.\n");
            }
        }


        /* in a real Instrument, the method would return when in Syntax-Check
         * Mode, but for this demo, we comment these lines */
        // return if in Syntax-check mode
        if (inSyntaxCheckMode())
            return;

        /* in a real Instrument, the method would return when in No-Communication
         * Mode, but for this demo, we comment these lines */
        /* Remark: Sometimes it is convenient to return a (unrealistic) number when
         * in No-Communication-Mode to debug the rest of the program. */
        // return if in No-Communication Mode
//        if (inNoCommunicationMode())
//            return;




        ///////////////////////
        // GPIB communication ?
        if (m_UsedCommPort == CommPorts.GPIB) {

            //////////////////
            // query a command

            // build the GPIB command
            String cmd = String.format(Locale.US, "COMMAND 1, %.3f", Param1);

            // add the String
            cmd += "," + Param2;

            // add the boolean value
            cmd += (Param3 ? "ON" : "OFF");



            /* let's assume we like to wait until the queried response equals
             * Param1, and that it can take a while until this condition is met.
             * An example of such a situation would be to wait until a SetPoint
             * temperature is reached.
             */
            boolean Done = false;
            while ( !Done &&
                    m_StopScripting == false) {

                // query the command
                String ans = QueryViaGPIB(cmd);


                // in No-Communication-Mode, 'ans' will be an empty string
                // therefore, we simulate the Instrument's answer
                ans = Float.toString(Param1);


                /* Convert to float
                 * Using the predefined conversion routine is useful, because
                 * it throws a ScriptException when the conversion fails.
                 */
                float dummy = getFloat(ans);

                /////////////////////////////////
                // do something useful with dummy
                
                // check if the Instrument's answer is what we want it to be
                // to exit the method
                if (dummy == Param1) {
                    Done = true;
                }



                /* If some portion of your method takes long to execute, it's advisable
                 * to check if Scripting has been Paused or Stopped. It might be
                 * better to check for Pause and Stop condition at the beginning
                 * of the while loop, depending on the application.
                 * If you like to start a new thread to process this long task,
                 * you can define an anonymous inner class that implements Runnable
                 * as shown for instance in Lakeshore340.MonitorTemp(). The thread
                 * must terminate itself when the Stop condition is met.
                 */

                // check for pause button
                isPaused(true);
            }



            /////////////////////////////
            // for demonstration purposes

            // add a Status Message
            IcontrolView.DisplayStatusMessage("in NewCommand\n");


            // show a dialog
            String str = "\nYuhuu it works!\n";
            str += "Parameter 1 = " + Float.toString(Param1) + "\n";
            str += "Parameter 2 = " + Param2 + "\n\n";
            str += "You can simulate that an error occurred during execution.\n";
            
            Object[] ButtonLabels = { "Complete successfully", "Simulate error" };
            Integer ret = JOptionPane.showOptionDialog(m_GUI.getComponent(), str,
                                "Simulate error?", JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE, m_iC_Properties.getLogoSmall(),
                                ButtonLabels, ButtonLabels[0]);


            // check the User's answer
            if ( ret == 1) {
                // the user chose to fail the command
                str = "You have selected to simulate an error in NewInstrument.NewCommand.\n";
                str += "Please correct it and try again.\n";
                
                throw new ScriptException(str);
                
            } else {
                // the user chose to let the command succeed, so do nothing 
                // and just exit
            }
        }
    }//</editor-fold>


    /**
     * This is a reference implementation of a typical Instrument command.<p>
     *
     * This method performs a Syntax-Check.
     *
     * @param Param1 is a placeholder.
     * @param Param2 is a placeholder.
     * @param CommandLine is interpreted as a Command Line which is executed when not
     * in SyntaxCheck-Mode and the returned value is interpreted as a
     * <code>float</code>.
     *
     * @throws ScriptException when conversion of the Instrument's answer to a
     * float fails during processing the <code>CommandLine</code>.
     *
     * @throws IOException re-thrown from {@link Device#QueryViaGPIB}
     *
     * @throws DataFormatException when the Syntax Check failed.
     */
    // <editor-fold defaultstate="collapsed" desc="Reference Implementation">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>A reference implementation of<br>a Instrument Command.</html>",
        ParameterNames = {"Value 1", "Value 2", "Value 3"},
        DefaultValues = {"1.1", "true", "\"Command-Line in double quotes\""},
        ToolTips = {"TT1", "TT2", "The command to execute"})
    @iC_Annotation(  MethodChecksSyntax = true )
    public void ReferenceImplementation(float   Param1,
                                        boolean Param2,
                                        String  CommandLine)
           throws ScriptException, IOException, DataFormatException {

        // local variables
        float dummy = 0;


        // remove double-quotes from the beginning and end of the CommandLine
        CommandLine = CommandLine.replaceFirst("^\"", "").replaceFirst("\"$", "");


        //////////////////////////////////////
        // Syntax-Check + Execute Command Line

        // Execute the Command Line:
        // in Syntax-Check mode the returned object is null
        // when not in Syntax-Check mode, the object should be valid
        if ( !CommandLine.isEmpty() ) {

            // make a new Device object to call it's DispatchCommand method
            // see Remark in javadoc (How to write new Instrument-Classes)
            Device dev = new Device();

            // perform the Syntax check
            Object obj = dev.DispatchCommand(CommandLine);

            // convert to float if returned object is valid
            // old:   &&  obj.getClass() == Float.class
            if (obj != null) {

                // convert the Instrument's answer to a float
                try {
                    dummy = getFloat(obj.toString());

                } catch (ScriptException ex) {
                    // returned object is no convertible into a float, so throw an Exception
                    String str = "Executing the Command Line\n" + CommandLine + "\n";
                    str += "did not return an object that can be converted into a float value. Please check the Command Line.\n";
                    
                    throw new ScriptException(str);
                }
            }
            // the returned object is not valid, so throw an Exception when
            // not in Syntax-Check mode
            else if ( !inSyntaxCheckMode() ) {

                // returned object is null, so throw an Exception
                String str = "Executing the Command Line\n" + CommandLine + "\n";
                str += "did not return a valid object. Please check the Command Line.\n";

                throw new ScriptException(str);
            }
        }


        // perform further Syntax-Checks here
        /* if (...) {
            String str = "Descriptive Text.\n";

            throw new DataFormatException(str);
        }*/

        
        // Syntax check for a String value defined in a List
        // consider if it makes sense to define the list in iC.properties
        // see LakeshoreTC.checkInputChannel for an example
        /*final List<String> NEW_LIST =
                Arrays.asList("A", "B", "C");   // the entries need to be in uppercase letters!
        if ( !NEW_LIST.contains(YourVariableToCheck.toUpperCase()) ) {
            String str = "YourVariableToCheck '" + YourVariableToCheck + "' is not valid.";
            str += "Please select a value from:\n " + NEW_LIST.toString() + ".\n";
            throw new DataFormatException(str);
        }*/
        
        // Range check for a Float value defined in a List
        // consider if it makes sense to define the list in iC.properties
        /* final List<Float> YOUR_RANGE = Arrays.asList(-100f, 100f);

        if (YourVariableToCheck < YOUR_RANGE.get(0) || YourVariableToCheck > YOUR_RANGE.get(1)) {
            String str = "The zyx value is out of range.\n"
                    + "Please select a value between " + YOUR_RANGE.toString() + ".\n";
            throw new DataFormatException(str);
        }*/

        




        // return if in Syntax-check mode
        if (inSyntaxCheckMode())
            return;

        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return;



        // insert code to perform the desired task
        // for instance open files, calculate intermediate values, etc.


        ///////////////////////
        // GPIB communication ?
        // This check for the CommPort is only necessary if the class promises
        // to support multiple communication ports in the class' Annotation
        if (m_UsedCommPort == CommPorts.GPIB) {

            //////////////////
            // query a command

            // build the GPIB command
            String cmd = String.format(Locale.US, "COMMAND 1, %.3f", Param1);

            // add the boolean value
            cmd += (Param2 ? "ON" : "OFF");



            // if new code requires to wait until a certain criteria is met, the
            // following while-loop can be used
            boolean Done = false;
            while ( !Done && !m_StopScripting) {

                // check for pause button
                isPaused(true);


                // query the command
                String ans = QueryViaGPIB(cmd);


                /* Convert to float
                 * Using the predefined conversion routine is useful, because
                 * it throws a ScriptException when the conversion fails.
                 */
                dummy = getFloat(ans);


                // do something useful with dummy


                // check if the Instrument's answer is what we want it to be
                // to exit the method
                if (dummy == Param1) {
                    Done = true;
                }
            }
        }


        // if new code requires to start a new task, the following anonymous
        // inner class can be used. Ensure that the task terminates itself
        // upon the Stop signal.
        // See iC_Instrument.MonitorChart() for a working example displaying
        // data in a chart.

        /**
         * Implement a new class from an anonymous inner class that extends
         * Thread and implements run(), in which the actual job is done.<p>
         *
         * The thread pauses, respectively, stops when <code>m_Paused</code>, respectively,
         * <code>m_StopScripting</code> is true.
         */
        // <editor-fold defaultstate="collapsed" desc="myThread as anonymous inner class">
        class myThread extends Thread {

            // member variables
            public String       m_dummy;



            /**
             * Constructor.
             */
            public myThread(String dummy) {

                // assign the passed values
                m_dummy = dummy;
            }


            @Override
            public void run() {
                // local variables
                float dummy = 0.0f;

                // Display a status message
                IcontrolView.DisplayStatusMessage("Starting the thread.\n");

                // do until the thread should be stopped
                while ( m_StopScripting == false) {

                    // check for pause button
                    isPaused(true);

                    // do something useful here


                    // wait the desired time
                    try {
                        Thread.sleep( 500 );
                    } catch (InterruptedException ex) {}
                }

                // clean up the thread


                // Display a status message
                IcontrolView.DisplayStatusMessage("Thread stopped.\n");
            }
        }//</editor-fold>


        // make a new thread object
        myThread myT = new myThread("Love All, Serve All");

        // start the thread and return
        myT.start();
    }//</editor-fold>



    /**
     * This method demonstrates the use of the result of the succeeding script
     * command. If the result is of type float, it is displayed as Status Text
     * in the GUI.
     *
     * @throws ScriptException when the data type of the return value of the
     * previous script command was different from float.
     */
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Displays the result of the previous Script command<br>if it is of ype float.</html>",
        ParameterNames = {},
        DefaultValues = {},
        ToolTips = {})
    // <editor-fold defaultstate="collapsed" desc="Display Previous Result">
    public void DisplayPreviousResult() 
           throws ScriptException {

        // build the string
        String str = "The result of the previous Script command was: ";

        // check if the type was float
        if (m_LastReturnValue instanceof Float) {
                str += m_LastReturnValue.toString() + "\n";
        } else {
            str = "The data type of the return value of the previous script command is not float as expected.\n";
            throw new ScriptException(str);
        }


        // display the previous result as Status Text
        IcontrolView.DisplayStatusMessage(str, false);
    }//</editor-fold>


    /**
     * Reference implementation of a minimal method body. This is the example
     * listed in the publication.
     *
     * @param SetPoint The new temperature
     *
     * @throws IOException re-thrown from {@link Device#SendViaGPIB} or
     * {@link Device#QueryViaGPIB}
     *
     * @throws DataFormatException when the Syntax Check failed.
     */
    // TODO 1* add Citation
    @AutoGUIAnnotation(
	DescriptionForUser = "Sets the SetPoint temperature.",
	ParameterNames = {"Temperature [K]"},
	DefaultValues= {"295"},
	ToolTips = {"Define tool-tips here"})
    @iC_Annotation(MethodChecksSyntax = true )
    public void setTemp(float SetPoint)
       throws IOException, DataFormatException {

	// perform Syntax-check
	if (SetPoint < 0 || SetPoint > 500)
		throw new DataFormatException("Set point out of range.");

	// exit if in Syntax-check mode
	if ( inSyntaxCheckMode() )
		return;

        // build the GPIB command string
	String str = "SETP 1," + SetPoint;

	// send to the Instrument
	SendViaGPIB(str);

	// wait until setpoint is reached
        float T;
	do {
            // it is recommended to check if scripting has been paused
            isPaused(true);

            // get current temperature
            str = QueryViaGPIB("KRDG? A");

            // convert to a float value
            //T = getFloat(str);    // this is the recommended way of converting
            T = Float.parseFloat(str);
	} while ( Math.abs(T-SetPoint) > 0.1 &&
                  m_StopScripting == false);
    }
}
