/* *****************************************************************************

Copyright (C) 2011-2012 Jaroslaw Czekalski - jarekczek@poczta.onet.pl

jedit settings: :folding=explicit:indentSize=2:noTabs=true:collapseFolds=1:

The MIT License (MIT)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

*****************************************************************************
*/

package jc;

import java.awt.Component;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import javax.swing.JOptionPane;

public class f {
  public static String sLf;
  public static String sDirSep;
  public static int nDebugLevel = 0;
  private static ResourceBundle m_res;
  
  static { //{{{
    sLf = System.getProperty("line.separator");
    sDirSep = System.getProperty("file.separator");
    m_res = ResourceBundle.getBundle("jc.f", Locale.getDefault());

    String sDebugLevel = System.getProperty("debug.level");
    if (sDebugLevel == null)
      sDebugLevel = System.getProperty("jc.debug");
    setDebugLevel(sDebugLevel);
  } //}}}

  // setDebugLevel methods {{{
  /** Sets debug level.<p>
   * If the level is not set explicitly, <code>f</code> tries to parse
   * properties <code>debug.level</code> and <code>jc.debug</code>
   * and sets the level appropriately (at class static startup).
   * @param sNewLevel If not number, 1 is assumed.
   * @return Old level.
   */
  public static int setDebugLevel(String sNewLevel)
  {
    int nOldLevel = nDebugLevel;
    if (sNewLevel != null) {
      try {
        nDebugLevel = Integer.parseInt(sNewLevel);
      } catch (NumberFormatException nfe) {
        nDebugLevel = 1;
      }
    }
    return nOldLevel;
  }

  public static int setDebugLevel(int nNew)
  {
    int nOld = nDebugLevel;
    nDebugLevel = nNew;
    return nOld;
  }
  // }}}
  
  public static int getDebugLevel() { return nDebugLevel; }
    
  public static boolean stringIn(String s, String as[]) { //{{{
    int i;
    for (i=0; i<as.length; i++) { if (s.equals(as[i])) return true; }
    return false;
  } //}}}
    
  public static String getResStr(Object o, String sKey) { //{{{
    return java.util.ResourceBundle.getBundle(o.getClass().getName()).getString(sKey);
  } //}}}
    
  public static void out(String s) { //{{{
    System.out.println(s);
  } //}}}
  
  // trace method {{{
  /** Output text if current debug level is suitable */
  public static void trace(int nLevel, String s) {
    if (nLevel <= nDebugLevel) {
      System.out.println(s);
    }
  } //}}}

  public static void err(String s) { //{{{
    System.err.println(s);
  } //}}}

  public static boolean isNullOrEmpty(Object o)
  {
    if (o == null)
      return true;
    if (o instanceof String)
      return ((String)o).isEmpty();
    else
      return false;
  }

  public static boolean isDebugMode() { //{{{
    return nDebugLevel > 0;
  } //}}}

  public static String basePath(Class c) { //{{{
    String sPath = c.getResource("").getPath();
    if (sPath.startsWith("file:/"))
      sPath = sPath.substring(5);
    sPath = sPath.replaceFirst("!.*$", "");
    File file = new File(decodeUrl(sPath));
    return file.getParentFile().getAbsolutePath();
  } //}}}

  public static int desktopOpen(String s) { //{{{
    try {
      java.awt.Desktop.getDesktop().open(new File(s));
      return 0;
    }
    catch (java.io.IOException e) {
      javax.swing.JOptionPane.showMessageDialog(null, e.toString());
      return -1;
    }
  } //}}}

  public static int desktopBrowse(Component parent, String s) { //{{{
    try {
      java.awt.Desktop.getDesktop().browse(new URI(s));
      return 0;
    }
    catch (Exception e) {
      msgException(parent, e); 
      return -1;
    }
  } //}}}

  public static String toSpacedString(Object a[]) { //{{{
    StringBuffer sb = new StringBuffer();
    if (a!=null && a.length>0) {
      sb.append(a[0]);
      for (int i=1; i<a.length; i++) {
        sb.append(' ');
        sb.append(a[i]);
      }
    }
    return new String(sb);
  } //}}}

  //{{{ swing methods
  public static void msg(String sMsg) {
    javax.swing.JOptionPane.showMessageDialog(null, sMsg);
  }
  
  public static void msgException(Component parent, Throwable t) {
    JOptionPane.showMessageDialog(parent, t.toString(),
      m_res.getString("exception.title"), JOptionPane.ERROR_MESSAGE);
  }
  
