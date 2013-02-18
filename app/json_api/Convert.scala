package json_api

import models._
import Reads._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.reflect.runtime._
import scala.reflect.ClassTag
import scala.collection.immutable.List
object Convert {

  val typeList = Map(
    "ShortCampaignInfo" -> "models.ShortCampaignInfo",
    "List[ShortCampaignInfo]" -> "scala.collection.immutable.List[models.ShortCampaignInfo]",

    "BannerInfo" -> "models.BannerInfo",
    "List[BannerInfo]" -> "scala.collection.immutable.List[models.BannerInfo]",

    "StatItem" -> "models.StatItem",
    "List[StatItem]" -> "scala.collection.immutable.List[models.StatItem]",

    "ReportInfo" -> "models.ReportInfo",
    "List[ReportInfo]" -> "scala.collection.immutable.List[models.ReportInfo]")

  /*----- YANDEX -----*/

  def fromJson[T](jsData: JsValue)(implicit mf: Manifest[T]): Option[T] = {
    typeList.filter(_._2.equals(mf.toString)).headOption map {
      _._1 match {

        /* ************************************ */
        case "ShortCampaignInfo" => {
          Json.fromJson[ShortCampaignInfo](jsData) map (s => Some(s.asInstanceOf[T])) recoverTotal (e => None)
        }
        case "List[ShortCampaignInfo]" => {
          Json.fromJson[List[ShortCampaignInfo]](jsData) map (s => Some(s.asInstanceOf[T])) recoverTotal (e => None)
        }

        /* ************************************ */
        case "BannerInfo" => {
          Json.fromJson[BannerInfo](jsData) map (s => Some(s.asInstanceOf[T])) recoverTotal (e => None)
        }
        case "List[BannerInfo]" => {
          Json.fromJson[List[BannerInfo]](jsData) map (s => Some(s.asInstanceOf[T])) recoverTotal (e => None)
        }

        /* ************************************ */
        case "StatItem" => {
          Json.fromJson[StatItem](jsData) map (s => Some(s.asInstanceOf[T])) recoverTotal (e => None)
        }
        case "List[StatItem]" => {
          Json.fromJson[List[StatItem]](jsData) map (s => Some(s.asInstanceOf[T])) recoverTotal (e => None)
        }

        /* ************************************ */
        case "ReportInfo" => {
          Json.fromJson[ReportInfo](jsData) map (s => Some(s.asInstanceOf[T])) recoverTotal (e => None)
        }
        case "List[ReportInfo]" => {
          Json.fromJson[List[ReportInfo]](jsData) map (s => Some(s.asInstanceOf[T])) recoverTotal (e => None)
        }

      }
    } getOrElse (None)
  }
}

/*
Map(
    "ShortCampaignInfo" -> classOf[ShortCampaignInfo],
    "List[ShortCampaignInfo]" -> classOf[List[ShortCampaignInfo]],

    "BannerInfo" -> classOf[BannerInfo],
    "List[BannerInfo]" -> classOf[List[BannerInfo]],

    "StatItem" -> classOf[StatItem],
    "List[StatItem]" -> classOf[List[StatItem]],

    "ReportInfo" -> classOf[ReportInfo],
    "List[ReportInfo]" -> classOf[List[ReportInfo]])    
    
     
    val classT = mf.runtimeClass
    typeList.filter(_._2 isAssignableFrom classT).headOption map {}
    
*/