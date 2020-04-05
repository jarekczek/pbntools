/* *****************************************************************************

    jedit options: :folding=explicit:tabSize=2:indentSize=2:noTabs=true:

    Copyright (C) 2011-13 Jaroslaw Czekalski - jarekczek@poczta.onet.pl

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

package jc;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <code>SoupProxy</code> class retrieves an html page through
 * <code>org.jsoup.Jsoup</code> library. Latest pages are cached.
 * Allows supplying http or file urls.
 * <p>
 * Cache is static, so it's correct to create a new proxy instance every
 * time it is needed.
 */

public class SoupProxy
{
  protected static LinkedList<CacheElement> m_cache;
  protected final static int m_nCacheSize = 10;
  public static final int NO_FLAGS = 0;
  public static final int NO_CACHE = 1;
  private File jsoupLogFolder;
  protected Logger log;
  private static Map<String, Map<String, String>> serverCookies;
  
  protected URL m_url;
  
  static {
    m_cache = new LinkedList<CacheElement>();
    serverCookies = new HashMap<String, Map<String, String>>();
  }

  public SoupProxy()
  {
    m_url = null;
    log = LoggerFactory.getLogger(this.getClass().toString());
    log.debug("SoupProxy constructor.");

    if (System.getProperty("jsoup.log.folder") != null) {
      jsoupLogFolder = new File(System.getProperty("jsoup.log.folder"));
      if (!jsoupLogFolder.exists()) {
        log.warn("Turning off jsoup.log.folder, because "
          + jsoupLogFolder + " does not exist.");
        jsoupLogFolder = null;
      }
    }
  }
  
  public URL getUrl() {
    return m_url;
  }
  
  public Document getDocumentFromFile(String sFile)
    throws SoupProxy.Exception
  {
    log.debug("getDocumentFromFile " + sFile);
    Document doc;
    try {
      doc = Jsoup.parse(new File(sFile), null);
    }
    catch (java.io.IOException e) {
      throw new Exception(e);
    }
    return doc;
  }

  public Document getDocumentFromHttp(URL url)
    throws SoupProxy.Exception
  {
    log.debug("getDocumentFromHttp " + url);
    Document doc = null;
    try {
      Connection con = Jsoup.connect(""+url);
      con.userAgent(
        System.getProperty("jc.soupproxy.useragent", "JSoup"));
      con.ignoreContentType(true);
      con.cookies(getCookies(url));
      doc = con.get();
      setCookies(url, con.response().cookies());
      if (jsoupLogFolder != null)
        saveDocumentToTempFile(doc, jsoupLogFolder);
    }
    catch (java.lang.Exception e) {
      throw new SoupProxy.Exception(e);
    }
    return doc;
  }

  public Document post(URL url, Map<String, String> data)
    throws SoupProxy.Exception
  {
    log.debug("post " + url);
    Document doc = null;
    try {
      Connection con = Jsoup.connect(""+url);
      con.userAgent(
        System.getProperty("jc.soupproxy.useragent", "JSoup"));
      con.ignoreContentType(true);
      con.cookies(getCookies(url));
      con.data(data);
      doc = con.post();
      setCookies(url, con.response().cookies());
      if (jsoupLogFolder != null)
        saveDocumentToTempFile(doc, jsoupLogFolder);
    }
    catch (java.lang.Exception e) {
      throw new SoupProxy.Exception(e);
    }
    return doc;
  }

  private void setCookies(URL url, Map<String, String> cookies) {
    log.debug("setCookies " + url + ", " + mapToString(cookies));
    String server = url.getHost();
    Map<String, String> ourCookies = getCookies(url);
    for(Map.Entry<String, String> entry: cookies.entrySet())
      ourCookies.put(entry.getKey(), entry.getValue());
    serverCookies.put(server, ourCookies);
  }

  private String mapToString(Map<String, String> cookies) {
    return cookies.entrySet().stream()
      .map(e -> e.getKey() + "=" + e.getValue())
      .collect(Collectors.joining(","));
  }

  public Map<String, String> getCookies(URL url) {
    String server = url.getHost();
    Map<String, String> cookieMap;
    if (serverCookies.containsKey(server)) {
      cookieMap = serverCookies.get(server);
    } else {
      cookieMap = new HashMap<String, String>();
    }
    log.debug("getCookies " + url + ", " + mapToString(cookieMap));
    return cookieMap;
  }

  public String getCookie(URL url, String key) {
    log.debug("getCookie " + url + ", " + key);
    Map<String, String> ourCookies = getCookies(url);
    return ourCookies.get(key);
  }

