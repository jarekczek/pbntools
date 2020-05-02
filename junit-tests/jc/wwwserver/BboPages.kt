package jc.wwwserver

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.content.OutgoingContent
import io.ktor.content.TextContent
import io.ktor.http.ContentType
import io.ktor.pipeline.PipelineContext
import io.ktor.request.receiveParameters
import io.ktor.response.*
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import java.io.File
import java.nio.charset.Charset
import java.util.logging.Logger

object BboPages {
  val log = Logger.getLogger("jarek")

  fun bboRouting(srv: WwwServer, route: Route) {
    route.bboRoutingExt(srv)
  }

  private fun Route.bboRoutingExt(srv: WwwServer) {
    get {
      BboPages.bboHomePage(this)
    }
    get("/login") {
      call.respond(srv.staticContents("/pbntools/bbo/login.html"))
    }
    get("/login_failed") {
      call.respond(srv.staticContents("/pbntools/bbo/login_incorrect.html"))
    }
    get("/myhands/fetchlin.php") {
      srv.requireAuth(this)
      val linId = call.parameters["id"]!!
      call.response.header("Content-Disposition", "inline; filename=\"$linId.lin\"")
      call.respond(bboFetchlinResponse(srv, linId))
    }
    get("/myhands/hands.php") {
      srv.requireAuth(this)
      if ("2196-1376162040-".equals(call.parameters["tourney"]))
        call.respondRedirect("/pbntools/test_6_bbo_skyclub_20130810/SKY_CLUB_2196_Pairs_SKY_CLUB_JACKPOT_2000/hands.php@tourney=2196-1376162040-&offset=0.html")
      else if ("someone".equals(call.parameters["username"])) {
        call.respond(bboHistoryResponse(srv))
      } else
        call.respond(bboTourneyResponse(srv, call.parameters["tourney"] ?: "?"))
    }
    post("/login") {
      handleLoginPost()
    }
    post("/login_failed") {
      handleLoginPost()
    }
    get("/myhands") {
      srv.requireAuth(this)
      call.respondText("bbo myhands")
    }
  }

  private suspend fun PipelineContext<Unit, ApplicationCall>.handleLoginPost() {
    val session = context.attributes[WwwServer.sessionKey]
    val params = call.receiveParameters()
    params.entries().forEach {
      println("post parameter ${it.key}=${it.value}")
    }
    if (params["username"]?.equals("u") ?: false) {
      log.info("User %s authenticated, redirected to %s."
        .format(params["username"], session.lastPage))
      session.authenticated = true
      call.respondRedirect(session.lastPage.toString())
    }
    else
      call.respondRedirect("/bbo/login_failed")
  }

  private fun bboTourneyResponse(srv: WwwServer, tourneyName: String): OutgoingContent {
    return when(tourneyName) {
      "2196-1376162040-" -> srv.staticContents("test_6_bbo_skyclub_20130810/SKY_CLUB_2196_Pairs_SKY_CLUB_JACKPOT_2000/hands.php%3Ftourney=2196-1376162040-&offset=0.html")
      else -> {
        val text = File(srv.staticDir, "bbo/no_tournament_data.html")
          .readText(Charset.forName("utf-8"))
          .replace("7173-1519560481-", tourneyName)
        TextContent(text, ContentType.Text.Html)
      }
    }
  }

  private fun bboHistoryResponse(srv: WwwServer): OutgoingContent {
    val text = File(srv.staticDir, "bbo/hands_history.html")
      .readText(Charset.forName("utf-8"))
    return TextContent(text, ContentType.Text.Html)
  }

  private fun bboFetchlinResponse(srv: WwwServer, linId:String): OutgoingContent {
    val text = File(srv.staticDir, "bbo/$linId.lin")
      .readText(Charset.forName("utf-8"))
    return TextContent(text, ContentType.parse("application/bbolin"))
  }

  suspend fun bboHomePage(ctx: PipelineContext<Unit, ApplicationCall>) {
    ctx.bboHomePageExt()
  }

  private suspend fun PipelineContext<Unit, ApplicationCall>.bboHomePageExt() {
    call.respondText(ContentType.Text.Html) {
      createHTML().html {
        head {
          title("BBO home")
        }
        body {
          h1 { text("bbo home") }
          ul {
            li { a { href = "/bbo/login"; text("login") } }
            li { a { href = "/bbo/login_failed"; text("login failed") } }
            li { a { href = "/bbo/myhands/hands.php"; text("hands.php") } }
            li { a { href = "/bbo/myhands"; text("myhands") } }
            li { a { href = "/bbo/myhands/hands.php?username=someone"; text("someone's hands history") } }
            li {
              a {
                href = "http://localhost:15863/bbo/myhands/hands.php?tourney=2196-1376162040-"
                text("tourney sky club 2196")
              }
            }
          }
        }
      }
    }
  }
}