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
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.awt.Component;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import jc.f;
import jc.SoupProxy;
import jc.outputwindow.DialogOutputWindow;
import jc.outputwindow.StandardOutputWindow;
import jc.pbntools.*;
import jc.pbntools.download.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class PbnTools {
  static String m_sCurDir;
  static String m_sScriptDir;
  static String m_sBinDir;
  public static boolean bLinux;
  public static boolean bWindows;
  static DlgPbnToolsMain m_dlgMain;
  static String m_sSlash;
  public static ResourceBundle m_res;
  private static String m_sPropsFile;
  static Properties m_props;
  static boolean m_bPropsRead;
  private static boolean m_bVerbose;
  static boolean m_bRunMainDialog;
  
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
    bWindows = System.getProperty("os.name").startsWith("Windows");
    m_bVerbose = false;
    f.trace(1, "curDir=" + m_sCurDir + ", binDir=" + m_sBinDir
            + ", scriptDir=" + m_sScriptDir);

    if (m_sCurDir.indexOf(' ') >= 0) {
      f.msg(m_res.getString("error.installedWithSpaces"));
    }
    //    JOptionPane.showMessageDialog(null, m_res.getString("test") + System.getProperties().getProperty("jarek.wersja"));
    }

  public static String getStr(String sPropName)
  {
    return m_res.getString(sPropName);
  }
    
  public static String getStr(String sPropName, Object... ao)
  {
    return java.text.MessageFormat.format(m_res.getString(sPropName), ao);
  }

  public static ResourceBundle getRes()
  {
    return m_res;
  }

  /** Returns verbosity level: 0..1 */
  public static int getVerbos()
  {
    return m_bVerbose ? 1 : 0;
  }
  
  /** Sets the verbosity level.
   *  @return Previous value
   */
  public static int setVerbos(int nVerb)
  {
    int nOldVerb = getVerbos();
    m_bVerbose = (nVerb != 0);
    return nOldVerb;
  }

  public static String getWorkDir()
  {
    String sWorkDir = PbnTools.m_props.getProperty("workDir");
    if (sWorkDir==null || sWorkDir.length()==0) {
      f.msg(PbnTools.m_res.getString("error.noWorkDir"));
      return null;
    }
    return sWorkDir;
  }
  
  /** Get our bin subdirectory path, with trailing slash added */
  public static String getBin()
  {
    return m_sCurDir + m_sSlash + "bin" + m_sSlash;
  }
  
  /** Get our wget full pathname */
  public static String getWgetPath()
  {
    String sWget = m_sCurDir + m_sSlash + "bin" + m_sSlash + "wget"
                   + m_sSlash + "wget";
    if (bWindows)
      sWget += ".exe";
    return sWget;
  }
  
  public static void checkUpdates(Component parent)
  {
    String sHomePl = "http://jarek.katowice.pl/pbntools";
    String sCurrentVer = getStr("wersja");
    String sHtmlVer = null;
    try {
      sHtmlVer = PbnTools.getVersionFromUrl(sHomePl);
    } catch (jc.SoupProxy.Exception spe) {
      f.msgException(parent, spe);
      return;
    }
    if (sHtmlVer == null) {
      JOptionPane.showMessageDialog(parent, getStr("checkUpd.unknownVersion"), 
        getStr("checkUpd.title"), JOptionPane.WARNING_MESSAGE);
      return;
    }
    if (f.compareStrings(sCurrentVer, sHtmlVer, true) < 0) {
      Object options[] = { getStr("download.label"),
                           getStr("cancel.label") };
      int rv = JOptionPane.showOptionDialog(parent,
        getStr("checkUpd.lower", sCurrentVer, sHtmlVer),
        getStr("checkUpd.title"), JOptionPane.DEFAULT_OPTION,
        JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
      if (rv == 0) {
        browseInstallPage(parent);
      }
    }
    else if (f.compareStrings(sCurrentVer, sHtmlVer, true) > 0) {
      JOptionPane.showMessageDialog(parent,
        getStr("checkUpd.greater", sCurrentVer, sHtmlVer),
        getStr("checkUpd.title"), JOptionPane.PLAIN_MESSAGE);
    }
    else {
      JOptionPane.showMessageDialog(parent,
        getStr("checkUpd.equal", sCurrentVer),
        getStr("checkUpd.title"), JOptionPane.PLAIN_MESSAGE);
    }
  }
  
  public static void browseInstallPage(Component parent)
  {
    f.desktopBrowse(parent, getStr("homepage") + "#download");
  }
  
  static void pobierzKops(String sLink) {
    int rv;
    String sWorkDir = getWorkDir();
    if (sWorkDir==null) { return; }
    // msys needs converting all path separators from \ to /
    String sScript = (m_sScriptDir + m_sSlash + "get_tur_kops.sh").replaceAll("\\\\", "/");
    String asArgs[] = { "-c",
                        sScript
                        + " -d \"" + sWorkDir.replaceAll("\\\\", "/") + "\" "
                        + sLink };
    if (bLinux) {
      rv = RunProcess.runCmd(m_dlgMain, "bash", asArgs, m_sScriptDir);
      }
    else {
      String sMsysBin = m_sCurDir + m_sSlash + "bin" + m_sSlash + "msys" + m_sSlash + "bin";
      String asPaths[] = { sMsysBin,
                           m_sCurDir + m_sSlash + "bin" + m_sSlash + "wget" };
      String sBash = sMsysBin + m_sSlash + "bash";
      rv = RunProcess.runCmd(m_dlgMain, sBash, asArgs, m_sScriptDir, asPaths);
      }
  }
  
  static void pobierzPary(String sLink, boolean bGui) {
    if (getWorkDir()==null) { return; }
    TourDownloaderThread thr = new TourDownloaderThread(sLink, new ParyTourDownloader());
    if (bGui) {
      DialogOutputWindow ow =  new DialogOutputWindow(m_dlgMain, thr, m_res);
      ow.setVisible(true);
    } else {
      StandardOutputWindow ow =  new StandardOutputWindow(thr, m_res);
    }
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

  /** Reads version string from given <code>sUrl</code>. This should be
    * an url of pbntools web page, in Polish.
    * @return <code>null</code> on error. */  
  public static String getVersionFromUrl(String sUrl)
                throws jc.SoupProxy.Exception
  {
    SoupProxy sp = new SoupProxy(); 
    Document doc = sp.getDocument(sUrl);
    Pattern pat = Pattern.compile("Aktualna wersja. ([0-9\\.]+)[,:].*");
    for (Element e : doc.body().getElementsMatchingOwnText("Aktualna wersja")) {
      Matcher m = pat.matcher(e.text());
      if (m.matches()) {
        return m.group(1);
      }
    }
    return null;
  }

  static void printUsage() {
    f.out(m_res.getString("usage.1"));
  }
  
    
  static void parseCommandLine(String args[]) {
    m_bRunMainDialog = true;
    for (int i=0; i<args.length; i++) {
      if (args[i].equals("--debug")) {
        System.setProperty("jc.debug", "1");
      }
      if (args[i].equals("--verbose")) {
        setVerbos(1);
      }
      if (args[i].equals("-h") || args[i].equals("--help") || args[i].equals("/?")) {
        printUsage();
        System.exit(0);
      }
      if (args[i].equals("-dt")) {
        m_bRunMainDialog = false;
        ++i;
        if (i >= args.length) { System.err.println(getStr("error.missingArg")); System.exit(1); }
        pobierzPary(args[i], false);
      }
    }
  }
    
  /** Return values:
    * <dl>
    * <dt>1 - wrong command line arguments</dt>
    * <dt>128 - interrupted by user</dt>
    * </dl>
    */
  public static void main(String args[])
  {
    m_sPropsFile = System.getProperty("user.home") + System.getProperty("file.separator") + "PbnTools.props";
    try {
      m_props.load(new FileReader(m_sPropsFile));
      m_bPropsRead = true;
      f.trace(1, "Properties read from file " + m_sPropsFile);
    }
    catch (java.io.FileNotFoundException e) { m_bPropsRead = true; }
    catch (IOException e) { System.out.println(m_res.getString("props.load.error") + ": " + e.toString()); }
    setVerbos(f.getIntProp(m_props, "verbosity", 0));
    
    parseCommandLine(args);

    if (m_bRunMainDialog) {
      SwingUtilities.invokeLater(new Runnable() { public void run() {
        m_dlgMain = new DlgPbnToolsMain();
        m_dlgMain.setVisible(true);
      }});
    }

    }
    
  public static void closeDown()
  {
    try {
      if (m_bPropsRead) {
        m_props.store(new FileWriter(m_sPropsFile), null);
        f.trace(1, "Properties stored in file " + m_sPropsFile);
      }
    }
    catch (IOException e) { System.out.println(m_res.getString("props.save.error") + ": " + e.toString()); } 
    System.out.println("koniec");
  }

}
