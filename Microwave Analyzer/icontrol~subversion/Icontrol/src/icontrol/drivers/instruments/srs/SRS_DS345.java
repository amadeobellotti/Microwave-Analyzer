// TODO 3* ignore HeaderLine with labels in setARBtoCELIV2

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
package icontrol.drivers.instruments.srs;

import icontrol.AutoGUIAnnotation;
import icontrol.Utilities;
import icontrol.drivers.Device;
import icontrol.iC_Annotation;
import icontrol.drivers.Device.CommPorts;
import icontrol.iC_ChartXY;
import icontrol.iC_ChartXY.SeriesIdentification;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;
import javax.script.ScriptException;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.interpolation.LoessInterpolator;
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;

/**
 * This class implements functionality to communicate with an SRS DS345 Synthesized
 * Function Generator.
 *
 * <h3>Instrument Commands:</h3>
 * <ul>
 *  <li>{@link #enableModulation(boolean) }
 *  <li>{@link #setARBtoCELIV(double, double, double, double) }
 *  <li>{@link #setModulationType(String) }
 *  <li>{@link #setOutputFunction(String) }
 *  <li>{@link #setTriggerSource(String) }
 *  <li> Note that more commands are defined as generic GPIB commands
 * </ul><p>
 * 
 * @author KPP (Kurt Pernstich: pernstich@alumni.ethz.ch)
 * @version 0.1
 */
// promise that this class supports GPIB communication
@iC_Annotation(CommPorts = CommPorts.GPIB,
InstrumentClassName = "SRS DS345")
public class SRS_DS345 extends Device {

    ///////////////////
    // member variables
    
    /** The Logger for this class. Note that the Logger Level is set in the
     * constructor of the <code>Device</code> class. */
    private static final Logger m_Logger = Logger.getLogger("iC.Instruments.SRS_DS345");

    
    /**
     * Sets the Output Function of the DS345 to Sine, Square, ...
     * 
     * @param OutputFunction The desired Output Function; can be Sine, Square,
     * Triangle, Ramp, Noise, Arbitrary (case insensitive).
     * 
     * @throws DataFormatException When the Syntax-Check failed.
     * @throws IOException Bubbles up from <code>SendToInstrument</code>
     */
    // <editor-fold defaultstate="collapsed" desc="setOutputFunction">
    @AutoGUIAnnotation(DescriptionForUser = "Sets the Output Function to Sine, Square, etc.",
    ParameterNames = {"Output Function"},
    ToolTips = {"Can be {Sine, Square, Triangle, Ramp, Noise, Arbirtray (case insensitive)}"},
    DefaultValues = {"Sine"})
    @iC_Annotation(MethodChecksSyntax = true)
    public void setOutputFunction(String OutputFunction)
            throws DataFormatException, IOException {

        ///////////////
        // Syntax-Check
        // also builds the command string
        
        // build the String
        String cmd = "FUNC ";
        if (OutputFunction.equalsIgnoreCase("Sine")) {
            cmd += "0";
        } else if (OutputFunction.equalsIgnoreCase("Square")) {
            cmd += "1";
        } else if (OutputFunction.equalsIgnoreCase("Triangle")) {
            cmd += "2";
        } else if (OutputFunction.equalsIgnoreCase("Ramp")) {
            cmd += "3";
        } else if (OutputFunction.equalsIgnoreCase("Noise")) {
            cmd += "4";
        } else if (OutputFunction.equalsIgnoreCase("Arbitrary")) {
            cmd += "5";
        } else {
            String str = "The Output Function '" + OutputFunction + "' is not valid."
                    + "Please select a value from:\n"
                    + "Sine, Square, Triangle, Ramp, Noise, Arbitrary\n";
                    
            throw new DataFormatException(str);            
        }

        // return if in Syntax-check mode
        if (inSyntaxCheckMode()) {
            return;
        }

        // exit if in No-Communication-Mode
        if (inNoCommunicationMode()) {
            return;
        }


        // send the command
        SendToInstrument(cmd);
    }// </editor-fold>

