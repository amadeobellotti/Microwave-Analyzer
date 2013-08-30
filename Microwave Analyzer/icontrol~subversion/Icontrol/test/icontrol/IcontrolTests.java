// TODO 4* define a var that is true during JUnit tests (e.g. for setARBtoCELIV2)
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

import icontrol.dialogs.WaitForUser;
import javax.swing.JOptionPane;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * The superclass of all JUnit tests that all Test classes should extend.
 * It's main purpose is to set up the Test Environment by creating an instance
 * of <code>IcontrolAppMockup</code>. It also grants access to the GUI-Mockup 
 * via <code>m_GUI</code>. Setting up the Test Environment is done in a static
 * method marked with <code>@BeforeClass</code>, hence it is automatically
 * set up for each class extending <code>IcontrolTests</code>.<p>
 * 
 * <code>IcontrolAppMockup</code> instantiates <code>GUI_InterfaceMockup</code>
 * and <code>Dispatcher</code> and initializes the global No-Communication-Mode, 
 * Syntax-Check Mode, set up the Loggers and more.
 *
 * Remark: External resources used in test cases should be copied to
 * Icontrol/tests/resources. This directory was included in the Project properties
 * under the Library/Run Tests tab, and is copied to the build/test directory.
 * The <code>GUI_InterfaceMockup</code> returns this directory as Project Path.
 * Therefore, files required for JUnit tests can be specified as Script Command
 * FileNames as would regular files. See {@link icontrol.GUI_InterfaceMockup#getProjectPath() }
 * for an example of how to access the test ressources directly.
 * 
 * Remark: <code>Device.inJUnitTest</code> can be used to exit method during 
 * JUnit testing.
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
    // <editor-fold defaultstate="collapsed" desc="setUpClass">
    @BeforeClass
    public static void setUpClass()  {
        
        // instantiate Test environment
        m_TestMockups = new IcontrolAppMockup();
        
        // get access to the GUI
        m_GUI = Utilities.getView();
    }//</editor-fold>
    
    
    
    /** Does nothing */
    // <editor-fold defaultstate="collapsed" desc="tearDownClass">
    @AfterClass
    public static void tearDownClass() throws Exception {
    }//</editor-fold>


    /**
     * Sets the global Simulation Mode during Tests (No-Communication Mode).
     *
     * @param SimulationMode When true, the No-Communication-Mode is enabled.
     */
    // <editor-fold defaultstate="collapsed" desc="setSimulationMode">
    public void setSimulationMode(boolean SimulationMode) {
        m_TestMockups.setSimulationMode(SimulationMode);
    }//</editor-fold>

    /**
     * Sets the Syntax-Check mode
     *
     * @param SyntaxCheckMode When true, the Syntax-Check mode is enabled
     */
    // <editor-fold defaultstate="collapsed" desc="setSyntaxCheckMode">
    public void setSyntaxCheckMode(boolean SyntaxCheckMode) {
        m_TestMockups.setSyntaxCheckMode(SyntaxCheckMode);
    }//</editor-fold>
    
    
    /** Displays a new Dialog Window, and waits until the user presses the 
     * continue button. It is possible to interact with the rest of the program
     * while waiting for the user to press the button. This is, for instance, 
     * useful if a JUnit test produces some XY_Chart that should be checked manually.
     */
    // <editor-fold defaultstate="collapsed" desc="WaitForUser">
    public void WaitForUser() {
        
        
        // show WaitForUser Dialog
        // TODO 1* understand why: Interestingly, if I pass m_GUI.getTheFrame()
        // instead of null, the Dialog is shown over and over again.
        WaitForUser wfu = new WaitForUser(null);
            
            
            // wait until user has pressed Continue
        while (wfu.isRunning() == true) {
            try { Thread.sleep( 100 ); } catch (InterruptedException ignore) {}
        }   
    }//</editor-fold>
 
}
