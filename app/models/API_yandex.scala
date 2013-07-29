package models

import yandex.direct._
import common.Yandex
import play.api.libs.ws.{ WS, Response }
import scala.concurrent.{ Future, Await }
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits._
import java.util.Date
import play.api.libs.json._
import play.api.libs.functional.syntax._
import json_api.Convert._
import org.joda.time.DateTime

case class API_yandex(
  val login: String,
  val token: String,
  val url: String = Yandex.url) {

  /* Generate request to Yandex Direct API as JSON String
   * and return response as JSON String 
   * T is a request(or input) type (i.e., GetBannersInfo or GetSummaryStatRequest)
   * */

  /*
   * Initialize Direct class
   */
  val direct = Direct(login, token, Yandex.app_id, url = url)

  /****************************** Methods implementation *******************************/

  /* PingAPI */
  def pingAPI: Boolean = {
    val fres = direct.pingAPI

    (fres \ ("data")).asOpt[Int].getOrElse(false) match { case 1 => true case _ => false }
  }

  /* GetClientInfo */
  def getClientInfo(logins: List[String]): Option[List[ClientInfo]] = {
    val fres = direct.getClientInfo(Json.toJson(logins))

    fromJson[List[ClientInfo]](fres \ ("data"))

  }
  /* GetClientsList 
   * this method is available only for Agency
   * */
  def getClientsList: Option[List[ClientInfo]] = {
    val fres = direct.getClientsList(JsNull)

    fromJson[List[ClientInfo]](fres \ ("data"))
  }

  /* GetCampaignsList */
  def getCampaignsList(param: List[String] /* List of client Logins */ ): Option[List[ShortCampaignInfo]] = {
    val jparam = param match {
      case List("") => JsNull //for simple user
      case par => Json.toJson(param) //for Agency
    }
    val fres = direct.getCampaignsList(jparam)

    fromJson[List[ShortCampaignInfo]](fres \ ("data"))

  }

  /* GetBanners */
  def getBanners(campaignIDS: List[Int]): (Option[List[BannerInfo]], JsValue) = {
    val fres = direct.getBanners(toJson[GetBannersInfo](GetBannersInfo(campaignIDS)))

    (fromJson[List[BannerInfo]](fres \ ("data")), fres)

  }

  /* GetSummaryStat */
  def getSummaryStat(campaignIDS: List[Int], start_date: Date, end_date: Date): (Option[List[StatItem]], JsValue) = {
    val fres = direct.getSummaryStat(
      toJson[GetSummaryStatRequest](
        GetSummaryStatRequest(
          CampaignIDS = campaignIDS,
          StartDate = Yandex.date_fmt.format(start_date),
          EndDate = Yandex.date_fmt.format(end_date))))

    (fromJson[List[StatItem]](fres \ ("data")), fres)

  }

  /*-- detailed BannerPhrases report (DURING the day)--*/
  /* GetBannersStat */
  def getBannersStat(campaignID: Int, start_date: Date, end_date: Date): (Option[GetBannersStatResponse], JsValue) = {
    val fres = direct.getBannersStat(
      toJson[NewReportInfo](
        NewReportInfo(
          CampaignID = campaignID,
          StartDate = Yandex.date_fmt.format(start_date),
          EndDate = Yandex.date_fmt.format(end_date))))

    (fromJson[GetBannersStatResponse](fres \ ("data")), fres)

  }

  /*-- detailed BannerPhrases report (at the END of the day)--*/
  /* CreateNewReport */
  def createNewReport(campaignID: Int, start_date: Date, end_date: Date): Option[Int] = {
    val fres = direct.createNewReport(
      toJson[NewReportInfo](
        NewReportInfo(
          CampaignID = campaignID,
          StartDate = Yandex.date_fmt.format(start_date),
          EndDate = Yandex.date_fmt.format(end_date))))

    (fres \ ("data")).asOpt[Int]

  }

  /* GetReportList */
  def getReportList: (Option[List[ReportInfo]], JsValue) = {
    val fres = direct.getReportList

    (fromJson[List[ReportInfo]](fres \ ("data")), fres)

  }

  /* Download XML report*/
  def getXML(reportUrl: String): xml.Elem = { //won't require login and token 
    val fres = WS.url(reportUrl).get()
      .map { response =>
        response.xml
      }
    Await.result(fres, Duration.Inf)
  }

  /* DeleteReport */
  def deleteReport(reportID: Int): Boolean = {
    val fres = direct.deleteReport(Json.toJson(reportID))

    (fres \ ("data")).asOpt[Int].getOrElse(false) match { case 1 => true case _ => false }

  }

  /* UpdatePrices */
  def updatePrice(phrasepriceInfo: List[PhrasePriceInfo]): Boolean = {
    val fres = direct.updatePrices(toJson[List[PhrasePriceInfo]](phrasepriceInfo))

    (fres \ ("data")).asOpt[Int].getOrElse(false) match { case 1 => true case _ => false }

  }

  /* 
   * Copy full campaign   
   */
  def copyCampaign(campaignID: Int): Int = {
    val param = direct.getCampaignsParams(campaignIDS = List(campaignID)) match {
      case JsNull => JsNull
      case jspar => {

        val jsparfilter = (jspar \ "data")
          .as[List[JsObject]]
          .filter(_ \ "CampaignID" == JsNumber(campaignID))
          .head

        val zeroID = JsObject(Seq(("CampaignID" -> JsNumber(0))))
        val name = JsObject(Seq(("Name" -> JsString("NewCampaign"))))

        Json.toJson(jsparfilter ++ zeroID ++ name)
      }
    }
    //println("<<< " + param + " >>>")
    val res = direct.createOrUpdateCampaign(param)
    println("<<< " + res + " >>>")
    (res \ ("data")).asOpt[Int].getOrElse(0)
  }
}