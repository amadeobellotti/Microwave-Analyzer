
"""Each time the Python Interpreter is (re-)started, this script is executed.

The Python search path includes the Project Directory specified in the GUI, the
path to the iC directory, and the iC/Python_scripts directory. Python files 
residing in those directories can be imported (and hence executed) using the 
import command.

Instrument Control (iC) exports a Device object called '_device' to Python (it
resides in namespace __main__). It is recommended to use the functions defined 
in the module iC.py to access the Device class' functionality; all methods in
the iC.py module are imported by Startup.py and, hence, accessible from __main__
(the command line if you will). If other packages need to access methods defined
in iC.py, they simply need to import the iC module. Direct access to any method 
of _device should be encapsulated in a Python function, preferably in iC.py.
"""

# --- some imports
import sys


    
# add path to Project Directory
sys.path = [project_directory, iC_directory, iC_directory+'Python_Scripts/'] + sys.path


# import iC's Python Utility methods (must be after setting the path)
from iC import *


print '== executing Startup.py =='
#print 'In Startup.main these names are known: ' + str(dir()) + '\n'
