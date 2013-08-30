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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.util.ShapeUtilities;

/**
 * This is a wrapper class for creating JFreeCharts in a convenient way. Instantiating
 * an object of this class automatically displays a new window (JFrame) with an
 * empty XY-Chart.<p>
 *
 * New traces (XYSeries) can be added with defined line style and marker types by
 * calling <code>AddXYSeries</code>. Data points are added to the defined 
 * traces by invoking <code>AddXYDataPoints</code>.<p>
 *
 * This class implements <code>ActionListener</code> to handle events from a
 * customized PopUp Menu.<p>
 *
 * <h3>Known issues:</h3>
 * <ul>
 *  <li>Initial tests (110331) indicated that too many charts can occupy too much
 * heap space so that the Java VM crashes. That's why for now the number of data
 * points displayed is limited to 1000. All data points are logged however. It
 * is likely, that this bug was introduced with the Java 6 update 25, but maybe it
 * was also there before. New hints indicate that having more than one window
 * displaying a chart increases instability.
 * </ul>

 *
 * <h3>Some other useful commands:</h3>
 * <ul>
 *  <li>m_Chart.getXAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());
 *  <li>m_Chart.getXAxis().setNumberFormatOverride(new DecimalFormat("##0.#####E0")); mixed 
 * engineering/scientific format
 *  <li>m_Chart.getXAxis().setFixedAutoRange(23);
 *  <li>m_Chart.LogXAxis(true); (from outside this class)
 *  <li>m_Chart.ClearSeries(); (from outside this class)
 *  <li>m_Chart.getYAxis(0).setStandardTickUnits(new StandardTickUnitSource()); dynamically 
 * create tick units to correctly display very small and very large numbers
 *  <li>m_Chart.getYAxis(0).setAutoRangeMinimumSize(1e-15); minimum value used
 * for AutoRanging
 * </ul>
 *
 * @author KPP (Kurt Pernstich: pernstich@alumni.ethz.ch)
 * @version 0.1
 */
public class iC_ChartXY extends JFrame implements ActionListener {
        // JFreeChart Demos extend ApplicationFrame
        // org.jfree.ui.ApplicationFrame extends JFrame

    ///////////////////
    // member variables

    /** The Title of the Frame and Chart (?) */
    private String      m_Title;

    /** 
     * The Collections of multiple XY Series which are displayed in the Chart.<br>
     * Each Y-axis has it's own SeriesCollection (and Renderer). The index 0
     * refers to the SeriesCollection of the default (left) Y axis and it is always present.
     */
    private ArrayList<XYSeriesCollection>   m_XYSeriesCollections;
            

    /** The Chart itself */
    private JFreeChart  m_Chart;

    /** The Plot in the chart */
    private XYPlot m_Plot;

    /** 
     * The Renderers of the Plot.<br>
     * Each Y-axis has it's own Renderer (and SeriesCollection). The index 0
     * refers to the Renderer of the default (left) Y axis and it is always present.
     */
    private ArrayList<XYLineAndShapeRenderer> m_Renderers;

    /** The Logger for this class */
    private static final Logger m_Logger = Logger.getLogger("iC.ChartXY");

    /**
     * Handle to identify the Data Series. Consists of SeriesNumber and AxisIndex.<br>
     * At a later stage it might also contain the number of the Sub-Chart if multiple
     * charts are shown in one Panel.
     */
    public class SeriesIdentification {
        int SeriesNumber;
        int AxisIndex;
    }
    
    /**
     * Grants access to global properties of the iC application in a resource
     * bundle file 'iC.properties'
     */
    protected static iC_Properties m_iC_Properties;
    
    /* Counter for the Title bar */
    private static int m_TitleCounter = 1;
    
    /** 
     * Counter to manually start Garbage Collection.
     * On a few occasions iC crashed the Java VM without much info what caused the
     * crash. The only info I could extract is that the C heap became too small
     * and JNA cannot allocate memory. When iC crashed, I always had lot's of
     * JFreeCharts open, so it might be related to that. Direct ByteBuffers 
     * are not GC automatically, which is a known problem but iC only uses
     * indirect ByteBuffers. So I try starting GC manually after displaying
     * 100 data points. This variable stores how many data points have been plotted
     * since the last GC.
     */
    private static int m_GCcounter = 0;
    
    /**
     * Holds a reference to the <code>IcontrolView</code> class. Using getView 
     * to get a handle to the View (to use DisplayStatusMessage etc.) was introduced 
     * with JUnit tests on 120727 (v1.3.469).
     */
    protected static final GUI_Interface m_GUI = Utilities.getView();
    
    ////////////
    // constants
    //                              {width, opaque lenght, transparent length}
    public final float[] LINE_SOLID = {1.0f, 1.0f, 0.0f};
    public final float[] LINE_SOLID_FINE = {0.5f, 1.0f, 0.0f};
    public final float[] LINE_DASHED = {1.0f, 6.0f, 10.0f};
    public final float[] LINE_DASHED_FINE = {0.5f, 6.0f, 10.0f};
    public final float[] LINE_DOTTED = {1.0f, 2.0f, 4.0f};
    public final float[] LINE_DOTTED_FINE = {0.5f, 2.0f, 4.0f};
    public final float[] LINE_NONE = {0.0f, 0.0f, 0.0f};
        //the value of LINE_NONE is ignored, it just needs to be different to any other value

