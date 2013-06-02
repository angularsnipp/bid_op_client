package jobs

import scala.concurrent.{ Future, Await }
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.concurrent.Akka
import controllers._
import models._
import java.util.Date
import java.util.concurrent.TimeUnit

import play.api.Play.current //or use (implicit app: play.api.Application)
import play.api.libs.json.{ JsValue, JsNull }
import json_api._

import org.joda.time.DateTime

import org.quartz._
import org.quartz.impl.StdSchedulerFactory

/**
 * ----------------------------------------------------------------------------------------------
 * Scheduler object
 */
object Scheduler {

  val nMinutes = 15

  val scheduler = StdSchedulerFactory.getDefaultScheduler()

  //CampaignPerformance		
  val jKeyCP = JobKey.jobKey("jobCP")
  val tKeyCP = TriggerKey.triggerKey("triggerCP")

  //BannerPhrasePerformance
  val jKeyBPP = JobKey.jobKey("jobBPP")
  val tKeyBPP = TriggerKey.triggerKey("triggerBPP")

  def start = {
    scheduler.clear() //remove all jobs and triggers
    scheduler.start()

    val now = new DateTime()
    val startSS = now //ShortScheduler
      .minusMillis(now.getMillisOfDay())
      .plusMinutes(nMinutes * (now.getMinuteOfDay() / nMinutes + 1)) //multiple to "nMinutes" minutes

    val startLS = now //LongScheduler
      .minusMillis(now.getMillisOfDay())
      .plusDays(1) //next day in 00:00

    println("================== SHEDULER start ==================")
    println(startSS)
    println(startLS)

    /** CampaignPerformance **/
    // define the job and tie it to our CampaignPerformanceReport class
    val jobCP = JobBuilder.newJob(classOf[ShortScheduler]).withIdentity(jKeyCP).build()

    // Trigger the job to run at some time "startCP", and then repeat every "nMinutes" minutes
    val triggerCP = TriggerBuilder.newTrigger()
      .withIdentity(tKeyCP)
      .startAt(startSS.toDate())
      //.startNow()
      .withSchedule(
        SimpleScheduleBuilder.simpleSchedule()
          .withIntervalInMinutes(nMinutes)
          .repeatForever())
      .build()

    // Tell quartz to schedule the job using our trigger
    scheduler.scheduleJob(jobCP, triggerCP)

    /** BannerPhrasePerformance **/
    // define the job and tie it to our BannerPhrasePerformanceReport class
    val jobBPP = JobBuilder.newJob(classOf[LongScheduler]).withIdentity(jKeyBPP).build()

    // Trigger the job to run at some time "startBPP", and then repeat every 24 hours
    val triggerBPP = TriggerBuilder.newTrigger()
      .withIdentity(tKeyBPP)
      .startAt(startLS.toDate())
      //.startNow()
      .withSchedule(
        SimpleScheduleBuilder.simpleSchedule()
          .withIntervalInHours(24)
          .repeatForever())
      .build()

    // Tell quartz to schedule the job using our trigger
    scheduler.scheduleJob(jobBPP, triggerBPP)

  }

  def stop = {
    scheduler.standby()
    println("================== SHEDULER stop ==================")
  }

  def shutdown = {
    scheduler.shutdown()
    println("================== SHEDULER shutdown ==================")
  }

  def isStarted: Boolean = scheduler.isStarted()
  def isInStandbyMode: Boolean = scheduler.isInStandbyMode()
}

/**
 * --------------------------------------- SHORT Scheduler-------------------------------------------------------
 * CampaignPerformanceReport
 * BannersPerformanceReport
 * ActualNetAdvisedBids
 */
class ShortScheduler extends Job {

