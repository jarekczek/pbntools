package jc.pbntools.download

import jc.JCException
import jc.SoupProxy
import jc.f
import jc.pbntools.PbnTools
import jc.pbntools.SimpleResultsRanker
import jc.pbntools.UserResult
import jc.pbntools.UserResultRanked
import org.jsoup.nodes.Document
import java.io.*
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.math.BigDecimal
import java.math.MathContext
import java.nio.charset.Charset

class BboResultsExtractor(val der: BboTourDownloader) {
  companion object {
    val sep = ";"
  }

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
      if (outputWindow != null && !silent)
        outputWindow.addLine(getMsg("extractResultsFailed", e.message))
    }
  }

  /**
   * May throw exceptions.
   */
  private fun BboTourDownloader.extractResultsCsv(
    doc: Document, outDir: String)
  {
    val rows = ArrayList<UserResult>()
    doc.select("div.sectionbreak").forEach {
      val sectionName = it.text()
      val sectionElem = it.nextElementSibling()
      require(sectionElem.className().equals("onesection"),
        { getMsg("expectedClass", "onesection", sectionElem.className()) })
      sectionElem.select("td.username").forEach {
        it.text().split('+').forEach { user: String ->
          val result = it.nextElementSibling().text()
          val row = UserResult(user, BigDecimal(result))
          rows.add(row)
        }
      }
    }
    require(rows.size > 0, { getMsg("noUsersInSections") })
    writeToCsv(rows)
    writeToCsvRanked(SimpleResultsRanker().rank(rows))
  }

  private fun BboTourDownloader.writeToCsv(rows: Iterable<UserResult>) {
    val outFile = File(m_sLocalDir, "results.csv")
    val pr = PrintWriter(OutputStreamWriter(outFile.outputStream(),
      Charset.forName("UTF-8")))
    pr.println(listOf("No.", "User", "Result").joinToString(sep))
    rows.sortedBy { it.name.toLowerCase() }
      .forEachIndexed { i: Int, row: UserResult ->
        with(row) {
          pr.println(listOf("${i+1}", name, points).joinToString(sep))
        }
    }
    pr.close()
  }

  private fun BboTourDownloader.writeToCsvRanked(
    rows: Iterable<UserResultRanked>)
  {
    val outFile = File(m_sLocalDir, "results_ranked.csv")
    val pr = PrintWriter(OutputStreamWriter(outFile.outputStream(),
      Charset.forName("UTF-8")))
    pr.println(listOf("No.", "User", "Result", "Rank", "PDF").joinToString(sep))
    rows.forEachIndexed { i: Int, row: UserResultRanked ->
        with(row) {
          pr.println(
            listOf("${i+1}", res.name, res.points, rank, pdf)
              .joinToString(sep)
          )
        }
      }
    pr.close()
  }

  @Deprecated("We don't need bbo ranks anymore, they give only top places.")
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