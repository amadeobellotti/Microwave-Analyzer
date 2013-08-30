
"""Defines utility methods to encapsulate interaction with the iC-Framework.

Instrument Control (iC) exports a Device object called '_device' to Python (it
resides in namespace __main__). It is recommended to use the functions defined 
in this module (iC.py) to access the Device class' functionality; all methods in
the iC.py module are imported by Startup.py and, hence, accessible from __main__
(the command line if you will). If other packages need to access methods defined
in iC.py, they simply need to import the iC module. Direct access to any method 
of _device should be encapsulated in a Python function, preferably in iC.py.

"""

# for some reason __main__ this import statement does NOT help the methods
# defined below, so an extra import is required in each method, or, at least,
# in last_return_value
import __main__



# --- function definitions

def dispatch_command(InstrumentName, CommandName, *Parameters):
    """Send a command to the specified Instrument.

    This method is the Python-equivalent to Device.DispatchCommand(String). The
    returned value is originally (in Java) of type Object.

    """

    # for some reason __main__ is NOT imported by the first import statement above
    import __main__

    # build the first part of the Command String
    cmd = InstrumentName + ' ' + CommandName + ' '

    # append additional parameters
    for i in range( len(Parameters) ):
        cmd += str( Parameters[i] )
        if i+1 < len(Parameters):
            cmd += '; '

    # dispatch the iC command and return the result
    return __main__._device.DispatchCommand(cmd)





def is_syntax_check_mode():
    """Returns true only if the script runs in Syntax-Check Mode."""
 
    # for some reason __main__ is NOT imported by the first import statement above
    import __main__

    return __main__._device.inSyntaxCheckMode()




def is_stop_scripting():
    """Returns true if the user has pressed the Stop button.

    Tasks that may take a while should check this flag regularly to ensure that
    processing of the script can be interrupted by the user.

    """

    # for some reason __main__ is NOT imported by the first import statement above
    import __main__

    return __main__._device.getStopScripting()




def is_paused(wait_until_not_paused):
    """Returns true if processing the script has been paused.

    This is the Python-equivalent of IcontrolView.isPaused(), and it returns
    true if Scripting has been paused (either by the user pressing 'Pause' 
    button or by enabling Options/AutoPause) and false if the Script is being 
    processed. The method returns immediately if Scripting has been stopped 
    (because the user pressed the 'Stop' button.
    
    When wait_until_not_paused is set to true, the method waits until the 
    Script is being processed (Scripting has been resumed if it was paused), 
    and in this case, always returns false.

    Tasks that may take a while should check this flag regularly to ensure that
    processing of the script can be interrupted by the user.

    """

    # for some reason __main__ is NOT imported by the first import statement above
    import __main__

    return __main__._device.isPaused(wait_until_not_paused)



def is_no_communication_mode():
    """Returns true if the Instrument is set to No-Communication Mode.

    In No-Communication mode, no commands are sent to Instruments. If queries are
    made, the result is undefined, and might even be null.

    """

    # for some reason __main__ is NOT imported by the first import statement above
    import __main__

    return __main__._device.inNoCommunicationMode()



def last_return_value():
    """Returns the return value of the last Script-Command.
       
       The return value of the last (non-Python) command is exported to the 
       Python environment as '_ans' and made available with this convenience
       method. See iC/Example_Scripts/Python Demo for an example.
    """
    
    # for some reason __main__ is NOT imported by the first import statement above
    import __main__

    #print 'iC.last_return_values knows: ' + str(dir()) + '\n'

    return __main__._ans


def get_file_name(extension):
   """Returns the File Name including Path and Extension"""

   # for some reason __main__ is NOT imported by the first import statement above
   import __main__

   return __main__._device.getFileName(extension)



# --- "main"
#print 'iC.py knows these names: ' + str(dir()) + '\n'

