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

      /* take null, but required Int */
      val data2 = """[
      {"CampaignID": null,
        "Login": "krisp0"}]"""

      getCampaignsList(Json.parse(data2)) must_== (None)
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
}


/*val sci = Map(
        "CampaignID" -> 100,
        "Login" -> "krisp0",
        "Name" -> "some_name",
        "StartDate" -> Yandex.date_fmt.format(date),
        "Sum" -> 10.3,
        "Rest" -> 20.1,
        "SumAvailableForTransfer" -> 30.0,
        "Shows" -> 100,
        "Clicks" -> 10,
        "Status" -> null,
        "StatusShow" -> null,
        "StatusArchive" -> null,
        "StatusActivating" -> null,
        "StatusModerate" -> null,
        "IsActive" -> "yes",
        "ManagerName" -> "manager",
        "AgencyName" -> null)*/