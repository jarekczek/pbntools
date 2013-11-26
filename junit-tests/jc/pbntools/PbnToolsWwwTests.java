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
import java.io.FileWriter;
import java.io.PrintStream;
import java.io.Writer;
import jc.f;
import jc.pbntools.download.BboTourDownloader;
import jc.pbntools.download.HtmlTourDownloader;
import jc.pbntools.download.KopsTourDownloader;
import jc.pbntools.download.ParyTourDownloader;
import junitx.framework.FileAssert;
import org.junit.*;
import static org.junit.Assert.*;

public class PbnToolsWwwTests
{

private static PrintStream origOut;

@Test public void pobierzKopsWwwTest4()
  throws java.io.FileNotFoundException, java.io.IOException
{
  PbnToolsTests.pobierzTestHelper(
    new KopsTourDownloader(),
    "http://localhost/pbntools/test_4_kops_www_20130807/protokoly/01/chorzow/13/SM0807/",
    "test/test_4_kops_www_20130807/sm0807.pbn",
    "SM0807/sm0807.pbn");
}

@Test public void pobierzParyWwwTest5()
  throws java.io.FileNotFoundException, java.io.IOException
{
  PbnToolsTests.pobierzTestHelper(
    new ParyTourDownloader(),
    "http://localhost/pbntools/test_5_pary_www_20130808/WB130808/wb130808.html",
    "test/test_5_pary_www_20130808/wb130808.pbn",
    "WB130808/wb130808.pbn");
}

@Test public void pobierzBboWwwTest7()
  throws java.io.FileNotFoundException, java.io.IOException
{
  f.setDebugLevel(1);
  PbnToolsTests.pobierzTestHelper(
    new BboTourDownloader(),
    "http://localhost/pbntools/test_7_bbo_www_acbl_20130810/www.bridgebase.com/myhands/hands.php@tourney=1607-1376154000-&offset=0.html",
    "test/test_7_bbo_www_acbl_20130810/acbl_1607_pairs_acbl_sat_1pm_speedball.pbn",
    "ACBL_1607_Pairs_ACBL_Sat_1pm_Speedball/acbl_1607_pairs_acbl_sat_1pm_speedball.pbn");
}

@Test public void pobierzParyWwwTest9()
  throws java.io.FileNotFoundException, java.io.IOException
{
  PbnToolsTests.pobierzTestHelper(
    new ParyTourDownloader(),
    "http://localhost/pbntools/test_9_pary_www_szczyrk_20131108/131108szczyrk/W-impy.html",
    "test/test_9_pary_www_szczyrk_20131108/131108szczyrk.pbn",
    "131108szczyrk/131108szczyrk.pbn");
}

@Test public void pobierzBboWwwTest10()
  throws java.io.FileNotFoundException, java.io.IOException
{
  PbnToolsTests.pobierzTestHelper(
    new BboTourDownloader(),
    "http://localhost/pbntools/test_10_bbo_www_annamar_1538_php/hands.php?tourney=1538-1385205517-&offset=0",
    "test/test_10_bbo_www_annamar_1538_php/annamar_1538_pairs_untitled.pbn",
    "annamar_1538_Pairs_Untitled/annamar_1538_pairs_untitled.pbn");
}

}
