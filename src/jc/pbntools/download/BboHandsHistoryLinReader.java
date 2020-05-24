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
import jc.outputwindow.SimplePrinter;
import jc.pbntools.Deal;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class BboHandsHistoryLinReader extends HtmlTourDownloader
{
  private static Logger log = LoggerFactory.getLogger(BboHandsHistoryLinReader.class);
  protected Document m_doc;
  protected boolean m_bSilent = true;
  protected int m_nCurCard = 0; // for gathering game play

  public Deal[] readDeals(String sUrl, boolean bSilent) //{{{
    throws DownloadFailedException
  {
    assert(m_doc != null);
    String sLin = m_doc.text();
    return null;
  } //}}}

  // verify method {{{
  /** Verifies if the <code>sUrl</code> contains valid data in this format */
  public boolean verify(String sUrl, boolean bSilent)
  {
    m_doc = null;
    parseUrl(sUrl);
    try {
      HttpProxy proxy = new BboLoginDecoratorForHtmlProxy(new SoupProxy(), getOutputWindow());
      m_doc = proxy.getDocument(sUrl);
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
      log.debug("", e);
      return false;
    }
  } //}}}

  private void parseUrl(String sUrl) {
    List<NameValuePair> paramsList = URLEncodedUtils.parse(sUrl, Charset.forName("UTF-8"));
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

  }

  @Override
  protected Deal[] readDealsFromDir(String sDir) throws DownloadFailedException {
    return new Deal[0];
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
    return null;
  }

  @Override
  public SimplePrinter getOutputWindow() {
    return m_ow;
  }
}
