package jc;

import org.jsoup.nodes.Document;

import java.io.File;
import java.net.URL;
import java.util.Map;

public interface HttpProxy {
  URL getUrl();

  Document getDocumentFromFile(String sFile)
    throws SoupProxy.Exception;

  Document getDocumentFromHttp(URL url)
      throws SoupProxy.Exception;

  Document post(URL url, Map<String, String> data)
        throws SoupProxy.Exception;

  void setCookies(URL url, Map<String, String> cookies);

  Map<String, String> getCookies(URL url);

  String getCookie(URL url, String key);

  void saveDocumentToTempFile(Document doc, File dir);

  Document getDocument(String sUrl)
          throws SoupProxy.Exception;

  Document getDocument(String sUrl, int nFlags)
            throws SoupProxy.Exception;
}
