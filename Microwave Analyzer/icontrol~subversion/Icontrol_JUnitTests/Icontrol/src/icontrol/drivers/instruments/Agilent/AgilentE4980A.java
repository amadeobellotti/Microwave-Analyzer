// TODO 4* fix that Y2 axis format, include 0 in auto range does not work

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
package icontrol.drivers.instruments.Agilent;

import icontrol.AutoGUIAnnotation;
import icontrol.Utilities;
import static icontrol.Utilities.getFloat;
import static icontrol.Utilities.getInteger;
import icontrol.drivers.Device;
import icontrol.iC_Annotation;
import icontrol.iC_ChartXY;
import icontrol.iC_ChartXY.SeriesIdentification;
import icontrol.drivers.Device.CommPorts;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;
import javax.script.ScriptException;

/**
 * Agilent E4980A Precision LCR Meter.<p>
 *
 * All device commands that the Agilent E4980A understands are
 * implemented here.<p>
 * 
 * The main body of this class was copied and adapted from <code>HP4192</code>, so
 * if this class is modified, it might be a good idea to modify <code>HP4192</code>
 * as well.<p>
 *
 * <h3>Instrument Commands:</h3>
 * <ul>
 *  <li>{@link #SweepFthenV(double, double, double, boolean, double, double, double, boolean, int, String) }
 *  <li>{@link #Trigger() } (not used, so it's commented out)
 *  <li>{@link #configCircuitMode(String) }
 *  <li>{@link #setAC_Amplitude(double) }
 *  <li>{@link #setDCBias(double) }
 *  <li>{@link #setFrequency(double) }
 * </ul>
 *
 * @author KPP (Kurt Pernstich: pernstich@alumni.ethz.ch)
 * @version 0.1
 *
 */

// promise that this class supports GPIB communication
@iC_Annotation( CommPorts=CommPorts.GPIB,
                InstrumentClassName="Agilent E4980A")
public class AgilentE4980A extends Device {


    ///////////////////
    // member variables

    /**
     * The Logger for this class. Note that the Logger Level is set in the
     * constructor of the <code>Device</code> class.
     */
    private static final Logger m_Logger = Logger.getLogger("iC.Instruments.AgilentE4980A");
    
    // define a new data type for the returned answer
    // Apache's Common Math would also offer complex numbers
    // <editor-fold defaultstate="collapsed" desc="MValue">
    private class MValue {

        // complex number
        private float   Value1 = Float.NaN; // e.g. Z
        private float   Value2 = Float.NaN; // e.g. phi

    }//</editor-fold>

    /** Defines the allowed circuit modes, e.g. Cp-D, Cp-Q, ... */
    private static final List<String> CIRCUIT_MODES =
        Arrays.asList("CPD", "CPQ", "CPG", "CPRP", "CSD", "CSQ", "CSRS", "LPD", 
        "LPQ", "LPG", "LPRP", "LPRD", "LSD", "LSQ", "LSRS", "LSRD", "RX", "ZTD", 
        "ZTR", "GB", "YTD", "YTR", "VDID");

