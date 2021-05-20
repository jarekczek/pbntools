/* *****************************************************************************

    jedit options: :folding=explicit:tabSize=2:noTabs=true:
    
    Copyright (C) 2020 Jaroslaw Czekalski - jarekczek@poczta.onet.pl

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

import jc.HttpProxy;
import jc.JCException;
import jc.SoupProxy;
import jc.f;
import jc.outputwindow.SimplePrinter;
import jc.pbntools.Deal;
import jc.pbntools.PbnTools;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class BboHandsHistoryLinReader extends BboCommonDownloader
{
  private static Logger log = LoggerFactory.getLogger(BboHandsHistoryLinReader.class);
  protected int m_nCurCard = 0; // for gathering game play

  // verify method {{{
  /** Verifies if the <code>sUrl</code> contains valid data in this format */
  @Override
  public boolean verify(String sUrl, boolean bSilent)
  {
    sUrl = addOffset(sUrl);
    setLink(sUrl);
    m_doc = null;
    parseUrl(sUrl);
    try {
      HttpProxy proxy = new BboLoginDecoratorForHtmlProxy(new SoupProxy(), getOutputWindow());
      m_doc = proxy.getDocument(sUrl);
      m_remoteUrl = proxy.getUrl();
      Elements aElems = getElems(m_doc, "a:matches(Lin)", bSilent);
      if (aElems.size() == 0) {
        log.debug("No lin links found in {}", sUrl);
        return false;
      }
      // Examine only the first link, not to overload the server.
      Element firstAElem = aElems.get(0);
      String linUrl = SoupProxy.absUrl(firstAElem, "href");
      LinReader singleLinReader = new LinReader();
      singleLinReader.setOutputWindow(getOutputWindow());
      delayBetweenDownloads(linUrl);
      Document linDoc = proxy.getDocument(linUrl);
      if (!singleLinReader.verify(linUrl, bSilent)) {
        log.info("Returning false from verify for {}, because linUrl {} returned false.");
        return false;
      }
//        Deal d = singleLinReader.readLin(linDoc.text(), bSilent)[0];
//        d.setId(aElems.attr("href").replaceFirst("^[^0-9]*([0-9]+)[^0-9]*.*$", "$1"));
//        deals.add(d);
//      }
      log.info("verification positive for {}", sUrl);
      m_cDeals = aElems.size();
      return true;
    }
    catch (JCException e) {
      if (!bSilent) {
        m_ow.addLine(e.toString());
      }
      log.debug("", e);
      return false;
    }
  } //}}}

  private void delayBetweenDownloads(String sUrl) {
    int millis = 1000 * delayForUrl(sUrl);
    log.debug("Waiting between downloads, " + millis + " ms.");
    f.sleepNoThrow(millis);
  }

  private void parseUrl(String sUrl) {
    List<NameValuePair> paramsList = null;
    try {
      URL url = new URL(sUrl);
      paramsList = URLEncodedUtils.parse(url.toURI(), Charset.forName("UTF-8"));
    } catch (URISyntaxException e) {
      return;
    } catch (MalformedURLException e) {
      return;
    }
    String username = getFromNameValueList(paramsList, "username", "unknown_user");
    String from = epochToTimeString(getFromNameValueList(paramsList, "start_time", null));
    String until = epochToTimeString(getFromNameValueList(paramsList, "end_time", null));
    StringBuilder title = new StringBuilder();
    title.append("Hands of " + username);
    if (from != null) {
      title.append(" from " + from);
    }
    if (until != null) {
      title.append(" until " + until);
    }
    m_sTitle = title.toString();
    m_sDirName = m_sTitle.replaceAll(" ", "_");
  }

  private String epochToTimeString(String epochString) {
    if (epochString == null) {
      return null;
    }
    Date date = new Date(Long.parseLong(epochString) * 1000L);
    return new SimpleDateFormat("yyyyMMdd_HHmm").format(date);
  }

  private String getFromNameValueList(List<NameValuePair> paramsList, String name, String defaultValue) {
    String value = defaultValue;
    for (NameValuePair nameValuePair : paramsList) {
      if (name.equals(nameValuePair.getName())) {
        value = nameValuePair.getValue();
      }
    }
    return value;
  }

  @Override
  protected void wget() throws DownloadFailedException {
    String sLinksFile = createIndexFile();
    wgetLinks(sLinksFile);
    downloadLins(getLocalFile(m_sLink));
  }

  private String createIndexFile() throws DownloadFailedException {
    String sLinksFile = new File(m_sLocalDir, "links.txt").getAbsolutePath();
    try {
      if (!(new File(m_sLocalDir).mkdir())) {
        throw new DownloadFailedException(PbnTools.getStr("error.unableToCreateDir", m_sLocalDir), m_ow, true);
      }
      BufferedWriter fw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(sLinksFile), "ISO-8859-1"));
      fw.write(m_sLink);
      fw.newLine();
      fw.close();
    } catch (java.io.IOException ioe) {
      throw new DownloadFailedException(ioe, m_ow, !m_bSilent);
    }
    return sLinksFile;
  }

  @Override
  protected Deal[] readDealsFromDir(String sDir) throws DownloadFailedException {
    return readDeals(this.m_localUrl.toString(), m_bSilent);
  }

  // setOutputWindow method {{{
  /** Sets the window to which output messages will be directed */
  @Override
  public void setOutputWindow(SimplePrinter sp)
  {
    m_ow = sp;
  } //}}}

  @Override
  public String getName() {
    return "Bbo Hand records";
  }

  @Override
  public SimplePrinter getOutputWindow() {
    return m_ow;
  }
}
