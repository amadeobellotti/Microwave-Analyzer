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
import icontrol.drivers.Device;
import icontrol.iC_Annotation;
import icontrol.iC_Properties;
import icontrol.drivers.Device.CommPorts;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;
import javax.script.ScriptException;

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
        ToolTips = {"", "", "", "If 0 is chosen, the Fall Time is made equal to the rise time."},
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
        
        if (Slope <= 0) {
            String str = "The Voltage slope must be > 0";
            throw new DataFormatException(str);
        }
        
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
        T.add(Tdelay + Vpeak / Slope);
        if (Tfall > 0) {
            // add chosen Fall Time in uses
            T.add(Tdelay + Vpeak / Slope + Tfall);
        } else {
            // choose a Fall Time equal to the rise time
            T.add(Tdelay + 2 * Vpeak / Slope);
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
        y.add((short)2047);
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
        cmd = String.format(Locale.US, "AMPL 0Vpp");
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
        cmd = String.format(Locale.US, "AMPL %fVpp", Vpeak * 2);
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
