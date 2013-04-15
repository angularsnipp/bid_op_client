package models

import common.Yandex
import play.api.libs.ws.{ WS, Response }
import scala.concurrent.{ Future, Await }
import scala.concurrent.duration._
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

  /* send request repeatedly */
  def withRetry[T](n: Int, dl: Deadline)(f: => Future[T]): Future[T] = {
    f.recoverWith { //if failed
      case t: Throwable =>
        if ((n > 0) & (dl.hasTimeLeft))
          withRetry(n - 1, dl)(f)
        else {
          println("=== 5 attempts have made === ")
          f
        }
    }
  }

  /* check if failed */
  def isFailed(response: Response): Boolean = {
    val oerr = (response.json \ ("error_code")).asOpt[Int] //isDefined //if an error is defined
    if (oerr.isDefined) println("<<< " + response.json + " >>>")
    oerr.isDefined
  }

  def post(method: String, param: JsValue = JsNull): Future[Response] = {
    val jsData = InputData(login, token, method, param)

    def wsCall = WS.url(url).post[JsValue](jsData) map { response =>
      require(!isFailed(response)) //success is required
      response
    }

    withRetry(n = 5, 5.minutes.fromNow)(wsCall) //try to repeat WS calling up to 5 times until success
  }

  /****************************** Methods implementation *******************************/

  /* PingAPI */
  def pingAPI: Boolean = {
    val fres = post("PingAPI").map { response =>
      (response.json \ ("data")).asOpt[Int].getOrElse(false) match { case 1 => true case _ => false }
    }
    Await.result(fres, Duration.Inf)
  }

  /* GetClientInfo */
  def getClientInfo(logins: List[String]): Future[Option[List[ClientInfo]]] = {
    val fres = post("GetClientInfo", Json.toJson(logins))
      .map { response =>
        fromJson[List[ClientInfo]](response.json \ ("data"))
      }
    fres
    //Await.result(fres, Duration.Inf)
  }
  /* GetClientsList 
   * this method is available only for Agency
   * */
  def getClientsList: Option[List[ClientInfo]] = {
    val fres = post("GetClientsList")
      .map { response =>
        fromJson[List[ClientInfo]](response.json \ ("data"))
      }
    //fres
    Await.result(fres, Duration.Inf)
  }

  /* GetCampaignsList */
  def getCampaignsList(param: List[String] /* List of client Logins */ ): Future[Option[List[ShortCampaignInfo]]] = {
    val jparam = param match {
      case List("") => JsNull //for simple user
      case par => Json.toJson(param) //for Agency
    }
    val fres = post("GetCampaignsList", jparam)
      .map { response =>
        fromJson[List[ShortCampaignInfo]](response.json \ ("data"))
      }
    fres
  }

  /* GetBanners */
  def getBanners(campaignIDS: List[Int]): Future[(Option[List[BannerInfo]], JsValue)] = {
    val fres = post("GetBanners", toJson[GetBannersInfo](GetBannersInfo(campaignIDS)))
      .map { response =>
        (fromJson[List[BannerInfo]](response.json \ ("data")), response.json)
      }
    fres
  }

  /* GetSummaryStat */
  def getSummaryStat(campaignIDS: List[Int], start_date: Date, end_date: Date): Future[(Option[List[StatItem]], JsValue)] = {
    val fres = post(
      "GetSummaryStat",
      toJson[GetSummaryStatRequest](
        GetSummaryStatRequest(
          CampaignIDS = campaignIDS,
          StartDate = Yandex.date_fmt.format(start_date),
          EndDate = Yandex.date_fmt.format(end_date))))
      .map { response =>
        (fromJson[List[StatItem]](response.json \ ("data")), response.json)
      }
    fres
  }

  /*-- detailed BannerPhrases report (DURING the day)--*/
  /* GetBannersStat */
  def getBannersStat(campaignID: Int, start_date: Date, end_date: Date): Future[(Option[GetBannersStatResponse], JsValue)] = {
    val fres = post(
      "GetBannersStat",
      toJson[NewReportInfo](
        NewReportInfo(
          CampaignID = campaignID,
          StartDate = Yandex.date_fmt.format(start_date),
          EndDate = Yandex.date_fmt.format(end_date))))
      .map { response =>
        (fromJson[GetBannersStatResponse](response.json \ ("data")), response.json)
      }
    fres
  }

  /*-- detailed BannerPhrases report (at the END of the day)--*/
  /* CreateNewReport */
  def createNewReport(campaignID: Int, start_date: Date, end_date: Date): Future[Option[Int]] = {
    val fres = post(
      "CreateNewReport",
      toJson[NewReportInfo](
        NewReportInfo(
          CampaignID = campaignID,
          StartDate = Yandex.date_fmt.format(start_date),
          EndDate = Yandex.date_fmt.format(end_date))))
      .map { response =>
        (response.json \ ("data")).asOpt[Int]
      }
    fres
  }

  /* GetReportList */
  def getReportList: (Option[List[ReportInfo]], JsValue) = {
    val fres = post("GetReportList")
      .map { response =>
        (fromJson[List[ReportInfo]](response.json \ ("data")), response.json)
      }
    Await.result(fres, Duration.Inf)
  }

  /* Download XML report*/
  def getXML(reportUrl: String): Future[xml.Elem] = { //won't require login and token 
    WS.url(reportUrl).get()
      .map { response =>
        response.xml
      }
  }

  /* DeleteReport */
  def deleteReport(reportID: Int): Boolean = {
    val fres = post("DeleteReport", Json.toJson(reportID))
      .map { response =>
        (response.json \ ("data")).asOpt[Int].getOrElse(false) match { case 1 => true case _ => false }
      }
    Await.result(fres, Duration.Inf)
  }

  /* UpdatePrices */
  def updatePrice(phrasepriceInfo: List[PhrasePriceInfo]): Boolean = {
    val fres = post("UpdatePrices", toJson[List[PhrasePriceInfo]](phrasepriceInfo))
      .map { response =>
        (response.json \ ("data")).asOpt[Int].getOrElse(false) match { case 1 => true case _ => false }
      }
    Await.result(fres, Duration.Inf)
  }
}