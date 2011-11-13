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
import jc.JCException;
import jc.OutputWindow;
import jc.SoupProxy;
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
  
  /** verify whether link points to a valid data in this format */
  boolean verify() throws VerifyFailedException
  {
    Document doc;
    try {
      doc = SoupProxy.getDocument(m_sLink);
    }
    catch (JCException e) {
      throw new VerifyFailedException(e);
    }
    Elements tds = doc.select("td");
    
    m_ow.addLine(PbnTools.m_res.getString("msg.documentLoaded"));
    // m_ow.addLine(doc.html());
    for (Element td : tds) {
      String tdText = td.text();
      // m_ow.addLine(tdText);
    }

    for (int i=1; i<=3; i++) {
      m_ow.addLine("hello " + i);
      if (m_ow.isStopped()) { break; }
      try {Thread.sleep(100);} catch(Exception e) {}
    }
    return true;
  }
  
  /** thread's main method */ 
  public void run()
  {
    m_ow.setTitle(f.extractTextAndMnem("pobierzPary")[0]);
    m_ow.addLine(String.format(PbnTools.m_res.getString("tourDown.msg.fetching"),
                        m_sLink));
    try {
      verify();
    }
    catch (VerifyFailedException e) { }
    catch (Throwable e) {
      e.printStackTrace();
      m_ow.addLine(e.toString() + ": " + e.getMessage());
    }
    m_ow.threadFinished();
  }
  
  class VerifyFailedException extends JCException
  {
    VerifyFailedException(Throwable t)
    {
      super(t);
      m_ow.addLine(t.getMessage());
    }
  }
  
}