  /**
   * Jobs execution
   */
  def execute(jec: JobExecutionContext) {
    println("-------- START Job ----- CampaignPerformance ------------------")

    wakeUP /* !!! WAKE UP Internal Server !!! */

    User.findAll match {
      case Nil => println("Users not found")
      case ul => ul map { u =>
        Future { //send requests concurrently for all users
          val n = "Yandex"
          val cl = API_bid.getCampaigns(u, n).get

          val uniqueLogins = cl.map(_._login).distinct //get unique logins, for each we can send up to 5 concurrent requests to Yandex.Direct API

          uniqueLogins map { l =>
            Future { //send requests concurrently for all unique logins for the specific user
              val prev_ft = new DateTime(jec.getPreviousFireTime())
              var cur_ft = new DateTime(jec.getFireTime())

              if (cur_ft.getMinuteOfDay() < prev_ft.getMinuteOfDay()) //if cur_ft is a new day, i.e., 00:00:00
                cur_ft = cur_ft.minusMillis(cur_ft.getMillisOfDay() + 1) //change cur_ft to 23:59:59

              val ucl = cl.filter(_._login == l) //list of campaigns for unique login - l

              /**
               * get Statistics from Yandex.Metrika
               */
              val m = API_metrika(l, ucl.head._token)
              val counterList = m.counters
              val ssml = m.summary(counterList, cur_ft, cur_ft)
              val cgl = ssml.map { v =>
                v._1 -> v._2.goals
              }
              val ssmgl = m.summaryGoals(cgl, cur_ft, cur_ft)

              val cMetrikaPerformance: List[PerformanceMetrika] = m.cSummary(ssml, ssmgl)
              val bpMetrikaPerformance: List[PerformanceMetrika] = m.bpSummary(ssml, ssmgl)

              /**
               * get Statistics from Yandex.Direct -> add stats from Yandex.Metrika -> send to BID
               */
              Future { //send request for all campaigns belonging to the specific login
                //CampaignPerformance
                get_post_CP(u, n, ucl, cur_ft, prev_ft, cMetrikaPerformance).onSuccess {
                  case true => println("!!! SUCCESS FINISH - CP for: " + u.name + ", " + l + ", " + cur_ft + " !!!")
                  case false => println("??? FAILED... FINISH - CP for: " + u.name + ", " + l + ", " + cur_ft + " ???")
                }
              }

              Future { //send request succesively for all campaigns belonging to the specific login
                //BannersPerformance
                if (get_post_BP(u, n, ucl, cur_ft, prev_ft, bpMetrikaPerformance))
                  println("!!! SUCCESS FINISH - BP for: " + u.name + ", " + l + ", " + cur_ft + " !!!")
                else
                  println("??? FAILED... FINISH - BP for: " + u.name + ", " + l + ", " + cur_ft + " ???")
              }

              Future { //for make requests in parallel way
                //ActualBids and NetAdvisedBids
                get_post_ANA(u, n, ucl).onSuccess {
                  case true => println("!!! SUCCESS FINISH - ANA for: " + u.name + ", " + l + ", " + cur_ft + " !!!")
                  case false => println("??? FAILED... FINISH - ANA for: " + u.name + ", " + l + ", " + cur_ft + " ???")
                }
              }

            } //logins future
          } //logins
        } onSuccess {
          case _ => println("<<<<<<<<< User: " + u.name + " >>>>>>>>")
        } //users future
      } //users
    } // users match
    println("-------- END Job -----  CampaignPerformance ------------------")
  }

  /**
   * CampaignPerformance
   */
  def get_post_CP(u: User, n: String, ucl: List[Campaign], cur_ft: DateTime, prev_ft: DateTime, mpList: List[PerformanceMetrika] = Nil): Future[Boolean] = {
    /* LIMIT = 100 in the day!!! */

    val login = ucl.head._login
    val token = ucl.head._token

    // get StatItem list from Yandex
    API_yandex(login, token)
      .getSummaryStat(ucl.map(_.network_campaign_id.toInt), prev_ft.toDate(), cur_ft.toDate())
      .map {
        case (statItem_List, json_stat) =>
          // post StatItem list to BID
          statItem_List map { sil =>
            val bl = ucl map { c =>
              val performance = API_bid.postCampaignStats(u, n, c.network_campaign_id,
                Performance._apply(prev_ft, cur_ft, sil.filter(_.CampaignID == c.network_campaign_id.toInt)),
                mpList.filter(mp => mp.campaignID.getOrElse(0) == c.network_campaign_id.toInt))
              if (performance.isDefined)
                println("!!! SUCCESS - CP for: " + u.name + ", " + login + ", " + c.network_campaign_id + " !!!")
              else
                println("??? FAILED... - CP for: " + u.name + ", " + login + ", " + c.network_campaign_id + " ???")
              performance.isDefined
            }
            bl.find(!_).isEmpty //true if CPs for all campaigns have been added successful
          } getOrElse {
            println("<< failed CP: " + u.name + ", " + login + ": " + json_stat + " >>")
            false
          }
      }
  }

