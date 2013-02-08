package controllers

import play.api.mvc._

object Application extends Controller with Secured {

  def home = IsAuthenticated {
    user => implicit request => Ok(views.html.static_pages.home(user))
  }
  
  def about = IsAuthenticated {
    user => implicit request => Ok(views.html.static_pages.home(user))
  }
  
  def contact = IsAuthenticated {
    user => implicit request => Ok(views.html.static_pages.home(user))
  }
  
  def help = IsAuthenticated {
    user => implicit request => Ok(views.html.static_pages.home(user))
  }

}