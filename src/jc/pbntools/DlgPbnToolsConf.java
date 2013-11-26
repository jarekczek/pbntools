/* *****************************************************************************

    Copyright (C) 2011-13 Jaroslaw Czekalski - jarekczek@poczta.onet.pl

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

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import jc.f;
import jc.MyAction;

/**
 *
 * @author Administrator
 */
public class DlgPbnToolsConf extends javax.swing.JDialog {
  public static ResourceBundle m_res;
  protected JTextField m_ebWorkDir;
  protected JTextField m_ebZbarcamOpts;
  protected JTextField m_ebUserAgent;
  protected JTextField m_ebDelay;

  //{{{ verifiers
  public class DelayVerifier extends InputVerifier {
    @Override
    public boolean verify(JComponent ctrl)
    {
      int nVal = f.str2Int(((JTextField)ctrl).getText(), 0);
      return nVal > 0 && nVal <= 1000;
    }
  }
  //}}}

  public DlgPbnToolsConf(javax.swing.JFrame parent, boolean modal) {
    super(parent, PbnTools.m_res.getString("configDlg.title"), modal);
    m_res = PbnTools.m_res;
    initComponents();
  }
  
  void initComponents() {

    PbnTools.setWindowIcons(this);
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

    GroupLayout lay = new GroupLayout(getContentPane());
    lay.setAutoCreateGaps(true);
    lay.setAutoCreateContainerGaps(true);
    setLayout(lay);
    JButton pbSave = new JButton();
    JButton pbCancel = new JButton();
    pbCancel.setAction(new CancelAction(PbnTools.getRes(), "cancel"));
    pbSave.setAction(new SaveAction(PbnTools.getRes(), "save"));
    
    JLabel stWorkDir = new JLabel();
    f.setTextAndMnem(stWorkDir, PbnTools.getRes(), "config.workDir");
    m_ebWorkDir = new JTextField();
    m_ebWorkDir.setColumns(40);
    stWorkDir.setLabelFor(m_ebWorkDir);
    JButton pbWorkDir = new JButton("...");
    pbWorkDir.setAction(new f.SelectDirAction(m_ebWorkDir));
    m_ebWorkDir.setText(PbnTools.m_props.getProperty("workDir"));
    
    JLabel stZbarcamOpts = new JLabel();
    f.setTextAndMnem(stZbarcamOpts, PbnTools.getRes(), "config.zbarcamOpts");
    stZbarcamOpts.setText(String.format(stZbarcamOpts.getText(), "/dev/video1"));
    m_ebZbarcamOpts = new JTextField();
    m_ebZbarcamOpts.setColumns(40);
    stZbarcamOpts.setLabelFor(m_ebZbarcamOpts);
    m_ebZbarcamOpts.setText(PbnTools.m_props.getProperty("zbarcamOpts"));
    
    JLabel stUserAgent = new JLabel();
    f.setTextAndMnem(stUserAgent, PbnTools.getRes(), "config.userAgent");
    m_ebUserAgent = new JTextField();
    m_ebUserAgent.setColumns(40);
    stUserAgent.setLabelFor(m_ebUserAgent);
    m_ebUserAgent.setText(PbnTools.m_props.getProperty("userAgent"));

    JLabel stDelay = new JLabel();
    f.setTextAndMnem(stDelay, PbnTools.getRes(), "config.delay");
    m_ebDelay = new JTextField();
    m_ebDelay.setColumns(40);
    stDelay.setLabelFor(m_ebDelay);
    m_ebDelay.setText(String.valueOf(PbnTools.m_nDelay));
    m_ebDelay.setInputVerifier(new DelayVerifier());

    lay.setVerticalGroup(
      lay.createSequentialGroup()
        .addComponent(stWorkDir)
        .addGroup(lay.createParallelGroup()
          .addComponent(m_ebWorkDir)
          .addComponent(pbWorkDir)
        )
        .addComponent(stZbarcamOpts)
        .addComponent(m_ebZbarcamOpts)
        .addComponent(stUserAgent)
        .addComponent(m_ebUserAgent)
        .addComponent(stDelay)
        .addComponent(m_ebDelay)
        .addGroup(
          lay.createParallelGroup()
            .addComponent(pbSave)
            .addComponent(pbCancel)
          )
    );
    lay.setHorizontalGroup(
      lay.createParallelGroup(Alignment.CENTER)
        .addGroup(lay.createParallelGroup()
          .addComponent(stWorkDir)
          .addGroup(lay.createSequentialGroup()
            .addComponent(m_ebWorkDir)
            .addComponent(pbWorkDir)
          )
        .addGroup(lay.createParallelGroup()
          .addComponent(stZbarcamOpts)
          .addComponent(m_ebZbarcamOpts)
          )
        .addGroup(lay.createParallelGroup()
          .addComponent(stUserAgent)
          .addComponent(m_ebUserAgent)
          )
        .addGroup(lay.createParallelGroup()
          .addComponent(stDelay)
          .addComponent(m_ebDelay)
          )
        )
        .addGroup(lay.createSequentialGroup()
          .addComponent(pbSave)
          .addComponent(pbCancel)
        )
    );
    
    
    pack();
  }
  
  boolean verifyData() {
    if (!m_ebDelay.getInputVerifier().verify(m_ebDelay)) {
      f.msg(PbnTools.m_res.getString("config.delay.invalid"));
      return false;
    }
    return true;
  }
  
  class CancelAction extends MyAction {
    CancelAction(ResourceBundle res, String s) { super(res, s); }
    public void actionPerformed(ActionEvent e) {
      dispose();
    }
  }
    
  class SaveAction extends MyAction {
    SaveAction(ResourceBundle res, String s) { super(res, s); }
    public void actionPerformed(ActionEvent e) {
      if (!verifyData()) { return; }
      PbnTools.m_props.setProperty("workDir", m_ebWorkDir.getText());
      PbnTools.m_props.setProperty("zbarcamOpts", m_ebZbarcamOpts.getText());
      PbnTools.m_props.setProperty("userAgent", m_ebUserAgent.getText());
      PbnTools.m_props.setProperty("delay", m_ebDelay.getText());
      PbnTools.propertiesChanged();
      dispose();
    }
  }
  
}
