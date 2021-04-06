package jc.pbntools.download;

import jc.JCException;
import jc.SoupProxy;
import jc.f;
import jc.pbntools.Deal;
import jc.pbntools.PbnTools;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

abstract public class BboCommonDownloader extends HtmlTourDownloader {

  /** Whether to download all the lins or only the first one from each
      board */
  protected boolean m_bAllLins = true;
  /** Number of generated lins. */
  protected int m_cLins;
  private static Logger log = LoggerFactory.getLogger(BboCommonDownloader.class);

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
  }

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
        if (!saveLinFromMovie(elem, outFile)) {
          saveLinFromLinLink(sLinLink, outFile);
        }
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

  private void saveLinFromLinLink(String sLinLink, File outFile) throws IOException {
    f.saveUrlAsFile(sLinLink, outFile);
    f.sleepNoThrow(1000 * delayForUrl(sLinLink));
  }

  protected String getLinIdFromLink(String sLinLink) {
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

  /** Saves lin to the file.
   * @param elemLin The <code>a</link> element with a Lin link.
   * @return false if lin not included in movie
   */
  protected boolean saveLinFromMovie(Element elemLin, File outFile)
    throws DownloadFailedException, IOException
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
    if (sOnClick.startsWith("hv_popup(")) {
      log.debug("lin not included in movie link.");
      return false;
    }
    Matcher m = Pattern.compile("^.*lin\\('(.*)'\\);.*$").matcher(sOnClick);
    if (!m.matches())
      throw new DownloadFailedException(PbnTools.getStr("error.onClickNotRec",
        sOnClick), m_ow, false);
    String sLin = f.decodeUrl(m.group(1));
    sLin = correctLin(sLin);
    f.writeToFile(sLin, outFile);
    return true;
  } //}}}

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
}
