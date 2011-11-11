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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * DlgProcess.java
 *
 * Created on Jan 1, 2011, 9:04:46 PM
 */

/**
 *
 * @author root
 */
public class DlgProcess extends javax.swing.JDialog {

    RunProcess m_rp;

    /** Creates new form DlgProcess */
    public DlgProcess(java.awt.Dialog parent, boolean modal, RunProcess rp) {
        super(parent, modal);
        m_rp = rp;
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

        pbZamknij = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        ebOut = new javax.swing.JTextArea();
        pbZatrzymaj = new javax.swing.JButton();

        setTitle("Proces");
        setResizable(false);

        pbZamknij.setMnemonic('z');
        pbZamknij.setText("Zamknij");
        pbZamknij.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pbZamknijActionPerformed(evt);
            }
        });

        ebOut.setColumns(20);
        ebOut.setRows(5);
        jScrollPane1.setViewportView(ebOut);

        pbZatrzymaj.setMnemonic('t');
        pbZatrzymaj.setText("Zatrzymaj");
        pbZatrzymaj.setEnabled(false);
        pbZatrzymaj.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pbZatrzymajActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 661, Short.MAX_VALUE)
                .addGap(12, 12, 12))
            .addGroup(layout.createSequentialGroup()
                .addGap(218, 218, 218)
                .addComponent(pbZatrzymaj)
                .addGap(38, 38, 38)
                .addComponent(pbZamknij)
                .addContainerGap(248, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(30, Short.MAX_VALUE)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 305, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(pbZamknij)
                    .addComponent(pbZatrzymaj))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void pbZamknijActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pbZamknijActionPerformed
        // TODO add your handling code here:
        setVisible(false);
        dispose();
    }//GEN-LAST:event_pbZamknijActionPerformed

    private void pbZatrzymajActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pbZatrzymajActionPerformed
        // TODO add your handling code here:
        if (m_rp!=null) synchronized(m_rp) {
          m_rp.m_bDestroy = true;
          m_rp.notifyAll();
          pbZatrzymaj.setEnabled(false);
          }
    }//GEN-LAST:event_pbZatrzymajActionPerformed

    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                DlgProcess dialog = new DlgProcess(new javax.swing.JDialog(), true, null);
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
    public javax.swing.JTextArea ebOut;
    private javax.swing.JScrollPane jScrollPane1;
    public javax.swing.JButton pbZamknij;
    public javax.swing.JButton pbZatrzymaj;
    // End of variables declaration//GEN-END:variables

}