  /**
   * BannersPerformance
   */
  def get_post_BP(u: User, n: String, ucl: List[Campaign], cur_ft: DateTime, prev_ft: DateTime, mpList: List[PerformanceMetrika] = Nil): Boolean = {

    val login = ucl.head._login
    val token = ucl.head._token

    ucl map { c =>
      // get BannersStat from Yandex
      val fb = API_yandex(login, token)
        .getBannersStat(c.network_campaign_id.toInt, prev_ft.toDate(), cur_ft.toDate())
        .map {
          case (bannersStat, json_stat) =>
            // post StatItem list to BID
            bannersStat map { bs =>
              val performance = API_bid.postBannersStats(u, n, c.network_campaign_id, bs, cur_ft,
                mpList.filter(mp => mp.campaignID.getOrElse(0) == c.network_campaign_id.toInt))
              if (performance.isDefined)
                println("!!! SUCCESS - BP for: " + u.name + ", " + login + ", " + c.network_campaign_id + " !!!")
              else
                println("??? FAILED... - BP for: " + u.name + ", " + login + ", " + c.network_campaign_id + " ???")
              performance.isDefined
            } getOrElse {
              println("<< failed BP: " + u.name + ", " + login + ", " + c.network_campaign_id + ": " + json_stat + " >>")
              false
            }
        }
      Await.result(fb, 30 seconds) //await up to 30s for getting response for each campaign
    } find (!_) isEmpty
  }

  /**
   * ActualBids and NetAdvisedBids
   */
  def get_post_ANA(u: User, n: String, ucl: List[Campaign]): Future[Boolean] = {

    val login = ucl.head._login
    val token = ucl.head._token

    def getBanners(cl: List[Campaign]) =
      // get BannersInfo list from Yandex
      API_yandex(login, token)
        .getBanners(cl.map(_.network_campaign_id.toInt))
        .map {
          case (bannerInfo_List, json_banners) =>
            // post BannersInfo list to BID
            bannerInfo_List map { bil =>
              cl map { c =>
                val res = API_bid.postBannerReports(u, n, c.network_campaign_id, bil.filter(_.CampaignID == c.network_campaign_id.toLong))
                if (res)
                  println("!!! SUCCESS - ANA for: " + u.name + ", " + login + ", " + c.network_campaign_id + " !!!")
                else
                  println("??? FAILED... - ANA for: " + u.name + ", " + login + ", " + c.network_campaign_id + " ???")
                res
              }
              //if (API_bid.postBannerReports(u, n, c.network_campaign_id, bil)) true else false
            } getOrElse {
              println("<< failed ANA: " + u.name + ", " + login + ": " + json_banners + " >>")
              List(false)
            }
        }

    def cl10(cl: List[Campaign]): List[Boolean] =
      if (cl.length > 10) { //10 is a max value for Yandex
        val bl = getBanners(cl.take(10))
        Await.result(bl, 30 seconds) ::: cl10(cl.drop(10))
      } else {
        val bl = getBanners(cl)
        Await.result(bl, 30 seconds)
      }

    Future { cl10(ucl).find(!_).isEmpty } //true if ANAs for all campaigns have been added successful
  }

  def wakeUP = {
    import play.api.libs.ws.{ WS, Response }
    import scala.concurrent.Await
    import scala.concurrent.duration.Duration
    val result = WS.url("http://bid-op-client.herokuapp.com/wakeUP") //("http://localhost:9000")
      .get()
      .map { response =>
        if (response.status == play.mvc.Http.Status.OK)
          println("!!! Client Server is AWAKED !!!")
        else
          println("??? Client Server SLEEP... ???")
      }
    result.onFailure {
      case _ => println("??? Failed request to Client Server... ???")
    }
  }
}

/**
 * ----------------------------------------- LONG Scheduler-----------------------------------------------------
 * BannerPhrasePerformanceReport
 */
