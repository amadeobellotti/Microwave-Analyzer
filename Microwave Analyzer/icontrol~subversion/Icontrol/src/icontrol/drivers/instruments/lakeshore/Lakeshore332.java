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

package icontrol.drivers.instruments.lakeshore;

import icontrol.AutoGUIAnnotation;
import icontrol.iC_Annotation;
import icontrol.drivers.Device.CommPorts;
import java.io.IOException;
import java.util.zip.DataFormatException;
import javax.script.ScriptException;

/**
 * Lakeshore 332 Temperature Controller driver-class.<p>
 *
 * At the time of writing, all device commands that the Lakeshore 332 understands are
 * implemented in the base class {@link icontrol.drivers.instruments.Lakeshore.LakeshoreTC}.<p>
 *
 *
 * <h3>Instrument Commands:</h3>
 * <ul>
 *  <li>all commands are inherited from class <code>LakeshoreTC</code>
 * </ul>
 *
 * @author KPP (Kurt Pernstich: pernstich@alumni.ethz.ch)
 * @version 0.3
 *
 */


// promise that this class supports GPIB communication
@iC_Annotation( CommPorts=CommPorts.GPIB,
                InstrumentClassName="Lakeshore 332")
public class Lakeshore332 extends LakeshoreTC {

    /**
     * Default constructor. Updates the parameter range for this temperature
     * controller, such as maximum heater range, maximum input curve number, etc.<p>
     */
    // <editor-fold defaultstate="collapsed" desc="Constructor">
    public Lakeshore332() {

        // invoke the base class' constructor
        super();
        
        // choose to add termination characters
        // 120725: for some reason the CSET? 2 or RANGE 3 commands works only if
        // \r\n is appended or the termination characters are changed in the
        // instrument while other commands work. ?!?
        // TODO 9* what's with the Termination Characters?
        //m_TerminationCharacters = m_iC_Properties.getString("Lakeshore332.TerminationCharacters", "");


        // the Instrument configuration is the same as for the LakeshoreTC, hence
        // no extra initializations are necessary

    }//</editor-fold>
    
    
    /**
     * Sets the Heater Range (see Lakeshore 331 manual p. 6-30) of the default 
     * Loop set with the script command configDefaults.<p>
     *
     * This method performs a Syntax-Check.
     *
     * @param Range The Heater range.
     *
     * @throws DataFormatException when the Syntax Check failed.
     * @throws IOException re-thrown from <code>SendToInstrument</code> or from
     * <code>QueryInstrument</code>
     * @throws ScriptException When the answer to the ANALOG? query could not
     * be interpreted when a Heater other than Loop 1 was addressed.
     */
    // <editor-fold defaultstate="collapsed" desc="setHeaterRange">
    @AutoGUIAnnotation(
        DescriptionForUser = "Set the Heater Range of the default Loop.",
        ParameterNames = {"Range { 0,1,2,... }"},
        DefaultValues = {"1"},
        ToolTips = {"0 turns off the Heater."})
    @iC_Annotation(  MethodChecksSyntax = true )
    public void setHeaterRange(int Range)
                throws IOException, DataFormatException, ScriptException {

        ///////////////
        // Syntax-Check

        // Syntax Check for correct Heater Range
        checkHeaterRange(Range);
        
        // use default Loop
        int Loop = m_DefaultLoop;
        
        // check if a valid default loop was selected with configDefaults
        if (Loop == -1) {
            String str = "You have addressed to set the HeaterRange of the default Control Loop\n"
                    + "without having assigned a valid default Loop.\n";
            str += "Please use 'configDefaults' first.\n";
            throw new DataFormatException(str);
        }
        
        // Syntax Check for correct loop number (is probably redundant)
        Loop = checkLoop(Loop);
        
        
        // check range of Range 
        if ( Loop == 1 && (Range < 0 || Range > 5) ) {
            String str = "Range must be within 0...5";
            throw new DataFormatException(str);
        } 
        if ( Loop == 2 && (Range < 0 || Range > 1) ) {
            String str = "Range must be within 0...1";
            throw new DataFormatException(str);
        }
        if (Loop > 2) {
            String str = "Cannot enable Heater of a Loop > 2.";
            throw new DataFormatException(str);
        }
        

        // return if in Syntax-check mode
        if (inSyntaxCheckMode())
            return;

        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return;
        
        // which Loop is addressed?
        if (Loop == 1) {
            // build the GPIB command
            String cmd = String.format("RANGE %d", Range);

            // send via GPIB
            SendToInstrument(cmd);
        } else {
            
            // get current state of ANALOG Output Parameters
            String PresentState = QueryInstrument("ANALOG?");
            
            // check length (roughly; only to prevent index out of range error)
            if (PresentState.length() < 10) {
                String str = "Could not interpret response to ANALOG? 2 when\n"
                        + "trying to enable Heater for Loop 2.\n";
                throw new ScriptException(str);
            }

            // n,N,a,n,±nnnnnn,±nnnnnn,±nnnnnn
            String NewState = PresentState.substring(0, 2)
                    + (Range == 0 ? "0" : "3")
                    + PresentState.substring(3);
                
            // change to the new Heater Range
            SendToInstrument("ANALOG " + NewState);
        }

    }//</editor-fold>
}
