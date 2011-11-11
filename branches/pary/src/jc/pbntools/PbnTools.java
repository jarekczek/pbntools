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

import java.io.*;
import java.util.*;
import javax.swing.*;
import jc.f;
import jc.OutputWindow;
import jc.pbntools.*;

public class PbnTools {
  static String m_sCurDir;
  static String m_sScriptDir;
  static String m_sBinDir;
  public static boolean bLinux;
  static DlgPbnToolsMain m_dlgMain;
  static String m_sSlash;
  public static ResourceBundle m_res;
  static Properties m_props;
  static boolean m_bPropsRead;
  public static boolean m_bVerbose;
  
  static {
    try { m_sCurDir = f.basePath(Class.forName("jc.pbntools.PbnTools")); }
    catch (ClassNotFoundException e) { throw new RuntimeException(e); }
    m_sScriptDir = new File(m_sCurDir, "script").getAbsolutePath();
    m_sBinDir = new File(m_sCurDir, "bin").getAbsolutePath();
    //System.getProperties().getProperty("user.dir");
    m_sSlash = System.getProperties().getProperty("file.separator");
    m_res = ResourceBundle.getBundle("jc.pbntools.PbnTools", Locale.getDefault() /* new Locale("en_US") */ );
    m_props = new Properties();
    m_bPropsRead = false;
    bLinux = System.getProperty("os.name").equals("Linux");
    m_bVerbose = false;

    if (m_sCurDir.indexOf(' ') >= 0) {
      f.msg(m_res.getString("error.installedWithSpaces"));
    }
    //    JOptionPane.showMessageDialog(null, m_res.getString("test") + System.getProperties().getProperty("jarek.wersja"));
    }

  static void pobierzKops(String sLink) {
    int rv;
    String sWorkDir = PbnTools.m_props.getProperty("workDir");
    if (sWorkDir==null || sWorkDir.length()==0) {
      f.msg(PbnTools.m_res.getString("error.noWorkDir"));
      return;
    }
    // msys needs converting all path separators from \ to /
    String sScript = (m_sScriptDir + m_sSlash + "get_tur_kops.sh").replaceAll("\\\\", "/");
    String asArgs[] = { "-c",
                        sScript
                        + " -d \"" + sWorkDir.replaceAll("\\\\", "/") + "\" "
                        + sLink };
    if (bLinux) {
      rv = RunProcess.runCmd((JDialog)m_dlgMain, "bash", asArgs, m_sScriptDir);
      }
    else {
      String sMsysBin = m_sCurDir + m_sSlash + "bin" + m_sSlash + "msys" + m_sSlash + "bin";
      String asPaths[] = { sMsysBin,
                           m_sCurDir + m_sSlash + "bin" + m_sSlash + "wget" };
      String sBash = sMsysBin + m_sSlash + "bash";
      rv = RunProcess.runCmd((JDialog)m_dlgMain, sBash, asArgs, m_sScriptDir, asPaths);
      }
  }
  
  static void pobierzPary(String sLink) {
    // javax.swing.JOptionPane.showMessageDialog(null, "Pary!");
    OutputWindow ow = OutputWindow.create(m_dlgMain, PbnTools.m_res);
    ow.showWindow();
    for (int i=1; i<=50; i++) { ow.addLine(""+i); }
    javax.swing.JOptionPane.showMessageDialog(null, "ready!");
    f.out("will close");
    ow.close();

    // ow.setVisible(true);
  }
    
  static void pobierzBbo(String sLink) {
    if (sLink==null) { sLink = "owm"; }
    //RunProcess.runCmd((JDialog)m_dlgMain, "sh" + (bLinux ? "" : " --login") + " "+m_sCurDir+m_sSlash+"get_tur_bbo.sh " + sLink);
    }

  public static void setWindowIcons(java.awt.Window wnd) {
    String asSuff[] = { "48", "32", "24", "16" };
    ArrayList<java.awt.Image> ai = new ArrayList<java.awt.Image>();
    try {
      for (String sSuf : asSuff) {
        String sRes = "res/pik_nieb_zol_"+sSuf+".png";
        InputStream is = PbnTools.class.getResourceAsStream(sRes);
        if (is == null) {
          f.err("Brak pliku "+sRes);
        } else {
          ai.add(javax.imageio.ImageIO.read(is));
        }
      }
    } catch (java.io.IOException e) {
      e.printStackTrace();
    }
    wnd.setIconImages(ai);
  }
  
  static void printUsage() {
    f.out(m_res.getString("usage.1"));
  }
  
    
  static void parseCommandLine(String args[]) {
    for (int i=0; i<args.length; i++) {
      if (args[i].equals("--verbose")) {
        m_bVerbose = true;
      }
      if (args[i].equals("-h") || args[i].equals("--help") || args[i].equals("/?")) {
        printUsage();
        System.exit(0);
      }
    }
  }
    
    
  public static void main(String args[]) {
    parseCommandLine(args);
    String sPropsFile = System.getProperty("user.home") + System.getProperty("file.separator") + "PbnTools.props";
    try { m_props.load(new FileReader(sPropsFile)); m_bPropsRead = true; }
    catch (java.io.FileNotFoundException e) { m_bPropsRead = true; }
    catch (IOException e) { System.out.println(m_res.getString("props.load.error") + ": " + e.toString()); } 
    
    //try { Runtime.getRuntime().exec("cmd.exe /C start cmd.exe /C \"echo hi && pause\""); }
    //catch (Exception e) { System.err.println(e.toString()); }
    // String asPaths[] = { m_sCurDir + m_sSlash + "bin" + m_sSlash + "msys" + m_sSlash + "bin",
                           // m_sCurDir + m_sSlash + "bin" + m_sSlash + "wget" };
    // String asArgs[] = { "-c", "ls" };                  
    // RunProcess.runCmd(null, "bash", asArgs, m_sScriptDir, asPaths);

    
    m_dlgMain = new DlgPbnToolsMain(null, true);
    m_dlgMain.setModalityType(java.awt.Dialog.ModalityType.DOCUMENT_MODAL);
    m_dlgMain.setVisible(true);



    
    try { if (m_bPropsRead) { m_props.store(new FileWriter(sPropsFile), null); } }
    catch (IOException e) { System.out.println(m_res.getString("props.save.error") + ": " + e.toString()); } 
    System.out.println("koniec");
    }
  }
