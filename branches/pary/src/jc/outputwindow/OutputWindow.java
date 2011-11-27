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

package jc.outputwindow;

import java.io.*;
import java.util.*;
import jc.f;

/** General purpose window which serves as output for longer processes.
    <code>OutputWindow</code> object is created through a static method
    <code>create()</code>.
  */

public abstract class OutputWindow {
  
  protected StringBuffer m_sb;
  /** implements <code>Runnable</code> */
  protected Client m_cli;
  protected Thread m_thr;
  protected boolean m_bStop;
  
  protected ResourceBundle m_res;

  /** Subclasses should run m_thr.run() at the end of the constructor */
  public OutputWindow(Client cli, ResourceBundle res)
  {
    m_res = res;
    m_sb = new StringBuffer();
    m_bStop = false;
    m_cli = cli;
    m_cli.setOutputWindow(this);
    m_thr = new Thread(m_cli);
    m_thr.setName("OutputWindow-client");
  }
  
  /** Adds a line of text */
  public abstract void addLine(String s);
  
  /** Adds text without new line */
  public abstract void addText(String s);
  
  public boolean isStopped() { return m_bStop; }
  
  public static abstract class Client implements Runnable
  {
    public void setOutputWindow(OutputWindow ow) {};
  }
  
  /** Override this method to react on thread finishing */
  public void threadFinished()
  {
  }
  
  /** Override if can set title */
  public void setTitle(String sTitle) {}
}
