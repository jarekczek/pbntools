package jc.pbntools.download;

import org.jsoup.nodes.Document;

public class TourCalcTourDownloaderV1 extends TourCalcTourDownloaderBase {

  public String getName() {
    return "TourCalc v1";
  }

  @Override
  protected boolean verifyThisVersion(Document doc, boolean bSilent) {
    return !hasPbnButton(doc);
  }

  @Override
  protected String jsonSuffix() {
    return "";
  }
}
