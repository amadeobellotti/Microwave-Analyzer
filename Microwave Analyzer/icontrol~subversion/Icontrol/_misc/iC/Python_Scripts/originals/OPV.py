"""Extract the characteristic parameters for Organic Photovoltaic Cells.

More explanations ...
"""

import Startup

def evaluate(vi):
    """Extract characteristic parameters from the measured OPV characteristics.
	
		The passed parameter must be a double[][] where the voltage
        values are in vi[:][0] and the current values are in vi[:][1].
    """


    # do nothing in Syntax-Check mode
    if Startup.is_syntax_check_mode() == true:
        return;

    print vi[1][1]



# --- "main"

#if __name__ == '__main__':

print("In OPV.main these names are known")
print(dir())