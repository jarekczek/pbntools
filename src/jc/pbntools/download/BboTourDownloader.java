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

import jc.JCException;
import jc.SoupProxy;
import jc.f;
import jc.outputwindow.SimplePrinter;
import jc.pbntools.Deal;
import jc.pbntools.PbnTools;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Server requires authentication. Sessions are kept in cookie PHPSESSID.
 * There is also a cookie SRV, probably meaningless for us.
 */
public class BboTourDownloader extends HtmlTourDownloader
{
  private static Logger log = LoggerFactory.getLogger(BboTourDownloader.class);

  /** Overall turney results document */
  protected Document m_docRes;
  /** Overall turney results link */
  protected String m_sResLink;
  /** Whether to download all the lins or only the first one from each
      board */
  protected boolean m_bAllLins = true;
  /** Number of generated lins. */
  protected int m_cLins;

  public String getName() { return "Bbo"; }

  @Override
  public void setOutputWindow(SimplePrinter ow) //{{{
  {
    m_ow = ow;
  } //}}}

  @Override
  public SimplePrinter getOutputWindow() {
    return m_ow;
  }

  // getLinkForDeal method //{{{
  /** Gets remote link for the deal with the given number */
  protected String getLinkForDeal(int iDeal)
    throws DownloadFailedException 
  {
    Element a = getNthTag(m_doc, ".board > a", iDeal, false);
    String sLink = SoupProxy.absUrl(a, "href");
    if (sLink.length() == 0)
      throw new DownloadFailedException(
        PbnTools.getStr("error.noAttr", "href", "a"), m_ow, false);
    // wget does not convert & and ? to %xx, so we need the decoded url
    // TODO: on windows it does it with ?, and I just added --restrict switch
    // so Windows behaviour will always be applied. Probably this breaks now.
    return f.decodeUrl(sLink);
  } //}}}
  
  // getLocalLinkForDeal method //{{{
  /** Gets local link for the deal with the given number */
  protected String getLocalLinkForDeal(int iDeal)
    throws DownloadFailedException
  {
    return getLocalFile(getLinkForDeal(iDeal));
  } //}}}
  
  // getNumberOfDeals method //{{{
  /** @param doc Document after redirection, containing 2 frames.
    *  */
  protected void getNumberOfDeals(Document doc, boolean bSilent)
    throws DownloadFailedException {
    m_cDeals = 0;
  } //}}}

