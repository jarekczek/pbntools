/* *****************************************************************************

Copyright (C) 2012 Jaroslaw Czekalski - jarekczek@poczta.onet.pl

jedit settings: :folding=explicit:indentSize=2:noTabs=true:collapseFolds=1:

The MIT License (MIT)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

*****************************************************************************
*/

package jc.outputwindow;

import java.io.InputStream;
import jc.f;

/**
 * The thread class that copies the inputstream into the given
 * destination. This is to workaround <code>InputStream.read</code>
 * method, which cannot be interrupted. Overriden 
 * {@link #interrupt} method works.
 */

public class StreamCopyThread extends Thread
{
  protected Object m_dest;
  protected InputStream m_is;
  
  public StreamCopyThread(InputStream is, SimplePrinter sp)
  {
    m_is = is;
    m_dest = sp;
  }
  
  @Override
  public void interrupt()
  {
    if (m_is != null) {
      try {
        m_is.close();
      }
      catch (java.io.IOException e) {
        if (f.isDebugMode())
          e.printStackTrace();
      }
    }
    super.interrupt();
  }
  
  public void run()
  {
    try {
      int b;
      while ( (b = m_is.read()) >= 0 ) {
        StringBuilder sb = new StringBuilder(String.valueOf((char)b));
        while (m_is.available() > 0) {
          byte[] buf = new byte[m_is.available()];
          m_is.read(buf);
          sb.append(new String(buf));
        }
        ((SimplePrinter)m_dest).addText(sb.toString());
      }
      f.trace(1, "StreamCopyThread (" + getName() + ") finished normally.");
    }
    catch (java.io.IOException e) {
      if (f.isDebugMode())
        e.printStackTrace();
    }
    
  }
}
