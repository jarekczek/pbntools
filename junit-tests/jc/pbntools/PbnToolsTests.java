/* *****************************************************************************

    Copyright (C) 2012 Jaroslaw Czekalski - jarekczek@poczta.onet.pl

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

package jc.pbntools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FilenameFilter;
import java.io.FileWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.net.URI;

import jc.SoupProxy;
import jc.JCException;
import jc.f;
import jc.outputwindow.SimplePrinter;
import jc.outputwindow.StandardSimplePrinter;
import jc.outputwindow.TestPrinter;
import jc.pbntools.download.BboHandsHistoryLinReader;
import jc.pbntools.download.BboTourDownloader;
import jc.pbntools.download.DealReader;
import jc.pbntools.download.DownloadFailedException;
import jc.pbntools.download.HtmlTourDownloader;
import jc.pbntools.download.KopsTourDownloader;
import jc.pbntools.download.LinReader;
import jc.pbntools.download.ParyTourDownloader;
import junitx.framework.FileAssert;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.*;
import static org.junit.Assert.*;

public class PbnToolsTests
{

private static PrintStream origOut;

@Before public void setUp()
  {
    assertEquals(0, PbnTools.getVerbos());
    assertEquals(false, f.isDebugMode());
  }

@Test public void checkUpdateTest()
             throws java.io.IOException, jc.SoupProxy.Exception
  {
    String sCurrentVer = PbnTools.m_res.getString("wersja");
    // when taking PbnTools classes directly from classes dir,
    // we get path work/comp/jc here, so 3 times .. is needed
    File pbntoolsDir = new File(f.basePath(this.getClass()));
    while (!pbntoolsDir.getName().endsWith("build"))
      pbntoolsDir = pbntoolsDir.getParentFile();
    pbntoolsDir = pbntoolsDir.getParentFile();
    System.out.println("pbntoolsDir: " + pbntoolsDir.getAbsolutePath());
    File helpFile = new File(new File(pbntoolsDir, "doc"), "help_pl.html");
    String sHelpUrl = helpFile.toURI().toString();
    System.out.println(sHelpUrl);
    String sHtmlVer = PbnTools.getVersionFromUrl(sHelpUrl);
    assertTrue("versions do not match: current=" + sCurrentVer
               + ", html:" + sHtmlVer, sCurrentVer.equals(sHtmlVer));
  }

@Test public void getBaseUrlTest()
{
  String asUrl[] = { "http://test.com/start",
                     "http://test.com/start/",
                     "http://test.com/start/index.html",
                     "http://test.com/start/test.htm" };
  for (String sUrl: asUrl) {
    assertEquals("getBaseUrl(" + sUrl + ")",
                 "http://test.com/start/",
                 HtmlTourDownloader.getBaseUrl(sUrl));
  }

  String sUrl = "http://www.halo.com/test/";
  assertEquals("getBaseUrl(" + sUrl,
               sUrl, HtmlTourDownloader.getBaseUrl(sUrl));
}

static void pobierzTestHelper(HtmlTourDownloader der,
  String sHtmlFile, String sPbnFileTemplate,
  String sPbnFileTest, SimplePrinter pr)
{
  if (pr != null)
    der.setOutputWindow(pr);
  File fTempDir = new File("work/junit-tmp").getAbsoluteFile();
  fTempDir.mkdir();
  assertTrue("fTempDir (" + fTempDir.getAbsolutePath()
    + ") should already be a directory", fTempDir.isDirectory());
  System.setProperty("jc.debug", "0");
  PbnTools.m_props.setProperty("workDir", fTempDir.getAbsolutePath());
  PbnTools.downTour(sHtmlFile, der, false);
  String sDesc = "Resulting pbn files differ:\n";
  sDesc += "  " + sPbnFileTemplate + "\n";
  sDesc += "  " + new File(fTempDir, sPbnFileTest) + "\n";
  FileAssert.assertEquals(sDesc,
    new File(sPbnFileTemplate),
    new File(fTempDir, sPbnFileTest));
}

static void pobierzTestHelper(HtmlTourDownloader der,
                              String sHtmlFile, String sPbnFileTemplate,
                              String sPbnFileTest)
{
  pobierzTestHelper(der, sHtmlFile, sPbnFileTemplate, sPbnFileTest, null);
}

@Test public void pobierzParyTest1()
  throws java.io.FileNotFoundException, java.io.IOException
{
  pobierzTestHelper(
    new ParyTourDownloader(),
    "test/test_1_pary/WB120802/wb120802.html",
    "test/test_1_pary/WB120802/wb120802.pbn",
    "WB120802/wb120802.pbn");
}

@Test public void pobierzParyTest3()
  throws java.io.FileNotFoundException, java.io.IOException
{
  pobierzTestHelper(
    new ParyTourDownloader(),
    "test/test_3_pary_20130801/WB130801/wb130801.html",
    "test/test_3_pary_20130801/WB130801/wb130801.pbn",
    "WB130801/wb130801.pbn");
}

@Test public void pobierzBboTest6()
  throws java.io.FileNotFoundException, java.io.IOException
{
  pobierzTestHelper(
    new BboTourDownloader(),
    "test/test_6_bbo_skyclub_20130810" +
      "/SKY_CLUB_2196_Pairs_SKY_CLUB_JACKPOT_2000" +
      "/hands.php@tourney=2196-1376162040-&offset=0.html",
    "test/test_6_bbo_skyclub_20130810" +
      "/sky_club_2196_pairs_sky_club_jackpot_2000.pbn",
    "2196-1376162040/2196-1376162040.pbn");
}

@Test public void pobierzBboTest8()
  throws java.io.FileNotFoundException, java.io.IOException
{
  pobierzTestHelper(
    new BboTourDownloader(),
    "test/test_8_bbo_wronie_20130824" +
      "/Wronie_9533_Pairs_2720_PRZYJACIELE_WRONIA" +
      "/hands.php%3Ftourney=9533-1377369061-&offset=0.html",
    "test/test_8_bbo_wronie_20130824" +
      "/wronie_9533_pairs_2720_przyjaciele_wronia.pbn",
    "9533-1377369061/9533-1377369061.pbn");
}

@Test public void downloadBboLinsFromHistory() throws DownloadFailedException {
  DealReader dr = new BboHandsHistoryLinReader();
  dr.setOutputWindow(new StandardSimplePrinter());
  String sUrl = "test/test_bbo_history_with_lins/history_page.html";
  assert(dr.verify(sUrl, false));
  dr.readDeals(sUrl, false);
}

// pobierzKopsTest {{{
/** To compare older pbn files, generated by bash script, some contents
  * must be removed */
