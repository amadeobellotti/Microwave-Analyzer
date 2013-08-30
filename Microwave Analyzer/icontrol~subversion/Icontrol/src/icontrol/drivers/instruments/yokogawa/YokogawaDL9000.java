// TODO without Trigger and with History Cleared, I can save the last waveform
// so what do I need to do to ensure that the waveforms that I am about to
// save are valid ??

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
package icontrol.drivers.instruments.yokogawa;

import icontrol.AutoGUIAnnotation;
import static icontrol.Utilities.getInteger;
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
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;
import javax.script.ScriptException;

/**
 * This class implements functionality to communicate with a 
 * Yokogawa DL9000 series digital sampling oscilloscope. It was developed and
 * tested on a DL9240L oscilloscope.<p>
 * 
 * This class uses Yokogawa's TMCTL library, which is encapsulated in
 * <code>TMCTL_Driver</code>. This driver supports Ethernet, USB, GPIB, and RS232
 * communication. USB is fastest (1.6 sec for 1 MB) followed by Ethernet (2.3 sec)
 * and GPIB (3.2 sec). The TMCTL driver allows to select either protocol, and at
 * the time of writing, only the USBTMC(DL9000) protocol is supported. Please
 * contact the developer if you like to use other protocols.<p>
 * 
 * The TMCTL library requires a special USB Driver to be installed to be able to 
 * communicate with the DL9000 Series oscilloscope. This driver can be downloaded from
 * <a href="https://y-link.yokogawa.com/YL008.po?V_ope_type=Show&Download_id=DL00002094"></a>
 * and it is also copied into the _misc folder of the developers version of iC.<p>
 * 
 * The MAKE command for this device looks like:<br>
 * MAKE dT; Yokogawa DL9000; TMCTL: USBTMC(DL9000) = SerialNumber (e.g. 27E000001)<p>
 *
 * All device commands that the Yokogawa DL9000/DL92240L understands are
 * implemented here.<p>
 *
 * <h3>Instrument Commands:</h3>
 * <ul>
 *  <li>{@link #getParameter(int, int, String) }
 *  <li>{@link #SaveWaveform(String, String, boolean) }
 *  <li>{@link #setAcquisitionMode(int) }
 *  <li>{@link #setTimeBase(double) }
 *  <li>{@link #Start() }
 *  <li> Note that more commands are defined as generic commands
 * </ul>
 *
 * @author KPP (Kurt Pernstich: pernstich@alumni.ethz.ch)
 * @version 0.1
 *
 */

// promise that this class supports GPIB communication
@iC_Annotation(CommPorts=CommPorts.TMCTL,
                InstrumentClassName="Yokogawa DL9000")
public class YokogawaDL9000 extends Device {


    ///////////////////
    // member variables

    /**
     * The Logger for this class. Note that the Logger Level is set in the
     * constructor of the <code>Device</code> class.
     */
    // change the Logger name to the name of your class
    private static final Logger m_Logger = Logger.getLogger("iC.Instruments.YokogawaDL9000");

    
    /**
     * A method for additional initializations after establishing the connection
     * to the Instrument. This method is called from <code>Dispatcher.HandleMakeCommand</code>
     * after calling <code>Openinstrument</code>. Does NOT call the superclass' 
     * <code>Open</code> method (because this class does not support GPIB
     * communication, hence, calling the superclass is not required).<p>
     *
     * Note that this method is not called when in Syntax-Check mode or when in
     * No-Communication mode!<p>
     *
     * @throws IOException when an sending / receiving data caused an error
     * @throws ScriptException 
     */
    // <editor-fold defaultstate="collapsed" desc="Open">
    @Override
    public void Open()
           throws IOException, ScriptException {

        //////////////////////////
        // initialize oscilloscope
        
        
        // Set the data format for transmission
        // if RBYTE is used, :WAVEFORM:SIGN? needs to be considered
        SendToInstrument(":Waveform:Format WORD");
       
        
        // set BIG_ENDIAN
        SendToInstrument("Waveform:Byteorder MSBFirst");
        
        
        // switch on that header is transmitted in queries.
        // e.g. :TIMEBASE:TDIV? returns either :TIMEBASE:TDIV 1.000E-06 or 1.000E-06
        // use .replaceFirst("(:.+)+\\s", ""); to remove header if Header is ON
        // Note that at least  SaveWaveform will not function if set to OFF
        SendToInstrument("Communicate:Header ON");   
        
        // set Transition Filter for the Extended Event Register to detect when 
        // a Trigger has occurred to find if a "valid" waveform can be saved
        SendToInstrument(":STATUS:FILTER3 FALL");
        
    }//</editor-fold>

    
    
