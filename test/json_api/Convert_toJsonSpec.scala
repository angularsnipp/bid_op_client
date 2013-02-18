package json_api

import models._

import common.Yandex
import json_api.Convert._
import org.specs2.mutable._
import org.specs2.specification._
import play.api.libs.json._
import org.joda.time._

class Convert_toJsonSpec extends Specification with AllExpectations {

  /*------------- ShortCampaignInfo ---------------------------------------------------*/
  "toJson - ShortCampaignInfo" should {
    sequential

    "take TRUE data" in {
      val date = Yandex.date_fmt.parse("2013-01-01")

      val data = ShortCampaignInfo(
        CampaignID = 100,
        Login = "krisp0",
        Name = "some_name",
        StartDate = new DateTime(date),
        Sum = 10.3,
        Rest = 20.1,
        SumAvailableForTransfer = Some(30.0),
        Shows = 100,
        Clicks = 10,
        Status = Some(""),
        StatusModerate = None,
        IsActive = Some("yes"),
        ManagerName = Some("manager"),
        AgencyName = None)

      val res = toJson[ShortCampaignInfo](data)

      res \ "CampaignID" must_== (JsNumber(100))
      res \ "Login" must_== (JsString("krisp0"))
      res \ "Sum" must_== (JsNumber(10.3))
      (res \ "Status").asOpt[String] must_== (Some(""))
      (res \ "StartDate").as[DateTime] must_== (new DateTime(date))
      (res \ "ManagerName").asOpt[String] must_== (Some("manager"))
      (res \ "AgencyName").asOpt[String] must_== (None)

    }
  }

  /*------------- List[ShortCampaignInfo] ---------------------------------------------------*/
  "toJson - List[ShortCampaignInfo]" should {
    sequential

    "take TRUE data" in {
      val date = Yandex.date_fmt.parse("2013-01-01")
      val sci = ShortCampaignInfo(
        CampaignID = 100,
        Login = "krisp0",
        Name = "some_name",
        StartDate = new DateTime(date),
        Sum = 10.3,
        Rest = 20.1,
        SumAvailableForTransfer = Some(30.0),
        Shows = 100,
        Clicks = 10,
        Status = Some(""),
        StatusModerate = None,
        IsActive = Some("yes"),
        ManagerName = Some("manager"),
        AgencyName = None)
      val data = List(sci, sci)

      val res = toJson[List[ShortCampaignInfo]](data)

      res \\ "CampaignID" map (_.as[Int]) must_== (List(100, 100))
      res \\ "Login" map (_.as[String]) must_== (List("krisp0", "krisp0"))
      res \\ "Sum" must_== (List(JsNumber(10.3), JsNumber(10.3)))
      res \\ "ManagerName" map (_.asOpt[String]) must_== (List(Some("manager"), Some("manager")))
      res \\ "AgencyName" must_== (List())

    }
  }

  /*------------- BannerInfo ---------------------------------------------------*/
  "toJson - BannerInfo" should {
    sequential

    "take TRUE data" in {
      val bphi = BannerPhraseInfo(Phrase = "some_phrase")
      val data = BannerInfo(
        BannerID = 100,
        Text = "some",
        Geo = "12, 11",
        Phrases = List(bphi, bphi))

      val res = toJson[BannerInfo](data)

      res \ "BannerID" must_== (JsNumber(100))
      res \ "Text" must_== (JsString("some"))
      (res \ "Geo").as[String] must_== ("12, 11")
      res \ "Phrases" \\ "CampaignID" must_== (List(JsNumber(0), JsNumber(0)))
      res \ "Phrases" \\ "Phrase" map (_.as[String]) must_== (List("some_phrase", "some_phrase"))
    }
  }

  /*------------- List[BannerInfo] ---------------------------------------------------*/
  "toJson - List[BannerInfo]" should {
    sequential

    "take TRUE data" in {
      val bphi = BannerPhraseInfo(Phrase = "some_phrase")
      val bi = BannerInfo(
        BannerID = 100,
        Text = "some",
        Geo = "12, 11",
        Phrases = List(bphi, bphi))
      val data = List(bi, bi)

      val res = toJson[List[BannerInfo]](data)

      res \\ "Text" map (_.as[String]) must_== (List("some", "some"))
      (res \\ "Phrases").head \\ "Phrase" map (_.as[String]) must_== (List("some_phrase", "some_phrase"))
    }
  }

  /*------------- StatItem ---------------------------------------------------*/
  "toJson - StatItem" should {
    sequential

    "take TRUE data" in {
      val data = StatItem(
        SumSearch = 1.0,
        SumContext = 2.0,
        ShowsSearch = 10,
        ClicksContext = 40)

      val res = toJson[StatItem](data)
      res \ "SumSearch" must_== (JsNumber(1.0))
      res \ "ClicksContext" must_== (JsNumber(40))
    }
  }

  /*------------- List[StatItem] ---------------------------------------------------*/
  "toJson - List[StatItem]" should {
    sequential

    "take TRUE data" in {
      val data = List(
        StatItem(
          SumSearch = 1.0,
          SumContext = 2.0,
          ShowsSearch = 10,
          ClicksContext = 40),
        StatItem())

      val res = toJson[List[StatItem]](data)

      res \\ "SumSearch" must_== (List(JsNumber(1.0), JsNumber(0.0)))
      res \\ "ClicksContext" map (_.as[Int]) must_== (List(40, 0))
    }
  }