void makePbnNakedAsFromBash(File file)
  throws java.io.IOException
{
  String asTagsToRemove[] = new String[] { "Event", "West", "East", "North",
    "South", "Scoring" };
  String sCont = f.readFile(file.toString());
  String sTags = "(";
  for (String sTag: asTagsToRemove) {
    if (sTags.length() > 1)
      sTags += "|";
    sTags += "(" + sTag + ")";
  }
  sTags += ")";
  sCont = sCont.replaceAll("\\[" + sTags + ".*[\r\n]+", "");
  sCont = sCont.replaceAll("(\\[Board \")([0-9]\")", "$10$2");
  // swap order of Declared and Contract
  sCont = sCont.replaceAll(
    "\\[Declarer (\".*\")\\]([\r\n]+)\\[Contract (\".*\")\\]",
    "[Contract $3]$2[Declarer $1]");
  // bash used lowercase x for double
  sCont = sCont.replaceAll("Contract \"(.*)(XX)\"", "Contract \"$1xx\"");
  sCont = sCont.replaceAll("Contract \"(.*)(X)\"", "Contract \"$1x\"");
  // bash started cards always from N
  sCont = sCont.replaceAll("Deal \"E:(.*) (.*) (.*) (.*)\"",
    "Deal \"N:$4 $1 $2 $3\"");
  sCont = sCont.replaceAll("Deal \"S:(.*) (.*) (.*) (.*)\"",
    "Deal \"N:$3 $4 $1 $2\"");
  sCont = sCont.replaceAll("Deal \"W:(.*) (.*) (.*) (.*)\"",
    "Deal \"N:$2 $3 $4 $1\"");
  // vulnerability - different wording
  sCont = sCont.replaceAll("Vulnerable \"All\"", "Vulnerable \"Both\"");

  Writer bw = new FileWriter(file.toString());
  bw.write(sCont);
  bw.close();
}

