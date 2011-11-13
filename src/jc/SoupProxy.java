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

package jc;

import java.net.URL;
import java.io.File;
import java.util.LinkedList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/** <code>SoupProxy</code> class retrieves an html page through
 *  <code>org.jsoup.Jsoup</code> library. Latest pages are cached.
 *  Allows supplying http or file urls.
 */

public class SoupProxy
{
  protected static LinkedList<CacheElement> m_cache;
  protected final static int m_nCacheSize = 10;
  
  static {
    m_cache = new LinkedList<CacheElement>();
  }

  public static Document getDocumentFromFile(String sFile)
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

  public static Document getDocumentFromHttp(URL url)
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

  public static Document getDocument(String sUrl)
    throws SoupProxy.Exception
  {
    // check cache
    Document doc = cacheGet(sUrl);
    if (doc != null) { return doc; }
    
    URL url;
    try {
      url = new URL(sUrl);
    }
    catch (java.net.MalformedURLException eUrl) {
      throw new SoupProxy.Exception(eUrl);
    }

    if (url.getProtocol().equals("file")) {
      doc = getDocumentFromFile(url.getFile());
    } else {
      doc = getDocumentFromHttp(url);
    }
    if (doc != null) { cachePut(sUrl, doc); }
    return doc;
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
    
    public CacheElement(String sUrl, Document doc) {
      this.sUrl = sUrl;
      this.doc = doc;
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
  
  protected static void cachePut(String sUrl, Document doc)
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
      elem = new CacheElement(sUrl, doc);
    }
    m_cache.add(elem); 
  }
  //}}}
  
}

