package jc.pbntools.download;

import org.jsoup.nodes.Document;

/**
 * TourCalc v2 serves json files with gz extension. This downloader detects v2 version
 * by checking if there is a PBN button on the page.
 */
public class TourCalcTourDownloaderV2 extends TourCalcTourDownloaderBase {

  public String getName() {
    return "TourCalc v2";
  }

  @Override
  protected boolean verifyThisVersion(Document doc, boolean bSilent) {
    return hasPbnButton(doc);
  }

  @Override
  protected String jsonSuffix() {
    return ".gz";
  }
}
