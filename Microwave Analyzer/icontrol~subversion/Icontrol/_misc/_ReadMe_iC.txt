Instrument Control (iC) with JAVA
=================================

Instrument Control (iC) – An Open-Source Software to Automate Test Equipment

It has become common practice to automate data acquisition from programmable 
instrumentation, and a range of different software solutions fulfill this task. 
Many routine measurements require sequential processing of certain tasks, for 
instance to adjust the temperature of a sample stage, take a measurement, and 
repeat that cycle for other temperatures. Instrument Control (iC) is an 
open-source Java program that processes a series of text-based commands that 
define the measurement sequence. These commands are in an intuitive format which 
provides great flexibility and allows quick and easy adaptation to various 
measurement needs. For each of these commands, the iC-framework calls a 
corresponding Java method that addresses the specified instrument to perform
the desired task. The functionality of iC can be extended with minimal 
programming effort in Java or Python, and new measurement equipment can be 
addressed by defining new commands in a text file without any programming.


KPP dedicates this work in devotion to Bhagavan Sri Sathya Sai Baba.


Contact: pernstich@alumni.ethz.ch (Kurt Pernstich)

If you like iC, please cite: http://dx.doi.org/10.6028/jres.117.010 .



Online resources 
----------------

Project website: http://java.net/projects/icontrol

Manual & javadoc: http://icontrol.java.net

Tutorial Videos:
http://java.net/projects/icontrol/downloads/directory/Tutorial_Videos

Publication: http://dx.doi.org/10.6028/jres.117.010



Installation
------------ 

To run the program the Java SE Runtime Environment (version 6.0 or later) must 
be installed. Get the latest version from www.java.com.

Copy the 'iC' directory to your home directory. To find the path to your home
directory, start the program and it will show the path. While copying the iC
directory to your home folder is recommended, it can also reside in the same
directory as the Icontrol.jar file.

In general it should be enough to double click the Icontrol.jar.

If not, try the command java -jar Icontrol.jar from the command line. The
command line is also helpful to see error messages that might appear.

See also the detailed installation instructions in the documentation at
http://icontrol.java.net/ .



Supported Instruments
---------------------

For a list of supported Operating Systems, GPIB cards and other communication
protocols, as well as a list of supported Instruments please consult the 
javadoc/manual at http://icontrol.java.net/ .




Credits
-------
Without the following programs the Instrument Control (iC) program would not
have been possible. Thank's a lot to the developers of these open source and
freeware programs !! 

* Java Native Access (JNA)  http://jna.java.net
* JFreeChart:               http://www.jfree.org/jfreechart
* Jython:                   http://www.jython.org
* Apache Commons Math       http://commons.apache.org/math
* RxTx:                     http://rxtx.qbang.org
* Netbeans:                 http://www.netbeans.com
* Java.net:                 http://java.net
* KompoZer:                 http://www.kompozer.net

All required files are included in the distribution, and the original files are
included in the Developer's version for convenience.



Disclaimer
----------
This software was developed at the National Institute of Standards and
Technology by a guest researcher in the course of his official duties and with
the partial support of the Swiss National Science Foundation. Pursuant to title
17 Section 105 of the United States Code this software is not subject to
copyright protection and is in the public domain. The Instrument Control (iC)
software is an experimental system. Neither NIST, nor the Swiss National Science
Foundation nor any of the authors assumes any responsibility whatsoever for its
use by other parties, and makes no guarantees, expressed or implied, about its
quality, reliability, or any other characteristic. We would appreciate
acknowledgement if the software is used: http://dx.doi.org/10.6028/jres.117.010 .

This software can be redistributed and/or modified freely under the terms of the
GNU Public Licence and provided that any derivative works bear some notice that
they are derived from it, and any modified versions bear some notice that they
have been modified.

This software is distributed in the hope that it will be useful, but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
PARTICULAR PURPOSE. See the GNU Public License for more details.
http://www.fsf.org
