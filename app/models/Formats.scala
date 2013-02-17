package models

import play.api.libs.json._
import play.api.libs.functional.syntax._

/************* JSON WRITEs and READs for creating REQUESTs and handling RESULTs ************/
/************* or just define JSON FORMATs 									  ************/

object Formats {

  implicit lazy val shortCampaignInfo = Json.format[ShortCampaignInfo]

  implicit lazy val bannerPhraseInfo = Json.format[BannerPhraseInfo]
  implicit lazy val bannerInfo = Json.format[BannerInfo]

  implicit lazy val statItem = Json.format[StatItem]

  implicit lazy val reportInfo = Json.format[ReportInfo]

  implicit lazy val user = Json.format[User]

  implicit lazy val campaign = Json.format[Campaign]

  implicit lazy val performance = Json.format[Performance]

  //implicit lazy val phrasePriceInfo = Json.format[PhrasePriceInfo]

}

object Reads {
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

object Writes {
  import play.api.libs.json.Writes._

  implicit lazy val shortCampaignInfo = Json.writes[ShortCampaignInfo]

  implicit lazy val bannerPhraseInfo = Json.writes[BannerPhraseInfo]
  implicit lazy val bannerInfo = Json.writes[BannerInfo]

  implicit lazy val statItem = Json.writes[StatItem]

  implicit lazy val reportInfo = Json.writes[ReportInfo]

  implicit lazy val user = Json.writes[User]

  implicit lazy val campaign = Json.writes[Campaign]

  implicit lazy val performance = Json.writes[Performance]

  //implicit lazy val phrasePriceInfo = Json.writes[PhrasePriceInfo]

}
