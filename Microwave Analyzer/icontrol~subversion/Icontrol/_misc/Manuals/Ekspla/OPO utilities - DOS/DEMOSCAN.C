/*PG401 VIR+SHG*/
/* tested with Borland C++ 4.5, Large memory model */

#define LOBYTE(w)           ((unsigned char)(w))
#define HIBYTE(w)           ((unsigned char)((unsigned int)(w) >> 8))

#include <dos.h>
#include <mem.h>
#include <conio.h>
#include <stdio.h>
#include <string.h>

/*---------------------------Serial port-----------------------------------*/
/*interrupt controller registers*/
#define  INTCTLR   0x21
#define  INTFLAG   0x20
/* receiver buffer size */
#define  BufLen   0x200

const  int MinV=420, MaxV=680,    /*signal wave bounds*/
           MinI=740, MaxI=2300,   /*idler wave bounds*/
           MinD=2300, MaxD=10000; /*mixed wave bounds if exists DFG stage*/

/* default scaning parameters*/
unsigned int ScanFrom=420,
             ScanTo=800,
             ScanStep=5,
             ScanTime=3;


/*storage for old interrupt handler address*/
void interrupt ( *AsyncVector)(void);

/* receiver buffer */
char  Buffer[BufLen];
/*offsets of pointers to circular buffer*/
unsigned int  CircOut, CircIn, CharsInBuf;
/* UART adapter registers */
unsigned int  Base,DataPort, LCR, MCR, IER, LSR, PortNr;
unsigned char  InterNr, IntMask;

/* storage for device name */
char  DeviceName[5];

/***********************************/
/*Sends character in to serial port*/
void DirectOut (char X)
{
   while ( (0x20&inportb(LSR)) != 0x20);
   outportb(DataPort,X);
};

void ClearBuffer(void)
{
   CircIn     = 0;
   CircOut    = 0;
   CharsInBuf = 0;
};

/* in you use memory models with near code pointers declare
this routine with 'far' modifier .*/ 
void interrupt AsyncInt()
{
   disable();
   if (CharsInBuf < BufLen)
   {
      CharsInBuf++;
      /*puts char in to Buffer*/
      Buffer[CircIn] = inportb(DataPort);
      if (CircIn < (BufLen-1))
         CircIn++;
      else
         CircIn= 0;
   }
   else  inportb(DataPort);
   outportb(MCR,0xB);
   outportb(INTFLAG,0x20);
   enable();
};

/*****************************************/
/*Sets int vector, baud rate, port number*/
void EnablePorts(unsigned int PortNr,unsigned int Baud)
{
unsigned char B;
unsigned int  Dv; /* baud rate divider */

   switch(PortNr)
   {
      case 1: Base=0x3F0;break;
      case 2: Base=0x2F0;break;
      case 3: Base=0x3E0;break;
      case 4: Base=0x2E0;
   };
   DataPort  =Base+0x8;
   IER       =Base+0x9;
   LCR       =Base+0xB;
   MCR       =Base+0xC;
   LSR       =Base+0xD;
   if ((PortNr==1)||(PortNr==3))
   {
      IntMask   = 0xEF;
      InterNr   = 0x0C;
   }
   else
   {
      IntMask   = 0xF7;
      InterNr   = 0x0B;
   };
   ClearBuffer();
   AsyncVector=getvect(InterNr);
   setvect(InterNr,AsyncInt);
   /*Baud setting*/
   outportb(LCR,0x80);
   Dv=(unsigned int)(115200L/Baud);
   outportb(DataPort,LOBYTE(Dv));
   outportb(IER,HIBYTE(Dv));
   /*Data=8 bits, Stop=1, Parity=none*/
   outportb(LCR,0x3);

   B= inportb(INTCTLR);
   B= B & IntMask;
   outportb(INTCTLR,B);

   outportb(MCR,0xB);
   outportb(IER,0x1);
   outportb(INTFLAG,0x20);
};

/************************************/
/*Gets one Char from circular Buffer*/
unsigned char GetCharInBuf(void)
{
unsigned char X;

   if (CharsInBuf > 0)
   {
      X=Buffer[CircOut];
      if (CircOut < BufLen-1)CircOut++;
         else CircOut= 0;
      CharsInBuf--;
   };
   return X;
};

/************************************/
/*restores com port to initial state*/
void DisablePorts(void)
{
unsigned char  B;
   B= inportb(INTCTLR);
   B= B | (~IntMask); /*disable interrupt from serial port*/
   outportb(INTCTLR,B);
   outportb(IER,0);
   outportb(INTFLAG,0x20);
   setvect(InterNr,AsyncVector);
};

/*------------------------Mini-terminal------------------------------------*/
void Terminal(void)
{
char C;

   clrscr();
   puts("Press ESC to Quit MiniTerminal");
   do
   {
      if (kbhit())
      {
         C=getch();
         DirectOut(C);
      };
      if (CharsInBuf>0)
         putch(GetCharInBuf());
   }while(C!=27); /*ESC code*/
};
/*-------------------------------------------------------------------------*/

/*---------------------Change scan parameters------------------------------*/
void WriteScanParameters(void)
{
   printf("Scan From     = %d nm\n",ScanFrom);
   printf("Scan To       = %d nm\n",ScanTo);
   printf("Scan Step     = %d nm\n",ScanStep);
   printf("Scan Interval = %d s\n",ScanTime);
};

