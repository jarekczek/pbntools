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

import java.io.Writer;

/** Wrapper for OutputWindow class to serve as a <code>Writer</code> */

public class OutputWindowWriter extends Writer {
  
  protected OutputWindow m_ow;

  public OutputWindowWriter(OutputWindow ow)
  {
    m_ow = ow;
  }
  
  public void close() {}
  public void flush() {}
  
  public void write(char[] cbuf, int off, int len)
  {
    m_ow.addText(new String(cbuf, off, len));
  }
}
