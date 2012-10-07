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

import jc.f;
import org.junit.*;
import static org.junit.Assert.*;

public class PbnToolsTests
{

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

}