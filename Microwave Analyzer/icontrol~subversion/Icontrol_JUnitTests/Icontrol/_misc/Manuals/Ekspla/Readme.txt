NL3XX driver library for LabVIEW 7.1.1 

GETTING READY TO OPERATE

1.Connect one of COM ports in PC to RS232 terminal in power supply stand. 

Serial connection cable: the following connections must be made:

1.1.connector "RS232" of NL3XX - RS232 port of PC (9 pin connector)
female DB9 - female DB9
pin 5 - pin 5 (GND)
pin 2 - pin 2 (RxD)
pin 3 - pin 3 (TxD)

1.2.connector "RS232" of NL3XX - RS232 port of PC (25 pin connector)
female DB9 - female DB25
pin 5 - pin 7 (GND)
pin 2 - pin 3 (RxD)
pin 3 - pin 2 (TxD)

1.3.use cable supplied by Ekspla.

2.Launch NL GettingStarted.VI from NL.llb. Check VISA resource name for serial port you use. 
This library uses VISA. You may need to install VISA if you haven't
done this before. 

3.Run VI

4.Connection might be checked with the help of any terminal program.

4.1.data exchange parameters must be set in the following way:
 baud rate: 19200 bd
 data bits: 8
 parity: none
 stop bits: 1

4.2.every message starts with symbol '[' and ends with ']'. Receiving device does not repeat the received symbols. Therefore, in case any terminal program (Hyperterminal...) is used for manual operation, you may switch on the option 'echo typed characters locally' to view symbols.

4.3.general format of a command is [Name:Command\SenderName]. 'Name' for NL series laser is programmed as 'NL'. Use 'PC' as 'SenderName'.
Command to check the connection should be [NL:SAY\PC]. The
laser will reply with [PC:READY\NL].

4.4.command interpretator of NL3XX is case sensitive.

4.5.there are several samples of commands:

[NL:START\PC] start the laser;
[NL:STOP\PC] stop the laser;
[NL:EO/S2\PC] set output mode to Max;
[NL:E1/S1\PC] set output mode to Adj.

See product manual for more details.

