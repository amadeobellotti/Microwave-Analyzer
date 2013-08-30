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

/**
 * Mockup for the test environment. Instantiate this class and it's constructor
 * initialized the MockupView as well as the Dispatcher. All classes of the 
 * iC-framework should now be able to run.<p>
 * 
 * It would be nicer if this class were in the test/ directory, but becuase the
 * <code>Utilities.getView</code> needs access <code>TestMockup.getView</code>,
 * it needs to be in the source directory, where it also gets distributed, but well.
 * 
 * @author KPP (Kurt Pernstich: pernstich@alumni.ethz.ch)
 * @version 0.1
 *
 */

// TODo migh better be done in a TestSuit that runs all tests. Cannot keep the View
// around anyways. Do not want to press buttons, but make automated tests ...
// TODO rename to IcontrolAppMockup?
public class TestMockup {
    
    private static GUI_InterfaceMockup m_GUI = null;
    private Dispatcher m_Dispatcher;
    
    /**
     * @return Returns the <code>GUI_Interface</code> (aka, the GUI) when iC
     * is run as a Unit Test and not as a "regular" application.
     */
    // <editor-fold defaultstate="collapsed" desc="getView">
    public static GUI_Interface getView() {
        return m_GUI;
    }//</editor-fold>
    
    public TestMockup() {
        
        // only do the init if the view has not been initialized before
        if (m_GUI == null) {
            
            // instantiate the Mockup View
            m_GUI = new GUI_InterfaceMockup();

            // disply the test view
            m_GUI.Show();

            /////////////////////
            // prepare Dispatcher

            // make a new Dispatcher instance
            // this also calls RegisterGenericDeviceClasses() which registers all available
            // Instrument Classes
            m_Dispatcher = new Dispatcher(m_GUI);
            
        }
        
        // TODO assign Loggers
        // TODO set LogLevel
    }
    
    
}
