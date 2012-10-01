/* *****************************************************************************

    jedit options: :folding=explicit:tabSize=2:noTabs=true:

    Copyright (C) 2011-2 Jaroslaw Czekalski - jarekczek@poczta.onet.pl

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
   *****************************************************************************
*/

package jc.pbntools.download;

import java.io.PrintWriter;

import jc.JCException;
import jc.outputwindow.OutputWindow;
import jc.outputwindow.OutputWindowWriter;

/**
 * An exception thrown when downloading tournaments, for example by class
 * {@link HtmlTourDownloader}
 */
public class VerifyFailedException extends JCException
{
  public VerifyFailedException(String sMessage, OutputWindow ow, boolean bPrint) {
    super(sMessage);
    if (bPrint) { ow.addLine(sMessage); }
  }
  
  public VerifyFailedException(String sMessage) { super(sMessage); }
  
  public VerifyFailedException(Throwable t, OutputWindow ow)
  {
    super(t);
    ow.addLine(t.getMessage());
    // if (!System.getProperty("jc.debug", "0").equals("0")) {
      // t.printStackTrace(new PrintWriter(new OutputWindowWriter(ow)));
    // }
  }
}
