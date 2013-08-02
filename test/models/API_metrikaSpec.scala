package models

import org.specs2.mutable._
import org.specs2.specification._
import org.joda.time.DateTime

import common.Yandex

class API_metrikaSpec extends Specification with AllExpectations {
  val user_login = "moksellegroup" //fake login
  val token = "ead3771587ef460e8a4d38d87d5203fc"//"3283fafd354840bda4f6c727028495de" //fake token

  val m = API_metrika(user_login, token)
  val cur_ft = new DateTime(m.date_fmt.parse("20130524"))

  /*------------- Ping API ------------------------------------------------------------*/
  "pingAPI" should {
    sequential

    "counters" in {
      val res = m.counters(List(user_login))
      res must_== (List(19740241, 19740337))
    }

    "summary" in {
      val res0 = m.summary(m.counters(List(user_login)), cur_ft.minusMonths(3), cur_ft)

      val res = res0.head._2
      res.date2 must_== ("20130524")
      res.id must_== ("19740241")
      res.data.length must_== (1)
      res.data.head.visits must_== (Some(232))
      res.data.head.denial must_== (Some(0.2155))

      val res1 = res0.last._2
      res1.date2 must_== ("20130524")
      res1.id must_== ("19740337")
      res1.data.length must_== (1)
      res1.data.head.visits must_== (Some(3595))
      res1.data.head.denial must_== (Some(0.1744))

    }

    "summaryGoals" in {
      val ssml = m.summary(m.counters(List(user_login)), cur_ft.minusMonths(3), cur_ft)
      val cgl = ssml.map { v =>
        v._1 -> v._2.goals
      }
      val res0 = m.summaryGoals(cgl, cur_ft.minusMonths(3), cur_ft)
      res0.map(_._1) must_== (List(19740241, 19740337))
      res0.map(_._2) must_== (List(1867900, 1867921))

      val res1 = res0.filter(s => s._1 == 19740241 & s._2 == 1867900).map(_._3).head
      res1.date2 must_== ("20130524")
      res1.id must_== ("19740241")
      res1.goal_id must_== (Some(1867900))
      res1.data.length must_== (1)

      val cdata1 = res1.data.head
      cdata1.direct_id must_== (Some("N-5862077"))
      cdata1.visits must_== (Some(39))
      cdata1.visits_all must_== (Some(232))
      cdata1.chld.get.length must_== (4)

      val bdata1 = cdata1.chld.get.head
      bdata1.direct_id must_== (Some("M-135501682"))
      bdata1.visits must_== (Some(14))
      bdata1.visits_all must_== (Some(79))
      bdata1.chld.get.length must_== (2)

      val phdata1 = bdata1.chld.get.head
      phdata1.phrase_id must_== (Some(204569894))
      phdata1.visits must_== (Some(13))
      phdata1.visits_all must_== (Some(59))
      phdata1.chld.get.length must_== (3)

      /******-------------------------------------------------******/
      val res2 = res0.filter(s => s._1 == 19740337 & s._2 == 1867921).map(_._3).head
      res2.date2 must_== ("20130524")
      res2.id must_== ("19740337")
      res2.goal_id must_== (Some(1867921))
      res2.data.length must_== (1)

      val cdata2 = res2.data.head
      cdata2.direct_id must_== (Some("N-5871547"))
      cdata2.visits must_== (Some(49))
      cdata2.visits_all must_== (Some(3595))
      cdata2.chld.get.length must_== (4)

      val bdata2 = cdata2.chld.get.head
      bdata2.direct_id must_== (Some("M-135501595"))
      bdata2.visits must_== (Some(28))
      bdata2.visits_all must_== (Some(1367))
      bdata2.chld.get.length must_== (5)

      val phdata2 = bdata2.chld.get.head
      phdata2.phrase_id must_== (Some(204565942))
      phdata2.visits must_== (Some(20))
      phdata2.visits_all must_== (Some(1015))
      phdata2.chld.get.length must_== (6)
    }

    "cSummary" in {

      val ssml = m.summary(m.counters(List(user_login)), cur_ft.minusMonths(3), cur_ft)
      val cgl = ssml.map { v =>
        v._1 -> v._2.goals
      }

      val ssmgl = m.summaryGoals(cgl, cur_ft.minusMonths(3), cur_ft)

      val res0 = m.cSummary(ssml, ssmgl)
      res0.length must_== (2)
      val pm1 = res0.head
      pm1.counter_id must_== (19740241)
      pm1.campaignID must_== (Some(5862077))
      pm1.statWithoutGoals.visits must_== (232)
      pm1.statWithoutGoals.denial must_== (0.2155)
      val wg1 = pm1.statWithGoals
      wg1.length must_== (1)
      wg1.head.goal_id must_== (1867900)
      wg1.head.visits must_== (39)
      wg1.head.visits_all must_== (232)

      val pm2 = res0.last
      pm2.counter_id must_== (19740337)
      pm2.campaignID must_== (Some(5871547))
      pm2.statWithoutGoals.visits must_== (3595)
      pm2.statWithoutGoals.denial must_== (0.1744)
      val wg2 = pm2.statWithGoals
      wg2.length must_== (1)
      wg2.head.goal_id must_== (1867921)
      wg2.head.visits must_== (49)
      wg2.head.visits_all must_== (3595)

    }

    "bpSummary" in {
      val ssml = m.summary(m.counters(List(user_login)), cur_ft.minusMonths(3), cur_ft)
      val cgl = ssml.map { v =>
        v._1 -> v._2.goals
      }

      val ssmgl = m.summaryGoals(cgl, cur_ft.minusMonths(3), cur_ft)

      val res0 = m.bpSummary(ssml, ssmgl)
      res0.length must_== (52)
      val pm1 = res0.head
      pm1.counter_id must_== (19740241)
      pm1.campaignID must_== (Some(5862077))
      pm1.bannerID must_== (Some(135501682))
      pm1.phrase_id must_== (Some(204569894))
      pm1.statWithoutGoals.visits must_== (59)
      pm1.statWithoutGoals.denial must_== (0.2881)
      pm1.statWithGoals.length must_== (1)
      val wg1 = pm1.statWithGoals.head
      wg1.goal_id must_== (1867900)
      wg1.visits must_== (13)
      wg1.visits_all must_== (59)
      wg1.goal_reaches must_== (13)

    }
  }
}