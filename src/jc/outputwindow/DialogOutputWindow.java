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

import java.awt.Dialog;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.EventQueue;
import java.io.*;
import java.util.*;
import java.util.ResourceBundle;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import jc.f;
import jc.MyAction;

/** Subclass of {@link OutputWindow} using JDialog as the output */

public class DialogOutputWindow extends OutputWindow {
  
  protected JDialog m_dlg;
  protected JTextArea m_ebOut;
  protected CloseAction m_closeAction;
  protected StopAction m_stopAction;
  protected Document m_doc;
  /** <code>true</code> when gui update scheduled. This flag is to avoid
    * multiple overlapping updates, not to call
    * <code>invokeLater</code> too frequently.
    */
  private volatile boolean m_bUpdScheduled;
  
  public DialogOutputWindow(Window parent, Client cli, ResourceBundle res)
  {
    super(cli, res);
    m_dlg = new JDialog(parent, Dialog.ModalityType.MODELESS);

    // standard dialog startup code
    m_dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    GroupLayout lay = new GroupLayout(m_dlg.getContentPane());
    lay.setAutoCreateGaps(true);
    lay.setAutoCreateContainerGaps(true);
    m_dlg.setLayout(lay);
    
    JButton pbClose = new JButton();
    m_closeAction = new CloseAction(res, "close"); 
    pbClose.setAction(m_closeAction);
    m_closeAction.setEnabled(false);
    
    JButton pbStop = new JButton();
    m_stopAction = new StopAction(res, "stop"); 
    pbStop.setAction(m_stopAction);
    m_stopAction.setEnabled(true);
    
    m_doc = new PlainDocument();
    m_ebOut = new javax.swing.JTextArea();
    m_ebOut.setColumns(80);
    m_ebOut.setRows(25);
    m_ebOut.setDocument(m_doc);
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
    
    
    m_dlg.pack();
    runClient();
  }
  
  public void setVisible(boolean bVisible) { m_dlg.setVisible(bVisible); }
  
  /** Updates output window so that the last line be visible */
  protected void update()
  {
    if (!m_bUpdScheduled) {
      m_bUpdScheduled = true;
      EventQueue.invokeLater(new Runnable() {
          public void run() {
            m_bUpdScheduled = false;
            try {
              m_ebOut.setCaretPosition(m_doc.getLength());
            }
            catch (IllegalArgumentException iae) {
              // doc not in sync with text field - too bad
            }
          }
      });
    }
  }

  /** Adds a line of text */
  @Override
  public void addLine(String s)
  {
    addText(s + "\n");
  }
  
  /** Adds text without new line */
  @Override
  public synchronized void addText(String s)
  {
    try {
      boolean bInterrupted = Thread.interrupted();
      m_doc.insertString(m_doc.getLength(), s, null);
      if (bInterrupted)
        Thread.currentThread().interrupt();
    }
    catch (BadLocationException ble) {
      throw new RuntimeException(ble);
    }
    m_sb.append(s);
    update();
  }
  
  @Override
  public void setTitle(final String sTitle)
  {
    EventQueue.invokeLater(new Runnable() { public void run() {
        m_dlg.setTitle(sTitle);
    }});
  }
  
  class CloseAction extends MyAction {
    CloseAction(ResourceBundle res, String s) { super(res, s); }
    public void actionPerformed(java.awt.event.ActionEvent e) {
      m_dlg.dispose();
    }
  }
 
  class StopAction extends MyAction {
    StopAction(ResourceBundle res, String s) { super(res, s); }
    public void actionPerformed(java.awt.event.ActionEvent e) {
      stopClient();
      setEnabled(false);
    }
  }
 
  public void threadFinished()
  {
    m_closeAction.setEnabled(true);
    m_stopAction.setEnabled(false);
    super.threadFinished();
  }
}