    /**
     * Calls <code>Device.Open</code> and sets the Trigger so that continuous 
     * measurements are made. It also sets the data format for the data received
     * from the Instrument to 10 significant digits, and switches off the 
     * Comparator mode (not sure what the comparator mode is). To set the circuit
     * mode, use <code>configCircuitMode</code>.<p>
     * 
     * @throws IOException Bubbles up from <code>SendToInstrument</code> or from
     * <code>Device.Open</code>.
     * @throws ScriptException when the response to a *IDN? query does not
     * match the expected result; bubbles up from <code>checkIDN</code>.
     */
    // <editor-fold defaultstate="collapsed" desc="Open">
    @Override
    public void Open()
           throws IOException, ScriptException {
        
        // call Superclass' Open method
        super.Open();
        
        // set 10 digit Ascii format for data transmission
        SendToInstrument(":Form:Data ASC");
        SendToInstrument(":Form:Asc:Long ON");
        
        // switch off comparator (not exactly sure what this does)
        SendToInstrument(":Comparator:State OFF");
        
        // select Value1-phi(deg) display mode
        // this format is also used to save the data
        //SendToInstrument(":Function:Impedance:Type ZTD");
        
        // Set Trigger source to internal
        SendToInstrument(":Trigger:Source INTERNAL");
        
        // Start Continuous initiation mode of the Trigger
        SendToInstrument(":Initiate:Continuous ON");

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
    // <editor-fold defaultstate="collapsed" desc="setAC_Amplitude">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Sets the oscillation amplitude.</html>",
        ParameterNames = {"AC amplitude [V] { [100e-6, 20] }"},
        DefaultValues = {"0.1"},
        ToolTips = {""})
    @iC_Annotation(  MethodChecksSyntax = true )
    public void setAC_Amplitude(double ACLevel)
           throws IOException, DataFormatException {


        ///////////////
        // Syntax-Check

        // Syntax check for max. AC level
        if ( ACLevel < 1e-6 || ACLevel > 20 ) {
            String str = "The AC amplitude must be between 100 uV and 20 V.\n";

            throw new DataFormatException(str);
        }

        // return if in Syntax-check mode
        if (inSyntaxCheckMode())
            return;

        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return;


        // build the string
        String dummy = String.format(Locale.US, ":Voltage:Level %+.4f", ACLevel);

        // send to device
        SendToInstrument(dummy);
  
    }//</editor-fold>


    /**
     * Sets the frequency.<p>
     *
     * This method performs a Syntax-Check.
     *
     * @param Frequency Sets the measurement (spot) frequency.
     *
     * @throws IOException re-thrown from <code>SendToInstrument</code>
     * @throws DataFormatException when the Syntax Check failed.
     */
    // <editor-fold defaultstate="collapsed" desc="setFrequency">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Sets the spot frequency.</html>",
        ParameterNames = {"Spot Frequency [Hz] { [20, 20e6] }"},
        DefaultValues = {"1"},
        ToolTips = {""})
    @iC_Annotation(  MethodChecksSyntax = true )
    public void setFrequency(double Frequency)
           throws IOException, DataFormatException {


        ///////////////
        // Syntax-Check

        // Syntax check for max. frequency
        if ( Frequency < 20 || Frequency > 20e6 ) {
            String str = "The Spot Frequency must be between 20 Hz and 20 MHz.\n";

            throw new DataFormatException(str);
        }

        // return if in Syntax-check mode
        if (inSyntaxCheckMode())
            return;

        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return;


        // build the string
        String dummy = String.format(Locale.US, ":Frequency:CW %f", Frequency);

        // send to device
        SendToInstrument(dummy);
        
    }//</editor-fold>


    /**
     * Sets the DC bias. When the specified DC bias is different from 0, then
     * the DC bias output is enabled, else it's disabled.<p>
     *
     * This method performs a Syntax-Check.
     *
     * @param DCBias Sets the DC bias; only 4 digits after the comma are
     * relevant.
     *
     * @throws IOException re-thrown from {@link Device#SendToInstrument}
     * @throws DataFormatException when the Syntax Check failed.
     */
    // <editor-fold defaultstate="collapsed" desc="setDCBias">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Sets the DC (spot) bias.</html>",
        ParameterNames = {"Spot Bias [V] { [-40, 40] }"},
        DefaultValues = {"0"},
        ToolTips = {""})
    @iC_Annotation(  MethodChecksSyntax = true )
    public void setDCBias(double DCBias)
           throws IOException, DataFormatException {


        ///////////////
        // Syntax-Check

        // Syntax check for max. bias
        if ( DCBias < -40 || DCBias > 40 ) {
            String str = "The DC Bias must be between -40 and 40 V.\n";

            throw new DataFormatException(str);
        }


        // return if in Syntax-check mode
        if (inSyntaxCheckMode())
            return;

        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return;


        // build the string
        String dummy = String.format(Locale.US, ":Bias:Voltage:Level %.4f", DCBias);

        // send to device
        SendToInstrument(dummy);
        
        
        
        // switch on/off DC Bias
        if (Math.abs(DCBias) < 1e-6) {
            // switch off DC bias
            SendToInstrument(":Bias:State OFF");
        } else {        
            // switch on DC bias
            SendToInstrument(":Bias:State ON");
        }
        

    }//</editor-fold>


    /**
     * Triggers the device to make a measurement.<p>
     *
     * @throws IOException re-thrown from <code>SendToInstrument</code>
     */
    // <editor-fold defaultstate="collapsed" desc="Trigger">
