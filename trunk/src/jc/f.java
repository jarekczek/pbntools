/* *****************************************************************************

    Copyright (C) 2011 Jaroslaw Czekalski - jarekczek@poczta.onet.pl

    jedit settings: :folding=explicit:indentSize=2:noTabs=true:

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

package jc;

import java.io.File;
import jc.pbntools.PbnTools;

public class f {
  public static String sLf;
  public static String sDirSep;
  
  static {
    sLf = System.getProperty("line.separator");
    sDirSep = System.getProperty("file.separator");
    }
    
  public static boolean stringIn(String s, String as[]) {
    int i;
    for (i=0; i<as.length; i++) { if (s.equals(as[i])) return true; }
    return false;
    }
    
  public static String getResStr(Object o, String sKey) {
    return java.util.ResourceBundle.getBundle(o.getClass().getName()).getString(sKey);
    }
    
  public static void out(String s) {
    System.out.println(s);
  }
  
  public static void err(String s) {
    System.err.println(s);
  }

  public static String basePath(Class c) {
    String sPath = c.getResource("").getPath();
    sPath = sPath.substring(5);
    sPath = sPath.replaceFirst("!.*$", "");
    File file = new File(sPath);
    return file.getParentFile().getAbsolutePath();
  }

  public static int desktopOpen(String s) {
    try {
      java.awt.Desktop.getDesktop().open(new File(s));
      return 0;
    }
    catch (java.io.IOException e) {
      javax.swing.JOptionPane.showMessageDialog(null, e.toString());
      return -1;
    }
  }

  public static String toSpacedString(Object a[]) {
    StringBuffer sb = new StringBuffer();
    if (a!=null && a.length>0) {
      sb.append(a[0]);
      for (int i=1; i<a.length; i++) {
        sb.append(' ');
        sb.append(a[i]);
      }
    }
    return new String(sb);
  }
  
  //{{{ swing methods
  public static void msg(String sMsg) {
    javax.swing.JOptionPane.showMessageDialog(null, sMsg);
  }
  
  public static String[] extractTextAndMnem(String sPropName) {
    String sText = PbnTools.m_res.getString(sPropName);
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
  
  public static void setTextAndMnem(javax.swing.AbstractButton but, String sPropName) {
    String as[] = extractTextAndMnem(sPropName);
    but.setText(as[0]);
    if (as[1]!=null) { but.setMnemonic(as[1].charAt(0)); }
  }

  public static void setTextAndMnem(javax.swing.JLabel lab, String sPropName) {
    String as[] = extractTextAndMnem(sPropName);
    lab.setText(as[0]+":");
    if (as[1]!=null) { lab.setDisplayedMnemonic(as[1].charAt(0)); }
  }

  public static void setTextAndMnem(javax.swing.AbstractAction a, String sPropName) {
    String as[] = extractTextAndMnem(sPropName);
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
      fc.setDialogTitle(PbnTools.m_res.getString("chooseDir"));
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
      fc.setDialogTitle(PbnTools.m_res.getString("chooseFileOpen"));
      int rv = fc.showOpenDialog(null);
      if (rv==fc.APPROVE_OPTION) {
        m_eb.setText(fc.getSelectedFile().getAbsolutePath());
      }
    }
  }
  //}}}
  
}
