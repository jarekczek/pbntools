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

import jc.f;
import javax.swing.JCheckBox;
import javax.swing.JRootPane;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * DlgPobierzKops.java
 *
 * Created on 2010-02-23, 08:47:17
 */

/**
 *
 * @author Jarek
 */
public class DlgPobierzKops extends javax.swing.JDialog {
  int rv;
  String m_sLink;

    /** Creates new form DlgPobierzKops */
    public DlgPobierzKops(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        rv = 0;
        initComponents();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        PbnTools.setWindowIcons(this);

        jLabel1 = new javax.swing.JLabel();
        ebLink = new javax.swing.JTextField();
        pbOk = new javax.swing.JButton();
        pbAnuluj = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(PbnTools.getStr("pobierzKops.title"));

        jLabel1.setText(PbnTools.getStr("pobierzKops.link.label"));
        ebLink.setText(PbnTools.m_props.getProperty("pobierzKops.link"));
        ebLink.selectAll();

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
                .addComponent(chkVerb)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(pbOk)
                    .addComponent(pbAnuluj))
                .addContainerGap(20, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void pbOkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pbOkActionPerformed
      m_sLink = ebLink.getText();
      if (m_sLink.isEmpty()) { javax.swing.JOptionPane.showMessageDialog(null, PbnTools.m_res.getString("error.emptyLink")); return; }
      PbnTools.m_props.setProperty("pobierzKops.link", ebLink.getText());
      PbnTools.setVerbos(chkVerb.isSelected() ? 1 : 0);
      PbnTools.m_props.setProperty("verbosity", "" + PbnTools.getVerbos());
      rv = 2;
      dispose();
    }//GEN-LAST:event_pbOkActionPerformed

    private void pbAnulujActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pbAnulujActionPerformed
      rv = -1;
      dispose();
    }//GEN-LAST:event_pbAnulujActionPerformed

    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                DlgPobierzKops dialog = new DlgPobierzKops(new javax.swing.JFrame(), true);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField ebLink;
    private javax.swing.JLabel jLabel1;
    private JCheckBox chkVerb;
    private javax.swing.JButton pbAnuluj;
    private javax.swing.JButton pbOk;
    // End of variables declaration//GEN-END:variables

}
