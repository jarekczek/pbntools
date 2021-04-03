package jc.pbntools.download

import jc.HttpProxy
import jc.SoupProxy
import jc.f
import jc.outputwindow.SimplePrinter
import jc.pbntools.PbnTools
import org.jsoup.nodes.Document
import java.io.*
import java.net.MalformedURLException
import java.net.URL
import java.nio.charset.Charset
import java.util.*

class BboLoginDecoratorForHtmlProxy(val proxy: HttpProxy, val outputWindow: SimplePrinter): HttpProxy by proxy {
  override fun getDocument(sUrl: String?) = getDocument(sUrl, SoupProxy.NO_FLAGS)

  override fun getDocument(sUrl: String?, nFlags: Int): Document {
    val doc: Document = proxy.getDocument(sUrl, nFlags)
    println("proxying $sUrl")
    if (isLoginPage(doc)) {
      println("this is a login page, $sUrl")
      doLogin(doc.baseUri())
      return proxy.getDocument(sUrl, nFlags.or(SoupProxy.NO_CACHE))
    } else {
      return doc
    }
  }

  fun isLoginPage(doc: Document): Boolean {
    return SoupProxy.getSelectText(doc, "div.bbo_content").startsWith("Please login")
  }

  fun doLogin(loginLink0: String) {
    if (f.isNullOrEmpty(PbnTools.getProp("bbo.user"))
      || f.isNullOrEmpty(PbnTools.getProp("bbo.pass")))
        throw DownloadFailedException(PbnTools.getStr("tourDown.error.noBboUser"), outputWindow, true)
    val loginLink = loginLink0.replaceFirst("\\?.*$".toRegex(), "?t=%2Fmyhands%2Findex.php%3F")
    outputWindow.addLine(PbnTools.getStr("tourDown.msg.willLogin", loginLink))
    val proxy = SoupProxy()
    val data: MutableMap<String, String> = HashMap()
    data["t"] = "/myhands/index.php?"
    data["count"] = "1"
    data["username"] = PbnTools.getProp("bbo.user")
    data["password"] = PbnTools.getProp("bbo.pass")
    data["submit"] = "Login"
    val doc: Document = try {
      val url = URL(loginLink)
      proxy.post(url, data)
    } catch (e: MalformedURLException) {
      throw RuntimeException(e)
    } catch (e2: SoupProxy.Exception) {
      throw RuntimeException(e2)
    }
    saveDocumentAsFile(doc, "bbo_login_result.html")
    val mainText = SoupProxy.getSelectText(doc, "div.bbo_content").toLowerCase()
    if (mainText.contains("username or password incorrect")) {
      throw DownloadFailedException(PbnTools.getStr("tourDown.msg.authFailed"), outputWindow, true)
    }

  }

  private fun saveDocumentAsFile(doc: Document, filename: String) {
    try {
      val dir = PbnTools.getWorkDir(false)
      val outFile = File(dir, filename)
      val pr = PrintWriter(OutputStreamWriter(FileOutputStream(outFile), Charset.forName("UTF-8")))
      pr.print(doc.outerHtml())
      pr.close()
    } catch (e: FileNotFoundException) {
      throw RuntimeException(e)
    }
  }

}