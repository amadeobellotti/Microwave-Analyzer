// TODO 3* un-make Dispatcher.Tokenizer static and move to Utilities? add contructor that requires m_GUI
// TODO search for DELIMITER in javadoc and correct that it's in Utilities not in Dispatcher
// !! TODO there are many classes that have no m_GUI defined !!

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

import javax.swing.JComponent;
import javax.swing.JFrame;

/**
 * This interface declares the methods the view needs to implement for the
 * <code>Dispatcher</code> and the <code>Device</code> class. It was introduced
 * in version 1.3 to implement JUnit tests. It will also be useful when changing
 * the GUI to a Rich CLient Platform as it decouples more clearly the GUI from the
 * rest of the program.<p>
 * 
 * @author KPP (Kurt Pernstich: pernstich@alumni.ethz.ch)
 * @version 0.1
 */
public interface GUI_Interface {

    /**
     * See the javadoc at the main implementing class 
     * {@link icontrol.IcontrolView#DisplayStatusMessage(String) }
     * or the mockup test class.
     */
    public void DisplayStatusMessage(String StatusMessage);
    
    /**
     * See the javadoc at the main implementing class 
     * {@link icontrol.IcontrolView#DisplayStatusMessage(String, boolean AddTimeStamp) }
     * or the mockup test class.
     */
    public void DisplayStatusMessage(String StatusMessage, boolean AddTimeStamp);
    
    /**
     * See the javadoc at the main implementing class 
     * {@link icontrol.IcontrolView#DisplayStatusLine(java.lang.String, boolean) }
     * or the mockup test class.
     */
    String DisplayStatusLine(String StatusText, boolean FadeOut);

    /**
     * See the javadoc at the main implementing class 
     * {@link icontrol.IcontrolView#DoneScripting(boolean) }
     * or the mockup test class.
     */
    void DoneScripting(boolean Erroroccurred);

    /**
     * See the javadoc at the main implementing class 
     * {@link icontrol.IcontrolView#getFileName(java.lang.String) }
     * or the mockup test class.
     */
    String getFileName(String FileExtension);

    /**
     * See the javadoc at the main implementing class 
     * {@link icontrol.IcontrolView#getProjectPath() }
     * or the mockup test class.
     */
    String getProjectPath();
    
    /**
     * See the javadoc at the main implementing class 
     * {@link icontrol.IcontrolView#isAutoPaused() }
     * or the mockup test class.
     */
    boolean isAutoPaused();

    /**
     * See the javadoc at the main implementing class 
     * {@link icontrol.IcontrolView#isPaused(boolean) }
     * or the mockup test class.
     */
    boolean isPaused(boolean WaitUntilNotPaused);
    
    /**
     * See the javadoc at the main implementing class 
     * {@link icontrol.IcontrolView#getComponent() }
     * or the mockup test class.
     */
    public JComponent getTheComponent();
    
    /**
     * See the javadoc at the main implementing class 
     * {@link icontrol.IcontrolView#getFrame() }
     * or the mockup test class.
     */
    public JFrame getTheFrame();
    
    /**
     * See the javadoc at the main implementing class 
     * {@link icontrol.IcontrolView#Pause() }
     * or the mockup test class.
     */
    public void Pause();
    
    /**
     * See the javadoc at the main implementing class 
     * {@link icontrol.IcontrolView#Stop() }
     * or the mockup test class.
     */
    public void Stop();
    
}
