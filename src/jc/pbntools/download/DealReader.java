/* *****************************************************************************

    jedit options: :folding=explicit:tabSize=2:noTabs=true:
    
    Copyright (C) 2011 Jaroslaw Czekalski - jarekczek@poczta.onet.pl

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

import jc.outputwindow.SimplePrinter;
import jc.pbntools.Deal;

public interface DealReader
{
  /** First <code>verify</code> must be called as it caches some data. */
  public Deal[] readDeals(String sUrl, boolean bSilent)
    throws DownloadFailedException;

  /** Verifies if the <code>sUrl</code> contains valid data in this format */
  public boolean verify(String sUrl, boolean bSilent);
    
  /** Sets the window to which output messages will be directed */
  abstract public void setOutputWindow(SimplePrinter sp);

  abstract public SimplePrinter getOutputWindow();
}
