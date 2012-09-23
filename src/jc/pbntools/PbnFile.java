/* *****************************************************************************

    jedit options: :folding=explicit:tabSize=2:indentSize=2:noTabs=true:

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

import java.io.*;
import java.util.*;
import javax.swing.*;
import jc.pbntools.*;
import jc.f;

public class PbnFile  {

  ArrayList<Deal> m_ar;
  Set<Integer> m_setBoardNrs;
  String m_sPlik;

  public PbnFile() {
    m_ar = new ArrayList<Deal>();
    m_setBoardNrs = new HashSet<Integer>();
    m_sPlik = "";
  }

  public void addDeal(Deal d) {
    m_ar.add(d);
    m_setBoardNrs.add(d.getNumber());
  }

  public void addDeals(Deal[] deals) {
    for (Deal d : deals) {
      m_ar.add(d);
      m_setBoardNrs.add(d.getNumber());
    }
  }

  public Deal[] getDeals() {
    return m_ar.toArray(new Deal[0]);
  }

  public int wczytaj(String sPlik) {
    m_sPlik = sPlik;
    File plik = new File(sPlik);
    try {
      BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(plik)));
      Deal r;
      do {
        r = new Deal();
        if (!r.wczytaj(br) && !r.m_bEmpty) {
          f temp;
          System.err.print(
            PbnTools.m_res.getString("pbnFile.errorReadingDeal")
            + " " + r.getNumber() + ": " + r.getErrorsStr(" ") + f.sLf);
          }
        if (r.m_bOk) {
          if (!m_setBoardNrs.contains(r.getNumber())) {
            m_ar.add(r);
            m_setBoardNrs.add(r.getNumber());
            }
          }
        }
      while (!r.m_bEof);
      }
    catch (FileNotFoundException e) { e.printStackTrace(System.err); return -1; }
    catch (IOException e) { e.printStackTrace(System.err); return -2; }
    return 0;
    }

  public void save(String sFile) throws IOException {
    BufferedWriter bw = new BufferedWriter(
      new OutputStreamWriter(new FileOutputStream(sFile), "ISO-8859-1"));
    for (Deal d : m_ar) {
      d.savePbn(bw);
    }
    bw.close();
  }

  public void arkusz() {
    String sPlikOut = "";
    String sTdStart = "<td>";
    try {
      ///@todo internationalize sheet output
      int i;
      sPlikOut = m_sPlik.replaceFirst("\\..*$", "") + "_arkusz.html";
      BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(sPlikOut), "UTF-8"));
      bw.write("<html><head>"); bw.newLine();
      bw.write("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">"); bw.newLine();
      bw.write("<style type=\"text/css\">"); bw.newLine();
      bw.write("table { border: solid black 1px; border-spacing:0; border-collapse: collapse; }"); bw.newLine();
      bw.write("td { border:solid black 1px; text-align:center; padding: 0.1em 2em; }"); bw.newLine();
      bw.write("</style>"); bw.newLine();
      bw.write("</head><body>"); bw.newLine();
      bw.write("<h1>"+m_sPlik+"</h1>"); bw.newLine();
      bw.write("<table>"); bw.newLine();
      bw.write("<tr>"); bw.newLine();
      bw.write(sTdStart+"Numer</td>");
      bw.write(sTdStart+"Rozdawa³</td>");
      bw.write(sTdStart+"Za³o¿enia</td>");
      bw.write(sTdStart+"Wynik NS</td>");
      bw.write(sTdStart+"Wynik EW</td>");
      bw.write("</tr>"); bw.newLine();
      for (i=0; i<m_ar.size(); i++) {
        Deal r = m_ar.get(i);
        bw.write("<tr>"); bw.newLine();
        bw.write(sTdStart + r.getNumber() + "</td>");
        bw.write(sTdStart + Deal.personChar(r.m_nDealer) + "</td>");
        bw.write(sTdStart + r.m_sVulner + "</td>");
        bw.write(sTdStart + "&nbsp;" + "</td>");
        bw.write(sTdStart + "&nbsp;" + "</td>");
        bw.write("</tr>"); bw.newLine();
        }
      bw.write("</table>"); bw.newLine();
      bw.write("</body></html>"); bw.newLine();
      bw.close();
      javax.swing.JOptionPane.showMessageDialog(null,
        String.format(PbnTools.m_res.getString("pbnFile.sheetSaveOk"), sPlikOut));
      }
    catch (java.io.IOException e) {
      javax.swing.JOptionPane.showMessageDialog(null,
        String.format(PbnTools.m_res.getString("pbnFile.saveError"), sPlikOut));
      f.err(e.toString());
    }
    }

  public static void main(String args[]) {
    PbnFile p = new PbnFile();
    p.wczytaj(args[0]);
    p.m_ar.get(0).rozdaj();
    System.out.println("koniec");
    }
  }
