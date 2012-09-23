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

import java.io.File;
import java.io.PrintStream;
import jc.f;
import jc.pbntools.download.HtmlTourDownloader;
import junitx.framework.FileAssert;
import org.junit.*;
import static org.junit.Assert.*;

public class PbnToolsTests
{

private static PrintStream origOut;

@Test public void checkUpdateTest()
             throws java.io.IOException, jc.SoupProxy.Exception
  {
    String sCurrentVer = PbnTools.m_res.getString("wersja");
    String sHelpPath = f.basePath(this.getClass()) + f.sDirSep + ".."
                       + f.sDirSep + "doc" + f.sDirSep;
    String sHelpUrl = "file://" + sHelpPath + "help_pl.html";
    // System.out.println(sHelpUrl);
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

@Test public void pobierzParyTest()
  throws java.io.FileNotFoundException, java.io.IOException
{
  File fTempDir = new File("work/junit-tmp");
  fTempDir.mkdir();
  System.setProperty("jc.debug", "0");
  PbnTools.m_props.setProperty("workDir", fTempDir.toString());
  PbnTools.pobierzPary("test/test_1_pary/WB120802/wb120802.html", false);
  FileAssert.assertEquals("Resulting pbn files",
    new File("test/test_1_pary/WB120802/wb120802.pbn"),
    new File(fTempDir, "WB120802/wb120802.pbn"));
}

}
