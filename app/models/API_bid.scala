package models

import controllers.Application._

import com.codahale.jerkson.Json.generate
import play.api.libs.json.Json.parse
import play.api.libs.json.JsValue

import play.api.libs.ws.WS
import org.joda.time._
import scala.collection.immutable.List

object API_bid {
  /* Generate request to Bid Optimizer API as JSON String
   * and return response as JSON String 
   * * user - user name
   * * net - network name (i.e., Yandex)
   * * id - network campaign id
   * */

  def getUser(
    user: String): String = {
    WS.url(Base_URI + "/user/" + user).
      get().value.get.body
  }

  def getUser(
    user: String,
    password: String): String = {
    val pass = Map("password" -> password)
    WS.url(Base_URI + "/user/" + user).
      post[JsValue](parse(generate(pass))).value.get.body
  }

  def postUser(
    user: String,
    password: String): String = {
    val user_pass = Map("name" -> user, "password" -> password)
    WS.url(Base_URI + "/user").
      post[JsValue](parse(generate(user_pass))).value.get.body
  }

  def getCampaigns(
    user: String,
    net: String): String = {
    WS.url(Base_URI + "/user/" + user + "/net/" + net + "/camp").
      get().value.get.body
  }

  def getCampaign(
    user: String,
    net: String,
    id: String): String = {
    WS.url(Base_URI + "/user/" + user + "/net/" + net + "/camp/" + id).
      get().value.get.body
  }

  def postCampaign(
    user: String,
    net: String,
    campaign: Campaign): String = {
    WS.url(Base_URI + "/user/" + user + "/net/" + net + "/camp").
      post[JsValue](parse(generate(campaign))).value.get.body
  } 

  def postStats( /*DURING the day*/
    user: String,
    net: String,
    id: String,
    Performance: Performance): String = {
    WS.url(Base_URI + "/user/" + user + "/net/" + net + "/camp/" + id + "/stats").
      post[JsValue](parse(generate(Performance))).value.get.body
  }

  def postReports( /*at the END of the day*/
    user: String,
    net: String,
    id: String,
    BannerPhrasePerformance: String): String = {
    WS.url(Base_URI + "/user/" + user + "/net/" + net + "/camp/" + id + "/reports").
      post(BannerPhrasePerformance).value.get.body
  }

  def postBannerReports( /*ActualBids and NetAdvisedBids*/
    user: String,
    net: String,
    id: String,
    BannerPhraseReport: String): String = {
    WS.url(Base_URI + "/user/" + user + "/net/" + net + "/camp/" + id + "/bannerreports").
      post[JsValue](parse(BannerPhraseReport)).value.get.body
  }

  def getRecommendations(
    user: String,
    net: String,
    id: String,
    datetime: DateTime = new DateTime): String = {
    WS.url(Base_URI + "/user/" + user + "/net/" + net + "/camp/" + id + "/recommendations").
      withHeaders(("If-Modified-Since", datetime.toString())).get().value.get.body
  }

  def clearDB = WS.url(Base_URI + "/clear_db").get().value.get.body
}