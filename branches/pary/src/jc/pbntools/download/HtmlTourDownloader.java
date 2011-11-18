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

import java.net.URL;

import jc.JCException;
import jc.OutputWindow;
import jc.pbntools.PbnTools;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

abstract public class HtmlTourDownloader
{
  protected String m_sLink;
  protected URL m_url;
  public String m_sTitle;
  public String m_sDirName;
  protected OutputWindow m_ow;
  protected Document m_doc;
  
  /** set the window to which output messages will be directed */
  abstract public void setOutputWindow(OutputWindow ow);
  
  public void setLink(String sLink) {
    m_sLink = sLink;
  }
  
  boolean checkGenerator(Document doc, String sExpValue, boolean bSilent) {
    Elements elems = doc.head().select("meta[name=GENERATOR]");
    if (elems.size()==0) {
      if (!bSilent) {
        m_ow.addLine(PbnTools.getStr("error.tagNotFound", "<meta name=\"GENERATOR\""));
        return false;
      }
    }
    if (elems.size()>1) {
      if (!bSilent) {
        m_ow.addLine(PbnTools.getStr("error.onlyOneTagAllowed", "<meta name=\"GENERATOR\""));
        return false;
      }
    }
    String sFound = elems.get(0).attr("content");
    if (sFound.isEmpty()) {
      if (!bSilent) {
        m_ow.addLine(PbnTools.getStr("error.tagNotFound", "<meta name=\"GENERATOR\" content="));
        return false;
      }
    }
    if (!sFound.equals(sExpValue)) {
      if (!bSilent) {
        m_ow.addLine(PbnTools.getStr("error.invalidTagValue", "<meta name=\"GENERATOR\" content=",
                                     sExpValue, sFound));
        return false;
      }
    }
    return true;
  }
  
  /** Gather title and dirname in a standard way. To be called from subclass. */
  protected void getTitleAndDir()
  {
    m_sTitle=""; m_sDirName="";
    Elements elems = m_doc.head().select("title");
    if (elems.size()>0) { m_sTitle = elems.get(0).text(); }
    m_ow.addLine(m_sTitle);
    String sPath = m_url.getPath();
    String sLast = sPath.replaceFirst("^.*/", "");
    if (sLast.indexOf('.')>=0) {
      m_sDirName = sPath.replaceFirst("^.*/([^/]+)/[^/]*$", "$1");
    } else {
      m_sDirName = sLast;
    }
    m_ow.addLine(m_sDirName);
    
  }
  
  /** verify whether link points to a valid data in this format */
  abstract public boolean verify() throws VerifyFailedException;
  
  public class VerifyFailedException extends JCException //{{{
  {
    VerifyFailedException(Throwable t)
    {
      super(t);
      m_ow.addLine(t.getMessage());
    }
  } //}}}

}
