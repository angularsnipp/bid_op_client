package controllers

import play.api.mvc._
import org.joda.time.format

object Bid extends Controller {

  /**----------------------------- BID_OP information ------------------------------**/
  /**----------------------------- ------------------ ------------------------------**/

  /* url for BID api web server */
  val Base_URI = "http://localhost:9001"

  /* DateTime format on BID */
  val iso_fmt = format.ISODateTimeFormat.dateTime()
}