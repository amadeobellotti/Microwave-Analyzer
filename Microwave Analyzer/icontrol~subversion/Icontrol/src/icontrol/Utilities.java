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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.ScriptException;

/**
 * This is collects a set of utility methods used throughout the program.<p>
 * 
 * @author KPP (Kurt Pernstich: pernstich@alumni.ethz.ch)
 * @version 0.1
 */
public class Utilities {
    
    /** The Logger for this class */
    private static final Logger m_Logger = Logger.getLogger("iC.Instruments.Utilities");
    
    
    /** Handle to the GUI. Used to access DisplayStatusMessage. */
    // TODO delme
    //GUI_Interface m_GUI;
    
    /** character used to separate individual arguments in a Script-Command.
     * It is also accessed from <code>IcontrolView</code>, so it is made static */
    protected static final char DELIMITER = ';';
    
    
    /** Constructor
     * @param GUI An implementation of the GUI_Interface interface. At the time
     * of writing, it is only required by <code>Tokenizer</code> to access the 
     * GUI's <code>DisplayStatusMessage</code>. It <code>Tokenizer</code> is not
     * used in a particular instance of <code>Utilities</code>, it can be set to
     * <code>null</code>.
     */
    // <editor-fold defaultstate="collapsed" desc="Constructor">
    // TODO delme
//    public Utilities(GUI_Interface GUI) {
//        m_GUI = GUI;
//    }//</editor-fold>
    
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
    static public float getFloat(String str)
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
    static public boolean getBoolean(String str)
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
     * Fills an ArrayList with values starting for <code>Start</code> to <code>Stop</code>
     * with either equidistant or logarithmically spaced values. If
     * <code>AppendToList==null</code> a new list is created, otherwise the new
     * values are appended to the specified list.<p>
     * 
     * This method is not static because it is used infrequently (as compared to
     * <code>getInteger</code> etc.
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
    public ArrayList<Double> GenerateValues( double Start, double Stop, double Increment,
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
            
            // calc number of steps; it's the number of points minus 1
            int NrSteps = (int)Math.round( Math.abs(Start - Stop) / Math.abs(Increment) );
            
            // ensure sign of Increment is correct
            if ( Start < Stop && Math.signum(Increment) < 0 ) {
                Increment *= -1.0;
            }
            if ( Start > Stop && Math.signum(Increment) > 0 ) {
                Increment *= -1.0;
            }
            
            // add the new values
            for (int i=0; i <= NrSteps; i++) {
                
                // calc value
                double val = Start + i * Increment;
                
                if (DecimalPlaces > 0) {
                    
                    // calc rounded value
                    double rounded = Math.round(val*RoundingFactor) / RoundingFactor;
                    
                    // add the rounded value
                    ret.add( rounded );
                    
                } else {
                    // add the 'full-length' value
                    ret.add(val);
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
                    
                    // calc the rounded value
                    double rounded = Math.floor(t*RoundingFactor) / RoundingFactor;
                    
                    // check if there is a previous value
                    if ( ret.isEmpty()) {
                        // add first value
                        ret.add(rounded);
                        
                    } else {
                        // check if previous value is the same as current value
                        if (ret.get(ret.size()-1) != rounded) {
                            // add the rounded value
                            ret.add( rounded );
                        }
                        
                    }
                    
                    
                } else {
                    // add the 'full-length' value
                    ret.add(t);
                }
            }
        }
        
        // log values for debugging
        m_Logger.log(Level.FINER, "Generated values: {0}\n", ret.toString());

        return ret;
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
    // <editor-fold defaultstate="collapsed" desc="printStackTrace">
    public static String printStackTrace(Throwable ex) {
        
        // maybe I could use something like this
        // http://stackoverflow.com/questions/1760654/java-printstream-to-string
        /*ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        ex.printStackTrace(ps);
        String StackTrace2 = baos.toString();   // could also use CharacterSet*/


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
     * This method is made static because it is accessed from the View, from 
     * <code>Dispatcher</code> and from <code>Device</code>
     *
     * @param CommandLine is split into Tokens
     *
     * @return ArrayList of String with the Tokens where leading and trailing
     * white spaces have been removed, double-quotes are still present, escaped
     * semicolons appear as semicolons.
     *
     */
    // <editor-fold defaultstate="collapsed" desc="Tokenizer">
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
//        if (false) {
//            m_GUI.DisplayStatusMessage(CommandLine + "\n", false);
//            m_GUI.DisplayStatusMessage(Tokens.get(0) + "|", false);
//            for (int ii=1; ii<Tokens.size()-1; ii++) {
//                m_GUI.DisplayStatusMessage(Tokens.get(ii) + "|", false);
//            }
//            m_GUI.DisplayStatusMessage(Tokens.get(Tokens.size()-1) + "~\n\n", false);
//        }
        
        return Tokens;
    }//</editor-fold>
    
    /**
     * This method serves as the Client Factory to return the View. When iC is
     * run as usual (not as Unit test), this method returns the instance of the
     * <code>GUI_Interface</code> returned from <code>IcontrolApp</code>. During
     * Unit tests, the <code>GUI_Interface</code> of the <code>IcontrolAppMockup</code>
     * is returned.
     * 
     * @return The <code>GUI_Interface</code> of the <code>IcontrolApp</code> or
     * of the <code>IcontrolAppMockup</code>.
     */
    // <editor-fold defaultstate="collapsed" desc="getView">
    public static GUI_Interface getView() {
        
        GUI_Interface ret;
        
        // get the View from the Application
        ret = IcontrolApp.getApplication().getView();
        
        // is iC run as Unit Test?
        if (ret == null) {         
            
            // yes, so get the GUI from the IcontrolAppMockup
            ret = IcontrolAppMockup.getView();
            
            // double check if it's assigned now
            if (ret == null) {
                String str = "An error occurred. Could not obtain the View in Utilities.getView. "
                        + "Please tell the developer.\n";
                throw new RuntimeException(str);
            }
        }
        
        // return the GUI
        return ret;
    }//</editor-fold>
    
}
