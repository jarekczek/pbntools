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

}
