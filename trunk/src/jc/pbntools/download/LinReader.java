/* *****************************************************************************

    jedit options: :folding=explicit:tabSize=2:noTabs=true:
    
    Copyright (C) 2011 Jaroslaw Czekalski - jarekczek@poczta.onet.pl

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

package jc.pbntools.download;

import java.io.FileWriter;
import java.io.Writer;
import java.util.Scanner;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import jc.JCException;
import jc.outputwindow.SimplePrinter;
import jc.pbntools.Deal;
import jc.pbntools.PbnTools;
import jc.SoupProxy;

public class LinReader implements DealReader
{
  protected Document m_doc;
  protected SimplePrinter m_sp; 
  
  public Deal[] readDeals(String sUrl, boolean bSilent)
    throws DownloadFailedException
  {
    assert(m_doc != null);
    String sLin = m_doc.text();
    // m_sp.addLine(sLin);
    Scanner sc = new Scanner(sLin).useDelimiter("\\|");
    while (sc.hasNext()) {
      String sComm = sc.next();
      if (sComm.length() == 0) {
       throw new DownloadFailedException(
         PbnTools.getStr("lin.error.emptyCmd", sc.match().start()));
      }
      if (!sc.hasNext()) {
       throw new DownloadFailedException(
         PbnTools.getStr("lin.error.noArg", sComm));
      }
      String sArg = sc.next();
      // m_sp.addLine("Command: " + sComm + ", arg: " + sArg);
    }
    return null;
  }

  /** Verifies if the <code>sUrl</code> contains valid data in this format */
  public boolean verify(String sUrl, boolean bSilent)
    throws VerifyFailedException
  {
    // We should read lin file directly, but SoupProxy has a cache
    // so it would be more network efficient to use it.
    m_doc = null;
    try {
      SoupProxy proxy = new SoupProxy();
      m_doc = proxy.getDocument(sUrl);
    }
    catch (JCException e) {
      throw new VerifyFailedException(e, m_sp, !bSilent);
    }
    String sLin = m_doc.text();
    for (int i=0; i<sLin.length(); i++) {
      String sChar = sLin.substring(i, i+1);
      if (!sChar.matches("[a-zA-Z0-9|, ]")) {
        if (!bSilent)
          m_sp.addLine(PbnTools.getStr("msg.unexpChar", sChar, i));
        return false;
      }
    }
    if (sLin.indexOf('|') < 0) {
      if (!bSilent)
        m_sp.addLine(PbnTools.getStr("msg.missChar", "|"));
      return false;
    }
    return true;
  }
    
  /** Sets the window to which output messages will be directed */
  public void setOutputWindow(SimplePrinter sp)
  {
    m_sp = sp;
  }
}
