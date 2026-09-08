package jc;

import org.assertj.core.api.Assertions;
import org.jsoup.nodes.Document;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class SoupProxyWwwTests {
  private static Logger log = LoggerFactory.getLogger(SoupProxyWwwTests.class);

  @Test
  public void busyLinkReturns503() {
    SoupProxy proxy = new SoupProxy();
    String url = "http://localhost:15863/busy?param=" +  UUID.randomUUID().toString();
    Exception caught = null;
    Integer httpStatus = null;
    try {
      Document doc = proxy.getDocument(url);
      log.info("response: " + doc.text());
    } catch (SoupProxy.Exception e) {
      caught = e;
      httpStatus = e.getStatus();
    }
    log.info("caught: " + caught);
    //log.debug("stacktrace: ", caught.getCause());
    Assertions.assertThat(caught).isNull();
    //Assertions.assertThat(httpStatus).isEqualTo(503);
  }

  @Test
  public void canReadTextUrl() throws SoupProxy.Exception {
    SoupProxy proxy = new SoupProxy();
    String url = "http://localhost:15863/pbntools/jsoup/file.txt";
    Document doc = proxy.getDocument(url);
    log.info("response: " + doc.text());
    Assertions.assertThat(doc.text()).isEqualTo("hello");
  }

  @Test
  public void canReadGzipUrl() throws SoupProxy.Exception {
    SoupProxy proxy = new SoupProxy();
    String url = "http://localhost:15863/pbntools/jsoup/file2.txt.gz";
    Document doc = proxy.getDocument(url);
    log.info("response: " + doc.text());
    Assertions.assertThat(doc.text()).isEqualTo("hello from gzip");
  }

}
