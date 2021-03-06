package jc.wwwserver

import io.ktor.application.*
import io.ktor.content.*
import io.ktor.features.CallLogging
import io.ktor.features.StatusPages
import io.ktor.http.*
import io.ktor.pipeline.PipelineContext
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.AttributeKey
import io.ktor.util.url
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.net.URL
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit
import org.slf4j.event.Level
import java.util.Collections
import java.util.logging.Logger

class WwwServer(val port: Int, val staticDir: String) {
  val log = Logger.getLogger("jarek")
  val sessions = Collections.synchronizedMap(HashMap<String, WwwSession>())
  val busyCounters = HashMap<String, Int>()

  companion object {
    val sessionKey = AttributeKey<WwwSession>("session")
  }

  init {
    require(File(staticDir).isDirectory && File(staticDir).exists()) {
      "Brak katalogu $staticDir"
    }
  }

  val server = embeddedServer(Netty, port) {
    install(CallLogging) { level = Level.INFO }
    install(StatusPages) {
      exception<Throwable> {
        call.respondText(status = HttpStatusCode.InternalServerError) {
          getStackTraceString(it)
        }
        it.printStackTrace()
      }
    }

    intercept(ApplicationCallPipeline.Call) {
      System.out.println("whole url: " + call.url())
      context.request.queryParameters.entries().forEach {
        println("query parameter ${it.key}=${it.value}")
      }
    }

    routing {
      sessionInterceptor()

      route("/bbo") {
        BboPages.bboRouting(this@WwwServer, this)
      }

      get("/") {
        mainPage()
      }
      get("/busy") {
        busyPage()
      }
      get("/pbntools/{...}") {
        if (context.request.path().toLowerCase().contains("test_6_bbo"))
          requireAuthExt()
        log.debug("serving pbntools/* from file " + context.request.uri)
        call.respond(staticContents(context.request.uri))
      }
      get("/stop") {
        call.respondText {
          System.out.println("received stop request, $this");
          Thread { stopInternal() }.start()
          "stop"
        }
      }
    }
  }

  private fun Route.sessionInterceptor() {
    intercept(ApplicationCallPipeline.Call) {
      fun createSession(): WwwSession {
        val newSession = WwwSession("" + System.currentTimeMillis())
        context.response.cookies.append("PHPSESSID", newSession.id, path = "/")
        sessions.put(newSession.id, newSession)
        log.fine("Created new session with id ${newSession.id}.")
        return newSession
      }

      val session =
              if (context.request.cookies.rawCookies.containsKey("PHPSESSID")) {
                val oldSessionId = context.request.cookies.rawCookies.get("PHPSESSID")
                if (sessions.containsKey(oldSessionId))
                  log.fine("Session id $oldSessionId recognized.")
                else
                  log.info("Invalid session id: " + oldSessionId)
                sessions.getOrElse(oldSessionId) { createSession() }
              } else
                createSession()
      context.attributes.put(WwwServer.sessionKey, session)
    }
  }

  suspend fun PipelineContext<Unit, ApplicationCall>.requireAuthExt() {
    val session = context.attributes[sessionKey]
    log.fine("entered requireAuthExt, current session id: ${session.id}.")
    if (!session.authenticated) {
      session.lastPage = this.context.request.uri
      call.respondRedirect("/bbo/login")
    }
  }

  suspend fun requireAuth(ctx: PipelineContext<Unit, ApplicationCall>) {
    ctx.requireAuthExt()
  }

  private suspend fun PipelineContext<Unit, ApplicationCall>.mainPage() {
    call.respondText(ContentType.Text.Html) {
      createHTML().html {
        head {
          title("PbnTools local ktor server")
        }
        body {
          h1 { text("links") }
          ul {
            li { a {
              href = "/pbntools/test_1_pary/WB120802/wb120802"
              text("turniej pary 1")
            } }
            li { a { href = "/bbo"; text("BBO") } }
            li { a { href = "/busy"; text("busy") } }
            li { a { href = "/stop"; text("stop") } }
          }
        }
      }
    }
  }

  private suspend fun PipelineContext<Unit, ApplicationCall>.busyPage() {
    val cnt = busyCounters.compute(call.url(), { key: String, value: Int? -> (value ?: 0) + 1 })!!
    val busy = cnt.rem(3) != 0
    val statusCode = if (busy) HttpStatusCode.ServiceUnavailable else HttpStatusCode.OK
    call.respondText(ContentType.Text.Html, statusCode) {
      createHTML().html {
        body {
          p { text(if (busy) "server is busy" else "server was able to respond") }
        }
      }
    }
  }

  fun staticContents(uri: String): LocalFileContent {
    var relativePath = uri
      .replace(Regex("^/pbntools/"), "")
      .replace('?', '@')
    System.out.println("relativePath: $relativePath")
    if (relativePath.endsWith("/"))
      relativePath += "index.html"
    val file = File(staticDir, relativePath)
    val fileHtml = File(staticDir, relativePath + ".html")
    System.out.println("getting file ${file.absolutePath}")
    System.out.println("or file ${fileHtml.absolutePath}")
    return listOf(file, fileHtml)
      .filter { it.exists() }
      .map {
        localFileContentWithCorrectCharset(it)
      }
      .firstOrNull()
      ?: throw RuntimeException("Brak pliku ${file.absolutePath} i ${fileHtml.absolutePath}.")
  }

  private fun localFileContentWithCorrectCharset(file: File): LocalFileContent {
    val fc1 = LocalFileContent(file)
    val contentType = ContentType(
      fc1.contentType.contentType,
      fc1.contentType.contentSubtype
    ).withCharset(Charset.forName("iso-8859-2"))
    return LocalFileContent(file, contentType)
  }

  private fun getStackTraceString(t: Throwable): String {
    val sw = StringWriter()
    val pr = PrintWriter(sw)
    t.printStackTrace(pr)
    return t.message + System.getProperty("line.separator") + sw.toString()
  }

  fun start() {
    server.start(wait = false)
  }

  private fun stopInternal() {
    server.stop(1, 1, TimeUnit.SECONDS)
    System.out.println("stopInternal finished.")
  }
}

object WwwServerControl {
  val port = 15863
  val staticDir = "test"
  fun start() {
    System.out.println("Ktor server starts on port $port.")
    try {
      WwwServer(port, staticDir).start()
    } catch (e: Exception) {
      e.printStackTrace()
      System.out.println("Finishing application manually, because otherwise netty would hang.")
      System.exit(232)
    }
    System.out.println("Ktor server started.")
  }
  fun stop() {
    System.out.println("Stopping ktor.")
    URL("http://localhost:$port/stop").getContent()
    System.out.println("Ktor server stopped.")
  }
}

fun usage() {
  System.out.println("Usage: WwwServer <start>|<stop>")
}

fun main(args: Array<String>) {
  if (args.size != 1)
    usage()
  else {
    when (args[0]) {
      "start" -> WwwServerControl.start()
      "stop" -> WwwServerControl.stop()
      else -> { usage(); throw IllegalArgumentException(args[0]) }
    }
  }
}
