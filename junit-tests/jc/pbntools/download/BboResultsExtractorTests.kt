package jc.pbntools.download

import jc.outputwindow.TestPrinter
import junitx.framework.FileAssert
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

class BboResultsExtractorTests {
  @Test
  fun extractResultsCsvTest() {
    val der = BboTourDownloader()
    der.m_sSourceDir = "test/test_bbo_postprocess"
    der.m_sLocalDir = "junit-tmp/postprocess"
    File(der.m_sSourceDir, "tview.php@t=6805-1520125200.html")
      .copyTo(File(der.m_sLocalDir, "tview.html"), true)
    der.outputWindow = TestPrinter()
    BboResultsExtractor(der).extractResultsCsv()

    val outFile = File(der.m_sLocalDir, "results.csv")
    val lines = outFile.readLines()
    Assert.assertEquals(33, lines.size)
    Assert.assertEquals("32;venzel;48.21", lines.get(32))
    Assert.assertEquals("2;3ntlarry;69.05", lines.get(2))
    FileAssert.assertEquals(
      File(der.m_sSourceDir, "results.csv"),
      outFile)

    val outFileRanked = File(der.m_sLocalDir, "results_ranked.csv")
    val linesRanked = outFileRanked.readLines()
    Assert.assertEquals(33, linesRanked.size)
    Assert.assertEquals("1;3ntlarry;69.05;1;16", linesRanked.get(1))
    Assert.assertEquals("14;tbaker2167;52.98;6;11", linesRanked.get(14))
    Assert.assertEquals("32;mbuaba;30.95;16;1", linesRanked.get(32))
    FileAssert.assertEquals(
      File(der.m_sSourceDir, "results_ranked.csv"),
      outFileRanked)
  }
}