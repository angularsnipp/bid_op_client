package json_api

import models._

import play.api.libs.json._
import play.api.libs.functional.syntax._

/************* JSON WRITEs and READs for creating REQUESTs and handling RESULTs ************/
/************* or just define JSON FORMATs 									  ************/

object Formats {

  implicit lazy val shortCampaignInfo = Format(Reads.shortCampaignInfo, Writes.shortCampaignInfo)

  implicit lazy val bannerPhraseInfo = Format(Reads.bannerPhraseInfo, Writes.bannerPhraseInfo)
  implicit lazy val bannerInfo = Format(Reads.bannerInfo, Writes.bannerInfo)

  implicit lazy val statItem = Format(Reads.statItem, Writes.statItem)

  implicit lazy val reportInfo = Format(Reads.reportInfo, Writes.reportInfo)

  implicit lazy val user = Format(Reads.user, Writes.user)

  implicit lazy val campaign = Format(Reads.campaign, Writes.campaign)

  implicit lazy val performance = Format(Reads.performance, Writes.performance)

  //implicit lazy val phrasePriceInfo = Json.format[PhrasePriceInfo]

}

object Reads { //-------------------------- fromJson ---------------------------------
  import play.api.libs.json.Reads._

  implicit lazy val shortCampaignInfo = Json.reads[ShortCampaignInfo]

  implicit lazy val bannerPhraseInfo = Json.reads[BannerPhraseInfo]
  implicit lazy val bannerInfo = Json.reads[BannerInfo]

  implicit lazy val statItem = Json.reads[StatItem]

  implicit lazy val reportInfo = Json.reads[ReportInfo]

  implicit lazy val user = Json.reads[User]

  implicit lazy val campaign = Json.reads[Campaign]

  implicit lazy val performance = Json.reads[Performance]

  //implicit lazy val phrasePriceInfo = Json.reads[PhrasePriceInfo]

}

object Writes { //---------------------- toJson -----------------------------------
  import play.api.libs.json.Writes._

  implicit lazy val shortCampaignInfo = Json.writes[ShortCampaignInfo]

  implicit lazy val bannerPhraseInfo = Json.writes[BannerPhraseInfo]
  implicit lazy val bannerInfo = Json.writes[BannerInfo]

  implicit lazy val statItem = Json.writes[StatItem]

  implicit lazy val reportInfo = Json.writes[ReportInfo]

  implicit lazy val user = Json.writes[User]

  implicit lazy val campaign = Json.writes[Campaign]

  implicit lazy val performance = Json.writes[Performance]
 
  implicit lazy val phrasePriceInfo = Json.writes[PhrasePriceInfo]

  implicit lazy val getBannersInfo = Json.writes[GetBannersInfo]

  implicit lazy val getSummaryStatRequest = Json.writes[GetSummaryStatRequest]

  implicit lazy val newReportInfo = Json.writes[NewReportInfo]
}
