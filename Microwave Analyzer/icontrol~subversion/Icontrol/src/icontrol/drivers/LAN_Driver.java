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

import icontrol.iC_Properties;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownServiceException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.ScriptException;

/**
 * "Driver" class to support network communication over TCP-IP.<p>
 * 
 * To convert between <code>String</code> and <code>byte[]</code> use the
 * UTF-8 character set, for instance String.getBytes(Charset.forName("UTF-8"))<p>
 * 
 * @author KPP (Kurt Pernstich: pernstich@alumni.ethz.ch)
 * @version 0.1
 *
 */
public class LAN_Driver {
    
    /** The Logger for this class */
    private static final Logger m_Logger = Logger.getLogger("iC.Instruments.LAN_Driver");
    
    /**
     * Grants access to global properties of the iC application in a resource
     * bundle file 'iC.properties'
     */
    protected static iC_Properties m_iC_Properties;
    
    /** Handle to access the network connection */
    private URLConnection m_URLConnection;
    
    /** The input stream of the network connection */
    private BufferedReader m_URLReader;
    
    /** The output stream of the network connection */
    private BufferedWriter m_URLWriter;
    
    
    
    /**
     * Constructor. Initializes the Logger for this class. To open the network
     * connection call <code>OpenConnection</code>.<p>
     */
    // <editor-fold defaultstate="collapsed" desc="Constructor">
    public LAN_Driver() {
        
        // init Logger to inherit Logger level from Parent Logger
        m_Logger.setLevel(null);
        
        // instantiate iC_Properties
        m_iC_Properties = new iC_Properties();
    }//</editor-fold>
    
    
    /**
     * Opens the specified network address using the specified network protocol.
     * 
     * @param InstrumentURL The URL of the Instrument (protocol + IP address or 
     * host name)
     * 
     * @throws ScriptException when the network connection could not be opened due
     * to an I/O error.
     */
    // <editor-fold defaultstate="collapsed" desc="Open Port">
    public void OpenConnection(URL InstrumentURL) 
           throws ScriptException {
        
        // init
        m_URLConnection = null;
        
        // log status
        m_Logger.log(Level.FINER, "Trying to open the network connection to {0}\n", InstrumentURL);
        
        try {
            
            // TODO @LAN once I know what parts of the URL are necessary, I can check for
            // protocol, host, etc to avoid a null exception
            
            
            // open connection
            m_URLConnection = InstrumentURL.openConnection();
            
            // enable output to the URL
            m_URLConnection.setDoOutput(true);
            
            // connect
            m_URLConnection.connect();
            
            // assign input stream
            m_URLReader = new BufferedReader( new InputStreamReader( 
                    m_URLConnection.getInputStream() ));
            
            // assign output stream
            m_URLWriter = new BufferedWriter( new OutputStreamWriter(
                    m_URLConnection.getOutputStream() ));
            
        } catch (NullPointerException ex) {
            String str = "Could not open the network connection to\n"
                    + InstrumentURL + "\nThe error message returned was:\n"
                    + ex.getMessage() + "\n"
                    + "It is likely that the URL is not properly formed.\n";
            throw new ScriptException(str);
            
        } catch (UnknownServiceException ex) {
            String str = "The specified URL\n"
                    + InstrumentURL + "\ndoes not support output. The error message returned was:\n"
                    + ex.getMessage() + "\n";
            throw new ScriptException(str);
            
        } catch (IOException ex) {
            String str = "Could not open the network connection to\n"
                    + InstrumentURL + "\nThe error message returned was:\n"
                    + ex.getMessage() + "\n";
            throw new ScriptException(str);
        }
        
        // log status
        m_Logger.log(Level.CONFIG, "Opened network connection to {0}\n", InstrumentURL);
    }//</editor-fold>
    
    
    
    /**
     * Closes the network connection. Called from <code>Device.Closeinstrument</code>.
     * 
     * @throws IOException When closing the in/out streams caused an error.
     */
    // <editor-fold defaultstate="collapsed" desc="Close Connection">
    public void CloseConnection() 
           throws IOException {
        
        // close in-out streams
        try {
            if (m_URLReader != null) {
                m_URLReader.close();
            }
            
            if (m_URLWriter != null) {
                m_URLWriter.close();
            }
        } catch (IOException ex) {
            String str = "Closing the URL reader/writer streams caused the error:\n"
                    + ex.getMessage() + "\n";
            throw new IOException(str, ex);
        }
        

    }//</editor-fold>
    
}
