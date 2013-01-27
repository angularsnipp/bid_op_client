package controllers

import play.api.mvc._

 /**
 * Provide security features
 */ 
trait Secured {

  /**
   * Retrieve the connected user name.
   */
  private def username(request: RequestHeader) = request.session.get("name")

  /** 
   * Redirect to login if the user in not authorized.
   */ 
  private def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Auth.login)

  /**
   * Action for authenticated users. 
   */
  def IsAuthenticated(f: => String => Request[AnyContent] => Result) = Security.Authenticated(username, onUnauthorized) { user =>
    Action(request => f(user)(request))
  }
}