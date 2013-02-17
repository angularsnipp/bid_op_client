package models

import common.Yandex
import play.api.libs.ws.{ WS, Response }
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import play.api.libs.concurrent.Execution.Implicits._
import java.util.Date
import play.api.libs.json._
import play.api.libs.functional.syntax._
import models.Reads._


object API_yandex {

  /* Generate request to Yandex Direct API as JSON String
   * and return response as JSON String 
   * T is a request(or input) type (i.e., GetBannersInfo or GetSummaryStatRequest)
   * */

  /****************************** POST request *****************************************/

  def post(jsData: JsValue, url: String = Yandex.url): Response = {
    val result = WS.url(url).post[JsValue](jsData)
    Await.result(result, Duration.Inf)
  }

  /****************************** Methods implementation *******************************/

  /* PingAPI */
  def pingAPI(login: String, token: String, url: String = Yandex.url): Boolean = {
    val response = API_yandex.post(
      InputData(
        login = login,
        token = token,
        method = "PingAPI"),
      url)

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

    getCampaignsList(response.json \ ("data"))
  }

  def getCampaignsList(response_data: JsValue): Option[List[ShortCampaignInfo]] = {
    val campaigns_List = Json.fromJson[List[ShortCampaignInfo]](response_data).map {
      list => Some(list)
    }.recoverTotal(err => { println(err); None })
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

    (getBanners(response.json \ ("data")), response.json)
  }

  def getBanners(response_data: JsValue): Option[List[BannerInfo]] = {
    val bannerInfo_List = Json.fromJson[List[BannerInfo]](response_data).map {
      list => Some(list)
    }.recoverTotal(err => None)
    bannerInfo_List
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
          StartDate = Yandex.date_fmt.format(start_date),
          EndDate = Yandex.date_fmt.format(end_date)).toJson))

    (getSummaryStat(response.json \ ("data")), response.json)
  }

  def getSummaryStat(response_data: JsValue): Option[List[StatItem]] = {
    val statItem_List = Json.fromJson[List[StatItem]](response_data).map {
      list => Some(list)
    }.recoverTotal(err => None)
    statItem_List
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
          StartDate = Yandex.date_fmt.format(start_date),
          EndDate = Yandex.date_fmt.format(end_date)).toJson))

    (response.json \ ("data")).asOpt[Int]
  }

  /* GetReportList */
  def getShortReportList(login: String, token: String): (Option[List[ShortReportInfo]], JsValue) = {

    val response = API_yandex.post(
      InputData(
        login = login,
        token = token,
        method = "GetReportList"))

    (getShortReportList(response.json \ ("data")), response.json)
  }

  def getShortReportList(response_data: JsValue): Option[List[ShortReportInfo]] = {
    val report_List = Json.fromJson[List[ShortReportInfo]](response_data).map {
      list => Some(list)
    }.recoverTotal(err => None)
    report_List
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

    (response.json \ ("data")).asOpt[Int].getOrElse(false) match {
      case 1 => true
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

    (response.json \ ("data")).asOpt[Int].getOrElse(false) match {
      case 1 => true
      case _ => false
    }
  }
}
