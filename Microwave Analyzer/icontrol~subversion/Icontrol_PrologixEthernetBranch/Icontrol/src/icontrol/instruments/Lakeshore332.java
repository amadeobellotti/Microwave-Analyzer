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

import icontrol.iC_Annotation;
import icontrol.instruments.Device.CommPorts;

/**
 * Lakeshore 332 Temperature Controller driver-class.<p>
 *
 * At the time of writing, all device commands that the Lakeshore 332 understands are
 * implemented in the base class {@link icontrol.instruments.LakeshoreTC}.<p>
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

        // the Instrument configuration is the same as for the LakeshoreTC, hence
        // no extra initializations are necessary

    }//</editor-fold>
}
