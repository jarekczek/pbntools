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
    for (int i=1; i<=30; i++) {
      m_ow.addLine("hello " + i);
      if (m_ow.isStopped()) { break; }
      try {Thread.sleep(100);} catch(Exception e) {}
    }
    m_ow.threadFinished();
  }
}
