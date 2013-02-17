package controllers

import Application._
import models._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.ws.WS
import play.api.libs.json._
import play.api.libs.functional.syntax._
import common.Yandex

object Networks extends Controller with Secured {

  val getLoginForm = Form(
    tuple(
      "login" -> nonEmptyText,
      "token" -> text,
      "network" -> text) verifying ("Invalid user or password", lform => lform match {
        case (login, token, network) => network match {
          case "Yandex" => true //{ println("!!!!!!!!!!"); val b = Yandex.isSuccess(login, token); println("^^^^^^^^ " + b + "^^^^^^^^"); b }
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
            Yandex.getToken(code.head) match {
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

  def campaignReport(network: String, campaign: String) = IsAuthenticated {
    user => _ => Ok(views.html.workspace.reports.report(user, network, Json.fromJson[Campaign](Json.parse(campaign))(models.Formats.campaign).get))
  }

  def externalLogin(network: String, token: String) = IsAuthenticated {
    user => _ => Ok(views.html.workspace.campaigns.external_login(user, network, token, getLoginForm))
  }

  def externalCampaigns(network: String, token: String) = IsAuthenticated {
    user =>
      implicit request => {
        getLoginForm.bindFromRequest.fold(
          formWithErrors => BadRequest(views.html.workspace.campaigns.external_login(user, network, token, formWithErrors)),
          request => request match {
            case (login, token, network) => Ok(views.html.workspace.campaigns.external(user, network, login, token))
          })

      }
  }

}