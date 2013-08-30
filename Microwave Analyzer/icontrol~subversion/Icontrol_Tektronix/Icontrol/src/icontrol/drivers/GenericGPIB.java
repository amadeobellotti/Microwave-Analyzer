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

import icontrol.iC_Annotation;
import icontrol.drivers.Device.CommPorts;
import java.io.IOException;
import java.util.logging.Logger;
import javax.script.ScriptException;

/**
 * Generic GPIB Instruments are registered in <code>Dispatcher.RegisterDeviceClasses</code>
 * with this class as their Instrument-Class. Essentially all functionality to 
 * support generic GPIB Instruments is implemented in class <code>Device</code>, 
 * hence, this class only overrides the <code>getInstrumentNameIdentifier</code>
 * method, because Instrument-Name-Identifiers are not supported by generic GPIB
 * Instruments, and also <code>Open</code> because generic GPIB instruments are
 * assumed to be non IEEE 488.2 compliant (no *CLS is sent upon open).
 * 
 * @author KPP (Kurt Pernstich: pernstich@alumni.ethz.ch)
 * @version 0.1
 */

// promise that this class supports GPIB communication
// Note that no InstrumentClassName field is specified, hence, this class is not
// considered as "regular" Device Class in Device.RegisterDeviceClasses
@iC_Annotation(CommPorts=CommPorts.GPIB)
public class GenericGPIB extends Device {
    

    ///////////////////
    // member variables

    /** The Logger for this class. Note that the Logger Level is set in the
     * constructor of the <code>Device</code> class. */
    private static final Logger m_Logger = Logger.getLogger("iC.Instruments.iC_GenericGPIBInstrument");



    /**
     * Overrides the standard <code>getInstrumentNameIdentifier</code> method which
     * retrieves the Instrument Name Identifier (part of the response to a *IDN?
     * query) from the <code>iC.properties</code>. Instead, the Instrument Name
     * Identifier used to define the generic Instrument is returned.<p>
     * 
     * Note: At time of writing, the Instrument-Name-Identifier is not supported 
     * for generic Instruments, hence, this method always returns an empty String.
     * 
     * @return The Instrument Name Identifier that was defined for the generic
     * GPIB Instrument. Because generic Instruments do not support Instrument Name 
     * Identifiers, this String is always empty.
     */
    // <editor-fold defaultstate="collapsed" desc="getInstrumentNameIdentifier">
    @Override
    protected String getInstrumentNameIdentifier() {
        
        // use m_InstrumentClassName to get Instrument from m_GenericGPIBInstruments to return the IDN
        return "";
    }//</editor-fold>
    
    
    /**
     * See also {@link Device#Open() }.<p>
     * 
     * This overridden method for generic GPIB instruments assumes that the
     * instrument does NOT implement IEEE 488.2 common commands, hence, it
     * does not send *CLS and it does not call <code>checkIDN</code> - it 
     * actually does nothing and just returns.<p>
     * 
     * A later implementation might define an extra command for generic GPIB 
     * instruments like "supports 488.2" which could then be used to check if a 
     * *CLS should be sent. An other possibility would be to define an extra command to 
     * define IDN and use a non-empty IDN to also send *CLS.<p>
     *
     * To clear the event status and error queue, use the script-command
     * Instrument-Name SendCommand *CLS
     */
    // <editor-fold defaultstate="collapsed" desc="Open">
    @Override
    public void Open() {
    }//</editor-fold>
    
}
