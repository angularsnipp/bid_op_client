package controllers

import Application._
import models._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.ws.WS
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json.Json
import json_api.Convert._

object Networks extends Controller with Secured {

  val getLoginForm = Form(
    tuple(
      "login" -> nonEmptyText,
      "token" -> text,
      "network" -> text) verifying ("Invalid user or password", lform => lform match {
        case (login, token, network) => network match {
          case "Yandex" => OAuth_yandex.isSuccess(login, token)
          case "Google" => false
          case "Begun" => false
        }
      }))

  def index(network: String) = IsAuthenticated {
    user =>
      implicit request => {
        request.queryString.get("code") match {
          // try to get token, if creating campaign
          case Some(code) => {
            //TODO
            OAuth_yandex.getToken(code.head) match {
              case None => BadRequest("Invalid token...")
              case Some(token) => Redirect(routes.Networks.externalLogin(network, token))
            }
          }
          case None => {
            user match {
              case None => Redirect(routes.Application.home)
              case Some(someuser) =>
                network match {

                  case "Yandex" => Ok(views.html.workspace.campaigns.yandex(user, "Yandex"))

                  case "Google" => Ok(views.html.workspace.campaigns.google(user, "Google"))

                  case "Begun" => Ok(views.html.workspace.campaigns.begun(user, "Begun"))

                  case "" => Ok(views.html.workspace.campaigns.index(user))

                }
            }
          }
        }
      }

  }

  def getCharts(network: String, campaignID: String) = IsAuthenticated {
    user =>
      _ => {
        import scala.concurrent.Future
        import scala.concurrent.ExecutionContext.Implicits.global
        import java.util.concurrent.TimeUnit
        val futureResult = Future {
          user map { u =>
            u.name match {
              case "krisp0" =>
                val url = common.Bid.Base_URI + "/user/" + u.name + "/net/" + network + "/camp/" + campaignID + "/charts/" + u.password
                Redirect(url)
              case _ => NotFound
            }
            //Redirect(url).withHeaders(("password" -> u.password))          
          } getOrElse (NotFound)
        }

        // if service handles request too slow => return Timeout response
        val timeoutFuture = play.api.libs.concurrent.Promise.timeout(
          message = "Oops, TIMEOUT while calling BID server...",
          duration = 2,
          unit = TimeUnit.MINUTES)

        Async {
          Future.firstCompletedOf(Seq(futureResult, timeoutFuture)).map {
            case f: Result => f
            case t: String => InternalServerError(t)
          }
        }
      }
  }

  def campaignReport(network: String, campaign: String) = IsAuthenticated {
    user => _ => Ok(views.html.workspace.reports.report(user, network, fromJson[Campaign](Json.parse(campaign)).get))
  }

  def externalLogin(network: String, token: String) = IsAuthenticated {
    user => _ => Ok(views.html.workspace.campaigns.external_login(user, network, token, getLoginForm))
  }

  def externalCampaigns(network: String, token: String) = IsAuthenticated {
    user =>
      implicit request => {
        getLoginForm.bindFromRequest.fold(
          formWithErrors => BadRequest(views.html.workspace.campaigns.external_login(user, network, token, formWithErrors)),
          req => req match {
            case (login, token, network) => {
              import play.api.libs.concurrent.Akka
              import scala.concurrent.duration._
              import play.api.Play.current

              val keepAlive = Akka.system.scheduler.schedule(10 seconds, 10 seconds) {
                //send head request to the client to keep alive
                WS.url(request.headers.get("Referer").get).head().onSuccess {
                  case _ => println("!!! wake up client !!!" + request.headers.get("Referer").get)
                }
              }

              Async {
                API_yandex(login, token).getCampaignsList map { cList =>
                  keepAlive.cancel //stop schedule keepAlive - the result is ready!

                  Ok(views.html.workspace.campaigns.external(user, network, login, token, cList))
                }
              }
            }
          })

      }
  }

}