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

import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * The superclass of all JUnit tests that all Test classes can/should extend.
 * It's main purpose is to set up the Test Environment by creating an instance
 * of <code>IcontrolAppMockup</code>. It also grants access to the GUI-Mockup 
 * via <code>m_GUI</code>. Setting up the Test Environment is done in a static
 * method marked with <code>@BeforeClass</code>, hence it is automatically
 * set up for each class extending <code>IcontrolTests</code>.<p>
 * 
 * <code>IcontrolAppMockup</code> instantiates <code>GUI_InterfaceMockup</code>
 * and <code>Dispatcher</code>. It set's the global No-Communication-Mode, set
 * up the Loggers (on the todo list) and more.
 * 
 * @author KPP (Kurt Pernstich: pernstich@alumni.ethz.ch)
 * @version 0.1
 */
public class IcontrolTests {
    
    /**
     * A handle to the Test Environment. Would not be necessary to store it as
     * a member variable because the instance would live as long as this class
     * does because it is instantiated in a static method.
     */
    protected static IcontrolAppMockup m_TestMockups;
    
    /** Access to the GUI, respectively, the GUI_Interface */
    protected static GUI_Interface m_GUI;
    
    
    /**
     * This gets called once when each Test-Class is instantiated.
     */
    @BeforeClass
    public static void setUpClass()  {
        
        // instantiate Test environment
        m_TestMockups = new IcontrolAppMockup();
        
        // get access to the GUI
        m_GUI = Utilities.getView();
        
    }
    
    /** Does nothing */
    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
}
