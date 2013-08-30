// check out JMathLib http://www.jmathlib.de/docs/handbook/function_linspace.php

// TODO 5* only show What's new since last shown; make clear it's important to read
// TODO 5* make Jython prompt understand arrow up / search for java python editor

// TODO 5* add C:\Users\iC_for_macro\iC_V1.3.439\Icontrol.jar "C:\Users\iC_for_macro\MeasureOPV.iC" to javadoc
// in WinXP, add a shortcut to icontrol.jar and ensure that the path name does not
// contain any spaced. If you inspect the properties of this shortcut, the Target
// line should reflect your path and not contain double-quotes. Then add the
// full path to an iC script encloded in double quotes at the end, and upon 
// double clicking the shortcut, ic should open and automatically load the 
// specified iC script.


// TODO 4* run something in pdb (Python debugger)
// TODO 4* add GPIB card to MAKE command GPIB=5, Prologix
//      (pass controller to Open/OpenGPIB instead)
//      add optional parameter to MAKE command 
//      e.g. MAKE i6; AgilentZYX; GPIB=5; Controller=GPIB_Prologix_Ethernet?IP=129.168.1.1:1234
//      or make it: MAKE i6; AgilentZYX; GPIB=5, NI/Prologix_Ethernet=129.168...


// TODO 3* make Jython startup faster (go through namespace and 
//         clear what's not in a list-could be done in a method in Startup.py)
// TODO 3* test DS345.setARBtoCELIV with Prologix
// TODO 3* move iC description to Lyx
// TODO 3* add a r-> button (replace line)(ctrl/mac click) also show selected line in the line below the script
// TODO 3* move Device's Python into iC_Python class
// TODO 3* move generic Device stuff into a separate class
// TODO 3* add iC.properties to javadoc as a linked file

// TODO 2* add .TMCTLinstrument to code; rename Yokogawa.GPIBinstrument


// @NB Platform:
//      make script an editor
//      register file Type iC
//      implment Save Script (as opposed to Save As)
//      make sure the doc folder is not in the .jar
//      show ToolTips also for unselected Device Commands (override getToolTipText in JList or DefualtListModel I guess)
//      select values in Table upon double-click

// reevaluate:

// TODO 2* load script from command line (might be easier with Netbeans framework)


// TODO 2* how to add code-sniplets in javadoc; @example in javadoc, <blockquote>, <pre> 
// use: <pre>
//      {@code ...}
//      </pre>

/* If I were to update the screen casts, these points should be changed:
 */


/* HomeAdjustment:
 * FR, 8.6: 20m
 */

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


/*
 * Hint for a GUI error:
 * If some elements in the GUI don't show up at runtime, close IcontrolView.java from
 * the editor, run Clean and Build, and then re-open the GUI class.
 *
 * Hint if Netbeans shows errors in the project where there are none or the
 * program suddenly doesn't compile or suddenly shows a strange errors, especially
 * during start up when loading classes:
 * Quit Netbeans and delete the userdir\.netbeans\[version]\var\cache\ directory
 * 
 * Backup Project Settings for javadoc
 * -overview "/Users/kurtp/Documents/my Software/icontrol~subversion/Icontrol/src/doc-files/overview.html"
 * -overview "${basedir}/${src.dir}/doc-files/overview.html"
 *
 * Hint for svn: 
 * - If committing fails, do an update and sync again.
 * - Important: Rename and Delete Folders ALWAYS from within Netbeans.
 *   Note that deleted folders disappear from the file system only after the next
 *   commit, so DO NOT delete them directly in the file system.
 * - Sometimes it can help to delete the .svn folder(s) in a folder that does not sync
 *   To delete all .svn folders recursively use find -d . -type d -name '.svn' -exec rm -rf {} \;
 * - http://kenai.com/projects/help/pages/Usingsvn-unixmac#Using_SSH_With_Command-Line_Subversion
 * - To manually delete a folder use svn delete -m "description" svn+ssh://url

 * 
 * To publish:
 * - Clean & Build, build javadoc
 * - double-check that the iC folder has been copied to the dist folder
 * - try to change the icon of the jar (optional, also of the zip file)
 * - rename the dist folder into iC_vZ.Y.X
 * - use ZipItUp no svn workflow to zip the directory
 * - test it, and copy to Java.net (https://java.net/website/icontrol)
 * - update javadoc on Java.net
 * - copy source code folder, remove .svn, and store zip file as a backup
 * 
 * Remarks:
 * - To delete all .svn dirs: find -d . -type d -name '.svn' -exec rm -rf {} \;
 * - To delete all .DS_Store files: find . -type f -name '.DS_Store' -exec rm -rf {} \; 
 */
package icontrol;


import icontrol.dialogs.WhatsNew;
import icontrol.drivers.Device;
import icontrol.drivers.Device.CommPorts;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.TaskMonitor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Scanner;
import java.util.logging.FileHandler;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.script.ScriptException;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Position;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;


/**
 * The View-Class that shows and handles the GUI (the application's main frame).<p>
 *
 * @author KPP (Kurt Pernstich: pernstich@alumni.ethz.ch)
 * @version 0.4
 */
public final class IcontrolView   extends FrameView implements GUI_Interface {
    
    //////////////////////
    // my member variables

    /** Command Line arguments passed to the constructor from IcontrolApp and
     * evaluated in <code>myInit</code> */
    private String[] m_Args;
  
    /** Data model for <code>jScriptList</code> that holds the list of script 
     * commands (the script) */
    // in Java 1.7 it could read DefaultListModel<String>
    private DefaultListModel    m_ScriptList;


    /** Data model for <code>jInstrumentList</code> that holds a list of all 
     * instruments used in the script */
    // in Java 1.7 it could read DefaultListModel<String>
    private DefaultListModel    m_InstrumentList;


    /** Data model for <code>jCommandList</code> that holds the list of 
     * Instrument-Commands of the selected instrument */
    // in JDE 1.7 it would read DefaultListModel<ScriptMethod>
    private DefaultListModel    m_CommandList;


    /**
     * Contains InstrumentName - InstrumentClassName pairs of all Instruments used 
     * in the Script (for instance PA / Agilent 4155, Tsample / Lakeshore 340, 
     * srs / SRS DS 345).<br>
     * It is populated in <code>ScriptAddLine</code>, and entries 
     * are deleted from within <code>jScriptListKeyReleased</code>. It is used in
     * <code>jInstrumentNameSelected</code> to identify the Instrument-Class
     * respectively the generic GPIB Instrument.
     */
    private HashMap<String, String> m_ScriptInstruments;


    /** reference to the Dispatcher */
    private Dispatcher      m_Dispatcher;

    /** reference to the thread that processes the script */
    private Thread          m_DispachingThread;


    /** The GUI part of the Table; also grants access to the data in the Table */
    private JTable      m_jTable;


    /** Pause button flag; is true when the user pressed the "Pause" button */
    private boolean     m_PauseScripting;

    /** Stop button flag; is true when the user pressed the "Stop" button */
    private boolean     m_StopScripting;


    /** Flag if the script has been modified since the last Save */
    private boolean     m_ScriptModified;


    /** Convenient access to application wide properties defined in iC.properties */
    private iC_Properties m_iC_Properties;

    
    /** The Logger for this class */
    private static final Logger m_Logger = Logger.getLogger("iC");
    
    /** The Logger for logging all Communication traffic. It is defined here to 
     * keep a strong reference to avoid that the Logger is being garbage collected. */
    private static final Logger m_Comm_Logger = Logger.getLogger("Comm");

    
    /** Holds the text which is displayed in the Status Area of the GUI */
    private static DefaultStyledDocument m_StatusTextDoc;
    
    /** Defines the styles for the text in the Status Area of the GUI */
    private static StyleContext m_StatusTextStyles;

    
    /** System time in milliseconds when the script was started */
    private long m_StartTime;


