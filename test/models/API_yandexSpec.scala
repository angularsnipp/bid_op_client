package models

import org.specs2.mutable._
import org.specs2.specification._

import common.Yandex
import models.API_yandex._

class API_yandexSpec extends Specification with AllExpectations {

  val login = "krisp0"
  val token = "1eac9f413271443ab402586ab45c1c93"

  /*------------- Ping API ------------------------------------------------------------*/
  "pingAPI" should {
    sequential

    "ping SANDBOX" in {
      //API_yandex(login, token, Yandex.url_sandbox) must_== (true)
    }

    "ping MAIN" in {
      API_yandex(login, token, Yandex.url_main).pingAPI must_== (true)
    }
  }

}