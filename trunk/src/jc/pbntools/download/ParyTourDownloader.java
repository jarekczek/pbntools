/* *****************************************************************************

    jedit options: :folding=explicit:tabSize=2:indentSize=2:noTabs=true:

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
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.StringTokenizer;
import javax.swing.JDialog;

import jc.f;
import jc.JCException;
import jc.outputwindow.OutputWindow;
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

public class ParyTourDownloader extends HtmlTourDownloader
{

  /** each deal is in file m_sDealPrefix + "nnn.html" */
  private String m_sDealPrefix = "";
  

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

  /** Gets remote link for the deal with the given number */
  protected String getLinkForDeal(int iDeal) {
    return m_sLink.replaceFirst("/[^/]+$", "/" + m_sDealPrefix 
      + String.format("%03d.html", iDeal));
  }
  
  /** Gets local link for the deal with the given number */
  protected String getLocalLinkForDeal(int iDeal) {
    return getLocalFile(getLinkForDeal(iDeal));
  }
  
  /** @param doc Document after redirection, containing 2 frames.
    *  */
  protected void getNumberOfDeals(Document doc, boolean bSilent) throws VerifyFailedException {
    String sFrameTag = "frameset > frame[name=lewa]";
    String sExpectedSrc = m_sDirName.toLowerCase() + "001.html";
    Element frame = getOneTag(doc, sFrameTag, false);
    if (frame == null) {
      throw new VerifyFailedException(
        PbnTools.getStr("error.getNumberOfDeals"), m_ow, !bSilent);
    }
    String sFoundSrc = frame.attr("src");
    if (!sFoundSrc.equalsIgnoreCase(sExpectedSrc)) {
      throw new VerifyFailedException(PbnTools.getStr("error.invalidTagValue",
                  sFrameTag, sExpectedSrc, sFoundSrc), m_ow, true);
    }
    assert(m_sDealPrefix.matches("*001.html"));
    m_sDealPrefix = sFoundSrc.replaceFirst("001.html$", "");
    
    // download page with the first deal
    String sLink1 = getLinkForDeal(1);
    m_ow.addLine(sLink1);
    Document doc1 = null;
    try {
      SoupProxy proxy = new SoupProxy();
      doc1 = proxy.getDocument(sLink1);
    }
    catch (JCException e) { throw new VerifyFailedException(e, m_ow); }
    
    // look for a link to the last one
    if (doc1.body() == null) {
      throw new VerifyFailedException(PbnTools.getStr("error.noBody"),
                                      m_ow, !bSilent);
    }
    Element elemLast = getOneTag(doc1.body(), "a[title=ostatnie]", false);
    if (elemLast == null) {
      throw new VerifyFailedException(
        PbnTools.getStr("error.getNumberOfDeals"), m_ow, !bSilent);
    }
    
    // parse the link to get the deal number
    String sLast = elemLast.attr("href");
    String sNoLast = sLast.replaceFirst(
      "^" + m_sDealPrefix + "([0-9]{3})\\.html", "$1");
    m_cDeals = 0;
    try {
      m_cDeals = Integer.parseInt(sNoLast);
    } catch (java.lang.NumberFormatException e) {} 
    if (m_cDeals == 0) {
      throw new VerifyFailedException(
        PbnTools.getStr("tourDown.error.parseNumber", sLast), m_ow, !bSilent);
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
      throw new VerifyFailedException(e, m_ow);
    }
    if (!bSilent)
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
  
  public boolean verify(String sLink, boolean bSilent)
    throws VerifyFailedException
  {
    setLink(sLink);
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
      createLocalDir();
      BufferedWriter fw = new BufferedWriter(new OutputStreamWriter(
        new FileOutputStream(sLinksFile), "ISO-8859-1"));
      fw.write(m_remoteUrl.toString());
      fw.newLine();
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
        // fw.write(sDealLinkTxt);
        // fw.newLine();
      }
      fw.close();
    }
    catch (java.io.IOException ioe) {
      throw new DownloadFailedException(ioe, m_ow, m_bSilent);
    }
    return sLinksFile;
  }
  
  protected void wget() throws DownloadFailedException
  {
    String sLinksFile = createIndexFile();
      
    String sCmdLine = "wget -p -k -nH -nd -nc --random-wait -E -e robots=off";
    // download slowly if not from localhost
    if (m_remoteUrl.toString().indexOf("localhost") < 0)
      sCmdLine += "-w 1";
    ArrayList<String> asCmdLine = new ArrayList<String>(Arrays.asList(sCmdLine.split(" ")));
    asCmdLine.add("--directory-prefix=" + m_sLocalDir);
    asCmdLine.add("--input-file=" + sLinksFile);
    
    if (PbnTools.bWindows) {
      // on Windows we need to point our wget.exe
      String sWget = PbnTools.getWgetPath();
      asCmdLine.set(0, sWget);
    }
    
    OutputWindow.Process p = m_ow.createProcess();
    try {
      p.exec(asCmdLine.toArray(new String[0]));
    } catch (JCException e) {
      throw new DownloadFailedException(e, m_ow, m_bSilent);
    }
    
    for (int iDeal=1; iDeal<=m_cDeals; iDeal++) {
      ajaxFile(getLinkForDeal(iDeal), true);
    }
  }

  protected Deal[] readDealsFromDir(String sDir)
    throws DownloadFailedException
  {
    ArrayList<Deal> deals = new ArrayList<Deal>();
    for (int iDeal=1; iDeal<=m_cDeals; iDeal++) {
      Deal ad[] = readDeals(getLocalLinkForDeal(iDeal), false);
      if (ad != null) {
        for (Deal d: ad) {
          d.setIdentField("Event", m_sTitle);
          deals.add(d);
        }
        if (PbnTools.getVerbos() > 0) {
          m_ow.addLine(PbnTools.getStr("tourDown.msg.readOne",
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
      throw new DownloadFailedException(e, m_ow, m_bSilent);
    }

    // locate tbody with deal definition without results
    Element dealElem = null;
    for (Element elemH4 : doc.select("h4")) {
      Elements parents = elemH4.parents();
      if (parents.size() >= 4) {
        dealElem = parents.get(3);
        break;
      }
    }
    if (dealElem == null || !"table".equals(dealElem.tagName()))
      throwElemNotFound("deal table"); 
    // java.lang.System.out.println("1:" + dealElem.html());
    extractHands(deal, dealElem);
    readScoring(deal, doc);
    return processResults(deal, doc);
  }
  
  /** Extracts hands from the given element and saves them to
    * <code>deal</code>.
    * @param dealElem tbody with deal definition without results */
  protected void extractHands(Deal deal, Element dealElem)
    throws DownloadFailedException
  {
    Elements elems = dealElem.select("tr");
    // first 4 rows describe the deal
    for (int iRow=0; iRow<=3; iRow++) {
      if (iRow > elems.size()) { throwElemNotFound("row no " + iRow); }
      switch (iRow) {
        
      case 0:
        // text "ROZDANIE xx"
        String sRozdanie = elems.get(iRow).text();
        sRozdanie = sRozdanie.replace("ROZDANIE ", "");
        try {
          deal.setNumber(Integer.parseInt(sRozdanie));
        } catch (NumberFormatException nfe) {
          throwElemNotFound("rozdanie");
        }
        break;
        
      case 1:
        // first character of this row denotes dealer
        String sText = elems.get(iRow).text();
        deal.setDealer(Deal.person(sText.substring(0,1)));
        
        // second word - vulnerability
        String sVulner = sText.replaceFirst("^. (\\S+) .*", "$1");
        sVulner = sVulner.replace("obie", "all");
        sVulner = sVulner.replace("nikt", "none");
        deal.setVulner(sVulner);
      }
      
    }
    
    // there are more than 4 .w tags, but the first 4 are ok
    // later comes minimax for example
    Elements handElems = dealElem.select(".w");
    if (handElems.size() < 4) {
      throw new DownloadFailedException(
        PbnTools.getStr("tourDown.error.wrongTagCount",
                        ".w", ">=4", handElems.size()));
    }
    int anPersons[] = new int[] { Deal.N, Deal.W, Deal.E, Deal.S };
    int iPerson = 0;
    for (Element handElem : handElems) {
      setCardsJfr(deal, anPersons[iPerson], handElem);
      iPerson++;
      if (iPerson >= anPersons.length) { break; }
    }
    
    //throw new DownloadFailedException("dosc", true);
  }

  /** readScoring method {{{
   * Reads scoring type of the deal from <code>doc</code> and
   * sets it in <code>deal</code>.
   */
  private void readScoring(Deal deal, Document doc)
  {
    boolean bOk;
    String sScoring = null;
    ArrayList<Deal> ad = new ArrayList<Deal>();
    Elements elems = doc.select("div#pro tr");
    if (elems.size() >= 2) {
      // potrzebny nam jest drugi wiersz tabelki, nag³ówkowy
      Elements tds = elems.get(1).select("td");
      if (tds.size() == 8) {
        // musi mieæ 8 kolumn, pierwsza jest niewidoczna
        if ("numery".equals(tds.get(1).text())) {
          sScoring = tds.get(7).text();
        }
      }
    }
    if ("%".equals(sScoring)) {
      deal.setScoring("MP");
    } else {
      if (PbnTools.getVerbos() > 0) {
        m_ow.addLine("Unknown scoring header: " + sScoring);
      }
    }
  } //}}}

  /** Multiplies given <code>deal</code> by the number of results. */
  private Deal[] processResults(Deal deal0, Document doc)
    throws DownloadFailedException
  {
    ArrayList<Deal> ad = new ArrayList<Deal>();
    Elements elems = doc.select("div#pro tr");
    if (elems.size() < 1) { throwElemNotFound("div#pro tr"); }
    for (Element tr: elems) {
      Elements tds = tr.select("td");
      // 1st column is invisible (index 0)
      // w 2. kolumnie powinien byæ numer pary, wiêc tylko te wiersze
      // bêdziemy czytaæ
      boolean bValidContract = false;
      // valid deals have pair numbers in columns 1 and 2
      if (tds.size() >= 8
          && tds.get(1).text().matches("[0-9]+")
          && tds.get(2).text().matches("[0-9]+")) {
        bValidContract = true;
      }
      // columns 7 and 8 contain plain result in contract points
      // valid deals have digits in one of these columns, even PASS deal
      if (bValidContract
          && !tds.get(7).text().matches("[0-9]+")
          && !tds.get(8).text().matches("[0-9]+")) {
        bValidContract = false;
      }
      
      // if ("TD".equals(tds.get(3).text())) {
        // rozdanie bez wyniku
        // bValidContract = false;
      // }
      if (bValidContract) {
        Deal d = deal0.clone();
        d.setIdentField("North", "Para-" + tds.get(1).text());
        d.setIdentField("South", "Para-" + tds.get(1).text());
        d.setIdentField("East", "Para-" + tds.get(2).text());
        d.setIdentField("West", "Para-" + tds.get(2).text());
        d.setDeclarer(Deal.person(tds.get(4).text()));
        processContract(d, tds.get(3));
        processResult(d, tds.get(6).text());
        if (!d.isOk()) {
          reportErrors(d.getErrors());
        }
        ad.add(d);
      }
    }
    
    return ad.toArray(new Deal[0]);
  }
}
