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

import java.net.URL;

import jc.f;
import jc.JCException;
import jc.OutputWindow;
import jc.SoupProxy;
import jc.pbntools.PbnTools;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ParyTourDownloader extends HtmlTourDownloader
{

  public void setOutputWindow(OutputWindow ow)
  {
    m_ow = ow;
  }
  
  /** Verifies whether link points to a valid data in this format.
    * Sets m_sTitle and m_sDirName members
    */ //{{{
  public boolean verify() throws VerifyFailedException
  {
    Document doc;
    try {
      SoupProxy proxy = new SoupProxy();
      doc = proxy.getDocument(m_sLink);
      m_doc = doc;
      m_url = proxy.getUrl();
    }
    catch (JCException e) {
      throw new VerifyFailedException(e);
    }
    Elements tds = doc.head().select("meta[name=GENERATOR]");
    
    m_ow.addLine(PbnTools.m_res.getString("msg.documentLoaded"));
    // m_ow.addLine(doc.html());
    for (Element td : tds) {
      String tdText = td.attr("name") + ":" + td.attr("content");
      m_ow.addLine(tdText);
    }

    if (!checkGenerator(doc, "JFR 2005", false)) { return false; }
    getTitleAndDir();
    
    for (int i=1; i<=3; i++) {
      m_ow.addLine("hello " + i);
      if (m_ow.isStopped()) { break; }
      try {Thread.sleep(100);} catch(Exception e) {}
    }
    return true;
  } //}}}
    
}