class LongScheduler extends Job {

  /**
   * Jobs execution
   */
  def execute(jec: JobExecutionContext) {
    println("-------- START Job ----- BannerPhrasePerformance ------------------")

    User.findAll match {
      case Nil => println("Users are NOT found...")
      case ul => ul map { u =>
        Future {
          val n = "Yandex"
          val cl = API_bid.getCampaigns(u, n).get

          val uniqueLogins = cl.map(_._login).distinct //get unique logins, for each we can send up to 5 concurrent requests to Yandex.Direct API

          uniqueLogins map { l =>
            Future { //send requests concurrently for all unique logins for the specific user

              var cur_ft = new DateTime(jec.getFireTime()) //after 00:00:00
              cur_ft = cur_ft.minusMillis(cur_ft.getMillisOfDay() + 1) //set to 23:59:59 of previous day

              val ucl = cl.filter(_._login == l) //list of campaigns for unique login - l

              ucl map { c =>
                Await.result(get_post_BPP(u, n, c, cur_ft), 1 minutes)
                println("!!! FINISH - BPP for: " + u.name + ", " + l + ", " + c.network_campaign_id + " - " + cur_ft + " !!!")
                /*get_post_BPP(u, n, c, cur_ft).onSuccess {
                  case _ => println("!!! FINISH - BannerPhrasePerformance for campaignID " + c.network_campaign_id + ", user: " + u.name + ", " + cur_ft + " !!!")
                }*/
              }
            }
          }
        } onSuccess {
          case _ => println("<<<<<<<<< User: " + u.name + " >>>>>>>>")
        }
      }
    }
    println("-------- END Job ----- BannerPhrasePerformance ------------------")
  }

