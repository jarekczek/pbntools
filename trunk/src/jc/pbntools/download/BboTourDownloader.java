/* *****************************************************************************

    jedit options: :folding=explicit:tabSize=2:indentSize=2:noTabs=true:

    Copyright (C) 2012 Jaroslaw Czekalski - jarekczek@poczta.onet.pl

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
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.StringTokenizer;
import javax.swing.JDialog;

import jc.f;
import jc.JCException;
import jc.outputwindow.OutputWindow;
import jc.outputwindow.SimplePrinter;
import jc.SoupProxy;
import jc.pbntools.Card;
import jc.pbntools.Deal;
import jc.pbntools.PbnFile;
import jc.pbntools.PbnTools;
import jc.pbntools.RunProcess;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BboTourDownloader extends HtmlTourDownloader
{
  /** Overall turney results document */
  protected Document m_docRes; 

  public String getName() { return "Bbo"; }
  
  public void setOutputWindow(SimplePrinter ow)
  {
    m_ow = ow;
  }

  /** Gets remote link for the deal with the given number */
  protected String getLinkForDeal(int iDeal) {
    return null;
  }
  
  /** Gets local link for the deal with the given number */
  protected String getLocalLinkForDeal(int iDeal) {
    return getLocalFile(getLinkForDeal(iDeal));
  }
  
  /** @param doc Document after redirection, containing 2 frames.
    *  */
  protected void getNumberOfDeals(Document doc, boolean bSilent)
    throws DownloadFailedException {
    m_cDeals = 0;
  }

  /**
   * Tries to fetch overall results of the tourney, where the descriptive
   * title is given. If any of the steps fails it just returns, without
   * complaining about problems.
   */
  protected void getBetterTitle()
  {
    try {
      String sLinkRes = "http://webutil.bridgebase.com/v2/tview.php?t="
        + m_sTitle;
      SoupProxy proxy = new SoupProxy();
      m_docRes = proxy.getDocument(sLinkRes);
      Element title = getFirstTag(m_docRes, ".bbo_tlv", !f.isDebugMode());
      m_sTitle = title.text();
    }
    catch (DownloadFailedException dfe) {
    }
    catch (JCException e) {
      if (f.isDebugMode()) m_ow.addLine(e.toString());
    }
  }

  /** Verifies whether link points to a valid data in this format.
    * Sets m_sTitle and m_sDirName members. Leaves m_doc filled. */
  public boolean verify(String sLink, boolean bSilent)
  {
    setLink(sLink);
    Document doc;
    try {
      SoupProxy proxy = new SoupProxy();
      doc = proxy.getDocument(m_sLink);
      m_doc = doc;
      m_remoteUrl = proxy.getUrl();
    }
    catch (JCException e) {
      m_ow.addLine(e.toString());
      return false;
    }
    if (!bSilent)
      println(PbnTools.m_res.getString("msg.documentLoaded"));
    try {
      firstTagStartsWith(doc, "th", "Tourney ", bSilent);
      firstTagMatches(doc, "td.board", "Board [0-9]+ traveller", bSilent);
      // as a fallback construct the title from the link - tourney=xxx
      m_sTitle = f.getFileNameNoExt(m_sLink);
      m_sTitle = m_sTitle.replaceFirst(".*[\\?&]tourney=", "");
      m_sTitle = m_sTitle.replaceFirst("[\\?&].*", "");
      m_sTitle = m_sTitle.replaceFirst("-$", "");
      getBetterTitle();
      if (f.isDebugMode()) m_ow.addLine(m_sTitle);
    }
    catch (DownloadFailedException dfe) {
      return false;
    }

    return false;
  }

  protected String createIndexFile() throws DownloadFailedException
  {
    int iDeal;
    String sLinksFile = new File(m_sLocalDir, "links.txt").getAbsolutePath();
    println(PbnTools.getStr("tourDown.msg.creatingIndex", sLinksFile));
    try {
      if (!(new File(m_sLocalDir).mkdir())) {
        throw new DownloadFailedException(
          PbnTools.getStr(
            "error.unableToCreateDir", m_sLocalDir), m_ow, true);
      }
      BufferedWriter fw = new BufferedWriter(new OutputStreamWriter(
        new FileOutputStream(sLinksFile), "ISO-8859-1"));
      for (iDeal=1; iDeal<=m_cDeals; iDeal++) {
        String sDealLink = getLinkForDeal(iDeal); 
        fw.write(sDealLink);
        fw.newLine();
        String sDealLinkTxt = sDealLink.replace(".html", ".txt");
        if (!sDealLinkTxt.endsWith(".txt")) {
          throw new DownloadFailedException(
            PbnTools.getStr("tourDown.error.convertExt", sDealLink, "html", "txt"),
            m_ow, true);
        }
        fw.write(m_sLink);
        fw.newLine();
      }
      fw.close();
    }
    catch (java.io.IOException ioe) {
      throw new DownloadFailedException(ioe, m_ow, !m_bSilent);
    }
    return sLinksFile;
  }
  
  protected void wget() throws DownloadFailedException
  {
    String sLinksFile = createIndexFile();
      
    String sCmdLine = "wget -p -k -nH -nd -nc --random-wait -E -e robots=off";
    if (m_remoteUrl.toString().indexOf("localhost") < 0)
      sCmdLine += " -w 1";
    ArrayList<String> asCmdLine = new ArrayList<String>(Arrays.asList(sCmdLine.split(" ")));
    asCmdLine.add("--directory-prefix=" + m_sLocalDir);
    asCmdLine.add("--input-file=" + sLinksFile);
    
    if (PbnTools.bWindows) {
      // on Windows we need to point our wget.exe
      String sWget = PbnTools.getWgetPath();
      asCmdLine.set(0, sWget);
    }
    
    OutputWindow.Process p = new OutputWindow.Process(m_ow);
    try {
      p.exec(asCmdLine.toArray(new String[0]));
    } catch (JCException e) {
      throw new DownloadFailedException(e, m_ow, !m_bSilent);
    }
  }

  protected Deal[] readDealsFromDir(String sDir)
    throws DownloadFailedException
  {
    ArrayList<Deal> deals = new ArrayList<Deal>();
    for (int iDeal=1; iDeal<=m_cDeals; iDeal++) {
      if (Thread.interrupted()) {
        println(PbnTools.getStr("msg.interrupted"));
        break;
      }
      Deal ad[] = readDeals(getLocalLinkForDeal(iDeal), false);
      if (ad != null) {
        for (Deal d: ad) {
          d.setIdentField("Event", m_sTitle);
          deals.add(d);
        }
        if (PbnTools.getVerbos() > 0) {
          println(PbnTools.getStr("tourDown.msg.readOne",
            iDeal, ad.length));
        }
      }
    }
    return deals.toArray(new Deal[0]);
  }

  public Deal[] readDeals(String sUrl, boolean bSilent)
    throws DownloadFailedException
  {
    Document doc;
    m_sCurFile = sUrl;
    m_bSilent = bSilent;
    
    resetErrors();
    Deal deal = new Deal();
    try {
      SoupProxy proxy = new SoupProxy();
      doc = proxy.getDocument(sUrl);
    }
    catch (JCException e) {
      throw new DownloadFailedException(e, m_ow, !m_bSilent);
    }

    return processResults(deal, doc);
  }
  
  /** Extracts hands from the given element and saves them to
    * <code>deal</code>.
    * @param dealElem tbody with deal definition without results */
  protected void extractHands(Deal deal, Element dealElem)
    throws DownloadFailedException
  {
    //throw new DownloadFailedException("dosc", true);
  }

  /** readNumber method {{{
   * Reads deal number from <code>doc</code> and
   * sets it in <code>deal</code>.
   */
  private void readNumber(Deal deal, Document doc)
    throws DownloadFailedException
  {
  } //}}}

  /** readScoring method {{{
   * Reads scoring type of the deal from <code>doc</code> and
   * sets it in <code>deal</code>.
   */
  private void readScoring(Deal deal, Document doc)
  {
  } //}}}

  /** Multiplies given <code>deal</code> by the number of results. */
  private Deal[] processResults(Deal deal0, Document doc)
    throws DownloadFailedException
  {
    ArrayList<Deal> ad = new ArrayList<Deal>();
    
    return ad.toArray(new Deal[0]);
  }
}
