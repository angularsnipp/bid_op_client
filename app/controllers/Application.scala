package controllers

import models._

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.ws.WS
import com.codahale.jerkson.Json

import org.joda.time.format
import org.joda.time._
import java.util.{ Date, Locale }
import java.text._

import play.api.data.validation.Constraints._

object Application extends Controller {

  // configuration parameters from conf/application.conf
  val conf = Play.current.configuration

  /* BID_OP information*/
  val Base_URI = conf.getString("url.api.bid").get

  /* YANDEX information*/
  val url = conf.getString("url.api.yandex").get
  val app_id = conf.getString("client_id.yandex").get
  val app_secret = conf.getString("client_secret.yandex").get
  val url_OAuthAuthorization = conf.getString("url.OAuth.authorization.yandex").get + app_id
  val url_OAuthToken = conf.getString("url.OAuth.token.yandex").get
  val url_apiLogin = conf.getString("url.api.login.yandex").get

  val iso_fmt = format.ISODateTimeFormat.dateTime()
  val date_fmt = new SimpleDateFormat("yyyy-MM-dd")

  /* ------------------ Actions ------------------ */

  def getBanners = Action { implicit request =>
    {
      request.body.asJson match {
        case None => BadRequest
        case Some(data) => {
          val user = (data \ ("user")).as[String]
          val net = (data \ ("net")).as[String]
          val login = (data \ ("_login")).as[String]
          val token = (data \ ("_token")).as[String]
          val id = (data \ ("network_campaign_id")).as[String]

          // get BannersInfo from Yandex
          val (bannerInfo_List, json_banners) = API_yandex.getBanners(login, token, List(id.toInt))

          // post BannersInfo to BID
          val bid_res = API_bid.postBannerReports(user, net, id, nullparser(json_banners))

          //return List[BannerInfo] to client browser
          Ok(Json generate bannerInfo_List)
        }
      }
    }
  }

  def postCampaign = Action { implicit request =>
    {
      request.body.asJson match {
        case None => BadRequest
        case Some(data) => {
          val jstring = API_bid.postCampaign(
            user = (data \ ("user")).as[String],
            net = (data \ ("net")).as[String],
            campaign = Campaign(
              _login = (data \ ("_login")).as[String],
              _token = (data \ ("_token")).as[String],
              network_campaign_id = (data \ ("network_campaign_id")).as[Long].toString(),
              start_date = iso_fmt.parseDateTime((data \ ("start_date")).as[String]),
              daily_budget = (data \ ("daily_budget")).as[Double]))

          println("CREATED CAMPAIGN!!!!!!!!!!")

          Created("SUCCESS!")
        }
      }
    }
  }

  def getStats = Action { implicit request =>
    { //During the day!!!
      request.body.asJson match {
        case None => BadRequest
        case Some(data) => {
          val camp = (data \ ("camp")).toString
          val c = Json.parse[Campaign](camp)

          val sdf = new SimpleDateFormat("dd MMMMM yyyy - HH:mm", Locale.US)

          val start_date = sdf.parse((data \ ("startDate")).as[String]) //c.start_date
          val end_date = sdf.parse((data \ ("endDate")).as[String]) //new DateTime()

          //Get Statistics from Yandex
          val (statItem_List, json_stat) = API_yandex.getSummaryStat(
            login = c._login,
            token = c._token,
            campaignIDS = List(c.network_campaign_id.toInt),
            start_date = start_date,
            end_date = end_date)

          //Post Statistics to BID
          val res_bid = API_bid.postStats(
            user = (data \ ("user")).as[String],
            net = (data \ ("net")).as[String],
            id = c.network_campaign_id,
            Performance = Performance(
              sd = new DateTime(start_date),
              ed = new DateTime(end_date),
              si = statItem_List.head))

          Ok(Json generate statItem_List.head)
        }
      }
    }
  }

  def getReport = Action { implicit request =>
    { //at the END of the day!!!
      request.body.asJson match {
        case None => BadRequest
        case Some(data) => {
          val camp = (data \ ("camp")).toString
          val c = Json.parse[Campaign](camp)
          val sdf = new SimpleDateFormat("dd MMMMM yyyy - HH:mm", Locale.US)

          val start_date = sdf.parse((data \ ("startDate")).as[String]) //c.start_date
          val end_date = sdf.parse((data \ ("endDate")).as[String]) //new DateTime()

          //create Report on Yandex server
          val newReportID = API_yandex.createNewReport(
            login = c._login,
            token = c._token,
            campaignID = c.network_campaign_id.toInt,
            start_date = start_date,
            end_date = end_date)
          println("!!! ReportID: " + newReportID)

          def getUrl: String = {
            try {
              val (short_reportInfo_List, json_reports) = API_yandex.getShortReportList(c._login, c._token)
              println(json_reports)
              val short_reportInfo = short_reportInfo_List.filter(_.ReportID == newReportID).head
              short_reportInfo.StatusReport match {
                case "Pending" => {
                  Thread.sleep(1000)
                  println("!!!!!! PENDING !!!!!");
                  getUrl
                }
                case "Done" => {
                  println("!!!!!! DONE !!!!!")
                  val reportInfo_List = API_yandex.getReportList(json_reports)
                  val reportInfo = reportInfo_List.filter(_.ReportID == newReportID).head
                  reportInfo.Url
                }
              }
            } catch {
              case t => {
                println("!!! EMPTY !!!")
                "EMPTY data"
              }
            }
          }

          //Get current report Url 
          val reportUrl = getUrl

          //download XML report from Yandex Url
          val xml_node = API_yandex.getXML(reportUrl)
          println("!!! XML: " + xml_node)

          //post report to BID
          val postToBid = API_bid.postReports(
            user = (data \ ("user")).as[String],
            net = (data \ ("net")).as[String],
            id = c.network_campaign_id,
            BannerPhrasePerformance = xml_node)
          println("!!!!!!" + postToBid)

          //remove current report from Yandex Server
          if (API_yandex.deleteReport(login = c._login, token = c._token, reportID = newReportID))
            println("!!! Report is DELETED !!!")
          else
            println("!!! Report is NOT DELETED !!!")

          Ok(Json generate postToBid)
        }
      }
    }
  }

  def getRecommendations = Action { implicit request =>
    {
      request.body.asJson match {
        case None => BadRequest
        case Some(data) => {

          val user = (data \ ("user")).as[String]
          val net = (data \ ("net")).as[String]
          val login = (data \ ("_login")).as[String]
          val token = (data \ ("_token")).as[String]
          val id = (data \ ("network_campaign_id")).as[String]

          //get Recommendations from BID
          val jstring = API_bid.getRecommendations(user, net, id, new DateTime().minusMonths(1))
          val ppInfo_List = responseData_bid[PhrasePriceInfo](jstring).get

          //Update Prices on Yandex
          val res =
            if (API_yandex.updatePrice(login, token, ppInfo_List))
              println("TRUE: Prices is updated!!!")
            else
              println("FALSE: Prices is NOT updated!!!")

          Ok(Json generate ppInfo_List)
        }
      }
    }
  }

  def clearDB = Action {
    val r = API_bid.clearDB
    println("!!! DB is CLEAR !!!")
    Ok("success!")
  }
}