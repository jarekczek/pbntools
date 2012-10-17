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

public abstract class OutputWindow implements SimplePrinter {
  
  protected StringBuffer m_sb;
  /** implements <code>Runnable</code> */
  protected Client m_cli;
  private boolean m_bStop;
  private CountDownLatch m_runLatch;
  private SwingWorker m_sw;
  
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
  
  public boolean isStopped() { return m_bStop; }

  /** Sets interrupted flag for the client thread. This method is
    * synchronized to not let the thread be interrupted while
    * performing critical operations, for instance
    * <code>Document.insertString</code> */
  public synchronized void stopClient() {
    m_bStop = true;
    if (m_sw != null)
      m_sw.cancel(true);
  }
  
  /** <code>run</code> must call <code>ow.threadFinished()</code>
    * when finishes. */
  public static abstract class Client implements Runnable
  {
    abstract public void setOutputWindow(SimplePrinter ow);
  }

  // runClient method {{{
  protected void runClient()
  {
    m_sw = new SwingWorker() {
      @Override
      public Object doInBackground()
      {
        m_cli.run();
        threadFinished();
        return null;
      }
      @Override
      protected void done()
      {
      }
    };
    if (!m_bStop)
      m_sw.execute();
  }
  
  /** Override this method to react on thread finishing, but call
    * also super. */
  public void threadFinished()
  {
    m_runLatch.countDown();
    if (f.isDebugMode())
      System.out.println("Client thread finished.");
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
  public static class Process {
    
    public boolean m_bShowCommand = true;
    protected SimplePrinter m_sp;
    
    public Process(SimplePrinter sp) {
      m_sp = sp;
    }
    
    public boolean stillRunning(java.lang.Process p) {
      try {
        p.exitValue();
        return false;
      }
      catch (java.lang.IllegalThreadStateException e) {
        return true;
      }
    }
    
    public int exec(String as[]) throws JCException {
      if (m_bShowCommand) { m_sp.addLine(f.toSpacedString(as)); }
      ProcessBuilder pb = new ProcessBuilder(as);
      pb.redirectErrorStream(true);
      java.lang.Process p = null;
      StreamCopyThread sct = null; 
      try {
        p = pb.start();
        InputStream is = p.getInputStream();
        sct = new StreamCopyThread(is, m_sp);
        sct.start();
        p.waitFor();
      }
      catch (java.io.IOException eio) {
        throw new JCException(eio);
      }
      catch (InterruptedException ie) {
        // no problem, but we must pass the flag further
        Thread.currentThread().interrupt();
      }
      if (Thread.currentThread().isInterrupted()) {
        p.destroy();
        sct.interrupt();
        return 128;
      }
      return p.exitValue();
    }
  }

}
