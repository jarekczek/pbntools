/* *****************************************************************************

    jedit options: :folding=explicit:tabSize=2:noTabs=true:collapseFolds=1:

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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jc.f;
import jc.JCException;
import jc.outputwindow.OutputWindow;
import jc.outputwindow.SimplePrinter;
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
 *
 * This class is used for tournaments, which require wget download.
 *
 *
 */

abstract public class HtmlTourDownloader
  implements DealReader
{
  public static final String HTML_SPACE_REG = "[ \u00A0]";
  public static final String HTML_SPACE = " \u00A0";
  
  protected String m_sLink;
  protected URL m_remoteUrl;
  protected URL m_localUrl;
  public String m_sTitle;
  /** Not the whole path, but only the name of one directory, to which
    * the tournament should be saved */
  public String m_sDirName;
  /** The local directory to which output files will be saved */
  public String m_sLocalDir;
  /** The local source directory, from which files will be read.
    * If the tournament is downloaded from net, it will be the same as
    * <code>m_sLocalDir</code>. In case of downloading from a file link,
    * it would be a different directory, because the files are not copied
    * to output directory. */
  public String m_sSourceDir;
  protected SimplePrinter m_ow;
  protected Document m_doc;
  public int m_cDeals;
  /** Current file, to show in error messages */
  protected String m_sCurFile;
  /** Whether to show error messages. */ 
  protected boolean m_bSilent;
  /** Errors for the current hand. The same error set is reused for
    * all the results for a given hand. Strings in the set must be
    * interned. */
  protected Set<String> m_setErr = new HashSet<String>();

  // abstract methods {{{  
  @Override
  abstract public void setOutputWindow(SimplePrinter ow);
  
  abstract public String getName();
  /**
   * Verifies whether link points to a valid data in this format.
   * Sets the following members:
   * <ul>
   * <li>m_doc
   * <li>m_sTitle
   * <li>m_sDirName
   * <li>m_sLink (setLink)</li>
   * <li>m_remoteUrl
   * <li>m_cDeals
   * </ul>
   * Prints the title and dirname.
   */
  abstract public boolean verify(String sLink, boolean bSilent);

  abstract protected void wget() throws DownloadFailedException;
  
  abstract protected Deal[] readDealsFromDir(String sDir)
    throws DownloadFailedException;

  /** Reads deals from the given url. */
  abstract public Deal[] readDeals(String sUrl, boolean bSilent)
    throws DownloadFailedException;
  //}}} abstract

  public String toString() { return getName(); }

  protected String getMsg(String name, Object... ao) {
    return PbnTools.getStr("tourDown.msg." + name, ao);
  }
  
  /** Not called from anywhere yet. Default constructor is sufficient. */
  protected void clear() {
    m_sLink = "";
    m_remoteUrl = null; m_localUrl = null;
    m_sTitle = ""; m_sDirName = ""; m_sLocalDir = ""; m_sSourceDir = "";
    m_ow = null;
    m_doc = null;
    m_cDeals = 0;
    m_sCurFile = "";
    m_bSilent = false;
  }
  
  public void setLink(String sLink) {
    m_sLink = sLink;
  }

  // print methods {{{
  /** Allows a reader/downloader to output information during processing. */
  protected void println(String sLine)
  {
    if (m_ow != null)
      m_ow.addLine(sLine);
  }

  /** Allows a reader/downloader to output information during processing. */
  protected void print(String sText)
  {
    if (m_ow != null)
      m_ow.addText(sText);
  }
  //}}}

  /** Returns the url which may be used as base url for links from inside
    * the given url. That is <code>http://aaa.com/start/</code> for both
    * <code>http://aaa.com/start/page.htm</code>
    * and <code>http://aaa.com/start</code>. */
  public static String getBaseUrl(String sUrl)
  {
    return SoupProxy.getBaseUrl(sUrl);
  }

  //{{{ JSoup helper methods
  
  /** Selects <code>sTag</code>.
   * @throws DownloadFailedException if tag not found.
   */
  public Elements getElems(Element parent, String selector, boolean bSilent)
    throws DownloadFailedException
  {
    Elements elems = parent.select(selector);
    if (elems.size() == 0) {
      throw new DownloadFailedException(PbnTools.getStr("error.tagNotFound", selector), m_ow, !bSilent);
    }
    return elems;
  }
  
  // getOneTag methods {{{

  /** Selects <code>sTag</code> but require exactly one match.
   * @throws DownloadFailedException if not found.
   */
  protected Element getOneTagEx(Element parent, String sTag, boolean bSilent)
    throws DownloadFailedException
  {
    Elements elems = parent.select(sTag);
    if (elems.size() == 0) {
      throw new DownloadFailedException(
        PbnTools.getStr("error.tagNotFound", sTag), m_ow, !bSilent);
    }
    if (elems.size() > 1) {
      throw new DownloadFailedException(
        PbnTools.getStr("error.onlyOneTagAllowed", sTag), m_ow, !bSilent);
    }
    return elems.get(0);
  }

  /** Selects <code>sTag</code> but require exactly one match.
   * @return <code>null</code> if tag not found.
   */
  protected Element getOneTag(Element parent, String sTag, boolean bSilent)
  {
    try {
      Element e = getOneTagEx(parent, sTag, bSilent);
      return e;
    }
    catch (DownloadFailedException dfe) {
      return null;
    }
  }
  
  // }}} getOneTag
  
  /** Returns first <code>sTag</code>, throws exception if not tag
    * found. */
  protected Element getFirstTag(Element parent, String sTag, boolean bSilent)
    throws DownloadFailedException
  {
    Elements elems = parent.select(sTag);
    if (elems.size()==0) {
      throw new DownloadFailedException(
        PbnTools.getStr("error.tagNotFound", sTag), m_ow, !bSilent);
    }
    return elems.get(0);
  }
  
  /** Returns n-th <code>sTag</code>, throws exception if not found.
    * @param n Starting from 1. */
  protected Element getNthTag(Element parent, String sTag, int n,
                              boolean bSilent)
    throws DownloadFailedException
  {
    Elements elems = parent.select(sTag);
    if (elems.size() < n) {
      throw new DownloadFailedException(
        PbnTools.getStr("error.tooFewTags", sTag, n, elems.size()),
                        m_ow, !bSilent);
    }
    return elems.get(n - 1);
  }

  /** Checks if text of first <code>sTag</code> starts with the given text.
    * If not, throws exception.
    * @throws DownloadFailedException */
  protected void firstTagStartsWith(Element parent, String sTag,
    String sStart, boolean bSilent)
    throws DownloadFailedException
  {
    Element first = getFirstTag(parent, sTag, bSilent);
    if (!first.text().startsWith(sStart))
      throw new DownloadFailedException(
        PbnTools.getStr("error.tagStarts", sTag, sStart, first.text()),
        m_ow, !bSilent);
  }
  
  /** Checks if text of first <code>sTag</code> starts with the given text.
    * If not, throws exception.
    * @throws DownloadFailedException */
  protected void firstTagMatches(Element parent, String sTag,
    String sMatch, boolean bSilent)
    throws DownloadFailedException
  {
    Element first = getFirstTag(parent, sTag, bSilent);
    if (!first.text().matches(sMatch))
      throw new DownloadFailedException(
        PbnTools.getStr("error.tagMatches", sTag, sMatch, first.text()),
        m_ow, !bSilent);
  }
  // }}} JSoup methods
  
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
      if (!bSilent || f.isDebugMode()) {
        println(PbnTools.getStr("error.tagNotFound", "<meta name=\"GENERATOR\" content="));
        return false;
      }
    }
    if (!sFound.equals(sExpValue)) {
      if (!bSilent || f.isDebugMode()) {
        println(PbnTools.getStr("error.invalidTagValue", "<meta name=\"GENERATOR\" content=",
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
      println(PbnTools.getStr("error.invalidTagValue", sTag, sTextReg, elem.text()));
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
    println(m_sTitle);
    String sPath = m_remoteUrl.getPath();
    while (sPath.endsWith("/")) sPath = sPath.replaceFirst("/$", "");
    String sLast = sPath.replaceFirst("^.*/", "");
    if (sLast.indexOf('.')>=0) {
      m_sDirName = sPath.replaceFirst("^.*/([^/]+)/[^/]*$", "$1");
    } else {
      m_sDirName = sLast;
    }
    assert(!m_sDirName.isEmpty());
    println(m_sDirName);
  }

  protected void setDirNameFromTitle()
  {
    // leaving only characters accepted by uri syntax, rfc 3986 2.3
    m_sDirName = m_sTitle.replaceAll("[^-_.~a-zA-Z0-9]", " ");
    m_sDirName = m_sDirName.replaceAll(" +", " ");
    m_sDirName = m_sDirName.trim();
    m_sDirName = m_sDirName.replaceAll(" ", "_");
  }
  
  /** Sets m_sLocalDir member, based on m_sDirName and current
    * configuration. */
  protected void setLocalDir()
  {
    File fWork = new File(PbnTools.getWorkDir(false));
    File fDir = new File(fWork, m_sDirName);

    m_sLocalDir = fDir.getAbsolutePath();
    try { m_sLocalDir = fDir.getCanonicalPath(); }
    catch (Exception e) {}
  }

  protected void createLocalDir() throws DownloadFailedException
  {
    File fDir = new File(m_sLocalDir);
    if (fDir.exists())
      return;
    if (!(fDir.mkdir())) {
      throw new DownloadFailedException(
        PbnTools.getStr(
          "error.unableToCreateDir", m_sLocalDir), m_ow, true);
    }
  }
  
  /** Checks whether the tournament is already downloaded. */
  protected boolean isDownloaded()
  {
    File fDir = new File(m_sLocalDir);
    return fDir.exists();
  }

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
      throw new DownloadFailedException(ioe, m_ow, !m_bSilent);
    }
    return sPath;
  }

  /** Adds errors to error set for the current hand. New errors
    * are reported. */
  public void reportErrors(String asErr[])
  {
    for (String sErr: asErr) {
      sErr = sErr.intern();
      if (!m_setErr.contains(sErr)) {
        println(sErr);
        m_setErr.add(sErr);
      }
    }
  }

  /** Resets error set, when a new hand is being processed. */
  public void resetErrors()
  {
    m_setErr.clear();
  }
  
  // delayForUrl method {{{
  /** Get the delay [s] that should be applied to given url (string) */
  protected int delayForUrl(String sUrl)
  {
    int nDelay = PbnTools.m_nDelay;
    if (sUrl.toString().indexOf("localhost") >= 0)
      nDelay = 0;
    return nDelay;
  } //}}}

  // wgetLinks method {{{
  /** Downloads files contained in the <code>sLinksFile</code>
   *  into <code>m_sLocalDir</code>
   */
  protected void wgetLinks(String sLinksFile)
    throws DownloadFailedException
  {
    int nDelay = delayForUrl(m_remoteUrl.toString());
    String sCmdLine = "wget -p -k -nH -nd -nc -E -e "
      + "robots=off --restrict-file-names=windows";
    if (System.getProperty("jc.soupproxy.useragent") != null)
      sCmdLine += " --user-agent="
                  + System.getProperty("jc.soupproxy.useragent");
    if (nDelay > 0) {
      sCmdLine += " -w " + nDelay;
      f.sleepNoThrow(1000 * nDelay);
    }
    ArrayList<String> asCmdLine = new ArrayList<String>(
      Arrays.asList(sCmdLine.split(" ")));
    asCmdLine.add("--directory-prefix=" + m_sLocalDir);
    addSessionCookies(asCmdLine, m_remoteUrl, new File(sLinksFile).getParent());
    asCmdLine.add("--input-file=" + sLinksFile);
    asCmdLine.add("--no-check-certificate");

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
  } //}}}

  private void addSessionCookies(ArrayList<String> asCmdLine, URL url,
                                 String cookieFileDir)
          throws DownloadFailedException
  {
    try {
      SoupProxy proxy = new SoupProxy();
      Map<String, String> cookies = proxy.getCookies(m_remoteUrl);
      if (cookies.isEmpty())
        return;
      File cookieFile = new File(cookieFileDir, "session-cookie.txt");
      File cookieFileOut = new File(cookieFileDir, "session-cookie-out.txt");
      FileOutputStream out = new FileOutputStream(cookieFile);
      String server = url.toString()
        .replaceFirst("^[^/]+//", "")
        .replaceFirst("/.*", "");
      for (Map.Entry<String, String> e: cookies.entrySet()) {
        String cookie = server + "\tFALSE\t/\tFALSE\t0\t"
          + e.getKey() + "\t" + e.getValue() + "\n";
        out.write(cookie.getBytes(Charset.forName("UTF-8")));
      }
      out.close();
      asCmdLine.add("--load-cookies=" + cookieFile.getAbsolutePath());
      asCmdLine.add("--keep-session-cookies");
      asCmdLine.add("--save-cookies=" + cookieFileOut.getAbsolutePath());
    } catch (IOException e) {
      throw new DownloadFailedException(e, m_ow, !m_bSilent);
    }
  }

  /** performs 2 operations: downloading (if required) from internet and
    * converting (locally) to pbns */
  public boolean fullDownload(boolean bSilent)
    throws DownloadFailedException
  {
    m_bSilent = bSilent;
    setLocalDir();
    if (m_remoteUrl.getProtocol().equals("file")) {
      m_localUrl = m_remoteUrl;
      m_sSourceDir = getBaseUrl(m_localUrl.getFile());

      if (m_sSourceDir.endsWith("/"))
        m_sSourceDir = m_sSourceDir.substring(0, m_sSourceDir.length() - 1);
      m_sSourceDir = new File(m_sSourceDir).getAbsolutePath();
      println(PbnTools.getStr("tourDown.msg.localLink", m_sLocalDir));
    } else {
      m_sSourceDir = m_sLocalDir;
      // constructing local url after isDownloaded set m_sLocalDir
      String sFileName = m_remoteUrl.toString().replaceFirst("^.*/", "");
      sFileName = sFileName.replace("?", "@");
      if (sFileName.indexOf('.') < 0) {
        sFileName = "index.html";
      }
      if (!sFileName.endsWith("html")) {
        sFileName = sFileName + ".html";
      }
      try {
        m_localUrl = new File(m_sLocalDir, sFileName).toURI().toURL();
      } catch (Exception e) {
        throw new DownloadFailedException(e, m_ow, !m_bSilent);
      }
      println("local url: " + m_localUrl);
      
      if (!isDownloaded()) {
        println(PbnTools.getStr("tourDown.msg.willWget", m_sLocalDir));
        wget();
        if (!Thread.currentThread().isInterrupted())
          println(PbnTools.getStr("tourDown.msg.wgetDone", m_sLocalDir));
      } else {
        println(PbnTools.getStr("tourDown.msg.alreadyWgetted", m_sLocalDir));
      }
    }

    Deal[] aDeal = readDealsFromDir(m_sSourceDir);
    createLocalDir();
    String sPbnFile = saveDealsAsPbn(aDeal, m_sLocalDir);
    int nUnique = Deal.getUniqueCount(aDeal);
    String sAver = "0";
    if (nUnique != 0)
      sAver = String.format("%.3f", aDeal.length * 1.0 / nUnique);
    println(PbnTools.getStr("tourDown.msg.dealsSaved", sPbnFile,
      aDeal.length, nUnique, sAver));

    postProcess();

    return true;
  }

  protected void postProcess() {}

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
      URLConnection con = url.openConnection();
      con.setRequestProperty("User-Agent",
        System.getProperty("jc.soupproxy.useragent"));
      InputStream is = con.getInputStream();
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
      throw new DownloadFailedException(mue, m_ow, !m_bSilent); 
    } catch (IOException ioe) {
      throw new DownloadFailedException(ioe, m_ow, !m_bSilent); 
    }
    
  }

  /** Returns the local file in <code>m_sSourceDir</code> resembling
    * <code>sRemoteLink</code>.
    */
  protected String getLocalFile(String sRemoteLink)
  {
    assert(sRemoteLink != null && sRemoteLink.length() > 0);
    if (sRemoteLink.endsWith("/"))
      throw new IllegalArgumentException("file link: " + sRemoteLink);
    String sRemoteFile = sRemoteLink.replaceFirst("^.*[/\\\\]([^/\\\\]+)$", "$1");
    String sLocalFile = m_sSourceDir + "/" + sRemoteFile;
    if (f.isDebugMode()) {
      System.out.println("getLocalFile(" + sRemoteLink + ") = " + sLocalFile);
      System.out.println("m_sSourceDir:" + m_sSourceDir);
      System.out.println("sRemoteFile:" + sRemoteFile);
    }
    // local files come from wget with -k switch (add html extension)
    // so we must add this extension if absent
    if (!sLocalFile.matches(".*\\.htm(l?)"))
      sLocalFile += ".html";
    // wget is run with --restrict-file-names=windows and it is
    // documented that : -> +, ? -> @
    sLocalFile = sLocalFile.replaceAll("\\?", "@");
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
    int nDelay = delayForUrl(sRemoteLink);

    f.sleepNoThrow(1000*nDelay);
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
        PbnTools.getStr("error.noBody"), m_ow, true);
    }
    
    // determining name of the file with missing content
    String sAjaxCmd = docLocal.body().attr("onload");
    if (sAjaxCmd.isEmpty()) {
      // nothing to do, no onload handler
      if (bWarn) { println(PbnTools.getStr("tourDown.msg.noOnLoad", sLocalFile)); }
      return;
    }
    Matcher m = Pattern.compile("^initAjax\\('([^']+)','([^']+)'.*$").matcher(sAjaxCmd);
    if (!m.matches()) {
      if (bWarn) { println(PbnTools.getStr("tourDown.msg.noInitAjax", sLocalFile)); }
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
        int nDoublePos = 1; // starting position for x marks (double)
        int nHeight = Integer.parseInt(contrElem.text().substring(0,1));
        d.setContractHeight(nHeight);
        if (contrElem.text().substring(1).startsWith("NT")) {
          nDoublePos = 3;
          d.setContractColor(0);
        } else if (contrElem.text().substring(1).startsWith("N")) {
          nDoublePos = 2;
          d.setContractColor(0);
        }
        else {
          // try to match next char as color (bbo style)
          char chColor = 0;
          if (contrElem.text().length() > 1)
            chColor = contrElem.text().charAt(nDoublePos);
          String sBboColors = "\u2660\u2665\u2666\u2663"; // S H D C
          int iFound = sBboColors.indexOf(chColor);
          if (iFound >= 0) {
            d.setContractColor(iFound + Card.SPADE);
            nDoublePos++;
          } else {
            Element img = getOneTag(contrElem, "img", true);
            if (img == null) {
              throw new DownloadFailedException(PbnTools.getStr(
                "tourDown.error.noImgInContr", d.getNumber(), contrElem.html()));
            }
            d.setContractColor(getImgColorOrNt(img));
          }
        }

        String sDoubles = contrElem.text().substring(nDoublePos);
        int nDouble = 0;
        for (int i=0; i<2; i++) {
          if (sDoubles.startsWith("×") || sDoubles.startsWith("x")) {
            nDouble++;
            sDoubles = sDoubles.substring(1);
          } else {
            break;
          }
        }
        if (sDoubles.length() > 0) {
          println(sDoubles);
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

  /** A helper function to make error messages code shorter. */
  void throwElemNotFound(String sElem)
    throws DownloadFailedException
  {
    throw new DownloadFailedException(
      PbnTools.getStr("error.elementNotFound", sElem, m_sCurFile),
      m_ow,
      !m_bSilent);
  }

  // JFR methods {{{

  // setCardsJfr method {{{
  /** Deals cards presented by html <code>hand</code> to
    * <code>nPerson</code>. Saves it in <code>deal</code>.
    * This code works for JFR formats: Kops, Pary */
  protected void setCardsJfr(Deal deal, int nPerson, Element hand)
    throws DownloadFailedException
  {
    String asText[] = SoupProxy.splitElemText(hand);
    for (Element img : hand.getElementsByTag("img")) {
      int nColor = getImgColor(img);
      String sCards = asText[img.elementSiblingIndex() + 1];
      // we need all symbols to be 1 char long
      sCards = sCards.replaceAll("10", "T");
      for (int i = 0; i < sCards.length(); i++) {
        char chCard = sCards.charAt(i);
        // skip spaces
        if (HTML_SPACE.indexOf(chCard) >= 0)
          continue;
        Card card = new Card();
        card.setColor(nColor);
        card.setRankCh(chCard);
        deal.setCard(card, nPerson);
      }
    }
    deal.fillHands();
  } //}}}

  // setScoringJfr method {{{
  /** Sets scoring in <code>deal</code> based on
      coding <code>sScoring</code>provided by JFR formats.
      Returns <code>true</code> if scoring was recognized. */
  protected boolean setScoringJfr(Deal deal, String sScoring)
  {
    if ("%".equals(sScoring)) {
      deal.setScoring("MP");
    } else if ("PUNKTY".equals(sScoring)
               || "IMP".equals(sScoring)) {
      deal.setScoring("IMP");
    } else
      return false;
    return true;
  } //}}}

  //}}} JFR methods

}