    /**
     * Downloads an arbitrary waveform to the DS345 that is used in CELIV measurements
     * (Current Extraction using a Linearly Increasing Voltage Pulse), that is,
     * a linearly increasing voltage with a defined delay time. This method adds
     * a 100 ms delay after programming the arbitrary waveform to ensure the
     * DS345 is done programming before continuing. The output voltage is set to
     * 0 while programming, but it does not really help as becomes evident when
     * looking at the waveform with an oscilloscope.
     * 
     * @param Tdelay The time before the voltage ramp start; in [us]
     * @param Slope The slope of the linearly increasing voltage pulse; in [V/sec]
     * @param Vpeak The maximum voltage of the voltage pulse; in [V]
     * @param Tfall The time for voltage to return to 0V; in [us]. If a value of
     * 0 is chosen, the Fall Time is made equal to the rise time.
     * 
     * @throws ScriptException When the DS345 is not ready to receive Arbitrary
     * Waveform Data (no specific reason is given in the manual), or during development
     * when the vertice vector is not formed properly.
     * @throws DataFormatException When the Syntax-Check failed.
     * @throws IOException Bubbles up from <code>SendToInstrument</code> or 
     * <code>QueryInstrument</code>
     */
    // <editor-fold defaultstate="collapsed" desc="setARBtoCELIV">
    @AutoGUIAnnotation(DescriptionForUser = "Download an arbitrary waveform for CELIV measurements to the DS345.",
        ParameterNames = {"Delay Time [us]", "Voltage slope [V/sec]", "Peak voltage [V]", "Fall Time [us]"},
        ToolTips = {"", "Can be negative", "", "If 0 is chosen, the Fall Time is made equal to the rise time."},
        DefaultValues = {"100", "1000", "1", "100"})
    @iC_Annotation(MethodChecksSyntax=true)
    public void setARBtoCELIV(double Tdelay, double Slope, double Vpeak, double Tfall)
            throws DataFormatException, IOException, ScriptException {

        //////////////////
        // local variables
        short CheckSum = 0;
        ArrayList<Double> T = new ArrayList<Double>();
        ArrayList<Short> x = new ArrayList<Short>();
        ArrayList<Short> y = new ArrayList<Short>();

        // Arbitrary Waveform max. Sampling Frequency
        final double fmax = 40e6;
        final short NrPoints = 16300;
        
        
        ///////////////
        // Syntax-check
        
        if (Tdelay < 0) {
            String str = "The delay time must be >= 0";
            throw new DataFormatException(str);
        }
        
//        if (Slope <= 0) {
//            String str = "The Voltage slope must be > 0";
//            throw new DataFormatException(str);
//        }
        
        if (Vpeak <= 0) {
            String str = "The peak voltage must be > 0";
            throw new DataFormatException(str);
        }
        
        if (Tfall < 0) {
            String str = "The fall time time must be >= 0";
            throw new DataFormatException(str);
        }

        // exit if in Syntax-Check-Mode
        if (inSyntaxCheckMode()) {
            return;
        }
        
        // exit if in No-Communication-Mode
        if (inNoCommunicationMode()) {
            return;
        }
        
        // scale input values
        Tdelay *= 1e-6;
        Tfall *= 1e-6;
        
        /* Define Vertices:
         * x: [0, 16299]    y: [-2047, 2047]
         * also set ARB-Sampling Frequency with FSMP
         * also set Voltage amplitude with AMPL
         * y = 2047 equals to Vpeak-to-peak / 2
         */


        // calc Time-points
        T.add(0.0);
        T.add(Tdelay);
        T.add(Tdelay + Vpeak / Math.abs(Slope));
        if (Tfall > 0) {
            // add chosen Fall Time in usec
            T.add(Tdelay + Vpeak / Math.abs(Slope) + Tfall);
        } else {
            // choose a Fall Time equal to the rise time
            T.add(Tdelay + 2 * Vpeak / Math.abs(Slope));
        }
        
        // calc sampling frequency ( NrPoints / Tmax )
        double fs_calc = NrPoints / T.get(T.size()-1);
        
        // calc divisor ( round_up 40 MHz / fs_calc )
        // Divisor is never smaller than 1, so it limit's fs to fmax
        double Divisor = Math.ceil(fmax / fs_calc);
        
        // calc real sampling frequency (with rounded_up Divisor)
        double fs = fmax / Divisor;
        
        // calc time between two points
        double dT = 1 / fs;
        
        // log numbers
        m_Logger.log(Level.FINER, "fs_calc = {0}", fs_calc);
        m_Logger.log(Level.FINER, "Divisor = {0}", Divisor);
        m_Logger.log(Level.FINER, "fs = {0}", fs);
        m_Logger.log(Level.FINER, "dT = {0}", dT);
        
        // calc x-points
        for (double t : T) {
            x.add( (short)Math.round(t / dT) );
        }
        
        // add y-points
        y.add((short)0);
        y.add((short)0);
        y.add(Slope>0 ? (short)2047 : (short)-2047);
        y.add((short)0);
        
        
        // add last point if not already assigned
        if ( x.get(x.size()-1) < (NrPoints-1) ) {
            x.add( (short)(NrPoints - 1) );
            y.add( (short)0);
        }
        
        // make sure they are the same length to prevent a cryptic error message
        if (x.size() != y.size()) {
            String str = "x and y are of differnt lenght in SRS_DS345.setARBtoCELIV.\n"
                    + "Should only occur during development";
            throw new ScriptException(str);
        }
        
        
        // define a ByteBuffer with lowest significant byte first
        ByteBuffer BBuffer = ByteBuffer.allocate(2 /*short*/ * 2 /*x, y*/ * x.size() + 2 /*checksum*/);
        BBuffer.order(ByteOrder.LITTLE_ENDIAN);

        // iterate through all Vertices
        for (int i=0; i<x.size(); i++) {

            // add x-point of Vertice to ByteBuffer
            BBuffer.putShort(x.get(i));
            BBuffer.putShort(y.get(i));

            // calc checksum
            CheckSum += x.get(i) + y.get(i);
        }

        // add checksum
        BBuffer.putShort(CheckSum);
        
        // build the string from the ByteBuffer
        String ByteString = new String(BBuffer.array(), CHARACTER_ENCODING);
        
        
        // display content of BBuffer
        if (false) {
            BBuffer.rewind();
            String Bstr = "0x";
            while (BBuffer.hasRemaining()) {
                Bstr += String.format(" %02X", BBuffer.get());
            }
            m_GUI.DisplayStatusMessage(Bstr + "\n", false);
        }


        // set ARB sampling frequency
        String cmd = String.format(Locale.US, "FSMP %.6f", fs);
        SendToInstrument(cmd);
        
        // set output voltage to 0 while programming
        cmd = String.format(Locale.US, "AMPL 0VP");
        SendToInstrument(cmd);


        // query if it's okay to send ARB binary data
        String Status = QueryInstrument("LDWF? 1," + x.size());

        // check answer
        if (Status.equals("1")) {

            // send the binary data string
            SendToInstrument(ByteString);

        } else {
            // the DS345 is not ready to receive ARB binary data
            String str = "The SRS DS345 is not ready to receive arbitrary waveform data.\n"
                    + "No additional reasons are given by the DS345.\n";
            throw new ScriptException(str);
        }
        
        // wait a bit to give the DS345 time to store the data
        try {Thread.sleep(100);} catch (InterruptedException ignore) {}
        
        
        // set output voltage
        cmd = String.format(Locale.US, "AMPL %fVP", Vpeak * 2);
        SendToInstrument(cmd);



        // === 120426: learning to understand Unicode Conversion
        if (false) {

            ///////////////////////////////////////////
            // get nr. of bytes for different encodings

            // define a byte array (8-bit)  hex: 41 7F 80 AB 41
            byte[] barray = new byte[]{(byte) 65, (byte) 127, (byte) 128, (byte) 0xAB, (byte) 65};
            byte[] barray0 = new byte[]{0, (byte) 65, 0, (byte) 127, 0, (byte) 128, 0, (byte) 0xAB, 0, (byte) 65};

            // convert to strings from bytearry
            String utf8 = new String(barray, "UTF-8");          // http://en.wikipedia.org/wiki/UTF-8
            String utf80 = new String(barray0, "UTF-8");
            String utf16 = new String(barray, "UTF-16");        // http://en.wikipedia.org/wiki/UTF-16
            String utf160 = new String(barray0, "UTF-16");
            String iso88591 = new String(barray, "ISO-8859-1");
            //String ucs2 = new String(barray, "UCS-2");
            /* The bytes appear to specify unicode code points (65 = 'A').
             * In UTF-8, code points between 80h and FFh are not defined, hence
             * the utf8 string contains twice the same u-number (A\u007f\ufffd\ufffdA).
             * UTF-16 encodes *valid code points* in the range U+0000 to U+FFFF as 
             * as single 16-bit code units that are numerically equal to the corresponding
             * code points. This seems to be the case (\u417f\u80ab\ufffd); not sure where
             * the last "A" went to.
             * The Unicode standard says that U+D800 to U+DFFF will never be assigned
             * a character, so there should be no reason to encode them. The Unicode 
             * standard says that all UTF forms, including UTF-16, *cannot* encode 
             * these code points ==> iC should not encode short's into Strings but bytes.
             * BUT, UTF-16 encoding indicated that two bytes are used as one code point,
             * hence, I would need to build the String with every odd byte beeing 0
             * ISO8859-1 is a 8-bit single-byte coded graphic character sets. It seems
             * to do what I want.
             */
            /* How does JNA handle String conversion? I can (but do not yet do it)
             * specify a character set used for conversion, so I guess I need to
             * set it.
             * TODO How does the Prologix behave when trying to send binary data? The
             * conversion to and from the String/char* might be right, but is the length
             * also correct (is there a end-of-string termination character that might
             * be part of the binary data?)
             */

            // convert to string using String.format via %c
            //String format_barray = String.format(locnull, "%c%c%c", barray[0], barray[1], barray[2]);


            // get bytes
            byte[] utf8_b = utf8.getBytes("UTF-8");
            byte[] utf16_b = utf16.getBytes("UTF-16");
            byte[] iso88591_b = iso88591.getBytes("ISO-8859-1");
            //byte[] ucs2_b = ucs2.getBytes("UCS-2");


            // thorough test with all values from 0 to 255
            byte[] AllValues = new byte[256];
            for (int i = 0; i <= 255; i++) {
                AllValues[i] = (byte) i;
            }

            // convert to string
            String AllValuesStr = new String(AllValues, "ISO-8859-1");

            // convert back to byte[]
            byte[] ConvertedBack = AllValuesStr.getBytes("ISO-8859-1");

            // compare
            String Check = "Juhuu!";
            for (int i = 0; i <= 255; i++) {
                if (AllValues[i] != ConvertedBack[i]) {
                    Check = "Uje";
                }
            }
            m_GUI.DisplayStatusMessage("Unicode Test: " + Check + "\n");
        }
    }// </editor-fold>




