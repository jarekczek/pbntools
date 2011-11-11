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
  
  protected ResourceBundle m_res;

  /** Default constructor is disabled. Use <code>create</code>
      method to instantiate the object. 
    */
  private OutputWindow() {}
  
  protected OutputWindow(Dialog parent, ResourceBundle res)
  {
    super(parent, false);
    m_res = res;

    // standard dialog startup code
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    GroupLayout lay = new GroupLayout(getContentPane());
    lay.setAutoCreateGaps(true);
    lay.setAutoCreateContainerGaps(true);
    setLayout(lay);
    
    JButton pbClose = new JButton();
    pbClose.setAction(new CloseAction("close"));

    lay.setVerticalGroup(
      lay.createSequentialGroup()
        .addComponent(pbClose)
    );
    lay.setHorizontalGroup(
      lay.createSequentialGroup()
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

  class CloseAction extends MyAction {
    CloseAction(String s) { super(s); }
    public void actionPerformed(java.awt.event.ActionEvent e) {
      dispose();
    }
  }
  
}
