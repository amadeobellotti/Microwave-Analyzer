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

import java.awt.Toolkit;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;


/**
 * Some useful application-wide properties. A static initialization block tries
 * to load resources in the file iC.properties and constructs a <code>Properties</code>
 * object <code>m_DefaultProperties</code> from them. A second
 * <code>Properties</code> object is constructed that inherits key/value pairs from
 * <code>m_DefaultProperties</code> that contains User defined properties, for
 * instance the InstrumentNameIdentifier when the User chooses to accept a different
 * response to an *IDN? query. The User defined properties are saved in the
 * default directory ($userhome$/iC/) in the file iC_User.properties.<p>
 *
 * Note: Add "iC.Debug 1" (without quotes) to the command line arguments (Project
 * settings/Run/Arguments) to enable debug mode by setting the iC.Debug value in
 * the User preferences to 1.<p>
 *
 * In retrospect, using the Preferences class might have been more elegant.<p>
 *
 * @author KPP (Kurt Pernstich: pernstich@alumni.ethz.ch)
 * @version 0.1
 */
// http://www.iks.hs-merseburg.de/~uschroet/Literatur/Java_Lit/JAVA_Insel/javainsel_12_009.htm#mjbcfbb236db83fd8065521c7aea5db884
// http://www.javaworld.com/javaworld/javaqa/2003-08/01-qa-0808-property.html
// http://www.javaworld.com/javaworld/javaqa/2003-08/02-qa-0822-urls.html
// http://www.javaworld.com/javaworld/javaqa/2002-11/02-qa-1122-resources.html
public class iC_Properties {


    /**
     * Grants access to global properties of the iC application. This includes
     * the default properties and the user defined properties.
     */
    static private final Properties m_UserProperties;
    static private final Properties m_DefaultProperties;


    /**
     * Holds the image used as logo of the application.
     */
    static private ImageIcon m_LogoTiny;
    static private ImageIcon m_LogoSmall;
    static private ImageIcon m_LogoMedium;

    // the file names of the resource bundles with the default properties
    static private final String RESOURCE_BUNDLE_NAMES[] =
        {"resources/iC.properties"};

    // the file names used to store the User's preferences
    static private final String USER_PROPERTIES_FILE_NAME = "iC_User.properties";


    /** The Logger for this class */
    private static final Logger m_Logger = Logger.getLogger("iC.iC_Properties");
    
    /**
     * Holds a reference to the <code>IcontrolView</code> class. Using getView 
     * to get a handle to the View (to use DisplayStatusMessage etc.) was introduced 
     * with JUnit tests on 120727 (v1.3.469).
     */
    protected static final GUI_Interface m_GUI = Utilities.getView();


