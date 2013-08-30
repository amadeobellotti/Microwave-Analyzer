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
package icontrol.drivers.instruments.SRS;

import icontrol.IcontrolTests;
import icontrol.drivers.instruments.srs.SRS_DS345;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.zip.DataFormatException;
import javax.script.ScriptException;
import junit.framework.Assert;
import org.junit.Test;


/**
 * Tests for the SRS_DS345 class.
 *
 * @author KPP (Kurt Pernstich: pernstich@alumni.ethz.ch)
 * @version 0.1
 *
 */
public class SRS_DS345Test extends IcontrolTests {

    @Test
    public void setARBtoCELIV2() 
           throws DataFormatException, IOException, ScriptException, URISyntaxException {

        // instantiate class
        SRS_DS345 dev = new SRS_DS345();
        

        // disable Syntx-Check Mode
        setSyntaxCheckMode(false);
        
        // disable No-Communication-Mode
        setSimulationMode(false);

        ///////////////////////
        //tests with T/V file 2
        
        // Vpeak as usual (negative)
        dev.setARBtoCELIV2(10, 10000, -0.7, 10, "OPV1_Device4_ResTest_merged.txt");
        
        if (true) {
            WaitForUser();
            return;
        }
        

        ///////////////////////
        //tests with T/V file 1     
        
        
        // test comments in file
        dev.setARBtoCELIV2(10, 10000, -0.7, 10, "full_merged_pV comments.txt");
        
        if (true) {
            WaitForUser();
            return;
        }
        
        // test file header with labels
        //dev.setARBtoCELIV2(10, 10000, -0.7, 10, "full_merged_pV comments labels.txt");

       
        // Vpeak as usual (negative)
        dev.setARBtoCELIV2(10, 10000, -0.7, 10, "full_merged_pV.txt");
        
        
        // Vpeak unusual sign (positive)
        dev.setARBtoCELIV2(10, 10000, 0.7, 10, "full_merged_pV.txt");
        
        // Vpeak as usual (negative) - same Tfall
        dev.setARBtoCELIV2(10, 10000, -0.7, 0, "full_merged_pV.txt");
        
        // Vpeak unusual sign (positive) - same Tfall
        dev.setARBtoCELIV2(10, 10000, 0.7, 0, "full_merged_pV.txt");
        
        // Vpeak as usual (negative) - longer Tdelay
        dev.setARBtoCELIV2(500, 10000, -0.7, 10, "full_merged_pV.txt");
        
        // Vpeak unusual sign (positive) - longer Tdelay
        dev.setARBtoCELIV2(500, 10000, 0.7, 10, "full_merged_pV.txt");
        
        // Vpeak as usual (negative) - longer than 40MHz
        dev.setARBtoCELIV2(1000, 1000, -0.7, 0, "full_merged_pV.txt");
        
        // Vpeak unusual sign (positive) - longer than 40 MHz
        dev.setARBtoCELIV2(1000, 1000, 0.7, 0, "full_merged_pV.txt");
        
        
        // should display a warning because Vpeak < max photovoltage
        dev.setARBtoCELIV2(10, 1000, -0.1, 0, "full_merged_pV.txt");

        
        // test File Not Found
        try {
            dev.setARBtoCELIV2(10, 1000, 0.7, 0, "FileNotThere.txt");
            
            // fail the test if no Exception is thrown
            Assert.fail("Expected an Exception for not finding the file");
        } catch (DataFormatException ignore) {}

        
        // test where Tdelay is larger than measured photovoltage time span
        try {
            dev.setARBtoCELIV2(10e6, 1000, 0.7, 0, "full_merged_pV.txt");
            
            // fail the test if no Exception is thrown
            Assert.fail("Expected an Exception as Tdelay is too large");
        } catch (DataFormatException ignore) {}
        
        
        
        WaitForUser();

    }

}
