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

  val scheduler = StdSchedulerFactory.getDefaultScheduler()

  val jKey = JobKey.jobKey("job")
  val tKey = TriggerKey.triggerKey("trigger")

  def start = {
    scheduler.clear() //remove all jobs and triggers
    scheduler.start()

    println("================== SHEDULER start ==================")

    // define the job and tie it to our executeBlock class
    val job = JobBuilder.newJob(classOf[CampaignPerformanceReport]).withIdentity(jKey).build()

    // Trigger the job to run AT some time t, and then repeat every k seconds
    val trigger = TriggerBuilder.newTrigger()
      .withIdentity(tKey)
      .startNow()
      .withSchedule(
        SimpleScheduleBuilder.simpleSchedule()
          .withIntervalInSeconds(1)
          .repeatForever())
      .build()

    // Tell quartz to schedule the job using our trigger
    println(scheduler.scheduleJob(job, trigger))

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
   
    println("-------- END Job -----  CampaignPerformance ------------------")
  }
}

class BannerPhrasePerformanceReport extends Job {
  def execute(jec: JobExecutionContext) {
    println("-------- START Job ----- BannerPhrasePerformance ------------------")
    println("-------- END Job ----- BannerPhrasePerformance ------------------")
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