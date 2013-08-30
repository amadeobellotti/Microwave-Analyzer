Instrument Control (iC) with JAVA
=================================

Control (scientific) instruments over the GPIB bus with this JAVA program.

It has become common practice to automate data acquisition from programmable 
instrumentation and a range of different software solutions fulfill this task. 
Many routine measurements require sequential processing of certain tasks, for 
instance to adjust the temperature of a sample stage, take a measurement, and 
repeat that cycle for other temperatures. Instrument Control (iC) is an 
open-source Java program that processes a series of text-based commands that 
define the measurement sequence. These commands are in an intuitive format 
which provides great flexibility and allows quick and easy adaptation to 
various measurement needs. For each of these commands, the iC-framework calls 
a corresponding Java method that addresses the specified instrument to perform 
the desired task. The way iC was designed enables one to quickly extend the 
functionality of Instrument Control with minimal programming effort or by 
defining new commands in a text file without any programming.

KPP dedicates this work in devotion to Bhagavan Sri Sathya Sai Baba.


Contact: pernstich@alumni.ethz.ch (Kurt Pernstich)



Online resources 
----------------

Project website: http://kenai.com/projects/icontrol

Manual & javadoc: http://www.icontrol.kenai.com

Tutorial Videos:
http://kenai.com/projects/icontrol/downloads/directory/Tutorial_Videos



Installation
------------ 

To run the program the Java SE Runtime Environment
must be installed. Get the latest version from www.java.com.

Copy the 'iC' directory to your home directory. To find the path to your home
directory, start the program and it will show the path.

In general it should be enough to double click the Icontrol.jar.

If not, try the command java -jar Icontrol.jar from the command line. The
command line is also helpful to see error messages that might appear.

See also the detailed installation instructions in the documentation at
http://icontrol.kenai.com/



System Requirements
------------------- 

A Java SE Runtime Environment (version 6.0 or later [version 5 might also work])
and one of the following GPIB cards with the appropriate drivers installed
should be enough to run the program. At the time of writing, only Windows
operating system is supported.

The Instrument Control (iC) program has been tested with the following configurations:

* Windows XP: National Instruments PCI-GPIB card with NI 488.2 (version 2.4)
drivers - this is the typical configuration and most work was done using this
configuration

* Windows XP: Agilent 82357B USB/GPIB interface with the Agilent IO Suite
(version 15.1) - an *IDN? query worked as expected; no more tests were performed

* Windows XP: Prologix GPIB-USB controller (version 4.2 and 6.0) with the 
direct drivers (version  2.08.08) - the program logged 5 temperatures every
second for 20 h without problems

* Mac OS 10.6: National Instruments GPIB-USB-HS controller with NI 488.2
(version 2.5) - an *IDN? query worked as expected; no more tests were performed


For a list of supported Instruments, please consult the javadoc/manual. The
online version can be found at http://www.icontrol.kenai.com



How to manually change default properties
-----------------------------------------
- In the default include directory (typically "YourHomeDirectory/iC/") add a text
file named "iC_User.properties" and add the key/value pairs as used in 
"iC.properties".
- To view the content of "iC.properties" rename Icontrol.jar to Icontrol.jar.zip,
and inspect icontrol/resources/iC.properties.



Credits
-------
Without the following programs the Instrument Control (iC) program would not
have been possible. Thank's a lot to the developers of these open source and
freeware programs !! 

* Java Native Access (JNA)  http://jna.java.net
* JFreeChart:               http://www.jfree.org/jfreechart
* Apache Commons Math       http://commons.apache.org/math
* RxTx:                     http://rxtx.qbang.org
* Netbeans:                 http://www.netbeans.com
* Kenai:                    http://kenai.com
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
acknowledgement if the software is used. The publication introducing this
program is planned to be published in 2011/12 with K. P. Pernstich as first
author.

This software can be redistributed and/or modified freely under the terms of the
GNU Public Licence and provided that any derivative works bear some notice that
they are derived from it, and any modified versions bear some notice that they
have been modified.

This software is distributed in the hope that it will be useful, but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
PARTICULAR PURPOSE. See the GNU Public License for more details.
http://www.fsf.org
