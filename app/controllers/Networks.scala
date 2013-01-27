package controllers

import Application._
import models._
import play.api.mvc._

import play.api.data._
import play.api.data.Forms._

import com.codahale.jerkson.Json
import play.api.libs.ws.WS

object Networks extends Controller with Secured {

  val getLoginForm = Form(
    tuple(
      "login" -> nonEmptyText,
      "token" -> text,
      "network" -> text) verifying ("Invalid user or password", lform => lform match {
        case (login, token, network) => network match {
          case "Yandex" => Yandex.isSuccess(login, token)
          case "Google" => false
          case "Begun" => false
        }
      }))

  def index(network: String) = IsAuthenticated {
    username =>
      implicit request => {
        User.findByName(username).map { user =>
          request.queryString.get("code") match {
            // try to get token, if creating campaign
            case Some(code) => {
              //TODO
              Yandex.getToken(code.head) match {
                case None => BadRequest("Invalid token...")
                case Some(token) =>
                  val net = "Yandex"
                  Redirect(routes.Networks.externalLogin(net, token))
              }
            }
            case None => {
              network match {

                case "Yandex" => Ok(views.html.campaigns.yandex(user.name, "Yandex"))

                case "Google" => Ok(views.html.campaigns.google(user.name, "Goggle"))

                case "Begun" => Ok(views.html.campaigns.begun(user.name, "Begun"))

                case "" => Ok(views.html.campaigns.index(user.name))

              }
            }
          }
        }.getOrElse(Forbidden)
      }

  }

  def campaignReport(network: String, campaign: String) = IsAuthenticated {
    username => _ => Ok(views.html.reports.report(username, network, Json.parse[models.Campaign](campaign)))
  }

  def externalLogin(network: String, token: String) = IsAuthenticated {
    username => _ => Ok(views.html.campaigns.external_login(username, network, token, getLoginForm))
  }

  def externalCampaigns(network: String, token: String) = IsAuthenticated {
    username =>
      implicit request => {
        getLoginForm.bindFromRequest.fold(
          formWithErrors => BadRequest(views.html.campaigns.external_login(username, network, token, formWithErrors)),
          request => request match {
            case (login, token, network) => Ok(views.html.campaigns.external(username, network, login, token))
          })

      }
  }

}