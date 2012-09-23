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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.util.*;
import javax.swing.GroupLayout;
import javax.swing.SwingConstants;
import jc.f;

public class DlgPbnToolsMain extends javax.swing.JFrame {

    ResourceBundle m_res;

    /** Creates new form DlgPbnToolsMain */
    public DlgPbnToolsMain() {
        super();
        m_res = jc.pbntools.PbnTools.m_res;
        initComponents();
    }

    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        PbnTools.setWindowIcons(this);
      
        pbPobierzKops = new javax.swing.JButton();
        pbPobierzPary = new javax.swing.JButton();
        // pbPobierzBbo = new javax.swing.JButton();
        pbKonfig = new javax.swing.JButton();
        pbZakoncz = new javax.swing.JButton();
        pbPomoc = new javax.swing.JButton();
        pbOProgramie = new javax.swing.JButton();
        pbOtworzPbn = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        addWindowListener(new CloseHandler());
        setTitle("Pbn Tools " + PbnTools.m_res.getString("wersja"));

        f.setTextAndMnem(pbPobierzKops, PbnTools.getRes(), "pobierzKops");
        pbPobierzKops.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pbPobierzKopsActionPerformed(evt);
            }
        });

        f.setTextAndMnem(pbPobierzPary, PbnTools.getRes(), "pobierzPary");
        pbPobierzPary.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pbPobierzParyActionPerformed(evt);
            }
        });

        /*pbPobierzBbo.setMnemonic('b');
        pbPobierzBbo.setText("Pobierz Bbo");
        pbPobierzBbo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pbPobierzBboActionPerformed(evt);
            }
        });*/

        f.setTextAndMnem(pbKonfig, PbnTools.getRes(), "button.config");
        pbKonfig.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pbKonfigActionPerformed(evt);
            }
        });

        f.setTextAndMnem(pbZakoncz, PbnTools.getRes(), "finish");
        pbZakoncz.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pbZakonczActionPerformed(evt);
            }
        });

        f.setTextAndMnem(pbPomoc, PbnTools.getRes(), "help");
        pbPomoc.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pbPomocActionPerformed(evt);
            }
        });

        f.setTextAndMnem(pbOProgramie, PbnTools.getRes(), "about");
        pbOProgramie.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pbOProgramieActionPerformed(evt);
            }
        });

        f.setTextAndMnem(pbOtworzPbn, PbnTools.getRes(), "openPbnFile");
        pbOtworzPbn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pbOtworzPbnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        int cxChar = getContentPane().getFontMetrics(
                       getContentPane().getFont()).charWidth('X');
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(5*cxChar)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pbZakoncz, javax.swing.GroupLayout.DEFAULT_SIZE, 136, Short.MAX_VALUE)
                    .addComponent(pbKonfig, javax.swing.GroupLayout.DEFAULT_SIZE, 136, Short.MAX_VALUE)
                    .addComponent(pbPomoc, javax.swing.GroupLayout.DEFAULT_SIZE, 136, Short.MAX_VALUE)
                    .addComponent(pbOProgramie, javax.swing.GroupLayout.DEFAULT_SIZE, 136, Short.MAX_VALUE)
                    // .addComponent(pbPobierzBbo, javax.swing.GroupLayout.DEFAULT_SIZE, 136, Short.MAX_VALUE)
                    .addComponent(pbPobierzKops, javax.swing.GroupLayout.DEFAULT_SIZE, 136, Short.MAX_VALUE)
                    .addComponent(pbPobierzPary, javax.swing.GroupLayout.DEFAULT_SIZE, 136, Short.MAX_VALUE)
                    .addComponent(pbOtworzPbn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(5*cxChar))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pbOProgramie)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pbPomoc)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pbKonfig)
                .addGap(67, 67, 67)
                .addComponent(pbPobierzKops)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pbPobierzPary)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                // .addComponent(pbPobierzBbo)
                // .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pbOtworzPbn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 36, Short.MAX_VALUE)
                .addComponent(pbZakoncz)
                .addGap(21, 21, 21))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void pbZakonczActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pbZakonczActionPerformed
        // TODO add your handling code here:
        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }//GEN-LAST:event_pbZakonczActionPerformed

    private void pbKonfigActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pbKonfigActionPerformed
        // TODO add your handling code here:
        DlgPbnToolsConf d = new DlgPbnToolsConf(this, true);
        d.setVisible(true);
    }//GEN-LAST:event_pbKonfigActionPerformed

    private void pbPobierzKopsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pbPobierzKopsActionPerformed
      // TODO add your handling code here:
      DlgPobierzKops d = new DlgPobierzKops(null,true);
      d.setVisible(true);
      if (d.rv==2) {
        PbnTools.pobierzKops(d.m_sLink, true);
        }
    }//GEN-LAST:event_pbPobierzParyActionPerformed

    private void pbPobierzParyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pbPobierzParyActionPerformed
      // TODO add your handling code here:
      DlgPobierzPary d = new DlgPobierzPary(null,true);
      d.setVisible(true);
      if (d.rv==2) {
        PbnTools.pobierzPary(d.m_sLink, true);
        }
    }//GEN-LAST:event_pbPobierzParyActionPerformed

    private void pbPobierzBboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pbPobierzBboActionPerformed
        // TODO add your handling code here:
      DlgPobierzBbo d = new DlgPobierzBbo(null,true);
      d.setVisible(true);
      if (d.rv==2) {
        PbnTools.pobierzBbo(d.m_sLink);
        }

    }//GEN-LAST:event_pbPobierzBboActionPerformed

    private void pbPomocActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pbPomocActionPerformed
        // TODO add your handling code here:
        // DlgPbnToolsKonf.browse
        String sHelpPath = f.basePath(this.getClass()) + f.sDirSep;
        String sHelpFile = sHelpPath + "help_" + System.getProperty("user.language") + ".html";
        String sHelpFile2 = sHelpPath + "help_en.html";
        if (!new File(sHelpFile).exists()) { sHelpFile = sHelpFile2; }
        if (!new File(sHelpFile).exists()) {
          f.msg(String.format(PbnTools.m_res.getString("error.helpFileNotFound"), sHelpFile));
        } else {
          f.desktopOpen(sHelpFile);
        }
    }//GEN-LAST:event_pbPomocActionPerformed

    private void pbOProgramieActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pbOProgramieActionPerformed
        // TODO add your handling code here:
        DlgPbnToolsAbout d = new DlgPbnToolsAbout(this, true);
        d.setVisible(true);
}//GEN-LAST:event_pbOProgramieActionPerformed

    private void pbOtworzPbnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pbOtworzPbnActionPerformed
        // TODO add your handling code here:
        jc.pbntools.DlgPbnOpen d = new jc.pbntools.DlgPbnOpen(null, true);
        d.setVisible(true);
    }//GEN-LAST:event_pbOtworzPbnActionPerformed

    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                DlgPbnToolsMain dialog = new DlgPbnToolsMain();
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
    private javax.swing.JButton pbKonfig;
    private javax.swing.JButton pbOProgramie;
    private javax.swing.JButton pbOtworzPbn;
    private javax.swing.JButton pbPobierzBbo;
    private javax.swing.JButton pbPobierzKops;
    private javax.swing.JButton pbPobierzPary;
    private javax.swing.JButton pbPomoc;
    private javax.swing.JButton pbZakoncz;
    // End of variables declaration//GEN-END:variables

  private static class CloseHandler extends WindowAdapter
  {
    @Override
    public void windowClosing(WindowEvent e) {
      f.trace(1, "closing");
      PbnTools.closeDown();
    }
  }

}

// :encoding=windows-1250:

