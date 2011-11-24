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

import java.io.File;
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
  protected URL m_remoteUrl;
  protected URL m_localUrl;
  public String m_sTitle;
  public String m_sDirName;
  public String m_sLocalDir;
  protected OutputWindow m_ow;
  protected Document m_doc;
  
  /** set the window to which output messages will be directed */
  abstract public void setOutputWindow(OutputWindow ow);
  
  public void setLink(String sLink) {
    m_sLink = sLink;
  }

  /** select <code>sTag</code> but require exactly one match */
  protected Element getOneTag(Element parent, String sTag, boolean bSilent) {
    Elements elems = parent.select(sTag);
    if (elems.size()==0) {
      if (!bSilent) {
        m_ow.addLine(PbnTools.getStr("error.tagNotFound", sTag));
        return null;
      }
    }
    if (elems.size()>1) {
      if (!bSilent) {
        m_ow.addLine(PbnTools.getStr("error.onlyOneTagAllowed", sTag));
        return null;
      }
    }
    return elems.get(0);
  }
  
  protected boolean checkGenerator(Document doc, String sExpValue, boolean bSilent) {
    Element elem = getOneTag(doc.head(), "meta[name=GENERATOR]", bSilent);
    if (elem==null) { return false; }
    String sFound = elem.attr("content");
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
  
  /** check whether only one given tag exists and matches <code>sTextReg</code> */ 
  protected boolean checkTagText(Element parent, String sTag, String sTextReg, boolean bSilent)
  {
    Element elem = getOneTag(parent, sTag, bSilent); 
    if (elem == null) { return false; }
    String sText = elem.text();
    sText = sText.replace('\u00a0', ' ');
    if (!sText.matches(sTextReg)) {
      m_ow.addLine(PbnTools.getStr("error.invalidTagValue", sTag, sTextReg, elem.text()));
      return false;
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
    String sPath = m_remoteUrl.getPath();
    String sLast = sPath.replaceFirst("^.*/", "");
    if (sLast.indexOf('.')>=0) {
      m_sDirName = sPath.replaceFirst("^.*/([^/]+)/[^/]*$", "$1");
    } else {
      m_sDirName = sLast;
    }
    m_ow.addLine(m_sDirName);
    
  }
  
  /** Checks whether the tournament is already downloaded. Sets the member
    * <code>m_sLocalDir</code> */
  protected boolean isDownloaded()
  {
    File fWork = new File(PbnTools.getWorkDir());
    File fDir = new File(fWork, m_sDirName);
    m_sLocalDir = fDir.getAbsolutePath();
    try { m_sLocalDir = fDir.getCanonicalPath(); }
    catch (Exception e) {}
    return fDir.exists();
  }
  
  /** Changes the link if user did not provide direct link to expected page.
    * As a default does nothing.
    * @return <code>true</code> if redirection occured 
    */
  public boolean redirect() throws VerifyFailedException
  {
    return false;
  }

  /** Verifies link without doing redirection  
    * @see verify(boolean) */  
  abstract protected boolean verifyDirect(boolean bSilent) throws VerifyFailedException;

  /** Verify whether link points to a valid data in this format */
  public boolean verify(boolean bSilent) throws VerifyFailedException
  {
    if (!verifyDirect(bSilent)) { return false; }
    if (redirect())
      { return verifyDirect(bSilent); }
    else
      { return true; }
  }
  
  abstract protected void wget();
  
  /** performs 2 operations: downloading (if required) from internet and
    * converting (locally) to pbns */
  public boolean fullDownload() throws DownloadFailedException
  {
    if (m_remoteUrl.getProtocol().equals("file")) {
      m_ow.addLine(PbnTools.getStr("tourDown.msg.localLink", m_sLocalDir));
      m_localUrl = m_remoteUrl;
    } else {
      boolean bDownloaded = isDownloaded();

      // constructing local url after isDownloaded set m_sLocalDir
      String sFileName = m_remoteUrl.toString().replaceFirst("^.*/", "");
      if (sFileName.indexOf('.')<0) { sFileName = "index.html"; }
      try {
        m_localUrl = new File(new File(m_sLocalDir, "html"), sFileName).toURI().toURL();
      } catch (Exception e) { throw new DownloadFailedException(e); }
      m_ow.addLine("local url: " + m_localUrl);
      
      if (bDownloaded) {
        m_ow.addLine(PbnTools.getStr("tourDown.msg.willWget", m_sLocalDir));
        wget();
      } else {
        m_ow.addLine(PbnTools.getStr("tourDown.msg.alreadyWgetted", m_sLocalDir));
      }

    }
    return true;
  }
  
  public class VerifyFailedException extends JCException //{{{
  {
    VerifyFailedException(String sMessage) { super(sMessage); }
    
    VerifyFailedException(Throwable t)
    {
      super(t);
      m_ow.addLine(t.getMessage());
    }
  } //}}}

  public class DownloadFailedException extends JCException //{{{
  {
    DownloadFailedException(Throwable t)
    {
      super(t);
      m_ow.addLine(t.getMessage());
    }
  } //}}}

  
}
