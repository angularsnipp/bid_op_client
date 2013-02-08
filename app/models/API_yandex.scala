package models

import controllers.Yandex._
import play.api.libs.ws.{ WS, Response }
import java.util.Date

import play.api.libs.json._
import play.api.libs.functional.syntax._

import models.Formats._

import controllers.Yandex

object API_yandex {

  /* Generate request to Yandex Direct API as JSON String
   * and return response as JSON String 
   * T is a request(or input) type (i.e., GetBannersInfo or GetSummaryStatRequest)
   * */
  
  /****************************** POST request *****************************************/

  def post(jsData: JsValue): Response = {
    WS.url(url).post(Json.stringify(jsData)).value.get.get
  }

  /****************************** Methods implementation *******************************/

  /* PingAPI */
  def pingAPI(login: String, token: String): Boolean = {
    val res = API_yandex.post(
      InputData(
        login = login,
        token = token,
        method = "PingAPI"))

    (res.json \ ("data")).asOpt[String].getOrElse(false) match {
      case "1" => true
      case _ => false
    }
  }

  /* GetCampaignsList */
  def getCampaignsList(login: String, token: String): Option[List[ShortCampaignInfo]] = {

    val res = API_yandex.post(
      InputData(
        login = login,
        token = token,
        method = "GetCampaignsList"))

    val campaigns_List = Json.fromJson[List[ShortCampaignInfo]](res.json \ ("data")).map {
      list => Some(list)
    }.recoverTotal(err => None)

    /*listT match {
        case Nil => None
        case list => Some(list)
      }*/

    campaigns_List
  }

  /* GetBanners */
  def getBanners(login: String, token: String, campaignIDS: List[Int]): (Option[List[BannerInfo]], JsValue) = {

    val res = API_yandex.post(
      InputData(
        login = login,
        token = token,
        method = "GetBanners",
        param = GetBannersInfo(campaignIDS).toJson))

    val bannerInfo_List = Json.fromJson[List[BannerInfo]](res.json \ ("data")).map {
      list => Some(list)
    }.recoverTotal(err => None)

    (bannerInfo_List, res.json)

  }

  /* GetSummaryStat */
  def getSummaryStat(
    login: String,
    token: String,
    campaignIDS: List[Int],
    start_date: Date,
    end_date: Date): (Option[List[StatItem]], JsValue) = {

    val res = API_yandex.post(
      InputData(
        login = login,
        token = token,
        method = "GetSummaryStat",
        param = GetSummaryStatRequest(
          CampaignIDS = campaignIDS,
          StartDate = date_fmt.format(start_date),
          EndDate = date_fmt.format(end_date)).toJson))

    val statItem_List = Json.fromJson[List[StatItem]](res.json \ ("data")).map {
      list => Some(list)
    }.recoverTotal(err => None)

    (statItem_List, res.json)
  }

  /*-- detailed BannerPhrases report (at the END of the day)--*/
  /* CreateNewReport */
  def createNewReport(
    login: String,
    token: String,
    campaignID: Int,
    start_date: Date,
    end_date: Date): Option[Int] = {

    val res = API_yandex.post(
      InputData(
        login = login,
        token = token,
        method = "CreateNewReport",
        param = NewReportInfo(
          CampaignID = campaignID,
          StartDate = date_fmt.format(start_date),
          EndDate = date_fmt.format(end_date)).toJson))

    (res.json \ ("data")).asOpt[String] match {
      case None => None
      case Some(str) => Some(str.toInt)
    }
  }

  /* GetReportList */
  def getShortReportList(login: String, token: String): (Option[List[ShortReportInfo]], JsValue) = {

    val res = API_yandex.post(
      InputData(
        login = login,
        token = token,
        method = "GetReportList"))

    val report_List = Json.fromJson[List[ShortReportInfo]](res.json \ ("data")).map {
      list => Some(list)
    }.recoverTotal(err => None)

    (report_List, res.json)

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

    val res = API_yandex.post(
      InputData(
        login = login,
        token = token,
        method = "DeleteReport",
        param = Json.toJson(reportID)))

    (res.json \ ("data")).asOpt[String].getOrElse(false) match {
      case "1" => true
      case _ => false
    }
  }

  /* UpdatePrices */
  //def updatePrice(login: String, token: String, phrasepriceInfo_List: List[PhrasePriceInfo]): Boolean = {
  def updatePrice(login: String, token: String, phrasepriceInfo_List: JsValue): Boolean = {
    val res = API_yandex.post(
      InputData(
        login = login,
        token = token,
        method = "UpdatePrices",
        param = phrasepriceInfo_List))

    (res.json \ ("data")).asOpt[String].getOrElse(false) match {
      case "1" => true
      case _ => false
    }
  }
}
