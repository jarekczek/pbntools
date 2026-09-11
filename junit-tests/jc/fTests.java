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

package jc;

import org.junit.*;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

public class fTests
{

@Test public void fileOpTest()
{
  String asDirs[] = new String[] { "c:\\temp\\1\\", "http://test.com/",
  "/tmp/x/" };
  String asFiles[] = new String[] { "file1", "file.number.two" };
  String asExts[] = new String[] { "txt", null, "" };
  for (String sDir: asDirs)
    for (String sFile: asFiles)
      for (String sExt: asExts) {
        if (sFile.indexOf(".") >= 0 && (sExt == null || sExt.equals(""))) {
          // some tests are not doable: dotted filename plus no extension
          // routines would see the extension as the last part of the filename
          continue;
        }
        String sFileExt = sFile;
        if (sExt != null)
          sFileExt += "." + sExt;
        String sPath = sDir + sFileExt;
        assertEquals("getFileName(" + sPath,
                     sFileExt, f.getFileName(sPath));
        assertEquals("getFileExt(" + sPath,
                     sExt, f.getFileExt(sPath));
        assertEquals("getFileNameNoExt(" + sPath,
                     sFile, f.getFileNameNoExt(sPath));
        assertEquals("getDirOfFile(" + sPath,
                     sDir, f.getDirOfFile(sPath));
      }
  assertEquals("getFileNameNoExt of url with ?",
    "test_id=500_name=300",
               f.getFileNameNoExt("http://server.com/dir/test.php?id=500&name=300"));
}

@Test public void str2IntTest()
{
  assertEquals(f.str2Int(null, 2), 2);
  assertEquals(f.str2Int("-1", 0), -1);
  assertEquals(f.str2Int("a", 1), 1);
}

@Test public void readFileByName() throws IOException {
  String text = f.readFile("test/f/file1.txt");
  assertEquals("nothing", text);
}

@Test public void readFileByAbsoluteUrl() throws IOException {
  String uri = new File("test/f/file1.txt").toURI().toString();
  String text = f.readFile(uri);
  assertEquals("nothing", text);
}

@Test public void getLocalFileName_forLinLink() {
  String input = "https://www.bridgebase.com/tools/handviewer.html?v3b=web&v3v=6.61.5&lin=pn%7Cplayer1%2C%7E%7EM2408pfx%2Cplayer2%2C%7E%7EM6554vje%7Cst%7C%7Cmd%7C2S97653HDT8654C975%2CST2HQJ4D93CAT8642%2CSQJ8HAK9732DAK72C%2CSAK4HT865DQJCKQJ3%7Csv%7Cb%7Crh%7C%7Cah%7CBoard+4%7Cmb%7CP%7Cmb%7C1H%7Cmb%7CP%7Cmb%7CP%7Cmb%7CP%7Cpc%7CCK%7Cpc%7CC5%7Cpc%7CC2%7Cpc%7CH2%7Cpc%7CDA%7Cpc%7CDJ%7Cpc%7CD4%7Cpc%7CD3%7Cpc%7CDK%7Cpc%7CDQ%7Cpc%7CD5%7Cpc%7CD9%7Cpc%7CS8%7Cpc%7CS4%7Cpc%7CS3%7Cpc%7CST%7Cpc%7CC6%7Cpc%7CH3%7Cpc%7CC3%7Cpc%7CC7%7Cpc%7CSJ%7Cpc%7CSK%7Cpc%7CS5%7Cpc%7CS2%7Cpc%7CCQ%7Cpc%7CC9%7Cpc%7CCT%7Cpc%7CH7%7Cpc%7CSQ%7Cpc%7CSA%7Cpc%7CS6%7Cpc%7CC8%7Cpc%7CH8%7Cpc%7CD6%7Cpc%7CHJ%7Cpc%7CHK%7Cpc%7CHA%7Cpc%7CH5%7Cpc%7CD8%7Cpc%7CH4%7Cpc%7CD2%7Cpc%7CCJ%7Cpc%7CDT%7Cpc%7CHQ%7Cpc%7CCA%7Cpc%7CH9%7Cpc%7CHT%7Cpc%7CS7%7Cpc%7CH6%7Cpc%7CS9%7Cpc%7CC4%7Cpc%7CD7%7C";

  String output = f.getLocalFileName(input);

  assertEquals("handviewer.html?v3b=web", output);
}

@Test public void getLocalFileName_forBboTraveller() {
  String input = "http://localhost:15863/pbntools/test_11_aba_17901/hands.php?traveller=17901-1617418791-72774673";

  String output = f.getLocalFileName(input);

  assertEquals("hands.php?traveller=17901-1617418791-72774673", output);
}

}
