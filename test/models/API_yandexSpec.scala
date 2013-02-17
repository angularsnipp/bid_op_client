package models

import common.Yandex
import models.API_yandex._

import org.specs2.mutable._
import org.specs2.specification._

import play.api.libs.json._

import org.joda.time._

class API_yandexSpec extends Specification with AllExpectations {

  val login = "krisp0"
  val token = "1eac9f413271443ab402586ab45c1c93"

  /*------------- Ping API ------------------------------------------------------------*/
  "pingAPI" should {
    sequential

    "ping SANDBOX" in {
      //pingAPI(login, token, Yandex.url_sandbox) must_== (true)
    }

    "ping MAIN" in {
      pingAPI(login, token, Yandex.url_main) must_== (true)
    }
  }

  /*------------- Get CAMPAIGNS LIST ---------------------------------------------------*/
  "getCampaignsList from response data in json" should {
    sequential

    "take wrong data null" in {
      val data = JsNull
      getCampaignsList(data) must_== (None)
    }

    "take WRONG data" in {
      /* wrong campaign id type */
      val data1 = """[
      {"CampaignID": "100",
        "Login": "krisp0"}]"""

      getCampaignsList(Json.parse(data1)) must_== (None)
    }

    "take TRUE data" in {
      val date = Yandex.date_fmt.parse("2013-01-01")

      val data = """[
      {"CampaignID": 100,
        "Login": "krisp0",
        "Name": "some_name",
        "StartDate": "2013-01-01",
        "Sum": 10.3,
        "Rest": 20.1,
        "SumAvailableForTransfer": 30.0,
        "Shows": 100,
        "Clicks": 10,
        "Status": "",
        "StatusShow": null,
        "StatusArchive": null,
        "StatusActivating": null,
        "StatusModerate": null,
        "IsActive": "yes",
        "ManagerName": "manager",
        "AgencyName": null},
      {"CampaignID": 200,
        "Login": "krisp0",
        "Name": "some_name",
        "StartDate": "2013-01-01",
        "Sum": 10.3,
        "Rest": 20.1,
        "SumAvailableForTransfer": 30.0,
        "Shows": 100,
        "Clicks": 10,
        "Status": null,
        "StatusShow": null,
        "StatusArchive": null,
        "StatusActivating": null,
        "StatusModerate": null,
        "IsActive": "yes",
        "ManagerName": "manager",
        "AgencyName": null}
        ]"""

      val Some(res) = getCampaignsList(Json.parse(data))
      res.length must_== (2)

      res.head.CampaignID must_== (100)
      res.head.Login must_== ("krisp0")
      res.head.Sum must_== (10.3)
      res.head.Status must_== (Some(""))
      res.head.StartDate must_== (new DateTime(date))
      res.head.ManagerName must_== (Some("manager"))
      res.head.AgencyName must_== (None)

    }

    "take POOR and MIXTURE data" in {
      val date = Yandex.date_fmt.parse("2013-01-01")

      val data = """[
      {"CampaignID": 100,
        "Login": "krisp0",
        "Name": "some_name",
        "StartDate": "2013-01-01",
        "Sum": 10.3,
        "Rest": 20.1,        
        "Shows": 100,
        "Clicks": 10,
        "SomeField": null,
        "SomeFieldElse": "somevalue"}
        ]"""

      val Some(res) = getCampaignsList(Json.parse(data))
      res.length must_== (1)

      res.head.CampaignID must_== (100)
      res.head.Login must_== ("krisp0")
      res.head.Sum must_== (10.3)
      res.head.StartDate must_== (new DateTime(date))
    }
  }

  /*------------- Get BANNERS ---------------------------------------------------*/
  "getBanners" should {
    sequential

    "take wrong data null" in {
      val data = JsNull
      getBanners(data) must_== (None)
    }

    "take WRONG data" in {
      /* wrong data */
      val data1 = """[
      {"BannerID": "100",
        "Text": "text1"
        }]"""

      getCampaignsList(Json.parse(data1)) must_== (None)
    }

    "take TRUE data" in {
      val file_name = "test/models/bannerList.json"
      val data = io.Source.fromFile(file_name, "utf-8").getLines.mkString

      val Some(res) = getBanners(Json.parse(data))
      res.length must_== (2)

      res.head.BannerID must_== (11)
      res.head.Text must_== ("some")
      res.head.Geo must_== ("12, 11")
      res.head.Phrases.length must_== (2)
      res.head.Phrases.head.PhraseID must_== (22)
      res.head.Phrases.head.Max must_== (2.0)
      res.head.Phrases.head.AutoBroker must_== ("Yes")
    }
  }

  /*------------- Get SUMMARY STATs ---------------------------------------------------*/
  "getSummaryStat" should {
    sequential

    "take wrong data null" in {
      val data = JsNull
      getSummaryStat(data) must_== (None)
    }

    "take WRONG data" in {
      /* wrong data */
      val data = """[
      {"SumSearch": 100.0,
       "SumContext": "",
       "ShowsSearch": null}]"""

      getSummaryStat(Json.parse(data)) must_== (None)
    }

    "take TRUE data" in {
      val data = """[
      {"SumSearch": 1.0,
       "SumContext": 2.0,
       "ShowsSearch": 10,
       "ShowsContext": 20,
       "ClicksSearch": 30,
       "ClicksContext": 40},
      {"SumSearch": 1.0,
       "SumContext": 2.0,
       "ShowsSearch": 10,
       "ShowsContext": 20,
       "ClicksSearch": 30,
       "ClicksContext": 40}]"""

      val Some(res) = getSummaryStat(Json.parse(data))
      res.length must_== (2)

      res.head.SumSearch must_== (1.0)
      res.head.ClicksContext must_== (40)
    }
  }

  /*------------- Get REPORT LIST ---------------------------------------------------*/
  "getReportList" should {
    sequential

    "take wrong data null" in {
      val data = JsNull
      getReportList(data) must_== (None)
    }

    "take WRONG data" in {
      /* wrong data */
      val data = """[
      {"ReportID": "error",
       "StatusReport": null}]"""

      getReportList(Json.parse(data)) must_== (None)
    }

    "take TRUE data" in {
      val data = """[
      {"ReportID": 100,
       "StatusReport": "Pending"},
      {"ReportID": 200,
       "Url": "https://some.address",
       "StatusReport": "Done"}  
      ]"""

      val Some(res) = getReportList(Json.parse(data))
      res.length must_== (2)

      res.head.ReportID must_== (100)
      res.head.Url must_== (None)
      res.last.Url must_== (Some("https://some.address"))
      res.last.StatusReport must_== ("Done")
    }
  }
}