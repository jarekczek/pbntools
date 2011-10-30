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

import java.io.*;
import java.util.*;
import javax.swing.*;
import jc.pbntools.*;
import jc.f;

class Diagram {
  boolean m_bColor;
  int m_nNr;
  int m_nDealer;
  static final String sSides = "NESW";
  Diagram(boolean bColor, int nNr) {
    m_bColor = bColor;
    m_nNr = nNr;
    m_nDealer = (m_nNr-1)%4;
    }

  void drawQuadr(Writer w, boolean bVulner, boolean bDealer, String sLabel) throws java.io.IOException {
    w.write("\\pspolygon(-45,45)(45,45)(25,25)(-25,25)" + f.sLf);
    w.write("\\rput{180}(0,35){" + sLabel + "}" + f.sLf);
    }

  public void draw(Writer w, int nWidth) throws java.io.IOException {
    int i;

    w.write("\\psset{unit=" + nWidth/100.0 + "mm}" + f.sLf);
    w.write("\\fontencoding{T1}\\fontfamily{phv}\\fontsize{" + nWidth/100.0*14 + "mm}{0}\\fontseries{b}\\selectfont" + f.sLf);
    w.write("\\psset{linewidth=0.4pt}" + f.sLf);
    w.write("\\pspicture(-50,-50)(50,50)" + f.sLf);
    w.write("\\psframe(-50,-50)(50,50)" + f.sLf);
    for (i=0; i<4; i++) {
      w.write("\\rput{" + i*90 + "}(0,0){" + f.sLf);
      drawQuadr(w, false, i==m_nDealer, sSides.substring(i,i+1));
      w.write("} %rput" + f.sLf);
      }
    w.write("\\endpspicture" + f.sLf);
    }

  }

public class DealLabel {
  public static void drawDiagram(Writer w, int nNr, int nWidth) throws java.io.IOException {
    int i, nDealer;

    nDealer = (nNr-1)%4;
    Diagram d = new Diagram(true, nNr);
    d.draw(w, nWidth);
    }

  public static void main(String args[]) throws java.io.IOException {
    int iDiag;
    FileWriter fw = new FileWriter("/tmp/deal.tex");
    fw.write("\\documentclass{article}" + f.sLf);
    fw.write("\\usepackage{pstricks}" + f.sLf);
    fw.write("\\begin{document}" + f.sLf);
    fw.write("hello, today is " + new Date() + f.sLf + f.sLf);
    for (iDiag=1; iDiag<=2; iDiag++) {
      drawDiagram(fw, iDiag, 100);
//      fw.write("" + f.sLf + "\\null" + f.sLf + f.sLf);
      }
    
//    fw.write("\\psset{linewidth=.4pt}" + f.sLf);
//    fw.write("\\pspolygon(0, 0)(420, 0)(420, 420)(0, 420)" + f.sLf);
//    fw.write("\\pscircle(206, 222){166.18}" + f.sLf);
//    fw.write("\\psset{linewidth=1pt}" + f.sLf);
//    fw.write("\\pspolygon(43, 253)(361, 283)(287, 77)" + f.sLf);
    fw.write("\\end{document}" + f.sLf);
    fw.write("" + f.sLf);
    fw.write("" + f.sLf);
    fw.write("" + f.sLf);
    fw.close();
//    if (0 == RunProcess.runCmd(null, "sh -c \"latex /tmp/deal.tex; dvips /tmp/deal.dvi;\"", "/tmp")) {
//      java.awt.Desktop.getDesktop().open(new File("/tmp/deal.pdf"));
//      }
//    ProcessBuilder pb = new ProcessBuilder("latex", "/tmp/deal.tex");
//    pb.directory(new File("/tmp"));
//    pb.redirectErrorStream(true);
//    pb.start().;
//    System.out.println("ok");
    }
  }
