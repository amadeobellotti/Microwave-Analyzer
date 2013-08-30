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
 * other characteristic. We would appreciate acknowledgement if the software
 * is used. The publication introducing this program is planned to be published
 * in 2011/12 with K. P. Pernstich as first author.
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// keep Annotations until Runtime
@Retention(RetentionPolicy.RUNTIME)

// only methods may bare this Annotation
@Target(ElementType.METHOD)

// that Annotation shows up in javadoc
@Documented

/**
 * Interface that defines Annotations that are used at Runtime
 * using Reflection to find all methods of Instrument classes
 * that should be shown in the User Interface.<p>
 *
 * For such methods, this interface defines annotations that can
 * be placed before methods to automatically create an entry in
 * the GUI with fields that create the command string.<p>
 *
 * Usage example:
 * @AutoGUIAnnotation(
        DescriptionForUser = "<html>Use html tags for <br>very long descriptions.</html>",
        ParameterNames = {"Parameter 1", "Parameter 2", "Parameter 3"},
        DefaultValues = {"3", "a default string", "-1.1"},
        ToolTips = {"Tooltip 1"})
 *
 * http://tutorials.jenkov.com/java-reflection/annotations.html
 * http://tutorials.jenkov.com/java-reflection/index.html
 * http://download.oracle.com/javase/tutorial/java/javaOO/annotations.html
 *
 * @author KPP (Kurt Pernstich: pernstich@alumni.ethz.ch)
 * @version 0.1
 */
public @interface AutoGUIAnnotation {

    
    /**
     * This should be a short description for the user of the functionality
     * implemented by the method. This Text will be displayed as ToolTip when
     * the mouse hovers long enough over an item in the <code>jCommandList</code>.<p>
     *
     * Use html-tags (see example above) to format the message nicely if you like.
     *
     * An empty String is allowed, and specifying the <code>DescriptionForUser</code>
     * can be omitted because a default <code>DescriptionForUser</code> is
     * specified in the declaration of <code>AutoGUIAnnotation</code>.
     * 
     */
    String      DescriptionForUser() default "";

    /**
     * An array of Strings that name all parameters of the method. These names are used
     * in the GUI to populate the table.
     *
     * If a method takes no parameters, specify an empty String[] (that is to
     * use ParameterNames = {}).
     *
     * Note that the number of Strings must equal the number of arguments the
     * method requires.
     *
     */
    String[]    ParameterNames();

    /**
     * An array of Strings that are used as initial values when populating
     * the table in the User Interface. Not every parameter has to have a
     * default value. The String must be formated in such a way that the
     * constructor of the corresponding type (for instance Integer(), Boolean())
     * can convert the String, otherwise an error message is shown. Specifying an
     * empty String is also allowed. Specifying the <code>DefaultValues</code> can
     * be omitted because default <code>DefaultValues</code> is specified in
     * the declaration of <code>AutoGUIAnnotation</code>.
     */
    String[]    DefaultValues() default {};

    /**
     * An array of Strings specifying the ToolTips shown when the mouse hovers
     * long enough over the parameter name in the Table. Not every parameter needs
     * a ToolTip, and empty ToolTips are also allowed. Specifying the
     * <code>ToolTips</code> can be omitted because default <code>ToolTips</code>
     * is specified in the declaration of <code>AutoGUIAnnotation</code>.
     */
    String[]    ToolTips() default {};
}