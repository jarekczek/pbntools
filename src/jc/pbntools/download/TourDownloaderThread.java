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

import jc.f;
import jc.JCException;
import jc.OutputWindow;
import jc.SoupProxy;
import jc.pbntools.PbnTools;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class TourDownloaderThread extends OutputWindow.Client
{

  protected String m_sLink;
  protected OutputWindow m_ow;
  protected HtmlTourDownloader m_dloader;
  
  public TourDownloaderThread(String sLink, HtmlTourDownloader dloader) //{{{
  {
    m_sLink = sLink;
    m_dloader = dloader;
    m_dloader.setLink(m_sLink);
    m_ow = null;
  } //}}}
  
  public void setOutputWindow(OutputWindow ow)
  {
    m_ow = ow;
    m_dloader.setOutputWindow(m_ow);
  }
  
  /** thread's main method */ //{{{
  public void run()
  {
    m_ow.setTitle(f.extractTextAndMnem("pobierzPary")[0]);
    m_ow.addLine(String.format(PbnTools.m_res.getString("tourDown.msg.fetching"),
                        m_sLink));
    try {
      m_dloader.verify();
    }
    catch (HtmlTourDownloader.VerifyFailedException e) { }
    catch (Throwable e) {
      e.printStackTrace();
      m_ow.addLine(e.toString() + ": " + e.getMessage());
    }
    m_ow.threadFinished();
  } //}}}
  
}
