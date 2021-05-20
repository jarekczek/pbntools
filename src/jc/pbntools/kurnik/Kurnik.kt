package jc.pbntools.kurnik

import jc.SoupProxy
import org.apache.http.client.utils.URLEncodedUtils
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.File
import java.lang.RuntimeException
import java.net.URI
import java.net.URL
import java.util.regex.Pattern

object Kurnik {
  val cacheDir = "cache"
  val proxy = SoupProxy()
  val knownDeals = HashSet<String>()
  var dealAverageResults = mutableListOf<Double>()
  val processedPlayers = HashSet<String>()
  val maxProcessedDeals = 1200
  val sleepSeconds = 5

  @JvmStatic
  fun main(args: Array<String>) {
    val startUrl = "https://www.kurnik.pl/stat.phtml?u=jarekczek&g=br&sk=2"
    File(cacheDir).mkdir()
    processPlayerHistoryUrl(startUrl)
    println("cummulated average of " + dealAverageResults.size + ": " + dealAverageResults.average())
  }

  private fun processPlayerHistoryUrl(playerHistoryUrl: String) {
    if (dealAverageResults.size > maxProcessedDeals) {
      return
    }
    println("processing history $playerHistoryUrl, averages count: " + dealAverageResults.size)
    val doc = loadPage(playerHistoryUrl)
    val results = doc.select(".ktb").get(0)
    val dealRows = results.select("tr").filter { it.attr("class") != "kbl" }
    if (dealRows.size == 0) {
      throw RuntimeException("no deal rows")
    }
    val newPlayers = mutableListOf<String>()
    for (dealRow in dealRows) {
      if (dealIsNotFinished(dealRow)) {
        continue
      }
      val dealId = dealId(dealRow, playerHistoryUrl)
      processDeal(dealId)
      newPlayers.addAll(getPlayerNames(dealRow))
    }

    val recurse = dealAverageResults.size < maxProcessedDeals
    if (recurse) {
      newPlayers.forEach { newPlayerName ->
        if (!processedPlayers.contains(newPlayerName)) {
          println("found new player: $newPlayerName")
          processedPlayers.add(newPlayerName)
          val playerHistoryUrl = "https://www.kurnik.pl/stat.phtml?u=$newPlayerName&g=br&sk=2"
          processPlayerHistoryUrl(playerHistoryUrl)
        }
      }
    }
  }

  private fun getPlayerNames(dealRow: Element): List<String> {
    val players = mutableListOf<String>()
    dealRow.select("td").get(1).select("a").forEach { a ->
      val playerLink = a.attr("href")
      val matcher = Pattern.compile("\\?u=(.+)&").matcher(playerLink)
      matcher.find()
      players.add(matcher.group(1))
    }
    return players
  }

  private fun dealIsNotFinished(dealRow: Element): Boolean {
    dealRow.select("td").forEach {
      if (it.text().contains("w trakcie")) {
        return true
      }
      if (it.text() == "(uc.)") {
        return true
      }
    }
    return false
  }

  private fun processDeal(dealId: String) {
    if (knownDeals.contains(dealId)) {
      //println("deal $dealId already processed")
    } else {
      println("processing deal $dealId")
      knownDeals.add(dealId)
      val dealUrl = "https://www.kurnik.pl/rozd.phtml?hid=$dealId"
      val dealPage = loadPage(dealUrl)
      println("loaded deal $dealUrl")
      val dealResultsRows = dealPage.select(".ktb").select("tr").filter { it.attr("class") != "kbl" }
      val nsResults = dealResultsRows.map { dealResultRow ->
        val nsResultStr = dealResultRow.select("td").get(3).text()
        val nsResult = java.lang.Double.valueOf(nsResultStr)
        nsResult
      }
      println("average for deal $dealId: " + (nsResults.average()))
      dealAverageResults.add(nsResults.average())
    }
  }

  private fun dealId(dealRow: Element, currentLink: String): String {
    if (dealRow.select("a").isEmpty()) {
      throw RuntimeException("brak elementow a dla " + dealRow.toString())
    }
    //dealRow.select("a").forEach { println("a contents: " + it.text()) }
    val porownania = dealRow.select("a").filter { it.text().equals("porównanie") }
    if (porownania.size == 0) {
      throw RuntimeException("brak elementow porownania dla " + dealRow.toString() + ", link: $currentLink")
    }
    porownania.forEach {
      val ref = it.attr("href")
      val params = URLEncodedUtils.parse(URI(ref), Charsets.UTF_8)
      //params.forEach { println(it.name) }
      params.filter { it.name == "hid" }.forEach { return it.value }
      throw RuntimeException("no attribute hid in " + it.toString())
    }
    throw RuntimeException("unexpected")
  }

  private fun loadPage(url: String): Document {
    val cacheFile = File(cacheDir, normalizeUrlToFilename(url))
    if (cacheFile.exists()) {
      return proxy.getDocument(cacheFile.absolutePath)
    } else {
      jc.f.sleepNoThrow(sleepSeconds * 1000L)
      val doc = proxy.getDocument(url)
      cacheFile.writeText(doc.toString())
      return doc
    }
  }

  private fun normalizeUrlToFilename(strUrl: String): String {
    val url = URL(strUrl)
    var filename = url.file.replace("/", "")
    filename = filename.replace("?", "")
    println(filename)
    return filename
  }
}