    /**
     * Downloads an arbitrary waveform to the DS345 that is used in CELIV measurements
     * (Current Extraction using a Linearly Increasing Voltage Pulse), that is,
     * a linearly increasing voltage with a defined delay time. This method adds
     * a 100 ms delay after programming the arbitrary waveform to ensure the
     * DS345 is done programming before continuing. The output voltage is set to
     * 0 while programming, but it does not really help as becomes evident when
     * looking at the waveform with an oscilloscope. This version of setARBtoCELIV
     * requires the specification of a text file containing Time/Voltage pairs that
     * will be used during Tdelay. It also sets the offset voltage to the value
     * at t=0.
     *
     * @param Tdelay The time before the voltage ramp start; in [us]
     * @param Slope The absolut value of the slope of the linearly increasing
     * voltage pulse; in [V/sec]
     * @param Vpeak The maximum voltage of the voltage pulse; in [V]. The sign
     * determines if the linearly increasing voltage is ramped towards positiv or
     * negative values.
     * @param Tfall The time for voltage to return to 0V, respectively Voffset;
     * in [us]. If a value of 0 is chosen, the Fall Time is made equal to the rise time.
     * @param FileName The File Name containing the Time/Voltage data (comma or
     * tab separated) to be used during Tdelay
     *
     * @throws ScriptException When the DS345 is not ready to receive Arbitrary
     * Waveform Data (no specific reason is given in the manual) or if a number
     * could not be interpreted in the Time/Voltage file
     * @throws DataFormatException When the Syntax-Check failed.
     * @throws IOException Bubbles up from <code>SendToInstrument</code> or
     * <code>QueryInstrument</code>
     */
    // <editor-fold defaultstate="collapsed" desc="setARBtoCELIV2">
    @AutoGUIAnnotation(DescriptionForUser = "Download an arbitrary waveform for CELIV measurements to the DS345 including t/V data during Tdelay.",
        ParameterNames = {"Delay Time [us]", "|Voltage slope| [V/sec]", "Peak voltage [V]", "Fall Time [us]", "File Name"},
        ToolTips = {"", "", "Can be negative", "If 0 is chosen, the Fall Time is made equal to the rise time.",
        "<html>Comma or Tab separated list of Time/Voltage data.<br>Should be smoothed.<br>The value at t=0 is used for the offset voltage.<br>Can contain comments (// or %).</html>"},
        DefaultValues = {"100", "1000", "1", "100", "Photovoltage.txt"})
    @iC_Annotation(MethodChecksSyntax=true)
    public void setARBtoCELIV2(double Tdelay, double Slope, double Vpeak, double Tfall, String FileName)
            throws DataFormatException, IOException, ScriptException {

        //////////////////
        // local variables
        short CheckSum = 0;

        // Arbitrary Waveform max. Sampling Frequency
        final double fmax = 40e6;
        final short NrPoints = 16300;
        
        String  Line;
        ArrayList<Float> Tf = new ArrayList<Float>();   // Time in the T/V file
        ArrayList<Float> Vf = new ArrayList<Float>();
        double   Vmax = 0;

        
        // scale input values
        Tdelay *= 1e-6;
        Tfall *= 1e-6;


        ///////////////
        // Syntax-check

        if (Tdelay < 0) {
            String str = "The delay time must be >= 0";
            throw new DataFormatException(str);
        }

        if (Slope <= 0) {
            String str = "The Voltage slope must be > 0";
            throw new DataFormatException(str);
        }

        if (Tfall < 0) {
            String str = "The fall time time must be >= 0";
            throw new DataFormatException(str);
        }


        ////////////////
        // load T/V data
        // <editor-fold defaultstate="collapsed" desc="load V/T data points">
        // open the file for reading
        BufferedReader fr;
        try {
            FileName = m_GUI.getProjectPath() + FileName;
            fr = new BufferedReader(new FileReader(new File(FileName)));

        } catch (FileNotFoundException ex) {

            // check if file is found
            String str = "Error loading Time/Voltage data. Could not find the file\n"
                    + FileName + "\n" + ex.getMessage() + "\n";

            // throw Exception
            throw new DataFormatException(str);
        }

        try {
            // compile the Regex pattern
            Pattern pattern = Pattern.compile("(.*)[,\\t](.*)");
            // http://www.regular-expressions.info/floatingpoint.html
            //Pattern pattern = Pattern.compile("([-+]?[0-9]*\.?[0-9]+([eE][-+]?[0-9]+)?)[,\\t]([-+]?[0-9]*\.?[0-9]+([eE][-+]?[0-9]+)?)");
                
            // process all lines in the file
            while ( (Line = fr.readLine()) != null ) {

                // remove all characters after a comment (% or //)
                // original Regex was ^\\s*[//[%]].*  (it's wrong)
                // updated Regex was (//|%).+   (it's also wrong, strips off everything after % (e.g. in FR %.2f))
                // up-updated Regex is ^\\s*(//|%).+ (also wrong, a line containing nothing but // is not recognized as comment line)
                // up-updated Regex is ^\\s*(//|%).*
                Line = Line.replaceAll("^\\s*(//|%).*", "");

                // remove leading and trailing whitespaces
                Line = Line.replaceAll("^\\s+", "").replaceAll("\\s+$", "");

                // go to the next line if Line is empty
                if (Line.isEmpty())
                    continue;


                // make the Matcher
                Matcher m = pattern.matcher(Line);

                // Make the match
                if (m.matches()) {
                    
                    // convert to numbers
                    // this throws an Eception for the HederLine
                    //float t = Float.parseFloat(m.group(1));
                    //float v = Float.parseFloat(m.group(2));
                    float t = Utilities.getFloat(m.group(1));
                    float v = Utilities.getFloat(m.group(2));
                    
                    // store T/V pair
                    Tf.add(t);
                    Vf.add(v);
                    
                    
                    // speed up reading from file
                    int size = Tf.size();
                    if (size >= 2) {
                        
                        // keep only one value after Tdelay
                        if (Tf.get(size-2) > Tdelay) {
                            break;
                        }
                        
                        // remove values where t<0, but keep one negative time
                        if ( Math.signum(Tf.get(size-1)) == -1.0 &&
                             Math.signum(Tf.get(size-2)) == -1.0) {
                            
                            Tf.remove(size-2);
                            Vf.remove(size-2);
                            
                            continue;
                        }
                    }

                } else {
                    // no valid match was found so throw an Exception
                    String str = "Could not convert the line\n" + Line +
                            "\ninto two numbers. Please correct the line and try again.";
                    throw new DataFormatException(str);
                }

            }
        } catch (IOException ex) {
            String str = "An IO error occurred during reading the file\n"
                    + FileName + "\n";

            throw new DataFormatException(str);
        }

        // close the file
        if ( fr != null ) {
            try { fr.close(); } catch (IOException ignore) {}
        }
        //</editor-fold>



        /////////////////////////
        // interpolate T/V values
        // <editor-fold defaultstate="collapsed" desc="interpolate">
        // check length of Tf, Vf
        if (Tf.size() != Vf.size()) {
            String str = "The number of Time points is not equal to the number of "
                    + "voltage points in the file\n" + FileName;
            throw new DataFormatException(str);
        }

        // array for data interpolation
        double[] Tfint = new double[Tf.size()];
        double[] Vfint = new double[Tf.size()];

        // copy data into the array
        // (interpolate needs double, ArrayList can only be Double, so toArray doesn't work
        for (int i=0; i<Tf.size(); i++) {
            Tfint[i] = Tf.get(i);
            Vfint[i] = Vf.get(i);
            
            // remember max V value in the file
            if ( Math.abs(Vfint[i]) > Math.abs(Vmax) ) {
                
                // store the maximum with sign (required for scaling)
                Vmax = Vfint[i];
            }
        }
        
        // check Vmax
        if (Math.abs(Vmax) > Math.abs(Vpeak)) {
            
            // display a warning
            String str = "Warning: setARBtoCELIV2: Vpeak is smaller than maximum voltage in " + FileName + "\n";
            m_GUI.DisplayStatusMessage(str, false);
        }
        
        // also consider Vpeak in calculating Vmax
        if (Math.abs(Vpeak) > Math.abs(Vmax)) {
            
            // store the maximum with sign (required for scaling)
            Vmax = Vpeak;
        }
        
        
        // don't need the ArrayList anymore, so free resources
        Tf = null;
        Vf = null;



        // use Apache Common Math
        UnivariateInterpolator interpolator;
        UnivariateFunction function;
        
        int InterpolationType = m_iC_Properties.getInt("SRS_DS345.InterpolationType", 1);

        try {
            // make the interpolator
            // http://commons.apache.org/math/userguide/analysis.html#a12.6_Curve_Fitting
            if (InterpolationType == 2) {
                interpolator = new LoessInterpolator();
            } else {
                interpolator = new LinearInterpolator();
            }

            // generate the interpolating function
            function = interpolator.interpolate(Tfint, Vfint);


            // When experimenting with real measurement data, I found that the Loess
            // interpolation sometimes works with a bandwidth of 0.1, sometimes 0.01
            // is required. When the interpolation did not work, the function evaluation
            // returned NaN, so I test for NaN here
            if (function.value(0) == Double.NaN) {
                String str = "Info: The Loess interpolation seems to have failed. Try decreasing the Loess Bandwidth\n";
                        //+ "in the iC_User.properties using Keithley2600.MeasureOPV.LoessBandwidth = 0.01.\n";
                throw new DataFormatException(str);
            }

            /* Catch all Exceptions as Commons.Math might throw different Exceptions
             * that have not a common Math.Exception as their root, and the
             * javadoc is not 100% correct. */
        } catch (Exception ex) {

            // throw Exception
            String str = "Error: Could not generate the interpolating curve.\n";
            str += ex.getMessage() + "\n";
            throw new DataFormatException(str);
        }

        //</editor-fold>



        ///////////////////////////////////////
        // find voltages at T=0 and at T=Tdelay
        double Voffset;
        double Vdelay;
        try {
            // find V at T=0
            Voffset = function.value(0);

            // find V at T=Tdelay
            Vdelay = function.value(Tdelay);

            /* Catch all Exceptions as Commons.Math might throw different Exceptions
             * that have not a common Math.Exception as their root, and the
             * javadoc is not 100% correct. */
        } catch (Exception ex) {
            String str = "Could not determine voltage at T = 0 or at T = " + Tdelay + "\n"
                    + "Maybe Tdelay is larger than the measured photovoltage in the file\n"
                    + FileName + "\n" + ex.getMessage() + "\n";
            throw new DataFormatException(str);
        }
        
        // check if apmlitude is within range
        if (Vmax-Voffset > 5) {
            
            // voltage is to high to be supplied by the SRS DS345
            String str = "The output voltage is larger than the 5V maximum of the SRS DS345\n"
                    + "Try selecting a smaller Vpeak.\n";
            throw new DataFormatException(str);
        }



        /////////////
        // calc Times

        // time at which V=Vpeak
        double Tpeak = Tdelay + Math.abs(Vpeak - Vdelay) / Slope;

        // time at which V=Voffset again
        double Tend;        
        if (Tfall == 0) {
            Tend = Tpeak + Math.abs(Vpeak-Voffset) / Slope;
        } else {
            Tend = Tpeak + Tfall;
        }
        
        // calc Slope of second ramp with correct sign
        double Slope2 = Slope * Math.signum(Voffset-Vpeak);
        if (Tfall != 0) {
            Slope2 = (Voffset-Vpeak) / Tfall;
        }

        

        /* Define Points:
         * y: [-2047, 2047]
         * also set ARB-Sampling Frequency with FSMP
         * also set Voltage amplitude with AMPL
         * y = 2047 equals to Vpeak-to-peak / 2
         */

        
        // calc sampling frequency ( NrPoints / Tend )
        double fs_calc = NrPoints / Tend;

        // calc divisor ( round_up 40 MHz / fs_calc )
        // Divisor is never smaller than 1, so it limit's fs to fmax
        double Divisor = Math.ceil(fmax / fs_calc);

        // calc real sampling frequency (with rounded_up Divisor)
        double fs = fmax / Divisor;

        // calc time between two points
        double dT = 1 / fs;

        
        //////////////////////
        // build the ARB curve
        double[] Voltages = new double[NrPoints];
        double[] Time = new double[NrPoints];
        for (int i=0; i<NrPoints; i++) {

            // calc present time
            Time[i] = dT*i;

            // is it during Tdelay?
            if (Time[i] <= Tdelay) {

                // yes, so get the photovoltage from the file
                try {
                    // find V at present time
                    double V = function.value(Time[i]);

                    // remember it
                    Voltages[i] = V;

                    /* Catch all Exceptions as Commons.Math might throw different Exceptions
                     * that have not a common Math.Exception as their root, and the
                     * javadoc is not 100% correct. */
                } catch (Exception ex) {
                    String str = "Could not determine voltage at T = " + Time + "\n"
                            + "Is this time included in the file\n"
                            + FileName + "\n" + ex.getMessage() + "\n";
                    throw new ScriptException(str);
                }
                
                continue;
            }
                
            // is it during first slope?
            if (Time[i] <= Tpeak) {
                
                // calc voltage during first ramp
                Voltages[i] = Vdelay + (Time[i] - Tdelay) * Slope * Math.signum(Vpeak);
                
                continue;
            }
            
            // is it during second slope?
            if (Time[i] <= Tend) {
                
                // calc voltage during second ramp
                Voltages[i] = Vpeak + (Time[i] - Tpeak) * Slope2;
                
                continue;
            }
            
            // no it's all over and I need to fill with Voffset until all
            // available points are filled
            Voltages[i] = Voffset;
        }


        // show all values
        //for (int i=0; i<NrPoints; i++) {
        //    m_GUI.DisplayStatusMessage(Time[i]*1e6 + "     " + Voltages[i]+"\n", false);
        //}
        
        
        // exit if in Syntax-Check-Mode
        if (inSyntaxCheckMode()) {
            return;
        }

        // exit if in No-Communication-Mode
        if (inNoCommunicationMode()) {
            return;
        }
        
        // log numbers
        String str = String.format(Locale.US,"\nVmax from file = %f\n", Vmax);
        str += String.format(Locale.US,"Voffset(T=0) = %f\n", Voffset);
        str += String.format(Locale.US,"Vdelay(T=Tdelay) = %f\n", Vdelay);
        str += String.format(Locale.US,"Tpeak = %E\n", Tpeak);
        str += String.format(Locale.US,"Tend = %E\n", Tend);
        str += String.format(Locale.US,"Slope2 = %f\n", Slope2);
        str += String.format(Locale.US,"fs_calc = %f\n", fs_calc);
        str += String.format(Locale.US,"Divisor = %f\n", Divisor);
        str += String.format(Locale.US,"fs = %f\n", fs);
        str += String.format(Locale.US,"dT = %E\n", dT);
        m_Logger.log(Level.FINER, str);


        //////////////////
        // plot the graphs

        // make a new XYChart
        iC_ChartXY TV_Chart = new iC_ChartXY("setARBtoCELIV2",
                                "Time [sec]", "Photovoltage [V]",
                                false  /*legend*/,
                                640, 480);
        
        // change to mixed scientific/engineering format on X axis
        TV_Chart.getXAxis().setNumberFormatOverride(new DecimalFormat("##0.#####E0"));

        // plot measured data
        SeriesIdentification Original_Series = TV_Chart.AddXYSeries("TV_from_file",
                0, false, true, TV_Chart.LINE_SOLID_FINE, TV_Chart.MARKER_NONE);
        TV_Chart.AddXYDataPoints(Original_Series, Tfint, Vfint);

        // plot ARB data
        SeriesIdentification ARB_Series = TV_Chart.AddXYSeries("ARB",
                0, false, true, TV_Chart.LINE_SOLID_FINE, TV_Chart.MARKER_DOT);
        TV_Chart.AddXYDataPoints(ARB_Series, Time, Voltages);



        ////////////////////
        // build byte stream

        // define a ByteBuffer with lowest significant byte first
        ByteBuffer BBuffer = ByteBuffer.allocate(2 /*short*/ * NrPoints + 2 /*checksum*/);
        BBuffer.order(ByteOrder.LITTLE_ENDIAN);
        
        // for checking Byte range
        int VscaledMax = Integer.MIN_VALUE;
        int VscaledMin = Integer.MAX_VALUE;

        // iterate through all Vertices
        for (int i=0; i<NrPoints; i++) {
            
            // scale voltages to [-2047, 2047]
            double Vscaled = (Voltages[i] - Voffset) / (Vmax-Voffset) * 2047;
            
            // for checking Byte range (uncomment 3x)
            // TODO make false when setARBtoCELIV2 works
            if (true) {
                if (Vscaled > VscaledMax)
                    VscaledMax = (short)Vscaled;
                if (Vscaled < VscaledMin)
                    VscaledMin = (short)Vscaled;
            }

            // add voltage point to ByteBuffer
            BBuffer.putShort((short)Vscaled);

            // calc checksum
            CheckSum += (short)Vscaled;
            
            // log all values during development
            // TODO make false when setARBtoCELIV2 works
            if (true) {
                // do not change to m_Logger.log(...)
                m_Logger.log(Level.FINEST, "{0}\n", (short)Vscaled);
            }
        }
        
        // for checking Byte range
        if (true) {
            // do not change to m_Logger.log(...)
            m_Logger.log(Level.FINE, "Vscaled min = {0}\n", VscaledMin);
            m_Logger.log(Level.FINE, "Vscaled max = {0}\n", VscaledMax);
        }

        // add checksum
        BBuffer.putShort(CheckSum);

        // build the string from the ByteBuffer
        String ByteString = new String(BBuffer.array(), CHARACTER_ENCODING);

        
        
        // exit here if run as JUnit test 
        if (inJUnitTest()) {
            return;
        }

        // set ARB sampling frequency
        String cmd = String.format(Locale.US, "FSMP %.6f", fs);
        SendToInstrument(cmd);

        // set output voltage to 0 while programming
        cmd = String.format(Locale.US, "AMPL 0VP");
        SendToInstrument(cmd);


        // query if it's okay to send ARB binary data
        String Status = QueryInstrument("LDWF? 0," + NrPoints);

        // check answer
        if (Status.equals("1")) {

            // send the binary data string
            SendToInstrument(ByteString);

        } else {
            // the DS345 is not ready to receive ARB binary data
            str = "The SRS DS345 is not ready to receive arbitrary waveform data.\n"
                    + "No additional reasons are given by the DS345.\n";
            throw new ScriptException(str);
        }

        // wait a bit to give the DS345 time to store the data
        try {Thread.sleep(1000);} catch (InterruptedException ignore) {}
        
        // ask for the Instrument's name in the hope it might be done
        // storing the data when this method returns
        str = QueryInstrument("*IDN?");


        // set output voltage
        cmd = String.format(Locale.US, "AMPL %fVP", Vmax * 2);
        SendToInstrument(cmd);
        
        // set offset voltage
        cmd = String.format(Locale.US, "OFFS %E", Voffset);
        SendToInstrument(cmd);

    }// </editor-fold>

    
    /**
     * Sets the Trigger Source.
     * 
     * @param TriggerSource The desired Trigger Source; can be Single, Internal Rate,
     * +Slope External, -Slope External, Line (case insensitive).
     * 
     * @throws DataFormatException When the Syntax-Check failed.
     * @throws IOException Bubbles up from <code>SendToInstrument</code>
     */
    // <editor-fold defaultstate="collapsed" desc="setTriggerSource">
    @AutoGUIAnnotation(DescriptionForUser = "Sets the Trigger Source.",
        ParameterNames = {"Trigger Source"},
        ToolTips = {"Can be Single, Internal Rate, +Slope External, -Slope External, Line (case insensitive)"},
        DefaultValues = {"Internal Rate"})
    @iC_Annotation(MethodChecksSyntax = true)
    public void setTriggerSource(String TriggerSource)
            throws DataFormatException, IOException {

        ///////////////
        // Syntax-Check
        // also assigns the Command String
        
        // build the String
        String cmd = "TSRC ";
        if (TriggerSource.equalsIgnoreCase("Single")) {
            cmd += "0";
        } else if (TriggerSource.equalsIgnoreCase("Internal Rate")) {
            cmd += "1";
        } else if (TriggerSource.equalsIgnoreCase("+Slope External")) {
            cmd += "2";
        } else if (TriggerSource.equalsIgnoreCase("-Slope External")) {
            cmd += "3";
        } else if (TriggerSource.equalsIgnoreCase("Line")) {
            cmd += "4";
        } else {
            String str = "The Trigger Source '" + TriggerSource + "' is not valid."
                    + "Please select a value from:\n"
                    + "Single, Internal Rate, +Slope External, -Slope External, Line\n ";
            throw new DataFormatException(str);
        }

        
        // return if in Syntax-check mode
        if (inSyntaxCheckMode()) {
            return;
        }

        // exit if in No-Communication-Mode
        if (inNoCommunicationMode()) {
            return;
        }

        // send the command
        SendToInstrument(cmd);
        
    }// </editor-fold>
    
