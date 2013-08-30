The program DEMOSCAN demonstrates how Optical parametric oscillator PGxxx (OPO)
 is controllable from PC.

GETTING READY TO OPERATE

1. Connect one of COM ports in PC to RS232 terminal in OPO. 

2. Turn the OPO on. 

3. Turn on your PC. 

4. Run program DEMOSCAN.EXE. 

5. Enter number of the specific PC port connected to the OPO.

6. Enter the name of OPO. 
As RS232 port allows to control several devices at once, eg PG401 and SHG stage, 
each device owns an unique name composed of two or three symbols. This name is 
used when sending the commands. 
The general format of a command is [Name:Command\SenderName]. In routine 
operation, both 'Name' and 'SenderName' can be omitted if only one device is 
controlled via RS232. Then you do not need to bother about the name at all. Eg, 
the command to set the wavelength to 1000.1 nm will look like [W1/S1000.1]. The 
OPO will reply with [MS:DONE\D1]; here the symbols following '\' are the name.
The DEMOSCAN.EXE uses a full command format (without omissions), so it is 
necessary to enter a correct name - otherwise, the OPO will not perform the 
commands sent. So, if you do not know the name, just press Enter. To find out 
what the name is, select Miniterminal and enter sequence [SAY]. A reply 
[MS:READY\D1] will follow. The symbols after '\' are the name.


After the 6 above steps have been completed, the following menu pops up:

Change Scan parameters
Begin scanning
Miniterminal

here Miniterminal: everything you enter on keyboard is routed to the OPO, and 
feedback from the OPO is shown on PC display.
Note that the OPO responds ONLY to acknowledged commands received.


DEMOSCAN.PAS is program source. It needs compilation with Borland Turbo Pascal 
versions 5.0, 6.0 or 7.0.
The procedures and functions of chapter "Serial port" are analogous to those 
described in "Turbo Pascal 5.5: The Complete Reference" (chapter "Interrupts, 
Telecommunications and Memory-Resident Programs").
