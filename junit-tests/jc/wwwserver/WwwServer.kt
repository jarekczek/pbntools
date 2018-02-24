package jc.wwwserver

import io.ktor.application.*
import io.ktor.content.LocalFileContent
import io.ktor.content.file
import io.ktor.content.files
import io.ktor.content.static
import io.ktor.features.CallLogging
import io.ktor.features.StatusPages
import io.ktor.http.*
import io.ktor.pipeline.PipelineContext
import io.ktor.pipeline.PipelinePhase
import io.ktor.request.httpMethod
import io.ktor.request.path
import io.ktor.request.uri
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.url
import org.slf4j.event.Level
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.net.URL
import java.util.concurrent.TimeUnit

class WwwServer(val port: Int, val staticDir: String) {

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
      }
    }

    intercept(ApplicationCallPipeline.Call) {
      if (false && context.request.path().startsWith("/pbntools")
          && call.url().endsWith("/"))
        call.respondRedirect(call.url() + "index.html")
      System.out.println("whole url: " + call.url())
      if (context.request.httpMethod.equals(HttpMethod.Get) &&
          context.request.uri.startsWith("/pbntools/")) {
        call.respond(staticContents(context.request.uri))
      }
    }

    routing {
      get("/") {
        call.respondText("Hello, world!", ContentType.Text.Html)
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

  private fun staticContents(uri: String): LocalFileContent {
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
      .map { LocalFileContent(it) }
      .firstOrNull()
      ?: throw RuntimeException("Brak pliku ${file.absolutePath} i ${fileHtml.absolutePath}.")
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
    WwwServer(port, staticDir).start()
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
