package models

import controllers.Yandex._

import com.codahale.jerkson.Json
import play.api.libs.ws.WS
import java.util.Date

object API_yandex {

  /* Generate request to Yandex Direct API as JSON String
   * and return response as JSON String 
   * T is a request(or input) type (i.e., GetBannersInfo or GetSummaryStatRequest)
   * */
  def post[T](data: inputData[T]): String = {
    println("*************API_Yandex************")
    WS.url(url).post(Json generate data).value.get.body
  }

  /****************************** Methods implementation *******************************/

  /* PingAPI */
  def pingAPI(login: String, token: String) = {
    val json_res = API_yandex.post[None.type](
      data = inputData[None.type](
        authdata = authData_Yandex(
          login = login,
          token = token),
        method = "PingAPI"))

    val res = Json.parse[Map[String, String]](json_res)

    res.get("data").getOrElse(false) match {
      case "1" => true
      case _ => false
    }
  }

  /* GetCampaignsList */
  def getCampaignsList(login: String, token: String): List[ShortCampaignInfo] = {

    val json_campaigns = API_yandex.post[None.type](
      data = inputData[None.type](
        authdata = authData_Yandex(login, token),
        method = "GetCampaignsList"))

    val campaigns_List = responseData[ShortCampaignInfo](nullparser(json_campaigns))

    campaigns_List
  }

  /* GetBanners */
  def getBanners(login: String, token: String, CampaignIDS: List[Int]): (List[BannerInfo], String) = {

    val json_banners = API_yandex.post[GetBannersInfo](
      data = inputData[GetBannersInfo](
        authdata = authData_Yandex(login, token),
        method = "GetBanners",
        param = GetBannersInfo(CampaignIDS)))

    val bannerInfo_List = responseData[BannerInfo](nullparser(json_banners))

    (bannerInfo_List, nullparser(json_banners))
  }

  /* GetSummaryStat */
  def getSummaryStat(
    login: String,
    token: String,
    campaignIDS: List[Int],
    start_date: Date,
    end_date: Date): (List[StatItem], String) = {

    val json_stat = API_yandex.post[GetSummaryStatRequest](
      data = inputData[GetSummaryStatRequest](
        authdata = authData_Yandex(
          login = login,
          token = token),
        method = "GetSummaryStat",
        param = GetSummaryStatRequest(
          CampaignIDS = campaignIDS,
          StartDate = date_fmt.format(start_date),
          EndDate = date_fmt.format(end_date))))

    val statItem_List = responseData[StatItem](json_stat)

    (statItem_List, json_stat)
  }

  /*-- detailed BannerPhrases report (at the END of the day)--*/
  /* CreateNewReport */
  def createNewReport(
    login: String,
    token: String,
    campaignID: Int,
    start_date: Date,
    end_date: Date): Int = {

    val json_reportID = API_yandex.post[NewReportInfo](
      data = inputData[NewReportInfo](
        authdata = authData_Yandex(
          login = login,
          token = token),
        method = "CreateNewReport",
        param = NewReportInfo(
          CampaignID = campaignID,
          StartDate = date_fmt.format(start_date),
          EndDate = date_fmt.format(end_date))))

    val reportID = Json.parse[Map[String, String]](json_reportID)
    println("***********" + json_reportID)
    reportID.get("data").get.toInt
  }

  /* GetReportList */
  def getShortReportList(login: String, token: String): (List[ShortReportInfo], String) = {

    val json_reports = API_yandex.post[None.type](
      data = inputData[None.type](
        authdata = authData_Yandex(
          login = login,
          token = token),
        method = "GetReportList"))

    val report_List = responseData[ShortReportInfo](json_reports)

    (report_List, json_reports)
  }
  def getReportList(json_reports: String): List[ReportInfo] = responseData[ReportInfo](json_reports)

  /* Download XML report*/
  def getXML(reportUrl: String): xml.Elem = {
    val b = WS.url(reportUrl).get()
    val f = b.value.get.xml
    println("****************" + b.toString())
    f
  }

  /* DeleteReport */
  def deleteReport(login: String, token: String, reportID: Int) = {

    val json_res = API_yandex.post[Int](
      data = inputData[Int](
        authdata = authData_Yandex(
          login = login,
          token = token),
        method = "DeleteReport",
        param = reportID))

    val res = Json.parse[Map[String, String]](json_res)

    res.get("data").getOrElse(false) match {
      case "1" => true
      case _ => false
    }
  }

  /* UpdatePrices */
  def updatePrice(login: String, token: String, phrasepriceInfo_List: List[PhrasePriceInfo]): Boolean = {

    val json_res = API_yandex.post[List[PhrasePriceInfo]](
      data = inputData[List[PhrasePriceInfo]](
        authdata = authData_Yandex(login, token),
        method = "UpdatePrices",
        param = phrasepriceInfo_List))

    val res = Json.parse[Map[String, String]](json_res)

    res.get("data").getOrElse(false) match {
      case "1" => true
      case _ => false
    }
  }

}
