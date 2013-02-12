package controllers

import play.api.mvc._

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

}