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

import icontrol.IcontrolTests;
import icontrol.IcontrolAppMockup;
import java.io.IOException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for the YokogawaDL9000 class.
 * 
 * @author KPP (Kurt Pernstich: pernstich@alumni.ethz.ch)
 * @version 0.1
 *
 */public class YokogawaDL9000Test extends IcontrolTests {
     

    
    public YokogawaDL9000Test() {
    }

    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }
    
    
    /**
     * Tests the method <code>setTimeBase</code>
     */
    @Test
    public void setTimeBase() 
           throws IOException {
        
        // make a new Yokogawa instance
        YokogawaDL9000 scope = new YokogawaDL9000();
        
        // highest Time Base
        assertEquals(50, scope.setTimeBase(1.2e3), 0);
        
        // lowest Time Base
        assertEquals(500e-12, scope.setTimeBase(1.2e-15), 0);
        
        assertEquals(2e-3, scope.setTimeBase(1.2e-3), 0);
        assertEquals(2e-3, scope.setTimeBase(1.9e-3), 0);
        assertEquals(5e-3, scope.setTimeBase(2.2e-3), 0);
        assertEquals(5e-3, scope.setTimeBase(4.9e-3), 0);
        assertEquals(5e-3, scope.setTimeBase(5.0e-3), 0);
        assertEquals(10e-3, scope.setTimeBase(5.0000001e-3), 0);
        
        assertEquals(1, scope.setTimeBase(1.0), 0);
        assertEquals(2, scope.setTimeBase(1.000000000000001), 0);
        assertEquals(2, scope.setTimeBase(1.999999999999999), 0);
        assertEquals(5, scope.setTimeBase(2.01), 0);
        assertEquals(10, scope.setTimeBase(5.01), 0);
        assertEquals(20, scope.setTimeBase(10.00001), 0);
        assertEquals(20, scope.setTimeBase(19.999999), 0);
        assertEquals(20, scope.setTimeBase(20.0), 0);
        assertEquals(50, scope.setTimeBase(20.1), 0);
        
    }
}
