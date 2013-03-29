package common

import java.text._

object Yandex {

  /**-------------------------------- YANDEX information ----------------------------------**/
  /**--------------------------------------------------------------------------------------**/

  /* url for YANDEX api */
  val url_sandbox = "https://api-sandbox.direct.yandex.ru/json-api/v4/" //SANDBOX - for testing 
  val url_main = "https://api.direct.yandex.ru/live/v4/json/" //works (recommended)
  //val url_main = "https://soap.direct.yandex.ru/json-api/v4/" //works  

  val url_simulator = "http://localhost:9002/api"

  val url = url_main
  //val url = url_sandbox
  //val url = url_simulator

  /* application information for YANDEX api */
  //// krisp0_bid app - for heroku
  val app_id = "bee99a08160d4287aa0e468b9be7ed91" //client_id
  val app_secret = "d08e6a30a6d44c3b9da147f3e76880de" //client_secret

  //local_app_bid - for local testing
  //val app_id = "e3a82ab5b4054deda4bb917d2a537224" //client_id
  //val app_secret = "f36429ca82044f3990338ea6f93b8ba7" //client_secret  

  /* OAuth information */
  val url_OAuthAuthorization = "https://oauth.yandex.ru/authorize?response_type=code&client_id=" + app_id //url for user Authorization on Yandex
  val url_OAuthToken = "https://oauth.yandex.ru/token" //url for getting TOKEN using authorization code

  /* url for YANDEX api to get login */
  val url_apiLogin = "https://api.direct.yandex.ru/live/v4/json/me/" //failed ((

  /* Date format */
  val date_fmt = new SimpleDateFormat("yyyy-MM-dd")
}