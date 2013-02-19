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

    fromJson[List[ShortCampaignInfo]](response.json \ ("data"))
  }

  /* GetBanners */
  def getBanners(login: String, token: String, campaignIDS: List[Int]): (Option[List[BannerInfo]], JsValue) = {
    val response = API_yandex.post(
      InputData(
        login = login,
        token = token,
        method = "GetBanners",
        param = toJson[GetBannersInfo](GetBannersInfo(campaignIDS))))

    (fromJson[List[BannerInfo]](response.json \ ("data")), response.json)
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
        param = toJson[GetSummaryStatRequest](
          GetSummaryStatRequest(
            CampaignIDS = campaignIDS,
            StartDate = Yandex.date_fmt.format(start_date),
            EndDate = Yandex.date_fmt.format(end_date)))))

    (fromJson[List[StatItem]](response.json \ ("data")), response.json)
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
        param = toJson[NewReportInfo](
          NewReportInfo(
            CampaignID = campaignID,
            StartDate = Yandex.date_fmt.format(start_date),
            EndDate = Yandex.date_fmt.format(end_date)))))

    (response.json \ ("data")).asOpt[Int]
  }

  /* GetReportList */
  def getReportList(login: String, token: String): (Option[List[ReportInfo]], JsValue) = {
    val response = API_yandex.post(
      InputData(
        login = login,
        token = token,
        method = "GetReportList"))

    (fromJson[List[ReportInfo]](response.json \ ("data")), response.json)
  }

  /* Download XML report*/
  def getXML(reportUrl: String): xml.Elem = {
    Await.result(WS.url(reportUrl).get(), Duration.Inf).xml
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
  def updatePrice(login: String, token: String, phrasepriceInfo: List[PhrasePriceInfo]): Boolean = {
    val response = API_yandex.post(
      InputData(
        login = login,
        token = token,
        method = "UpdatePrices",
        param = toJson[List[PhrasePriceInfo]](phrasepriceInfo)))

    (response.json \ ("data")).asOpt[Int].getOrElse(false) match {
      case 1 => true
      case _ => false
    }
  }
}
