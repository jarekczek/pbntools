package jc.pbntools.download;

import jc.JCException;
import jc.SoupProxy;
import jc.outputwindow.SimplePrinter;
import jc.pbntools.Deal;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Reads lin directly from a url, like https://www.bridgebase.com/tools/handviewer.html?lin=pn%7C...
 */
public class LinLinkReader implements DealReader {
  private static Logger log = LoggerFactory.getLogger(LinLinkReader.class);
  protected SimplePrinter m_sp;
  private String resolvedUrl;

  @Override
  public boolean verify(String sUrl, boolean bSilent) {
    SoupProxy proxy = new SoupProxy();
    try {
      Document doc = proxy.getDocument(sUrl);
      resolvedUrl = doc.baseUri();
      log.debug("Resolved url: " + resolvedUrl);
      return resolvedUrl.contains("handviewer.html") && resolvedUrl.contains("lin=pn");
    } catch (JCException e) {
      m_sp.addLine(e.getMessage());
      log.debug("", e);
      return false;
    }
  }

  @Override
  public Deal[] readDeals(String sUrl, boolean bSilent) throws DownloadFailedException {
    String urlEncodedData = resolvedUrl.replaceFirst("^.*/handviewer.html.*[&?]lin=", "");
    try {
      String linData = URLDecoder.decode(urlEncodedData, StandardCharsets.UTF_8.name());
      LinReader linReader = new LinReader();
      linReader.setOutputWindow(m_sp);
      return linReader.readLin(linData, bSilent);
    } catch (UnsupportedEncodingException e) {
      throw new DownloadFailedException(e, m_sp, bSilent);
    }
  }

  @Override
  public void setOutputWindow(SimplePrinter sp) {
    m_sp = sp;
  }

  @Override
  public SimplePrinter getOutputWindow() {
    return m_sp;
  }
}
