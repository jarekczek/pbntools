/* *****************************************************************************

    jedit options: :folding=explicit:tabSize=2:noTabs=true:

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

package jc.pbntools.download;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.JDialog;

import jc.f;
import jc.JCException;
import jc.outputwindow.OutputWindow;
import jc.SoupProxy;
import jc.pbntools.PbnTools;
import jc.pbntools.RunProcess;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ParyTourDownloader extends HtmlTourDownloader
{

  public void setOutputWindow(OutputWindow ow)
  {
    m_ow = ow;
  }

  /** Redirect to url without W- */
  protected boolean redirect() throws VerifyFailedException
  {
    if (m_sLink.matches("^.*/W-[^/]*$")) {
      m_sLink = m_sLink.replaceFirst("/W-([^/]*)$", "/$1");
      m_ow.addLine(PbnTools.getStr("tourDown.msg.redir", m_sLink));  
      return true;
    }
    else
      return false;
  }

  /** Verifies whether link points to a valid data in this format.
    * Sets m_sTitle and m_sDirName members
    */ //{{{
  protected boolean verifyDirect(boolean bSilent) throws VerifyFailedException
  {
    Document doc;
    try {
      SoupProxy proxy = new SoupProxy();
      doc = proxy.getDocument(m_sLink);
      m_doc = doc;
      m_remoteUrl = proxy.getUrl();
    }
    catch (JCException e) {
      throw new VerifyFailedException(e);
    }
    m_ow.addLine(PbnTools.m_res.getString("msg.documentLoaded"));

    if (!checkGenerator(doc, "JFR 2005", bSilent)) { throw new VerifyFailedException("generator"); }
    if (doc.body() != null) {
      // only W- link has body
      // direct link has frames which should be read instead
      if (!checkTagText(doc.body(), "p.f", "^\\sPary\\..*$", bSilent)) {
        throw new VerifyFailedException("p.f");
      }
    }
    getTitleAndDir();
    if (!bSilent) { m_ow.addLine(PbnTools.getStr("msg.tourFound", m_sTitle)); }
    return true;
  } //}}}
  
  public boolean verify(boolean bSilent) throws VerifyFailedException
  {
    boolean bRedirected = true;
    while (bRedirected) {
      if (!verifyDirect(bSilent)) { return false; }
      bRedirected = redirect();
    }
    return true;
  }

  protected void wget()
  {
    String sArgs = "-p -k -nH -nd -r -l 2 -w 2 --random-wait -e robots=off -N";
    ArrayList<String> args = new ArrayList<String>(Arrays.asList(sArgs.split(" ")));
    RunProcess.runCmd(null, "wget", args.toArray(new String[0]));
    // msys needs converting all path separators from \ to /
    /*
    String sScript = (m_sScriptDir + m_sSlash + "get_tur_kops.sh").replaceAll("\\\\", "/");
    String asArgs[] = { "-c",
                        sScript
                        + " -d \"" + sWorkDir.replaceAll("\\\\", "/") + "\" "
                        + sLink };
    if (bLinux) {
      rv = RunProcess.runCmd((JDialog)m_dlgMain, "bash", asArgs, m_sScriptDir);
      }
    else {
      String sMsysBin = m_sCurDir + m_sSlash + "bin" + m_sSlash + "msys" + m_sSlash + "bin";
      String asPaths[] = { sMsysBin,
                           m_sCurDir + m_sSlash + "bin" + m_sSlash + "wget" };
      String sBash = sMsysBin + m_sSlash + "bash";
      rv = RunProcess.runCmd((JDialog)m_dlgMain, sBash, asArgs, m_sScriptDir, asPaths);
      }
      */
  }


}
