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

import jc.f;
import org.junit.*;
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
}

}
