package models

import controllers.Yandex._

import com.codahale.jerkson.Json
import play.api.libs.ws.{ WS, Response }
import java.util.Date

object API_yandex {

  /* Generate request to Yandex Direct API as JSON String
   * and return response as JSON String 
   * T is a request(or input) type (i.e., GetBannersInfo or GetSummaryStatRequest)
   * */
  def post[T](data: inputData[T]): Response = {
    WS.url(url).post(Json generate data).value.get
  }

  /****************************** Methods implementation *******************************/

  /* PingAPI */
  def pingAPI(login: String, token: String) = {
    val res = API_yandex.post[None.type](
      data = inputData[None.type](
        authdata = authData_Yandex(
          login = login,
          token = token),
        method = "PingAPI"))

    (res.json \ ("data")).asOpt[String].getOrElse(false) match {
      case "1" => true
      case _ => false
    }
  }

  /* GetCampaignsList */
  def getCampaignsList(login: String, token: String): Option[List[ShortCampaignInfo]] = {

    val res = API_yandex.post[None.type](
      data = inputData[None.type](
        authdata = authData_Yandex(login, token),
        method = "GetCampaignsList"))

    val campaigns_List = responseData[ShortCampaignInfo](nullparser(res.body))

    campaigns_List
  }

  /* GetBanners */
  def getBanners(login: String, token: String, campaignIDS: List[Int]): (Option[List[BannerInfo]], String) = {

    val res = API_yandex.post[GetBannersInfo](
      data = inputData[GetBannersInfo](
        authdata = authData_Yandex(login, token),
        method = "GetBanners",
        param = GetBannersInfo(campaignIDS)))

    val bannerInfo_List = responseData[BannerInfo](nullparser(res.body))

    (bannerInfo_List, nullparser(res.body))
  }

  /* GetSummaryStat */
  def getSummaryStat(
    login: String,
    token: String,
    campaignIDS: List[Int],
    start_date: Date,
    end_date: Date): (Option[List[StatItem]], String) = {

    val res = API_yandex.post[GetSummaryStatRequest](
      data = inputData[GetSummaryStatRequest](
        authdata = authData_Yandex(
          login = login,
          token = token),
        method = "GetSummaryStat",
        param = GetSummaryStatRequest(
          CampaignIDS = campaignIDS,
          StartDate = date_fmt.format(start_date),
          EndDate = date_fmt.format(end_date))))

    val statItem_List = responseData[StatItem](nullparser(res.body))

    (statItem_List, nullparser(res.body))
  }

  /*-- detailed BannerPhrases report (at the END of the day)--*/
  /* CreateNewReport */
  def createNewReport(
    login: String,
    token: String,
    campaignID: Int,
    start_date: Date,
    end_date: Date): Option[Int] = {

    val res = API_yandex.post[NewReportInfo](
      data = inputData[NewReportInfo](
        authdata = authData_Yandex(
          login = login,
          token = token),
        method = "CreateNewReport",
        param = NewReportInfo(
          CampaignID = campaignID,
          StartDate = date_fmt.format(start_date),
          EndDate = date_fmt.format(end_date))))

    (res.json \ ("data")).asOpt[String] match {
      case None => None
      case Some(str) => Some(str.toInt)
    }
  }

  /* GetReportList */
  def getShortReportList(login: String, token: String): (Option[List[ShortReportInfo]], String) = {

    val res = API_yandex.post[None.type](
      data = inputData[None.type](
        authdata = authData_Yandex(
          login = login,
          token = token),
        method = "GetReportList"))

    val report_List = responseData[ShortReportInfo](res.body)

    (report_List, res.body)
  }
  def getReportList(json_reports: String): Option[List[ReportInfo]] = responseData[ReportInfo](json_reports)

  /* Download XML report*/
  def getXML(reportUrl: String): xml.Elem = {
    WS.url(reportUrl).get().value.get.xml
  }

  /* DeleteReport */
  def deleteReport(login: String, token: String, reportID: Int): Boolean = {

    val res = API_yandex.post[Int](
      data = inputData[Int](
        authdata = authData_Yandex(
          login = login,
          token = token),
        method = "DeleteReport",
        param = reportID))

    (res.json \ ("data")).asOpt[String].getOrElse(false) match {
      case "1" => true
      case _ => false
    }
  }

  /* UpdatePrices */
  def updatePrice(login: String, token: String, phrasepriceInfo_List: List[PhrasePriceInfo]): Boolean = {

    val res = API_yandex.post[List[PhrasePriceInfo]](
      data = inputData[List[PhrasePriceInfo]](
        authdata = authData_Yandex(login, token),
        method = "UpdatePrices",
        param = phrasepriceInfo_List))

    (res.json \ ("data")).asOpt[String].getOrElse(false) match {
      case "1" => true
      case _ => false
    }
  }
}
