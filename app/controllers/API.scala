package controllers

import play.api.mvc._
import models._
import play.api.libs.json._

import scala.concurrent.{ Future, Promise }
import scala.concurrent.ExecutionContext.Implicits.global
import java.util.concurrent.TimeUnit

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
    val login = request.headers.get("login").get
    val token = cs.filter(_._login == login).headOption.map(_._token).getOrElse("")

    val res = request
      .headers
      .get("campaignID")
      .map { cID =>
        cs.filter(_.network_campaign_id == cID).headOption map { c =>
          val param = API_bid.getPhrases(user, "Yandex", cID).get.as[List[JsValue]]
          println("!!!!!!!" + param)
          val js = Json.obj(("Phrases" -> param.slice(0, 9)))
          val wsr = API_yandex(login, token).getWordstat(js)

          Ok(Json.stringify(wsr))
        } getOrElse {
          BadRequest("??? Campaign not found...")
        }
      }
      .getOrElse {
        val param = request.body.asJson.getOrElse(JsNull)
        //println("<<< param: " + login + " - " + token + " >>>")
        val wsr = API_yandex(login, token).getWordstat(param)
        Ok(Json.stringify(wsr))
      }

    res
  })

}