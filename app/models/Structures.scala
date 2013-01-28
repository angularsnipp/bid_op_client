package models

import controllers.Bid._
import controllers.Yandex._

import org.joda.time._
import com.codahale.jerkson.Json
import scala.Nothing
import play.libs.Scala
import java.util.Date

/*--------------------------------------------------------------------------------------------------*/
/*---------------------------------------WEB FORMS--------------------------------------------------*/

/*--------- for web forms implementation ----------*/
case class cForm(
  //User: String = "",
  Network: String = "",
  CampaignID: Option[String] = None)

/*--------------------------------------------------------------------------------------------------*/
/*---------------------------------------YANDEX API-------------------------------------------------*/

/*--------- input data ----------*/
/* main structure of input data*/
case class authData(
  val base_URI: String = "",
  val url: String = "",
  val login: String = "",
  val application_id: String = "",
  val token: String = "",
  val locale: String = "")

object authData_Yandex {
  def apply(login: String, token: String): authData = authData(
    base_URI = Base_URI,
    url = url,
    login = login,
    application_id = app_id,
    token = token,
    locale = "en")
}

case class inputData[T](
  @transient val authdata: authData,
  val method: String = "",
  val param: T = None) {
  val login = authdata.login
  val application_id = authdata.application_id
  val token = authdata.token
  val locale = authdata.locale
}

/*--------- response data ----------*/

/* Parse json string with elements of T type. 
 * the main structure is: "data" -> List(T)
 * */
object responseData {
  def apply[T](jsonString: String)(implicit m: Manifest[T]): Option[List[T]] = {
    val dataMap = Json.parse[Map[String, List[T]]](jsonString)
    val dataOpt = dataMap.get("data")
    dataOpt match {
      case None => None
      case Some(listT) => listT match {
        case Nil => None
        case list => Some(list)
      }
    }
  }
}

/*----------- METHODS ------------*/

/* respond data for method GetCampaignsList */
case class ShortCampaignInfo(
  val CampaignID: Int = 0,
  val Login: String = "",
  val Name: String = "",
  val StartDate: DateTime = new DateTime,
  val Sum: Double = 0.0,
  val Rest: Double = 0.0,
  val SumAvailableForTransfer: Double = 0.0,
  val Shows: Int = 0,
  val Clicks: Int = 0,
  val Status: String = "",
  val StatusShow: String = "",
  val StatusArchive: String = "",
  val StatusActivating: String = "",
  val StatusModerate: String = "",
  val IsActive: String = "",
  val ManagerName: String = "",
  val AgencyName: String = "")

/* method GetSummaryStat -----  for postStats ----------------------------------------------*/
/* input T */
case class GetSummaryStatRequest(
  val CampaignIDS: List[Int],
  val StartDate: String, //Date
  val EndDate: String) //Date) //DateTime = new DateTime)

/* output List[T] */
case class StatItem(
  // other parameters are not useful yet  
  val SumSearch: Double = 0.0,
  val SumContext: Double = 0.0,
  val ShowsSearch: Int = 0,
  val ShowsContext: Int = 0,
  val ClicksSearch: Int = 0,
  val ClicksContext: Int = 0)

/* ------------------------  for postReports ----------------------------------------------*/
/* ----- method CreateNewReport ------------------------------*/
/* input T */
case class NewReportInfo(
  val CampaignID: Int,
  val StartDate: String, //Date
  val EndDate: String, //Date
  val GroupByColumns: List[String] = List("clBanner", "clPhrase")) //, "clPage", "clGeo", "clPositionType"))

/* output Report ID : Int,  {"data" : 123456} */

/* ----- method GetReportList --------------------------------*/
/* input T  - None.type */
/* output List[T] */
case class ShortReportInfo(
  val ReportID: Int,
  val StatusReport: String)

case class ReportInfo(
  val ReportID: Int,
  val Url: String = "",
  val StatusReport: String)

/* ----- method DeleteReport ---------------------------------*/
/* input T  - Report ID : Int */
/* output If success: {"data" : 1} */

/* method GetBanners (Live) -----  for postBannerReports ----------------------------------------------*/
/* input */
case class GetBannersInfo(
  val CampaignIDS: List[Int],
  //val FieldsNames: List[String] = List("BannerID", "Text", "Geo", "Phrases"),
  val GetPhrases: String = "WithPrices")

/* output */
case class BannerInfo(
  val BannerID: Int,
  val Text: String,
  val Geo: String,
  val Phrases: List[BannerPhraseInfo])

case class BannerPhraseInfo(
  val BannerID: Long = 0,
  val PhraseID: Long = 0,
  val CampaignID: Long = 0,
  val Phrase: String = "",
  val Min: Double = 0.0,
  val Max: Double = 0.0,
  val PremiumMin: Double = 0.0,
  val PremiumMax: Double = 0.0,
  //  val ContextPrice: Double = 0.0, //CPC on sites in the Yandex Advertising Network
  val AutoBroker: String = "", // Yes / No
  val Price: Double = 0.0, // Maximum CPC on Yandex search set for the phrase.
  val CurrentOnSearch: Double = 0.0 //The current CPC set by Autobroker
  )

/* method UpdatePrice -----------  use after getRecommendation ----------------------------------------------*/
/* input */
case class PhrasePriceInfo(
  val PhraseID: Int = 0,
  val BannerID: Int = 0,
  val CampaignID: Int = 0,
  val Price: Double = 0.0) {
  /////////////////////////
  def AutoBroker: String = "Yes"
  def AutoBudgetPriority: String = "Medium"
  def ContextPrice: Double = 0.0
}

/* output */
// {"data" : 1} if successful 

/*--------------------------------------------------------------------------------------------------*/
/*----------------------------------------BID API---------------------------------------------------*/

/*
 * getUser :- () -> List[User] --- take List(0)
 * getCampaigns :- () -> List[Campaign]
 * getCampaign :- () -> List[Campaign]  --- take List(0)
 * postCampaign :- Campaign -> header = "Created" or "Bad Request"
 * postStats :- Performance -> header = "Created" or "Bad Request"
 * postReports :- TODO
 * postBannerReports :- {"data" : List[BannerInfo]} -> header = "Created" or "Bad Request"
 * getRecommendations :- Headers: If-Modified-Since: Date Time -> List[PhrasePriceInfo]
 * */

/* Parse json string of BID API response with elements of T type. 
 * the main structure is: List(T)
 * */
object responseData_bid {
  def apply[T](jsonString: String)(implicit m: Manifest[T]): Option[List[T]] = {
    try {
      Some(Json.parse[List[T]](jsonString))
    } catch {
      case t => None
    }
  }
}

object nullparser {
  //replace all "null" to "0" in Yandex response
  def apply(s: String): String = s.replaceAll("null", "0")
}
  