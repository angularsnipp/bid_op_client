package controllers

import models._
import common.Bid
import play.api._
import play.api.mvc._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.mvc.BodyParsers.parse
import play.api.data.Forms._
import play.api.libs.ws.WS
import org.joda.time._
import java.util.{ Date, Locale }
import java.text._
import play.api.data.validation.Constraints._
import play.api.libs.json._

import json_api.Convert._

object Workspace extends Controller {

  /****************************** POST request *****************************************/

  /* ------------------ Actions ------------------ */

  def getBanners = Action(parse.json) { implicit request =>
    //Async {
    val data = request.body
    val user = User.findByName((data \ ("user")).as[String]).get
    val net = (data \ ("net")).as[String]
    val login = (data \ ("_login")).as[String]
    val token = (data \ ("_token")).as[String]
    val id = (data \ ("network_campaign_id")).as[String]

    // get BannersInfo from Yandex
    API_yandex(login, token)
      .getBanners(List(id.toInt)) match {
        case (bannerInfo_List, json_banners) =>
          val biList = (json_banners \ "data").asOpt[List[JsValue]]
          if (biList.isDefined) {
            println("!!! SUCCESS getBanners !!!")

            // post BannersInfo to BID
            if (API_bid.postBannerReports(user, net, id, json_banners \ "data"))
              println("!!! ActualBids and NetAdvisedBids is POSTED to BID !!!")
            else
              println("??? ActualBids and NetAdvisedBids is NOT POSTED to BID !!!")

            //return List[BannerInfo] to client browser
            Ok(toJson[List[BannerInfo]](bannerInfo_List.get))
          } else {
            println("??? FAILED getBanners ???");
            BadRequest
          }

        /*if (bannerInfo_List.isDefined) {
              println("!!! SUCCESS getBanners !!!")

              // post BannersInfo to BID
              if (API_bid.postBannerReports(user, net, id, bannerInfo_List.get))
                println("!!! ActualBids and NetAdvisedBids is POSTED to BID !!!")
              else
                println("??? ActualBids and NetAdvisedBids is NOT POSTED to BID !!!")

              //return List[BannerInfo] to client browser
              Ok(toJson[List[BannerInfo]](bannerInfo_List.get))
            } else println("??? FAILED getBanners ???"); BadRequest*/
      }
    //}
  }

  def postCampaign = Action(parse.json) { implicit request =>
    {
      val data = request.body

      val user = User.findByName((data \ ("user")).as[String]).get
      val net = (data \ ("net")).as[String]
      val start_date = Bid.iso_fmt.parseDateTime((data \ ("start_date")).as[String])

      val camp = API_bid.postCampaign(
        user = user,
        net = net,
        campaign = Campaign(
          _login = (data \ ("_login")).as[String],
          _token = (data \ ("_token")).as[String],
          _clientLogin = (data \ ("_clientLogin")).as[String],
          network_campaign_id = (data \ ("network_campaign_id")).as[Long].toString(),
          start_date = start_date,
          daily_budget = (data \ ("daily_budget")).as[Double],
          strategy = (data \ ("strategy")).as[String]))

      if (camp.isDefined) {
        println("!!! SUCCESS postCampaign !!!")

        // Get First Detailed Report from Yandex direct
        // It should be done for sync direct and metrika phrase IDs
        Future {
          (new jobs.LongScheduler).get_post_BPP(user, net, camp.get, new DateTime(), Some(start_date))
        } onSuccess {
          case _ => println("!!! START - First BPP for: " + user.name + ", " + camp.get.network_campaign_id + " !!!")
        }

        Created
      } else {
        println("??? FAILED postCampaign ???");
        BadRequest
      }
    }
  }

