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
package icontrol.drivers.instruments.keithley;

import icontrol.AutoGUIAnnotation;
import icontrol.drivers.Device;
import icontrol.iC_Annotation;
import icontrol.drivers.Device.CommPorts;
import icontrol.iC_Properties;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;

/**
 * This class implements functionality to communicate with Keithley 7001 Switch System.<p>
 * 
 * <h3>Instrument Commands:</h3>
 * <ul>
 *  <li>{@link #CloseSwitch(String) }
 *  <li>{@link #CloseOnly(java.lang.String) }
 *  <li>{@link #DefineChannelList(String, String) }
 *  <li>{@link #OpenSwitch(String) }
 * </ul>
 *
 * @author KPP (Kurt Pernstich: pernstich@alumni.ethz.ch)
 * @version 0.1
 *
 */

// promise that this class supports GPIB communication
@iC_Annotation(CommPorts=CommPorts.GPIB,
                InstrumentClassName="Keithley 7001")
public class Keithley7001 extends Device {
    
    ///////////////////
    // member variables
    
    // store Channel List Definitions
    HashMap<String, String> m_ChannelLists = new HashMap<String, String>();

    
    /**
     * The Logger for this class. Note that the Logger Level is set in the
     * constructor of the <code>Device</code> class.
     */
    // change the Logger name to the name of your class
    private static final Logger m_Logger = Logger.getLogger("iC.Instruments.Keithley7001");

    
    /**
     * Defines the wait time after opening/closing the switch. This is to give
     * the switch enough time to close/open before any successive script command
     * starts a process which relies on the switch being opened/closed.
     */
    private static final int m_WaitTime = new iC_Properties().getInt("Keithley7001.WaitTime", 250);
      
    
    /**
     * Defines a Channel List and associates it with the specified name. This method
     * performs a Syntax-Check in which the Name-List pair is stored to enable
     * the syntax-Check of the <code>Close</code> method, but no further
     * check for correctness of <code>List</code> is performed.<p>
     * 
     * Example: <code>defineChannelList("OPV1", "1!1,2!1")</code>
     * 
     * @param Name The name of the Channel Definition. This name can be used
     * to close certain switches.
     * 
     * @param List The Channel list. For Switch Matrices the format is Slot!Row!Column
     * and for any other system the format is Slot!Channel. Use Min:Max notation
     * to specify ranges.
     * 
     * @throws DataFormatException If the Syntax-Check fails
     */
    // <editor-fold defaultstate="collapsed" desc="DefineChannelList">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Associates a Channel List with a Name.</html>",
        ParameterNames = {"Name", "Channel List"},
        DefaultValues = {"Config 1", "Slot!Channel"},
        ToolTips = {"The name is case sensitive", "<html>Slot!Row!Column for Switch Matrix cards<br>Slot!Channel for any other system<br>Use Min:Max to specify ranges<br>Use comma (,) to separate multiple entries</html>"})
    @iC_Annotation(  MethodChecksSyntax = true )
    public void DefineChannelList(String Name, String List) 
           throws DataFormatException {
        
        // remove leading and trainling white spaces
        Name = Name.trim();
        List = List.trim();
        
        
        ///////////////
        // Syntax-Check
        
        // name must not be 'All'
        if (Name.equals("All")) {
            String str = "The name 'All' is reserved for OpenSwitch. Please use a different name.\n";
            
            throw new DataFormatException(str);
        }
        
        // add the Name-List pair
        m_ChannelLists.put(Name, "(@ " + List + ")");
    }// </editor-fold>
    
    
    
    /**
     * Opens the Switches defined in the named Channel List. The channel lists
     * have to be assigned previously using <code>DefineChannelList</code>. This 
     * method performs a Syntax-Check to ensure the used Channel List has been
     * defined previously. Other Switches not included in the Channel List remain
     * unchanged. After switching, <code>m_WaitTime</code> is waited to allow
     * switching to complete.
     * 
     * @param ListName The name of the Channel List; use <code>DefineChannelList</code>
     * @throws DataFormatException When the Syntax-Check failed
     * @throws IOException When communication with the Instrument failed; bubbles
     * up from <code>SendToInstrument</code>
     */
    // <editor-fold defaultstate="collapsed" desc="OpenSwitch">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Opens the specified switches (other switches are unchanged).</html>",
        ParameterNames = {"Channel List Name"},
        DefaultValues = {"Config 1"},
        ToolTips = {"Can be 'All' or the List-Name used in DefineChannelList"})
    @iC_Annotation(  MethodChecksSyntax = true )
    public void OpenSwitch(String ListName) 
           throws DataFormatException, IOException {
        
        // get Channel List
        String ChannelList = m_ChannelLists.get(ListName);
        
        // handle special case of "All"
        if (ListName.equals("All")) {
            ChannelList = "All";
        }
        
        // Syntax-Check
        if (ChannelList == null) {
            String str = "The Channel List '" + ListName + "' was not defined.\n"
                    + "Please use DefineChannelList to define it.\n";
            throw new DataFormatException(str);
        }
        
        // return if in Syntax-check mode
        if (inSyntaxCheckMode())
            return;

        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return;
        
        
        // build the command string
        String cmd = ":Route:open " + ChannelList;
        
        // close the switch
        SendToInstrument(cmd);
        
        // wait default wait time for switching to complete
        try {Thread.sleep(m_WaitTime);} catch (InterruptedException ignore) {}
        
    }// </editor-fold>
    
    /**
     * Closes the Switches defined in the named Channel List. The channel lists
     * have to be assigned previously using <code>DefineChannelList</code>. This 
     * method performs a Syntax-Check to ensure the used Channel List has been
     * defined previously. Other Switches not included in the Channel List remain
     * unchanged. After switching, <code>m_WaitTime</code> is waited to allow
     * switching to complete.
     * 
     * @param ListName The name of the Channel List; use <code>DefineChannelList</code>
     * @throws DataFormatException When the Syntax-Check failed
     * @throws IOException When communication with the Instrument failed; bubbles
     * up from <code>SendToInstrument</code>
     */
    // <editor-fold defaultstate="collapsed" desc="CloseSwitch">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Closes the specified switches (other switches are unchanged).</html>",
        ParameterNames = {"Channel List Name"},
        DefaultValues = {"Config 1"},
        ToolTips = {"The List-Name used in DefineChannelList"})
    @iC_Annotation(  MethodChecksSyntax = true )
    public void CloseSwitch(String ListName) 
           throws DataFormatException, IOException {
        
        // get Channel List
        String ChannelList = m_ChannelLists.get(ListName);
        
        // Syntax-Check
        if (ChannelList == null) {
            String str = "The Channel List '" + ListName + "' was not defined.\n"
                    + "Please use DefineChannelList to define it.\n";
            throw new DataFormatException(str);
        }
        
        // return if in Syntax-check mode
        if (inSyntaxCheckMode())
            return;

        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return;
        
        
        // build the command string
        String cmd = ":Route:Close " + ChannelList;
        
        // close the switch
        SendToInstrument(cmd);
        
        // wait default wait time for switching to complete
        try {Thread.sleep(m_WaitTime);} catch (InterruptedException ignore) {}
        
    }// </editor-fold>
    
    
    /**
     * Opens all Switches and then closes the Switches defined in the named 
     * Channel List by calling <code>CloseSwitch</code>. The channel lists
     * have to be assigned previously using <code>DefineChannelList</code>. This 
     * method performs a Syntax-Check to ensure the used Channel List has been
     * defined previously.
     * 
     * @param ListName The name of the Channel List; use <code>DefineChannelList</code>
     * @throws DataFormatException When the Syntax-Check failed
     * @throws IOException When communication with the Instrument failed; bubbles
     * up from <code>OpenSwitch</code> or <code>CloseSwitch</code>
     */
    // <editor-fold defaultstate="collapsed" desc="CloseOnly">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Closes the specified switches.</html>",
        ParameterNames = {"Channel List Name"},
        DefaultValues = {"Config 1"},
        ToolTips = {"Can be 'All' or the List-Name used in DefineChannelList"})
    @iC_Annotation(  MethodChecksSyntax = true )
    public void CloseOnly(String ListName) 
           throws DataFormatException, IOException {
        
        // It's okay to call both methods below during syntax-Check mode
        
        // open all switches
        OpenSwitch("All");
        
        // close the selected switches
        CloseSwitch(ListName);   
    }// </editor-fold>
    
}