  public static String[] extractTextAndMnem(ResourceBundle res,
                                            String sPropName) {
    String sText = res.getString(sPropName);
    String as[];
    String asRet[] = new String[2];
    asRet[0] = sText;
    asRet[1] = null;
    as = sText.split("\\|");
    if (as.length>1) {
      asRet[0] = sText.substring(0, sText.length()-2);
      asRet[1] = as[as.length-1];
    }
    return asRet;
  }
  
  public static void setTextAndMnem(javax.swing.AbstractButton but,
                                    ResourceBundle res,
                                    String sPropName) {
    String as[] = extractTextAndMnem(res, sPropName);
    but.setText(as[0]);
    if (as[1]!=null) { but.setMnemonic(as[1].charAt(0)); }
  }

  public static void setTextAndMnem(javax.swing.JLabel lab,
                                    ResourceBundle res,
                                    String sPropName) {
    String as[] = extractTextAndMnem(res, sPropName);
    lab.setText(as[0]+":");
    if (as[1]!=null) { lab.setDisplayedMnemonic(as[1].charAt(0)); }
  }

  public static void setTextAndMnem(javax.swing.AbstractAction a,
                                    ResourceBundle res,
                                    String sPropName) {
    String as[] = extractTextAndMnem(res, sPropName);
    a.putValue(a.NAME, as[0]);
    if (as[1]!=null) {
      javax.swing.JButton but = new javax.swing.JButton();
      but.setMnemonic(as[1].charAt(0));
      a.putValue(a.MNEMONIC_KEY, but.getMnemonic());
    }
  }

  public static class SelectDirAction extends javax.swing.AbstractAction {
    javax.swing.text.JTextComponent m_eb;
    public SelectDirAction(javax.swing.text.JTextComponent eb) {
      super("...");
      m_eb = eb;
    }

    public void actionPerformed(java.awt.event.ActionEvent e) {
      javax.swing.JFileChooser fc = new javax.swing.JFileChooser(m_eb.getText());
      fc.setDialogType(fc.OPEN_DIALOG);
      fc.setFileSelectionMode(fc.DIRECTORIES_ONLY);
      fc.setDialogTitle(m_res.getString("chooseDir"));
      int rv = fc.showOpenDialog(null);
      if (rv==fc.APPROVE_OPTION) {
        m_eb.setText(fc.getSelectedFile().getAbsolutePath());
      }
    }
  }

  public static class OpenFileAction extends javax.swing.AbstractAction {
    javax.swing.text.JTextComponent m_eb;
    public OpenFileAction(javax.swing.text.JTextComponent eb) {
      super("...");
      m_eb = eb;
    }

    public void actionPerformed(java.awt.event.ActionEvent e) {
      javax.swing.JFileChooser fc = new javax.swing.JFileChooser(m_eb.getText());
      fc.setDialogType(fc.OPEN_DIALOG);
      fc.setFileSelectionMode(fc.FILES_ONLY );
      fc.setDialogTitle(m_res.getString("chooseFileOpen"));
      int rv = fc.showOpenDialog(null);
      if (rv==fc.APPROVE_OPTION) {
        m_eb.setText(fc.getSelectedFile().getAbsolutePath());
      }
    }
  }

  //}}}

  //{{{ getIntProp method
  public static int getIntProp(Properties props, String sProp, int nDefault)
  {
    String sValue = props.getProperty(sProp);
    if (sValue == null) {
      sValue = System.getProperty(sProp);
    }
    int nValue = nDefault;
    if (sValue != null) {
      try {
        nValue = Integer.parseInt(sValue);
      } catch (NumberFormatException nfe) {}
    }
    return nValue;
  }
  //}}}

  // file and path string operations {{{

  // getFileName(String) method {{{
  /** Extracts the file name (stripping the path) from a link/file.
    * Both slashes are treated as path separators. */
  public static String getFileName(String sPath)
  {
    return sPath.replaceFirst("^.*[/\\\\]", "");
  } //}}}
  
  // getFileExt(String) method {{{
  /** Returns the extension of the file, the part after the last dot.
    * @return <code>null</code> if there is no extension. */
  public static String getFileExt(String sPath)
  {
    String sExt = sPath.replaceFirst("^.*\\.([^\\./\\\\]*)$", "$1");
    if (sExt.equals(sPath)) {
      // no extension was actually found
      return null;
    }
    else
      return sExt;
  } //}}}
  
