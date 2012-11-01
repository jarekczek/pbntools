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

import jc.f;
import jc.JCException;
import jc.outputwindow.SimplePrinter;
import jc.pbntools.Deal;
import jc.pbntools.PbnTools;
import jc.SoupProxy;

/**
<h1>Lin specification</h1>

<dl>
<dt>ah - deal label</dt>
<dt>an - bid explanation (for the previous bid)
<dt>mb - a bid</dt>
<dt>mc - claim, number of tricks
<dt>md - dealer and hands
  <ul>
  <li>First goes a digit denoting the dealer 1 for S, 2 = W, 3 = N, 4 = S
  <li>Right after the dealer digit come dealers cards,
      color and cards</li>
  <li>Commas separate cards of consecutive players, only 3 hands are given
      </li>
  </ul>
  For example:
  <code>3S8QKHJD8TJKC2349J,S23459TAH368DC68K,S67JH45QKD357QC7A,</code>
</dt>
<dt>pc - play card</dt>
<dt>pg - pause game. This is done in lin files downloaded directly,
      after bidding and every 4 cards</dt>
<dt>pn - player names, comma separated</dt>
<dt>rh - ? (seen empty)</dt>
<dt>st - ? (seen empty)</dt>
<dt>sv - vulnerability, n = None, b = Both, n = NS, e = EW
    (<code>o</code> also found to denote None)</dt>
<dt></dt>
<dt></dt>
<dt></dt>
<dt></dt>
</dl>

*/

public class LinReader implements DealReader
{
  protected Document m_doc;
  protected SimplePrinter m_sp;
  
  // readLin method {{{
  /**
   * Reads deals from a lin contents.
   * @param sLin Lin contents
   */
  public Deal[] readLin(String sLin, boolean bSilent)
    throws DownloadFailedException
  {
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
      if (false && !sComm.equals("pc") && !sComm.equals("mb"))
        m_sp.addLine("Command: " + sComm + ", arg: " + sArg);
    }
    return new Deal[] { new Deal() };
  } //}}}
  
  public Deal[] readDeals(String sUrl, boolean bSilent)
    throws DownloadFailedException
  {
    assert(m_doc != null);
    String sLin = m_doc.text();
    return readLin(sLin, bSilent);
  }

  /** Verifies if the <code>sUrl</code> contains valid data in this format */
  public boolean verify(String sUrl, boolean bSilent)
  {
    // We should read lin file directly, but SoupProxy has a cache
    // so it would be more network efficient to use it.
    m_doc = null;
    try {
      SoupProxy proxy = new SoupProxy();
      m_doc = proxy.getDocument(sUrl);
    }
    catch (JCException e) {
      m_sp.addLine(e.getMessage());
      return false;
    }
    String sLin = m_doc.text();
    for (int i=0; i<sLin.length(); i++) {
      String sChar = sLin.substring(i, i+1);
      if (!sChar.matches("[a-zA-Z0-9|, ]")) {
        if (!bSilent || f.isDebugMode())
          m_sp.addLine(PbnTools.getStr("msg.unexpChar", sChar, i));
        return false;
      }
    }
    if (sLin.indexOf('|') < 0) {
      if (!bSilent || f.isDebugMode())
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
