package models

import play.api.db._
import play.api.Play.current
import anorm._
import anorm.SqlParser._

case class User(
  val name: String,
  val password: String)

object User extends Function2[String, String, User] {

  // -- Parsers

  /**
   * Parse a User from a ResultSet
   */
  val simple = {
    get[String]("user.name") ~
      get[String]("user.password") map {
        case name ~ password => User(name, password)
      }
  }

  /**
   * Retrieve all users.
   */
  def findAll: Seq[User] = {
    DB.withConnection { implicit connection =>
      SQL("""
          select * from "user"
          """).as(User.simple *)
    }
  }

  /**
   * Retrieve a User from name.
   */
  def findByName(name: String): Option[User] = {
    findAll.filter(_.name.equals(name)) match {
      case Nil => None
      case userSeq => userSeq.headOption
    }
  }

  /**
   * Authenticate a User.
   */
  def authenticate(user: User): Option[User] = {
    DB.withConnection { implicit connection =>
      SQL(
        """
    		  select * from "user" where 
    		  name = {name} and password = {password}
        """).on(
          'name -> user.name,
          'password -> user.password).as(User.simple.singleOpt)
    }
  }

  /**
   * Create a User.
   */
  def create(user: User): Option[User] = {
    //Check if user EXISTs
    if (User.findByName(user.name).isDefined) None
    else {
      //If User is Created on BID => Create User on internal Client Server
      if (API_bid.postUser(user).isDefined) {
        DB.withConnection { implicit connection =>
          SQL(
            """
        		  insert into "user" values (
        		  {name}, {password} )
          """).on(
              'name -> user.name,
              'password -> user.password).executeUpdate()
        }
        Some(user)
      } else None
    }
  }

  //Clear User table
  def truncate: Boolean = {
    DB.withConnection(implicit connection =>
      SQL("""
    		  truncate table "user"
          """).execute())
  }
}

/*postgresql query
   select 'drop table "' || tablename || '" cascade;' 
   from pg_tables where schemaname = 'public';
 */
/*
 truncate table user;
 truncate table play_evolutions
 */