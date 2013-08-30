// TODO 4* implement arrow-up to recall history
// TODO 2* the path to iC and project dir is exported twice


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
package icontrol.dialogs;

import icontrol.GUI_Interface;
import icontrol.IcontrolApp;
import icontrol.Utilities;
import java.io.IOException;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import org.python.core.PyException;
import org.python.util.PythonInterpreter;

/**
 * This class shows a new window which acts like a Python console: it shows 
 * Python's standard output and allows the user to type in and execute Python
 * commands. Instantiation causes the window to appear, and it is necessary to 
 * set the PythonInterpreter using <code>setPythonInterpreter</code> afterwards
 * or whenever a new PythonInterpreter is instantiated.
 *
 * @author KPP (Kurt Pernstich: pernstich@alumni.ethz.ch)
 * @version 0.1
 */
public class JythonPrompt extends javax.swing.JFrame {
    
    ///////////////////
    // member variables
    
//    // access iC's properties
//    private iC_Properties   m_iC_Properties;
    
    /** The content of this <code>Writer</code> is displayed in the text area
     * associated with Python's standard output stream. */
    private Writer          m_StdOutWriter;

    
    /** The Python Interpreter used to execute commands typed into this Form */
    private PythonInterpreter m_PyInterp;

    /** The Logger for this class */
    private static final Logger m_Logger = Logger.getLogger("iC.Dialogs.JythonPrompt");
    
    /**
     * Holds a reference to the <code>IcontrolView</code> class. Using getView 
     * to get a handle to the View (to use DisplayStatusMessage etc.) was introduced 
     * with JUnit tests on 120727 (v1.3.469).
     */
    protected static final GUI_Interface m_GUI = Utilities.getView();
   
    /** 
     * Creates and shows a new JythonPrompt window. For proper operation it is
     * necessary to assign a PythonInterpreter by calling 
     * <code>setPythonInterpreter</code>.
     */
    // <editor-fold defaultstate="collapsed" desc="Constructor">
    public JythonPrompt() {

        // init components
        initComponents();

        // set Logger level to inherit level of parent logger
        m_Logger.setLevel(null);

//        // instantiate iC_Properties
//        m_iC_Properties = new iC_Properties();
        
        // show the new window
        // could I use IcontrolApp.getApplication().show(this);
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                setVisible(true);
            }
        });
        
    }//</editor-fold>
    
    /**
     * Stores the handle to PythonInterpreter and assigns Python's standard 
     * output stream so that it is displayed in the TextArea.
     * 
     * @param PyInterp The PythonInterpreter to which the commands will be sent
     * and who's standard output will be shown.
     */
    // <editor-fold defaultstate="collapsed" desc="set Python Interpreter">
    public void setPythonInterpreter(PythonInterpreter PyInterp) {
        
        // assign parameters
        m_PyInterp = PyInterp;
                
        // check if null to avoid null pointer exceptions
        if (PyInterp == null) {
            m_GUI.DisplayStatusMessage("Error: PythonInterpreter must not be null.\n", false);
            return;
        }
                
        // make a new Writer to display Python's standard output stream
        m_StdOutWriter = new TextAreaWriter(jPythonOutputStream);
        
        // assign Python's standard output writer
        m_PyInterp.setOut( m_StdOutWriter );
    }//</editor-fold>
    

    /**
     * Display and log (with level FINE) the content of a <code>Writer</code> 
     * in a <code>JTextArea</code>. Used to display the standard Python output 
     * in the TextArea of this Form.
     */
    //http://javacook.darwinsys.com/new_recipes/14.9betterTextToTextArea.jsp
    // <editor-fold defaultstate="collapsed" desc="Text Area Writer">
    private final class TextAreaWriter extends Writer {
        
        private JTextArea m_TextArea;

        // Constructor
        public TextAreaWriter(JTextArea TextArea) {
            // store the Text Area used to display the content of the Writer
            m_TextArea = TextArea;    
        }

        @Override
        public void flush() {}

        @Override
        public void close() {
            // display a message
            m_TextArea.append("Python session has been closed.\n");
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            // get the new string to display
            String str = new String(cbuf, off, len);
            
            // display the new characters (JTextArea.append is thread safe)
            m_TextArea.append(str);
            
            // scroll TextArea to the very bottom
            jPythonOutputStream.setCaretPosition(jPythonOutputStream.getText().length());
            
            // also log the string
            m_Logger.fine(str);
        }
    }//</editor-fold>



    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jScrollPane1 = new javax.swing.JScrollPane();
        jPythonOutputStream = new javax.swing.JTextArea();
        jPythonPrompt = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(icontrol.IcontrolApp.class).getContext().getResourceMap(JythonPrompt.class);
        setTitle(resourceMap.getString("JythonPrompt.title")); // NOI18N
        setName("JythonPrompt"); // NOI18N
        getContentPane().setLayout(new java.awt.GridBagLayout());

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        jPythonOutputStream.setColumns(20);
        jPythonOutputStream.setEditable(false);
        jPythonOutputStream.setRows(5);
        jPythonOutputStream.setName("jPythonOutputStream"); // NOI18N
        jScrollPane1.setViewportView(jPythonOutputStream);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 300;
        gridBagConstraints.ipady = 200;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.8;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(jScrollPane1, gridBagConstraints);

        jPythonPrompt.setText(resourceMap.getString("jPythonPrompt.text")); // NOI18N
        jPythonPrompt.setFocusCycleRoot(true);
        jPythonPrompt.setName("jPythonPrompt"); // NOI18N
        jPythonPrompt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jPythonPromptActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.BASELINE_LEADING;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 5);
        getContentPane().add(jPythonPrompt, gridBagConstraints);

        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 5, 5);
        getContentPane().add(jLabel1, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    
    /**
     * This method is called when the user pressed return in the text field
     * to enter a Python command. It displays the command in TextArea for Python's
     * standard output, logs the command with level FINE, and executes the command.
     * If executing causes a Python error, the error message is also shown in the
     * TextArea.
     * 
     * @param evt Passed by Java and is not used
     */
    // <editor-fold defaultstate="collapsed" desc="User pressed Return">
    private void jPythonPromptActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jPythonPromptActionPerformed
        
        // get the Python command
        String cmd = jPythonPrompt.getText();
        
        // clear the command prompt
        jPythonPrompt.setText("");
        
        
        // display the command as Python output
        jPythonOutputStream.append(">>> " + cmd + "\n");

        // log command
        m_Logger.log(Level.FINE, ">>> {0}\n", cmd);


        // scroll TextArea to the very bottom
        jPythonOutputStream.setCaretPosition(jPythonOutputStream.getText().length());
        
        try {
            // execute the Python command
            m_PyInterp.exec(cmd);

        } catch (PyException ex) {
            
            // display the error in the TextArea
            jPythonOutputStream.append(ex.toString());
            
            // log Error Message
            m_Logger.log(Level.SEVERE, "Python Error:\n{0}\n", ex.toString());
        }
    }//GEN-LAST:event_jPythonPromptActionPerformed
    //</editor-fold>

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JTextArea jPythonOutputStream;
    private javax.swing.JTextField jPythonPrompt;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables
}
