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

import java.lang.System;
import java.util.ResourceBundle;
import jc.f;

/** Subclass of {@link OutputWindow} using standard output */

public class StandardOutputWindow extends OutputWindow {
  
  public StandardOutputWindow(Client cli, ResourceBundle res)
  {
    super(cli, res);
    runClient();
  }
  
  /** Adds a line of text */
  public void addLine(String s)
  {
    System.out.println(s);
  }
  
  /** Adds text without new line */
  public void addText(String s)
  {
    System.out.print(s);
  }
  
}

