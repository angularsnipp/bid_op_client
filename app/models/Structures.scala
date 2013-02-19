package models

import common.Yandex

import org.joda.time._
//import com.codahale.jerkson.Json
import scala.Nothing
import play.libs.Scala
import java.util.Date

import play.api.libs.json._
import play.api.libs.functional.syntax._

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
object InputData {
  def apply(login: String, token: String, method: String, param: JsValue = JsNull): JsValue =
    Json.toJson(
      Json.obj(
        "login" -> login,
        "token" -> token,
        "application_id" -> Yandex.app_id,
        "locale" -> "en",
        "method" -> method,
        "param" -> param))
}

/*--------- response data ----------*/

/* Parse json string with elements of T type. 
 * the main structure is: "data" -> List(T)
 * */
/*object responseData {
  def apply[T](jsValue: JsValue)(implicit m: Manifest[T]): Option[List[T]] = {
    //val dataMap = Json.parse[Map[String, List[T]]](jsonString)
    val dataMap = (jsValue \("data")).validate[List[T]]dataMap 
    val dataOpt = dataMap.get("data")
    dataOpt match {
      case None => None
      case Some(listT) => listT match {
        case Nil => None
        case list => Some(list)
      }
    }
  }
}*/

object responseData {
  def apply[T](jsData: JsValue)(implicit m: Manifest[T], f: Format[List[T]]): Option[List[T]] = {
    Json.fromJson[List[T]](jsData).map {
      listT => Some(listT)
    }.recoverTotal(err => None)

    /*listT match {
        case Nil => None
        case list => Some(list)
      }*/
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
  val Shows: Int = 0,
  val Clicks: Int = 0,
  val SumAvailableForTransfer: Option[Double] = Some(0.0),
  val Status: Option[String] = None,
  val StatusShow: Option[String] = None,
  val StatusArchive: Option[String] = None,
  val StatusActivating: Option[String] = None,
  val StatusModerate: Option[String] = None,
  val IsActive: Option[String] = None,
  val ManagerName: Option[String] = None,
  val AgencyName: Option[String] = None)

/* method GetSummaryStat -----  for postStats ----------------------------------------------*/
/* input T */
case class GetSummaryStatRequest(
  val CampaignIDS: List[Int],
  val StartDate: String, //Date
  val EndDate: String)

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

case class ReportInfo(
  val ReportID: Int,
  val Url: Option[String] = None,
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
  val BannerID: Long,
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
  val ContextPrice: Option[Double] = Some(0.0), //CPC on sites in the Yandex Advertising Network
  val AutoBroker: String = "", // Yes / No
  val Price: Double = 0.0, // Maximum CPC on Yandex search set for the phrase.
  val CurrentOnSearch: Option[Double] = Some(0.0) //The current CPC set by Autobroker
  )

/* method UpdatePrice -----------  use after getRecommendation ----------------------------------------------*/
/* input */
case class PhrasePriceInfo(
  val PhraseID: Int = 0,
  val BannerID: Int = 0,
  val CampaignID: Int = 0,
  val Price: Double = 0.0,
  val AutoBroker: Option[String] = Some("Yes"),
  val AutoBudgetPriority: Option[String] = Some("Medium"),
  val ContextPrice: Option[Double] = Some(0.0))

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
/*object responseData_bid {
  def apply[T](jsonString: String)(implicit m: Manifest[T]): Option[List[T]] = {
    try {
      Some(Json.parse[List[T]](jsonString))
    } catch {
      case t => None
    }
  }
}*/

/*object nullparser {
  //replace all "null" to "0" in Yandex response
  def apply(s: String): String = s.replaceAll("null", "0")
}*/
  