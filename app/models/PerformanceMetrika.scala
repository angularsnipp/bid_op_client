package models

import org.joda.time.DateTime

/**
 * TO JSON -> to BID
 */
case class PerformanceMetrika(
  val counter_id: Long = 0,
  val campaignID: Option[Long] = None,
  val bannerID: Option[Long] = None,
  val phrase_id: Option[Long] = None,

  val statWithoutGoals: WithoutGoal = WithoutGoal(),
  val statWithGoals: List[WithGoal] = Nil) {
}

case class WithoutGoal(
  val visits: Int = 0,
  val denial: Double = 0d //percentage
  )

case class WithGoal(
  val counter_id: Long,
  val goal_id: Long = 0,

  val visits: Int = 0,
  val visits_all: Int = 0,
  val goal_reaches: Int = 0)

/**
 * FROM JSON -> from METRIKA
 */
case class StatSummaryMetrika(
  val id: String, //counter_id
  val goal_id: Option[Long] = None,
  val goals: List[Long] = Nil,
  val rows: Int,
  val date1: String,
  val date2: String,
  val data: List[DataStat] = Nil)

case class DataStat(
  val visits: Option[Int] = None,
  val visits_all: Option[Int] = None,
  val denial: Option[Double] = None, //percentage
  val goal_reaches: Option[Int] = None,

  val direct_id: Option[String] = None, //"N-5862077" - campaign; "M-135501682" - banner
  val phrase_id: Option[Long] = None,

  val `type`: Option[String] = None, //"campaign", "banner", "phrase"
  val chld: Option[List[DataStat]] = None)
