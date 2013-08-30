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
package icontrol.drivers.instruments.hp;

import icontrol.AutoGUIAnnotation;
import icontrol.Utilities;
import static icontrol.Utilities.getFloat;
import icontrol.drivers.Device;
import icontrol.iC_Annotation;
import icontrol.iC_ChartXY;
import icontrol.iC_ChartXY.SeriesIdentification;
import icontrol.drivers.Device.CommPorts;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;
import javax.script.ScriptException;

/**
 * Hewlett-Packard 4192A LF Impedance Analyzer.<p>
 *
 * All device commands that the Hewlett-Packard 4192A understands are
 * implemented here.<p>
 *
 * The main body of this class was copied to <code>AgilentE4980A</code>, so
 * if this class is modified, it might be a good idea to modify <code>AgilentE4980A</code>
 * as well.<p>
 * 
 * <h3>Instrument Commands:</h3>
 * <ul>
 *  <li>{@link #Init(int, boolean, int)  }
 *  <li>{@link #SweepFthenV(double, double, double, boolean, double, double, double, boolean, int, String) }
 *  <li>{@link #Trigger() }
 *  <li>{@link #setAC_Amplitude(double) }
 *  <li>{@link #setSpotBias(double) }
 *  <li>{@link #setSpotFrequency(double) }
 * </ul>
 *
 * @author KPP (Kurt Pernstich: pernstich@alumni.ethz.ch)
 * @version 0.4
 *
 */

// promise that this class supports GPIB communication
@iC_Annotation( CommPorts=CommPorts.GPIB,
                InstrumentClassName="HP 4192")
public class HP4192 extends Device {


    ///////////////////
    // member variables

    /**
     * The Logger for this class. Note that the Logger Level is set in the
     * constructor of the <code>Device</code> class.
     */
    private static final Logger m_Logger = Logger.getLogger("iC.Instruments.HP4192");
    
    // define a new data type for the returned answer
    // Apache's Common Math would also offer complex numbers
    // <editor-fold defaultstate="collapsed" desc="MValue">
    private class MValue {

        // complex number
        private float   Z = Float.NaN;
        private float   Phi = Float.NaN;

        private String StatusA, StatusB;
        private String FunctionA, FunctionB;
        private String DeviationA, DeviationB;
        private String UnitC, ValueC;

    }//</editor-fold>

    // flag to remember if Init() was called
    private boolean m_Initialized = false;


    /**
     * Override the default <code>Open</code> method in the <code>Device</code>
     * class because this Instrument does not support IEEE 488.2 calls which
     * are used in the default implementation.<p>
     * 
     * It is convenient to clear the queue here.
     * 
     * @throws IOException if the queue could not be cleared; bubbles up from
     * <code>ClearQueue</code>.
     */
    //// <editor-fold defaultstate="collapsed" desc="Open">
    @Override
    public void Open()
           throws IOException {

        // clear the queue, just in case
        ClearQueue();
    }//</editor-fold>

