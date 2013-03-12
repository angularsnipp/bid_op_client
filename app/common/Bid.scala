package common

import play.api.mvc._
import org.joda.time.format

object Bid extends Controller {

  /**----------------------------- BID_OP information ------------------------------**/
  /**----------------------------- ------------------ ------------------------------**/

  /* url for BID api web server */
  val Base_URI = "http://localhost:9001"
  //val Base_URI = "http://bid-op-service.herokuapp.com/"
    

  /* DateTime format on BID */
  val iso_fmt = format.ISODateTimeFormat.dateTime()
}