package models

import play.api.libs.json._
import play.api.libs.functional.syntax._

object Formats {
  
  /************* JSON WRITEs and READs for creating REQUESTs and handling RESULTs ************/
  /************* or just define JSON FORMATs 									  ************/	
  
  implicit lazy val shortCampaignInfo = Json.format[ShortCampaignInfo]
  
  implicit lazy val bannerPhraseInfo = Json.format[BannerPhraseInfo]
  implicit lazy val bannerInfo = Json.format[BannerInfo]

  implicit lazy val statItem = Json.format[StatItem]

  implicit lazy val shortReportInfo = Json.format[ShortReportInfo]

  implicit lazy val reportInfo = Json.format[ReportInfo]

  
  implicit lazy val user = Json.format[User]
  
  implicit lazy val campaign = Json.format[Campaign]
  
  implicit lazy val performance = Json.format[Performance]
  
  //implicit lazy val phrasePriceInfo = Json.format[PhrasePriceInfo]

}