  def getStats = Action(parse.json) { implicit request =>
    //Async { //During the day!!!
    val data = request.body

    val c = fromJson[Campaign](data \ ("camp")).get

    val sdf = new SimpleDateFormat("dd MMMMM yyyy - HH:mm", Locale.US)

    val start_date = sdf.parse((data \ ("startDate")).as[String]) //c.start_date
    val end_date = sdf.parse((data \ ("endDate")).as[String]) //new DateTime()

    //Get Statistics from Yandex
    API_yandex(c._login, c._token)
      .getSummaryStat(
        campaignIDS = List(c.network_campaign_id.toInt),
        start_date = start_date,
        end_date = end_date) match {
          case (statItem_List, json_stat) =>
            if (statItem_List.isDefined) {
              println("!!! SUCCESS getStats !!!")

              //Post Statistics to BID
              val res_bid = API_bid.postCampaignStats(
                user = User.findByName((data \ ("user")).as[String]).get,
                net = (data \ ("net")).as[String],
                id = c.network_campaign_id,
                performance = Performance._apply(
                  sd = new DateTime(start_date),
                  ed = new DateTime(end_date),
                  si = statItem_List.get))
              if (res_bid.isDefined)
                println("!!! Stats is POSTED to BID !!!")
              else
                println("??? Stats is NOT POSTED to BID ???")

              Ok(toJson[StatItem](statItem_List.get.head))
            } else println("??? FAILED getStats ???"); BadRequest
        }
    //}
  }

  //at the END of the day!!!
  def getReport = Action(parse.json) { implicit request =>
    //Async { //for createNewReport method
    val data = request.body
    val c = fromJson[Campaign](data \ ("camp")).get

    val sdf = new SimpleDateFormat("dd MMMMM yyyy - HH:mm", Locale.US)

    val start_date = sdf.parse((data \ ("startDate")).as[String]) //c.start_date
    val end_date = sdf.parse((data \ ("endDate")).as[String]) //new DateTime()

    //create Report on Yandex server
    API_yandex(c._login, c._token)
      .createNewReport(
        campaignID = c.network_campaign_id.toInt,
        start_date = start_date,
        end_date = end_date)
      .map { id =>
        //Get current report Url 
        getUrl(id, c._login, c._token) map { reportUrl =>
          //download XML report from Yandex Url
          API_yandex(c._login, c._token)
            .getXML(reportUrl) match {
              case xml_node =>

                //post report to BID
                val postToBid = API_bid.postReports(
                  user = User.findByName((data \ ("user")).as[String]).get,
                  net = (data \ ("net")).as[String],
                  id = c.network_campaign_id,
                  bannerPhrasePerformance = xml_node)
                if (postToBid.isDefined)
                  println("!!! Report is POSTED to BID !!!")
                else
                  println("??? Report is NOT POSTED to BID ???")

                //remove current report from Yandex Server
                if (API_yandex(c._login, c._token).deleteReport(id))
                  println("!!! Report is DELETED from Yandex!!!")
                else
                  println("??? Report is NOT DELETED from Yandex ???")

                Ok(xml_node) as XML
            }
        } getOrElse { BadRequest }
      } getOrElse (BadRequest)
    //}
  }

  def getUrl(id: Int, login: String, token: String): Option[String] = {
    val (reportInfo_List, json_reports) = API_yandex(login, token).getReportList
    reportInfo_List map { ril =>
      val reportInfo = ril.filter(_.ReportID == id).head
      reportInfo.StatusReport match {
        case "Pending" => {
          Thread.sleep(5000)
          println("!!!!!! PENDING !!!!!");
          getUrl(id, login, token)
        }
        case "Done" => {
          println("!!!!!! DONE !!!!!")
          reportInfo.Url
        }
      }
    } getOrElse (None)
  }

  def getRecommendations = Action(parse.json) { implicit request =>
    {
      val data = request.body
      val user = User.findByName((data \ ("user")).as[String]).get
      val net = (data \ ("net")).as[String]
      val login = (data \ ("_login")).as[String]
      val token = (data \ ("_token")).as[String]
      val id = (data \ ("network_campaign_id")).as[String]

      //get Recommendations from BID
      val ppInfo_List = API_bid.getRecommendations(user, net, id, new DateTime().minusMonths(2))

      if (ppInfo_List.isDefined) {
        println("!!! SUCCESS: Recommendations have TAKEN from BID !!!")
        //Update Prices on Yandex        
        val res =
          if (API_yandex(login, token).updatePrice(ppInfo_List.get))
            println("SUCCESS: Prices is updated!!!")
          else
            println("FAILED: Prices is NOT updated!!!")

        Ok(toJson[List[PhrasePriceInfo]](ppInfo_List.get))
      } else println("??? FAILED: Recommendations have NOT TAKEN from BID ???"); BadRequest
    }
  }
}