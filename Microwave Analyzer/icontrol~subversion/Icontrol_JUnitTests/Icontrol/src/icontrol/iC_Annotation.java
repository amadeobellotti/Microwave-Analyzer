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

package icontrol;

/**
 * General Annotation to methods (and classes). See the elements for a further
 * description.
 *
 * @author KPP (Kurt Pernstich: pernstich@alumni.ethz.ch)
 * @version 0.1
 */

import icontrol.drivers.Device;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/** declare to keep Annotations until Runtime */
@Retention(RetentionPolicy.RUNTIME)

/** declare that methods and classes may bare this Annotation */
@Target({ElementType.METHOD, ElementType.TYPE})

/** declare so that Annotations show up in javadoc */
@Documented

/**
 * This Annotation defines fields that can be used to specify if a method
 * performs a Syntax-Check and to specify the communication ports/protocolls
 * the (Instrument) class supports. See the documentation of individual fields
 * for more details.
 */
public @interface iC_Annotation {

    /**
     * When <code>true</code>, this signals that the method baring this
     * annotation does check the syntax, hence, the dispatcher should call this
     * method when performing the syntax check. <p>
     *
     * When the syntax check fails, the method should throw a <code>DataFormatException</code>
     * which will then be shown to the user by re-throwing as <code>ScriptException</code>
     * in <code>Device.DispatchCommand</code> which is caught in <code>Dispatcher.run</code>.
     * @return <code>true</code> if the method performs a Syntax-Check
     */
    public boolean MethodChecksSyntax() default false;

    /**
     * This is a promise of the class that all public methods support the specified
     * communication ports/protocols.
     * @return The supported communication protocols
     */
    public Device.CommPorts[] CommPorts() default {};

    /**
     * Defines the name for this class which is used in the Framework-Command MAKE
     * for Instruments of this class. This name also shows up in the GUI.<p>
     * This name is used in {@link Dispatcher#RegisterDeviceClasses() }
     * @return The name of this class used in the Framework-Command MAKE
     */
    public String InstrumentClassName() default "";
    
}
