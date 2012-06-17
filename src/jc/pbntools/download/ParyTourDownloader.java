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
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
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
    ArrayList<Deal> deals = new ArrayList<Deal>();
    for (int iDeal=1; iDeal<=m_cDeals; iDeal++) {
      Deal ad[] = readDeals(getLinkForDeal(iDeal), false);
      if (ad != null) {
        for (Deal d: ad) {
          d.setIdentField("Event", m_sTitle);
          deals.add(d);
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
    
    Deal deal = new Deal();
    try {
      SoupProxy proxy = new SoupProxy();
      doc = proxy.getDocument(sUrl);
    }
    catch (JCException e) {
      throw new DownloadFailedException(e);
    }

    // locate tbody with deal definition without results
    Element dealElem = null;
    for (Element elemH4 : doc.select("h4")) {
      Elements parents = elemH4.parents();
      if (parents.size() >= 2) {
        dealElem = parents.get(2);
        break;
      }
    }
    if (dealElem == null) { throwElemNotFound("deal table"); } 
    // java.lang.System.out.println("1:" + dealElem.html());
    extractHands(deal, dealElem);
    return new Deal[] { deal };
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
        setCards(deal, anPersons[iPerson], handElem);
        iPerson++;
        if (iPerson >= anPersons.length) { break; }
      }
    
    }
    
    
    //throw new DownloadFailedException("dosc", true);
  }

  /** Deals cards presented by html <code>hand</code> to
    * <code>nPerson</code>. Saves it in <code>deal</code>. */
  protected void setCards(Deal deal, int nPerson, Element hand)
    throws DownloadFailedException
  {
    String asText[] = SoupProxy.splitElemText(hand);
    for (Element img : hand.getElementsByTag("img")) {
      int nColor = getImgColor(img);
      String sCards = asText[img.elementSiblingIndex() + 1];
      StringTokenizer st = new StringTokenizer(sCards, HTML_SPACE_REG);
      while (st.hasMoreTokens()) {
        Card card = new Card();
        card.setColor(nColor);
        card.setRank(st.nextToken());
        deal.setCard(card, nPerson);
      }
    }
    deal.fillHands();
  }

}
