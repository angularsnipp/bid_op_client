package models
import com.codahale.jerkson.Json

case class User(
  val name: String,
  val password: String)

object User {

  /**
   * Authenticate a User.
   */
  def authenticate(name: String, password: String): Option[User] = {
    val juser = API_bid.getUser(user = name, password = password)
    try {
      val user = Json.parse[User](juser)
      Some(user)
    } catch {
      case t => None
    }
  }

  def authenticate(user: User): Option[User] = {
    this.authenticate(user.name, user.password)
  }

  def create(name: String, password: String): Option[User] = {
    val juser = API_bid.postUser(name, password)
    try {
      Some(Json.parse[User](juser))
    } catch {
      case t => None
    }
  }

  def create(user: User): Option[User] = {
    this.create(user.name, user.password)
  }
}