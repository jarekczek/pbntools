/* *****************************************************************************

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

package jc.pbntools;

import jc.f;
import jc.OutputWindow;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class PobierzPary extends OutputWindow.Client
{

  protected String m_sLink;
  protected OutputWindow m_ow;
  
  PobierzPary(String sLink)
  {
    m_sLink = sLink;
    m_ow = null;
  }
  
  public void setOutputWindow(OutputWindow ow)
  {
    m_ow = ow;
  }
  
  public void run()
  {
    m_ow.setTitle(f.extractTextAndMnem("pobierzPary")[0]);
    try {
      Document doc;
      try {
        doc = Jsoup.connect(m_sLink).get();
      }
      catch (java.lang.IllegalArgumentException eUrl) {
        m_ow.addLine(String.format(PbnTools.m_res.getString("error.invalidUrl"),
                     m_sLink));
        throw eUrl;
      }
      Elements tds = doc.select("td");
      
      for (Element td : tds) {
        String tdText = td.text();
        m_ow.addLine(tdText);
      }
  
      for (int i=1; i<=3; i++) {
        m_ow.addLine("hello " + i);
        if (m_ow.isStopped()) { break; }
        try {Thread.sleep(100);} catch(Exception e) {}
      }
    }
    catch (java.io.IOException e) { e.printStackTrace(); }
    catch (Throwable e) { e.printStackTrace(); }
    m_ow.threadFinished();
  }
}
