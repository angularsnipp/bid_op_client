package json_api

import models._

import common.Yandex
import json_api.Convert._
import org.specs2.mutable._
import org.specs2.specification._
import play.api.libs.json._
import org.joda.time._

class Convert_fromJsonSpec extends Specification with AllExpectations {

  /*------------- ShortCampaignInfo ---------------------------------------------------*/
  "fromJson - ShortCampaignInfo" should {
    sequential

    "take wrong data null" in {
      val data = JsNull
      fromJson[ShortCampaignInfo](data) must_== (None)
    }

    "take WRONG data" in {
      /* wrong campaign id type */
      val data1 = """[
      {"CampaignID": "100",
        "Login": "krisp0"}]"""

      fromJson[ShortCampaignInfo](Json.parse(data1)) must_== (None)
    }

    "take TRUE data" in {
      val date = Yandex.date_fmt.parse("2013-01-01")

      val data = """
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
        "StatusModerate": null,
        "IsActive": "yes",
        "ManagerName": "manager",
        "AgencyName": null,
        "SomeFieldElse": "somevalue"}
      """

      val Some(res) = fromJson[ShortCampaignInfo](Json.parse(data))

      res.CampaignID must_== (100)
      res.Login must_== ("krisp0")
      res.Sum must_== (10.3)
      res.Status must_== (Some(""))
      res.StartDate must_== (new DateTime(date))
      res.ManagerName must_== (Some("manager"))
      res.AgencyName must_== (None)

    }
  }

