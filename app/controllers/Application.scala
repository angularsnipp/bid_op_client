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
   * */
  
  def admin = IsAuthenticated {
    user =>
      implicit request => user match {
        case Some(krisp0) => Ok(views.html.static_pages.admin(user, Scheduler.isStarted & !Scheduler.isInStandbyMode))
        case _ => Redirect(routes.Application.home)
      }
  }

  def startJobs = IsAuthenticated {
    user =>
      implicit request => user match {
        case Some(krisp0) =>
          Scheduler.start
          Ok(views.html.static_pages.admin(user, Scheduler.isStarted & !Scheduler.isInStandbyMode))
        case _ => Redirect(routes.Application.home)
      }

  }

  def stopJobs = IsAuthenticated {
    user =>
      implicit request => user match {
        case Some(krisp0) =>
          Scheduler.stop
          Ok(views.html.static_pages.admin(user, Scheduler.isStarted & !Scheduler.isInStandbyMode))
        case _ => Redirect(routes.Application.home)
      }
  }
}