     /**
      * Static initialization block. It tries to load the key/value pairs
      * defined in iC.properties as the Default properties and constructs
      * the User properties. When initialization fails, a Message Dialog is shown.
      */
     // <editor-fold defaultstate="collapsed" desc="Static initialization block">
     static {

        // init the logos
        m_LogoTiny = null;
        m_LogoSmall = null;
        m_LogoMedium = null;

        // init Logger level to inherit parent Logger level
        m_Logger.setLevel(null);
        
        // construct the Default Properties
        m_DefaultProperties = new Properties();
        
        // construct the User Properties
        m_UserProperties = new Properties( m_DefaultProperties );


        // load Default properties from iC.properties
        InputStream in;
        String BundleName = "";
        try {
            // load all specified resource bundles
            for (String dummy : RESOURCE_BUNDLE_NAMES) {
                // remember the bundle name in case an Exception is thrown
                BundleName = dummy;

                in = iC_Properties.class.getResourceAsStream(BundleName);
                // http://www.javaworld.com/javaworld/javaqa/2002-11/02-qa-1122-resources.html

                // load the iC.properties into the Properties
                m_DefaultProperties.load(in);

                // close the stream
                in.close();
                
                // log event
                // would be nice, but Logger has not been initialized at this stage
                // because I need the iC_Properties to get the path for the Logger's
                // FileHandler. It's logged to Java's default logger
                m_Logger.log(Level.FINE, "Loaded {0}", BundleName);
            }
            
            // free the resource (InputStream.close does nothing)
            in = null;

        } catch (Exception ex) {
            // logging does not work at this stage (see above)
            m_Logger.log(Level.WARNING, " Resource bundle {0} not found.\n{1}",
                    new Object[]{BundleName, ex.getMessage()});

            // display a message
            String str = "Could not load the resources from\n";
            str += BundleName + "\n";
            str += "Using default values instead.\n";
            str += "Please report this case to the developer.\n";
            str += ex.getMessage();

            JOptionPane.showMessageDialog(null, str, "Missing Resource", JOptionPane.ERROR_MESSAGE);
        }


        //////////////////////////////////
        // load user properties if present

        // get path
        String FileName = m_DefaultProperties.getProperty("iC.DefaultPath", "$user.home$/iC");

        // replace $user.home$ with real path
        FileName = FileName.replace("$user.home$", System.getProperty("user.home"));

        // replace '/' with path separator
        FileName = FileName.replace("/", System.getProperty("file.separator"));

        // add File Name
        FileName += System.getProperty("file.separator") + USER_PROPERTIES_FILE_NAME;


        
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(FileName);

        } catch (FileNotFoundException ex) {
            // logging does not work at this stage (see above)
            m_Logger.log(Level.INFO, "Did not find the User''s properties in\n{0}\n", FileName);

            // this is ignored because the user might not have defined
            // any properties yet, so just return
        }

        
        try {
            if (fin != null) {
                // load the user properties
                m_UserProperties.load(fin);
                
                // close the file (ignored that it's not closed if the above causes an error)
                fin.close();
            }

        } catch (IOException ex) {
            m_Logger.log(Level.WARNING, "iC_User.properties could not be loaded (or closed).{0}\n", ex);
            
        } finally {
            try {
                if (fin != null) {
                    // close the file
                    fin.close();
                }
            } catch (IOException ignored) {}
        }
        
