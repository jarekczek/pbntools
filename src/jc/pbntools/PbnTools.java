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
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.awt.Component;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import jc.f;
import jc.fgpl;
import jc.JCException;
import jc.SoupProxy;
import jc.outputwindow.DialogOutputWindow;
import jc.outputwindow.OutputWindow;
import jc.outputwindow.SimplePrinter;
import jc.outputwindow.StandardSimplePrinter;
import jc.pbntools.*;
import jc.pbntools.download.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class PbnTools {
  static String m_sCurDir;
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
    m_sBinDir = new File(m_sCurDir, "bin").getAbsolutePath();
    //System.getProperties().getProperty("user.dir");
    m_sSlash = System.getProperties().getProperty("file.separator");
    m_res = ResourceBundle.getBundle("jc.pbntools.PbnTools", Locale.getDefault() /* new Locale("en_US") */ );
    m_props = new Properties();
    m_bPropsRead = false;
    bLinux = System.getProperty("os.name").equals("Linux");
    bWindows = System.getProperty("os.name").startsWith("Windows");
    m_bVerbose = false;

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

  public static String getWorkDir(boolean bGui)
  {
    String sWorkDir = PbnTools.m_props.getProperty("workDir");
    if (sWorkDir==null || sWorkDir.length()==0) {
      if (bGui) {
        f.msg(PbnTools.m_res.getString("error.noWorkDir"));
      } else {
        throw new RuntimeException(
          PbnTools.m_res.getString("error.noWorkDir"));
      }
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
    if (fgpl.compareStrings(sCurrentVer, sHtmlVer, true) < 0) {
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
    else if (fgpl.compareStrings(sCurrentVer, sHtmlVer, true) > 0) {
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
  
  /** Returns all supported tournament downloaders. */
  public static HtmlTourDownloader[] getTourDownloaders()
  {
    return new HtmlTourDownloader[] {
      new KopsTourDownloader(),
      new ParyTourDownloader(),
      new BboTourDownloader()
    };
  }
  
  /** Returns all supported deal readers. */
  public static DealReader[] getDealReaders()
  {
    return new DealReader[] {
      new LinReader()
    };
  }
  
  static void pobierzKops(String sLink, boolean bGui) //{{{
  {
    if (getWorkDir(bGui) == null) { return; }
    TourDownloaderThread thr =
      new TourDownloaderThread(sLink, new KopsTourDownloader());
    if (bGui) {
      DialogOutputWindow ow =  new DialogOutputWindow(m_dlgMain, thr, m_res);
      ow.setVisible(true);
    } else {
      thr.setOutputWindow(new StandardSimplePrinter());
      thr.run();
    }
  } //}}}

  static void pobierzPary(String sLink, boolean bGui) {
    if (getWorkDir(bGui)==null) { return; }
    TourDownloaderThread thr = new TourDownloaderThread(sLink, new ParyTourDownloader());
    if (bGui) {
      DialogOutputWindow ow =  new DialogOutputWindow(m_dlgMain, thr, m_res);
      ow.setVisible(true);
    } else {
      thr.setOutputWindow(new StandardSimplePrinter());
      thr.run();
    }
  }
    
  static void pobierzBbo(String sLink) {
    if (sLink==null) { sLink = "owm"; }
    //RunProcess.runCmd((JDialog)m_dlgMain, "sh" + (bLinux ? "" : " --login") + " "+m_sCurDir+m_sSlash+"get_tur_bbo.sh " + sLink);
    }

  // convert method {{{
  /** Converts deals from <code>sLink</code> and saves them as pbn file,
    * <code>sOutFile</code>.
    * @param sLink Url or local filename.
    * @param sOutFile May be <code>null</code>, in which case a new filename
    * is constructed in working directory. */
  static void convert(String sLink, String sOutFile0,
                      boolean bGui)
    throws DownloadFailedException
  {
    Convert cnv = new Convert(sLink, sOutFile0, bGui);
    if (bGui) {
      DialogOutputWindow ow =  new DialogOutputWindow(m_dlgMain, cnv, m_res);
      ow.setVisible(true);
    } else {
      cnv.setOutputWindow(new StandardSimplePrinter());
      cnv.run();
    }

  } //}}}
    
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
    ArrayList<String> asFileArgs = new ArrayList<String>();
    String sOutFile = null;

    for (int i=0; i<args.length; i++) {
      if (args[i].equals("--debug")) {
        System.setProperty("jc.debug", "1");
        f.setDebugLevel(1);
      } else if (args[i].equals("--verbose")) {
        setVerbos(1);
      } else if (args[i].equals("-h")
                 || args[i].equals("--help") || args[i].equals("/?")) {
        printUsage();
        System.exit(0);
      } else if (args[i].equals("-dtk")) {
        m_bRunMainDialog = false;
        ++i;
        if (i >= args.length) { System.err.println(getStr("error.missingArg")); System.exit(1); }
        pobierzKops(args[i], false);
      } else if (args[i].equals("-dtp")) {
        m_bRunMainDialog = false;
        ++i;
        if (i >= args.length) { System.err.println(getStr("error.missingArg")); System.exit(1); }
        pobierzPary(args[i], false);
      } else if (args[i].equals("-o")) {
        ++i;
        if (i >= args.length) { System.err.println(getStr("error.missingArg")); System.exit(1); }
        sOutFile = args[i];
      } else if (args[i].startsWith("-")) {
        m_bRunMainDialog = false;
        System.err.println(getStr("error.invSwitch", args[i]));
      } else {
        // non-switch argument
        asFileArgs.add(args[i]);
      }
    }
    
    if (asFileArgs.size() > 0) {
      m_bRunMainDialog = false;
      if (asFileArgs.size() > 1) {
        f.err(getStr("error.wrongArgCount", asFileArgs.size()));
      }
      try {
        convert(asFileArgs.get(0), sOutFile, false);
      } catch (JCException e) {
        if (f.isDebugMode())
          e.printStackTrace();
        else
          System.err.println(e.toString());
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
      m_props.load(new InputStreamReader(
        new FileInputStream(m_sPropsFile), "ISO-8859-1"));
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
        m_props.store(new OutputStreamWriter(
          new FileOutputStream(m_sPropsFile), "ISO-8859-1"),
          null);
        f.trace(1, "Properties stored in file " + m_sPropsFile);
      }
    }
    catch (IOException e) { System.out.println(m_res.getString("props.save.error") + ": " + e.toString()); } 
    System.out.println("koniec");
  }

  // Convert class {{{
  /** Runs converters (DealReaders) for link. */
  static class Convert extends OutputWindow.Client
  {
    private String m_sLink;
    private String m_sOutFile0;
    private boolean m_bGui;
    private SimplePrinter m_ow;
    
    public Convert(String sLink, String sOutFile0, boolean bGui)
    {
      m_sLink = sLink;
      m_sOutFile0 = sOutFile0;
      m_bGui = bGui;
    }

    public void setOutputWindow(SimplePrinter ow) {
      m_ow = ow;
    }
      
    public void run() {
      String sOutFile = m_sOutFile0;
      if (sOutFile == null) {
        String sFile = f.getFileNameNoExt(m_sLink);
        sOutFile = new File(getWorkDir(false),
                            f.getFileNameNoExt(m_sLink) + ".pbn").toString();
      }

      if (getVerbos() > 0)
        f.out(getStr("msg.converting", m_sLink, sOutFile));
      boolean bRightReader = false;
      for (DealReader dr: getDealReaders()) {
        if (f.isDebugMode())
          m_ow.addLine("Trying reader: " + dr.getClass().getName()); 
        dr.setOutputWindow(m_ow);
        if (dr.verify(m_sLink, !f.isDebugMode())) {
          bRightReader = true;
          m_ow.addLine(
            getStr("msg.readerFound", dr.getClass().getName()));
          try {
            Deal deals[] = dr.readDeals(m_sLink, false);
          }
          catch (DownloadFailedException dfe) {
            m_ow.addLine(dfe.toString());
            bRightReader = false;
          }
          break;
        }
      }
      if (!bRightReader) {
        m_ow.addLine(getStr("msg.noDealReader"));
      }
    }
  } //}}}
}