    public final Shape MARKER_NONE = null;
    public final Shape MARKER_DOT = new Ellipse2D.Float(-1f, -1f, 2.0f, 2.0f);
    public final Shape MARKER_CIRCLE = new Ellipse2D.Float(-2.0f, -2.0f, 4.0f, 4.0f);
    public final Shape MARKER_SQUARE = ShapeUtilities.createRegularCross(3.0f, 3.0f);
    public final Shape MARKER_DIAMOND = ShapeUtilities.createDiamond(3.0f);
    public final Shape MARKER_DIAMOND_15x = ShapeUtilities.createDiamond(4.5f);
    public final Shape MARKER_DIAMOND_2x = ShapeUtilities.createDiamond(6.0f);


    // entries in the PopUp Menu
    private JMenuItem           m_MenuFixedAutoRange;
    private JMenuItem           m_MenuShowHideSeries;
    private JCheckBoxMenuItem   m_MenuLogXAxis;
    private JCheckBoxMenuItem   m_MenuLogYAxis;


    /**
     * Constructor<p>
     * 
     * Sets the Title of the <code>JFrame</code>, initializes the Swing components
     * defined by Netbeans (if any), and calls <code>CreatChart</code> which connects
     * the newly created ChartPanel with the <code>JFrame</code>.<p>
     * 
     * @param Title The title of the Frame (and of the Chart?)
     * @param XLabel for the chart
     * @param YLabel for the chart
     * @param Legend when <code>true</code>, a Legend is shown
     *
     * @param PreferredWidth The preferred width of the JFrame. If a value equal
     * or below 0 is specified, the default size is set to 640x480.
     *
     * @param PreferredHeight The preferred height of the JFrame. If a value equal
     * or below 0 is specified, the default size is set to 640x480.
     */
    // TODO 3* add this JFrame to the list of windows in the GUI
    // <editor-fold defaultstate="collapsed" desc="Constructor">
    public iC_ChartXY( String Title,
                       String XLabel, String YLabel,
                       boolean Legend,
                       int PreferredWidth, int PreferredHeight) {

        // set the window title
        super("iC " + Title + " " + m_TitleCounter);
        m_TitleCounter++;

        // remember the title
        m_Title = Title + " " + m_TitleCounter;

        // set Logger level to inherit level of parent logger
        m_Logger.setLevel(null);
        
        // instantiate iC_Properties
        m_iC_Properties = new iC_Properties();

        // init other Swing components
        initComponents();

        // init and show the Chart
        CreateChart(XLabel, YLabel, Legend, PreferredWidth, PreferredHeight);


        // get the logo from the resource map
        ImageIcon Logo = (new iC_Properties()).getLogoTiny();

        // make it the icon of this window
        if (Logo != null) {
            this.setIconImage(Logo.getImage());
        }

        // make the new JFrame visible
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                setVisible(true);
            }
        });

    }//</editor-fold>


    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setName("Form"); // NOI18N

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 300, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents








    /**
     * Creates a new <code>JFrame</code> that contains a <code>ChartPanel</code> which
     * in turn contains the XYChart (with no data in it). Use <code>AddXYSeries</code>
     * and <code>AddXYDataPoints</code> to add data points. Called by the Constructor.
     * The chart uses double buffering to display updates and adds entries to the
     * PopUp Menu.<p>
     *
     * If either <code>PreferredWidth</code> or <code>PreferredHeight</code> is 0
     * the preferred size of the chart is set to 640x480.<p>
     *
     * See JFreeChart's MultiplePieChartDemo1.java to see how more than one
     * chart fits into one JFrame or XYSplineRenderDemo1.java to see how to
     * create multiple charts in a tabbed view.<p>
     *
     * See MultipleDatasetDemo1.java (JFreeChart) to see how buttons can be added
     * to the JFrame. A Menu can simply be added with Netbeans GUI editor.<p>
     *
     * See XYSplineRenderDemo1.java (JFreeChart) to see how to created multiple
     * charts in a tabbed view.<p>
     *
     * @param XLabel The label of the x-axis of the chart
     * @param YLabel The label of the y-axis of the chart
     *
     * @param Legend When <code>true</code> a Legend with the name of the
     * XYSeries specified in <code></code> will be displayed.
     *
     * @param PreferredWidth The preferred width of the JFrame. If a value equal
     * or below 0 is specified, the default size is set to 640x480.
     *
     * @param PreferredHeight The preferred height of the JFrame. If a value equal
     * or below 0 is specified, the default size is set to 640x480.
     */
    // <editor-fold defaultstate="collapsed" desc="Create Chart">
    private void CreateChart( String XLabel, String YLabel,
                              boolean Legend,
                              int PreferredWidth, int PreferredHeight) {

        // make a new (and empty) XY Series Collection for the XYSeries to be displayed
        // on the left Y axis and add it to the list of all SeriesCollections (at index = 0)
        m_XYSeriesCollections = new ArrayList<XYSeriesCollection>();
        m_XYSeriesCollections.add(new XYSeriesCollection());


        // create the chart
        m_Chart = ChartFactory.createXYLineChart(
                null, //m_Title,
                XLabel, YLabel,
                m_XYSeriesCollections.get(0),   // index 0 refers to the first Y axis
                PlotOrientation.VERTICAL,
                Legend,
                false,      // tooltips
                false       // urls
                );

        // put chart in a new ChartPanel
        // use a Buffer to update the chart off screen (double buffered)
        ChartPanel Panel = new ChartPanel(m_Chart, true);


        // assign preferred size if none was specified
        if (PreferredWidth <= 0 || PreferredHeight <= 0) {
            PreferredWidth = 640;
            PreferredHeight = 480;
        }

        // set preferred size
        Panel.setPreferredSize(new Dimension(PreferredWidth, PreferredHeight));

        // assign the new ChartPanel to the JFrame
        setContentPane(Panel);

        // pack the frame (make it the preferred size of the content pane)
        pack();

        // store a handle to the Plot and Renderer
        m_Plot = (XYPlot) m_Chart.getPlot();

        // store a handle to the Renderer (index = 0)
        m_Renderers = new ArrayList<XYLineAndShapeRenderer>();
        m_Renderers.add( (XYLineAndShapeRenderer)m_Plot.getRenderer(0) );

        // allow panning
        m_Plot.setDomainPannable(true);
        m_Plot.setRangePannable(true);



        ////////////////////////
        // modify the Popup Menu

        // get the PopUp Menu
        JPopupMenu PopUpMenu = Panel.getPopupMenu();

        // add a separator
        PopUpMenu.addSeparator();

        // add a FixedAutoRange menu entry
        m_MenuFixedAutoRange = new JMenuItem("Fixed Auto Range");
        m_MenuFixedAutoRange.addActionListener(this);
        PopUpMenu.add(m_MenuFixedAutoRange);
        
        // add a 'Show/Hide Series' menu entry
        m_MenuShowHideSeries = new JMenu("Show/Hide Series");
        m_MenuShowHideSeries.addActionListener(this);
        PopUpMenu.add(m_MenuShowHideSeries);
        
        // add a separator
        PopUpMenu.addSeparator();
        
        // add a Lin/Log menu entry
        m_MenuLogXAxis = new JCheckBoxMenuItem("log X axis");
        m_MenuLogXAxis.setSelected(false);
        m_MenuLogXAxis.addActionListener(this);
        PopUpMenu.add(m_MenuLogXAxis);
        
        m_MenuLogYAxis = new JCheckBoxMenuItem("log Y axis");
        m_MenuLogYAxis.setSelected(false);
        m_MenuLogYAxis.addActionListener(this);
        PopUpMenu.add(m_MenuLogYAxis);
        


        // TODO 2* rename AutoRange Domain/Range axis to X/Y or Xlabel/YLabel Lin/log axis
        // TODO 4* Lin/Log axis (also set from SweepFthenV)

    }//</editor-fold>


    /**
     * Method that handles the following Action Events sent from the PopUp Menu:<br>
     * - Fixed Auto Range<br>
     * - Show/Hide Series<br><p>
     *
     * To ensure robustness of the iC program, any (unexpected) Exception in this
     * method is caught and disregarded. This might result in a non-responsive
     * PopUp Menu but ensures stability of the iC program.
     *
     * @param e Contains information on the event that fired the action
     */
    // <editor-fold defaultstate="collapsed" desc="action Performed">
    public void actionPerformed(ActionEvent e) {

        try {
            // get the source of the action
            JMenuItem source = (JMenuItem) (e.getSource());


            // check for FixedAutoRange
            // <editor-fold defaultstate="collapsed" desc="Fixed Auto Range">
            if (source == m_MenuFixedAutoRange) {

                // ask user for the new FixedautoRange value
                // build the message for the user
                String msg = "Enter the number of data values that should\n"
                           + "be shown in the Chart. Enter '0' to view\n"
                           + "all data points.";

                // get current FixedAutoRange
                double far = getXAxis().getFixedAutoRange();

                // display the message dialog
                String s = (String)JOptionPane.showInputDialog(
                            null, msg , "Fixed Auto Range",
                            JOptionPane.QUESTION_MESSAGE, null, null,
                            Double.toString(far));

                // evaluate the answer
                if ( s!= null) {
                    try {
                        far = Double.parseDouble(s);
                    } catch (NumberFormatException ex) { /* ignored */ }

                    // set the new FixedAutoRange
                    getXAxis().setFixedAutoRange(far);
                }

                return;
            }//</editor-fold>

            // check for Show/Hide Series:
            // check for all Series' names
            // <editor-fold defaultstate="collapsed" desc="Show/Hide Series">

            // search through all SeriesCollections of all axes
            for (int ic=0; ic < m_XYSeriesCollections.size(); ic++) {

                // get the current SeriesCollection
                XYSeriesCollection  col = m_XYSeriesCollections.get(ic);

                // search all Series in the current SeriesCollection
                for (int is=0; is < col.getSeriesCount(); is++) {
                    if ( source.getText().equals(col.getSeriesKey(is)) ) {
                        // series found, get current state of the CheckBox
                        boolean state = ((JCheckBoxMenuItem)e.getSource()).getState();

                        // set the visibility state for the series
                        m_Renderers.get(ic).setSeriesVisible(is, state);

                        return;
                    }
                }
            }//</editor-fold>

                   
            
            // check for Log X-axis
            // <editor-fold defaultstate="collapsed" desc="Log X axis">
            if (source == m_MenuLogXAxis) {
                                
                // get current state of the CheckBox
                boolean state = ((JCheckBoxMenuItem)e.getSource()).getState();
                
                // change scaling of the axis
                LogXAxis(state);

                return;
            }//</editor-fold>
            
            // check for Log Y-axis
            // <editor-fold defaultstate="collapsed" desc="Log Y axis">
            if (source == m_MenuLogYAxis) {
                                
                // get current state of the CheckBox
                boolean state = ((JCheckBoxMenuItem)e.getSource()).getState();
                
                // change scaling of the (first) Y axis
                LogYAxis(state);
                
                return;
            }//</editor-fold>
            
            
        } catch (Exception ex) {

            String str  = "Warning: An unexpected Exception was caused by the PopUp Menu:\n";
            str += ex.getMessage();

            // log the event
            m_Logger.severe(str);

            // show to the user
            m_GUI.DisplayStatusMessage(str);

            return;
        }
    }//</editor-fold>

    /**
     * Adds a new trace (XYSeries) to the Chart. Allows to choose which Y-axis
     * is used to display the data and also to set Line Style and Marker preferences.<p>
     *
     * Note: Strictly speaking, the <code>SeriesName</code> has to be unique only
     * within the Series' of a Y-axis. But the PopUp Menu that allows to hide
     * and show individual traces also uses the Series Name to identify the trace,
     * it is recommended to use unique <code>SeriesName</code>s over all data series.
     *
     * @param SeriesName The name of the series; must be unique or else the two
     * Series appear as one! JFreeChart uses it as a key and also for
     * the legend. Must not be <code>null</code> or else a dummy value is assigned
     * to prevent an <code>IllegalArgumentException</code> (see JFreeChartManual
     * page 896).
     *
     * @param AxisIndex The index of the axis which should display the data.
     * <code>AxisIndex</code> 0 is always present and refers to the default (left)
     * Y-axis. More axes can be added using <code>newYAxis</code>.
     *
     * @param AutoSort When <code>true</code> the data is automatically sorted
     * by JFreeChar.
     *
     * @param allowDuplicateXValues When <code>true</code> identical x-values are
     * allowed by JFreeChart. I am not sure what happens what happens if an
     * identical x-value is added when set to <code>false</code>.
     *
     * @param LineStyle Specifies the dashing pattern. Use values as defined by
     * <code>BasicStroke</code> or use the predefined values in <code>iC_ChartXY</code>
     * (for instance LINE_DASHED, LINE_SOLID). Can be <code>null</code> in which
     * case JFreeChart uses the default line style (solid line).
     *
     * @param Marker Shape of the Marker. Can be a custom defined <code>Shape</code>
     * or a predefined value in <code>iC_ChartXY</code> (for instance MARKER_CIRCLE).
     * Can be <code>null</code> in which case no marker is used.
     *
     * @return The index of the added <code>XYSeries</code>. Used to address the DataSeries to
     * add new data.
     *
     * @throws IllegalArgumentException when the <code>AxisIndex</code> is out of
     * bounds or the Series could not be added.
     *
     * @see org.jfree.data.xy.XYSeries
     * @see java.awt.BasicStroke
     * @see java.awt.Shape
     * @see org.jfree.util.ShapeUtilities
     */
    // TODO 4* add color to series
    // <editor-fold defaultstate="collapsed" desc="Add XY Series">
    public SeriesIdentification AddXYSeries( String SeriesName,
                                             int AxisIndex,
                                             boolean AutoSort,
                                             boolean allowDuplicateXValues,
                                             float[] LineStyle,
                                             Shape   Marker)
           throws IllegalArgumentException {

        // local variables
        SeriesIdentification SeriesID = new SeriesIdentification();


        // prevent an IllegalArgumentException
        if (SeriesName == null)
            SeriesName = "dummy" + AxisIndex;

        // check if AxisIndex exists
        if (AxisIndex >= m_XYSeriesCollections.size()) {
            String str = "The Axis Index (" + AxisIndex + ") is out of bounds.";
            throw new IllegalArgumentException(str);
        }


        // make a new data series
        XYSeries XYdata = new XYSeries(SeriesName, AutoSort, allowDuplicateXValues);

        // add it to the collection that displays data on the selected axis
        m_XYSeriesCollections.get(AxisIndex).addSeries(XYdata);

        // remember the index of the newly added series
        SeriesID.SeriesNumber = m_XYSeriesCollections.get(AxisIndex).indexOf(XYdata);

        // remember AxisIndex
        SeriesID.AxisIndex = AxisIndex;

        // just show a warning if the series is not found. Should actually never occur
        if (SeriesID.SeriesNumber == -1) {
            String str = "Could not add the Series in iC_ChartXY.AddXYSeries.\n";

            throw new IllegalArgumentException(str);
        }

        // This is an attempt to fix the Stack overflow bug that was possibly
        // introduced after Java update 6.25 (maybe it was always there before)
        int dummy = m_iC_Properties.getInt("ChartXY.MaxDataPoints", 10000);
        if (dummy > 0) {
            
            // set maximum number of data points kept in the graph
            XYdata.setMaximumItemCount(dummy);
            
            // display Status Message
            m_GUI.DisplayStatusMessage("Limiting the number of Data Points kept in series '" 
                    + SeriesName + "' to " + dummy + "\n", false);
        }
        

        /////////////////////////
        // customise the renderer
        // a separat Renderer for each DataSeriesCollection exists

        // get the renderer from the plot
        XYLineAndShapeRenderer Renderer = m_Renderers.get(AxisIndex);

        // set the stroke for the new series
        if (LineStyle == LINE_NONE) {
            Renderer.setSeriesLinesVisible(SeriesID.SeriesNumber, false);
        } else {
            float[] dash = {LineStyle[1], LineStyle[2]};
            Renderer.setSeriesStroke(SeriesID.SeriesNumber, new BasicStroke(LineStyle[0],
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f,
                     dash, 0.0f));
            //http://download.oracle.com/javase/tutorial/2d/geometry/strokeandfill.html

            // do not reset dash-pattern at each point but draw the dashed line as one (see page 599)
            Renderer.setDrawSeriesLineAsPath(true);
        }

        // set the Marker for the new series and make it visible
        if (Marker != null) {
            Renderer.setSeriesShape(SeriesID.SeriesNumber, Marker);
            Renderer.setSeriesShapesVisible(SeriesID.SeriesNumber, true);
        }

        // fill the Markers
        Renderer.setBaseFillPaint(Color.white);
        Renderer.setUseFillPaint(true);

        // draw outlines (lines around the marker symbols
        Renderer.setDrawOutlines(true);


        ///////////////////////////////////////
        // add the new Series to the PopUp Menu
        JCheckBoxMenuItem CheckBox = new JCheckBoxMenuItem(SeriesName);
        CheckBox.setSelected(true);
        CheckBox.addActionListener(this);
        m_MenuShowHideSeries.add(CheckBox);


        // return the ID of the newly added series
        return SeriesID;
    }//</editor-fold>


    /**
     * Adds a new Y-axis to the chart.
     *
     * @param Label The axis label of the new Y-axis
     *
     * @return The AxisIndex of the newly created axis to be used to access the Data
     * Series Collection (<code>m_XYSeriesCollectionArrayList</code>) and Renderer
     * (<code>m_Renderers</code>) associated with the new axis. Mainly used in
     * <code>AddXYSeries</code>.
     */
    // <editor-fold defaultstate="collapsed" desc="newYaxis">
    public int newYaxis(String Label) {

        // get number of axis of the plot
        int NewAxisIndex = m_Plot.getRangeAxisCount();
        m_Logger.log(Level.FINER, "number of axis = {0}", NewAxisIndex);


        ///////////
        // new Axis

        // make a new Axis
        NumberAxis NewAxis = new NumberAxis("Y Axis " + NewAxisIndex);

        // set label
        NewAxis.setLabel(Label);

        // attach it to the plot
        m_Plot.setRangeAxis(NewAxisIndex, NewAxis);
        m_Plot.setRangeAxisLocation(NewAxisIndex, AxisLocation.BOTTOM_OR_RIGHT);

        
        ///////////////////////////
        // new DataSeriesCollection

        // make a new XYDataSeriesCollection that stores the XY series to be
        // displayed on the new axis (index = 1, 2, 3, ...)
        m_XYSeriesCollections.add(new XYSeriesCollection());
        int SeriesIndex = m_XYSeriesCollections.size() - 1;
        m_Logger.log(Level.FINER, "SeriesIndex = {0}", SeriesIndex);

        // add it to the plot
        m_Plot.setDataset(NewAxisIndex, m_XYSeriesCollections.get(NewAxisIndex));
        m_Plot.mapDatasetToRangeAxis(SeriesIndex, NewAxisIndex);


        ///////////////
        // new Renderer

        // make a new Renderer (index = 1, 2, 3, ...)
        XYLineAndShapeRenderer Renderer = new XYLineAndShapeRenderer(true, true);
        m_Renderers.add(Renderer);

        // attach it to the plot and the just added DataseriesCollection
        m_Plot.setRenderer(NewAxisIndex, Renderer);


        // apply the current theme to the new axis (e.g. use the same font size)
        ChartUtilities.applyCurrentTheme(m_Chart);

        // return the new axis index
        return NewAxisIndex;    //old: return SeriesIndex;
    }//</editor-fold>

    /**
     * Adds a new XY Data Point to the specified XYSeries and updates the chart.
     * If <code>allowDuplicateXValues</code> for this series is set to <code>false</code>
     * and an identical x-value is added, the corresponding y-value is updated
     * with the new y-value.<p>
     *
     * When the data point cannot be added to the XYSeries a warning message is 
     * displayed in the GUI. This might render the chart somewhat useless but
     * guarantees the stability of the iC Program. This can be caused when an
     * <code>IllegalArgumentException</code> bubbles up from JFreeChart's
     * <code>XYDataItem.addOrUpdate</code>, or when the <code>SeriesID</code>
     * contains an <code>AxisIndex</code> which is out of bounds, or when a 
     * negative or zero value is added to a log-axis, and possibly some more
     * cases.<p>
     * 
     * This method also manually starts Garbage Collection after displaying 500
     * data points in the hope to prevent the out-of-memory crash (see ChagngeLog 111102)
     *
     * @param SeriesID The identification of the XYSeries to which the new data point should
     * be added. The <code>SeriesID</code> is obtained when a new Series is
     * made with <code>AddXYSeries</code>.
     *
     * @param X The x value of the new data point to display
     * @param Y The y value of the new data point to display
     */
    // <editor-fold defaultstate="collapsed" desc="AddXYDataPoint">
    public void AddXYDataPoint(SeriesIdentification SeriesID, double X, double Y) {

        try {
            // check if AxisIndex exists
            if (SeriesID.AxisIndex >= m_XYSeriesCollections.size()) {
                String str = "Warning: The Axis Index (" + SeriesID.AxisIndex + ") in the SeriesID is out of bounds.";
                throw new IllegalArgumentException(str);
            }
          
            // add the data point
            m_XYSeriesCollections.get(SeriesID.AxisIndex)
                                 .getSeries(SeriesID.SeriesNumber)
                                 .addOrUpdate(X, Y);
            
            // increase counter for Garbage Collection
            m_GCcounter++;
            
            // start GC manually?
            if (m_GCcounter >= 500) {
                // start GC
                System.gc();
                
                // reset counter
                m_GCcounter = 0;
                
                // log event
                m_Logger.finest("Started Garbage Collection\n");
            }

        } catch (Exception ex) {
            String str = "Warning: Could not add the datapoint to the chart.\n"
                    + ex.getMessage() + "\n";

            // log the event
            m_Logger.severe(str);
            m_Logger.log(Level.SEVERE, 
                    "AxisIndex = {0}\tSeriesNumber= {1}\n", 
                    new Object[]{SeriesID.AxisIndex, SeriesID.SeriesNumber});

            // show to the user (it's also logged from there)
            m_GUI.DisplayStatusMessage(str);

            return;
        }
    }//</editor-fold>


    /**
     * Same as {@link #AddXYDataPoints(SeriesIdentification, double, double)} but with arrays as
     * input to display in the chart.<p>
     *
     * When the data point cannot be added to the XYSeries a warning message is
     * displayed in the GUI. This might render the chart somewhat useless but
     * guarantees the stability of the iC Program. This can be caused when 1) an
     * <code>IllegalArgumentException</code> bubbles up from JFreeChart's
     * <code>XYDataItem.addOrUpdate</code> or 2) when the <code>SeriesID</code>
     * contains an <code>AxisIndex</code> which is out of bounds, or 3) when the
     * arrays <code>X</code> and <code>Y</code> have different lengths.
     *
     * @param SeriesID The identification of the XYSeries to which the new data point should
     * be added. The <code>SeriesID</code> is obtained when a new Series is
     * made with <code>AddXYSeries</code>.
     *
     * @param X The x values of the new data points to display
     * @param Y The y values of the new data points to display
     */
    // <editor-fold defaultstate="collapsed" desc="AddXYDataPoints">
    public void AddXYDataPoints(SeriesIdentification SeriesID, double[] X, double[] Y) {

        try {
            // error check
            if (X.length != Y.length) {
                String str = "X and Y have different lenght in iC_ChartXY.AddXYDataPoint.\n";
                throw new IllegalArgumentException(str);
            }

            // check if AxisIndex exists
            if (SeriesID.AxisIndex >= m_XYSeriesCollections.size()) {
                String str = "Warning: The Axis Index (" + SeriesID.AxisIndex + ") in the SeriesID is out of bounds.";

                throw new IllegalArgumentException(str);
            }

            // disable event notification upon adding new data points
            m_Chart.setNotify(false);

            // add all data points
            for (int i=0; i < X.length; i++) {
                
                // enable updating before adding the last data point
                // apparently, this could also be done after the for loop (YokogawaDL9000.SaveWaveform)
                if (i == X.length-1) {
                    m_Chart.setNotify(true);
                }
                
                // add the data point
                m_XYSeriesCollections.get(SeriesID.AxisIndex)
                                     .getSeries(SeriesID.SeriesNumber)
                                     .addOrUpdate(X[i], Y[i]);
            }
            
            // increase counter for Garbage Collection (GC is started in the other AddXYDataPoints)
            m_GCcounter += X.length;

        } catch (Exception ex) {
            String str = "Warning: Could not add the array of datapoints to the chart.\n"
                    + ex.getMessage() + "\n";

            // log the event
            m_Logger.severe(str);
            m_Logger.log(Level.SEVERE, 
                    "AxisIndex = {0}\tSeriesNumber= {1}\n", 
                    new Object[]{SeriesID.AxisIndex, SeriesID.SeriesNumber});

            // show to the user
            m_GUI.DisplayStatusMessage(str);

            return;
        } 
    }//</editor-fold>
    

    /**
     * Saves the Chart in a .png file. If an <code>IOException</code> occurs a
     * Warning message is displayed in the Status Field of the GUI.<p>
     *
     * If either <code>Width</code> or <code>Height</code> is 0 the default
     * values are used.
     *
     * @param file The file so save the Chart to.
     * @param Width of the graph.
     * @param Height of the graph.
     *
     * @throws IOException When saving failed (bubbles up from JFreeChart)
     *
     */
    // <editor-fold defaultstate="collapsed" desc="Save as PNG">
    public void SaveAsPNG(File file, int Width, int Height) 
            throws IOException {

        // check if a size was specified
        if (Width == 0 || Height == 0) {
            // set to default values
            Width = 640;
            Height = 480;
        }

        // save the chart
        try {
            ChartUtilities.saveChartAsPNG(file, m_Chart, Width, Height);
            
        } catch (Exception ex) {    // was IOException but it crashed in one instance, maybe some other exeption will get thrown

            String str = "Warning: Could not save the '" + m_Title + "' Chart as "
                    + "a .png file.\n" + ex.getMessage() + "\n";

            // log event
            m_Logger.log(Level.WARNING, str);

            throw new IOException(str);
        }
    }//</editor-fold>


    /**
     * Returns a handle to the Y-axis (Range axis). Can be used to manipulate the axis in
     * more specialized tasks; see the javadoc entry for this class for some
     * useful examples.
     *
     * @param AxisIndex The index of the axis for which the handle should be returned.
     * <code>AxisIndex</code> 0 is always present and refers to the default (left)
     * Y-axis. More axes can be added using <code>newYAxis</code>. The
     * <code>AxisIndex</code> might be obtained from a <code>SeriesIdentification</code>
     * object obtained from <code>AddXYSeries</code>.
     *
     * @return A <code>NumberAxis</code> object to access the Range Axis. If the
     * type of the axis is not <code>NumberAxis</code> (which should never occur
     * because XYPlots always have <code>NumberAxis</code>') or the <code>AxisIndex</code>
     * is out of bounds, a new and empty <code>NumberAxis</code> object is returned
     * to prevent <code>null</code> pointer exceptions and avoid <code>try-catch</code> blocks.
     * If such an error occurs the desired result is not obtained, but the stability
     * of the iC Program is guaranteed.
     */
    // <editor-fold defaultstate="collapsed" desc="get Y Axis">
    public NumberAxis getYAxis(int AxisIndex) {

        // local variable
        ValueAxis axis;

        // error check AxisIndex and get the axis
        // (would probably not be necessary, javadoc of JFreeChart is not specific about that)
        if (AxisIndex >= m_Plot.getRangeAxisCount()) {
            axis = null;
        } else {
            // get Range axis
            axis = m_Plot.getRangeAxis(AxisIndex);
        }


        if ( axis != null && axis instanceof NumberAxis ) {
            
            // return the axis
            return (NumberAxis) axis;
            
        } else {
            
            // log event
            m_Logger.warning("Did not find the requested NumberAxis; returning "
                    + "a new instance without meaning.\n");
            
            // return a valid but useless object
            return new NumberAxis();
        }
    }//</editor-fold>

    /**
     * Returns the X-axis (Domain axis). Can be used to manipulate the axis in
     * more specialized tasks; see the javadoc entry for this class for some
     * useful examples.
     *
     * @return A <code>NumberAxis</code> object to access the Range Axis. If the
     * type of the axis is not <code>NumberAxis</code> (which should never occur
     * because XYPlots always have <code>NumberAxis</code>') a new and empty
     * <code>NumberAxis</code> object is returned to prevent <code>null</code>
     * pointer exceptions and avoid <code>try-catch</code> blocks.
     *
     */
    // <editor-fold defaultstate="collapsed" desc="get X Axis">
    public NumberAxis getXAxis() {

        // get Domain (Y) axis
        if ( m_Plot.getDomainAxis() instanceof NumberAxis ) {
            return (NumberAxis) m_Plot.getDomainAxis();
        } else {
            // return a valid, but useless object
            return new NumberAxis();
        }
    }//</editor-fold>


    /**
     * Enables or disables AntiAliasing. Disabling AntiAliasing supposedly speeds
     * up drawing the chart, but doesn't look so good. Currently, AntiAliasing
     * for text and lines are not controlled separately.<p>
     *
     * Switching off AntiAliasing is not recommended as it is much slower than
     * with AntiAliasing switched off, at least on a Mac.
     * 
     * @param AntiAliasing When <code>true</code> AntiAliasing is enabled.
     * @return The old state of AntiAliasing
     */
    // <editor-fold defaultstate="collapsed" desc="set AntiAliasing">
    public boolean setAntiAlias(boolean AntiAliasing) {
        // remember the state of AntiAliasing
        boolean ret = m_Chart.getAntiAlias();

        // set the new state
        m_Chart.setAntiAlias(AntiAliasing);

        // return the old state
        return ret;
    }//</editor-fold>


    /**
     * Enables or disables notification of event listeners. This can be used to
     * update the chart only once when multiple data points are added at the same
     * time.
     *
     * @param NotifyEventListeners When <code>true</code> the registered event
     * listeners are informed, if <code>false</code> no events are sent.
     */
    // <editor-fold defaultstate="collapsed" desc="setNotify">
    public void setNotify(boolean NotifyEventListeners) {
        m_Chart.setNotify(NotifyEventListeners);
    }//</editor-fold>

    /**
     * Changes the scaling of the X axis from linear and logarithmic. This method
     * is also used by <code>actionPerformed</code> when the user selected to 
     * change the scaling via the Popup menu.<p>
     * Because this method can be called from outside this class, the state of the
     * checkbox entry in the popup menu is updated as well.
     * 
     * @param LogAxis If <code>true</code> the X axis is shown in a logarithmic
     * scale, if <code>false</code>, a linear scale is used.
     */
    // <editor-fold defaultstate="collapsed" desc="Log X Axis">
    public void LogXAxis(boolean LogAxis) {
        // get current axis label
        String Title = getXAxis().getLabel();

        if (LogAxis == true) {
            // TODO 2* why can't I cast the LogAxis to a NumberAxis ?!?
            //LogAxis xAxis = new LogAxis(Title);
            
            // make a new log axis
            LogarithmicAxis xAxis = new LogarithmicAxis(Title);

            // allow negative values (whatever this means)
            // can screw up auto ranging
            xAxis.setAllowNegativesFlag(false);

            // show 1E3 tick labels, not 10^3
            xAxis.setExpTickLabelsFlag(true);

            // show the new axis
            m_Plot.setDomainAxis(xAxis);
        } else {
            // make a new lin axis
            NumberAxis xAxis = new NumberAxis(Title);

            // show the new axis
            m_Plot.setDomainAxis(xAxis);
        }
        
        // set the state of the popup menu entry accordingly
        m_MenuLogXAxis.setState(LogAxis);
        
        // apply the current theme to the new axis (e.g. use the same font size)
        ChartUtilities.applyCurrentTheme(m_Chart);
    }//</editor-fold>
    
    /**
     * Changes the scaling of the first Y axis from linear and logarithmic. This method
     * is also used by <code>actionPerformed</code> when the user selected to 
     * change the scaling via the Popup menu.<p>
     * Because this method can be called from outside this class, the state of the
     * checkbox entry in the popup menu is updated as well.
     * 
     * @param LogAxis If <code>true</code> the X axis is shown in a logarithmic
     * scale, if <code>false</code>, a linear scale is used.
     */
    // <editor-fold defaultstate="collapsed" desc="Log Y Axis">
    public void LogYAxis(boolean LogAxis) {
        // get current axis label
        String Title = getYAxis(0).getLabel();

        if (LogAxis == true) {
            // make a new log axis
            LogarithmicAxis yAxis = new LogarithmicAxis(Title);

            // allow negative values (whatever this means)
            // it also screws up the AutoRanigning for small numbers (e.g. 1e-6)
            yAxis.setAllowNegativesFlag(false);

            // show 1E3 tick labels, not 10^3
            yAxis.setExpTickLabelsFlag(true);
            
            // show the new axis
            m_Plot.setRangeAxis(yAxis);

        } else {
            // make a new lin axis
            NumberAxis yAxis = new NumberAxis(Title);

            // show the new axis
            m_Plot.setRangeAxis(yAxis);
        }
        
        // set the state of the popup menu entry accordingly
        m_MenuLogYAxis.setState(LogAxis);
        
        // apply the current theme to the new axis (e.g. use the same font size)
        ChartUtilities.applyCurrentTheme(m_Chart);
    }//</editor-fold>
    
    
    /**
     * Removes all XY Data Points from the specified XYSeries and updates the 
     * chart.<p>
     *
     * @param SeriesID The identification of the XYSeries from which all data 
     * points should be removed. The <code>SeriesID</code> is obtained when a 
     * new Series is made with <code>AddXYSeries</code>.
     */
    // <editor-fold defaultstate="collapsed" desc="Clear Series">
    public void ClearSeries(SeriesIdentification SeriesID) {

        try {
            // check if AxisIndex exists
            if (SeriesID.AxisIndex >= m_XYSeriesCollections.size()) {
                String str = "Warning: The Axis Index (" + SeriesID.AxisIndex + ") in the SeriesID is out of bounds.";
                throw new IllegalArgumentException(str);
            }

            // clear the data points
            m_XYSeriesCollections.get(SeriesID.AxisIndex)
                                 .getSeries(SeriesID.SeriesNumber)
                                 .clear();

        } catch (IllegalArgumentException ex) {
            String str = "Warning: Could not clear the datapoints in the chart.\n"
                    + "AxisIndex = " + SeriesID.AxisIndex + "\n"
                    + "SeriesNumber= " +SeriesID.SeriesNumber + "\n"
                    + ex.getMessage() + "\n";

            // log the event
            m_Logger.severe(str);

            // show to the user (it's also logged from there)
            m_GUI.DisplayStatusMessage(str);

            return;
        }
    }//</editor-fold>
    
    
    /**
     * Shows or hides lines at zero for the X and the first(?) Y axis.
     * 
     * @param XAxis If set to <code>true</code>, a line a x=0 will be shown
     * @param YAxis If set to <code>true</code>, a line a y=0 will be shown
     */
    // <editor-fold defaultstate="collapsed" desc="ShowZeroLines">
    public void ShowZeroLines(boolean XAxis, boolean YAxis) {
        
        // show/hide the line at zero for the X Axis
        m_Plot.setDomainZeroBaselineVisible(XAxis);
        
        // show/hide the line at zero for the Y Axis
        m_Plot.setRangeZeroBaselineVisible(YAxis);
    }//</editor-fold>

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables




}
