package controllers

import play.api.mvc._
import models._
import play.api.libs.json._
import scala.concurrent.{ Future, Promise }
import scala.concurrent.ExecutionContext.Implicits.global
import java.util.concurrent.TimeUnit
import play.api.libs.iteratee.Enumerator
import play.api.libs.iteratee.Iteratee
import play.api.libs.iteratee.Enumeratee

object API extends Controller {

  /**
   * Authentication for using API
   */
  def isAuth(f: User => Request[AnyContent] => Result) = Action { request =>
    val futureResult = Future {
      val username = request.headers.get("username").getOrElse("")
      val password = request.headers.get("password").getOrElse("")

      User.authenticate(User(username, password)) match {
        case None => BadRequest("Invalid USERNAME or PASSWORD...")
        case Some(user) => f(user)(request)
      }
    }
    // if service handles request too slow => return Timeout response
    val timeoutFuture = play.api.libs.concurrent.Promise.timeout(
      message = "Oops, TIMEOUT while calling BID client...",
      duration = 30,
      unit = TimeUnit.SECONDS)

    Async {
      Future.firstCompletedOf(Seq(futureResult, timeoutFuture)).map {
        case f: Result => f
        case t: String => InternalServerError(t)
      }
    }
  }

  def getUsers = isAuth(user => implicit request =>
    user.name match {
      case "krisp0" =>
        Ok(Json.stringify(Json.toJson(User.findAll.map(_.name))))
      case _ => Ok(Json.stringify(JsString("Access denied...")))
    })

  /**
   * BID
   */
  def postWordstatReport = isAuth(user => implicit request => Ok)

  /**
   * ******************************************************************
   * YANDEX
   */
  /*
   * CreateNewWordstatReport, GetWordstatReportList, GetWordstatReport
   */
  def getWordstatReport = isAuth(user => implicit request => {
    val cs = API_bid.getCampaigns(user, "Yandex").get
    request.headers.get("login").map { l =>
      val login = l
      val token = cs.filter(_._login == login).headOption.map(_._token).getOrElse("")

      def wordstat(c: Campaign) = {
        println("<<<" + c.network_campaign_id + ">>>")

        val param = API_bid.getPhrases(user, "Yandex", c.network_campaign_id).get.as[List[JsValue]]
        println("!!!!!!!" + param)

        def ph10(phlist: List[JsValue]): Enumerator[JsValue] = {
          phlist match {
            case Nil => Enumerator.eof
            case phl =>
              val (cur, next) =
                if (phl.length > 10) {
                  (phl.take(10), phl.drop(10))
                } else {
                  (phl, Nil)
                }

              val js = Json.obj(("Phrases" -> cur))
              val wsr = API_yandex(login, token).getWordstat(js)
              println("Number of Words remain: " + (phl.length - 10))
              API_bid.postPhrasesStats(user, "Yandex", wsr) match {
                case true => println("!!! Wordstat report is POSTED !!!")
                case false => println("??? Failed... Wordstat report is NOT posted...")
              }
              //(wsr \ "data").as[List[JsValue]] ::: ph10(next)
              Enumerator(wsr) >- ph10(next)
          }
        }

        val enum = ph10(param)
        enum
      }

      request.headers.get("mode") match {
        case Some("test") =>
          val param = request.body.asJson.getOrElse(JsNull)
          val wsr = API_yandex(login, token).getWordstat(param)
          API_bid.postPhrasesStats(user, "Yandex", wsr) match {
            case true => println("!!! Wordstat report is POSTED !!!")
            case false => println("??? Failed... Wordstat report is NOT posted...")
          }
          Ok(Json.stringify(wsr))
        case _ =>
          val res = request
            .headers
            .get("campaignID")
            .map { cID =>
              cs.filter(c => c.network_campaign_id == cID & c._login == login).headOption map { c =>
                Ok.stream(wordstat(c) >>> Enumerator(JsString("The End")))
              } getOrElse {
                InternalServerError("??? Campaign or login is NOT found...")
              }
            }
            .getOrElse {
              val el = cs.filter(c => c._login == login) map { c =>
                wordstat(c)
              }
              val e = el.foldRight(Enumerator.eof[JsValue]) { case (e1, e2) => e1 >- e2 }

              Ok.stream(e >>> Enumerator(JsString("The End")))
            }
          res
      }
    }.getOrElse {
      Ok("??? Login parameter is not defined...")
    }
  })

}