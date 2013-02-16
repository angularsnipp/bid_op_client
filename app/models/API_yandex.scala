package models

import controllers.Yandex._

import play.api.libs.ws.{ WS, Response }
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import play.api.libs.concurrent.Execution.Implicits._

import java.util.Date

import play.api.libs.json._
import play.api.libs.functional.syntax._

import models.Reads._

import controllers.Yandex

object API_yandex {

  /* Generate request to Yandex Direct API as JSON String
   * and return response as JSON String 
   * T is a request(or input) type (i.e., GetBannersInfo or GetSummaryStatRequest)
   * */

  /****************************** POST request *****************************************/

  def post(jsData: JsValue): Response = {
    val result = WS.url(url).post[JsValue](jsData)
    Await.result(result, Duration.Inf)
  }

  /****************************** Methods implementation *******************************/

  /* PingAPI */
  def pingAPI(login: String, token: String): Boolean = {
    val response = API_yandex.post(
      InputData(
        login = login,
        token = token,
        method = "PingAPI"))

    (response.json \ ("data")).asOpt[Int].getOrElse(false) match {
      case 1 => true
      case _ => false
    }
  }

  /* GetCampaignsList */
  def getCampaignsList(login: String, token: String): Option[List[ShortCampaignInfo]] = {

    val response = API_yandex.post(
      InputData(
        login = login,
        token = token,
        method = "GetCampaignsList"))
    println("********" + response.json)
    println("%%%%%%%%" + response.json \ ("data")) 
    val campaigns_List = Json.fromJson[List[ShortCampaignInfo]](response.json \ ("data")).map {
      list => Some(list)
    }.recoverTotal(err => { println(err); None })

    /*listT match {
        case Nil => None
        case list => Some(list)
      }*/

    campaigns_List
  }

  /* GetBanners */
  def getBanners(login: String, token: String, campaignIDS: List[Int]): (Option[List[BannerInfo]], JsValue) = {

    val response = API_yandex.post(
      InputData(
        login = login,
        token = token,
        method = "GetBanners",
        param = GetBannersInfo(campaignIDS).toJson))

    val bannerInfo_List = Json.fromJson[List[BannerInfo]](response.json \ ("data")).map {
      list => Some(list)
    }.recoverTotal(err => None)

    (bannerInfo_List, response.json)

  }

  /* GetSummaryStat */
  def getSummaryStat(
    login: String,
    token: String,
    campaignIDS: List[Int],
    start_date: Date,
    end_date: Date): (Option[List[StatItem]], JsValue) = {

    val response = API_yandex.post(
      InputData(
        login = login,
        token = token,
        method = "GetSummaryStat",
        param = GetSummaryStatRequest(
          CampaignIDS = campaignIDS,
          StartDate = date_fmt.format(start_date),
          EndDate = date_fmt.format(end_date)).toJson))

    val statItem_List = Json.fromJson[List[StatItem]](response.json \ ("data")).map {
      list => Some(list)
    }.recoverTotal(err => None)

    (statItem_List, response.json)
  }

  /*-- detailed BannerPhrases report (at the END of the day)--*/
  /* CreateNewReport */
  def createNewReport(
    login: String,
    token: String,
    campaignID: Int,
    start_date: Date,
    end_date: Date): Option[Int] = {

    val response = API_yandex.post(
      InputData(
        login = login,
        token = token,
        method = "CreateNewReport",
        param = NewReportInfo(
          CampaignID = campaignID,
          StartDate = date_fmt.format(start_date),
          EndDate = date_fmt.format(end_date)).toJson))

    (response.json \ ("data")).asOpt[String] match {
      case None => None
      case Some(str) => Some(str.toInt)
    }
  }

  /* GetReportList */
  def getShortReportList(login: String, token: String): (Option[List[ShortReportInfo]], JsValue) = {

    val response = API_yandex.post(
      InputData(
        login = login,
        token = token,
        method = "GetReportList"))

    val report_List = Json.fromJson[List[ShortReportInfo]](response.json \ ("data")).map {
      list => Some(list)
    }.recoverTotal(err => None)

    (report_List, response.json)

  }
  def getReportList(json_reports: JsValue): Option[List[ReportInfo]] = {
    val report_List = Json.fromJson[List[ReportInfo]](json_reports).map {
      list => Some(list)
    }.recoverTotal(err => None)

    report_List
  }

  /* Download XML report*/
  def getXML(reportUrl: String): xml.Elem = {
    WS.url(reportUrl).get().value.get.get.xml
  }

  /* DeleteReport */
  def deleteReport(login: String, token: String, reportID: Int): Boolean = {

    val response = API_yandex.post(
      InputData(
        login = login,
        token = token,
        method = "DeleteReport",
        param = Json.toJson(reportID)))

    (response.json \ ("data")).asOpt[String].getOrElse(false) match {
      case "1" => true
      case _ => false
    }
  }

  /* UpdatePrices */
  //def updatePrice(login: String, token: String, phrasepriceInfo_List: List[PhrasePriceInfo]): Boolean = {
  def updatePrice(login: String, token: String, phrasepriceInfo_List: JsValue): Boolean = {
    val response = API_yandex.post(
      InputData(
        login = login,
        token = token,
        method = "UpdatePrices",
        param = phrasepriceInfo_List))

    (response.json \ ("data")).asOpt[String].getOrElse(false) match {
      case "1" => true
      case _ => false
    }
  }
}
