package jobs

import scala.concurrent.Future
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.concurrent.Akka
import controllers._
import models._
import java.util.Date
import java.util.concurrent.TimeUnit

import play.api.Play.current //or use (implicit app: play.api.Application)

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
        Future {
          val n = "Yandex"
          val cl = API_bid.getCampaigns(u, n).get

          val prev_ft = new DateTime(jec.getPreviousFireTime())
          var cur_ft = new DateTime(jec.getFireTime())

          if (cur_ft.getMinuteOfDay() < prev_ft.getMinuteOfDay()) //if cur_ft is a new day, i.e., 00:00:00
            cur_ft = cur_ft.minusMillis(cur_ft.getMillisOfDay() + 1) //change cur_ft to 23:59:59

          Future { //for make requests in parallel way
            cl map { c =>
              //CampaignPerformance
              get_post_CP(u, n, c, cur_ft, prev_ft).onSuccess {
                case true => println("!!! SUCCESS - CampaignPerformance for campaignID " + c.network_campaign_id + ", user: " + u.name + ", " + cur_ft + " !!!")
                case false => println("??? FAILED... - CampaignPerformance for campaignID " + c.network_campaign_id + ", user: " + u.name + ", " + cur_ft + " ???")
              }
            }
          }

          Future { //for make requests in parallel way
            cl map { c =>
              //BannersPerformance
              get_post_BP(u, n, c, cur_ft, prev_ft).onSuccess {
                case true => println("!!! SUCCESS - BannersPerformance for campaignID " + c.network_campaign_id + ", user: " + u.name + ", " + cur_ft + " !!!")
                case false => println("??? FAILED... - BannersPerformance for campaignID " + c.network_campaign_id + ", user: " + u.name + ", " + cur_ft + " ???")
              }
            }
          }

          Future { //for make requests in parallel way
            cl map { c =>
              //ActualBids and NetAdvisedBids
              get_post_ANA(u, n, c).onSuccess {
                case true => println("!!! SUCCESS - ActualBids and NetAdvisedBids for campaignID " + c.network_campaign_id + ", user: " + u.name + ", " + cur_ft + " !!!")
                case false => println("??? FAILED... - ActualBids and NetAdvisedBids for campaignID " + c.network_campaign_id + ", user: " + u.name + ", " + cur_ft + " ???")
              }
            }
          }

        } onSuccess {
          case _ => println("<<<<<<<<< User: " + u.name + " >>>>>>>>")
        }
      }
    }
    println("-------- END Job -----  CampaignPerformance ------------------")
  }

  /**
   * CampaignPerformance
   */
  def get_post_CP(u: User, n: String, c: Campaign, cur_ft: DateTime, prev_ft: DateTime): Future[Boolean] = {

    /* LIMIT = 100 in the day!!! */
    // get StatItem list from Yandex
    API_yandex(c._login, c._token)
      .getSummaryStat(List(c.network_campaign_id.toInt), prev_ft.toDate(), cur_ft.toDate())
      .map {
        case (statItem_List, json_stat) =>
          // post StatItem list to BID
          statItem_List map { sil =>
            val performance = API_bid.postCampaignStats(u, n, c.network_campaign_id, Performance._apply(prev_ft, cur_ft, sil))
            if (performance.isDefined) true else false
          } getOrElse {
            println("<< failed CP: " + u.name + ", " + c.network_campaign_id + ": " + json_stat + " >>")
            false
          }
      }
  }

  /**
   * BannersPerformance
   */
  def get_post_BP(u: User, n: String, c: Campaign, cur_ft: DateTime, prev_ft: DateTime): Future[Boolean] = {

    // get BannersStat from Yandex
    API_yandex(c._login, c._token)
      .getBannersStat(c.network_campaign_id.toInt, prev_ft.toDate(), cur_ft.toDate())
      .map {
        case (bannersStat, json_stat) =>
          // post StatItem list to BID
          bannersStat map { bs =>
            val performance = API_bid.postBannersStats(u, n, c.network_campaign_id, bs, cur_ft)
            if (performance.isDefined) true else false
          } getOrElse {
            println("<< failed BP: " + u.name + ", " + c.network_campaign_id + ": " + json_stat + " >>")
            false
          }
      }
  }

  /**
   * ActualBids and NetAdvisedBids
   */
  def get_post_ANA(u: User, n: String, c: Campaign): Future[Boolean] = {
    // get BannersInfo list from Yandex
    API_yandex(c._login, c._token)
      .getBanners(List(c.network_campaign_id.toInt))
      .map {
        case (bannerInfo_List, json_banners) =>
          // post BannersInfo list to BID
          bannerInfo_List map { bil =>
            if (API_bid.postBannerReports(u, n, c.network_campaign_id, bil)) true else false
          } getOrElse {
            println("<< failed ANA: " + u.name + ", " + c.network_campaign_id + ": " + json_banners + " >>")
            false
          }
      }
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
    Await.result(result, Duration.Inf)
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

          var cur_ft = new DateTime(jec.getFireTime()) //after 00:00:00
          cur_ft = cur_ft.minusMillis(cur_ft.getMillisOfDay() + 1) //set to 23:59:59 of previous day

          cl map { c =>
            get_post_BPP(u, n, c, cur_ft).onSuccess {
              case _ => println("!!! FINISH - BannerPhrasePerformance for campaignID " + c.network_campaign_id + ", user: " + u.name + ", " + cur_ft + " !!!")
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
  def get_post_BPP(u: User, n: String, c: Campaign, cur_ft: DateTime) = {
    val login = c._login
    val token = c._token
    val cID = c.network_campaign_id

    //create Report on Yandex server
    API_yandex(login, token)
      .createNewReport(cID.toInt, cur_ft.toDate(), cur_ft.toDate())
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