//    @AutoGUIAnnotation(
//        DescriptionForUser = "<html>Triggers the device to make a measurement.</html>",
//        ParameterNames = {},
//        DefaultValues = {},
//        ToolTips = {})
//    public void Trigger()
//           throws IOException {
//
//
//        // exit if in No-Communication-Mode
//        if (inNoCommunicationMode())
//            return;
//
//        // send to device
//        SendToInstrument(":Trigger:Immediate");
//
//    }//</editor-fold>

    /**
     * Reads a measurement from the Instrument. The method returns
     * with the measured Value1 and phi values or with NaN if the measurement was
     * invalid. The returned value is always an impedance value (Value1) even if the 
     * circuit mode was parallel.
     *
     * @throws IOException re-thrown from <code>SendToInstrument</code>
     * @throws ScriptException re-thrown from <code>Utilities.getFloat</code>
     * @return Returns an <code>MValue</code> object with the measured values of
     * with Nan if the measurement was invalid. The returned value always contains
     * an impedance value (Value1) even if the circuit mode is parallel (Y is converted
     * to Value1 in this case).
     */
    // <editor-fold defaultstate="collapsed" desc="Measure">
    private MValue Measure()
            throws IOException, ScriptException {

        // local variables
        MValue ret = new MValue();

        // return something useful while debugging
        if (false) {
            //ret.Valid = true;
            ret.Value1 = -10.0f * (float)Math.random();
            ret.Value2 = Math.random() > 0.5 ? Float.NaN : 360 * (float)Math.random();
            return ret;
        }

        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return ret;


        // read data from the instrument
        String ans = QueryInstrument(":Fetch:Impedance:Formatted?");
        
        
        // split the ans at the ,
        String Components[] = ans.split(",");
        
        // check length
        if (Components.length != 3) {
            // log event
            m_Logger.severe("The answer did not contain 3 commas\n");
            
            // return an invalid result
            return ret;
        }

        try {
            // get Status
            int Status = getInteger(Components[2]);

            // check status
            if (Status != 0) {
                String StatusMessage = "";
                
                // translate Status (manual page 266)
                switch (Status) {
                    case -1: StatusMessage = "No data (in the data buffer memory)"; break;
                    case 1: StatusMessage = "Overload"; break;
                    case 3: StatusMessage = "Signal source overloaded"; break;
                    case 4: StatusMessage = "ALC (auto level control) unable to regulate"; break;
                    default: StatusMessage = "Unknown Status Number";
                }
                
                // build the String
                String str = "Agilent E4980A: Invalid measurement (" 
                        + StatusMessage + ")\n";
                
                // display a Status message (also logs it)
                m_GUI.DisplayStatusMessage(str, false);
                
                // log event
                //m_Logger.log(Level.SEVERE, str);
                
                // return invalid measurement
                return ret;
            }

            // get Value1
            ret.Value1 = getFloat(Components[0]);

            // get phi
            ret.Value2 = getFloat(Components[1]);
            
        } catch (ScriptException ex) {
            String str = "Could not convert the answer of the Agilent E4980A\n"
                    + "into a number.\n";
            throw new ScriptException(str);
        }
        
        // return the answer
        return ret;
        
    }//</editor-fold>


    /**
     * Sweeps the frequency for all given DC biases and stores the impedance in units
     * set by the circuit mode.
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
     * @param VDelay Time delay after applying the DC bias and starting the
     * frequency sweep.
     * @param FileExtension This String is appended to the File Name. If the
     * filename already exists, a new file with a counter is inserted between the 
     * FileName and the FileExtension.
     *
     * @throws ScriptException When generating the voltage or frequency values
     * failed (bubbles up from <code>GenerateValues</code>).
     * @throws IOException When communication with the Instrument failed (bubbled
     * up from <code>setDCBias</code> or <code>setFrequency</code> or from 
     * <code>QueryInstrument</code>.
     * @throws DataFormatException when the Syntax Check failed.
     */
    // <editor-fold defaultstate="collapsed" desc="sweepFthenV">
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Measures the impedance for frequency<br>sweeps at each DC bias volatge.</html>",
        ParameterNames = {"Start Frequency [Hz] { [20, 20e6] }", "Stop Frequency [Hz] { [20, 20e6] }",
                          "Frequency Increment", "logarithmic Freq. spacing?",
                          "Start Bias [V] { [-40, 40] }", "Stop Bias [V] { [-40, 40] }",
                          "Bias Increment [V]", "logarithmic Bias spacing?", "Delay time [msec]", "File Extension"},
        DefaultValues = {"2000", "3000", "100", "false",
                         "-0.5", "0.5", "1", "false",
                         "250", ".CV.txt"},
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
            public double Value1;  // first value, e.g. Impedance (amount)
            public double Value2;  // second value, e.g. phase angle
            public double V;       // DC bias

            // empty constructor
            // required when a copy constructor is defined
            Storage() {
                this.Freq = 0.0;
                this.Value1 = 0.0;
                this.Value2 = 0.0;
                this.V = 0.0;
            }
            
            // copy constructor
            // used to create a new object to be stored in the Result's ArrayList
            Storage(Storage val) {
                this.Freq = val.Freq;
                this.Value1 = val.Value1;
                this.Value2 = val.Value2;
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
        SeriesIdentification SeriesID_Value1,
                             SeriesID_Value2;



        ///////////////
        // Syntax-Check

        // Syntax check for max. bias
        if ( inSyntaxCheckMode() ) {
            // call appropriate functions that perform the Syntax check
            setDCBias(VStart);
            setDCBias(VStop);
            setFrequency(FreqStart);
            setFrequency(FreqStop);
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

        
        // get current circuit mode
        String CircuitMode = QueryInstrument(":Function:Impedance:Type?");
        
        // build nice circuit mode strings
        String CircuitModeString1, CircuitModeString2;
        if (CircuitMode.equalsIgnoreCase("CPD"))      {CircuitModeString1="Cp [F]"; CircuitModeString2="D [1]";}
        else if (CircuitMode.equalsIgnoreCase("CPQ")) {CircuitModeString1="Cp [F]"; CircuitModeString2="Q [1]";}
        else if (CircuitMode.equalsIgnoreCase("CPG")) {CircuitModeString1="Cp [F]"; CircuitModeString2="G [1/Ohm]";}
        else if (CircuitMode.equalsIgnoreCase("CPRP")){CircuitModeString1="Cp [F]"; CircuitModeString2="Rp [Ohm]";}
        else if (CircuitMode.equalsIgnoreCase("CSD")) {CircuitModeString1="Cs [F]"; CircuitModeString2="D [1]";}
        else if (CircuitMode.equalsIgnoreCase("CSQ")) {CircuitModeString1="Cs [F]"; CircuitModeString2="Q [1]";}
        else if (CircuitMode.equalsIgnoreCase("CSRS")){CircuitModeString1="Cs [F]"; CircuitModeString2="Rs [Ohm]";}
        else if (CircuitMode.equalsIgnoreCase("LPD")) {CircuitModeString1="Lp [H]"; CircuitModeString2="D [1]";}
        else if (CircuitMode.equalsIgnoreCase("LPQ")) {CircuitModeString1="Lp [H]"; CircuitModeString2="Q [1]";}
        else if (CircuitMode.equalsIgnoreCase("LPG")) {CircuitModeString1="Lp [H]"; CircuitModeString2="G [1/Ohm1]";}
        else if (CircuitMode.equalsIgnoreCase("LPRP")){CircuitModeString1="Lp [H]"; CircuitModeString2="Rp [Ohm]";}
        else if (CircuitMode.equalsIgnoreCase("LPRD")){CircuitModeString1="Lp [H]"; CircuitModeString2="Rdc [Ohm]";}
        else if (CircuitMode.equalsIgnoreCase("LSD")){CircuitModeString1="Ls [H]"; CircuitModeString2="D [1]";}
        else if (CircuitMode.equalsIgnoreCase("LSQ")){CircuitModeString1="Ls [H]"; CircuitModeString2="Q [1]";}
        else if (CircuitMode.equalsIgnoreCase("LSRS")){CircuitModeString1="Ls [H]"; CircuitModeString2="Rs [Ohm]";}
        else if (CircuitMode.equalsIgnoreCase("LSRD")){CircuitModeString1="Ls [H]"; CircuitModeString2="Rdc [Ohm]";}
        else if (CircuitMode.equalsIgnoreCase("RX")){CircuitModeString1="R [Ohm]"; CircuitModeString2="X [Ohm]";}
        else if (CircuitMode.equalsIgnoreCase("ZTD")){CircuitModeString1="Z [Ohm]"; CircuitModeString2="phi [deg]";}
        else if (CircuitMode.equalsIgnoreCase("ZTR")){CircuitModeString1="Z [Ohm]"; CircuitModeString2="phi [rad]";}
        else if (CircuitMode.equalsIgnoreCase("GB")){CircuitModeString1="G [1/Ohm]"; CircuitModeString2="B [1/Ohm]";}
        else if (CircuitMode.equalsIgnoreCase("YTD")){CircuitModeString1="Y [1/Ohm]"; CircuitModeString2="phi [deg]";}
        else if (CircuitMode.equalsIgnoreCase("YTR")){CircuitModeString1="Y [1/Ohm]"; CircuitModeString2="phi [rad]";}
        else if (CircuitMode.equalsIgnoreCase("VDID")){CircuitModeString1="Vdc [V]"; CircuitModeString2="Idc [A]";}
        else {
            String str = "The instrument's circuit mode is not recognized.\n"
                    + "Try to use configCircuitMode to set a valid circuit mode.\n";
            throw new ScriptException(str);
            }

        // make a new XYChart object
        Chart = new iC_ChartXY("Sweep f then V",
                               "f [ Hz ]",
                               CircuitModeString1, //  left Y axis label, e.g |Z| [ Ohm ]
                               true  /*legend*/,
                               1024, 768);
        
        // add second Y axis for Value2
        int SecondAxisIndex = Chart.newYaxis(CircuitModeString2); // right Y axis label, e.g. phi [deg]
        
        // change scaling of X axis to log if log-frequency sweep
        if (FreqLog) {
            Chart.LogXAxis(true);
        }
        
        // use nice number format
        Chart.getYAxis(0).setNumberFormatOverride(new DecimalFormat("##0.#####E0"));
        Chart.getYAxis(SecondAxisIndex).setNumberFormatOverride(new DecimalFormat("##0.#####E0"));
        
        // don't include 0 in autoscaling
        Chart.getYAxis(0).setAutoRangeIncludesZero(false);
        Chart.getYAxis(SecondAxisIndex).setAutoRangeIncludesZero(false);

        
        // sweep through all Voltages
        sweep:          // label for the break statment
        for (double v : V) {

            // add a new data series
            String SeriesName = String.format("DC%.4f", v);
            // Value1 axis (left), e.g. |Z|
            SeriesID_Value1 = Chart.AddXYSeries(SeriesName + "_" + CircuitModeString1, 0, false, true,
                                             Chart.LINE_SOLID, Chart.MARKER_NONE);

            // value2 axis (right), e.g. phi in degree
            SeriesID_Value2 = Chart.AddXYSeries(SeriesName + "_" + CircuitModeString2, SecondAxisIndex, false, true,
                                             Chart.LINE_DASHED, Chart.MARKER_NONE);
            

            // set the spot (DC) voltage
            setDCBias(v);


            // wait a bit
            try { Thread.sleep(VDelay); } catch (InterruptedException ignore) {}

            // sweep through all frequencies
            for (double f : F) {

                // check Stop button
                if (m_StopScripting)
                    break sweep;

                // check for Pause button
                isPaused(true);

                // set the spot frequency
                setFrequency(f);

                // measure
                MValue data = Measure();

                // store the frequency and bias
                DataPoint.Freq = f;
                DataPoint.V = v;
                
                // store measured value if valid or not
                // (Value1 and Value2 are always double values because Measure() throws a ScriptException
                // if the answer cannot be converted to double
                DataPoint.Value1 = data.Value1;
                DataPoint.Value2 = data.Value2;
                

                // plot the impedance if valid

                // add the datapoint to the graph
                Chart.AddXYDataPoint(SeriesID_Value1, DataPoint.Freq, DataPoint.Value1);
                Chart.AddXYDataPoint(SeriesID_Value2, DataPoint.Freq, DataPoint.Value2);


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
            
            // write header lines
            
            // Aperture (Integration Time)
            String str = QueryInstrument(":Aperture?");
            FWriter.write("% Averaging time and number of averages: " + str);
            FWriter.newLine();
            
            // number of averages
            str = QueryInstrument(":Voltage:Level?");
            FWriter.write("AC Amplitude: " + str);
            FWriter.newLine();
            

            // column headers
            FWriter.write("f [Hz]\t" + CircuitModeString1 + "\t" +
                    CircuitModeString2 + "\tVbias [V]");
            FWriter.newLine();

            // save data
            for (Storage s : Results) {

                // build one line
                String dummy = String.format(Locale.US, "%E\t%E\t%E\t%f",
                        s.Freq, s.Value1, s.Value2, s.V);

                // store to file
                FWriter.write(dummy);
                FWriter.newLine();
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
            m_GUI.DisplayStatusMessage("Could not save the impedance chart.\n");
        }
 
    }//</editor-fold>
    
    
    /**
     * Sets the circuit mode, or function, to the desired value. The instrument's
     * display and the data saved by <code>SweepFthenV</code> will be in this
     * format, e.g. Cp-Rp, Cs-Rs, or R-X
     * 
     * @param CircuitMode
     * @throws IOException re-thrown from {@link Device#SendToInstrument}
     * @throws DataFormatException when the Syntax Check failed. 
     */
    @AutoGUIAnnotation(
        DescriptionForUser = "<html>Sets the Circuit Mode, e.g. Cp-Rp, Cs-Rs, R-X, ...</html>",
        ParameterNames = {"Circuit Mode"},
        DefaultValues = {"RX"},
        ToolTips = {"<html>Can be CPD, CPQ, CPG, CPRP, CSD, CSQ, CSRS, LPD, LPQ, LPG, LPRP,<br>"
                + "LPRD, LSD, LSQ, LSRS, LSRD, RX, ZTD, ZTR, GB, YTD, YTR, VDID</html>"})
    @iC_Annotation(MethodChecksSyntax=true)
    public void configCircuitMode(String CircuitMode) 
           throws DataFormatException, IOException {

        // Syntax Check for correct Mode
        if ( !CIRCUIT_MODES.contains(CircuitMode.toUpperCase()) ) {
            String str = "The Circuit Mode is not valid.";
            str += "Please select a measurement mode from:\n " + CIRCUIT_MODES.toString() + ".\n";
            throw new DataFormatException(str);
        }
        
        // return if in Syntax-check mode
        if (inSyntaxCheckMode())
            return;

        // exit if in No-Communication-Mode
        if (inNoCommunicationMode())
            return;
        
        // build the string
        String dummy = String.format(Locale.US, ":Function:Impedance:Type %s", CircuitMode);

        // send to device
        SendToInstrument(dummy);
    }

}