@Test public void pobierzKopsTest()
  throws java.io.FileNotFoundException, java.io.IOException
{
  File fTempDir = new File("work/junit-tmp");
  fTempDir.mkdir();
  System.setProperty("jc.debug", "0");
  PbnTools.m_props.setProperty("workDir", fTempDir.toString());
  PbnTools.downTour("test/test_2_kops/PCH1003/index.html",
    new KopsTourDownloader(), false);
  makePbnNakedAsFromBash(new File(fTempDir, "PCH1003/pch1003.pbn"));
  
  // the pbn file from bash also needs adjusting, so copying it
  File fOrig = new File("test/test_2_kops/PCH1003/PCH1003.pbn");
  File fOrig2 = new File(fTempDir, "PCH1003/pch1003_0.pbn");
  String sOrigCont = f.readFile(fOrig.toString());
  BufferedWriter bw = new BufferedWriter(new FileWriter(fOrig2));
  bw.write(sOrigCont);
  bw.close();
  makePbnNakedAsFromBash(fOrig2);

  FileAssert.assertEquals("Resulting pbn files",
    fOrig2,
    new File(fTempDir, "PCH1003/pch1003.pbn"));
} //}}}

class FileFilterByExt implements FilenameFilter
{
  private String sExt;
  
  /** Give the extension together with a dot */
  FileFilterByExt(String sExt)
  {
    this.sExt = sExt;
  }
  
  public boolean accept(File dir, String name)
  {
    return (name.endsWith(sExt));
  }
}

class FileFilterNameMask implements FilenameFilter
{
  private String sMask;
  
  /** Give the extension together with a dot */
  FileFilterNameMask(String sMask)
  {
    this.sMask = sMask;
  }
  
  public boolean accept(File dir, String name)
  {
    return (name.matches(sMask));
  }
}

protected void LinToPbnConvertTestForDir(String sDirIn, String sDirOut)
  throws java.io.FileNotFoundException, java.io.IOException,
         DownloadFailedException, JCException
{
  new File(sDirOut).mkdir();
  PbnTools.m_props.setProperty("workDir", sDirOut);
  File afPbnFiles[] = new File(sDirIn + "/..")
    .listFiles(new FileFilterByExt(".pbn"));
  assertEquals("number of pbn files in the input directory",
    1, afPbnFiles.length);
  File fPbn0 = afPbnFiles[0];
  PbnFile pbnFile = new PbnFile();
  LinReader dr = new LinReader();
  dr.setOutputWindow(new StandardSimplePrinter());
  
  File tourneyHtmlFile = new File(sDirIn)
    .listFiles(new FileFilterNameMask(".*tourney.*.html"))[0];
  SoupProxy proxy = new SoupProxy();
  Document mainDoc = proxy.getDocument(tourneyHtmlFile.toString());
  Elements ele = mainDoc.select("a:contains(Board)");
  for (Element e: ele) {
    File travFile = new File(f.decodeUrlRes(SoupProxy.absUrl(e, "href")));
    Document travDoc = proxy.getDocument(travFile.toString());
    Elements ele2 = travDoc.select("a:matches(Lin)");
    for (Element e2: ele2) {
      String sLinFile = SoupProxy.absUrl(e2, "href");
      assert(dr.verify(sLinFile, !f.isDebugMode()));
      Deal[] deals = dr.readDeals(sLinFile, false); // bSilent
      pbnFile.addDeals(deals);
    }
  }

  String sNewPbnFile = sDirOut + "/" + f.getFileNameNoExt(sDirIn) + ".pbn";
  pbnFile.save(sNewPbnFile);
  
  // need to remove some contents from the tournament file
  String sCont = f.readFile(fPbn0+"");
  sCont = sCont.replaceAll("\\[Event.*[\r\n]+", "");
  sCont = sCont.replaceAll("\\[Date.*[\r\n]+", "");
  // When lin reader reads a lin file, it uses SoupProxy for that.
  // SoupProxy reduces multiple whitespaces, so we better do that too.
  sCont = sCont.replaceAll(" +", " ");
  File fPbn1 = new File(new File(sDirOut),
    f.getFileNameNoExt(sDirIn) + "_stripped.pbn");
  f.writeToFile(sCont, fPbn1);
  
  String sDesc = "lin to pbn\n" + fPbn1 + "\n" + sNewPbnFile + "\n";
  FileAssert.assertEquals(sDesc, fPbn1, new File(sNewPbnFile));
}

@Test public void LinToPbnConvertTest()
  throws java.io.FileNotFoundException, java.io.IOException,
         DownloadFailedException, JCException
{
  f.setDebugLevel(0);
  LinToPbnConvertTestForDir("test/test_6_bbo_skyclub_20130810" +
    "/SKY_CLUB_2196_Pairs_SKY_CLUB_JACKPOT_2000",
    "work/junit-tmp/lin_to_pbn");
  LinToPbnConvertTestForDir("test/test_8_bbo_wronie_20130824" +
    "/Wronie_9533_Pairs_2720_PRZYJACIELE_WRONIA",
    "work/junit-tmp/lin_to_pbn");
}

}
