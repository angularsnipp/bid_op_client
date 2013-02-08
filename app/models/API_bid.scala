package models

import controllers.Bid._

//import com.codahale.jerkson.Json.generate

import play.api.libs.json._
import play.api.libs.functional.syntax._

import models.Formats._

import play.api.libs.ws.WS
import org.joda.time._
import scala.collection.immutable.List
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
    val res = WS.url(Base_URI + "/user/" + user.name).
      withHeaders(("password" -> user.password)).get().value.get.get

    if (res.status == Http.Status.OK) Some(user) else None
  }

  def postUser(user: User): Option[User] = {
    val res = WS.url(Base_URI + "/user").post[JsValue](Json.toJson(user)(Formats.user)).value.get.get

    if (res.status == Http.Status.CREATED) Some(user) else None
  }

  def getCampaigns(
    user: User,
    net: String): Option[List[Campaign]] = {
    val res = WS.url(Base_URI + "/user/" + user.name + "/net/" + net + "/camp").
      withHeaders(("password" -> user.password)).get().value.get.get

    if (res.status == Http.Status.OK) {
      val campaigns_List = Json.fromJson[List[Campaign]](res.json).map {
        list => Some(list)
      }.recoverTotal(err => None)
      campaigns_List
    } else None
  }

  def getCampaign(
    user: User,
    net: String,
    id: String): Option[Campaign] = {
    val res = WS.url(Base_URI + "/user/" + user.name + "/net/" + net + "/camp/" + id).
      withHeaders(("password" -> user.password)).get().value.get.get

    if (res.status == Http.Status.OK) {
      val campaigns_List = Json.fromJson[List[Campaign]](res.json).map {
        list => Some(list)
      }.recoverTotal(err => None)
      campaigns_List.get.headOption
    } else None
  }

  def postCampaign(
    user: User,
    net: String,
    campaign: Campaign): Option[Campaign] = {
    val res = WS.url(Base_URI + "/user/" + user.name + "/net/" + net + "/camp").
      withHeaders(("password" -> user.password)).post[JsValue](Json.toJson(campaign)(Formats.campaign)).value.get.get

    if (res.status == Http.Status.CREATED) Some(campaign) else None
  }

  def postStats( /*DURING the day*/
    user: User,
    net: String,
    id: String,
    performance: Performance): Option[Performance] = {
    val res = WS.url(Base_URI + "/user/" + user.name + "/net/" + net + "/camp/" + id + "/stats").
      withHeaders(("password" -> user.password)).post[JsValue](Json.toJson(performance)(Formats.performance)).value.get.get

    if (res.status == Http.Status.CREATED) Some(performance) else None
  }

  def postReports( /*at the END of the day*/
    user: User,
    net: String,
    id: String,
    bannerPhrasePerformance: xml.Elem): Option[xml.Elem] = {
    val res = WS.url(Base_URI + "/user/" + user.name + "/net/" + net + "/camp/" + id + "/reports").
      withHeaders(("password" -> user.password)).post[xml.Elem](bannerPhrasePerformance).value.get.get

    if (res.status == Http.Status.CREATED) Some(bannerPhrasePerformance) else None
  }

  def postBannerReports( /*ActualBids and NetAdvisedBids*/
    user: User,
    net: String,
    id: String,
    bannerPhraseReport: JsValue): Boolean = {
    val res = WS.url(Base_URI + "/user/" + user.name + "/net/" + net + "/camp/" + id + "/bannerreports").
      withHeaders(("password" -> user.password)).post[JsValue](bannerPhraseReport).value.get.get

    if (res.status == Http.Status.CREATED) true else false
  }

  def getRecommendations(
    user: User,
    net: String,
    id: String,
    datetime: DateTime = new DateTime): Option[List[models.PhrasePriceInfo]] = {
    val res = WS.url(Base_URI + "/user/" + user.name + "/net/" + net + "/camp/" + id + "/recommendations").
      withHeaders(("If-Modified-Since" -> datetime.toString()), ("password" -> user.password)).get().value.get.get

    if (res.status == Http.Status.OK) {
      implicit val phrasePriceInfo = Json.format[PhrasePriceInfo]
      val k = Json.fromJson[List[PhrasePriceInfo]](res.json).map {
        list => Some(list)
      }.recoverTotal(err => None)
      k
    } else None
  }

  def clearDB: Boolean = {
    val res = WS.url(Base_URI + "/clear_db").get().value.get.get
    if (res.status == Http.Status.OK) true else false
  }
}