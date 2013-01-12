package models

import org.joda.time._
import com.codahale.jerkson.Json

case class Campaigns(
  val list: List[String] = List())

case class Campaign(
  val _login: String = "",
  val _token: String = "",
  val network_campaign_id: String = "",
  val start_date: DateTime = new DateTime,
  val end_date: DateTime = new DateTime,
  val daily_budget: Double = 0.0)

object Campaign {

  //create Campaign using ShortCampaignInfo from Yandex 
  def apply(login: String, token: String, sci: ShortCampaignInfo): Campaign = {
    Campaign(
      _login = login,
      _token = token,
      network_campaign_id = sci.CampaignID.toString(),
      start_date = sci.StartDate,
      daily_budget = sci.Rest)
  }

}