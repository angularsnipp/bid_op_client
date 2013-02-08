package controllers

import models._
import play.api.mvc._
import play.api.libs.iteratee._

/**
 * Provide security features
 */
trait Secured {

  /**
   * Retrieve the connected user name.
   */
  private def username(request: RequestHeader) = request.session.get("name")

  /**
   * Redirect to login if the user is not authorized.
   */
  private def onUnauthorized(f: => Option[User] => Request[AnyContent] => Result) = Action { request =>
    f(None)(request)
  }

  /**
   * Action for authenticated users.
   */
  def IsAuthenticated(f: => Option[User] => Request[AnyContent] => Result) =
    Authenticated(username, onUnauthorized(f)) { username =>
      Action { request =>
        User.findByName(username).map {
          user => f(Some(user))(request)
        }.getOrElse(f(None)(request))
      }
    }

  /**
   * Wraps another action, allowing only authenticated HTTP requests.
   * Furthermore, it lets users to configure where to retrieve the user info from
   * and what to do in case unsuccessful authentication
   *
   * @tparam A the type of the user info value (e.g. `String` if user info consists only in a user name)
   * @param userinfo function used to retrieve the user info from the request header
   * @param onUnauthorized function used to generate alternative result if the user is not authenticated
   * @param action the action to wrap
   */
  def Authenticated[A](
    userinfo: RequestHeader => Option[A],
    onUnauthorized: EssentialAction)(action: A => EssentialAction): EssentialAction = {

    EssentialAction { request =>
      userinfo(request).map { user =>
        action(user)(request)
      }.getOrElse {
        onUnauthorized(request)
      }
    }

  }
}
