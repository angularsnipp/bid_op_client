package models

import org.joda.time._

case class Performance(
  val start_date: DateTime,
  val end_date: DateTime,
  val sum_search: Double = 0.0,
  val sum_context: Double = 0.0,
  val impress_search: Int = 0,
  val impress_context: Int = 0,
  val clicks_search: Int = 0,
  val clicks_context: Int = 0)

object Performance extends Function8[DateTime, DateTime, Double, Double, Int, Int, Int, Int, Performance] {
  /* constructor for request to BID API 
   * si - we take from YANDEX API*/
  def _apply(sd: DateTime, ed: DateTime, si: List[StatItem]): Performance = Performance(
    start_date = sd,
    end_date = ed,
    sum_search = si.map(_.SumSearch).sum,
    sum_context = si.map(_.SumContext).sum,
    impress_search = si.map(_.ShowsSearch).sum,
    impress_context = si.map(_.ShowsContext).sum,
    clicks_search = si.map(_.ClicksSearch).sum,
    clicks_context = si.map(_.ClicksContext).sum)
}