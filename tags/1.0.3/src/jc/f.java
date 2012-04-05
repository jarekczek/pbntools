/* *****************************************************************************

    Copyright (C) 2011-2012 Jaroslaw Czekalski - jarekczek@poczta.onet.pl
    compareString: Copyright (C) 2001 Slava Pestov

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

import java.awt.Component;
import java.io.File;
import java.net.URI;
import java.util.Locale;
import java.util.ResourceBundle;
import javax.swing.JOptionPane;

public class f {
  public static String sLf;
  public static String sDirSep;
  public static int nDebugLevel = 0;
  private static ResourceBundle m_res;
  
  static {
    sLf = System.getProperty("line.separator");
    sDirSep = System.getProperty("file.separator");
    m_res = ResourceBundle.getBundle("jc.f", Locale.getDefault());

    String sDebugLevel = System.getProperty("debug.level");
    if (sDebugLevel != null) {
      try {
        nDebugLevel = Integer.parseInt(sDebugLevel);
      } catch (NumberFormatException nfe) {
        nDebugLevel = 1;
      }
    }
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
  
  /** Output text if current debug level is suitable */
  public static void trace(int nLevel, String s) {
    if (nLevel <= nDebugLevel) {
      System.out.println(s);
    }
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

  public static int desktopBrowse(Component parent, String s) {
    try {
      java.awt.Desktop.getDesktop().browse(new URI(s));
      return 0;
    }
    catch (Exception e) {
      msgException(parent, e); 
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
  
  //{{{ compareStrings method
  /**
   * Compares two strings. Copied from jedit: org.gjt.sp.StandardUtilities.
   * Last modified in r3881 in org.gjt.sp.jedit.MiscUtilities.
   * Author: Slava Pestov.<p>
   *
   * Unlike <function>String.compareTo()</function>,
   * this method correctly recognizes and handles embedded numbers.
   * For example, it places "My file 2" before "My file 10".<p>
   *
   * @param str1 The first string
   * @param str2 The second string
   * @param ignoreCase If true, case will be ignored
   * @return negative If str1 &lt; str2, 0 if both are the same,
   * positive if str1 &gt; str2
   * @since jEdit 4.3pre5
   */
  public static int compareStrings(String str1, String str2, boolean ignoreCase)
  {
    char[] char1 = str1.toCharArray();
    char[] char2 = str2.toCharArray();

    int len = Math.min(char1.length,char2.length);

    for(int i = 0, j = 0; i < len && j < len; i++, j++)
    {
      char ch1 = char1[i];
      char ch2 = char2[j];
      if(Character.isDigit(ch1) && Character.isDigit(ch2)
        && ch1 != '0' && ch2 != '0')
      {
        int _i = i + 1;
        int _j = j + 1;

        for(; _i < char1.length; _i++)
        {
          if(!Character.isDigit(char1[_i]))
          {
            //_i--;
            break;
          }
        }

        for(; _j < char2.length; _j++)
        {
          if(!Character.isDigit(char2[_j]))
          {
            //_j--;
            break;
          }
        }

        int len1 = _i - i;
        int len2 = _j - j;
        if(len1 > len2)
          return 1;
        else if(len1 < len2)
          return -1;
        else
        {
          for(int k = 0; k < len1; k++)
          {
            ch1 = char1[i + k];
            ch2 = char2[j + k];
            if(ch1 != ch2)
              return ch1 - ch2;
          }
        }

        i = _i - 1;
        j = _j - 1;
      }
      else
      {
        if(ignoreCase)
        {
          ch1 = Character.toLowerCase(ch1);
          ch2 = Character.toLowerCase(ch2);
        }

        if(ch1 != ch2)
          return ch1 - ch2;
      }
    }

    return char1.length - char2.length;
  } //}}}
}