void EnterScanParameters(void)
{
   printf("\nScan From (valid %d..%d) nm - ",MinV,MaxD);
   scanf("%d",&ScanFrom);
   printf("Scan To (valid %d..%d) nm -",MinV,MaxD);
   scanf("%d",&ScanTo);
   fputs("Scan Step (valid >0) nm - ",stdout);
   scanf("%d",&ScanStep);
   fputs("Scan Interval (valid >0) sec -",stdout);
   scanf("%d",&ScanTime);
};

int CheckBounds(int Figure)
{
   return ((Figure>MaxD) || (Figure<MinV)) ? 0 : 1;
};

/*-------------------------------------------------------------------------*/

/*--------------------------- Scaning -------------------------------------*/

void Timer(unsigned int DelayInSeconds) /*Doesn't return for DelayInSeconds seconds*/
{
unsigned long Time, NewTime;
struct time tt;

   gettime(&tt);
   Time=tt.ti_hour*3600UL+60*tt.ti_min+tt.ti_sec;
   NewTime=Time+DelayInSeconds;
   do
   {
      gettime(&tt);
      Time=tt.ti_hour*3600UL+60*tt.ti_min+tt.ti_sec;
   }while((Time<NewTime) && (!kbhit()));
};

/*Sends command line in form [<device name>:W1/Snnn\<sender>]*/
void SendWavelength(float W)
{
int I;
char S[20];

   sprintf(S,"[%s:W1/S%-.1f\\PC] ",DeviceName,W);
   for (I=0;I<strlen(S);I++)DirectOut(S[I]);
};

/*waits for sequence 'PC:' from serial port*/
void WaitForLineTerminator(void)
{
char DN[4];

   strcpy(DN,"   ");
   do
   {
      if (CharsInBuf>0)
      {
         DN[0]=DN[1];
         DN[1]=DN[2];
         DN[2]=GetCharInBuf();
      }
   }while (strcmp(DN,"PC:") && (!kbhit()));
   if (kbhit())getch();
};

void Scaning(void)
{
int wavelength, Step/*,OldWaveLength*/;

   clrscr();
   puts("Scaning Mode. Press any Key to Quit");
   WriteScanParameters();
   puts("\nNow at      nm");

   if (ScanFrom<ScanTo) /*determine scaning direction*/
      Step=ScanStep;
   else
      Step=-ScanStep;
   wavelength=ScanFrom;
   do
   {
      gotoxy(8,7); printf("%5d",wavelength);
      SendWavelength(wavelength); /*Sends command Set Wavelength*/
      WaitForLineTerminator();   /*Waits util command is performed*/
      Timer(ScanTime);         /*Waits Scan Interval*/
      do
         wavelength=wavelength+Step;  /*Next wavelength*/
      while ((MaxV<wavelength)&&(wavelength<MinV));

      /*skip discontinuity interval if exists*/
      /*if ( ((OldWaveLength>=8500)&&(wavelength<8500)) ||
           ((OldWaveLength<8500)&&(wavelength>=8500)) )
      {
         gotoxy(5,10);puts("Please, change crystal and press any key ..");
         getch();
         gotoxy(5,10);puts("                                           ");
      };*/  /*for two crystals DFG stage only*/
   }while ( (((Step<0) && (wavelength>ScanTo)) ||
            ((Step>0) && (wavelength<ScanTo))) && (!kbhit()) );
   if (kbhit()) getch();
};

/*-------------------------------------------------------------------------*/

void MainMenu(void)
{
char N;

   do
   {
      clrscr();
      printf("Port Nr =%d",PortNr);
      printf("   Device: %s\n\n",DeviceName);
      WriteScanParameters();
      puts("---------------------");
      puts("Select:");
      puts("1)   Change Scan parameters");
      puts("2)   Begin Scanning");
      puts("3)   MiniTerminal");
      puts("ESC  Quit");
      N=getch();
      switch(N)
      {
         case '1':
            EnterScanParameters();
            if  ( !CheckBounds(ScanFrom) ||
                  !CheckBounds(ScanTo) ||
                  (ScanStep<1) || (ScanTime<1) )
            {
               puts("Invalid parameter. Press any Key");
               ScanFrom=MinV;
               ScanTo=MaxV;
               ScanStep=5;
               ScanTime=3;
               getch();
            };
            break;
         case '2':Scaning();break;
         case '3':Terminal();
      };
   }while  (N!=27);
};

int main(void)
{
char C;

   clrscr();
   strcpy(DeviceName,"");
   puts("Demo program for PG401 VIR/DFG/SHG\n");
   fputs("Enter serial port number. Valid 1..4 - ? ",stdout);
   scanf("%d",&PortNr);
   fputs("Enter device name. Only Enter for D1 - ?",stdout);
   if((C=getch())=='\r')
      strcpy(DeviceName,"D1");
   else
   {
      DeviceName[0]=C; putch(C);
      if((C=getch())!='\r'){DeviceName[1]=C;putch(C);}
   }
   EnablePorts(PortNr,38400U);
   MainMenu();
   DisablePorts();
   return 0;
}