  /**
   * BannerPhrasePerformance
   */
  def get_post_BPP(u: User, n: String, c: Campaign, cur_ft: DateTime, start_date: Option[DateTime] = None) = {
    val login = c._login
    val token = c._token
    val cID = c.network_campaign_id

    val start_dt = start_date.getOrElse(cur_ft)

    //create Report on Yandex server
    API_yandex(login, token)
      .createNewReport(cID.toInt, start_dt.toDate(), cur_ft.toDate())
      .map { newReportID =>
        newReportID map { id =>

          def getUrl: Option[String] = {
            val (reportInfo_List, json_reports) = API_yandex(login, token).getReportList
            reportInfo_List map { ril =>
              val reportInfo = ril.filter(_.ReportID == id).head
              reportInfo.StatusReport match {
                case "Pending" => {
                  Thread.sleep(5000)
                  println("!!!!!! PENDING !!!!!");
                  getUrl
                }
                case "Done" => {
                  println("!!!!!! DONE !!!!!")
                  reportInfo.Url
                }
              }
            } getOrElse (None)
          }

          //Get current report Url 
          getUrl map { reportUrl =>
            //download XML report from Yandex Url
            API_yandex(login, token)
              .getXML(reportUrl)
              .map { xml_node =>
                //post report to BID
                val postToBid = API_bid.postReports(u, n, cID, xml_node)

                if (postToBid.isDefined)
                  println("!!! Report is POSTED to BID !!!")
                else
                  println("??? Report is NOT POSTED to BID ???")

                //remove current report from Yandex Server
                if (API_yandex(login, token).deleteReport(newReportID.get))
                  println("!!! Report is DELETED from Yandex!!!")
                else
                  println("??? Report is NOT DELETED from Yandex ???")
              }
          } getOrElse println("??? FAILED... getting report url ???")
        } getOrElse println("??? FAILED... report is NOT created ???")
      }
  }
}

  /*

  /* The main plan of scheduler
       1. GET CAMPAIGNS for all users
       2. for all CAMPAIGNS:
       			[getBanners]
       		1). get ActualBids and NetAdvisedBids from Yandex 	(API_yandex.getBanners)
       		2). post ActualBids and NetAdvisedBids to BID 		(API_bid.postBannerReports)
       		
       			[getStats] - !!! DURING the day!!!
       		3). get Statistics from Yandex 						(API_yandex.getSummaryStat)
       		4). post Statistics to BID 							(API_bid.postStats) 
       			[getReport] - !!! At the END of the day !!!
       		3'). get XML Report from Yandex 					(API_yandex.createNewReport, ...)
       		4'). post XML Report to BID 						(API_bid.postReports)
       		
       			[getRecommendations]
       		5). get Recommendations from BID 					(API_bid.getRecommendations)
       		6). post Recommendations or Update prices on Yandex 	(API_yandex.updatePrice)
  */

  def start {
    Akka.system.scheduler.schedule(0 seconds, 1 minutes) {
      println("!!! START Job ============================================================================ !!!")
      //executeAll
      println("!!! END Job ============================================================================== !!!")
    }
  }

  def shutdown {
    println("!!! SCHEDULER has STOPPED !!!")
    Akka.system.shutdown()
  }

  /**/

  def executeAll = {
    User.findAll match {
      case Nil => println("Users are NOT FOUND ...")
      case userSeq => userSeq map (user => executeUser(user))
    }
  }

  def executeUser(user: User) = {
    Network.list map (network => executeUserNetwork(user, network))
  }

  def executeUserNetwork(user: User, network: String) = {
    API_bid.getCampaigns(user, network) match {
      case None => println("User '" + user.name + "' has NO campaigns in " + network)
      case Some(campaigns) => campaigns map (campaign => executeUserNetworkCampaign(user, network, campaign))
    }
    /*val promiseCampaigns = Akka.future(API_bid.getCampaigns(user, network))
    promiseCampaigns.orTimeout("Oops", 5, TimeUnit.SECONDS).map { eitherCampaignsOrTimeout =>
      eitherCampaignsOrTimeout.fold(
        campaignsOpt => campaignsOpt match {
          case None => println("User '" + user.name + "' has NO campaigns in " + network)
          case Some(campaigns) => val u = campaigns map (campaign => executeUserNetworkCampaign(user, network, campaign))
        },
        timeout => println("InternalServerError: " + timeout))
    }*/
  }

  def executeUserNetworkCampaign(
    user: User,
    network: String,
    campaign: Campaign,
    start_date: DateTime = new DateTime().minusMonths(2),
    end_date: DateTime = new DateTime()) = {

    val login = campaign._login
    val token = campaign._token
    val campaignID = campaign.network_campaign_id
    println("user: " + user + "; network: " + network + "; id: " + campaignID)

    /* 1. */
    val (bannerInfo_List, json_banners) = API_yandex(login, token).getBanners(List(campaignID.toInt))
    if (bannerInfo_List.isDefined) {
      println("!!! SUCCESS: getBanners from " + network + "!!!")

      /* 2. */
      if (API_bid.postBannerReports(user, network, campaignID, bannerInfo_List.get)) {
        println("!!! SUCCESS: ActualBids and NetAdvisedBids have POSTED to BID")

        /* 3. LIMIT = 100 in the day!!! */
        val (statItem_List, json_stat) = API_yandex(login, token).getSummaryStat(List(campaignID.toInt), start_date.toDate(), end_date.toDate())
        if (statItem_List.isDefined) {
          println("!!! SUCCESS: getStats !!!")

          /* 4. */
          val performance = API_bid.postStats(user, network, campaignID, Performance._apply(start_date, end_date, statItem_List.get.head))
          if (performance.isDefined) {
            println("!!! SUCCESS: Stats have POSTED to BID")

            /* 5. */
            val ppInfo_List = API_bid.getRecommendations(user, network, campaignID, start_date)
            if (ppInfo_List.isDefined) {
              println("!!! SUCCESS: Recommendations have TAKEN from BID !!!")

              /* 6. */
              if (API_yandex(login, token).updatePrice(ppInfo_List.get)) {
                println("SUCCESS: Prices is updated!!!")
              } else println("??? FAILED: Prices is NOT updated ???")
            } else println("??? FAILED: Recommendations have NOT TAKEN from BID ???")
          } else println("??? FAILED... Stats have NOT POSTED to BID")
        } else println("??? FAILED... getStats ???")
      } else println("??? FAILED... ActualBids and NetAdvisedBids have NOT POSTED to BID")
    } else println("??? FAILED... getBanners from " + network + "???")
  }
  */