/* *****************************************************************************

    Copyright (C) 2013 Jaroslaw Czekalski - jarekczek@poczta.onet.pl

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

import java.io.File;

import org.assertj.core.api.Assertions;
import org.jsoup.nodes.Document;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SoupProxyTests
{
private static Logger log = LoggerFactory.getLogger(SoupProxyTests.class);

  @Test
  public void canReadTextFile() throws SoupProxy.Exception {
    SoupProxy proxy = new SoupProxy();
    File inputFile = new File("test/jsoup/file.txt");
    log.info("will read file: " + inputFile.getAbsolutePath());
    Document doc = proxy.getDocument(inputFile.getAbsolutePath());
    log.info("response: " + doc.text());
    Assertions.assertThat(doc.text()).isEqualTo("hello");
  }

  @Test
  public void canReadGzipFile() throws SoupProxy.Exception {
    SoupProxy proxy = new SoupProxy();
    File inputFile = new File("test/jsoup/file2.txt.gz");
    log.info("will read file: " + inputFile.getAbsolutePath());
    Document doc = proxy.getDocument(inputFile.getAbsolutePath());
    log.info("response: " + doc.text());
    Assertions.assertThat(doc.text()).isEqualTo("hello from gzip");
  }

}
