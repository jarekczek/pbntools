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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.JDialog;

import jc.f;
import jc.JCException;
import jc.outputwindow.OutputWindow;
import jc.SoupProxy;
import jc.pbntools.Deal;
import jc.pbntools.PbnFile;
import jc.pbntools.PbnTools;
import jc.pbntools.RunProcess;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ParyTourDownloader extends HtmlTourDownloader
{

  public void setOutputWindow(OutputWindow ow)
  {
    m_ow = ow;
  }

  /** Redirect to url without W- */
  protected boolean redirect() throws VerifyFailedException
  {
    if (m_sLink.matches("^.*/W-[^/]*$")) {
      m_sLink = m_sLink.replaceFirst("/W-([^/]*)$", "/$1");
      m_ow.addLine(PbnTools.getStr("tourDown.msg.redir", m_sLink));  
      return true;
    }
    else
      return false;
  }

  /** Gets link for a deal with a given number */
  protected String getLinkForDeal(int iDeal) {
    return m_sLink.replaceFirst("/[^/]+$", "/" + m_sDirName.toLowerCase() 
      + String.format("%03d.html", iDeal));
  }
  
  /** @param doc Document after redirection, containing 2 frames.
    *  */
  protected void getNumberOfDeals(Document doc, boolean bSilent) throws VerifyFailedException {
    String sFrameTag = "frameset > frame[name=lewa]";
    String sExpectedSrc = m_sDirName.toLowerCase() + "001.html";
    Element frame = getOneTag(doc, sFrameTag, false);
    if (frame == null) {
      throw new VerifyFailedException(PbnTools.getStr("error.getNumberOfDeals"), !bSilent);
    }
    String sFoundSrc = frame.attr("src");
    if (!sFoundSrc.equals(sExpectedSrc)) {
      throw new VerifyFailedException(PbnTools.getStr("error.invalidTagValue",
                  sFrameTag, sExpectedSrc, sFoundSrc), true);
    }
    
    // download page with the first deal
    String sLink1 = getLinkForDeal(1);
    m_ow.addLine(sLink1);
    Document doc1 = null;
    try {
      SoupProxy proxy = new SoupProxy();
      doc1 = proxy.getDocument(sLink1);
    }
    catch (JCException e) { throw new VerifyFailedException(e); }
    
    // look for a link to the last one
    if (doc1.body() == null) {
      throw new VerifyFailedException(PbnTools.getStr("error.noBody"), !bSilent);
    }
    Element elemLast = getOneTag(doc1.body(), "a[title=ostatnie]", false);
    if (elemLast == null) {
      throw new VerifyFailedException(PbnTools.getStr("error.getNumberOfDeals"), !bSilent);
    }
    
    // parse the link to get the deal number
    String sLast = elemLast.attr("href");
    String sNoLast = sLast.replaceFirst("^" + m_sDirName.toLowerCase() + "([0-9]{3})\\.html", "$1");
    m_cDeals = 0;
    try {
      m_cDeals = Integer.parseInt(sNoLast);
    } catch (java.lang.NumberFormatException e) {} 
    if (m_cDeals == 0) {
      throw new VerifyFailedException(PbnTools.getStr("tourDown.error.parseNumber", sLast), !bSilent);
    }
  }

  /** Verifies whether link points to a valid data in this format.
    * Sets m_sTitle and m_sDirName members. Leaves m_doc filled.
    */ //{{{
  protected boolean verifyDirect(boolean bSilent) throws VerifyFailedException
  {
    Document doc;
    try {
      SoupProxy proxy = new SoupProxy();
      doc = proxy.getDocument(m_sLink);
      m_doc = doc;
      m_remoteUrl = proxy.getUrl();
    }
    catch (JCException e) {
      throw new VerifyFailedException(e);
    }
    m_ow.addLine(PbnTools.m_res.getString("msg.documentLoaded"));

    if (!checkGenerator(doc, "JFR 2005", bSilent)) { throw new VerifyFailedException("generator"); }
    if (doc.body() != null) {
      // only W- link has body
      // direct link has frames which should be read instead
      if (!checkTagText(doc.body(), "p.f", "^\\sPary\\..*$", bSilent)) {
        throw new VerifyFailedException("p.f");
      }
    }

    return true;
  } //}}}
  
  public boolean verify(boolean bSilent) throws VerifyFailedException
  {
    boolean bRedirected = true;
    while (bRedirected) {
      if (!verifyDirect(bSilent)) { return false; }
      bRedirected = redirect();
    }
    getTitleAndDir();
    getNumberOfDeals(m_doc, bSilent);
    if (!bSilent) { m_ow.addLine(PbnTools.getStr("msg.tourFound", m_sTitle, m_cDeals)); }

    return true;
  }

  protected String createIndexFile() throws DownloadFailedException
  {
    int iDeal;
    String sLinksFile = new File(m_sLocalDir, "links.txt").getAbsolutePath();
    m_ow.addLine(PbnTools.getStr("tourDown.msg.creatingIndex", sLinksFile));
    try {
      if (!(new File(m_sLocalDir).mkdir())) {
        throw new DownloadFailedException(PbnTools.getStr("error.unableToCreateDir", m_sLocalDir), true);
      }
      BufferedWriter fw = new BufferedWriter(new FileWriter(sLinksFile));
      for (iDeal=1; iDeal<=m_cDeals; iDeal++) {
        String sDealLink = getLinkForDeal(iDeal); 
        fw.write(sDealLink);
        fw.newLine();
        String sDealLinkTxt = sDealLink.replace(".html", ".txt");
        if (!sDealLinkTxt.endsWith(".txt")) {
          throw new DownloadFailedException(
            PbnTools.getStr("tourDown.error.convertExt", sDealLink, "html", "txt"),
            true);
        }
        // fw.write(sDealLinkTxt);
        // fw.newLine();
      }
      fw.close();
    }
    catch (java.io.IOException ioe) { throw new DownloadFailedException(ioe); }
    return sLinksFile;
  }
  
  protected void wget() throws DownloadFailedException
  {
    String sLinksFile = createIndexFile();
      
    String sCmdLine = "wget -p -k -nH -nd -nc -w 0 --random-wait -E -e robots=off";
    ArrayList<String> asCmdLine = new ArrayList<String>(Arrays.asList(sCmdLine.split(" ")));
    asCmdLine.add("--directory-prefix=" + m_sLocalDir);
    asCmdLine.add("--input-file=" + sLinksFile);
    
    OutputWindow.Process p = m_ow.createProcess();
    try {
      p.exec(asCmdLine.toArray(new String[0]));
    } catch (JCException e) {
      throw new DownloadFailedException(e);
    }
    
    for (int iDeal=1; iDeal<=m_cDeals; iDeal++) {
      ajaxFile(getLinkForDeal(iDeal), true);
    }
  }

  protected Deal[] readDealsFromDir(String sDir)
    throws DownloadFailedException
  {
    m_ow.addLine("reading htmls");
    return null;
  }

  protected void saveDealsAsPbn(Deal[] aDeal, String sDir)
    throws DownloadFailedException
  {
  }

}
