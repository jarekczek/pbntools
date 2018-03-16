package jc.pbntools.download

import jc.outputwindow.TestPrinter
import org.junit.Assert
import org.junit.Test
import java.io.File

class BboResultsExtractorTests {

  @Test
  fun extractResultsCsvTest() {
    val der = BboTourDownloader()
    der.m_sLocalDir = "test/test_bbo_postprocess"
    der.m_sSourceDir = der.m_sLocalDir
    der.outputWindow = TestPrinter()
    BboResultsExtractor(der).extractResultsCsv()

    val outFile = File(der.m_sLocalDir, "results.csv")
    val lines = outFile.readLines()
    Assert.assertEquals(33, lines.size)
    Assert.assertEquals("32;venzel;48.21", lines.get(32))
    Assert.assertEquals("2;3ntlarry;69.05", lines.get(2))
    outFile.delete()

    val outFileRanked = File(der.m_sLocalDir, "results_ranked.csv")
    val linesRanked = outFileRanked.readLines()
    Assert.assertEquals(33, linesRanked.size)
    Assert.assertEquals("1;3ntlarry;69.05;1;16", linesRanked.get(1))
    Assert.assertEquals("14;tbaker2167;52.98;6;11", linesRanked.get(14))
    Assert.assertEquals("32;mbuaba;30.95;16;1", linesRanked.get(32))
    outFileRanked.delete()
  }
}