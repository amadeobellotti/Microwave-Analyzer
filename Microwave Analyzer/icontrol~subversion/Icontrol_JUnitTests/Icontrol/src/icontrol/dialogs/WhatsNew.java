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

import icontrol.iC_Properties;
import java.io.InputStream;
import java.util.Scanner;
import java.util.logging.Logger;
import javax.swing.text.Position;
import org.jdesktop.application.Action;

/**
 * This class shows a new window which displays the contet of the file 'Whats_New.txt'.
 * The first line in this file is interpreted as the date the latest news were
 * added. If this date is present in the iC_User.properties, the dialog is not 
 * shown. If not present, the dialog is shown and the user can select to not show
 * news up to that specific date. In this case, the fist line is saved in the user
 * properties. The date needs to be in the form yymmdd so that it can be interpreted
 * as an Integer and sorted easily.
 *
 * @author KPP (Kurt Pernstich: pernstich@alumni.ethz.ch)
 * @version 0.1
 */
public class WhatsNew extends javax.swing.JDialog {
    
    ///////////////////
    // member variables    
    
    /** access iC's properties */
    private iC_Properties   m_iC_Properties;
    
    /** The Logger for this class */
    private static final Logger m_Logger = Logger.getLogger("iC.Dialogs.WhatsNew");

    
    /** 
     * Creates and shows a new What's new window if the date of the latest News
     * is after the date the News Dialog has been shown the last time. The date
     * the News was shown last is stored in the iC_User properties as iC.LatestNews.
     * 
     * @param parent The parent Frame
     */
    // <editor-fold defaultstate="collapsed" desc="Constructor">
    public WhatsNew(java.awt.Frame parent) {
        
        // always make it a modal dialog
        super(parent, true);
        
        // local variables
        boolean IsFirstLine = true;
        int LatestNewsDate = 0;
        String  TheNews = "";
        
        // set Logger level to inherit level of parent logger
        m_Logger.setLevel(null);
        
        // instantiate iC_Properties
        m_iC_Properties = new iC_Properties();
        
        
        // get latest date the news were shown
        int LatestNewsViewedDate = m_iC_Properties.getInt("iC.LatestNews", 0);
        
        
        // get the What's new document, store the latest news data
        InputStream is=null;
        try {
        
            // open file
            is = getClass().getResourceAsStream("resources/Whats_New.txt");
            

            // read the version number
            if (is != null) {
                
                // http://stackoverflow.com/questions/309424/in-java-how-do-i-read-convert-an-inputstream-to-a-string
                Scanner myScanner = new Scanner(is).useDelimiter("\\r\\n?|\\n\\r?");// was \\A
                
                while (myScanner.hasNext()) {
                    
                    // get next line
                    String str = myScanner.next();
                    
                    // check if it's the first line
                    if (IsFirstLine) {
                        
                        // store the date of the latest news
                        try {
                            LatestNewsDate = Integer.parseInt(str);
                        } catch (NumberFormatException ex) {
                            LatestNewsDate = 0;
                        }
                        
                        // set flag
                        IsFirstLine = false;
                    }
                    
                    // remember the news
                    TheNews += str + "\n";
                }

            }
        } catch (Exception ex) {
            m_Logger.severe("Could not access WhatsNew.txt ");
            m_Logger.severe(ex.getMessage());
            
        } finally {
            try {
                if (is != null) {
                    // close input stream (InputStream.close does nothing)
                    is.close();
                }
            } catch (Exception ignore) {}
        }
        
        // check if it's necessary to show the Dialog
        if (LatestNewsViewedDate >= LatestNewsDate) {
            // no, it's not, so return
            return;
        }
        
        
        // init components
        initComponents();
        
        // display the news
        jNewsArea.setText(TheNews);
        
        // add a new Window Listener that reacts on closing the window
        this.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                
                // hide the frame. this causes this.setVisible to return
                setVisible(false);
            }
        });
        
        
        
        // show the dialog and wait until it is closed
        this.setVisible(true);
        
        // remember view date if so desired
        if ( jDontShowAgain.isSelected() ) {
            m_iC_Properties.setString("iC.LatestNews", Integer.toString(LatestNewsDate));
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

        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jNewsArea = new javax.swing.JTextArea();
        jDontShowAgain = new javax.swing.JCheckBox();
        jButtonClose = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(icontrol.IcontrolApp.class).getContext().getResourceMap(WhatsNew.class);
        setTitle(resourceMap.getString("Form.title")); // NOI18N
        setName("Form"); // NOI18N
        setPreferredSize(new java.awt.Dimension(800, 450));

        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        jNewsArea.setColumns(80);
        jNewsArea.setEditable(false);
        jNewsArea.setLineWrap(true);
        jNewsArea.setRows(5);
        jNewsArea.setText(resourceMap.getString("jNewsArea.text")); // NOI18N
        jNewsArea.setWrapStyleWord(true);
        jNewsArea.setName("jNewsArea"); // NOI18N
        jScrollPane1.setViewportView(jNewsArea);

        jDontShowAgain.setText(resourceMap.getString("jDontShowAgain.text")); // NOI18N
        jDontShowAgain.setName("jDontShowAgain"); // NOI18N

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(icontrol.IcontrolApp.class).getContext().getActionMap(WhatsNew.class, this);
        jButtonClose.setAction(actionMap.get("Close")); // NOI18N
        jButtonClose.setText(resourceMap.getString("jButtonClose.text")); // NOI18N
        jButtonClose.setName("jButtonClose"); // NOI18N

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jScrollPane1, 0, 0, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                        .add(jDontShowAgain)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 454, Short.MAX_VALUE)
                        .add(jButtonClose))
                    .add(jLabel1))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .add(20, 20, 20)
                .add(jLabel1)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 292, Short.MAX_VALUE)
                .add(24, 24, 24)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jButtonClose)
                    .add(jDontShowAgain))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Action which is executed when the user presses the 'Close' button.
     */
    // <editor-fold defaultstate="collapsed" desc="Close">
    @Action
    public void Close() {
        // just close the Frame
        // This will cause setVisible() in the constructor to return
        setVisible(false);
    }//</editor-fold>

    
    

    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonClose;
    private javax.swing.JCheckBox jDontShowAgain;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JTextArea jNewsArea;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables
}