    /**
     * Initialize the instrument.<p>
     *
     * This method performs a Syntax-Check.
     *
     * @param Range The Z/Y measurement range. Can be 1 to 8.
     * @param Averaging Determines if averaging should be turned on or off.
     * @param CircuitMode Determines whether serial or parallel circuit mode 
     * should be used; see ToolTips for the explanation.
     * 
     * @throws IOException re-thrown from <code>SendToInstrument</code>   
     * @throws DataFormatException when the Syntax Check failed.
     */
    // <editor-fold defaultstate="collapsed" desc="Init">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Initializes the Instrument.<br>Should be called once in every Script before making measurements.</html>",
        ParameterNames = {"Measurement Range { [1,8] }", "Averaging", "Circuit mode {1,2,3} "},
        DefaultValues = {"8", "false", "1"},
        ToolTips = {"<html>1: 1 Ohm/10 S<br>7: 1MOhm/10uS<br>8: Auto</html>", "",
                    "<html>1: Auto<br>2: Serial<br>3: Parallel."})
    @iC_Annotation(  MethodChecksSyntax = true )
    public void Init(int Range, boolean Averaging, int CircuitMode)
           throws IOException, DataFormatException {


        ///////////////
        // Syntax-Check

        // Syntax check for Range
        if ( Range < 1 || Range > 8 ) {
            String str = "The Measurement Range must be between 1 and 8.\n";

            throw new DataFormatException(str);
        }

        // Syntax check for Circuit Mode
        if ( CircuitMode < 1 || CircuitMode > 3 ) {
            String str = "The CircuitMode must be between 1 and 3.\n";

            throw new DataFormatException(str);
        }

        // remember that Init was called
        m_Initialized = true;

        // return if in Syntax-check mode
        if (inSyntaxCheckMode())
            return;

        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return;


        // clear the output queue (service the Instrument if service is requested)
        ClearQueue();

        // wait a bit (computer is faster than HP4192)
        try { Thread.sleep(350); } catch (InterruptedException ex) {}


        // Display |Z|, phi
        String dummy = "A1B1";

        // *** Bias on
        dummy += "I1";

        // XY Recorder off
        dummy += "X0";

        // log sweep off
        dummy += "G0";

        // circuit mode auto (paralles/serial)
        dummy += "C" + CircuitMode;

        // ZY Range (1..7, 8=Auto)
        dummy += "R" + Integer.toString(Range);

        // High Speed off
        dummy += "H0";

        // Average off(0=normal)/on(1=average)
        dummy += "V" + (Averaging ? "1" : "0");

        // Trigger hold/manual
        dummy += "T3";

        // Data ready (send SRQ)
        dummy += "D1";

        // Data format A/B/C
        dummy += "F1";

        // SelfTest
        //dummy += "S1";

        // debugging log
        m_Logger.finer("Will be sending Init Strings.\n");

        // send to device
        SendToInstrument(dummy);

        // in the original C++ program I serviced the device here

    }//</editor-fold>



    /**
     * Checks if the Instrument requests service, and if it does, the Instrument
     * is addressed to talk so that the unfetched data is read to empty the
     * output buffer of the 4192 and end the service request.<p>
     *
     * NOTE: Receiving the Status Byte is only implemented when using driver classes
     * derived from <code>GPIB_Driver</code> and an Exception is thrown if some other
     * driver is used (at the time of writing, no such driver exists).
     *
     * @throws IOException When GPIB communication caused an error; bubbles up
     * from <code>QueryInstrument</code>.
     */
    // <editor-fold defaultstate="collapsed" desc="Clear Queue">
    private void ClearQueue()
            throws IOException {

        // return if in No-Communication Mode
        if (inNoCommunicationMode())
            return;

        // check if a driver class derived from GPIB_Driver is used
        if (m_GPIB_Driver != null) {

            // debugging log
            m_Logger.finest("Entering ClearQueue.\n");

            // get the Status Byte
            int Status = m_GPIB_Driver.ReadStatusByte();

            // debugging log
            m_Logger.log(Level.FINER, "Status Byte = {0}\n", Integer.toString(Status));

            // check for an error
            if ( (Status & 0x40) > 0) {
                // log event
                m_Logger.fine("The 4192 requested service. Will now read from buffer.\n");

                // Instrument is requesting service, so read from the Instrument
                //m_GPIB_Driver.Receive(false);
                QueryInstrument("");
            } else {
                // log event
                m_Logger.fine("The 4192 did not request service\n");
            }

            // get the Status Byte again
            Status = m_GPIB_Driver.ReadStatusByte();

            // debugging log
            m_Logger.log(Level.FINER, "Status Byte = {0}\n", Integer.toString(Status));

            // check for errors
            if ( (Status & 0x40) > 0) {
                // just log the event
                m_Logger.warning("The 4192 is still requesting service\n");
            }
        } else {
            String str = "It seems a new driver has been implemented but not been\n"
                    + "considered in HP4192.ClearQueue(). Plese tell the developer.\n";

            // log event
            m_Logger.severe(str);

            // show to the user
            //icontrol.m_GUI.DisplayStatusMessage(str);
            throw new IOException(str);
        }
    }//</editor-fold>


    /**
     * Sets the oscillation amplitude.<p>
     *
     * This method performs a Syntax-Check.
     *
     * @param ACLevel The oscillation (AC) amplitude used to measure the 
     * impedance; only 4 digits after the comma are relevant.
     *
     * @throws IOException re-thrown from <code>SendToInstrument</code>
     *
     * @throws DataFormatException when the Syntax Check failed.
     */
    // <editor-fold defaultstate="collapsed" desc="set AC Amplitude">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Sets the oscillation amplitude.</html>",
        ParameterNames = {"AC amplitude [V] { [0.005,1.1] }"},
        DefaultValues = {"0.1"},
        ToolTips = {""})
    @iC_Annotation(  MethodChecksSyntax = true )
    public void setAC_Amplitude(double ACLevel)
           throws IOException, DataFormatException {


        ///////////////
        // Syntax-Check

        // Syntax check for max. AC level
        if ( ACLevel < 0.005 || ACLevel > 1.1 ) {
            String str = "The AC amplitude must be between 5 mV and 1.1 V.\n";

            throw new DataFormatException(str);
        }

        // return if in Syntax-check mode
        if (inSyntaxCheckMode())
            return;

        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return;


        // build the string
        String dummy = String.format(Locale.US, "OL%+4.4fEN", ACLevel);

        // send to device
        SendToInstrument(dummy);
  
    }//</editor-fold>


    /**
     * Sets the spot frequency.<p>
     *
     * This method performs a Syntax-Check.
     *
     * @param Frequency Sets the measurement (spot) frequency; only 4 digits
     * after the comma are relevant.
     *
     * @throws IOException re-thrown from <code>SendToInstrument</code>
     *
     * @throws DataFormatException when the Syntax Check failed.
     */
    // <editor-fold defaultstate="collapsed" desc="set Spot Frequency">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Sets the spot frequency.</html>",
        ParameterNames = {"Spot Frequency [kHz] { [0.005, 13000] }"},
        DefaultValues = {"1"},
        ToolTips = {""})
    @iC_Annotation(  MethodChecksSyntax = true )
    public void setSpotFrequency(double Frequency)
           throws IOException, DataFormatException {


        ///////////////
        // Syntax-Check

        // Syntax check for max. frequency
        if ( Frequency < 0.005 || Frequency > 13000 ) {
            String str = "The Spot Frequency must be between 0.005 and 13000 kHz.\n";

            throw new DataFormatException(str);
        }

        // return if in Syntax-check mode
        if (inSyntaxCheckMode())
            return;

        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return;


        // build the string
        String dummy = String.format(Locale.US, "FR%+4.4fEN", Frequency);

        // send to device
        SendToInstrument(dummy);
        
    }//</editor-fold>


    /**
     * Sets the DC (spot) bias.<p>
     *
     * This method performs a Syntax-Check.
     *
     * @param DCBias Sets the DC (spot) bias; only 4 digits after the comma are
     * relevant.
     *
     * @throws IOException re-thrown from {@link Device#SendToInstrument}
     *
     * @throws DataFormatException when the Syntax Check failed.
     */
    // <editor-fold defaultstate="collapsed" desc="set Spot Bias">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Sets the DC (spot) bias.</html>",
        ParameterNames = {"Spot Bias [V] { [-35, 35] }"},
        DefaultValues = {"0"},
        ToolTips = {""})
    @iC_Annotation(  MethodChecksSyntax = true )
    public void setSpotBias(double DCBias)
           throws IOException, DataFormatException {


        ///////////////
        // Syntax-Check

        // Syntax check for max. bias
        if ( DCBias < -35 || DCBias > 35 ) {
            String str = "The Spot Bias must be between -35 and 35 V.\n";

            throw new DataFormatException(str);
        }


        // return if in Syntax-check mode
        if (inSyntaxCheckMode())
            return;

        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return;


        // build the string
        String dummy = String.format(Locale.US, "BI%+4.4fEN", DCBias);

        // send to device
        SendToInstrument(dummy);

    }//</editor-fold>


    /**
     * Triggers the device to make a measurement.<p>
     *
     * @throws IOException re-thrown from {@link Device#SendToInstrument}
     */
    // <editor-fold defaultstate="collapsed" desc="Trigger">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Triggers the device to make a measurement.</html>",
        ParameterNames = {},
        DefaultValues = {},
        ToolTips = {})
    public void Trigger()
           throws IOException {


        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return;

        // send to device
        SendToInstrument("EX");

    }//</editor-fold>

    /**
     * Triggers a measurement and reads the Instrument's response. The method returns
     * with the default values for <code>ret</code> (Z and phi are NaN) if the 
     * received answer was not as expected. The returned value is always an impedance
     * value (Z) even if the circuit mode was parallel.
     *
     * @throws IOException re-thrown from <code>SendToInstrument</code>
     * @throws ScriptException re-thrown from <code>Utilities.getFloat</code>
     * @return Returns an <code>MValue</code> object with the measured Z and phi
     * values or NaN if the measurement was invalid> The returned value always contains
     * an impedance value (Z) even if the circuit mode is parallel (Y is converted
     * to Z in this case).
     */
    // <editor-fold defaultstate="collapsed" desc="Measure">
    private MValue Measure()
            throws IOException, ScriptException {

        /* the original C++ code waited until the Request Status (SRQ) was
         * cleared before proceeding. It did not, however, undertake any
         * measures to clear the status request, so I doubt that it was
         * very useful.
         */

        // local variables
        MValue ret = new MValue();

        // return something useful while debugging
        if (false) {
            //ret.Valid = true;
            ret.Z = -10.0f * (float)Math.random();
            ret.Phi = Math.random() > 0.5 ? Float.NaN : 360 * (float)Math.random();
            return ret;
        }

        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return ret;

            
        // test to clear the queue before reading
        //ClearQueue();


        // read data from the instrument (see manual page 151 (3-68)
        String ans = QueryInstrument("EX");    // add \n?


        // for debugging: simulate the answer to debug String spliting
        //ans = "NZFN+0.8200E+06,NTDN-001.00E+00,K+01000.000";
        //ans = "NZFN+321.21E+21,NZFN+321.21E+21,K+54321.321\r\n";
        //                 1         2         3         4
        //       01234567890123456789012345678901234567890123 4


        // check if answer has the proper lengh
        if (ans.length() != 43) {
            m_Logger.finer("answer does not have the expected length (GPIB transmission error?) - returning.\n");
            return ret;
        }


        // split the String
        ret.StatusA = ans.substring(0, 1);
        ret.FunctionA = ans.substring(1, 3);
        ret.DeviationA = ans.substring(3, 4);
        ret.Z = getFloat(ans.substring(4, 15));

        ret.StatusB = ans.substring(16, 17);
        ret.FunctionB = ans.substring(17, 19);
        ret.DeviationB = ans.substring(19, 20);
        ret.Phi = getFloat(ans.substring(20, 31));

        ret.UnitC = ans.substring(32, 33);
        ret.ValueC = ans.substring(33, 43);

        // convert Y to Z if Circuit Mode was parallel
        if (ret.FunctionA.equalsIgnoreCase("YF")) {
            if (ret.Z != 0 ) {
                ret.Z = 1 / ret.Z;
            }
            ret.Phi = -ret.Phi;
        }

        // check if Z was valid
        if ( !ret.StatusA.equalsIgnoreCase("N") )
            ret.Z = Float.NaN;

        // check if phi was valid
        if ( !ret.StatusB.equalsIgnoreCase("N") )
            ret.Phi = Float.NaN;

        // log answer for debugging
        m_Logger.log(Level.FINER, "{0} -- length={1}\n", new Object[]{
            ans, Integer.toString(ans.length())});


        // return the answer
        return ret;
        
    }//</editor-fold>


    /**
     * Sweeps the frequency for all given DC biases and stores the impedance.
     *
     * @param FreqStart Start Frequency
     * @param FreqStop Stop Frequency
     * @param FreqInc Increment in Frequency respectively number of points per 
     * decade. See <code>Utilities.GenerateValues</code>
     * @param FreqLog When <code>false</code> the frequency points are equally spaced;
     * if <code>true</code> they are logarithmically spaced. See <code>GenerateValues</code>
     * @param VStart First DC bias
     * @param VStop Last DC bias
     * @param VInc Increment in the DC bias respectively number of points per
     * decade (see <code>GenerateValues</code>).
     * @param VLog When <code>false</code> the voltage points are equally spaced;
     * if <code>true</code> they are logarithmically spaced. See <code>GenerateValues</code>
     * @param VDelay Time delay after applying the dC bias and starting the
     * frequency sweep.
     * @param FileExtension This String is appended to the File Name. If the
     * filename already exists, a new file with a counter is inserted between the 
     * FileName and the FileExtension.
     *
     * @throws ScriptException When generating the voltage or frequency values
     * failed (bubbles up from <code>GenerateValues</code>).
     * @throws IOException When communication with the Instrument failed (bubbled
     * up from <code>setSpotBias</code> or <code>setSpotFrequency</code>.
     * @throws DataFormatException when the Syntax Check failed.
     */
    // <editor-fold defaultstate="collapsed" desc="sweepFthenV">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Measures the impedance for frequency<br>sweeps at each DC bias volatge.</html>",
        ParameterNames = {"Start Frequency [kHz] { [0.005, 13000] }", "Stop Frequency [kHz] { [0.005, 13000] }",
                          "Frequency Increment","logarithmic Freq. spacing?",
                          "Start Bias [V] { [-35, 35] }", "Stop Bias [V] { [-35, 35] }",
                          "Bias Increment [V]", "logarithmic Bias spacing?", "Delay time [msec]", "File Extension"},
        DefaultValues = {"1", "100", "1", "false",
                         "-0.5", "0.5", "1", "false",
                         "500", ".CV"},
        ToolTips = {"", "", "<html>Frequency difference or number of steps per decade<br>if logarithmic sweep is selected</html>", 
                    "", "", "", "<html>Voltage difference or number of steps per decade<br>if logarithmic sweep is selected</html>", "",
                    "<html>Time waited after applying the DC bias and<br>starting the frequency sweep.</html>", ""})
    @iC_Annotation(  MethodChecksSyntax = true )
    public void SweepFthenV(double FreqStart, double FreqStop, double FreqInc, boolean FreqLog,
                            double VStart, double VStop, double VInc, boolean VLog,
                            int VDelay, String FileExtension)
                throws ScriptException, IOException, DataFormatException {

        //////////////////
        // local variables

        // define structure to store a measurement point
        // <editor-fold defaultstate="collapsed" desc="Storage">
        class Storage {
            public double Freq;    // frequency
            public double Z;       // Impedance (amount)
            public double Phi;     // phase angle
            public double V;       // DC bias

            // empty constructor
            // required when a copy constructor is defined
            Storage() {
                this.Freq = 0.0;
                this.Z = 0.0;
                this.Phi = 0.0;
                this.V = 0.0;
            }
            
            // copy constructor
            // used to create a new object to be stored in the Result's ArrayList
            Storage(Storage val) {
                this.Freq = val.Freq;
                this.Z = val.Z;
                this.Phi = val.Phi;
                this.V = val.V;
            }
        }//</editor-fold>



        // intermediate storage for one data point
        Storage DataPoint = new Storage();

        // store all measurements here
        ArrayList<Storage> Results = new ArrayList<Storage>();

        // holds the chart
        iC_ChartXY  Chart;
        
        // Identification (handle) of the Data Series
        SeriesIdentification SeriesID_Z,
                             SeriesID_Phi;



        ///////////////
        // Syntax-Check

        // Syntax check for max. bias
        if ( inSyntaxCheckMode() ) {
            // call appropriate functions that perform the Syntax check
            setSpotBias(VStart);
            setSpotBias(VStop);
            setSpotFrequency(FreqStart);
            setSpotFrequency(FreqStop);

            // check if Init was called
            if (m_Initialized == false) {
                throw new DataFormatException("Please use the Script-Command Init before using SweepFthenV.\n");
            }
        }

        // return if in Syntax-check mode
        if (inSyntaxCheckMode())
            return;

        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return;


        // ensure that VDelay is greater than 0
        if (VDelay < 1)
            VDelay = 1;

        // make new Utilities object
        Utilities util = new Utilities();
        
        // generate Vbias values with 4 digits after the comma
        ArrayList<Double> V = util.GenerateValues(VStart, VStop, VInc, VLog, null, 4);

        // generate Frequency values
        ArrayList<Double> F = util.GenerateValues(FreqStart, FreqStop, FreqInc, FreqLog, null, 4);


        // make a new XYChart object
        Chart = new iC_ChartXY("Sweep f then V",
                                 "f [ kHz ]",
                                 "|Z| [ Ohm or S ]",
                                 true  /*legend*/,
                                 640, 480);

        // add second Y axis for Phi
        int PhiAxisIndex = Chart.newYaxis("phi [deg]");
        
        // change scaling of X axis to log if log-frequency sweep
        if (FreqLog) {
            Chart.LogXAxis(true);
        }

        
        // Test third axis
        //int ThirdAxisIndex = Chart.newYaxis("testing");
        

        
        // sweep through all Voltages
        sweep:          // label for the break statment
        for (double v : V) {

            // add a new data series
            String SeriesName = String.format("DC%.4f", v);
            // |Z| axis (left)
            SeriesID_Z = Chart.AddXYSeries(SeriesName + "_Z", 0, false, true,
                                             Chart.LINE_SOLID, Chart.MARKER_CIRCLE);

            // phi axis (right)
            SeriesID_Phi = Chart.AddXYSeries(SeriesName + "_Phi", PhiAxisIndex, false, true,
                                             Chart.LINE_DASHED, Chart.MARKER_DIAMOND);

            // Test third axis
            //SeriesIdentification SeriesID_Third;
            //SeriesID_Third = Chart.AddXYSeries(SeriesName + "_3rd", ThirdAxisIndex, false, true,
            //                             Chart.LINE_NONE, Chart.MARKER_SQUARE);
            

            // set the spot (DC) voltage
            setSpotBias(v);


            // wait a bit
            try { Thread.sleep(VDelay*1000); } catch (InterruptedException ex) {}

            // sweep through all frequencies
            for (double f : F) {

                // check Stop button
                if (m_StopScripting)
                    break sweep;

                // check for Pause button
                isPaused(true);

                // set the spot frequency
                setSpotFrequency(f);

                // measure
                MValue data = Measure();

                // to debug
                if (false){
                    data.Z = (float) v;
                }

                // store the frequency and bias
                DataPoint.Freq = f;
                DataPoint.V = v;
                
                // store measured value if valid or not
                // (Z and Phi are always double values because Measure() throws a ScriptException
                // if the answer cannot be converted to double
                DataPoint.Z = data.Z;
                DataPoint.Phi = data.Phi;
                

                // plot the impedance if valid

                // add the datapoint to the graph
                //Chart.AddXYDataPoint(SeriesNumber, DataPoint.Freq, DataPoint.Z);
                Chart.AddXYDataPoint(SeriesID_Z, DataPoint.Freq, DataPoint.Z);
                Chart.AddXYDataPoint(SeriesID_Phi, DataPoint.Freq, DataPoint.Phi);

                // Test third axis
                //Chart.AddXYDataPoint(SeriesID_Third, DataPoint.Freq, DataPoint.Freq);


                // store in the list
                Results.add(new Storage(DataPoint) );
            }
        }

        // done measuring, save the data

        // get the filename
        String FileName = m_GUI.getFileName(FileExtension);


        // open the file
        BufferedWriter FWriter;
        try {
            FWriter = new BufferedWriter(new FileWriter(FileName));

            // write header line
            FWriter.write("f [kHz?]\t|Z| [Ohm]\tphi [deg]\tVbias [V]\n");

            // save data
            for (Storage s : Results) {

                // build one line
                String dummy = String.format(Locale.US, "%f\t%f\t%f\t%f\n",
                        s.Freq, s.Z, s.Phi, s.V);

                // store to file
                FWriter.write(dummy);
            }

            // close the file
            FWriter.close();

        } catch (IOException ex) {
            m_Logger.log(Level.SEVERE, "Error writing to the file in SweepFthenV", ex);

            // show a dialog
            String str = "Could not open/write/close the file\n";
            str += FileName + "\n\n";
            str += ex.getMessage();

            throw new ScriptException(str);
        }
        
        
        ////////////////////////
        // save the chart as png
        
        // make the file
        File file = new File( m_GUI.getFileName(FileExtension + ".png") );
        
        // save the chart
        try {
            Chart.SaveAsPNG(file, 0, 0);
        } catch (IOException ignore) {
            m_GUI.DisplayStatusMessage("Could not save the Z(phi) chart.\n");
        }
        
    }//</editor-fold>


    /**
     * "Cleans-up" the Instrument after processing the script has been finished.
     * It sets the Spot-Bias to 0.
     * 
     * @throws IOException bubbles up from <code>SendToInstrument</code>
     */
    // <editor-fold defaultstate="collapsed" desc="Close">
    @Override
    public void Close() 
           throws IOException {
        
        try {
            // set any possible bias to zero
            setSpotBias(0.0);
        
        } catch (DataFormatException ignore) {
            /* cannot happen because 0.0 is valid */;
        }
    }// </editor-fold>
}
