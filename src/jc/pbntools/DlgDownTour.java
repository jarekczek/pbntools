/* *****************************************************************************

    Copyright (C) 2011-2 Jaroslaw Czekalski - jarekczek@poczta.onet.pl

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

import jc.f;
import jc.pbntools.download.HtmlTourDownloader;
import javax.swing.GroupLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JRootPane;
import javax.swing.JTextField;
import javax.swing.LayoutStyle;

public class DlgDownTour extends javax.swing.JDialog {
  int rv;
  String m_sLink;

  public DlgDownTour(java.awt.Frame parent, boolean modal) {
    super(parent, modal);
    rv = 0;
    initComponents();
  }

  private void initComponents()
  {

    PbnTools.setWindowIcons(this);
    
    jLabel1 = new javax.swing.JLabel();
    ebLink = new javax.swing.JTextField();
    pbOk = new javax.swing.JButton();
    pbAnuluj = new javax.swing.JButton();
    
    setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
    setTitle(PbnTools.getStr("downTour.title"));
    
    f.setTextAndMnem(jLabel1, PbnTools.getRes(), "downTour.link.label");
    jLabel1.setLabelFor(ebLink);
    ebLink.setText(PbnTools.m_props.getProperty("downTour.link"));
    ebLink.selectAll();
    
    labType = new JLabel();
    cbType = new JComboBox();
    f.setTextAndMnem(labType, PbnTools.getRes(), "downTour.type.label");
    labType.setLabelFor(cbType);
    zaladujTypyTurniejow();
    
    chkVerb = new JCheckBox();
    f.setTextAndMnem(chkVerb, PbnTools.getRes(), "button.verbose");
    chkVerb.setSelected(PbnTools.getVerbos() != 0);
    
    f.setTextAndMnem(pbOk, PbnTools.getRes(), "download");
    pbOk.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
          pbOkActionPerformed(evt);
        }
    });
    getRootPane().setDefaultButton(pbOk);
    
    f.setTextAndMnem(pbAnuluj, PbnTools.getRes(), "cancel");
    pbAnuluj.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
          pbAnulujActionPerformed(evt);
        }
    });

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(ebLink, javax.swing.GroupLayout.DEFAULT_SIZE, 380, Short.MAX_VALUE)
          .addComponent(jLabel1)
          .addGroup(layout.createSequentialGroup()
            .addComponent(labType)
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(cbType))
          .addComponent(chkVerb))
        .addContainerGap())
      .addGroup(layout.createSequentialGroup()
        .addGap(77, 77, 77)
        .addComponent(pbOk)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 104, Short.MAX_VALUE)
        .addComponent(pbAnuluj)
        .addGap(85, 85, 85))
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(jLabel1)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(ebLink, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
          .addComponent(labType)
          .addComponent(cbType))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
        .addComponent(chkVerb)
        .addGap(18, 18, 18)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(pbOk)
          .addComponent(pbAnuluj))
        .addContainerGap(20, Short.MAX_VALUE))
    );

    pack();
  }

  private void zaladujTypyTurniejow()
  {
    String sLastTour = PbnTools.m_props.getProperty("downTour.type");
    cbType.addItem("<" + PbnTools.getStr("auto-detect") + ">");
    for (HtmlTourDownloader dr: PbnTools.getTourDownloaders()) {
      cbType.addItem(dr);
      if (dr.getName().equals(sLastTour))
        cbType.setSelectedItem(cbType.getItemAt(cbType.getItemCount() - 1));
    }
  }

  private void pbOkActionPerformed(java.awt.event.ActionEvent evt)
  {
    m_sLink = ebLink.getText();
    if (m_sLink.isEmpty()) {
      javax.swing.JOptionPane.showMessageDialog(
        null, PbnTools.m_res.getString("error.emptyLink"));
      return;
    }
    PbnTools.m_props.setProperty("downTour.link", ebLink.getText());
    PbnTools.m_props.setProperty(
      "downTour.type", cbType.getSelectedItem().toString());
    PbnTools.setVerbos(chkVerb.isSelected() ? 1 : 0);
    PbnTools.m_props.setProperty("verbosity", "" + PbnTools.getVerbos());
    rv = 2;
    dispose();
  }

  private void pbAnulujActionPerformed(java.awt.event.ActionEvent evt)
  {
    rv = -1;
    dispose();
  }

  private javax.swing.JTextField ebLink;
  private javax.swing.JLabel jLabel1;
  private JLabel labType;
  private JComboBox cbType;
  private JCheckBox chkVerb;
  private javax.swing.JButton pbAnuluj;
  private javax.swing.JButton pbOk;

}