    /** 
     * Receives waveform data from the oscilloscope and saves it in a text file.
     * The data can optionally be plotted in a chart; note that this plot contains
     * only a limited number of data points to speed up plotting. When averaging
     * is enabled on the scope, the averaged waveform is acquired, otherwise the
     * most recent one is saved.
     * 
     * @param ChannelsToSave Comma separated list of channels to save; can be 1..4 
     * for "regular" channels 1 to 4. Could also be MATH1..4, or REFerence1..4
     * (same format as in :Waveform:Trace) but it's not clear that those traces
     * have the same number of points, hence, saving and plotting might fail, and 
     * obtaining the channel setup will also fail.
     * 
     * @param FileExtension The File Extension appended to the FileName defined
     * in the GUI to save the data
     * 
     * @param PlotWaveforms If <code>true</code>, the waveforms are also plotted
     * in a chart, which is then also saved as a png file.
     * 
     * @return Returns a <code>float[][]</code> with the Time in the first column
     * and the voltages of the waveforms in the second, third, etc. columns. If
     * an error occurred or in No-Communication Mode, the return value can be
     * <code>null</code>.
     * 
     * @throws IOException Bubbles up from <code>SendViaTMCTL</code> or
     * <code>QueryViaTMCTL</code>.
     * 
     * @throws ScriptException When the DL9000's answer could not be converted
     * to a number or the scope was not triggered.
     */
    // <editor-fold defaultstate="collapsed" desc="SaveWaveform">
    @AutoGUIAnnotation(
        DescriptionForUser = "Saves the specified Waveforms in a Text File.",
        ParameterNames = {"Channels to save", "File Extension", "Plot Waveforms?"},
        ToolTips = {"Comma separated list of Channels (1...4) to save.", "", ""},
        DefaultValues={"1,2", ".wv.txt", "true"})
    public float[][] SaveWaveform(String ChannelsToSave, String FileExtension, 
                                  boolean PlotWaveforms) 
           throws IOException, ScriptException {
        
        
        //////////////////
        // local variables
        boolean Is16Bit;
        boolean IsAveraging = false;
        boolean WasRunning;
        int NrDataPoints = 0;
        int AverageCount = 0;
        float TimeBase = 0;
        float RecordLength = 0;
        float TV_Matrix[][] = null;
        String dummy;
        String FileName = null;
        String FileHeader = null;
        final String NewLine = System.getProperty("line.separator");
        
        
        // just return if in no-Communication mode
        if (inNoCommunicationMode())
            return TV_Matrix;
        
        
        // make a list of Channels that need to be saved
        List<String> ChannelNames = Arrays.asList( ChannelsToSave.split(",") );
        
        
        // tic
        long tic = System.currentTimeMillis();
        
        // lock the Lock
        m_LockTMCTLlib.lock();
        

        // it's good to catch all exceptions to be able to release the lock in 
        // case of an Exception (probably not necessary because if an error occured,
        // scripting stops and in the next run, a new Lock is instantiated. If, however,
        // a second thread request this instrument, this thread can at least finish
        // nicely if the lock is released).
        try {
                      
            // query Condition register to find if acquisition is in progress
            dummy = QueryInstrument(":Status:Condition?");
            int ConditionRegister = getInteger( dummy );
            
            // remember if aquisition is in progres (running)
            if ( (ConditionRegister & 0x01) == 1) {
                WasRunning = true;
            } else {
                WasRunning = false;
            }
            
            // stop aquisition (required or else the waveform cannot be read correctly)
            SendToInstrument("STOP");
            
            // iterate through all Channels to save
            for (int ch=1; ch <= ChannelNames.size(); ch++) {
                
                // get current Channel name and remove whitespaces
                String CurrentChannel = ChannelNames.get(ch-1).trim();

                // set Waveform to query
                SendToInstrument("Waveform:Trace " + CurrentChannel);

                
                /////////////////////////
                // acquire one-time data
                // also build the FileHeader (:Waveform:Trace has to be called before I guess)
                if (ch == 1) {
                    
                    // query nr. of data points in current trace
                    dummy = QueryInstrument(":Waveform:Length?").replaceFirst("(:.+)+\\s", "");
                    NrDataPoints = getInteger(dummy);

                    // log them
                    m_Logger.log(Level.FINER, "Nr Data Points to save = {0}\n", NrDataPoints);

                   
                    // get the timebase (Note that 10 divisions are recorded)
                    dummy = QueryInstrument(":Timebase:TDiv?").replaceFirst("(:.+)+\\s", "");
                    TimeBase = getFloat(dummy);
                    
                    // get the recrod length (125kW, 250kW, ...)
                    dummy = QueryInstrument(":ACQuire:RLENgth?").replaceFirst("(:.+)+\\s", "");
                    RecordLength = getFloat(dummy);
                    
                    // reserve space for the result matrix
                    // T ch1 ch2 ...
                    TV_Matrix = new float[ChannelNames.size()+1][NrDataPoints];
                    
                    // fill Time into result matrix
                    for (int t=0; t < NrDataPoints; t++) {
                        TV_Matrix[0][t] = TimeBase * 10f / RecordLength * t;
                    }
                    
                    
                    // get Aquisition mode (Normal, Averaging, Envelop)
                    String AquisitionMode = QueryInstrument(":Acquire:Mode?").replaceFirst("(:.+)+\\s", "");

                    // remember if Averaging is enabled on the scope
                    if ( AquisitionMode.toUpperCase().contains("AVER")) {
                        IsAveraging = true;
                        m_GUI.DisplayStatusMessage("saving average\n", false);
                    } else {
                        IsAveraging = false;
                        m_GUI.DisplayStatusMessage("saving record 0\n", false);
                    }
                    
                    
                    // choose to query the most recent trace or the averaged trace
                    if (IsAveraging) {
                        
                        // get Averaging count
                        dummy = QueryInstrument(":Acquire:Average:EWeight?").replaceFirst("(:.+)+\\s", "");
                        AverageCount = getInteger(dummy);
                        int NrWaveformsToSave = AverageCount;
                                                
                        // get the number of "valid" waveforms
                        // Note that this call returns 0 even without a valid 
                        // waveform on the scope it returns 0 (also checked with 
                        // Visual C++, it return 0 not NaN)
                        dummy = QueryInstrument(":WAVeform:RECord? MINimum").replaceFirst("(:.+)+\\s", "");
                        int NrRecordedWaveforms = -1 * getInteger(dummy) + 1;
                        
                        // check if as many waveforms have been aquired as specified
                        // in the Averaging count
                        if (NrRecordedWaveforms < AverageCount) {
                            m_GUI.DisplayStatusMessage("Warning: Expected "
                                    + AverageCount + " waveforms but only " +
                                    NrRecordedWaveforms + " waveforms were recorded (and saved). Did "
                                    + "the scope miss a Trigger?\n", false);
                            
                            // only save as many waveforms as have been recorded
                            NrWaveformsToSave = NrRecordedWaveforms;
                        }
                        
                        SendToInstrument(":Waveform:Record AVERAGE");

                        // it's required to set the highlight display
                        SendToInstrument(":History:Current:Mode AVERAGE");

                        // it's required to set the record numbers to be averaged
                        SendToInstrument(":History:Current:Display 0,-" + (NrWaveformsToSave-1)); // TODO was AverageCount+1); it's always wrong, needs to be -1

                        // choose to display history waveforms in HalfTone mode
                        SendToInstrument(":History:Current:DMode AHTone");

                    } else {
                        
                        // query the Extended Event Register
                        dummy = QueryInstrument(":STATUS:EESR?").replaceFirst("(:.+)+\\s", "");
                        int ExtendedEventRegister = getInteger(dummy);
                        
                        // check if a Trigger was received:
                        // The Transition filter is set to FALL, hence, after a
                        // Trigger has been received, bit 3 in the EER is set to 1.
                        // That is how I view these settings, in reality, the EER bit
                        // is set to 1 when still waiting for a Trigger.
                        if ( (ExtendedEventRegister & 4) == 4) {
                            // no Trigger was received
                            String str = "Yokogawa DL9000 could not save the waveform(s) because\n"
                                    + "the oscilloscope was not triggered.\n";
                            throw new ScriptException(str);
                        }
                        
                        // choose the last waveform to save
                        SendToInstrument(":Waveform:Record 0");
                    }
                    
                    
                 
                    
                    // reformat the current date and time and add to File Header
                    SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd_HHmmss");
                    String DateString = sdf.format(Calendar.getInstance().getTime());
                    FileHeader = "% iC: date and time measured:" + DateString;
                    
                    // add more header data
                    FileHeader += "% Time Base\t" + TimeBase + "\t[sec/division]" + NewLine;
                    FileHeader += "% Sample Rate\t" + QueryInstrument(":Waveform:SRate?")
                                .replaceFirst("(:.+)+\\s", "") + "\t[samples/sec]" + NewLine;
                    FileHeader += "% Record Length\t" + RecordLength + "\t[samples]" + NewLine;
                    FileHeader += "% Aquisition Mode\t" + AquisitionMode + NewLine;
                    if (IsAveraging)
                        FileHeader += "% Averaging Count/Weight\t" + QueryInstrument(":Acquire:Average?") + NewLine;
                    FileHeader += "% Trigger Position\t" + QueryInstrument(":Waveform:Trigger?")
                                .replaceFirst("(:.+)+\\s", "") + "\t[point number]" + NewLine;
                    FileHeader += "% Trigger Position2\t" + QueryInstrument(":TRIGger:POSition?")
                                .replaceFirst("(:.+)+\\s", "") + "\t[%]" + NewLine;
                    FileHeader += "% Trigger Delay Time\t" + QueryInstrument(":TRIGger:DELay:TIME?")
                                .replaceFirst("(:.+)+\\s", "") + "\t[s]" + NewLine;
                    FileHeader += "% Trigger Hold Off\t" + QueryInstrument(":TRIGger:HOLDoff?")
                                .replaceFirst("(:.+)+\\s", "") + "\t[s]" + NewLine;
                    //FileHeader += "% Trigger Setup\t" + QueryInstrument(":Trigger?") + NewLine;
  
                }
                
                // query Channel setup
                dummy = QueryInstrument(":Channel" + CurrentChannel + "?");
                
                // add channel config to File Header
                FileHeader += "% Setup Channel " + CurrentChannel + "\t" + dummy
                        + " (Averaging last " + AverageCount + " records)" + NewLine;
                

                // get the offset
                dummy = QueryInstrument("Waveform:Offset?").replaceFirst("(:.+)+\\s", "");
                float Offset = getFloat(dummy);

                // get the Scale ( V/division )
                dummy = QueryInstrument("Waveform:Range?").replaceFirst("(:.+)+\\s", "");
                float Scale = getFloat(dummy);


                // set to receive entire waveform
                SendToInstrument(":Waveform:Start 0;End " + (NrDataPoints-1));


                // request to send Waveform
                SendToInstrument("Waveform:Send?");

                // receive the waveform as Block Data
                ByteBuffer DataBytes = m_TMCTL_Driver.ReceiveBlockData();

                // initialize starting position
                DataBytes.rewind();


                // query number of bits per data point of the currently selected trace
                dummy = QueryInstrument("Waveform:Bits?").replaceFirst("(:.+)+\\s", "");

                // 8 bit or 16 bit data?
                if (dummy.equals("16")) {
                    Is16Bit = true;
                } else {
                    Is16Bit = false;
                }

                // convert into voltage values
                for (int t=0; t < NrDataPoints; t++) {

                    // get next voltage
                    if (Is16Bit) {
                        TV_Matrix[ch][t] = Scale * DataBytes.getShort() / 3200f + Offset;
                    } else {
                        TV_Matrix[ch][t] = Scale * DataBytes.get() / 12.5f + Offset;
                    }
                }
            }   // end iterate through all channels to save
            
        } catch (ScriptException ex) {
            String str = "Could not interpret the DL9000's answer.\n"
                    + ex.getMessage();
            throw new ScriptException(str);

        } catch (IOException ex) {
            // just re-throw the exception
            throw ex;
            
        } finally {
            // unlock the Lock (probably not necessary as if an error occured
            // scripting stops and in the next run, a new Lock is instantiated)
            m_LockTMCTLlib.unlock();
        }
        
        // toc
        long dT = System.currentTimeMillis() - tic;
        m_GUI.DisplayStatusMessage("Time to receive data: " + dT + "ms\n", false);
        

        /////////////////////
        // plot the waveforms
        iC_ChartXY Chart = null;
        if (PlotWaveforms) {
            
            // make a new XYChart object
            Chart = new iC_ChartXY("Waveforms " + FileExtension, 
                    "Time [sec]", "Voltage [V]", true  /*legend*/, 0, 0);
            
            // iterate through all Channels to Plot
            for (int ch=1; ch <= ChannelNames.size(); ch++) {
            
                // get current channel name
                String CurrentChannel = ChannelNames.get(ch-1);
                
                // add a new trace (series)
                SeriesIdentification Series1 = Chart.AddXYSeries("Ch " + CurrentChannel, 
                        0, false, true, Chart.LINE_SOLID, Chart.MARKER_NONE);


                // disable event notification upon adding new data points
                Chart.setNotify(false);


                // calc "sparsity"
                // JFreeChart cannot handle large datasets nicely, so only plot
                // approx. 2000 datapoints per trace
                int i_inc = 1;
                int PlotSparsity = m_iC_Properties.getInt("YokogawaDL9000.SaveWaveform.PlotSparsity", 2311);
                if (NrDataPoints > PlotSparsity) {
                    i_inc = Math.round(NrDataPoints / PlotSparsity);
                }

                // add data points
                for (int i=0; i<NrDataPoints; i += i_inc) {
                    Chart.AddXYDataPoint(Series1, TV_Matrix[0][i], TV_Matrix[ch][i]);
                }
            }
            
            // re-enable event notification (surprisingly, this also updates the chart)
            Chart.setNotify(true);
            
            // it appears as if under certain circumstances SaveAsPNG 
            // raises an Exception (null). I suspect that the chart
            // is still updating in those cases, hence save the
            // chart after saving the data
        }
        
        
        // toc
        dT = System.currentTimeMillis() - tic - dT;
        m_GUI.DisplayStatusMessage("Time to plot the data: " + dT + "ms\n", false);
        
        
        //////////////////////////
        // save the data to a file
        String line;
        
        // get preference if Time should be saved in the File as well
        boolean IncludeTime;
        if (m_iC_Properties.getInt("YokogawaDL9000.SaveWaveform.IncludeTime", 0) == 1) {
            IncludeTime = true;
        } else {
            IncludeTime = false;
        }
        
        // get the filename
        FileName = m_GUI.getFileName(FileExtension);
        
        try {
            // open the file for writing
            BufferedWriter file = new BufferedWriter( new FileWriter(FileName) );
            
            // write File Header
            file.write(FileHeader);
            
            // write "Time" if so desired
            if (IncludeTime) {
                line = "Time [sec]\t";
            } else {
                line = "";
            }
            
            // write Channel names
            for (int ch=1; ch <= ChannelNames.size(); ch++) {
                line += (ch==1 ? "":"\t") + "Ch" + ChannelNames.get(ch-1);
            }
            file.write(line);
            file.newLine();
            
            // go through all data points
            for (int T=0; T < NrDataPoints; T++) {
                
                // write Time if so desired
                if (IncludeTime) {
                    line = TV_Matrix[0][T] + "\t";
                } else {
                    line = "";
                }
                    
                // go through all channels
                for (int ch=1; ch <= ChannelNames.size(); ch++) {
                    line += (ch==1 ? "" : "\t") + TV_Matrix[ch][T];
                }
                
                // write the line
                file.write(line);
                file.newLine();
            }
            
            // close file
            file.close();
            
        } catch (IOException ex) {
            String str = "Could not write to the file\n" + FileName + "\n"
                    + "Error message:\n" + ex.getMessage();
                throw new ScriptException(str);
        }
        
        if (PlotWaveforms) {
            // get File Name to save the chart
            FileName = m_GUI.getFileName(FileExtension + ".png");

            // save the chart
            Chart.SaveAsPNG(new File(FileName), 1024, 768);
        }
        
        // toc
        dT = System.currentTimeMillis() - tic - dT;
        m_GUI.DisplayStatusMessage("Time to save the data: " + dT + "ms\n", false);
        
        
        // resume aquisition if aquisition was running before
        if (WasRunning) {
            Start();
        }
        
        
        // return results for nextr script command
        return TV_Matrix;
    }// </editor-fold>
    
    
    /**
     * Starts data acquisition and waits until data acquisition is started.
     * 
     * @throws IOException Bubbles up from <code>SendViaTMCTL</code> or
     * from <code>QueryViaTMCTL</code>.
     * @throws ScriptException If the Condition Register could not be queried
     * properly.
     */
    // <editor-fold defaultstate="collapsed" desc="Start">
    @AutoGUIAnnotation(
        DescriptionForUser = "Start Data Acquisition.",
        ParameterNames = {},
        ToolTips = {},
        DefaultValues={})
    public void Start() 
           throws IOException, ScriptException {
        
        
        // exit if in No-Communication Mode
        if (inNoCommunicationMode()) {
            return;
        }
        
        
        boolean IsRunning = false;
                
        // send Start command
        SendToInstrument("START");
        
              
        try {
            do {
                // query Condition register to find if acquisition is in progress
                int ConditionRegister = getInteger( QueryInstrument(":Status:Condition?") );

                // remember if aquisition is in progres (running)
                if ( (ConditionRegister & 0x01) == 1) {
                    // yes, it's running
                    IsRunning = true;
                }
                
                // wait a bit
                try { Thread.sleep(100); } catch (InterruptedException ignore) {}
                
            } while (!IsRunning);
            
        } catch (ScriptException ex) {
            // one might loose a few pulses if this method returns prematurely,
            // so raise an Exception
            String str = "Error evaluating if Data Acquisition has started:\n"
                    + ex.getMessage();
            
            throw new ScriptException(str);
        }
        
        // query the Extended Event Register to clear it
        // (used to decide if a Trigger was received in saveWaveform)
        QueryInstrument(":STATUS:EESR?");
        
    }// </editor-fold>
    
    
    /**
     * Sets the Acquisition Mode to either Normal or Average; it also sets the
     * Average Count
     * 
     * @param AverageCount If 1, Acquisition Mode Normal is selected, otherwise
     * Average is selected and the selected Average Count is set
     * 
     * @throws IOException Bubbles up from <code>SendToInstrument</code>
     * @throws DataFormatException When the Syntax-check fails
     */
    // <editor-fold defaultstate="collapsed" desc="setAcquisitionMode">
    @AutoGUIAnnotation(
        DescriptionForUser = "Sets the acquisition mode to Normal of Average.",
        ParameterNames = {"Average Count"},
        ToolTips = {"<html>1 ... sets the ACQ Mode to Normal<br>2^n ... selects Average with Average Count (2...65535)</html>"},
        DefaultValues={"16"})
    public void setAcquisitionMode(int AverageCount) 
           throws IOException, DataFormatException {
        
        ///////////////
        // Syntax-Check
        
        if (AverageCount < 1) {
            String str = "Average Count needs to be >=1\n";
            throw new DataFormatException(str);
        }
        
        
        // is it 2^n with n=1..16
        double n = Math.log(AverageCount) / Math.log(2);

        if ( n != Math.round(n)) {
            String str = "Average count must be a value of 2^n with n=1...16";
            throw new DataFormatException(str);
        }
        
        
        // return if in Syntax-check mode
        if (inSyntaxCheckMode())
            return;
        
        // exit if in No-Communication Mode
        if (inNoCommunicationMode()) {
            return;
        }
        
        // set Acquisition Mode
        SendToInstrument(":Acquire:Mode " + (AverageCount==1 ? "Normal" : "Average") );
        
        // set average count
        if (AverageCount > 1) {
            SendToInstrument(":Acquire:Average:Count " + AverageCount);
        }
        
    }// </editor-fold>

    
    /**
     * The DL9000 can calculate certain parameters. This method allows to query
     * these parameters from the scope. The 'Param' function must be enabled
     * on the scope or else the parameters are not available.
     * 
     * @param Trace Specifies which waveform should be used to calculate the
     * parameter
     * @param Area The scope allows to specify two different areas (aka time spans)
     * over which the parameters are calculated. Areas 1 spans per default
     * the entire 10 time divisions
     * @param ParameterName The name of the parameter to calculate. Can be 
     * BURSt|CMEan|COUNt|CRMS|CSDeviation|DELay|DT|DUTYcycle|FALL|FREQuency|HIGH|
     * HILow|LOW|MAXimum|MEAN|MINimum|NOVershoot|NWIDth|PERFrequency|PERiod|POVershoot|
     * PTOPeak|PWIDth|RISE|RMS|SDEViation|TYCInteg|TYINteg|V1|V2.
     * @return The numerical value of the parameter calculated by the scope
     * @throws IOException Bubbles up from <code>QueryInstrument</code>
     * @throws ScriptException Bubbles up from <code>getFloat</code>
     */
    // <editor-fold defaultstate="collapsed" desc="getParameter">
    @AutoGUIAnnotation(
        DescriptionForUser = "Queries the specified parameter calculated by the scope.",
        ParameterNames = {"Trace", "Area", "ParameterName"},
        ToolTips = {"Which waveform should be used to calculate the parameter? Can be 1-4",
            "Over which area (time span) should the parameter be calculated? Can be 1 or 2",
            "<html>The name of the parameter to calculate. Can be:<br>"
            + "BURSt|CMEan|COUNt|CRMS|CSDeviation|DELay|DT|DUTYcycle|FALL|FREQuency|HIGH|<br>"
            + "HILow|LOW|MAXimum|MEAN|MINimum|NOVershoot|NWIDth|PERFrequency|PERiod|POVershoot|<br>"
            + "PTOPeak|PWIDth|RISE|RMS|SDEViation|TYCInteg|TYINteg|V1|V2.</html>"},
        DefaultValues={"1", "2", "PTOP"})
    public float getParameter(int Trace, int Area, String ParameterName) 
           throws IOException, ScriptException {
        
        // local variables
        float ret = Float.NaN;
        
        // TODO 1* add Syntax check to getParameter
        // Syntax-Check for Area 1 or 2
        // Trace 1..4 or however much are shown
        // ParameterName can be 
        
        // exit if in No-Communication Mode
        if (inNoCommunicationMode()) {
            return ret;
        }
        
        // build the command String
        String cmd = ":MEASure:TRACe" + Trace + ":AREA" + Area + ":"
                + ParameterName + ":Value?";
        String ans = QueryInstrument(cmd).replaceFirst("(:.+)+\\s", "");
        //m_GUI.DisplayStatusMessage("DL9000 says: " + ans + "\n");
        
        // convert to a double
        ret = getFloat(ans);
        
        // return 
        return ret;
    }//</editor-fold>
    
    
    /**
     * Finds the next largest time base (time per division) and sets it on the 
     * scope. Example: When this method is called with 1.2e-3 the time base is 
     * set to 2e-3 so that the desired waveform is shown in it's entirety.
     * 
     * @param TimeBase The time base (time per division) to be set on the scope
     * @return The next larger time base setting available on the scope
     * @throws IOException Bubbles up from <code>SendToInstrument</code>
     */
    // <editor-fold defaultstate="collapsed" desc="setTimeBase">
    @AutoGUIAnnotation(
        DescriptionForUser = "Set the Time Base (seconds / division).",
        ParameterNames = {"Time Base"},
        ToolTips = {"Sets the Time Base to the smalles one available that is larger than the specified"},
        DefaultValues={"1e-3"})
    public double setTimeBase(double TimeBase) 
           throws IOException {
        
        
        // get exponent
        double Exp = Math.log10(TimeBase);
        
        // get "next smaller" exponent
        int Exponent = (int)Math.floor(Exp);       
        
        // calc "significant"
        double Digits = TimeBase / Math.pow(10, Exponent);
        
        // find net largets significant
        double NewDigits = Digits;
        if (Digits > 1) NewDigits = 2;
        if (Digits > 2) NewDigits = 5;
        if (Digits > 5) NewDigits = 10;
        
        // calc the new timebase
        TimeBase = NewDigits * Math.pow(10, Exponent);
        
        // check for lowest time base
        if (TimeBase < 500e-12) {
            TimeBase = 500e-12;
        }
        
        // check for highest time base
        if (TimeBase > 50) {
            TimeBase = 50;
        }
        
        // set the TimeBase on the scope
        String str = String.format(":TIMEBASE:TDIV %E", TimeBase);
        SendToInstrument(str);
        
        // also return the TimeBase (e.g. for JUint testing)
        return TimeBase;
    }//</editor-fold>
    
//    public void printEER() throws IOException, ScriptException {
//
//        // query the Extended Event Register
//        String dummy = QueryInstrument(":STATUS:EESR?").replaceFirst("(:.+)+\\s", "");
//        int ExtendedEventRegister = getInteger(dummy);
//        
//        m_GUI.DisplayStatusMessage("EER= " + ExtendedEventRegister + "\n", false);
//    }
}
