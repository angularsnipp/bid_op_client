package models

import common.Bid._
import json_api.Convert._

import play.api.libs.json._
import json_api.Formats._
import play.api.libs.ws.WS
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import play.api.libs.concurrent.Execution.Implicits._
import org.joda.time._
import scala.collection.immutable.List
import play.mvc.Http
import json_api.Formats

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
        if (response.status == Http.Status.OK)
          fromJson[User](response.json) else None
      }
    Await.result(result, Duration.Inf)
  }

  def postUser(user: User): Option[User] = {
    val result = WS.url(Base_URI + "/user")
      .post[JsValue](toJson[User](user))
      .map { response =>
        if (response.status == Http.Status.CREATED)
          fromJson[User](response.json) else None
      }
    Await.result(result, Duration.Inf)
  }

  def getCampaigns(
    user: User,
    net: String): Option[List[Campaign]] = {
    val result = WS.url(Base_URI + "/user/" + user.name + "/net/" + net + "/camp")
      .withHeaders(("password" -> user.password))
      .get()
      .map { response =>
        if (response.status == Http.Status.OK) {
          val campaigns_List = (response.json \ ("key1")).validate[List[Campaign]].map {
            list => Some(list)
          }.recoverTotal(err => None)
          campaigns_List
        } else None
      }

    Await.result(result, Duration.Inf)
  }

  def getCampaign(
    user: User,
    net: String,
    id: String): Option[Campaign] = {
    val result = WS.url(Base_URI + "/user/" + user.name + "/net/" + net + "/camp/" + id)
      .withHeaders(("password" -> user.password))
      .get()
      .map { response =>
        if (response.status == Http.Status.OK) {
          val campaigns_List = (response.json \ ("key1")).validate[List[Campaign]].map {
            list => Some(list)
          }.recoverTotal(err => None)
          campaigns_List.get.headOption
        } else None
      }

    Await.result(result, Duration.Inf)
  }

  def postCampaign(
    user: User,
    net: String,
    campaign: Campaign): Option[Campaign] = {
    val result = WS.url(Base_URI + "/user/" + user.name + "/net/" + net + "/camp")
      .withHeaders(("password" -> user.password))
      .post[JsValue](Json.toJson(campaign)(Formats.campaign))
      .map { response =>
        if (response.status == Http.Status.CREATED) Some(campaign) else None
      }

    Await.result(result, Duration.Inf)
  }

  def postStats( /*DURING the day*/
    user: User,
    net: String,
    id: String,
    performance: Performance): Option[Performance] = {
    val result = WS.url(Base_URI + "/user/" + user.name + "/net/" + net + "/camp/" + id + "/stats")
      .withHeaders(("password" -> user.password))
      .post[JsValue](Json.toJson(performance)(Formats.performance))
      .map { response =>
        if (response.status == Http.Status.CREATED) Some(performance) else None
      }

    Await.result(result, Duration.Inf)
  }

  def postReports( /*at the END of the day*/
    user: User,
    net: String,
    id: String,
    bannerPhrasePerformance: xml.Elem): Option[xml.Elem] = {
    val result = WS.url(Base_URI + "/user/" + user.name + "/net/" + net + "/camp/" + id + "/reports")
      .withHeaders(("password" -> user.password))
      .post[xml.Elem](bannerPhrasePerformance)
      .map { response =>
        if (response.status == Http.Status.CREATED) Some(bannerPhrasePerformance) else None
      }

    Await.result(result, Duration.Inf)
  }

  def postBannerReports( /*ActualBids and NetAdvisedBids*/
    user: User,
    net: String,
    id: String,
    bannerPhraseReport: JsValue): Boolean = {
    val result = WS.url(Base_URI + "/user/" + user.name + "/net/" + net + "/camp/" + id + "/bannerreports")
      .withHeaders(("password" -> user.password))
      .post[JsValue](bannerPhraseReport)
      .map { response =>
        if (response.status == Http.Status.CREATED) true else false
      }

    Await.result(result, Duration.Inf)
  }

  def getRecommendations(
    user: User,
    net: String,
    id: String,
    datetime: DateTime = new DateTime): Option[List[models.PhrasePriceInfo]] = {
    val result = WS.url(Base_URI + "/user/" + user.name + "/net/" + net + "/camp/" + id + "/recommendations")
      .withHeaders(("If-Modified-Since" -> datetime.toString()), ("password" -> user.password))
      .get()
      .map { response =>
        if (response.status == Http.Status.OK) {
          implicit val phrasePriceInfo = Json.format[PhrasePriceInfo]
          Json.fromJson[List[PhrasePriceInfo]](response.json).map {
            list => Some(list)
          }.recoverTotal(err => None)
        } else None
      }

    Await.result(result, Duration.Inf)
  }

  def clearDB: Boolean = {
    val result = WS.url(Base_URI + "/clear_db")
      .get()
      .map { response =>
        if (response.status == Http.Status.OK) true else false
      }

    Await.result(result, Duration.Inf)
  }
}