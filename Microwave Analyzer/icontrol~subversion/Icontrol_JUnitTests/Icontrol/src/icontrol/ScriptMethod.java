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

import static icontrol.Utilities.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.script.ScriptException;

/**
 * This class defines a data structure for a Script-Method (a method that is called
 * for a Script-Command with the same name). It can store programmatic methods as
 * well as generic methods. Use <code>isGenericGPIB</code> to distinguish between the two.<p>
 * 
 * Note: Because this class is more like a new data type (a la typedef in C++)
 * it's members are declared public without explicit getter and setter methods.<p>
 * 
 * Note: This class was originally defined as inner class in <code>Device</code>,
 * but in <code>IcontrolView</code> no instance of <code>Device</code> existed,
 * hence, <code>ScriptMethod</code> could not be instantiated; so it was moved
 * to this separate file.
 * 
 * @author KPP (Kurt Pernstich: pernstich@alumni.ethz.ch)
 * @version 0.2
 */
public class ScriptMethod 
        implements Comparable<ScriptMethod> {

    /** The Logger for this class */
    private static final Logger m_Logger = Logger.getLogger("iC.ScriptMethod");
    
    /** Defines the character used to delimit entries in the definition of generic methods */
    protected final String DELIMITER = "|";
        
    ////////////////////
    // the method itself
    
    /** The name of the Method. This name is the same as used in the script. */
    public String       DeviceCommandName;
    
    /** Stores a reference to the method. The type is either <code>Method</code>
     * or <code>GenericGPIBMethod</code>. */
    public Method       ReferenceToMethod;
    
    /** The types of the arguments this method accepts */
    public Class<?>[]   ParameterTypes;
    
    /** Flag that stores whether or not the method was defined as a generic GPIB
     * Instrument or as programmatic method. It is <code>false</code> per default,
     * and set to <code>true</code> in <code>parseDefinition</code>. */
    public boolean      isGenericGPIB = false;
    
    
    ////////////
    // GPIB part
    
    /** This String is used with <code>String.format</code> to build the string
     * which is sent via GPIB to perform the desired action */
    public String GPIBString;
    
    //////////
    // AutoGUI
    
    /** This flag stores whether or not the script-method should be included in 
     * the AutoGUI, that is, if the method either bares an AutoGUI annotation (for
     * programmatic methods) or the corresponding fields were specified for generic
     * methods. */
    public boolean      AutoGUI;
    
    /** A detailed description of the method which is shown to the user as ToolTip
     * text. It is never <code>null</code>. */
    public String       DescriptionForUser;
    
    /** The name of the arguments this method accepts. These names are used by the
     * AutoGUI to fill the table. It is never <code>null</code>. */
    public String[]     ParameterNames;
    
    /** The ToolTip text for each argument the method accepts. It is never 
     * <code>null</code>. */
    public String[]     ToolTips;
    
    /** The default values shown in the AutoGUI for each argument the method 
     * accepts. It is never <code>null</code>. */
    public String[]     ParameterDefaultValues;
    
    
    ///////////////
    // Syntax-check
    
    /** This flag stores whether or not the method performs a Syntax-Check */
    public boolean      MethodChecksSyntax = false;
    

    /** Holds the min/max value allowed for this Parameter. The type can be
     * <code>double</code> or <code>int</code>. */
    public Number[]     ParameterMinValue;
    public Number[]     ParameterMaxValue;
    
    /** Flag that indicates if a min/max range for the Parameter has been assigned,
     * and a Range Check should be performed */
    public boolean[]    ParameterRangeCheck;
    
    

    /**
     * This overridden <code>toString</code> method is used by the 
     * <code>DefaultListModel</code> to obtain the text which is to be
     * displayed in the Device-Command list.
     * 
     * @return The name of the method.
     */
    // <editor-fold defaultstate="collapsed" desc="toString">
    @Override
    public String toString() {
        return DeviceCommandName;
    }//</editor-fold>


    /**
     * Overridden <code>equals</code> method that compares the DeviceCommandName of
     * two <code>ScriptMethod</code> objects using the standard 
     * <code>Stirng.equals</code> method.<p>
     * It is only "loosely" implemented because it's original purpose was 
     * to find the entry in the Command List in <code>jScriptCommandSelected</code>
     * which only searches for the DeviceCommandName.
     * 
     * @param obj The object to compare with
     * @return <code>true</code> if <code>obj</code> is of type 
     * <code>ScriptMethod</code> and if both <code>DeviceCommandName</code>s are
     * equal; returns <code>false</code> otherwise.
     */
    // <editor-fold defaultstate="collapsed" desc="equals">
    @Override
    public boolean equals(Object obj) {

        // check if obj is of the correct type
        if (obj instanceof ScriptMethod) {

            // Note: after the instanceof check, obj is never null
            // http://hanuska.blogspot.com/2006/08/tricky-instanceof-operator.html
            return this.DeviceCommandName.equals( ((ScriptMethod)obj).DeviceCommandName );
        } else {
            return false;
        }

    }//</editor-fold>


    /**
     * Whenever <code>equals</code> is overridden, it is recommended to override
     * <code>hashCode</code> as well so that equal object return equal hash codes.
     * Because <code>equals</code> only check for equality of the field
     * <code>DeviceCommandName</code>, the hash code is calculated for this field only.
     * 
     * @return The hashCode for this object, calculated from the 
     * <code>DeviceCommandName</code> field only.
     */
    // <editor-fold defaultstate="collapsed" desc="hash Code">
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 47 * hash + (this.DeviceCommandName != null ? this.DeviceCommandName.hashCode() : 0);
        return hash;
    }//</editor-fold>

    
    //http://www.regular-expressions.info/java.html
    /**
     * Parses a String that defines a Device-Command of a generic GPIB Instrument.
     * This String is split into Tokens at the delimiter defined in <code>DELIMITER</code>
     * The result of the parsing is stored in the instance's member variables.<p>
     * 
     * The fields in the String to define a Device-Command for a generic GPIB 
     * Instrument are separated by | and have the following order and meaning:
     *
     * 1) Device-Command-Name {description for the user}<br>
     * This name must not contain spaces and it is used in the Script as a command 
     * to perform the desired action. The description for the user (which is shown 
     * as a Tool Tip for the Device-Command) is optional and enclosed in {}.
     *
     * 2) GPIB String<br>
     * Use %d for a decimal integer value, %s for a String value, %e or %E or
     * %f for a double value, and %b or %B for a boolean value. Format specifiers
     * such as %.4e are be allowed.
     *
     * 3) Parameter-Name {description for the user} [min value, max value] (default value)<br>
     * The Parameter-Name is shown to the user in the Table of the Auto-GUI, and
     * the description as a Tool Tip. The min/max value are used in the Syntax-Check
     * to ensure the value specified by the user is within the range. The default
     * value is used to fill the Table in the Auto-GUI if the user selects this
     * Device-Command.<br>
     * [], [], and () are all optional and can be given in any order.
     * 
     * 4) more Parameter-Names if the GPIB String contains more than one %-character
     * 
     * 5) if the Device-Command-Name starts with 'save', then the last field is
     * interpreted as a File-Extension to save the data returned by the Instrument
     * to a corresponding file.<p>
     * 
     * Note that all fields can include escaped version of |, {}, [], and (). The
     * escaped version will be substituted by the un-escaped character so that all
     * characters are available.<p>
     * 
     * Examples:
     * setTemp_G | SETP %d,%.3f | Loop# {can be 1 or 2} [1, 2] (1) | Temperature {the new Set Point temperature in K} [295, 300] (295)<br>
     * getTemp_G | KRDG? %.1s | Input Channel {can be A, B, C, or D} (A)<br>
     * saveIDN_G {saves the Identification query to a file} | *IDN? | File Extension {will be added to the File Name}<br>
     * More examples can be found in iC/Generic GPIB Instruments directory.
     *
     * @param Line The Strings containing the definition of the generically defined Device-Command.
     * 
     * @throws ScriptException If parsing the <code>Tokens</code> failed. The 
     * Exception's message contains details about what caused the failure.
     */
    // <editor-fold defaultstate="collapsed" desc="parse Definition">
    //public void parseDefinition(ArrayList<String> Tokens) 
    public void parseDefinition(String Line) 
           throws ScriptException {
        
        /////////////////////////////////////////////////////////////
        // split line at the delimiters but not on escaped delimiters

        // copile the Regex pattern
        Pattern pattern = Pattern.compile("[^\\\\][" + DELIMITER + "]");

        // split the line
        String[] dummy = pattern.split(Line, 0);

        // make a new Tokens list
        ArrayList<String> Tokens = new ArrayList<String>();

        // iterate through all Tokens
        for (String str : dummy) {
            // replace escaped delimiter (\|) with delimiter (|)
            // and add to the Tokens List
            Tokens.add(str.replaceAll("[\\\\][|]", "|"));
        }
                
        
        //////////////
        // error check
        if (Tokens.size() < 1) {
            String str = "Did not find a Device-Command-Name for the generic Script-Command.\n";
            throw new ScriptException(str);
        }

        if (Tokens.size() < 2) {
            String str = "Did not find a GPIB String for the generic Script-Command.\n";
            throw new ScriptException(str);
        }


        /////////////////////////////////////////////////
        // get device command name + description for user

        // get the device command name
        DeviceCommandName = Tokens.get(0)
            .replaceAll("\\{.*\\}", "") // remove everything enclosed in { } incl. {}
            .replaceAll("^\\s+", "")    // remove leading whitespace
            .replaceAll("\\s+$", "");   // remove trailling whitespace

        // check if device command name contains whitespaces
        if (DeviceCommandName.contains(" ")) {
            String str = "The Device Command Name must not contain white spaces.\n"
                    + "Please remove them from '" + DeviceCommandName + "'.\n";
            throw new ScriptException(str);
        }


        // get everything inside {} 
        Pattern p = Pattern.compile("([^\\\\][\\{])(.*[^\\\\])([\\}])");   // the last } is taken as group 3, not a \} inside { }
        Matcher m = p.matcher(Tokens.get(0));
        if ( m.find() ) {
            DescriptionForUser = m.group(2)
                //.replaceAll("\\\\\\{", "{")     // replace \{ with {
                //.replaceAll("\\\\\\}", "}");    // replace \} with }
                .replaceAll("\\\\([\\{\\[\\(\\}\\]\\)])", "$1"); // replace escaped {[()]} with un-escaped version
        } else {
            DescriptionForUser = "";
        }




        //////////////
        // GPIB String

        // get GPIB String
        GPIBString = Tokens.get(1)
                .replaceAll("^\\s+", "")    // remove leading whitespace
                .replaceAll("\\s+$", "");   // remove trailling whitespace;

        // count number of (unescaped) % signs
        int NrPercent = GPIBString.split("[^\\\\][%]").length - 1;
        
        // correction if Device Command Name starts with 'save'
        int SaveCorrection = 0;
        if ( DeviceCommandName.toLowerCase().startsWith("save") ) {
            SaveCorrection = 1;
        }
        
        // check if correct number of Parameter-Names have been supplied
        if (Tokens.size() - 2 - SaveCorrection < NrPercent) {
            String str = "Too few Parameter-Names were specified for the given GPIB String.\n"
                + "The number of %-signs does not match the number of Parameter Names.\n";
            throw new ScriptException(str);
        }
        if (Tokens.size() - 2 - SaveCorrection > NrPercent) {
            String str = "Too many Parameter-Names were specified for the given GPIB String.\n"
                + "The number of %-signs does not match the number of Parameter Names.\n";
            throw new ScriptException(str);
        }
        
        
        // reserve space for the Parameters
        ParameterTypes          = new Class<?>[NrPercent + SaveCorrection];
        ParameterNames          = new String[NrPercent + SaveCorrection];
        ToolTips                = new String[NrPercent + SaveCorrection];
        ParameterDefaultValues  = new String[NrPercent + SaveCorrection];
        ParameterMinValue       = new Number[NrPercent + SaveCorrection];
        ParameterMaxValue       = new Number[NrPercent + SaveCorrection];
        ParameterRangeCheck     = new boolean[NrPercent + SaveCorrection];
        
        
        // build the pattern + matcher
        String Converters = "sSdeEfbB";   // ([^\\][%])([^sSdeEfbB]*)([sSdeEfbB])
        p = Pattern.compile("([^\\\\][%])([^" + Converters + "]*)([" + Converters + "])");
        Matcher mGPIB = p.matcher(GPIBString);

        /////////////////////////////////
        // iterate through all parameters
        char Converter;
        for (int i=0; i < NrPercent + SaveCorrection; i++) {
            
            
            // distinguish between the File-Name and a Parameter for the GPIB String
            if ( i == NrPercent ) {
                // this parameter is for the FileName
                Converter = 's';
            } else {
                // this parameter is for the GPIB String
                
                // find next match
                if ( !mGPIB.find() ) {
                    // no match was found
                    String str = "The GPIB String could not be matched to the Regex.\n"
                            + "Please check the GPIB String and try again.\n";
                    throw new ScriptException(str);
                } else {
                    // get the converter character
                    // the returned string can only contain one character
                    Converter = mGPIB.group(3).toLowerCase().charAt(0);
                }
            }
            
            
            
                
            // build the Parameter-Type
            switch (Converter) {
                
                // String
                case 's':
                    ParameterTypes[i] = String.class;
                    break;
                    
                case 'd':
                    ParameterTypes[i] = Integer.TYPE;
                    break;
                    
                case 'e':
                case 'f':
                    ParameterTypes[i] = Double.TYPE;
                    break;
                    
                case 'b':
                    ParameterTypes[i] = Boolean.TYPE;
                    break;
                    
                default:
                    // Format Converter not recognized
                    String str = "The Format-String " + m.group(0) + "\n"
                            + "could not be interpreted. '" + Converter
                            + "' is not a recognized data type converter.\n";
                    
                    throw new ScriptException(str);     
            }
            
            // get next Token which contains a Parameter-Name++ definition
            String ParameterString = Tokens.get(i+2);
            
            // get the Parameter-Name
            // TODO 1* fix RegEx to not eat characters 
            // as e.g. in OpenMEM | :OPEN (@ M%d) | Memory Number {can be 1 to 40}[1,40](36)
            ParameterNames[i] = ParameterString
                .replaceAll("[^\\\\][\\{].*[^\\\\][\\}]", "") // remove everything enclosed in { } incl. {} but not \{ or \}
                .replaceAll("[^\\\\][\\[].*[^\\\\][\\]]", "") // remove everything enclosed in [ ] incl. [] but not \[ or \]
                .replaceAll("[^\\\\][\\(].*[^\\\\][\\)]", "") // remove everything enclosed in ( ) incl. () but not \( or \)
                .replaceAll("^\\s+", "")    // remove leading whitespace
                .replaceAll("\\s+$", "")    // remove trailling whitespace
                .replaceAll("\\\\([\\{\\[\\(\\}\\]\\)])", "$1"); // replace escaped {[()]} with un-escaped version
            
            
            // get ToolTip for the Parameter ( everything inside {} )
            p = Pattern.compile("([^\\\\][\\{])(.*[^\\\\])([\\}])");
            m = p.matcher(ParameterString);
            if ( m.find() ) {
                ToolTips[i] = m.group(2)
                    .replaceAll("\\\\([\\{\\[\\(\\}\\]\\)])", "$1"); // replace escaped {[()]} with un-escaped version;
            } else {
                ToolTips[i] = "";
            }
             
            
            // get Range for the Parameter ( everything inside [] )
            // a non-escaped-[ +or- digit.digit , digit.digit ]
            p = Pattern.compile("([^\\\\]?\\[)([+-]?\\d+\\.?\\d*),([+-]?\\d+\\.?\\d*)\\]");
            m = p.matcher(ParameterString.replaceAll("\\s+", "")); // remove whitespaces
            if ( m.find() ) {
                
                // switch according to data type
                switch (Converter) {
                    case 'd':
                        ParameterMinValue[i] = getInteger(m.group(2));
                        ParameterMaxValue[i] = getInteger(m.group(3));
                        break;
                    default:
                        ParameterMinValue[i] = getDouble(m.group(2));
                        ParameterMaxValue[i] = getDouble(m.group(3));
                }
                
                
                // remember to perform a Range Check for this Parameter
                ParameterRangeCheck[i] = true;

                // remember that at least one parameter range is checked
                MethodChecksSyntax = true;
                
            } else {
                // don't perform a Range Check for this Parameter
                ParameterRangeCheck[i] = false;
            }


            // get Default Value for the Parameter ( everything inside () )
            // a non-escaped-( +or- digit.digit , digit.digit )
            p = Pattern.compile("[^\\\\]\\((.*)\\)");
            m = p.matcher(ParameterString.replaceAll("\\s+", "")); // remove whitespaces
            if ( m.find() ) {
                ParameterDefaultValues[i] = m.group(1);
            } else {
                ParameterDefaultValues[i] = "0";
            }
        } // end for

        // TODO 2* replace all escaped-% with % in GPIB String
        
        
        // remember that this ScriptMethod is a generic GPIB method
        isGenericGPIB = true;
        
        // set that this method should be included in the AutoGUI
        AutoGUI = true;
        
        // log result of parsing for development
        String str = "Method Name: " + DeviceCommandName
                   + "\nDescription for User: " + DescriptionForUser
                   + "\nGPIB String: " + GPIBString
                   + "\nParameter Types: "; for (Class<?> type : ParameterTypes) str += type.getSimpleName() + " | ";
              str += "\nParameter Names: "; for (String s : ParameterNames) str += s + " | ";
              str += "\nToolTips: "; for (String s : ToolTips) str += s + " | ";
              str += "\nDefault Values: "; for (String s : ParameterDefaultValues) str += s + " | ";
              str += "\nParameter Min: "; for (Number n : ParameterMinValue) str += n + " | ";
              str += "\nParameter Max: "; for (Number n : ParameterMaxValue) str += n + " | ";
              str += "\nRange check: "; for (boolean b : ParameterRangeCheck) str += b + " | ";
              str += "\n\n";

        m_Logger.finer(str);
        //m_GUI.DisplayStatusMessage(str, false);

    }//</editor-fold>


    /** 
     * Compares the <code>DeviceCommandName</code> of two <code>ScriptMethod</code> 
     * objects to determine their alphabetical ordering. Used to sort the entries
     * in the Device-Command-List using <code>Collections.sort</code> which
     * requires the class to implement the <code>Comparable</code> interface.
     * 
     * @param o The second object to compare.
     * @return The same as <code>String.compareToIgnoreCase</code> for the
     * <code>DeviceCommandName</code>
     */
    public int compareTo(ScriptMethod o) {
        return DeviceCommandName.compareToIgnoreCase(o.DeviceCommandName);
    }
}
