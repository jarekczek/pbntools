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

import jc.f;
import jc.JCException;
import jc.outputwindow.SimplePrinter;

/**
 * An exception thrown when downloading tournaments, for example by class
 * {@link HtmlTourDownloader}
 */
public class VerifyFailedException extends JCException
{
  public VerifyFailedException(String sMessage, SimplePrinter ow, boolean bPrint) {
    super(sMessage);
    if (ow != null && (bPrint || f.isDebugMode()))
      ow.addLine(sMessage);
  }
  
  public VerifyFailedException(String sMessage) { super(sMessage); }
  public VerifyFailedException(Throwable t) { super(t.getMessage()); }
  
  public VerifyFailedException(Throwable t, SimplePrinter ow, boolean bPrint)
  {
    super(t);
    if (ow != null && (bPrint || f.isDebugMode()))
      ow.addLine(t.getMessage());
  }
}