        // 
     }//</editor-fold>


    /**
     * Returns the property of the specified Property-Key. This method is specialized
     * in retrieving path properties, because it replaces '$user.home$' appearing
     * in the property with the real user path. It also replaces '/' with the
     * System's file separator, and $dist.dir$ with the path to the distribution
     * directory (where additional library files are copied via Ant). The path
     * includes a file separator at the end.
     *
     * @param PropertyKey The Property-Key identifying the property that should be returned.
     *
     * @param DefaultPath If the specified Property-Key does not exist, this value
     * is returned instead.
     *
     * @return The property of the specified Property-Key
     */
    // <editor-fold defaultstate="collapsed" desc="getPath">
    public String getPath(String PropertyKey, String DefaultPath) {

        // get desired file path from the iC properties, and
        // assign default value if necessary
        String dummy = getString(PropertyKey, DefaultPath);
        

        // replace $user.home$ with real path
        dummy = dummy.replace("$user.home$", System.getProperty("user.home"));

        // replace $dist.dir$ with real path
        dummy = dummy.replace("$dist.dir$", System.getProperty("user.dir"));
        
        // add file seperator at the end if necessary
        if ( !dummy.endsWith( System.getProperty("file.separator") ) ) {
            // add separator
            dummy += System.getProperty("file.separator");
        }

        // replace '/' with path separator
        dummy = dummy.replace("/", System.getProperty("file.separator"));

        // return the result
        return dummy;
    }//</editor-fold>


    /**
     * Returns the Integer valued property of the specified Property-Key. When the
     * retrieved property cannot be found or converted into an Integer, then the
     * specified default value is used, and a message is shown to the user.
     *
     * @param PropertyKey The Property-Key identifying the property that should be returned.
     *
     * @param DefaultValue If the specified Property-Key does not exist or it cannot
     * be converted to an Integer, this value is returned instead.
     *
     * @return The property of the specified Property-Key
     */
     // <editor-fold defaultstate="collapsed" desc="get Integer">
    public int getInt(String PropertyKey, int DefaultValue) {

        // get the property from the resource file
        String dummy = getString(PropertyKey, "");


        // assign default value if necessary
        if (dummy.isEmpty()) {

            // return the default value
            return DefaultValue;
        }

        // convert to integer
        int IntValue;
        try {
            IntValue = Integer.parseInt(dummy);

        } catch (NumberFormatException ex) {
            // assign default value
            IntValue = DefaultValue;

            // display a status message
            String str = "Warning: Could not convert the property value for " + PropertyKey + " to Integer. Using default value.\n";
            m_GUI.DisplayStatusMessage(str, false);

            // log
            m_Logger.warning(str);

            // beep
            Toolkit.getDefaultToolkit().beep();
        }

        // return the retrieved value
        return IntValue;
    }//</editor-fold>


    /**
     * Returns the double valued property of the specified Property-Key. When the
     * retrieved property cannot be found or converted into a double, then the
     * specified default value is used, and a message is shown to the user.
     *
     * @param PropertyKey The Property-Key identifying the property that should be returned.
     *
     * @param DefaultValue If the specified Property-Key does not exist or it cannot
     * be converted to a double, this value is returned instead.
     *
     * @return The property of the specified Property-Key
     */
     // <editor-fold defaultstate="collapsed" desc="get Double">
    public double getDouble(String PropertyKey, double DefaultValue) {

        // get the property from the resource file
        String dummy = getString(PropertyKey, "");


        // assign default value if necessary
        if (dummy.isEmpty()) {

            // return the default value
            return DefaultValue;
        }

        // convert to double
        double DoubleValue;
        try {
            DoubleValue = Double.parseDouble(dummy);

        } catch (NumberFormatException ex) {
            // assign default value
            DoubleValue = DefaultValue;

            // display a status message
            String str = "Warning: Could not convert the property value for " + PropertyKey + " to double. Using default value.\n";
            m_GUI.DisplayStatusMessage(str, false);

            // log
            m_Logger.warning(str);

            // beep
            Toolkit.getDefaultToolkit().beep();
        }

        // return the retrieved value
        return DoubleValue;
    }//</editor-fold>


    /**
     * Returns the property value of the specified Property-Key as a String. When the
     * retrieved property cannot be found, the specified default value is used,
     * and a message is shown to the user.
     *
     * @param PropertyKey The Property-Key identifying the property that should be returned.
     *
     * @param DefaultValue If the specified Property-Key does not exist this
     * value is returned instead.
     *
     * @return The property of the specified Property-Key
     */
     // <editor-fold defaultstate="collapsed" desc="get String">
    public String getString(String PropertyKey, String DefaultValue) {

        // get the property from the user/default properties
        // I could also use getProperty(key, default) but then I cannot display the warning msg
        String dummy = m_UserProperties.getProperty(PropertyKey);

        // assign default value if necessary
        if (dummy == null) {

            // show a message to the user
            String str = "Warning: Could not retrieve property value for " + PropertyKey + ". Using default value.\n";
            m_GUI.DisplayStatusMessage(str, false);

            // log
            m_Logger.warning(str);

            return DefaultValue;
        }

        // return the retrieved value
        return dummy;
    }//</editor-fold>


    /**
     * Sets the new key/value pair in the User properties and stores the User
     * properties in a file in the iC-Directory to make them persistent. The
     * iC-Directory is defined in property file RESOURCE_BUNDLE_NAME
     * (iC.properties) and the file name of the User properties is hard coded
     * in USER_PROPERTIES_FILE_NAME.<p>
     *
     * Any error occurring during saving of the property file are presented to
     * the User in message boxes.<p>
     * 
     * Note: I tried to add a parameter Persist, but becuase all user properties
     * are saved to the file at once, it's not so easy to remember which key/value
     * pairs should not be persisted.
     *
     * @param PropertyKey The new key for the User property.
     * @param PropertyValue The new value for the User property.
     */
    // <editor-fold defaultstate="collapsed" desc="set String">
    public void setString(String PropertyKey, String PropertyValue) {

        // set the new key/value in the User properties
        m_UserProperties.setProperty(PropertyKey, PropertyValue);


        ////////////////////////////////////////
        // store the User properties in the file

        // get the path
        String FileName = getPath("iC.DefaultPath", "$user.home$/iC/");

        // add the File Name
        FileName += USER_PROPERTIES_FILE_NAME;

        // make a Output Stream
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(FileName, false);

         } catch (FileNotFoundException ex) {
            m_Logger.log(Level.WARNING, "Could not open iC_User.properties.{0}\n", ex);

            // display a message
            String str = "Could not save the User properties to\n" + FileName + "\n\n";
            str += ex.getMessage();

            JOptionPane.showMessageDialog(null, str, "Missing Resource", JOptionPane.ERROR_MESSAGE);
         }


        // store the User properties
        if (fout != null) {
            try {
                m_UserProperties.store(fout, "User Properties for Instrument Control (iC)");

                // close the stream
                fout.close();

            } catch (Exception ex) {
                m_Logger.log(Level.WARNING, "Could not open iC_User.properties.{0}\n", ex);

                // display a message
                String str = "Could not save the User properties to the file.\n";
                str += ex.getMessage();

                JOptionPane.showMessageDialog(null, str, "Missing Resource", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//</editor-fold>


    /**
     * Get the image Logo for the application in size tiny (30 pix wide)
     *
     * @return The Logo as ImageIcon
     */
    // <editor-fold defaultstate="collapsed" desc="get Logo tiny">
    public ImageIcon getLogoTiny() {

        // load the logo image (size tiny) if it hasn't been loaded already
        if ( m_LogoTiny == null ) {

            // get the file name
            //String Logo = m_iC_Properties.getString("iC.LogoTiny");
            String Logo = getString("iC.LogoTiny", "");

            // get the file path
            ClassLoader cl = iC_Properties.class.getClassLoader();
            URL url = cl.getResource("icontrol/resources/" + Logo);

            // load the image
            if (url != null)
                m_LogoTiny = new ImageIcon(url);
        }

        return m_LogoTiny;
    }//</editor-fold>

    /**
     * Get the image Logo for the application in size small (80 pix wide).
     *
     * @return The Logo as ImageIcon
     */
    // <editor-fold defaultstate="collapsed" desc="get Logo small">
    public ImageIcon getLogoSmall() {

        // load the logo image (size small) if it hasn't been loaded already
        if ( m_LogoSmall == null) {

            // get the file name
            String Logo = getString("iC.LogoSmall", "");

            // get the file path
            ClassLoader cl = iC_Properties.class.getClassLoader();
            URL url = cl.getResource("icontrol/resources/" + Logo);

            // load the image
            if (url != null)
                m_LogoSmall = new ImageIcon(url);
        }

        return m_LogoSmall;
    }//</editor-fold>


    /**
     * Get the image Logo for the application in size medium (160 pix wide).
     *
     * @return The Logo as ImageIcon
     */
    // <editor-fold defaultstate="collapsed" desc="get Logo medium">
    public ImageIcon getLogoMedium() {

        // load the logo image (size medium) if it hasn't been loaded already
        if ( m_LogoMedium == null) {

            // get the file name
            String Logo = getString("iC.LogoMedium", "");

            // get the file path
            ClassLoader cl = iC_Properties.class.getClassLoader();
            URL url = cl.getResource("icontrol/resources/" + Logo);

            // load the image
            if (url != null)
                m_LogoMedium = new ImageIcon(url);
        } 

        return m_LogoMedium;
    }//</editor-fold>

}
