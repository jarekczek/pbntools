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

import java.util.concurrent.CountDownLatch;
import java.io.InputStream;
import java.lang.ProcessBuilder;
import java.util.ResourceBundle;
import javax.swing.SwingWorker;
import jc.f;
import jc.JCException;

/** General purpose window which serves as output for longer processes.
    <code>OutputWindow</code> object is created through a static method
    <code>create()</code>.
  */

public abstract class OutputWindow {
  
  protected StringBuffer m_sb;
  /** implements <code>Runnable</code> */
  protected Client m_cli;
  protected boolean m_bStop;
  private CountDownLatch m_runLatch;
  
  protected ResourceBundle m_res;

  /** Subclasses should invoke runClient() at the end of the constructor */
  public OutputWindow(Client cli, ResourceBundle res)
  {
    m_res = res;
    m_sb = new StringBuffer();
    m_bStop = false;
    m_cli = cli;
    m_cli.setOutputWindow(this);
    m_runLatch = new CountDownLatch(1);
  }
  
  /** Adds a line of text */
  public abstract void addLine(String s);
  
  /** Adds text without new line */
  public abstract void addText(String s);
  
  public boolean isStopped() { return m_bStop; }
  
  /** <code>run</code> must call <code>ow.threadFinished()</code>
    * when finishes. */
  public static abstract class Client implements Runnable
  {
    public void setOutputWindow(OutputWindow ow) {};
  }

  // runClient method {{{
  protected void runClient()
  {
    SwingWorker sw = new SwingWorker() {
      @Override
      public Object doInBackground()
      {
        m_cli.run();
        return null;
      }
      @Override
      protected void done()
      {
        threadFinished();
      }
    };
    sw.execute();
  }
  
  /** Override this method to react on thread finishing, but call
    * also super. */
  public void threadFinished()
  {
    m_runLatch.countDown();
  }
  
  /** Has the thread finished running? */
  public boolean isFinished()
  {
    return m_runLatch.getCount() == 0;
  }
  
  /** Waits until the thread finishes */
  public void waitFor()
  {
    boolean interrupted = false;
    while (m_runLatch.getCount() != 0) {
      try {
        m_runLatch.await();
      }
      catch (InterruptedException ie) {
        interrupted = true;
      }
    }
    if (interrupted)
      Thread.currentThread().interrupt();
  }
  
  /** Override if can set title */
  public void setTitle(String sTitle) {}

  /** A class to enable running of external processes and having their output
  * in our window. */
  public class Process {
    
    public boolean m_bShowCommand = true;
    
    protected Process() {}
    
    public boolean stillRunning(java.lang.Process p) {
      try {
        p.exitValue();
        return false;
      }
      catch (java.lang.IllegalThreadStateException e) {
        return true;
      }
    }
    
    protected void printStream(InputStream is) {
      try {
        while (is.available() > 0) {
          byte[] buf = new byte[is.available()];
          is.read(buf);
          addText(new String(buf));
        }
      }
      catch (java.io.IOException e) { }
    }
    
    public int exec(String as[]) throws JCException {
      if (m_bShowCommand) { addLine(f.toSpacedString(as)); }
      ProcessBuilder pb = new ProcessBuilder(as);
      pb.redirectErrorStream(true);
      java.lang.Process p = null;
      InputStream is = null;
      try {
        p = pb.start();
        is = p.getInputStream();
        while (stillRunning(p) && !m_bStop) {
          printStream(is);
        }
      }
      catch (java.io.IOException eio) {
        throw new JCException(eio);
      }
      if (m_bStop) {
        p.destroy();
        printStream(is);
        return 128;
      }
      printStream(is);
      return p.exitValue();
    }
  }

  public Process createProcess() {
    return new Process();
  }
    
}
