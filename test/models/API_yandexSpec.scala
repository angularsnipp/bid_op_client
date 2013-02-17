package models

import common.Yandex
import models.API_yandex._

import org.specs2.mutable._
import org.specs2.specification._

class API_yandexSpec extends Specification with AllExpectations {

  val login = "krisp0"
  val token = "1eac9f413271443ab402586ab45c1c93"

  /*------------- Ping API ------------------------------------------------------------*/
  "pingAPI" should {
    sequential

    "ping SANDBOX" in {
      //pingAPI(login, token, Yandex.url_sandbox) must_== (true)
    }

    "ping MAIN" in {
      pingAPI(login, token, Yandex.url_main) must_== (true)
    }
  }  
}