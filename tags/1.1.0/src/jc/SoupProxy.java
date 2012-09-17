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

package jc;

import java.net.URL;
import java.io.File;
import java.util.LinkedList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** <code>SoupProxy</code> class retrieves an html page through
 *  <code>org.jsoup.Jsoup</code> library. Latest pages are cached.
 *  Allows supplying http or file urls.
 */

public class SoupProxy
{
  protected static LinkedList<CacheElement> m_cache;
  protected final static int m_nCacheSize = 10;
  public static final int NO_FLAGS = 0;
  public static final int NO_CACHE = 1;
  
  protected URL m_url;
  
  static {
    m_cache = new LinkedList<CacheElement>();
  }

  public SoupProxy()
  {
    m_url = null;
  }
  
  public URL getUrl() {
    return m_url;
  }
  
  public Document getDocumentFromFile(String sFile)
    throws SoupProxy.Exception
  {
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
    Document doc = null;
    try {
      doc = Jsoup.connect(""+url).get();
    }
    catch (java.lang.Exception e) {
      throw new SoupProxy.Exception(e);
    }
    return doc;
  }

  public Document getDocument(String sUrl)
    throws SoupProxy.Exception
  {
    return getDocument(sUrl, NO_FLAGS);
  }
  
  public Document getDocument(String sUrl, int nFlags)
    throws SoupProxy.Exception
  {
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

