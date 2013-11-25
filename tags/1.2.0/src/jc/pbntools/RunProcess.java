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

import java.awt.Window;
import java.io.*;
import java.util.*;
// import javax.swing.;
import jc.f;



class StreamCopier extends Thread {
  StringBuffer m_sb = null;
  InputStream m_is = null;
  String m_sName = null;
  Object m_monitor = null;
  public boolean m_bFinished = false;

  StreamCopier(InputStream is, StringBuffer sb, String sName, Object monitor) {
    m_sb = sb; m_is = is; m_sName = sName; m_monitor = monitor;
  }

  public static StreamCopier start(InputStream is, StringBuffer sb, String sName, Object monitor) {
    StreamCopier thr = new StreamCopier(is, sb, sName, monitor);
    thr.start();
    return thr;
  }
  
  public void waitIfAvailable() {
    try {
      while (m_is.available()>0) {
        synchronized(m_monitor) {
          try { m_monitor.wait(); }
          catch (InterruptedException e) { e.printStackTrace(); }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  public void close() {
    synchronized(m_monitor) {
      try {
        m_is.close();
        }
      catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  int read() throws java.io.IOException {
    int c;
    // f.out(m_sName+"will read");
    c = m_is.read();
    // f.out(m_sName+"has read "+c);
    return c;
  }
  
  public void run() {
    int c;
    try {
      while ((c = read()) != -1) synchronized(m_monitor) {
        m_sb.append((char)c); 
        while (m_is.available() > 0) {
          //nBytes += m_is.available();
          byte[] buf = new byte[m_is.available()];
          m_is.read(buf);
          m_sb.append(new String(buf));
        }
        // f.out(m_sName+" notifies.");
        m_monitor.notifyAll();
      }
    } catch (IOException e) {
      // maybe the process has finished, let's remain silent
      // e.printStackTrace();
    }
    // f.out(m_sName+" after loop");

    synchronized(m_monitor) {
      m_bFinished = true;
      m_monitor.notifyAll();
    }
    // f.out("StreamCopier "+m_sName+" finished");
  }
}

class RunProcessThread extends Thread {

  RunProcess m_rp;
  RunProcessThread(RunProcess rp) {
    m_rp = rp;
    }

  int appendStream(StringBuffer sb, InputStream is) {
    int nBytes = 0;
    try {
      while (is.available() > 0) {
        nBytes += is.available();
        byte[] buf = new byte[is.available()];
        is.read(buf);
        sb.append(new String(buf));
        }
      }
    catch (IOException e) { sb.append(e.toString()); }
    return nBytes;
    }

  private void ustawTekstOut(String sCmd, StringBuffer sbOut, StringBuffer sbErr, Integer intExitCode, Exception e) {
    String sMsg = "COMMAND:\n"+sCmd+"\n\nOUTPUT:\n" + sbOut + (sbErr.length()==0 ? "" : "\nERROR:\n" + sbErr);
    if (intExitCode!=null) { sMsg += "\nPROCESS EXIT CODE: "+intExitCode+"\n"; }
    if (e!=null) { sMsg += "\nJAVA EXCEPTION: "+e.toString()+"\n"; }
    m_rp.m_dlgProc.ebOut.setText(sMsg);
    m_rp.m_dlgProc.ebOut.setCaretPosition(sMsg.length());
    }

  static String[] getCmdArray(String sCmd, String sArgs[]) {
    String asCmd[];
    asCmd = new String[1 + (sArgs==null ? 0 : sArgs.length)];
    asCmd[0] = sCmd;
    if (sArgs!=null) {
      System.arraycopy(sArgs, 0, asCmd, 1, sArgs.length);
    }
    return asCmd;
  }

  /// gets environment from System and translates it to a format accepted
  /// by Runtime.exec(). Prepends path variable with given paths.
  protected String[] makeEnv(String asPaths[]) {
    if (asPaths==null || asPaths.length==0) { return null; }
    
    char sPathSep = m_rp.m_bLinux ? ':' : ';';  
    ArrayList<String> asEnv = new ArrayList<String>();
    for (Map.Entry<String, String> e : System.getenv().entrySet()) {
      String sValue = e.getValue();
      if (e.getKey().equalsIgnoreCase("path")) {
        StringBuffer sb = new StringBuffer(sValue);
        for (int i=asPaths.length-1; i>=0; i--) {
          String p = asPaths[i];
          sb.insert(0, sPathSep);
          sb.insert(0, p);
        }
        //f.out(new String(sb));
        sValue = new String(sb);
      }
      asEnv.add(e.getKey() + "=" + sValue);
    }
    return asEnv.toArray(new String[0]);
  }
  
  public void run() {
    Process p;
    StreamCopier scOut, scErr;
    StringBuffer sbOut = new StringBuffer(), sbErr = new StringBuffer();
    String sCmd2;
    String asCmd[] = getCmdArray(m_rp.m_sCmd, m_rp.m_asArgs);
    sCmd2 = f.toSpacedString(asCmd);
    try {
      p = Runtime.getRuntime().exec(asCmd, makeEnv(m_rp.m_asPaths), m_rp.m_dir);
      scOut = StreamCopier.start(p.getInputStream(), sbOut, "out", m_rp);
      scErr = StreamCopier.start(p.getErrorStream(), sbErr, "err", m_rp);
      ustawTekstOut(sCmd2, sbOut, sbErr, null, null);
      while (m_rp.m_rv<0 && !m_rp.m_bDestroy) {
//        System.out.println("in while " + new Date().getTime()%(60*1000));
        try { m_rp.m_rv = p.exitValue(); } //p.waitFor()
        catch (IllegalThreadStateException e) {
          synchronized(m_rp) {
            // we checked that the process was still running, but things may
            // have changed in this millisecond, so we're gonna wait only
            // if streamcopiers are still running
            if (!scOut.m_bFinished || !scErr.m_bFinished) {
              try { m_rp.wait(3000); }
              catch (InterruptedException e2) { e.printStackTrace(); }
            }
            if (m_rp.m_filtr != null) { m_rp.m_filtr.filtruj(sbOut); }
            ustawTekstOut(sCmd2, sbOut, sbErr, null, null);
          }
          }
        }
      
      if (m_rp.m_rv<0 && m_rp.m_bDestroy) {
        p.destroy();
      }
      try {
        synchronized(m_rp) { m_rp.notifyAll(); }
        scOut.join(2000);
        scErr.join(2000);
        if (!scOut.m_bFinished || !scErr.m_bFinished) {
          f.msg(PbnTools.m_res.getString("error.processesStillRunning"));
        }
        scOut.join();
        scErr.join();
      }
      catch (InterruptedException e) { System.err.println(e.toString()); }
      ustawTekstOut(sCmd2, sbOut, sbErr, m_rp.m_rv, null);
      }
    catch (IOException e) {
      System.err.println(e.toString());
      ustawTekstOut(sCmd2, sbOut, sbErr, null, e);
      }

    m_rp.m_dlgProc.pbZatrzymaj.setEnabled(false);
    m_rp.m_dlgProc.pbZamknij.setEnabled(true);
    if (m_rp.m_rv==126) { f.msg(PbnTools.m_res.getString("error.shRet126")); }
    if (m_rp.m_rv==127) { f.msg(PbnTools.m_res.getString("error.shRet127")); }
    }
  }

/// \brief uruchamia modalny dialog, a w nim proces wskazany parametrem; wywolanie przez static runCmd
public class RunProcess {

  abstract static class FiltrTekstu {
    abstract void filtruj(StringBuffer sb);
    }

  DlgProcess m_dlgProc;
  Window m_dlgMain;
  String m_sCmd;
  String m_asArgs[];
  String m_asPaths[];
  boolean m_bDestroy;
  int m_rv;
  boolean m_bLinux;
  FiltrTekstu m_filtr;
  File m_dir;

  public RunProcess(Window dlgMain, String sCmd, String asArgs[], String sDir) {
    m_dlgProc = null;
    m_dlgMain = dlgMain;
    m_rv = -1;
    m_sCmd = sCmd;
    m_asArgs = asArgs;
    m_asPaths = null;
    m_bLinux = System.getProperty("os.name").equals("Linux");
    m_filtr = null;
    m_dir = sDir==null ? null : new File(sDir);
    }

  int run() {
    RunProcessThread rpt = new RunProcessThread(this);
    rpt.start();
    return m_rv;
    }
  
  void runProcessDialog() {
    if (m_dlgProc==null) {
      m_dlgProc = new DlgProcess(m_dlgMain, true, this);
      }
    run();
    m_dlgProc.pbZatrzymaj.setEnabled(true);
    m_dlgProc.pbZamknij.setEnabled(false);
    // to jest modalne wywolanie, tu zawisniemy az uzytkownik zamknie dialog
    m_dlgProc.setVisible(true);
    }
  
  /** This version of <code>runCmd</code> gives better control over
    * process startup parameters, which may be set by caller after creating
    * {@link RunProcess} object.
    * @see #RunProcess
    */
  public static int runCmd(RunProcess rp)
  {
    rp.m_bDestroy = false;
    rp.runProcessDialog();
    rp.m_bDestroy = true;
    return rp.m_rv;
  }

  static int runCmd(Window dlgMain, String sCmd, String asArgs[],
                    FiltrTekstu filtr, String sDir, String asPaths[]) {
    RunProcess rp = new RunProcess(dlgMain, sCmd, asArgs, sDir);
    rp.m_filtr = filtr;
    rp.m_asPaths = asPaths;
    return runCmd(rp);
    }

  public static int runCmd(Window dlgMain, String sCmd, String asArgs[], String sDir)
    { return runCmd(dlgMain, sCmd, asArgs, null, sDir, new String[0]); }
  public static int runCmd(Window dlgMain, String sCmd, String asArgs[], String sDir, String asPaths[])
    { return runCmd(dlgMain, sCmd, asArgs, null, sDir, asPaths); }
  public static int runCmd(Window dlgMain, String sCmd, String asArgs[], FiltrTekstu filtr)
    { return runCmd(dlgMain, sCmd, asArgs, filtr, null, new String[0]); }
  public static int runCmd(Window dlgMain, String sCmd, FiltrTekstu filtr) {
    String asCmdArray[] = sCmd.split(" ");
    String asArgs[] = Arrays.copyOfRange(asCmdArray, 1, asCmdArray.length);
    return runCmd(dlgMain, asCmdArray[0], asArgs, filtr, null, new String[0]);
  }
  public static int runCmd(Window dlgMain, String sCmd, String asArgs[])
    { return runCmd(dlgMain, sCmd, asArgs, null, null, new String[0]); }
  public static int runCmd(Window dlgMain, String sCmd) {
    return runCmd(dlgMain, sCmd, (FiltrTekstu)null);
  }

  static void printStream(InputStream is, PrintStream ps) {
    int ch;
    try {
      while ((ch=is.read()) >= 0) {
        ps.print(String.format("%c", ch));
        }
      }
    catch (IOException e) { System.err.println(e.toString()); }
    }
    
  static String stream2String(InputStream is) {
    // DataInputStream ds = new DataInputStream(is);
    StringBuffer sb = new StringBuffer();
    String sLinia = null;
    try {
      int nBytes;
      byte buf[];
      while ((nBytes = is.available()) > 0) {
        buf = new byte[nBytes];
        is.read(buf);
        sb.append(new String(buf));
        }
      }
    catch (IOException e) { sb.append(e.toString()); }
    return new String(sb);
    }
  
  }
