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

package jc;

import java.awt.Dialog;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener; 
import java.io.*;
import java.util.*;
import javax.swing.*;
import jc.f;
import jc.MyAction;

/** General purpose window which serves as output for longer processes.
    <code>OutputWindow</code> object is created through a static method
    <code>create()</code>.
  */

public class OutputWindow extends JDialog {
  
  protected JTextArea m_ebOut;
  protected StringBuffer m_sb;
  protected OutputWindowThread m_thr;
  protected boolean m_bClosed;
  
  protected ResourceBundle m_res;

  /** Default constructor is disabled. Use <code>create</code>
      method to instantiate the object. 
    */
  private OutputWindow() {}
  
  protected OutputWindow(Dialog parent, ResourceBundle res)
  {
    super(parent, false);
    m_res = res;
    m_sb = new StringBuffer();
    m_bClosed = false;
    addWindowListener(new WindowListener());

    // standard dialog startup code
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    GroupLayout lay = new GroupLayout(getContentPane());
    lay.setAutoCreateGaps(true);
    lay.setAutoCreateContainerGaps(true);
    setLayout(lay);
    
    JButton pbClose = new JButton();
    pbClose.setAction(new CloseAction("close"));
    
    m_ebOut = new javax.swing.JTextArea();
    m_ebOut.setColumns(80);
    m_ebOut.setRows(25);

    lay.setVerticalGroup(
      lay.createSequentialGroup()
        .addComponent(m_ebOut)
        .addComponent(pbClose)
    );
    lay.setHorizontalGroup(
      lay.createParallelGroup()
        .addComponent(m_ebOut)
        .addComponent(pbClose)
    );
    
    
    pack();
  }
  
  /** Create a new <code>OutputWindow</code> instance. */
  public static OutputWindow create(Dialog parent, ResourceBundle res)
  {
    OutputWindow ow = new OutputWindow(parent, res);
    return ow;
  }

  /** Show window and return to caller (not blocking) */
  public void showWindow()
  {
    m_thr = new OutputWindowThread();
    m_thr.start();
  }
  
  /** Dispose dialog */
  public void close()
  {
    dispose();
  }
  
  /** Adds a line of text */
  public void addLine(String s)
  {
    m_sb.append(s);
    m_sb.append('\n');
    m_ebOut.setText(new String(m_sb));
    m_ebOut.setCaretPosition(m_sb.length());
  }
  
  class CloseAction extends MyAction {
    CloseAction(String s) { super(s); }
    public void actionPerformed(java.awt.event.ActionEvent e) {
      dispose();
    }
  }
 
  protected class WindowListener extends WindowAdapter
  {
    public void windowStateChanged(WindowEvent e)
    {
      f.out(e.toString());
    }
  }
  
  protected class OutputWindowThread extends Thread 
  {
    public void run()
    {
      setName("output window");
      f.out("thread started");
      OutputWindow.this.setVisible(true);
      f.out("thread finishing");
    }
  }
}
