package controllers

import play.api.mvc._

import jobs._

object Application extends Controller with Secured {

  def index = Action { implicit request =>
    Redirect(routes.Networks.index(""))
  }

  def home = IsAuthenticated {
    user => implicit request => Ok(views.html.static_pages.home(user))
  }

  def about = IsAuthenticated {
    user => implicit request => Ok(views.html.static_pages.about(user))
  }

  def contact = IsAuthenticated {
    user => implicit request => Ok(views.html.static_pages.contact(user))
  }

  def help = IsAuthenticated {
    user => implicit request => Ok(views.html.static_pages.help(user))
  }

  /**
   * ---------------- Schedule Jobs Control -----------------
   * only krisp0 have access
   */

  import models._

  def admin = IsAuthenticated {
    user =>
      implicit request => user map { u =>
        u.name match {
          case "krisp0" => Ok(views.html.static_pages.admin(user, Scheduler.isStarted & !Scheduler.isInStandbyMode))
          case _ => Redirect(routes.Application.home)
        }
      } getOrElse { Redirect(routes.Application.home) }
  }

  def startJobs = IsAuthenticated {
    user =>
      implicit request => user map { u =>
        u.name match {
          case "krisp0" =>
            Scheduler.start
            Ok(views.html.static_pages.admin(user, Scheduler.isStarted & !Scheduler.isInStandbyMode))
          case _ => Redirect(routes.Application.home)
        }
      } getOrElse { Redirect(routes.Application.home) }
  }

  def stopJobs = IsAuthenticated {
    user =>
      implicit request => user map { u =>
        u.name match {
          case "krisp0" =>
            Scheduler.stop
            Ok(views.html.static_pages.admin(user, Scheduler.isStarted & !Scheduler.isInStandbyMode))
          case _ => Redirect(routes.Application.home)
        }
      } getOrElse { Redirect(routes.Application.home) }
  }

  def clearDB = IsAuthenticated {
    user =>
      implicit request => user map { u =>
        u.name match {
          case "krisp0" =>
            if (API_bid.clearDB)
              println("!!! BID DB is CLEAR !!!")
            else
              println("??? FAIL ---> BID DB is NOT CLEAR ???")

            if (User.truncate)
              println("!!! CLIENT DB is CLEAR !!!")
            else
              println("??? FAIL ---> CLIENT DB is NOT CLEAR ???")
            Ok
          case _ => Redirect(routes.Application.home)
        }
      } getOrElse { Redirect(routes.Application.home) }
  }

  def getUsers = IsAuthenticated {
    user =>
      implicit request => user map { u =>
        u.name match {
          case "krisp0" =>
            Ok(User.findAll.toString)
          case _ => Redirect(routes.Application.home)
        }
      } getOrElse { Redirect(routes.Application.home) }
  }

  def wakeUP = Action { Ok }
}