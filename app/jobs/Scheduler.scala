package jobs

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

object Scheduler {

  val nMinutes = 15

  val scheduler = StdSchedulerFactory.getDefaultScheduler()

  val jKeyCP = JobKey.jobKey("jobCP")
  val tKeyCP = TriggerKey.triggerKey("triggerCP")
  val jKeyBPP = JobKey.jobKey("jobBPP")
  val tKeyBPP = TriggerKey.triggerKey("triggerBPP")

  def start = {
    scheduler.clear() //remove all jobs and triggers
    scheduler.start()

    val now = new DateTime()
    val startCP = now
      .minusMillis(now.getMillisOfDay())
      .plusMinutes(nMinutes * (now.getMinuteOfDay() / nMinutes + 1) - 1) //multiple to "nMinutes" minutes

    val startBPP = now
      .minusMillis(now.getMillisOfDay())
      .plusDays(1) //next day in 00:00

    println("================== SHEDULER start ==================")
    println(startCP)
    println(startBPP)
    /** CampaignPerformance **/
    // define the job and tie it to our CampaignPerformanceReport class
    val jobCP = JobBuilder.newJob(classOf[CampaignPerformanceReport]).withIdentity(jKeyCP).build()

    // Trigger the job to run AT some time t, and then repeat every k seconds
    val triggerCP = TriggerBuilder.newTrigger()
      .withIdentity(tKeyCP)
      .startAt(startCP.toDate())
      .withSchedule(
        SimpleScheduleBuilder.simpleSchedule()
          .withIntervalInMinutes(nMinutes)
          .repeatForever())
      .build()

    // Tell quartz to schedule the job using our trigger
    println(scheduler.scheduleJob(jobCP, triggerCP))

    /** BannerPhrasePerformance **/
    // define the job and tie it to our executeBlock class
    val jobBPP = JobBuilder.newJob(classOf[BannerPhrasePerformanceReport]).withIdentity(jKeyBPP).build()

    // Trigger the job to run AT some time t, and then repeat every k seconds
    val triggerBPP = TriggerBuilder.newTrigger()
      .withIdentity(tKeyBPP)
      .startAt(startBPP.toDate())
      .withSchedule(
        SimpleScheduleBuilder.simpleSchedule()
          .withIntervalInHours(24)
          .repeatForever())
      .build()

    // Tell quartz to schedule the job using our trigger
    println(scheduler.scheduleJob(jobBPP, triggerBPP))

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

class CampaignPerformanceReport extends Job {
  def execute(jec: JobExecutionContext) {
    println("-------- START Job ----- CampaignPerformance ------------------")

    val u = User.findByName("krisp0").get
    val n = "Yandex"
    val cl = API_bid.getCampaigns(u, n).get

    val now = jec.getFireTime()

    val res = cl map { c =>
      if (get_post_CP(u, n, c, now))
        println("!!! SUCCESS - CampaignPerformance for campaignID " + c.network_campaign_id + ", " + now + " !!!")
      else
        println("??? FAILED... - CampaignPerformance for campaignID " + c.network_campaign_id + ", " + now + " ???")
    }

    println("-------- END Job -----  CampaignPerformance ------------------")
  }

  def get_post_CP(u: User, n: String, c: Campaign, d: Date) = {
    val login = c._login
    val token = c._token
    val cID = c.network_campaign_id

    /* LIMIT = 100 in the day!!! */
    val (statItem_List, json_stat) = API_yandex(login, token).getSummaryStat(List(cID.toInt), d, d)
    if (statItem_List.isDefined) {
      val dt = new DateTime(d)
      val performance = API_bid.postStats(u, n, cID, Performance._apply(dt.minusMillis(dt.getMillisOfDay()), dt, statItem_List.get))
      if (performance.isDefined) true else false
    } else false
  }
}

class BannerPhrasePerformanceReport extends Job {
  def execute(jec: JobExecutionContext) {
    println("-------- START Job ----- BannerPhrasePerformance ------------------")

    val u = User.findByName("krisp0").get
    val n = "Yandex"
    val cl = API_bid.getCampaigns(u, n).get

    val now = new DateTime(jec.getFireTime()).minusDays(1).toDate()

    cl map { c =>
      get_post_BPP(u, n, c, now)
      println("!!! FINISH - BannerPhrasePerformance for campaignID " + c.network_campaign_id + ", " + now + " !!!")
    }

    println("-------- END Job ----- BannerPhrasePerformance ------------------")
  }

  def get_post_BPP(u: User, n: String, c: Campaign, d: Date) = {
    val login = c._login
    val token = c._token
    val cID = c.network_campaign_id

    //create Report on Yandex server
    val newReportID = API_yandex(login, token).createNewReport(
      campaignID = cID.toInt,
      start_date = d,
      end_date = d)

    newReportID map { id =>

      def getUrl: Option[String] = {
        val (reportInfo_List, json_reports) = API_yandex(login, token).getReportList
        reportInfo_List map { ril =>
          val reportInfo = ril.filter(_.ReportID == id).head
          reportInfo.StatusReport match {
            case "Pending" => {
              Thread.sleep(1000)
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
        val xml_node = API_yandex(login, token).getXML(reportUrl)
        println(xml_node)
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
      } getOrElse println("??? FAILED... getting report url ???")
    }
  } getOrElse println("??? FAILED... report is NOT created ???")

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