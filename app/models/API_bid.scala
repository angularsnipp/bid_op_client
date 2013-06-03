package models

import common.Bid._
import json_api.Convert._

import play.api.libs.json._
import play.api.libs.ws.WS
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import play.api.libs.concurrent.Execution.Implicits._
import org.joda.time._
import play.mvc.Http

object API_bid {
  /* Generate request to Bid Optimizer API as JSON String
   * and return response as JSON String 
   * * user - user name
   * * net - network name (i.e., Yandex)
   * * id - network campaign id
   * */

  /* STATUS:
     200 - Ok
     201 - Created
     202 - Accepted
     204 - NoContent
     400 - BadRequest 
     401 - Unauthorized
     403 - Forbidden
     404 - NotFound
   */

  /* ----------------- Method implementation -------------------------------- */

  /*def getUser(
    user: User): String = {
    WS.url(Base_URI + "/user/" + user.name).
      get().value.get.body
  }*/

  def getUser(user: User): Option[User] = {
    val result = WS.url(Base_URI + "/user/" + user.name)
      .withHeaders(("password" -> user.password))
      .get()
      .map { response =>
        if (response.status == Http.Status.OK) fromJson[User](response.json) else None
      }
    Await.result(result, Duration.Inf)
  }

  def postUser(user: User): Option[User] = {
    val result = WS.url(Base_URI + "/user")
      .post[JsValue](toJson[User](user))
      .map { response =>
        if (response.status == Http.Status.CREATED) fromJson[User](response.json) else None
      }
    Await.result(result, Duration.Inf)
  }

  def getCampaigns(user: User, net: String): Option[List[Campaign]] = {
    val result = WS.url(Base_URI + "/user/" + user.name + "/net/" + net + "/camp")
      .withHeaders(("password" -> user.password))
      .get()
      .map { response =>
        if (response.status == Http.Status.OK) fromJson[List[Campaign]](response.json) else None
      }

    Await.result(result, Duration.Inf)
  }

  def getCampaign(user: User, net: String, id: String): Option[Campaign] = {
    val result = WS.url(Base_URI + "/user/" + user.name + "/net/" + net + "/camp/" + id)
      .withHeaders(("password" -> user.password))
      .get()
      .map { response =>
        if (response.status == Http.Status.OK)
          fromJson[List[Campaign]](response.json).map(cl => cl.headOption).getOrElse(None)
        else
          None
      }

    Await.result(result, Duration.Inf)
  }

  def postCampaign(user: User, net: String, campaign: Campaign): Option[Campaign] = {
    val result = WS.url(Base_URI + "/user/" + user.name + "/net/" + net + "/camp")
      .withHeaders(("password" -> user.password))
      .post[JsValue](toJson[Campaign](campaign))
      .map { response =>
        if (response.status == Http.Status.CREATED) fromJson[Campaign](response.json) else None
      }

    Await.result(result, Duration.Inf)
  }

  /*DURING the day - Campaigns Stats*/
  def postCampaignStats(user: User, net: String, id: String, performance: Performance, mpList: List[PerformanceMetrika] = Nil): Option[Performance] = {
    val jsdata = Json.obj(
      "direct" -> toJson[Performance](performance),
      "metrika" -> toJson[List[PerformanceMetrika]](mpList))
    val result = WS.url(Base_URI + "/user/" + user.name + "/net/" + net + "/camp/" + id + "/stats")
      .withHeaders(("password" -> user.password))
      .post[JsValue](Json.toJson(jsdata))
      .map { response =>
        if (response.status == Http.Status.CREATED) fromJson[Performance](response.json) else None
      }

    Await.result(result, Duration.Inf)
  }

  /*DURING the day - BannerPhrase Stats*/
  def postBannersStats(user: User, net: String, id: String, getBannersStatResponse: GetBannersStatResponse, cur_dt: DateTime, mpList: List[PerformanceMetrika] = Nil): Option[GetBannersStatResponse] = {
    val jsdata = Json.obj(
      "direct" -> toJson[GetBannersStatResponse](getBannersStatResponse),
      "metrika" -> toJson[List[PerformanceMetrika]](mpList))
    val result = WS.url(Base_URI + "/user/" + user.name + "/net/" + net + "/camp/" + id + "/bannersstats")
      .withHeaders(("password" -> user.password), ("current_datetime" -> iso_fmt.print(cur_dt)))
      .post[JsValue](Json.toJson(jsdata))
      .map { response =>
        if (response.status == Http.Status.CREATED) Some(getBannersStatResponse) else None
      }

    Await.result(result, Duration.Inf)
  }

  /*at the END of the day*/
  def postReports(user: User, net: String, id: String, bannerPhrasePerformance: xml.Elem): Option[xml.Elem] = {
    val result = WS.url(Base_URI + "/user/" + user.name + "/net/" + net + "/camp/" + id + "/reports")
      .withHeaders(("password" -> user.password))
      .post[xml.Elem](bannerPhrasePerformance)
      .map { response =>
        if (response.status == Http.Status.CREATED) Some(bannerPhrasePerformance) else None
      }

    Await.result(result, Duration.Inf)
  }

  /*ActualBids and NetAdvisedBids*/
  def postBannerReports(user: User, net: String, id: String, bannerPhraseReport: List[BannerInfo]): Boolean = {
    val result = WS.url(Base_URI + "/user/" + user.name + "/net/" + net + "/camp/" + id + "/bannerreports")
      .withHeaders(("password" -> user.password))
      .post[JsValue](toJson[List[BannerInfo]](bannerPhraseReport))
      .map { response =>
        if (response.status == Http.Status.CREATED) true else false
      }

    Await.result(result, Duration.Inf)
  }

  def getRecommendations(user: User, net: String, id: String, datetime: DateTime = new DateTime): Option[List[PhrasePriceInfo]] = {
    val result = WS.url(Base_URI + "/user/" + user.name + "/net/" + net + "/camp/" + id + "/recommendations")
      .withHeaders(("If-Modified-Since" -> datetime.toString()), ("password" -> user.password))
      .get()
      .map { response =>
        if (response.status == Http.Status.OK) fromJson[List[PhrasePriceInfo]](response.json) else None
      }

    Await.result(result, Duration.Inf)
  }

  /* Metrika Reports */
  def postMetrikaReports(user: User, net: String, metrikaReport: JsValue): Boolean = {
    val result = WS.url(Base_URI + "/user/" + user.name + "/net/" + net + "/metrika/stats")
      .withHeaders(("password" -> user.password))
      .post[JsValue](metrikaReport)
      .map { response =>
        if (response.status == Http.Status.CREATED) true else false
      }

    Await.result(result, Duration.Inf)
  }

  def clearDB(user: User): Boolean = {
    val result = WS.url(Base_URI + "/clearDB/" + user.name)
      .withHeaders(("password" -> user.password))
      .get()
      .map { response =>
        if (response.status == Http.Status.OK) true else false
      }

    Await.result(result, Duration.Inf)
  }

  /*def getCharts(user: User, net: String, id: String): Boolean = {
    val result = WS.url(Base_URI + "/user/" + user.name + "/net/" + net + "/camp/" + id + "/charts")
      .withHeaders(("password" -> user.password))
      .get()
      .map { response =>
        if (response.status == Http.Status.OK) true else false
      }

    Await.result(result, Duration.Inf)
  }*/
}