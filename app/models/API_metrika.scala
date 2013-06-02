package models

import api._
import json_api.Convert._
import play.api.libs.json._
import org.joda.time._
import java.text._

case class API_metrika(
  val login: String,
  val token: String) {

  /* Date format */
  val date_fmt = new SimpleDateFormat("yyyyMMdd")

  lazy val metrika = Metrika(login, token)

  def counters: List[Long] = {
    val res = metrika.getCounterList()
    val cl = (res \ "counters").as[List[JsValue]]

    cl.map { js => (js \ "id").as[String].toLong }
  }

  def summary(counters: List[Long], date1: DateTime, date2: DateTime): List[(Long, StatSummaryMetrika)] = {
    val p = OParameters(
      date1 = Some(date_fmt.format(date1.toDate)),
      date2 = Some(date_fmt.format(date2.toDate)),
      table_mode = Some("tree"))

    counters.flatMap { counter_id =>
      p.id = Some(counter_id)
      val res = metrika.getStatSourcesDirectSummary(p)

      fromJson[StatSummaryMetrika](res).map { ssm =>
        counter_id -> ssm
      }
    }
  }

  def summaryGoals(cgl: List[(Long, List[Long])], date1: DateTime, date2: DateTime): List[(Long, Long, StatSummaryMetrika)] = {
    val p = OParameters(
      date1 = Some(date_fmt.format(date1.toDate)),
      date2 = Some(date_fmt.format(date2.toDate)),
      table_mode = Some("tree"))

    (for {
      cg <- cgl //(counter_id,goals)
      goal_id <- cg._2
    } yield {
      p.id = Some(cg._1)
      p.goal_id = Some(goal_id)
      val res = metrika.getStatSourcesDirectSummary(p)
      fromJson[StatSummaryMetrika](res).map { ssm =>
        (cg._1, goal_id, ssm)
      }
    }).flatten
  }

  def cSummary(ssml: List[(Long, StatSummaryMetrika)], ssmgl: List[(Long, Long, StatSummaryMetrika)]): List[PerformanceMetrika] = {
    ssml.flatMap {
      case (counter_id, ssm) =>
        val cssmgl = ssmgl.filter(_._1 == counter_id)
        for {
          cl <- ssm.data
        } yield {
          val campaignID = cl.direct_id.map(_.filter(_.isDigit).toLong)

          val withGoals = cssmgl.flatMap {
            case (_, goal_id, ssmg) =>
              ssmg.data
                .filter(_.direct_id.map(_.filter(_.isDigit).toLong) == campaignID)
                .headOption
                .map { cdata =>
                  WithGoal(
                    counter_id = counter_id,
                    goal_id = goal_id,

                    visits = cdata.visits.getOrElse(0),
                    visits_all = cdata.visits_all.getOrElse(0),
                    goal_reaches = cdata.goal_reaches.getOrElse(0))
                }
          }

          PerformanceMetrika(
            counter_id = counter_id,
            campaignID = campaignID, //"N-5862077"

            statWithoutGoals = WithoutGoal(
              visits = cl.visits.getOrElse(0),
              denial = cl.denial.getOrElse(0d)),

            statWithGoals = withGoals)
        }
    }
  }

  def bpSummary(ssml: List[(Long, StatSummaryMetrika)], ssmgl: List[(Long, Long, StatSummaryMetrika)]): List[PerformanceMetrika] = {
    ssml.flatMap {
      case (counter_id, ssm) =>
        val cssmgl = ssmgl.filter(_._1 == counter_id)
        for {
          cl <- ssm.data
          bl <- cl.chld.getOrElse(Nil)
          phl <- bl.chld.getOrElse(Nil)
        } yield {
          val campaignID = cl.direct_id.map(_.filter(_.isDigit).toLong)
          val bannerID = bl.direct_id.map(_.filter(_.isDigit).toLong)
          val phrase_id = phl.phrase_id

          val withGoals = cssmgl.flatMap {
            case (_, goal_id, ssmg) =>
              ssmg.data
                .filter(_.direct_id.map(_.filter(_.isDigit).toLong) == campaignID)
                .headOption
                .map { cdata =>
                  cdata.chld
                    .map {
                      _
                        .filter(_.direct_id.map(_.filter(_.isDigit).toLong) == bannerID)
                        .headOption
                        .map { bdata =>
                          bdata.chld
                            .map {
                              _
                                .filter(_.phrase_id == phrase_id)
                                .headOption
                                .map { phdata =>
                                  WithGoal(
                                    counter_id = counter_id,
                                    goal_id = goal_id,

                                    visits = phdata.visits.getOrElse(0),
                                    visits_all = phdata.visits_all.getOrElse(0),
                                    goal_reaches = phdata.goal_reaches.getOrElse(0))
                                }
                            }
                            .getOrElse { None }
                        }
                        .getOrElse(None)
                    }
                    .getOrElse { None }
                }
                .getOrElse(None)
          }

          PerformanceMetrika(
            counter_id = counter_id,
            campaignID = campaignID, //"N-5862077"
            bannerID = bannerID,
            phrase_id = phrase_id,

            statWithoutGoals = WithoutGoal(
              visits = phl.visits.getOrElse(0),
              denial = phl.denial.getOrElse(0d)),

            statWithGoals = withGoals)
        }
    }
  }

}