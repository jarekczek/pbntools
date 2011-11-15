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
import jc.JCException;
import jc.OutputWindow;

abstract public class HtmlTourDownloader
{
  protected String m_sLink;
  public String m_sTitle;
  public String m_sDirName;
  protected OutputWindow m_ow;
  
  /** set the window to which output messages will be directed */
  abstract public void setOutputWindow(OutputWindow ow);
  
  public void setLink(String sLink) {
    m_sLink = sLink;
  }
  
  /** verify whether link points to a valid data in this format */ //{{{
  abstract public boolean verify() throws VerifyFailedException;
  
  public class VerifyFailedException extends JCException
  {
    VerifyFailedException(Throwable t)
    {
      super(t);
      m_ow.addLine(t.getMessage());
    }
  }

}