    /**
     * The main frame's constructor. The code that instantiates this class
     * should also call <code>myInit</code> (it was done from here before
     * introducing JUint tests).
     *
     * @param app provided by the framework
     */
    // <editor-fold defaultstate="collapsed" desc="IcontrolView">
    public IcontrolView(SingleFrameApplication app, String[] args) {
        super(app);

        initComponents();

        // store the command line parameters
        m_Args = args;

        // status bar initialization - message timeout, idle icon and busy animation, etc
        ResourceMap resourceMap = getResourceMap();
        int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
        messageTimer = new Timer(messageTimeout, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                statusMessageLabel.setText("");
            }
        });
        messageTimer.setRepeats(false);
        int busyAnimationRate = resourceMap.getInteger("StatusBar.busyAnimationRate");
        for (int i = 0; i < busyIcons.length; i++) {
            busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
        }
        busyIconTimer = new Timer(busyAnimationRate, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
                statusAnimationLabel.setIcon(busyIcons[busyIconIndex]);
            }
        });
        idleIcon = resourceMap.getIcon("StatusBar.idleIcon");
        statusAnimationLabel.setIcon(idleIcon);
        progressBar.setVisible(false);

        // connecting action tasks to status bar via TaskMonitor
        TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());
        taskMonitor.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                if ("started".equals(propertyName)) {
                    if (!busyIconTimer.isRunning()) {
                        statusAnimationLabel.setIcon(busyIcons[0]);
                        busyIconIndex = 0;
                        busyIconTimer.start();
                    }
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(true);
                } else if ("done".equals(propertyName)) {
                    busyIconTimer.stop();
                    statusAnimationLabel.setIcon(idleIcon);
                    progressBar.setVisible(false);
                    progressBar.setValue(0);
                } else if ("message".equals(propertyName)) {
                    String text = (String)(evt.getNewValue());
                    statusMessageLabel.setText((text == null) ? "" : text);
                    messageTimer.restart();
                } else if ("progress".equals(propertyName)) {
                    int value = (Integer)(evt.getNewValue());
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(value);
                }
            }
        });

        ////////////////////////////////
        // do additional initializations
        //myInit(); make it private if comments are removed
    }// </editor-fold>
    
    /**
     * @return The main <code>JComponent</code> for this View. Used to
     * "connect" Dialogs with this View.
     */
    @Override
    public JComponent getTheComponent() {
        return this.getComponent();
    }
    
    /**
     * @return The main <code>JFrame</code> for this View. Used to
     * "connect" Dialogs with this View.
     */
    @Override
    public JFrame getTheFrame() {
        return this.getFrame();
    }


    /**
     * Whenever the script has been modified (or saved) this method should be called
     * to update the ScriptModified flag and to show the User if the script has been
     * modified since the last save.
     * 
     * @param ScriptModified This becomes the new value of the ScriptModified flag
     */
    // <editor-fold defaultstate="collapsed" desc="set Script Modified">
    protected void setScriptModified(boolean ScriptModified) {

        // remember the new state
        m_ScriptModified = ScriptModified;

        // get access to the title of the boarder the jScriptList is embedded in
        TitledBorder dummy = (TitledBorder) jPanelScript.getBorder();

        // update the GUI
        if (ScriptModified) {

            // show the '*'
            dummy.setTitle("Script [modified]");

        } else {
            // remove the '*'
            dummy.setTitle("Script");
        }

        // TODO 2* update the frame/boarder (on Win this isn't done automatically it seems)
    }//</editor-fold>


    /**
     * Define a new TableModel as anonymous inner class.
     * Note that getColumnClass needs access to the Table (m_jTab)
     * because it is still called by JTable:getTableCellEditorComponent
     * although methods JTable:getCellEditor() and JTable:getCellRenderer
     * are overridden.<p>
     * 
     * The text for the Headers of the Table is defined here.<p>
     * 
     * Note that by selecting 2 or 3 as return value in <code>getColumnCount</code>
     * it is possible to show the Tooltips as part of the Table.
     */
    // <editor-fold defaultstate="collapsed" desc="my Table Model">
    private class MyTableModel extends AbstractTableModel {

        // local variables holding the Table's data
        private String[] Header = { "Parameter", "Value", "ToolTips" };
        private ArrayList<Object[]> Data = new ArrayList<Object[]>();
        

        public int getColumnCount() { 
            // don't show tooltips in the Table, so return one column less
            return Header.length - 1;
        }

        public int getRowCount() { return Data.size(); }

        @Override
        public String getColumnName(int col) { return Header[col]; }

        public Object getValueAt(int row, int col) {
            Object[] value = Data.get(row);
            return value[col];
        }

        /**
         * @return the class of the content of the selected cell.
         * The default implementation assumes the same data type
         * for the entire column.
         */
        @Override
        public Class getColumnClass(int c) {
            // while I expected that this method is not called anymore,
            // it is necessary to override it because it is called by
            // JTable:getTableCellEditorComponent

            // get selected row
            // It would be nice to get rid of m_jTable
            int row = m_jTable.getSelectedRow();


            // set row to 0 if no row was selected
            row = row<0 ? 0 : row;

            return getValueAt(row, c).getClass();
        }

        // Decides whitch cells, respectively, which columns are editable
        // only the column with the values is editable
        @Override
        public boolean isCellEditable(int row, int col) {
            if (col == 1)
                return true;
            else
                return false;
        }

        /*
         * Don't need to implement this method unless your table's
         * data can change.
         */
        @Override
        public void setValueAt(Object value, int row, int col) {
            // get the selecte row first
            Object[] OldData = Data.get(row);
            
            // set the new value
            OldData[col] = value;
            
            // write new value back
            Data.set(row, OldData);
            
            // notify all other Table listeners so they get updated
            fireTableCellUpdated(row, col);
        }

        /**
         * Adds the given parameters to the Table in a new row.
         *
         * @param ParameterName the name of the parameter (e.g. SetTemp)
         * @param Value the user will change this value.
         * @param ToolTip Longer description of the function of the parameter for the user
         */
        public void addRow(String ParameterName, Object Value, String ToolTip) {

            // make a new Array out of the given parameters
            Object[] dummy = new Object[3];
            dummy[0] = ParameterName;
            dummy[1] = Value;
            dummy[2] = ToolTip;

            // add to the Table
            Data.add(dummy);

            // notify all other Table listeners so they get updated
            fireTableDataChanged();
        }

        /**
         * Returns the ToolTip text for the specified row. This wrapper is useful
         * because the TooTips are stored in the 3rd column and because this column
         * is not shown, invoking <code>getValueAt()</code> throws an out of
         * bound exception.
         *
         * @param row The row for which the ToolTip should be returned
         * @return The TooTip text for the specified row
         */
        public String getToolTip(int row) {
            return (String)getValueAt(row, 2);
        }

        /**
         * Clears all data displayed by the Table
         */
        public void ClearAll() {
            Data.clear();

            // notify all other Table listeners so they get updated
            fireTableDataChanged();
        }
    }//</editor-fold>


    @Action
    // <editor-fold defaultstate="collapsed" desc="showAboutBox">
    public void showAboutBox() {
        if (aboutBox == null) {
            JFrame mainFrame = IcontrolApp.getApplication().getMainFrame();
            aboutBox = new IcontrolAboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        IcontrolApp.getApplication().show(aboutBox);
    }// </editor-fold>

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        mainPanel = new javax.swing.JPanel();
        statusPanel = new javax.swing.JPanel();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(51, 0), new java.awt.Dimension(51, 0), new java.awt.Dimension(51, 32767));
        jSplitPaneMain = new javax.swing.JSplitPane();
        jPanelScript = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jScriptList = new javax.swing.JList();
        jStart = new javax.swing.JButton();
        jStop = new javax.swing.JButton();
        jPause = new javax.swing.JButton();
        jScriptLine = new javax.swing.JTextField();
        jScriptLineUp = new javax.swing.JButton();
        jPython = new javax.swing.JButton();
        jPanelLeft = new javax.swing.JPanel();
        jPanelFileName = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jProjectDirSelect = new javax.swing.JButton();
        jProjectDir = new javax.swing.JTextField();
        jFileName = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jSeparator4 = new javax.swing.JSeparator();
        jTabbedPaneMakeInclude = new javax.swing.JTabbedPane();
        jPanelMake = new javax.swing.JPanel();
        jAddress = new javax.swing.JFormattedTextField();
        jLabel5 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jPortSelector = new javax.swing.JComboBox();
        jLabel4 = new javax.swing.JLabel();
        jMakeInstrument = new javax.swing.JButton();
        jInstrumentModel = new javax.swing.JComboBox();
        jInstrumentName = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jRS232params = new javax.swing.JTextField();
        jPanelInclude = new javax.swing.JPanel();
        jSelectScriptFileToInclude = new javax.swing.JButton();
        jAddSubScript = new javax.swing.JButton();
        jLabel7 = new javax.swing.JLabel();
        jScriptFileToInclude = new javax.swing.JTextField();
        jSplitPaneAutoGUI_Status = new javax.swing.JSplitPane();
        jSplitPaneAutoGUI = new javax.swing.JSplitPane();
        jPanelLists = new javax.swing.JPanel();
        jScrollPane5 = new javax.swing.JScrollPane();
        jDeviceCommandList = new javax.swing.JList() {
            public String getToolTipText(MouseEvent evt){
                return jCommandListGetToolTipText(evt);}
        }
        ;
        jScrollPane4 = new javax.swing.JScrollPane();
        jInstrumentList = new javax.swing.JList();
        jPanelTable = new javax.swing.JPanel();
        jTableScrollPane = new javax.swing.JScrollPane();
        jScriptAddInstrumentCommand = new javax.swing.JButton();
        jSendNow = new javax.swing.JButton();
        jScrollPaneStatusText = new javax.swing.JScrollPane();
        jStatusText1 = new javax.swing.JEditorPane();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu jMenuFile = new javax.swing.JMenu();
        jFileNew = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        jFileLoad = new javax.swing.JMenuItem();
        jFileAppend = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        jFileSaveAs = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        jMenuOptions = new javax.swing.JMenu();
        jNoCommunicationMode = new javax.swing.JCheckBoxMenuItem();
        jAutoPauseScript = new javax.swing.JCheckBoxMenuItem();
        jMenuGPIB = new javax.swing.JMenu();
        jRB_GPIB_NI = new javax.swing.JRadioButtonMenuItem();
        jRB_GPIB_Prologix = new javax.swing.JRadioButtonMenuItem();
        jRB_GPIB_IOtech = new javax.swing.JCheckBoxMenuItem();
        javax.swing.JMenu jMenuHelp = new javax.swing.JMenu();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        jiCWebsite = new javax.swing.JMenuItem();
        jiCPublication = new javax.swing.JMenuItem();
        jSeparator5 = new javax.swing.JPopupMenu.Separator();
        jiCOnlineJavadoc = new javax.swing.JMenuItem();
        jiCLocalJavadoc = new javax.swing.JMenuItem();
        buttonGroupGPIBMenu = new javax.swing.ButtonGroup();

        mainPanel.setName("mainPanel"); // NOI18N

        statusPanel.setName("statusPanel"); // NOI18N
        statusPanel.setLayout(new java.awt.GridBagLayout());

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(icontrol.IcontrolApp.class).getContext().getResourceMap(IcontrolView.class);
        statusMessageLabel.setText(resourceMap.getString("statusMessageLabel.text")); // NOI18N
        statusMessageLabel.setName("statusMessageLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(5, 20, 5, 0);
        statusPanel.add(statusMessageLabel, gridBagConstraints);

        statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        statusAnimationLabel.setText(resourceMap.getString("statusAnimationLabel.text")); // NOI18N
        statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(5, 30, 5, 5);
        statusPanel.add(statusAnimationLabel, gridBagConstraints);

        progressBar.setName("progressBar"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
        statusPanel.add(progressBar, gridBagConstraints);

        filler1.setName("filler1"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        statusPanel.add(filler1, gridBagConstraints);

        jSplitPaneMain.setBorder(null);
        jSplitPaneMain.setContinuousLayout(true);
        jSplitPaneMain.setName("jSplitPaneMain"); // NOI18N

        jPanelScript.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("jPanelScript.border.title"))); // NOI18N
        jPanelScript.setMinimumSize(new java.awt.Dimension(290, 230));
        jPanelScript.setName("jPanelScript"); // NOI18N
        jPanelScript.setPreferredSize(new java.awt.Dimension(500, 640));
        jPanelScript.setLayout(new java.awt.GridBagLayout());

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        jScriptList.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "jScriptList" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        jScriptList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScriptList.setToolTipText(resourceMap.getString("jScriptList.toolTipText")); // NOI18N
        jScriptList.setDragEnabled(true);
        jScriptList.setDropMode(javax.swing.DropMode.INSERT);
        jScriptList.setName("jScriptList"); // NOI18N
        jScriptList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                jScriptCommandSelected(evt);
            }
        });
        jScriptList.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                jScriptListKeyReleased(evt);
            }
        });
        jScrollPane1.setViewportView(jScriptList);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 5, 5);
        jPanelScript.add(jScrollPane1, gridBagConstraints);

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(icontrol.IcontrolApp.class).getContext().getActionMap(IcontrolView.class, this);
        jStart.setAction(actionMap.get("Start")); // NOI18N
        jStart.setText(resourceMap.getString("jStart.text")); // NOI18N
        jStart.setToolTipText(resourceMap.getString("jStart.toolTipText")); // NOI18N
        jStart.setName("jStart"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.8;
        jPanelScript.add(jStart, gridBagConstraints);

        jStop.setAction(actionMap.get("Stop")); // NOI18N
        jStop.setText(resourceMap.getString("jStop.text")); // NOI18N
        jStop.setToolTipText(resourceMap.getString("jStop.toolTipText")); // NOI18N
        jStop.setName("jStop"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.1;
        jPanelScript.add(jStop, gridBagConstraints);

        jPause.setAction(actionMap.get("Pause")); // NOI18N
        jPause.setText(resourceMap.getString("jPause.text")); // NOI18N
        jPause.setToolTipText(resourceMap.getString("jPause.toolTipText")); // NOI18N
        jPause.setName("jPause"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.1;
        jPanelScript.add(jPause, gridBagConstraints);

        jScriptLine.setToolTipText(resourceMap.getString("jScriptLine.toolTipText")); // NOI18N
        jScriptLine.setName("jScriptLine"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        jPanelScript.add(jScriptLine, gridBagConstraints);

        jScriptLineUp.setAction(actionMap.get("ScriptAddUserLine")); // NOI18N
        jScriptLineUp.setText(resourceMap.getString("jScriptLineUp.text")); // NOI18N
        jScriptLineUp.setToolTipText(resourceMap.getString("jScriptLineUp.toolTipText")); // NOI18N
        jScriptLineUp.setName("jScriptLineUp"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        jPanelScript.add(jScriptLineUp, gridBagConstraints);

        jPython.setText(resourceMap.getString("jPython.text")); // NOI18N
        jPython.setToolTipText(resourceMap.getString("jPython.toolTipText")); // NOI18N
        jPython.setName("jPython"); // NOI18N
        jPython.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Python(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        jPanelScript.add(jPython, gridBagConstraints);

        jSplitPaneMain.setRightComponent(jPanelScript);

        jPanelLeft.setName("jPanelLeft"); // NOI18N
        jPanelLeft.setLayout(new java.awt.GridBagLayout());

        jPanelFileName.setMinimumSize(new java.awt.Dimension(250, 57));
        jPanelFileName.setName("jPanelFileName"); // NOI18N
        jPanelFileName.setLayout(new java.awt.GridBagLayout());

        jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        jPanelFileName.add(jLabel2, gridBagConstraints);

        jProjectDirSelect.setAction(actionMap.get("ChooseProjectDir")); // NOI18N
        jProjectDirSelect.setText(resourceMap.getString("jProjectDirSelect.text")); // NOI18N
        jProjectDirSelect.setToolTipText(resourceMap.getString("jProjectDirSelect.toolTipText")); // NOI18N
        jProjectDirSelect.setName("jProjectDirSelect"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.ipadx = -20;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanelFileName.add(jProjectDirSelect, gridBagConstraints);

        jProjectDir.setToolTipText(resourceMap.getString("jProjectDir.toolTipText")); // NOI18N
        jProjectDir.setName("jProjectDir"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        jPanelFileName.add(jProjectDir, gridBagConstraints);

        jFileName.setToolTipText(resourceMap.getString("jFileName.toolTipText")); // NOI18N
        jFileName.setFocusCycleRoot(true);
        jFileName.setName("jFileName"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        jPanelFileName.add(jFileName, gridBagConstraints);

        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        jPanelFileName.add(jLabel1, gridBagConstraints);

        jSeparator4.setName("jSeparator4"); // NOI18N
        jPanelFileName.add(jSeparator4, new java.awt.GridBagConstraints());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
        jPanelLeft.add(jPanelFileName, gridBagConstraints);

        jTabbedPaneMakeInclude.setToolTipText(resourceMap.getString("jTabbedPaneMakeInclude.toolTipText")); // NOI18N
        jTabbedPaneMakeInclude.setMinimumSize(new java.awt.Dimension(300, 134));
        jTabbedPaneMakeInclude.setName("jTabbedPaneMakeInclude"); // NOI18N
        jTabbedPaneMakeInclude.setPreferredSize(new java.awt.Dimension(330, 134));

        jPanelMake.setMinimumSize(new java.awt.Dimension(309, 100));
        jPanelMake.setName("jPanelMake"); // NOI18N
        jPanelMake.setPreferredSize(new java.awt.Dimension(309, 85));
        jPanelMake.setLayout(new java.awt.GridBagLayout());

        jAddress.setToolTipText(resourceMap.getString("jAddress.toolTipText")); // NOI18N
        jAddress.setName("jAddress"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipadx = 40;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.6;
        jPanelMake.add(jAddress, gridBagConstraints);

        jLabel5.setText(resourceMap.getString("jLabel5.text")); // NOI18N
        jLabel5.setName("jLabel5"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        jPanelMake.add(jLabel5, gridBagConstraints);

        jLabel3.setText(resourceMap.getString("jLabel3.text")); // NOI18N
        jLabel3.setName("jLabel3"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        jPanelMake.add(jLabel3, gridBagConstraints);

        jPortSelector.setName("jPortSelector"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipadx = 20;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.4;
        jPanelMake.add(jPortSelector, gridBagConstraints);

        jLabel4.setText(resourceMap.getString("jLabel4.text")); // NOI18N
        jLabel4.setName("jLabel4"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        jPanelMake.add(jLabel4, gridBagConstraints);

        jMakeInstrument.setAction(actionMap.get("ScriptAddMake")); // NOI18N
        jMakeInstrument.setText(resourceMap.getString("jMakeInstrument.text")); // NOI18N
        jMakeInstrument.setToolTipText(resourceMap.getString("jMakeInstrument.toolTipText")); // NOI18N
        jMakeInstrument.setName("jMakeInstrument"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 5);
        jPanelMake.add(jMakeInstrument, gridBagConstraints);

        jInstrumentModel.setMaximumRowCount(15);
        jInstrumentModel.setName("jInstrumentModel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.6;
        jPanelMake.add(jInstrumentModel, gridBagConstraints);

        jInstrumentName.setToolTipText(resourceMap.getString("jInstrumentName.toolTipText")); // NOI18N
        jInstrumentName.setName("jInstrumentName"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.4;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
        jPanelMake.add(jInstrumentName, gridBagConstraints);

        jLabel6.setText(resourceMap.getString("jLabel6.text")); // NOI18N
        jLabel6.setName("jLabel6"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        jPanelMake.add(jLabel6, gridBagConstraints);

        jLabel8.setText(resourceMap.getString("jLabel8.text")); // NOI18N
        jLabel8.setName("jLabel8"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        jPanelMake.add(jLabel8, gridBagConstraints);

        jRS232params.setText(resourceMap.getString("jRS232params.text")); // NOI18N
        jRS232params.setToolTipText(resourceMap.getString("jRS232params.toolTipText")); // NOI18N
        jRS232params.setName("jRS232params"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
        jPanelMake.add(jRS232params, gridBagConstraints);

        jTabbedPaneMakeInclude.addTab(resourceMap.getString("jPanelMake.TabConstraints.tabTitle"), jPanelMake); // NOI18N

        jPanelInclude.setMinimumSize(new java.awt.Dimension(0, 0));
        jPanelInclude.setName("jPanelInclude"); // NOI18N
        jPanelInclude.setLayout(new java.awt.GridBagLayout());

        jSelectScriptFileToInclude.setAction(actionMap.get("SelectScriptToInclude")); // NOI18N
        jSelectScriptFileToInclude.setText(resourceMap.getString("jSelectScriptFileToInclude.text")); // NOI18N
        jSelectScriptFileToInclude.setName("jSelectScriptFileToInclude"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        jPanelInclude.add(jSelectScriptFileToInclude, gridBagConstraints);

        jAddSubScript.setAction(actionMap.get("ScriptAddSubScript")); // NOI18N
        jAddSubScript.setText(resourceMap.getString("jAddSubScript.text")); // NOI18N
        jAddSubScript.setToolTipText(resourceMap.getString("jAddSubScript.toolTipText")); // NOI18N
        jAddSubScript.setName("jAddSubScript"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        jPanelInclude.add(jAddSubScript, gridBagConstraints);

        jLabel7.setText(resourceMap.getString("jLabel7.text")); // NOI18N
        jLabel7.setName("jLabel7"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        jPanelInclude.add(jLabel7, gridBagConstraints);

        jScriptFileToInclude.setToolTipText(resourceMap.getString("jScriptFileToInclude.toolTipText")); // NOI18N
        jScriptFileToInclude.setName("jScriptFileToInclude"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        jPanelInclude.add(jScriptFileToInclude, gridBagConstraints);

        jTabbedPaneMakeInclude.addTab(resourceMap.getString("jPanelInclude.TabConstraints.tabTitle"), jPanelInclude); // NOI18N

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
        jPanelLeft.add(jTabbedPaneMakeInclude, gridBagConstraints);

        jSplitPaneAutoGUI_Status.setBorder(null);
        jSplitPaneAutoGUI_Status.setDividerLocation(330);
        jSplitPaneAutoGUI_Status.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPaneAutoGUI_Status.setContinuousLayout(true);
        jSplitPaneAutoGUI_Status.setMinimumSize(new java.awt.Dimension(270, 190));
        jSplitPaneAutoGUI_Status.setName("jSplitPaneAutoGUI_Status"); // NOI18N
        jSplitPaneAutoGUI_Status.setPreferredSize(new java.awt.Dimension(330, 440));

        jSplitPaneAutoGUI.setBorder(null);
        jSplitPaneAutoGUI.setDividerLocation(170);
        jSplitPaneAutoGUI.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPaneAutoGUI.setContinuousLayout(true);
        jSplitPaneAutoGUI.setMinimumSize(new java.awt.Dimension(270, 160));
        jSplitPaneAutoGUI.setName("jSplitPaneAutoGUI"); // NOI18N
        jSplitPaneAutoGUI.setPreferredSize(new java.awt.Dimension(330, 340));

        jPanelLists.setName("jPanelLists"); // NOI18N
        jPanelLists.setLayout(new java.awt.GridBagLayout());

        jScrollPane5.setName("jScrollPane5"); // NOI18N

        jDeviceCommandList.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "jDeviceCommandList" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        jDeviceCommandList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jDeviceCommandList.setToolTipText(resourceMap.getString("jDeviceCommandList.toolTipText")); // NOI18N
        jDeviceCommandList.setName("jDeviceCommandList"); // NOI18N
        jDeviceCommandList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                jDeviceCommandSelected(evt);
            }
        });
        jScrollPane5.setViewportView(jDeviceCommandList);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanelLists.add(jScrollPane5, gridBagConstraints);

        jScrollPane4.setName("jScrollPane4"); // NOI18N

        jInstrumentList.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "jInstrumentList" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        jInstrumentList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jInstrumentList.setToolTipText(resourceMap.getString("jInstrumentList.toolTipText")); // NOI18N
        jInstrumentList.setName("jInstrumentList"); // NOI18N
        jInstrumentList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                jInstrumentNameSelected(evt);
            }
        });
        jScrollPane4.setViewportView(jInstrumentList);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanelLists.add(jScrollPane4, gridBagConstraints);

        jSplitPaneAutoGUI.setTopComponent(jPanelLists);

        jPanelTable.setName("jPanelTable"); // NOI18N
        jPanelTable.setLayout(new java.awt.GridBagLayout());

        jTableScrollPane.setName("jTableScrollPane"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 0);
        jPanelTable.add(jTableScrollPane, gridBagConstraints);

        jScriptAddInstrumentCommand.setAction(actionMap.get("ScriptAddInstrumentCommand")); // NOI18N
        jScriptAddInstrumentCommand.setText(resourceMap.getString("jScriptAddInstrumentCommand.text")); // NOI18N
        jScriptAddInstrumentCommand.setToolTipText(resourceMap.getString("jScriptAddInstrumentCommand.toolTipText")); // NOI18N
        jScriptAddInstrumentCommand.setName("jScriptAddInstrumentCommand"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        jPanelTable.add(jScriptAddInstrumentCommand, gridBagConstraints);

        jSendNow.setAction(actionMap.get("SendNow")); // NOI18N
        jSendNow.setText(resourceMap.getString("jSendNow.text")); // NOI18N
        jSendNow.setToolTipText(resourceMap.getString("jSendNow.toolTipText")); // NOI18N
        jSendNow.setName("jSendNow"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanelTable.add(jSendNow, gridBagConstraints);

        jSplitPaneAutoGUI.setRightComponent(jPanelTable);

        jSplitPaneAutoGUI_Status.setTopComponent(jSplitPaneAutoGUI);

        jScrollPaneStatusText.setMinimumSize(new java.awt.Dimension(250, 57));
        jScrollPaneStatusText.setName("jScrollPaneStatusText"); // NOI18N
        jScrollPaneStatusText.setPreferredSize(new java.awt.Dimension(280, 95));

        jStatusText1.setContentType(resourceMap.getString("jStatusText1.contentType")); // NOI18N
        jStatusText1.setEditable(false);
        jStatusText1.setName("jStatusText1"); // NOI18N
        jScrollPaneStatusText.setViewportView(jStatusText1);

        jSplitPaneAutoGUI_Status.setRightComponent(jScrollPaneStatusText);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
        jPanelLeft.add(jSplitPaneAutoGUI_Status, gridBagConstraints);

        jSplitPaneMain.setLeftComponent(jPanelLeft);

        org.jdesktop.layout.GroupLayout mainPanelLayout = new org.jdesktop.layout.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(mainPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jSplitPaneMain, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 852, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, statusPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 852, Short.MAX_VALUE))
                .addContainerGap())
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(jSplitPaneMain, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 686, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(statusPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        );

        menuBar.setName("menuBar"); // NOI18N

        jMenuFile.setText(resourceMap.getString("jMenuFile.text")); // NOI18N
        jMenuFile.setName("jMenuFile"); // NOI18N

        jFileNew.setAction(actionMap.get("FileNew")); // NOI18N
        jFileNew.setText(resourceMap.getString("jFileNew.text")); // NOI18N
        jFileNew.setName("jFileNew"); // NOI18N
        jMenuFile.add(jFileNew);
        jMenuFile.add(jSeparator2);

        jFileLoad.setAction(actionMap.get("FileLoad")); // NOI18N
        jFileLoad.setText(resourceMap.getString("jFileLoad.text")); // NOI18N
        jFileLoad.setToolTipText(resourceMap.getString("jFileLoad.toolTipText")); // NOI18N
        jFileLoad.setName("jFileLoad"); // NOI18N
        jMenuFile.add(jFileLoad);

        jFileAppend.setAction(actionMap.get("FileAppend")); // NOI18N
        jFileAppend.setText(resourceMap.getString("jFileAppend.text")); // NOI18N
        jFileAppend.setName("jFileAppend"); // NOI18N
        jMenuFile.add(jFileAppend);
        jMenuFile.add(jSeparator3);

        jFileSaveAs.setAction(actionMap.get("SaveScript")); // NOI18N
        jFileSaveAs.setText(resourceMap.getString("jFileSaveAs.text")); // NOI18N
        jFileSaveAs.setName("jFileSaveAs"); // NOI18N
        jMenuFile.add(jFileSaveAs);

        jSeparator1.setName("jSeparator1"); // NOI18N
        jMenuFile.add(jSeparator1);

        exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        jMenuFile.add(exitMenuItem);

        menuBar.add(jMenuFile);

        jMenuOptions.setText(resourceMap.getString("jMenuOptions.text")); // NOI18N
        jMenuOptions.setName("jMenuOptions"); // NOI18N

        jNoCommunicationMode.setSelected(true);
        jNoCommunicationMode.setText(resourceMap.getString("jNoCommunicationMode.text")); // NOI18N
        jNoCommunicationMode.setToolTipText(resourceMap.getString("jNoCommunicationMode.toolTipText")); // NOI18N
        jNoCommunicationMode.setName("jNoCommunicationMode"); // NOI18N
        jMenuOptions.add(jNoCommunicationMode);

        jAutoPauseScript.setSelected(true);
        jAutoPauseScript.setText(resourceMap.getString("jAutoPauseScript.text")); // NOI18N
        jAutoPauseScript.setToolTipText(resourceMap.getString("jAutoPauseScript.toolTipText")); // NOI18N
        jAutoPauseScript.setName("jAutoPauseScript"); // NOI18N
        jMenuOptions.add(jAutoPauseScript);

        menuBar.add(jMenuOptions);

        jMenuGPIB.setText(resourceMap.getString("jMenuGPIB.text")); // NOI18N
        jMenuGPIB.setName("jMenuGPIB"); // NOI18N

        buttonGroupGPIBMenu.add(jRB_GPIB_NI);
        jRB_GPIB_NI.setText(resourceMap.getString("jRB_GPIB_NI.text")); // NOI18N
        jRB_GPIB_NI.setName("jRB_GPIB_NI"); // NOI18N
        jMenuGPIB.add(jRB_GPIB_NI);

        buttonGroupGPIBMenu.add(jRB_GPIB_Prologix);
        jRB_GPIB_Prologix.setText(resourceMap.getString("jRB_GPIB_Prologix.text")); // NOI18N
        jRB_GPIB_Prologix.setName("jRB_GPIB_Prologix"); // NOI18N
        jRB_GPIB_Prologix.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showWarningPrologix(evt);
            }
        });
        jMenuGPIB.add(jRB_GPIB_Prologix);

        buttonGroupGPIBMenu.add(jRB_GPIB_IOtech);
        jRB_GPIB_IOtech.setSelected(true);
        jRB_GPIB_IOtech.setText(resourceMap.getString("jRB_GPIB_IOtech.text")); // NOI18N
        jRB_GPIB_IOtech.setName("jRB_GPIB_IOtech"); // NOI18N
        jMenuGPIB.add(jRB_GPIB_IOtech);

        menuBar.add(jMenuGPIB);

        jMenuHelp.setText(resourceMap.getString("jMenuHelp.text")); // NOI18N
        jMenuHelp.setName("jMenuHelp"); // NOI18N

        aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        jMenuHelp.add(aboutMenuItem);

        jiCWebsite.setText(resourceMap.getString("jiCWebsite.text")); // NOI18N
        jiCWebsite.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showWebsite(evt);
            }
        });
        jMenuHelp.add(jiCWebsite);

        jiCPublication.setText(resourceMap.getString("jiCPublication.text")); // NOI18N
        jiCPublication.setName("jiCPublication"); // NOI18N
        jiCPublication.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showPublication(evt);
            }
        });
        jMenuHelp.add(jiCPublication);

        jSeparator5.setName("jSeparator5"); // NOI18N
        jMenuHelp.add(jSeparator5);

        jiCOnlineJavadoc.setText(resourceMap.getString("jiCOnlineJavadoc.text")); // NOI18N
        jiCOnlineJavadoc.setName("jiCOnlineJavadoc"); // NOI18N
        jiCOnlineJavadoc.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showOnlineJavadoc(evt);
            }
        });
        jMenuHelp.add(jiCOnlineJavadoc);

        jiCLocalJavadoc.setText(resourceMap.getString("jiCLocalJavadoc.text")); // NOI18N
        jiCLocalJavadoc.setName("jiCLocalJavadoc"); // NOI18N
        jiCLocalJavadoc.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showLocalJavadoc(evt);
            }
        });
        jMenuHelp.add(jiCLocalJavadoc);

        menuBar.add(jMenuHelp);

        setComponent(mainPanel);
        setMenuBar(menuBar);
        setStatusBar(statusPanel);
    }// </editor-fold>//GEN-END:initComponents


    /**
     * This method returns <code>true</code> if Scripting has been paused (either
     * by the user pressing 'Pause' button or by enabling Options/AutoPause) and
     * <code>false</code> if the Script is being processed. The method returns
     * immediately if Scripting has been stopped (because the user pressed the
     * 'Stop' button).<p>
     *
     * When <code>WaitUntilNotPaused</code> is set to <code>true</code>,
     * the method waits until the Script is being processed (Scripting has been
     * resumed if it was paused), and in this case, always returns <code>false</code>.
     *
     * @param WaitUntilNotPaused When <code>false</code> the method returns immediately
     * the present state of scripting, and waits until the Script is being executed
     * when set to <code>true</code>.
     *
     * @return <code>true</code> when scripting is paused, and <code>false</code>
     * if the script is being processed.
     */
    // <editor-fold defaultstate="collapsed" desc="isPaused()">
    @Override
    public boolean isPaused(boolean WaitUntilNotPaused) {

        // check if method should wait until scripting has been resumed
        if ( WaitUntilNotPaused ) {

            // check for pause button
            while ( m_PauseScripting && !m_StopScripting ) {
                try {
                    // in pause mode, wait a bit and check again
                    Thread.sleep(300);
                } catch (InterruptedException ignore) {}
            }
        }

        // return state of scripting
        return m_PauseScripting;

    }//</editor-fold>


    /**
     * Getter method for the state of the menu item to select Auto-Pausing of
     * the Script. When true
     *
     * @return Returns true if the user selected Auto-Pausing of the script, which
     * means that the Dispatcher class re-enables pausing of the script after each
     * script line.
     */
    // <editor-fold defaultstate="collapsed" desc="isAutoPaused()">
    @Override
    public boolean isAutoPaused() {
        return jAutoPauseScript.getState();
    }//</editor-fold>


    /**
     * Returns the FileName including the project path as specified in the GUI. 
     * The FileExtension is also appended, and if no '.' was specified, it is 
     * added automatically. If the resultant file name already exists in the 
     * project directory, a counter is added, so that no files are overwritten.
     *
     * @param FileExtension This String is appended to the FileName; if it does
     * not include a '.', a '.' is added. If <code>FileExtension</code> is an 
     * empty String, only Path+FileName is returned. If the file exists, a counter
     * is added before the extension.
     *
     * @return FileName including path and FileExtension
     */
    // <editor-fold defaultstate="collapsed" desc="get File Name + Extension">
    @Override
    public String getFileName(String FileExtension) {
        
        // returned variable
        String ret;

        // get the path
        String PathAndName = jProjectDir.getText();

        // path separator character (platform specific)
        String sep = File.separator;


        // check if it contains a path separator
        if ( !PathAndName.endsWith(sep)) {
            // it doesn't contain a path separator, so add it
            PathAndName += sep;
        }

        // add the filename
        PathAndName += jFileName.getText();
        
        
        // add File Extension if a non-empty String was specified
        if ( !FileExtension.isEmpty() ) {

            // add a '.' if no '.' was included
            if ( !FileExtension.contains(".") )
                FileExtension = "." + FileExtension;
        }
        
        // build the returned file name
        ret = PathAndName + FileExtension;
                
        
        /////////////////////////////
        // add counter if file exists
        
        int cnt = 0;
        File f;
        
        do {
            if (cnt == 0) {
                // build "original" FileName (without counter)
                ret = String.format("%s%s", PathAndName, FileExtension);
                
            } else {
                // build FileName with counter
                ret = String.format("%s%03d%s", PathAndName, cnt, FileExtension);
            }

            // make a new file
            f = new File(ret);
            
            // increase counter
            cnt++;

        } while (f.exists());
        
        // log filename
        m_Logger.log(Level.FINE, "getFileName will return: {0}\n", ret);
        
        // return
        return ret;
    }//</editor-fold>


    /**
     * Returns the Project Path including a file separator at the end
     * @return ProjectPath including file separator
     */
    // <editor-fold defaultstate="collapsed" desc="getProjectPath">
    @Override
    public String getProjectPath() {

        // get the path
        String ret = jProjectDir.getText();

        // path separator character (platform specific)
        String sep = File.separator;


        // check if it contains a path separator
        if ( !ret.endsWith(sep)) {
            // it doesn't contain a path separator, so add it
            ret += sep;
        }
        
        // return the result
        return ret;
    }//</editor-fold>
    

    /**
     * Returns the ToolTip Text for the item in the <code>jCommandList</code>.
     * The default code generated for the jList from NetBeans has been modified
     * (right click on the jList and choose Custom Code) to call this method.<p>
     *
     * The ToolTipText is taken from the <code>ScriptMethod</code> which is 
     * stored in the elements of the List.
     *
     * @param evt holds the position of the mouse.
     * @return A String that holds the ToolTip Text (DescripttionForUser in the
     * <code>AutoGUIAnnotation</code>.
     */
    // <editor-fold defaultstate="collapsed" desc="Command List Get Tool Tip Text">
    private String jCommandListGetToolTipText(MouseEvent evt) {

        String str = "";

        // get the index of the selected item/cell
        int i = jDeviceCommandList.locationToIndex( evt.getPoint() );

        
        // if there was no error
        if ( i != -1) {
            // get the selected script command
            ScriptMethod met = (ScriptMethod) jDeviceCommandList.getSelectedValue();
            
            // get the name of the selected instrument command
            str = met.DescriptionForUser;
        }

        return str;
    }//</editor-fold>


    /**
     * Called when the User selects 'iC Website' from the Help Menu. It opens
     * the default browser to display the homepage of iC.
     * @param evt not used
     */
    // <editor-fold defaultstate="collapsed" desc="show project Website">
    private void showWebsite(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showWebsite
        
        // URL to the site
        String url = "http://java.net/projects/icontrol";

        try {
            // open default browser
            Desktop.getDesktop().browse(java.net.URI.create(url));

        } catch (Exception ex) {
            DisplayStatusMessage("Could not open the default browser.\n"
                                    + ex.getMessage() + "\n"
                                    + "Please visit " + url + "\n");

            // beep
            Toolkit.getDefaultToolkit().beep();
        }
    }//GEN-LAST:event_showWebsite
    //</editor-fold>


    /**
     * Called when the User selects 'Show online javadoc' from the Help Menu. It opens
     * the default browser to display the online javadoc.
     * @param evt
     */
    // <editor-fold defaultstate="collapsed" desc="show Online Javadoc">
    private void showOnlineJavadoc(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showOnlineJavadoc
   
        // URL to the site
        String url = "http://icontrol.java.net/";

        try {
            // open default browser
            Desktop.getDesktop().browse(java.net.URI.create(url));

        } catch (Exception ex) {
            DisplayStatusMessage("Error: Could not open the default browser.\n"
                               + "Please visit " + url + "\n");
        }
    }//GEN-LAST:event_showOnlineJavadoc
    //</editor-fold>
    
    /**
     * This method is called when the user selects to use the Prologix GPIB-USB 
     * controller. It displays a Message Dialog warning the user that the program 
     * might terminate when the FTDI direct drivers are not installed.
     * @param evt
     */
    // TODO 1* delete warning Prologix
    // <editor-fold defaultstate="collapsed" desc="show Warning Prologix">
    private void showWarningPrologix(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showWarningPrologix

        // show warning only for non-Windows OSes
        if ( false && !System.getProperty("os.name").toLowerCase().contains("win") ) {
            String str = "You are about to use the Prologix GPIB-USB controler.\n\n";
            str += "ATTENTION: Support for the Prologix GPIB-USB controller is currently\n"
                 + "under development and not yet fully tested/functional.\n";

            // show the Message
            Object[] Buttons = { "I understood this warning"};
            JOptionPane.showOptionDialog(this.getComponent(), str,
                        "Attention", JOptionPane.DEFAULT_OPTION,
                        JOptionPane.WARNING_MESSAGE, m_iC_Properties.getLogoSmall(),
                        Buttons, Buttons[0]);
        }
    }//GEN-LAST:event_showWarningPrologix
    //</editor-fold>
    
    
    /**
     * Called when the user selected a line in the script or the script is
     * being updated.
     * @param evt not used
     */
    // <editor-fold defaultstate="collapsed" desc="jScript Command Selected">
    private void jScriptCommandSelected(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_jScriptCommandSelected
        
        // upon selection of an item in a list, this event might be sent
        // multiple times, so only react on this event when the selection
        // is complete
        if ( evt.getValueIsAdjusting() )
            return;


        // do nothing if nothing was selected
        if (jScriptList.getSelectedIndex() == -1)
            return;

        // get the content of the current selection
        String Line = jScriptList.getSelectedValue().toString();

        // copy the content to the line below for the user to edit
        jScriptLine.setText(Line);
        
        
        // split it into tokens
        ArrayList<String> Tokens = Utilities.Tokenizer(Line);


        // check if the first Token appears in the Instrument List (e.g. 'Tsample')
        if (Tokens != null && Tokens.size() >= 1) {

            int index = m_InstrumentList.indexOf(Tokens.get(0));

            if (index != -1) {
                // Instrument-Name found, so select it in the list
                jInstrumentList.setSelectedIndex(index);

                /* setSelectedIndex() returns after jInstrumentNameSelected()
                 * has been executed and the Instrument List has been updated.
                 * While I did not find a description of this behaviour in
                 * the javadoc, it appears to be the case, which is good anyway.
                 * If you know more, please inform me too! 
                 * http://java.sun.com/products/jfc/tsc/tech_topics/jlist_1/jlist.html */
            } else {
                // no Instrument-Name was selected, so don't do anything, that means,
                // keep the current selection, and just return
                return;
            }
        } else {
            // a too short script line was selected, so don't do anything and just return
            return;
        }


        // check if the Instrument-Command appears in the Command List (e.g. 'SetTemp')
        if (Tokens.size() >= 2) {

            // because Token.get(1) is used, DefaultListModel (Vector) uses
            // String.equals to determin the index. I need to pass a ScriptMethod
            // object to use ScriptMethod.equals and implement the equal method there
            //int index = m_CommandList.indexOf(Tokens.get(1));

            // make a dummy ScriptMethod object with the name to look for
            ScriptMethod sm = new ScriptMethod();
            sm.DeviceCommandName = Tokens.get(1);

            int index = m_CommandList.indexOf(sm);

            // select command
            if (index != -1) {
                // Instrument-Name found, so select it in the list
                jDeviceCommandList.setSelectedIndex(index);
            }
        }
        
        
        
        // get the TableModel
        MyTableModel tm = (MyTableModel)m_jTable.getModel();

        // fill the table
        String TT;
        for (int i=0; i < tm.getRowCount() ; i++) {

            // ensure index is inside bounds
            // when a wrong script line was added, just fill the table as far as possible
            if (Tokens.size() >= tm.getRowCount()+2) {
                // get the current value to reveal the data type
                Object value = tm.getValueAt(i, 1);

                // assign the new value with the proper type
                try {
                    if (value.getClass() == String.class)
                        value = Tokens.get(i+2);
                    else if (value.getClass() == Integer.class)
                        value = new Integer(Tokens.get(i+2));
                    else if (value.getClass() == Double.class)
                        value = new Double(Tokens.get(i+2));
                    else if (value.getClass() == Float.class)
                        value = new Float(Tokens.get(i+2));
                    else if (value.getClass() == Boolean.class)
                        value = Boolean.valueOf(Tokens.get(i+2));
                    else {
                        // default branch. data type not supported
                        String str = "The specified data type is not supported.\n"
                            + "Please report this incident to the developer and include the log files.\n";

                        // show a dialog
                        JOptionPane.showMessageDialog(this.getComponent(), str,
                                "Programming Error", JOptionPane.ERROR_MESSAGE, m_iC_Properties.getLogoSmall());

                        // log event
                        m_Logger.severe(str);

                        // exit this method
                        return;
                    }
                } catch (NumberFormatException ex) {
                    // ignore this error, which means that the original value
                    // (which should be the default value, hence, 0, false, or "")
                    // remains in the Table
                    // Any possible error will be recovered during Syntax-Check-Mode anyways
                }

                // set value in col 1 of the Table
                tm.setValueAt(value, i, 1);
            }
        }
}//GEN-LAST:event_jScriptCommandSelected
    ////</editor-fold>
    
    
    private void jScriptListKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jScriptListKeyReleased
        
        // check if the key was a delete key
        if (    evt.getKeyCode() == KeyEvent.VK_DELETE ||
                evt.getKeyCode() == 8 ) { // Commad+backspace on a Mac
            
            
            // make sure an item is selected or return otherwise
            // this prevents null-pointer exceptions
            if (jScriptList.isSelectionEmpty())
                return;
            
            // get index of current selection
            int ListIndex = jScriptList.getSelectedIndex();
            
            
            //////////////////////////////////////////////
            // check if the script line was a make command
            
            // get the line
            String Line = m_ScriptList.getElementAt(ListIndex).toString();
            
            // split into Tokens
            ArrayList<String> Tokens = Utilities.Tokenizer(Line);
            
            
            // is the first token a 'MAKE' command?
            if (Tokens != null &&
                    Tokens.get(0).equalsIgnoreCase("MAKE")) {
                
                // yes, so let's find the Instrument name in the Instrument-List
                int i = m_InstrumentList.indexOf(Tokens.get(1));
                
                // and remove it from the list
                if (i != -1) {
                    m_InstrumentList.remove(i);
                    
                    // remove it also from the Script-Instrument-Hashmap
                    m_ScriptInstruments.remove(Tokens.get(1));
                }
            }
            
            
            // delete this entry
            m_ScriptList.removeElementAt(ListIndex);
            
            
            // set the selection to the line above or line below the deleted one
            if (ListIndex > 0) {
                // select the line above the deleted one
                jScriptList.setSelectedIndex(ListIndex-1);
            } else {
                // select the line below the deleted one
                jScriptList.setSelectedIndex(ListIndex);
                
            }
            
            // remember that the script has been modified
            setScriptModified(true);
        }
}//GEN-LAST:event_jScriptListKeyReleased

    
    /**
     * Is called when the user selects a different Script Instrument, and
     * fills the CommandList with the commands of the selected Instrument.
     *
     * @param evt is not really used and passed by JAVA
     */
    // <editor-fold defaultstate="collapsed" desc="Instrument Name Selected">
    private void jInstrumentNameSelected(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_jInstrumentNameSelected
        
        // upon selection of an item in a list, this event might be sent
        // multiple times, so only react on this event when the selection
        // is complete
        if ( evt.getValueIsAdjusting() )
            return;

        // make sure an item is selected or return otherwise
        // this prevents null-pointer exceptions
        if (jInstrumentList.isSelectionEmpty())
            return;

        // local variable
        Device DeviceInstance;


        // disable the button to add a Script Command from the Auto GUI
        jScriptAddInstrumentCommand.setEnabled(false);
        
        /* If the user changed a value in the table and selects a different instrument
         * before editing is ended, the 'editing focus' remains, and it's not possible
         * to edit an other cell. Therefore, end editing by selecting an other cell
         * (which does not exist) to edit. */
        m_jTable.editCellAt(-1, -1);




        // get the selected Instrument Name (e.g. Tsample, PA, srs, ...)
        String InstrumentName = (String) jInstrumentList.getSelectedValue();
      
        // get the Instrument-Class-Name (e.g. Lakeshore 340, Agilent 4155, SRS DS345, ...)
        String InstrumentClassName = m_ScriptInstruments.get(InstrumentName);
        
        // get the Instrument-Class from the Registered Instruments
        String InstrumentClass = m_Dispatcher.getRegisteredInstruments().get(InstrumentClassName);
        

        // make sure a class is found or return otherwise
        // this prevents some null-pointer exceptions
        // (e.g. for a MAKE command: "MAKE ni NewInstrument; GPIB 7" in myInit)
        if (InstrumentClass == null)
            return;


        // instantiate the class
        try {
            // get the class
            Class theClass = Class.forName(InstrumentClass);

            // ensure the class is derived from class Device
            if ( !Device.class.isAssignableFrom(theClass)) {
                // just return without any action
                return;
            }

            /* get appropriate constructor
             * Remark : this cast generates a compiler warning which is manually
             * suppressed, because the above if-statement ensures that 'theClass'
             * is derived from class Device.
             */
            @SuppressWarnings("unchecked")
            Constructor<Device> c = (Constructor<Device>) theClass.getConstructor();

            // instantiate the device
            DeviceInstance = (Device) c.newInstance();

            // in order to access the generic GPIB Instruments, the Device
            // Instance needs to know it's Instrument-Class-Name, so set it
            DeviceInstance.setInstrumentClassName(InstrumentClassName);


        } catch (Exception ex) {
            // just return without action
            return;
        }


        // get available script commands
        // contains all script methods, also those that do not bare an @AutoGUIAnnotation
        ArrayList<ScriptMethod> ScriptCommands = DeviceInstance.listAllScriptMethods();

        // sort by name
        Collections.sort(ScriptCommands);



        ////////////////////////////
        // populate the Command List

        // clear the Command List before refilling
        m_CommandList.clear();
             
        // add all script methods for the selected Instrument
        for (ScriptMethod met : ScriptCommands) {

            // add all methods that bare a AutoGUIAnnotation
            if (met.AutoGUI) {
                // just add it if the Command List was empty
                m_CommandList.addElement(met);
            }
        }
    }//GEN-LAST:event_jInstrumentNameSelected
    ////</editor-fold>
    
    
    /**
     * Is called when the user selects a different Instrument Command, and
     * fills the Command-Parameter Table with the description of the 
     * parameters the selected command requires (taken from AutoGUIAnnotation)
     * 
     * @param evt is used to determine if this event is the last in a series of
     * of events fired in a single selection
     *
     * @see javax.swing.event.ListSelectionEvent#getValueIsAdjusting()
     */
    // <editor-fold defaultstate="collapsed" desc="Device Command Selected">
    private void jDeviceCommandSelected(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_jDeviceCommandSelected
                    
        // upon selection of an item in a list, this event might be sent
        // multiple times, so only react on this event when the selection
        // is complete
        if ( evt.getValueIsAdjusting() )
            return;

        // make sure an item is selected or return otherwise
        // this prevents null-pointer exceptions
        if (jDeviceCommandList.isSelectionEmpty()) {
            // get the TableModel
            MyTableModel tm = (MyTableModel) m_jTable.getModel();

            // clear the table
            tm.ClearAll();
            
            return;
        }
        
        /* If the user changed a value in the table and selects a different device command
         * before editing is ended, the 'editing focus' remains, and it's not possible
         * to edit an other cell. Therefore, end editing by selecting an other cell
         * (which does not exist) to edit. */
        m_jTable.editCellAt(-1, -1);

        // get the selected script command
        ScriptMethod met = (ScriptMethod) jDeviceCommandList.getSelectedValue();

        // just to be save and prevent null pointer exceptions
        // when no command was selected and this method get's called regardless
        if (met == null)
            return;

        // ensure that the number of ParameterNames specified in the
        // AutoGUIAnnotation equals the number of parameters required by the method
        if (met.ParameterNames.length != met.ParameterTypes.length) {
            String str = "The method '" + met.DeviceCommandName + "' requires ";
            str += met.ParameterTypes.length + " arguments,\nbut only " + met.ParameterNames.length;
            str += " Parameter-Name(s) have been specified in the @AutoGUIAnnotation.\n";
            str += "Please specify all Paramter-Names.";

            // show a dialog
            JOptionPane.showMessageDialog(this.getComponent(), str,
                    "Specify all ParameterNames", JOptionPane.ERROR_MESSAGE, m_iC_Properties.getLogoSmall());

            // exit this method
            return;
        }



        ///////////////////
        // update the Table

        // get the TableModel
        MyTableModel tm = (MyTableModel) m_jTable.getModel();

        // clear the table
        tm.ClearAll();

        // fill the table
        String TT;
        for (int i=0; i < met.ParameterTypes.length ; i++) {
            
            // it's not necessary to specify ToolTips for every parameter
            // so check if TooTips are present
            if (i >= met.ToolTips.length)
                TT = "No ToolTips specified.";
            else
                TT = met.ToolTips[i];

            // check if a default value was specified in the @AutoGUIAnnotation
            String InitValue = "0";
            if (met.ParameterDefaultValues.length >= i+1) {

                // yes, so remember it
                InitValue = met.ParameterDefaultValues[i];

            } else {
                // no, so specify proper initial values
                if (met.ParameterTypes[i] == String.class) InitValue = "";
                if (met.ParameterTypes[i] == Integer.TYPE) InitValue = "0";
                if (met.ParameterTypes[i] == Double.TYPE) InitValue = "0.0";
                if (met.ParameterTypes[i] == Float.TYPE) InitValue = "0.0";
                if (met.ParameterTypes[i] == Boolean.TYPE) InitValue = "false";
            }


            // make a new instance of the proper type
            Object dummy;
            try {
                if (met.ParameterTypes[i] == String.class)
                    dummy = InitValue;
                else if (met.ParameterTypes[i] == Integer.TYPE)
                    dummy = new Integer(InitValue);
                else if (met.ParameterTypes[i] == Double.TYPE)
                    dummy = new Double(InitValue);
                else if (met.ParameterTypes[i] == Float.TYPE)
                    dummy = new Float(InitValue);
                else if (met.ParameterTypes[i] == Boolean.TYPE)
                    dummy = Boolean.valueOf(InitValue);
                else {
                    // default branch. data type not supported
                    String str = "An unexpected error occurred in jDeviceCommandSelected.\n"
                        + "The specified data type is not supported.\n"
                        + "Please consider reporting this incident to the developer, and"
                        + "include the log-files and your script.\n";

                    // show a dialog
                    JOptionPane.showMessageDialog(this.getComponent(), str,
                            "Programming Error", JOptionPane.ERROR_MESSAGE, m_iC_Properties.getLogoSmall());

                    // exit this method
                    return;
                }
            } catch (NumberFormatException ex) {
                String str = "The String specifying the Default Value for '" + met.ParameterNames[i] +"'\n"
                    + ">" + InitValue + "< "
                    + "could not be converted into the appropriate type.\n"
                    + "Please correct the String for this default value.\n";

                // show a dialog
                JOptionPane.showMessageDialog(this.getComponent(), str,
                    "Unrecognized Default Value", JOptionPane.ERROR_MESSAGE, m_iC_Properties.getLogoSmall());

                // exit this method
                return;
            }

            // add to the Table
            tm.addRow(met.ParameterNames[i], dummy, TT);
        }

        // enable the button to add a Script Command from the Auto GUI
        jScriptAddInstrumentCommand.setEnabled(true);
    }//GEN-LAST:event_jDeviceCommandSelected
    //</editor-fold>
    
    /**
     * The user pressed the 'Python' button, so start a new Python Interpreter
     * @param evt 
     */
    // <editor-fold defaultstate="collapsed" desc="Python Button pressed">
    private void Python(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Python
        
        // make a new temporary Device object
        Device dev = new Device();
        
        try {
            // start Python Interpreter by issuing an arbitrary Python command
            dev.execPython("print '\\nPython Interpreter started manually.'");
            
        } catch (ScriptException ex) {
            
            String str = "Error starting the Python Interpreter.\n" 
                    + ex.getMessage();
            
            // show the dialog
            JOptionPane.showMessageDialog(this.getComponent(), str,
                    "Python Error", JOptionPane.ERROR_MESSAGE, m_iC_Properties.getLogoSmall());
        }
    }//GEN-LAST:event_Python

    
    
    /**
     * Called when the User selects 'Show local javadoc' from the Help Menu. It opens
     * the default browser to display the local javadoc if present.
     * @param evt
     */
    // <editor-fold defaultstate="collapsed" desc="show Local Javadoc">
    private void showLocalJavadoc(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showLocalJavadoc
        
        // get Current Working Directory (the directory from which Icontrol.jar was started)
        String WorkingDirectory = System.getProperty("user.dir");
        
        // replace all '\' (path separator under Windows) with '/'
        WorkingDirectory = WorkingDirectory.replace("\\", "/");
        
        // URL to the site
        String url = "file:///" + WorkingDirectory + "/javadoc/index.html";
        
        // replace all spaces with %20 otherwise new UIR() fails
        url = url.replaceAll(" ", "%20");
        
        URI uri;
        try {
            // make URI
            uri = new URI(url);
            
        } catch (URISyntaxException ex) {
            
            // Display a Status Message
            String str = "Error: The path to the local javadoc is not valid:\n"
                    + ex.getMessage() + "\n";
            DisplayStatusMessage(str, false);
            
            // exit method
            return;
        }

        try {          
            // open default browser
            Desktop.getDesktop().browse(uri);

        } catch (Exception ex) {
            DisplayStatusMessage("Error: Could not open the local javadoc.\n"
                               + ex.getMessage()
                               + "Try opening " + url + "\n"
                               + "or visit http://icontrol.java.net/.\n");
        }
    }//GEN-LAST:event_showLocalJavadoc

    
    /**
     * Called when the User selects 'iC Publication' from the Help Menu. It opens
     * the default browser to display the local javadoc if present. 
     * @param evt 
     */
    private void showPublication(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showPublication
        
        // URL to the site
        String url = "http://dx.doi.org/10.6028/jres.117.010";

        try {
            // open default browser
            Desktop.getDesktop().browse(java.net.URI.create(url));

        } catch (Exception ex) {
            DisplayStatusMessage("Error: Could not open the default browser.\n"
                               + "Please visit " + url + "\n");
        }
    }//GEN-LAST:event_showPublication
    //</editor-fold>
   



    /**
     * GENERAL / MAKE:<br>
     * Adds a line to the script list when the left-arrow button
     * in the Make New instrument section is pressed.
     * See <code>Dispatcher</code> for details on the command MAKE.<p>
     *
     * General command form:
     *      MAKE InstrumentName; Instrument_Model; GPIB_Address or 
     *      MAKE InstrumentName; Instrument_Model; RS232 port parameters or 
     *      MAKE InstrumentName; Instrument_Model; URL (IP address of host name for LAN (TCP/IP) communication)
     *
     * @see Dispatcher
     */
    @Action
    // <editor-fold defaultstate="collapsed" desc="Script Add Make">
    public void ScriptAddMake() {

        // build the command string

        // first comes the command
        String cmd = "MAKE ";

        // add the Instrument name
        cmd += jInstrumentName.getText() + Utilities.DELIMITER + "\t";

        // add the Instrument model
        cmd +=  jInstrumentModel.getSelectedItem().toString() + Utilities.DELIMITER + "\t";


        // select which port to add
        String port = (String)jPortSelector.getSelectedItem();

        switch (CommPorts.valueOf(port) ) {
            case none:
                cmd += "none";
                break;
            case GPIB:
                // add the GPIB adress
                cmd += "GPIB=" + jAddress.getText();
                break;
            case RS232:
                // add the RS232 port
                cmd += jRS232params.getText();
                break;
            case LAN:
                // add the URL
                cmd += "URL=" + jAddress.getText();
                break;
            case TMCTL:
                // add the Serial Number
                cmd += "TMCTL: USBTMC(DL9000)=" + jAddress.getText();
                break;
            default:
                // this should never happen. in case it does, just prevent a
                // runtme error which will be detected during syntax check
                cmd += "Wrong Port";
        }


        // add command to the command list
        ScriptAddLine(cmd);
        
        
        // handle the special case of the Eurotherm temperature controller, which
        // needs to set a Modbus address as well.
        if (jInstrumentModel.getSelectedItem().toString().toLowerCase().startsWith("eurotherm")) {
            
            // add a second line which sets the Modbus address
            cmd = jInstrumentName.getText() + " setModbusAddress "
                + jAddress.getText();
            
            // add command to the command list
            ScriptAddLine(cmd);
        }
    }// </editor-fold>


    
    /**
     * Add the command line entered by the user in the GUI
     * to the script list.
     */
    @Action
    // <editor-fold defaultstate="collapsed" desc="Add User command to the Script">
    public void ScriptAddUserLine() {
        
        // get the string entered by the user
        String cmd = jScriptLine.getText();

        // add a space if the line is empty so that an empty line is displayed in the GUI
        if (cmd.isEmpty())
            cmd = " ";


        // add command to the command list
        ScriptAddLine(cmd);
    }//</editor-fold>



    /**
     * Adds the given String to the script list.<br>
     * The string is added to the end of the script if no command line is
     * selected, or adds it below the current selection. Also sets the new
     * selection to the line just added.<p>
     *
     * It also sets the Script-Modified flag to true.<p>
     *
     * Remark: Because tab-characters (\t) did not show up on Windows tab-characters
     * are replaced with space.<br>
     * Remark: Because empty lines are not shown in a JTable, a space is added
     * to empty lines.
     *
     * @param CommandLine This string is added to the command list
     */
    // <editor-fold defaultstate="collapsed" desc="Wrapper to add a command line to the script">
    private void ScriptAddLine(String CommandLine) {

        // because on Windows the Tab character (\t) does not show
        // up in the JList, so \t is replaced with a space
        CommandLine = CommandLine.replaceAll("[\t]", " ");

        // empty lines are not shown in the JList, so a space is added to empty lines
        if (CommandLine.isEmpty())
            CommandLine = " ";

        // get index of current selection
        int index = jScriptList.getSelectedIndex();

        if (index == -1 || index == m_ScriptList.getSize()-1 ) {
            // add entry to the end of the list
            m_ScriptList.addElement(CommandLine);

            // select the last entry
            jScriptList.setSelectedIndex(m_ScriptList.getSize() - 1);

        } else {
            // add entry above the selected line
            m_ScriptList.add(index+1, CommandLine);

            // select the new entry
            jScriptList.setSelectedIndex(index+1);
        }

        // remember that the script has been modified
        setScriptModified(true);

        
        ///////////////////////////////////////////////
        // check if ScriptLine contained a MAKE command
        // if changes are made below, also make the changes in LoadScript
        
        // split line into Tokens
        ArrayList<String> Tokens = Utilities.Tokenizer(CommandLine);


        // check if command is MAKE
        // correctness check is a bit lazy, just so that no runtime error
        // occurs. Any error is detected later when the syntax check is performed
        // upon pressing start
        if (    Tokens != null &&
                Tokens.size() >= 3 &&
                Tokens.get(0).equalsIgnoreCase("MAKE") ) {

            // and remember this Instrument
            // Instrument-Name / Instrument-Class-Name (Tsample / Lakeshore 340)
            m_ScriptInstruments.put(Tokens.get(1), Tokens.get(2));

            // and also show it to the user (sorted alphabetically)
            if (m_InstrumentList.isEmpty()) {

                // just add it if the Instrument List was empty
                m_InstrumentList.addElement( Tokens.get(1) );
            } else {

                // add it sorted if the Instrument List was not empty
                int i;
                for (i=0; i<m_InstrumentList.getSize(); i++) {
                    // get current method name
                    String dummy = (String)m_InstrumentList.getElementAt(i);

                    // compare it lexiographically to the one to be inserted
                    if (dummy.compareToIgnoreCase(Tokens.get(1)) > 0) {
                        // Index (i) found, so don't look further
                        break;
                    }
                }

                // add it sorted
                m_InstrumentList.add(i, Tokens.get(1));
            }
        }


    }//</editor-fold>


    /**
     * This action starts the processing of the script in a new
     * Dispatcher thread.<p>
     *
     * This method also checks if the Project Directory specified in the GUI exists,
     * and ask the user to create the directory in case it doesn't.<p>
     *
     * A message is shown to the user if the files that start with the File Name
     * specified in the GUI already exist. The user can then select to overwrite
     * the files or cancel Start processing the script.<p>
     *
     * When logging GPIB traffic is enabled, a buffered file-writer is opened
     * in 'User-home/iC log' with the current date and time to save all GPIB I/O.
     *
     * @see Dispatcher
     */
    @Action
    // <editor-fold defaultstate="collapsed" desc="Start Scripting">
    public void Start() {

        // reformat the current date and time
        m_StartTime = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd HH:mm:ss");
        String TimeString = sdf.format(new Date(m_StartTime));


        // discard previous Status messages
        try {
            m_StatusTextDoc.remove(0, m_StatusTextDoc.getLength());
        } catch (BadLocationException ex) {
            DisplayStatusMessage("Error: Could not remove previous Status Messages.\n");
        }

        // display a start message on the Status display
        DisplayStatusMessage("iC started at " + TimeString + "\n\n", false);


        ///////////////////////
        // check if path exists
        // aks the user to create it if it does not exists
        File path = new File(jProjectDir.getText());
        if ( !path.exists() ) {

            // path does not exist so ask user if it should be created
            // show a dialog to ask the user what to do
            String str = "The Project Directory does not exists.\n";
            str += "Do you want to create the directory?";

            Object[] ButtonLabels = { "Create Directory", "Cancel Start" };
            Integer ret = JOptionPane.showOptionDialog(this.getComponent(), str,
                                "Directory does not exist", JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE, m_iC_Properties.getLogoSmall(),
                                ButtonLabels, ButtonLabels[0]);

            // check the User's answer
            if ( ret == 0) {
                // the user choose to create the directory
                boolean success;
                try {
                    success = path.mkdirs();
                } catch (SecurityException ex) {
                    // creating the directories obviously failed ...
                    success = false;
                }

                // creating the directory failed, so show a message
                if ( !success ) {
                    str = "Could not create the Project Directory.\n";
                    str += "Check the spelling, ensure that you have proper permissions, \n";
                    str += "or create the path manually and try again.\n";

                    JOptionPane.showMessageDialog(this.getComponent(), str,
                                "Directory could not be created", JOptionPane.ERROR_MESSAGE, m_iC_Properties.getLogoSmall());
                    return;
                }     
            } else {
                // don't start, just exit
                return;
            }
        }
        
        // update Project Directory in the User's properties
        m_iC_Properties.setString("iC.ProjectDir", jProjectDir.getText());

        
        ///////////////////////
        // check if file exists
        // and ask user if it is okay to proceed (and potentially overwrite the files)

        // make a filter for files that start with the FileName specified in the GUI
        FilenameFilter filter = new FilenameFilter(){
            public boolean accept(File dir, String name) {
                return name.toLowerCase().startsWith(jFileName.getText().toLowerCase());
            }
        };

        // get directory listing
        String[] AllFiles = path.list(filter);

        // if the received list is not empty, some files exist,
        // so ask the user what to do if the FileName is not 'SeqTest"
        if ( AllFiles.length != 0 &&
             !jFileName.getText().contains("SeqTest") ) {
            String str = "Files with the chosen File Name already exist in the Project Directory.\n"
                + "It is recommended to change the File Name or copy the old files to a safe location.\n"
                + "If you continue processing the script, a counter will be added to new File names,\n"
                + "hence, no files should be overwritten.\n\n"
                + "Pay attention, it's your data !\n\n"
                + "Do you want to continue?";

            Object[] ButtonLabels = { "Continue and overwrite files", "Cancel Start" };
            Integer ret = JOptionPane.showOptionDialog(this.getComponent(), str,
                                "Directory does not exist", JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE, m_iC_Properties.getLogoSmall(),
                                ButtonLabels, ButtonLabels[1]);

            // check the User's answer
            if ( ret == 1) {
                // the user choose to cancel, so just return
                return;
            }
        }
        
        // update File Name in the User's properties
        m_iC_Properties.setString("iC.FileName", jFileName.getText());
        
        // log file name
        m_Logger.log(Level.CONFIG, "Base File Name: {0}\n", jFileName.getText());


        // get the name of the default GPIB controller from iC.properties
        String GPIBcontroller = m_iC_Properties.getString("iC.DefaultGPIBcontroller", "");
        
        // pass the GPIB controller to the Device class
        if (jRB_GPIB_Prologix.isSelected()) {
            Device.setGPIBcontroller(Device.GPIBcontroller.Prologix);
            
            // store as default GPIB controller
            if ( !GPIBcontroller.equalsIgnoreCase("Prologix") ) {
                m_iC_Properties.setString("iC.DefaultGPIBcontroller", "Prologix");
            }
            
            // log selection
            m_Logger.config("Prologix GPIB controller is selected\n");
        
        } else if (jRB_GPIB_IOtech.isSelected()) {
            Device.setGPIBcontroller(Device.GPIBcontroller.IOtech);
            
            // store as default GPIB controller
            if ( !GPIBcontroller.equalsIgnoreCase("IOtech") ) {
                m_iC_Properties.setString("iC.DefaultGPIBcontroller", "IOtech");
            }
            
            // log selection
            m_Logger.config("IOtech GPIB controller is selected\n");
            
        } else {
            // this is the default selection
            Device.setGPIBcontroller(Device.GPIBcontroller.NI);
            
            // store as default GPIB controller
            if ( !GPIBcontroller.equalsIgnoreCase("NI") ) {
                m_iC_Properties.setString("iC.DefaultGPIBcontroller", "NI");
            }
            
            // log selection
            m_Logger.config("NI GPIB controller is selected\n");
        }
        



        ///////////////////////////////////
        // copy the list of command strings
        // into a more convenient ArrayList

        // make a new ArrayList of appropriate size
        ArrayList<String> CommandList = new ArrayList<String>( m_ScriptList.getSize() );


        // copy element by element
        for (int i=0; i < m_ScriptList.getSize(); i++) {
            CommandList.add( m_ScriptList.get(i).toString() );
        }


        ////////////
        // GUI stuff

        // disable start button
        jStart.setEnabled(false);

        // enable the stop and puse buttons
        jStop.setEnabled(true);
        jPause.setEnabled(true);

        // enable SendCommand Now button
        jSendNow.setEnabled(true);


        // reset Pause flag
        m_PauseScripting = false;

        // reset Stop flag
        m_StopScripting = false;



        ///////////////////
        // Dispatcher stuff

        // pass the ArrayList to the Dispatcher so it knows the script
        m_Dispatcher.setCommandList(CommandList);

        // set the appropriate simulation mode of all instruments
        m_Dispatcher.setSimulationModeGlobal(jNoCommunicationMode.getState());



        // ensure that processing the script is not paused
        m_PauseScripting = false;



        // prepare a new thread which processes the script by
        // dispatching the command lines
        // for some funny reason I have to make a new thread after it
        // ran once or else I get an InvokationTargetException ...
        m_DispachingThread = new Thread( m_Dispatcher );


        // enable SyntaxCheckMode
        m_Dispatcher.setSyntaxCheckMode(true);

        // Run through the script in setSyntaxCheckMode first. T This is done by
        // manually calling run(), hence this is not in a separate thread.
        // Why not start the thread? Becasue this method would continue before 
        // SyntaxCheck finished, hence the Dispatcher.run would need to tell the
        // GUI to continue with the real run
        m_Dispatcher.run();

        // if no error occurred in SyntaxCheckMode, do the real run
        if (!m_Dispatcher.getErrorFlag()) {
            
            // spin the busy-wheel in the GUI
            busyIconTimer.start();

            // remind the user if Auto-Pausing of the script is enabled
            if (jAutoPauseScript.getState()) {
                String str = "Auto-Pausing of the script is enabled.\n";
                str += "Processing of the script will be paused after each line.\n";
                str += "Press 'Continue' to continue";
                JOptionPane.showMessageDialog(this.getComponent(), str,
                        "Auto-Pause Scripting", JOptionPane.INFORMATION_MESSAGE, m_iC_Properties.getLogoSmall());
            }

            // disable SyntaxCheckMode
            m_Dispatcher.setSyntaxCheckMode(false);

            // start the thread, hence, processing the script
            m_DispachingThread.start();
        }
        
    }// </editor-fold>



    
    /**
     * This method is called by the Dispatcher after the script has been
     * processed. Used to clean up the GUI (re-enable start/stop) buttons,
     * and show the user a message.
     *
     * @param Erroroccurred is true when an error occurred during processing the
     * script, and is used to suppress the message to the user
     */
    // <editor-fold defaultstate="collapsed" desc="Done scripting">
    @Override
    public void DoneScripting(boolean Erroroccurred) {


        // stop spining the busy-wheel in the GUI
        busyIconTimer.stop();
        statusAnimationLabel.setIcon(idleIcon);

        // enable start button
        jStart.setEnabled(true);

        // disable the stop and puse buttons
        jStop.setEnabled(false);
        jPause.setEnabled(false);

        // disable SendCommand Now Button
        jSendNow.setEnabled(false);

        // calc elapsed time
        long dT = System.currentTimeMillis() - m_StartTime;
        int h = (int)Math.floor(dT/3600000);
        int m = (int)Math.floor( (dT - 3600000*h) / 60000 );
        int s = Math.round( (dT - 3600000*h - 60000*m) / 1000 );
        String str = String.format("Done processing the script.\n"
                + "Elapsed time: %dh:%dm:%ds\n", h, m, s);

        // display in Status Text
        DisplayStatusMessage(str);

        // display command line which will be / is being processed
        DisplayStatusLine("Done.", true);
        
        // show that the script has been processed
        if (!Erroroccurred) {

            // display a Popup window
            str = "Done processing the script.";
            JOptionPane.showMessageDialog(this.getComponent(), str,
                        "Script processed", JOptionPane.INFORMATION_MESSAGE, m_iC_Properties.getLogoSmall());
        }
    }//</editor-fold>


    
    /**
     * Adds the given String to the Status text Area with a time stamp. See
     * {@link #DisplayStatusMessage(String, boolean)} for details.
     *
     * Remark: Maybe this method will be made non-static again. (replace m_GUI first)
     *
     * @param StatusMessage - the String to display
     */
    // <editor-fold defaultstate="collapsed" desc="Display a status message">
    public void DisplayStatusMessage(String StatusMessage) {

        // call the overloaded method with the default value
        DisplayStatusMessage(StatusMessage, true);
    }//</editor-fold>

    
    /**
     * Adds the given String to the Status Text Area in the GUI. It allows to
     * specify if a time stamp should preceed the given String. This method is
     * called by the overloaded method of <code>DisplayStatusMessage</code> that
     * takes only one parameter and provides a default value for 
     * <code>AddTimeStamp</code>. If <code>StatusMessage</code> contains the
     * String "Warning" or "Error" (case insensitive) then the font color is
     * changed to highlight this error. All messages displayed are also logged.
     *
     * @param StatusMessage - the String to display
     *
     * @param AddTimeStamp it <code>true</code> a time stamp is added in front
     * of the the String to display.
     */
    // <editor-fold defaultstate="collapsed" desc="Display a status message + TimeStamp">
    public void DisplayStatusMessage(String StatusMessage, boolean AddTimeStamp) {

        // reformat the current date and time
        SimpleDateFormat sdf = new SimpleDateFormat("HH.mm.ss");
        String DateString = sdf.format(Calendar.getInstance().getTime());

        // add the time stamp
        if (AddTimeStamp)
            StatusMessage = DateString + ": " + StatusMessage;

        // log message
        m_Logger.log(Level.FINE, StatusMessage);


        // get the end position
        Position end = m_StatusTextDoc.getEndPosition();

        // choose the Style
        AttributeSet attr = m_StatusTextStyles.getStyle("DefaultStyle");

        if (StatusMessage.toLowerCase().contains("warning"))
            attr = m_StatusTextStyles.getStyle("WarningStyle");

        if (StatusMessage.toLowerCase().contains("error"))
            attr = m_StatusTextStyles.getStyle("ErrorStyle");
        

        try {
            // append the Status Message at the end
            m_StatusTextDoc.insertString(end.getOffset(), StatusMessage, attr);

        } catch (BadLocationException ex) {
            // should actually never occur, but well ...

            String str = "Could not append Status Message (BadLocationException).\n";
            str += "Please report this case to the developer.";

            // log event
            m_Logger.severe(str);

            // display a message
            JOptionPane.showMessageDialog(null, str,
                    "Bad Location Exception", JOptionPane.ERROR_MESSAGE, null);
        }

        // move cursor to the line just added so that the last Status Message is visible
        // could also be m_StatusTextDoc.getLength()
        jStatusText1.setCaretPosition(end.getOffset()-1);

    }//</editor-fold>

    /**
     * Displays a text in the Status bar. If selected, the text fades out after
     * a brief moment as defined in the constructor.
     *
     * @param StatusText The text to display
     * @param FadeOut If <code>true</code> the timer is started after which the
     * text fades out.
     * @return The text previously shown in the Status bar
     */
    // <editor-fold defaultstate="collapsed" desc="Display Status Line">
    @Override
    public String DisplayStatusLine (String StatusText, boolean FadeOut) {
        
        String ret = statusMessageLabel.getText();
        
        statusMessageLabel.setText((StatusText == null) ? "" : StatusText);

        if (FadeOut) {
            messageTimer.restart();
        }
        
        return ret;
    }//</editor-fold>

    /**
     * Asks the user to specify a script file and loads it. If the current script
     * is not empty, the user is asked to either discard the current script and
     * load the new script, or append the new script below the current selection.
     * Updates the Script-Modified flag
     */
    // <editor-fold defaultstate="collapsed" desc="Load Script">
    @Action
    public void FileLoad() {

        // delete the current script (user is asked to save script is modified)
        FileNew();

        // load the script
        try {
            LoadScript("Load Script", "");
        } catch (FileNotFoundException ex) {
            
            String str = "The file could not be found. The returned error is:\n"
                    + ex.getMessage();
            
            // show a dialog
            JOptionPane.showMessageDialog(this.getComponent(), str,
                    "File Load Error", JOptionPane.ERROR_MESSAGE, m_iC_Properties.getLogoSmall());
        }

        // Display a status message
        DisplayStatusMessage("Script loaded.\n");
    }//</editor-fold>

    /**
     * Asks the user to specify a script file and appends it to the current
     * script. This method is called when the user selects the menu entry
     * File/Append.
     */
    // <editor-fold defaultstate="collapsed" desc="FileAppend">
    @Action
    public void FileAppend() {

        // add a comment line
        ScriptAddLine("// A Script was appended here");
        
        // append to the script
        try {
            LoadScript("Append Script", "");
        } catch (FileNotFoundException ex) {
            
            String str = "The file could not be found. The returned error is:\n"
                    + ex.getMessage();
            
            // show a dialog
            JOptionPane.showMessageDialog(this.getComponent(), str,
                    "File Load Error", JOptionPane.ERROR_MESSAGE, m_iC_Properties.getLogoSmall());
        }

        // Display a status message
        DisplayStatusMessage("Script has been appended.\n");
    }//</editor-fold>

    
    /**
     * This is a helper function for <code>FileLoad</code> and <code>FileAppend</code>.
     * It opens a dialog to let the user choose a script file which is then 
     * appended to the script at the current position. If the script was empty,
     * this is equivalent to loading a new script.<p>
     *
     * If the optional argument <code>FileName</code> is specified, no dialog is
     * presented to the user and it is interpreted as the file name of the script
     * to load. This is mainly used for testing/debugging in <code>myInit</code>.<p>
     * 
     * If the file was successfully read, it stores it's name in iC_User.properties
     * so that upon a new start of iC, the same file is loaded. For now, this 
     * feature is enabled only when iC.Debug=1. Upon saving a script, iC.LastLoaded
     * is also updated.
     * 
     * @param Title This String will be displayed in the title bar of the dialog
     *
     * @param FileName If <code>FileName</code> is specified it is interpreted
     * as the file name of the script to load. An empty string causes a dialog
     * to be presented to the user asking to select a script file.
     * 
     * @throws FileNotFoundException when the file could not be found
     */
    // <editor-fold defaultstate="collapsed" desc="LoadScript">
    private void LoadScript(String Title, String FileName) 
            throws FileNotFoundException {
        
        // local variables
        File file = null;
        String line;

        // make a new FileChooser object
        JFileChooser fc = new JFileChooser();

        // allow to select files only
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);

        // set a nice title
        fc.setDialogTitle(Title);


        // set choosable file extension filters
        // all files is added automatically
        fc.addChoosableFileFilter(new FileNameExtensionFilter("Script file (*.iC)", "iC"));
        fc.addChoosableFileFilter(new FileNameExtensionFilter("Text file (*.txt)", "txt"));


        // set default filter
        fc.setFileFilter(fc.getChoosableFileFilters()[1]);

        // set current directory of file chooser
        fc.setCurrentDirectory(new File(jProjectDir.getText()));


        // show the dialog if no FileName has been specified
        if (FileName.isEmpty()) {
            
            int ret = fc.showOpenDialog(this.getFrame());

            
            if ( ret == JFileChooser.APPROVE_OPTION) {
                
                // get the path and show it in the GUI
                file = fc.getSelectedFile();
                
                // remember the last file loaded in iC_User.properties
                m_iC_Properties.setString("iC.LastLoaded", file.getPath());
                
            } else {
                // User canceled the dialog, so exit
                return;
            }
        } else {
            // Auto-Load behavior has been changed so that the full path to the
            // script last loaded is not saved in the iC_User.properties
            
//            // get current directory
//            String Path = jProjectDir.getText();
//            
//            // add file separator if necessary
//            if ( !Path.endsWith( System.getProperty("file.separator") ) ) {
//                Path += System.getProperty("file.separator");
//            }
//            
//            // append File Name
//            FileName =  Path + FileName;

            // make a new file
            file = new File(FileName);
        }


        // make a buffered file reader
        // this can throw a FileNotFoundException
        BufferedReader br = new BufferedReader(new FileReader(file));


        // The buffered reader should never be null, but check anyways
        if (br == null) {
            JOptionPane.showMessageDialog(this.getComponent(), "Could not open file.",
                    "File Load Error", JOptionPane.ERROR_MESSAGE, m_iC_Properties.getLogoSmall());
            return;
        }


        try {
            // read line by line
            while ((line = br.readLine()) != null) {
                // and add it to the script
                ScriptAddLine(line);
                
                //////////////////////////////////////
                // check if line is an INCLUDE command
                // to properly handle the making of
                // new instruments in a sub-script
                
                // split the Command Line into it's Tokens
                ArrayList<String> Tokens = Utilities.Tokenizer( line );
                
                // is it a INCLUDE command?
                if(Tokens.get(0).equalsIgnoreCase("INCLUDE")) {
                    
                    // check if it's a Python script to include
                    if (Tokens.get(1).toLowerCase().endsWith(".py")) {
                        // nothing to do in case of a Python script
                        // AND, do not execute the Python script when loading
                        continue;
                    }
                    
                    
                    // temporary command list
                    ArrayList<String> CommandList = new ArrayList<String>();
                    
                    // remove first Token
                    Tokens.remove(0);
                    
                    try {
                        // build a dummy CommandList that can be parsed for MAKE commands
                        m_Dispatcher.HandleIncludeCommand(CommandList, -1, Tokens);//must be -1 because of Index++
                        
                    } catch (ScriptException ex) {
                        String str = "Could not parse the sub-script to be included in line\n"
                                + line + "\n\n" 
                                + "Reason:\n" + ex.getMessage() + "\n"
                                + "Try to locate and fix the sub-script before you include it here.\n"
                                + "Loading the rest of the script will be cancelled.\n";
                        m_Logger.severe(str);
                        
                        JOptionPane.showMessageDialog(this.getComponent(), str,
                        "Error loading script file", JOptionPane.ERROR_MESSAGE, m_iC_Properties.getLogoSmall());
                        
                        return;
                    }
                    
                    // iterate through all lines to find any MAKE commands
                    for (String dummyline : CommandList) {
                        // if changes are made below, also make the changes in ScriptAddLine

                        // split line into Tokens
                        Tokens = Utilities.Tokenizer(dummyline);

                        // check if command is MAKE
                        // correctness check is a bit lazy, just so that no runtime error
                        // occurs. Any error is detected later when the syntax check is performed
                        // upon pressing start
                        if (    Tokens != null &&
                                Tokens.size() >= 3 &&
                                Tokens.get(0).equalsIgnoreCase("MAKE") ) {

                            // and remember this Instrument
                            // Instrument-Name / Instrument-Class-Name (Tsample / Lakeshore 340)
                            m_ScriptInstruments.put(Tokens.get(1), Tokens.get(2));

                            // and also show it to the user (sorted alphabetically)
                            if (m_InstrumentList.isEmpty()) {

                                // just add it if the Instrument List was empty
                                m_InstrumentList.addElement( Tokens.get(1) );
                            } else {

                                // add it sorted if the Instrument List was not empty
                                int i;
                                for (i=0; i<m_InstrumentList.getSize(); i++) {
                                    // get current method name
                                    String dummy = (String)m_InstrumentList.getElementAt(i);

                                    // compare it lexiographically to the one to be inserted
                                    if (dummy.compareToIgnoreCase(Tokens.get(1)) > 0) {
                                        // Index (i) found, so don't look further
                                        break;
                                    }
                                }

                                // add it sorted
                                m_InstrumentList.add(i, Tokens.get(1));
                            }
                        }
                    }
                }
            }

        } catch (IOException ex) {
            m_Logger.log(Level.SEVERE, "IcontrolView.LoadScript: read from BufferedReader", ex);

            JOptionPane.showMessageDialog(this.getComponent(), "Could not read from the file",
                    "File Load Error", JOptionPane.ERROR_MESSAGE, m_iC_Properties.getLogoSmall());
            
            // close the file
            try { if (br!=null) br.close(); } catch (IOException ignore) {}
            
            return;
        }
        

        // close the file
        if ( br != null ) {
            try { 
                br.close(); 
            } catch (IOException ex) {
                DisplayStatusMessage("Could not close the file");
            }    
        }
        
        // reset Script-Modified-Flag
        setScriptModified(false);
    }//</editor-fold>


    /**
     * Asks the user to specify a file location and saves the current
     * script as a script file (which is a simple text file containing
     * all command lines). Updates the Script-Modified flag. Also saves the
     * chosen FileName in iC_User.properties so that this script is loaded
     * next time iC is started (currently this is done only when iC.Debug=1)
     */
    // <editor-fold defaultstate="collapsed" desc="Save Script">
    @Action
    public void SaveScript() {

        // local variables
        File file = null;

        // make a new FileChooser object
        JFileChooser fc = new JFileChooser();

        // allow selection of files only (no directories)
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);

        // select a Svae Dialog
        fc.setDialogType(JFileChooser.SAVE_DIALOG);

        // set a nice title
        fc.setDialogTitle("Save Script As");

        // set choosable file extension filters
        // all files is added automatically
        fc.addChoosableFileFilter(new FileNameExtensionFilter("Script file (*.iC)", "iC"));
        fc.addChoosableFileFilter(new FileNameExtensionFilter("Text file (*.txt)", "txt"));

        // set default filter
        fc.setFileFilter(fc.getChoosableFileFilters()[1]);

        // set current directory of file chooser
        fc.setCurrentDirectory(new File(jProjectDir.getText()));

        // add a checkbox to the SaveScript dialog that asks if the
        // selected extension should be automatically added if it's not already
        // specified by the user
        //JCheckBox cb= new JCheckBox("Automatically append extention");
        //cb.setSelected(true);
        //fc.setAccessory(cb);

        // show the dialog and wait for the user to select something
        int ret = fc.showSaveDialog(this.getFrame());

        // open the file for writing if the user selected okay
        if ( ret == JFileChooser.APPROVE_OPTION) {

            // get the selected FileFilter
            FileNameExtensionFilter ff = (FileNameExtensionFilter) fc.getFileFilter();

            // get the selected Extension
            // when multiple extensions are allowed by this filter (e.g. jpg, jpeg)
            // use the first one specified
            String ext = ff.getExtensions()[0];

            // get the selected file (a FILE object is immutable)
            file = fc.getSelectedFile();
            
            // check if an extension is present
            if ( !file.getName().contains(".") ) {
                // no '.' is present in the filename, so append the default extension
                file = new File(file.getPath() + "." + ext);
            }
        } else {
            // User canceled the dialog, so exit
            return;
        }




        // check if file exists already
        if ( file.exists() ) {
            // show a dialog
            String str = "The file already exists. Do you want to overwrite it?";
            int dummy = JOptionPane.showConfirmDialog(this.getComponent(), str,
                        "File Exists", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, m_iC_Properties.getLogoSmall());

            // return if user selected to cancel saving the script
            if (dummy == 1)
                return;
        }
        
        // make a file writer
        BufferedWriter filewriter = null;
        try {
            filewriter = new BufferedWriter( new FileWriter(file) );

        } catch (IOException ex) {
            String str = "The file could not be created or opened.\n";
            str += "Try again or something else.";

            // show a dialog
            JOptionPane.showMessageDialog(this.getComponent(), str,
                    "File Save Error", JOptionPane.ERROR_MESSAGE, m_iC_Properties.getLogoSmall());

            // exit
            return;
        }

        // The FileWriter should never be null, but check anyways
        if (filewriter == null) {
            JOptionPane.showMessageDialog(this.getComponent(), "Could not open file.",
                    "File Save Error", JOptionPane.ERROR_MESSAGE, m_iC_Properties.getLogoSmall());
            return;
        }

        // now write line by line
        for (int i=0; i<m_ScriptList.getSize(); i++) {
            try {
                // write the next line
                filewriter.write((String) m_ScriptList.get(i));

                // write a newline
                filewriter.newLine();

            } catch (IOException ex) {
                String str = "Could not write into the file. Aborting.";
                JOptionPane.showMessageDialog(this.getComponent(), str,
                    "File Save Error", JOptionPane.ERROR_MESSAGE, m_iC_Properties.getLogoSmall());
                return;
            }
        }
        
        // save filename to load this script next time
        m_iC_Properties.setString("iC.LastLoaded", file.getPath());
        


        // close the file
        try {
            filewriter.close();

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this.getComponent(), "Could not close the file",
                    "File Save Error", JOptionPane.ERROR_MESSAGE, m_iC_Properties.getLogoSmall());
            return;
        }

        // Display a status message
        DisplayStatusMessage("Script saved.\n");

        // reset Script-Modified-Flag
        setScriptModified(false);
    }//</editor-fold>

        
    /**
     * Deletes the current script and updates the Script-Modified flag. Called
     * when the User selects "New" from the "File" Menu. A dialog is presented
     * if the script is modified since it was last saved.
     */
    // <editor-fold defaultstate="collapsed" desc="File New">
    @Action
    public void FileNew() {

        // check if the current script has been modified
        if (m_ScriptModified) {

            // show a dialog
            String str = "The script has been modified.\n";
            str += "Do you want to save the changes?";

            Object[] ButtonLabels = { "Save changes", "Discard changes", "Cancel" };
            Integer ret = JOptionPane.showOptionDialog(this.getComponent(), str,
                                "Script is modified", JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE, m_iC_Properties.getLogoSmall(),
                                ButtonLabels, ButtonLabels[0]);



            // check the User's answer
            if ( ret == 0) {
                // the user chose to save the changes
                SaveScript();
            } else if (ret == 2) {
                // user pressed cancel, so return
                return;
            }
        }

        // clear the Script
        m_ScriptList.clear();

        // set Script-Modified-Flag to false
        setScriptModified(false);

        // clear the CommandList
        m_CommandList.clear();

        // clear the InstrumentList
        m_InstrumentList.clear();

    }//</editor-fold>


    
    /**
     * Allows the user to select a Project Directory. There is a bug that disables
     * the 'Create new directory' button on certain directories on WinXP and
     * apparently also on MacOS: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4847375
     */
    // <editor-fold defaultstate="collapsed" desc="Choose Project Directory">
    @Action
    public void ChooseProjectDir() {

        //UIManager.put("FileChooser.readOnly", Boolean.FALSE);
        
        // make a new FileChooser object
        JFileChooser fc = new JFileChooser();
 
        // allow to select directories only
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        // set a nice title
        fc.setDialogTitle("Select Project Directory");

        // set current directory of file chooser
        fc.setCurrentDirectory(new File(jProjectDir.getText()));


        // show the dialog
        int ret = fc.showOpenDialog(this.getFrame());

        // get the path and show it in the GUI
        if ( ret == JFileChooser.APPROVE_OPTION) {
            jProjectDir.setText( fc.getSelectedFile().getPath());
        }
    }//</editor-fold>


    /**
     * Allows the user to select a script file which can then be included
     * as a sub-script in a script.<p>
     *
     * When the selected script is in the default include directory specified
     * in <code>m_DefaultIncludePath</code>, then only the filename without
     * the path is shown. Successive code will check if the filename includes
     * the System's file-separator, and if not, the default directory will
     * be appended automatically. This way the most common sub-scripts don't
     * get cluttered up with long path names.
     */
    // <editor-fold defaultstate="collapsed" desc="Select Script To Include">
    @Action
    public void SelectScriptToInclude() {

        // get the file separator character
        String separator = System.getProperty("file.separator");
        
        // get the default include directory for sub-scripts
        String Default_iCPath = m_iC_Properties.getPath("iC.DefaultPath", "$user.home$/iC");

        // make a new FileChooser object
        JFileChooser fc = new JFileChooser();

        // allow to select files only
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);

        // set a nice title
        fc.setDialogTitle("Select Script to include");

        // set current directory of file chooser
        if (jScriptFileToInclude.getText().isEmpty()) {

            // start in the default include directory
            fc.setCurrentDirectory(new File(Default_iCPath));

        } else {    // start in the last used directory

            // get the current file
            String dummy = jScriptFileToInclude.getText();

            // append the default directory if necessary
            if ( !dummy.contains(separator) ) {
                dummy = Default_iCPath + separator + dummy;
            }

            fc.setCurrentDirectory(new File(dummy));
        }

        // TODO 2* add FileFilters


        // show the dialog
        int ret = fc.showOpenDialog(this.getFrame());

        // get the file and show it in the GUI
        if ( ret == JFileChooser.APPROVE_OPTION) {
            // get the current directory
            String dir = fc.getCurrentDirectory().toString();

            // get the selected file path
            //String path = fc.getSelectedFile().getName();

            // check if it is the default directory
            String dummy = "";
            if (dir.equals(Default_iCPath)) {

                // yes, so only display the file name without the path
                dummy = fc.getSelectedFile().getName();
            } else {
                
                // no, so display the full filename including path
                dummy = fc.getSelectedFile().getPath();
            }

            // set the text in the GUI
            jScriptFileToInclude.setText( dummy );
        }
    }//</editor-fold>


    /**
     * The User selected an Instrument Command, has hopefully supplied
     * reasonable values in the Table and wants to add this command
     * to the script list. This method adds the corresponding string
     * to the script list.
     */
    // <editor-fold defaultstate="collapsed" desc="Add a line to the script">
    @Action
    public void ScriptAddInstrumentCommand() {

        /* Any changes to this method should be also made in SendNow()
         * because the code below is copied there. */

        String str;

        // get the selected Instrument Name
        str = (String) jInstrumentList.getSelectedValue();
        str += " ";

        // get the selected script command
        ScriptMethod met = (ScriptMethod) jDeviceCommandList.getSelectedValue();
            
        // get the name of the selected instrument command
        str += met.DeviceCommandName;


        /* If the user changed a value in the table and adds the line to the script
         * before editing is ended, the newly entered values are not recognized. Therefore,
         * end editing by selecting an other cell (which does not exist) to edit.
         */
        m_jTable.editCellAt(-1, -1);

        // get the TableModel
        MyTableModel tm = (MyTableModel)m_jTable.getModel();

        // attach the parameters
        for (int i=0; i<tm.getRowCount(); i++) {
            // add a delimiter (;) if it's not the first Argument
            str += i>0 ? Utilities.DELIMITER + " " : " ";

            str += tm.getValueAt(i, 1).toString();   
        }

        // add it to the script list
        ScriptAddLine(str);
    }//</editor-fold>


    /**
     * This method is called when the user presses the Pause/Continue button.
     * It toggles the Pause Status Flag and changes the text of the
     * button between "Pause" and "Continue".
     */
    // <editor-fold defaultstate="collapsed" desc="Pause Button">
    @Action
    public void Pause() {

        if (m_PauseScripting == true) {
            // was in Pause mode, so change to go
            m_PauseScripting = false;

            // set GUI text
            jPause.setText("Pause");

        } else {
            // was in go mode, so pause it
            m_PauseScripting = true;

            // set GUI text
            jPause.setText("Continue");

            // show a message to the user (especially useful if Paused from the script)
            // only if not in Auto-Pause mode
            if ( !isAutoPaused() ) {
                String str = "Scripting is paused. Press \"Continue\" to continue\n";
                //JOptionPane.showMessageDialog(this.getComponent(), str,
                //        "Scripting is paused", JOptionPane.INFORMATION_MESSAGE, m_iC_Properties.getLogoSmall());
                DisplayStatusMessage(str);
            }
        }
    }//</editor-fold>

    /**
     * Called when the user presses the Stop button.
     * It sets the <code>m_Stop</code> flag, which is used by the instruments
     * to terminate any threads (for instance in MonitorTemp).
     *
     * Also changes the state of some buttons in the GUI.
     *
     * @see icontrol.drivers.instruments.iC_Instrument#MonitorChart(float, String, String, String, String, String, String, String) 
     */
    // <editor-fold defaultstate="collapsed" desc="Stop Button">
    @Action
    public void Stop() {

        // stop the dispatcher and the Instruments
        m_Dispatcher.StopScripting();

        // remember that stop was pressed so to end isPaused()
        m_StopScripting = true;

    }//</editor-fold>


    /**
     * This method is called when the user selects the left arrow to include
     * a sub-script in the script. It adds a new command line to the script.
     */
    // <editor-fold defaultstate="collapsed" desc="Script Add Sub Script">
    @Action
    public void ScriptAddSubScript() {

        // build the command line
        String cmd = "INCLUDE ";

        // get the string entered by the user
        cmd += jScriptFileToInclude.getText();

        // add command to the command list
        ScriptAddLine(cmd);
    }
    //</editor-fold>



    /**
     * Called when the User selects to send the Command in the Table directly and
     * instantly to the Instrument.
     */
    // <editor-fold defaultstate="collapsed" desc="SendNow">
    @Action
    public void SendNow() {

        /* Any changes to the code generating the command line should also be made
         * made in ScriptAddInstrumentCommand() because part of the code below is
         * copied from there. */

        // get the selected Instrument Name
        String Command = (String) jInstrumentList.getSelectedValue();
        Command += " ";

        // get the name of the selected instrument command
        ScriptMethod met = (ScriptMethod) jDeviceCommandList.getSelectedValue();
        Command += met.DeviceCommandName;

        /* If the user changed a value in the table and adds the line to the script
         * before editing is ended, the newly entered values are not recognized. Therefore,
         * end editing by selecting an other cell (which does not exist) to edit.
         */
        m_jTable.editCellAt(-1, -1);

        // get the TableModel
        MyTableModel tm = (MyTableModel)m_jTable.getModel();

        // attach the parameters
        for (int i=0; i<tm.getRowCount(); i++) {
            Command += i>0 ? Utilities.DELIMITER + " " : " ";
            Command += tm.getValueAt(i, 1).toString();

        }

        ///////////////////////
        // dispatch the command

        // make a new Device object
        Device dev = new Device();

        try {
            // dispatch the command
            dev.DispatchCommand(Command);
        } catch (ScriptException ex) {

            // get the message of the exception
            String str = ex.getMessage();

            // append command line
            str += "The processed command was:\n";
            str += Command;

            // log event
            m_Logger.severe(str);

            // show the dialog
            JOptionPane.showMessageDialog(this.getComponent(), str,
                    "Command Error", JOptionPane.ERROR_MESSAGE, m_iC_Properties.getLogoSmall());
        }

        // show to the user (also loggs the event)
        DisplayStatusMessage("Just sent: " + Command +"\n");

    }//</editor-fold>
    
    
    

    /**
     * my Initializations:
     *
     * <code>myInit</code> is called after the standard initializations of
     * the View class from <code>IcontrolView<\code>.<p>
     *
     * All variables that this call uses are initialized here. Also some elements
     * of the GUI are initialized. Add your own inits here.<p>
     *
     * To enable the Debug mode, change the entry 'iC.Debug' in iC.properties -
     * it might be necessary to Clean&Build after changing property files.
     *
     */
    protected void myInit() {
        
        // local variables
        String CommandLineFileName = "";
              
        // set the default Locale to US
        // GPIB commands usually require a '.' as comma separator, not a ',' as
        // is used in e.g. German language
        Locale.setDefault(Locale.US);

        // misc
        m_ScriptModified = false;
        
        ///////////////////////////////////////
        // init Editor Pane for the Status Text
        // http://www.medienpunkt.com/index.php?option=com_content&view=article&id=51%3Anachrichtenfeld-mit-formatierung&catid=34%3Aswing&lang=de

        // create the styles
        m_StatusTextStyles = new StyleContext();

        Style DefaultStyle = m_StatusTextStyles.getStyle(StyleContext.DEFAULT_STYLE);
        StyleConstants.setFontSize(DefaultStyle, 14);

        Style ErrorStyle = m_StatusTextStyles.addStyle("ErrorStyle", DefaultStyle);
        StyleConstants.setForeground(ErrorStyle, Color.red);

        Style WarningStyle = m_StatusTextStyles.addStyle("WarningStyle", DefaultStyle);
        StyleConstants.setForeground(WarningStyle, Color.blue);


        // create the document
        m_StatusTextDoc = new DefaultStyledDocument(m_StatusTextStyles);

        // Set the default (logical) style
        m_StatusTextDoc.setLogicalStyle(0, DefaultStyle);
        

        // attach document to the Editor Pane
        jStatusText1.setContentType("text/rtf");
        jStatusText1.setDocument(m_StatusTextDoc);
        

        // show a welcome message to the user
        // ATTENTION: also change version number in About Box
        DisplayStatusMessage("Welcome to Instrument Control (iC) v1.3\n", false);
        DisplayStatusMessage("Done by KPP in 2010-12!\n\n", false);
        DisplayStatusMessage("If you like iC, please cite http://dx.doi.org/10.6028/jres.117.010\n\n", false);



        ////////////////
        // iC properties

        // instantiate iC_Properties
        m_iC_Properties = new iC_Properties();

        /* Add command line parameters.
         * Every space in the command line adds a new String to the m_Args array
         *
         * It is possible to set every property using the general form:
         * PropertyName=Value, for instance: iC.Debug=1 or iC.Debug=0
         * Note that no spaces are allowed
         */

        // parse all command line parameters
        for (String str : m_Args) {
            // compile the Regex pattern for String=String
            Pattern pattern = Pattern.compile("(\\S+)=(.+)");
            
            // match pattern
            Matcher m = pattern.matcher(str);

            // correct arguments found?
            if ( m.find() ) {
                String Key = m.group(1);
                String Value = m.group(2);
                
                // check if it's an iC_Property
                String dummy = m_iC_Properties.getString(Key, "&UGEt#xu4raRa2raxudrASWuQ4Daq=w?");
                if ( !dummy.equals("&UGEt#xu4raRa2raxudrASWuQ4Daq=w?") ) {
                    // yes, the name was found in the iC.properties
                    // add the new key/value pair to the user properties
                    m_iC_Properties.setString(Key, Value);
                } else {
                    // display a warning in the status area
                    dummy = "Warning: The command line argument " + str + "\n"
                        + "contained a key (" + Key + ")\n"
                        + "which is not defined in iC.properties.\n";
                    DisplayStatusMessage(dummy, false);
                }
                
            } else if (new File(str).exists()) {
                // it's a file path, so remember to load this, not the AutoLoad
                CommandLineFileName = str;
                
            } else {
                // command line argument not recognized
                String dummy = "Warning: Could not find the file or interpret the command line argument\n" + str + "\n";
                DisplayStatusMessage(dummy, false);
            }
        }
        


        /////////////////////////
        // set/make default paths
        
        // search iC directory first in the default directory (user home), and
        // if not there, in the current working directory. Note, that when iC is 
        // started from the command line, the working directory is not necessarily 
        // the directory where the Icontrol.jar file resides (java -jar /iC/Icontrol.jar)
        
        // <editor-fold defaultstate="collapsed" desc="search iC-dir in home/working dir">
        
        // get default path from iC properties
        String Default_iCPath = m_iC_Properties.getPath("iC.DefaultPath", "$user.home$/iC");
        
        // check if directory exists
        if ( !new File(Default_iCPath).exists() ) {
            
            // no, so check in current working directory
            
            // get the path to iC in the working directroy
            String TestDir = System.getProperty("user.dir") + System.getProperty("file.separator") 
                             + "iC" + System.getProperty("file.separator");
            
            
            // use this path if it exists
            if (new File(TestDir).exists()) {
                Default_iCPath = TestDir;
                
                // also store it in iC_User.properties for later access (e.g. to find Startup.py)
                m_iC_Properties.setString("iC.DefaultPath", TestDir);
            }
            
        }//</editor-fold>
        
        // check if directory 'iC log' exists in the iC directory from above
        // <editor-fold defaultstate="collapsed" desc="check for iC log in iC dir from above">
        
        // make a new file to test if dir 'iC' exists in the working dir
        String TestDir = Default_iCPath + "iC log";
        File path = new File(TestDir);
        
        // assign the default directory for the 'iC' folder 
        // which can contain sub-scripts and Generic GPIB Instruments
        String LogFilePath;
        if (path.exists()) {
            
            // use current working directory
            LogFilePath = TestDir;
            
        } else {
            
            // directory for the log files
            LogFilePath = m_iC_Properties.getPath("iC.DefaultLogPath", Default_iCPath + "iC log");
        }//</editor-fold>
        
        // check if path 'iC' exists and ask user to create it if not
        // <editor-fold defaultstate="collapsed" desc="check & make iC path">
        path = new File(Default_iCPath);
        if ( !path.exists() ) {

            // path does not exist so ask user if it should be created
            // show a dialog to ask the user what to do
            String str = "The iC directory does not exist in your home directory.\n"
                + "This directory is used to store log-files, it contains the definitions\n"
                + "for generic instruments, and it is the default directory to include sub-scripts.\n\n"
                + "It is recommended to create the directory to ensure proper operation of iC.\n"
                + "If you have downloaded a zip'ed version of Instrument Control (iC)\n"
                + "the zip file should contain a iC directory which you could copy to\n"
                + path.getParent() + ".\n"
                + "Do you want to create the directory now?";

            Object[] ButtonLabels = { "Create Directory", "No, thanks" };
            Integer ret = JOptionPane.showOptionDialog(this.getComponent(), str,
                                "Directory does not exist", JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE, m_iC_Properties.getLogoSmall(),
                                ButtonLabels, ButtonLabels[0]);

            // check the User's answer
            if ( ret == 0) {
                // the user choose to create the directory
                boolean success;
                try {
                    success = path.mkdirs();
                } catch (SecurityException ex) {
                    // creating the directories obviously failed ...
                    success = false;
                }

                // creating the directory failed, so show a message
                if ( !success ) {
                    str = "Could not create the directory '" + Default_iCPath + "'.\n";
                    str += "Ensure that you have proper permissions, or create the path manually and try again.\n";

                    JOptionPane.showMessageDialog(this.getComponent(), str,
                                "Directory could not be created", JOptionPane.ERROR_MESSAGE, m_iC_Properties.getLogoSmall());
                    return;
                }
            }
        }//</editor-fold>

        // check if path 'iC log' exists and ask user to create it if not
        // <editor-fold defaultstate="collapsed" desc="check & make iC log path">
        path = new File(LogFilePath);
        if ( !path.exists() ) {

            // path does not exist so ask user if it should be created
            // show a dialog to ask the user what to do
            String str = "The directory for the Log-files does not exists.\n";
            str += "Log-files can be very helpful, and it is recommended to create this directory.\n\n";
            str += "Do you want to create the directory now?";

            Object[] ButtonLabels = { "Create Directory", "No thanks" };
            Integer ret = JOptionPane.showOptionDialog(this.getComponent(), str,
                                "Directory does not exist", JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE, m_iC_Properties.getLogoSmall(),
                                ButtonLabels, ButtonLabels[0]);

            // check the User's answer
            if ( ret == 0) {
                // the user choose to create the directory
                boolean success;
                try {
                    success = path.mkdirs();
                } catch (SecurityException ex) {
                    // creating the directories obviously failed ...
                    success = false;
                }

                // creating the directory failed, so show a message
                if ( !success ) {
                    str = "Could not create the directory '" + LogFilePath + "'.\n";
                    str += "Ensure that you have proper permissions, or create the path manually and try again.\n";

                    JOptionPane.showMessageDialog(this.getComponent(), str,
                                "Directory could not be created", JOptionPane.ERROR_MESSAGE, m_iC_Properties.getLogoSmall());
                    return;
                }
            }
        }//</editor-fold>


        
        
        ////////////
        // iC Logger
        // <editor-fold defaultstate="collapsed" desc="iC Logger">
        // set Logger level
        String LogLevel = m_iC_Properties.getString("iC.LogLevel", "FINE").toUpperCase();
        m_Logger.setLevel(Level.parse(LogLevel));


        // reformat the current date and time
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd_HHmmss");
        String DateString = sdf.format(Calendar.getInstance().getTime());

        // get Log directory and make the file name
        String LogFileName = LogFilePath
                + System.getProperty("file.separator") + DateString + "_iC_Log.txt";

        // define a new Formatter-class
        // <editor-fold defaultstate="collapsed" desc="myFormatter">
        class myFormatter extends java.util.logging.Formatter {
            
            // constructor is probably not necessary

            @Override
            public String format(LogRecord record) {
                
                // add LoggerName
                String str = record.getLoggerName() + ":\t";
                
                // add Message
                str += formatMessage(record);

                // add a newline if non was given
                if ( !str.endsWith("\n") )
                        str += "\n";

                // replace every \n with a \r\n
                str = str.replaceAll("[\n]", "\r\n");

                return str;
            }
        }//</editor-fold>
        
        // make the Formatter
        myFormatter form = new myFormatter();

        // direct Logger output to file
        try {
            // make new file handler
            FileHandler fh = new FileHandler(LogFileName, true);

            // attach to logger
            m_Logger.addHandler(fh);

            // attach the Formatter to the FileHandler
            fh.setFormatter(form);

        } catch (IOException ex) {
            DisplayStatusMessage("Error: Could not open the Logger's FileHander.\n");
        } catch (SecurityException ex) {
            DisplayStatusMessage("Error: Insufficient rights to configure the Logger:\n"
                    + ex.getMessage() + "\n");
        }

        // log Logger level
        m_Logger.log(Level.CONFIG, "Logger Level = {0}\n", m_Logger.getLevel().toString());
        
        // log path to iC directory
        m_Logger.log(Level.CONFIG, "Location of the iC directory = {0}\n", Default_iCPath);
        //</editor-fold>

        
        //////////////
        // Comm Logger
        // <editor-fold defaultstate="collapsed" desc="Comm Logger">
        
        // set Logger level
        LogLevel = m_iC_Properties.getString("iC.CommLogLevel", "FINE").toUpperCase();
        m_Comm_Logger.setLevel(Level.parse(LogLevel));


        // get Log directory and make the file name
        LogFileName = LogFilePath
                + System.getProperty("file.separator") + DateString + "_Comm_Log.txt";

        // define a new Formatter-class
        // <editor-fold defaultstate="collapsed" desc="myCommFormatter">
        class myCommFormatter extends java.util.logging.Formatter {

            // local variables
            //SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.S");


            @Override
            public String format(LogRecord record) {

                // add Message
                String str = formatMessage(record);

                // add a newline if non was given
                if ( !str.endsWith("\n") )
                        str += "\n";

                // replace every \n with a \r\n
                str = str.replaceAll("[\n]", "\r\n");

                // precede string with a time stamp
                //str = sdf.format(Calendar.getInstance().getTime()) + " " + str;


                return str;
            }
        }//</editor-fold>


        // direct Logger output to file
        try {
            // make new file handler
            FileHandler fh = new FileHandler(LogFileName, true);

            // attach to logger
            m_Comm_Logger.addHandler(fh);

            // attach the Formatter to the FileHandler
            fh.setFormatter( new myCommFormatter() );

        } catch (IOException ex) {
            DisplayStatusMessage("Error: Could not open the Comm-Logger's FileHander.\n");
        } catch (SecurityException ex) {
            DisplayStatusMessage("Error: Insufficient rights to configure the Comm-Logger:\n"
                    + ex.getMessage() + "\n");
        }

        // log Logger level
        m_Comm_Logger.log(Level.CONFIG, "Comm-Logger Level = {0}\n", m_Logger.getLevel().toString());
        //</editor-fold>
        
        
        // <editor-fold defaultstate="collapsed" desc="Log Subversion number">
        // the ANT script in build.xml is modified to save it to file scm-version.txt
        InputStream is=null;
        try {
        
            // open file
            is = getClass().getResourceAsStream("resources/scm-version.txt");

            // read the version number
            if (is != null) {
                // http://stackoverflow.com/questions/309424/in-java-how-do-i-read-convert-an-inputstream-to-a-string
                String str = new Scanner(is).useDelimiter("\\A").next();

                // log version number
                m_Logger.log(Level.CONFIG, "Subversion revision: {0}", str);
            }
        } catch (Exception ex) {
            m_Logger.severe("Could not log the subversion revision number.");
        } finally {
            try {
                if (is != null) {
                    // close input stream (InputStream.close does nothing)
                    is.close();
                }
            } catch (Exception ignore) {}
        }//</editor-fold>

        
        
        /////////////////////////////////////////
        // set default uncaught exception handler
        // <editor-fold defaultstate="collapsed" desc="Default Uncaught Exception Handler">
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
              public void uncaughtException(Thread t, Throwable e) {

                  String str = "An unexpected Error occurred which was not handled by the program.\n"
                          + "The event has been logged, but the program will probably terminate.\n"
                          + "I apologize for the inconvenience. Please consider submitting this\n"
                          + "incident to the developer at http://java.net/projects/icontrol\n\n"
                          + "Thread:\t" + t.toString() + "\n"
                          + "Exception:\t" + e.getMessage() + "\n"
                          + "Cause:\t" + e.getCause().toString()+ "\n"
                          + "The stack trace has been added to the log-file.";

                  // add stack trace
                  //str += "\t" + Dispatcher.printStackTrace(e);

                  // log event
                  m_Logger.log(Level.SEVERE, "{0}\n\t{1}", new Object[]{str, 
                      icontrol.Utilities.printStackTrace(e)});

                  // display a last message to the user
                  JOptionPane.showMessageDialog(null, str, "Uncought Exception",
                          JOptionPane.ERROR_MESSAGE);
              }
        });

        // to test uncought exception handler
        if (false) {
            Device test = null;
            test.inNoCommunicationMode();
        }//</editor-fold>



        //////////////
        // prepare GUI

        // disable the Stop & Pause buttons
        jStop.setEnabled(false);
        jPause.setEnabled(false);

        // disable SendCommand Now button
        jSendNow.setEnabled(false);

        // populate the Port Selection Combo Box
        for (CommPorts i:CommPorts.values()) {
            jPortSelector.addItem(i.toString());
        }

        // disable No-Communication-Mode
        jNoCommunicationMode.setState(true);

        // disable auto-Pause Script
        jAutoPauseScript.setState(false);


        // disable the button to add a Script Command from the Auto GUI
        jScriptAddInstrumentCommand.setEnabled(false);

        // select GPIB card
        String GPIBcontroller = m_iC_Properties.getString("iC.DefaultGPIBcontroller", "NI");
        if (GPIBcontroller.equalsIgnoreCase("Prologix")) {
            // select Prologix GPIB-USB controller
            jRB_GPIB_Prologix.setSelected(true);
        } else if (GPIBcontroller.equalsIgnoreCase("IOtech")) {
            // select Prologix GPIB-USB controller
            jRB_GPIB_IOtech.setSelected(true);
        } else {
            // default option: choose NI GPIB controller using JNA to access it
            jRB_GPIB_NI.setSelected(true);
        }
        // get Key-Modifier depending on OS
        int ModifierKey;
        if (System.getProperty("os.name").equalsIgnoreCase("Mac OS X")) {
            // Command (apple)
            ModifierKey = InputEvent.META_MASK;
        } else {
            ModifierKey = InputEvent.CTRL_MASK;
        }
            
        // set keyboard shortcuts
        jFileNew.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, ModifierKey));
        jFileLoad.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, ModifierKey));
        jFileAppend.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, ModifierKey));
        jFileSaveAs.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, ModifierKey));
        
        



        ////////////////
        // show the logo

        // get Logo in different sizes
        ArrayList<Image> Icons = new ArrayList<Image>();
        Icons.add( m_iC_Properties.getLogoTiny().getImage() );
        Icons.add( m_iC_Properties.getLogoSmall().getImage() );
        Icons.add( m_iC_Properties.getLogoMedium().getImage() );
        
        // make it the icon of the application (I did not see the consequences of this)
        this.getFrame().setIconImages(Icons);
        
      
        // disable No-Communication-Mode
        jNoCommunicationMode.setState(false);




        // -----------
        // Script List
        // holds the script commands

        // make a new and empty List
        m_ScriptList = new DefaultListModel();

        // and attach it to the List Box in the GUI
        jScriptList.setModel(m_ScriptList);

        /* Enable drag and drop to and in the Script list.
         * The transfer handler seems to support single selections only,
         * hence, only single selections are allowed
         */
        jScriptList.setTransferHandler(new ListTransferHandler(this));
        jScriptList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);


        // ---------------
        // Instrument List
        // holds all instruments used in the script

        // make a new and empty List
        m_InstrumentList = new DefaultListModel();

        // and attach it to the List Box in the GUI
        jInstrumentList.setModel(m_InstrumentList);


        // ------------
        // Command List
        // holds the commands of the selected instrument

        // make a new and empty List
        m_CommandList = new DefaultListModel();

        // and attach it to the List Box in the GUI
        jDeviceCommandList.setModel(m_CommandList);

        // new HashMap for the ScriptInstruments
        m_ScriptInstruments = new HashMap<String, String>();





        ////////////////////
        // prepare the Table

        /**
         * Make a new JTable which shows the Instrument-Command's parameters.
         * The Table also displays different tool-tips
         * for different cells. This is achieved by overriding
         * <code>getToolTipText</code>. It also overrides
         * <code>JTable#getCellEditor</code> and <code>JTable#getCellRenderer</code>
         * to enable different Objects (String, Integer, Double, ...) in the same
         * column.<p>
         *
         * Note that the JTable needs to be created manually, hence, in Netbeans'
         * GUI builder only a ScrollPane is created, which is then manually connected
         * with the Table.
         */
        // <editor-fold defaultstate="collapsed" desc="My Table Model">
        m_jTable = new JTable( new MyTableModel() ) {
            /**
             * This method is called when the mouse hovers a few moments
             * above a cell in the table.
             * @return The ToolTip Text for the row the mouse hovers over
             */
            @Override
            public String getToolTipText(MouseEvent e) {
                java.awt.Point p = e.getPoint();

                int row = convertRowIndexToModel(rowAtPoint(p));
                //int col = convertColumnIndexToModel(columnAtPoint(p));

                MyTableModel tm = (MyTableModel)m_jTable.getModel();
                return tm.getToolTip(row);
            }

            /**
             * @return the CellEditor for the selected cell-type, rather than
             * the selected column as in the standard implementation of JTable.
             */
            @Override
            public TableCellEditor getCellEditor(int row, int col) {
                // get the TableModel, then the value, then the class of the value
                // and return a DefaultEditor of that type
                
                //TODO 6* testing auto select Table entries
//                DefaultCellEditor dummy = (DefaultCellEditor) getDefaultEditor(getModel().getValueAt(row, col).getClass());
//                Component TextField = dummy.getComponent();
//                if (TextField instanceof JTextField) {
//                    ((JTextField)TextField).selectAll();
//                }
//                final JTextField c = (JTextField) super.getTableCellEditorComponent(
//                    table,
//                    ((Cell) value).text, // edit the text field of Cell
//                    isSelected,
//                    row,
//                    column);
//
//                c.selectAll(); // automatically select the whole string in the cell

                //dummy.shouldSelectCell(null);
                
                return getDefaultEditor(getModel().getValueAt(row, col).getClass());
            }

            /**
             * @return the CellRenderer for the selected cell-type, rather than
             * the selected column as in the standard implementation of JTable.
             */
            @Override
            public TableCellRenderer getCellRenderer(int row, int col) {
                return getDefaultRenderer(getModel().getValueAt(row, col).getClass());
            }
        };//</editor-fold>

        // Netbeans assigned a name, so I do it too
        // might be good for automated resource injection
        m_jTable.setName("jTable");

        // choose to select individual cells as SelectionMode
        m_jTable.setCellSelectionEnabled(true);

        // view the table in the scroll view defined in the GUI
        jTableScrollPane.setViewportView(m_jTable);

        // does not help to show the desired effect
        //m_jTable.setDragEnabled(false);
        m_jTable.setShowHorizontalLines(true);
        m_jTable.setShowVerticalLines(true);
        m_jTable.setShowGrid(true);




        /////////////////////
        // prepare Dispatcher

        // make a new Dispatcher instance
        // this also calls RegisterGenericDeviceClasses() which registers all available
        // Instrument Classes
        m_Dispatcher = new Dispatcher(this);
        
        

        ///////////////////////////
        // populate Instrument List

        // get registered instuments
        String[] KeyStrings = m_Dispatcher.getRegisteredInstruments().keySet().toArray(new String[0]);

        // Populate the list of available Instruments
        for (int i=0; i < KeyStrings.length; i++) {
            jInstrumentModel.addItem( KeyStrings[i] );
        }


        // <editor-fold defaultstate="collapsed" desc="OLD: add path to external libraries">
        // Also add -Djava.library.path=${dist.dir} to the VM Options in
        // the project properties so that the libraries can be found when
        // iC is started from within Netbeans (which provides a different
        // user.home as when the .jar is started directly
        // Even with the proper path added to java.library.path the RxTx libs were not found
        /*try {
            // get current path
            String Path = System.getProperty("java.library.path");
            
            // log path
            m_Logger.log(Level.CONFIG, "old java.library.path= {0}\n", Path);
            DisplayStatusMessage("old path= " + Path + "\n\n");
                      
            // create path to "dist/lib" (for RxTx files reside; copied by ANT)
            // old: System.getProperty("user.dir")
            String LibPath = WorkingDirectory + System.getProperty("file.separator") + "lib";
         
            // add "dist/lib" if not already present
            if ( !Path.toLowerCase().contains(LibPath.toLowerCase()) ) {
                
                // add path
                Path += System.getProperty("path.separator") + LibPath;
                
                // set the path
                System.setProperty("java.library.path", Path);
                
                // log path
                m_Logger.log(Level.CONFIG, "new java.library.path= {0}\n", Path);
                DisplayStatusMessage("new path= " + Path + "\n\n\n");
            }

        } catch (SecurityException ex) {

            String str = "The java.library.path could not be set.\n"
                + "Some required libraries might not be found, hence, iC might not work properly.\n"
                + "Please report this incident to the developer and include the log files.\n"
                + "A workaround for the problem is to copy the library files that were not found\n"
                + "(from the lib directory) to somewhere in this path:\n";
            str += System.getProperty("java.library.path" + "\n\n");
            str += "When iC is started from within Netbeans, add\n";
            str += "-Djava.library.path=${dist.dir} to the VM Options in the Project Properties.";
            JOptionPane.showMessageDialog(this.getComponent(), str,
                    "Insufficient rights", JOptionPane.INFORMATION_MESSAGE, m_iC_Properties.getLogoSmall());
        }*/
        //</editor-fold>


        ///////////////////////////////////
        // set and check Character encoding
        // TODO move to iC_Utilities class ?
        // <editor-fold defaultstate="collapsed" desc="set and check Character encoding">
        
        /* When programming the SRS DS345 I found that it was not trivial to convert
         * a byte[] into a string so that the bytes were the same upon back-converting
         * them using String.getBytes(). For a long time I have used the default
         * character encoding (for JNA and where I forgot to specify UFT-8) and
         * UTF-8 in some other parts. UTF-8 does not map Unicode Code Point 0-255
         * into equal unicode values, but ISO-8859-1 does. There are, however,
         * certain caveats in my understanding, becuase not all values below 255
         * are defined in the code page, so I prefer to make sure that all values
         * up to 255 are encoded properly on the platform iC runs on.
         */
        
        // get character encoding used to convert between String and byte[]
        final String CHARACTER_ENCODING = m_iC_Properties.getString("iC.CharacterEncoding", "ISO-8859-1");

        // set character encoding used by JNA
        // if this character encoding is not available, JNa will fall back to
        // the defualt character encoding (probably without warning)
        System.setProperty("jna.encoding", CHARACTER_ENCODING);
        
        
        // TODO 4* break up myInit into small subroutines ?
        
        // thorough test with all values from 0 to 255
        byte[] AllValues = new byte[256];
        for (int i = 0; i <= 255; i++) {
            AllValues[i] = (byte) i;
        }
        
        try {
            // convert to string
            String AllValuesStr = new String(AllValues, CHARACTER_ENCODING);

            // convert back to byte[]
            byte[] ConvertedBack = AllValuesStr.getBytes(CHARACTER_ENCODING);
            
            for (int i = 0; i <= 255; i++) {
                if (AllValues[i] != ConvertedBack[i]) {
                    String str = "Error: The chosen Character Encoding did not yield the expected results.\n"
                            + "Some methods might not work properly, especially if they rely on transferring\n"
                            + "binary data, e.g. SRS DS345 setARBtoCELIV. Most other methods should still work.\n";
                    DisplayStatusMessage(str, false);
                }
            }
        } catch (UnsupportedEncodingException ex) {
            String str = "Error: The chosen Character Encoding (" + CHARACTER_ENCODING + ")\n"
                    + "is not supported on your platfom. You might choose to add\n"
                    + "iC.CharacterEncoding = UTF-8 to your iC_User.properties file.\n"
                    + "Be warned that some methods might not work properly, especially if they rely on transferring\n"
                    + "binary data, e.g. SRS DS345 setARBtoCELIV. Most other methods should still work.\n";
            DisplayStatusMessage(str, false);
        }
        
        // log result
        m_Logger.log(Level.CONFIG, "Character encoding {0} successfully tested\n", CHARACTER_ENCODING);
        // </editor-fold>

        
        
        
        /////////////////////////
        // show What's new Dialog
        new WhatsNew(this.getFrame());
        
        
        
        /////////////////////////
        // auto load last session
        
        // set the initial project directory to the user's home directory
        jProjectDir.setText(System.getProperty("user.home"));
        
        
        if (m_iC_Properties.getInt("iC.AutoLoadLastSession", 0) == 1 ) {
            
            // initialize project directory to the directory used when the 
            // Start Button was pressed the last time (if it still exists)
            File f = new File(m_iC_Properties.getPath("iC.ProjectDir", ""));
            if (f.exists()) {
                jProjectDir.setText(m_iC_Properties.getPath("iC.ProjectDir", ""));
            }

            // set file name used last time
            jFileName.setText(m_iC_Properties.getString("iC.FileName", ""));
        
            // load script that was loaded or saved last
            String dummy = m_iC_Properties.getString("iC.LastLoaded", "");
            
            if ( !dummy.isEmpty() && CommandLineFileName.isEmpty()) {
                
                boolean FileLoaded = true;
                
                try {
                    
                    LoadScript("", dummy);
                    
                } catch (FileNotFoundException ex) {

                    String str = "Warning: Could not auto-load the last script:\n" + dummy + " was not found.\n";
                                        
                    // show Status Message
                    DisplayStatusMessage(str, false);
                    
                    // log event
                    m_Logger.log(Level.INFO, "{0}{1}", new Object[]{str, ex.getMessage()});
                    
                    // remember than an error occured
                    FileLoaded = false;
                }

                // show Status Message
                if (FileLoaded) {
                    DisplayStatusMessage("Auto-Loaded " + dummy + "\n", false);
                }
            }
        }
        
        // load script specified in Command Line
        try {
            if ( !CommandLineFileName.isEmpty() ) {
                LoadScript("", CommandLineFileName);
            }
                    
        } catch (FileNotFoundException ex) {

            String str = "Warning: Could not load the file specified in the Command Line\n" + CommandLineFileName + " was not found.\n";

            // show Status Message
            DisplayStatusMessage(str, false);
        }
        

        ////////////////////////////
        // for debugging/development

        // return if not in Debug mode
        if ( m_iC_Properties.getInt("iC.Debug", 0) == 0 )
            return;

        
        // init text fields
        jInstrumentName.setText("dT");
        jAddress.setText("4");
        jRS232params.setText("COM1, 9600, 8, 1, none");


        // display a status message
        DisplayStatusMessage("Debug Mode enabled.\n", false);


        // log directories
        m_Logger.log(Level.FINER, "user.dir: {0}\n", System.getProperty("user.dir"));
        m_Logger.log(Level.FINER, "current.dir: {0}\n", new File(".").getAbsolutePath());
        m_Logger.log(Level.FINER, "java.library.path: {0}\n", System.getProperty("java.library.path"));
        // http://stackoverflow.com/questions/661320/how-to-add-native-library-to-java-library-path-with-eclipse-launch-instead-of
        
        
        
        // init Script depending on which computer I use
        if ( System.getProperty("os.name").equalsIgnoreCase("Mac OS X") ) {

            ///////////
            // MAC init
            // Office, no GPIB Instruments attached

            // select No-Comminucation-Mode
            jNoCommunicationMode.setState(true);

            // load a script
            //LoadScript("", "Calibrate and Test at 290K.iC");
        } else {

            ///////////
            // WIN init
            // Lab, GPIB Instruments are connected

        }
        
        // log everything
        m_Logger.setLevel(Level.parse("ALL"));
        

        // enable auto-Pause Script
        //jAutoPauseScript.setState(true);

        // press the start button for me
        //Start();
    }
    
    
    // TODO 1* allow multiple selections as well
    // <editor-fold defaultstate="collapsed" desc="List Transfer Handler as inner class">
    /*
     * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
     *
     * Redistribution and use in source and binary forms, with or without
     * modification, are permitted provided that the following conditions
     * are met:
     *
     *   - Redistributions of source code must retain the above copyright
     *     notice, this list of conditions and the following disclaimer.
     *
     *   - Redistributions in binary form must reproduce the above copyright
     *     notice, this list of conditions and the following disclaimer in the
     *     documentation and/or other materials provided with the distribution.
     *
     *   - Neither the name of Oracle or the names of its
     *     contributors may be used to endorse or promote products derived
     *     from this software without specific prior written permission.
     *
     * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
     * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
     * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
     * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
     * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
     * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
     * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
     * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
     * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
     * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
     * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
     */
    /*
     * There was a bug in the demo version of the ListTransferHandler. Thanks to
     * n0weak I got an idea how to fix it. Changes are marked with 'bugfix' in the comment
     * http://stackoverflow.com/questions/5198132/listtransferhandler-in-dropdemo-has-a-bug
     */
    private class ListTransferHandler extends TransferHandler {
        private int[] indices = null;
        private int addIndex = -1;  //Location where items were added
        private int addCount = 0;   //Number of items added.
        private boolean insert;     // bugfix

        // allows accessing the GUI to change the flag when the script has been modified
        private IcontrolView m_GUI;

        public ListTransferHandler(IcontrolView GUI) {
            m_GUI = GUI;
        }

        @Override
        public boolean canImport(TransferHandler.TransferSupport info) {
            // Check for String flavor
            if (!info.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                return false;
            }
            return true;
       }

        @Override
        protected Transferable createTransferable(JComponent c) {
            return new StringSelection(exportString(c));
        }

        @Override
        public int getSourceActions(JComponent c) {
            return TransferHandler.COPY_OR_MOVE;
        }

        @Override
        public boolean importData(TransferHandler.TransferSupport info) {
            if (!info.isDrop()) {
                return false;
            }

            JList list = (JList)info.getComponent();
            DefaultListModel listModel = (DefaultListModel)list.getModel();
            JList.DropLocation dl = (JList.DropLocation)info.getDropLocation();

            // bugfix (method local variables were mistakenly used)
            insert = dl.isInsert();     // was boolean insert = dl.isInsert();
            addIndex = dl.getIndex();   // was int index = dl.getIndex();
            addCount = indices.length;

            // Get the string that is being dropped.
            Transferable t = info.getTransferable();
            String data;
            try {
                data = (String)t.getTransferData(DataFlavor.stringFlavor);
            } 
            catch (Exception e) { return false; }

            // Perform the actual import.  
            if (insert) {
                // bugfix
                listModel.add(addIndex, data);
            } else {
                // bugfix
                listModel.set(addIndex, data);
            }

            // remember that the script has been modified
            m_GUI.setScriptModified(true);


            return true;
        }

        @Override
        protected void exportDone(JComponent c, Transferable data, int action) {
            // bugfix; was: cleanup(c, action == TransferHandler.MOVE);
            cleanup(c, insert && action == TransferHandler.MOVE);
        }

        //Bundle up the selected items in the list as a single string, for export.
        protected String exportString(JComponent c) {
            JList list = (JList)c;
            indices = list.getSelectedIndices();
            Object[] values = list.getSelectedValues();

            //StringBuffer buff = new StringBuffer();
            StringBuilder buff = new StringBuilder();

            for (int i = 0; i < values.length; i++) {
                Object val = values[i];
                buff.append(val == null ? "" : val.toString());
                if (i != values.length - 1) {
                    buff.append("\n");
                }
            }

            return buff.toString();
        }

        //Take the incoming string and wherever there is a
        //newline, break it into a separate item in the list.
        /* bugfix: This method is never called it seems */
        /* protected void importString(JComponent c, String str)*/

        //If the remove argument is true, the drop has been
        //successful and it's time to remove the selected items 
        //from the list. If the remove argument is false, it
        //was a Copy operation and the original list is left intact.
        protected void cleanup(JComponent c, boolean remove) {
            if (remove && indices != null) {
                JList source = (JList)c;
                DefaultListModel model  = (DefaultListModel)source.getModel();

                //If we are moving items around in the same list, we
                //need to adjust the indices accordingly, since those
                //after the insertion point have moved.
                if (addCount > 0) {
                    for (int i = 0; i < indices.length; i++) {
                        if (indices[i] > addIndex) {
                            indices[i] += addCount;
                        }
                    }
                }
                for (int i = indices.length - 1; i >= 0; i--) {
                    model.remove(indices[i]);
                }
            }
            indices = null;
            addCount = 0;
            addIndex = -1;
        }
    }// </editor-fold>



