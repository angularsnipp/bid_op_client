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
  def _apply(sd: DateTime, ed: DateTime, si: StatItem): Performance = Performance(
    start_date = sd,
    end_date = ed,
    sum_search = si.SumSearch,
    sum_context = si.SumContext,
    impress_search = si.ShowsSearch,
    impress_context = si.ShowsContext,
    clicks_search = si.ClicksSearch,
    clicks_context = si.ClicksContext)
}