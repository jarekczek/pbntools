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
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jc.f;
import jc.JCException;
import jc.outputwindow.OutputWindow;
import jc.outputwindow.OutputWindowWriter;
import jc.pbntools.Card;
import jc.pbntools.Deal;
import jc.pbntools.PbnFile;
import jc.pbntools.PbnTools;
import jc.SoupProxy;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

/**
 * This class's methods are called from {@link TourDownloaderThread#run}.
 */

abstract public class HtmlTourDownloader
  implements DealReader
{
  public static final String HTML_SPACE_REG = "[ \u00A0]";
  
  protected String m_sLink;
  protected URL m_remoteUrl;
  protected URL m_localUrl;
  public String m_sTitle;
  public String m_sDirName;
  public String m_sLocalDir;
  protected OutputWindow m_ow;
  protected Document m_doc;
  public int m_cDeals;
  /** Current file, to show in error messages */
  protected String m_sCurFile;
  /** Whether to show error messages. */ 
  protected boolean m_bSilent;
  
  /** set the window to which output messages will be directed */
  abstract public void setOutputWindow(OutputWindow ow);
  
  /** Not called from anywhere yet. Default constructor is sufficient. */
  protected void clear() {
    m_sLink = "";
    m_remoteUrl = null; m_localUrl = null;
    m_sTitle = ""; m_sDirName = ""; m_sLocalDir = "";
    m_ow = null;
    m_doc = null;
    m_cDeals = 0;
    m_sCurFile = "";
    m_bSilent = false;
  }
  
  public void setLink(String sLink) {
    m_sLink = sLink;
  }

  /** Returns the url which may be used as base url for links from inside
    * the given url. That is <code>http://aaa.com/start/</code> for both
    * <code>http://aaa.com/start/page.htm</code>
    * and <code>http://aaa.com/start</code>. */
  public static String getBaseUrl(String sUrl)
  {
    sUrl = sUrl.replaceFirst("/[^/]+\\.[a-zA-Z]+$", "/");
    if (!sUrl.endsWith("/"))
      sUrl += "/";
    return sUrl;
  }

  /** select <code>sTag</code> but require exactly one match */
  protected Element getOneTag(Element parent, String sTag, boolean bSilent) {
    Elements elems = parent.select(sTag);
    if (elems.size()==0) {
      if (!bSilent) {
        m_ow.addLine(PbnTools.getStr("error.tagNotFound", sTag));
      }
      return null;
    }
    if (elems.size()>1) {
      if (!bSilent) {
        m_ow.addLine(PbnTools.getStr("error.onlyOneTagAllowed", sTag));
      }
      return null;
    }
    return elems.get(0);
  }
  
  /** Returns the card color corresponding to the given img tag element. 
    * @param img <code>img</code> Element. Its <code>src</code> attribute
    * denotes the card color. This version also accepts NT.
    * @return The color from {@link jc.pbntools.Card} constants
    *         or 0 for NT. 
    */
  protected int getImgColorOrNt(Element img)
    throws DownloadFailedException
  {
      String sSrc = img.attr("src");
      int nColor = 0;
      String sColor = sSrc.replaceFirst("^.*/", "");
      sColor = sColor.replaceFirst("\\.[a-zA-Z]+$", "");
      if ("N".equals(sColor)) {
        // no trump
        return 0;
      }
      if (sColor.length() == 1) {
        nColor = Card.color(sColor.charAt(0));
      }
      if (nColor == 0) { throw new DownloadFailedException(
        PbnTools.getStr("tourDown.error.notRecognColor", img.outerHtml()));
      }
      return nColor;
  }
  
  /** Returns the card color corresponding to the given img tag element. 
    * @param img <code>img</code> Element. Its <code>src</code> attribute
    * denotes the card color.
    * @return The color from {@link jc.pbntools.Card} constants. */
  protected int getImgColor(Element img)
    throws DownloadFailedException
  {
      int nColor = getImgColorOrNt(img);
      if (nColor == 0) { throw new DownloadFailedException(
        PbnTools.getStr("tourDown.error.notRecognColor", img.outerHtml()));
      }
      return nColor;
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
  
  /** Verify whether link points to a valid data in this format */
  abstract protected boolean verify(boolean bSilent) throws VerifyFailedException;
  
  abstract protected void wget() throws DownloadFailedException;
  
  abstract protected Deal[] readDealsFromDir(String sDir)
    throws DownloadFailedException;

  /** Reads deals from the given url. */
  abstract public Deal[] readDeals(String sUrl, boolean bSilent)
    throws DownloadFailedException;

  protected String saveDealsAsPbn(Deal[] aDeal, String sDir)
    throws DownloadFailedException
  {
    String sPath = "";
    try {
      File file = new File(sDir, m_sDirName.toLowerCase() + ".pbn");
      PbnFile pbnFile = new PbnFile();
      pbnFile.addDeals(aDeal);
      sPath = file.getAbsolutePath();
      pbnFile.save(file.getAbsolutePath());
    }
    catch (IOException ioe) {
      throw new DownloadFailedException(ioe, m_ow, m_bSilent);
    }
    return sPath;
  }

  /** performs 2 operations: downloading (if required) from internet and
    * converting (locally) to pbns */
  public boolean fullDownload() throws DownloadFailedException
  {
    if (m_remoteUrl.getProtocol().equals("file")) {
      m_localUrl = m_remoteUrl;
      m_sLocalDir = getBaseUrl(m_localUrl.getFile());
      m_ow.addLine(PbnTools.getStr("tourDown.msg.localLink", m_sLocalDir));
    } else {
      boolean bDownloaded = isDownloaded();

      // constructing local url after isDownloaded set m_sLocalDir
      String sFileName = m_remoteUrl.toString().replaceFirst("^.*/", "");
      if (sFileName.indexOf('.')<0) { sFileName = "index.html"; }
      try {
        m_localUrl = new File(new File(m_sLocalDir, "html"), sFileName).toURI().toURL();
      } catch (Exception e) {
        throw new DownloadFailedException(e, m_ow, m_bSilent);
      }
      m_ow.addLine("local url: " + m_localUrl);
      
      if (!bDownloaded) {
        m_ow.addLine(PbnTools.getStr("tourDown.msg.willWget", m_sLocalDir));
        wget();
        m_ow.addLine(PbnTools.getStr("tourDown.msg.wgetDone", m_sLocalDir));
      } else {
        m_ow.addLine(PbnTools.getStr("tourDown.msg.alreadyWgetted", m_sLocalDir));
      }
    }

    Deal[] aDeal = readDealsFromDir(m_sLocalDir);
    String sPbnFile = saveDealsAsPbn(aDeal, m_sLocalDir);
    int nUnique = Deal.getUniqueCount(aDeal);
    m_ow.addLine(PbnTools.getStr("tourDown.msg.dealsSaved", sPbnFile,
      aDeal.length, nUnique,
      String.format("%.3f", aDeal.length * 1.0 / nUnique)));

    return true;
  }

  /** Changes the html contents of <code>elem</code> with the contents downloaded
    * from <code>sRemoteLink</code>
    * @param doc Whole document to write, containing elem
    * @param sOutputFile Filename to write the complete html document to
    */
  protected void replaceHtmlAndWrite(Document doc, Element elem, String sRemoteLink, String sOutputFile)
    throws DownloadFailedException
  {
    try {
      URL url = new URL(sRemoteLink);
      InputStream is = url.openStream();
      // http://stackoverflow.com/questions/309424/in-java-how-do-i-read-convert-an-inputstream-to-a-string#5445161
      // thanks to Pavel Repin from stackoverflow for the trick:
      String sNewCont = new Scanner(is).useDelimiter("\\A").next();
      sNewCont = sNewCont.replaceAll("\"images/", "\"");
      elem.html(sNewCont);
      Writer w = new OutputStreamWriter(new FileOutputStream(sOutputFile), doc.outputSettings().charset());
      w.write(doc.html());
      w.close();
    }
    catch (MalformedURLException mue) {
      throw new DownloadFailedException(mue, m_ow, m_bSilent); 
    } catch (IOException ioe) {
      throw new DownloadFailedException(ioe, m_ow, m_bSilent); 
    }
    
  }

  /** Returns the local file in <code>m_sLocalDir</code> resembling
    * <code>sRemoteLink</code>.
    */
  protected String getLocalFile(String sRemoteLink)
  {
    if (sRemoteLink.endsWith("/"))
      throw new IllegalArgumentException("file link: " + sRemoteLink);
    String sRemoteFile = sRemoteLink.replaceFirst("^.*/([^/]+)$", "$1");
    String sLocalFile = m_sLocalDir + "/" + sRemoteFile;
    if (f.isDebugMode()) {
      System.out.println("getLocalFile(" + sRemoteLink + ") = " + sLocalFile);
      System.out.println("m_sLocalDir:" + m_sLocalDir);
      System.out.println("sRemoteFile:" + sRemoteFile);
    }
    return sLocalFile;
  }
  
  /** Changes the local file in m_sLocalDir resembling <code>sRemoteLink</code>.
    * Dynamic content, loaded by browser in <code>onload</code> handler,
    * is pulled from server and inserted into local file
    * @param bWarn Whether to warn when no ajax command found
    */
  protected void ajaxFile(String sRemoteLink, boolean bWarn) throws DownloadFailedException
  {
    String sLocalFile = getLocalFile(sRemoteLink);

    Document docLocal = null;
    try {
      SoupProxy proxy = new SoupProxy();
      docLocal = proxy.getDocumentFromFile(sLocalFile);
    }
    catch (JCException e) {
      throw new DownloadFailedException(e, m_ow, m_bSilent);
    }
    
    if (docLocal.body() == null) {
      throw new DownloadFailedException(
        PbnTools.getStr("error.noBody"), m_ow, true);
    }
    
    // determining name of the file with missing content
    String sAjaxCmd = docLocal.body().attr("onload");
    if (sAjaxCmd.isEmpty()) {
      // nothing to do, no onload handler
      if (bWarn) { m_ow.addLine(PbnTools.getStr("tourDown.msg.noOnLoad", sLocalFile)); }
      return;
    }
    Matcher m = Pattern.compile("^initAjax\\('([^']+)','([^']+)'.*$").matcher(sAjaxCmd);
    if (!m.matches()) {
      if (bWarn) { m_ow.addLine(PbnTools.getStr("tourDown.msg.noInitAjax", sLocalFile)); }
      return;
    }
    String sContentFile = m.group(1);
    
    // replacing the body of m.group(2) element
    Element elemToReplace = getOneTag(docLocal.body(), "div#" + m.group(2), false);
    if (elemToReplace == null) {
      throw new DownloadFailedException(
        PbnTools.getStr("tourDown.error.ajaxFailed", sLocalFile), m_ow, true);
    }
    
    String sRemoteContentLink = sRemoteLink.replaceFirst("/([^/]+)$", "/" + sContentFile); 
    replaceHtmlAndWrite(docLocal, elemToReplace, sRemoteContentLink, sLocalFile); 
  }

  /** Reads contract info from <code>contrElem</code> and stores it
   * in the <code>Deal</code>.
   * @param contrElem Element containing contract data, for example
   *                  <code>4<;img src="H.gif" alt="h" /></code>
   */
  public void processContract(Deal d, Element contrElem)
    throws DownloadFailedException
  {
    if (contrElem.text().length() < 1) {
      throw new DownloadFailedException(PbnTools.getStr(
        "tourDown.error.emptyContract", d.getNumber(), contrElem.html()));
    }
    if ("PASS".equals(contrElem.text())) {
      d.setContractHeight(0);
    } else {
      try {
        int nHeight = Integer.parseInt(contrElem.text().substring(0,1));
        d.setContractHeight(nHeight);
        Element img = getOneTag(contrElem, "img", true);
        if (img == null) {
          throw new DownloadFailedException(PbnTools.getStr(
            "tourDown.error.noImgInContr", d.getNumber(), contrElem.html()));
        }
        d.setContractColor(getImgColorOrNt(img));

        String sDoubles = contrElem.text().substring(1);
        int nDouble = 0;
        for (int i=0; i<2; i++) {
          if (sDoubles.startsWith("×")) {
            nDouble++;
            sDoubles = sDoubles.substring(1);
          } else {
            break;
          }
        }
        if (sDoubles.length() > 0) {
          m_ow.addLine(sDoubles);
          // after reading doubles (if present) the string should be empty
          throw new DownloadFailedException(PbnTools.getStr(
            "tourDown.error.wrongDblContr", d.getNumber(), contrElem.html()));
        }
        d.setContractDouble(nDouble);
      }
      catch (NumberFormatException ne) {
        throw new DownloadFailedException(PbnTools.getStr(
          "tourDown.error.unrecognizedContract",
          String.valueOf(d.getNumber()),
          contrElem.html()));
      }
    }
  }

  // processResult method {{{
  /** Reads the result as a string in form +x, -x, =, and writes it as
   * a number of tricks won by the declarer into <code>d</code>.
   * A contract must be valid.
   * @param sResult Valid values: <code>"", "+n", "-n", "="</code>
   */
  public void processResult(Deal d, String sResult)
    throws DownloadFailedException
  {
    assert(d.getContractHeight() >= 0);
    boolean bBad = false;
    if (sResult == null) {
      sResult = "";
      bBad = true;
    } else if (d.getContractHeight() == 0) {
      // passed out deal
      if (sResult.equals("\u00A0")) {
        // ok, nothing to do
      } else {
        bBad = true;
      }
    } else {
      // regular contract with a given number
      int nTricksDeclared = d.getContractHeight() + 6;
      if (sResult.equals("=")) {
        d.setResult(nTricksDeclared);
      } else {
        if (sResult.startsWith("-") || sResult.startsWith("+")) {
          // read the delta of tricks
          int nDelta = 0;
          try {
            nDelta = Integer.parseInt(sResult.substring(1));
            int nSign = (sResult.charAt(0) == '+') ? 1 : -1;
            d.setResult(nTricksDeclared + nSign * nDelta);
          } catch (NumberFormatException nfe) {
            bBad = true;
          }
        } else {
          bBad = true;
        }
      }
    }
    if (bBad) {
      throw new DownloadFailedException(
        PbnTools.getStr("tourDown.error.invRes", d.getNumber(), sResult));
    }
  } //}}}

  public class VerifyFailedException extends JCException //{{{
  {
    VerifyFailedException(String sMessage, boolean bPrint) {
      super(sMessage);
      if (bPrint) { m_ow.addLine(sMessage); }
    }
    
    VerifyFailedException(String sMessage) { super(sMessage); }
    
    VerifyFailedException(Throwable t)
    {
      super(t);
      m_ow.addLine(t.getMessage());
      if (f.isDebugMode()) {
        t.printStackTrace(new PrintWriter(new OutputWindowWriter(m_ow)));
      }
    }
  } //}}}

  /** A helper function to make error messages code shorter. */
  void throwElemNotFound(String sElem)
    throws DownloadFailedException
  {
    throw new DownloadFailedException(
      PbnTools.getStr("error.elementNotFound", sElem, m_sCurFile),
      m_ow,
      !m_bSilent);
  }
  
}
