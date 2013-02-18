package json_api

import models._

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

  def fromJson[T](data: JsValue)(implicit mf: Manifest[T]): Option[T] = {
    import Reads._
    typeList.filter(_._2.equals(mf.toString)).headOption map {
      _._1 match {

        /* ************************************ */
        case "ShortCampaignInfo" => {
          Json.fromJson[ShortCampaignInfo](data) map (s => Some(s.asInstanceOf[T])) recoverTotal (e => None)
        }
        case "List[ShortCampaignInfo]" => {
          Json.fromJson[List[ShortCampaignInfo]](data) map (s => Some(s.asInstanceOf[T])) recoverTotal (e => None)
        }

        /* ************************************ */
        case "BannerInfo" => {
          Json.fromJson[BannerInfo](data) map (s => Some(s.asInstanceOf[T])) recoverTotal (e => None)
        }
        case "List[BannerInfo]" => {
          Json.fromJson[List[BannerInfo]](data) map (s => Some(s.asInstanceOf[T])) recoverTotal (e => None)
        }

        /* ************************************ */
        case "StatItem" => {
          Json.fromJson[StatItem](data) map (s => Some(s.asInstanceOf[T])) recoverTotal (e => None)
        }
        case "List[StatItem]" => {
          Json.fromJson[List[StatItem]](data) map (s => Some(s.asInstanceOf[T])) recoverTotal (e => None)
        }

        /* ************************************ */
        case "ReportInfo" => {
          Json.fromJson[ReportInfo](data) map (s => Some(s.asInstanceOf[T])) recoverTotal (e => None)
        }
        case "List[ReportInfo]" => {
          Json.fromJson[List[ReportInfo]](data) map (s => Some(s.asInstanceOf[T])) recoverTotal (e => None)
        }

      }
    } getOrElse (None)
  }

  def toJson[T](data: T)(implicit mf: Manifest[T]): JsValue = {
    import Writes._
    typeList.filter(_._2.equals(mf.toString)).headOption map {
      _._1 match {

        /* ************************************ */
        case "ShortCampaignInfo" => Json.toJson[ShortCampaignInfo](data.asInstanceOf[ShortCampaignInfo])
        case "List[ShortCampaignInfo]" => Json.toJson[List[ShortCampaignInfo]](data.asInstanceOf[List[ShortCampaignInfo]])

        /* ************************************ */
        case "BannerInfo" => Json.toJson[BannerInfo](data.asInstanceOf[BannerInfo])
        case "List[BannerInfo]" => Json.toJson[List[BannerInfo]](data.asInstanceOf[List[BannerInfo]])

        /* ************************************ */
        case "StatItem" => Json.toJson[StatItem](data.asInstanceOf[StatItem])
        case "List[StatItem]" => Json.toJson[List[StatItem]](data.asInstanceOf[List[StatItem]])

        /* ************************************ */
        case "ReportInfo" => Json.toJson[ReportInfo](data.asInstanceOf[ReportInfo])
        case "List[ReportInfo]" => Json.toJson[List[ReportInfo]](data.asInstanceOf[List[ReportInfo]])

      }
    } getOrElse (JsNull)
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