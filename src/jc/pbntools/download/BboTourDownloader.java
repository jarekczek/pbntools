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
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
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
  /** Overall turney results link */
  protected String m_sResLink;
  /** Whether to download all the lins or only the first one from each
      board */
  protected boolean m_bAllLins;

  public String getName() { return "Bbo"; }
  
  public void setOutputWindow(SimplePrinter ow)
  {
    m_ow = ow;
  }

  /** Gets remote link for the deal with the given number */
  protected String getLinkForDeal(int iDeal)
    throws DownloadFailedException 
  {
    Element a = getNthTag(m_doc, ".board > a", iDeal, false);
    String sRelativeLink = a.attr("href");
    if (sRelativeLink.length() == 0)
      throw new DownloadFailedException(
        PbnTools.getStr("error.noAttr", "href", "a"), m_ow, false);
    // attr() return relative url, we need absolutu
    String sLink = a.absUrl("href");
    assert(sLink != null);
    // jsoup doesn't give a good abs url for file locations, so workarounding
    if (sLink.length() == 0) {
      sLink = getBaseUrl(a.baseUri()) + sRelativeLink;
    }
    // wget does not convert & and ? to %xx, so we need the decoded url
    return URLDecoder.decode(sLink);
  }
  
  /** Gets local link for the deal with the given number */
  protected String getLocalLinkForDeal(int iDeal)
    throws DownloadFailedException
  {
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
    ArrayList<String> asLinkRes = new ArrayList<String>();
    String sLastPart = "tview.php?t=" + m_sTitle;
    asLinkRes.add(getBaseUrl(m_sLink) + "tview.html");
    asLinkRes.add(getBaseUrl(m_sLink) + sLastPart);
    asLinkRes.add(getBaseUrl(m_sLink) + sLastPart + ".html");
    asLinkRes.add("http://webutil.bridgebase.com/v2/" + sLastPart);
    SoupProxy proxy = new SoupProxy();
    for (String sLinkRes: asLinkRes) {
      if (f.isDebugMode()) m_ow.addLine("Trying to get better title from "
        + sLinkRes);
      try {
        m_docRes = proxy.getDocument(sLinkRes);
        Elements titles = m_docRes.select(".bbo_tlv");
        if (titles == null)
          throw new DownloadFailedException(
            PbnTools.getStr("error.tagNotFound", ".bbo_tlv"), m_ow, true);
        if (titles.size() < 2)
          // first 2 tags are Title, Host - we need both
          continue;
        m_sTitle = titles.get(1).text() + " " + titles.get(0).text();
        m_sResLink = sLinkRes;
        // on success leave the loop
        break;
      }
      catch (JCException e) {
        if (f.isDebugMode()) m_ow.addLine(e.toString());
        continue;
      }
    }
  }

  // addOfset method{{{
  /** Adds <code>&offset=0</code> parameter to php url, which is necessary
    * because we don't support javascript */
  public static String addOffset(String sLink)
  {
    // only add offset if this is a php link
    if (!sLink.matches(".*\\.php\\?.*")) return sLink;
    // must not end with *.htm*
    if (sLink.matches(".*\\.htm(l?)")) return sLink;
    // maybe already has offset given?
    if (sLink.matches(".*[&\\?]offset=.*")) return sLink;
    // need to add if after all
    return sLink + "&offset=0";
  } //}}}
  
  // verify method {{{
  /** Verifies whether link points to a valid data in this format.
    * Sets m_sTitle and m_sDirName members. Leaves m_doc filled. */
  public boolean verify(String sLink, boolean bSilent)
  {
    sLink = addOffset(sLink);
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
      // as a fallback get the numeric title
      Element th = getFirstTag(doc, "th", true);
      m_sTitle = th.text();
      m_sTitle = m_sTitle.replaceFirst("Tourney ", "");
      m_sTitle = m_sTitle.replaceFirst("-$", "");
      getBetterTitle();
      if (f.isDebugMode()) m_ow.addLine(m_sTitle);
      setDirNameFromTitle();
      // throw exception if not found:
      getFirstTag(m_doc, ".board > a", true);
      m_cDeals = m_doc.select(".board > a").size();
      m_ow.addLine(PbnTools.getStr("tourDown.msg.title",
        m_sTitle, m_sDirName, m_cDeals));
    }
    catch (DownloadFailedException dfe) {
      return false;
    }

    return true;
  } //}}}

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
      fw.write(m_sLink); fw.newLine();
      if (m_docRes != null) {
        fw.write(m_sResLink); fw.newLine();
      }
      for (iDeal=1; iDeal<=m_cDeals; iDeal++) {
        String sDealLink = getLinkForDeal(iDeal); 
        fw.write(sDealLink);
        fw.newLine();
      }
      fw.close();
    }
    catch (java.io.IOException ioe) {
      throw new DownloadFailedException(ioe, m_ow, !m_bSilent);
    }
    return sLinksFile;
  }
  
  protected void downloadLins(String sLocalFile)
    throws DownloadFailedException
  {
    Document docLocal = null;
    try {
      SoupProxy proxy = new SoupProxy();
      docLocal = proxy.getDocumentFromFile(sLocalFile);
    }
    catch (JCException e) {
      throw new DownloadFailedException(e, m_ow, !m_bSilent);
    }
    
    if (docLocal.body() == null) {
      throw new DownloadFailedException(
        PbnTools.getStr("error.noBody"), m_ow, false);
    }
    
    m_ow.addLine(sLocalFile);
    for (Element elem: docLocal.select("a:matches(Lin)")) {
      String sLinLink = elem.attr("href");
      Matcher m =
        Pattern.compile("^.*[?&]id=([0-9]+)([?&].*)?$").matcher(sLinLink);
      if (!m.matches())
        throw new DownloadFailedException(PbnTools.getStr("error.linLinkId",
          sLinLink), m_ow, false);
      String sId = m.group(1);
      String sFileName = sId + ".lin";
      elem.attr("href", sFileName);
      try {
        f.saveUrlAsFile(sLinLink, new File(sFileName, m_sLocalDir));
        Writer w = new OutputStreamWriter(new FileOutputStream(sLocalFile),
          docLocal.outputSettings().charset());
        w.write(docLocal.html());
        w.close();
      } catch (IOException ioe) {
        throw new DownloadFailedException(ioe, m_ow, !m_bSilent); 
      }
      if (!m_bAllLins)
        break;
    }
/*      sNewCont = sNewCont.replaceAll("\"images/", "\"");
    }
    catch (MalformedURLException mue) {
      throw new DownloadFailedException(mue, m_ow, !m_bSilent); 
    */
  }

  protected void wget() throws DownloadFailedException
  {
    String sLinksFile = createIndexFile();
    wgetLinks(sLinksFile);
    for (int i=1; i<m_cDeals; i++) {
      downloadLins(getLocalLinkForDeal(i));
      break; //TODO remove it
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
