package controllers

import common.Yandex._

import play.api.mvc._

import play.api.libs.ws.WS
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import play.api.libs.concurrent.Execution.Implicits._

import play.mvc.Http
import models.API_yandex

object OAuth_yandex extends Controller {

  /**-------------------------------- YANDEX OAuth methods ----------------------------------**/
  /**----------------------------------------------------------------------------------------**/

  /* GET authorization CODE */
  def oAuth = Action {
    Redirect(url_OAuthAuthorization)
  }

  /* GET TOKEN using authorization code */
  def getToken(code: String): Option[String] = {
    val result = WS.url(url_OAuthToken)
      .post("grant_type=authorization_code" +
        "&code=" + code +
        "&client_id=" + app_id +
        "&client_secret=" + app_secret)
      .map { response =>
        if (response.status == Http.Status.OK) {
          (response.json \ ("access_token")).asOpt[String]
        } else None
      }
    Await.result(result, Duration.Inf)
  }

  /* Check network is alive and user authorization is successful*/
  def isSuccess(login: String, token: String): Boolean = API_yandex(login, token).pingAPI
}

/* try to get Login
  val response_login = WS.url(url_apiLogin).
  	withHeaders(("Authorization", "OAuth " + token)).get().value.get.body
    println("!!!! YANDEX RESPONSE LOGIN: " + response_login)
*/