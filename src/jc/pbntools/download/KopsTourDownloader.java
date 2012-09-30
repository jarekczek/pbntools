/* *****************************************************************************

    jedit options: :folding=explicit:tabSize=2:indentSize=2:noTabs=true:

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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.URL;
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

public class KopsTourDownloader extends HtmlTourDownloader
{

  /** Subdocument (frame contents), wyn.html */
  protected Document m_docWyn;
  /** Subdocument (frame contents), roz.html */
  protected Document m_docRoz;
  

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
    String sLink = getBaseUrl(m_sLink) + "p" + iDeal + ".html";
    if (f.isDebugMode())
      System.out.println("getLinkForDeal(" + iDeal + ") = " + sLink);
    return sLink;
  }
  
  /** Gets local link for the deal with the given number */
  protected String getLocalLinkForDeal(int iDeal) {
    return getLocalFile(getLinkForDeal(iDeal));
  }
  
  /** @param doc Document after redirection, containing 2 frames.
    *  */
  protected void getNumberOfDeals(Document doc, boolean bSilent)
    throws VerifyFailedException {
    m_cDeals = m_docRoz.select("tr").size();
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
    m_ow.addLine(PbnTools.m_res.getString("msg.documentLoaded"));

    if (!checkGenerator(doc, "KoPS2www, JFR 2005", bSilent))
      throw new VerifyFailedException("generator");
    if (getOneTag(doc, "frame[src=wyn.html]", bSilent) == null)
      throw new VerifyFailedException("frame[src=wyn.html]");
    if (getOneTag(doc, "frame[src=roz.html]", bSilent) == null)
      throw new VerifyFailedException("frame[src=roz.html]");
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

    // download 2 frames
    try {
      SoupProxy proxy = new SoupProxy();
      m_docWyn = proxy.getDocument(getBaseUrl(m_sLink) + "wyn.html");
      m_docRoz = proxy.getDocument(getBaseUrl(m_sLink) + "roz.html");
    }
    catch (JCException e) {
      throw new VerifyFailedException(e, m_ow);
    }
    
    // default title, as <title> tag does not work well for kops
    // an internal path is given there, so we get a better title
    Element title = getOneTag(m_docWyn, "h4", bSilent);
    if (title == null)
      throw new VerifyFailedException(
        PbnTools.getStr("error.oneTagExpected", "h4", " (wyn.html)"));
    m_sTitle = title.text();

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
        throw new DownloadFailedException(
          PbnTools.getStr(
            "error.unableToCreateDir", m_sLocalDir), m_ow, true);
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
            m_ow, true);
        }
        fw.write(m_sLink);
        fw.newLine();
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
    
    OutputWindow.Process p = m_ow.createProcess();
    try {
      p.exec(asCmdLine.toArray(new String[0]));
    } catch (JCException e) {
      throw new DownloadFailedException(e, m_ow, m_bSilent);
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

    readNumber(deal, doc);
    // locate tbody with deal definition without results
    Element dealElem = null;
    Elements tables = doc.select("table");
    if (tables.size() < 1)
      throwElemNotFound("table");
    dealElem = tables.get(0);
    extractHands(deal, dealElem);
    readScoring(deal, doc);
    //TODO isOk()
    return processResults(deal, doc);
  }
  
  /** Extracts hands from the given element and saves them to
    * <code>deal</code>.
    * @param dealElem tbody with deal definition without results */
  protected void extractHands(Deal deal, Element dealElem)
    throws DownloadFailedException
  {
    Elements elems = dealElem.select("td");
    // 9 cells, (3x3), describe the deal
    for (int iCell=0; iCell<=8; iCell++) {
      if (iCell > elems.size()) { throwElemNotFound("td no " + iCell); }
      int nPerson = -1;
      switch (iCell) {
        
      case 0:
        // first character of this cell denotes dealer
        String sText = elems.get(iCell).text();
        deal.setDealer(Deal.person(sText.substring(0,1)));
        
        // second word - vulnerability
        String sVulner = sText.replaceFirst("^. (\\S+)$", "$1");
        sVulner = sVulner.replace("obie", "all");
        sVulner = sVulner.replace("nikt", "none");
        deal.setVulner(sVulner);
        break;
        
      case 1:
        nPerson = Deal.N; break;
      case 3:
        nPerson = Deal.W; break;
      case 5:
        nPerson = Deal.E; break;
      case 7:
        nPerson = Deal.S; break;
      }
      
      if (nPerson >= 0) {
        setCardsJfr(deal, nPerson, elems.get(iCell));
      }

    }
    
    //throw new DownloadFailedException("dosc", true);
  }

  /** readNumber method {{{
   * Reads deal number from <code>doc</code> and
   * sets it in <code>deal</code>.
   */
  private void readNumber(Deal deal, Document doc)
    throws DownloadFailedException
  {
    Elements h4 = doc.select("h4");
    if (h4.size() == 0)
      throwElemNotFound("h4");
    String sRozdanie = h4.get(0).text();
    sRozdanie = sRozdanie.replace("ROZDANIE NR ", "");
    try {
      deal.setNumber(Integer.parseInt(sRozdanie));
    } catch (NumberFormatException nfe) {
      throwElemNotFound("h4: ROZDANIE NR n");
    }
  } //}}}

  /** readScoring method {{{
   * Reads scoring type of the deal from <code>doc</code> and
   * sets it in <code>deal</code>.
   */
  private void readScoring(Deal deal, Document doc)
  {
    boolean bOk;
    String sScoring = null;
    ArrayList<Deal> ad = new ArrayList<Deal>();
    Elements elems = doc.select("tr.nagl");
    if (elems.size() > 0) {
      // first header contains scoring in 7 column (out of 8)
      Elements tds = elems.get(0).select("td");
      if (tds.size() == 8) {
        if ("numery".equalsIgnoreCase(tds.get(0).text())) {
          sScoring = tds.get(6).text();
        }
      }
    }
    if ("%".equals(sScoring)) {
      deal.setScoring("MP");
    } else if ("PUNKTY".equals(sScoring)) {
      deal.setScoring("IMP");
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
    Elements elems = doc.select("tr.niep, tr.parz");
    if (elems.size() < 1) { throwElemNotFound("tr.niep, tr.parz"); }
    for (Element tr: elems) {
      Elements tds = tr.select("td");
      boolean bValidContract = false;
      // valid deals have pair numbers in columns 1 and 2
      if (tds.size() >= 8
          && tds.get(0).text().matches("[0-9]+")
          && tds.get(1).text().matches("[0-9]+")) {
        bValidContract = true;
      }
      // columns 7 and 8 contain plain result in contract points
      // valid deals have digits in one of these columns, even PASS deal
      if (bValidContract
          && !tds.get(6).text().matches("[0-9]+")
          && !tds.get(7).text().matches("[-0-9]+")) {
        bValidContract = false;
      }
      
      if (bValidContract) {
        Deal d = deal0.clone();
        d.setIdentField("North", "Para-" + tds.get(0).text());
        d.setIdentField("South", "Para-" + tds.get(0).text());
        d.setIdentField("East", "Para-" + tds.get(1).text());
        d.setIdentField("West", "Para-" + tds.get(1).text());
        d.setDeclarer(Deal.person(tds.get(3).text()));
        processContract(d, tds.get(2));
        processResult(d, tds.get(5).text());
        if (!d.isOk()) {
          reportErrors(d.getErrors());
        }
        ad.add(d);
      }
    }
    
    return ad.toArray(new Deal[0]);
  }
}