  // getFileNameNoExt(String) method {{{
  /** Returns the file name, stripping the path and the extension */
  public static String getFileNameNoExt(String sPath)
  {
    // In case of urls like .php&id=xxx - cut only the alphanumeric part.
    // To be more precise: cut the part until the first special char.
    // If no special char, cut from first dot to end.
    String sFileName = getFileName(sPath)
        .replaceFirst("\\.((([^&?]*(?=[?&])))|([^\\.]*$))", "");
    return sFileName.replaceAll("[?&]", "_");
  } //}}}
  
  // getDirOfFile(String) method {{{
  /** Returns the directory name, with the trailing slash */
  public static String getDirOfFile(String sPath)
  {
    return sPath.replaceFirst("([/\\\\])[^/\\\\]+$", "$1");
  } //}}}
  
  // }}} file and path operations

  // I/O functions //{{{
  
  // String readFile(String) method {{{
  /** Reads file contents and returns string */
  public static String readFile(String sFile)
    throws java.io.FileNotFoundException, java.io.IOException
  {
    BufferedReader br = new BufferedReader(
      new InputStreamReader(new FileInputStream(sFile), "ISO-8859-1"));
    int ch;
    StringBuilder sb = new StringBuilder();
    while ((ch = br.read()) >= 0)
      sb.append((char)ch);
    br.close();
    return sb.toString();
  } //}}}

  // copyStream method //{{{
  /** Copies streams. Always closes both streams.
    * @throws java.io.IOException */
  public static void copyStream(InputStream is, OutputStream os)
    throws java.io.IOException
  {
    try {
      int b;
      while (true) {
        b = is.read();
        if (b < 0)
          break;
        os.write(b);
      }
    }
    finally {
      try {
        is.close();
      }
      finally {
        os.close();
      }
    }
  } //}}}
  
  // saveUrlAsFile method //{{{
  public static void saveUrlAsFile(String sUrl, File file)
    throws java.net.MalformedURLException, java.io.IOException
  {
    URL url = new URL(sUrl);
    // may throw ioe and return
    InputStream is = url.openStream();
    OutputStream os = null;
    try {
      os = new FileOutputStream(file);
    }
    catch (java.io.IOException ioe1) {
      is.close();
      throw ioe1;
    }
    copyStream(is, os);
  } //}}}

  // writeToFile method //{{{
  public static void writeToFile(String sWhat, File file)
    throws java.io.IOException
  {
    Writer w = null;
    try {
    w = new OutputStreamWriter(
      new FileOutputStream(file), "ISO-8859-1");
    }
    catch (java.io.UnsupportedEncodingException uee) {
      throw new RuntimeException(uee);
    }
    catch (java.io.FileNotFoundException fnfe) {
      throw new java.io.IOException(fnfe);
    }
    try {
      w.write(sWhat);
    }
    finally {
      w.close();
    }
  } //}}}
  
   //}}} IO functions
  
  // sleepUnint method  //{{{
  /**
   * Uninterruptable sleep. The implementation is not perfect.
   * If interrupted once, it may sleep up to 2*milis.
   * If interrupted twice, it may not sleep at all.
   */
  public static void sleepUnint(long milis)
  {
    try {
      Thread.sleep(milis);
    }
    catch (InterruptedException ie) {
      // let's try once more, maybe this time it won't interrupt
      try {
        Thread.sleep(milis);
      }
      catch (InterruptedException ie2) {}
      Thread.currentThread().interrupt();
    }
  } //}}}

  // str2Int method  //{{{
  /**
   * Converts string to int, using a default in case of error.
   */
  public static int str2Int(String s, int nDefault)
  {
    int nRes = nDefault;
    try {
      nRes = Integer.parseInt(s);
    } catch (Exception e) {}
    return nRes;
  } //}}}

  // decodeUrl methods //{{{
  /**
   * Uses URLDecoder to convert percent escapes to regular characters.
   */
  public static String decodeUrl(String sUrl)
  {
    try {
      return URLDecoder.decode(sUrl, "UTF-8");
    }
    catch (java.io.UnsupportedEncodingException e) {
      // Actually it is not thrown, even with incorrect encoding name
      throw new RuntimeException(e);
    }
  }

  /**
   * Uses URLDecoder to convert percent escapes to regular characters,
   * but leaves reserved chars encoded, i.e. ?
   */
  public static String decodeUrlRes(String sUrl)
  {
    String sRes = sUrl;
    try {
      sRes = URLDecoder.decode(sUrl, "UTF-8");
    }
    catch (java.io.UnsupportedEncodingException e) {
      // Actually it is not thrown, even with incorrect encoding name
      throw new RuntimeException(e);
    }
    sRes = sRes.replaceAll("\\?", "%3F");
    return sRes;
  } //}}}
}