  /*------------- ReportInfo ---------------------------------------------------*/
  "toJson - ReportInfo" should {
    sequential

    "take TRUE data" in {
      val data = ReportInfo(
        ReportID = 100,
        StatusReport = "Pending")

      val res = toJson[ReportInfo](data)

      res \ "ReportID" must_== (JsNumber(100))
      (res \ "Url").asOpt[String] must_== (None)
      res \ "StatusReport" must_== (JsString("Pending"))
    }
  }

  /*------------- List[ReportInfo] ---------------------------------------------------*/
  "toJson - List[ReportInfo]" should {
    sequential

    "take TRUE data" in {
      val data = List(
        ReportInfo(
          ReportID = 100,
          StatusReport = "Pending"),
        ReportInfo(
          ReportID = 200,
          Url = Some("https://some.address"),
          StatusReport = "Done"))

      val res = toJson[List[ReportInfo]](data)

      res \\ "ReportID" must_== (List(JsNumber(100), JsNumber(200)))
      res \\ "Url" map (_.asOpt[String]) must_== (List(Some("https://some.address")))
      res \\ "StatusReport" map (_.as[String]) must_== (List("Pending", "Done"))
    }
  }

  /*------------- GetBannersInfo ---------------------------------------------------*/
  "toJson - GetBannersInfo" should {
    sequential

    "take TRUE data" in {
      val data = GetBannersInfo(
        CampaignIDS = List(1, 2, 3),
        GetPhrases = "WithPrices")

      val res = toJson[GetBannersInfo](data)

      res \ "CampaignIDS" must_== (Json.arr(1, 2, 3))
      res \ "GetPhrases" must_== (JsString("WithPrices"))
    }
  }

  /*------------- GetSummaryStatRequest ---------------------------------------------------*/
  "toJson - GetSummaryStatRequest" should {
    sequential

    "take TRUE data" in {
      val data = GetSummaryStatRequest(
        CampaignIDS = List(1, 2, 3),
        StartDate = "2013-01-01",
        EndDate = "2013-01-01")

      val res = toJson[GetSummaryStatRequest](data)

      res \ "CampaignIDS" must_== (Json.arr(1, 2, 3))
      res \ "StartDate" must_== (JsString("2013-01-01"))
    }
  }

  /*------------- NewReportInfo ---------------------------------------------------*/
  "toJson - NewReportInfo" should {
    sequential

    "take TRUE data" in {
      val data = NewReportInfo(
        CampaignID = 100,
        StartDate = "2013-01-01",
        EndDate = "2013-01-01",
        GroupByColumns = List("clBanner", "clPhrase"))

      val res = toJson[NewReportInfo](data)

      res \ "CampaignID" must_== (JsNumber(100))
      res \ "GroupByColumns" must_== (Json.arr("clBanner", "clPhrase"))
    }
  }

  /*------------- PhrasePriceInfo ---------------------------------------------------*/
  "toJson - PhrasePriceInfo" should {
    sequential

    "take TRUE data" in {
      val data = PhrasePriceInfo(
        PhraseID = 1,
        BannerID = 2,
        CampaignID = 3,
        Price = 1.0)

      val res = toJson[PhrasePriceInfo](data)
      res \ "PhraseID" must_== (JsNumber(1))
      res \ "Price" must_== (JsNumber(1.0))
      (res \ "AutoBroker").asOpt[String] must_== (Some("Yes"))
    }
  }
  /*------------- List[PhrasePriceInfo] ---------------------------------------------------*/
  "toJson - List[PhrasePriceInfo]" should {
    sequential

    "take TRUE data" in {
      val data = List(
        PhrasePriceInfo(PhraseID = 1, Price = 1.1),
        PhrasePriceInfo(PhraseID = 2, Price = 2.2))

      val res = toJson[List[PhrasePriceInfo]](data)
      res \\ "PhraseID" map (_.as[Int]) must_== (List(1, 2))
      res \\ "Price" must_== (List(JsNumber(1.1), JsNumber(2.2)))
      res \\ "AutoBroker" map (_.asOpt[String]) must_== (List(Some("Yes"), Some("Yes")))
    }
  }

  /************************************** BID ************************************************************/
  /*------------- User ---------------------------------------------------*/
  "toJson - User" should {
    sequential

    "take TRUE data" in {
      val data = User("krisp0", "123")

      val res = toJson[User](data)

      res \ "name" must_== (JsString("krisp0"))
      res \ "password" must_== (JsString("123"))
    }
  }

  /*------------- Campaign ---------------------------------------------------*/
  "toJson - Campaign" should {
    sequential

    "take TRUE data" in {
      import common.Bid.iso_fmt
      val date = iso_fmt.parseDateTime("2013-01-01T12:00:00.000+04:00")

      val data = Campaign(_login = "krisp0", _token = "123", start_date = date)

      val res = toJson[Campaign](data)

      res \ "_login" must_== (JsString("krisp0"))
      res \ "_token" must_== (JsString("123"))
      (res \ "start_date").as[DateTime] must_== (date)
    }
  }
  /*------------- List[Campaign] ---------------------------------------------------*/
  "toJson - List[Campaign]" should {
    sequential

    "take TRUE data" in {
      import common.Bid.iso_fmt
      val date = iso_fmt.parseDateTime("2013-01-01T12:00:00.000+04:00")

      val data = List(
        Campaign(_login = "krisp0", _token = "123", start_date = date),
        Campaign(_login = "0psikr", _token = "321", start_date = date))

      val res = toJson[List[Campaign]](data)

      res \\ "_login" map (_.as[String]) must_== (List("krisp0", "0psikr"))
      res \\ "_token" map (_.as[String]) must_== (List("123", "321"))
      res \\ "start_date" map (_.as[DateTime]) must_== (List(date, date))
    }
  }
}