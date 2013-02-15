package controllers

import play.api.mvc._

import play.api.libs.ws.WS
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import play.api.libs.concurrent.Execution.Implicits._

import play.mvc.Http
import java.text._
import models.API_yandex

object Yandex extends Controller {

  /**-------------------------------- YANDEX information ----------------------------------**/
  /**--------------------------------------------------------------------------------------**/

  /* url for YANDEX api */
  //val url = "https://api-sandbox.direct.yandex.ru/json-api/v4/" //SANDBOX - for testing 
  val url = "https://api.direct.yandex.ru/live/v4/json/" //works (recommended)
  //val url = "https://soap.direct.yandex.ru/json-api/v4/" //works  

  /* application information for YANDEX api */
  val app_id = "bee99a08160d4287aa0e468b9be7ed91" //client_id
  val app_secret = "d08e6a30a6d44c3b9da147f3e76880de" //client_secret

  /* OAuth information */
  val url_OAuthAuthorization = "https://oauth.yandex.ru/authorize?response_type=code&client_id=" + app_id //url for user Authorization on Yandex
  val url_OAuthToken = "https://oauth.yandex.ru/token" //url for getting TOKEN using authorization code

  /* url for YANDEX api to get login */
  val url_apiLogin = "https://api.direct.yandex.ru/live/v4/json/me/" //failed ((

  /* Date format */
  val date_fmt = new SimpleDateFormat("yyyy-MM-dd")

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
  def isSuccess(login: String, token: String): Boolean = API_yandex.pingAPI(login, token)

}

/* try to get Login
  val response_login = WS.url(url_apiLogin).
  	withHeaders(("Authorization", "OAuth " + token)).get().value.get.body
    println("!!!! YANDEX RESPONSE LOGIN: " + response_login)
*/