  // getBetterTitle method //{{{
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
    asLinkRes.add(getBaseUrl(m_sLink)
                  + sLastPart.replace('?', '@') + ".html");
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
            PbnTools.getStr("error.tagNotFound", ".bbo_tlv"), m_ow, false);
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
  } //}}}

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

  @Override
  public boolean verify(String sLink, boolean bSilent)
  {
    return verify(sLink, bSilent, true);
  }

  // verify method {{{
  /** Verifies whether link points to a valid data in this format.
    * Sets m_sTitle and m_sDirName members. Leaves m_doc filled. */
  public boolean verify(String sLink, boolean bSilent, boolean tryLogin)
  {
    sLink = addOffset(sLink);
    setLink(sLink);
    if (!bSilent)
      println(PbnTools.getStr("tourDown.msg.willVerify", sLink));
    Document doc;
    try {
      SoupProxy proxy = new SoupProxy();
      doc = proxy.getDocument(m_sLink, SoupProxy.NO_CACHE);
      m_doc = doc;
      m_remoteUrl = proxy.getUrl();
    }
    catch (JCException e) {
      m_ow.addLine(e.toString());
      if (f.isDebugMode())
        e.printStackTrace();
      return false;
    }
    if (!bSilent)
      println(PbnTools.m_res.getString("msg.documentLoaded"));
    try {
      if (SoupProxy.getSelectText(doc, "div.bbo_content").startsWith("Please login")) {
        m_ow.addLine(PbnTools.getStr("tourDown.msg.asksLogin",
          "BBO"));
        if (tryLogin)
          return verifyAfterLogin(sLink, doc.baseUri(), bSilent);
        else {
          throw new RuntimeException(PbnTools.getStr("tourDown.error.asksLoginAgain",
            "BBO"));
        }
      }
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

  private boolean verifyAfterLogin(String sLink, String loginLink, boolean bSilent)
    throws DownloadFailedException {
    if (f.isNullOrEmpty(PbnTools.getProp("bbo.user"))
      || f.isNullOrEmpty(PbnTools.getProp("bbo.pass")))
      throw new DownloadFailedException(
        PbnTools.getStr("tourDown.error.noBboUser"), m_ow, true);
    loginLink = loginLink.replaceFirst("\\?.*$", "?t=%2Fmyhands%2Findex.php%3F");
    m_ow.addLine(PbnTools.getStr("tourDown.msg.willLogin", loginLink));
    SoupProxy proxy = new SoupProxy();
    Map<String, String> data = new HashMap<String, String>();
    data.put("t", "/myhands/index.php?");
    data.put("count", "1");
    data.put("username", PbnTools.getProp("bbo.user"));
    data.put("password", PbnTools.getProp("bbo.pass"));
    data.put("submit", "Login");
    Document doc = null;
    try {
      URL url = new URL(loginLink);
      doc = proxy.post(url, data);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    } catch (SoupProxy.Exception e2) {
      throw new RuntimeException(e2);
    }
    saveDocumentAsFile(doc, "bbo_login_result.html");
    String mainText = SoupProxy.getSelectText(doc, "div.bbo_content").toLowerCase();
    if (mainText.contains("username or password incorrect")) {
      throw new DownloadFailedException(
        PbnTools.getStr("tourDown.msg.authFailed"), m_ow, true);
    }
    return verify(sLink, bSilent, false);
  }

  private void saveDocumentAsFile(Document doc, String filename) {
    try {
      String dir = m_sLocalDir != null ? m_sLocalDir : PbnTools.getWorkDir(false);
      File outFile = new File(dir, filename);
      PrintWriter pr = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outFile), Charset.forName("UTF-8")));
      pr.print(doc.outerHtml());
      pr.close();
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  protected String createIndexFile() throws DownloadFailedException //{{{
  {
    int iDeal;
    String sLinksFile = new File(m_sLocalDir, "links.txt").getAbsolutePath();
    println(PbnTools.getStr("tourDown.msg.creatingIndex", sLinksFile));
    try {
      if (!(new File(m_sLocalDir).mkdir())) {
        throw new DownloadFailedException(
          PbnTools.getStr(
            "error.unableToCreateDir", m_sLocalDir), m_ow, !m_bSilent);
      }
      BufferedWriter fw = new BufferedWriter(new OutputStreamWriter(
        new FileOutputStream(sLinksFile), "ISO-8859-1"));
      fw.write(m_sLink); fw.newLine();
      if (m_sResLink != null) {
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
  } //}}}

  // correctLin method {{{
  /** Does necessary corrections to supplied LIN contents:
   *  Inserts `pg` commands (pause game).
   */
  String correctLin(String sLin0)
  {
    int cPc = 0;
    StringBuilder sb = new StringBuilder();
    Scanner sc = new Scanner(sLin0).useDelimiter("\\|");
    while (sc.hasNext()) {
      String sComm = sc.next();
      String sArg = "";
      if (sc.hasNext()) {
        sArg = sc.next();
      }
      if (sComm.equals("pc")) {
        if (cPc % 4 == 0) {
          sb.append("pg||");
        }
        cPc++;
      }
      sb.append(sComm);
      sb.append('|');
      sb.append(sArg);
      sb.append('|');
    }
    sb.append("pg||");
    return sb.toString();
  } //}}}
  
  // saveLinFromMovie method //{{{
  /** Saves lin to the file.
   * @param elemLin The <code>a</link> element with a Lin link.
   */
  protected void saveLinFromMovie(Element elemLin, File outFile)
    throws DownloadFailedException, java.io.IOException
  {
    Element td = elemLin.parent();
    Elements elems = td.select("a:matches(Movie)");
    if (elems.size() == 0)
      throw new DownloadFailedException(PbnTools.getStr("error.noMovie",
        td.html()), m_ow, false);
    Element movieElem = elems.get(0);
    String sOnClick = movieElem.attr("onclick");
    if (sOnClick.length() == 0)
      throw new DownloadFailedException(PbnTools.getStr("error.noAttr",
        "onclick", movieElem.outerHtml()), m_ow, false);
    Matcher m = Pattern.compile("^.*lin\\('(.*)'\\);.*$").matcher(sOnClick);
    if (!m.matches())
      throw new DownloadFailedException(PbnTools.getStr("error.onClickNotRec",
        sOnClick), m_ow, false);
    String sLin = f.decodeUrl(m.group(1));
    sLin = correctLin(sLin);
    f.writeToFile(sLin, outFile);
  } //}}}
  
  protected void downloadLins(String sLocalFile) //{{{
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
    
    Elements elems = docLocal.select("a:matches(Lin)");
    if (elems.size() == 0)
      throw new DownloadFailedException(PbnTools.getStr("error.tagNotFound",
        "a:matches(Lin)"), m_ow, !m_bSilent);
    for (Element elem: elems) {
      String sLinLink = elem.attr("href");
      String sId = getLinIdFromLink(sLinLink);
      if (sId == null)
        throw new DownloadFailedException(PbnTools.getStr("error.linLinkId",
          sLinLink), m_ow, false);
      String sFileName = sId + ".lin";
      elem.attr("href", sFileName);
      try {
        File outFile = new File(m_sLocalDir, sFileName);
        if (PbnTools.getVerbos() > 0)
          m_ow.addLine(PbnTools.getStr("tourDown.msg.savingLin",
            outFile, sLinLink));
        // f.saveUrlAsFile(sLinLink, outFile);
        // f.sleepUnint(1000);
        saveLinFromMovie(elem, outFile);
        m_cLins += 1;
        Writer w = new OutputStreamWriter(new FileOutputStream(sLocalFile),
          docLocal.outputSettings().charset());
        try {
          w.write(docLocal.html());
        }
        finally {
          w.close();
        }
      } catch (IOException ioe) {
        throw new DownloadFailedException(ioe, m_ow, !m_bSilent); 
      }
      if (!m_bAllLins)
        break;
    }
  } //}}}

  private String getLinIdFromLink(String sLinLink) {
    Matcher m =
      Pattern.compile("^.*[?&]id=([0-9]+)([?&].*)?$").matcher(sLinLink);
    String sId = null;
    if (m.matches())
      sId = m.group(1);
    else {
      Matcher m2 = Pattern.compile("^.*/([0-9]+)\\.lin$").matcher(sLinLink);
      if (m2.matches())
        sId = m2.group(1);
    }
    return sId;
  }

  protected void wget() throws DownloadFailedException //{{{
  {
    m_cLins = 0;
    String sLinksFile = createIndexFile();
    wgetLinks(sLinksFile);
    for (int i=1; i <= m_cDeals; i++) {
      downloadLins(getLocalLinkForDeal(i));
    }
    m_ow.addLine(PbnTools.getStr("tourDown.msg.linsSaved", m_cLins)); 
  } //}}}

  protected Deal[] readDealsFromDir(String sDir) //{{{
    throws DownloadFailedException
  {
    if (f.isDebugMode())
      m_ow.addLine("readDealsFromDir: " + sDir);
    ArrayList<Deal> deals = new ArrayList<Deal>();
    for (int iDeal=1; iDeal<=m_cDeals; iDeal++) {
      if (Thread.interrupted()) {
        println(PbnTools.getStr("msg.interrupted"));
        break;
      }
      Deal ad[] = readDeals(getLocalLinkForDeal(iDeal), m_bSilent);
      if (ad != null) {
        for (Deal d: ad) {
          if (m_sTitle != null)
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
  } //}}}

  public Deal[] readDeals(String sTravellerUrl, boolean bSilent) //{{{
    throws DownloadFailedException
  {
    Document doc = null;
    m_sCurFile = sTravellerUrl;
    m_bSilent = bSilent;
    ArrayList<Deal> aDeals = new ArrayList<Deal>();

    if (PbnTools.getVerbos() > 0)
      m_ow.addLine(PbnTools.getStr("msg.processing", sTravellerUrl));
    resetErrors();
    try {
      SoupProxy proxy = new SoupProxy();
      doc = proxy.getDocument(sTravellerUrl);
      Elements nums = getElems(doc, "td.handnum", m_bSilent);
      for (Element num: nums) {
        Element tr = num.parent();
        Element aLin = getOneTagEx(tr, "a:matches(Lin)", m_bSilent);
        String sFile = SoupProxy.absUrl(aLin, "href");
        if (f.isDebugMode())
          m_ow.addLine("reading lin: " + sFile);
        String sLin = f.readFile(sFile);
        LinReader linReader = new LinReader();
        linReader.setOutputWindow(m_ow);
        Deal d = linReader.readLin(sLin, m_bSilent)[0];
        d.setId(f.getFileName(sFile));
        processResults(d, tr);
        
        // At this moment we have both contract (taken from traveller) and
        // plays (from lin). But it can happen that the play is judged
        // without contract (N/A) and the plays are present in the file.
        // This is kind of contradiction, but since the plays are in the
        // lin file, let's leave it here too. Thus commenting line below.
        // if (d.getDeclarer() < 0)
          // d.clearPlays();

        if (!d.isOk()) {
          reportErrors(d.getErrors());
        }
        aDeals.add(d);
      }
    }
    catch (DownloadFailedException e) {
      // bPrint = false, because rethrowing
      throw new DownloadFailedException(e, m_ow, false);
    }
    catch (JCException e) {
      throw new DownloadFailedException(e, m_ow, !m_bSilent);
    }
    catch (java.io.IOException ioe) {
      throw new DownloadFailedException(ioe, m_ow, !m_bSilent);
    }
    assert(aDeals.size() > 0);

    return aDeals.toArray(new Deal[0]);
  } //}}}

  @Override
  protected void postProcess() {
    new BboResultsExtractor(this).extractResultsCsv();
  }

  // extractHands method //{{{
  /** Extracts hands from the given element and saves them to
    * <code>deal</code>.
    * @param dealElem tbody with deal definition without results */
  protected void extractHands(Deal deal, Element dealElem)
    throws DownloadFailedException
  {
    //throw new DownloadFailedException("dosc", true);
  } //}}}

  /** readScoring method {{{
   * Reads scoring type of the deal from <code>doc</code> and
   * sets it in <code>deal</code>.
   */
  private void readScoring(Deal deal, Document doc)
  {
  } //}}}

  // processBboResult method {{{
  /** Bbo result is a string <code>PASS</code> or of type
   *  <code>2NTxE-2</code>.
   */
  public void processBboResult(Deal d, String sBboResult)
    throws DownloadFailedException
  {
    if (sBboResult.equalsIgnoreCase("pass")) {
      d.setContractHeight(0);
      return;
    }
    if (sBboResult.matches("A[+-=][+-=]")) { /* A=+, A==, A+= */
      return;
    }
    Matcher m = Pattern.compile("^(.*)([NESW])((=)|([-+][0-9]+))$")
      .matcher(sBboResult);
    if (!m.matches()) {
      throw new DownloadFailedException(PbnTools.getStr(
        "tourDown.error.unrecognizedContract", d.getNumber(), sBboResult));
    }
    Element contractElem = m_doc.createElement("p");
    contractElem.appendText(m.group(1));
    processContract(d, contractElem);
    d.setDeclarer(Deal.person(m.group(2)));
    processResult(d, m.group(3));
  } //}}}

  // processResults method //{{{
  /** Updates deal with the info found in results.
   * @param tr Element containing results for this deal. */
  private void processResults(Deal d, Element tr)
    throws DownloadFailedException
  {
    d.setIdentField("Date", getOneTag(tr, "td:eq(1)", m_bSilent).text());
    d.setIdentField("North", getOneTag(tr, "td.north", m_bSilent).text());
    d.setIdentField("South", getOneTag(tr, "td.south", m_bSilent).text());
    d.setIdentField("East", getOneTag(tr, "td.east", m_bSilent).text());
    d.setIdentField("West", getOneTag(tr, "td.west", m_bSilent).text());
    processBboResult(d, getOneTag(tr, "td.result", m_bSilent).text());
  } //}}}
}
