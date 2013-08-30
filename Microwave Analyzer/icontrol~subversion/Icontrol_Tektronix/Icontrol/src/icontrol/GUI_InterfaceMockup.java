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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import javax.swing.JComponent;
import javax.swing.JFrame;

/**
 * Mockup View used for JUnit tests. Implements the <code>GUI_Interface</code>
 * which defines all methods that are accessed from the iC-Framework and usually
 * reside in the View class.<p>
 * 
 * It would be nicer if this class were in the test/ directory, but because the
 * <code>TestMockup</code> (which cannot be moved to the test/ directory but
 * needs to reside in the source directory; see comments there) needs to access
 * this class, it is in the source directory and not in the test directory.
 * 
 * @author KPP (Kurt Pernstich: pernstich@alumni.ethz.ch)
 * @version 0.1
 *
 */
public class GUI_InterfaceMockup implements GUI_Interface {
    
    
    public void DisplayStatusMessage(String StatusMessage) {
        
        // just add the text by calling the other DisplayStatusMessage method
        DisplayStatusMessage(StatusMessage, false);
    }

    
    public void DisplayStatusMessage(String StatusMessage, boolean AddTimeStamp) {
        
        // display the message in the System out (Netbeans)
        System.out.print(StatusMessage);
    }

    public String DisplayStatusLine(String StatusText, boolean FadeOut) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void DoneScripting(boolean Erroroccurred) {
        
        // just display that it's done
        DisplayStatusMessage("Done Scripting.\n");
        
        while(true){}
    }

    
    public String getFileName(String FileExtension) {
        
        // get the path
        String PathAndName = getProjectPath();
        
        // reformat the current date and time
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd_HHmmss");
        String DateString = sdf.format(Calendar.getInstance().getTime());

        // add the filename
        PathAndName += DateString + " TestResult ";
        
        
        // add File Extension if a non-empty String was specified
        if ( !FileExtension.isEmpty() ) {

            // add a '.' if no '.' was included
            if ( !FileExtension.contains(".") )
                FileExtension = "." + FileExtension;
        }
        
        // return the file name
        return PathAndName + FileExtension;
        
    }

    
    /**
     * @return Returns the path to <user home>/iC/UnitTests/. This directory is 
     * created if it does not exist. The path ends with a Path Separator.
     */
    // <editor-fold defaultstate="collapsed" desc="getProjectPath">
    public String getProjectPath() {
        
        // get access to the iC_Properties
        iC_Properties Properties = new iC_Properties();
        
        // get default path from iC properties
        String Path = Properties.getPath("iC.DefaultPath", "$user.home$/iC");
        
        // add UnitTests directory to the path
        Path += "UnitTests" + System.getProperty("file.separator");
        
        
        // check if directory exists
        File file = new File(Path);
        if ( !file.exists() ) {
            
            // no, so create it
            try {
                file.mkdirs();
            } catch (SecurityException ex) {
                DisplayStatusMessage("Could not create the directory: " + Path);
            }   
        }
            
        
        // return the path
        return Path;
    }//</editor-fold>

    public boolean isAutoPaused() {
        return false;
    }

    public boolean isPaused(boolean WaitUntilNotPaused) {
        return false;
    }

    public JComponent getTheComponent() {
        return null;
    }

    public JFrame getTheFrame() {
        return null;
    }

    public void Pause() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void Stop() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
