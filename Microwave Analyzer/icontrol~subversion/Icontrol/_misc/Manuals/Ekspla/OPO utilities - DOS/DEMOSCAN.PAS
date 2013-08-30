{PG401}
uses dos,crt;

{---------------------------Serial port-----------------------------------}
const
  INTCTLR   = $21;
  BufLen    = $100;
  MinV:integer=420; MaxV=680; {signal wave bounds}
  MinI=740; MaxI=2300; {idler wave bounds}
  MinD=2301; MaxD=10000; {mixed wave bounds if exists DFG stage}
  {for communications:}
  Start='['; Fin=']'; SendAdr='\'; RecAdr='/'; Space=' '; DeviceAdr=':';

var
  AsyncVector : pointer;
  Buffer    : array [1..BufLen] of Char;
  CircOut, CircIn, CharsInBuf : word; {pointers of circular buffer}
  Base,DataPort, LCR, MCR, IER, LSR, PortNr : word;
  InterNr, IntMask: byte;
  C:char;
  Name, DeviceName:string;

procedure DirectOut (X: char); {Sends character in to serial port}
begin
repeat until (port [LSR] and $20) = $20; {to wait until send previuos byte}
port [DataPort] := ord (X);
end;

procedure ClearBuffer;
begin
CircIn     := 1;
CircOut    := 1;
CharsInBuf := 0;
FillChar(Buffer, SizeOf(Buffer),0);
end;

procedure AsyncInt; Interrupt;
var B:byte;
begin
inline ($FB);
if (CharsInBuf < BufLen) then
  begin  {puts char in to Buffer}
  Buffer[CircIn] := Char(Port[DataPort]);
  if (CircIn < BufLen) then
    inc(CircIn,1)
  else
    CircIn:= 1;
  Inc(CharsInBuf,1);
  end
else  B:=Port[DataPort];
port [MCR] :=$0B;
inline ($FA);
port [$20] :=$20;
End;

procedure EnablePorts(PortNr,Baud:word);
  {Sets int vector, baud rate, port number}
var
  B : Byte; Dv: word;
begin
case PortNr of
  1:Base:=$3F0;
  2:Base:=$2F0;
  3:Base:=$3E0;
  4:Base:=$2E0;
  end;
DataPort  :=Base+$8;
IER       :=Base+$9;
LCR       :=Base+$B;
MCR       :=Base+$C;
LSR       :=Base+$D;
if (PortNr=1)or(PortNr=3) then
  begin
  IntMask   := $EF;
  InterNr   := $0C;
  end
else
  begin
  IntMask   := $F7;
  InterNr   := $0B;
  end;
ClearBuffer;
GetIntVec(InterNr, AsyncVector);
SetIntVec(InterNr,@AsyncInt);
{Baud setting}
port [LCR] :=$80;
Dv:=115200 div Baud;
port[DataPort]:=lo(Dv);
port[IER]:=hi(DV);
{Data=8 bits, Stop=1, Parity=none}
port [LCR] :=$03;

B:= Port[INTCTLR];
B:= B and IntMask;
port [INTCTLR] :=B;

port [MCR] :=$0B;
port [IER] :=$01;
Port [$20]:= $20;
End;

function GetCharInBuf: Char; {Gets one Char from circular Buffer}
var X: integer;
begin
if CharsInBuf > 0 then
  begin
  GetCharInBuf := Buffer[Circout];
  if CircOut < BufLen then inc (CircOut,1)
    else CircOut:= 1; Dec(CharsInBuf,1);
  end;
end;

procedure DisablePorts;
{returns old int vector}
var
  B: byte;
begin
B:= port[INTCTLR];
B:= B or (not IntMask); {disable interrupt from serial port}
port[INTCTLR]:= B;
port[IER] := $0;
port[$20] := $20;
SetIntVec(InterNr,AsyncVector);
end;
{-------------------------------------------------------------------------}
const     { default values}
ScanFrom:real=420;
ScanTo:real=680;
ScanStep:real=5;
ScanTime:word=3;
{------------------------Mini-terminal------------------------------------}
procedure Terminal;
var C:char;
begin
clrscr;
writeln('Press ESC to Quit MiniTerminal');
C:='A';
repeat
if keypressed then
  begin
  C:=ReadKey;
  DirectOut(C);
  end;
if CharsInBuf>0 then
  write(GetCharInBuf);
until ord(C)=27; {ESC code}
end;
{-------------------------------------------------------------------------}

{---------------------Change scan parameters------------------------------}

procedure WriteScanParameters;
begin
writeln('Scan From     = ',ScanFrom:6:1,' nm');
writeln('Scan To       = ',ScanTo:6:1,' nm');
writeln('Scan Step     = ',ScanStep:6:1,' nm');
writeln('Scan Interval = ',ScanTime,' s');
end;

