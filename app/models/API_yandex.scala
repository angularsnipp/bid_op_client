package models

import common.Yandex
import play.api.libs.ws.{ WS, Response }
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import play.api.libs.concurrent.Execution.Implicits._
import java.util.Date
import play.api.libs.json._
import play.api.libs.functional.syntax._

import json_api.Convert._

case class API_yandex(
  val login: String,
  val token: String,
  val url: String = Yandex.url) {

  /* Generate request to Yandex Direct API as JSON String
   * and return response as JSON String 
   * T is a request(or input) type (i.e., GetBannersInfo or GetSummaryStatRequest)
   * */

  /****************************** POST request *****************************************/

  def post(method: String, param: JsValue = JsNull): Response = {
    val jsData = InputData(login, token, method, param)
    val result = WS.url(url).post[JsValue](jsData)
    Await.result(result, Duration.Inf)

  }

  /****************************** Methods implementation *******************************/

  /* PingAPI */
  def pingAPI: Boolean = {
    val response = post("PingAPI")
    (response.json \ ("data")).asOpt[Int].getOrElse(false) match { case 1 => true case _ => false }
  }

  /* GetCampaignsList */
  def getCampaignsList: Option[List[ShortCampaignInfo]] = {
    val response = post("GetCampaignsList")

    fromJson[List[ShortCampaignInfo]](response.json \ ("data"))
  }

  /* GetBanners */
  def getBanners(campaignIDS: List[Int]): (Option[List[BannerInfo]], JsValue) = {
    val response = post("GetBanners", toJson[GetBannersInfo](GetBannersInfo(campaignIDS)))

    (fromJson[List[BannerInfo]](response.json \ ("data")), response.json)
  }

  /* GetSummaryStat */
  def getSummaryStat(campaignIDS: List[Int], start_date: Date, end_date: Date): (Option[List[StatItem]], JsValue) = {
    val response = post(
      "GetSummaryStat",
      toJson[GetSummaryStatRequest](
        GetSummaryStatRequest(
          CampaignIDS = campaignIDS,
          StartDate = Yandex.date_fmt.format(start_date),
          EndDate = Yandex.date_fmt.format(end_date))))

    (fromJson[List[StatItem]](response.json \ ("data")), response.json)

  }

  /*-- detailed BannerPhrases report (DURING the day)--*/
  /* GetBannersStat */
  def getBannersStat(campaignID: Int, start_date: Date, end_date: Date): (Option[GetBannersStatResponse], JsValue) = {
    val response = post(
      "GetBannersStat",
      toJson[NewReportInfo](
        NewReportInfo(
          CampaignID = campaignID,
          StartDate = Yandex.date_fmt.format(start_date),
          EndDate = Yandex.date_fmt.format(end_date))))

    (fromJson[GetBannersStatResponse](response.json \ ("data")), response.json)
  }

  /*-- detailed BannerPhrases report (at the END of the day)--*/
  /* CreateNewReport */
  def createNewReport(campaignID: Int, start_date: Date, end_date: Date): Option[Int] = {
    val response = post(
      "CreateNewReport",
      toJson[NewReportInfo](
        NewReportInfo(
          CampaignID = campaignID,
          StartDate = Yandex.date_fmt.format(start_date),
          EndDate = Yandex.date_fmt.format(end_date))))

    (response.json \ ("data")).asOpt[Int]
  }

  /* GetReportList */
  def getReportList: (Option[List[ReportInfo]], JsValue) = {
    val response = post("GetReportList")

    (fromJson[List[ReportInfo]](response.json \ ("data")), response.json)
  }

  /* Download XML report*/
  def getXML(reportUrl: String): xml.Elem = { //won't require login and token 
    Await.result(WS.url(reportUrl).get(), Duration.Inf).xml
  }

  /* DeleteReport */
  def deleteReport(reportID: Int): Boolean = {
    val response = post("DeleteReport", Json.toJson(reportID))

    (response.json \ ("data")).asOpt[Int].getOrElse(false) match { case 1 => true case _ => false }
  }

  /* UpdatePrices */
  def updatePrice(phrasepriceInfo: List[PhrasePriceInfo]): Boolean = {
    val response = post("UpdatePrices", toJson[List[PhrasePriceInfo]](phrasepriceInfo))

    (response.json \ ("data")).asOpt[Int].getOrElse(false) match { case 1 => true case _ => false }
  }
}