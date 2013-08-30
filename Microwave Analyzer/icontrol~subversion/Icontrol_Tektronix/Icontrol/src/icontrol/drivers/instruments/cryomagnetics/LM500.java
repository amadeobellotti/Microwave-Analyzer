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
package icontrol.drivers.instruments.cryomagnetics;

import icontrol.AutoGUIAnnotation;
import icontrol.IcontrolView;
import icontrol.Utilities;
import icontrol.drivers.Device;
import icontrol.drivers.Device.CommPorts;
import icontrol.iC_Annotation;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;
import javax.script.ScriptException;

/**
 * This class implements functionality to communicate with a 
 * Cryomagnetics LM-500 Liquid Cryogen Level Monitor.
 *
 * <h3>Instrument Commands:</h3>
 * <ul>
 *  <li>{@link #Measure(int) }
 *  <li>{@link #Refill(float) }
 * </ul><p>
 * 
 * <h3>Some peculiarities of this Instrument-Class:</h3>
 * <ul>
 *  <li>The LM-500 echoes all RS232 commands, hence, the <code>SendToInstrument</code>
 *      and <code>QueryInstrument</code> methods have been overridden.
 * </ul><p>
 *
 * @author KPP (Kurt Pernstich: pernstich@alumni.ethz.ch)
 * @version 0.1
 */
// promise that this class supports RS232 communication
@iC_Annotation( CommPorts=CommPorts.RS232,
                InstrumentClassName="Cryomagnetics LM 500")
public class LM500 extends Device {
    
    
    /**
     * Overridden <code>SendToInstrument</code> method that reads back the echoed
     * response of the LM-500. It compares the response to the send one and throws
     * an Exception if they are different to indicate a transmission error. The
     * <code>Message</code> to be sent is appended with a <code>\r</code> (RETURN)
     * to "execute" the command on the LM-500.
     * 
     * @param Message that will be logged and sent to the Instrument, appended with
     * a \r (return).
     * @throws IOException if the transmission caused a communication error; can
     * bubble up from the RS232 driver, or when the echoed response is not
     * identical to <code>Message</code>
     */
    // <editor-fold defaultstate="collapsed" desc="SendToInstrument">
    @Override
    public void SendToInstrument(String Message)
                    throws IOException {
               
        // send the message and query the response
        String Echo = super.QueryInstrument(Message + "\r");
        
        // trim \r\n from response
        Echo = Echo.trim();
        
        // check echo
        if ( !Echo.equals(Message) ) {
            // transmission faild
            String str = "The echoed response of the LM-500 was different from\n"
                    + "what was sent. Please check the connection to the LM-500.\n";
            
            throw new IOException(str);
        }
    }// </editor-fold>
    
    
    /**
     * Overridden <code>QueryInstrument</code> method which sends a string via 
     * the chosen communication port (which is RS232) to the Instrument (a 
     * <code>\r</code> is added before sending to "execute" the command at the
     * LM-500), and reads the Instrument's reply. The reply contains the echoed 
     * Message and it is stripped off before returning.<p>
     * 
     * Note that if <code>Message</code> contains characters with a special 
     * meaning for Regex (?, \, $, ^, ...), this method needs to be adapted 
     * because, as of now, only the ? is handled appropriately when stripping off
     * the echoed response.
     *
     * @param Message is sent via RS232 with an appended \r (RETURN).
     * @return Returns the string read from the instrument where the echoed
     * <code>Message</code> has been stripped off, or an empty String if
     * in No-Communication-Mode.
     * @throws IOException if the transmission caused a communication error; bubbles
     * up from the RS232 driver or if the response did not contain the echoed
     * Message, hence, a transmission error occurred.
     */
    // <editor-fold defaultstate="collapsed" desc="QueryInstrument">
    @Override
    protected String QueryInstrument(String Message)
                    throws IOException {
        
        String ret = "unassigned";
             
        // send the message and query the response
        String Answer = super.QueryInstrument(Message + "\r");
             
        // get a regex compatible version of Message 
        // i.e. replace ? with \?
        String MessageRegexd = Message.replaceAll("\\?", "\\\\?");
        
        // build a pattern and matcher
        Pattern p = Pattern.compile("^" + MessageRegexd + "\\r\\n(.*)\\r\\n");
        Matcher m = p.matcher(Answer);
        
        // find a match
        if (m.find()) {
            ret = m.group(1);
        } else {
            // transmission faild
            String str = "The LM-500's response did not contain the echoed Message\n"
                    + "that what was sent or processing the command did not complete (missing newline).\n"
                    + "Please check the connection to the LM-500.\n";
            
            throw new IOException(str);
        }
                
        // return the result
        return ret;
    }// </editor-fold>
    