    /**
     * Enables or Disables Modulation of the DS345's output.
     * 
     * @param EnableModulation If <code>true</code>, modulation will be enabled
     * 
     * @throws IOException Bubbles up from <code>SendToInstrument</code>
     */
    // <editor-fold defaultstate="collapsed" desc="enableModulation">
    @AutoGUIAnnotation(DescriptionForUser = "Enables or Disables Modulation.",
    ParameterNames = {"Enable Modulation"},
    ToolTips = {""},
    DefaultValues = {"true"})
    public void enableModulation(boolean EnableModulation)
            throws DataFormatException, IOException {

        
        // exit if in No-Communication-Mode
        if (inNoCommunicationMode()) {
            return;
        }

        // send the command
        SendToInstrument("MENA " + (EnableModulation ? "1" : "0"));
    }// </editor-fold>
    
    
    /**
     * Sets the Modulation Type of the DS345 to Lin Sweep, Log Sweep, ...
     * 
     * @param ModulationType The desired Modulation Type; can be Lin Sweep, Log Sweep,
     * Internal AM, FM, Phi, Burst (case insensitive).
     * 
     * @throws DataFormatException When the Syntax-Check failed.
     * @throws IOException Bubbles up from <code>SendToInstrument</code>
     */
    // <editor-fold defaultstate="collapsed" desc="setModulationType">
    @AutoGUIAnnotation(DescriptionForUser = "Sets the Modulation Type to Lin Sweep, Log Sweep, etc.",
    ParameterNames = {"Modulation Type"},
    ToolTips = {"Can be {Lin Sweep, Log Sweep,Internal AM, FM, Phi, Burst (case insensitive)}"},
    DefaultValues = {"Sine"})
    @iC_Annotation(MethodChecksSyntax = true)
    public void setModulationType(String ModulationType)
            throws DataFormatException, IOException {

        ///////////////
        // Syntax-Check
        // also builds the command string
        
        // build the String
        String cmd = "MTYP ";
        if (ModulationType.equalsIgnoreCase("Lin Sweep")) {
            cmd += "0";
        } else if (ModulationType.equalsIgnoreCase("Log Sweep")) {
            cmd += "1";
        } else if (ModulationType.equalsIgnoreCase("Internal AM")) {
            cmd += "2";
        } else if (ModulationType.equalsIgnoreCase("FM")) {
            cmd += "3";
        } else if (ModulationType.equalsIgnoreCase("Phi")) {
            cmd += "4";
        } else if (ModulationType.equalsIgnoreCase("Burst")) {
            cmd += "5";
        } else {
            String str = "The Modulation Type '" + ModulationType + "' is not valid."
                    + "Please select a value from:\n"
                    + "SLin Sweep, Log Sweep,Internal AM, FM, Phi, Burst\n";
                    
            throw new DataFormatException(str);            
        }

        // return if in Syntax-check mode
        if (inSyntaxCheckMode()) {
            return;
        }

        // exit if in No-Communication-Mode
        if (inNoCommunicationMode()) {
            return;
        }


        // send the command
        SendToInstrument(cmd);
    }// </editor-fold>
}
