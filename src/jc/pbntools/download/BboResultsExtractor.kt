package jc.pbntools.download

import jc.JCException
import jc.SoupProxy
import jc.f
import jc.pbntools.PbnTools
import org.jsoup.nodes.Document
import java.io.*
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.nio.charset.Charset

class BboResultsExtractor(val der: BboTourDownloader) {

  data class Row(
    val user: String,
    val rank: String,
    val result: String
  )

  fun BboTourDownloader.extractResultsCsvExt() {
    try {
      val tviewFiles = File(m_sSourceDir).listFiles { f: File ->
        f.name.matches(Regex("tview.*\\.html"))
      }
      require(tviewFiles.size == 1,
        { getMsg("oneFileExpected", "tview*.html", tviewFiles.size) })
      val soup = SoupProxy()
      val doc = soup.getDocumentFromFile(tviewFiles.first().absolutePath)
      extractResultsCsv(doc, m_sLocalDir)
    } catch (e: Exception) {
      val silent = m_bSilent && !f.isDebugMode() && PbnTools.getVerbos() == 0
      if (m_ow != null && !silent)
        m_ow.addLine(getMsg("extractResultsFailed", e.message))
    }
  }

  /**
   * May throw exceptions.
   */
  private fun BboTourDownloader.extractResultsCsv(
    doc: Document, outDir: String)
  {
    val ranks = collectRanks(doc)
    val rows = ArrayList<Row>()
    doc.select("div.sectionbreak").forEach {
      val sectionName = it.text()
      val sectionElem = it.nextElementSibling()
      require(sectionElem.className().equals("onesection"),
        { getMsg("expectedClass", "onesection", sectionElem.className()) })
      sectionElem.select("td.username").forEach {
        it.text().split('+').forEach { user: String ->
          val result = it.nextElementSibling().text()
          val row = Row(user, ranks.getOrDefault(user, ""), result)
          rows.add(row)
        }
      }
    }
    require(rows.size > 0, { getMsg("noUsersInSections") })
    writeToCsv(rows)
  }

  private fun BboTourDownloader.writeToCsv(rows: Iterable<Row>) {
    val outFile = File(m_sLocalDir, "results.csv")
    val pr = PrintWriter(OutputStreamWriter(outFile.outputStream(),
      Charset.forName("UTF-8")))
    pr.println("No.\tUser\tRank\tResult")
    rows.sortedBy { it.user.toLowerCase() }
      .forEachIndexed { i: Int, row: Row ->
        with(row) {
          pr.println("${i+1}\t$user\t$rank\t$result")
        }
    }
    pr.close()
  }

  fun BboTourDownloader.collectRanks(doc: Document): Map<String, String> {
    val m = HashMap<String, String>()
    val honorList = getOneTagEx(doc, "div.bbo_tr_o", true)
    // "Robot" users are not unique, so reversing the list, to get the
    // highest rank.
    honorList.select("td.username").reversed().forEach {
      val rank = it.parent().child(0).text()
      it.text().split('+').forEach { user: String ->
        m[user] = rank
      }
    }
    if (m.size == 0)
      throw JCException(getMsg("noRanks"))
    return m
  }

  fun extractResultsCsv() {
    der.extractResultsCsvExt()
  }
}