  private void saveDocumentToTempFile(Document doc, File dir) {
    log.debug("saveDocumentToTempFile");
    try {
      File tempFile = File.createTempFile("jsoup_", ".html", dir);
      FileOutputStream ostr = new FileOutputStream(tempFile);
      ostr.write(doc.toString().getBytes(Charset.forName("iso-8859-1")));
      ostr.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public Document getDocument(String sUrl)
    throws SoupProxy.Exception
  {
    return getDocument(sUrl, NO_FLAGS);
  }
  
  public Document getDocument(String sUrl, int nFlags)
    throws SoupProxy.Exception
  {
    log.debug("getDocument " + sUrl + ", " + nFlags);
    Document doc;
    if ((nFlags & NO_CACHE) == 0) {
      // check cache
      CacheElement elem = cacheGetElem(sUrl);
      if (elem != null) {
        m_url = elem.url;
        doc = elem.doc;
        return doc;
      }
    }
    
    try {
      m_url = new URL(sUrl);
    }
    catch (java.net.MalformedURLException eUrl) {
      // Not an url? Let's try to get the file
      try {
        m_url = new URL("file:" + sUrl);
      }
      catch (java.net.MalformedURLException eUrl2) {
        throw new SoupProxy.Exception(eUrl);
      }
    }
      
    if (m_url.getProtocol().equals("file")) {
      doc = getDocumentFromFile(m_url.getFile());
    } else {
      doc = getDocumentFromHttp(m_url);
    }

    if ((nFlags & NO_CACHE) == 0) {
      if (doc != null) { cachePut(sUrl, m_url, doc); }
    }

    return doc;
  }
  
  public static String htmlToText(String sHtml)
  {
    Document doc = Jsoup.parse(sHtml);
    return doc.text();
  }
  
  /** Retrieves the part of text directly after the child node. Due to jsoup
    * limitations it works only for unique child items
    * (<code>iChilde1</code>). It's safer to use {@link #splitElemText}.
    */
  public static String getTextAfterElem(Element elem)
  {
    return getTextBetweenChildren(
      elem.parent(),
      elem.elementSiblingIndex(),
      elem.elementSiblingIndex() + 1
      );
  }
  
  /** Retrieves the part of text between 2 child nodes. Due to jsoup
    * limitations it works only for unique child items
    * (<code>iChilde1</code>). It's safer to use {@link #splitElemText}.
    */
  public static String getTextBetweenChildren(Element parent,
      int iChild1, int iChild2)
  {
    String sPar = parent.html();
    Element e1 = parent.child(iChild1);
    Element e2 = null;
    if (iChild2 < parent.children().size()) {
      e2 = parent.child(iChild2);
    }
    int iStart = sPar.indexOf(e1.outerHtml());
    if (iStart < 0) { throw new RuntimeException("tag " + e1.outerHtml() +
                         " not contained in html: " + sPar); }
    iStart += e1.outerHtml().length();
    int iEnd = -1;
    if (e2 != null) {
      iEnd = sPar.indexOf(e2.outerHtml(), iStart);
    }
    if (iEnd < 0) { iEnd = sPar.length(); }
    return htmlToText(sPar.substring(iStart, iEnd));
  }
  
  /** Splits the text of the element <code>elem</code> by the children
    * tags.
    * @return An array of size <code>c+1</code>, where <copde>c</code>
    * is the number of child elements.
    * <p>Text after <code>n</code>th element is found in <code>[n+1]</code>.
    */
  public static String[] splitElemText(Element elem)
  {
    int c = elem.children().size();
    String as[] = new String[c + 1];
    String sAll = elem.html();
    int iBeg = 0;
    int iChild = 0;
    for (Element ch : elem.children()) {
      String sChild = ch.outerHtml();
      int iEnd = sAll.indexOf(sChild, iBeg);
      if (iEnd < 0) { throw new RuntimeException("Tag " + sChild
                      +" not found in its parent: " + sAll);
      }
      as[iChild] = htmlToText(sAll.substring(iBeg, iEnd));
      iBeg = iEnd + sChild.length();
      iChild += 1;
    }
    as[iChild] = htmlToText(sAll.substring(iBeg));
    assert(iChild == c);
    return as;
  }

  // getBaseUrl method {{{  
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
  } //}}}

  // absUrl method {{{
  /*
   * A wrapper for <code>Node.absUrl</code> which fails for local
   * files (uris without a protocol).
   */
  public static String absUrl(Node node, String sAttrName)
  {
    String sRelativeLink = node.attr(sAttrName);
    if (sRelativeLink.length() == 0)
      return "";
    String sLink = node.absUrl(sAttrName);
    assert(sLink != null);
    if (sLink.length() == 0) {
      String sBaseUri = node.baseUri();
      
      // In case of a file this uri is not base, but it's a complete
      // filename.
      Matcher m = Pattern.compile("(.*)([\\\\/])[^\\\\/]+\\.html?")
        .matcher(sBaseUri);
      if (m.matches())
        sBaseUri = m.group(1) + m.group(2);
      
      sLink = sBaseUri + sRelativeLink;
    }
    return sLink;
  } //}}}
  
  public static class Exception extends JCException
  {
    Exception(Throwable t) { super(t); }
  }
  
  //{{{ cache support
  protected static class CacheElement
  {
    String sUrl;
    Document doc;
    URL url;
    
    public CacheElement(String sUrl, URL url, Document doc) {
      this.sUrl = sUrl;
      this.doc = doc;
      this.url = url;
    }
  }

  protected static CacheElement cacheGetElem(String sUrl)
  {
    for (CacheElement elem : m_cache) {
      if (elem.sUrl.equals(sUrl)) {
        return elem;
      }
    }
    return null;
  }
  
  protected static Document cacheGet(String sUrl)
  {
    CacheElement elem = cacheGetElem(sUrl);
    return elem==null ? null : elem.doc;
  }
  
  protected static void cachePut(String sUrl, URL url, Document doc)
  {
    // remove from cache if already in, to make it go to the end of the list
    CacheElement elem = cacheGetElem(sUrl);
    if (elem != null) {
      m_cache.remove(elem);
    }

    // remove oldest if size exceeded
    if (m_cache.size() >= m_nCacheSize) {
      m_cache.removeFirst();
    }
    
    if (elem == null) {
      elem = new CacheElement(sUrl, url, doc);
    }
    m_cache.add(elem); 
  }
  //}}}
  
}

