package controllers

import Application._

import models._

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

object Auth extends Controller {

  /* -- Authentication ---------------------------------------------------------------------------------------*/

  val loginForm = Form(
    tuple(
      "user" -> mapping(
        "name" -> nonEmptyText,
        "password" -> nonEmptyText)(User.apply)(User.unapply),
      "create" -> boolean //checked("Create new User")    
      ) verifying ("Invalid user or password", lform => lform match {
        case (user, create) => {
          /*if a new User is checked -> try to put it in DB*/
          if (create)
            User.create(user).isDefined
          else
            User.authenticate(user).isDefined
        }
      }))

  /**
   * Login page.
   */
  def login = Action { implicit request =>
    Ok(views.html.login(loginForm))
  }

  /**
   * Handle login form submission.
   */
  def authenticate = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.login(formWithErrors)),
      request => request match {
        case (user, create) => Redirect(routes.Networks.index()).withSession("name" -> user.name)
      })
  }

  /**
   * Logout and clean the session.
   */
  def logout = Action { implicit request =>
    Redirect(routes.Auth.login).withNewSession.flashing(
      "success" -> "You've been logged out")
  }  

}