// <editor-fold defaultstate="collapsed" desc="The Framework's variables declaration - do not modify">
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroupGPIBMenu;
    private javax.swing.Box.Filler filler1;
    private javax.swing.JButton jAddSubScript;
    private javax.swing.JFormattedTextField jAddress;
    private javax.swing.JCheckBoxMenuItem jAutoPauseScript;
    private javax.swing.JList jDeviceCommandList;
    private javax.swing.JMenuItem jFileAppend;
    private javax.swing.JMenuItem jFileLoad;
    private javax.swing.JTextField jFileName;
    private javax.swing.JMenuItem jFileNew;
    private javax.swing.JMenuItem jFileSaveAs;
    private javax.swing.JList jInstrumentList;
    private javax.swing.JComboBox jInstrumentModel;
    private javax.swing.JTextField jInstrumentName;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JButton jMakeInstrument;
    private javax.swing.JMenu jMenuGPIB;
    private javax.swing.JMenu jMenuOptions;
    private javax.swing.JCheckBoxMenuItem jNoCommunicationMode;
    private javax.swing.JPanel jPanelFileName;
    private javax.swing.JPanel jPanelInclude;
    private javax.swing.JPanel jPanelLeft;
    private javax.swing.JPanel jPanelLists;
    private javax.swing.JPanel jPanelMake;
    private javax.swing.JPanel jPanelScript;
    private javax.swing.JPanel jPanelTable;
    private javax.swing.JButton jPause;
    private javax.swing.JComboBox jPortSelector;
    private javax.swing.JTextField jProjectDir;
    private javax.swing.JButton jProjectDirSelect;
    private javax.swing.JButton jPython;
    private javax.swing.JCheckBoxMenuItem jRB_GPIB_IOtech;
    private javax.swing.JRadioButtonMenuItem jRB_GPIB_NI;
    private javax.swing.JRadioButtonMenuItem jRB_GPIB_Prologix;
    private javax.swing.JTextField jRS232params;
    private javax.swing.JButton jScriptAddInstrumentCommand;
    private javax.swing.JTextField jScriptFileToInclude;
    private javax.swing.JTextField jScriptLine;
    private javax.swing.JButton jScriptLineUp;
    private javax.swing.JList jScriptList;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPaneStatusText;
    private javax.swing.JButton jSelectScriptFileToInclude;
    private javax.swing.JButton jSendNow;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JPopupMenu.Separator jSeparator5;
    private javax.swing.JSplitPane jSplitPaneAutoGUI;
    private javax.swing.JSplitPane jSplitPaneAutoGUI_Status;
    private javax.swing.JSplitPane jSplitPaneMain;
    private javax.swing.JButton jStart;
    private static javax.swing.JEditorPane jStatusText1;
    private javax.swing.JButton jStop;
    private javax.swing.JTabbedPane jTabbedPaneMakeInclude;
    private javax.swing.JScrollPane jTableScrollPane;
    private javax.swing.JMenuItem jiCLocalJavadoc;
    private javax.swing.JMenuItem jiCOnlineJavadoc;
    private javax.swing.JMenuItem jiCPublication;
    private javax.swing.JMenuItem jiCWebsite;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JLabel statusMessageLabel;
    private javax.swing.JPanel statusPanel;
    // End of variables declaration//GEN-END:variables

    private final Timer messageTimer;
    private final Timer busyIconTimer;
    private final Icon idleIcon;
    private final Icon[] busyIcons = new Icon[15];
    private int busyIconIndex = 0;

    private JDialog aboutBox;
    //</editor-fold>
}