procedure EnterScanParameters;
begin
writeln;
write('Scan From (valid ',MinV,'..',MaxD,') nm - ');
ReadLn(ScanFrom);
write('Scan To (valid ',MinV,'..',MaxD,') nm - ');
ReadLn(ScanTo);
write('Scan Step (valid >0) nm - ');
ReadLn(ScanStep);
write('Scan Interval (valid >0) sec -');
Readln(ScanTime);
end;

function CheckBounds(Figure:real):boolean;
begin
if (Figure>MaxD) or (Figure<MinV) or ((Figure>MaxV) and (Figure<MinI))
  then CheckBounds:=false else CheckBounds:=true;
end;

{-------------------------------------------------------------------------}

{--------------------------- Scaning -------------------------------------}

procedure Timer(DelayInSeconds:word); {Holds for DelayInSeconds time}
var Time, NewTime:longint; hour, Minute,Second,Sec100:word; C:char;

begin
GetTime(Hour,Minute,Second,Sec100);
Time:=36000*Hour+600*Minute+10*Second+Sec100;
NewTime:=Time+10*DelayInSeconds;
repeat
GetTime(Hour,Minute,Second,Sec100);
Time:=36000*Hour+600*Minute+10*Second+Sec100;
until (Time>=NewTime) or keypressed;
end;

procedure SendWavelength(W:real);
  {Sends command line in form [MS:W2/Snnn ]}
var I:integer; S,Num:string;
begin
str(W:1:1,Num); {converts figure in to string}
S:='['+DeviceName+':W1/S'+Num+' \PC]';
for I:=1 to length(S) do DirectOut(S[I]);
end;

procedure WaitForLineTerminator; {waits for sequence 'PC:' from serial port}
var C:char;
begin
repeat
if CharsInBuf>0 then
  begin
  Name:=copy(Name,2,2);{to cut first sym}
  C:=GetCharInBuf;
  Name:=Name+C;
  end
until (Name='PC:') or KeyPressed;
end;

procedure Scaning;
var wavelength, Step, OldWavelength:real;  C:char;

begin
clrscr;
writeln('Scaning Mode. Press any Key to Quit');
WriteScanParameters;
Writeln;
Writeln('Now at        nm');

if ScanFrom<ScanTo then {determine scaning direction}
  Step:=ScanStep
else
  Step:=-ScanStep;
wavelength:=ScanFrom;
repeat
  gotoxy(8,7); write(wavelength:6:1);
  SendWavelength(wavelength); {Sends command Set Wavelength}
  WaitForLineTerminator;   {Waits util command is performt}
  Timer(ScanTime);         {Waits Scan Interval}
  OldWavelength:=wavelength;
  repeat
    wavelength:=wavelength+Step;  {Next wavelength}
  until (MaxV>=wavelength)or(wavelength>=MinI);
   {skip discontinuity interval if exists}
{ if ((OldWavelength>=10001)and(Wavelength<10001)) or
    ((OldWavelength<10001)and(Wavelength>=10001)) then
 begin
 gotoxy(5,10);writeln('Please, change crystal and press any key ..');
 readkey;
 gotoxy(5,10);writeln('                                           ');
 end; } {only for two crystals DFG stage}
until ((Step<0) and (wavelength<ScanTo)) or
   ((Step>0) and (wavelength>ScanTo)) or KeyPressed;
if KeyPressed then C:=ReadKey;
end;

{-------------------------------------------------------------------------}

procedure MainMenu;
var N:char;
begin
repeat
  clrscr;
  write('Port Nr =',PortNr);
  writeln('   Device: ',DeviceName);
  writeln;
  WriteScanParameters;
  writeln('---------------------');
  writeln('Select:');
  writeln('1)   Change Scan parameters');
  writeln('2)   Begin Scanning');
  writeln('3)   MiniTerminal');
  writeln('ESC  Quit');
  N:=readkey;
  case N of
    '1':begin
        EnterScanParameters;
        if  not CheckBounds(ScanFrom) or
              not CheckBounds(ScanTo) or
                (ScanStep<=0) or (ScanTime<1) then
           begin
           writeln('Invalid parameter. Press any Key');
           ScanFrom:=MinV;
           ScanTo:=MaxV;
           ScanStep:=5;
           ScanTime:=3;
           C:=readKey;
           end;
        end;
    '2':Scaning;
    '3':Terminal;
    end;
until  ord(N)=27;
end;

begin
Name:='   ';
clrscr;
Writeln('Demo program for PG401 VIR/DFG/SHG');
writeln;
write('Enter serial port number. Valid 1..4 - ? ');
Readln(PortNr);
write('Enter device name. Only Enter for D1 - ?');
Readln(DeviceName);
if DeviceName='' then DeviceName:='D1';
EnablePorts(PortNr,38400);
MainMenu;
DisablePorts
end.