    /**
     * Returns the last measurement of the cryogen level of the  selected channel.
     * 
     * @param Channel The channel which should be measured; can be 1 or 2
     * @return The current liquid cryogen level
     * @throws IOException Bubbles up from <code>SendToInstrument</code> or from
     * <code>QueryInstrument</code>
     * @throws DataFormatException When the Syntax-check fails
     * @throws ScriptException When conversion of the answer into a double value
     * failed; bubbles up from <code>Utilities.getDouble</code>
     */
    // <editor-fold defaultstate="collapsed" desc="Measure">
    @AutoGUIAnnotation(
        DescriptionForUser = "Measures the liquid cryogen level in displayed units.",
        ParameterNames = {"Channel {1 or 2}"},
        DefaultValues = {"1"},
        ToolTips = {""})
    @iC_Annotation(MethodChecksSyntax=true)
    public double Measure(int Channel) 
           throws IOException, DataFormatException, ScriptException {
        
        ///////////////
        // Syntax-Check

        if (Channel < 1 || Channel > 2) {
            String str = "Channel must be 1 or 2\n";
            throw new DataFormatException(str);
        }


        // return if in Syntax-check mode
        if (inSyntaxCheckMode())
            return Double.NaN;

        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return Double.NaN;
        
        
        // start a measurement (somewhat optional)
        //SendToInstrument("MEAS " + Channel);
        
        // get a measurement
        String Answer = QueryInstrument("MEAS? " + Channel);
        
        // strip off the unit: remove everything other than a number or a .
        String NumberOnly = Answer.replaceAll("[^0-9\\.]", "");
        
        // convert to a Double
        double ret = Utilities.getDouble(NumberOnly);
               
        return ret;
    }// </editor-fold>
    
    
    /**
     * Measures the current  cryogen level of the specified channel and compares
     * with <code>ThresholdLevel</code>. If the cryogen level is below 
     * <code>ThresholdLevel</code> Auto-Refilling is started. When Auto-Refilling
     * is started, this method waits a time specified in the iC.properties (180 sec)
     * after starting the refill and before returning. If the current
     * cryogen level cannot be measured, a notification is shown to the user but
     * processing of the script continues.
     * 
     * @param Channel The channel which should be measured; can be 1 or 2
     * @param ThresholdLevel If the cryogen level of channel <code>Channel</code> is below 
     * <code>Level</code>, Auto-Refilling is started
     * @throws IOException Bubbles up from <code>SendToInstrument</code> or from
     * <code>QueryInstrument</code>
     * @throws DataFormatException When the Syntax-check fails
     */
    @AutoGUIAnnotation(
        DescriptionForUser = "Starts Auto-Refill if measured cryogen level is below a threshold.",
        ParameterNames = {"Channel {1 or 2}", "Threshold level"},
        DefaultValues = {"2", "40"},
        ToolTips = {"", "in display units, e.g. %"})
    @iC_Annotation(MethodChecksSyntax=true)
    public void Refill(int Channel, double ThresholdLevel) 
           throws IOException, DataFormatException {
        
        
        ///////////////
        // Syntax-Check
                
        if (Channel < 1 || Channel > 2) {
            String str = "Channel must be 1 or 2\n";
            throw new DataFormatException(str);
        }

        // return if in Syntax-check mode
        if (inSyntaxCheckMode())
            return;

        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return;
        

        
        // measure cryogen level
        double CurrentLevel;
        try {
            CurrentLevel = Measure(Channel);
            
        } catch (ScriptException ex) {
            // could not convert the answer to a double
            
            // display (and log) event
            String str = "Error: Could not measure the cryogen level. Auto-Refill "
                    + "will not be started. Let's hope there was enough cryogen left.\n";
            m_GUI.DisplayStatusMessage(str);
            
            // return without stop scripting
            return;
        }
        
        
        // compare levels
        if (CurrentLevel > ThresholdLevel) {
            // no need to refill, so exit
            return;
        }
        
        
        // start Auto-Refill
        SendToInstrument("FILL " + Channel);
        
        // get wait time
        int WaitTime = m_iC_Properties.getInt("LM500.WaitAfterRefill", 180);
        
        // get current Status Bar String (need to display it again)
        String StatusBarText = m_GUI.DisplayStatusLine("", false);
        m_GUI.DisplayStatusLine(StatusBarText + " - waiting " + WaitTime + " sec", false);
        
        // wait for refilling to be finished
        try{Thread.sleep(1000*WaitTime);} catch (InterruptedException ignore) {}
    }
}