  /*------------- List[ShortCampaignInfo] ---------------------------------------------------*/
  "fromJson - List[ShortCampaignInfo]" should {
    sequential

    "take wrong data null" in {
      val data = JsNull
      fromJson[List[ShortCampaignInfo]](data) must_== (None)
    }

    "take WRONG data" in {
      /* wrong campaign id type */
      val data1 = """[
      {"CampaignID": "100",
        "Login": "krisp0"}]"""

      fromJson[List[ShortCampaignInfo]](Json.parse(data1)) must_== (None)
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
        "StatusModerate": null,
        "IsActive": "yes",
        "ManagerName": "manager",
        "AgencyName": null,
        "SomeFieldElse": "somevalue"},
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
        "StatusModerate": null,
        "IsActive": "yes",
        "ManagerName": "manager",
        "AgencyName": null,
        "SomeFieldElse": "somevalue"}
        ]"""

      val Some(res) = fromJson[List[ShortCampaignInfo]](Json.parse(data))
      res.length must_== (2)

      res.head.CampaignID must_== (100)
      res.head.Login must_== ("krisp0")
      res.head.Sum must_== (10.3)
      res.head.Status must_== (Some(""))
      res.head.StartDate must_== (new DateTime(date))
      res.head.ManagerName must_== (Some("manager"))
      res.head.AgencyName must_== (None)

    }
  }

  /*------------- BannerInfo ---------------------------------------------------*/
  "fromJson - BannerInfo" should {
    sequential

    "take wrong data null" in {
      val data = JsNull
      fromJson[BannerInfo](data) must_== (None)
    }

    "take WRONG data" in {
      /* wrong data */
      val data1 = """
      {"BannerID": "100",
        "Text": "text1"
        }"""

      fromJson[BannerInfo](Json.parse(data1)) must_== (None)
    }

    "take TRUE data" in {
      val file_name = "test/json_api/bannerInfo.json"
      val data = io.Source.fromFile(file_name, "utf-8").getLines.mkString

      val Some(res) = fromJson[BannerInfo](Json.parse(data))

      res.BannerID must_== (11)
      res.Text must_== ("some")
      res.Geo must_== ("12, 11")
      res.Phrases.length must_== (2)
      res.Phrases.head.PhraseID must_== (22)
      res.Phrases.head.Max must_== (2.0)
      res.Phrases.head.AutoBroker must_== ("Yes")
    }
  }

  /*------------- List[BannerInfo] ---------------------------------------------------*/
  "fromJson - List[BannerInfo]" should {
    sequential

    "take wrong data null" in {
      val data = JsNull
      fromJson[List[BannerInfo]](data) must_== (None)
    }

    "take WRONG data" in {
      /* wrong data */
      val data1 = """[
      {"BannerID": "100",
        "Text": "text1"
        }]"""

      fromJson[List[BannerInfo]](Json.parse(data1)) must_== (None)
    }

    "take TRUE data" in {
      val file_name = "test/json_api/bannerInfoList.json"
      val data = io.Source.fromFile(file_name, "utf-8").getLines.mkString

      val Some(res) = fromJson[List[BannerInfo]](Json.parse(data))
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

  /*------------- StatItem ---------------------------------------------------*/
  "fromJson - StatItem" should {
    sequential

    "take wrong data null" in {
      val data = JsNull
      fromJson[StatItem](data) must_== (None)
    }

    "take WRONG data" in {
      /* wrong data */
      val data = """
      {"SumSearch": 100.0,
       "SumContext": "",
       "ShowsSearch": null}"""

      fromJson[StatItem](Json.parse(data)) must_== (None)
    }

    "take TRUE data" in {
      val data = """
      {"SumSearch": 1.0,
       "SumContext": 2.0,
       "ShowsSearch": 10,
       "ShowsContext": 20,
       "ClicksSearch": 30,
       "ClicksContext": 40}"""

      val Some(res) = fromJson[StatItem](Json.parse(data))
      res.SumSearch must_== (1.0)
      res.ClicksContext must_== (40)
    }
  }

  /*------------- List[StatItem] ---------------------------------------------------*/
  "fromJson - List[StatItem]" should {
    sequential

    "take wrong data null" in {
      val data = JsNull
      fromJson[List[StatItem]](data) must_== (None)
    }

    "take WRONG data" in {
      /* wrong data */
      val data = """[
      {"SumSearch": 100.0,
       "SumContext": "",
       "ShowsSearch": null}]"""

      fromJson[List[StatItem]](Json.parse(data)) must_== (None)
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

      val Some(res) = fromJson[List[StatItem]](Json.parse(data))
      res.length must_== (2)

      res.head.SumSearch must_== (1.0)
      res.head.ClicksContext must_== (40)
    }
  }

  /*------------- ReportInfo ---------------------------------------------------*/
  "fromJson - ReportInfo" should {
    sequential

    "take wrong data null" in {
      val data = JsNull
      fromJson[ReportInfo](data) must_== (None)
    }

    "take WRONG data" in {
      /* wrong data */
      val data = """
      {"ReportID": "error",
       "StatusReport": null}"""

      fromJson[ReportInfo](Json.parse(data)) must_== (None)
    }

    "take TRUE data" in {
      val data = """
      {"ReportID": 100,
       "StatusReport": "Pending"}  
      """

      val Some(res) = fromJson[ReportInfo](Json.parse(data))

      res.ReportID must_== (100)
      res.Url must_== (None)
    }
  }

  /*------------- List[ReportInfo] ---------------------------------------------------*/
  "fromJson - List[ReportInfo]" should {
    sequential

    "take wrong data null" in {
      val data = JsNull
      fromJson[List[ReportInfo]](data) must_== (None)
    }

    "take WRONG data" in {
      /* wrong data */
      val data = """[
      {"ReportID": "error",
       "StatusReport": null}]"""

      fromJson[List[ReportInfo]](Json.parse(data)) must_== (None)
    }

    "take TRUE data" in {
      val data = """[
      {"ReportID": 100,
       "StatusReport": "Pending"},
      {"ReportID": 200,
       "Url": "https://some.address",
       "StatusReport": "Done"}  
      ]"""

      val Some(res) = fromJson[List[ReportInfo]](Json.parse(data))
      res.length must_== (2)

      res.head.ReportID must_== (100)
      res.head.Url must_== (None)
      res.last.Url must_== (Some("https://some.address"))
      res.last.StatusReport must_== ("Done")
    }
  }

  /************************************** BID ************************************************************/
  /*------------- User ---------------------------------------------------*/
  "fromJson - User" should {
    sequential

    "take TRUE data" in {
      val data = """
       {"name": "krisp0",
        "password": "123"}"""

      val Some(res) = fromJson[User](Json.parse(data))

      res.name must_== ("krisp0")
      res.password must_== ("123")
    }
  }

  /*------------- Campaign ---------------------------------------------------*/
  "fromJson - Campaign" should {
    sequential

    "take TRUE data" in {
      import common.Bid.iso_fmt
      val date = iso_fmt.parseDateTime("2013-01-01T12:00:00.000+04:00")

      val data = """
       {"start_date": %d,
        "end_date": %d,
        "_login": "krisp0",
        "_token": "123",
        "network_campaign_id": "100",        
        "daily_budget": 50.0}""".format(date.getMillis(), date.plusDays(1).getMillis())

      val Some(res) = fromJson[Campaign](Json.parse(data))

      res._login must_== ("krisp0")
      res._token must_== ("123")
      res.start_date must_== (date)
    }
  }
  /*------------- List[Campaign] ---------------------------------------------------*/
  "fromJson - List[Campaign]" should {
    sequential

    "take TRUE data" in {
      import common.Bid.iso_fmt
      val date = iso_fmt.parseDateTime("2013-01-01T12:00:00.000+04:00")

      val data = """[
       {"start_date": %d,
        "end_date": %d,
        "_login": "krisp0",
        "_token": "123",
        "network_campaign_id": "100",        
        "daily_budget": 50.0},
       {"start_date": %d,
        "end_date": %d,
        "_login": "krisp0",
        "_token": "123",
        "network_campaign_id": "100",        
        "daily_budget": 50.0}
       ]""".format(date.getMillis(), date.plusDays(1).getMillis(), date.getMillis(), date.plusDays(1).getMillis())

      val Some(res) = fromJson[List[Campaign]](Json.parse(data))

      res.head._login must_== ("krisp0")
      res.head._token must_== ("123")
      res.head.start_date must_== (date)
    }
  }

  /*------------- Performance ---------------------------------------------------*/
  "fromJson - Performance" should {
    sequential

    "take TRUE data" in {
      import common.Bid.iso_fmt
      val date = iso_fmt.parseDateTime("2013-01-01T12:00:00.000+04:00")

      val data = """
       {"start_date": %d,
        "end_date": %d,
        "sum_search": 100.1,
        "sum_context": 99.9,
        "impress_search": 51,        
        "impress_context": 49,
        "clicks_search": 101,        
        "clicks_context": 99
        }""".format(date.getMillis(), date.plusDays(1).getMillis())

      val Some(res) = fromJson[Performance](Json.parse(data))

      res.sum_search must_== (100.1)
      res.impress_context must_== (49)
      res.start_date must_== (date)
    }
  }

  /*------------- PhrasePriceInfo ---------------------------------------------------*/
  "fromJson - PhrasePriceInfo" should {
    sequential

    "take TRUE data" in {
      val data = """
       {"PhraseID": 1,
        "BannerID": 2,
        "CampaignID": 3,
        "Price": 4}"""

      val Some(res) = fromJson[PhrasePriceInfo](Json.parse(data))
      res.PhraseID must_== (1)
      res.BannerID must_== (2)
      res.AutoBroker must_== (None)
    }
  }
  /*------------- List[PhrasePriceInfo] ---------------------------------------------------*/
  "fromJson - List[PhrasePriceInfo]" should {
    sequential

    "take TRUE data" in {
      val data = """[
       {"PhraseID": 1,
        "BannerID": 2,
        "CampaignID": 3,
        "Price": 4},
       {"PhraseID": 6,
        "BannerID": 7,
        "CampaignID": 8,
        "Price": 9,
        "AutoBroker": "Yes"}
        ]"""

      val Some(res) = fromJson[List[PhrasePriceInfo]](Json.parse(data))
      res.head.PhraseID must_== (1)
      res.head.Price must_== (4)
      res.head.AutoBroker must_== (None)
      res.last.AutoBroker must_== (Some("Yes"))
    }
  }

  /*------------- GetBannersStatResponse ---------------------------------------------------*/
  "fromJson - GetBannersStatResponse" should {
    sequential

    "take wrong data null" in {
      val data = JsNull
      fromJson[GetBannersStatResponse](data) must_== (None)
    }

    "take TRUE data" in {

      val date = Yandex.date_fmt.parse("2013-01-01")

      val file_name = "test/json_api/getBannersStatResponse.json"
      val data = io.Source.fromFile(file_name, "utf-8").getLines.mkString

      val Some(res) = fromJson[GetBannersStatResponse](Json.parse(data))

      res.CampaignID must_== (10)
      res.StartDate must_== ("2013-01-01")
      res.EndDate must_== ("2013-01-01")
      res.Stat.length must_== (3)
      res.Stat.head.BannerID must_== (11)
      res.Stat.head.Phrase must_== ("some")
      res.Stat.head.PhraseID must_== (1)
      res.Stat.head.Clicks must_== (50)
      res.Stat.head.Shows must_== (550)
      res.Stat.head.Sum must_== (12.3)
      res.Stat.head.StatDate must_== (new DateTime(date))
    }
  }
}