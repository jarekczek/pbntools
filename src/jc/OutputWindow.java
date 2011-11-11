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
  protected CloseAction m_closeAction;
  protected StopAction m_stopAction;
  protected StringBuffer m_sb;
  protected Client m_cli;
  protected Thread m_thr;
  protected boolean m_bStop;
  
  protected ResourceBundle m_res;

  public OutputWindow(Dialog parent, Client cli, ResourceBundle res)
  {
    super(parent, false);
    m_res = res;
    m_sb = new StringBuffer();
    m_bStop = false;
    m_cli = cli;
    m_cli.setOutputWindow(this);
    m_thr = new Thread(m_cli);
    m_thr.setName("OutputWindow-client");

    // standard dialog startup code
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    GroupLayout lay = new GroupLayout(getContentPane());
    lay.setAutoCreateGaps(true);
    lay.setAutoCreateContainerGaps(true);
    setLayout(lay);
    
    JButton pbClose = new JButton();
    m_closeAction = new CloseAction("close"); 
    pbClose.setAction(m_closeAction);
    m_closeAction.setEnabled(false);
    
    JButton pbStop = new JButton();
    m_stopAction = new StopAction("stop"); 
    pbStop.setAction(m_stopAction);
    m_stopAction.setEnabled(true);
    
    m_ebOut = new javax.swing.JTextArea();
    m_ebOut.setColumns(80);
    m_ebOut.setRows(25);
    JScrollPane scrollPane = new javax.swing.JScrollPane();
    scrollPane.setViewportView(m_ebOut);

    lay.setVerticalGroup(
      lay.createSequentialGroup()
        .addComponent(scrollPane)
        .addGroup(lay.createParallelGroup()
          .addComponent(pbClose)
          .addComponent(pbStop)
        )
    );
    lay.setHorizontalGroup(
      lay.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
        .addComponent(scrollPane)
        .addGroup(lay.createSequentialGroup()
          .addComponent(pbStop)
          .addComponent(pbClose)
        )
    );
    
    
    pack();
    m_thr.start();
  }
  
  /** Adds a line of text */
  public void addLine(String s)
  {
    m_sb.append(s);
    m_sb.append('\n');
    m_ebOut.setText(new String(m_sb));
    m_ebOut.setCaretPosition(m_sb.length());
  }
  
  public boolean isStopped() { return m_bStop; }
  
  class CloseAction extends MyAction {
    CloseAction(String s) { super(s); }
    public void actionPerformed(java.awt.event.ActionEvent e) {
      dispose();
    }
  }
 
  class StopAction extends MyAction {
    StopAction(String s) { super(s); }
    public void actionPerformed(java.awt.event.ActionEvent e) {
      m_bStop = true;
      setEnabled(false);
    }
  }
 
  public static abstract class Client implements Runnable
  {
    public void setOutputWindow(OutputWindow ow) {};
  }
  
  public void threadFinished()
  {
    m_closeAction.setEnabled(true);
    m_stopAction.setEnabled(false);
  }
}
